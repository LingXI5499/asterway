# 从当前用户的已确认记忆和知识文档中检索相关片段。
# 知识检索与来源引用：向量优先、关键词降级，结果只作为背景资料注入。

# 引入 logging，供下面调用其功能。
import logging

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 sqlalchemy 引入 选择（select）。
from sqlalchemy import select

# 从 sqlalchemy.exc 引入 SQLAlchemy错误（SQLAlchemyError）。
from sqlalchemy.exc import SQLAlchemyError

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings
from app.db.knowledge_models import KnowledgeBase

# 从 app.db.models 引入 知识资料知识文档（KnowledgeDocument）。
from app.db.models import KnowledgeDocument

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.services 引入 知识资料业务服务（knowledge_service）。
from app.services import knowledge_service

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 RetrievedChunk，把Retrieved文档片段需要的数据与操作放在同一个对象中。
class RetrievedChunk:
    # 声明 知识文档编号（document_id），类型为 int。
    document_id: int
    # 声明 修订版本编号（revision_id），类型为 int。
    revision_id: int
    # 声明 文档片段编号（chunk_id），类型为 int。
    chunk_id: int
    # 声明 当前页数值（page_number），类型为 int | None。
    page_number: int | None
    # 声明 heading，类型为 str | None。
    heading: str | None
    # 声明 标题（title），类型为 str。
    title: str
    # 声明 文档片段no（chunk_no），类型为 int。
    chunk_no: int
    # 声明 内容（content），类型为 str。
    content: str
    # 声明 score，类型为 float。
    score: float

    knowledge_base_id: int | None = None
    knowledge_base_name: str = ""


# 异步函数 retrieve：调用 retrieve 完成retrieve处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、查询条件（query）。
async def retrieve(db: Database, user_id: int, query: str, knowledge_base_ids: list[int] | None = None) -> list[RetrievedChunk]:
    if not knowledge_base_ids:
        return []
    settings = get_settings()
    try:
        scored = await knowledge_service.search_chunks(db, user_id, query, settings.agent_retrieval_max_chunks, knowledge_base_ids)
    except SQLAlchemyError:
        logger.warning("agent_retrieval_unavailable user=%s", user_id)
        return []
    if not scored:
        return []
    document_ids = list({chunk.document_id for chunk, _ in scored})
    async with db.session() as session:
        rows = await session.execute(select(KnowledgeDocument, KnowledgeBase.name).join(KnowledgeBase,
            KnowledgeBase.id == KnowledgeDocument.knowledge_base_id).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.id.in_(document_ids),
            KnowledgeDocument.knowledge_base_id.in_(knowledge_base_ids), KnowledgeDocument.deleted_at.is_(None),
            KnowledgeDocument.status == "READY", KnowledgeBase.user_id == user_id, KnowledgeBase.deleted_at.is_(None)))
        documents = {row.id: (row.title, row.knowledge_base_id, base_name) for row, base_name in rows}
    return [RetrievedChunk(document_id=chunk.document_id, revision_id=chunk.revision_id, chunk_id=chunk.id,
        page_number=chunk.page_number, heading=chunk.heading, title=documents[chunk.document_id][0],
        chunk_no=chunk.chunk_no, content=chunk.content, score=score,
        knowledge_base_id=documents[chunk.document_id][1], knowledge_base_name=documents[chunk.document_id][2])
        for chunk, score in scored if chunk.content.strip() and chunk.document_id in documents]



# 异步函数 context_block：调用 context_block 完成上下文block处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、查询条件（query）、最大chars（max_chars）、附加说明（metadata）。
async def context_block(db: Database, user_id: int, query: str, *, max_chars: int | None=None, metadata: dict | None=None, knowledge_base_ids: list[int] | None=None) -> str | None:
    if not query.strip():
        return None
    settings = get_settings()
    budget = max_chars if max_chars is not None else settings.agent_retrieval_max_chars
    chunks = await retrieve(db, user_id, query, knowledge_base_ids)
    if not chunks:
        return None
    lines: list[str] = []
    used = 0
    for (index, chunk) in enumerate(chunks, start=1):
        line = f'[资料{index}｜来源：{chunk.title}｜第{chunk.chunk_no + 1}段]\n{chunk.content.strip()}'
        if used + len(line) + 2 > budget:
            break
        lines.append(line)
        if metadata is not None:
            metadata.setdefault('citations', []).append({'label': f'资料{index}', 'documentId': str(chunk.document_id), 'knowledgeBaseId': str(chunk.knowledge_base_id), 'knowledgeBaseName': chunk.knowledge_base_name, 'revisionId': str(chunk.revision_id), 'chunkId': str(chunk.chunk_id), 'title': chunk.title, 'pageNumber': chunk.page_number, 'heading': chunk.heading, 'chunkNo': chunk.chunk_no, 'excerpt': chunk.content[:400]})
        used += len(line) + 2
    if not lines:
        return None
    return '\n\n'.join(lines)
