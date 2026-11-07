"""统计只读工具：周期汇总、逐日趋势、成长时间线、习惯统计与对象状态分布。

全部读取既有 Java 统计接口，不修改历史、不补写耗时；周期与今天口径由 Java 账号时区决定。
"""

import json
import re
from datetime import date
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient
from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec

_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
StatisticsPeriod = Literal["TOTAL", "WEEK", "MONTH", "YEAR", "RANGE"]
HabitGroupBy = Literal["HABIT", "CATEGORY"]
_PERIOD_LABELS = {
    "TOTAL": "全部历史",
    "WEEK": "本周",
    "MONTH": "本月",
    "YEAR": "今年",
    "RANGE": "自定义区间",
}
_KIND_LABELS = {
    "GOAL": "目标",
    "STAGE": "阶段",
    "TASK": "任务",
    "HABIT": "习惯",
    "EXECUTION": "执行计时",
    "PLANNING": "计划安排",
    "ASSIGNMENT": "计划安排",
}
_EVENT_LABELS = {
    "STATUS_CHANGED": "状态变更",
    "COMPLETED": "完成",
    "ARCHIVED": "归档",
    "RESTORED": "恢复",
    "CHECKIN": "打卡",
    "CHECKIN_CANCELLED": "撤销打卡",
    "SEGMENT_STARTED": "开始计时",
    "USER_PAUSE": "用户暂停",
    "AUTO_PAUSE": "自动暂停",
    "PLAN_BOUNDARY": "计划边界暂停",
    "AUTH_REVOKED": "登录失效暂停",
    "USER_FINISH": "用户结束",
    "TASK_DELETED": "任务删除结束",
    "GOAL_ARCHIVED": "目标归档结束",
    "FINISH": "结束",
}
_STATUS_LABELS = {
    "NOT_STARTED": "未开始",
    "TODO": "待开始",
    "IN_PROGRESS": "进行中",
    "PAUSED": "已暂停",
    "ACTIVE": "进行中",
    "COMPLETED": "已完成",
    "DONE": "已完成",
    "ENDED": "已结束",
    "ARCHIVED": "已归档",
    "CANCELLED": "已取消",
}


class StatisticsArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    period: StatisticsPeriod = Field(
        default="MONTH",
        description="统计周期：TOTAL 全部历史、WEEK 本周、MONTH 本月、YEAR 今年、RANGE 自定义区间",
    )
    anchorDate: str | None = Field(
        default=None, description="周期参照日期 YYYY-MM-DD；省略时使用账号今天"
    )
    dateFrom: str | None = Field(default=None, description="自定义区间开始日期，仅 period=RANGE")
    dateTo: str | None = Field(default=None, description="自定义区间结束日期，仅 period=RANGE")
    goalId: int | None = Field(default=None, description="只看某个目标相关统计")
    stageId: int | None = Field(default=None, description="只看某个阶段相关统计")
    taskId: int | None = Field(default=None, description="只看某个任务相关统计")

    @model_validator(mode="after")
    def validate_dates(self) -> "StatisticsArgs":
        if self.period == "RANGE":
            if not self.dateFrom or not self.dateTo:
                raise ValueError("period=RANGE 时必须同时提供 dateFrom 与 dateTo")
        elif self.dateFrom is not None or self.dateTo is not None:
            raise ValueError("只有 period=RANGE 才能指定 dateFrom/dateTo")
        for value in (self.anchorDate, self.dateFrom, self.dateTo):
            if value is not None and not _DATE.fullmatch(value):
                raise ValueError("日期必须是 YYYY-MM-DD")
        if (
            self.dateFrom
            and self.dateTo
            and date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo)
        ):
            raise ValueError("开始日期不能晚于结束日期")
        return self


class StatisticsPageArgs(StatisticsArgs):
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class HabitStatisticsArgs(StatisticsPageArgs):
    groupBy: HabitGroupBy = Field(
        default="HABIT", description="分组方式：HABIT 按习惯、CATEGORY 按历史分类归属"
    )


def _labeled(value: Any, labels: dict[str, str]) -> str | None:
    if isinstance(value, str):
        return labels.get(value, value)
    return None


def _statistics_params(args: StatisticsArgs) -> dict[str, str | int]:
    params: dict[str, str | int] = {"period": args.period}
    if args.anchorDate:
        params["anchorDate"] = args.anchorDate
    if args.period == "RANGE":
        params["dateFrom"] = args.dateFrom or ""
        params["dateTo"] = args.dateTo or ""
    for key in ("goalId", "stageId", "taskId"):
        value = getattr(args, key)
        if value is not None:
            params[key] = value
    return params


def _range_fields(data: dict[str, Any]) -> dict[str, Any]:
    result = {
        "period": data.get("period"),
        "periodLabel": _labeled(data.get("period"), _PERIOD_LABELS),
        "dateFrom": data.get("dateFrom"),
        "dateTo": data.get("dateTo"),
        "timeZone": data.get("timeZone"),
    }
    return {key: value for key, value in result.items() if value is not None}


def _execution(data: Any) -> dict[str, Any] | None:
    if not isinstance(data, dict):
        return None
    result = {
        key: data.get(key)
        for key in (
            "totalActualSeconds",
            "rangeActualSeconds",
            "totalSessions",
            "rangeSessions",
            "totalFinishedSessions",
            "rangeFinishedSessions",
            "lastExecutionAt",
        )
        if data.get(key) is not None
    }
    if result.get("totalActualSeconds") is not None:
        result["totalActualMinutes"] = round(result["totalActualSeconds"] / 60, 2)
    if result.get("rangeActualSeconds") is not None:
        result["rangeActualMinutes"] = round(result["rangeActualSeconds"] / 60, 2)
    return result


def _habit_totals(data: Any) -> dict[str, Any] | None:
    if not isinstance(data, dict):
        return None
    return {
        key: data.get(key)
        for key in ("doneCount", "dueCount", "completionRate")
        if data.get(key) is not None
    }


def _state_totals(data: Any) -> dict[str, Any] | None:
    if not isinstance(data, dict):
        return None
    return {
        key: data.get(key)
        for key in (
            "goalCompletionActions",
            "stageCompletionActions",
            "taskCompletionActions",
            "completedDistinctTasks",
            "goalArchiveActions",
            "goalRestoreActions",
        )
        if data.get(key) is not None
    }


def _trend_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("businessDate") is None:
        return None
    return {
        key: row[key]
        for key in (
            "businessDate",
            "actualSeconds",
            "actualMilliseconds",
            "sessions",
            "habitDone",
            "habitDue",
            "habitCompletionRate",
            "goalCompletionActions",
            "stageCompletionActions",
            "taskCompletionActions",
            "completedDistinctTasks",
            "goalArchiveActions",
            "goalRestoreActions",
        )
        if row.get(key) is not None
    }


def _timeline_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("occurredAt") is None:
        return None
    return {
        key: value
        for key, value in {
            "kind": row.get("kind"),
            "kindLabel": _labeled(row.get("kind"), _KIND_LABELS),
            "eventType": row.get("eventType"),
            "eventLabel": _labeled(row.get("eventType"), _EVENT_LABELS),
            "occurredAt": row.get("occurredAt"),
            "label": row.get("label"),
            "objectId": row.get("objectId"),
        }.items()
        if value is not None
    }


def _habit_group_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("name") is None:
        return None
    return {
        key: value
        for key, value in {
            "id": row.get("id"),
            "name": row.get("name"),
            "deleted": row.get("deleted"),
            "doneCount": row.get("doneCount"),
            "dueCount": row.get("dueCount"),
            "completionRate": row.get("completionRate"),
        }.items()
        if value is not None
    }


def _object_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("kind") is None:
        return None
    return {
        key: value
        for key, value in {
            "kind": row.get("kind"),
            "kindLabel": _labeled(row.get("kind"), _KIND_LABELS),
            "status": row.get("status"),
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            "objectCount": row.get("objectCount"),
        }.items()
        if value is not None
    }


async def run_statistics_summary(
    context: AgentContext, client: JavaApiClient, args: StatisticsArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context, client, "statistics/summary", _statistics_params(args)
    )
    if error_message is not None:
        return shared.error(error_message, "统计汇总查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict):
        return shared.error("统计汇总返回结构不正确", "统计汇总返回异常")
    payload: dict[str, Any] = _range_fields(data)
    payload["execution"] = _execution(data.get("execution"))
    payload["habits"] = _habit_totals(data.get("habits"))
    payload["states"] = _state_totals(data.get("states"))
    filters = {
        key: data.get(key)
        for key in ("goalId", "stageId", "taskId")
        if data.get(key) is not None
    }
    if filters:
        payload["filters"] = filters
    payload["note"] = "total* 为账号全部历史累计，range* 为所选统计周期；习惯统计独立于目标筛选"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"统计汇总（{payload.get('periodLabel') or payload.get('period')}）",
    )


async def run_statistics_trends(
    context: AgentContext, client: JavaApiClient, args: StatisticsPageArgs
) -> ToolOutcome:
    params = _statistics_params(args)
    params["page"] = 1
    params["size"] = args.limit
    result, error_message = await shared.get(context, client, "statistics/trends", params)
    if error_message is not None:
        return shared.error(error_message, "逐日趋势查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("逐日趋势返回结构不正确", "逐日趋势返回异常")
    payload = shared.page(
        [item for row in data["items"] if (item := _trend_item(row)) is not None],
        data.get("total"),
    )
    payload["note"] = "逐日趋势来自真实业务事实，不从当前状态反推过去完成"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"逐日趋势 {payload['returned']} 天（共 {payload['total']} 天）",
    )


async def run_growth_timeline(
    context: AgentContext, client: JavaApiClient, args: StatisticsPageArgs
) -> ToolOutcome:
    params = _statistics_params(args)
    params["page"] = 1
    params["size"] = args.limit
    result, error_message = await shared.get(context, client, "statistics/timeline", params)
    if error_message is not None:
        return shared.error(error_message, "成长时间线查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("成长时间线返回结构不正确", "成长时间线返回异常")
    payload = shared.page(
        [item for row in data["items"] if (item := _timeline_item(row)) is not None],
        data.get("total"),
    )
    payload["note"] = "时间线是真实状态、打卡、执行与计划事件，按发生时间倒序，不按当前状态推测"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"成长时间线 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_habit_statistics(
    context: AgentContext, client: JavaApiClient, args: HabitStatisticsArgs
) -> ToolOutcome:
    params = _statistics_params(args)
    params["page"] = 1
    params["size"] = args.limit
    params["groupBy"] = args.groupBy
    result, error_message = await shared.get(context, client, "statistics/habits", params)
    if error_message is not None:
        return shared.error(error_message, "习惯统计查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict):
        return shared.error("习惯统计返回结构不正确", "习惯统计返回异常")
    groups = data.get("groups") if isinstance(data.get("groups"), dict) else {}
    payload: dict[str, Any] = _range_fields(data)
    payload["groupBy"] = data.get("groupBy")
    payload["totals"] = _habit_totals(data.get("totals"))
    payload["groups"] = shared.page(
        [
            item
            for row in groups.get("items", []) if isinstance(row, dict)
            if (item := _habit_group_item(row)) is not None
        ],
        groups.get("total"),
    )
    payload["note"] = "完成率=实际完成/应做；分母为零时为空；习惯统计不受目标筛选影响"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"习惯统计 {payload['groups'].get('returned', 0)} 组",
    )


async def run_object_statistics(
    context: AgentContext, client: JavaApiClient, args: StatisticsArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context, client, "statistics/objects", _statistics_params(args)
    )
    if error_message is not None:
        return shared.error(error_message, "对象状态统计查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, list):
        return shared.error("对象状态统计返回结构不正确", "对象状态统计返回异常")
    items = [item for row in data if (item := _object_item(row)) is not None]
    payload: dict[str, Any] = {"items": items, "returned": len(items), "total": len(items)}
    payload["note"] = "只统计当前有效未归档对象，与历史完成动作分开"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"对象状态分布 {len(items)} 组",
    )


STATISTICS_SUMMARY_TOOL = ToolSpec(
    name="get_statistics_summary",
    label="统计汇总",
    description=(
        "按全部历史/本周/本月/今年/自定义区间汇总成长数据：实际投入（累计与区间、会话数）、"
        "习惯完成与应做数量、目标/阶段/任务完成动作、归档与恢复动作；可按目标/阶段/任务限定范围。"
        "用户询问投入了多少时间、完成了多少任务、习惯完成率、某目标/任务统计时使用。"
    ),
    permission="statistics:report:read",
    args_model=StatisticsArgs,
    handler=run_statistics_summary,
)

STATISTICS_TRENDS_TOOL = ToolSpec(
    name="get_statistics_trends",
    label="逐日趋势",
    description=(
        "按日期返回逐日趋势：当天实际秒数与会话数、习惯完成/应做与完成率、各类完成动作数、"
        "去重完成任务数、归档与恢复动作。最多 20 天并给出总天数。"
        "用户询问最近每天/每周投入变化、趋势时使用；周期与今天口径以账号时区为准。"
    ),
    permission="statistics:report:read",
    args_model=StatisticsPageArgs,
    handler=run_statistics_trends,
)

GROWTH_TIMELINE_TOOL = ToolSpec(
    name="get_growth_timeline",
    label="成长时间线",
    description=(
        "按发生时间倒序返回真实成长事件：目标/阶段/任务状态变更与完成、习惯打卡与撤销、"
        "执行开始与结束、计划安排事件，包含对象名称和发生时间。最多 20 条并给出总条数。"
        "用户询问最近发生了什么、成长轨迹、某段时间完成了哪些事时使用。"
    ),
    permission="statistics:report:read",
    args_model=StatisticsPageArgs,
    handler=run_growth_timeline,
)

HABIT_STATISTICS_TOOL = ToolSpec(
    name="get_habit_statistics",
    label="习惯统计",
    description=(
        "按周期统计习惯完成情况：总完成/应做与完成率，并按习惯或历史分类归属分组返回每组"
        "完成数、应做数与完成率，最多 20 组。习惯统计独立于目标/阶段/任务筛选。"
        "用户询问某段时间习惯完成率、哪个习惯完成得好、分类维度习惯统计时使用。"
    ),
    permission="statistics:report:read",
    args_model=HabitStatisticsArgs,
    handler=run_habit_statistics,
)

OBJECT_STATISTICS_TOOL = ToolSpec(
    name="get_object_statistics",
    label="对象状态分布",
    description=(
        "查询当前有效未归档对象的状态分布：目标、阶段、任务各自按状态的数量。"
        "这是当前状态快照，不等于历史完成动作。用户询问现在有多少未完成/进行中的目标或任务时使用。"
    ),
    permission="statistics:report:read",
    args_model=StatisticsArgs,
    handler=run_object_statistics,
)
