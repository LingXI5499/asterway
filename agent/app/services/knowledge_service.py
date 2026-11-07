"""知识文档上传、解析、分块、向量化与后台任务处理。"""

import asyncio
import hashlib
import logging
import re
from datetime import UTC, datetime
from importlib.util import find_spec
from pathlib import Path
from uuid import uuid4

from sqlalchemy.exc import SQLAlchemyError

from app.core.config import get_settings
from app.core.errors import AgentError
from app.db.models import BackgroundTask, KnowledgeChunk, KnowledgeDocument, Memory
from app.db.session import Database
from app.db.time import database_time
from app.db.workspace_models import KnowledgeRevision
from app.repositories import background_task_repository as task_repo
from app.repositories import knowledge_repository as repo
from app.services.embedding_service import embed_texts

logger = logging.getLogger(__name__)

_TEXT_TYPES = {
    "text/plain",
    "text/markdown",
    "application/octet-stream",
}
_PDF_TYPE = "application/pdf"
_DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
_ALLOWED_TYPES = _TEXT_TYPES | {_PDF_TYPE, _DOCX_TYPE}
_AGENT_ROOT = Path(__file__).resolve().parents[2]


def _storage_root() -> Path:
    configured = Path(get_settings().agent_knowledge_dir)
    root = configured if configured.is_absolute() else _AGENT_ROOT / configured
    root.mkdir(parents=True, exist_ok=True)
    return root


def _clean_name(value: str, *, limit: int) -> str:
    name = Path(value).name.strip()
    if not name:
        raise AgentError(422, "KNOWLEDGE_NAME_INVALID", "文件名称不能为空白")
    return name[:limit]


def _stamp(row: KnowledgeDocument | BackgroundTask | Memory) -> datetime:
    return max(datetime.now(UTC), row.created_at)


def document_vo(row: KnowledgeDocument) -> dict[str, object]:
    return {
        "id": str(row.id),
        "title": row.title,
        "fileName": row.file_name,
        "mimeType": row.mime_type,
        "sizeBytes": row.size_bytes,
        "status": row.status,
        "chunkCount": row.chunk_count,
        "errorMessage": row.error_message,
        "createdAt": row.created_at,
        "updatedAt": row.updated_at,
        "version": row.version,
        "currentRevisionId": str(row.current_revision_id) if row.current_revision_id else None,
    }


def chunk_vo(row: KnowledgeChunk) -> dict[str, object]:
    return {
        "id": str(row.id),
        "chunkNo": row.chunk_no,
        "revisionId": str(row.revision_id),
        "pageNumber": row.page_number,
        "heading": row.heading,
        "content": row.content,
        "tokenCount": row.token_count,
        "hasEmbedding": row.embedding is not None,
        "createdAt": row.created_at,
    }


def task_vo(row: BackgroundTask) -> dict[str, object]:
    return {
        "id": str(row.id),
        "kind": row.kind,
        "refType": row.ref_type,
        "refId": str(row.ref_id),
        "status": row.status,
        "attempts": row.attempts,
        "errorCode": row.error_code,
        "errorMessage": row.error_message,
        "scheduledAt": row.scheduled_at,
        "startedAt": row.started_at,
        "finishedAt": row.finished_at,
        "version": row.version,
        "createdAt": row.created_at,
        "updatedAt": row.updated_at,
    }


async def upload(
    db: Database,
    user_id: int,
    *,
    file_name: str,
    mime_type: str,
    data: bytes,
    title: str | None,
) -> tuple[KnowledgeDocument, bool]:
    settings = get_settings()
    clean_name = _clean_name(file_name, limit=255)
    if Path(clean_name).suffix.lower() not in {".txt", ".md", ".pdf", ".docx"}:
        raise AgentError(
            422, "KNOWLEDGE_TYPE_UNSUPPORTED", "只支持 TXT、Markdown、PDF 与 DOCX 文件"
        )
    if not data:
        raise AgentError(422, "KNOWLEDGE_EMPTY", "上传文件不能为空")
    if len(data) > settings.agent_knowledge_max_bytes:
        raise AgentError(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的最大大小")
    mime = (mime_type or "application/octet-stream").split(";")[0].strip().lower()
    if mime not in _ALLOWED_TYPES:
        raise AgentError(
            422, "KNOWLEDGE_TYPE_UNSUPPORTED", "只支持文本、Markdown、PDF 与 DOCX 文件"
        )
    checksum = hashlib.sha256(data).hexdigest()
    async with db.session() as session:
        existing = await repo.find_by_checksum(session, user_id, checksum)
        if existing is not None:
            return existing, False
        storage_key = f"{user_id}/{uuid4().hex}{Path(clean_name).suffix.lower()}"
        target = _storage_root() / storage_key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        title_value = (title or clean_name).strip()
        if not title_value:
            title_value = clean_name
        row = await repo.create_document(
            session,
            user_id=user_id,
            title=_clean_name(title_value, limit=200),
            file_name=clean_name,
            mime_type=mime,
            size_bytes=len(data),
            checksum=checksum,
            storage_key=storage_key,
            status="UPLOADED",
        )
        revision = KnowledgeRevision(
            user_id=user_id, document_id=row.id, revision_no=1, file_name=clean_name,
            mime_type=mime, size_bytes=len(data), checksum=checksum, storage_key=storage_key,
            status="UPLOADED", index_status="PENDING", chunk_count=0,
        )
        session.add(revision)
        await session.flush()
        row.current_revision_id = revision.id
        await task_repo.enqueue(
            session,
            user_id=user_id,
            kind="DOCUMENT_PARSE",
            ref_type="KNOWLEDGE_DOCUMENT",
            ref_id=row.id,
        )
        await session.refresh(row)
        return row, True


async def list_documents(
    db: Database,
    user_id: int,
    *,
    status: str | None,
    keyword: str | None,
    page: int,
    size: int,
) -> tuple[list[KnowledgeDocument], int]:
    async with db.session() as session:
        return await repo.page_documents(
            session,
            user_id=user_id,
            status=status,
            keyword=keyword,
            page_number=page,
            size=size,
        )


async def get_document(db: Database, user_id: int, document_id: int) -> KnowledgeDocument:
    async with db.session() as session:
        row = await repo.find_document(session, user_id, document_id)
    if row is None:
        raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "没有找到可访问的知识文档")
    return row


async def page_chunks(
    db: Database, user_id: int, document_id: int, *, page: int, size: int
) -> tuple[list[KnowledgeChunk], int]:
    async with db.session() as session:
        row = await repo.find_document(session, user_id, document_id)
        if row is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "没有找到可访问的知识文档")
        return await repo.page_chunks(
            session,
            user_id=user_id,
            document_id=document_id,
            page_number=page,
            size=size,
        )


async def delete_document(db: Database, user_id: int, document_id: int, version: int) -> None:
    async with db.session() as session:
        row = await repo.find_document(session, user_id, document_id, for_update=True)
        if row is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "没有找到可访问的知识文档")
        if row.version != version:
            raise AgentError(409, "KNOWLEDGE_VERSION_CONFLICT", "文档状态已变化，请刷新后重试")
        stamp = await database_time(session, row)
        row.status = "DELETED"
        row.deleted_at = stamp
        row.updated_at = stamp
        row.version += 1
        await task_repo.cancel_for_ref(
            session,
            kind="DOCUMENT_PARSE",
            ref_type="KNOWLEDGE_DOCUMENT",
            ref_id=document_id,
        )
        await task_repo.cancel_for_ref(
            session,
            kind="DOCUMENT_EMBEDDING",
            ref_type="KNOWLEDGE_DOCUMENT",
            ref_id=document_id,
        )


async def page_tasks(
    db: Database, user_id: int, *, status: str | None, page: int, size: int
) -> tuple[list[BackgroundTask], int]:
    async with db.session() as session:
        return await task_repo.page_tasks(
            session, user_id=user_id, status=status, page_number=page, size=size
        )


def extract_text(*, file_name: str, mime_type: str, data: bytes) -> str:
    suffix = Path(file_name).suffix.lower()
    if mime_type == _PDF_TYPE or suffix == ".pdf":
        if find_spec("pypdf") is None:
            raise AgentError(503, "KNOWLEDGE_PARSER_MISSING", "PDF 解析组件未安装")
        from io import BytesIO

        from pypdf import PdfReader

        reader = PdfReader(BytesIO(data))
        return "\n".join((page.extract_text() or "") for page in reader.pages)
    if mime_type == _DOCX_TYPE or suffix in {".docx", ".doc"}:
        if find_spec("docx") is None:
            raise AgentError(503, "KNOWLEDGE_PARSER_MISSING", "Word 解析组件未安装")
        from io import BytesIO

        import docx

        document = docx.Document(BytesIO(data))
        return "\n".join(paragraph.text for paragraph in document.paragraphs)
    if mime_type in _TEXT_TYPES or suffix in {".txt", ".md", ".markdown"}:
        return data.decode("utf-8", errors="replace")
    raise AgentError(422, "KNOWLEDGE_TYPE_UNSUPPORTED", "不支持的文件类型")


def split_text(text: str) -> list[str]:
    settings = get_settings()
    normalized = re.sub(r"\r\n?", "\n", text).strip()
    if not normalized:
        return []
    paragraphs = [part.strip() for part in re.split(r"\n{2,}", normalized) if part.strip()]
    chunks: list[str] = []
    current = ""
    overlap = settings.agent_knowledge_chunk_overlap_chars
    for paragraph in paragraphs:
        candidate = f"{current}\n\n{paragraph}" if current else paragraph
        if len(candidate) <= settings.agent_knowledge_chunk_chars:
            current = candidate
            continue
        if current:
            chunks.append(current)
        start = max(0, len(current) - overlap) if current else 0
        current = (current[start:] + "\n\n" + paragraph).strip() if start else paragraph
        while len(current) > settings.agent_knowledge_chunk_chars:
            chunks.append(current[: settings.agent_knowledge_chunk_chars])
            current = current[settings.agent_knowledge_chunk_chars - overlap :].strip()
    if current:
        chunks.append(current)
    if len(chunks) > settings.agent_knowledge_max_chunks:
        raise AgentError(413, "KNOWLEDGE_TOO_MANY_CHUNKS", "文档分块超出上限，请拆分文件后上传")
    return chunks


def extract_sections(*, file_name: str, mime_type: str, data: bytes) -> list[tuple[int | None, str | None, str]]:
    if Path(file_name).suffix.lower() == ".pdf" or mime_type == _PDF_TYPE:
        from io import BytesIO

        from pypdf import PdfReader
        return [(number, None, page.extract_text() or "")
                for number, page in enumerate(PdfReader(BytesIO(data)).pages, 1)]
    text = extract_text(file_name=file_name, mime_type=mime_type, data=data)
    if Path(file_name).suffix.lower() != ".md":
        return [(None, None, text)]
    sections = []
    heading = None
    lines = []
    for line in text.splitlines():
        match = re.match(r"^#{1,6}\s+(.+)$", line)
        if match:
            if lines:
                sections.append((None, heading, "\n".join(lines)))
            heading = match.group(1)[:300]
            lines = [line]
        else:
            lines.append(line)
    if lines:
        sections.append((None, heading, "\n".join(lines)))
    return sections


def _token_count(text: str) -> int:
    return max(1, (len(text) + 3) // 4)


async def process_parse(db: Database, task: BackgroundTask) -> None:
    settings = get_settings()
    async with db.session() as session:
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        if document is None:
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待解析文档不存在")
        if document.status == "DELETED":
            raise AgentError(409, "KNOWLEDGE_DELETED", "文档已删除")
        await task_repo.require_claim(session, task)
        document.status = "PARSING"
        document.updated_at = await database_time(session, document)
        file_name = document.file_name
        mime_type = document.mime_type
        storage_key = document.storage_key
        revision_id = document.current_revision_id
        revision = await session.get(KnowledgeRevision, revision_id)
        if revision is not None:
            revision.status = "PARSING"
            revision.updated_at = document.updated_at
    path = _storage_root() / storage_key
    try:
        data = await asyncio.to_thread(path.read_bytes)
        sections = await asyncio.to_thread(
            extract_sections, file_name=file_name, mime_type=mime_type, data=data
        )
        if sum(len(text) for _, _, text in sections) > settings.agent_knowledge_parse_max_chars:
            raise AgentError(413, "KNOWLEDGE_TEXT_TOO_LARGE", "文档正文超出解析上限")
        pieces = [(page, heading, piece) for page, heading, text in sections for piece in split_text(text)]
        if len(pieces) > settings.agent_knowledge_max_chunks:
            raise AgentError(413, "KNOWLEDGE_TOO_MANY_CHUNKS", "文档分块超出上限，请拆分文件")
        if not pieces:
            raise AgentError(422, "KNOWLEDGE_NO_TEXT", "文档没有可检索的文本")
        async with db.session() as session:
            document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
            if document is None or document.status == "DELETED":
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待解析文档不存在")
            await task_repo.require_claim(session, task)
            if document.current_revision_id != revision_id:
                raise AgentError(409, "TASK_CLAIM_LOST", "文档已有新版本")
            await repo.delete_chunks(session, task.user_id, task.ref_id, revision_id=revision_id)
            for index, (page, heading, piece) in enumerate(pieces):
                session.add(
                    KnowledgeChunk(
                        document_id=document.id,
                        revision_id=revision_id,
                        page_number=page, heading=heading,
                        user_id=document.user_id,
                        chunk_no=index,
                        content=piece,
                        token_count=_token_count(piece),
                    )
                )
            stamp = await database_time(session, document)
            document.status = "READY"
            document.chunk_count = len(pieces)
            document.error_message = None
            document.updated_at = stamp
            document.version += 1
            revision = await session.get(KnowledgeRevision, revision_id)
            if revision is not None:
                revision.status = "READY"
                revision.chunk_count = len(pieces)
                revision.error_message = None
                revision.updated_at = stamp
            await task_repo.enqueue(
                session,
                user_id=task.user_id,
                kind="DOCUMENT_EMBEDDING",
                ref_type="KNOWLEDGE_DOCUMENT",
                ref_id=document.id,
            )
    except AgentError as error:
        await _document_failed(db, task.user_id, task.ref_id, error.code, error.message, task=task)
        raise
    except (OSError, SQLAlchemyError, UnicodeError) as error:
        await _document_failed(
            db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档解析失败", task=task
        )
        raise AgentError(500, "KNOWLEDGE_PARSE_FAILED", "文档解析失败") from error
    except Exception as error:
        await _document_failed(
            db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档内容无法解析", task=task
        )
        raise AgentError(422, "KNOWLEDGE_PARSE_FAILED", "文档内容无法解析") from error


async def _document_failed(
    db: Database, user_id: int, document_id: int, code: str, message: str,
    *, task: BackgroundTask | None = None,
) -> None:
    async with db.session() as session:
        document = await repo.find_document(session, user_id, document_id, for_update=True)
        if document is None or document.status == "DELETED":
            return
        if task is not None:
            try:
                await task_repo.require_claim(session, task)
            except AgentError:
                return
        stamp = await database_time(session, document)
        document.status = "FAILED"
        document.error_message = f"{code}: {message}"[:1000]
        document.updated_at = stamp
        document.version += 1
        revision = await session.get(KnowledgeRevision, document.current_revision_id)
        if revision is not None:
            revision.status = "FAILED"
            revision.error_message = document.error_message
            revision.updated_at = stamp


async def revision_index_failed(
    db: Database, task: BackgroundTask, code: str, message: str
) -> None:
    """向量化失败时标记当前版本的索引状态；必须先复核任务领取代次。"""
    async with db.session() as session:
        try:
            await task_repo.require_claim(session, task)
        except AgentError:
            return
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        if document is None or document.status != "READY":
            return
        revision = await session.get(
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        if revision is None or revision.index_status == "READY":
            return
        stamp = await database_time(session, revision)
        revision.index_status = "FAILED"
        revision.error_message = f"{code}: {message}"[:1000]
        revision.updated_at = stamp


async def recover_parse_failed(db: Database, task: BackgroundTask) -> None:
    """超时任务最终失败后补齐解析状态；只处理仍停留在解析中的当前版本。"""
    async with db.session() as session:
        active = await task_repo.find_active(
            session, kind="DOCUMENT_PARSE", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id
        )
        if active is not None:
            return
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        if document is None or document.status != "PARSING":
            return
        stamp = await database_time(session, document)
        document.status = "FAILED"
        document.error_message = "TASK_TIMEOUT: 后台任务超时且超过最大尝试次数"[:1000]
        document.updated_at = stamp
        document.version += 1
        revision = await session.get(
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        if revision is not None and revision.status == "PARSING":
            revision.status = "FAILED"
            revision.error_message = document.error_message
            revision.updated_at = stamp


async def recover_embedding_failed(db: Database, task: BackgroundTask) -> None:
    """超时任务最终失败后补齐索引状态；已成功或已有新任务时不覆盖。"""
    async with db.session() as session:
        active = await task_repo.find_active(
            session, kind="DOCUMENT_EMBEDDING", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id
        )
        if active is not None:
            return
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        if document is None or document.status != "READY":
            return
        revision = await session.get(
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        if revision is None or revision.index_status != "PENDING":
            return
        revision.index_status = "FAILED"
        revision.error_message = "TASK_TIMEOUT: 后台任务超时且超过最大尝试次数"
        revision.updated_at = await database_time(session, revision)


async def process_embedding(db: Database, task: BackgroundTask) -> None:
    """一个领取处理全部批次；每批写入前复核文档和任务领取代次。"""
    settings = get_settings()
    while True:
        async with db.session() as session:
            document = await repo.find_document(session, task.user_id, task.ref_id)
            if document is None or document.status != "READY":
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待向量化文档不存在")
            await task_repo.require_claim(session, task)
            chunks = await repo.chunks_without_embedding(
                session, task.user_id, task.ref_id, settings.agent_knowledge_embedding_batch
            )
            if not chunks:
                revision = await session.get(KnowledgeRevision, document.current_revision_id)
                if revision is not None:
                    revision.index_status = "READY"
                    revision.error_message = None
                    revision.updated_at = await database_time(session, revision)
                return
            texts = [chunk.content for chunk in chunks]
        vectors = await embed_texts(texts)
        if len(vectors) != len(chunks):
            raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量数量与文本数量不一致")
        async with db.session() as session:
            document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
            if document is None or document.status != "READY":
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待向量化文档不存在")
            await task_repo.require_claim(session, task)
            for chunk, vector in zip(chunks, vectors, strict=True):
                row = await session.get(KnowledgeChunk, chunk.id)
                if row is None or row.user_id != task.user_id:
                    continue
                if row.revision_id != document.current_revision_id:
                    raise AgentError(409, "TASK_CLAIM_LOST", "文档已有新版本")
                row.vector_value = vector
                row.embedding = vector
                row.embedding_model = settings.embedding_model
                row.embedding_dimension = len(vector)
                row.updated_at = await database_time(session, row)


async def process_memory_embedding(db: Database, task: BackgroundTask) -> None:
    settings = get_settings()
    async with db.session() as session:
        memory = await session.get(Memory, task.ref_id)
        if memory is None or memory.user_id != task.user_id or memory.deleted_at is not None:
            raise AgentError(404, "MEMORY_NOT_FOUND", "待向量化记忆不存在")
        if memory.status != "CONFIRMED":
            return
        content = memory.content
    vectors = await embed_texts([content])
    vector = vectors[0]
    async with db.session() as session:
        from sqlalchemy import select

        from app.db.models import MemoryEmbedding

        memory = await session.get(Memory, task.ref_id, with_for_update=True)
        if (
            memory is None
            or memory.user_id != task.user_id
            or memory.deleted_at is not None
            or memory.status != "CONFIRMED"
        ):
            return
        await task_repo.require_claim(session, task)
        if memory.content != content:
            raise AgentError(409, "TASK_CLAIM_LOST", "记忆已修改，等待重新向量化")
        existing = await session.scalar(
            select(MemoryEmbedding).where(
                MemoryEmbedding.memory_id == task.ref_id, MemoryEmbedding.user_id == task.user_id
            )
        )
        if existing is None:
            session.add(
                MemoryEmbedding(
                    memory_id=task.ref_id,
                    user_id=task.user_id,
                    embedding_model=settings.embedding_model,
                    dimension=len(vector),
                    embedding=vector,
                    vector_value=vector,
                )
            )
        else:
            existing.embedding_model = settings.embedding_model
            existing.dimension = len(vector)
            existing.embedding = vector
            existing.vector_value = vector
            existing.updated_at = await database_time(session, existing)


async def search_chunks(
    db: Database, user_id: int, query: str, limit: int
) -> list[tuple[KnowledgeChunk, float]]:
    """在数据库内按用户、有效版本、模型过滤后计算距离；文本作为可见降级。"""
    from sqlalchemy import select

    settings = get_settings()
    try:
        vector = (await embed_texts([query]))[0]
        async with db.session() as session:
            distance = KnowledgeChunk.vector_value.cosine_distance(vector)
            result = await session.execute(
                select(KnowledgeChunk, distance.label("distance"))
                .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
                .where(
                    KnowledgeChunk.user_id == user_id,
                    KnowledgeDocument.user_id == user_id,
                    KnowledgeDocument.deleted_at.is_(None),
                    KnowledgeDocument.status == "READY",
                    KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id,
                    KnowledgeChunk.embedding_model == settings.embedding_model,
                    KnowledgeChunk.embedding_dimension == 1024,
                    KnowledgeChunk.vector_value.is_not(None),
                )
                .order_by(distance, KnowledgeChunk.id).limit(limit)
            )
            rows = [(row, 1.0-float(score)) for row, score in result if score <= 0.8]
            if rows:
                return rows
    except AgentError:
        logger.warning("knowledge_vector_fallback user=%s", user_id)
    terms = [term for term in re.split(r"[\s，。、,.;；:：!?！？]+", query) if len(term) >= 2][:6]
    if not terms:
        return []
    async with db.session() as session:
        rows = await repo.keyword_chunks(session, user_id, terms, limit)
    return [(row, 0.0) for row in rows]
