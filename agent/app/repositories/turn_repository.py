"""轮次表的读写；终态和取消都用比较更新，避免旧任务覆盖新结果。"""

from datetime import datetime
from uuid import UUID

from sqlalchemy import func, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import ConversationTurn


def _now():
    """数据库执行时间且不早于本轮已记录的 started_at。

    宿主或数据库所在时钟发生回拨时，单纯 CURRENT_TIMESTAMP/clock_timestamp()
    可能早于已写入的行时间，触发 ck_agent_turn_time；这里取两者较大值，
    保证终态时间仍落在 started_at 之后。
    """
    return func.greatest(func.clock_timestamp(), ConversationTurn.started_at)


async def find_by_request(
    session: AsyncSession, user_id: int, request_id: UUID
) -> ConversationTurn | None:
    return (
        await session.execute(
            select(ConversationTurn).where(
                ConversationTurn.user_id == user_id,
                ConversationTurn.request_id == request_id,
            )
        )
    ).scalar_one_or_none()


async def find_owned(
    session: AsyncSession, user_id: int, conversation_id: int, turn_id: int
) -> ConversationTurn | None:
    return (
        await session.execute(
            select(ConversationTurn).where(
                ConversationTurn.id == turn_id,
                ConversationTurn.user_id == user_id,
                ConversationTurn.conversation_id == conversation_id,
            )
        )
    ).scalar_one_or_none()


async def find_running(
    session: AsyncSession, conversation_id: int
) -> ConversationTurn | None:
    return (
        await session.execute(
            select(ConversationTurn).where(
                ConversationTurn.conversation_id == conversation_id,
                ConversationTurn.status == "RUNNING",
            )
        )
    ).scalar_one_or_none()


async def create(
    session: AsyncSession,
    *,
    user_id: int,
    conversation_id: int,
    request_id: UUID,
    request_hash: str,
    deadline_at: datetime,
) -> ConversationTurn:
    turn = ConversationTurn(
        user_id=user_id,
        conversation_id=conversation_id,
        request_id=request_id,
        request_hash=request_hash,
        status="RUNNING",
        deadline_at=deadline_at,
        version=0,
    )
    session.add(turn)
    await session.flush()
    await session.refresh(turn)
    return turn


async def expire_running(
    session: AsyncSession, conversation_id: int, now: datetime
) -> int:
    result = await session.execute(
        update(ConversationTurn)
        .where(
            ConversationTurn.conversation_id == conversation_id,
            ConversationTurn.status == "RUNNING",
            ConversationTurn.deadline_at <= now,
        )
        .values(
            status="FAILED",
            error_code="AGENT_TIMEOUT",
            finish_reason=None,
            finished_at=_now(),
            updated_at=_now(),
            version=ConversationTurn.version + 1,
        )
    )
    return result.rowcount or 0


async def finalize(
    session: AsyncSession,
    *,
    turn_id: int,
    expected_version: int,
    status: str,
    finish_reason: str | None,
    error_code: str | None,
) -> bool:
    result = await session.execute(
        update(ConversationTurn)
        .where(
            ConversationTurn.id == turn_id,
            ConversationTurn.status == "RUNNING",
            ConversationTurn.version == expected_version,
        )
        .values(
            status=status,
            finish_reason=finish_reason,
            error_code=error_code,
            finished_at=_now(),
            updated_at=_now(),
            version=ConversationTurn.version + 1,
        )
    )
    return (result.rowcount or 0) > 0


async def cancel(
    session: AsyncSession,
    *,
    turn_id: int,
    expected_version: int,
) -> bool:
    result = await session.execute(
        update(ConversationTurn)
        .where(
            ConversationTurn.id == turn_id,
            ConversationTurn.status == "RUNNING",
            ConversationTurn.version == expected_version,
        )
        .values(
            status="CANCELLED",
            finish_reason=None,
            error_code="AGENT_CANCELLED",
            finished_at=_now(),
            updated_at=_now(),
            version=ConversationTurn.version + 1,
        )
    )
    return (result.rowcount or 0) > 0


async def count_running(session: AsyncSession, conversation_id: int) -> int:
    return int(
        (
            await session.execute(
                select(func.count())
                .select_from(ConversationTurn)
                .where(
                    ConversationTurn.conversation_id == conversation_id,
                    ConversationTurn.status == "RUNNING",
                )
            )
        ).scalar_one()
    )
