"""工作流运行、方案版本、事件与提交记录的数据访问。"""

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import PlanCommit, PlanVersion, WorkflowEvent, WorkflowRun


async def create_run(session: AsyncSession, **values: object) -> WorkflowRun:
    row = WorkflowRun(**values)
    session.add(row)
    await session.flush()
    return row


async def find_run(
    session: AsyncSession, user_id: int, run_id: int, *, for_update: bool = False
) -> WorkflowRun | None:
    query = select(WorkflowRun).where(WorkflowRun.id == run_id, WorkflowRun.user_id == user_id)
    if for_update:
        query = query.with_for_update()
    return await session.scalar(query)


async def find_active_run(session: AsyncSession, conversation_id: int) -> WorkflowRun | None:
    return await session.scalar(
        select(WorkflowRun).where(
            WorkflowRun.conversation_id == conversation_id,
            WorkflowRun.status.in_(("DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING")),
        )
    )


async def next_event_no(session: AsyncSession, run_id: int) -> int:
    value = await session.scalar(
        select(func.coalesce(func.max(WorkflowEvent.event_no), 0)).where(
            WorkflowEvent.run_id == run_id
        )
    )
    return int(value or 0) + 1


async def add_event(
    session: AsyncSession,
    *,
    run_id: int,
    user_id: int,
    event_no: int,
    event_type: str,
    payload: dict[str, object] | None = None,
) -> WorkflowEvent:
    row = WorkflowEvent(
        run_id=run_id,
        user_id=user_id,
        event_no=event_no,
        event_type=event_type,
        payload=payload or {},
    )
    session.add(row)
    await session.flush()
    return row


async def next_version_no(session: AsyncSession, run_id: int) -> int:
    value = await session.scalar(
        select(func.coalesce(func.max(PlanVersion.version_no), 0)).where(
            PlanVersion.run_id == run_id
        )
    )
    return int(value or 0) + 1


async def create_plan_version(session: AsyncSession, **values: object) -> PlanVersion:
    row = PlanVersion(**values)
    session.add(row)
    await session.flush()
    return row


async def find_plan_version(
    session: AsyncSession,
    user_id: int,
    plan_version_id: int,
    *,
    for_update: bool = False,
) -> PlanVersion | None:
    query = select(PlanVersion).where(
        PlanVersion.id == plan_version_id, PlanVersion.user_id == user_id
    )
    if for_update:
        query = query.with_for_update()
    return await session.scalar(query)


async def list_versions(session: AsyncSession, run_id: int) -> list[PlanVersion]:
    rows = await session.scalars(
        select(PlanVersion).where(PlanVersion.run_id == run_id).order_by(PlanVersion.version_no)
    )
    return list(rows)


async def list_events(session: AsyncSession, run_id: int) -> list[WorkflowEvent]:
    rows = await session.scalars(
        select(WorkflowEvent).where(WorkflowEvent.run_id == run_id).order_by(WorkflowEvent.event_no)
    )
    return list(rows)


async def create_commit(session: AsyncSession, **values: object) -> PlanCommit:
    row = PlanCommit(**values)
    session.add(row)
    await session.flush()
    return row


async def find_commit_by_version(
    session: AsyncSession, plan_version_id: int
) -> PlanCommit | None:
    return await session.scalar(
        select(PlanCommit).where(PlanCommit.plan_version_id == plan_version_id)
    )


async def page_runs(
    session: AsyncSession,
    *,
    user_id: int,
    status: str | None,
    page_number: int,
    size: int,
    conversation_id: int | None = None,
) -> tuple[list[WorkflowRun], int]:
    conditions = [WorkflowRun.user_id == user_id]
    if conversation_id is not None:
        conditions.append(WorkflowRun.conversation_id == conversation_id)
    if status:
        conditions.append(WorkflowRun.status == status)
    total = await session.scalar(
        select(func.count()).select_from(WorkflowRun).where(*conditions)
    )
    rows = await session.scalars(
        select(WorkflowRun)
        .where(*conditions)
        .order_by(WorkflowRun.updated_at.desc(), WorkflowRun.id.desc())
        .offset((page_number - 1) * size)
        .limit(size)
    )
    return list(rows), int(total or 0)
