"""工作流运行时：运行、方案版本、确认、取消、过期与幂等提交记录。

说明：为避免新增未安装的 LangGraph 依赖导致无法编译，P1.5.1 使用等价的内置状态机
（显式状态迁移 + 不可变方案版本 + 持久化事件表），后续可在保持契约不变的前提下替换为 LangGraph。
"""

import hashlib
import secrets
from datetime import UTC, datetime, timedelta
from typing import Any

from app.core.config import get_settings
from app.core.context import AgentContext
from app.core.errors import AgentError
from app.db.models import PlanCommit, PlanVersion, WorkflowEvent, WorkflowRun
from app.db.session import Database
from app.db.time import database_time
from app.integrations.java_client import JavaApiClient, JavaApiError
from app.repositories import conversation_repository, turn_repository
from app.repositories import workflow_repository as repo
from app.schemas.planning import INTENTS, PlanSnapshot

INTENT_LABELS = {
    "GOAL_PLAN": "目标规划",
    "STAGE_PLAN": "阶段规划",
    "TASK_PLAN": "任务规划",
    "WEEKLY_PLAN": "周计划",
    "DAILY_PLAN": "日计划",
    "HABIT_PLAN": "习惯规划",
    "COMPREHENSIVE_PLAN": "综合成长规划",
}
RUN_STATUS_LABELS = {
    "DRAFT": "草稿",
    "WAITING_CONFIRM": "待确认",
    "CONFIRMED": "已确认",
    "EXECUTING": "执行中",
    "SUCCEEDED": "已完成",
    "FAILED": "失败",
    "CANCELLED": "已取消",
    "EXPIRED": "已过期",
}
PLAN_STATUS_LABELS = {
    "DRAFT": "草稿",
    "WAITING_CONFIRM": "待确认",
    "CONFIRMED": "已确认",
    "SUPERSEDED": "已被替代",
    "REJECTED": "已拒绝",
    "EXPIRED": "已过期",
}
ACTIVE_RUN_STATUSES = ("DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING")
# 与 Java AgentPlanCommitServiceImpl 的 MAX_ATTEMPTS 保持一致。
MAX_COMMIT_ATTEMPTS = 10


def snapshot_hash(snapshot: dict[str, Any]) -> str:
    import json

    canonical = json.dumps(snapshot, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def _summary(intent: str, snapshot: PlanSnapshot) -> str:
    parts = [INTENT_LABELS.get(intent, intent)]
    if snapshot.goal is not None:
        parts.append(f"目标「{snapshot.goal.title}」")
    if snapshot.stages:
        parts.append(f"{len(snapshot.stages)} 个阶段")
    if snapshot.tasks:
        parts.append(f"{len(snapshot.tasks)} 个任务")
    if snapshot.weeklyPlans:
        parts.append(f"{len(snapshot.weeklyPlans)} 个周计划")
    if snapshot.dailyPlans:
        parts.append(f"{len(snapshot.dailyPlans)} 个日计划")
    if snapshot.taskAssignments:
        parts.append(f"{len(snapshot.taskAssignments)} 条任务安排")
    if snapshot.habits:
        parts.append(f"{len(snapshot.habits)} 个习惯")
    return "，".join(parts)[:1000]


def _stamp(row: WorkflowRun | PlanVersion) -> datetime:
    return max(datetime.now(UTC), row.created_at)


def _not_found() -> AgentError:
    return AgentError(404, "WORKFLOW_NOT_FOUND", "没有找到可访问的规划方案")


def _conflict(code: str, message: str) -> AgentError:
    return AgentError(409, code, message)


def run_vo(row: WorkflowRun) -> dict[str, Any]:
    progress = dict((row.graph_state or {}).get("planning", {}))
    if row.status not in ("DRAFT", "WAITING_CONFIRM") or not progress.get("phase"):
        progress["phase"] = row.status
    return {
        "id": str(row.id),
        "conversationId": str(row.conversation_id),
        "intent": row.intent,
        "intentLabel": INTENT_LABELS.get(row.intent),
        "status": row.status,
        "statusLabel": RUN_STATUS_LABELS.get(row.status),
        "latestPlanVersionId": str(row.latest_plan_version_id)
        if row.latest_plan_version_id
        else None,
        "expiresAt": row.expires_at,
        "confirmedAt": row.confirmed_at,
        "finishedAt": row.finished_at,
        "errorCode": row.error_code,
        "createdAt": row.created_at,
        "updatedAt": row.updated_at,
        "version": row.version,
        "progress": progress,
        "sourceReviewId": (row.graph_state or {}).get("sourceReviewId"),
    }


def plan_vo(row: PlanVersion) -> dict[str, Any]:
    return {
        "id": str(row.id),
        "runId": str(row.run_id),
        "versionNo": row.version_no,
        "status": row.status,
        "statusLabel": PLAN_STATUS_LABELS.get(row.status),
        "summary": row.summary,
        "snapshot": row.snapshot,
        "diffSummary": row.diff_summary,
        "expiresAt": row.expires_at,
        "confirmedAt": row.confirmed_at,
        "createdAt": row.created_at,
    }


def event_vo(row: WorkflowEvent) -> dict[str, Any]:
    return {
        "id": str(row.id),
        "eventNo": row.event_no,
        "eventType": row.event_type,
        "payload": row.payload,
        "createdAt": row.created_at,
    }


def commit_vo(row: PlanCommit) -> dict[str, Any]:
    return {
        "id": str(row.id),
        "runId": str(row.run_id),
        "planVersionId": str(row.plan_version_id),
        "status": row.status,
        "attempts": row.attempts,
        "result": row.result,
        "errorCode": row.error_code,
        "errorMessage": row.error_message,
        "createdAt": row.created_at,
        "finishedAt": row.finished_at,
    }


async def expire_stale(db: Database, user_id: int) -> int:
    """把本人超时未确认/未执行的工作流标记为过期。"""
    async with db.session() as session:
        from sqlalchemy import select

        runs = await session.scalars(
            select(WorkflowRun)
            .where(
                WorkflowRun.user_id == user_id,
                WorkflowRun.status.in_(("DRAFT", "WAITING_CONFIRM", "CONFIRMED")),
                WorkflowRun.expires_at <= datetime.now(UTC),
            )
            .with_for_update(skip_locked=True)
        )
        changed = 0
        for run in runs:
            stamp = await database_time(session, run)
            run.status = "EXPIRED"
            run.finished_at = stamp
            run.updated_at = stamp
            run.version += 1
            event_no = await repo.next_event_no(session, run.id)
            await repo.add_event(
                session,
                run_id=run.id,
                user_id=user_id,
                event_no=event_no,
                event_type="EXPIRED",
            )
            changed += 1
        return changed


async def start_run(
    db: Database,
    *,
    user_id: int,
    conversation_id: int,
    intent: str,
    source_turn_id: int | None = None,
) -> WorkflowRun:
    if intent not in INTENTS:
        raise AgentError(422, "WORKFLOW_INTENT_INVALID", "规划意图无效")
    settings = get_settings()
    await expire_stale(db, user_id)
    async with db.session() as session:
        await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        if source_turn_id is None and await turn_repository.count_running(session, conversation_id):
            raise _conflict("PLAN_CONVERSATION_RUNNING", "对话正在生成，请完成后再整理调整方案")
        if source_turn_id is not None:
            turn = await turn_repository.find_owned(
                session, user_id, conversation_id, source_turn_id
            )
            if turn is None:
                raise _not_found()
        active = await repo.find_active_run(session, conversation_id)
        if active is not None:
            raise _conflict("WORKFLOW_ACTIVE_EXISTS", "当前会话已有进行中的规划，请先完成或取消")
        run = await repo.create_run(
            session,
            user_id=user_id,
            conversation_id=conversation_id,
            source_turn_id=source_turn_id,
            intent=intent,
            status="DRAFT",
            expires_at=datetime.now(UTC) + timedelta(seconds=settings.agent_workflow_ttl_seconds),
            graph_state={},
        )
        await repo.add_event(
            session,
            run_id=run.id,
            user_id=user_id,
            event_no=1,
            event_type="RUN_STARTED",
            payload={"intent": intent},
        )
        await session.refresh(run)
        return run


async def save_plan(
    db: Database,
    *,
    user_id: int,
    run_id: int,
    snapshot: dict[str, Any],
    revision: bool = False,
) -> PlanVersion:
    try:
        parsed = PlanSnapshot.model_validate(snapshot)
    except ValueError as error:
        raise AgentError(422, "WORKFLOW_SNAPSHOT_INVALID", "方案内容不符合约定结构") from error
    if parsed.intent not in INTENTS:
        raise AgentError(422, "WORKFLOW_INTENT_INVALID", "规划意图无效")
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        if run is None:
            raise _not_found()
        if run.status not in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前方案不能再修改，请重新生成")
        if parsed.intent != run.intent:
            raise AgentError(422, "WORKFLOW_INTENT_INVALID", "修订不能改变当前规划类型")
        stamp = await database_time(session, run)
        if run.expires_at <= datetime.now(UTC):
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        previous = await repo.list_versions(session, run.id)
        for row in previous:
            if row.status in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
                row.status = "SUPERSEDED"
                row.superseded_at = stamp
                row.updated_at = stamp
                row.version += 1
        version_no = await repo.next_version_no(session, run.id)
        version = await repo.create_plan_version(
            session,
            run_id=run.id,
            user_id=user_id,
            version_no=version_no,
            status="WAITING_CONFIRM",
            summary=_summary(parsed.intent, parsed),
            snapshot=parsed.model_dump(mode="json"),
            diff_summary={
                "revision": revision,
                "previousVersionNo": version_no - 1 if revision else None,
            }
            if revision
            else None,
            expires_at=run.expires_at,
        )
        run.status = "WAITING_CONFIRM"
        run.confirmed_at = None
        run.latest_plan_version_id = version.id
        run.state_version += 1
        run.updated_at = stamp
        run.version += 1
        event_no = await repo.next_event_no(session, run.id)
        await repo.add_event(
            session,
            run_id=run.id,
            user_id=user_id,
            event_no=event_no,
            event_type="PLAN_REVISED" if revision else "PLAN_DRAFTED",
            payload={"planVersionId": str(version.id), "versionNo": version_no},
        )
        await session.refresh(version)
        await session.refresh(run)
        return version


async def confirm_plan(
    db: Database, *, user_id: int, run_id: int, plan_version_id: int
) -> PlanVersion:
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        if run is None:
            raise _not_found()
        # 已经确认的同一最新版本直接返回，重复点击或确认成功后执行失败都能继续执行。
        if run.status in ("CONFIRMED", "EXECUTING", "SUCCEEDED") \
                and run.latest_plan_version_id is not None \
                and run.latest_plan_version_id == plan_version_id:
            confirmed = await repo.find_plan_version(
                session, user_id, plan_version_id, for_update=True
            )
            if (
                confirmed is not None
                and confirmed.run_id == run.id
                and confirmed.status == "CONFIRMED"
            ):
                return confirmed
        if run.status != "WAITING_CONFIRM":
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前方案不处于待确认状态")
        if await turn_repository.count_running(session, run.conversation_id) > 0:
            raise _conflict("PLAN_CONVERSATION_RUNNING", "请等待本轮对话处理完成后再接受方案")
        if run.expires_at <= datetime.now(UTC):
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        version = await repo.find_plan_version(session, user_id, plan_version_id, for_update=True)
        if version is None or version.run_id != run.id:
            raise _not_found()
        if run.latest_plan_version_id != version.id:
            raise _conflict("PLAN_STATUS_INVALID", "只能确认当前最新方案")
        if version.status != "WAITING_CONFIRM":
            raise _conflict("PLAN_STATUS_INVALID", "该方案版本不处于待确认状态")
        stamp = await database_time(session, run, version)
        token = hashlib.sha256(secrets.token_bytes(32)).hexdigest()
        version.status = "CONFIRMED"
        version.confirmation_token = token
        version.confirmed_at = stamp
        version.updated_at = stamp
        version.version += 1
        run.status = "CONFIRMED"
        run.confirmed_at = stamp
        run.state_version += 1
        run.updated_at = stamp
        run.version += 1
        event_no = await repo.next_event_no(session, run.id)
        await repo.add_event(
            session,
            run_id=run.id,
            user_id=user_id,
            event_no=event_no,
            event_type="CONFIRMED",
            payload={"planVersionId": str(version.id)},
        )
        await session.refresh(version)
        return version


async def cancel_run(db: Database, *, user_id: int, run_id: int) -> None:
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        if run is None:
            raise _not_found()
        if run.status not in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前状态不能取消")
        stamp = await database_time(session, run)
        run.status = "CANCELLED"
        run.finished_at = stamp
        run.updated_at = stamp
        run.version += 1
        for version in await repo.list_versions(session, run.id):
            if version.status in ("DRAFT", "WAITING_CONFIRM"):
                version.status = "REJECTED"
                version.superseded_at = stamp
                version.updated_at = stamp
                version.version += 1
        event_no = await repo.next_event_no(session, run.id)
        await repo.add_event(
            session, run_id=run.id, user_id=user_id, event_no=event_no, event_type="CANCELLED"
        )


async def begin_commit(db: Database, *, user_id: int, run_id: int) -> PlanCommit:
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        if run is None:
            raise _not_found()
        if run.latest_plan_version_id is not None:
            existing = await repo.find_commit_by_version(session, run.latest_plan_version_id)
            if existing is not None and existing.status == "SUCCEEDED":
                return existing
        if run.status not in ("CONFIRMED", "EXECUTING", "FAILED"):
            raise _conflict("WORKFLOW_STATUS_INVALID", "方案尚未确认，不能提交")
        if run.latest_plan_version_id is None:
            raise _not_found()
        version = await repo.find_plan_version(
            session, user_id, run.latest_plan_version_id, for_update=True
        )
        if version is None or version.status != "CONFIRMED":
            raise _conflict("PLAN_STATUS_INVALID", "方案版本未确认")
        existing = await repo.find_commit_by_version(session, version.id)
        if existing is not None:
            if existing.status == "FAILED" and existing.attempts >= MAX_COMMIT_ATTEMPTS:
                raise _conflict("PLAN_COMMIT_EXHAUSTED", "提交尝试次数已用完，请重新生成方案")
            run.status = "EXECUTING"
            run.finished_at = None
            run.error_code = None
            run.updated_at = await database_time(session, run)
            run.version += 1
            return existing
        if run.status == "FAILED":
            raise _conflict("PLAN_COMMIT_MISSING", "失败状态没有可重试的提交记录，请重新生成方案")
        if run.expires_at <= datetime.now(UTC):
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        stamp = await database_time(session, run)
        key = hashlib.sha256(
            f"{run.id}:{version.id}:{snapshot_hash(version.snapshot)}".encode()
        ).hexdigest()
        commit = await repo.create_commit(
            session,
            user_id=user_id,
            run_id=run.id,
            plan_version_id=version.id,
            idempotency_key=key,
            request_hash=snapshot_hash(version.snapshot),
            status="PENDING",
        )
        run.status = "EXECUTING"
        run.updated_at = stamp
        run.version += 1
        event_no = await repo.next_event_no(session, run.id)
        await repo.add_event(
            session,
            run_id=run.id,
            user_id=user_id,
            event_no=event_no,
            event_type="COMMIT_STARTED",
            payload={"planCommitId": str(commit.id), "idempotencyKey": key},
        )
        await session.refresh(commit)
        return commit


async def finish_commit(
    db: Database,
    *,
    user_id: int,
    commit_id: int,
    success: bool,
    result: dict[str, Any] | None = None,
    error_code: str | None = None,
    error_message: str | None = None,
) -> PlanCommit:
    async with db.session() as session:
        commit = await session.get(PlanCommit, commit_id)
        if commit is None or commit.user_id != user_id:
            raise _not_found()
        run = await repo.find_run(session, user_id, commit.run_id, for_update=True)
        if run is None:
            raise _not_found()
        # 提交记录由 Java 与业务事务一起写；Python 只投影工作流状态。
        # 网络超时不能覆盖 Java 的成功回执，也不能把未知结果伪装成失败。
        if commit.status not in ("SUCCEEDED", "FAILED"):
            run.error_code = error_code
            run.updated_at = await database_time(session, run, commit)
            return commit
        target = "SUCCEEDED" if commit.status == "SUCCEEDED" else "FAILED"
        if run.status == target:
            return commit
        stamp = await database_time(session, run, commit)
        run.status = target
        run.error_code = commit.error_code
        run.finished_at = stamp
        run.updated_at = stamp
        run.version += 1
        event_no = await repo.next_event_no(session, run.id)
        await repo.add_event(
            session,
            run_id=run.id,
            user_id=user_id,
            event_no=event_no,
            event_type="COMMIT_SUCCEEDED" if target == "SUCCEEDED" else "COMMIT_FAILED",
            payload={"planCommitId": str(commit.id), "errorCode": commit.error_code},
        )
        await session.refresh(commit)
        return commit


async def detail(db: Database, *, user_id: int, run_id: int) -> dict[str, Any]:
    await expire_stale(db, user_id)
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id)
        if run is None:
            raise _not_found()
        commit = (
            await repo.find_commit_by_version(session, run.latest_plan_version_id)
            if run.latest_plan_version_id
            else None
        )
        if commit is not None and commit.user_id != user_id:
            raise _not_found()
    if (
        commit is not None
        and commit.status in ("SUCCEEDED", "FAILED")
        and run.status == "EXECUTING"
    ):
        await finish_commit(
            db, user_id=user_id, commit_id=commit.id, success=commit.status == "SUCCEEDED"
        )
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id)
        if run is None:
            raise _not_found()
        versions = await repo.list_versions(session, run.id)
        events = await repo.list_events(session, run.id)
        return {
            "run": run_vo(run),
            "plans": [plan_vo(row) for row in versions],
            "events": [event_vo(row) for row in events],
            "commit": commit_vo(commit) if commit is not None else None,
        }


async def page_runs(
    db: Database, *, user_id: int, status: str | None, page: int, size: int,
    conversation_id: int | None = None,
) -> tuple[list[WorkflowRun], int]:
    await expire_stale(db, user_id)
    async with db.session() as session:
        return await repo.page_runs(
            session, user_id=user_id, status=status, page_number=page, size=size,
            conversation_id=conversation_id,
        )


async def execute_commit(
    db: Database,
    *,
    context: AgentContext,
    java_client: JavaApiClient,
    run_id: int,
) -> PlanCommit:
    """调用 Java 原子提交接口；已成功的幂等提交直接返回既有结果。"""
    user_id = int(context.user_id)
    commit = await begin_commit(db, user_id=user_id, run_id=run_id)
    if commit.status == "SUCCEEDED":
        return await finish_commit(db, user_id=user_id, commit_id=commit.id, success=True)
    async with db.session() as session:
        run = await repo.find_run(session, user_id, run_id)
        if run is None or run.latest_plan_version_id is None:
            raise _not_found()
        version = await repo.find_plan_version(session, user_id, run.latest_plan_version_id)
        if version is None:
            raise _not_found()
        payload = {
            "runId": str(run.id),
            "planVersionId": str(version.id),
            "idempotencyKey": commit.idempotency_key,
            "requestHash": commit.request_hash,
            "snapshot": version.snapshot,
        }
    try:
        result = await java_client.post_agent(
            "agent/plans/commit",
            authorization=context.authorization,
            trace_id=context.trace_id,
            payload=payload,
        )
    except JavaApiError as error:
        reconciled = await finish_commit(
            db,
            user_id=user_id,
            commit_id=commit.id,
            success=False,
            error_code=error.code,
            error_message=error.message,
        )
        if reconciled.status == "SUCCEEDED":
            return reconciled
        raise AgentError(error.status_code, error.code, error.message) from error
    data = result.data if isinstance(result.data, dict) else {}
    if data.get("status") != "SUCCEEDED" or str(data.get("commitId")) != str(commit.id):
        raise AgentError(502, "PLAN_COMMIT_RESULT_INVALID", "提交结果尚未核实，请刷新规划状态")
    return await finish_commit(
        db,
        user_id=user_id,
        commit_id=commit.id,
        success=True,
        result=data,
    )
