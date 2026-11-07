"""计划只读工具：周计划、日计划、计划任务、待重新安排任务与任务当前安排。

所有工具只读取 Java 本人业务数据；今天/本周由 Java 看板返回的账号日期与有效时区解析。
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
TaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED"]
OpenTaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED"]
_PLANNING_TASK_FIELDS = (
    "assignmentId",
    "taskId",
    "taskName",
    "status",
    "priority",
    "weight",
    "goalId",
    "goalName",
    "stageId",
    "stageName",
    "weeklyPlanId",
    "dailyPlanId",
    "weekStartDate",
    "planDate",
    "sortOrder",
    "archivedAt",
)


# ---------- 参数模型 ----------


class WeeklyPlanListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    period: Literal["ALL", "CURRENT", "PREVIOUS", "NEXT"] = Field(
        default="ALL",
        description="周范围：CURRENT 本周、PREVIOUS 上周、NEXT 下周、ALL 全部；不传为 ALL",
    )
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class WeeklyPlanDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    weeklyPlanId: int | None = Field(
        default=None, description="周计划 ID；与 period 二选一"
    )
    period: Literal["CURRENT", "PREVIOUS", "NEXT"] | None = Field(
        default=None, description="按相对周查询：本周/上周/下周；与 weeklyPlanId 二选一"
    )

    @model_validator(mode="after")
    def validate_mode(self) -> "WeeklyPlanDetailArgs":
        if (self.weeklyPlanId is None) == (self.period is None):
            raise ValueError("weeklyPlanId 与 period 必须且只能提供一个")
        return self


class DailyPlanArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    dailyPlanId: int | None = Field(
        default=None, description="日计划 ID，来自周计划任务列表或日计划结果"
    )
    weeklyPlanId: int | None = Field(
        default=None, description="周计划 ID；指定后按该周内日期查询日计划"
    )
    planDate: str | None = Field(
        default=None, description="日期 YYYY-MM-DD；不传且未给 relativeDay 时默认今天"
    )
    relativeDay: Literal["TODAY", "TOMORROW", "YESTERDAY"] | None = Field(
        default=None, description="相对今天：今天/明天/昨天；与 planDate 二选一"
    )

    @model_validator(mode="after")
    def validate_mode(self) -> "DailyPlanArgs":
        if self.dailyPlanId is not None:
            if (
                self.weeklyPlanId is not None
                or self.planDate is not None
                or self.relativeDay is not None
            ):
                raise ValueError("dailyPlanId 不能与日期或周计划参数同时使用")
            return self
        if self.planDate is not None and self.relativeDay is not None:
            raise ValueError("planDate 与 relativeDay 只能提供一个")
        if self.planDate is not None and not _DATE.fullmatch(self.planDate):
            raise ValueError("planDate 必须是 YYYY-MM-DD")
        return self


class PlanningTasksArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    weeklyPlanId: int | None = Field(default=None, description="周计划 ID；与日计划二选一")
    dailyPlanId: int | None = Field(default=None, description="日计划 ID；与周计划二选一")
    status: TaskStatus | None = Field(default=None, description="任务状态筛选")
    keyword: str | None = Field(default=None, max_length=50, description="任务名称关键字")
    goalId: int | None = Field(default=None, description="只看某个目标下的计划任务")
    stageId: int | None = Field(default=None, description="只看某个阶段下的计划任务")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    @model_validator(mode="after")
    def validate_mode(self) -> "PlanningTasksArgs":
        if (self.weeklyPlanId is None) == (self.dailyPlanId is None):
            raise ValueError("weeklyPlanId 与 dailyPlanId 必须且只能提供一个")
        return self


class PendingTasksArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    status: OpenTaskStatus | None = Field(
        default=None, description="任务状态筛选：未开始、进行中或暂停"
    )
    keyword: str | None = Field(default=None, max_length=50, description="任务名称关键字")
    goalId: int | None = Field(default=None, description="只看某个目标下的任务")
    stageId: int | None = Field(default=None, description="只看某个阶段下的任务")
    dateFrom: str | None = Field(
        default=None, description="原安排日期下限 YYYY-MM-DD，可省略"
    )
    dateTo: str | None = Field(
        default=None, description="原安排日期上限 YYYY-MM-DD，可省略"
    )
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    @model_validator(mode="after")
    def validate_dates(self) -> "PendingTasksArgs":
        for value in (self.dateFrom, self.dateTo):
            if value is not None and not _DATE.fullmatch(value):
                raise ValueError("日期必须是 YYYY-MM-DD")
        return self


class TaskAssignmentArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    taskId: int = Field(description="任务 ID，来自本人任务列表")


# ---------- 公共辅助 ----------


def _parse_date(value: str | None) -> date | None:
    if not value or not _DATE.fullmatch(value):
        return None
    try:
        return date.fromisoformat(value)
    except ValueError:
        return None


def _monday(value: date) -> date:
    return value - timedelta(days=value.weekday())


def _week_end(week_start: Any) -> str | None:
    parsed = _parse_date(week_start) if isinstance(week_start, str) else None
    return (parsed + timedelta(days=6)).isoformat() if parsed else None


def _weekly_brief(row: Any) -> dict[str, Any] | None:
    """周计划列表只返回定位信息；版本、创建时间等完整字段在详情工具返回。"""
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    return {
        key: row[key]
        for key in ("id", "weekStartDate")
        if row.get(key) is not None
    } | {"weekEndDate": _week_end(row.get("weekStartDate"))}


def _weekly_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    item: dict[str, Any] = {
        key: row[key] for key in ("id", "weekStartDate", "version", "createdAt", "updatedAt") if row.get(key) is not None
    }
    item["weekEndDate"] = _week_end(row.get("weekStartDate"))
    return item


def _daily_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    return {
        key: row[key]
        for key in ("id", "weeklyPlanId", "planDate", "version", "createdAt", "updatedAt")
        if row.get(key) is not None
    }


def _planning_task(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("taskId") is None or not isinstance(
        row.get("taskName"), str
    ):
        return None
    return {key: row[key] for key in _PLANNING_TASK_FIELDS if row.get(key) is not None}


def _has(context: AgentContext, permission: str) -> bool:
    return permission in context.permissions


async def _today(context: AgentContext, client: JavaApiClient) -> tuple[date | None, str | None]:
    """通过 Java 看板解析账号当前业务日期；无看板权限时返回可读错误。"""
    if not _has(context, "dashboard:board:read"):
        return None, "当前账户无法读取账号今天日期，请改用明确日期或 ALL 范围查询"
    value, _time_zone, error_message = await shared.today(context, client)
    if error_message is not None:
        return None, error_message
    parsed = _parse_date(value) if value else None
    if parsed is None:
        return None, "账号今天日期格式不正确"
    return parsed, None


async def _relative_week_start(
    context: AgentContext, client: JavaApiClient, period: str
) -> tuple[date | None, str | None]:
    today_value, error_message = await _today(context, client)
    if error_message is not None:
        return None, error_message
    assert today_value is not None
    monday = _monday(today_value)
    if period == "PREVIOUS":
        monday -= timedelta(days=7)
    elif period == "NEXT":
        monday += timedelta(days=7)
    return monday, None


# ---------- 周计划 ----------


async def run_list_weekly_plans(
    context: AgentContext, client: JavaApiClient, args: WeeklyPlanListArgs
) -> ToolOutcome:
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    today_value: str | None = None
    if args.period != "ALL":
        monday, error_message = await _relative_week_start(context, client, args.period)
        if error_message is not None:
            return shared.error(error_message, "周计划查询失败")
        assert monday is not None
        today_value = monday.isoformat()
        params["dateFrom"] = monday.isoformat()
        params["dateTo"] = monday.isoformat()
    result, error_message = await shared.get(context, client, "weekly-plans", params)
    if error_message is not None:
        return shared.error(error_message, "周计划查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("周计划返回结构不正确", "周计划返回异常")
    items = [item for row in data["items"] if (item := _weekly_brief(row)) is not None]
    payload = shared.page(items, data.get("total"))
    payload["period"] = args.period
    if args.period != "ALL" and items:
        payload["weekStartDate"] = items[0].get("weekStartDate")
    elif args.period != "ALL":
        payload["queryWeekStartDate"] = today_value
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"周计划 {payload['returned']} 个（共 {payload['total']} 个）",
    )


async def run_get_weekly_plan(
    context: AgentContext, client: JavaApiClient, args: WeeklyPlanDetailArgs
) -> ToolOutcome:
    resolved_week: str | None = None
    mode = "id" if args.weeklyPlanId is not None else "period"
    if args.weeklyPlanId is not None:
        path = f"weekly-plans/{args.weeklyPlanId}"
    else:
        monday, error_message = await _relative_week_start(context, client, args.period or "CURRENT")
        if error_message is not None:
            return shared.error(error_message, "周计划查询失败")
        assert monday is not None
        resolved_week = monday.isoformat()
        path = f"weekly-plans/by-week/{monday.isoformat()}"
    result, error_message = await shared.get(context, client, path)
    if error_message is not None:
        return shared.error(error_message, "周计划查询失败")
    data = result.data if result is not None else None
    payload: dict[str, Any] = {"resolvedBy": mode}
    if resolved_week is not None:
        payload["queryWeekStartDate"] = resolved_week
    plan = _weekly_item(data)
    payload["weeklyPlan"] = plan
    if plan is None:
        payload["note"] = "查询的周没有有效周计划"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=("周计划「" + str(plan.get("weekStartDate")) + "」详情") if plan else "该周没有周计划",
    )


# ---------- 日计划 ----------


async def _resolve_daily_plan(
    context: AgentContext, client: JavaApiClient, args: DailyPlanArgs
) -> ToolOutcome:
    if args.dailyPlanId is not None:
        result, error_message = await shared.get(
            context, client, f"daily-plans/{args.dailyPlanId}"
        )
        if error_message is not None:
            return shared.error(error_message, "日计划查询失败")
        plan = _daily_item(result.data if result is not None else None)
        return ToolOutcome(
            content=json.dumps(
                {"resolvedBy": "id", "dailyPlan": plan}
                | ({"note": "该日计划不存在或已删除"} if plan is None else {}),
                ensure_ascii=False,
                separators=(",", ":"),
            ),
            summary="日计划详情" if plan else "该日计划不存在",
        )

    # 日期模式：today/相对日期/指定日期
    today_value: date | None = None
    if args.relativeDay is not None or args.planDate is None:
        today_value, error_message = await _today(context, client)
        if error_message is not None:
            return shared.error(error_message, "日计划查询失败")
    if args.planDate is not None:
        target = _parse_date(args.planDate)
        if target is None:
            return shared.error("日期格式不正确，请使用 YYYY-MM-DD", "日计划查询失败")
    elif args.relativeDay is not None:
        assert today_value is not None
        offset = {"TODAY": 0, "TOMORROW": 1, "YESTERDAY": -1}[args.relativeDay]
        target = today_value + timedelta(days=offset)
    else:
        assert today_value is not None
        target = today_value
    target_text = target.isoformat()
    payload: dict[str, Any] = {"resolvedBy": "date", "date": target_text}

    weekly_id = args.weeklyPlanId
    if weekly_id is None:
        if not _has(context, "planning:weekly:read"):
            return shared.error(
                "当前账户无法读取周计划，请提供 weeklyPlanId 或 dailyPlanId", "日计划查询失败"
            )
        monday = _monday(target)
        payload["queryWeekStartDate"] = monday.isoformat()
        week_result, week_error = await shared.get(
            context, client, f"weekly-plans/by-week/{monday.isoformat()}"
        )
        if week_error is not None:
            return shared.error(week_error, "日计划查询失败")
        week = _weekly_item(week_result.data if week_result is not None else None)
        if week is None:
            payload["dailyPlan"] = None
            payload["note"] = "该日期所在自然周没有有效周计划，因此没有日计划"
            return ToolOutcome(
                content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                summary="该日期没有日计划",
            )
        weekly_id = week.get("id")
        payload["weeklyPlanId"] = weekly_id

    result, error_message = await shared.get(
        context, client, f"weekly-plans/{weekly_id}/daily-plans/{target_text}"
    )
    if error_message is not None:
        return shared.error(error_message, "日计划查询失败")
    plan = _daily_item(result.data if result is not None else None)
    payload["dailyPlan"] = plan
    if plan is None:
        payload["note"] = "该日期没有创建日计划"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary="该日期有日计划" if plan else "该日期没有日计划",
    )


async def run_get_daily_plan(
    context: AgentContext, client: JavaApiClient, args: DailyPlanArgs
) -> ToolOutcome:
    return await _resolve_daily_plan(context, client, args)


# ---------- 计划任务 / 待安排任务 ----------


def _task_params(
    limit: int,
    status: str | None = None,
    keyword: str | None = None,
) -> dict[str, str | int]:
    params: dict[str, str | int] = {"page": 1, "size": limit}
    if status:
        params["status"] = status
    if keyword:
        cleaned = keyword.strip()
        if cleaned:
            params["keyword"] = cleaned
    return params


async def run_list_planning_tasks(
    context: AgentContext, client: JavaApiClient, args: PlanningTasksArgs
) -> ToolOutcome:
    params = _task_params(args.limit, args.status, args.keyword)
    if args.goalId is not None:
        params["goalId"] = args.goalId
    if args.stageId is not None:
        params["stageId"] = args.stageId
    if args.weeklyPlanId is not None:
        if not _has(context, "planning:weekly:read"):
            return shared.error("当前账户没有读取周计划任务的权限", "计划任务查询失败")
        path = f"weekly-plans/{args.weeklyPlanId}/tasks"
        mode, plan_id = "weekly", args.weeklyPlanId
    else:
        if not _has(context, "planning:daily:read"):
            return shared.error("当前账户没有读取日计划任务的权限", "计划任务查询失败")
        path = f"daily-plans/{args.dailyPlanId}/tasks"
        mode, plan_id = "daily", args.dailyPlanId
    result, error_message = await shared.get(context, client, path, params)
    if error_message is not None:
        return shared.error(error_message, "计划任务查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("计划任务返回结构不正确", "计划任务返回异常")
    items = [item for row in data["items"] if (item := _planning_task(row)) is not None]
    payload = shared.page(items, data.get("total"))
    payload["mode"] = mode
    payload[mode + "PlanId"] = plan_id
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"计划任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_list_pending_tasks(
    context: AgentContext, client: JavaApiClient, args: PendingTasksArgs
) -> ToolOutcome:
    params = _task_params(args.limit, args.status, args.keyword)
    if args.goalId is not None:
        params["goalId"] = args.goalId
    if args.stageId is not None:
        params["stageId"] = args.stageId
    if args.dateFrom:
        params["dateFrom"] = args.dateFrom
    if args.dateTo:
        params["dateTo"] = args.dateTo
    result, error_message = await shared.get(
        context, client, "planning/pending-tasks", params
    )
    if error_message is not None:
        return shared.error(error_message, "待安排任务查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("待安排任务返回结构不正确", "待安排任务返回异常")
    items = [item for row in data["items"] if (item := _planning_task(row)) is not None]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"待安排任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# ---------- 任务当前安排 ----------


async def run_get_task_assignment(
    context: AgentContext, client: JavaApiClient, args: TaskAssignmentArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context, client, f"tasks/{args.taskId}/assignment"
    )
    if error_message is not None:
        return shared.error(error_message, "任务安排查询失败")
    assignment = result.data if result is not None else None
    payload: dict[str, Any] = {"taskId": str(args.taskId)}
    if not isinstance(assignment, dict) or assignment.get("id") is None:
        payload["assignment"] = None
        payload["note"] = "该任务当前没有有效安排（在任务池中），可安排到周计划或日计划"
        return ToolOutcome(
            content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            summary="任务当前未安排",
        )
    item = {
        key: assignment[key]
        for key in (
            "id",
            "taskId",
            "weeklyPlanId",
            "dailyPlanId",
            "sortOrder",
            "assignedAt",
            "endedAt",
            "endReason",
            "version",
        )
        if assignment.get(key) is not None
    }
    payload["assignment"] = item
    weekly_id = assignment.get("weeklyPlanId")
    daily_id = assignment.get("dailyPlanId")
    if isinstance(weekly_id, str) and _has(context, "planning:weekly:read"):
        week_result, week_error = await shared.get(context, client, f"weekly-plans/{weekly_id}")
        if week_error is None and week_result is not None:
            week = _weekly_item(week_result.data)
            if week is not None:
                payload["weekStartDate"] = week.get("weekStartDate")
                payload["weekEndDate"] = week.get("weekEndDate")
    if isinstance(daily_id, str) and _has(context, "planning:daily:read"):
        day_result, day_error = await shared.get(context, client, f"daily-plans/{daily_id}")
        if day_error is None and day_result is not None:
            day = _daily_item(day_result.data)
            if day is not None:
                payload["planDate"] = day.get("planDate")
    if daily_id is not None:
        payload["placement"] = "DAY"
    else:
        payload["placement"] = "WEEK"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary="任务当前安排："
        + ("日计划" if daily_id is not None else "周计划"),
    )


# ---------- 工具声明 ----------


WEEKLY_PLAN_LIST_TOOL = ToolSpec(
    name="list_weekly_plans",
    label="周计划列表",
    description=(
        "查询用户本人的周计划列表，可按相对周筛选：CURRENT 本周、PREVIOUS 上周、NEXT 下周、"
        "ALL 全部；最多返回 20 条并给出总条数。今天/本周取自 Java 账号有效时区。"
        "返回周计划 ID、周一日期和周日日期。需要具体任务时再用 list_planning_tasks(weeklyPlanId)。"
        "用户询问本周/上周/下周计划或有哪些周计划时使用。"
    ),
    permission="planning:weekly:list",
    args_model=WeeklyPlanListArgs,
    handler=run_list_weekly_plans,
)

WEEKLY_PLAN_DETAIL_TOOL = ToolSpec(
    name="get_weekly_plan",
    label="周计划详情",
    description=(
        "按周计划 ID 或相对周（CURRENT/PREVIOUS/NEXT）查询本人的周计划：周一日期、周日日期和版本。"
        "相对周以 Java 账号有效时区的今天解析。该周没有周计划时返回空并说明。"
        "用户询问某一周计划的完整信息时使用；任务列表请用 list_planning_tasks。"
    ),
    permission="planning:weekly:read",
    args_model=WeeklyPlanDetailArgs,
    handler=run_get_weekly_plan,
)

DAILY_PLAN_TOOL = ToolSpec(
    name="get_daily_plan",
    label="日计划详情",
    description=(
        "查询用户本人的日计划：可按日计划 ID 查询，或按日期/相对日期（TODAY 今天、TOMORROW 明天、"
        "YESTERDAY 昨天、或不传表示今天）查询；不传 weeklyPlanId 时自动按账号时区定位所在自然周。"
        "该日期没有周计划或日计划时返回空并说明。需要日期内的任务时再用 list_planning_tasks(dailyPlanId)。"
    ),
    permission="planning:daily:read",
    args_model=DailyPlanArgs,
    handler=run_get_daily_plan,
)

PLANNING_TASKS_TOOL = ToolSpec(
    name="list_planning_tasks",
    label="计划任务列表",
    description=(
        "查询某个周计划或日计划下的任务安排，周计划与日计划 ID 二选一，最多返回 20 条并给出总条数。"
        "返回安排 ID、任务 ID/名称、状态、优先级、所属目标/阶段、周一日期、安排日期和排序。"
        "用户询问本周/某天计划中有哪些任务时使用。"
    ),
    permission="planning:weekly:read",
    args_model=PlanningTasksArgs,
    handler=run_list_planning_tasks,
)

PENDING_TASKS_TOOL = ToolSpec(
    name="list_pending_tasks",
    label="待安排任务",
    description=(
        "查询用户本人已过原计划日期或所在周已结束、但仍未完成的任务，用于回答哪些任务需要重新安排。"
        "可按状态、名称关键字、目标、阶段和原安排日期范围筛选，最多返回 20 条并给出总条数。"
        "返回任务、原安排日期、所属目标/阶段和原安排 ID。不会自动延期、复制或修改任务。"
    ),
    permission="planning:pending:list",
    args_model=PendingTasksArgs,
    handler=run_list_pending_tasks,
)

TASK_ASSIGNMENT_TOOL = ToolSpec(
    name="get_task_assignment",
    label="任务安排",
    description=(
        "按任务 ID 查询用户本人的任务当前安排：安排在哪个周计划/日计划、周一日期、安排日期、排序和状态；"
        "任务未安排时返回空并说明在任务池中。任务 ID 必须来自本人任务列表。"
        "用户询问某个任务安排在哪里或是否已安排时使用。"
    ),
    permission="planning:assignment:read",
    args_model=TaskAssignmentArgs,
    handler=run_get_task_assignment,
)
