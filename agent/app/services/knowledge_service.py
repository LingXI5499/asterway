# 维护知识文档上传、解析任务、详情、重试和删除状态。
# 知识文档上传、解析、分块、向量化与后台任务处理。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 hashlib，供下面调用其功能。
import hashlib

# 引入 logging，供下面调用其功能。
import logging

# 引入 re，供下面调用其功能。
import re

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）。
from datetime import UTC, datetime

# 从 importlib.util 引入 查找spec（find_spec）。
# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 从 uuid 引入 uuid4。
from uuid import uuid4

# 从 sqlalchemy.exc 引入 SQLAlchemy错误（SQLAlchemyError）。
from sqlalchemy.exc import SQLAlchemyError

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 后台任务（BackgroundTask）、知识资料文档片段（KnowledgeChunk）、知识资料知识文档（KnowledgeDocument）、长期记忆（Memory）。
from app.db.models import BackgroundTask, KnowledgeChunk, KnowledgeDocument, Memory

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 从 app.db.workspace_models 引入 知识资料修订版本（KnowledgeRevision）。
from app.db.workspace_models import KnowledgeRevision

# 从 app.repositories 引入 任务repo（task_repo）。
from app.repositories import background_task_repository as task_repo

# 从 app.repositories 引入 repo。
from app.repositories import knowledge_repository as repo
from app.services import knowledge_base_service as bases
from app.services import knowledge_formats as formats

# 从 app.services.embedding_service 引入 embedtexts（embed_texts）。
from app.services.embedding_service import embed_texts

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 把依次包含 固定文字 'text/plain'、固定文字 'text/markdown'、固定文字 'application/octet-stream' 的集合保存到 文字TYPES（_TEXT_TYPES）。
_TEXT_TYPES = {
    # 继续传入或组合固定文字 'text/plain'，完成上面尚未结束的表达式。
    "text/plain",
    # 继续传入或组合固定文字 'text/markdown'，完成上面尚未结束的表达式。
    "text/markdown",
    # 继续传入或组合固定文字 'application/octet-stream'，完成上面尚未结束的表达式。
    "application/octet-stream",
}
# 把固定文字 'application/pdf'保存到 PDF类型（_PDF_TYPE）。
_PDF_TYPE = "application/pdf"
# 把固定文字 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'保存到 DOCX类型（_DOCX_TYPE）。
_DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
# 把按 _TEXT_TYPES | {_PDF_TYPE, _DOCX_TYPE} 计算的结果保存到 ALLOWEDTYPES（_ALLOWED_TYPES）。
_ALLOWED_TYPES = _TEXT_TYPES | {_PDF_TYPE, _DOCX_TYPE}
# 把从执行“调用 Path(__file__).resolve 完成resolve处理”得到的结果的parents属性中按固定数值 2取出的内容保存到 成长助手根（_AGENT_ROOT）。
_AGENT_ROOT = Path(__file__).resolve().parents[2]


# 函数 _storage_root：调用 _storage_root 完成存储根处理。
def _storage_root() -> Path:
    # 把执行“调用 Path 完成路径处理”得到的结果保存到 已配置（configured）。
    configured = Path(get_settings().agent_knowledge_dir)
    # 把条件 configured.is_absolute() 是否成立时使用已配置（configured），否则使用按 _AGENT_ROOT / configured 计算的结果保存到 根（root）。
    root = configured if configured.is_absolute() else _AGENT_ROOT / configured
    # 调用 root.mkdir 完成mkdir处理。
    root.mkdir(parents=True, exist_ok=True)
    # 把根（root）交还给调用方，本函数到此结束。
    return root


# 函数 _clean_name：调用 _clean_name 完成clean名称处理；接收 值（value）、上限（limit）。
def _clean_name(value: str, *, limit: int) -> str:
    # 把执行“去掉文字两端的空白”得到的结果保存到 名称（name）。
    name = Path(value).name.strip()
    # 检查条件 not name 是否成立；成立时执行缩进的分支。
    if not name:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "KNOWLEDGE_NAME_INVALID", "文件名称不能为空白")
    # 把从名称（name）中按表达式 :limit 的结果取出的内容交还给调用方，本函数到此结束。
    return name[:limit]


# 函数 _stamp：调用 _stamp 完成stamp处理；接收 当前记录（row）。
def _stamp(row: KnowledgeDocument | BackgroundTask | Memory) -> datetime:
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(datetime.now(UTC), row.created_at)


# 函数 document_vo：调用 document_vo 完成知识文档处理；接收 当前记录（row）。
def document_vo(row: KnowledgeDocument) -> dict[str, object]:
    return {'id': str(row.id), 'knowledgeBaseId': str(row.knowledge_base_id), 'knowledgeBaseName': getattr(row, 'knowledge_base_name', ''), 'indexStatus': getattr(row, 'index_status', 'PENDING'), 'indexError': getattr(row, 'index_error', None), 'title': row.title, 'fileName': row.file_name, 'mimeType': row.mime_type, 'sizeBytes': row.size_bytes, 'status': row.status, 'chunkCount': row.chunk_count, 'errorMessage': row.error_message, 'createdAt': row.created_at, 'updatedAt': row.updated_at, 'version': row.version, 'currentRevisionId': str(row.current_revision_id) if row.current_revision_id else None}


# 函数 chunk_vo：调用 chunk_vo 完成文档片段处理；接收 当前记录（row）。
def chunk_vo(row: KnowledgeChunk) -> dict[str, object]:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id),
        # 填写文档片段No（chunkNo）字段，值为表达式 row.chunk_no 的结果。
        "chunkNo": row.chunk_no,
        # 填写修订版本编号（revisionId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "revisionId": str(row.revision_id),
        # 填写当前页数值（pageNumber）字段，值为表达式 row.page_number 的结果。
        "pageNumber": row.page_number,
        # 填写heading字段，值为表达式 row.heading 的结果。
        "heading": row.heading,
        # 填写内容（content）字段，值为表达式 row.content 的结果。
        "content": row.content,
        # 填写身份凭据数量（tokenCount）字段，值为表达式 row.token_count 的结果。
        "tokenCount": row.token_count,
        # 填写是否存在向量（hasEmbedding）字段，值为条件判断的结果：row.embedding is not None。
        "hasEmbedding": row.embedding is not None,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at 的结果。
        "createdAt": row.created_at,
    }


# 函数 task_vo：调用 task_vo 完成任务处理；接收 当前记录（row）。
def task_vo(row: BackgroundTask) -> dict[str, object]:
    return {'id': str(row.id), 'documentTitle': getattr(row, 'document_title', None), 'fileName': getattr(row, 'file_name', None), 'knowledgeBaseId': getattr(row, 'knowledge_base_id', None), 'knowledgeBaseName': getattr(row, 'knowledge_base_name', None), 'kind': row.kind, 'refType': row.ref_type, 'refId': str(row.ref_id), 'status': row.status, 'attempts': row.attempts, 'errorCode': row.error_code, 'errorMessage': row.error_message, 'scheduledAt': row.scheduled_at, 'startedAt': row.started_at, 'finishedAt': row.finished_at, 'version': row.version, 'createdAt': row.created_at, 'updatedAt': row.updated_at}


# 异步函数 upload：调用 upload 完成上传处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、文件名称（file_name）、mime类型（mime_type）、数据（data）、标题（title）。
async def upload(db: Database, user_id: int, *, file_name: str, mime_type: str, data: bytes, title: str | None, knowledge_base_id: int | None = None) -> tuple[KnowledgeDocument, bool]:
    clean_name = _clean_name(file_name, limit=255)
    mime = formats.validate_file(clean_name, mime_type, data)
    checksum = hashlib.sha256(data).hexdigest()
    target = None
    try:
        async with db.session() as session:
            await bases.lock_user(session, user_id)
            base = await bases.require_base(session, user_id, knowledge_base_id) if knowledge_base_id is not None else await bases.default_base(session, user_id)
            existing = await repo.find_by_checksum(session, user_id, checksum, base.id)
            if existing is not None:
                await bases.enrich_documents(session, [existing])
                return existing, False
            storage_key = f"{user_id}/{uuid4().hex}{Path(clean_name).suffix.lower()}"
            target = _storage_root() / storage_key
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            row = await repo.create_document(session, user_id=user_id, knowledge_base_id=base.id,
                title=_clean_name((title or clean_name).strip() or clean_name, limit=200), file_name=clean_name,
                mime_type=mime, size_bytes=len(data), checksum=checksum, storage_key=storage_key, status="UPLOADED")
            revision = KnowledgeRevision(user_id=user_id, document_id=row.id, revision_no=1, file_name=clean_name,
                mime_type=mime, size_bytes=len(data), checksum=checksum, storage_key=storage_key,
                status="UPLOADED", index_status="PENDING", chunk_count=0)
            session.add(revision)
            await session.flush()
            row.current_revision_id = revision.id
            await task_repo.enqueue(session, user_id=user_id, kind="DOCUMENT_PARSE", ref_type="KNOWLEDGE_DOCUMENT", ref_id=row.id)
            await session.flush()
            await bases.enrich_documents(session, [row])
            return row, True
    except BaseException:
        # 事务未提交时清理本次生成的孤立文件，不处理任何已有文件。
        if target is not None and target.resolve().is_relative_to(_storage_root().resolve()):
            target.unlink(missing_ok=True)
        raise



# 异步函数 list_documents：列出 知识文档列表（documents）；接收 Agent 数据库访问器（db）、用户编号（user_id）、状态（status）、keyword、当前页（page）、每页条数（size）。
async def list_documents(db: Database, user_id: int, *, status: str | None, keyword: str | None, page: int, size: int, knowledge_base_id: int | None = None):
    async with db.session() as session:
        if knowledge_base_id is not None:
            await bases.require_base(session, user_id, knowledge_base_id)
        rows, total = await repo.page_documents(session, user_id=user_id, status=status, keyword=keyword,
            page_number=page, size=size, knowledge_base_id=knowledge_base_id)
        await bases.enrich_documents(session, rows)
        return rows, total



# 异步函数 get_document：读取 知识文档（_document） 的值；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）。
async def get_document(db: Database, user_id: int, document_id: int) -> KnowledgeDocument:
    async with db.session() as session:
        row = await repo.find_document(session, user_id, document_id)
        if row is not None:
            await bases.enrich_documents(session, [row])
    if row is None:
        raise AgentError(404, 'KNOWLEDGE_NOT_FOUND', '没有找到可访问的知识文档')
    return row


# 异步函数 page_chunks：调用 page_chunks 完成当前页文档片段列表处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、当前页（page）、每页条数（size）。
async def page_chunks(
    # 填写Agent 数据库访问器（db）字段，值为按表达式计算得到的值：Database, user_id: int, document_id: int, *, page: int, size: int。
    db: Database, user_id: int, document_id: int, *, page: int, size: int
# 继续传入或组合表达式 ) -> tuple[list[KnowledgeChunk], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[KnowledgeChunk], int]:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_document(session, user_id, document_id)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "没有找到可访问的知识文档")
        # 把等待执行“调用 repo.page_chunks 完成当前页文档片段列表处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await repo.page_chunks(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入知识文档编号（document_id），值为知识文档编号（document_id）。
            document_id=document_id,
            # 传入当前页数值（page_number），值为当前页（page）。
            page_number=page,
            # 传入每页条数（size），值为每页条数（size）。
            size=size,
        )


# 异步函数 delete_document：删除 知识文档（document）；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、版本号（version）。
async def delete_document(db: Database, user_id: int, document_id: int, version: int) -> None:
    async with db.session() as session:
        await bases.lock_user(session, user_id)
        row = await repo.find_document(session, user_id, document_id, for_update=True)
        if row is None:
            raise AgentError(404, 'KNOWLEDGE_NOT_FOUND', '没有找到可访问的知识文档')
        if row.version != version:
            raise AgentError(409, 'KNOWLEDGE_VERSION_CONFLICT', '文档状态已变化，请刷新后重试')
        stamp = await database_time(session, row)
        row.status = 'DELETED'
        row.deleted_at = stamp
        row.updated_at = stamp
        row.version += 1
        await task_repo.cancel_for_ref(session, kind='DOCUMENT_PARSE', ref_type='KNOWLEDGE_DOCUMENT', ref_id=document_id)
        await task_repo.cancel_for_ref(session, kind='DOCUMENT_EMBEDDING', ref_type='KNOWLEDGE_DOCUMENT', ref_id=document_id)


# 异步函数 page_tasks：调用 page_tasks 完成当前页任务列表处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、状态（status）、当前页（page）、每页条数（size）。
async def page_tasks(db: Database, user_id: int, *, status: str | None, page: int, size: int, knowledge_base_id: int | None=None):
    from sqlalchemy import func, select
    async with db.session() as session:
        if knowledge_base_id is not None:
            await bases.require_base(session, user_id, knowledge_base_id)
        conditions = [BackgroundTask.user_id == user_id, BackgroundTask.ref_type == 'KNOWLEDGE_DOCUMENT']
        if status:
            conditions.append(BackgroundTask.status == status)
        if knowledge_base_id is not None:
            conditions.append(BackgroundTask.ref_id.in_(select(KnowledgeDocument.id).where(KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id == knowledge_base_id)))
        total = await session.scalar(select(func.count()).select_from(BackgroundTask).where(*conditions))
        rows = list(await session.scalars(select(BackgroundTask).where(*conditions).order_by(BackgroundTask.created_at.desc(), BackgroundTask.id.desc()).offset((page - 1) * size).limit(size)))
        documents = list(await session.scalars(select(KnowledgeDocument).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.id.in_([row.ref_id for row in rows]))))
        await bases.enrich_documents(session, documents)
        by_id = {doc.id: doc for doc in documents}
        for row in rows:
            doc = by_id.get(row.ref_id)
            row.document_title = doc.title if doc else None
            row.file_name = doc.file_name if doc else None
            row.knowledge_base_id = str(doc.knowledge_base_id) if doc else None
            row.knowledge_base_name = doc.knowledge_base_name if doc else None
        return (rows, int(total or 0))



# 函数 extract_text：调用 extract_text 完成提取文字处理；接收 文件名称（file_name）、mime类型（mime_type）、数据（data）。
def extract_text(*, file_name: str, mime_type: str, data: bytes) -> str:
    # 兼容原调用入口；实际解析与分段共用同一实现。
    return "\n".join(text for _, _, text in formats.extract_sections(file_name=file_name, mime_type=mime_type, data=data))



# 函数 split_text：调用 split_text 完成split文字处理；接收 文字（text）。
def split_text(text: str) -> list[str]:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把执行“去掉文字两端的空白”得到的结果保存到 normalized。
    normalized = re.sub(r"\r\n?", "\n", text).strip()
    # 检查条件 not normalized 是否成立；成立时执行缩进的分支。
    if not normalized:
        # 把空列表或集合交还给调用方，本函数到此结束。
        return []
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 paragraphs。
    paragraphs = [part.strip() for part in re.split(r"\n{2,}", normalized) if part.strip()]
    # 声明 文档片段列表（chunks），类型为 list[str]；其内容为空列表或集合。
    chunks: list[str] = []
    # 把固定文字 ''保存到 当前（current）。
    current = ""
    # 把配置（settings）的成长助手知识资料文档片段overlapchars（agent_knowledge_chunk_overlap_chars）属性保存到 overlap。
    overlap = settings.agent_knowledge_chunk_overlap_chars
    # 从paragraphs依次取出paragraph，每项执行一次下面的处理。
    for paragraph in paragraphs:
        # 把条件 current 是否成立时使用在文字占位处填入变量后组成的字符串，否则使用paragraph保存到 候选（candidate）。
        candidate = f"{current}\n\n{paragraph}" if current else paragraph
        # 检查条件 len(candidate) <= settings.agent_knowledge_chunk_chars 是否成立；成立时执行缩进的分支。
        if len(candidate) <= settings.agent_knowledge_chunk_chars:
            # 把候选（candidate）保存到 当前（current）。
            current = candidate
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 检查条件 current 是否成立；成立时执行缩进的分支。
        if current:
            # 把新条目放到列表末尾。
            chunks.append(current)
        # 把条件 current 是否成立时使用执行“调用 max 完成最大处理”得到的结果，否则使用固定数值 0保存到 开始（start）。
        start = max(0, len(current) - overlap) if current else 0
        # 把条件 start 是否成立时使用执行“去掉文字两端的空白”得到的结果，否则使用paragraph保存到 当前（current）。
        current = (current[start:] + "\n\n" + paragraph).strip() if start else paragraph
        # 只要条件 len(current) > settings.agent_knowledge_chunk_chars 是否成立，就继续执行下面的循环。
        while len(current) > settings.agent_knowledge_chunk_chars:
            # 把新条目放到列表末尾。
            chunks.append(current[: settings.agent_knowledge_chunk_chars])
            # 把执行“去掉文字两端的空白”得到的结果保存到 当前（current）。
            current = current[settings.agent_knowledge_chunk_chars - overlap :].strip()
    # 检查条件 current 是否成立；成立时执行缩进的分支。
    if current:
        # 把新条目放到列表末尾。
        chunks.append(current)
    # 检查条件 len(chunks) > settings.agent_knowledge_max_chunks 是否成立；成立时执行缩进的分支。
    if len(chunks) > settings.agent_knowledge_max_chunks:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(413, "KNOWLEDGE_TOO_MANY_CHUNKS", "文档分块超出上限，请拆分文件后上传")
    # 把文档片段列表（chunks）交还给调用方，本函数到此结束。
    return chunks


# 函数 extract_sections：调用 extract_sections 完成提取sections处理；接收 文件名称（file_name）、mime类型（mime_type）、数据（data）。
def extract_sections(*, file_name: str, mime_type: str, data: bytes):
    return formats.extract_sections(file_name=file_name, mime_type=mime_type, data=data)



# 函数 _token_count：调用 _token_count 完成身份凭据数量处理；接收 文字（text）。
def _token_count(text: str) -> int:
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(1, (len(text) + 3) // 4)


# 异步函数 process_parse：调用 process_parse 完成处理过程解析处理；接收 Agent 数据库访问器（db）、任务（task）。
async def process_parse(db: Database, task: BackgroundTask) -> None:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        # 检查document is None，即对应内容是否不存在；成立时执行缩进的分支。
        if document is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待解析文档不存在")
        # 检查条件 document.status == 'DELETED' 是否成立；成立时执行缩进的分支。
        if document.status == "DELETED":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "KNOWLEDGE_DELETED", "文档已删除")
        # 等待要求满足 claim完成后再继续。
        await task_repo.require_claim(session, task)
        # 把固定状态或编码 PARSING（PARSING）保存到 知识文档中的状态（document.status）。
        document.status = "PARSING"
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 知识文档中的更新时at（document.updated_at）。
        document.updated_at = await database_time(session, document)
        # 把知识文档（document）的文件名称（file_name）属性保存到 文件名称（file_name）。
        file_name = document.file_name
        # 把知识文档（document）的mime类型（mime_type）属性保存到 mime类型（mime_type）。
        mime_type = document.mime_type
        # 把知识文档（document）的存储键（storage_key）属性保存到 存储键（storage_key）。
        storage_key = document.storage_key
        # 把知识文档（document）的当前修订版本编号（current_revision_id）属性保存到 修订版本编号（revision_id）。
        revision_id = document.current_revision_id
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(KnowledgeRevision, revision_id)
        # 检查条件 revision is not None 是否成立；成立时执行缩进的分支。
        if revision is not None:
            # 把固定状态或编码 PARSING（PARSING）保存到 修订版本中的状态（revision.status）。
            revision.status = "PARSING"
            # 把知识文档（document）的更新时at（updated_at）属性保存到 修订版本中的更新时at（revision.updated_at）。
            revision.updated_at = document.updated_at
    # 把按 _storage_root() / storage_key 计算的结果保存到 路径（path）。
    path = _storage_root() / storage_key
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 asyncio.to_thread 完成tothread处理”得到的结果完成后的结果保存到 数据（data）。
        data = await asyncio.to_thread(path.read_bytes)
        # 把等待执行“调用 asyncio.to_thread 完成tothread处理”得到的结果完成后的结果保存到 sections。
        sections = await asyncio.to_thread(
            # 继续传入或组合表达式 extract_sections, file_name=file_name, mime_type=mime_type, data=data 的结果，完成上面尚未结束的表达式。
            extract_sections, file_name=file_name, mime_type=mime_type, data=data
        )
        # 检查条件 sum((len(text) for _, _, text in sections)) > settings.agent_knowledge_parse_max_chars 是否成立；成立时执行缩进的分支。
        if sum(len(text) for _, _, text in sections) > settings.agent_knowledge_parse_max_chars:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(413, "KNOWLEDGE_TEXT_TOO_LARGE", "文档正文超出解析上限")
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 pieces。
        pieces = [(page, heading, piece) for page, heading, text in sections for piece in split_text(text)]
        # 检查条件 len(pieces) > settings.agent_knowledge_max_chunks 是否成立；成立时执行缩进的分支。
        if len(pieces) > settings.agent_knowledge_max_chunks:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(413, "KNOWLEDGE_TOO_MANY_CHUNKS", "文档分块超出上限，请拆分文件")
        # 检查条件 not pieces 是否成立；成立时执行缩进的分支。
        if not pieces:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(422, "KNOWLEDGE_NO_TEXT", "文档没有可检索的文本")
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
            document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
            # 检查document is None or document.status == 'DELETED'，即对应内容是否不存在；成立时执行缩进的分支。
            if document is None or document.status == "DELETED":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待解析文档不存在")
            # 等待要求满足 claim完成后再继续。
            await task_repo.require_claim(session, task)
            # 检查条件 document.current_revision_id != revision_id 是否成立；成立时执行缩进的分支。
            if document.current_revision_id != revision_id:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "TASK_CLAIM_LOST", "文档已有新版本")
            # 等待删除 文档片段列表（chunks）完成后再继续。
            await repo.delete_chunks(session, task.user_id, task.ref_id, revision_id=revision_id)
            # 从执行“调用 enumerate 完成enumerate处理”得到的结果依次取出索引当前页headingpiece（(index, (page, heading, piece))），每项执行一次下面的处理。
            for index, (page, heading, piece) in enumerate(pieces):
                # 加入集合或登记一条待保存记录。
                session.add(
                    # 继续传入或组合调用 KnowledgeChunk 完成知识资料文档片段处理，完成上面尚未结束的表达式。
                    KnowledgeChunk(
                        # 传入知识文档编号（document_id），值为表达式 document.id 的结果。
                        document_id=document.id,
                        # 传入修订版本编号（revision_id），值为修订版本编号（revision_id）。
                        revision_id=revision_id,
                        # 传入当前页数值（page_number），值为表达式 page, heading=heading 的结果。
                        page_number=page, heading=heading,
                        # 传入用户编号（user_id），值为表达式 document.user_id 的结果。
                        user_id=document.user_id,
                        # 传入文档片段no（chunk_no），值为索引（index）。
                        chunk_no=index,
                        # 传入内容（content），值为piece。
                        content=piece,
                        # 传入身份凭据数量（token_count），值为调用 _token_count 完成身份凭据数量处理。
                        token_count=_token_count(piece),
                    )
                )
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
            stamp = await database_time(session, document)
            # 把固定状态或编码 READY（已就绪）保存到 知识文档中的状态（document.status）。
            document.status = "READY"
            # 把执行“调用 len 完成len处理”得到的结果保存到 知识文档中的文档片段数量（document.chunk_count）。
            document.chunk_count = len(pieces)
            # 把空值，表示当前还没有对应内容保存到 知识文档中的错误消息（document.error_message）。
            document.error_message = None
            # 把stamp保存到 知识文档中的更新时at（document.updated_at）。
            document.updated_at = stamp
            # 把 document.version 加上 固定数值 1，再保存回原变量。
            document.version += 1
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
            revision = await session.get(KnowledgeRevision, revision_id)
            # 检查条件 revision is not None 是否成立；成立时执行缩进的分支。
            if revision is not None:
                # 把固定状态或编码 READY（已就绪）保存到 修订版本中的状态（revision.status）。
                revision.status = "READY"
                # 把执行“调用 len 完成len处理”得到的结果保存到 修订版本中的文档片段数量（revision.chunk_count）。
                revision.chunk_count = len(pieces)
                # 把空值，表示当前还没有对应内容保存到 修订版本中的错误消息（revision.error_message）。
                revision.error_message = None
                # 把stamp保存到 修订版本中的更新时at（revision.updated_at）。
                revision.updated_at = stamp
            # 等待调用 task_repo.enqueue 完成enqueue处理完成后再继续。
            await task_repo.enqueue(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入用户编号（user_id），值为表达式 task.user_id 的结果。
                user_id=task.user_id,
                # 传入种类（kind），值为固定状态或编码 DOCUMENT_EMBEDDING（知识文档向量）。
                kind="DOCUMENT_EMBEDDING",
                # 传入响应式引用类型（ref_type），值为固定状态或编码 KNOWLEDGE_DOCUMENT（知识资料知识文档）。
                ref_type="KNOWLEDGE_DOCUMENT",
                # 传入响应式引用编号（ref_id），值为表达式 document.id 的结果。
                ref_id=document.id,
            )
    # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
    except AgentError as error:
        # 等待调用 _document_failed 完成知识文档失败处理完成后再继续。
        await _document_failed(db, task.user_id, task.ref_id, error.code, error.message, task=task)
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 捕获(OSError, SQLAlchemyError, UnicodeError)异常，执行下面的错误处理，异常对象保存为 error。
    except (OSError, SQLAlchemyError, UnicodeError) as error:
        # 等待调用 _document_failed 完成知识文档失败处理完成后再继续。
        await _document_failed(
            # 继续传入或组合表达式 db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档解析失败", task=task 的结果，完成上面尚未结束的表达式。
            db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档解析失败", task=task
        )
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(500, "KNOWLEDGE_PARSE_FAILED", "文档解析失败") from error
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:
        # 等待调用 _document_failed 完成知识文档失败处理完成后再继续。
        await _document_failed(
            # 继续传入或组合表达式 db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档内容无法解析", task=task 的结果，完成上面尚未结束的表达式。
            db, task.user_id, task.ref_id, "KNOWLEDGE_PARSE_FAILED", "文档内容无法解析", task=task
        )
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "KNOWLEDGE_PARSE_FAILED", "文档内容无法解析") from error


# 异步函数 _document_failed：调用 _document_failed 完成知识文档失败处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、编码（code）、消息（message）、任务（task）。
async def _document_failed(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, document_id: int, code: str, message: str 的结果。
    db: Database, user_id: int, document_id: int, code: str, message: str,
    # 继续传入或组合按表达式计算得到的值：*, task: BackgroundTask | None = None，完成上面尚未结束的表达式。
    *, task: BackgroundTask | None = None,
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
        document = await repo.find_document(session, user_id, document_id, for_update=True)
        # 检查document is None or document.status == 'DELETED'，即对应内容是否不存在；成立时执行缩进的分支。
        if document is None or document.status == "DELETED":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 检查条件 task is not None 是否成立；成立时执行缩进的分支。
        if task is not None:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 等待要求满足 claim完成后再继续。
                await task_repo.require_claim(session, task)
            # 捕获AgentError异常，执行下面的错误处理。
            except AgentError:
                # 把空值交还给调用方，本函数到此结束。
                return
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, document)
        # 把固定状态或编码 FAILED（失败）保存到 知识文档中的状态（document.status）。
        document.status = "FAILED"
        # 把从在文字占位处填入变量后组成的字符串中按表达式 :1000 的结果取出的内容保存到 知识文档中的错误消息（document.error_message）。
        document.error_message = f"{code}: {message}"[:1000]
        # 把stamp保存到 知识文档中的更新时at（document.updated_at）。
        document.updated_at = stamp
        # 把 document.version 加上 固定数值 1，再保存回原变量。
        document.version += 1
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(KnowledgeRevision, document.current_revision_id)
        # 检查条件 revision is not None 是否成立；成立时执行缩进的分支。
        if revision is not None:
            # 把固定状态或编码 FAILED（失败）保存到 修订版本中的状态（revision.status）。
            revision.status = "FAILED"
            # 把知识文档（document）的错误消息（error_message）属性保存到 修订版本中的错误消息（revision.error_message）。
            revision.error_message = document.error_message
            # 把stamp保存到 修订版本中的更新时at（revision.updated_at）。
            revision.updated_at = stamp


# 异步函数 revision_index_failed：向量化失败时标记当前版本的索引状态；必须先复核任务领取代次。；接收 Agent 数据库访问器（db）、任务（task）、编码（code）、消息（message）。
async def revision_index_failed(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, task: BackgroundTask, code: str, message: str 的结果。
    db: Database, task: BackgroundTask, code: str, message: str
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 向量化失败时标记当前版本的索引状态；必须先复核任务领取代次。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待要求满足 claim完成后再继续。
            await task_repo.require_claim(session, task)
        # 捕获AgentError异常，执行下面的错误处理。
        except AgentError:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        # 检查document is None or document.status != 'READY'，即对应内容是否不存在；成立时执行缩进的分支。
        if document is None or document.status != "READY":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(
            # 继续传入或组合表达式 KnowledgeRevision, document.current_revision_id, with_for_update=True 的结果，完成上面尚未结束的表达式。
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        # 检查revision is None or revision.index_status == 'READY'，即对应内容是否不存在；成立时执行缩进的分支。
        if revision is None or revision.index_status == "READY":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, revision)
        # 把固定状态或编码 FAILED（失败）保存到 修订版本中的索引状态（revision.index_status）。
        revision.index_status = "FAILED"
        # 把从在文字占位处填入变量后组成的字符串中按表达式 :1000 的结果取出的内容保存到 修订版本中的错误消息（revision.error_message）。
        revision.error_message = f"{code}: {message}"[:1000]
        # 把stamp保存到 修订版本中的更新时at（revision.updated_at）。
        revision.updated_at = stamp


# 异步函数 recover_parse_failed：超时任务最终失败后补齐解析状态；只处理仍停留在解析中的当前版本。；接收 Agent 数据库访问器（db）、任务（task）。
async def recover_parse_failed(db: Database, task: BackgroundTask) -> None:
    # 超时任务最终失败后补齐解析状态；只处理仍停留在解析中的当前版本。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 页面是否仍处于活动状态（active）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
        active = await task_repo.find_active(
            # 继续传入或组合表达式 session, kind="DOCUMENT_PARSE", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id 的结果，完成上面尚未结束的表达式。
            session, kind="DOCUMENT_PARSE", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id
        )
        # 检查条件 active is not None 是否成立；成立时执行缩进的分支。
        if active is not None:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        # 检查document is None or document.status != 'PARSING'，即对应内容是否不存在；成立时执行缩进的分支。
        if document is None or document.status != "PARSING":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, document)
        # 把固定状态或编码 FAILED（失败）保存到 知识文档中的状态（document.status）。
        document.status = "FAILED"
        # 把从固定文字 'TASK_TIMEOUT: 后台任务超时且超过最大尝试次数'中按表达式 :1000 的结果取出的内容保存到 知识文档中的错误消息（document.error_message）。
        document.error_message = "TASK_TIMEOUT: 后台任务超时且超过最大尝试次数"[:1000]
        # 把stamp保存到 知识文档中的更新时at（document.updated_at）。
        document.updated_at = stamp
        # 把 document.version 加上 固定数值 1，再保存回原变量。
        document.version += 1
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(
            # 继续传入或组合表达式 KnowledgeRevision, document.current_revision_id, with_for_update=True 的结果，完成上面尚未结束的表达式。
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        # 检查条件 revision is not None and revision.status == 'PARSING' 是否成立；成立时执行缩进的分支。
        if revision is not None and revision.status == "PARSING":
            # 把固定状态或编码 FAILED（失败）保存到 修订版本中的状态（revision.status）。
            revision.status = "FAILED"
            # 把知识文档（document）的错误消息（error_message）属性保存到 修订版本中的错误消息（revision.error_message）。
            revision.error_message = document.error_message
            # 把stamp保存到 修订版本中的更新时at（revision.updated_at）。
            revision.updated_at = stamp


# 异步函数 recover_embedding_failed：超时任务最终失败后补齐索引状态；已成功或已有新任务时不覆盖。；接收 Agent 数据库访问器（db）、任务（task）。
async def recover_embedding_failed(db: Database, task: BackgroundTask) -> None:
    # 超时任务最终失败后补齐索引状态；已成功或已有新任务时不覆盖。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 页面是否仍处于活动状态（active）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
        active = await task_repo.find_active(
            # 继续传入或组合表达式 session, kind="DOCUMENT_EMBEDDING", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id 的结果，完成上面尚未结束的表达式。
            session, kind="DOCUMENT_EMBEDDING", ref_type="KNOWLEDGE_DOCUMENT", ref_id=task.ref_id
        )
        # 检查条件 active is not None 是否成立；成立时执行缩进的分支。
        if active is not None:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
        document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
        # 检查document is None or document.status != 'READY'，即对应内容是否不存在；成立时执行缩进的分支。
        if document is None or document.status != "READY":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(
            # 继续传入或组合表达式 KnowledgeRevision, document.current_revision_id, with_for_update=True 的结果，完成上面尚未结束的表达式。
            KnowledgeRevision, document.current_revision_id, with_for_update=True
        )
        # 检查revision is None or revision.index_status != 'PENDING'，即对应内容是否不存在；成立时执行缩进的分支。
        if revision is None or revision.index_status != "PENDING":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把固定状态或编码 FAILED（失败）保存到 修订版本中的索引状态（revision.index_status）。
        revision.index_status = "FAILED"
        # 把固定文字 'TASK_TIMEOUT: 后台任务超时且超过最大尝试次数'保存到 修订版本中的错误消息（revision.error_message）。
        revision.error_message = "TASK_TIMEOUT: 后台任务超时且超过最大尝试次数"
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 修订版本中的更新时at（revision.updated_at）。
        revision.updated_at = await database_time(session, revision)


# 异步函数 process_embedding：一个领取处理全部批次；每批写入前复核文档和任务领取代次。；接收 Agent 数据库访问器（db）、任务（task）。
async def process_embedding(db: Database, task: BackgroundTask) -> None:
    # 一个领取处理全部批次；每批写入前复核文档和任务领取代次。
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 只要条件 True 是否成立，就继续执行下面的循环。
    while True:
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
            document = await repo.find_document(session, task.user_id, task.ref_id)
            # 检查document is None or document.status != 'READY'，即对应内容是否不存在；成立时执行缩进的分支。
            if document is None or document.status != "READY":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待向量化文档不存在")
            # 等待要求满足 claim完成后再继续。
            await task_repo.require_claim(session, task)
            # 把等待执行“调用 repo.chunks_without_embedding 完成文档片段列表without向量处理”得到的结果完成后的结果保存到 文档片段列表（chunks）。
            chunks = await repo.chunks_without_embedding(
                # 继续传入或组合表达式 session, task.user_id, task.ref_id, settings.agent_knowledge_embedding_batch 的结果，完成上面尚未结束的表达式。
                session, task.user_id, task.ref_id, settings.agent_knowledge_embedding_batch
            )
            # 检查条件 not chunks 是否成立；成立时执行缩进的分支。
            if not chunks:
                # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
                revision = await session.get(KnowledgeRevision, document.current_revision_id)
                # 检查条件 revision is not None 是否成立；成立时执行缩进的分支。
                if revision is not None:
                    # 把固定状态或编码 READY（已就绪）保存到 修订版本中的索引状态（revision.index_status）。
                    revision.index_status = "READY"
                    # 把空值，表示当前还没有对应内容保存到 修订版本中的错误消息（revision.error_message）。
                    revision.error_message = None
                    # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 修订版本中的更新时at（revision.updated_at）。
                    revision.updated_at = await database_time(session, revision)
                # 把空值交还给调用方，本函数到此结束。
                return
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 texts。
            texts = [chunk.content for chunk in chunks]
        # 把等待执行“调用 embed_texts 完成embedtexts处理”得到的结果完成后的结果保存到 vectors。
        vectors = await embed_texts(texts)
        # 检查条件 len(vectors) != len(chunks) 是否成立；成立时执行缩进的分支。
        if len(vectors) != len(chunks):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量数量与文本数量不一致")
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 知识文档（document）。
            document = await repo.find_document(session, task.user_id, task.ref_id, for_update=True)
            # 检查document is None or document.status != 'READY'，即对应内容是否不存在；成立时执行缩进的分支。
            if document is None or document.status != "READY":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "待向量化文档不存在")
            # 等待要求满足 claim完成后再继续。
            await task_repo.require_claim(session, task)
            # 从执行“调用 zip 完成zip处理”得到的结果依次取出文档片段向量（(chunk, vector)），每项执行一次下面的处理。
            for chunk, vector in zip(chunks, vectors, strict=True):
                # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
                row = await session.get(KnowledgeChunk, chunk.id)
                # 检查row is None or row.user_id != task.user_id，即对应内容是否不存在；成立时执行缩进的分支。
                if row is None or row.user_id != task.user_id:
                    # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                    continue
                # 检查条件 row.revision_id != document.current_revision_id 是否成立；成立时执行缩进的分支。
                if row.revision_id != document.current_revision_id:
                    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                    raise AgentError(409, "TASK_CLAIM_LOST", "文档已有新版本")
                # 把向量（vector）保存到 当前记录中的向量值（row.vector_value）。
                row.vector_value = vector
                # 把向量（vector）保存到 当前记录中的向量（row.embedding）。
                row.embedding = vector
                # 把配置（settings）的向量模型（embedding_model）属性保存到 当前记录中的向量模型（row.embedding_model）。
                row.embedding_model = settings.embedding_model
                # 把执行“调用 len 完成len处理”得到的结果保存到 当前记录中的向量dimension（row.embedding_dimension）。
                row.embedding_dimension = len(vector)
                # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 当前记录中的更新时at（row.updated_at）。
                row.updated_at = await database_time(session, row)


# 异步函数 process_memory_embedding：调用 process_memory_embedding 完成处理过程长期记忆向量处理；接收 Agent 数据库访问器（db）、任务（task）。
async def process_memory_embedding(db: Database, task: BackgroundTask) -> None:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 长期记忆（memory）。
        memory = await session.get(Memory, task.ref_id)
        # 检查memory is None or memory.user_id != task.user_id or memory.deleted_at is not None，即对应内容是否不存在；成立时执行缩进的分支。
        if memory is None or memory.user_id != task.user_id or memory.deleted_at is not None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "MEMORY_NOT_FOUND", "待向量化记忆不存在")
        # 检查条件 memory.status != 'CONFIRMED' 是否成立；成立时执行缩进的分支。
        if memory.status != "CONFIRMED":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把长期记忆（memory）的内容（content）属性保存到 内容（content）。
        content = memory.content
    # 把等待执行“调用 embed_texts 完成embedtexts处理”得到的结果完成后的结果保存到 vectors。
    vectors = await embed_texts([content])
    # 把从vectors中按固定数值 0取出的内容保存到 向量（vector）。
    vector = vectors[0]
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:

        # 从 sqlalchemy 引入 选择（select）。
        from sqlalchemy import select

        # 从 app.db.models 引入 长期记忆向量（MemoryEmbedding）。
        from app.db.models import MemoryEmbedding

        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 长期记忆（memory）。
        memory = await session.get(Memory, task.ref_id, with_for_update=True)
        # 检查memory is None or memory.user_id != task.user_id or memory.deleted_at is not None or (memory.status …，即对应内容是否不存在；成立时执行缩进的分支。
        if (
            # 继续传入或组合表达式 memory is None 的结果，完成上面尚未结束的表达式。
            memory is None
            # 继续补充本次调用或判断：条件判断的结果：or memory.user_id != task.user_id。
            or memory.user_id != task.user_id
            # 继续补充本次调用或判断：条件判断的结果：or memory.deleted_at is not None。
            or memory.deleted_at is not None
            # 继续补充本次调用或判断：条件判断的结果：or memory.status != "CONFIRMED"。
            or memory.status != "CONFIRMED"
        ):
            # 把空值交还给调用方，本函数到此结束。
            return
        # 等待要求满足 claim完成后再继续。
        await task_repo.require_claim(session, task)
        # 检查条件 memory.content != content 是否成立；成立时执行缩进的分支。
        if memory.content != content:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "TASK_CLAIM_LOST", "记忆已修改，等待重新向量化")
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 existing。
        existing = await session.scalar(
            # 继续传入或组合调用 select(MemoryEmbedding).where 完成where处理，完成上面尚未结束的表达式。
            select(MemoryEmbedding).where(
                # 继续传入或组合条件判断的结果：MemoryEmbedding.memory_id == task.ref_id, MemoryEmbedding.user_id == task.user_id，完成上面尚未结束的表达式。
                MemoryEmbedding.memory_id == task.ref_id, MemoryEmbedding.user_id == task.user_id
            )
        )
        # 检查existing is None，即对应内容是否不存在；成立时执行缩进的分支。
        if existing is None:
            # 加入集合或登记一条待保存记录。
            session.add(
                # 继续传入或组合调用 MemoryEmbedding 完成长期记忆向量处理，完成上面尚未结束的表达式。
                MemoryEmbedding(
                    # 传入长期记忆编号（memory_id），值为表达式 task.ref_id 的结果。
                    memory_id=task.ref_id,
                    # 传入用户编号（user_id），值为表达式 task.user_id 的结果。
                    user_id=task.user_id,
                    # 传入向量模型（embedding_model），值为表达式 settings.embedding_model 的结果。
                    embedding_model=settings.embedding_model,
                    # 传入dimension，值为集合或文字的长度。
                    dimension=len(vector),
                    # 传入向量（embedding），值为向量（vector）。
                    embedding=vector,
                    # 传入向量值（vector_value），值为向量（vector）。
                    vector_value=vector,
                )
            )
        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
        else:
            # 把配置（settings）的向量模型（embedding_model）属性保存到 existing中的向量模型（existing.embedding_model）。
            existing.embedding_model = settings.embedding_model
            # 把执行“调用 len 完成len处理”得到的结果保存到 existing中的dimension（existing.dimension）。
            existing.dimension = len(vector)
            # 把向量（vector）保存到 existing中的向量（existing.embedding）。
            existing.embedding = vector
            # 把向量（vector）保存到 existing中的向量值（existing.vector_value）。
            existing.vector_value = vector
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 existing中的更新时at（existing.updated_at）。
            existing.updated_at = await database_time(session, existing)


# 异步函数 search_chunks：在数据库内按用户、有效版本、模型过滤后计算距离；文本作为可见降级。；接收 Agent 数据库访问器（db）、用户编号（user_id）、查询条件（query）、上限（limit）。
async def search_chunks(db: Database, user_id: int, query: str, limit: int, knowledge_base_ids: list[int] | None = None) -> list[tuple[KnowledgeChunk, float]]:
    if not knowledge_base_ids:
        return []
    from sqlalchemy import select
    settings = get_settings()
    try:
        vector = (await embed_texts([query]))[0]
        async with db.session() as session:
            distance = KnowledgeChunk.vector_value.cosine_distance(vector)
            result = await session.execute(select(KnowledgeChunk, distance.label('distance')).join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id).where(KnowledgeChunk.user_id == user_id, KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id.in_(knowledge_base_ids), KnowledgeDocument.deleted_at.is_(None), KnowledgeDocument.status == 'READY', KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id, KnowledgeChunk.embedding_model == settings.embedding_model, KnowledgeChunk.embedding_dimension == 1024, KnowledgeChunk.vector_value.is_not(None)).order_by(distance, KnowledgeChunk.id).limit(limit))
            rows = [(row, 1.0 - float(score)) for (row, score) in result if score <= 0.8]
            if rows:
                return rows
    except AgentError:
        logger.warning('knowledge_vector_fallback user=%s', user_id)
    terms = [term for term in re.split('[\\s，。、,.;；:：!?！？]+', query) if len(term) >= 2][:6]
    if not terms:
        return []
    async with db.session() as session:
        rows = await repo.keyword_chunks(session, user_id, terms, limit, knowledge_base_ids)
    return [(row, 0.0) for row in rows]
