"""知识检索与来源引用：向量优先、关键词降级，结果只作为背景资料注入。"""

import logging
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.exc import SQLAlchemyError

from app.core.config import get_settings
from app.db.models import KnowledgeDocument
from app.db.session import Database
from app.services import knowledge_service

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class RetrievedChunk:
    document_id: int
    revision_id: int
    chunk_id: int
    page_number: int | None
    heading: str | None
    title: str
    chunk_no: int
    content: str
    score: float


async def retrieve(db: Database, user_id: int, query: str) -> list[RetrievedChunk]:
    settings = get_settings()
    try:
        scored = await knowledge_service.search_chunks(
            db, user_id, query, settings.agent_retrieval_max_chunks
        )
    except SQLAlchemyError:
        logger.warning("agent_retrieval_unavailable user=%s", user_id)
        return []
    if not scored:
        return []
    document_ids = list({chunk.document_id for chunk, _ in scored})
    titles: dict[int, str] = {}
    async with db.session() as session:
        rows = await session.scalars(
            select(KnowledgeDocument).where(
                KnowledgeDocument.user_id == user_id,
                KnowledgeDocument.id.in_(document_ids),
                KnowledgeDocument.deleted_at.is_(None),
                KnowledgeDocument.status == "READY",
            )
        )
        titles = {row.id: row.title for row in rows}
    return [
        RetrievedChunk(
            document_id=chunk.document_id,
            revision_id=chunk.revision_id, chunk_id=chunk.id,
            page_number=chunk.page_number, heading=chunk.heading,
            title=titles.get(chunk.document_id, "未命名资料"),
            chunk_no=chunk.chunk_no,
            content=chunk.content,
            score=score,
        )
        for chunk, score in scored
        if chunk.content.strip() and chunk.document_id in titles
    ]


async def context_block(
    db: Database,
    user_id: int,
    query: str,
    *,
    max_chars: int | None = None,
    metadata: dict | None = None,
) -> str | None:
    if not query.strip():
        return None
    settings = get_settings()
    budget = max_chars if max_chars is not None else settings.agent_retrieval_max_chars
    chunks = await retrieve(db, user_id, query)
    if not chunks:
        return None
    lines: list[str] = []
    used = 0
    for index, chunk in enumerate(chunks, start=1):
        line = (
            f"[资料{index}｜来源：{chunk.title}｜第{chunk.chunk_no + 1}段]\n{chunk.content.strip()}"
        )
        if used + len(line) + 2 > budget:
            break
        lines.append(line)
        if metadata is not None:
            metadata.setdefault("citations", []).append({
                "label": f"资料{index}", "documentId": str(chunk.document_id),
                "revisionId": str(chunk.revision_id), "chunkId": str(chunk.chunk_id),
                "title": chunk.title, "pageNumber": chunk.page_number, "heading": chunk.heading,
                "chunkNo": chunk.chunk_no, "excerpt": chunk.content[:400],
            })
        used += len(line) + 2
    if not lines:
        return None
    return "\n\n".join(lines)
