# 封装对话数据的查询与保存，只负责存储访问。
# 会话表的读写；所有查询都带所属用户，删除一律按不存在处理。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 sqlalchemy 引入 func、选择（select）、更新（update）。
from sqlalchemy import func, select, update

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 对话（Conversation）。
from app.db.models import Conversation


# 函数 _now：数据库执行时间且不早于会话 created_at，避免宿主时钟回拨破坏时间 CHECK。
def _now():
    # 数据库执行时间且不早于会话 created_at，避免宿主时钟回拨破坏时间 CHECK。
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


# 函数 _like_pattern：调用 _like_pattern 完成like匹配规则处理；接收 keyword。
def _like_pattern(keyword: str) -> str:
    # 把执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果保存到 escaped。
    escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    # 把在文字占位处填入变量后组成的字符串交还给调用方，本函数到此结束。
    return f"%{escaped}%"


# 异步函数 find_owned：查找 owned；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、for更新（for_update）。
async def find_owned(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写for更新（for_update）字段，值为表达式 bool = False 的结果。
    for_update: bool = False,
# 继续传入或组合表达式 ) -> Conversation | None: 的结果，完成上面尚未结束的表达式。
) -> Conversation | None:
    # 把执行“调用 select(Conversation).where 完成where处理”得到的结果保存到 statement。
    statement = select(Conversation).where(
        # 继续传入或组合条件判断的结果：Conversation.id == conversation_id，完成上面尚未结束的表达式。
        Conversation.id == conversation_id,
        # 继续传入或组合条件判断的结果：Conversation.user_id == user_id，完成上面尚未结束的表达式。
        Conversation.user_id == user_id,
        # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
        Conversation.deleted_at.is_(None),
    )
    # 检查条件 for_update 是否成立；成立时执行缩进的分支。
    if for_update:
        # 把执行“调用 statement.with_for_update 完成withfor更新处理”得到的结果保存到 statement。
        statement = statement.with_for_update()
    # 把执行“调用 (await session.execute(statement)).scalar_one_or_none 完成单个查询值oneor空值处理”得到的结果交还给调用方，本函数到此结束。
    return (await session.execute(statement)).scalar_one_or_none()


# 异步函数 require_owned：要求满足 owned；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、for更新（for_update）。
async def require_owned(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写for更新（for_update）字段，值为表达式 bool = False 的结果。
    for_update: bool = False,
# 继续传入或组合表达式 ) -> Conversation: 的结果，完成上面尚未结束的表达式。
) -> Conversation:
    # 把等待执行“查找 owned”得到的结果完成后的结果保存到 对话（conversation）。
    conversation = await find_owned(
        # 继续传入或组合表达式 session, user_id, conversation_id, for_update=for_update 的结果，完成上面尚未结束的表达式。
        session, user_id, conversation_id, for_update=for_update
    )
    # 检查conversation is None，即对应内容是否不存在；成立时执行缩进的分支。
    if conversation is None:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(404, "CONVERSATION_NOT_FOUND", "会话不存在或不可访问")
    # 把对话（conversation）交还给调用方，本函数到此结束。
    return conversation


# 异步函数 page：调用 page 完成当前页处理；接收 当前会话（session）、用户编号（user_id）、状态（status）、keyword、当前页数值（page_number）、每页条数（size）。
async def page(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写状态（status）字段，值为表达式 str | None 的结果。
    status: str | None,
    # 填写keyword字段，值为表达式 str | None 的结果。
    keyword: str | None,
    # 填写当前页数值（page_number）字段，值为int。
    page_number: int,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[Conversation], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[Conversation], int]:
    # 把依次包含 条件 Conversation.user_id == user_id 是否成立、执行“判断是否为 ”得到的结果 的集合保存到 conditions。
    conditions = [Conversation.user_id == user_id, Conversation.deleted_at.is_(None)]
    # 检查条件 status 是否成立；成立时执行缩进的分支。
    if status:
        # 把新条目放到列表末尾。
        conditions.append(Conversation.status == status)
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把新条目放到列表末尾。
        conditions.append(Conversation.title.ilike(_like_pattern(keyword), escape="\\"))
    # 把执行“调用 (await session.execute(select(func.count()).select_from(Conversation).where(*conditions))).scalar_one 完成单个查询值one处理”得到的结果保存到 总数（total）。
    total = (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select(func.count 完成数量处理，完成上面尚未结束的表达式。
            select(func.count()).select_from(Conversation).where(*conditions)
        )
    # 继续传入或组合调用 ).scalar_one 完成单个查询值one处理，完成上面尚未结束的表达式。
    ).scalar_one()
    # 把执行“调用 (await session.execute(select(Conversation).where(*conditions).order_by(Conversation.updated_at.desc(), Conversation.id.desc()).offset((page_number - 1) * size).limit(size))).scalars().all 完成全部处理”得到的结果保存到 记录列表（rows）。
    rows = (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
            select(Conversation)
            # 继续补充本次调用或判断：调用 where 完成where处理。
            .where(*conditions)
            # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
            .order_by(Conversation.updated_at.desc(), Conversation.id.desc())
            # 继续补充本次调用或判断：调用 offset 完成分页时跳过的记录数处理。
            .offset((page_number - 1) * size)
            # 继续补充本次调用或判断：调用 limit 完成上限处理。
            .limit(size)
        )
    # 继续传入或组合取得查询结果的第一列对象，完成上面尚未结束的表达式。
    ).scalars().all()
    # 把依次包含 执行“调用 list 完成列表处理”得到的结果、执行“调用 int 完成int处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return list(rows), int(total)


# 异步函数 create：调用 create 完成创建处理；接收 当前会话（session）、用户编号（user_id）、标题（title）。
async def create(session: AsyncSession, user_id: int, title: str) -> Conversation:
    # 把执行“调用 Conversation 完成对话处理”得到的结果保存到 对话（conversation）。
    conversation = Conversation(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入标题（title），值为标题（title）。
        title=title,
        # 传入状态（status），值为固定状态或编码 ACTIVE（页面是否仍处于活动状态）。
        status="ACTIVE",
        # 传入上下文汇总（context_summary），值为固定文字 ''。
        context_summary="",
        # 传入汇总through消息no（summary_through_message_no），值为固定数值 0。
        summary_through_message_no=0,
        # 传入下一个消息no（next_message_no），值为固定数值 1。
        next_message_no=1,
        # 传入版本号（version），值为固定数值 0。
        version=0,
    )
    # 加入集合或登记一条待保存记录。
    session.add(conversation)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 等待调用 session.refresh 完成刷新处理完成后再继续。
    await session.refresh(conversation)
    # 把对话（conversation）交还给调用方，本函数到此结束。
    return conversation


# 函数 touch：调用 touch 完成touch处理；接收 对话（conversation）、当前时间（now）。
def touch(conversation: Conversation, now: datetime) -> None:
    # 把当前时间（now）保存到 对话中的更新时at（conversation.updated_at）。
    conversation.updated_at = now
    # 把 conversation.version 加上 固定数值 1，再保存回原变量。
    conversation.version += 1


# 异步函数 update_summary：摘要按版本和原覆盖边界比较更新，过期摘要不覆盖新内容。；接收 当前会话（session）、对话编号（conversation_id）、用户编号（user_id）、预计版本号（expected_version）、预计boundary（expected_boundary）、汇总（summary）、boundary。
async def update_summary(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写预计版本号（expected_version）字段，值为int。
    expected_version: int,
    # 填写预计boundary（expected_boundary）字段，值为int。
    expected_boundary: int,
    # 填写汇总（summary）字段，值为str。
    summary: str,
    # 填写boundary字段，值为int。
    boundary: int,
# 继续传入或组合表达式 ) -> bool: 的结果，完成上面尚未结束的表达式。
) -> bool:
    # 摘要按版本和原覆盖边界比较更新，过期摘要不覆盖新内容。
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(Conversation)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：Conversation.id == conversation_id，完成上面尚未结束的表达式。
            Conversation.id == conversation_id,
            # 继续传入或组合条件判断的结果：Conversation.user_id == user_id，完成上面尚未结束的表达式。
            Conversation.user_id == user_id,
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            Conversation.deleted_at.is_(None),
            # 继续传入或组合条件判断的结果：Conversation.version == expected_version，完成上面尚未结束的表达式。
            Conversation.version == expected_version,
            # 继续传入或组合条件判断的结果：Conversation.summary_through_message_no == expected_boundary，完成上面尚未结束的表达式。
            Conversation.summary_through_message_no == expected_boundary,
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入上下文汇总（context_summary），值为汇总（summary）。
            context_summary=summary,
            # 传入汇总through消息no（summary_through_message_no），值为boundary。
            summary_through_message_no=boundary,
            # 传入汇总更新时at（summary_updated_at），值为调用 _now 完成当前时间处理。
            summary_updated_at=_now(),
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
            # 传入版本号（version），值为按表达式计算得到的值：Conversation.version + 1。
            version=Conversation.version + 1,
        )
    )
    # 把条件 (result.rowcount or 0) > 0 是否成立交还给调用方，本函数到此结束。
    return (result.rowcount or 0) > 0
