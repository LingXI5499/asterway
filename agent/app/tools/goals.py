"""目标只读工具：查询本人目标列表与目标详情（含阶段概览）。"""

import json
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient
from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec

GoalStatus = Literal["NOT_STARTED", "IN_PROGRESS", "COMPLETED"]

_GOAL_SUMMARY_FIELDS = (
    "id",
    "name",
    "status",
    "startDate",
    "expectedEndDate",
    "progress",
    "archivedAt",
    "completionConditionMet",
)


class GoalListArgs(BaseModel):
    """目标列表条件；状态使用固定枚举，标识只接受合法数字。"""

    model_config = ConfigDict(extra="forbid", strict=True)

    status: GoalStatus | None = Field(
        default=None, description="目标状态：未开始、进行中或已完成"
    )
    keyword: str | None = Field(default=None, max_length=50, description="目标名称关键字")
    archived: bool = Field(default=False, description="是否查询已归档目标，默认 false")
    timing: Literal["ALL", "CURRENT", "FUTURE"] | None = Field(
        default=None,
        description="目标时间范围：CURRENT 当前进行中、FUTURE 未来、ALL 全部；不传按工作区默认",
    )
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class GoalDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    goalId: int = Field(description="目标 ID，来自目标列表中的 id")


def _list_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        return None
    return {key: row[key] for key in _GOAL_SUMMARY_FIELDS if row.get(key) is not None}


def _detail(row: dict[str, Any]) -> dict[str, Any]:
    stage_items = shared.overview(row.get("stages"))
    detail: dict[str, Any] = {
        "id": row.get("id"),
        "name": row.get("name"),
        "description": shared.text(row.get("description"), 300),
        "completionCriteria": shared.text(row.get("completionCriteria"), 300),
        "startDate": row.get("startDate"),
        "expectedEndDate": row.get("expectedEndDate"),
        "status": row.get("status"),
        "archivedAt": row.get("archivedAt"),
        "progress": row.get("progress"),
        "completionConditionMet": row.get("completionConditionMet"),
        "completionRound": row.get("completionRound"),
        "version": row.get("version"),
        "stageCount": len(stage_items),
        "stages": stage_items,
    }
    return {key: value for key, value in detail.items() if value is not None}


async def run_list_goals(
    context: AgentContext, client: JavaApiClient, args: GoalListArgs
) -> ToolOutcome:
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    if args.status:
        params["status"] = args.status
    if args.keyword:
        keyword = args.keyword.strip()
        if keyword:
            params["keyword"] = keyword
    if args.archived:
        params["archived"] = True
    if args.timing:
        params["timing"] = args.timing
    result, error_message = await shared.get(context, client, "goals", params)
    if error_message is not None:
        return shared.error(error_message, "目标查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("目标返回结构不正确", "目标返回异常")
    items = [item for row in data["items"] if (item := _list_item(row)) is not None]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"目标 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_get_goal(
    context: AgentContext, client: JavaApiClient, args: GoalDetailArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, f"goals/{args.goalId}")
    if error_message is not None:
        return shared.error(error_message, "目标详情查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or data.get("id") is None:
        return shared.error("目标详情返回结构不正确", "目标详情返回异常")
    goal = _detail(data)
    return ToolOutcome(
        content=json.dumps({"goal": goal}, ensure_ascii=False, separators=(",", ":")),
        summary=f"目标「{goal.get('name', '')}」详情",
    )


GOAL_LIST_TOOL = ToolSpec(
    name="list_goals",
    label="目标列表",
    description=(
        "查询用户本人的成长目标列表，可按目标状态、名称关键字、是否归档和时间范围筛选，"
        "最多返回 20 条并给出总条数。返回目标 ID、名称、状态、起止日期和进度。"
        "用户询问自己有哪些目标、目标进度或查找目标时使用；"
        "需要阶段概览等完整信息时再用 get_goal 查询详情。"
    ),
    permission="goal:goal:list",
    args_model=GoalListArgs,
    handler=run_list_goals,
)

GOAL_DETAIL_TOOL = ToolSpec(
    name="get_goal",
    label="目标详情",
    description=(
        "按目标 ID 查询用户本人的目标详情：名称、描述、完成条件、状态、起止日期、"
        "进度及阶段概览。目标 ID 必须来自本人目标列表，不能猜测或使用他人 ID。"
        "用户询问某个目标的完整信息或目标下有哪些阶段时使用。"
    ),
    permission="goal:goal:read",
    args_model=GoalDetailArgs,
    handler=run_get_goal,
)
