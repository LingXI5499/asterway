"""单进程后台任务循环：解析文档、向量化文档与记忆；不并发抢占同一任务。"""

import asyncio
import logging
from contextlib import suppress

from app.core.config import get_settings
from app.core.errors import AgentError
from app.db.models import BackgroundTask
from app.db.session import Database
from app.repositories import background_task_repository as task_repo
from app.services import knowledge_service, memory_extraction_service, review_service
from app.services.observation_service import Observation, current_observation

logger = logging.getLogger(__name__)


class BackgroundWorker:
    def __init__(self, db: Database) -> None:
        self._db = db
        self._task: asyncio.Task[None] | None = None

    async def start(self) -> None:
        if self._task is None:
            self._task = asyncio.create_task(self._loop(), name="agent-background-worker")

    async def stop(self) -> None:
        task, self._task = self._task, None
        if task is None:
            return
        task.cancel()
        with suppress(asyncio.CancelledError):
            await task

    async def _loop(self) -> None:
        interval = get_settings().agent_background_poll_seconds
        while True:
            try:
                worked = await self._run_once()
            except asyncio.CancelledError:
                raise
            except Exception as error:  # noqa: BLE001 - 后台任务失败不能终止整个循环
                logger.warning("agent_background_loop_failed type=%s", type(error).__name__)
                worked = False
            if not worked:
                await asyncio.sleep(interval)

    async def _run_once(self) -> bool:
        settings = get_settings()
        async with self._db.session() as session:
            recovered = await task_repo.recover_stale_running(
                session,
                timeout_seconds=settings.agent_background_task_timeout_seconds,
                max_attempts=settings.agent_background_max_attempts,
            )
            task = await task_repo.claim_next(session)
        # 超时回收的终态任务要同步关联资源，避免文档或复盘一直停留在处理中。
        for row in recovered:
            if row.status == "FAILED":
                await self._reconcile_recovered(row)
        if task is None:
            return False
        status, code, message = "SUCCEEDED", None, None
        observation_token = current_observation.set(
            Observation(self._db, task.user_id, f"background-{task.id}-{task.attempts}")
        )
        try:
            try:
                async with asyncio.timeout(settings.agent_background_task_timeout_seconds):
                    await self._process(task)
            except TimeoutError:
                status, code, message = "FAILED", "TASK_TIMEOUT", "后台任务执行超时"
                if task.kind == "DOCUMENT_PARSE":
                    await knowledge_service._document_failed(
                        self._db, task.user_id, task.ref_id, code, message, task=task
                    )
            except AgentError as error:
                status, code, message = "FAILED", error.code, error.message
            except Exception as error:  # noqa: BLE001 - 单个任务失败只记录结果
                logger.warning(
                    "agent_background_task_failed id=%s type=%s", task.id, type(error).__name__
                )
                status, code, message = "FAILED", "TASK_FAILED", "后台任务执行失败"
            if status == "FAILED":
                await self._reconcile_task_failed(task, code or "TASK_FAILED",
                    message or "后台任务执行失败")
        finally:
            current_observation.reset(observation_token)
        async with self._db.session() as session:
            row = await session.get(BackgroundTask, task.id, with_for_update=True)
            if (
                row is not None
                and row.status == "RUNNING"
                and row.attempts == task.attempts
                and row.version == task.version
            ):
                await task_repo.finish(
                    session, row, status=status, error_code=code, error_message=message
                )
        return True

    async def _reconcile_task_failed(
        self, task: BackgroundTask, code: str, message: str
    ) -> None:
        """执行失败时同步资源状态；所有写入都必须复核任务领取代次。"""
        try:
            if task.kind == "REVIEW_GENERATE":
                await review_service.failed(self._db, task, message)
            elif task.kind == "DOCUMENT_EMBEDDING":
                await knowledge_service.revision_index_failed(self._db, task, code, message)
        except AgentError:
            return
        except Exception as error:  # noqa: BLE001 - 补偿失败不覆盖任务终态
            logger.warning("agent_background_reconcile_failed kind=%s type=%s",
                task.kind, type(error).__name__)

    async def _reconcile_recovered(self, task: BackgroundTask) -> None:
        """超时回收到终态的任务：按仍处于处理中的状态补齐关联资源。"""
        try:
            if task.kind == "REVIEW_GENERATE":
                await review_service.recover_failed(
                    self._db, user_id=task.user_id, report_id=task.ref_id,
                    message="后台任务超时且超过最大尝试次数，可重新生成")
            elif task.kind == "DOCUMENT_PARSE":
                await knowledge_service.recover_parse_failed(self._db, task)
            elif task.kind == "DOCUMENT_EMBEDDING":
                await knowledge_service.recover_embedding_failed(self._db, task)
        except Exception as error:  # noqa: BLE001 - 回收补偿失败不终止后台循环
            logger.warning("agent_background_recover_failed kind=%s type=%s",
                task.kind, type(error).__name__)

    async def _process(self, task: BackgroundTask) -> None:
        if task.kind == "DOCUMENT_PARSE":
            await knowledge_service.process_parse(self._db, task)
        elif task.kind == "DOCUMENT_EMBEDDING":
            await knowledge_service.process_embedding(self._db, task)
        elif task.kind == "MEMORY_EMBEDDING":
            await knowledge_service.process_memory_embedding(self._db, task)
        elif task.kind == "MEMORY_EXTRACT":
            await memory_extraction_service.process(self._db, task)
        elif task.kind == "REVIEW_GENERATE":
            await review_service.process(self._db, task)
        else:
            raise AgentError(422, "TASK_KIND_INVALID", "未知的后台任务类型")
