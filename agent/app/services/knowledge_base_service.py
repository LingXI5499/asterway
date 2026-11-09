# 资料归属、库管理和会话选择统一在这里检查用户与版本，检索层不接受未核验的库。
from datetime import UTC, datetime

from sqlalchemy import delete, func, select

from app.core.errors import AgentError
from app.db.knowledge_models import ConversationKnowledgeBase, KnowledgeBase
from app.db.models import KnowledgeDocument
from app.db.workspace_models import KnowledgeRevision


async def lock_user(session, user_id: int) -> None:
    # 同用户的短写事务串行，避免批量上传时重复创建默认库或同一份文档。
    await session.execute(select(func.pg_advisory_xact_lock(-user_id)))


def base_vo(row, count: int = 0) -> dict:
    return {"id": str(row.id), "name": row.name, "description": row.description,
            "documentCount": count, "version": row.version,
            "createdAt": row.created_at, "updatedAt": row.updated_at}


async def require_base(session, user_id: int, base_id: int):
    row = await session.scalar(select(KnowledgeBase).where(
        KnowledgeBase.id == base_id, KnowledgeBase.user_id == user_id,
        KnowledgeBase.deleted_at.is_(None)))
    if row is None:
        raise AgentError(404, "KNOWLEDGE_BASE_NOT_FOUND", "知识库不存在或无权访问，请重新选择")
    return row


async def validate_selection(session, user_id: int, ids: list[int]) -> list[int]:
    ids = sorted(set(ids))
    if not ids:
        return []
    found = list(await session.scalars(select(KnowledgeBase.id).where(
        KnowledgeBase.user_id == user_id, KnowledgeBase.id.in_(ids), KnowledgeBase.deleted_at.is_(None))))
    if set(found) != set(ids):
        raise AgentError(409, "KNOWLEDGE_SCOPE_INVALID", "所选知识库已失效或无权访问，请重新选择")
    return ids


async def default_base(session, user_id: int):
    row = await session.scalar(select(KnowledgeBase).where(
        KnowledgeBase.user_id == user_id, KnowledgeBase.is_default.is_(True), KnowledgeBase.deleted_at.is_(None)))
    if row is None:
        row = await session.scalar(select(KnowledgeBase).where(
            KnowledgeBase.user_id == user_id, KnowledgeBase.name == "默认知识库", KnowledgeBase.deleted_at.is_(None)))
        if row is None:
            row = KnowledgeBase(user_id=user_id, name="默认知识库", description="未指定知识库时存放的资料。", is_default=True)
            session.add(row)
        else:
            row.is_default = True
        await session.flush()
    return row


async def list_bases(db, user_id: int):
    async with db.session() as session:
        counts = select(KnowledgeDocument.knowledge_base_id, func.count().label("n")).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.deleted_at.is_(None)
        ).group_by(KnowledgeDocument.knowledge_base_id).subquery()
        rows = await session.execute(select(KnowledgeBase, func.coalesce(counts.c.n, 0)).outerjoin(
            counts, counts.c.knowledge_base_id == KnowledgeBase.id).where(
            KnowledgeBase.user_id == user_id, KnowledgeBase.deleted_at.is_(None)).order_by(KnowledgeBase.created_at, KnowledgeBase.id))
        return [base_vo(row, count) for row, count in rows]


async def save_base(db, user_id: int, name: str, description: str, *, base_id=None, version=None):
    name, description = name.strip(), description.strip()
    if not name or len(name) > 100 or len(description) > 500:
        raise AgentError(422, "KNOWLEDGE_BASE_INVALID", "知识库名称为1至100字，简介不能超过500字")
    async with db.session() as session:
        await lock_user(session, user_id)
        row = await require_base(session, user_id, base_id) if base_id is not None else None
        if row and row.version != version:
            raise AgentError(409, "KNOWLEDGE_BASE_VERSION_CONFLICT", "知识库已更新，请刷新后重试")
        duplicate = await session.scalar(select(KnowledgeBase.id).where(
            KnowledgeBase.user_id == user_id, func.lower(KnowledgeBase.name) == name.lower(),
            KnowledgeBase.deleted_at.is_(None), KnowledgeBase.id != (base_id or 0)))
        if duplicate:
            raise AgentError(409, "KNOWLEDGE_BASE_DUPLICATE", "已经有同名知识库，请换个名称")
        if row is None:
            row = KnowledgeBase(user_id=user_id, name=name, description=description)
            session.add(row)
        else:
            row.name, row.description = name, description
            row.version += 1
            row.updated_at = datetime.now(UTC)
        await session.flush()
        count = await session.scalar(select(func.count()).select_from(KnowledgeDocument).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id == row.id,
            KnowledgeDocument.deleted_at.is_(None)))
        return base_vo(row, count)


async def delete_base(db, user_id: int, base_id: int, version: int):
    async with db.session() as session:
        await lock_user(session, user_id)
        row = await require_base(session, user_id, base_id)
        if row.version != version:
            raise AgentError(409, "KNOWLEDGE_BASE_VERSION_CONFLICT", "知识库已更新，请刷新后重试")
        count = await session.scalar(select(func.count()).select_from(KnowledgeDocument).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id == base_id,
            KnowledgeDocument.deleted_at.is_(None)))
        if count:
            raise AgentError(409, "KNOWLEDGE_BASE_NOT_EMPTY", "请先移动或删除库内文档，再删除知识库")
        row.deleted_at = row.updated_at = datetime.now(UTC)
        row.version += 1
        await session.execute(delete(ConversationKnowledgeBase).where(
            ConversationKnowledgeBase.user_id == user_id, ConversationKnowledgeBase.knowledge_base_id == base_id))


async def enrich_documents(session, rows):
    if not rows:
        return rows
    base_ids = {r.knowledge_base_id for r in rows}
    bases = {b.id: b.name for b in await session.scalars(select(KnowledgeBase).where(KnowledgeBase.id.in_(base_ids)))}
    revision_ids = [r.current_revision_id for r in rows if r.current_revision_id]
    revisions = {r.id: r for r in await session.scalars(select(KnowledgeRevision).where(KnowledgeRevision.id.in_(revision_ids)))}
    for row in rows:
        row.knowledge_base_name = bases.get(row.knowledge_base_id, "")
        revision = revisions.get(row.current_revision_id)
        row.index_status = revision.index_status if revision else "PENDING"
        row.index_error = revision.error_message if revision else None
    return rows


async def move_documents(db, user_id: int, base_id: int, documents: list[dict]):
    if not documents or len(documents) > 100 or len({d["id"] for d in documents}) != len(documents):
        raise AgentError(422, "KNOWLEDGE_MOVE_INVALID", "请选择1至100篇不同的文档")
    async with db.session() as session:
        await lock_user(session, user_id)
        await require_base(session, user_id, base_id)
        rows = []
        for item in sorted(documents, key=lambda d: d["id"]):
            row = await session.scalar(select(KnowledgeDocument).where(
                KnowledgeDocument.id == item["id"], KnowledgeDocument.user_id == user_id,
                KnowledgeDocument.deleted_at.is_(None)).with_for_update())
            if row is None:
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "有文档不存在或无权访问")
            if row.version != item["version"]:
                raise AgentError(409, "KNOWLEDGE_VERSION_CONFLICT", "文档已更新，请刷新后重试")
            rows.append(row)
        moving_ids = [r.id for r in rows]
        hashes = [r.checksum for r in rows]
        if len(hashes) != len(set(hashes)) or await session.scalar(select(KnowledgeDocument.id).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id == base_id,
            KnowledgeDocument.checksum.in_(hashes), KnowledgeDocument.id.not_in(moving_ids),
            KnowledgeDocument.deleted_at.is_(None)).limit(1)):
            raise AgentError(409, "KNOWLEDGE_DUPLICATE", "目标知识库中已有相同内容，未移动任何文档")
        for row in rows:
            if row.knowledge_base_id != base_id:
                row.knowledge_base_id = base_id
                row.version += 1
                row.updated_at = datetime.now(UTC)
        return {"moved": len(rows)}


async def selected_ids(session, user_id: int, conversation_id: int) -> list[int]:
    return list(await session.scalars(select(ConversationKnowledgeBase.knowledge_base_id).where(
        ConversationKnowledgeBase.user_id == user_id, ConversationKnowledgeBase.conversation_id == conversation_id
    ).order_by(ConversationKnowledgeBase.knowledge_base_id)))


async def set_selection(session, user_id: int, conversation_id: int, ids: list[int]):
    ids = await validate_selection(session, user_id, ids)
    await session.execute(delete(ConversationKnowledgeBase).where(
        ConversationKnowledgeBase.user_id == user_id, ConversationKnowledgeBase.conversation_id == conversation_id))
    session.add_all([ConversationKnowledgeBase(user_id=user_id, conversation_id=conversation_id, knowledge_base_id=i) for i in ids])
    await session.flush()
    return ids
