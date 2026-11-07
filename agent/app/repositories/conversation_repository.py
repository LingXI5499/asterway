"""会话表的读写；所有查询都带所属用户，删除一律按不存在处理。"""

from datetime import datetime

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import AgentError
from app.db.models import Conversation


def _now():
    """数据库执行时间且不早于会话 created_at，避免宿主时钟回拨破坏时间 CHECK。"""
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


def _like_pattern(keyword: str) -> str:
    escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return f"%{escaped}%"


async def find_owned(
    session: AsyncSession,
    user_id: int,
    conversation_id: int,
    *,
    for_update: bool = False,
) -> Conversation | None:
    statement = select(Conversation).where(
        Conversation.id == conversation_id,
        Conversation.user_id == user_id,
        Conversation.deleted_at.is_(None),
    )
    if for_update:
        statement = statement.with_for_update()
    return (await session.execute(statement)).scalar_one_or_none()


async def require_owned(
    session: AsyncSession,
    user_id: int,
    conversation_id: int,
    *,
    for_update: bool = False,
) -> Conversation:
    conversation = await find_owned(
        session, user_id, conversation_id, for_update=for_update
    )
    if conversation is None:
        raise AgentError(404, "CONVERSATION_NOT_FOUND", "会话不存在或不可访问")
    return conversation


async def page(
    session: AsyncSession,
    user_id: int,
    *,
    status: str | None,
    keyword: str | None,
    page_number: int,
    size: int,
) -> tuple[list[Conversation], int]:
    conditions = [Conversation.user_id == user_id, Conversation.deleted_at.is_(None)]
    if status:
        conditions.append(Conversation.status == status)
    if keyword:
        conditions.append(Conversation.title.ilike(_like_pattern(keyword), escape="\\"))
    total = (
        await session.execute(
            select(func.count()).select_from(Conversation).where(*conditions)
        )
    ).scalar_one()
    rows = (
        await session.execute(
            select(Conversation)
            .where(*conditions)
            .order_by(Conversation.updated_at.desc(), Conversation.id.desc())
            .offset((page_number - 1) * size)
            .limit(size)
        )
    ).scalars().all()
    return list(rows), int(total)


async def create(session: AsyncSession, user_id: int, title: str) -> Conversation:
    conversation = Conversation(
        user_id=user_id,
        title=title,
        status="ACTIVE",
        context_summary="",
        summary_through_message_no=0,
        next_message_no=1,
        version=0,
    )
    session.add(conversation)
    await session.flush()
    await session.refresh(conversation)
    return conversation


def touch(conversation: Conversation, now: datetime) -> None:
    conversation.updated_at = now
    conversation.version += 1


async def update_summary(
    session: AsyncSession,
    *,
    conversation_id: int,
    user_id: int,
    expected_version: int,
    expected_boundary: int,
    summary: str,
    boundary: int,
) -> bool:
    """摘要按版本和原覆盖边界比较更新，过期摘要不覆盖新内容。"""
    result = await session.execute(
        update(Conversation)
        .where(
            Conversation.id == conversation_id,
            Conversation.user_id == user_id,
            Conversation.deleted_at.is_(None),
            Conversation.version == expected_version,
            Conversation.summary_through_message_no == expected_boundary,
        )
        .values(
            context_summary=summary,
            summary_through_message_no=boundary,
            summary_updated_at=_now(),
            updated_at=_now(),
            version=Conversation.version + 1,
        )
    )
    return (result.rowcount or 0) > 0
