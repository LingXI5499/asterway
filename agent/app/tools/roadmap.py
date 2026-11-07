"""有界地聚合一个目标的阶段与任务，避免逐层查询耗尽对话预算。"""

import asyncio
import json

from app.tools import shared
from app.tools.base import ToolOutcome, ToolSpec
from app.tools.goals import GoalDetailArgs
from app.tools.goals import _detail as goal_detail
from app.tools.stages import _detail as stage_detail


async def run(context, client, args: GoalDetailArgs) -> ToolOutcome:
    response, error = await shared.get(context, client, f"goals/{args.goalId}")
    if error:
        return shared.error(error, "目标全貌暂未读取成功")
    data = response.data if response else None
    if not isinstance(data, dict) or data.get("id") is None:
        return shared.error("目标详情返回不完整", "目标全貌返回异常")
    stages = data.get("stages") or []
    selected = [row for row in stages if isinstance(row, dict) and row.get("id") is not None][:8]
    slots = asyncio.Semaphore(3)

    async def read(row):
        async with slots:
            value, failure = await shared.get(context, client, f"stages/{row['id']}")
        if failure or not value or not isinstance(value.data, dict):
            return {"id": row["id"], "name": row.get("name"), "unavailable": True}
        return stage_detail(value.data)

    details = await asyncio.gather(*(read(row) for row in selected))
    partial = len(selected) < len(stages) or any(row.get("unavailable") for row in details)
    payload = {"goal": goal_detail(data), "stages": details, "stageTotal": len(stages),
               "stageReturned": len(details), "partial": partial,
               "scope": "包含最多8个阶段的任务概览；未读取的阶段或详细任务内容不能视为空"}
    return ToolOutcome(json.dumps(payload, ensure_ascii=False),
        f"目标及 {len(details)} 个阶段的任务概览" + ("（部分资料）" if partial else ""))


GOAL_ROADMAP_TOOL = ToolSpec(
    name="get_goal_roadmap", label="目标全貌",
    description="一次查询本人目标的详情、阶段及阶段下的任务概览。询问整体进度、完整路线或目标有哪些任务时优先使用，"
                "无需再逐项重复 list_stages/get_stage。最多聚合8个阶段并明确部分结果。目标ID来自本人目标列表。",
    permission="goal:goal:read", extra_permissions=("stage:stage:read",),
    args_model=GoalDetailArgs, handler=run,
)
