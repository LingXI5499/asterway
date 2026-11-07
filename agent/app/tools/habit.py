"""习惯只读工具：习惯列表/详情、分类、今日应做、打卡历史与规则修订。

只读取 Java 习惯事实；不打卡、不撤销打卡、不创建/修改/暂停/结束习惯。
今日口径使用 Java 账号有效时区。
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
HabitStatus = Literal["ACTIVE", "PAUSED", "ENDED"]
_STATUS_LABELS = {"ACTIVE": "进行中", "PAUSED": "已暂停", "ENDED": "已结束"}
_CYCLE_LABELS = {"DAILY": "每天", "WEEKLY_DAYS": "指定星期"}
_ACTION_LABELS = {
    "CREATED": "创建",
    "EDITED": "编辑规则",
    "PAUSED": "暂停",
    "RESUMED": "恢复",
    "ENDED": "结束",
    "REOPENED": "重新开启",
    "DELETED": "删除",
    "SYSTEM_PAUSED": "系统暂停（来源目标归档）",
}
_WEEKDAY_NAMES = ("周一", "周二", "周三", "周四", "周五", "周六", "周日")


class HabitListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    status: HabitStatus | None = Field(
        default=None, description="习惯状态：ACTIVE 进行中、PAUSED 已暂停、ENDED 已结束"
    )
    keyword: str | None = Field(default=None, max_length=50, description="习惯名称关键字")
    categoryId: int | None = Field(default=None, description="只看某个分类下的习惯")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class HabitDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")


class HabitCategoryListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    keyword: str | None = Field(default=None, max_length=50, description="分类名称关键字")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class TodayHabitArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class HabitCheckinArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")
    dateFrom: str | None = Field(default=None, description="业务日期下限 YYYY-MM-DD，可省略")
    dateTo: str | None = Field(default=None, description="业务日期上限 YYYY-MM-DD，可省略")
    cancelled: bool | None = Field(
        default=None, description="按是否已撤销筛选：true 已撤销、false 有效打卡；不传为全部"
    )
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    @model_validator(mode="after")
    def validate_dates(self) -> "HabitCheckinArgs":
        if (self.dateFrom is None) != (self.dateTo is None):
            raise ValueError("dateFrom 与 dateTo 必须同时提供或同时省略")
        for value in (self.dateFrom, self.dateTo):
            if value is not None and not _DATE.fullmatch(value):
                raise ValueError("日期必须是 YYYY-MM-DD")
        if self.dateFrom and self.dateTo:
            try:
                if date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo):
                    raise ValueError("开始日期不能晚于结束日期")
            except ValueError as error:
                raise ValueError("日期格式不正确") from error
        return self


class HabitRevisionArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


def _weekdays(value: Any) -> list[int]:
    if isinstance(value, list):
        days = [
            day
            for day in value
            if isinstance(day, int) and not isinstance(day, bool) and 1 <= day <= 7
        ]
        return days
    mask = value
    if isinstance(mask, int) and not isinstance(mask, bool):
        return [day for day in range(1, 8) if mask & (1 << (day - 1))]
    return []


def _weekdays_text(days: list[int]) -> str | None:
    if not days:
        return None
    return "、".join(_WEEKDAY_NAMES[day - 1] for day in days)


def _labeled(value: Any, labels: dict[str, str]) -> str | None:
    if isinstance(value, str):
        return labels.get(value, value)
    return None


def _habit_item(row: Any, *, detail: bool) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        return None
    days = _weekdays(row.get("weekdays") if "weekdays" in row else row.get("weekdayMask"))
    item: dict[str, Any] = {
        key: row[key]
        for key in (
            "id",
            "status",
            "revisionNo",
            "name",
            "categoryId",
            "categoryName",
            "categoryDeleted",
            "cycleType",
            "startDate",
            "expectedTime",
            "checkedToday",
            "historyLockedThrough",
            "endedAt",
            "version",
        )
        if row.get(key) is not None
    }
    item["statusLabel"] = _labeled(row.get("status"), _STATUS_LABELS)
    item["cycleTypeLabel"] = _labeled(row.get("cycleType"), _CYCLE_LABELS)
    item["weekdays"] = days
    item["weekdaysText"] = _weekdays_text(days)
    if detail:
        item["description"] = shared.text(row.get("description"), 300)
        item["completionCriteria"] = shared.text(row.get("completionCriteria"), 300)
    return {key: value for key, value in item.items() if value is not None}


def _today_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("habitId") is None or not isinstance(
        row.get("name"), str
    ):
        return None
    done = row.get("doneCount")
    due = row.get("dueCount")
    checked = bool(row.get("checkinId")) or (isinstance(done, int) and done > 0)
    return {
        key: value
        for key, value in {
            "habitId": row.get("habitId"),
            "name": row.get("name"),
            "status": row.get("status"),
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            "expectedTime": row.get("expectedTime"),
            "categoryId": row.get("categoryId"),
            "checked": checked,
            "doneCount": done,
            "dueCount": due,
            "canCheckin": row.get("canCheckin"),
            "canCancel": row.get("canCancel"),
        }.items()
        if value is not None
    }


def _checkin_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None:
        return None
    cancelled = row.get("cancelledAt") is not None
    return {
        key: value
        for key, value in {
            "habitId": row.get("habitId"),
            "nameSnapshot": row.get("nameSnapshot"),
            "categoryId": row.get("categoryId"),
            "businessDate": row.get("businessDate"),
            "timeZone": row.get("timeZone"),
            "checkedAt": row.get("checkedAt"),
            "cancelledAt": row.get("cancelledAt"),
            "cancelled": cancelled,
            "cancelledLabel": "已撤销" if cancelled else "有效",
        }.items()
        if value is not None
    }


def _revision_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("revisionNo") is None:
        return None
    days = _weekdays(row.get("weekdays") if "weekdays" in row else row.get("weekdayMask"))
    return {
        key: value
        for key, value in {
            "revisionNo": row.get("revisionNo"),
            "actionType": row.get("actionType"),
            "actionTypeLabel": _labeled(row.get("actionType"), _ACTION_LABELS),
            "name": row.get("name"),
            "description": shared.text(row.get("description"), 200),
            "completionCriteria": shared.text(row.get("completionCriteria"), 200),
            "categoryId": row.get("categoryId"),
            "cycleType": row.get("cycleType"),
            "cycleTypeLabel": _labeled(row.get("cycleType"), _CYCLE_LABELS),
            "weekdays": days,
            "weekdaysText": _weekdays_text(days),
            "startDate": row.get("startDate"),
            "expectedTime": row.get("expectedTime"),
            "status": row.get("status"),
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            "deleted": row.get("deleted"),
            "effectiveDate": row.get("effectiveDate"),
            "changedAt": row.get("changedAt"),
            "timeZone": row.get("timeZone"),
        }.items()
        if value is not None
    }


async def run_list_habits(
    context: AgentContext, client: JavaApiClient, args: HabitListArgs
) -> ToolOutcome:
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    if args.status:
        params["status"] = args.status
    if args.categoryId is not None:
        params["categoryId"] = args.categoryId
    keyword = (args.keyword or "").strip()
    if keyword:
        params["keyword"] = keyword
    result, error_message = await shared.get(context, client, "habits", params)
    if error_message is not None:
        return shared.error(error_message, "习惯查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("习惯返回结构不正确", "习惯返回异常")
    items = [
        item for row in data["items"] if (item := _habit_item(row, detail=False)) is not None
    ]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"习惯 {payload['returned']} 个（共 {payload['total']} 个）",
    )


async def run_get_habit(
    context: AgentContext, client: JavaApiClient, args: HabitDetailArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, f"habits/{args.habitId}")
    if error_message is not None:
        return shared.error(error_message, "习惯详情查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or data.get("id") is None:
        return shared.error("习惯详情返回结构不正确", "习惯详情返回异常")
    habit = _habit_item(data, detail=True)
    return ToolOutcome(
        content=json.dumps({"habit": habit}, ensure_ascii=False, separators=(",", ":")),
        summary=f"习惯「{(habit or {}).get('name', '')}」详情",
    )


async def run_list_habit_categories(
    context: AgentContext, client: JavaApiClient, args: HabitCategoryListArgs
) -> ToolOutcome:
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    keyword = (args.keyword or "").strip()
    if keyword:
        params["keyword"] = keyword
    result, error_message = await shared.get(context, client, "habit-categories", params)
    if error_message is not None:
        return shared.error(error_message, "习惯分类查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("习惯分类返回结构不正确", "习惯分类返回异常")
    items = [
        {
            key: row[key]
            for key in ("id", "name", "sortOrder", "version")
            if isinstance(row, dict) and row.get(key) is not None
        }
        for row in data["items"]
        if isinstance(row, dict) and row.get("id") is not None
    ]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"习惯分类 {payload['returned']} 个（共 {payload['total']} 个）",
    )


async def run_today_habits(
    context: AgentContext, client: JavaApiClient, args: TodayHabitArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context, client, "habits/today", {"page": 1, "size": args.limit}
    )
    if error_message is not None:
        return shared.error(error_message, "今日习惯查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("今日习惯返回结构不正确", "今日习惯返回异常")
    items = [item for row in data["items"] if (item := _today_item(row)) is not None]
    payload = shared.page(items, data.get("total"))
    payload["note"] = "该列表是今天实际应做的习惯；canCheckin/canCancel 仅为页面可操作状态，助手不会打卡或撤销"
    if "dashboard:board:read" in context.permissions:
        today_value, time_zone, _ = await shared.today(context, client)
        if today_value:
            payload["date"] = today_value
        if time_zone:
            payload["timeZone"] = time_zone
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"今日习惯 {payload['returned']} 个（共 {payload['total']} 个）",
    )


async def run_habit_checkins(
    context: AgentContext, client: JavaApiClient, args: HabitCheckinArgs
) -> ToolOutcome:
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    if args.dateFrom:
        params["dateFrom"] = args.dateFrom
    if args.dateTo:
        params["dateTo"] = args.dateTo
    if args.cancelled is not None:
        params["cancelled"] = args.cancelled
    result, error_message = await shared.get(
        context, client, f"habits/{args.habitId}/checkins", params
    )
    if error_message is not None:
        return shared.error(error_message, "打卡历史查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("打卡历史返回结构不正确", "打卡历史返回异常")
    items = [
        item for row in data["items"] if (item := _checkin_item(row)) is not None
    ]
    payload = shared.page(items, data.get("total"))
    payload["habitId"] = str(args.habitId)
    payload["note"] = "已撤销的打卡仍保留为事实，cancelled=true 表示该次打卡已撤销"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"打卡记录 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_habit_revisions(
    context: AgentContext, client: JavaApiClient, args: HabitRevisionArgs
) -> ToolOutcome:
    result, error_message = await shared.get(
        context, client, f"habits/{args.habitId}/revisions", {"page": 1, "size": args.limit}
    )
    if error_message is not None:
        return shared.error(error_message, "习惯规则历史查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("习惯规则历史返回结构不正确", "习惯规则历史返回异常")
    items = [
        item for row in data["items"] if (item := _revision_item(row)) is not None
    ]
    payload = shared.page(items, data.get("total"))
    payload["habitId"] = str(args.habitId)
    payload["note"] = "修订按版本倒序返回，是规则与生命周期快照，不覆盖历史打卡事实"
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"习惯规则修订 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# ---------- 工具声明 ----------


HABIT_LIST_TOOL = ToolSpec(
    name="list_habits",
    label="习惯列表",
    description=(
        "查询用户本人的习惯列表，可按状态（进行中/已暂停/已结束）、名称关键字和分类筛选，"
        "最多返回 20 条并给出总条数。返回习惯 ID、名称、状态、周期（每天/指定星期）、执行星期、"
        "提示时间、分类、开始日期和今日是否已打卡。用户询问自己有哪些习惯、习惯状态时使用；"
        "完整资料用 get_habit，今日应做用 get_today_habits。"
    ),
    permission="habit:habit:list",
    args_model=HabitListArgs,
    handler=run_list_habits,
)

HABIT_DETAIL_TOOL = ToolSpec(
    name="get_habit",
    label="习惯详情",
    description=(
        "按习惯 ID 查询用户本人的习惯当前资料：名称、描述、完成标准、状态、周期、执行星期、"
        "提示时间、分类、开始日期、今日是否已打卡和版本。习惯 ID 必须来自本人习惯列表或今日习惯。"
        "用户询问某个习惯的完整规则时使用。"
    ),
    permission="habit:habit:read",
    args_model=HabitDetailArgs,
    handler=run_get_habit,
)

HABIT_CATEGORY_LIST_TOOL = ToolSpec(
    name="list_habit_categories",
    label="习惯分类",
    description=(
        "查询用户本人的习惯分类列表，可按名称关键字筛选，最多返回 20 条并给出总条数。"
        "返回分类 ID、名称和排序。用户询问习惯分类、按分类整理习惯时使用。"
    ),
    permission="habit:category:list",
    args_model=HabitCategoryListArgs,
    handler=run_list_habit_categories,
)

TODAY_HABIT_TOOL = ToolSpec(
    name="get_today_habits",
    label="今日习惯",
    description=(
        "查询用户本人今天实际应做的习惯：名称、状态、提示时间、是否已打卡、完成/应做数量，"
        "并给出今天日期和账号时区。最多返回 20 条并给出总条数。只读；不会替用户打卡或撤销打卡。"
        "用户询问今天有哪些习惯要做、哪些还没完成时使用。"
    ),
    permission="habit:habit:list",
    args_model=TodayHabitArgs,
    handler=run_today_habits,
)

HABIT_CHECKIN_LIST_TOOL = ToolSpec(
    name="list_habit_checkins",
    label="打卡历史",
    description=(
        "按习惯 ID 查询打卡历史：业务日期、打卡时间、时区、是否已撤销及撤销时间；"
        "可按业务日期范围和是否撤销筛选，最多返回 20 条并给出总条数。已撤销打卡仍作为事实保留。"
        "习惯 ID 必须来自本人习惯列表。用户询问某习惯打过几次卡、哪天没打、是否撤销过时使用。"
    ),
    permission="habit:checkin:read",
    args_model=HabitCheckinArgs,
    handler=run_habit_checkins,
)

HABIT_REVISION_LIST_TOOL = ToolSpec(
    name="list_habit_revisions",
    label="习惯规则历史",
    description=(
        "按习惯 ID 查询规则与生命周期修订历史：修订号、动作（创建/编辑/暂停/恢复/结束/重新开启/删除）、"
        "当时的名称、周期、执行星期、提示时间、状态快照、生效日期与变更时间，按修订号倒序，"
        "最多返回 20 条并给出总条数。用于解释习惯规则如何变化；不覆盖历史打卡事实。"
    ),
    permission="habit:habit:history",
    args_model=HabitRevisionArgs,
    handler=run_habit_revisions,
)
