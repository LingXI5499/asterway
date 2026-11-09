# 维护当前用户的对话列表、标题、历史分页与删除。
# 会话生命周期：列表、详情、创建、标题、关闭和逻辑删除。

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）。
from datetime import UTC, datetime

# 从 sqlalchemy 引入 func。
from sqlalchemy import func, select

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError
from app.db.knowledge_models import ConversationKnowledgeBase

# 从 app.db.models 引入 对话（Conversation）、对话消息（ConversationMessage）、对话本轮对话（ConversationTurn）。
from app.db.models import Conversation, ConversationMessage, ConversationTurn

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database
from app.repositories import (
    # 继续传入或组合对话数据仓库（conversation_repository），完成上面尚未结束的表达式。
    conversation_repository,
    # 继续传入或组合消息数据仓库（message_repository），完成上面尚未结束的表达式。
    message_repository,
    # 继续传入或组合本轮对话数据仓库（turn_repository），完成上面尚未结束的表达式。
    turn_repository,
)

# 从 app.schemas.conversation 引入 对话详情（ConversationDetailVO）、对话当前页（ConversationPageVO）、对话（ConversationVO）、消息当前页（MessagePageVO）、消息（MessageVO）、本轮对话（TurnVO）。
from app.schemas.conversation import (
    # 继续传入或组合对话详情（ConversationDetailVO），完成上面尚未结束的表达式。
    ConversationDetailVO,
    # 继续传入或组合对话当前页（ConversationPageVO），完成上面尚未结束的表达式。
    ConversationPageVO,
    # 继续传入或组合对话（ConversationVO），完成上面尚未结束的表达式。
    ConversationVO,
    # 继续传入或组合消息当前页（MessagePageVO），完成上面尚未结束的表达式。
    MessagePageVO,
    # 继续传入或组合消息（MessageVO），完成上面尚未结束的表达式。
    MessageVO,
    # 继续传入或组合本轮对话（TurnVO），完成上面尚未结束的表达式。
    TurnVO,
)

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、消息数据仓库（message_repository）、本轮对话数据仓库（turn_repository）。
from app.services import knowledge_base_service as knowledge_bases


# 函数 _conversation_now：会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。
def _conversation_now():
    # 会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), Conversation.created_at)


# 函数 conversation_vo：调用 conversation_vo 完成对话处理；接收 对话（conversation）。
def conversation_vo(conversation: Conversation) -> ConversationVO:
    return ConversationVO(id=str(conversation.id), title=conversation.title, status=conversation.status, summary_through_message_no=conversation.summary_through_message_no, last_message_at=conversation.last_message_at, created_at=conversation.created_at, updated_at=conversation.updated_at, version=conversation.version, knowledge_base_ids=[str(i) for i in getattr(conversation, "knowledge_base_ids", [])])


# 函数 turn_vo：调用 turn_vo 完成本轮对话处理；接收 本轮对话（turn）、reply。
def turn_vo(turn: ConversationTurn, reply: str | None = None) -> TurnVO:
    # 把执行“调用 TurnVO 完成本轮对话处理”得到的结果交还给调用方，本函数到此结束。
    return TurnVO(
        # 传入编号（id），值为把输入转换成文字，统一比较或展示时的格式。
        id=str(turn.id),
        # 传入对话编号（conversation_id），值为把输入转换成文字，统一比较或展示时的格式。
        conversation_id=str(turn.conversation_id),
        # 传入请求编号（request_id），值为把输入转换成文字，统一比较或展示时的格式。
        request_id=str(turn.request_id),
        # 传入状态（status），值为表达式 turn.status 的结果。
        status=turn.status,
        # 传入finish原因（finish_reason），值为表达式 turn.finish_reason 的结果。
        finish_reason=turn.finish_reason,
        # 传入错误编码（error_code），值为表达式 turn.error_code 的结果。
        error_code=turn.error_code,
        # 传入开始时at（started_at），值为表达式 turn.started_at 的结果。
        started_at=turn.started_at,
        # 传入finishedat（finished_at），值为表达式 turn.finished_at 的结果。
        finished_at=turn.finished_at,
        # 传入版本号（version），值为表达式 turn.version 的结果。
        version=turn.version,
        # 传入reply，值为reply。
        reply=reply,
    )


# 函数 _check_version：调用 _check_version 完成核对版本号处理；接收 对话（conversation）、版本号（version）。
def _check_version(conversation: Conversation, version: int) -> None:
    # 检查当前记录版本是否符合请求要求：conversation.version != version；成立时执行缩进的分支。
    if conversation.version != version:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "STATE_CONFLICT", "会话已被其他操作更新，请刷新后重试")


# 函数 message_vo：调用 message_vo 完成消息处理；接收 消息（message）、用量（usage）。
def message_vo(message: ConversationMessage, usage: dict[str, int] | None = None) -> MessageVO:
    # 把执行“调用 MessageVO 完成消息处理”得到的结果交还给调用方，本函数到此结束。
    return MessageVO(
        # 传入编号（id），值为把输入转换成文字，统一比较或展示时的格式。
        id=str(message.id),
        # 传入citations，值为表达式 message.citations, used_memory_ids=message.used_memory_ids 的结果。
        citations=message.citations, used_memory_ids=message.used_memory_ids,
        # 传入消息no（message_no），值为表达式 message.message_no 的结果。
        message_no=message.message_no,
        # 传入角色（role），值为表达式 message.role 的结果。
        role=message.role,
        # 传入内容（content），值为表达式 message.content 的结果。
        content=message.content,
        # 传入是否完成（is_complete），值为表达式 message.is_complete 的结果。
        is_complete=message.is_complete,
        # 传入finish原因（finish_reason），值为表达式 message.finish_reason 的结果。
        finish_reason=message.finish_reason,
        # 传入模型（model），值为表达式 message.model 的结果。
        model=message.model,
        # 传入用量（usage），值为条件判断的结果：usage if usage is not None else message.usage。
        usage=usage if usage is not None else message.usage,
        # 传入创建时at（created_at），值为表达式 message.created_at 的结果。
        created_at=message.created_at,
    )


# 异步函数 list_messages：列出 消息列表（messages）；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、变化前消息no（before_message_no）、每页条数（size）。
async def list_messages(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写变化前消息no（before_message_no）字段，值为表达式 int | None 的结果。
    before_message_no: int | None,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> MessagePageVO: 的结果，完成上面尚未结束的表达式。
) -> MessagePageVO:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, user_id, conversation_id)
        # 把等待执行“调用 message_repository.page_before 完成当前页变化前处理”得到的结果完成后的结果保存到 记录列表是否存在more下一个变化前（(rows, has_more, next_before)）。
        rows, has_more, next_before = await message_repository.page_before(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
            conversation_id=conversation_id,
            # 传入变化前消息no（before_message_no），值为变化前消息no（before_message_no）。
            before_message_no=before_message_no,
            # 传入每页条数（size），值为每页条数（size）。
            size=size,
        )
        # 把等待执行“调用 message_repository.usage_by_turn 完成用量按本轮对话处理”得到的结果完成后的结果保存到 用量（usage）。
        usage = await message_repository.usage_by_turn(
            # 继续传入或组合条件判断的结果：session, user_id, {row.turn_id for row in rows if row.role == "ASSISTANT"}，完成上面尚未结束的表达式。
            session, user_id, {row.turn_id for row in rows if row.role == "ASSISTANT"}
        )
        # 把执行“调用 MessagePageVO 完成消息当前页处理”得到的结果交还给调用方，本函数到此结束。
        return MessagePageVO(
            # 传入数据列表（items），值为表达式 [ 的结果。
            items=[
                # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                message_vo(row, usage.get(row.turn_id) if row.role == "ASSISTANT" else None)
                # 继续传入或组合表达式 for row in rows 的结果，完成上面尚未结束的表达式。
                for row in rows
            ],
            # 传入是否存在more（has_more），值为是否存在more（has_more）。
            has_more=has_more,
            # 传入下一个变化前消息no（next_before_message_no），值为下一个变化前（next_before）。
            next_before_message_no=next_before,
        )


# 异步函数 list_conversations：列出 对话列表（conversations）；接收 Agent 数据库访问器（db）、用户编号（user_id）、状态（status）、keyword、当前页（page）、每页条数（size）。
async def list_conversations(db: Database, user_id: int, *, status: str | None, keyword: str | None, page: int, size: int) -> ConversationPageVO:
    async with db.session() as session:
        (rows, total) = await conversation_repository.page(session, user_id, status=status, keyword=keyword.strip() if keyword else None, page_number=page, size=size)
        selections = await session.execute(select(ConversationKnowledgeBase.conversation_id, ConversationKnowledgeBase.knowledge_base_id).where(
            ConversationKnowledgeBase.user_id == user_id, ConversationKnowledgeBase.conversation_id.in_([r.id for r in rows])))
        grouped = {}
        for conv_id, base_id in selections:
            grouped.setdefault(conv_id, []).append(base_id)
        for row in rows:
            row.knowledge_base_ids = grouped.get(row.id, [])
        return ConversationPageVO(items=[conversation_vo(row) for row in rows], total=total, page=page, size=size)


# 异步函数 get_conversation：读取 对话（_conversation） 的值；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def get_conversation(db: Database, user_id: int, conversation_id: int) -> ConversationDetailVO:
    await normalize_expired(db, user_id, conversation_id)
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(session, user_id, conversation_id)
        conversation.knowledge_base_ids = await knowledge_bases.selected_ids(session, user_id, conversation_id)
        running = await turn_repository.find_running(session, conversation_id)
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=turn_vo(running) if running else None)


# 异步函数 normalize_expired：恢复读取前归一过期轮次；进程中断后不让页面一直停在生成中。；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def normalize_expired(db: Database, user_id: int, conversation_id: int) -> None:
    # 恢复读取前归一过期轮次；进程中断后不让页面一直停在生成中。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“要求满足 owned”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await conversation_repository.require_owned(
            # 继续传入或组合表达式 session, user_id, conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, user_id, conversation_id, for_update=True
        )
        # 把等待执行“调用 turn_repository.expire_running 完成过期运行中处理”得到的结果完成后的结果保存到 已修改（changed）。
        changed = await turn_repository.expire_running(session, conversation_id, datetime.now(UTC))
        # 检查条件 changed 是否成立；成立时执行缩进的分支。
        if changed:
            # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
            conversation.updated_at = _conversation_now()
            # 把 conversation.version 加上 固定数值 1，再保存回原变量。
            conversation.version += 1


# 异步函数 create_conversation：创建 对话（conversation）；接收 Agent 数据库访问器（db）、用户编号（user_id）、标题（title）。
async def create_conversation(db: Database, user_id: int, title: str | None, knowledge_base_ids: list[int] | None = None) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.create(session, user_id, title or '新对话')
        conversation.knowledge_base_ids = await knowledge_bases.set_selection(session, user_id, conversation.id, knowledge_base_ids or [])
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=None)


# 异步函数 rename_conversation：调用 rename_conversation 完成重命名对话处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、标题（title）、版本号（version）。
async def rename_conversation(db: Database, user_id: int, conversation_id: int, title: str, version: int) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(session, user_id, conversation_id, for_update=True)
        _check_version(conversation, version)
        conversation.title = title
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
        await session.refresh(conversation)
        conversation.knowledge_base_ids = await knowledge_bases.selected_ids(session, user_id, conversation_id)
        running = await turn_repository.find_running(session, conversation_id)
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=turn_vo(running) if running else None)


# 异步函数 close_conversation：调用 close_conversation 完成关闭对话处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、版本号（version）。
async def close_conversation(db: Database, user_id: int, conversation_id: int, version: int) -> ConversationDetailVO:
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(session, user_id, conversation_id, for_update=True)
        _check_version(conversation, version)
        if conversation.status == 'CLOSED':
            raise AgentError(409, 'CONVERSATION_CLOSED', '会话已经关闭')
        if await turn_repository.count_running(session, conversation_id) > 0:
            raise AgentError(409, 'TURN_RUNNING', '请先停止当前正在生成的回复')
        conversation.status = 'CLOSED'
        conversation.closed_at = _conversation_now()
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
        await session.refresh(conversation)
        conversation.knowledge_base_ids = await knowledge_bases.selected_ids(session, user_id, conversation_id)
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=None)


# 异步函数 delete_conversation：删除 对话（conversation）；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、版本号（version）。
async def delete_conversation(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, conversation_id: int, version: int 的结果。
    db: Database, user_id: int, conversation_id: int, version: int
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“要求满足 owned”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await conversation_repository.require_owned(
            # 继续传入或组合表达式 session, user_id, conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, user_id, conversation_id, for_update=True
        )
        # 调用 _check_version 完成核对版本号处理。
        _check_version(conversation, version)
        # 检查条件 await turn_repository.count_running(session, conversation_id) > 0 是否成立；成立时执行缩进的分支。
        if await turn_repository.count_running(session, conversation_id) > 0:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "TURN_RUNNING", "请先停止当前正在生成的回复")
        # 逻辑删除要求先关闭；同一事务内完成，历史消息与轮次保留。
        # 把固定状态或编码 CLOSED（CLOSED）保存到 对话中的状态（conversation.status）。
        conversation.status = "CLOSED"
        # 检查conversation.closed_at is None，即对应内容是否不存在；成立时执行缩进的分支。
        if conversation.closed_at is None:
            # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的closedat（conversation.closed_at）。
            conversation.closed_at = _conversation_now()
        # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的删除标记at（conversation.deleted_at）。
        conversation.deleted_at = _conversation_now()
        # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
        conversation.updated_at = _conversation_now()
        # 把 conversation.version 加上 固定数值 1，再保存回原变量。
        conversation.version += 1
        # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
        await session.flush()


async def update_knowledge_bases(db: Database, user_id: int, conversation_id: int, ids: list[int], version: int):
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(session, user_id, conversation_id, for_update=True)
        _check_version(conversation, version)
        conversation.knowledge_base_ids = await knowledge_bases.set_selection(session, user_id, conversation_id, ids)
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        await session.flush()
        await session.refresh(conversation)
        running = await turn_repository.find_running(session, conversation_id)
        return ConversationDetailVO(**conversation_vo(conversation).model_dump(), running_turn=turn_vo(running) if running else None)
