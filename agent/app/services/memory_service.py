"""长期记忆业务：候选、确认、修改、拒绝、归档、删除与跨会话上下文。"""

import logging
from datetime import UTC, datetime

from sqlalchemy import delete as sql_delete
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

from app.core.errors import AgentError
from app.db.models import Conversation, ConversationTurn, Memory, MemoryEmbedding
from app.db.session import Database
from app.db.time import database_time
from app.repositories import background_task_repository as task_repo
from app.repositories import memory_repository as repo
from app.schemas.memory import MemoryCandidateRequest, MemoryVO

logger = logging.getLogger(__name__)

_KIND_LABELS = {
    "PREFERENCE": "偏好",
    "FACT": "事实",
    "CONSTRAINT": "约束",
    "GOAL_CONTEXT": "目标背景",
}
_STATUS_LABELS = {
    "CANDIDATE": "待确认",
    "CONFIRMED": "已确认",
    "REJECTED": "已拒绝",
    "ARCHIVED": "已归档",
}


def memory_vo(row: Memory) -> MemoryVO:
    return MemoryVO(
        id=str(row.id),
        kind=row.kind,
        kind_label=_KIND_LABELS.get(row.kind, row.kind),
        content=row.content,
        source=row.source,
        status=row.status,
        status_label=_STATUS_LABELS.get(row.status, row.status),
        confidence=float(row.confidence),
        source_conversation_id=str(row.source_conversation_id)
        if row.source_conversation_id
        else None,
        source_turn_id=str(row.source_turn_id) if row.source_turn_id else None,
        supersedes_id=str(row.supersedes_id) if row.supersedes_id else None,
        confirmed_at=row.confirmed_at,
        rejected_at=row.rejected_at,
        archived_at=row.archived_at,
        created_at=row.created_at,
        updated_at=row.updated_at,
        version=row.version,
    )


def _stamp(row: Memory) -> datetime:
    """时钟回拨保护：状态时间必须落在行创建时间与更新时间之间。"""
    return max(datetime.now(UTC), row.created_at)


def _missing() -> AgentError:
    return AgentError(404, "MEMORY_NOT_FOUND", "没有找到可访问的记忆")


def _version_conflict() -> AgentError:
    return AgentError(409, "MEMORY_VERSION_CONFLICT", "记忆已被修改，请刷新后重试")


async def list_memories(
    db: Database,
    user_id: int,
    *,
    status: str | None,
    kind: str | None,
    keyword: str | None,
    page: int,
    size: int,
) -> tuple[list[Memory], int]:
    async with db.session() as session:
        return await repo.page(
            session,
            user_id=user_id,
            status=status,
            kind=kind,
            keyword=keyword,
            page_number=page,
            size=size,
        )


async def create_candidate(db: Database, user_id: int, request: MemoryCandidateRequest) -> Memory:
    """候选按内容幂等：已有候选或已确认的同一内容时直接返回既有记录。"""
    async with db.session() as session:
        conversation_id = request.conversationId
        if conversation_id is not None:
            conversation = await session.scalar(
                select(Conversation).where(
                    Conversation.id == conversation_id,
                    Conversation.user_id == user_id,
                    Conversation.deleted_at.is_(None),
                )
            )
            if conversation is None:
                raise _missing()
        if request.turnId is not None:
            turn = await session.scalar(
                select(ConversationTurn).where(
                    ConversationTurn.id == request.turnId, ConversationTurn.user_id == user_id
                )
            )
            if turn is None or conversation_id not in (None, turn.conversation_id):
                raise _missing()
            conversation_id = turn.conversation_id
        if request.supersedesId is not None:
            previous = await repo.find_owned(session, user_id, request.supersedesId)
            if previous is None:
                raise _missing()
        existing = await repo.find_same_content(
            session, user_id, request.content, ("CANDIDATE", "CONFIRMED")
        )
        if existing is not None:
            return existing
        row = await repo.create(
            session,
            user_id=user_id,
            kind=request.kind,
            content=request.content,
            source=request.source,
            confidence=request.confidence,
            source_conversation_id=conversation_id,
            source_turn_id=request.turnId,
            supersedes_id=request.supersedesId,
        )
        await session.refresh(row)
        return row


async def update_content(
    db: Database, user_id: int, memory_id: int, content: str, version: int
) -> Memory:
    async with db.session() as session:
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        if row is None:
            raise _missing()
        if row.version != version:
            raise _version_conflict()
        if row.status not in {"CANDIDATE", "CONFIRMED"}:
            raise AgentError(409, "MEMORY_STATUS_INVALID", "当前状态的记忆不能修改内容")
        duplicate = await repo.find_same_content(
            session, user_id, content, ("CANDIDATE", "CONFIRMED")
        )
        if duplicate is not None and duplicate.id != row.id:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆")
        stamp = await database_time(session, row)
        row.content = content
        row.updated_at = stamp
        row.version += 1
        try:
            await session.flush()
        except IntegrityError as error:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆") from error
        if row.status == "CONFIRMED":
            await session.execute(
                sql_delete(MemoryEmbedding).where(
                    MemoryEmbedding.user_id == user_id, MemoryEmbedding.memory_id == row.id
                )
            )
            await task_repo.enqueue(
                session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id
            )
        try:
            await session.flush()
        except IntegrityError as error:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆") from error
        await session.refresh(row)
        return row


async def confirm(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    async with db.session() as session:
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        if row is None:
            raise _missing()
        if row.version != version:
            raise _version_conflict()
        if row.status != "CANDIDATE":
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有待确认的记忆可以确认")
        duplicate = await repo.find_same_content(session, user_id, row.content, ("CONFIRMED",))
        if duplicate is not None and duplicate.id != row.id:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆")
        previous = None
        if row.supersedes_id is not None:
            previous = await repo.find_owned(session, user_id, row.supersedes_id, for_update=True)
            if previous is None:
                raise _missing()
            if previous.status != "CONFIRMED":
                raise AgentError(409, "MEMORY_STATUS_INVALID", "被替代的记忆状态已变化")
        stamp = await database_time(session, row, previous)
        if previous is not None:
            previous.status = "ARCHIVED"
            previous.archived_at = stamp
            previous.updated_at = stamp
            previous.version += 1
        row.status = "CONFIRMED"
        row.source = "USER_CONFIRMED"
        row.confirmed_at = stamp
        row.updated_at = stamp
        row.version += 1
        try:
            await session.flush()
        except IntegrityError as error:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆") from error
        await task_repo.enqueue(
            session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id
        )
        try:
            await session.flush()
        except IntegrityError as error:
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆") from error
        await session.refresh(row)
        return row


async def reject(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    async with db.session() as session:
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        if row is None:
            raise _missing()
        if row.version != version:
            raise _version_conflict()
        if row.status != "CANDIDATE":
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有待确认的记忆可以拒绝")
        stamp = await database_time(session, row)
        row.status = "REJECTED"
        row.rejected_at = stamp
        row.updated_at = stamp
        row.version += 1
        await session.flush()
        await session.refresh(row)
        return row


async def archive(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    async with db.session() as session:
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        if row is None:
            raise _missing()
        if row.version != version:
            raise _version_conflict()
        if row.status != "CONFIRMED":
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有已确认的记忆可以归档")
        stamp = await database_time(session, row)
        row.status = "ARCHIVED"
        row.archived_at = stamp
        row.updated_at = stamp
        row.version += 1
        await session.flush()
        await session.refresh(row)
        return row


async def delete(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    async with db.session() as session:
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        if row is None:
            raise _missing()
        if row.version != version:
            raise _version_conflict()
        stamp = await database_time(session, row)
        row.deleted_at = stamp
        row.updated_at = stamp
        row.version += 1
        await session.flush()
        await session.refresh(row)
        return row


async def context_block(
    db: Database, user_id: int, *, max_chars: int = 2000, limit: int = 40,
    query: str | None = None, metadata: dict | None = None
) -> str | None:
    """把已确认记忆压缩成上下文背景；表未建立时静默跳过，不影响对话。"""
    try:
        async with db.session() as session:
            rows = await repo.confirmed_for_context(session, user_id, limit)
    except SQLAlchemyError:
        logger.warning("agent_memory_context_unavailable user=%s", user_id)
        return None
    if not rows:
        return None
    if query:
        from app.core.config import get_settings
        from app.services.embedding_service import embed_texts
        try:
            vector = (await embed_texts([query]))[0]
            async with db.session() as session:
                distance = MemoryEmbedding.vector_value.cosine_distance(vector)
                matches = list(await session.scalars(select(Memory).join(
                    MemoryEmbedding, MemoryEmbedding.memory_id == Memory.id).where(
                    Memory.user_id == user_id, MemoryEmbedding.user_id == user_id,
                    Memory.status == "CONFIRMED", Memory.deleted_at.is_(None),
                    MemoryEmbedding.embedding_model == get_settings().embedding_model,
                    MemoryEmbedding.vector_value.is_not(None), distance <= 0.8,
                ).order_by(distance, Memory.id).limit(10)))
            seen = {row.id for row in matches}
            # 明确约束和偏好在语义不近时仍作为稳定背景；普通事实按相关性选择。
            rows = matches + [row for row in rows if row.id not in seen and row.kind in {"CONSTRAINT", "PREFERENCE"}]
        except AgentError:
            pass
    lines: list[str] = []
    used = 0
    for row in rows:
        label = _KIND_LABELS.get(row.kind, row.kind)
        line = f"- [{label}] {row.content}"
        if used + len(line) + 1 > max_chars:
            break
        lines.append(line)
        if metadata is not None:
            metadata.setdefault("memoryIds", []).append(str(row.id))
        used += len(line) + 1
    return "\n".join(lines) if lines else None
