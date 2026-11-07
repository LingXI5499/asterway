"""文档历史和重新处理；用户与文档范围在每次读取、修改时核对。"""

import hashlib
from pathlib import Path
from uuid import uuid4

from sqlalchemy import func, select

from app.core.config import get_settings
from app.core.errors import AgentError
from app.db.models import KnowledgeChunk
from app.db.session import Database
from app.db.time import database_time
from app.db.workspace_models import KnowledgeRevision
from app.repositories import background_task_repository as tasks
from app.repositories import knowledge_repository as documents
from app.services.knowledge_service import _ALLOWED_TYPES, _clean_name, _storage_root


def revision_vo(row: KnowledgeRevision) -> dict:
    return {
        "id": str(row.id), "revisionNo": row.revision_no, "fileName": row.file_name,
        "sizeBytes": row.size_bytes, "status": row.status, "indexStatus": row.index_status,
        "chunkCount": row.chunk_count, "errorMessage": row.error_message,
        "createdAt": row.created_at, "updatedAt": row.updated_at,
    }


async def revisions(db: Database, user_id: int, document_id: int) -> list[dict]:
    async with db.session() as session:
        if await documents.find_document(session, user_id, document_id) is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "文档不存在或已删除")
        rows = await session.scalars(select(KnowledgeRevision).where(
            KnowledgeRevision.user_id == user_id, KnowledgeRevision.document_id == document_id,
        ).order_by(KnowledgeRevision.revision_no.desc()))
        return [revision_vo(row) for row in rows]


async def replace(db: Database, user_id: int, document_id: int, *, version: int,
                  file_name: str, mime_type: str, data: bytes):
    name = _clean_name(file_name, limit=255)
    mime = mime_type.split(";")[0].strip().lower()
    if Path(name).suffix.lower() not in {".txt", ".md", ".pdf", ".docx"} or mime not in _ALLOWED_TYPES:
        raise AgentError(422, "KNOWLEDGE_TYPE_UNSUPPORTED", "只支持 TXT、Markdown、PDF、DOCX")
    if not data or len(data) > get_settings().agent_knowledge_max_bytes:
        raise AgentError(413, "KNOWLEDGE_SIZE_INVALID", "文件为空或超过允许的最大大小")
    checksum = hashlib.sha256(data).hexdigest()
    async with db.session() as session:
        doc = await documents.find_document(session, user_id, document_id, for_update=True)
        if doc is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "文档不存在或已删除")
        if doc.version != version:
            raise AgentError(409, "KNOWLEDGE_VERSION_CONFLICT", "文档已更新，请刷新后重试")
        if doc.checksum == checksum:
            return doc
        duplicate = await documents.find_by_checksum(session, user_id, checksum)
        if duplicate is not None and duplicate.id != document_id:
            raise AgentError(409, "KNOWLEDGE_DUPLICATE", "此文件已经存在于另一篇文档")
        for kind in ("DOCUMENT_PARSE", "DOCUMENT_EMBEDDING"):
            await tasks.cancel_for_ref(session, kind=kind, ref_type="KNOWLEDGE_DOCUMENT", ref_id=doc.id)
        number = int(await session.scalar(select(func.max(KnowledgeRevision.revision_no)).where(
            KnowledgeRevision.document_id == document_id)) or 0) + 1
        # 随机受控路径，原始文件名只作为元数据，旧版本文件永不覆盖。
        key = f"{user_id}/{uuid4().hex}{Path(name).suffix.lower()}"
        target = _storage_root() / key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        row = KnowledgeRevision(user_id=user_id, document_id=doc.id, revision_no=number,
            file_name=name, mime_type=mime, size_bytes=len(data), checksum=checksum,
            storage_key=key, status="UPLOADED", index_status="PENDING", chunk_count=0)
        session.add(row)
        await session.flush()
        doc.current_revision_id = row.id
        doc.file_name, doc.mime_type, doc.size_bytes = name, mime, len(data)
        doc.checksum, doc.storage_key = checksum, key
        doc.status, doc.chunk_count, doc.error_message = "UPLOADED", 0, None
        doc.updated_at = await database_time(session, doc)
        doc.version += 1
        await tasks.enqueue(session, user_id=user_id, kind="DOCUMENT_PARSE",
            ref_type="KNOWLEDGE_DOCUMENT", ref_id=doc.id)
        return doc


async def retry(db: Database, user_id: int, document_id: int, version: int) -> None:
    async with db.session() as session:
        doc = await documents.find_document(session, user_id, document_id, for_update=True)
        if doc is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "文档不存在或已删除")
        if doc.version != version:
            raise AgentError(409, "KNOWLEDGE_VERSION_CONFLICT", "文档已更新，请刷新后重试")
        kind = "DOCUMENT_EMBEDDING" if doc.status == "READY" else "DOCUMENT_PARSE"
        if doc.status in {"UPLOADED", "PARSING"}:
            raise AgentError(409, "KNOWLEDGE_PROCESSING", "文档正在处理，请稍后刷新")
        if await tasks.find_active(
            session, kind=kind, ref_type="KNOWLEDGE_DOCUMENT", ref_id=document_id
        ) is not None:
            raise AgentError(409, "KNOWLEDGE_PROCESSING", "文档正在处理，请稍后刷新")
        revision = await session.get(KnowledgeRevision, doc.current_revision_id)
        if kind == "DOCUMENT_EMBEDDING" and revision is not None and revision.index_status == "READY":
            raise AgentError(409, "KNOWLEDGE_ALREADY_READY", "文档已完成解析与索引，无需重试")
        if revision:
            revision.error_message = None
            revision.index_status = "PENDING"
            revision.updated_at = await database_time(session, revision)
        if kind == "DOCUMENT_PARSE":
            doc.status, doc.error_message = "UPLOADED", None
        doc.updated_at = await database_time(session, doc)
        doc.version += 1
        await tasks.enqueue(session, user_id=user_id, kind=kind,
            ref_type="KNOWLEDGE_DOCUMENT", ref_id=doc.id)


async def source(db: Database, user_id: int, document_id: int, revision_id: int, chunk_id: int) -> dict:
    async with db.session() as session:
        if await documents.find_document(session, user_id, document_id) is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "引用文档已删除或无权访问")
        row = await session.scalar(select(KnowledgeChunk).where(
            KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id,
            KnowledgeChunk.revision_id == revision_id, KnowledgeChunk.id == chunk_id))
        revision = await session.get(KnowledgeRevision, revision_id)
        if row is None or revision is None or revision.user_id != user_id:
            raise AgentError(404, "KNOWLEDGE_SOURCE_NOT_FOUND", "未找到该版本引用")
        return {"id": str(row.id), "revision": revision_vo(revision), "content": row.content,
                "pageNumber": row.page_number, "heading": row.heading, "chunkNo": row.chunk_no}
