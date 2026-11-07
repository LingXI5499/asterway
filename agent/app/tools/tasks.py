"""任务只读工具：任务列表与任务详情；支持状态、优先级、关键字、目标和阶段筛选。"""

import json
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient
from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec

TaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED"]
Priority = Literal["LOW", "MEDIUM", "HIGH", "URGENT"]

_BRIEF_FIELDS = (
    "id",
    "name",
    "status",
    "priority",
    "dueDate",
    "goalId",
    "goalName",
    "stageId",
    "stageName",
    "assignmentStatus",
    "estimatedMinutes",
)

_DETAIL_FIELDS = (
    "id",
    "name",
    "priority",
    "weight",
    "estimatedMinutes",
    "startDate",
    "dueDate",
    "status",
    "completedAt",
    "version",
    "goalId",
    "goalName",
    "stageId",
    "stageName",
    "assignmentStatus",
    "weeklyPlanId",
    "dailyPlanId",
    "goalArchivedAt",
)


class TaskListArgs(BaseModel):
    """任务查询参数；状态和优先级使用系统固定枚举，不接受任意文本。"""

    model_config = ConfigDict(extra="forbid", strict=True)

    status: TaskStatus | None = Field(
        default=None, description="任务状态：未开始、进行中、暂停或已完成"
    )
    priority: Priority | None = Field(
        default=None, description="优先级：低、中、高或紧急"
    )
    keyword: str | None = Field(
        default=None, max_length=50, description="任务标题关键字，最多 50 字"
    )
    goalId: int | None = Field(
        default=None, description="只看某个目标下的任务；ID 必须来自本人目标列表"
    )
    stageId: int | None = Field(
        default=None, description="只看某个阶段下的任务；ID 必须来自本人阶段列表"
    )
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


class TaskDetailArgs(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)

    taskId: int = Field(description="任务 ID，来自任务列表中的 id")


def _brief_task(row: Any) -> dict[str, Any] | None:
    if not isinstance(row, dict) or not isinstance(row.get("name"), str):
        return None
    return {key: row[key] for key in _BRIEF_FIELDS if row.get(key) is not None}


def _detail_task(row: dict[str, Any]) -> dict[str, Any]:
    detail: dict[str, Any] = {key: row[key] for key in _DETAIL_FIELDS if row.get(key) is not None}
    detail["description"] = shared.text(row.get("description"), 300)
    detail["completionCriteria"] = shared.text(row.get("completionCriteria"), 300)
    return {key: value for key, value in detail.items() if value is not None}


async def run_list_tasks(
    context: AgentContext,
    client: JavaApiClient,
    args: TaskListArgs,
) -> ToolOutcome:
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    if args.status:
        params["status"] = args.status
    if args.priority:
        params["priority"] = args.priority
    if args.goalId is not None:
        params["goalId"] = args.goalId
    if args.stageId is not None:
        params["stageId"] = args.stageId
    keyword = (args.keyword or "").strip()
    if keyword:
        params["keyword"] = keyword
    result, error_message = await shared.get(context, client, "tasks", params)
    if error_message is not None:
        return shared.error(error_message, "任务查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        return shared.error("任务返回结构不正确", "任务返回异常")
    items = [brief for row in data["items"] if (brief := _brief_task(row)) is not None]
    payload = shared.page(items, data.get("total"))
    return ToolOutcome(
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        summary=f"任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


async def run_get_task(
    context: AgentContext, client: JavaApiClient, args: TaskDetailArgs
) -> ToolOutcome:
    result, error_message = await shared.get(context, client, f"tasks/{args.taskId}")
    if error_message is not None:
        return shared.error(error_message, "任务详情查询失败")
    data = result.data if result is not None else None
    if not isinstance(data, dict) or data.get("id") is None:
        return shared.error("任务详情返回结构不正确", "任务详情返回异常")
    task = _detail_task(data)
    return ToolOutcome(
        content=json.dumps({"task": task}, ensure_ascii=False, separators=(",", ":")),
        summary=f"任务「{task.get('name', '')}」详情",
    )


TASK_LIST_TOOL = ToolSpec(
    name="list_tasks",
    label="任务列表",
    description=(
        "查询用户本人的任务列表，可按任务状态、优先级、标题关键字、所属目标或阶段筛选，"
        "最多返回 20 条并给出总条数。返回任务 ID、名称、状态、优先级、截止日期、"
        "所属目标/阶段、安排位置和预计时长。用户查询任务列表、统计任务数量或按条件筛选任务时使用；"
        "需要任务完整信息时再用 get_task 查询详情。"
    ),
    permission="task:task:list",
    args_model=TaskListArgs,
    handler=run_list_tasks,
)

TASK_DETAIL_TOOL = ToolSpec(
    name="get_task",
    label="任务详情",
    description=(
        "按任务 ID 查询用户本人的任务详情：名称、描述、完成条件、优先级、权重、"
        "预计时长、起止日期、状态、完成时间、所属目标/阶段和安排位置。"
        "任务 ID 必须来自本人任务列表。用户询问某个任务的完整信息或完成情况时使用。"
    ),
    permission="task:task:read",
    args_model=TaskDetailArgs,
    handler=run_get_task,
)
