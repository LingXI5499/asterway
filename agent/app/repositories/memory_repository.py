# 封装长期记忆数据的查询与保存，只负责存储访问。
# 长期记忆数据访问；只使用已确认的 agent.memory 结构。

# 从 sqlalchemy 引入 选择（Select）、func、选择（select）。
from sqlalchemy import Select, func, select

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.db.models 引入 长期记忆（Memory）。
from app.db.models import Memory


# 异步函数 page：调用 page 完成当前页处理；接收 当前会话（session）、用户编号（user_id）、状态（status）、种类（kind）、keyword、当前页数值（page_number）、每页条数（size）。
async def page(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写状态（status）字段，值为表达式 str | None 的结果。
    status: str | None,
    # 填写种类（kind）字段，值为表达式 str | None 的结果。
    kind: str | None,
    # 填写keyword字段，值为表达式 str | None 的结果。
    keyword: str | None,
    # 填写当前页数值（page_number）字段，值为int。
    page_number: int,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[Memory], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[Memory], int]:
    # 把依次包含 条件 Memory.user_id == user_id 是否成立、执行“判断是否为 ”得到的结果 的集合保存到 conditions。
    conditions = [Memory.user_id == user_id, Memory.deleted_at.is_(None)]
    # 检查条件 status 是否成立；成立时执行缩进的分支。
    if status:
        # 把新条目放到列表末尾。
        conditions.append(Memory.status == status)
    # 检查条件 kind 是否成立；成立时执行缩进的分支。
    if kind:
        # 把新条目放到列表末尾。
        conditions.append(Memory.kind == kind)
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把新条目放到列表末尾。
        conditions.append(Memory.content.ilike(f"%{keyword}%"))
    # 声明 查询条件（query），类型为 Select；其内容为执行“调用 select(Memory).where 完成where处理”得到的结果。
    query: Select = select(Memory).where(*conditions)
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
    total = await session.scalar(select(func.count()).select_from(query.subquery()))
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 query.order_by 完成顺序按处理，完成上面尚未结束的表达式。
        query.order_by(Memory.updated_at.desc(), Memory.id.desc())
        # 继续补充本次调用或判断：调用 offset 完成分页时跳过的记录数处理。
        .offset((page_number - 1) * size)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(size)
    )
    # 把依次包含 执行“调用 list 完成列表处理”得到的结果、执行“调用 int 完成int处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return list(rows), int(total or 0)


# 异步函数 find_owned：查找 owned；接收 当前会话（session）、用户编号（user_id）、长期记忆编号（memory_id）、for更新（for_update）。
async def find_owned(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, user_id: int, memory_id: int, *, for_update: bool = False。
    session: AsyncSession, user_id: int, memory_id: int, *, for_update: bool = False
# 继续传入或组合表达式 ) -> Memory | None: 的结果，完成上面尚未结束的表达式。
) -> Memory | None:
    # 把执行“调用 select(Memory).where 完成where处理”得到的结果保存到 查询条件（query）。
    query = select(Memory).where(
        # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
        Memory.id == memory_id, Memory.user_id == user_id, Memory.deleted_at.is_(None)
    )
    # 检查条件 for_update 是否成立；成立时执行缩进的分支。
    if for_update:
        # 把执行“调用 query.with_for_update 完成withfor更新处理”得到的结果保存到 查询条件（query）。
        query = query.with_for_update()
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(query)


# 异步函数 find_same_content：查找 same内容（same_content）；接收 当前会话（session）、用户编号（user_id）、内容（content）、statuses。
async def find_same_content(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, content: str, statuses: tuple[str, ...] 的结果。
    session: AsyncSession, user_id: int, content: str, statuses: tuple[str, ...]
# 继续传入或组合表达式 ) -> Memory | None: 的结果，完成上面尚未结束的表达式。
) -> Memory | None:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(Memory)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：Memory.user_id == user_id，完成上面尚未结束的表达式。
            Memory.user_id == user_id,
            # 继续传入或组合条件判断的结果：Memory.content == content，完成上面尚未结束的表达式。
            Memory.content == content,
            # 继续传入或组合调用 Memory.status.in_ 完成in处理，完成上面尚未结束的表达式。
            Memory.status.in_(statuses),
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            Memory.deleted_at.is_(None),
        )
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(Memory.id)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(1)
    )


# 异步函数 create：调用 create 完成创建处理；接收 当前会话（session）、用户编号（user_id）、种类（kind）、内容（content）、来源（source）、confidence、来源对话编号（source_conversation_id）、来源本轮对话编号（source_turn_id）、supersedes编号（supersedes_id）。
async def create(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写种类（kind）字段，值为str。
    kind: str,
    # 填写内容（content）字段，值为str。
    content: str,
    # 填写来源（source）字段，值为str。
    source: str,
    # 填写confidence字段，值为浮点小数（float）。
    confidence: float,
    # 填写来源对话编号（source_conversation_id）字段，值为表达式 int | None 的结果。
    source_conversation_id: int | None,
    # 填写来源本轮对话编号（source_turn_id）字段，值为表达式 int | None 的结果。
    source_turn_id: int | None,
    # 填写supersedes编号（supersedes_id）字段，值为表达式 int | None 的结果。
    supersedes_id: int | None,
# 继续传入或组合表达式 ) -> Memory: 的结果，完成上面尚未结束的表达式。
) -> Memory:
    # 把执行“调用 Memory 完成长期记忆处理”得到的结果保存到 当前记录（row）。
    row = Memory(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入种类（kind），值为种类（kind）。
        kind=kind,
        # 传入内容（content），值为内容（content）。
        content=content,
        # 传入来源（source），值为来源（source）。
        source=source,
        # 传入confidence，值为confidence。
        confidence=confidence,
        # 传入状态（status），值为固定状态或编码 CANDIDATE（候选）。
        status="CANDIDATE",
        # 传入来源对话编号（source_conversation_id），值为来源对话编号（source_conversation_id）。
        source_conversation_id=source_conversation_id,
        # 传入来源本轮对话编号（source_turn_id），值为来源本轮对话编号（source_turn_id）。
        source_turn_id=source_turn_id,
        # 传入supersedes编号（supersedes_id），值为supersedes编号（supersedes_id）。
        supersedes_id=supersedes_id,
    )
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 confirmed_for_context：确认 edfor上下文（ed_for_context）；接收 当前会话（session）、用户编号（user_id）、上限（limit）。
async def confirmed_for_context(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, limit: int 的结果。
    session: AsyncSession, user_id: int, limit: int
# 继续传入或组合表达式 ) -> list[Memory]: 的结果，完成上面尚未结束的表达式。
) -> list[Memory]:
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(Memory)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：Memory.user_id == user_id，完成上面尚未结束的表达式。
            Memory.user_id == user_id,
            # 继续传入或组合条件判断的结果：Memory.status == "CONFIRMED"，完成上面尚未结束的表达式。
            Memory.status == "CONFIRMED",
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            Memory.deleted_at.is_(None),
        )
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(Memory.updated_at.desc(), Memory.id.desc())
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(limit)
    )
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)
