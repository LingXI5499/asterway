"""阶段只读工具：按目标查询阶段列表与阶段详情（含任务概览）。"""

import json
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient
from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec

StageStatus = Literal["NOT_STARTED", "IN_PROGRESS", "COMPLETED"]


class StageListArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    goalId: int = Field(description="目标 ID，来自本人目标列表中的 id")
    status: StageStatus | None = Field(
        default=None, description="阶段状态：未开始、进行中或已完成"
    )
    keyword: str | None = Field(default=None, max_length=50, description="阶段名称关键字")
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class StageDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    stageId: int = Field(description="阶段 ID，来自阶段列表中的 id")


def _list_item(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        return None
    item: dict[str, Any] = {
        key: row[key]
        for key in ("id", "goalId", "name", "status", "startDate", "endDate", "sortOrder", "progress")
        if row.get(key) is not None
    }
    return item


def _detail(row: dict[str, Any]) -> dict[str, Any]:
    task_items = shared.overview(row.get("tasks"))
    detail: dict[str, Any] = {
        "id": row.get("id"),
        "goalId": row.get("goalId"),
        "name": row.get("name"),
        "description": shared.text(row.get("description"), 300),
        "completionCriteria": shared.text(row.get("completionCriteria"), 300),
        "startDate": row.get("startDate"),
        "endDate": row.get("endDate"),
        "sortOrder": row.get("sortOrder"),
        "status": row.get("status"),
        "progress": row.get("progress"),
        "version": row.get("version"),
        "taskCount": len(task_items),
        "tasks": task_items,
    }
    return {key: value for key, value in detail.items() if value is not None}


async def run_list_stages(
    context: AgentContext, client: JavaApiClient, args: StageListArgs
) -> ToolOutcome:
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    if args.status:
        params["status"] = args.status
    if args.keyword:
        keyword = args.keyword.strip()
        if keyword:
            params["keyword"] = keyword
    result, error_message = await shared.get(
        context, client, f"goals/{args.goalId}/stages", params
    )
    if error_message is not None:
        return shared.error(error_message, "阶段查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("阶段返回结构不正确", "阶段返回异常")
    items = [item for row in data["items"] if (item := _list_item(row)) is not None]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"阶段 {payload['returned']} 个（共 {payload['total']} 个）",
    )


async def run_get_stage(
    context: AgentContext, client: JavaApiClient, args: StageDetailArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, f"stages/{args.stageId}")
    if error_message is not None:
        return shared.error(error_message, "阶段详情查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or data.get("id") is None:
        return shared.error("阶段详情返回结构不正确", "阶段详情返回异常")
    stage = _detail(data)
    return ToolOutcome(
        content=json.dumps({"stage": stage}, ensure_ascii=False, separators=(",", ":")),
        summary=f"阶段「{stage.get('name', '')}」详情",
    )


STAGE_LIST_TOOL = ToolSpec(
    name="list_stages",
    label="阶段列表",
    description=(
        "查询用户本人某个目标下的阶段列表，可按阶段状态和名称关键字筛选，最多返回 20 条"
        "并给出总条数。返回阶段 ID、名称、状态、起止日期和进度；列表不含任务数量。"
        "目标 ID 必须来自本人目标列表。用户询问某个目标分几个阶段、阶段进度时使用；"
        "需要阶段下的任务或完整信息时，再用 get_stage 查询阶段详情。"
    ),
    permission="stage:stage:list",
    args_model=StageListArgs,
    handler=run_list_stages,
)

STAGE_DETAIL_TOOL = ToolSpec(
    name="get_stage",
    label="阶段详情",
    description=(
        "按阶段 ID 查询用户本人的阶段详情：所属目标、名称、描述、完成条件、起止日期、"
        "状态、进度及任务概览。阶段 ID 必须来自本人阶段列表。"
        "用户询问某个阶段的完整信息或该阶段有哪些任务时使用。"
    ),
    permission="stage:stage:read",
    args_model=StageDetailArgs,
    handler=run_get_stage,
)
