# 封装知识资料数据的查询与保存，只负责存储访问。
# 知识文档与分块数据访问。

# 从 typing 引入 Any。
from typing import Any

# 从 sqlalchemy 引入 选择（Select）、func、选择（select）。
from sqlalchemy import Select, func, select

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.db.models 引入 知识资料文档片段（KnowledgeChunk）、知识资料知识文档（KnowledgeDocument）。
from app.db.models import KnowledgeChunk, KnowledgeDocument


# 异步函数 page_documents：调用 page_documents 完成当前页知识文档列表处理；接收 当前会话（session）、用户编号（user_id）、状态（status）、keyword、当前页数值（page_number）、每页条数（size）。
async def page_documents(session: AsyncSession, *, user_id: int, status: str | None, keyword: str | None, page_number: int, size: int, knowledge_base_id: int | None = None) -> tuple[list[KnowledgeDocument], int]:
    conditions = [KnowledgeDocument.user_id == user_id, KnowledgeDocument.deleted_at.is_(None)]
    if knowledge_base_id is not None:
        conditions.append(KnowledgeDocument.knowledge_base_id == knowledge_base_id)
    if status:
        conditions.append(KnowledgeDocument.status == status)
    if keyword:
        conditions.append(KnowledgeDocument.title.ilike(f'%{keyword}%'))
    query: Select = select(KnowledgeDocument).where(*conditions)
    total = await session.scalar(select(func.count()).select_from(query.subquery()))
    rows = await session.scalars(query.order_by(KnowledgeDocument.created_at.desc(), KnowledgeDocument.id.desc()).offset((page_number - 1) * size).limit(size))
    return (list(rows), int(total or 0))


# 异步函数 find_document：查找 知识文档（document）；接收 当前会话（session）、用户编号（user_id）、知识文档编号（document_id）、for更新（for_update）。
async def find_document(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, user_id: int, document_id: int, *, for_update: bool = False。
    session: AsyncSession, user_id: int, document_id: int, *, for_update: bool = False
# 继续传入或组合表达式 ) -> KnowledgeDocument | None: 的结果，完成上面尚未结束的表达式。
) -> KnowledgeDocument | None:
    # 把执行“调用 select(KnowledgeDocument).where 完成where处理”得到的结果保存到 查询条件（query）。
    query = select(KnowledgeDocument).where(
        # 继续传入或组合条件判断的结果：KnowledgeDocument.id == document_id，完成上面尚未结束的表达式。
        KnowledgeDocument.id == document_id,
        # 继续传入或组合条件判断的结果：KnowledgeDocument.user_id == user_id，完成上面尚未结束的表达式。
        KnowledgeDocument.user_id == user_id,
        # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
        KnowledgeDocument.deleted_at.is_(None),
    )
    # 检查条件 for_update 是否成立；成立时执行缩进的分支。
    if for_update:
        # 把执行“调用 query.with_for_update 完成withfor更新处理”得到的结果保存到 查询条件（query）。
        query = query.with_for_update()
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(query)


# 异步函数 find_by_checksum：查找 按checksum（by_checksum）；接收 当前会话（session）、用户编号（user_id）、checksum。
async def find_by_checksum(session: AsyncSession, user_id: int, checksum: str, knowledge_base_id: int) -> KnowledgeDocument | None:
    return await session.scalar(select(KnowledgeDocument).where(KnowledgeDocument.user_id == user_id, KnowledgeDocument.checksum == checksum, KnowledgeDocument.knowledge_base_id == knowledge_base_id, KnowledgeDocument.deleted_at.is_(None)))


# 异步函数 create_document：创建 知识文档（document）；接收 当前会话（session）。
async def create_document(session: AsyncSession, **values: Any) -> KnowledgeDocument:
    # 把执行“调用 KnowledgeDocument 完成知识资料知识文档处理”得到的结果保存到 当前记录（row）。
    row = KnowledgeDocument(**values)
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 page_chunks：调用 page_chunks 完成当前页文档片段列表处理；接收 当前会话（session）、用户编号（user_id）、知识文档编号（document_id）、当前页数值（page_number）、每页条数（size）。
async def page_chunks(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写知识文档编号（document_id）字段，值为int。
    document_id: int,
    # 填写当前页数值（page_number）字段，值为int。
    page_number: int,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[KnowledgeChunk], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[KnowledgeChunk], int]:
    # 把依次包含 条件 KnowledgeChunk.user_id == user_id 是否成立、条件 KnowledgeChunk.document_id == document_id 是否成立、条件 KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where(KnowledgeDocument.id == document_id).scalar_subquery() 是否成立 的集合保存到 conditions。
    conditions = [
        # 继续传入或组合条件判断的结果：KnowledgeChunk.user_id == user_id，完成上面尚未结束的表达式。
        KnowledgeChunk.user_id == user_id,
        # 继续传入或组合条件判断的结果：KnowledgeChunk.document_id == document_id，完成上面尚未结束的表达式。
        KnowledgeChunk.document_id == document_id,
        # 继续传入或组合调用 KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where 完成where处理，完成上面尚未结束的表达式。
        KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where(KnowledgeDocument.id == document_id).scalar_subquery(),
    ]
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
    total = await session.scalar(
        # 继续传入或组合调用 select(func.count 完成数量处理，完成上面尚未结束的表达式。
        select(func.count()).select_from(KnowledgeChunk).where(*conditions)
    )
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(KnowledgeChunk)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(*conditions)
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(KnowledgeChunk.chunk_no)
        # 继续补充本次调用或判断：调用 offset 完成分页时跳过的记录数处理。
        .offset((page_number - 1) * size)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(size)
    )
    # 把依次包含 执行“调用 list 完成列表处理”得到的结果、执行“调用 int 完成int处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return list(rows), int(total or 0)


# 异步函数 all_chunks：调用 all_chunks 完成全部文档片段列表处理；接收 当前会话（session）、用户编号（user_id）、知识文档编号（document_id）。
async def all_chunks(session: AsyncSession, user_id: int, document_id: int) -> list[KnowledgeChunk]:
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(KnowledgeChunk)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id)
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(KnowledgeChunk.chunk_no)
    )
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)


# 异步函数 chunks_without_embedding：调用 chunks_without_embedding 完成文档片段列表without向量处理；接收 当前会话（session）、用户编号（user_id）、知识文档编号（document_id）、上限（limit）。
async def chunks_without_embedding(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, document_id: int, limit: int 的结果。
    session: AsyncSession, user_id: int, document_id: int, limit: int
# 继续传入或组合表达式 ) -> list[KnowledgeChunk]: 的结果，完成上面尚未结束的表达式。
) -> list[KnowledgeChunk]:
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(KnowledgeChunk)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：KnowledgeChunk.user_id == user_id，完成上面尚未结束的表达式。
            KnowledgeChunk.user_id == user_id,
            # 继续传入或组合条件判断的结果：KnowledgeChunk.document_id == document_id，完成上面尚未结束的表达式。
            KnowledgeChunk.document_id == document_id,
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            KnowledgeChunk.embedding.is_(None),
            # 继续传入或组合调用 KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where 完成where处理，完成上面尚未结束的表达式。
            KnowledgeChunk.revision_id == select(KnowledgeDocument.current_revision_id).where(KnowledgeDocument.id == document_id).scalar_subquery(),
        )
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(KnowledgeChunk.chunk_no)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(limit)
    )
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)


# 异步函数 embedded_chunks：调用 embedded_chunks 完成embedded文档片段列表处理；接收 当前会话（session）、用户编号（user_id）、上限（limit）、变化后编号（after_id）、模型（model）。
async def embedded_chunks(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, user_id: int, limit: int, *, after_id: int = 0, model: str | None = None。
    session: AsyncSession, user_id: int, limit: int, *, after_id: int = 0, model: str | None = None
# 继续传入或组合表达式 ) -> list[KnowledgeChunk]: 的结果，完成上面尚未结束的表达式。
) -> list[KnowledgeChunk]:
    # 把执行“调用 select(KnowledgeChunk).join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id).where(KnowledgeChunk.user_id == user_id, KnowledgeDocument.user_id == user_id, KnowledgeDocument.deleted_at.is_(None), KnowledgeDocument.status == 'READY', KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id, KnowledgeChunk.embedding.is_not(None), KnowledgeChunk.id > after_id).order_by(KnowledgeChunk.id).limit 完成上限处理”得到的结果保存到 查询条件（query）。
    query = (
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(KnowledgeChunk)
        # 继续补充本次调用或判断：用指定分隔符把多段文字合起来。
        .join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：KnowledgeChunk.user_id == user_id，完成上面尚未结束的表达式。
            KnowledgeChunk.user_id == user_id,
            # 继续传入或组合条件判断的结果：KnowledgeDocument.user_id == user_id，完成上面尚未结束的表达式。
            KnowledgeDocument.user_id == user_id,
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            KnowledgeDocument.deleted_at.is_(None),
            # 继续传入或组合条件判断的结果：KnowledgeDocument.status == "READY"，完成上面尚未结束的表达式。
            KnowledgeDocument.status == "READY",
            # 继续传入或组合条件判断的结果：KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id，完成上面尚未结束的表达式。
            KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id,
            # 继续传入或组合判断是否为 not，完成上面尚未结束的表达式。
            KnowledgeChunk.embedding.is_not(None),
            # 继续传入或组合条件判断的结果：KnowledgeChunk.id > after_id，完成上面尚未结束的表达式。
            KnowledgeChunk.id > after_id,
        )
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(KnowledgeChunk.id)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(limit)
    )
    # 检查条件 model is not None 是否成立；成立时执行缩进的分支。
    if model is not None:
        # 把执行“调用 query.where 完成where处理”得到的结果保存到 查询条件（query）。
        query = query.where(KnowledgeChunk.embedding_model == model)
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(query)
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)


# 异步函数 keyword_chunks：调用 keyword_chunks 完成keyword文档片段列表处理；接收 当前会话（session）、用户编号（user_id）、terms、上限（limit）。
async def keyword_chunks(session: AsyncSession, user_id: int, terms: list[str], limit: int, knowledge_base_ids: list[int]) -> list[KnowledgeChunk]:
    if not knowledge_base_ids:
        return []
    query = select(KnowledgeChunk).join(KnowledgeDocument, KnowledgeDocument.id == KnowledgeChunk.document_id).where(KnowledgeChunk.user_id == user_id, KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id.in_(knowledge_base_ids), KnowledgeDocument.deleted_at.is_(None), KnowledgeDocument.status == 'READY', KnowledgeChunk.revision_id == KnowledgeDocument.current_revision_id)
    for term in terms:
        query = query.where(KnowledgeChunk.content.ilike(f'%{term}%'))
    rows = await session.scalars(query.order_by(KnowledgeChunk.id.desc()).limit(limit))
    return list(rows)


# 异步函数 delete_chunks：删除 文档片段列表（chunks）；接收 当前会话（session）、用户编号（user_id）、知识文档编号（document_id）、修订版本编号（revision_id）。
async def delete_chunks(session: AsyncSession, user_id: int, document_id: int, *, revision_id: int) -> None:

    # 从 sqlalchemy 引入 删除（delete）。
    from sqlalchemy import delete

    # 等待执行传入的数据库语句并取得结果完成后再继续。
    await session.execute(
        # 继续传入或组合调用 delete(KnowledgeChunk).where 完成where处理，完成上面尚未结束的表达式。
        delete(KnowledgeChunk).where(
            # 继续传入或组合条件判断的结果：KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id，完成上面尚未结束的表达式。
            KnowledgeChunk.user_id == user_id, KnowledgeChunk.document_id == document_id,
            # 继续传入或组合条件判断的结果：KnowledgeChunk.revision_id == revision_id，完成上面尚未结束的表达式。
            KnowledgeChunk.revision_id == revision_id,
        )
    )
