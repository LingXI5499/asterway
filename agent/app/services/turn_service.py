"""会话轮次：幂等重放、生成、分批保存、终态提交、取消与到期归一。"""

import asyncio
import hashlib
import logging
import re
import time
from collections.abc import AsyncIterator, Awaitable, Callable
from contextlib import suppress
from dataclasses import dataclass, field, replace
from datetime import UTC, datetime, timedelta
from typing import Any
from uuid import UUID

from sqlalchemy import func
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.context import AgentContext, refresh_context
from app.core.errors import AgentError
from app.db.models import Conversation
from app.db.session import Database
from app.integrations.java_client import JavaApiClient
from app.llm.chat_service import event_frame, model_options, safe_error
from app.llm.client import get_llm_client
from app.llm.tool_calling import (
    ToolCall,
    assistant_tool_message,
    build_tool_calls,
    merge_tool_deltas,
    tool_result_message,
)
from app.repositories import conversation_repository, message_repository, turn_repository
from app.schemas.conversation import SendResultVO, TurnVO
from app.services import (
    context_service,
    conversation_planning,
    conversation_review,
    conversation_service,
    summary_service,
    tool_service,
)
from app.services.tool_budget import ReadBudget
from app.tools.base import ToolExecution
from app.tools.registry import tool_definitions

logger = logging.getLogger(__name__)

ERROR_CODE = re.compile(r"[A-Z][A-Z0-9_]{0,63}")
FINISH_REASON = re.compile(r"[a-z][a-z0-9_]{0,63}")


def _conversation_now():
    """会话更新使用数据库执行时间且不早于 created_at，避免宿主时钟回拨破坏时间 CHECK。"""
    return func.greatest(func.clock_timestamp(), Conversation.created_at)

# 本进程正在生成的轮次；取消接口可以立即终止同进程内的上游流。
_RUNNING_TASKS: dict[int, asyncio.Task[Any]] = {}
_BACKGROUND_TASKS: set[asyncio.Task[Any]] = set()
_SUMMARY_TASKS: dict[tuple[int, int], asyncio.Task[Any]] = {}
_PENDING_SUMMARIES: set[tuple[int, int]] = set()
_SHUTTING_DOWN = False


@dataclass(frozen=True)
class StartedTurn:
    user_id: int
    conversation_id: int
    turn_id: int
    request_id: str
    turn_version: int
    user_message_no: int
    assistant_message_id: int
    model: str
    summary_through_message_no: int
    context_summary: str
    deadline_at: datetime
    context_metadata: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class ReplayedTurn:
    conversation_id: int
    turn_id: int
    request_id: str
    turn_version: int
    model: str | None
    finish_reason: str | None
    reply: str
    usage: dict[str, int] | None = None


def _now() -> datetime:
    return datetime.now(UTC)


def request_hash(conversation_id: int, message: str) -> str:
    return hashlib.sha256(f"{conversation_id}\n{message}".encode()).hexdigest()


def _safe_error_code(value: str | None) -> str:
    return value if value and ERROR_CODE.fullmatch(value) else "AGENT_REQUEST_FAILED"


def _safe_finish_reason(value: str | None) -> str:
    return value if value and FINISH_REASON.fullmatch(value) else "other"


def _usage(completion: Any) -> dict[str, int] | None:
    usage = getattr(completion, "usage", None)
    if usage is None:
        return None
    return {
        "promptTokens": usage.prompt_tokens,
        "completionTokens": usage.completion_tokens,
        "totalTokens": usage.total_tokens,
    }


async def start_turn(
    db: Database,
    user_id: int,
    conversation_id: int,
    message: str,
    request_id: UUID,
) -> StartedTurn | ReplayedTurn:
    """在同一短事务内完成幂等检查、轮次登记、提问与助手占位。"""
    # 到期状态先独立提交，后续版本／幂等拒绝不会把它回滚为 RUNNING。
    await conversation_service.normalize_expired(db, user_id, conversation_id)
    settings = get_settings()
    now = _now()
    digest = request_hash(conversation_id, message)
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )

        existing = await turn_repository.find_by_request(session, user_id, request_id)
        if existing is not None:
            if existing.conversation_id != conversation_id or existing.request_hash != digest:
                raise AgentError(
                    409,
                    "REQUEST_ID_REUSED",
                    "同一请求标识已用于其他会话或内容，请重新发起",
                    {"turnId": str(existing.id), "status": existing.status},
                )
            if existing.status == "RUNNING" and now >= existing.deadline_at:
                await turn_repository.finalize(
                    session,
                    turn_id=existing.id,
                    expected_version=existing.version,
                    status="FAILED",
                    finish_reason=None,
                    error_code="AGENT_TIMEOUT",
                )
                await session.refresh(existing)
            if existing.status == "RUNNING":
                raise AgentError(
                    409,
                    "TURN_RUNNING",
                    "该请求仍在生成中，请查询原轮次状态",
                    {
                        "turnId": str(existing.id),
                        "status": existing.status,
                        "version": existing.version,
                    },
                )
            if existing.status == "COMPLETED":
                assistant = await message_repository.assistant_for_turn(
                    session, user_id=user_id, turn_id=existing.id
                )
                totals = await message_repository.usage_by_turn(session, user_id, {existing.id})
                return ReplayedTurn(
                    conversation_id=conversation_id,
                    turn_id=existing.id,
                    request_id=str(existing.request_id),
                    turn_version=existing.version,
                    model=assistant.model if assistant else None,
                    finish_reason=existing.finish_reason,
                    reply=assistant.content if assistant else "",
                    usage=totals.get(existing.id),
                )
            raise AgentError(
                409,
                "TURN_NOT_REPLAYABLE",
                "该请求已结束，不能重复发送；请查询原轮次状态",
                {
                    "turnId": str(existing.id),
                    "status": existing.status,
                    "version": existing.version,
                },
            )

        if conversation.status != "ACTIVE":
            raise AgentError(409, "CONVERSATION_CLOSED", "会话已关闭，不能继续提问")

        await turn_repository.expire_running(session, conversation_id, now)
        if await turn_repository.count_running(session, conversation_id) > 0:
            raise AgentError(409, "TURN_RUNNING", "该会话已有一轮回复正在生成")

        # 资料页触发的调整不占聊天轮次，但同一对话不能并发生成两份方案。
        if getattr(db, "workflow_graph", None) is not None:
            from app.repositories import workflow_repository
            run = await workflow_repository.find_active_run(session, conversation_id)
            phase = ((run.graph_state or {}).get("planning") or {}).get("phase") if run else None
            if run and run.source_turn_id is None and run.status == "DRAFT" \
                    and run.expires_at > now and phase not in {"ERROR", "INTERRUPTED"}:
                raise AgentError(409, "PLANNING_IN_PROGRESS", "正在根据复盘整理方案，请完成后继续对话")

        deadline_at = now + timedelta(seconds=settings.agent_turn_timeout_seconds)
        try:
            turn = await turn_repository.create(
                session,
                user_id=user_id,
                conversation_id=conversation_id,
                request_id=request_id,
                request_hash=digest,
                deadline_at=deadline_at,
            )
        except IntegrityError as error:
            # 并发重放时唯一索引是最后一道防线。
            if "uk_agent_turn_user_request" in str(getattr(error, "orig", "")):
                raise AgentError(
                    409, "REQUEST_ID_REUSED", "同一请求标识已用于其他请求，请重新发起"
                ) from None
            raise AgentError(409, "TURN_RUNNING", "该会话已有一轮回复正在生成") from None
        user_row, assistant_row = await message_repository.insert_pair(
            session,
            conversation=conversation,
            user_id=user_id,
            turn_id=turn.id,
            user_content=message,
            assistant_model=settings.llm_model,
            now=now,
        )
        return StartedTurn(
            user_id=user_id,
            conversation_id=conversation_id,
            turn_id=turn.id,
            request_id=str(request_id),
            turn_version=turn.version,
            user_message_no=user_row.message_no,
            assistant_message_id=assistant_row.id,
            model=settings.llm_model,
            summary_through_message_no=conversation.summary_through_message_no,
            context_summary=conversation.context_summary,
            deadline_at=deadline_at,
        )


async def _messages(db: Database, started: StartedTurn, message: str) -> list[dict[str, Any]]:
    return await context_service.build_model_messages(
        db,
        user_id=started.user_id,
        conversation_id=started.conversation_id,
        summary_through_message_no=started.summary_through_message_no,
        context_summary=started.context_summary,
        before_message_no=started.user_message_no,
        question=message,
        metadata=started.context_metadata,
    )


async def _save_partial(db: Database, started: StartedTurn, text: str, model: str) -> None:
    try:
        async with db.session() as session:
            await _lock_running(session, started)
            saved = await message_repository.save_assistant(
                session,
                message_id=started.assistant_message_id,
                user_id=started.user_id,
                conversation_id=started.conversation_id,
                turn_id=started.turn_id,
                content=text,
                is_complete=False,
                finish_reason=None,
                usage=None,
                model=model,
                now=_now(),
            )
            if not saved:
                raise AgentError(409, "TURN_STATE_CONFLICT", "回复状态已变化，请刷新会话")
    except AgentError:
        raise
    except Exception as error:  # noqa: BLE001 - 临时部分保存失败不伪造最终保存成功。
        logger.warning(
            "agent_partial_save_failed turn=%s type=%s",
            started.turn_id,
            type(error).__name__,
        )


async def _finalize(
    db: Database,
    started: StartedTurn,
    *,
    status: str,
    finish_reason: str | None,
    error_code: str | None,
    text: str | None = None,
    usage: dict[str, int] | None = None,
    model: str | None = None,
) -> bool:
    """会话行先于轮次行加锁，避免与取消操作形成死锁。"""
    now = _now()
    async with db.session() as session:
        conversation = await conversation_repository.require_owned(
            session, started.user_id, started.conversation_id, for_update=True
        )
        saved = await turn_repository.finalize(
            session,
            turn_id=started.turn_id,
            expected_version=started.turn_version,
            status=status,
            finish_reason=finish_reason,
            error_code=error_code,
        )
        if not saved:
            return False
        if text is not None:
            message_saved = await message_repository.save_assistant(
                session,
                message_id=started.assistant_message_id,
                user_id=started.user_id,
                conversation_id=started.conversation_id,
                turn_id=started.turn_id,
                content=text,
                is_complete=status == "COMPLETED",
                finish_reason=finish_reason,
                usage=usage,
                model=model or started.model,
                now=now,
            )
            if not message_saved:
                raise AgentError(409, "TURN_STATE_CONFLICT", "回复保存状态已变化，请刷新会话")
        from app.db.models import ConversationMessage
        from app.repositories import background_task_repository
        row = await session.get(ConversationMessage, started.assistant_message_id)
        if row is not None:
            row.citations = started.context_metadata.get("citations", [])
            row.used_memory_ids = started.context_metadata.get("memoryIds", [])
        if status == "COMPLETED":
            await background_task_repository.enqueue(
                session, user_id=started.user_id, kind="MEMORY_EXTRACT",
                ref_type="CONVERSATION_TURN", ref_id=started.turn_id,
            )
        conversation.last_message_at = _conversation_now()
        conversation.updated_at = _conversation_now()
        conversation.version += 1
        return True


def _schedule_summary(db: Database, started: StartedTurn, slots: asyncio.Semaphore) -> None:
    if _SHUTTING_DOWN:
        return
    key = (started.user_id, started.conversation_id)
    if key in _SUMMARY_TASKS and not _SUMMARY_TASKS[key].done():
        _PENDING_SUMMARIES.add(key)
        return
    task = asyncio.create_task(
        summary_service.run_summary_for_conversation(
            db, started.user_id, started.conversation_id, slots
        ),
        name=f"agent-summary-{started.conversation_id}",
    )
    _BACKGROUND_TASKS.add(task)
    _SUMMARY_TASKS[key] = task
    task.add_done_callback(_BACKGROUND_TASKS.discard)

    def completed(done: asyncio.Task[Any]) -> None:
        if _SUMMARY_TASKS.get(key) is done:
            _SUMMARY_TASKS.pop(key, None)
        if key in _PENDING_SUMMARIES:
            _PENDING_SUMMARIES.discard(key)
            if not done.cancelled():
                _schedule_summary(db, started, slots)

    task.add_done_callback(completed)


async def shutdown_generation() -> None:
    """先取消生成，再收敛派生补存任务；数据库与模型连接最后关闭。"""
    global _SHUTTING_DOWN
    _SHUTTING_DOWN = True
    _PENDING_SUMMARIES.clear()
    for _ in range(2):
        tasks = {
            task
            for task in (*_RUNNING_TASKS.values(), *_BACKGROUND_TASKS)
            if not task.done() and task is not asyncio.current_task()
        }
        if not tasks:
            return
        for task in tasks:
            task.cancel()
        with suppress(TimeoutError):
            await asyncio.wait_for(asyncio.gather(*tasks, return_exceptions=True), timeout=5.0)


async def _mark_cancelled(db: Database, started: StartedTurn, text: str, model: str) -> None:
    try:
        async with db.session() as session:
            conversation = await conversation_repository.require_owned(
                session, started.user_id, started.conversation_id, for_update=True
            )
            turn = await turn_repository.find_owned(
                session, started.user_id, started.conversation_id, started.turn_id
            )
            if turn is None:
                return
            if turn.status == "RUNNING" and turn.version == started.turn_version:
                await turn_repository.cancel(
                    session, turn_id=turn.id, expected_version=turn.version
                )
            elif not (turn.status == "CANCELLED" and turn.version == started.turn_version + 1):
                return
            # 显式取消可能已先提交；只补存本生产任务收到的部分文本，不重写终态时间。
            await message_repository.save_assistant(
                session,
                message_id=started.assistant_message_id,
                user_id=started.user_id,
                conversation_id=started.conversation_id,
                turn_id=started.turn_id,
                content=text,
                is_complete=False,
                finish_reason=None,
                usage=None,
                model=model,
                now=_now(),
            )
            conversation.updated_at = _conversation_now()
            conversation.last_message_at = _conversation_now()
            conversation.version += 1
    except Exception as error:  # noqa: BLE001 - 断开后的补存尽力完成，截止归一仍是兜底。
        logger.warning(
            "agent_cancel_save_failed turn=%s type=%s", started.turn_id, type(error).__name__
        )


def _best_effort_cancel(
    db: Database, started: StartedTurn, text: str = "", model: str | None = None
) -> None:
    task = asyncio.create_task(
        _mark_cancelled(db, started, text, model or started.model), name="agent-turn-cancel"
    )
    _BACKGROUND_TASKS.add(task)
    task.add_done_callback(_BACKGROUND_TASKS.discard)


async def _lock_running(session: AsyncSession, started: StartedTurn) -> Conversation:
    conversation = await conversation_repository.require_owned(
        session, started.user_id, started.conversation_id, for_update=True
    )
    turn = await turn_repository.find_owned(
        session, started.user_id, started.conversation_id, started.turn_id
    )
    if (
        turn is None
        or turn.status != "RUNNING"
        or turn.version != started.turn_version
        or conversation.status != "ACTIVE"
    ):
        raise AgentError(409, "TURN_STATE_CONFLICT", "轮次已停止或状态变化，请刷新会话")
    if _now() >= turn.deadline_at:
        raise AgentError(504, "AGENT_TIMEOUT", "回复已到达截止时间，请重新提问")
    return conversation


async def _prepare_tools(
    db: Database,
    started: StartedTurn,
    content: str,
    calls: list[ToolCall],
    model: str,
    usage: dict[str, int] | None,
) -> message_repository.ToolRoundReservation:
    async with db.session() as session:
        conversation = await _lock_running(session, started)
        return await message_repository.begin_tool_round(
            session,
            conversation=conversation,
            turn_id=started.turn_id,
            assistant_message_id=started.assistant_message_id,
            content=content,
            calls=assistant_tool_message(content, calls)["tool_calls"],
            model=model,
            usage=usage,
        )


async def _record_tool_result(
    db: Database,
    started: StartedTurn,
    reservation: message_repository.ToolRoundReservation,
    number: int,
    call: ToolCall,
    result: ToolExecution,
) -> None:
    async with db.session() as session:
        conversation = await _lock_running(session, started)
        await message_repository.insert_tool_result(
            session,
            conversation=conversation,
            turn_id=started.turn_id,
            request_message_id=reservation.request_message_id,
            message_no=number,
            call_id=call.id,
            name=result.name,
            content=result.content,
        )


@dataclass(frozen=True)
class GeneratedReply:
    reply: str
    model: str
    finish_reason: str
    usage: dict[str, int] | None


def _sum_usage(
    total: dict[str, int] | None, current: dict[str, int] | None
) -> dict[str, int] | None:
    if current is None:
        return total
    result = dict(total or {})
    for key in ("promptTokens", "completionTokens", "totalTokens"):
        value = current.get(key)
        if isinstance(value, int) and not isinstance(value, bool) and value >= 0:
            result[key] = result.get(key, 0) + value
    return result


async def _generate(
    db: Database,
    context: AgentContext,
    started: StartedTurn,
    message: str,
    java_client: JavaApiClient,
    slots: asyncio.Semaphore,
    emit: Callable[[str, dict[str, Any]], Awaitable[None]] | None = None,
) -> GeneratedReply:
    """普通与流式请求共用模型循环、工具持久化、权限复核和终态提交。"""
    settings = get_settings()
    active = started
    upstream = None
    parts: list[str] = []
    model = started.model
    round_usage: dict[str, int] | None = None
    total_usage: dict[str, int] | None = None
    tool_rounds = emitted_chars = 0
    read_budget = ReadBudget(settings.agent_tool_max_calls, settings.agent_tool_max_rounds)
    planning_used = False
    review_used = False
    last_save = time.monotonic()
    deadline = min(
        context.deadline, time.monotonic() + max(0, (started.deadline_at - _now()).total_seconds())
    )

    async def publish(event: str, data: dict[str, Any]) -> None:
        if emit is not None:
            await emit(event, data)

    try:
        async with asyncio.timeout_at(deadline):
            await publish("activity", {"id": "context", "label": "读取会话上下文", "status": "running"})
            messages = await _messages(db, started, message)
            if conversation_review.available(db, context):
                review_hint = await conversation_review.context_hint(db, started.user_id, started.conversation_id)
                if review_hint:
                    messages.insert(len(messages) - 1, {"role": "user", "content": review_hint})
            if conversation_planning.available(db, context):
                hint = await conversation_planning.context_hint(db, started.user_id,
                                                                  started.conversation_id)
                if hint:
                    messages.insert(len(messages) - 1, {"role": "user", "content": hint})
            await publish("activity", {"id": "context", "label": "读取会话上下文", "status": "ok",
                "summary": "已加载本轮可用的会话背景"})
            await publish("activity", {"id": "reply", "label": "组织回复", "status": "running"})
            while True:
                current = await refresh_context(context, java_client)
                definitions = tool_definitions(current.permissions) if read_budget.available else []
                if conversation_planning.available(db, current) and not planning_used:
                    definitions.append(conversation_planning.definition())
                if conversation_review.available(db, current) and not review_used:
                    definitions.append(conversation_review.definition())
                use_tools = (
                    bool(definitions)
                    and settings.agent_tools_enabled
                    and tool_rounds < settings.agent_tool_max_rounds + 2
                )
                request_messages = [messages[0], {"role": "system", "content": read_budget.hint()}, *messages[1:]]
                request: dict[str, Any] = {
                    **model_options(request_messages),
                    "stream": True,
                    "stream_options": {"include_usage": True},
                }
                if use_tools:
                    request.update(tools=definitions, tool_choice="auto")
                elif definitions and settings.agent_tools_enabled:
                    # 最后一轮显式禁止工具，不能通过省略 tools 隐式重启查询。
                    request.update(tools=definitions, tool_choice="none")
                parts = []
                calls_by_index: dict[int, dict[str, str]] = {}
                finish_reason = None
                round_usage = None
                async with slots:
                    upstream = await get_llm_client().chat.completions.create(**request)
                    try:
                        async for chunk in upstream:
                            model = chunk.model or model
                            if chunk.usage is not None:
                                # 用量是本次调用的累计快照，只在调用结束后合并一次。
                                round_usage = _usage(chunk)
                            if not chunk.choices:
                                continue
                            choice = chunk.choices[0]
                            if choice.delta.content:
                                text = choice.delta.content
                                emitted_chars += len(text)
                                if emitted_chars > 64000:
                                    raise AgentError(
                                        502,
                                        "LLM_REPLY_TOO_LARGE",
                                        "本轮输出超过长度预算，请缩小提问范围",
                                    )
                                parts.append(text)
                                await publish("delta", {"text": text})
                            if choice.delta.tool_calls:
                                merge_tool_deltas(calls_by_index, choice.delta.tool_calls)
                            if choice.finish_reason:
                                finish_reason = choice.finish_reason
                            if (
                                time.monotonic() - last_save
                                >= settings.agent_partial_save_interval_seconds
                            ):
                                await _save_partial(db, active, "".join(parts), model)
                                last_save = time.monotonic()
                    finally:
                        with suppress(Exception, asyncio.CancelledError):
                            await upstream.close()
                        upstream = None
                total_usage = _sum_usage(total_usage, round_usage)
                if not finish_reason:
                    raise AgentError(502, "LLM_INCOMPLETE_REPLY", "回复意外中断，请重新提问")
                if not calls_by_index:
                    if finish_reason == "tool_calls":
                        raise AgentError(502, "LLM_TOOL_CALL_INVALID", "模型没有返回完整的查询请求")
                    break
                if not use_tools or finish_reason != "tool_calls":
                    raise AgentError(
                        502, "LLM_TOOL_CALL_INCOMPLETE", "模型查询请求被截断或超过预算，未执行查询"
                    )
                calls = build_tool_calls(calls_by_index)
                content = "".join(parts)
                reservation = await _prepare_tools(db, active, content, calls, model, round_usage)
                messages.append(assistant_tool_message(content, calls))
                active = replace(active, assistant_message_id=reservation.next_assistant_id)
                parts = []
                round_usage = None
                queried = False
                for index, call in enumerate(calls):
                    await publish(
                        "tool_start",
                        {
                            "id": call.id,
                            "name": call.name,
                            "label": conversation_planning.TOOL_LABEL if call.name == conversation_planning.TOOL_NAME
                                else conversation_review.TOOL_LABEL if call.name == conversation_review.TOOL_NAME
                                else tool_service.label_for(call.name),
                            "resetReply": index == 0,
                        },
                    )
                    if call.name == conversation_planning.TOOL_NAME:
                        if planning_used or not conversation_planning.available(db, current):
                            result = ToolExecution(call.name, conversation_planning.TOOL_LABEL,
                                '{"error":"本轮已整理过规划或当前无权限，请基于已有结果回复"}',
                                "本轮不重复生成方案", True, 0)
                        else:
                            planning_used = True
                            result = await conversation_planning.execute(db, current, java_client,
                                conversation_id=started.conversation_id, turn_id=started.turn_id,
                                arguments=call.arguments, messages=messages,
                                summary=started.context_summary, slots=slots, emit=publish)
                    elif call.name == conversation_review.TOOL_NAME:
                        if review_used or not conversation_review.available(db, current):
                            result = ToolExecution(call.name, conversation_review.TOOL_LABEL,
                                '{"error":"本轮已发起复盘或当前没有权限"}', "本轮不重复发起复盘", True, 0)
                        else:
                            review_used = True
                            result = await conversation_review.execute(db, current, java_client,
                                conversation_id=started.conversation_id, arguments=call.arguments, emit=publish)
                    else:
                        cached = read_budget.cached(call.name, call.arguments)
                        if cached is not None:
                            fresh = await refresh_context(context, java_client)
                            spec = tool_service.tool_for(call.name)
                            if spec is not None and spec.allowed(fresh.permissions):
                                result = cached
                            else:
                                result = await tool_service.execute(fresh, java_client, call.name, call.arguments)
                        elif not read_budget.available:
                            result = tool_service.budget_exceeded(call.name)
                        else:
                            result = await tool_service.execute(current, java_client, call.name, call.arguments)
                            read_budget.remember(call.name, call.arguments, result)
                            queried = True
                    await _record_tool_result(
                        db, active, reservation, reservation.result_message_nos[index], call, result
                    )
                    messages.append(tool_result_message(call.id, result.content))
                    await publish(
                        "tool_end",
                        {
                            "id": call.id,
                            "name": result.name,
                            "label": result.label,
                            "status": "limited" if result.is_limited else "error" if result.is_error else "ok",
                            "durationMs": result.duration_ms,
                            "summary": result.summary,
                        },
                    )
                tool_rounds += 1
                if queried:
                    read_budget.rounds += 1
                last_save = time.monotonic()
            text = "".join(parts)
            if not text.strip():
                raise AgentError(502, "LLM_EMPTY_REPLY", "模型未返回有效回复，请重新提问")
            await refresh_context(context, java_client)
            reason = _safe_finish_reason(finish_reason)
            saved = await _finalize(
                db,
                active,
                status="COMPLETED" if reason == "stop" else "INCOMPLETE",
                finish_reason=reason,
                error_code=None if reason == "stop" else "LLM_OUTPUT_TRUNCATED",
                text=text,
                usage=round_usage,
                model=model,
            )
            if not saved:
                raise AgentError(409, "TURN_STATE_CONFLICT", "轮次状态已变化，请刷新会话")
            await publish("activity", {"id": "reply", "label": "组织回复", "status": "ok",
                                      "summary": "回复已保存"})
            return GeneratedReply(text, model, reason, total_usage)
    except asyncio.CancelledError:
        _best_effort_cancel(db, active, "".join(parts), model)
        raise
    except Exception as error:  # noqa: BLE001 - 对外边界脱敏，失败结果不能报告为成功。
        failure = safe_error(error, context.trace_id)
        with suppress(Exception):
            await _finalize(
                db,
                active,
                status="FAILED",
                finish_reason=None,
                error_code=_safe_error_code(failure.code),
                text="".join(parts),
                usage=round_usage,
                model=model,
            )
        raise failure from None


async def generate_plain(
    db: Database,
    context: AgentContext,
    started: StartedTurn,
    message: str,
    java_client: JavaApiClient,
    slots: asyncio.Semaphore,
) -> SendResultVO:
    current = asyncio.current_task()
    if current is not None:
        _RUNNING_TASKS[started.turn_id] = current
    try:
        result = await _generate(db, context, started, message, java_client, slots)
        if result.finish_reason == "stop":
            _schedule_summary(db, started, slots)
        return SendResultVO(
            reply=result.reply,
            model=result.model,
            finish_reason=result.finish_reason,
            conversation_id=str(started.conversation_id),
            turn_id=str(started.turn_id),
            request_id=started.request_id,
            turn_version=started.turn_version + 1,
        )
    finally:
        _RUNNING_TASKS.pop(started.turn_id, None)


async def stream_turn(
    db: Database,
    context: AgentContext,
    started: StartedTurn,
    message: str,
    java_client: JavaApiClient,
    slots: asyncio.Semaphore,
) -> AsyncIterator[bytes]:
    queue: asyncio.Queue[tuple[str, dict[str, Any]]] = asyncio.Queue(maxsize=32)
    consumer_closed = False

    async def emit(event: str, data: dict[str, Any]) -> None:
        await queue.put((event, data))

    async def produce() -> None:
        try:
            result = await _generate(db, context, started, message, java_client, slots, emit)
            await emit(
                "message_end",
                {
                    "conversationId": str(started.conversation_id),
                    "turnId": str(started.turn_id),
                    "requestId": started.request_id,
                    "turnVersion": started.turn_version + 1,
                    "model": result.model,
                    "finishReason": result.finish_reason,
                    "complete": result.finish_reason == "stop",
                    "usage": result.usage,
                    "citations": started.context_metadata.get("citations", []),
                    "usedMemoryIds": started.context_metadata.get("memoryIds", []),
                    "traceId": context.trace_id,
                },
            )
        except asyncio.CancelledError:
            # 连接关闭后无人消费队列，不能再阻塞等待写入停止事件。
            if not consumer_closed:
                await emit(
                    "error",
                    {
                        "code": "TURN_CANCELLED",
                        "message": "本轮生成已停止",
                        "status": 409,
                        "traceId": context.trace_id,
                        "conversationId": str(started.conversation_id),
                        "turnId": str(started.turn_id),
                    },
                )
        except Exception as error:  # noqa: BLE001 - SSE 开始后使用错误事件终止。
            failure = safe_error(error, context.trace_id)
            await emit(
                "error",
                {
                    "code": failure.code,
                    "message": failure.message,
                    "status": failure.status_code,
                    "traceId": context.trace_id,
                    "conversationId": str(started.conversation_id),
                    "turnId": str(started.turn_id),
                },
            )

    producer = asyncio.create_task(produce(), name=f"agent-turn-{started.turn_id}")
    _RUNNING_TASKS[started.turn_id] = producer
    try:
        yield event_frame(
            "message_start",
            {
                "traceId": context.trace_id,
                "model": started.model,
                "conversationId": str(started.conversation_id),
                "turnId": str(started.turn_id),
                "requestId": started.request_id,
                "turnVersion": started.turn_version,
            },
        )
        while True:
            try:
                event, data = await asyncio.wait_for(queue.get(), timeout=10.0)
            except TimeoutError:
                if producer.done():
                    yield event_frame(
                        "error",
                        {
                            "code": "STREAM_INCOMPLETE",
                            "message": "回复连接已结束，请刷新会话",
                            "status": 502,
                            "traceId": context.trace_id,
                        },
                    )
                    return
                yield b": keep-alive\n\n"
                continue
            yield event_frame(event, data)
            if event in {"message_end", "error"}:
                if event == "message_end" and data.get("complete") is True:
                    _schedule_summary(db, started, slots)
                return
    finally:
        consumer_closed = True
        _RUNNING_TASKS.pop(started.turn_id, None)
        if not producer.done():
            producer.cancel()
        with suppress(asyncio.CancelledError):
            await producer


async def stream_replay(replayed: ReplayedTurn, trace_id: str) -> AsyncIterator[bytes]:
    """已完成的同一请求直接重放已保存结果，不再次调用模型。"""
    yield event_frame(
        "message_start",
        {
            "traceId": trace_id,
            "model": replayed.model,
            "conversationId": str(replayed.conversation_id),
            "turnId": str(replayed.turn_id),
            "requestId": replayed.request_id,
            "turnVersion": replayed.turn_version,
        },
    )
    if replayed.reply:
        yield event_frame("delta", {"text": replayed.reply})
    yield event_frame(
        "message_end",
        {
            "conversationId": str(replayed.conversation_id),
            "turnId": str(replayed.turn_id),
            "requestId": replayed.request_id,
            "turnVersion": replayed.turn_version,
            "model": replayed.model,
            "finishReason": replayed.finish_reason or "stop",
            "complete": True,
            "usage": replayed.usage,
            "traceId": trace_id,
        },
    )


def replay_result(replayed: ReplayedTurn) -> SendResultVO:
    return SendResultVO(
        reply=replayed.reply,
        model=replayed.model or "",
        finish_reason=replayed.finish_reason or "stop",
        conversation_id=str(replayed.conversation_id),
        turn_id=str(replayed.turn_id),
        request_id=replayed.request_id,
        turn_version=replayed.turn_version,
    )


async def get_turn(db: Database, user_id: int, conversation_id: int, turn_id: int) -> TurnVO:
    await conversation_service.normalize_expired(db, user_id, conversation_id)
    async with db.session() as session:
        await conversation_repository.require_owned(session, user_id, conversation_id)
        turn = await turn_repository.find_owned(session, user_id, conversation_id, turn_id)
        if turn is None:
            raise AgentError(404, "TURN_NOT_FOUND", "轮次不存在或不可访问")
        assistant = await message_repository.assistant_for_turn(
            session, user_id=user_id, turn_id=turn_id
        )
        return conversation_service.turn_vo(turn, assistant.content if assistant else None)


async def cancel_turn(
    db: Database, user_id: int, conversation_id: int, turn_id: int, version: int
) -> TurnVO:
    async with db.session() as session:
        await conversation_repository.require_owned(
            session, user_id, conversation_id, for_update=True
        )
        turn = await turn_repository.find_owned(session, user_id, conversation_id, turn_id)
        if turn is None:
            raise AgentError(404, "TURN_NOT_FOUND", "轮次不存在或不可访问")
        if turn.version != version:
            raise AgentError(409, "STATE_CONFLICT", "轮次状态已变化，请刷新后重试")
        if turn.status == "CANCELLED":
            return conversation_service.turn_vo(turn)
        if turn.status != "RUNNING":
            raise AgentError(409, "STATE_CONFLICT", "轮次已经结束，不能取消")
        cancelled = await turn_repository.cancel(session, turn_id=turn_id, expected_version=version)
        if not cancelled:
            raise AgentError(409, "STATE_CONFLICT", "轮次状态已变化，请刷新后重试")
        await session.refresh(turn)
        result = conversation_service.turn_vo(turn)

    task = _RUNNING_TASKS.get(turn_id)
    if task is not None and not task.done():
        task.cancel()
    return result
