# 依次领取后台任务，定期回收超时任务并补齐失败状态。
# 单进程后台任务循环：解析文档、向量化文档与记忆；不并发抢占同一任务。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 logging，供下面调用其功能。
import logging

# 从 contextlib 引入 suppress。
from contextlib import suppress

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 后台任务（BackgroundTask）。
from app.db.models import BackgroundTask

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.repositories 引入 任务repo（task_repo）。
from app.repositories import background_task_repository as task_repo

# 从 app.services 引入 知识资料业务服务（knowledge_service）、长期记忆extraction业务服务（memory_extraction_service）、复盘业务服务（review_service）。
from app.services import knowledge_service, memory_extraction_service, review_service

# 从 app.services.observation_service 引入 调用观测（Observation）、当前调用观测（current_observation）。
from app.services.observation_service import Observation, current_observation

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 定义 BackgroundWorker，把后台后台处理器需要的数据与操作放在同一个对象中。
class BackgroundWorker:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 Agent 数据库访问器（db）。
    def __init__(self, db: Database) -> None:
        # 把Agent 数据库访问器（db）保存到 当前实例中的Agent 数据库访问器（self._db）。
        self._db = db
        # 声明 当前实例中的任务（self._task），类型为 asyncio.Task[None] | None；其内容为空值，表示当前还没有对应内容。
        self._task: asyncio.Task[None] | None = None

    # 异步函数 start：调用 start 完成开始处理。
    async def start(self) -> None:
        # 检查self._task is None，即对应内容是否不存在；成立时执行缩进的分支。
        if self._task is None:
            # 把执行“启动可同时推进的异步任务，并保存任务句柄”得到的结果保存到 当前实例中的任务（self._task）。
            self._task = asyncio.create_task(self._loop(), name="agent-background-worker")

    # 异步函数 stop：调用 stop 完成停止处理。
    async def stop(self) -> None:
        # 把依次包含 当前实例（self）的任务（_task）属性、空值，表示当前还没有对应内容 的集合保存到 任务self任务（(task, self._task)）。
        task, self._task = self._task, None
        # 检查task is None，即对应内容是否不存在；成立时执行缩进的分支。
        if task is None:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 调用 task.cancel 完成取消处理。
        task.cancel()
        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with suppress(asyncio.CancelledError):
            # 等待任务（task）完成后的结果。
            await task

    # 异步函数 _loop：调用 _loop 完成loop处理。
    async def _loop(self) -> None:
        # 把执行“读取并复用当前进程的运行配置”得到的结果的成长助手后台轮询秒数（agent_background_poll_seconds）属性保存到 间隔（interval）。
        interval = get_settings().agent_background_poll_seconds
        # 只要条件 True 是否成立，就继续执行下面的循环。
        while True:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 把等待执行“调用 self._run_once 完成方案运行once处理”得到的结果完成后的结果保存到 worked。
                worked = await self._run_once()
            # 捕获asyncio.CancelledError异常，执行下面的错误处理。
            except asyncio.CancelledError:
                # 中止本轮正常处理，把当前异常继续向上交给调用方。
                raise
            # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
            except Exception as error:  # noqa: BLE001 - 后台任务失败不能终止整个循环
                # 记录warning级别的运行信息，供定位处理过程和失败原因。
                logger.warning("agent_background_loop_failed type=%s", type(error).__name__)
                # 把假，表示条件不成立或开关关闭保存到 worked。
                worked = False
            # 检查条件 not worked 是否成立；成立时执行缩进的分支。
            if not worked:
                # 等待等待指定时间后再继续，给轮询或其他任务留出执行机会完成后再继续。
                await asyncio.sleep(interval)

    # 异步函数 _run_once：调用 _run_once 完成方案运行once处理。
    async def _run_once(self) -> bool:
        # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
        settings = get_settings()
        # 异步进入执行“调用 self._db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with self._db.session() as session:
            # 把等待执行“调用 task_repo.recover_stale_running 完成恢复stale运行中处理”得到的结果完成后的结果保存到 recovered。
            recovered = await task_repo.recover_stale_running(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入超时秒数（timeout_seconds），值为表达式 settings.agent_background_task_timeout_seconds 的结果。
                timeout_seconds=settings.agent_background_task_timeout_seconds,
                # 传入最大尝试次数（max_attempts），值为表达式 settings.agent_background_max_attempts 的结果。
                max_attempts=settings.agent_background_max_attempts,
            )
            # 把等待执行“调用 task_repo.claim_next 完成claim下一个处理”得到的结果完成后的结果保存到 任务（task）。
            task = await task_repo.claim_next(session)
        # 超时回收的终态任务要同步关联资源，避免文档或复盘一直停留在处理中。
        # 从recovered依次取出当前记录（row），每项执行一次下面的处理。
        for row in recovered:
            # 检查条件 row.status == 'FAILED' 是否成立；成立时执行缩进的分支。
            if row.status == "FAILED":
                # 等待调用 self._reconcile_recovered 完成reconcilerecovered处理完成后再继续。
                await self._reconcile_recovered(row)
        # 检查task is None，即对应内容是否不存在；成立时执行缩进的分支。
        if task is None:
            # 把假，表示条件不成立或开关关闭交还给调用方，本函数到此结束。
            return False
        # 把依次包含 固定状态或编码 SUCCEEDED（已成功）、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容 的集合保存到 状态编码消息（(status, code, message)）。
        status, code, message = "SUCCEEDED", None, None
        # 把执行“调用 current_observation.set 完成设置处理”得到的结果保存到 调用观测身份凭据（observation_token）。
        observation_token = current_observation.set(
            # 继续传入或组合调用 Observation 完成调用观测处理，完成上面尚未结束的表达式。
            Observation(self._db, task.user_id, f"background-{task.id}-{task.attempts}")
        )
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 异步进入执行“调用 asyncio.timeout 完成超时处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
                async with asyncio.timeout(settings.agent_background_task_timeout_seconds):
                    # 等待调用 self._process 完成处理过程处理完成后再继续。
                    await self._process(task)
            # 捕获TimeoutError异常，执行下面的错误处理。
            except TimeoutError:
                # 把依次包含 固定状态或编码 FAILED（失败）、固定状态或编码 TASK_TIMEOUT（任务超时）、固定文字 '后台任务执行超时' 的集合保存到 状态编码消息（(status, code, message)）。
                status, code, message = "FAILED", "TASK_TIMEOUT", "后台任务执行超时"
                # 检查条件 task.kind == 'DOCUMENT_PARSE' 是否成立；成立时执行缩进的分支。
                if task.kind == "DOCUMENT_PARSE":
                    # 等待调用 knowledge_service._document_failed 完成知识文档失败处理完成后再继续。
                    await knowledge_service._document_failed(
                        # 继续传入或组合表达式 self._db, task.user_id, task.ref_id, code, message, task=task 的结果，完成上面尚未结束的表达式。
                        self._db, task.user_id, task.ref_id, code, message, task=task
                    )
            # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
            except AgentError as error:
                # 把依次包含 固定状态或编码 FAILED（失败）、错误（error）的编码（code）属性、错误（error）的消息（message）属性 的集合保存到 状态编码消息（(status, code, message)）。
                status, code, message = "FAILED", error.code, error.message
            # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
            except Exception as error:  # noqa: BLE001 - 单个任务失败只记录结果
                # 记录warning级别的运行信息，供定位处理过程和失败原因。
                logger.warning(
                    # 继续传入或组合表达式 "agent_background_task_failed id=%s type=%s", task.id, type(error).__name__ 的结果，完成上面尚未结束的表达式。
                    "agent_background_task_failed id=%s type=%s", task.id, type(error).__name__
                )
                # 把依次包含 固定状态或编码 FAILED（失败）、固定状态或编码 TASK_FAILED（任务失败）、固定文字 '后台任务执行失败' 的集合保存到 状态编码消息（(status, code, message)）。
                status, code, message = "FAILED", "TASK_FAILED", "后台任务执行失败"
            # 检查条件 status == 'FAILED' 是否成立；成立时执行缩进的分支。
            if status == "FAILED":
                # 等待调用 self._reconcile_task_failed 完成reconcile任务失败处理完成后再继续。
                await self._reconcile_task_failed(task, code or "TASK_FAILED",
                    # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：message or "后台任务执行失败")，完成上面尚未结束的表达式。
                    message or "后台任务执行失败")
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 调用 current_observation.reset 完成reset处理。
            current_observation.reset(observation_token)
        # 异步进入执行“调用 self._db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with self._db.session() as session:
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
            row = await session.get(BackgroundTask, task.id, with_for_update=True)
            # 检查当前记录版本是否符合请求要求：row is not None and row.status == 'RUNNING' and (row.attempts == task.attempts) and (row.version == task.version)；成立时执行缩进的分支。
            if (
                # 继续传入或组合条件判断的结果：row is not None，完成上面尚未结束的表达式。
                row is not None
                # 继续补充本次调用或判断：条件判断的结果：and row.status == "RUNNING"。
                and row.status == "RUNNING"
                # 继续补充本次调用或判断：条件判断的结果：and row.attempts == task.attempts。
                and row.attempts == task.attempts
                # 继续补充本次调用或判断：条件判断的结果：and row.version == task.version。
                and row.version == task.version
            ):
                # 等待调用 task_repo.finish 完成finish处理完成后再继续。
                await task_repo.finish(
                    # 继续传入或组合表达式 session, row, status=status, error_code=code, error_message=message 的结果，完成上面尚未结束的表达式。
                    session, row, status=status, error_code=code, error_message=message
                )
        # 把真，表示条件成立或开关打开交还给调用方，本函数到此结束。
        return True

    # 异步函数 _reconcile_task_failed：执行失败时同步资源状态；所有写入都必须复核任务领取代次。；接收 任务（task）、编码（code）、消息（message）。
    async def _reconcile_task_failed(
        # 继续传入或组合表达式 self, task: BackgroundTask, code: str, message: str 的结果，完成上面尚未结束的表达式。
        self, task: BackgroundTask, code: str, message: str
    # 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
    ) -> None:
        # 执行失败时同步资源状态；所有写入都必须复核任务领取代次。
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 检查条件 task.kind == 'REVIEW_GENERATE' 是否成立；成立时执行缩进的分支。
            if task.kind == "REVIEW_GENERATE":
                # 等待调用 review_service.failed 完成失败处理完成后再继续。
                await review_service.failed(self._db, task, message)
            # 检查条件 task.kind == 'DOCUMENT_EMBEDDING' 是否成立；成立时执行缩进的分支。
            elif task.kind == "DOCUMENT_EMBEDDING":
                # 等待调用 knowledge_service.revision_index_failed 完成修订版本索引失败处理完成后再继续。
                await knowledge_service.revision_index_failed(self._db, task, code, message)
        # 捕获AgentError异常，执行下面的错误处理。
        except AgentError:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
        except Exception as error:  # noqa: BLE001 - 补偿失败不覆盖任务终态
            # 记录warning级别的运行信息，供定位处理过程和失败原因。
            logger.warning("agent_background_reconcile_failed kind=%s type=%s",
                # 继续传入或组合表达式 task.kind, type(error).__name__) 的结果，完成上面尚未结束的表达式。
                task.kind, type(error).__name__)

    # 异步函数 _reconcile_recovered：超时回收到终态的任务：按仍处于处理中的状态补齐关联资源。；接收 任务（task）。
    async def _reconcile_recovered(self, task: BackgroundTask) -> None:
        # 超时回收到终态的任务：按仍处于处理中的状态补齐关联资源。
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 检查条件 task.kind == 'REVIEW_GENERATE' 是否成立；成立时执行缩进的分支。
            if task.kind == "REVIEW_GENERATE":
                # 等待调用 review_service.recover_failed 完成恢复失败处理完成后再继续。
                await review_service.recover_failed(
                    # 继续传入或组合表达式 self._db, user_id=task.user_id, report_id=task.ref_id 的结果，完成上面尚未结束的表达式。
                    self._db, user_id=task.user_id, report_id=task.ref_id,
                    # 传入消息（message），值为表达式 "后台任务超时且超过最大尝试次数，可重新生成") 的结果。
                    message="后台任务超时且超过最大尝试次数，可重新生成")
            # 检查条件 task.kind == 'DOCUMENT_PARSE' 是否成立；成立时执行缩进的分支。
            elif task.kind == "DOCUMENT_PARSE":
                # 等待调用 knowledge_service.recover_parse_failed 完成恢复解析失败处理完成后再继续。
                await knowledge_service.recover_parse_failed(self._db, task)
            # 检查条件 task.kind == 'DOCUMENT_EMBEDDING' 是否成立；成立时执行缩进的分支。
            elif task.kind == "DOCUMENT_EMBEDDING":
                # 等待调用 knowledge_service.recover_embedding_failed 完成恢复向量失败处理完成后再继续。
                await knowledge_service.recover_embedding_failed(self._db, task)
        # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
        except Exception as error:  # noqa: BLE001 - 回收补偿失败不终止后台循环
            # 记录warning级别的运行信息，供定位处理过程和失败原因。
            logger.warning("agent_background_recover_failed kind=%s type=%s",
                # 继续传入或组合表达式 task.kind, type(error).__name__) 的结果，完成上面尚未结束的表达式。
                task.kind, type(error).__name__)

    # 异步函数 _process：调用 _process 完成处理过程处理；接收 任务（task）。
    async def _process(self, task: BackgroundTask) -> None:
        # 检查条件 task.kind == 'DOCUMENT_PARSE' 是否成立；成立时执行缩进的分支。
        if task.kind == "DOCUMENT_PARSE":
            # 等待调用 knowledge_service.process_parse 完成处理过程解析处理完成后再继续。
            await knowledge_service.process_parse(self._db, task)
        # 检查条件 task.kind == 'DOCUMENT_EMBEDDING' 是否成立；成立时执行缩进的分支。
        elif task.kind == "DOCUMENT_EMBEDDING":
            # 等待调用 knowledge_service.process_embedding 完成处理过程向量处理完成后再继续。
            await knowledge_service.process_embedding(self._db, task)
        # 检查条件 task.kind == 'MEMORY_EMBEDDING' 是否成立；成立时执行缩进的分支。
        elif task.kind == "MEMORY_EMBEDDING":
            # 等待调用 knowledge_service.process_memory_embedding 完成处理过程长期记忆向量处理完成后再继续。
            await knowledge_service.process_memory_embedding(self._db, task)
        # 检查条件 task.kind == 'MEMORY_EXTRACT' 是否成立；成立时执行缩进的分支。
        elif task.kind == "MEMORY_EXTRACT":
            # 等待调用 memory_extraction_service.process 完成处理过程处理完成后再继续。
            await memory_extraction_service.process(self._db, task)
        # 检查条件 task.kind == 'REVIEW_GENERATE' 是否成立；成立时执行缩进的分支。
        elif task.kind == "REVIEW_GENERATE":
            # 等待调用 review_service.process 完成处理过程处理完成后再继续。
            await review_service.process(self._db, task)
        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
        else:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(422, "TASK_KIND_INVALID", "未知的后台任务类型")
