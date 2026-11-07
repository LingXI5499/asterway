"""执行只读工具：当前执行、执行历史、时间段与耗时汇总。

只读取 Java 执行事实；不开始、暂停、继续、结束或补写任何执行记录。
今天/本周与日期区间使用 Java 账号有效时区解析。
"""

import json
import re
from datetime import date, timedelta
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient
from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec

_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
RunStatus = Literal["RUNNING", "PAUSED", "FINISHED"]
RelativePeriod = Literal["TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK"]
ListPeriod = Literal["ALL", "TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK", "CUSTOM"]
Scope = Literal["TASK", "STAGE", "GOAL"]

_STATUS_LABELS = {
    "RUNNING": "进行中",
    "PAUSED": "已暂停",
    "FINISHED": "已结束",
}
_REASON_LABELS = {
    "USER_PAUSE": "用户暂停",
    "AUTO_PAUSE": "心跳超时自动暂停",
    "PLAN_BOUNDARY": "到计划边界自动暂停",
    "AUTH_REVOKED": "登录失效自动暂停",
    "FINISH": "用户结束",
    "USER_FINISH": "用户结束",
    "TASK_DELETED": "任务删除后结束",
    "GOAL_ARCHIVED": "目标归档后结束",
}
_SESSION_FIELDS = (
    "id",
    "taskId",
    "taskName",
    "status",
    "startedAt",
    "finishedAt",
    "finishReason",
    "actualSeconds",
    "rangeActualSeconds",
    "version",
)
_ACTIVE_FIELDS = (
    "lastHeartbeatAt",
    "activeSegmentId",
    "activeSegmentStartedAt",
    "heartbeatDeadline",
    "eligibleUntil",
    "canHeartbeat",
    "serverNow",
)


# ---------- 参数模型 ----------


class CurrentExecutionArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


class ExecutionListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    status: RunStatus | None = Field(
        default=None, description="执行状态：RUNNING 进行中、PAUSED 已暂停、FINISHED 已结束"
    )
    taskId: int | None = Field(default=None, description="只看某个任务的执行")
    goalId: int | None = Field(default=None, description="只看某个目标下的执行")
    stageId: int | None = Field(default=None, description="只看某个阶段下的执行")
    period: ListPeriod = Field(
        default="ALL",
        description="日期范围：ALL 全部、TODAY 今天、YESTERDAY 昨天、THIS_WEEK 本周、LAST_WEEK 上周、CUSTOM 自定义",
    )
    dateFrom: str | None = Field(default=None, description="CUSTOM 时必填，开始日期 YYYY-MM-DD")
    dateTo: str | None = Field(default=None, description="CUSTOM 时必填，结束日期 YYYY-MM-DD")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    @model_validator(mode="after")
    def validate_period(self) -> "ExecutionListArgs":
        if self.period == "CUSTOM":
            if self.dateFrom is None or self.dateTo is None:
                raise ValueError("period=CUSTOM 时必须同时提供 dateFrom 与 dateTo")
        elif self.dateFrom is not None or self.dateTo is not None:
            raise ValueError("只有 period=CUSTOM 时才能提供 dateFrom/dateTo")
        for value in (self.dateFrom, self.dateTo):
            if value is not None and not _DATE.fullmatch(value):
                raise ValueError("日期必须是 YYYY-MM-DD")
        return self


class ExecutionDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    executionId: int = Field(description="执行记录 ID，来自执行历史列表")


class SegmentListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    executionId: int = Field(description="执行记录 ID，来自执行历史列表")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class ExecutionSummaryArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    scope: Scope | None = Field(
        default=None, description="汇总范围：TASK 任务、STAGE 阶段、GOAL 目标；不传为本人全部执行"
    )
    scopeId: int | None = Field(default=None, description="scope 对应的任务/阶段/目标 ID")
    period: RelativePeriod | Literal["CUSTOM"] = Field(
        default="TODAY", description="统计区间：今天/昨天/本周/上周/自定义"
    )
    dateFrom: str | None = Field(default=None, description="CUSTOM 时必填，开始日期 YYYY-MM-DD")
    dateTo: str | None = Field(default=None, description="CUSTOM 时必填，结束日期 YYYY-MM-DD")

    @model_validator(mode="after")
    def validate_mode(self) -> "ExecutionSummaryArgs":
        if (self.scope is None) != (self.scopeId is None):
            raise ValueError("scope 与 scopeId 必须同时提供或同时省略")
        if self.period == "CUSTOM":
            if self.dateFrom is None or self.dateTo is None:
                raise ValueError("period=CUSTOM 时必须同时提供 dateFrom 与 dateTo")
        elif self.dateFrom is not None or self.dateTo is not None:
            raise ValueError("只有 period=CUSTOM 时才能提供 dateFrom/dateTo")
        for value in (self.dateFrom, self.dateTo):
            if value is not None and not _DATE.fullmatch(value):
                raise ValueError("日期必须是 YYYY-MM-DD")
        return self


# ---------- 公共辅助 ----------


def _parse(value: str | None) -> date | None:
    if not value or not _DATE.fullmatch(value):
        return None
    try:
        return date.fromisoformat(value)
    except ValueError:
        return None


def _monday(value: date) -> date:
    return value - timedelta(days=value.weekday())


async def _range(
    context: AgentContext,
    client: JavaApiClient,
    period: str,
    date_from: str | None,
    date_to: str | None,
) -> tuple[dict[str, str] | None, str | None, str | None]:
    """解析统计区间；返回 (dateFrom/dateTo 参数, 有效时区, 错误说明)。"""
    if period == "CUSTOM":
        start = _parse(date_from)
        end = _parse(date_to)
        if start is None or end is None:
            return None, None, "自定义区间日期格式不正确，请使用 YYYY-MM-DD"
        if start > end:
            return None, None, "自定义区间开始日期不能晚于结束日期"
        return {"dateFrom": start.isoformat(), "dateTo": end.isoformat()}, None, None
    if period == "ALL":
        return None, None, None
    if "dashboard:board:read" not in context.permissions:
        return None, None, "当前账户无法读取账号今天日期，请改用 CUSTOM 明确日期"
    today_value, time_zone, error_message = await shared.today(context, client)
    if error_message is not None:
        return None, None, error_message
    today = _parse(today_value) if today_value else None
    if today is None:
        return None, None, "账号今天日期格式不正确"
    monday = _monday(today)
    if period == "TODAY":
        start = end = today
    elif period == "YESTERDAY":
        start = end = today - timedelta(days=1)
    elif period == "THIS_WEEK":
        start, end = monday, monday + timedelta(days=6)
    elif period == "LAST_WEEK":
        start, end = monday - timedelta(days=7), monday - timedelta(days=1)
    else:
        return None, None, "不支持的统计区间"
    return {"dateFrom": start.isoformat(), "dateTo": end.isoformat()}, time_zone, None


def _session_item(row: Any, *, active: bool = False) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    item = {key: row[key] for key in _SESSION_FIELDS if row.get(key) is not None}
    status = row.get("status")
    if isinstance(status, str):
        item["statusLabel"] = _STATUS_LABELS.get(status, status)
    reason = row.get("finishReason")
    if isinstance(reason, str) and reason:
        item["finishReasonLabel"] = _REASON_LABELS.get(reason, reason)
    if active:
        for key in _ACTIVE_FIELDS:
            if row.get(key) is not None:
                item[key] = row[key]
    return item


def _segment_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    item: dict[str, Any] = {
        key: row[key]
        for key in (
            "id",
            "segmentNo",
            "startedAt",
            "endedAt",
            "effectiveEndedAt",
            "actualSeconds",
            "endReason",
            "timeZone",
        )
        if row.get(key) is not None
    }
    reason = row.get("endReason")
    if isinstance(reason, str) and reason:
        item["endReasonLabel"] = _REASON_LABELS.get(reason, reason)
    item["open"] = row.get("endedAt") is None
    return item


def _minutes(seconds: Any) -> float | None:
    if isinstance(seconds, int) and not isinstance(seconds, bool):
        return round(seconds / 60, 2)
    return None


# ---------- 工具实现 ----------


async def run_current_execution(
    context: AgentContext, client: JavaApiClient, _args: CurrentExecutionArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, "executions/current")
    if error_message is not None:
        return shared.error(error_message, "当前执行查询失败")
    data = result.data if result is not None else None
    current = _session_item(data, active=True)
    if current is None:
        payload = {"currentExecution": None, "note": "当前没有正在进行的执行（可能已暂停或尚未开始）"}
        return ToolOutcome(
            content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            summary="当前没有正在执行的任务",
        )
    return ToolOutcome(
        content=json.dumps({"currentExecution": current}, ensure_ascii=False, separators=(",", ":")),
        summary=f"正在执行「{current.get('taskName', '')}」",
    )


async def run_list_executions(
    context: AgentContext, client: JavaApiClient, args: ExecutionListArgs
) -> ToolOutcome:
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    if args.status:
        params["status"] = args.status
    if args.taskId is not None:
        params["taskId"] = args.taskId
    if args.goalId is not None:
        params["goalId"] = args.goalId
    if args.stageId is not None:
        params["stageId"] = args.stageId
    time_zone = None
    if args.period != "ALL":
        date_range, time_zone, error_message = await _range(
            context, client, args.period, args.dateFrom, args.dateTo
        )
        if error_message is not None:
            return shared.error(error_message, "执行历史查询失败")
        assert date_range is not None
        params.update(date_range)
    result, error_message = await shared.get(context, client, "executions", params)
    if error_message is not None:
        return shared.error(error_message, "执行历史查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("执行历史返回结构不正确", "执行历史返回异常")
    items = [item for row in data["items"] if (item := _session_item(row)) is not None]
    payload = shared.page(items, data.get("total"))
    payload["period"] = args.period
    if args.period != "ALL":
        payload["dateRange"] = {"dateFrom": params.get("dateFrom"), "dateTo": params.get("dateTo")}
    if time_zone is not None:
        payload["timeZone"] = time_zone
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"执行记录 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_get_execution(
    context: AgentContext, client: JavaApiClient, args: ExecutionDetailArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, f"executions/{args.executionId}")
    if error_message is not None:
        return shared.error(error_message, "执行详情查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("execution"), dict):
        return shared.error("执行详情返回结构不正确", "执行详情返回异常")
    execution = _session_item(data["execution"], active=True)
    segment_page = data.get("segments") if isinstance(data.get("segments"), dict) else {}
    segment_items = [
        item
        for row in segment_page.get("items", [])
        if (item := _segment_item(row)) is not None
    ]
    total = segment_page.get("total")
    payload: dict[str, Any] = {
        "execution": execution,
        "segments": {
            "total": total if isinstance(total, int) else len(segment_items),
            "returned": len(segment_items),
            "hasMore": isinstance(total, int) and total > len(segment_items),
            "page": 1,
            "items": segment_items,
        },
    }
    if isinstance(total, int) and total > len(segment_items):
        payload["segments"]["note"] = "仅返回首屏时间段，更多时间段请用 list_execution_segments"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"执行记录「{(execution or {}).get('taskName', '')}」详情与时间段",
    )


async def run_list_segments(
    context: AgentContext, client: JavaApiClient, args: SegmentListArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context,
        client,
        f"executions/{args.executionId}/segments",
        {"page": 1, "size": args.limit},
    )
    if error_message is not None:
        return shared.error(error_message, "执行时间段查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("执行时间段返回结构不正确", "执行时间段返回异常")
    items = [item for row in data["items"] if (item := _segment_item(row)) is not None]
    payload = shared.page(items, data.get("total"))
    payload["executionId"] = str(args.executionId)
    if any(item.get("open") for item in items):
        payload["note"] = "存在未结束时间段，实际秒数按服务端有效截止时间计算"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"执行时间段 {payload['returned']} 段（共 {payload['total']} 段）",
    )


async def run_execution_summary(
    context: AgentContext, client: JavaApiClient, args: ExecutionSummaryArgs
) -> ToolOutcome:
    date_range, time_zone, error_message = await _range(
        context, client, args.period, args.dateFrom, args.dateTo
    )
    if error_message is not None:
        return shared.error(error_message, "执行耗时查询失败")
    assert date_range is not None
    params: dict[str, str | int] = dict(date_range)
    if args.scope == "TASK":
        params["taskId"] = args.scopeId
    elif args.scope == "STAGE":
        params["stageId"] = args.scopeId
    elif args.scope == "GOAL":
        params["goalId"] = args.scopeId
    result, error_message = await shared.get(context, client, "executions/summary", params)
    if error_message is not None:
        return shared.error(error_message, "执行耗时查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict):
        return shared.error("执行汇总返回结构不正确", "执行汇总返回异常")
    payload: dict[str, Any] = {
        "scope": args.scope or "ALL",
        "scopeId": str(args.scopeId) if args.scopeId is not None else None,
        "period": args.period,
        "dateRange": {
            "dateFrom": date_range.get("dateFrom"),
            "dateTo": date_range.get("dateTo"),
        },
        "totalActualSeconds": data.get("totalActualSeconds"),
        "totalActualMinutes": _minutes(data.get("totalActualSeconds")),
        "rangeActualSeconds": data.get("rangeActualSeconds"),
        "rangeActualMinutes": _minutes(data.get("rangeActualSeconds")),
        "totalSessions": data.get("totalSessions"),
        "rangeSessions": data.get("rangeSessions"),
        "finishedSessions": data.get("finishedSessions"),
        "runningSessions": data.get("runningSessions"),
        "pausedSessions": data.get("pausedSessions"),
        "latestExecutionAt": data.get("latestExecutionAt"),
        "note": (
            "totalActualSeconds/totalSessions/finishedSessions/runningSessions/pausedSessions 为 scope 内全部历史累计；"
            "rangeActualSeconds/rangeSessions 为所选日期区间内。totalSessions 是执行会话数，不是时间段数量。"
        ),
    }
    if time_zone is not None:
        payload["timeZone"] = time_zone
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"{args.period} 范围内实际投入 {payload['rangeActualSeconds']} 秒",
    )


# ---------- 工具声明 ----------


CURRENT_EXECUTION_TOOL = ToolSpec(
    name="get_current_execution",
    label="当前执行",
    description=(
        "查询用户本人当前正在进行的执行计时：任务名称、开始时间、累计实际秒数、当前时间段与心跳截止时间；"
        "没有正在执行时返回空并说明可能已暂停或未开始。工具只读，不会开始、暂停或结束计时。"
        "用户询问现在在做什么、当前计时情况时使用。"
    ),
    permission="execution:session:read",
    args_model=CurrentExecutionArgs,
    handler=run_current_execution,
)

EXECUTION_LIST_TOOL = ToolSpec(
    name="list_executions",
    label="执行历史",
    description=(
        "查询用户本人的执行记录列表，可按状态（进行中/已暂停/已结束）、任务、目标、阶段和执行日期范围筛选，"
        "最多返回 20 条并给出总条数。返回任务名称、状态、开始/结束时间、实际秒数；使用日期范围时同时返回区间内秒数。"
        "日期范围支持今天/昨天/本周/上周或自定义；今天/本周取自 Java 账号有效时区。"
        "用户询问今天/本周执行记录、某任务执行过几次、执行历史时使用。"
    ),
    permission="execution:session:list",
    args_model=ExecutionListArgs,
    handler=run_list_executions,
)

EXECUTION_DETAIL_TOOL = ToolSpec(
    name="get_execution",
    label="执行详情",
    description=(
        "按执行记录 ID 查询详情：任务、状态、开始/结束时间、累计与区间实际秒数、当前时间段，以及首屏执行时间段。"
        "执行记录 ID 必须来自本人执行历史。时间段超过首屏时用 list_execution_segments 继续查询。"
        "用户询问某次执行的详细情况时使用。"
    ),
    permission="execution:session:read",
    args_model=ExecutionDetailArgs,
    handler=run_get_execution,
)

SEGMENT_LIST_TOOL = ToolSpec(
    name="list_execution_segments",
    label="执行时间段",
    description=(
        "按执行记录 ID 查询时间段（开始/结束时间、实际秒数、结束原因和时区），最多返回 20 段并给出总秒数。"
        "结束原因区分用户暂停、心跳超时自动暂停、到计划边界暂停、登录失效暂停、用户结束、任务删除或目标归档。"
        "未结束时间段显示 open=true，实际秒数按服务端有效截止时间计算。"
        "用户询问某次执行暂停过几次、为什么暂停、每段实际投入时使用。"
    ),
    permission="execution:session:read",
    args_model=SegmentListArgs,
    handler=run_list_segments,
)

EXECUTION_SUMMARY_TOOL = ToolSpec(
    name="get_execution_summary",
    label="执行耗时汇总",
    description=(
        "查询本人执行耗时汇总，可按任务/阶段/目标限定范围，并按今天/昨天/本周/上周或自定义日期区间统计。"
        "同时返回总累计（scope 内全部历史）与所选区间内实际秒数、会话数量、已结束/进行中/已暂停数量，"
        "并区分执行会话数与时间段数。耗时只读取执行事实，不用任务当前状态倒推历史完成情况。"
        "用户询问累计投入、今天/本周耗时、某任务/目标花了多少时间时使用。"
    ),
    permission="execution:session:read",
    args_model=ExecutionSummaryArgs,
    handler=run_execution_summary,
)
