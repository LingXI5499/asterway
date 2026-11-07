"""只读工具注册表；新增工具时必须同时声明权限和参数模型。"""

from typing import Any

from app.tools.base import ToolSpec
from app.tools.dashboard import TODAY_OVERVIEW_TOOL
from app.tools.execution import (
    CURRENT_EXECUTION_TOOL,
    EXECUTION_DETAIL_TOOL,
    EXECUTION_LIST_TOOL,
    EXECUTION_SUMMARY_TOOL,
    SEGMENT_LIST_TOOL,
)
from app.tools.goals import GOAL_DETAIL_TOOL, GOAL_LIST_TOOL
from app.tools.habit import (
    HABIT_CATEGORY_LIST_TOOL,
    HABIT_CHECKIN_LIST_TOOL,
    HABIT_DETAIL_TOOL,
    HABIT_LIST_TOOL,
    HABIT_REVISION_LIST_TOOL,
    TODAY_HABIT_TOOL,
)
from app.tools.planning import (
    DAILY_PLAN_TOOL,
    PENDING_TASKS_TOOL,
    PLANNING_TASKS_TOOL,
    TASK_ASSIGNMENT_TOOL,
    WEEKLY_PLAN_DETAIL_TOOL,
    WEEKLY_PLAN_LIST_TOOL,
)
from app.tools.roadmap import GOAL_ROADMAP_TOOL
from app.tools.stages import STAGE_DETAIL_TOOL, STAGE_LIST_TOOL
from app.tools.statistics import (
    GROWTH_TIMELINE_TOOL,
    HABIT_STATISTICS_TOOL,
    OBJECT_STATISTICS_TOOL,
    STATISTICS_SUMMARY_TOOL,
    STATISTICS_TRENDS_TOOL,
)
from app.tools.tasks import TASK_DETAIL_TOOL, TASK_LIST_TOOL

TOOLS: tuple[ToolSpec, ...] = (
    TODAY_OVERVIEW_TOOL,
    TASK_LIST_TOOL,
    TASK_DETAIL_TOOL,
    GOAL_LIST_TOOL,
    GOAL_DETAIL_TOOL,
    GOAL_ROADMAP_TOOL,
    STAGE_LIST_TOOL,
    STAGE_DETAIL_TOOL,
    WEEKLY_PLAN_LIST_TOOL,
    WEEKLY_PLAN_DETAIL_TOOL,
    DAILY_PLAN_TOOL,
    PLANNING_TASKS_TOOL,
    PENDING_TASKS_TOOL,
    TASK_ASSIGNMENT_TOOL,
    CURRENT_EXECUTION_TOOL,
    EXECUTION_LIST_TOOL,
    EXECUTION_DETAIL_TOOL,
    SEGMENT_LIST_TOOL,
    EXECUTION_SUMMARY_TOOL,
    HABIT_LIST_TOOL,
    HABIT_DETAIL_TOOL,
    HABIT_CATEGORY_LIST_TOOL,
    TODAY_HABIT_TOOL,
    HABIT_CHECKIN_LIST_TOOL,
    HABIT_REVISION_LIST_TOOL,
    STATISTICS_SUMMARY_TOOL,
    STATISTICS_TRENDS_TOOL,
    GROWTH_TIMELINE_TOOL,
    HABIT_STATISTICS_TOOL,
    OBJECT_STATISTICS_TOOL,
)
TOOL_MAP: dict[str, ToolSpec] = {tool.name: tool for tool in TOOLS}


def tool_for(name: str) -> ToolSpec | None:
    return TOOL_MAP.get(name)


def label_for(name: str) -> str:
    spec = TOOL_MAP.get(name)
    return spec.label if spec is not None else "业务数据"


def tool_definitions(permissions: frozenset[str] | None = None) -> list[dict[str, Any]]:
    """OpenAI 兼容的 tools 数组；描述和 Schema 只来自服务端注册表。"""
    return [
        {
            "type": "function",
            "function": {
                "name": tool.name,
                "description": tool.description,
                "parameters": tool.parameters,
            },
        }
        for tool in TOOLS
        if permissions is None or tool.allowed(permissions)
    ]
