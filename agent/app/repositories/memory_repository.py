"""长期记忆数据访问；只使用已确认的 agent.memory 结构。"""

from sqlalchemy import Select, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import Memory


async def page(
    session: AsyncSession,
    *,
    user_id: int,
    status: str | None,
    kind: str | None,
    keyword: str | None,
    page_number: int,
    size: int,
) -> tuple[list[Memory], int]:
    conditions = [Memory.user_id == user_id, Memory.deleted_at.is_(None)]
    if status:
        conditions.append(Memory.status == status)
    if kind:
        conditions.append(Memory.kind == kind)
    if keyword:
        conditions.append(Memory.content.ilike(f"%{keyword}%"))
    query: Select = select(Memory).where(*conditions)
    total = await session.scalar(select(func.count()).select_from(query.subquery()))
    rows = await session.scalars(
        query.order_by(Memory.updated_at.desc(), Memory.id.desc())
        .offset((page_number - 1) * size)
        .limit(size)
    )
    return list(rows), int(total or 0)


async def find_owned(
    session: AsyncSession, user_id: int, memory_id: int, *, for_update: bool = False
) -> Memory | None:
    query = select(Memory).where(
        Memory.id == memory_id, Memory.user_id == user_id, Memory.deleted_at.is_(None)
    )
    if for_update:
        query = query.with_for_update()
    return await session.scalar(query)


async def find_same_content(
    session: AsyncSession, user_id: int, content: str, statuses: tuple[str, ...]
) -> Memory | None:
    return await session.scalar(
        select(Memory)
        .where(
            Memory.user_id == user_id,
            Memory.content == content,
            Memory.status.in_(statuses),
            Memory.deleted_at.is_(None),
        )
        .order_by(Memory.id)
        .limit(1)
    )


async def create(
    session: AsyncSession,
    *,
    user_id: int,
    kind: str,
    content: str,
    source: str,
    confidence: float,
    source_conversation_id: int | None,
    source_turn_id: int | None,
    supersedes_id: int | None,
) -> Memory:
    row = Memory(
        user_id=user_id,
        kind=kind,
        content=content,
        source=source,
        confidence=confidence,
        status="CANDIDATE",
        source_conversation_id=source_conversation_id,
        source_turn_id=source_turn_id,
        supersedes_id=supersedes_id,
    )
    session.add(row)
    await session.flush()
    return row


async def confirmed_for_context(
    session: AsyncSession, user_id: int, limit: int
) -> list[Memory]:
    rows = await session.scalars(
        select(Memory)
        .where(
            Memory.user_id == user_id,
            Memory.status == "CONFIRMED",
            Memory.deleted_at.is_(None),
        )
        .order_by(Memory.updated_at.desc(), Memory.id.desc())
        .limit(limit)
    )
    return list(rows)
