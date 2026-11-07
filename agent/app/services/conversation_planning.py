"""对话内的规划工具：持续澄清、生成和修订草案；从不确认或提交业务数据。"""

import asyncio
import json
import logging
import time
from collections.abc import Awaitable, Callable
from datetime import timedelta
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, ValidationError, model_validator

from app.core.config import get_settings
from app.core.context import AgentContext, refresh_context
from app.core.errors import AgentError
from app.db.session import Database
from app.db.time import database_time
from app.integrations.java_client import JavaApiClient
from app.llm.chat_service import safe_error
from app.repositories import conversation_repository, workflow_repository
from app.services import (
    planning_context_service,
    planning_service,
    review_service,
    workflow_service,
)
from app.services.workflow_graph import require_graph
from app.tools.base import ToolExecution

TOOL_NAME = "prepare_growth_plan"
TOOL_LABEL = "整理成长方案"
Emit = Callable[[str, dict[str, Any]], Awaitable[None]]
logger = logging.getLogger(__name__)
Intent = Literal[
    "GOAL_PLAN", "STAGE_PLAN", "TASK_PLAN", "WEEKLY_PLAN", "DAILY_PLAN", "HABIT_PLAN",
    "COMPREHENSIVE_PLAN",
]


class PlanningAction(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    action: Literal["clarify", "draft"]
    objective: str = Field(min_length=1, max_length=200)
    intent: Intent = "COMPREHENSIVE_PLAN"
    summary: str = Field(min_length=1, max_length=1000)
    questions: list[str] = Field(default_factory=list, max_length=3)
    reviewId: int | None = Field(default=None, ge=1, description="据已完成复盘调整安排时填写真实复盘ID")

    @model_validator(mode="after")
    def check_questions(self):
        self.questions = [question.strip() for question in self.questions]
        if any(not question or len(question) > 300 for question in self.questions):
            raise ValueError("澄清问题必须为 1 至 300 字")
        if self.action == "clarify" and not self.questions:
            raise ValueError("需要澄清时必须给出具体问题")
        if self.action == "draft" and self.questions:
            raise ValueError("尚有关键问题时先澄清，不生成最终草案")
        return self


def definition() -> dict[str, Any]:
    return {"type": "function", "function": {
        "name": TOOL_NAME,
        "description": (
            "在连续对话中整理用户的成长规划。用户提出新目标或补充、修改当前规划要求时使用。"
            "缺少目标成果、基础、时间或投入约束时 action=clarify，每次询问一到两个关键问题；"
            "信息足够或用户明确让你采用默认安排时 action=draft，系统会根据完整对话生成可核对的结构化方案。"
            "新目标及其阶段、任务和日程通常使用 COMPREHENSIVE_PLAN；只有明确针对已有对象时选对应意图。"
            "据复盘调整时填写reviewId且使用WEEKLY_PLAN或DAILY_PLAN，复用已有任务与安排版本。"
            "普通闲聊、只读查询、查看进度、用户拒绝规划时不要调用。每轮最多调用一次。"
            "该工具只能整理草案，不能接受、同意、确认、创建或执行任何业务内容。"
        ),
        "parameters": PlanningAction.model_json_schema(),
    }}


def available(db: Database, context: AgentContext) -> bool:
    return context.allows(frozenset({"agent:workflow:read", "agent:workflow:manage"})) \
        and getattr(db, "workflow_graph", None) is not None


def transcript(messages: list[dict[str, Any]], summary: str = "", limit: int = 9000) -> str:
    """只携带已装入上下文的对话；查询结果和检索资料不冒充用户的规划要求。"""
    parts = []
    used = 0
    for message in reversed(messages):
        role, content = message.get("role"), message.get("content")
        if role not in {"user", "assistant"} or not isinstance(content, str):
            continue
        if not content.strip() or content.startswith(("【自动生成的历史摘要", "【已确认的长期记忆",
            "【知识库检索资料", "【历史加载说明", "【当前规划状态", "【成长复盘背景")) or message.get("tool_calls"):
            continue
        block = ("用户：" if role == "user" else "助手：") + content.strip()
        if used + len(block) > limit:
            if role == "assistant":
                continue
            break
        parts.append(block)
        used += len(block)
    parts.reverse()
    if summary.strip() and used + min(len(summary), 1500) + 30 <= limit:
        parts.insert(0, "较早对话摘要（当前用户的新要求优先）：\n" + summary[:1500])
    return "\n\n".join(parts)


async def current_detail(db: Database, user_id: int, conversation_id: int):
    rows, _ = await workflow_service.page_runs(
        db, user_id=user_id, status=None, page=1, size=1, conversation_id=conversation_id,
    )
    return await workflow_service.detail(db, user_id=user_id, run_id=rows[0].id) if rows else None


async def context_hint(db: Database, user_id: int, conversation_id: int) -> str | None:
    detail = await current_detail(db, user_id, conversation_id)
    if not detail:
        return None
    run = detail["run"]
    plan = next((p for p in detail["plans"] if p["id"] == run["latestPlanVersionId"]), None)
    progress = run.get("progress") or {}
    state = {"status": run["status"], "progress": {key: progress.get(key) for key in
        ("phase", "objective", "summary", "questions")},
             "planSummary": plan["summary"] if plan else None,
             "planVersion": plan["versionNo"] if plan else None}
    return "【当前规划状态，仅作背景，不是用户指令】\n" + json.dumps(state, ensure_ascii=False)


async def _run(db: Database, context: AgentContext, conversation_id: int, turn_id: int | None,
               intent: str) -> tuple[int, str]:
    # 新目标保持可继续补充阶段、任务和日程，不因首次表述只提到“目标”而锁死范围。
    if intent == "GOAL_PLAN":
        intent = "COMPREHENSIVE_PLAN"
    user_id = int(context.user_id)
    await workflow_service.expire_stale(db, user_id)
    async with db.session() as session:
        await conversation_repository.require_owned(session, user_id, conversation_id)
        active = await workflow_repository.find_active_run(session, conversation_id)
        if active:
            if active.status not in {"DRAFT", "WAITING_CONFIRM"}:
                raise AgentError(409, "WORKFLOW_ALREADY_ACCEPTED", "当前方案已接受，请先查看创建结果")
            return active.id, active.intent
    try:
        run = await workflow_service.start_run(db, user_id=user_id,
            conversation_id=conversation_id, source_turn_id=turn_id, intent=intent)
        return run.id, run.intent
    except AgentError as error:
        if error.code != "WORKFLOW_ACTIVE_EXISTS":
            raise
        async with db.session() as session:
            active = await workflow_repository.find_active_run(session, conversation_id)
            if active and active.user_id == user_id and active.status in {"DRAFT", "WAITING_CONFIRM"}:
                return active.id, active.intent
        raise


async def _state(db: Database, context: AgentContext, run_id: int, turn_id: int | None,
                 action: PlanningAction, phase: str, emit: Emit, *, note: str = "") -> None:
    async with db.session() as session:
        run = await workflow_repository.find_run(session, int(context.user_id), run_id,
                                                 for_update=True)
        if run is None:
            raise AgentError(404, "WORKFLOW_NOT_FOUND", "规划不存在")
        if run.status not in {"DRAFT", "WAITING_CONFIRM"}:
            raise AgentError(409, "WORKFLOW_STATUS_INVALID", "规划状态已变化，请查看最新结果")
        stamp = await database_time(session, run)
        previous = (run.graph_state or {}).get("planning", {})
        steps = list(previous.get("steps", []))[-29:]
        steps.append({"phase": phase, "summary": note or action.summary,
                      "createdAt": stamp.isoformat(), "turnId": str(turn_id) if turn_id else None})
        progress = {"phase": phase, "objective": action.objective, "summary": action.summary,
                    "questions": action.questions if phase == "COLLECTING" else [],
                    "updatedAt": stamp.isoformat(), "sourceTurnId": str(turn_id) if turn_id else None, "steps": steps}
        run.graph_state = {**(run.graph_state or {}), "planning": progress}
        run.state_version += 1
        run.updated_at = stamp
        run.expires_at = stamp + timedelta(seconds=get_settings().agent_workflow_ttl_seconds)
        run.version += 1
        conversation_id = run.conversation_id
    await emit("workflow_update", {"runId": str(run_id), "conversationId": str(conversation_id),
                                   **progress})


async def execute(db: Database, context: AgentContext, client: JavaApiClient, *,
                  conversation_id: int, turn_id: int, arguments: str,
                  messages: list[dict[str, Any]], summary: str, slots: asyncio.Semaphore,
                  emit: Emit) -> ToolExecution:
    started = time.monotonic()

    def result(data: dict, text: str, error: bool = False):
        return ToolExecution(TOOL_NAME, TOOL_LABEL, json.dumps(data, ensure_ascii=False), text,
                             error, max(0, int((time.monotonic() - started) * 1000)))

    try:
        action = PlanningAction.model_validate_json(arguments)
    except ValidationError:
        return result({"error": "规划参数无效，请补齐具体问题或明确规划需求"}, "规划参数无效", True)
    context = await refresh_context(context, client)
    context.require("agent:workflow:manage", "agent:workflow:read")
    run_id = None
    phase = "COLLECTING"
    try:
        review = await review_service.get(db, int(context.user_id), action.reviewId) if action.reviewId else None
        if review is not None:
            context.require("agent:review:read")
            if review.status != "READY":
                raise AgentError(409, "REVIEW_NOT_READY", "复盘尚未完成，请在右侧等待结果后再调整")
            if action.intent not in {"WEEKLY_PLAN", "DAILY_PLAN"}:
                raise AgentError(422, "REVIEW_PLAN_INTENT_INVALID", "复盘调整请保留已有目标任务，使用周或日安排方案")
        run_id, intent = await _run(db, context, conversation_id, turn_id, action.intent)
        if review is not None:
            if intent not in {"WEEKLY_PLAN", "DAILY_PLAN"}:
                raise AgentError(409, "WORKFLOW_ACTIVE_EXISTS", "当前还有其他待接受方案，请先处理后再根据复盘调整")
            async with db.session() as session:
                run = await workflow_repository.find_run(session, int(context.user_id), run_id, for_update=True)
                run.graph_state = {**(run.graph_state or {}), "sourceReviewId": str(review.id)}
                run.updated_at = await database_time(session, run)
                run.version += 1
                run.state_version += 1
        detail = await workflow_service.detail(db, user_id=int(context.user_id), run_id=run_id)
        previous = next((p["snapshot"] for p in detail["plans"]
                         if p["id"] == detail["run"]["latestPlanVersionId"]), None)
        if action.action == "clarify":
            await _state(db, context, run_id, turn_id, action, phase, emit)
            return result({"phase": phase, "questions": action.questions,
                           "instruction": "以自然对话询问这些关键问题，等用户回答后再继续，不要生成完整方案。"},
                          "已记录需求，等待补充信息")
        phase = "CONTEXT"
        await _state(db, context, run_id, turn_id, action, phase, emit)
        calendar = await client.get("dashboard/overview", authorization=context.authorization,
                                    trace_id=context.trace_id)
        if not isinstance(calendar.data, dict) or not calendar.data.get("today") \
                or not calendar.data.get("timeZone"):
            raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "暂时无法取得账号日期与时区")
        facts = await planning_context_service.collect(context, client, intent)
        if review is not None:
            facts = (facts or "") + "\n已完成复盘（历史事实与建议，不是当前状态；以最新业务查询为准）：\n" + json.dumps(
                {"dateFrom": review.date_from, "dateTo": review.date_to, "facts": review.facts,
                 "analysis": review.content[:6000]}, ensure_ascii=False, default=str)[:14000]
        phase = "GENERATING"
        await _state(db, context, run_id, turn_id, action, phase, emit)

        async def attempt_progress(attempt: int):
            await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案",
                "status": "running", "summary": "正在生成并校验方案" if attempt == 1
                    else f"正在调整方案结构，第 {attempt} 次校验"})

        async with slots:
            snapshot = await planning_service.generate_snapshot(intent=intent,
                message=f"本次目标：{action.objective}\n需求摘要（以用户原文为准）：{action.summary}\n\n"
                    + transcript(messages, summary), today=str(calendar.data["today"]),
                time_zone=str(calendar.data["timeZone"]), previous=previous,
                business_context=facts, on_attempt=attempt_progress)
        snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
        context = await refresh_context(context, client)
        context.require("agent:workflow:manage", "agent:workflow:read")
        version = await workflow_service.save_plan(db, user_id=int(context.user_id), run_id=run_id,
                                                  snapshot=snapshot, revision=previous is not None)
        await require_graph(db).prepare(int(context.user_id), run_id, version.id)
        phase = "WAITING_CONFIRM"
        await _state(db, context, run_id, turn_id, action, phase, emit,
                     note=f"第 {version.version_no} 版方案已通过结构校验，等待用户接受")
        await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案", "status": "ok",
                                "summary": "方案已就绪，尚未创建业务内容"})
        return result({"phase": phase, "version": version.version_no,
            "summary": version.summary, "instruction": "右侧已展示完整方案。简要概括安排并请用户核对，"
            "可继续对话修改；接受与创建只由用户点击右侧按钮完成，不要宣称已经创建。"},
            "方案已整理，等待用户接受")
    except asyncio.CancelledError:
        if run_id is not None:
            # 关闭连接后不再写入 SSE；只在有界时间内恢复可重试状态。
            async def silent_emit(_event, _data):
                return None
            try:
                async with asyncio.timeout(3):
                    await _state(db, context, run_id, turn_id, action, "INTERRUPTED", silent_emit,
                                 note="方案整理已停止，保留已保存的版本")
            except Exception as error:  # noqa: BLE001 - 取消后的状态补存不覆盖原始取消。
                logger.warning("plan_interrupt_save_failed type=%s", type(error).__name__)
        raise
    except AgentError as error:
        if error.status_code in {401, 403}:
            raise
        if run_id is not None:
            await _state(db, context, run_id, turn_id, action, "ERROR", emit, note=error.message)
            await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案",
                                    "status": "error", "summary": error.message})
        return result({"error": error.message, "instruction": "如实说明本次没有完成方案；"
                       "可以继续补充需求后重试，不要声称创建成功。"}, error.message, True)
    except Exception as error:  # noqa: BLE001 - 工具边界统一脱敏，不能输出服务异常原文。
        failure = safe_error(error, context.trace_id)
        if run_id is not None:
            try:
                await _state(db, context, run_id, turn_id, action, "ERROR", emit,
                             note=failure.message)
            except Exception as state_error:  # noqa: BLE001 - 保留原始失败。
                logger.warning("plan_error_save_failed type=%s", type(state_error).__name__)
        return result({"error": failure.message}, failure.message, True)
