# 封装本轮对话数据的查询与保存，只负责存储访问。
# 轮次表的读写；终态和取消都用比较更新，避免旧任务覆盖新结果。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 uuid 引入 UUID。
from uuid import UUID

# 从 sqlalchemy 引入 func、选择（select）、更新（update）。
from sqlalchemy import func, select, update

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.db.models 引入 对话本轮对话（ConversationTurn）。
from app.db.models import ConversationTurn


# 函数 _now：数据库执行时间且不早于本轮已记录的 started_at。 宿主或数据库所在时钟发生回拨时，单纯 CURRENT_TIMESTAMP/clock_timestamp() 可能早于已写入的行时间，触发 ck_agent_turn_time；这里取两者较大值， 保证终态时间仍落在 started_at 之后。
def _now():
    # 数据库执行时间且不早于本轮已记录的 started_at。
    # 
    # 宿主或数据库所在时钟发生回拨时，单纯 CURRENT_TIMESTAMP/clock_timestamp()
    # 可能早于已写入的行时间，触发 ck_agent_turn_time；这里取两者较大值，
    # 保证终态时间仍落在 started_at 之后。
    # 
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), ConversationTurn.started_at)


# 异步函数 find_by_request：查找 按请求（by_request）；接收 当前会话（session）、用户编号（user_id）、请求编号（request_id）。
async def find_by_request(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, request_id: UUID 的结果。
    session: AsyncSession, user_id: int, request_id: UUID
# 继续传入或组合表达式 ) -> ConversationTurn | None: 的结果，完成上面尚未结束的表达式。
) -> ConversationTurn | None:
    # 把执行“调用 (await session.execute(select(ConversationTurn).where(ConversationTurn.user_id == user_id, ConversationTurn.request_id == request_id))).scalar_one_or_none 完成单个查询值oneor空值处理”得到的结果交还给调用方，本函数到此结束。
    return (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select(ConversationTurn).where 完成where处理，完成上面尚未结束的表达式。
            select(ConversationTurn).where(
                # 继续传入或组合条件判断的结果：ConversationTurn.user_id == user_id，完成上面尚未结束的表达式。
                ConversationTurn.user_id == user_id,
                # 继续传入或组合条件判断的结果：ConversationTurn.request_id == request_id，完成上面尚未结束的表达式。
                ConversationTurn.request_id == request_id,
            )
        )
    # 继续传入或组合调用 ).scalar_one_or_none 完成单个查询值oneor空值处理，完成上面尚未结束的表达式。
    ).scalar_one_or_none()


# 异步函数 find_owned：查找 owned；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、本轮对话编号（turn_id）。
async def find_owned(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, conversation_id: int, turn_id: int 的结果。
    session: AsyncSession, user_id: int, conversation_id: int, turn_id: int
# 继续传入或组合表达式 ) -> ConversationTurn | None: 的结果，完成上面尚未结束的表达式。
) -> ConversationTurn | None:
    # 把执行“调用 (await session.execute(select(ConversationTurn).where(ConversationTurn.id == turn_id, ConversationTurn.user_id == user_id, ConversationTurn.conversation_id == conversation_id))).scalar_one_or_none 完成单个查询值oneor空值处理”得到的结果交还给调用方，本函数到此结束。
    return (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select(ConversationTurn).where 完成where处理，完成上面尚未结束的表达式。
            select(ConversationTurn).where(
                # 继续传入或组合条件判断的结果：ConversationTurn.id == turn_id，完成上面尚未结束的表达式。
                ConversationTurn.id == turn_id,
                # 继续传入或组合条件判断的结果：ConversationTurn.user_id == user_id，完成上面尚未结束的表达式。
                ConversationTurn.user_id == user_id,
                # 继续传入或组合条件判断的结果：ConversationTurn.conversation_id == conversation_id，完成上面尚未结束的表达式。
                ConversationTurn.conversation_id == conversation_id,
            )
        )
    # 继续传入或组合调用 ).scalar_one_or_none 完成单个查询值oneor空值处理，完成上面尚未结束的表达式。
    ).scalar_one_or_none()


# 异步函数 find_running：查找 运行中（running）；接收 当前会话（session）、对话编号（conversation_id）。
async def find_running(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, conversation_id: int 的结果。
    session: AsyncSession, conversation_id: int
# 继续传入或组合表达式 ) -> ConversationTurn | None: 的结果，完成上面尚未结束的表达式。
) -> ConversationTurn | None:
    # 把执行“调用 (await session.execute(select(ConversationTurn).where(ConversationTurn.conversation_id == conversation_id, ConversationTurn.status == 'RUNNING'))).scalar_one_or_none 完成单个查询值oneor空值处理”得到的结果交还给调用方，本函数到此结束。
    return (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select(ConversationTurn).where 完成where处理，完成上面尚未结束的表达式。
            select(ConversationTurn).where(
                # 继续传入或组合条件判断的结果：ConversationTurn.conversation_id == conversation_id，完成上面尚未结束的表达式。
                ConversationTurn.conversation_id == conversation_id,
                # 继续传入或组合条件判断的结果：ConversationTurn.status == "RUNNING"，完成上面尚未结束的表达式。
                ConversationTurn.status == "RUNNING",
            )
        )
    # 继续传入或组合调用 ).scalar_one_or_none 完成单个查询值oneor空值处理，完成上面尚未结束的表达式。
    ).scalar_one_or_none()


# 异步函数 create：调用 create 完成创建处理；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、请求编号（request_id）、请求摘要值（request_hash）、截止时间at（deadline_at）。
async def create(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写请求编号（request_id）字段，值为UUID。
    request_id: UUID,
    # 填写请求摘要值（request_hash）字段，值为str。
    request_hash: str,
    # 填写截止时间at（deadline_at）字段，值为日期时间类型（datetime）。
    deadline_at: datetime,
# 继续传入或组合表达式 ) -> ConversationTurn: 的结果，完成上面尚未结束的表达式。
) -> ConversationTurn:
    # 把执行“调用 ConversationTurn 完成对话本轮对话处理”得到的结果保存到 本轮对话（turn）。
    turn = ConversationTurn(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
        conversation_id=conversation_id,
        # 传入请求编号（request_id），值为请求编号（request_id）。
        request_id=request_id,
        # 传入请求摘要值（request_hash），值为请求摘要值（request_hash）。
        request_hash=request_hash,
        # 传入状态（status），值为固定状态或编码 RUNNING（运行中）。
        status="RUNNING",
        # 传入截止时间at（deadline_at），值为截止时间at（deadline_at）。
        deadline_at=deadline_at,
        # 传入版本号（version），值为固定数值 0。
        version=0,
    )
    # 加入集合或登记一条待保存记录。
    session.add(turn)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 等待调用 session.refresh 完成刷新处理完成后再继续。
    await session.refresh(turn)
    # 把本轮对话（turn）交还给调用方，本函数到此结束。
    return turn


# 异步函数 expire_running：调用 expire_running 完成过期运行中处理；接收 当前会话（session）、对话编号（conversation_id）、当前时间（now）。
async def expire_running(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, conversation_id: int, now: datetime 的结果。
    session: AsyncSession, conversation_id: int, now: datetime
# 继续传入或组合表达式 ) -> int: 的结果，完成上面尚未结束的表达式。
) -> int:
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(ConversationTurn)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：ConversationTurn.conversation_id == conversation_id，完成上面尚未结束的表达式。
            ConversationTurn.conversation_id == conversation_id,
            # 继续传入或组合条件判断的结果：ConversationTurn.status == "RUNNING"，完成上面尚未结束的表达式。
            ConversationTurn.status == "RUNNING",
            # 继续传入或组合条件判断的结果：ConversationTurn.deadline_at <= now，完成上面尚未结束的表达式。
            ConversationTurn.deadline_at <= now,
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入状态（status），值为固定状态或编码 FAILED（失败）。
            status="FAILED",
            # 传入错误编码（error_code），值为固定状态或编码 AGENT_TIMEOUT（成长助手超时）。
            error_code="AGENT_TIMEOUT",
            # 传入finish原因（finish_reason），值为空值，表示当前还没有对应内容。
            finish_reason=None,
            # 传入finishedat（finished_at），值为调用 _now 完成当前时间处理。
            finished_at=_now(),
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
            # 传入版本号（version），值为按表达式计算得到的值：ConversationTurn.version + 1。
            version=ConversationTurn.version + 1,
        )
    )
    # 把结果（result）的rowcount属性或者固定数值 0交还给调用方，本函数到此结束。
    return result.rowcount or 0


# 异步函数 finalize：调用 finalize 完成finalize处理；接收 当前会话（session）、本轮对话编号（turn_id）、预计版本号（expected_version）、状态（status）、finish原因（finish_reason）、错误编码（error_code）。
async def finalize(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写预计版本号（expected_version）字段，值为int。
    expected_version: int,
    # 填写状态（status）字段，值为str。
    status: str,
    # 填写finish原因（finish_reason）字段，值为表达式 str | None 的结果。
    finish_reason: str | None,
    # 填写错误编码（error_code）字段，值为表达式 str | None 的结果。
    error_code: str | None,
# 继续传入或组合表达式 ) -> bool: 的结果，完成上面尚未结束的表达式。
) -> bool:
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(ConversationTurn)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：ConversationTurn.id == turn_id，完成上面尚未结束的表达式。
            ConversationTurn.id == turn_id,
            # 继续传入或组合条件判断的结果：ConversationTurn.status == "RUNNING"，完成上面尚未结束的表达式。
            ConversationTurn.status == "RUNNING",
            # 继续传入或组合条件判断的结果：ConversationTurn.version == expected_version，完成上面尚未结束的表达式。
            ConversationTurn.version == expected_version,
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入状态（status），值为状态（status）。
            status=status,
            # 传入finish原因（finish_reason），值为finish原因（finish_reason）。
            finish_reason=finish_reason,
            # 传入错误编码（error_code），值为错误编码（error_code）。
            error_code=error_code,
            # 传入finishedat（finished_at），值为调用 _now 完成当前时间处理。
            finished_at=_now(),
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
            # 传入版本号（version），值为按表达式计算得到的值：ConversationTurn.version + 1。
            version=ConversationTurn.version + 1,
        )
    )
    # 把条件 (result.rowcount or 0) > 0 是否成立交还给调用方，本函数到此结束。
    return (result.rowcount or 0) > 0


# 异步函数 cancel：调用 cancel 完成取消处理；接收 当前会话（session）、本轮对话编号（turn_id）、预计版本号（expected_version）。
async def cancel(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写预计版本号（expected_version）字段，值为int。
    expected_version: int,
# 继续传入或组合表达式 ) -> bool: 的结果，完成上面尚未结束的表达式。
) -> bool:
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(ConversationTurn)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：ConversationTurn.id == turn_id，完成上面尚未结束的表达式。
            ConversationTurn.id == turn_id,
            # 继续传入或组合条件判断的结果：ConversationTurn.status == "RUNNING"，完成上面尚未结束的表达式。
            ConversationTurn.status == "RUNNING",
            # 继续传入或组合条件判断的结果：ConversationTurn.version == expected_version，完成上面尚未结束的表达式。
            ConversationTurn.version == expected_version,
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入状态（status），值为固定状态或编码 CANCELLED（已取消）。
            status="CANCELLED",
            # 传入finish原因（finish_reason），值为空值，表示当前还没有对应内容。
            finish_reason=None,
            # 传入错误编码（error_code），值为固定状态或编码 AGENT_CANCELLED（成长助手已取消）。
            error_code="AGENT_CANCELLED",
            # 传入finishedat（finished_at），值为调用 _now 完成当前时间处理。
            finished_at=_now(),
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
            # 传入版本号（version），值为按表达式计算得到的值：ConversationTurn.version + 1。
            version=ConversationTurn.version + 1,
        )
    )
    # 把条件 (result.rowcount or 0) > 0 是否成立交还给调用方，本函数到此结束。
    return (result.rowcount or 0) > 0


# 异步函数 count_running：调用 count_running 完成数量运行中处理；接收 当前会话（session）、对话编号（conversation_id）。
async def count_running(session: AsyncSession, conversation_id: int) -> int:
    # 把执行“调用 int 完成int处理”得到的结果交还给调用方，本函数到此结束。
    return int(
        (
            # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
            await session.execute(
                # 继续传入或组合调用 select(func.count 完成数量处理，完成上面尚未结束的表达式。
                select(func.count())
                # 继续补充本次调用或判断：调用 select_from 完成选择来源处理。
                .select_from(ConversationTurn)
                # 继续补充本次调用或判断：调用 where 完成where处理。
                .where(
                    # 继续传入或组合条件判断的结果：ConversationTurn.conversation_id == conversation_id，完成上面尚未结束的表达式。
                    ConversationTurn.conversation_id == conversation_id,
                    # 继续传入或组合条件判断的结果：ConversationTurn.status == "RUNNING"，完成上面尚未结束的表达式。
                    ConversationTurn.status == "RUNNING",
                )
            )
        # 继续传入或组合调用 ).scalar_one 完成单个查询值one处理，完成上面尚未结束的表达式。
        ).scalar_one()
    )
