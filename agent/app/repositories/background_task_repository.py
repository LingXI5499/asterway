"""后台任务数据访问：单 worker 领取、状态推进与幂等入队。"""

from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import AgentError
from app.db.models import BackgroundTask
from app.db.time import database_time

_TERMINAL = {"SUCCEEDED", "FAILED", "CANCELLED"}


def _stamp(row: BackgroundTask) -> datetime:
    return max(datetime.now(UTC), row.created_at)


async def enqueue(
    session: AsyncSession,
    *,
    user_id: int,
    kind: str,
    ref_type: str,
    ref_id: int,
) -> BackgroundTask:
    existing = await session.scalar(
        select(BackgroundTask).where(
            BackgroundTask.user_id == user_id,
            BackgroundTask.kind == kind,
            BackgroundTask.ref_type == ref_type,
            BackgroundTask.ref_id == ref_id,
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )
    if existing is not None:
        if existing.status == "RUNNING":
            # 内容已改变时重新排队；旧领取者不能覆盖这次更新。
            existing.status = "PENDING"
            existing.started_at = None
            existing.finished_at = None
            existing.error_code = None
            existing.error_message = None
            existing.updated_at = await database_time(session, existing)
            existing.version += 1
        return existing
    row = BackgroundTask(
        user_id=user_id,
        kind=kind,
        ref_type=ref_type,
        ref_id=ref_id,
        status="PENDING",
    )
    session.add(row)
    await session.flush()
    return row


async def claim_next(session: AsyncSession) -> BackgroundTask | None:
    row = await session.scalar(
        select(BackgroundTask)
        .where(
            BackgroundTask.status == "PENDING",
            BackgroundTask.scheduled_at <= func.clock_timestamp(),
        )
        .order_by(BackgroundTask.scheduled_at, BackgroundTask.id)
        .with_for_update(skip_locked=True)
        .limit(1)
    )
    if row is None:
        return None
    row.status = "RUNNING"
    row.started_at = await database_time(session, row)
    row.updated_at = row.started_at
    row.attempts += 1
    row.version += 1
    await session.flush()
    return row


async def finish(
    session: AsyncSession,
    row: BackgroundTask,
    *,
    status: str,
    error_code: str | None = None,
    error_message: str | None = None,
) -> None:
    stamp = await database_time(session, row)
    row.status = status
    row.error_code = error_code
    row.error_message = error_message
    row.finished_at = stamp
    row.updated_at = stamp
    row.version += 1
    await session.flush()


async def cancel_for_ref(session: AsyncSession, *, kind: str, ref_type: str, ref_id: int) -> None:

    rows = await session.scalars(
        select(BackgroundTask).where(
            BackgroundTask.kind == kind,
            BackgroundTask.ref_type == ref_type,
            BackgroundTask.ref_id == ref_id,
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )
    for row in rows:
        stamp = await database_time(session, row)
        row.status = "CANCELLED"
        row.finished_at = stamp
        row.updated_at = stamp
    await session.flush()


async def find_active(
    session: AsyncSession, *, kind: str, ref_type: str, ref_id: int
) -> BackgroundTask | None:
    return await session.scalar(
        select(BackgroundTask).where(
            BackgroundTask.kind == kind,
            BackgroundTask.ref_type == ref_type,
            BackgroundTask.ref_id == ref_id,
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )


async def recover_stale_running(
    session: AsyncSession, *, timeout_seconds: float, max_attempts: int
) -> list[BackgroundTask]:
    """把超时 RUNNING 任务退回 PENDING；达到最大尝试次数时置为 FAILED。

    返回本次变更的任务，供后台循环补齐文档、复盘等关联资源状态。
    """
    from datetime import timedelta

    from sqlalchemy import select

    cutoff = func.clock_timestamp() - timedelta(seconds=timeout_seconds)
    rows = list(
        await session.scalars(
            select(BackgroundTask)
            .where(
                BackgroundTask.status == "RUNNING",
                BackgroundTask.started_at < cutoff,
            )
            .with_for_update(skip_locked=True)
        )
    )
    for row in rows:
        stamp = await database_time(session, row)
        if row.attempts >= max_attempts:
            row.status = "FAILED"
            row.error_code = "TASK_TIMEOUT"
            row.error_message = "后台任务超时且超过最大尝试次数"
            row.finished_at = stamp
        else:
            row.status = "PENDING"
            row.started_at = None
        row.updated_at = stamp
        row.version += 1
    await session.flush()
    return rows


async def require_claim(session: AsyncSession, task: BackgroundTask) -> BackgroundTask:
    """写入结果前验证领取代次，防止超时或取消后的旧任务覆盖新结果。"""
    row = await session.get(BackgroundTask, task.id, with_for_update=True)
    if (
        row is None
        or row.status != "RUNNING"
        or row.user_id != task.user_id
        or row.attempts != task.attempts
        or row.version != task.version
    ):
        raise AgentError(409, "TASK_CLAIM_LOST", "后台任务已取消或由新的执行接管")
    return row


async def page_tasks(
    session: AsyncSession,
    *,
    user_id: int,
    status: str | None,
    page_number: int,
    size: int,
) -> tuple[list[BackgroundTask], int]:
    from sqlalchemy import func

    conditions = [BackgroundTask.user_id == user_id]
    if status:
        conditions.append(BackgroundTask.status == status)
    total = await session.scalar(
        select(func.count()).select_from(BackgroundTask).where(*conditions)
    )
    rows = await session.scalars(
        select(BackgroundTask)
        .where(*conditions)
        .order_by(BackgroundTask.created_at.desc(), BackgroundTask.id.desc())
        .offset((page_number - 1) * size)
        .limit(size)
    )
    return list(rows), int(total or 0)


__all__ = [
    "cancel_for_ref",
    "claim_next",
    "enqueue",
    "find_active",
    "finish",
    "page_tasks",
    "recover_stale_running",
]
