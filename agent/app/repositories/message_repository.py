# 封装消息数据的查询与保存，只负责存储访问。
# 消息表读写；序号由会话行分配，历史按 message_no 游标而不是时间戳。

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 typing 引入 Any。
from typing import Any

# 从 sqlalchemy 引入 and（and_）、func、or（or_）、选择（select）、更新（update）。
from sqlalchemy import and_, func, or_, select, update

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 对话（Conversation）、对话消息（ConversationMessage）、对话本轮对话（ConversationTurn）。
from app.db.models import Conversation, ConversationMessage, ConversationTurn


# 函数 _now：数据库执行时间且不早于消息 created_at，避免宿主时钟回拨破坏时间 CHECK。
def _now():
    # 数据库执行时间且不早于消息 created_at，避免宿主时钟回拨破坏时间 CHECK。
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), ConversationMessage.created_at)


# 函数 _conversation_now：会话更新使用数据库执行时间且不早于会话 created_at。
def _conversation_now():
    # 会话更新使用数据库执行时间且不早于会话 created_at。
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 HistoryTurn，把历史本轮对话需要的数据与操作放在同一个对象中。
class HistoryTurn:
    # 按完整终态轮次读取，避免截断助手调用与工具结果。

    # 声明 本轮对话编号（turn_id），类型为 int。
    turn_id: int
    # 声明 状态（status），类型为 str。
    status: str
    # 声明 最后一次消息no（last_message_no），类型为 int。
    last_message_no: int
    # 声明 消息列表（messages），类型为 tuple[ConversationMessage, ...]。
    messages: tuple[ConversationMessage, ...]


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 ToolRoundReservation，把业务工具轮Reservation需要的数据与操作放在同一个对象中。
class ToolRoundReservation:
    # 工具结果先预留序号，随后助手占位始终排在这些结果之后。

    # 声明 请求消息编号（request_message_id），类型为 int。
    request_message_id: int
    # 声明 结果消息nos（result_message_nos），类型为 tuple[int, ...]。
    result_message_nos: tuple[int, ...]
    # 声明 下一个assistant编号（next_assistant_id），类型为 int。
    next_assistant_id: int


# 异步函数 insert_pair：在已锁定会话的事务内分配两个连续序号，写入提问和助手占位。；接收 当前会话（session）、对话（conversation）、用户编号（user_id）、本轮对话编号（turn_id）、用户内容（user_content）、assistant模型（assistant_model）、当前时间（now）。
async def insert_pair(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写对话（conversation）字段，值为对话（Conversation）。
    conversation: Conversation,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写用户内容（user_content）字段，值为str。
    user_content: str,
    # 填写assistant模型（assistant_model）字段，值为str。
    assistant_model: str,
    # 填写当前时间（now）字段，值为日期时间类型（datetime）。
    now: datetime,
# 继续传入或组合表达式 ) -> tuple[ConversationMessage, ConversationMessage]: 的结果，完成上面尚未结束的表达式。
) -> tuple[ConversationMessage, ConversationMessage]:
    # 在已锁定会话的事务内分配两个连续序号，写入提问和助手占位。
    # 把对话（conversation）的下一个消息no（next_message_no）属性保存到 用户no（user_no）。
    user_no = conversation.next_message_no
    # 把按 user_no + 2 计算的结果保存到 对话中的下一个消息no（conversation.next_message_no）。
    conversation.next_message_no = user_no + 2
    # 使用数据库执行时间且不早于会话 created_at，保证时间 CHECK 条件成立。
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的最后一次消息at（conversation.last_message_at）。
    conversation.last_message_at = _conversation_now()
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
    conversation.updated_at = _conversation_now()
    # 把 conversation.version 加上 固定数值 1，再保存回原变量。
    conversation.version += 1

    # 把执行“调用 ConversationMessage 完成对话消息处理”得到的结果保存到 用户当前记录（user_row）。
    user_row = ConversationMessage(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入对话编号（conversation_id），值为表达式 conversation.id 的结果。
        conversation_id=conversation.id,
        # 传入本轮对话编号（turn_id），值为本轮对话编号（turn_id）。
        turn_id=turn_id,
        # 传入消息no（message_no），值为用户no（user_no）。
        message_no=user_no,
        # 传入角色（role），值为固定状态或编码 USER（用户）。
        role="USER",
        # 传入内容（content），值为用户内容（user_content）。
        content=user_content,
        # 传入是否完成（is_complete），值为真，表示条件成立或开关打开。
        is_complete=True,
        # 传入业务工具调用列表（tool_calls），值为一个空列表，用来保存后续多条数据。
        tool_calls=[],
    )
    # 把执行“调用 ConversationMessage 完成对话消息处理”得到的结果保存到 assistant当前记录（assistant_row）。
    assistant_row = ConversationMessage(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入对话编号（conversation_id），值为表达式 conversation.id 的结果。
        conversation_id=conversation.id,
        # 传入本轮对话编号（turn_id），值为本轮对话编号（turn_id）。
        turn_id=turn_id,
        # 传入消息no（message_no），值为按表达式计算得到的值：user_no + 1。
        message_no=user_no + 1,
        # 传入角色（role），值为固定状态或编码 ASSISTANT（ASSISTANT）。
        role="ASSISTANT",
        # 传入内容（content），值为固定文字 ''。
        content="",
        # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
        is_complete=False,
        # 传入业务工具调用列表（tool_calls），值为一个空列表，用来保存后续多条数据。
        tool_calls=[],
        # 传入模型（model），值为assistant模型（assistant_model）。
        model=assistant_model,
    )
    # 调用 session.add_all 完成加入全部处理。
    session.add_all([user_row, assistant_row])
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 等待调用 session.refresh 完成刷新处理完成后再继续。
    await session.refresh(user_row)
    # 等待调用 session.refresh 完成刷新处理完成后再继续。
    await session.refresh(assistant_row)
    # 把依次包含 用户当前记录（user_row）、assistant当前记录（assistant_row） 的集合交还给调用方，本函数到此结束。
    return user_row, assistant_row


# 异步函数 save_assistant：保存 assistant；接收 当前会话（session）、消息编号（message_id）、用户编号（user_id）、对话编号（conversation_id）、本轮对话编号（turn_id）、内容（content）、是否完成（is_complete）、finish原因（finish_reason）、用量（usage）、模型（model）、当前时间（now）。
async def save_assistant(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写消息编号（message_id）字段，值为int。
    message_id: int,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写内容（content）字段，值为str。
    content: str,
    # 填写是否完成（is_complete）字段，值为bool。
    is_complete: bool,
    # 填写finish原因（finish_reason）字段，值为表达式 str | None 的结果。
    finish_reason: str | None,
    # 填写用量（usage）字段，值为表达式 dict[str, Any] | None 的结果。
    usage: dict[str, Any] | None,
    # 填写模型（model）字段，值为str。
    model: str,
    # 填写当前时间（now）字段，值为日期时间类型（datetime）。
    now: datetime,
# 继续传入或组合表达式 ) -> bool: 的结果，完成上面尚未结束的表达式。
) -> bool:
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(ConversationMessage)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：ConversationMessage.id == message_id，完成上面尚未结束的表达式。
            ConversationMessage.id == message_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
            ConversationMessage.user_id == user_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.conversation_id == conversation_id，完成上面尚未结束的表达式。
            ConversationMessage.conversation_id == conversation_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.turn_id == turn_id，完成上面尚未结束的表达式。
            ConversationMessage.turn_id == turn_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.role == "ASSISTANT"，完成上面尚未结束的表达式。
            ConversationMessage.role == "ASSISTANT",
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            ConversationMessage.is_complete.is_(False),
            # 继续传入或组合条件判断的结果：ConversationMessage.tool_calls == []，完成上面尚未结束的表达式。
            ConversationMessage.tool_calls == [],
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入内容（content），值为内容（content）。
            content=content,
            # 传入是否完成（is_complete），值为是否完成（is_complete）。
            is_complete=is_complete,
            # 传入finish原因（finish_reason），值为finish原因（finish_reason）。
            finish_reason=finish_reason,
            # 传入用量（usage），值为用量（usage）。
            usage=usage,
            # 传入模型（model），值为模型（model）。
            model=model,
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
        )
    )
    # 把条件 (result.rowcount or 0) == 1 是否成立交还给调用方，本函数到此结束。
    return (result.rowcount or 0) == 1


# 异步函数 begin_tool_round：调用前保存请求；会话锁及轮次运行状态由 Service 在同事务核对。；接收 当前会话（session）、对话（conversation）、本轮对话编号（turn_id）、assistant消息编号（assistant_message_id）、内容（content）、调用列表（calls）、模型（model）、用量（usage）。
async def begin_tool_round(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写对话（conversation）字段，值为对话（Conversation）。
    conversation: Conversation,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写assistant消息编号（assistant_message_id）字段，值为int。
    assistant_message_id: int,
    # 填写内容（content）字段，值为str。
    content: str,
    # 填写调用列表（calls）字段，值为表达式 list[dict[str, Any]] 的结果。
    calls: list[dict[str, Any]],
    # 填写模型（model）字段，值为str。
    model: str,
    # 填写用量（usage）字段，值为表达式 dict[str, int] | None 的结果。
    usage: dict[str, int] | None,
# 继续传入或组合表达式 ) -> ToolRoundReservation: 的结果，完成上面尚未结束的表达式。
) -> ToolRoundReservation:
    # 调用前保存请求；会话锁及轮次运行状态由 Service 在同事务核对。
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await session.execute(
        # 继续传入或组合调用 update 完成更新处理，完成上面尚未结束的表达式。
        update(ConversationMessage)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：ConversationMessage.id == assistant_message_id，完成上面尚未结束的表达式。
            ConversationMessage.id == assistant_message_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.user_id == conversation.user_id，完成上面尚未结束的表达式。
            ConversationMessage.user_id == conversation.user_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.conversation_id == conversation.id，完成上面尚未结束的表达式。
            ConversationMessage.conversation_id == conversation.id,
            # 继续传入或组合条件判断的结果：ConversationMessage.turn_id == turn_id，完成上面尚未结束的表达式。
            ConversationMessage.turn_id == turn_id,
            # 继续传入或组合条件判断的结果：ConversationMessage.role == "ASSISTANT"，完成上面尚未结束的表达式。
            ConversationMessage.role == "ASSISTANT",
            # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
            ConversationMessage.is_complete.is_(False),
            # 继续传入或组合条件判断的结果：ConversationMessage.tool_calls == []，完成上面尚未结束的表达式。
            ConversationMessage.tool_calls == [],
        )
        # 继续补充本次调用或判断：调用 values 完成值列表处理。
        .values(
            # 传入内容（content），值为内容（content）。
            content=content,
            # 传入业务工具调用列表（tool_calls），值为调用列表（calls）。
            tool_calls=calls,
            # 传入是否完成（is_complete），值为真，表示条件成立或开关打开。
            is_complete=True,
            # 传入模型（model），值为模型（model）。
            model=model,
            # 传入finish原因（finish_reason），值为固定文字 'tool_calls'。
            finish_reason="tool_calls",
            # 传入用量（usage），值为用量（usage）。
            usage=usage,
            # 传入更新时at（updated_at），值为调用 _now 完成当前时间处理。
            updated_at=_now(),
        )
    )
    # 检查条件 result.rowcount != 1 是否成立；成立时执行缩进的分支。
    if result.rowcount != 1:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "TURN_STATE_CONFLICT", "工具请求状态已变化，请刷新会话")
    # 把对话（conversation）的下一个消息no（next_message_no）属性保存到 首次（first）。
    first = conversation.next_message_no
    # 把执行“调用 tuple 完成tuple处理”得到的结果保存到 结果numbers（result_numbers）。
    result_numbers = tuple(range(first, first + len(calls)))
    # 把按 first + len(calls) + 1 计算的结果保存到 对话中的下一个消息no（conversation.next_message_no）。
    conversation.next_message_no = first + len(calls) + 1
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的最后一次消息at（conversation.last_message_at）。
    conversation.last_message_at = _conversation_now()
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
    conversation.updated_at = _conversation_now()
    # 把 conversation.version 加上 固定数值 1，再保存回原变量。
    conversation.version += 1
    # 把执行“调用 ConversationMessage 完成对话消息处理”得到的结果保存到 assistant。
    assistant = ConversationMessage(
        # 传入用户编号（user_id），值为表达式 conversation.user_id 的结果。
        user_id=conversation.user_id,
        # 传入对话编号（conversation_id），值为表达式 conversation.id 的结果。
        conversation_id=conversation.id,
        # 传入本轮对话编号（turn_id），值为本轮对话编号（turn_id）。
        turn_id=turn_id,
        # 传入消息no（message_no），值为按表达式计算得到的值：first + len(calls)。
        message_no=first + len(calls),
        # 传入角色（role），值为固定状态或编码 ASSISTANT（ASSISTANT）。
        role="ASSISTANT",
        # 传入内容（content），值为固定文字 ''。
        content="",
        # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
        is_complete=False,
        # 传入业务工具调用列表（tool_calls），值为一个空列表，用来保存后续多条数据。
        tool_calls=[],
        # 传入模型（model），值为模型（model）。
        model=model,
    )
    # 加入集合或登记一条待保存记录。
    session.add(assistant)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把执行“调用 ToolRoundReservation 完成业务工具轮Reservation处理”得到的结果交还给调用方，本函数到此结束。
    return ToolRoundReservation(assistant_message_id, result_numbers, assistant.id)


# 异步函数 insert_tool_result：调用 insert_tool_result 完成写入新记录业务工具结果处理；接收 当前会话（session）、对话（conversation）、本轮对话编号（turn_id）、请求消息编号（request_message_id）、消息no（message_no）、调用编号（call_id）、名称（name）、内容（content）。
async def insert_tool_result(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写对话（conversation）字段，值为对话（Conversation）。
    conversation: Conversation,
    # 填写本轮对话编号（turn_id）字段，值为int。
    turn_id: int,
    # 填写请求消息编号（request_message_id）字段，值为int。
    request_message_id: int,
    # 填写消息no（message_no）字段，值为int。
    message_no: int,
    # 填写调用编号（call_id）字段，值为str。
    call_id: str,
    # 填写名称（name）字段，值为str。
    name: str,
    # 填写内容（content）字段，值为str。
    content: str,
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 加入集合或登记一条待保存记录。
    session.add(
        # 继续传入或组合调用 ConversationMessage 完成对话消息处理，完成上面尚未结束的表达式。
        ConversationMessage(
            # 传入用户编号（user_id），值为表达式 conversation.user_id 的结果。
            user_id=conversation.user_id,
            # 传入对话编号（conversation_id），值为表达式 conversation.id 的结果。
            conversation_id=conversation.id,
            # 传入本轮对话编号（turn_id），值为本轮对话编号（turn_id）。
            turn_id=turn_id,
            # 传入消息no（message_no），值为消息no（message_no）。
            message_no=message_no,
            # 传入角色（role），值为固定状态或编码 TOOL（业务工具）。
            role="TOOL",
            # 传入内容（content），值为内容（content）。
            content=content,
            # 传入是否完成（is_complete），值为真，表示条件成立或开关打开。
            is_complete=True,
            # 传入业务工具调用列表（tool_calls），值为一个空列表，用来保存后续多条数据。
            tool_calls=[],
            # 传入业务工具请求消息编号（tool_request_message_id），值为请求消息编号（request_message_id）。
            tool_request_message_id=request_message_id,
            # 传入业务工具调用编号（tool_call_id），值为调用编号（call_id）。
            tool_call_id=call_id,
            # 传入业务工具名称（tool_name），值为名称（name）。
            tool_name=name,
        )
    )
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的最后一次消息at（conversation.last_message_at）。
    conversation.last_message_at = _conversation_now()
    # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
    conversation.updated_at = _conversation_now()
    # 把 conversation.version 加上 固定数值 1，再保存回原变量。
    conversation.version += 1
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()


# 异步函数 page_before：调用 page_before 完成当前页变化前处理；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、变化前消息no（before_message_no）、每页条数（size）。
async def page_before(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写变化前消息no（before_message_no）字段，值为表达式 int | None 的结果。
    before_message_no: int | None,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[ConversationMessage], bool, int | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[ConversationMessage], bool, int | None]:
    # 把执行“调用 select(ConversationMessage).where 完成where处理”得到的结果保存到 statement。
    statement = select(ConversationMessage).where(
        # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
        ConversationMessage.user_id == user_id,
        # 继续传入或组合条件判断的结果：ConversationMessage.conversation_id == conversation_id，完成上面尚未结束的表达式。
        ConversationMessage.conversation_id == conversation_id,
        # 公开聊天分页只展示提问与最终回复，内部工具事实仍完整存储。
        # 继续传入或组合调用 or_ 完成or处理，完成上面尚未结束的表达式。
        or_(
            # 继续传入或组合条件判断的结果：ConversationMessage.role == "USER"，完成上面尚未结束的表达式。
            ConversationMessage.role == "USER",
            # 继续传入或组合调用 and_ 完成and处理，完成上面尚未结束的表达式。
            and_(ConversationMessage.role == "ASSISTANT", ConversationMessage.tool_calls == []),
        ),
    )
    # 检查条件 before_message_no is not None 是否成立；成立时执行缩进的分支。
    if before_message_no is not None:
        # 把执行“调用 statement.where 完成where处理”得到的结果保存到 statement。
        statement = statement.where(ConversationMessage.message_no < before_message_no)
    # 把执行“调用 list 完成列表处理”得到的结果保存到 记录列表（rows）。
    rows = list(
        (
            # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
            await session.execute(
                # 继续传入或组合调用 statement.order_by 完成顺序按处理，完成上面尚未结束的表达式。
                statement.order_by(ConversationMessage.message_no.desc()).limit(size + 1)
            )
        # 继续传入或组合取得查询结果的第一列对象，完成上面尚未结束的表达式。
        ).scalars()
    )
    # 把条件 len(rows) > size 是否成立保存到 是否存在more（has_more）。
    has_more = len(rows) > size
    # 把从记录列表（rows）中按表达式 :size 的结果取出的内容保存到 记录列表（rows）。
    rows = rows[:size]
    # 调用 rows.reverse 完成reverse处理。
    rows.reverse()
    # 把条件 has_more and rows 是否成立时使用从记录列表（rows）中按固定数值 0取出的内容的消息no（message_no）属性，否则使用空值，表示当前还没有对应内容保存到 下一个变化前（next_before）。
    next_before = rows[0].message_no if has_more and rows else None
    # 把依次包含 记录列表（rows）、是否存在more（has_more）、下一个变化前（next_before） 的集合交还给调用方，本函数到此结束。
    return rows, has_more, next_before


# 异步函数 history_turns：选择完整终态轮次，再读取其全部消息；旧摘要边界在轮次内部时也不拆组。；接收 当前会话（session）、用户编号（user_id）、对话编号（conversation_id）、变化后消息no（after_message_no）、变化前消息no（before_message_no）、上限（limit）、newest。
async def history_turns(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写变化后消息no（after_message_no）字段，值为int。
    after_message_no: int,
    # 填写变化前消息no（before_message_no）字段，值为表达式 int | None 的结果。
    before_message_no: int | None,
    # 填写上限（limit）字段，值为int。
    limit: int,
    # 填写newest字段，值为表达式 bool = True 的结果。
    newest: bool = True,
# 继续传入或组合表达式 ) -> list[HistoryTurn]: 的结果，完成上面尚未结束的表达式。
) -> list[HistoryTurn]:
    # 选择完整终态轮次，再读取其全部消息；旧摘要边界在轮次内部时也不拆组。
    # 把依次包含 条件 ConversationMessage.user_id == user_id 是否成立、条件 ConversationMessage.conversation_id == conversation_id 是否成立、条件 ConversationTurn.status != 'RUNNING' 是否成立 的集合保存到 conditions。
    conditions = [
        # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
        ConversationMessage.user_id == user_id,
        # 继续传入或组合条件判断的结果：ConversationMessage.conversation_id == conversation_id，完成上面尚未结束的表达式。
        ConversationMessage.conversation_id == conversation_id,
        # 继续传入或组合条件判断的结果：ConversationTurn.status != "RUNNING"，完成上面尚未结束的表达式。
        ConversationTurn.status != "RUNNING",
    ]
    # 检查条件 before_message_no is not None 是否成立；成立时执行缩进的分支。
    if before_message_no is not None:
        # 把新条目放到列表末尾。
        conditions.append(ConversationMessage.message_no < before_message_no)
    # 把执行“调用 and_ 完成and处理”得到的结果保存到 relation。
    relation = and_(
        # 继续传入或组合条件判断的结果：ConversationTurn.id == ConversationMessage.turn_id，完成上面尚未结束的表达式。
        ConversationTurn.id == ConversationMessage.turn_id,
        # 继续传入或组合条件判断的结果：ConversationTurn.user_id == ConversationMessage.user_id，完成上面尚未结束的表达式。
        ConversationTurn.user_id == ConversationMessage.user_id,
        # 继续传入或组合条件判断的结果：ConversationTurn.conversation_id == ConversationMessage.conversation_id，完成上面尚未结束的表达式。
        ConversationTurn.conversation_id == ConversationMessage.conversation_id,
    )
    # 把执行“调用 func.max 完成最大处理”得到的结果保存到 最后一次no（last_no）。
    last_no = func.max(ConversationMessage.message_no)
    # 把执行“调用 select(ConversationMessage.turn_id).join(ConversationTurn, relation).where(*conditions).group_by(ConversationMessage.turn_id).having(last_no > after_message_no).order_by(last_no.desc() if newest else last_no.asc()).limit(limit).correlate 完成correlate处理”得到的结果保存到 已选（selected）。
    selected = (
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(ConversationMessage.turn_id)
        # 继续补充本次调用或判断：用指定分隔符把多段文字合起来。
        .join(ConversationTurn, relation)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(*conditions)
        # 继续补充本次调用或判断：调用 group_by 完成分组按处理。
        .group_by(ConversationMessage.turn_id)
        # 继续补充本次调用或判断：调用 having 完成having处理。
        .having(last_no > after_message_no)
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(last_no.desc() if newest else last_no.asc())
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(limit)
        # 继续补充本次调用或判断：调用 correlate 完成correlate处理。
        .correlate(None)
    )
    # 把执行“调用 (await session.execute(select(ConversationMessage, ConversationTurn.status).join(ConversationTurn, relation).where(*conditions, ConversationMessage.turn_id.in_(selected)).order_by(ConversationMessage.message_no))).all 完成全部处理”得到的结果保存到 记录列表（rows）。
    rows = (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
            select(ConversationMessage, ConversationTurn.status)
            # 继续补充本次调用或判断：用指定分隔符把多段文字合起来。
            .join(ConversationTurn, relation)
            # 继续补充本次调用或判断：调用 where 完成where处理。
            .where(*conditions, ConversationMessage.turn_id.in_(selected))
            # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
            .order_by(ConversationMessage.message_no)
        )
    # 继续传入或组合调用 ).all 完成全部处理，完成上面尚未结束的表达式。
    ).all()
    # 声明 分组列表（groups），类型为 dict[int, list[ConversationMessage]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
    groups: dict[int, list[ConversationMessage]] = {}
    # 声明 statuses，类型为 dict[int, str]；其内容为按字段名保存多个值的字典，这里先创建空字典。
    statuses: dict[int, str] = {}
    # 从记录列表（rows）依次取出消息状态（(message, status)），每项执行一次下面的处理。
    for message, status in rows:
        # 把新条目放到列表末尾。
        groups.setdefault(message.turn_id, []).append(message)
        # 把状态（status）保存到 statuses消息本轮对话编号（statuses[message.turn_id]）。
        statuses[message.turn_id] = status
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return [
        # 继续传入或组合调用 HistoryTurn 完成历史本轮对话处理，完成上面尚未结束的表达式。
        HistoryTurn(turn_id, statuses[turn_id], values[-1].message_no, tuple(values))
        # 继续传入或组合调用 for turn_id, values in groups.items 完成数据列表处理，完成上面尚未结束的表达式。
        for turn_id, values in groups.items()
    ]


# 异步函数 usage_by_turn：消息按调用保存实际用量；公开最终回复按轮次汇总，不重复保存累计值。；接收 当前会话（session）、用户编号（user_id）、本轮对话编号列表（turn_ids）。
async def usage_by_turn(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, user_id: int, turn_ids: set[int] 的结果。
    session: AsyncSession, user_id: int, turn_ids: set[int]
# 继续传入或组合表达式 ) -> dict[int, dict[str, int]]: 的结果，完成上面尚未结束的表达式。
) -> dict[int, dict[str, int]]:
    # 消息按调用保存实际用量；公开最终回复按轮次汇总，不重复保存累计值。
    # 检查条件 not turn_ids 是否成立；成立时执行缩进的分支。
    if not turn_ids:
        # 把按字段名保存多个值的字典，这里先创建空字典交还给调用方，本函数到此结束。
        return {}
    # 声明 结果（result），类型为 dict[int, dict[str, int]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
    result: dict[int, dict[str, int]] = {}
    # 把执行“调用 (await session.execute(select(ConversationMessage.turn_id, ConversationMessage.usage).where(ConversationMessage.user_id == user_id, ConversationMessage.turn_id.in_(turn_ids), ConversationMessage.role == 'ASSISTANT', ConversationMessage.usage.is_not(None)))).all 完成全部处理”得到的结果保存到 记录列表（rows）。
    rows = (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select(ConversationMessage.turn_id, ConversationMessage.usage).where 完成where处理，完成上面尚未结束的表达式。
            select(ConversationMessage.turn_id, ConversationMessage.usage).where(
                # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
                ConversationMessage.user_id == user_id,
                # 继续传入或组合调用 ConversationMessage.turn_id.in_ 完成in处理，完成上面尚未结束的表达式。
                ConversationMessage.turn_id.in_(turn_ids),
                # 继续传入或组合条件判断的结果：ConversationMessage.role == "ASSISTANT"，完成上面尚未结束的表达式。
                ConversationMessage.role == "ASSISTANT",
                # 继续传入或组合判断是否为 not，完成上面尚未结束的表达式。
                ConversationMessage.usage.is_not(None),
            )
        )
    # 继续传入或组合调用 ).all 完成全部处理，完成上面尚未结束的表达式。
    ).all()
    # 从记录列表（rows）依次取出本轮对话编号用量（(turn_id, usage)），每项执行一次下面的处理。
    for turn_id, usage in rows:
        # 检查条件 not isinstance(usage, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(usage, dict):
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把执行“设置 默认值（default） 的值”得到的结果保存到 值列表（values）。
        values = result.setdefault(turn_id, {})
        # 从依次包含 固定文字 'promptTokens'、固定文字 'completionTokens'、固定文字 'totalTokens' 的集合依次取出键（key），每项执行一次下面的处理。
        for key in ("promptTokens", "completionTokens", "totalTokens"):
            # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 值（value）。
            value = usage.get(key)
            # 检查条件 isinstance(value, int) and (not isinstance(value, bool)) and (value >= 0) 是否成立；成立时执行缩进的分支。
            if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
                # 把按 values.get(key, 0) + value 计算的结果保存到 值列表键（values[key]）。
                values[key] = values.get(key, 0) + value
    # 把结果（result）交还给调用方，本函数到此结束。
    return result


# 异步函数 assistant_for_turn：调用 assistant_for_turn 完成assistantfor本轮对话处理；接收 当前会话（session）、用户编号（user_id）、本轮对话编号（turn_id）。
async def assistant_for_turn(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, *, user_id: int, turn_id: int。
    session: AsyncSession, *, user_id: int, turn_id: int
# 继续传入或组合表达式 ) -> ConversationMessage | None: 的结果，完成上面尚未结束的表达式。
) -> ConversationMessage | None:
    # 把执行“调用 (await session.execute(select(ConversationMessage).where(ConversationMessage.user_id == user_id, ConversationMessage.turn_id == turn_id, ConversationMessage.role == 'ASSISTANT', ConversationMessage.tool_calls == []).order_by(ConversationMessage.message_no.desc()).limit(1))).scalar_one_or_none 完成单个查询值oneor空值处理”得到的结果交还给调用方，本函数到此结束。
    return (
        # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
        await session.execute(
            # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
            select(ConversationMessage)
            # 继续补充本次调用或判断：调用 where 完成where处理。
            .where(
                # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
                ConversationMessage.user_id == user_id,
                # 继续传入或组合条件判断的结果：ConversationMessage.turn_id == turn_id，完成上面尚未结束的表达式。
                ConversationMessage.turn_id == turn_id,
                # 继续传入或组合条件判断的结果：ConversationMessage.role == "ASSISTANT"，完成上面尚未结束的表达式。
                ConversationMessage.role == "ASSISTANT",
                # 继续传入或组合条件判断的结果：ConversationMessage.tool_calls == []，完成上面尚未结束的表达式。
                ConversationMessage.tool_calls == [],
            )
            # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
            .order_by(ConversationMessage.message_no.desc())
            # 继续补充本次调用或判断：调用 limit 完成上限处理。
            .limit(1)
        )
    # 继续传入或组合调用 ).scalar_one_or_none 完成单个查询值oneor空值处理，完成上面尚未结束的表达式。
    ).scalar_one_or_none()


# 异步函数 by_turn：调用 by_turn 完成按本轮对话处理；接收 当前会话（session）、用户编号（user_id）、本轮对话编号（turn_id）。
async def by_turn(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, *, user_id: int, turn_id: int。
    session: AsyncSession, *, user_id: int, turn_id: int
# 继续传入或组合表达式 ) -> list[ConversationMessage]: 的结果，完成上面尚未结束的表达式。
) -> list[ConversationMessage]:
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(
        (
            # 继续传入或组合等待 执行传入的数据库语句并取得结果 完成后取得结果，完成上面尚未结束的表达式。
            await session.execute(
                # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
                select(ConversationMessage)
                # 继续补充本次调用或判断：调用 where 完成where处理。
                .where(
                    # 继续传入或组合条件判断的结果：ConversationMessage.user_id == user_id，完成上面尚未结束的表达式。
                    ConversationMessage.user_id == user_id,
                    # 继续传入或组合条件判断的结果：ConversationMessage.turn_id == turn_id，完成上面尚未结束的表达式。
                    ConversationMessage.turn_id == turn_id,
                )
                # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
                .order_by(ConversationMessage.message_no)
            )
        # 继续传入或组合取得查询结果的第一列对象，完成上面尚未结束的表达式。
        ).scalars()
    )
