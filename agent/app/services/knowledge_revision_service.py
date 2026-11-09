# 保存不可变文档版本与片段，防止旧任务覆盖新版本。
# 文档历史和重新处理；用户与文档范围在每次读取、修改时核对。

# 引入 hashlib，供下面调用其功能。
import hashlib

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 从 uuid 引入 uuid4。
from uuid import uuid4

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 app.core.config 引入 读取运行配置（get_settings）。
# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 知识资料文档片段（KnowledgeChunk）。
from app.db.models import KnowledgeChunk

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 从 app.db.workspace_models 引入 知识资料修订版本（KnowledgeRevision）。
from app.db.workspace_models import KnowledgeRevision

# 从 app.repositories 引入 任务列表（tasks）。
from app.repositories import background_task_repository as tasks

# 从 app.repositories 引入 知识文档列表（documents）。
from app.repositories import knowledge_repository as documents
from app.services import knowledge_base_service as bases
from app.services import knowledge_formats as formats

# 从 app.services.knowledge_service 引入 ALLOWEDTYPES（_ALLOWED_TYPES）、clean名称（_clean_name）、存储根（_storage_root）。
from app.services.knowledge_service import _clean_name, _storage_root


# 函数 revision_vo：调用 revision_vo 完成修订版本处理；接收 当前记录（row）。
def revision_vo(row: KnowledgeRevision) -> dict:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id), "revisionNo": row.revision_no, "fileName": row.file_name,
        # 填写每页条数Bytes（sizeBytes）字段，值为表达式 row.size_bytes, "status": row.status, "indexStatus": row.index_status 的结果。
        "sizeBytes": row.size_bytes, "status": row.status, "indexStatus": row.index_status,
        # 填写文档片段数量（chunkCount）字段，值为表达式 row.chunk_count, "errorMessage": row.error_message 的结果。
        "chunkCount": row.chunk_count, "errorMessage": row.error_message,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at, "updatedAt": row.updated_at 的结果。
        "createdAt": row.created_at, "updatedAt": row.updated_at,
    }


# 异步函数 revisions：调用 revisions 完成修订记录处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）。
async def revisions(db: Database, user_id: int, document_id: int) -> list[dict]:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 检查await documents.find_document(session, user_id, document_id) is None，即对应内容是否不存在；成立时执行缩进的分支。
        if await documents.find_document(session, user_id, document_id) is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "文档不存在或已删除")
        # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
        rows = await session.scalars(select(KnowledgeRevision).where(
            # 继续传入或组合条件判断的结果：KnowledgeRevision.user_id == user_id, KnowledgeRevision.document_id == document_id，完成上面尚未结束的表达式。
            KnowledgeRevision.user_id == user_id, KnowledgeRevision.document_id == document_id,
        # 继续传入或组合调用 ).order_by 完成顺序按处理，完成上面尚未结束的表达式。
        ).order_by(KnowledgeRevision.revision_no.desc()))
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
        return [revision_vo(row) for row in rows]


# 异步函数 replace：生成保留原对象其余字段的新副本，只替换明确指定的值；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、版本号（version）、文件名称（file_name）、mime类型（mime_type）、数据（data）。
async def replace(db: Database, user_id: int, document_id: int, *, version: int, file_name: str, mime_type: str, data: bytes):
    name = _clean_name(file_name, limit=255)
    mime = formats.validate_file(name, mime_type, data)
    checksum = hashlib.sha256(data).hexdigest()
    async with db.session() as session:
        await bases.lock_user(session, user_id)
        doc = await documents.find_document(session, user_id, document_id, for_update=True)
        if doc is None:
            raise AgentError(404, 'KNOWLEDGE_NOT_FOUND', '文档不存在或已删除')
        await bases.enrich_documents(session, [doc])
        if doc.version != version:
            raise AgentError(409, 'KNOWLEDGE_VERSION_CONFLICT', '文档已更新，请刷新后重试')
        if doc.checksum == checksum:
            return doc
        duplicate = await documents.find_by_checksum(session, user_id, checksum, doc.knowledge_base_id)
        if duplicate is not None and duplicate.id != document_id:
            raise AgentError(409, 'KNOWLEDGE_DUPLICATE', '此文件已经存在于另一篇文档')
        for kind in ('DOCUMENT_PARSE', 'DOCUMENT_EMBEDDING'):
            await tasks.cancel_for_ref(session, kind=kind, ref_type='KNOWLEDGE_DOCUMENT', ref_id=doc.id)
        number = int(await session.scalar(select(func.max(KnowledgeRevision.revision_no)).where(KnowledgeRevision.document_id == document_id)) or 0) + 1
        key = f'{user_id}/{uuid4().hex}{Path(name).suffix.lower()}'
        target = _storage_root() / key
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        row = KnowledgeRevision(user_id=user_id, document_id=doc.id, revision_no=number, file_name=name, mime_type=mime, size_bytes=len(data), checksum=checksum, storage_key=key, status='UPLOADED', index_status='PENDING', chunk_count=0)
        session.add(row)
        await session.flush()
        doc.current_revision_id = row.id
        (doc.file_name, doc.mime_type, doc.size_bytes) = (name, mime, len(data))
        (doc.checksum, doc.storage_key) = (checksum, key)
        (doc.status, doc.chunk_count, doc.error_message) = ('UPLOADED', 0, None)
        doc.updated_at = await database_time(session, doc)
        doc.version += 1
        await tasks.enqueue(session, user_id=user_id, kind='DOCUMENT_PARSE', ref_type='KNOWLEDGE_DOCUMENT', ref_id=doc.id)
        doc.index_status = "PENDING"
        return doc


# 异步函数 retry：调用 retry 完成重试处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、版本号（version）。
async def retry(db: Database, user_id: int, document_id: int, version: int) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 知识文档（document）”得到的结果完成后的结果保存到 doc。
        doc = await documents.find_document(session, user_id, document_id, for_update=True)
        # 检查doc is None，即对应内容是否不存在；成立时执行缩进的分支。
        if doc is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "文档不存在或已删除")
        # 检查当前记录版本是否符合请求要求：doc.version != version；成立时执行缩进的分支。
        if doc.version != version:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "KNOWLEDGE_VERSION_CONFLICT", "文档已更新，请刷新后重试")
        # 把条件 doc.status == 'READY' 是否成立时使用固定状态或编码 DOCUMENT_EMBEDDING（知识文档向量），否则使用固定状态或编码 DOCUMENT_PARSE（知识文档解析）保存到 种类（kind）。
        kind = "DOCUMENT_EMBEDDING" if doc.status == "READY" else "DOCUMENT_PARSE"
        # 检查条件 doc.status in {'UPLOADED', 'PARSING'} 是否成立；成立时执行缩进的分支。
        if doc.status in {"UPLOADED", "PARSING"}:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "KNOWLEDGE_PROCESSING", "文档正在处理，请稍后刷新")
        # 检查条件 await tasks.find_active(session, kind=kind, ref_type='KNOWLEDGE_DOCUMENT', ref_id=document_id) is not None 是否成立；成立时执行缩进的分支。
        if await tasks.find_active(
            # 继续传入或组合表达式 session, kind=kind, ref_type="KNOWLEDGE_DOCUMENT", ref_id=document_id 的结果，完成上面尚未结束的表达式。
            session, kind=kind, ref_type="KNOWLEDGE_DOCUMENT", ref_id=document_id
        # 继续传入或组合条件判断的结果：) is not None:，完成上面尚未结束的表达式。
        ) is not None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "KNOWLEDGE_PROCESSING", "文档正在处理，请稍后刷新")
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(KnowledgeRevision, doc.current_revision_id)
        # 检查条件 kind == 'DOCUMENT_EMBEDDING' and revision is not None and (revision.index_status == 'READY') 是否成立；成立时执行缩进的分支。
        if kind == "DOCUMENT_EMBEDDING" and revision is not None and revision.index_status == "READY":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "KNOWLEDGE_ALREADY_READY", "文档已完成解析与索引，无需重试")
        # 检查条件 revision 是否成立；成立时执行缩进的分支。
        if revision:
            # 把空值，表示当前还没有对应内容保存到 修订版本中的错误消息（revision.error_message）。
            revision.error_message = None
            # 把固定状态或编码 PENDING（等待处理）保存到 修订版本中的索引状态（revision.index_status）。
            revision.index_status = "PENDING"
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 修订版本中的更新时at（revision.updated_at）。
            revision.updated_at = await database_time(session, revision)
        # 检查条件 kind == 'DOCUMENT_PARSE' 是否成立；成立时执行缩进的分支。
        if kind == "DOCUMENT_PARSE":
            # 把依次包含 固定状态或编码 UPLOADED（UPLOADED）、空值，表示当前还没有对应内容 的集合保存到 doc状态doc错误消息（(doc.status, doc.error_message)）。
            doc.status, doc.error_message = "UPLOADED", None
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 doc中的更新时at（doc.updated_at）。
        doc.updated_at = await database_time(session, doc)
        # 把 doc.version 加上 固定数值 1，再保存回原变量。
        doc.version += 1
        # 等待调用 tasks.enqueue 完成enqueue处理完成后再继续。
        await tasks.enqueue(session, user_id=user_id, kind=kind,
            # 传入响应式引用类型（ref_type），值为表达式 "KNOWLEDGE_DOCUMENT", ref_id=doc.id) 的结果。
            ref_type="KNOWLEDGE_DOCUMENT", ref_id=doc.id)


# 异步函数 source：调用 source 完成来源处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、知识文档编号（document_id）、修订版本编号（revision_id）、文档片段编号（chunk_id）。
async def source(db: Database, user_id: int, document_id: int, revision_id: int, chunk_id: int) -> dict:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 检查await documents.find_document(session, user_id, document_id) is None，即对应内容是否不存在；成立时执行缩进的分支。
        if await documents.find_document(session, user_id, document_id) is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_NOT_FOUND", "引用文档已删除或无权访问")
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 当前记录（row）。
        row = await session.scalar(select(KnowledgeChunk).where(
            # 继续传入或组合条件判断的结果：KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id，完成上面尚未结束的表达式。
            KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id,
            # 继续传入或组合条件判断的结果：KnowledgeChunk.revision_id == revision_id, KnowledgeChunk.id == chunk_id))，完成上面尚未结束的表达式。
            KnowledgeChunk.revision_id == revision_id, KnowledgeChunk.id == chunk_id))
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 修订版本（revision）。
        revision = await session.get(KnowledgeRevision, revision_id)
        # 检查row is None or revision is None or revision.user_id != user_id，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None or revision is None or revision.user_id != user_id:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "KNOWLEDGE_SOURCE_NOT_FOUND", "未找到该版本引用")
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"id": str(row.id), "revision": revision_vo(revision), "content": row.content,
                # 填写当前页数值（pageNumber）字段，值为表达式 row.page_number, "heading": row.heading, "chunkNo": row.chunk_no} 的结果。
                "pageNumber": row.page_number, "heading": row.heading, "chunkNo": row.chunk_no}
