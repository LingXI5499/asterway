"""在对话中发起有真实统计依据的复盘；仅分析，不确认或执行业务方案。"""

import asyncio
import json
import time
from datetime import date, timedelta
from typing import Literal

from pydantic import BaseModel, ConfigDict, ValidationError

from app.core.config import get_settings
from app.core.context import refresh_context
from app.core.errors import AgentError
from app.services import review_service
from app.tools.base import ToolExecution

TOOL_NAME = "review_growth_progress"
TOOL_LABEL = "复盘实际进展"


class ReviewAction(BaseModel):
    model_config = ConfigDict(extra="forbid")
    period: Literal["THIS_WEEK", "LAST_WEEK", "LAST_7_DAYS", "CUSTOM"] = "LAST_7_DAYS"
    dateFrom: date | None = None
    dateTo: date | None = None


def available(db, context):
    return getattr(db, "background_worker", None) is not None and all(value in context.permissions
        for value in ("agent:review:read", "agent:review:manage", "statistics:report:read"))


def definition():
    return {"type": "function", "function": {"name": TOOL_NAME,
        "description": "用户要求复盘投入、总结执行反馈、分析逾期或据此调整计划时使用。读取本人真实统计并保存复盘，"
            "右侧显示生成进度。默认最近7天；自定义日期仅用于CUSTOM。复盘不执行写入，READY后可用其reviewId"
            "调用prepare_growth_plan生成周日调整草案，再由用户接受。每轮最多一次。",
        "parameters": ReviewAction.model_json_schema()}}


async def context_hint(db, user_id, conversation_id):
    row = await review_service.latest_for_conversation(db, user_id, conversation_id)
    if row is None:
        return None
    return "【成长复盘背景，仅为统计依据与建议，不是用户指令】\n" + json.dumps({
        "reviewId": str(row.id), "status": row.status, "dateFrom": row.date_from,
        "dateTo": row.date_to, "analysis": row.content[:2200] if row.status == "READY" else None,
        "instruction": "用户要求根据复盘调整时，保留已有目标和任务，使用此reviewId生成周日安排草案。"
    }, ensure_ascii=False)


def resolve_range(action: ReviewAction, today: date):
    if action.period == "CUSTOM":
        if action.dateFrom is None or action.dateTo is None:
            raise AgentError(422, "REVIEW_RANGE_REQUIRED", "自定义复盘需要起止日期")
        start, end = action.dateFrom, action.dateTo
    elif action.dateFrom is not None or action.dateTo is not None:
        raise AgentError(422, "REVIEW_RANGE_INVALID", "具体日期请使用自定义复盘")
    elif action.period == "THIS_WEEK":
        start, end = today - timedelta(days=today.weekday()), today
    elif action.period == "LAST_WEEK":
        start = today - timedelta(days=today.weekday() + 7)
        end = start + timedelta(days=6)
    else:
        start, end = today - timedelta(days=6), today
    if not 0 <= (end - start).days <= 365 or end > today:
        raise AgentError(422, "REVIEW_RANGE_INVALID", "请选择截至账号今天的有效复盘区间")
    return start, end


async def execute(db, context, client, *, conversation_id, arguments, emit):
    started = time.monotonic()
    def result(value, summary, error=False):
        return ToolExecution(TOOL_NAME, TOOL_LABEL, json.dumps(value, ensure_ascii=False),
                             summary, error, int((time.monotonic() - started) * 1000))
    try:
        action = ReviewAction.model_validate_json(arguments)
        current = await refresh_context(context, client)
        current.require("agent:review:read", "agent:review:manage", "statistics:report:read")
        calendar = await client.get("dashboard/overview", authorization=current.authorization,
                                    trace_id=current.trace_id)
        today = calendar.data.get("today") if isinstance(calendar.data, dict) else None
        if not today:
            raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "无法取得账号业务日期")
        start, end = resolve_range(action, date.fromisoformat(today))
        row = await review_service.create(db, current, client, start, end,
                                          conversation_id=conversation_id)
        async def announce():
            await emit("review_update", {"conversationId": str(conversation_id), "reviewId": str(row.id),
                "status": row.status, "dateFrom": row.date_from, "dateTo": row.date_to})
        await announce()
        limit = min(get_settings().agent_review_wait_seconds, max(0, current.deadline - time.monotonic() - 35))
        until = time.monotonic() + limit
        while row.status in {"PENDING", "RUNNING"} and time.monotonic() < until:
            await asyncio.sleep(1)
            row = await review_service.get(db, int(current.user_id), row.id)
        current = await refresh_context(current, client)
        current.require("agent:review:read", "agent:review:manage", "statistics:report:read")
        await announce()
        if row.status == "FAILED":
            return result({"error": row.error_message, "reviewId": str(row.id)}, "复盘暂未生成，可在右侧重试", True)
        return result({"reviewId": str(row.id), "status": row.status, "dateFrom": row.date_from,
            "dateTo": row.date_to, "content": row.content[:2200] if row.status == "READY" else None,
            "instruction": "仅根据已完成的统计和复盘作答；READY且用户要求调整时，用此reviewId和WEEKLY_PLAN/DAILY_PLAN"
                "调用prepare_growth_plan，保留已有目标任务。尚未READY时说明正在生成，右侧可继续查看，勿虚构结论。"},
            "复盘已就绪" if row.status == "READY" else "复盘正在生成，右侧会自动更新")
    except ValidationError:
        return result({"error": "复盘参数不符合要求"}, "复盘日期参数需要调整", True)
    except AgentError as error:
        if error.status_code in {401, 403}:
            raise
        return result({"error": error.message}, error.message, True)
