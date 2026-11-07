"""今日看板工具：读取用户本人的今日任务、习惯、执行与当前执行摘要。"""

import json
from typing import Any

from pydantic import BaseModel, ConfigDict

from app.core.context import AgentContext
from app.core.errors import JavaApiError
from app.integrations.java_client import JavaApiClient, JavaApiResult
from app.tools.base import ToolOutcome, ToolSpec

_METRIC_KEYS = (
    "todayTasks",
    "todayCurrentCompletedTasks",
    "weekUndatedTasks",
    "actualSeconds",
    "habitDone",
    "habitDue",
    "habitCompletionRate",
    "unreadMessages",
)


class TodayOverviewArgs(BaseModel):
    """看板工具不需要参数；用户身份由服务端上下文确定。"""

    model_config = ConfigDict(extra="forbid", strict=True)


async def _get(
    client: JavaApiClient,
    context: AgentContext,
    path: str,
    params: dict[str, str | int | bool] | None = None,
) -> JavaApiResult:
    return await client.get(
        path,
        authorization=context.authorization,
        trace_id=context.trace_id,
        params=params,
    )


def _brief_task(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or not isinstance(row.get("name"), str):
        return None
    result: dict[str, Any] = {}
    for key in ("name", "status", "priority", "dueDate", "section"):
        if row.get(key) is not None:
            result[key] = row[key]
    return result


async def _section(
    client: JavaApiClient,
    context: AgentContext,
    section: str,
    size: int,
) -> tuple[list[dict[str, Any]], str | None]:
    try:
        result: JavaApiResult = await _get(
            client,
            context,
            "dashboard/tasks",
            {"section": section, "page": 1, "size": size},
        )
    except JavaApiError as error:
        if error.status_code == 401:
            raise
        return [], error.message
    data = result.data
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return [], "看板任务返回结构不正确"
    tasks = [brief for row in data["items"] if (brief := _brief_task(row)) is not None]
    return tasks, None


async def run_today_overview(
    context: AgentContext,
    client: JavaApiClient,
    _args: TodayOverviewArgs,
) -> ToolOutcome:
    try:
        overview: JavaApiResult = await _get(client, context, "dashboard/overview")
    except JavaApiError as error:
        if error.status_code == 401:
            raise
        return ToolOutcome(
            content=json.dumps({"error": error.message}, ensure_ascii=False),
            summary="今日看板查询失败",
            is_error=True,
        )
    data = overview.data
    if not isinstance(data, dict):
        return ToolOutcome(
            content=json.dumps({"error": "今日看板返回结构不正确"}, ensure_ascii=False),
            summary="今日看板返回异常",
            is_error=True,
        )
    metrics = data.get("metrics") if isinstance(data.get("metrics"), dict) else {}
    current = (
        data.get("currentExecution") if isinstance(data.get("currentExecution"), dict) else None
    )
    today_tasks, today_error = await _section(client, context, "TODAY", 5)
    week_tasks, week_error = await _section(client, context, "WEEK_UNDATED", 5)
    payload: dict[str, Any] = {
        "date": data.get("today"),
        "timeZone": data.get("timeZone"),
        "metrics": {key: metrics.get(key) for key in _METRIC_KEYS},
        "currentExecution": (
            {
                "taskName": current.get("taskName"),
                "actualSeconds": current.get("actualSeconds"),
            }
            if current
            else None
        ),
        "todayTasks": today_tasks,
        "weekUndatedTasks": week_tasks,
    }
    errors = [message for message in (today_error, week_error) if message]
    if errors:
        payload["errors"] = errors
    summary = f"今日任务 {metrics.get('todayTasks', 0)} 项"
    if metrics.get("habitDue"):
        summary += f"，习惯 {metrics.get('habitDone', 0)}/{metrics.get('habitDue')}"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=summary,
    )


TODAY_OVERVIEW_TOOL = ToolSpec(
    name="get_today_overview",
    label="今日看板",
    description=(
        "查询用户本人的今日看板：今日任务数、已完成数、本周未定日任务数、"
        "实际执行秒数、习惯完成情况、当前正在执行的任务，以及最多各 5 条"
        "今日任务和本周未定日任务。用户询问今天该做什么、今日进度、"
        "当前在执行什么时使用。"
    ),
    permission="dashboard:board:read",
    args_model=TodayOverviewArgs,
    handler=run_today_overview,
)
