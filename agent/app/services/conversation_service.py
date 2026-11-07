"""会话生命周期：列表、详情、创建、标题、关闭和逻辑删除。"""

from datetime import UTC, datetime

from sqlalchemy import func

from app.core.errors import AgentError
from app.db.models import Conversation, ConversationMessage, ConversationTurn
from app.db.session import Database
from app.repositories import (
    conversation_repository,
    message_repository,
    turn_repository,
)
from app.schemas.conversation import (
    ConversationDetailVO,
    ConversationPageVO,
    ConversationVO,
    MessagePageVO,
    MessageVO,
    TurnVO,
)


def _conversation_now():
    """会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。"""
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


def conversation_vo(conversation: Conversation) -> ConversationVO:
    return ConversationVO(
        id=str(conversation.id),
        title=conversation.title,
        status=conversation.status,
        summary_through_message_no=conversation.summary_through_message_no,
        last_message_at=conversation.last_message_at,
        created_at=conversation.created_at,
        updated_at=conversation.updated_at,
        version=conversation.version,
    )


def turn_vo(turn: ConversationTurn, reply: str | None = None) -> TurnVO:
    return TurnVO(
        id=str(turn.id),
        conversation_id=str(turn.conversation_id),
        request_id=str(turn.request_id),
        status=turn.status,
        finish_reason=turn.finish_reason,
        error_code=turn.error_code,
        started_at=turn.started_at,
        finished_at=turn.finished_at,
        version=turn.version,
        reply=reply,
    )


def _check_version(conversation: Conversation, version: int) -> None:
    if conversation.version != version:
        raise AgentError(409, "STATE_CONFLICT", "会话已被其他操作更新，请刷新后重试")


def message_vo(message: ConversationMessage, usage: dict[str, int] | None = None) -> MessageVO:
    return MessageVO(
        id=str(message.id),
        citations=message.citations, used_memory_ids=message.used_memory_ids,
        message_no=message.message_no,
        role=message.role,
        content=message.content,
        is_complete=message.is_complete,
        finish_reason=message.finish_reason,
        model=message.model,
        usage=usage if usage is not None else message.usage,
        created_at=message.created_at,
    )


async def list_messages(
    db: Database,
    user_id: int,
    conversation_id: int,
    *,
    before_message_no: int | None,
    size: int,
) -> MessagePageVO:
    async with db.session() as session:
        await conversation_repository.require_owned(session, user_id, conversation_id)
        rows, has_more, next_before = await message_repository.page_before(
            session,
            user_id=user_id,
            conversation_id=conversation_id,
            before_message_no=before_message_no,
            size=size,
        )
        usage = await message_repository.usage_by_turn(
            session, user_id, {row.turn_id for row in rows if row.role == "ASSISTANT"}
        )
        return MessagePageVO(
            items=[
                message_vo(row, usage.get(row.turn_id) if row.role == "ASSISTANT" else None)
                for row in rows
            ],
            has_more=has_more,
            next_before_message_no=next_before,
        )


async def list_conversations(
    db: Database,
    user_id: int,
    *,
    status: str | None,
    keyword: str | None,
    page: int,
    size: int,
) -> ConversationPageVO:
    async with db.session() as session:
        rows, total = await conversation_repository.page(
            session,
            user_id,
            status=status,
            keyword=keyword.strip() if keyword else None,
            page_number=page,
            size=size,
        )
        return ConversationPageVO(
            items=[conversation_vo(row) for row in rows],
            total=total,
            page=page,
            size=size,
        )


async def get_conversation(
    db: Database, user_id: int, conversation_id: int
) -> ConversationDetailVO:
    await normalize_expired(db, user_id, conversation_id)
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id
        )
        running = await turn_repository.find_running(session, conversation_id)
        return ConversationDetailVO(
            **conversation_vo(conversation).model_dump(),
            running_turn=turn_vo(running) if running else None,
        )


async def normalize_expired(db: Database, user_id: int, conversation_id: int) -> None:
    """恢复读取前归一过期轮次；进程中断后不让页面一直停在生成中。"""
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        changed = await turn_repository.expire_running(session, conversation_id, datetime.now(UTC))
        if changed:
            conversation.updated_at = _conversation_now()
            conversation.version += 1


async def create_conversation(
    db: Database, user_id: int, title: str | None
) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.create(session, user_id, title or "新对话")
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=None)


async def rename_conversation(
    db: Database, user_id: int, conversation_id: int, title: str, version: int
) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        _check_version(conversation, version)
        conversation.title = title
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
        await session.refresh(conversation)
        running = await turn_repository.find_running(session, conversation_id)
        return ConversationDetailVO(
            **conversation_vo(conversation).model_dump(),
            running_turn=turn_vo(running) if running else None,
        )


async def close_conversation(
    db: Database, user_id: int, conversation_id: int, version: int
) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        _check_version(conversation, version)
        if conversation.status == "CLOSED":
            raise AgentError(409, "CONVERSATION_CLOSED", "会话已经关闭")
        if await turn_repository.count_running(session, conversation_id) > 0:
            raise AgentError(409, "TURN_RUNNING", "请先停止当前正在生成的回复")
        conversation.status = "CLOSED"
        conversation.closed_at = _conversation_now()
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
        await session.refresh(conversation)
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=None)


async def delete_conversation(
    db: Database, user_id: int, conversation_id: int, version: int
) -> None:
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        _check_version(conversation, version)
        if await turn_repository.count_running(session, conversation_id) > 0:
            raise AgentError(409, "TURN_RUNNING", "请先停止当前正在生成的回复")
        # 逻辑删除要求先关闭；同一事务内完成，历史消息与轮次保留。
        conversation.status = "CLOSED"
        if conversation.closed_at is None:
            conversation.closed_at = _conversation_now()
        conversation.deleted_at = _conversation_now()
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
