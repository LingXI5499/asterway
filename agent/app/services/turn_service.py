# 组织完整对话轮次、流式事件、部分保存、取消和终态收尾。
# 会话轮次：幂等重放、生成、分批保存、终态提交、取消与到期归一。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 hashlib，供下面调用其功能。
import hashlib
import json

# 引入 logging，供下面调用其功能。
import logging

# 引入 re，供下面调用其功能。
import re

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 collections.abc 引入 可逐项读取的异步迭代结果（AsyncIterator）、Awaitable、Callable。
from collections.abc import AsyncIterator, Awaitable, Callable

# 从 contextlib 引入 suppress。
from contextlib import suppress

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）、字段（field）、整体替换（replace）。
from dataclasses import dataclass, field, replace

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）、两个时刻之间的时间长度（timedelta）。
from datetime import UTC, datetime, timedelta

# 从 typing 引入 Any。
from typing import Any

# 从 uuid 引入 UUID。
from uuid import UUID

# 从 sqlalchemy 引入 func。
from sqlalchemy import func

# 从 sqlalchemy.exc 引入 Integrity错误（IntegrityError）。
from sqlalchemy.exc import IntegrityError

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 对话（Conversation）。
from app.db.models import Conversation

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.chat_service 引入 事件frame（event_frame）、模型选项（model_options）、把底层异常转换成脱敏的可读错误（safe_error）。
from app.llm.chat_service import event_frame, model_options, safe_error

# 从 app.llm.client 引入 取得复用的模型客户端（get_llm_client）。
from app.llm.client import get_llm_client

# 从 app.llm.tool_calling 引入 业务工具调用（ToolCall）、assistant业务工具消息（assistant_tool_message）、构造业务工具调用列表（build_tool_calls）、merge业务工具deltas（merge_tool_deltas）、业务工具结果消息（tool_result_message）。
from app.llm.tool_calling import (
    # 继续传入或组合业务工具调用（ToolCall），完成上面尚未结束的表达式。
    ToolCall,
    # 继续传入或组合assistant业务工具消息（assistant_tool_message），完成上面尚未结束的表达式。
    assistant_tool_message,
    # 继续传入或组合构造业务工具调用列表（build_tool_calls），完成上面尚未结束的表达式。
    build_tool_calls,
    # 继续传入或组合merge业务工具deltas（merge_tool_deltas），完成上面尚未结束的表达式。
    merge_tool_deltas,
    # 继续传入或组合业务工具结果消息（tool_result_message），完成上面尚未结束的表达式。
    tool_result_message,
)

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、消息数据仓库（message_repository）、本轮对话数据仓库（turn_repository）。
from app.repositories import conversation_repository, message_repository, turn_repository

# 从 app.schemas.conversation 引入 发送结果（SendResultVO）、本轮对话（TurnVO）。
from app.schemas.conversation import SendResultVO, TurnVO

# 从 app.services 引入 上下文业务服务（context_service）、对话规划（conversation_planning）、对话复盘（conversation_review）、对话业务服务（conversation_service）、汇总业务服务（summary_service）、业务工具业务服务（tool_service）。
from app.services import (
    # 继续传入或组合上下文业务服务（context_service），完成上面尚未结束的表达式。
    context_service,
    # 继续传入或组合对话规划（conversation_planning），完成上面尚未结束的表达式。
    conversation_planning,
    # 继续传入或组合对话复盘（conversation_review），完成上面尚未结束的表达式。
    conversation_review,
    # 继续传入或组合对话业务服务（conversation_service），完成上面尚未结束的表达式。
    conversation_service,
    # 继续传入或组合汇总业务服务（summary_service），完成上面尚未结束的表达式。
    summary_service,
    # 继续传入或组合业务工具业务服务（tool_service），完成上面尚未结束的表达式。
    tool_service,
)
from app.services import knowledge_base_service as knowledge_bases

# 从 app.services.tool_budget 引入 读取额度（ReadBudget）。
from app.services.tool_budget import ReadBudget

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution

# 从 app.tools.registry 引入 业务工具definitions（tool_definitions）。
from app.tools.registry import tool_definitions

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 错误编码（ERROR_CODE）。
ERROR_CODE = re.compile(r"[A-Z][A-Z0-9_]{0,63}")
# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 FINISH原因（FINISH_REASON）。
FINISH_REASON = re.compile(r"[a-z][a-z0-9_]{0,63}")


# 函数 _conversation_now：会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。
def _conversation_now():
    # 会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。
    # 把执行“调用 func.greatest 完成greatest处理”得到的结果交还给调用方，本函数到此结束。
    return func.greatest(func.clock_timestamp(), Conversation.created_at)

# 本进程正在生成的轮次；取消接口可以立即终止同进程内的上游流。
# 声明 运行中任务列表（_RUNNING_TASKS），类型为 dict[int, asyncio.Task[Any]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
_RUNNING_TASKS: dict[int, asyncio.Task[Any]] = {}
# 声明 后台任务列表（_BACKGROUND_TASKS），类型为 set[asyncio.Task[Any]]；其内容为执行“调用 set 完成设置处理”得到的结果。
_BACKGROUND_TASKS: set[asyncio.Task[Any]] = set()
# 声明 汇总任务列表（_SUMMARY_TASKS），类型为 dict[tuple[int, int], asyncio.Task[Any]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
_SUMMARY_TASKS: dict[tuple[int, int], asyncio.Task[Any]] = {}
# 声明 等待处理SUMMARIES（_PENDING_SUMMARIES），类型为 set[tuple[int, int]]；其内容为执行“调用 set 完成设置处理”得到的结果。
_PENDING_SUMMARIES: set[tuple[int, int]] = set()
# 把假，表示条件不成立或开关关闭保存到 SHUTTINGDOWN（_SHUTTING_DOWN）。
_SHUTTING_DOWN = False


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 StartedTurn，把开始时本轮对话需要的数据与操作放在同一个对象中。
class StartedTurn:
    # 声明 用户编号（user_id），类型为 int。
    user_id: int
    # 声明 对话编号（conversation_id），类型为 int。
    conversation_id: int
    # 声明 本轮对话编号（turn_id），类型为 int。
    turn_id: int
    # 声明 请求编号（request_id），类型为 str。
    request_id: str
    # 声明 本轮对话版本号（turn_version），类型为 int。
    turn_version: int
    # 声明 用户消息no（user_message_no），类型为 int。
    user_message_no: int
    # 声明 assistant消息编号（assistant_message_id），类型为 int。
    assistant_message_id: int
    # 声明 模型（model），类型为 str。
    model: str
    # 声明 汇总through消息no（summary_through_message_no），类型为 int。
    summary_through_message_no: int
    # 声明 上下文汇总（context_summary），类型为 str。
    context_summary: str
    # 声明 截止时间at（deadline_at），类型为 datetime。
    deadline_at: datetime
    # 声明 上下文附加说明（context_metadata），类型为 dict[str, Any]；其内容为执行“声明数据类字段的默认值或创建规则，避免实例之间共用同一个可变对象”得到的结果。
    context_metadata: dict[str, Any] = field(default_factory=dict)

    knowledge_base_ids: list[int] = field(default_factory=list)


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 ReplayedTurn，把Replayed本轮对话需要的数据与操作放在同一个对象中。
class ReplayedTurn:
    # 声明 对话编号（conversation_id），类型为 int。
    conversation_id: int
    # 声明 本轮对话编号（turn_id），类型为 int。
    turn_id: int
    # 声明 请求编号（request_id），类型为 str。
    request_id: str
    # 声明 本轮对话版本号（turn_version），类型为 int。
    turn_version: int
    # 声明 模型（model），类型为 str | None。
    model: str | None
    # 声明 finish原因（finish_reason），类型为 str | None。
    finish_reason: str | None
    # 声明 reply，类型为 str。
    reply: str
    # 声明 用量（usage），类型为 dict[str, int] | None；其内容为空值，表示当前还没有对应内容。
    usage: dict[str, int] | None = None


# 函数 _now：调用 _now 完成当前时间处理。
def _now() -> datetime:
    # 把执行“调用 datetime.now 完成当前时间处理”得到的结果交还给调用方，本函数到此结束。
    return datetime.now(UTC)


# 函数 request_hash：调用 request_hash 完成请求摘要值处理；接收 对话编号（conversation_id）、消息（message）。
def request_hash(conversation_id: int, message: str, knowledge_base_ids: list[int] | None = None) -> str:
    if knowledge_base_ids is not None:
        message += "\nknowledgeBaseIds=" + json.dumps(sorted(set(knowledge_base_ids)))
    return hashlib.sha256(f'{conversation_id}\n{message}'.encode()).hexdigest()


# 函数 _safe_error_code：调用 _safe_error_code 完成safe错误编码处理；接收 值（value）。
def _safe_error_code(value: str | None) -> str:
    # 把条件 value and ERROR_CODE.fullmatch(value) 是否成立时使用值（value），否则使用固定状态或编码 AGENT_REQUEST_FAILED（成长助手请求失败）交还给调用方，本函数到此结束。
    return value if value and ERROR_CODE.fullmatch(value) else "AGENT_REQUEST_FAILED"


# 函数 _safe_finish_reason：调用 _safe_finish_reason 完成safefinish原因处理；接收 值（value）。
def _safe_finish_reason(value: str | None) -> str:
    # 把条件 value and FINISH_REASON.fullmatch(value) 是否成立时使用值（value），否则使用固定文字 'other'交还给调用方，本函数到此结束。
    return value if value and FINISH_REASON.fullmatch(value) else "other"


# 函数 _usage：调用 _usage 完成用量处理；接收 完成情况（completion）。
def _usage(completion: Any) -> dict[str, int] | None:
    # 把执行“读取 attr 的值”得到的结果保存到 用量（usage）。
    usage = getattr(completion, "usage", None)
    # 检查usage is None，即对应内容是否不存在；成立时执行缩进的分支。
    if usage is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写模型提示词凭据处理器（promptTokens）字段，值为表达式 usage.prompt_tokens 的结果。
        "promptTokens": usage.prompt_tokens,
        # 填写完成情况凭据处理器（completionTokens）字段，值为表达式 usage.completion_tokens 的结果。
        "completionTokens": usage.completion_tokens,
        # 填写总数凭据处理器（totalTokens）字段，值为表达式 usage.total_tokens 的结果。
        "totalTokens": usage.total_tokens,
    }


# 异步函数 start_turn：在同一短事务内完成幂等检查、轮次登记、提问与助手占位。；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、消息（message）、请求编号（request_id）。
async def start_turn(db: Database, user_id: int, conversation_id: int, message: str, request_id: UUID, knowledge_base_ids: list[int] | None = None, *, allow_knowledge: bool = True) -> StartedTurn | ReplayedTurn:
    await conversation_service.normalize_expired(db, user_id, conversation_id)
    settings = get_settings()
    now = _now()
    digest = request_hash(conversation_id, message, knowledge_base_ids)
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(session, user_id, conversation_id, for_update=True)
        existing = await turn_repository.find_by_request(session, user_id, request_id)
        if existing is not None:
            if existing.conversation_id != conversation_id or existing.request_hash != digest:
                raise AgentError(409, 'REQUEST_ID_REUSED', '同一请求标识已用于其他会话或内容，请重新发起', {'turnId': str(existing.id), 'status': existing.status})
            if existing.status == 'RUNNING' and now >= existing.deadline_at:
                await turn_repository.finalize(session, turn_id=existing.id, expected_version=existing.version, status='FAILED', finish_reason=None, error_code='AGENT_TIMEOUT')
                await session.refresh(existing)
            if existing.status == 'RUNNING':
                raise AgentError(409, 'TURN_RUNNING', '该请求仍在生成中，请查询原轮次状态', {'turnId': str(existing.id), 'status': existing.status, 'version': existing.version})
            if existing.status == 'COMPLETED':
                assistant = await message_repository.assistant_for_turn(session, user_id=user_id, turn_id=existing.id)
                totals = await message_repository.usage_by_turn(session, user_id, {existing.id})
                return ReplayedTurn(conversation_id=conversation_id, turn_id=existing.id, request_id=str(existing.request_id), turn_version=existing.version, model=assistant.model if assistant else None, finish_reason=existing.finish_reason, reply=assistant.content if assistant else '', usage=totals.get(existing.id))
            raise AgentError(409, 'TURN_NOT_REPLAYABLE', '该请求已结束，不能重复发送；请查询原轮次状态', {'turnId': str(existing.id), 'status': existing.status, 'version': existing.version})
        if conversation.status != 'ACTIVE':
            raise AgentError(409, 'CONVERSATION_CLOSED', '会话已关闭，不能继续提问')
        await turn_repository.expire_running(session, conversation_id, now)
        if await turn_repository.count_running(session, conversation_id) > 0:
            raise AgentError(409, 'TURN_RUNNING', '该会话已有一轮回复正在生成')
        if getattr(db, 'workflow_graph', None) is not None:
            from app.repositories import workflow_repository
            run = await workflow_repository.find_active_run(session, conversation_id)
            phase = ((run.graph_state or {}).get('planning') or {}).get('phase') if run else None
            if run and run.source_turn_id is None and (run.status == 'DRAFT') and (run.expires_at > now) and (phase not in {'ERROR', 'INTERRUPTED'}):
                raise AgentError(409, 'PLANNING_IN_PROGRESS', '正在根据复盘整理方案，请完成后继续对话')
        selected = knowledge_base_ids if knowledge_base_ids is not None else await knowledge_bases.selected_ids(session, user_id, conversation_id)
        if selected and not allow_knowledge:
            raise AgentError(403, "KNOWLEDGE_PERMISSION_REQUIRED", "当前账号没有读取知识库的权限，请清空资料选择后重试")
        selected = await knowledge_bases.validate_selection(session, user_id, selected)
        deadline_at = now + timedelta(seconds=settings.agent_turn_timeout_seconds)
        try:
            turn = await turn_repository.create(session, user_id=user_id, conversation_id=conversation_id, request_id=request_id, request_hash=digest, deadline_at=deadline_at)
            turn.knowledge_base_ids = [str(i) for i in selected]
        except IntegrityError as error:
            if 'uk_agent_turn_user_request' in str(getattr(error, 'orig', '')):
                raise AgentError(409, 'REQUEST_ID_REUSED', '同一请求标识已用于其他请求，请重新发起') from None
            raise AgentError(409, 'TURN_RUNNING', '该会话已有一轮回复正在生成') from None
        (user_row, assistant_row) = await message_repository.insert_pair(session, conversation=conversation, user_id=user_id, turn_id=turn.id, user_content=message, assistant_model=settings.llm_model, now=now)
        return StartedTurn(user_id=user_id, conversation_id=conversation_id, turn_id=turn.id, request_id=str(request_id), turn_version=turn.version, user_message_no=user_row.message_no, assistant_message_id=assistant_row.id, model=settings.llm_model, summary_through_message_no=conversation.summary_through_message_no, context_summary=conversation.context_summary, deadline_at=deadline_at, knowledge_base_ids=selected)


# 异步函数 _messages：调用 _messages 完成消息列表处理；接收 Agent 数据库访问器（db）、开始时（started）、消息（message）。
async def _messages(db: Database, started: StartedTurn, message: str) -> list[dict[str, Any]]:
    return await context_service.build_model_messages(db, user_id=started.user_id, conversation_id=started.conversation_id, summary_through_message_no=started.summary_through_message_no, context_summary=started.context_summary, before_message_no=started.user_message_no, question=message, metadata=started.context_metadata, knowledge_base_ids=started.knowledge_base_ids)


# 异步函数 _save_partial：调用 _save_partial 完成保存部分结果处理；接收 Agent 数据库访问器（db）、开始时（started）、文字（text）、模型（model）。
async def _save_partial(db: Database, started: StartedTurn, text: str, model: str) -> None:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 等待调用 _lock_running 完成加锁运行中处理完成后再继续。
            await _lock_running(session, started)
            # 把等待执行“保存 assistant”得到的结果完成后的结果保存到 已保存（saved）。
            saved = await message_repository.save_assistant(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入消息编号（message_id），值为表达式 started.assistant_message_id 的结果。
                message_id=started.assistant_message_id,
                # 传入用户编号（user_id），值为表达式 started.user_id 的结果。
                user_id=started.user_id,
                # 传入对话编号（conversation_id），值为表达式 started.conversation_id 的结果。
                conversation_id=started.conversation_id,
                # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
                turn_id=started.turn_id,
                # 传入内容（content），值为文字（text）。
                content=text,
                # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
                is_complete=False,
                # 传入finish原因（finish_reason），值为空值，表示当前还没有对应内容。
                finish_reason=None,
                # 传入用量（usage），值为空值，表示当前还没有对应内容。
                usage=None,
                # 传入模型（model），值为模型（model）。
                model=model,
                # 传入当前时间（now），值为调用 _now 完成当前时间处理。
                now=_now(),
            )
            # 检查条件 not saved 是否成立；成立时执行缩进的分支。
            if not saved:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "TURN_STATE_CONFLICT", "回复状态已变化，请刷新会话")
    # 捕获AgentError异常，执行下面的错误处理。
    except AgentError:
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 临时部分保存失败不伪造最终保存成功。
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning(
            # 继续传入或组合固定文字 'agent_partial_save_failed turn=%s type=%s'，完成上面尚未结束的表达式。
            "agent_partial_save_failed turn=%s type=%s",
            # 继续传入或组合表达式 started.turn_id 的结果，完成上面尚未结束的表达式。
            started.turn_id,
            # 继续传入或组合调用 type 完成类型处理，完成上面尚未结束的表达式。
            type(error).__name__,
        )


# 异步函数 _finalize：会话行先于轮次行加锁，避免与取消操作形成死锁。；接收 Agent 数据库访问器（db）、开始时（started）、状态（status）、finish原因（finish_reason）、错误编码（error_code）、文字（text）、用量（usage）、模型（model）。
async def _finalize(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写状态（status）字段，值为str。
    status: str,
    # 填写finish原因（finish_reason）字段，值为表达式 str | None 的结果。
    finish_reason: str | None,
    # 填写错误编码（error_code）字段，值为表达式 str | None 的结果。
    error_code: str | None,
    # 填写文字（text）字段，值为表达式 str | None = None 的结果。
    text: str | None = None,
    # 填写用量（usage）字段，值为表达式 dict[str, int] | None = None 的结果。
    usage: dict[str, int] | None = None,
    # 填写模型（model）字段，值为表达式 str | None = None 的结果。
    model: str | None = None,
# 继续传入或组合表达式 ) -> bool: 的结果，完成上面尚未结束的表达式。
) -> bool:
    # 会话行先于轮次行加锁，避免与取消操作形成死锁。
    # 把执行“调用 _now 完成当前时间处理”得到的结果保存到 当前时间（now）。
    now = _now()
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“要求满足 owned”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await conversation_repository.require_owned(
            # 继续传入或组合表达式 session, started.user_id, started.conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, started.user_id, started.conversation_id, for_update=True
        )
        # 把等待执行“调用 turn_repository.finalize 完成finalize处理”得到的结果完成后的结果保存到 已保存（saved）。
        saved = await turn_repository.finalize(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
            turn_id=started.turn_id,
            # 传入预计版本号（expected_version），值为表达式 started.turn_version 的结果。
            expected_version=started.turn_version,
            # 传入状态（status），值为状态（status）。
            status=status,
            # 传入finish原因（finish_reason），值为finish原因（finish_reason）。
            finish_reason=finish_reason,
            # 传入错误编码（error_code），值为错误编码（error_code）。
            error_code=error_code,
        )
        # 检查条件 not saved 是否成立；成立时执行缩进的分支。
        if not saved:
            # 把假，表示条件不成立或开关关闭交还给调用方，本函数到此结束。
            return False
        # 检查条件 text is not None 是否成立；成立时执行缩进的分支。
        if text is not None:
            # 把等待执行“保存 assistant”得到的结果完成后的结果保存到 消息已保存（message_saved）。
            message_saved = await message_repository.save_assistant(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入消息编号（message_id），值为表达式 started.assistant_message_id 的结果。
                message_id=started.assistant_message_id,
                # 传入用户编号（user_id），值为表达式 started.user_id 的结果。
                user_id=started.user_id,
                # 传入对话编号（conversation_id），值为表达式 started.conversation_id 的结果。
                conversation_id=started.conversation_id,
                # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
                turn_id=started.turn_id,
                # 传入内容（content），值为文字（text）。
                content=text,
                # 传入是否完成（is_complete），值为条件判断的结果：status == "COMPLETED"。
                is_complete=status == "COMPLETED",
                # 传入finish原因（finish_reason），值为finish原因（finish_reason）。
                finish_reason=finish_reason,
                # 传入用量（usage），值为用量（usage）。
                usage=usage,
                # 传入模型（model），值为优先使用前面的有效值，没有时采用后面的备用值：model or started.model。
                model=model or started.model,
                # 传入当前时间（now），值为当前时间（now）。
                now=now,
            )
            # 检查条件 not message_saved 是否成立；成立时执行缩进的分支。
            if not message_saved:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "TURN_STATE_CONFLICT", "回复保存状态已变化，请刷新会话")

        # 从 app.db.models 引入 对话消息（ConversationMessage）。
        from app.db.models import ConversationMessage

        # 从 app.repositories 引入 后台任务数据仓库（background_task_repository）。
        from app.repositories import background_task_repository
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
        row = await session.get(ConversationMessage, started.assistant_message_id)
        # 检查条件 row is not None 是否成立；成立时执行缩进的分支。
        if row is not None:
            # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 当前记录中的citations（row.citations）。
            row.citations = started.context_metadata.get("citations", [])
            # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 当前记录中的used长期记忆编号列表（row.used_memory_ids）。
            row.used_memory_ids = started.context_metadata.get("memoryIds", [])
        # 检查条件 status == 'COMPLETED' 是否成立；成立时执行缩进的分支。
        if status == "COMPLETED":
            # 等待调用 background_task_repository.enqueue 完成enqueue处理完成后再继续。
            await background_task_repository.enqueue(
                # 继续传入或组合表达式 session, user_id=started.user_id, kind="MEMORY_EXTRACT" 的结果，完成上面尚未结束的表达式。
                session, user_id=started.user_id, kind="MEMORY_EXTRACT",
                # 传入响应式引用类型（ref_type），值为表达式 "CONVERSATION_TURN", ref_id=started.turn_id 的结果。
                ref_type="CONVERSATION_TURN", ref_id=started.turn_id,
            )
        # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的最后一次消息at（conversation.last_message_at）。
        conversation.last_message_at = _conversation_now()
        # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
        conversation.updated_at = _conversation_now()
        # 把 conversation.version 加上 固定数值 1，再保存回原变量。
        conversation.version += 1
        # 把真，表示条件成立或开关打开交还给调用方，本函数到此结束。
        return True


# 函数 _schedule_summary：调用 _schedule_summary 完成安排汇总处理；接收 Agent 数据库访问器（db）、开始时（started）、slots。
def _schedule_summary(db: Database, started: StartedTurn, slots: asyncio.Semaphore) -> None:
    # 检查条件 _SHUTTING_DOWN 是否成立；成立时执行缩进的分支。
    if _SHUTTING_DOWN:
        # 把空值交还给调用方，本函数到此结束。
        return
    # 把依次包含 开始时（started）的用户编号（user_id）属性、开始时（started）的对话编号（conversation_id）属性 的集合保存到 键（key）。
    key = (started.user_id, started.conversation_id)
    # 检查条件 key in _SUMMARY_TASKS and (not _SUMMARY_TASKS[key].done()) 是否成立；成立时执行缩进的分支。
    if key in _SUMMARY_TASKS and not _SUMMARY_TASKS[key].done():
        # 加入集合或登记一条待保存记录。
        _PENDING_SUMMARIES.add(key)
        # 把空值交还给调用方，本函数到此结束。
        return
    # 把执行“启动可同时推进的异步任务，并保存任务句柄”得到的结果保存到 任务（task）。
    task = asyncio.create_task(
        # 继续传入或组合调用 summary_service.run_summary_for_conversation 完成方案运行汇总for对话处理，完成上面尚未结束的表达式。
        summary_service.run_summary_for_conversation(
            # 继续传入或组合表达式 db, started.user_id, started.conversation_id, slots 的结果，完成上面尚未结束的表达式。
            db, started.user_id, started.conversation_id, slots
        ),
        # 传入名称（name），值为表达式 f"agent-summary-{started.conversation_id}" 的结果。
        name=f"agent-summary-{started.conversation_id}",
    )
    # 加入集合或登记一条待保存记录。
    _BACKGROUND_TASKS.add(task)
    # 把任务（task）保存到 汇总任务列表键（_SUMMARY_TASKS[key]）。
    _SUMMARY_TASKS[key] = task
    # 调用 task.add_done_callback 完成加入已完成callback处理。
    task.add_done_callback(_BACKGROUND_TASKS.discard)

    # 函数 completed：调用 completed 完成已完成处理；接收 已完成（done）。
    def completed(done: asyncio.Task[Any]) -> None:
        # 检查条件 _SUMMARY_TASKS.get(key) is done 是否成立；成立时执行缩进的分支。
        if _SUMMARY_TASKS.get(key) is done:
            # 调用 _SUMMARY_TASKS.pop 完成pop处理。
            _SUMMARY_TASKS.pop(key, None)
        # 检查条件 key in _PENDING_SUMMARIES 是否成立；成立时执行缩进的分支。
        if key in _PENDING_SUMMARIES:
            # 调用 _PENDING_SUMMARIES.discard 完成discard处理。
            _PENDING_SUMMARIES.discard(key)
            # 检查条件 not done.cancelled() 是否成立；成立时执行缩进的分支。
            if not done.cancelled():
                # 调用 _schedule_summary 完成安排汇总处理。
                _schedule_summary(db, started, slots)

    # 调用 task.add_done_callback 完成加入已完成callback处理。
    task.add_done_callback(completed)


# 异步函数 shutdown_generation：先取消生成，再收敛派生补存任务；数据库与模型连接最后关闭。
async def shutdown_generation() -> None:
    # 先取消生成，再收敛派生补存任务；数据库与模型连接最后关闭。
    # 下面对 _SHUTTING_DOWN 的赋值使用模块共享变量。
    global _SHUTTING_DOWN
    # 把真，表示条件成立或开关打开保存到 SHUTTINGDOWN（_SHUTTING_DOWN）。
    _SHUTTING_DOWN = True
    # 调用 _PENDING_SUMMARIES.clear 完成清除处理。
    _PENDING_SUMMARIES.clear()
    # 从执行“调用 range 完成区间处理”得到的结果依次取出（_），每项执行一次下面的处理。
    for _ in range(2):
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 任务列表（tasks）。
        tasks = {
            # 继续传入或组合任务（task），完成上面尚未结束的表达式。
            task
            # 继续传入或组合调用 for task in (*_RUNNING_TASKS.values 完成值列表处理，完成上面尚未结束的表达式。
            for task in (*_RUNNING_TASKS.values(), *_BACKGROUND_TASKS)
            # 继续传入或组合调用 if not task.done 完成已完成处理，完成上面尚未结束的表达式。
            if not task.done() and task is not asyncio.current_task()
        }
        # 检查条件 not tasks 是否成立；成立时执行缩进的分支。
        if not tasks:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 从任务列表（tasks）依次取出任务（task），每项执行一次下面的处理。
        for task in tasks:
            # 调用 task.cancel 完成取消处理。
            task.cancel()
        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with suppress(TimeoutError):
            # 等待等待异步处理，但不超过指定的超时时间完成后再继续。
            await asyncio.wait_for(asyncio.gather(*tasks, return_exceptions=True), timeout=5.0)


# 异步函数 _mark_cancelled：调用 _mark_cancelled 完成mark已取消处理；接收 Agent 数据库访问器（db）、开始时（started）、文字（text）、模型（model）。
async def _mark_cancelled(db: Database, started: StartedTurn, text: str, model: str) -> None:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“要求满足 owned”得到的结果完成后的结果保存到 对话（conversation）。
            conversation = await conversation_repository.require_owned(
                # 继续传入或组合表达式 session, started.user_id, started.conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
                session, started.user_id, started.conversation_id, for_update=True
            )
            # 把等待执行“查找 owned”得到的结果完成后的结果保存到 本轮对话（turn）。
            turn = await turn_repository.find_owned(
                # 继续传入或组合表达式 session, started.user_id, started.conversation_id, started.turn_id 的结果，完成上面尚未结束的表达式。
                session, started.user_id, started.conversation_id, started.turn_id
            )
            # 检查turn is None，即对应内容是否不存在；成立时执行缩进的分支。
            if turn is None:
                # 把空值交还给调用方，本函数到此结束。
                return
            # 检查当前记录版本是否符合请求要求：turn.status == 'RUNNING' and turn.version == started.turn_version；成立时执行缩进的分支。
            if turn.status == "RUNNING" and turn.version == started.turn_version:
                # 等待调用 turn_repository.cancel 完成取消处理完成后再继续。
                await turn_repository.cancel(
                    # 继续传入或组合表达式 session, turn_id=turn.id, expected_version=turn.version 的结果，完成上面尚未结束的表达式。
                    session, turn_id=turn.id, expected_version=turn.version
                )
            # 检查当前记录版本是否符合请求要求：not (turn.status == 'CANCELLED' and turn.version == started.turn_version + 1)；成立时执行缩进的分支。
            elif not (turn.status == "CANCELLED" and turn.version == started.turn_version + 1):
                # 把空值交还给调用方，本函数到此结束。
                return
            # 显式取消可能已先提交；只补存本生产任务收到的部分文本，不重写终态时间。
            # 等待保存 assistant完成后再继续。
            await message_repository.save_assistant(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入消息编号（message_id），值为表达式 started.assistant_message_id 的结果。
                message_id=started.assistant_message_id,
                # 传入用户编号（user_id），值为表达式 started.user_id 的结果。
                user_id=started.user_id,
                # 传入对话编号（conversation_id），值为表达式 started.conversation_id 的结果。
                conversation_id=started.conversation_id,
                # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
                turn_id=started.turn_id,
                # 传入内容（content），值为文字（text）。
                content=text,
                # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
                is_complete=False,
                # 传入finish原因（finish_reason），值为空值，表示当前还没有对应内容。
                finish_reason=None,
                # 传入用量（usage），值为空值，表示当前还没有对应内容。
                usage=None,
                # 传入模型（model），值为模型（model）。
                model=model,
                # 传入当前时间（now），值为调用 _now 完成当前时间处理。
                now=_now(),
            )
            # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的更新时at（conversation.updated_at）。
            conversation.updated_at = _conversation_now()
            # 把执行“调用 _conversation_now 完成对话当前时间处理”得到的结果保存到 对话中的最后一次消息at（conversation.last_message_at）。
            conversation.last_message_at = _conversation_now()
            # 把 conversation.version 加上 固定数值 1，再保存回原变量。
            conversation.version += 1
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 断开后的补存尽力完成，截止归一仍是兜底。
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning(
            # 继续传入或组合表达式 "agent_cancel_save_failed turn=%s type=%s", started.turn_id, type(error).__name__ 的结果，完成上面尚未结束的表达式。
            "agent_cancel_save_failed turn=%s type=%s", started.turn_id, type(error).__name__
        )


# 函数 _best_effort_cancel：调用 _best_effort_cancel 完成besteffort取消处理；接收 Agent 数据库访问器（db）、开始时（started）、文字（text）、模型（model）。
def _best_effort_cancel(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, started: StartedTurn, text: str = "", model: str | None = None 的结果。
    db: Database, started: StartedTurn, text: str = "", model: str | None = None
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 把执行“启动可同时推进的异步任务，并保存任务句柄”得到的结果保存到 任务（task）。
    task = asyncio.create_task(
        # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：_mark_cancelled(db, started, text, model or started.model), name="agent-turn-cancel"，完成上面尚未结束的表达式。
        _mark_cancelled(db, started, text, model or started.model), name="agent-turn-cancel"
    )
    # 加入集合或登记一条待保存记录。
    _BACKGROUND_TASKS.add(task)
    # 调用 task.add_done_callback 完成加入已完成callback处理。
    task.add_done_callback(_BACKGROUND_TASKS.discard)


# 异步函数 _lock_running：调用 _lock_running 完成加锁运行中处理；接收 当前会话（session）、开始时（started）。
async def _lock_running(session: AsyncSession, started: StartedTurn) -> Conversation:
    # 把等待执行“要求满足 owned”得到的结果完成后的结果保存到 对话（conversation）。
    conversation = await conversation_repository.require_owned(
        # 继续传入或组合表达式 session, started.user_id, started.conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
        session, started.user_id, started.conversation_id, for_update=True
    )
    # 把等待执行“查找 owned”得到的结果完成后的结果保存到 本轮对话（turn）。
    turn = await turn_repository.find_owned(
        # 继续传入或组合表达式 session, started.user_id, started.conversation_id, started.turn_id 的结果，完成上面尚未结束的表达式。
        session, started.user_id, started.conversation_id, started.turn_id
    )
    # 检查当前记录版本是否符合请求要求：turn is None or turn.status != 'RUNNING' or turn.version != started.turn_version or (conversation.status != 'ACTIVE')；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 turn is None 的结果，完成上面尚未结束的表达式。
        turn is None
        # 继续补充本次调用或判断：条件判断的结果：or turn.status != "RUNNING"。
        or turn.status != "RUNNING"
        # 继续补充本次调用或判断：条件判断的结果：or turn.version != started.turn_version。
        or turn.version != started.turn_version
        # 继续补充本次调用或判断：条件判断的结果：or conversation.status != "ACTIVE"。
        or conversation.status != "ACTIVE"
    ):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "TURN_STATE_CONFLICT", "轮次已停止或状态变化，请刷新会话")
    # 检查条件 _now() >= turn.deadline_at 是否成立；成立时执行缩进的分支。
    if _now() >= turn.deadline_at:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(504, "AGENT_TIMEOUT", "回复已到达截止时间，请重新提问")
    # 把对话（conversation）交还给调用方，本函数到此结束。
    return conversation


# 异步函数 _prepare_tools：调用 _prepare_tools 完成prepare业务工具列表处理；接收 Agent 数据库访问器（db）、开始时（started）、内容（content）、调用列表（calls）、模型（model）、用量（usage）。
async def _prepare_tools(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 填写内容（content）字段，值为str。
    content: str,
    # 填写调用列表（calls）字段，值为表达式 list[ToolCall] 的结果。
    calls: list[ToolCall],
    # 填写模型（model）字段，值为str。
    model: str,
    # 填写用量（usage）字段，值为表达式 dict[str, int] | None 的结果。
    usage: dict[str, int] | None,
# 继续传入或组合表达式 ) -> message_repository.ToolRoundReservation: 的结果，完成上面尚未结束的表达式。
) -> message_repository.ToolRoundReservation:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“调用 _lock_running 完成加锁运行中处理”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await _lock_running(session, started)
        # 把等待执行“调用 message_repository.begin_tool_round 完成begin业务工具轮处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await message_repository.begin_tool_round(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入对话（conversation），值为对话（conversation）。
            conversation=conversation,
            # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
            turn_id=started.turn_id,
            # 传入assistant消息编号（assistant_message_id），值为表达式 started.assistant_message_id 的结果。
            assistant_message_id=started.assistant_message_id,
            # 传入内容（content），值为内容（content）。
            content=content,
            # 传入调用列表（calls），值为调用 assistant_tool_message 完成assistant业务工具消息处理。
            calls=assistant_tool_message(content, calls)["tool_calls"],
            # 传入模型（model），值为模型（model）。
            model=model,
            # 传入用量（usage），值为用量（usage）。
            usage=usage,
        )


# 异步函数 _record_tool_result：调用 _record_tool_result 完成记录业务工具结果处理；接收 Agent 数据库访问器（db）、开始时（started）、reservation、数值（number）、调用（call）、结果（result）。
async def _record_tool_result(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 填写reservation字段，值为表达式 message_repository.ToolRoundReservation 的结果。
    reservation: message_repository.ToolRoundReservation,
    # 填写数值（number）字段，值为int。
    number: int,
    # 填写调用（call）字段，值为业务工具调用（ToolCall）。
    call: ToolCall,
    # 填写结果（result）字段，值为业务工具执行计时（ToolExecution）。
    result: ToolExecution,
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“调用 _lock_running 完成加锁运行中处理”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await _lock_running(session, started)
        # 等待调用 message_repository.insert_tool_result 完成写入新记录业务工具结果处理完成后再继续。
        await message_repository.insert_tool_result(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入对话（conversation），值为对话（conversation）。
            conversation=conversation,
            # 传入本轮对话编号（turn_id），值为表达式 started.turn_id 的结果。
            turn_id=started.turn_id,
            # 传入请求消息编号（request_message_id），值为表达式 reservation.request_message_id 的结果。
            request_message_id=reservation.request_message_id,
            # 传入消息no（message_no），值为数值（number）。
            message_no=number,
            # 传入调用编号（call_id），值为表达式 call.id 的结果。
            call_id=call.id,
            # 传入名称（name），值为表达式 result.name 的结果。
            name=result.name,
            # 传入内容（content），值为表达式 result.content 的结果。
            content=result.content,
        )


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 GeneratedReply，把GeneratedReply需要的数据与操作放在同一个对象中。
class GeneratedReply:
    # 声明 reply，类型为 str。
    reply: str
    # 声明 模型（model），类型为 str。
    model: str
    # 声明 finish原因（finish_reason），类型为 str。
    finish_reason: str
    # 声明 用量（usage），类型为 dict[str, int] | None。
    usage: dict[str, int] | None


# 函数 _sum_usage：调用 _sum_usage 完成sum用量处理；接收 总数（total）、当前（current）。
def _sum_usage(
    # 填写总数（total）字段，值为表达式 dict[str, int] | None, current: dict[str, int] | None 的结果。
    total: dict[str, int] | None, current: dict[str, int] | None
# 继续传入或组合表达式 ) -> dict[str, int] | None: 的结果，完成上面尚未结束的表达式。
) -> dict[str, int] | None:
    # 检查current is None，即对应内容是否不存在；成立时执行缩进的分支。
    if current is None:
        # 把总数（total）交还给调用方，本函数到此结束。
        return total
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 结果（result）。
    result = dict(total or {})
    # 从依次包含 固定文字 'promptTokens'、固定文字 'completionTokens'、固定文字 'totalTokens' 的集合依次取出键（key），每项执行一次下面的处理。
    for key in ("promptTokens", "completionTokens", "totalTokens"):
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 值（value）。
        value = current.get(key)
        # 检查条件 isinstance(value, int) and (not isinstance(value, bool)) and (value >= 0) 是否成立；成立时执行缩进的分支。
        if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
            # 把按 result.get(key, 0) + value 计算的结果保存到 结果键（result[key]）。
            result[key] = result.get(key, 0) + value
    # 把结果（result）交还给调用方，本函数到此结束。
    return result


# 异步函数 _generate：普通与流式请求共用模型循环、工具持久化、权限复核和终态提交。；接收 Agent 数据库访问器（db）、上下文（context）、开始时（started）、消息（message）、Java后端客户端（java_client）、slots、向父组件发送事件的函数（emit）。
async def _generate(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 填写消息（message）字段，值为str。
    message: str,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写slots字段，值为表达式 asyncio.Semaphore 的结果。
    slots: asyncio.Semaphore,
    # 填写向父组件发送事件的函数（emit）字段，值为表达式 Callable[[str, dict[str, Any]], Awaitable[None]] | None = None 的结果。
    emit: Callable[[str, dict[str, Any]], Awaitable[None]] | None = None,
# 继续传入或组合表达式 ) -> GeneratedReply: 的结果，完成上面尚未结束的表达式。
) -> GeneratedReply:
    # 普通与流式请求共用模型循环、工具持久化、权限复核和终态提交。
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把开始时（started）保存到 页面是否仍处于活动状态（active）。
    active = started
    # 把空值，表示当前还没有对应内容保存到 upstream。
    upstream = None
    # 声明 parts，类型为 list[str]；其内容为空列表或集合。
    parts: list[str] = []
    # 把开始时（started）的模型（model）属性保存到 模型（model）。
    model = started.model
    # 声明 轮用量（round_usage），类型为 dict[str, int] | None；其内容为空值，表示当前还没有对应内容。
    round_usage: dict[str, int] | None = None
    # 声明 总数用量（total_usage），类型为 dict[str, int] | None；其内容为空值，表示当前还没有对应内容。
    total_usage: dict[str, int] | None = None
    # 把固定数值 0保存到 业务工具rounds（tool_rounds）、emittedchars（emitted_chars）。
    tool_rounds = emitted_chars = 0
    # 把执行“读取 额度（Budget）”得到的结果保存到 读取额度（read_budget）。
    read_budget = ReadBudget(settings.agent_tool_max_calls, settings.agent_tool_max_rounds)
    # 把假，表示条件不成立或开关关闭保存到 规划used（planning_used）。
    planning_used = False
    # 把假，表示条件不成立或开关关闭保存到 复盘used（review_used）。
    review_used = False
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 最后一次保存（last_save）。
    last_save = time.monotonic()
    # 把执行“调用 min 完成最小处理”得到的结果保存到 截止时间（deadline）。
    deadline = min(
        # 继续传入或组合调用 context.deadline, time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
        context.deadline, time.monotonic() + max(0, (started.deadline_at - _now()).total_seconds())
    )

    # 异步函数 publish：调用 publish 完成publish处理；接收 事件（event）、数据（data）。
    async def publish(event: str, data: dict[str, Any]) -> None:
        # 检查条件 emit is not None 是否成立；成立时执行缩进的分支。
        if emit is not None:
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit(event, data)

    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 asyncio.timeout_at 完成超时at处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        async with asyncio.timeout_at(deadline):
            # 等待调用 publish 完成publish处理完成后再继续。
            await publish("activity", {"id": "context", "label": "读取会话上下文", "status": "running"})
            # 把等待执行“调用 _messages 完成消息列表处理”得到的结果完成后的结果保存到 消息列表（messages）。
            messages = await _messages(db, started, message)
            # 检查条件 conversation_review.available(db, context) 是否成立；成立时执行缩进的分支。
            if conversation_review.available(db, context):
                # 把等待执行“调用 conversation_review.context_hint 完成上下文hint处理”得到的结果完成后的结果保存到 复盘hint（review_hint）。
                review_hint = await conversation_review.context_hint(db, started.user_id, started.conversation_id)
                # 检查条件 review_hint 是否成立；成立时执行缩进的分支。
                if review_hint:
                    # 调用 messages.insert 完成写入新记录处理。
                    messages.insert(len(messages) - 1, {"role": "user", "content": review_hint})
            # 检查条件 conversation_planning.available(db, context) 是否成立；成立时执行缩进的分支。
            if conversation_planning.available(db, context):
                # 把等待执行“调用 conversation_planning.context_hint 完成上下文hint处理”得到的结果完成后的结果保存到 hint。
                hint = await conversation_planning.context_hint(db, started.user_id,
                                                                  # 继续传入或组合表达式 started.conversation_id) 的结果，完成上面尚未结束的表达式。
                                                                  started.conversation_id)
                # 检查条件 hint 是否成立；成立时执行缩进的分支。
                if hint:
                    # 调用 messages.insert 完成写入新记录处理。
                    messages.insert(len(messages) - 1, {"role": "user", "content": hint})
            # 等待调用 publish 完成publish处理完成后再继续。
            await publish("activity", {"id": "context", "label": "读取会话上下文", "status": "ok",
                # 填写汇总（summary）字段，值为表达式 "已加载本轮可用的会话背景"}) 的结果。
                "summary": "已加载本轮可用的会话背景"})
            # 等待调用 publish 完成publish处理完成后再继续。
            await publish("activity", {"id": "reply", "label": "组织回复", "status": "running"})
            # 只要条件 True 是否成立，就继续执行下面的循环。
            while True:
                # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 当前（current）。
                current = await refresh_context(context, java_client)
                # 把条件 read_budget.available 是否成立时使用执行“调用 tool_definitions 完成业务工具definitions处理”得到的结果，否则使用空列表或集合保存到 definitions。
                definitions = tool_definitions(current.permissions) if read_budget.available else []
                # 检查条件 conversation_planning.available(db, current) and (not planning_used) 是否成立；成立时执行缩进的分支。
                if conversation_planning.available(db, current) and not planning_used:
                    # 把新条目放到列表末尾。
                    definitions.append(conversation_planning.definition())
                # 检查条件 conversation_review.available(db, current) and (not review_used) 是否成立；成立时执行缩进的分支。
                if conversation_review.available(db, current) and not review_used:
                    # 把新条目放到列表末尾。
                    definitions.append(conversation_review.definition())
                # 把执行“调用 bool 完成bool处理”得到的结果并且配置（settings）的成长助手业务工具列表是否启用（agent_tools_enabled）属性并且条件 tool_rounds < settings.agent_tool_max_rounds + 2 是否成立保存到 use业务工具列表（use_tools）。
                use_tools = (
                    # 继续传入或组合调用 bool 完成bool处理，完成上面尚未结束的表达式。
                    bool(definitions)
                    # 继续补充本次调用或判断：表达式 and settings.agent_tools_enabled 的结果。
                    and settings.agent_tools_enabled
                    # 继续补充本次调用或判断：条件判断的结果：and tool_rounds < settings.agent_tool_max_rounds + 2。
                    and tool_rounds < settings.agent_tool_max_rounds + 2
                )
                # 把依次包含 从消息列表（messages）中按固定数值 0取出的内容、按字段名保存多个值的字典、按表达式计算得到的值：*messages[1:] 的集合保存到 请求消息列表（request_messages）。
                request_messages = [messages[0], {"role": "system", "content": read_budget.hint()}, *messages[1:]]
                # 声明 请求（request），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
                request: dict[str, Any] = {
                    # 继续传入或组合按表达式计算得到的值：**model_options(request_messages)，完成上面尚未结束的表达式。
                    **model_options(request_messages),
                    # 填写分段回复流（stream）字段，值为真，表示条件成立或开关打开。
                    "stream": True,
                    # 填写分段回复流选项（stream_options）字段，值为表达式 {"include_usage": True} 的结果。
                    "stream_options": {"include_usage": True},
                }
                # 检查条件 use_tools 是否成立；成立时执行缩进的分支。
                if use_tools:
                    # 调用 request.update 完成更新处理。
                    request.update(tools=definitions, tool_choice="auto")
                # 检查条件 definitions and settings.agent_tools_enabled 是否成立；成立时执行缩进的分支。
                elif definitions and settings.agent_tools_enabled:
                    # 最后一轮显式禁止工具，不能通过省略 tools 隐式重启查询。
                    # 调用 request.update 完成更新处理。
                    request.update(tools=definitions, tool_choice="none")
                # 把空列表或集合保存到 parts。
                parts = []
                # 声明 调用列表按索引（calls_by_index），类型为 dict[int, dict[str, str]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
                calls_by_index: dict[int, dict[str, str]] = {}
                # 把空值，表示当前还没有对应内容保存到 finish原因（finish_reason）。
                finish_reason = None
                # 把空值，表示当前还没有对应内容保存到 轮用量（round_usage）。
                round_usage = None
                # 异步进入slots；退出时由上下文管理器提交、回滚或释放资源。
                async with slots:
                    # 把等待执行“调用 get_llm_client().chat.completions.create 完成创建处理”得到的结果完成后的结果保存到 upstream。
                    upstream = await get_llm_client().chat.completions.create(**request)
                    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
                    try:
                        # 从upstream依次取出文档片段（chunk），每项执行一次下面的处理。
                        async for chunk in upstream:
                            # 把文档片段（chunk）的模型（model）属性或者模型（model）保存到 模型（model）。
                            model = chunk.model or model
                            # 检查条件 chunk.usage is not None 是否成立；成立时执行缩进的分支。
                            if chunk.usage is not None:
                                # 用量是本次调用的累计快照，只在调用结束后合并一次。
                                # 把执行“调用 _usage 完成用量处理”得到的结果保存到 轮用量（round_usage）。
                                round_usage = _usage(chunk)
                            # 检查条件 not chunk.choices 是否成立；成立时执行缩进的分支。
                            if not chunk.choices:
                                # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                                continue
                            # 把从文档片段（chunk）的choices属性中按固定数值 0取出的内容保存到 choice。
                            choice = chunk.choices[0]
                            # 检查条件 choice.delta.content 是否成立；成立时执行缩进的分支。
                            if choice.delta.content:
                                # 把choice的delta属性的内容（content）属性保存到 文字（text）。
                                text = choice.delta.content
                                # 把 emitted_chars 加上 执行“调用 len 完成len处理”得到的结果，再保存回原变量。
                                emitted_chars += len(text)
                                # 检查条件 emitted_chars > 64000 是否成立；成立时执行缩进的分支。
                                if emitted_chars > 64000:
                                    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                                    raise AgentError(
                                        # 继续传入或组合固定数值 502，完成上面尚未结束的表达式。
                                        502,
                                        # 继续传入或组合固定状态或编码 LLM_REPLY_TOO_LARGE（语言模型REPLYTOOLARGE），完成上面尚未结束的表达式。
                                        "LLM_REPLY_TOO_LARGE",
                                        # 继续传入或组合固定文字 '本轮输出超过长度预算，请缩小提问范围'，完成上面尚未结束的表达式。
                                        "本轮输出超过长度预算，请缩小提问范围",
                                    )
                                # 把新条目放到列表末尾。
                                parts.append(text)
                                # 等待调用 publish 完成publish处理完成后再继续。
                                await publish("delta", {"text": text})
                            # 检查条件 choice.delta.tool_calls 是否成立；成立时执行缩进的分支。
                            if choice.delta.tool_calls:
                                # 调用 merge_tool_deltas 完成merge业务工具deltas处理。
                                merge_tool_deltas(calls_by_index, choice.delta.tool_calls)
                            # 检查条件 choice.finish_reason 是否成立；成立时执行缩进的分支。
                            if choice.finish_reason:
                                # 把choice的finish原因（finish_reason）属性保存到 finish原因（finish_reason）。
                                finish_reason = choice.finish_reason
                            # 检查条件 time.monotonic() - last_save >= settings.agent_partial_save_interval_seconds 是否成立；成立时执行缩进的分支。
                            if (
                                # 继续传入或组合调用 time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
                                time.monotonic() - last_save
                                # 继续传入或组合条件判断的结果：>= settings.agent_partial_save_interval_seconds，完成上面尚未结束的表达式。
                                >= settings.agent_partial_save_interval_seconds
                            ):
                                # 等待调用 _save_partial 完成保存部分结果处理完成后再继续。
                                await _save_partial(db, active, "".join(parts), model)
                                # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 最后一次保存（last_save）。
                                last_save = time.monotonic()
                    # 不论前面成功、失败还是取消，都执行下面的资源清理。
                    finally:
                        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
                        with suppress(Exception, asyncio.CancelledError):
                            # 等待释放连接、文件或客户端占用的资源完成后再继续。
                            await upstream.close()
                        # 把空值，表示当前还没有对应内容保存到 upstream。
                        upstream = None
                # 把执行“调用 _sum_usage 完成sum用量处理”得到的结果保存到 总数用量（total_usage）。
                total_usage = _sum_usage(total_usage, round_usage)
                # 检查条件 not finish_reason 是否成立；成立时执行缩进的分支。
                if not finish_reason:
                    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                    raise AgentError(502, "LLM_INCOMPLETE_REPLY", "回复意外中断，请重新提问")
                # 检查条件 not calls_by_index 是否成立；成立时执行缩进的分支。
                if not calls_by_index:
                    # 检查条件 finish_reason == 'tool_calls' 是否成立；成立时执行缩进的分支。
                    if finish_reason == "tool_calls":
                        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                        raise AgentError(502, "LLM_TOOL_CALL_INVALID", "模型没有返回完整的查询请求")
                    # 立即退出当前循环，不再处理后续条目。
                    break
                # 检查条件 not use_tools or finish_reason != 'tool_calls' 是否成立；成立时执行缩进的分支。
                if not use_tools or finish_reason != "tool_calls":
                    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                    raise AgentError(
                        # 继续传入或组合表达式 502, "LLM_TOOL_CALL_INCOMPLETE", "模型查询请求被截断或超过预算，未执行查询" 的结果，完成上面尚未结束的表达式。
                        502, "LLM_TOOL_CALL_INCOMPLETE", "模型查询请求被截断或超过预算，未执行查询"
                    )
                # 把执行“组装 业务工具调用列表（tool_calls）”得到的结果保存到 调用列表（calls）。
                calls = build_tool_calls(calls_by_index)
                # 把执行“用指定分隔符把多段文字合起来”得到的结果保存到 内容（content）。
                content = "".join(parts)
                # 把等待执行“调用 _prepare_tools 完成prepare业务工具列表处理”得到的结果完成后的结果保存到 reservation。
                reservation = await _prepare_tools(db, active, content, calls, model, round_usage)
                # 把新条目放到列表末尾。
                messages.append(assistant_tool_message(content, calls))
                # 把执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果保存到 页面是否仍处于活动状态（active）。
                active = replace(active, assistant_message_id=reservation.next_assistant_id)
                # 把空列表或集合保存到 parts。
                parts = []
                # 把空值，表示当前还没有对应内容保存到 轮用量（round_usage）。
                round_usage = None
                # 把假，表示条件不成立或开关关闭保存到 queried。
                queried = False
                # 从执行“调用 enumerate 完成enumerate处理”得到的结果依次取出索引调用（(index, call)），每项执行一次下面的处理。
                for index, call in enumerate(calls):
                    # 等待调用 publish 完成publish处理完成后再继续。
                    await publish(
                        # 继续传入或组合固定文字 'tool_start'，完成上面尚未结束的表达式。
                        "tool_start",
                        {
                            # 填写编号（id）字段，值为表达式 call.id 的结果。
                            "id": call.id,
                            # 填写名称（name）字段，值为表达式 call.name 的结果。
                            "name": call.name,
                            # 填写显示名称（label）字段，值为条件判断的结果：conversation_planning.TOOL_LABEL if call.name == conversation_planning.TOOL_NAME。
                            "label": conversation_planning.TOOL_LABEL if call.name == conversation_planning.TOOL_NAME
                                # 继续传入或组合条件判断的结果：else conversation_review.TOOL_LABEL if call.name == conversation_review.TOOL_NAME，完成上面尚未结束的表达式。
                                else conversation_review.TOOL_LABEL if call.name == conversation_review.TOOL_NAME
                                # 继续传入或组合调用 else tool_service.label_for 完成显示名称for处理，完成上面尚未结束的表达式。
                                else tool_service.label_for(call.name),
                            # 填写resetReply字段，值为条件判断的结果：index == 0。
                            "resetReply": index == 0,
                        },
                    )
                    # 检查条件 call.name == conversation_planning.TOOL_NAME 是否成立；成立时执行缩进的分支。
                    if call.name == conversation_planning.TOOL_NAME:
                        # 检查条件 planning_used or not conversation_planning.available(db, current) 是否成立；成立时执行缩进的分支。
                        if planning_used or not conversation_planning.available(db, current):
                            # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果保存到 结果（result）。
                            result = ToolExecution(call.name, conversation_planning.TOOL_LABEL,
                                # 继续传入或组合固定文字 '{"error":"本轮已整理过规划或当前无权限，请基于已有结果回复"}'，完成上面尚未结束的表达式。
                                '{"error":"本轮已整理过规划或当前无权限，请基于已有结果回复"}',
                                # 继续传入或组合表达式 "本轮不重复生成方案", True, 0) 的结果，完成上面尚未结束的表达式。
                                "本轮不重复生成方案", True, 0)
                        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
                        else:
                            # 把真，表示条件成立或开关打开保存到 规划used（planning_used）。
                            planning_used = True
                            # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
                            result = await conversation_planning.execute(db, current, java_client,
                                # 传入对话编号（conversation_id），值为表达式 started.conversation_id, turn_id=started.turn_id 的结果。
                                conversation_id=started.conversation_id, turn_id=started.turn_id,
                                # 传入arguments，值为表达式 call.arguments, messages=messages 的结果。
                                arguments=call.arguments, messages=messages,
                                # 传入汇总（summary），值为表达式 started.context_summary, slots=slots, emit=publish) 的结果。
                                summary=started.context_summary, slots=slots, emit=publish)
                    # 检查条件 call.name == conversation_review.TOOL_NAME 是否成立；成立时执行缩进的分支。
                    elif call.name == conversation_review.TOOL_NAME:
                        # 检查条件 review_used or not conversation_review.available(db, current) 是否成立；成立时执行缩进的分支。
                        if review_used or not conversation_review.available(db, current):
                            # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果保存到 结果（result）。
                            result = ToolExecution(call.name, conversation_review.TOOL_LABEL,
                                # 继续传入或组合表达式 '{"error":"本轮已发起复盘或当前没有权限"}', "本轮不重复发起复盘", True, 0) 的结果，完成上面尚未结束的表达式。
                                '{"error":"本轮已发起复盘或当前没有权限"}', "本轮不重复发起复盘", True, 0)
                        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
                        else:
                            # 把真，表示条件成立或开关打开保存到 复盘used（review_used）。
                            review_used = True
                            # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
                            result = await conversation_review.execute(db, current, java_client,
                                # 传入对话编号（conversation_id），值为表达式 started.conversation_id, arguments=call.arguments, emit=publish) 的结果。
                                conversation_id=started.conversation_id, arguments=call.arguments, emit=publish)
                    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
                    else:
                        # 把执行“按工具名与标准化参数读取本轮缓存；命中后返回可复用的结果副本”得到的结果保存到 缓存结果（cached）。
                        cached = read_budget.cached(call.name, call.arguments)
                        # 检查条件 cached is not None 是否成立；成立时执行缩进的分支。
                        if cached is not None:
                            # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 fresh。
                            fresh = await refresh_context(context, java_client)
                            # 把执行“调用 tool_service.tool_for 完成业务工具for处理”得到的结果保存到 spec。
                            spec = tool_service.tool_for(call.name)
                            # 检查当前用户是否具有需要的权限：spec is not None and spec.allowed(fresh.permissions)；成立时执行缩进的分支。
                            if spec is not None and spec.allowed(fresh.permissions):
                                # 把缓存结果（cached）保存到 结果（result）。
                                result = cached
                            # 前面的条件不满足时，按这里的条件或备用分支继续处理。
                            else:
                                # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
                                result = await tool_service.execute(fresh, java_client, call.name, call.arguments)
                        # 检查条件 not read_budget.available 是否成立；成立时执行缩进的分支。
                        elif not read_budget.available:
                            # 把执行“调用 tool_service.budget_exceeded 完成额度exceeded处理”得到的结果保存到 结果（result）。
                            result = tool_service.budget_exceeded(call.name)
                        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
                        else:
                            # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
                            result = await tool_service.execute(current, java_client, call.name, call.arguments)
                            # 累计一次查询；只有成功且未受限的结果才写入本轮缓存。
                            read_budget.remember(call.name, call.arguments, result)
                            # 把真，表示条件成立或开关打开保存到 queried。
                            queried = True
                    # 等待调用 _record_tool_result 完成记录业务工具结果处理完成后再继续。
                    await _record_tool_result(
                        # 继续传入或组合表达式 db, active, reservation, reservation.result_message_nos[index], call, result 的结果，完成上面尚未结束的表达式。
                        db, active, reservation, reservation.result_message_nos[index], call, result
                    )
                    # 把新条目放到列表末尾。
                    messages.append(tool_result_message(call.id, result.content))
                    # 等待调用 publish 完成publish处理完成后再继续。
                    await publish(
                        # 继续传入或组合固定文字 'tool_end'，完成上面尚未结束的表达式。
                        "tool_end",
                        {
                            # 填写编号（id）字段，值为表达式 call.id 的结果。
                            "id": call.id,
                            # 填写名称（name）字段，值为表达式 result.name 的结果。
                            "name": result.name,
                            # 填写显示名称（label）字段，值为表达式 result.label 的结果。
                            "label": result.label,
                            # 填写状态（status）字段，值为固定文字 'limited" if result.is_limited else "error" if result.is_error else "ok'。
                            "status": "limited" if result.is_limited else "error" if result.is_error else "ok",
                            # 填写持续时间Ms（durationMs）字段，值为表达式 result.duration_ms 的结果。
                            "durationMs": result.duration_ms,
                            # 填写汇总（summary）字段，值为表达式 result.summary 的结果。
                            "summary": result.summary,
                        },
                    )
                # 把 tool_rounds 加上 固定数值 1，再保存回原变量。
                tool_rounds += 1
                # 检查条件 queried 是否成立；成立时执行缩进的分支。
                if queried:
                    # 把 read_budget.rounds 加上 固定数值 1，再保存回原变量。
                    read_budget.rounds += 1
                # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 最后一次保存（last_save）。
                last_save = time.monotonic()
            # 把执行“用指定分隔符把多段文字合起来”得到的结果保存到 文字（text）。
            text = "".join(parts)
            # 检查条件 not text.strip() 是否成立；成立时执行缩进的分支。
            if not text.strip():
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(502, "LLM_EMPTY_REPLY", "模型未返回有效回复，请重新提问")
            # 等待刷新 上下文（context）完成后再继续。
            await refresh_context(context, java_client)
            # 把执行“调用 _safe_finish_reason 完成safefinish原因处理”得到的结果保存到 原因（reason）。
            reason = _safe_finish_reason(finish_reason)
            # 把等待执行“调用 _finalize 完成finalize处理”得到的结果完成后的结果保存到 已保存（saved）。
            saved = await _finalize(
                # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
                db,
                # 继续传入或组合页面是否仍处于活动状态（active），完成上面尚未结束的表达式。
                active,
                # 传入状态（status），值为固定文字 'COMPLETED" if reason == "stop" else "INCOMPLETE'。
                status="COMPLETED" if reason == "stop" else "INCOMPLETE",
                # 传入finish原因（finish_reason），值为原因（reason）。
                finish_reason=reason,
                # 传入错误编码（error_code），值为条件判断的结果：None if reason == "stop" else "LLM_OUTPUT_TRUNCATED"。
                error_code=None if reason == "stop" else "LLM_OUTPUT_TRUNCATED",
                # 传入文字（text），值为文字（text）。
                text=text,
                # 传入用量（usage），值为轮用量（round_usage）。
                usage=round_usage,
                # 传入模型（model），值为模型（model）。
                model=model,
            )
            # 检查条件 not saved 是否成立；成立时执行缩进的分支。
            if not saved:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "TURN_STATE_CONFLICT", "轮次状态已变化，请刷新会话")
            # 等待调用 publish 完成publish处理完成后再继续。
            await publish("activity", {"id": "reply", "label": "组织回复", "status": "ok",
                                      # 填写汇总（summary）字段，值为表达式 "回复已保存"}) 的结果。
                                      "summary": "回复已保存"})
            # 把执行“调用 GeneratedReply 完成GeneratedReply处理”得到的结果交还给调用方，本函数到此结束。
            return GeneratedReply(text, model, reason, total_usage)
    # 捕获asyncio.CancelledError异常，执行下面的错误处理。
    except asyncio.CancelledError:
        # 调用 _best_effort_cancel 完成besteffort取消处理。
        _best_effort_cancel(db, active, "".join(parts), model)
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 对外边界脱敏，失败结果不能报告为成功。
        # 把执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果保存到 失败（failure）。
        failure = safe_error(error, context.trace_id)
        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with suppress(Exception):
            # 等待调用 _finalize 完成finalize处理完成后再继续。
            await _finalize(
                # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
                db,
                # 继续传入或组合页面是否仍处于活动状态（active），完成上面尚未结束的表达式。
                active,
                # 传入状态（status），值为固定状态或编码 FAILED（失败）。
                status="FAILED",
                # 传入finish原因（finish_reason），值为空值，表示当前还没有对应内容。
                finish_reason=None,
                # 传入错误编码（error_code），值为调用 _safe_error_code 完成safe错误编码处理。
                error_code=_safe_error_code(failure.code),
                # 传入文字（text），值为用指定分隔符把多段文字合起来。
                text="".join(parts),
                # 传入用量（usage），值为轮用量（round_usage）。
                usage=round_usage,
                # 传入模型（model），值为模型（model）。
                model=model,
            )
        # 中止本轮正常处理，抛出 失败（failure）。
        raise failure from None


# 异步函数 generate_plain：调用 generate_plain 完成generateplain处理；接收 Agent 数据库访问器（db）、上下文（context）、开始时（started）、消息（message）、Java后端客户端（java_client）、slots。
async def generate_plain(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 填写消息（message）字段，值为str。
    message: str,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写slots字段，值为表达式 asyncio.Semaphore 的结果。
    slots: asyncio.Semaphore,
# 继续传入或组合表达式 ) -> SendResultVO: 的结果，完成上面尚未结束的表达式。
) -> SendResultVO:
    # 把执行“调用 asyncio.current_task 完成当前任务处理”得到的结果保存到 当前（current）。
    current = asyncio.current_task()
    # 检查条件 current is not None 是否成立；成立时执行缩进的分支。
    if current is not None:
        # 把当前（current）保存到 运行中任务列表开始时本轮对话编号（_RUNNING_TASKS[started.turn_id]）。
        _RUNNING_TASKS[started.turn_id] = current
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 _generate 完成generate处理”得到的结果完成后的结果保存到 结果（result）。
        result = await _generate(db, context, started, message, java_client, slots)
        # 检查条件 result.finish_reason == 'stop' 是否成立；成立时执行缩进的分支。
        if result.finish_reason == "stop":
            # 调用 _schedule_summary 完成安排汇总处理。
            _schedule_summary(db, started, slots)
        # 把执行“调用 SendResultVO 完成发送结果处理”得到的结果交还给调用方，本函数到此结束。
        return SendResultVO(
            # 传入reply，值为表达式 result.reply 的结果。
            reply=result.reply,
            # 传入模型（model），值为表达式 result.model 的结果。
            model=result.model,
            # 传入finish原因（finish_reason），值为表达式 result.finish_reason 的结果。
            finish_reason=result.finish_reason,
            # 传入对话编号（conversation_id），值为把输入转换成文字，统一比较或展示时的格式。
            conversation_id=str(started.conversation_id),
            # 传入本轮对话编号（turn_id），值为把输入转换成文字，统一比较或展示时的格式。
            turn_id=str(started.turn_id),
            # 传入请求编号（request_id），值为表达式 started.request_id 的结果。
            request_id=started.request_id,
            # 传入本轮对话版本号（turn_version），值为按表达式计算得到的值：started.turn_version + 1。
            turn_version=started.turn_version + 1,
        )
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 调用 _RUNNING_TASKS.pop 完成pop处理。
        _RUNNING_TASKS.pop(started.turn_id, None)


# 异步函数 stream_turn：调用 stream_turn 完成分段回复流本轮对话处理；接收 Agent 数据库访问器（db）、上下文（context）、开始时（started）、消息（message）、Java后端客户端（java_client）、slots。
async def stream_turn(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写开始时（started）字段，值为开始时本轮对话（StartedTurn）。
    started: StartedTurn,
    # 填写消息（message）字段，值为str。
    message: str,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写slots字段，值为表达式 asyncio.Semaphore 的结果。
    slots: asyncio.Semaphore,
# 继续传入或组合表达式 ) -> AsyncIterator[bytes]: 的结果，完成上面尚未结束的表达式。
) -> AsyncIterator[bytes]:
    # 声明 queue，类型为 asyncio.Queue[tuple[str, dict[str, Any]]]；其内容为执行“调用 asyncio.Queue 完成Queue处理”得到的结果。
    queue: asyncio.Queue[tuple[str, dict[str, Any]]] = asyncio.Queue(maxsize=32)
    # 把假，表示条件不成立或开关关闭保存到 consumerclosed（consumer_closed）。
    consumer_closed = False

    # 异步函数 emit：向父组件发送事件，通知数据或界面状态发生变化；接收 事件（event）、数据（data）。
    async def emit(event: str, data: dict[str, Any]) -> None:
        # 等待保存指定键对应的值完成后再继续。
        await queue.put((event, data))

    # 异步函数 produce：调用 produce 完成produce处理。
    async def produce() -> None:
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把等待执行“调用 _generate 完成generate处理”得到的结果完成后的结果保存到 结果（result）。
            result = await _generate(db, context, started, message, java_client, slots, emit)
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit(
                # 继续传入或组合固定文字 'message_end'，完成上面尚未结束的表达式。
                "message_end",
                {
                    # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                    "conversationId": str(started.conversation_id),
                    # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                    "turnId": str(started.turn_id),
                    # 填写请求编号（requestId）字段，值为表达式 started.request_id 的结果。
                    "requestId": started.request_id,
                    # 填写本轮对话版本号（turnVersion）字段，值为按表达式计算得到的值：started.turn_version + 1。
                    "turnVersion": started.turn_version + 1,
                    # 填写模型（model）字段，值为表达式 result.model 的结果。
                    "model": result.model,
                    # 填写finish原因（finishReason）字段，值为表达式 result.finish_reason 的结果。
                    "finishReason": result.finish_reason,
                    # 填写完成（complete）字段，值为条件判断的结果：result.finish_reason == "stop"。
                    "complete": result.finish_reason == "stop",
                    # 填写用量（usage）字段，值为表达式 result.usage 的结果。
                    "usage": result.usage,
                    # 填写citations字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                    "citations": started.context_metadata.get("citations", []),
                    # 填写used长期记忆编号列表（usedMemoryIds）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                    "usedMemoryIds": started.context_metadata.get("memoryIds", []),
                    # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                    "traceId": context.trace_id,
                },
            )
        # 捕获asyncio.CancelledError异常，执行下面的错误处理。
        except asyncio.CancelledError:
            # 连接关闭后无人消费队列，不能再阻塞等待写入停止事件。
            # 检查条件 not consumer_closed 是否成立；成立时执行缩进的分支。
            if not consumer_closed:
                # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
                await emit(
                    # 继续传入或组合固定文字 'error'，完成上面尚未结束的表达式。
                    "error",
                    {
                        # 填写编码（code）字段，值为固定状态或编码 TURN_CANCELLED（本轮对话已取消）。
                        "code": "TURN_CANCELLED",
                        # 填写消息（message）字段，值为固定文字 '本轮生成已停止'。
                        "message": "本轮生成已停止",
                        # 填写状态（status）字段，值为固定数值 409。
                        "status": 409,
                        # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                        "traceId": context.trace_id,
                        # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                        "conversationId": str(started.conversation_id),
                        # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                        "turnId": str(started.turn_id),
                    },
                )
        # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
        except Exception as error:  # noqa: BLE001 - SSE 开始后使用错误事件终止。
            # 把执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果保存到 失败（failure）。
            failure = safe_error(error, context.trace_id)
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit(
                # 继续传入或组合固定文字 'error'，完成上面尚未结束的表达式。
                "error",
                {
                    # 填写编码（code）字段，值为表达式 failure.code 的结果。
                    "code": failure.code,
                    # 填写消息（message）字段，值为表达式 failure.message 的结果。
                    "message": failure.message,
                    # 填写状态（status）字段，值为表达式 failure.status_code 的结果。
                    "status": failure.status_code,
                    # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                    "traceId": context.trace_id,
                    # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                    "conversationId": str(started.conversation_id),
                    # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                    "turnId": str(started.turn_id),
                },
            )

    # 把执行“启动可同时推进的异步任务，并保存任务句柄”得到的结果保存到 producer。
    producer = asyncio.create_task(produce(), name=f"agent-turn-{started.turn_id}")
    # 把producer保存到 运行中任务列表开始时本轮对话编号（_RUNNING_TASKS[started.turn_id]）。
    _RUNNING_TASKS[started.turn_id] = producer
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
        yield event_frame(
            # 继续传入或组合固定文字 'message_start'，完成上面尚未结束的表达式。
            "message_start",
            {
                # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                "traceId": context.trace_id,
                # 填写模型（model）字段，值为表达式 started.model 的结果。
                "model": started.model,
                # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                "conversationId": str(started.conversation_id),
                # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
                "turnId": str(started.turn_id),
                # 填写请求编号（requestId）字段，值为表达式 started.request_id 的结果。
                "requestId": started.request_id,
                # 填写本轮对话版本号（turnVersion）字段，值为表达式 started.turn_version 的结果。
                "turnVersion": started.turn_version,
            },
        )
        # 只要条件 True 是否成立，就继续执行下面的循环。
        while True:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 把等待执行“等待异步处理，但不超过指定的超时时间”得到的结果完成后的结果保存到 事件数据（(event, data)）。
                event, data = await asyncio.wait_for(queue.get(), timeout=10.0)
            # 捕获TimeoutError异常，执行下面的错误处理。
            except TimeoutError:
                # 检查条件 producer.done() 是否成立；成立时执行缩进的分支。
                if producer.done():
                    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
                    yield event_frame(
                        # 继续传入或组合固定文字 'error'，完成上面尚未结束的表达式。
                        "error",
                        {
                            # 填写编码（code）字段，值为固定状态或编码 STREAM_INCOMPLETE（分段回复流INCOMPLETE）。
                            "code": "STREAM_INCOMPLETE",
                            # 填写消息（message）字段，值为固定文字 '回复连接已结束，请刷新会话'。
                            "message": "回复连接已结束，请刷新会话",
                            # 填写状态（status）字段，值为固定数值 502。
                            "status": 502,
                            # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                            "traceId": context.trace_id,
                        },
                    )
                    # 把空值交还给调用方，本函数到此结束。
                    return
                # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
                yield b": keep-alive\n\n"
                # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                continue
            # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
            yield event_frame(event, data)
            # 检查条件 event in {'message_end', 'error'} 是否成立；成立时执行缩进的分支。
            if event in {"message_end", "error"}:
                # 检查条件 event == 'message_end' and data.get('complete') is True 是否成立；成立时执行缩进的分支。
                if event == "message_end" and data.get("complete") is True:
                    # 调用 _schedule_summary 完成安排汇总处理。
                    _schedule_summary(db, started, slots)
                # 把空值交还给调用方，本函数到此结束。
                return
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 把真，表示条件成立或开关打开保存到 consumerclosed（consumer_closed）。
        consumer_closed = True
        # 调用 _RUNNING_TASKS.pop 完成pop处理。
        _RUNNING_TASKS.pop(started.turn_id, None)
        # 检查条件 not producer.done() 是否成立；成立时执行缩进的分支。
        if not producer.done():
            # 调用 producer.cancel 完成取消处理。
            producer.cancel()
        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with suppress(asyncio.CancelledError):
            # 等待producer完成后的结果。
            await producer


# 异步函数 stream_replay：已完成的同一请求直接重放已保存结果，不再次调用模型。；接收 replayed、追踪标识编号（trace_id）。
async def stream_replay(replayed: ReplayedTurn, trace_id: str) -> AsyncIterator[bytes]:
    # 已完成的同一请求直接重放已保存结果，不再次调用模型。
    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
    yield event_frame(
        # 继续传入或组合固定文字 'message_start'，完成上面尚未结束的表达式。
        "message_start",
        {
            # 填写本次请求的追踪编号（traceId）字段，值为追踪标识编号（trace_id）。
            "traceId": trace_id,
            # 填写模型（model）字段，值为表达式 replayed.model 的结果。
            "model": replayed.model,
            # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "conversationId": str(replayed.conversation_id),
            # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "turnId": str(replayed.turn_id),
            # 填写请求编号（requestId）字段，值为表达式 replayed.request_id 的结果。
            "requestId": replayed.request_id,
            # 填写本轮对话版本号（turnVersion）字段，值为表达式 replayed.turn_version 的结果。
            "turnVersion": replayed.turn_version,
        },
    )
    # 检查条件 replayed.reply 是否成立；成立时执行缩进的分支。
    if replayed.reply:
        # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
        yield event_frame("delta", {"text": replayed.reply})
    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
    yield event_frame(
        # 继续传入或组合固定文字 'message_end'，完成上面尚未结束的表达式。
        "message_end",
        {
            # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "conversationId": str(replayed.conversation_id),
            # 填写本轮对话编号（turnId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "turnId": str(replayed.turn_id),
            # 填写请求编号（requestId）字段，值为表达式 replayed.request_id 的结果。
            "requestId": replayed.request_id,
            # 填写本轮对话版本号（turnVersion）字段，值为表达式 replayed.turn_version 的结果。
            "turnVersion": replayed.turn_version,
            # 填写模型（model）字段，值为表达式 replayed.model 的结果。
            "model": replayed.model,
            # 填写finish原因（finishReason）字段，值为优先使用前面的有效值，没有时采用后面的备用值：replayed.finish_reason or "stop"。
            "finishReason": replayed.finish_reason or "stop",
            # 填写完成（complete）字段，值为真，表示条件成立或开关打开。
            "complete": True,
            # 填写用量（usage）字段，值为表达式 replayed.usage 的结果。
            "usage": replayed.usage,
            # 填写本次请求的追踪编号（traceId）字段，值为追踪标识编号（trace_id）。
            "traceId": trace_id,
        },
    )


# 函数 replay_result：调用 replay_result 完成replay结果处理；接收 replayed。
def replay_result(replayed: ReplayedTurn) -> SendResultVO:
    # 把执行“调用 SendResultVO 完成发送结果处理”得到的结果交还给调用方，本函数到此结束。
    return SendResultVO(
        # 传入reply，值为表达式 replayed.reply 的结果。
        reply=replayed.reply,
        # 传入模型（model），值为优先使用前面的有效值，没有时采用后面的备用值：replayed.model or ""。
        model=replayed.model or "",
        # 传入finish原因（finish_reason），值为优先使用前面的有效值，没有时采用后面的备用值：replayed.finish_reason or "stop"。
        finish_reason=replayed.finish_reason or "stop",
        # 传入对话编号（conversation_id），值为把输入转换成文字，统一比较或展示时的格式。
        conversation_id=str(replayed.conversation_id),
        # 传入本轮对话编号（turn_id），值为把输入转换成文字，统一比较或展示时的格式。
        turn_id=str(replayed.turn_id),
        # 传入请求编号（request_id），值为表达式 replayed.request_id 的结果。
        request_id=replayed.request_id,
        # 传入本轮对话版本号（turn_version），值为表达式 replayed.turn_version 的结果。
        turn_version=replayed.turn_version,
    )


# 异步函数 get_turn：读取 本轮对话（_turn） 的值；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、本轮对话编号（turn_id）。
async def get_turn(db: Database, user_id: int, conversation_id: int, turn_id: int) -> TurnVO:
    # 等待统一处理 已过期（expired）完成后再继续。
    await conversation_service.normalize_expired(db, user_id, conversation_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, user_id, conversation_id)
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 本轮对话（turn）。
        turn = await turn_repository.find_owned(session, user_id, conversation_id, turn_id)
        # 检查turn is None，即对应内容是否不存在；成立时执行缩进的分支。
        if turn is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "TURN_NOT_FOUND", "轮次不存在或不可访问")
        # 把等待执行“调用 message_repository.assistant_for_turn 完成assistantfor本轮对话处理”得到的结果完成后的结果保存到 assistant。
        assistant = await message_repository.assistant_for_turn(
            # 继续传入或组合表达式 session, user_id=user_id, turn_id=turn_id 的结果，完成上面尚未结束的表达式。
            session, user_id=user_id, turn_id=turn_id
        )
        # 把执行“调用 conversation_service.turn_vo 完成本轮对话处理”得到的结果交还给调用方，本函数到此结束。
        return conversation_service.turn_vo(turn, assistant.content if assistant else None)


# 异步函数 cancel_turn：取消 本轮对话（turn）；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、本轮对话编号（turn_id）、版本号（version）。
async def cancel_turn(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, conversation_id: int, turn_id: int, version: int 的结果。
    db: Database, user_id: int, conversation_id: int, turn_id: int, version: int
# 继续传入或组合表达式 ) -> TurnVO: 的结果，完成上面尚未结束的表达式。
) -> TurnVO:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(
            # 继续传入或组合表达式 session, user_id, conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, user_id, conversation_id, for_update=True
        )
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 本轮对话（turn）。
        turn = await turn_repository.find_owned(session, user_id, conversation_id, turn_id)
        # 检查turn is None，即对应内容是否不存在；成立时执行缩进的分支。
        if turn is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "TURN_NOT_FOUND", "轮次不存在或不可访问")
        # 检查当前记录版本是否符合请求要求：turn.version != version；成立时执行缩进的分支。
        if turn.version != version:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "STATE_CONFLICT", "轮次状态已变化，请刷新后重试")
        # 检查条件 turn.status == 'CANCELLED' 是否成立；成立时执行缩进的分支。
        if turn.status == "CANCELLED":
            # 把执行“调用 conversation_service.turn_vo 完成本轮对话处理”得到的结果交还给调用方，本函数到此结束。
            return conversation_service.turn_vo(turn)
        # 检查条件 turn.status != 'RUNNING' 是否成立；成立时执行缩进的分支。
        if turn.status != "RUNNING":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "STATE_CONFLICT", "轮次已经结束，不能取消")
        # 把等待执行“调用 turn_repository.cancel 完成取消处理”得到的结果完成后的结果保存到 已取消（cancelled）。
        cancelled = await turn_repository.cancel(session, turn_id=turn_id, expected_version=version)
        # 检查条件 not cancelled 是否成立；成立时执行缩进的分支。
        if not cancelled:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "STATE_CONFLICT", "轮次状态已变化，请刷新后重试")
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(turn)
        # 把执行“调用 conversation_service.turn_vo 完成本轮对话处理”得到的结果保存到 结果（result）。
        result = conversation_service.turn_vo(turn)

    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 任务（task）。
    task = _RUNNING_TASKS.get(turn_id)
    # 检查条件 task is not None and (not task.done()) 是否成立；成立时执行缩进的分支。
    if task is not None and not task.done():
        # 调用 task.cancel 完成取消处理。
        task.cancel()
    # 把结果（result）交还给调用方，本函数到此结束。
    return result
