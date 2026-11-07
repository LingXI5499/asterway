"""消息表读写；序号由会话行分配，历史按 message_no 游标而不是时间戳。"""

from dataclasses import dataclass
from datetime import datetime
from typing import Any

from sqlalchemy import and_, func, or_, select, update
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import AgentError
from app.db.models import Conversation, ConversationMessage, ConversationTurn


def _now():
    """数据库执行时间且不早于消息 created_at，避免宿主时钟回拨破坏时间 CHECK。"""
    return func.greatest(func.clock_timestamp(), ConversationMessage.created_at)


def _conversation_now():
    """会话更新使用数据库执行时间且不早于会话 created_at。"""
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


@dataclass(frozen=True)
class HistoryTurn:
    """按完整终态轮次读取，避免截断助手调用与工具结果。"""

    turn_id: int
    status: str
    last_message_no: int
    messages: tuple[ConversationMessage, ...]


@dataclass(frozen=True)
class ToolRoundReservation:
    """工具结果先预留序号，随后助手占位始终排在这些结果之后。"""

    request_message_id: int
    result_message_nos: tuple[int, ...]
    next_assistant_id: int


async def insert_pair(
    session: AsyncSession,
    *,
    conversation: Conversation,
    user_id: int,
    turn_id: int,
    user_content: str,
    assistant_model: str,
    now: datetime,
) -> tuple[ConversationMessage, ConversationMessage]:
    """在已锁定会话的事务内分配两个连续序号，写入提问和助手占位。"""
    user_no = conversation.next_message_no
    conversation.next_message_no = user_no + 2
    # 使用数据库执行时间且不早于会话 created_at，保证时间 CHECK 条件成立。
    conversation.last_message_at = _conversation_now()
    conversation.updated_at = _conversation_now()
    conversation.version += 1

    user_row = ConversationMessage(
        user_id=user_id,
        conversation_id=conversation.id,
        turn_id=turn_id,
        message_no=user_no,
        role="USER",
        content=user_content,
        is_complete=True,
        tool_calls=[],
    )
    assistant_row = ConversationMessage(
        user_id=user_id,
        conversation_id=conversation.id,
        turn_id=turn_id,
        message_no=user_no + 1,
        role="ASSISTANT",
        content="",
        is_complete=False,
        tool_calls=[],
        model=assistant_model,
    )
    session.add_all([user_row, assistant_row])
    await session.flush()
    await session.refresh(user_row)
    await session.refresh(assistant_row)
    return user_row, assistant_row


async def save_assistant(
    session: AsyncSession,
    *,
    message_id: int,
    user_id: int,
    conversation_id: int,
    turn_id: int,
    content: str,
    is_complete: bool,
    finish_reason: str | None,
    usage: dict[str, Any] | None,
    model: str,
    now: datetime,
) -> bool:
    result = await session.execute(
        update(ConversationMessage)
        .where(
            ConversationMessage.id == message_id,
            ConversationMessage.user_id == user_id,
            ConversationMessage.conversation_id == conversation_id,
            ConversationMessage.turn_id == turn_id,
            ConversationMessage.role == "ASSISTANT",
            ConversationMessage.is_complete.is_(False),
            ConversationMessage.tool_calls == [],
        )
        .values(
            content=content,
            is_complete=is_complete,
            finish_reason=finish_reason,
            usage=usage,
            model=model,
            updated_at=_now(),
        )
    )
    return (result.rowcount or 0) == 1


async def begin_tool_round(
    session: AsyncSession,
    *,
    conversation: Conversation,
    turn_id: int,
    assistant_message_id: int,
    content: str,
    calls: list[dict[str, Any]],
    model: str,
    usage: dict[str, int] | None,
) -> ToolRoundReservation:
    """调用前保存请求；会话锁及轮次运行状态由 Service 在同事务核对。"""
    result = await session.execute(
        update(ConversationMessage)
        .where(
            ConversationMessage.id == assistant_message_id,
            ConversationMessage.user_id == conversation.user_id,
            ConversationMessage.conversation_id == conversation.id,
            ConversationMessage.turn_id == turn_id,
            ConversationMessage.role == "ASSISTANT",
            ConversationMessage.is_complete.is_(False),
            ConversationMessage.tool_calls == [],
        )
        .values(
            content=content,
            tool_calls=calls,
            is_complete=True,
            model=model,
            finish_reason="tool_calls",
            usage=usage,
            updated_at=_now(),
        )
    )
    if result.rowcount != 1:
        raise AgentError(409, "TURN_STATE_CONFLICT", "工具请求状态已变化，请刷新会话")
    first = conversation.next_message_no
    result_numbers = tuple(range(first, first + len(calls)))
    conversation.next_message_no = first + len(calls) + 1
    conversation.last_message_at = _conversation_now()
    conversation.updated_at = _conversation_now()
    conversation.version += 1
    assistant = ConversationMessage(
        user_id=conversation.user_id,
        conversation_id=conversation.id,
        turn_id=turn_id,
        message_no=first + len(calls),
        role="ASSISTANT",
        content="",
        is_complete=False,
        tool_calls=[],
        model=model,
    )
    session.add(assistant)
    await session.flush()
    return ToolRoundReservation(assistant_message_id, result_numbers, assistant.id)


async def insert_tool_result(
    session: AsyncSession,
    *,
    conversation: Conversation,
    turn_id: int,
    request_message_id: int,
    message_no: int,
    call_id: str,
    name: str,
    content: str,
) -> None:
    session.add(
        ConversationMessage(
            user_id=conversation.user_id,
            conversation_id=conversation.id,
            turn_id=turn_id,
            message_no=message_no,
            role="TOOL",
            content=content,
            is_complete=True,
            tool_calls=[],
            tool_request_message_id=request_message_id,
            tool_call_id=call_id,
            tool_name=name,
        )
    )
    conversation.last_message_at = _conversation_now()
    conversation.updated_at = _conversation_now()
    conversation.version += 1
    await session.flush()


async def page_before(
    session: AsyncSession,
    *,
    user_id: int,
    conversation_id: int,
    before_message_no: int | None,
    size: int,
) -> tuple[list[ConversationMessage], bool, int | None]:
    statement = select(ConversationMessage).where(
        ConversationMessage.user_id == user_id,
        ConversationMessage.conversation_id == conversation_id,
        # 公开聊天分页只展示提问与最终回复，内部工具事实仍完整存储。
        or_(
            ConversationMessage.role == "USER",
            and_(ConversationMessage.role == "ASSISTANT", ConversationMessage.tool_calls == []),
        ),
    )
    if before_message_no is not None:
        statement = statement.where(ConversationMessage.message_no < before_message_no)
    rows = list(
        (
            await session.execute(
                statement.order_by(ConversationMessage.message_no.desc()).limit(size + 1)
            )
        ).scalars()
    )
    has_more = len(rows) > size
    rows = rows[:size]
    rows.reverse()
    next_before = rows[0].message_no if has_more and rows else None
    return rows, has_more, next_before


async def history_turns(
    session: AsyncSession,
    *,
    user_id: int,
    conversation_id: int,
    after_message_no: int,
    before_message_no: int | None,
    limit: int,
    newest: bool = True,
) -> list[HistoryTurn]:
    """选择完整终态轮次，再读取其全部消息；旧摘要边界在轮次内部时也不拆组。"""
    conditions = [
        ConversationMessage.user_id == user_id,
        ConversationMessage.conversation_id == conversation_id,
        ConversationTurn.status != "RUNNING",
    ]
    if before_message_no is not None:
        conditions.append(ConversationMessage.message_no < before_message_no)
    relation = and_(
        ConversationTurn.id == ConversationMessage.turn_id,
        ConversationTurn.user_id == ConversationMessage.user_id,
        ConversationTurn.conversation_id == ConversationMessage.conversation_id,
    )
    last_no = func.max(ConversationMessage.message_no)
    selected = (
        select(ConversationMessage.turn_id)
        .join(ConversationTurn, relation)
        .where(*conditions)
        .group_by(ConversationMessage.turn_id)
        .having(last_no > after_message_no)
        .order_by(last_no.desc() if newest else last_no.asc())
        .limit(limit)
        .correlate(None)
    )
    rows = (
        await session.execute(
            select(ConversationMessage, ConversationTurn.status)
            .join(ConversationTurn, relation)
            .where(*conditions, ConversationMessage.turn_id.in_(selected))
            .order_by(ConversationMessage.message_no)
        )
    ).all()
    groups: dict[int, list[ConversationMessage]] = {}
    statuses: dict[int, str] = {}
    for message, status in rows:
        groups.setdefault(message.turn_id, []).append(message)
        statuses[message.turn_id] = status
    return [
        HistoryTurn(turn_id, statuses[turn_id], values[-1].message_no, tuple(values))
        for turn_id, values in groups.items()
    ]


async def usage_by_turn(
    session: AsyncSession, user_id: int, turn_ids: set[int]
) -> dict[int, dict[str, int]]:
    """消息按调用保存实际用量；公开最终回复按轮次汇总，不重复保存累计值。"""
    if not turn_ids:
        return {}
    result: dict[int, dict[str, int]] = {}
    rows = (
        await session.execute(
            select(ConversationMessage.turn_id, ConversationMessage.usage).where(
                ConversationMessage.user_id == user_id,
                ConversationMessage.turn_id.in_(turn_ids),
                ConversationMessage.role == "ASSISTANT",
                ConversationMessage.usage.is_not(None),
            )
        )
    ).all()
    for turn_id, usage in rows:
        if not isinstance(usage, dict):
            continue
        values = result.setdefault(turn_id, {})
        for key in ("promptTokens", "completionTokens", "totalTokens"):
            value = usage.get(key)
            if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
                values[key] = values.get(key, 0) + value
    return result


async def assistant_for_turn(
    session: AsyncSession, *, user_id: int, turn_id: int
) -> ConversationMessage | None:
    return (
        await session.execute(
            select(ConversationMessage)
            .where(
                ConversationMessage.user_id == user_id,
                ConversationMessage.turn_id == turn_id,
                ConversationMessage.role == "ASSISTANT",
                ConversationMessage.tool_calls == [],
            )
            .order_by(ConversationMessage.message_no.desc())
            .limit(1)
        )
    ).scalar_one_or_none()


async def by_turn(
    session: AsyncSession, *, user_id: int, turn_id: int
) -> list[ConversationMessage]:
    return list(
        (
            await session.execute(
                select(ConversationMessage)
                .where(
                    ConversationMessage.user_id == user_id,
                    ConversationMessage.turn_id == turn_id,
                )
                .order_by(ConversationMessage.message_no)
            )
        ).scalars()
    )
