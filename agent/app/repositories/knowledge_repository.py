"""知识文档与分块数据访问。"""

from typing import Any

from sqlalchemy import Select, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.db.models import KnowledgeChunk, KnowledgeDocument


async def page_documents(
    session: AsyncSession,
    *,
    user_id: int,
    status: str | None,
    keyword: str | None,
    page_number: int,
    size: int,
) -> tuple[list[KnowledgeDocument], int]:
    conditions = [
        KnowledgeDocument.user_id == user_id,
        KnowledgeDocument.deleted_at.is_(None),
    ]
    if status:
        conditions.append(KnowledgeDocument.status == status)
    if keyword:
        conditions.append(KnowledgeDocument.title.ilike(f"%{keyword}%"))
    query: Select = select(KnowledgeDocument).where(*conditions)
    total = await session.scalar(select(func.count()).select_from(query.subquery()))
    rows = await session.scalars(
        query.order_by(KnowledgeDocument.created_at.desc(), KnowledgeDocument.id.desc())
        .offset((page_number - 1) * size)
        .limit(size)
    )
    return list(rows), int(total or 0)


async def find_document(
    session: AsyncSession, user_id: int, document_id: int, *, for_update: bool = False
) -> KnowledgeDocument | None:
    query = select(KnowledgeDocument).where(
        KnowledgeDocument.id == document_id,
        KnowledgeDocument.user_id == user_id,
        KnowledgeDocument.deleted_at.is_(None),
    )
    if for_update:
        query = query.with_for_update()
    return await session.scalar(query)


async def find_by_checksum(
    session: AsyncSession, user_id: int, checksum: str
) -> KnowledgeDocument | None:
    return await session.scalar(
        select(KnowledgeDocument).where(
            KnowledgeDocument.user_id == user_id,
            KnowledgeDocument.checksum == checksum,
            KnowledgeDocument.deleted_at.is_(None),
        )
    )


async def create_document(session: AsyncSession, **values: Any) -> KnowledgeDocument:
    row = KnowledgeDocument(**values)
    session.add(row)
    await session.flush()
    return row


async def page_chunks(
    session: AsyncSession,
    *,
    user_id: int,
    document_id: int,
    page_number: int,
    size: int,
) -> tuple[list[KnowledgeChunk], int]:
    conditions = [
        KnowledgeChunk.user_id == user_id,
        KnowledgeChunk.document_id == document_id,
        KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where(KnowledgeDocument.id == document_id).scalar_subquery(),
    ]
    total = await session.scalar(
        select(func.count()).select_from(KnowledgeChunk).where(*conditions)
    )
    rows = await session.scalars(
        select(KnowledgeChunk)
        .where(*conditions)
        .order_by(KnowledgeChunk.chunk_no)
        .offset((page_number - 1) * size)
        .limit(size)
    )
    return list(rows), int(total or 0)


async def all_chunks(session: AsyncSession, user_id: int, document_id: int) -> list[KnowledgeChunk]:
    rows = await session.scalars(
        select(KnowledgeChunk)
        .where(KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id)
        .order_by(KnowledgeChunk.chunk_no)
    )
    return list(rows)


async def chunks_without_embedding(
    session: AsyncSession, user_id: int, document_id: int, limit: int
) -> list[KnowledgeChunk]:
    rows = await session.scalars(
        select(KnowledgeChunk)
        .where(
            KnowledgeChunk.user_id == user_id,
            KnowledgeChunk.document_id == document_id,
            KnowledgeChunk.embedding.is_(None),
            KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where(KnowledgeDocument.id == document_id).scalar_subquery(),
        )
        .order_by(KnowledgeChunk.chunk_no)
        .limit(limit)
    )
    return list(rows)


async def embedded_chunks(
    session: AsyncSession, user_id: int, limit: int, *, after_id: int = 0, model: str | None = None
) -> list[KnowledgeChunk]:
    query = (
        select(KnowledgeChunk)
        .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
        .where(
            KnowledgeChunk.user_id == user_id,
            KnowledgeDocument.user_id == user_id,
            KnowledgeDocument.deleted_at.is_(None),
            KnowledgeDocument.status == "READY",
            KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id,
            KnowledgeChunk.embedding.is_not(None),
            KnowledgeChunk.id > after_id,
        )
        .order_by(KnowledgeChunk.id)
        .limit(limit)
    )
    if model is not None:
        query = query.where(KnowledgeChunk.embedding_model == model)
    rows = await session.scalars(query)
    return list(rows)


async def keyword_chunks(
    session: AsyncSession, user_id: int, terms: list[str], limit: int
) -> list[KnowledgeChunk]:
    query = (
        select(KnowledgeChunk)
        .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
        .where(
            KnowledgeChunk.user_id == user_id,
            KnowledgeDocument.user_id == user_id,
            KnowledgeDocument.deleted_at.is_(None),
            KnowledgeDocument.status == "READY",
            KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id,
        )
    )
    for term in terms:
        query = query.where(KnowledgeChunk.content.ilike(f"%{term}%"))
    rows = await session.scalars(query.order_by(KnowledgeChunk.id.desc()).limit(limit))
    return list(rows)


async def delete_chunks(session: AsyncSession, user_id: int, document_id: int, *, revision_id: int) -> None:
    from sqlalchemy import delete

    await session.execute(
        delete(KnowledgeChunk).where(
            KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id,
            KnowledgeChunk.revision_id == revision_id,
        )
    )
