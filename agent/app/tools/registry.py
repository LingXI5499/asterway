# 登记允许模型调用的业务工具及参数结构。
# 只读工具注册表；新增工具时必须同时声明权限和参数模型。

# 从 typing 引入 Any。
from typing import Any

# 从 app.tools.base 引入 业务工具Spec（ToolSpec）。
from app.tools.base import ToolSpec

# 从 app.tools.dashboard 引入 今天OVERVIEW业务工具（TODAY_OVERVIEW_TOOL）。
from app.tools.dashboard import TODAY_OVERVIEW_TOOL

# 从 app.tools.execution 引入 当前执行计时业务工具（CURRENT_EXECUTION_TOOL）、执行计时详情业务工具（EXECUTION_DETAIL_TOOL）、执行计时列表业务工具（EXECUTION_LIST_TOOL）、执行计时汇总业务工具（EXECUTION_SUMMARY_TOOL）、连续计时段列表业务工具（SEGMENT_LIST_TOOL）。
from app.tools.execution import (
    # 继续传入或组合当前执行计时业务工具（CURRENT_EXECUTION_TOOL），完成上面尚未结束的表达式。
    CURRENT_EXECUTION_TOOL,
    # 继续传入或组合执行计时详情业务工具（EXECUTION_DETAIL_TOOL），完成上面尚未结束的表达式。
    EXECUTION_DETAIL_TOOL,
    # 继续传入或组合执行计时列表业务工具（EXECUTION_LIST_TOOL），完成上面尚未结束的表达式。
    EXECUTION_LIST_TOOL,
    # 继续传入或组合执行计时汇总业务工具（EXECUTION_SUMMARY_TOOL），完成上面尚未结束的表达式。
    EXECUTION_SUMMARY_TOOL,
    # 继续传入或组合连续计时段列表业务工具（SEGMENT_LIST_TOOL），完成上面尚未结束的表达式。
    SEGMENT_LIST_TOOL,
)

# 从 app.tools.goals 引入 目标详情业务工具（GOAL_DETAIL_TOOL）、目标列表业务工具（GOAL_LIST_TOOL）。
from app.tools.goals import GOAL_DETAIL_TOOL, GOAL_LIST_TOOL

# 从 app.tools.habit 引入 习惯分类列表业务工具（HABIT_CATEGORY_LIST_TOOL）、习惯打卡列表业务工具（HABIT_CHECKIN_LIST_TOOL）、习惯详情业务工具（HABIT_DETAIL_TOOL）、习惯列表业务工具（HABIT_LIST_TOOL）、习惯修订版本列表业务工具（HABIT_REVISION_LIST_TOOL）、今天习惯业务工具（TODAY_HABIT_TOOL）。
from app.tools.habit import (
    # 继续传入或组合习惯分类列表业务工具（HABIT_CATEGORY_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_CATEGORY_LIST_TOOL,
    # 继续传入或组合习惯打卡列表业务工具（HABIT_CHECKIN_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_CHECKIN_LIST_TOOL,
    # 继续传入或组合习惯详情业务工具（HABIT_DETAIL_TOOL），完成上面尚未结束的表达式。
    HABIT_DETAIL_TOOL,
    # 继续传入或组合习惯列表业务工具（HABIT_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_LIST_TOOL,
    # 继续传入或组合习惯修订版本列表业务工具（HABIT_REVISION_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_REVISION_LIST_TOOL,
    # 继续传入或组合今天习惯业务工具（TODAY_HABIT_TOOL），完成上面尚未结束的表达式。
    TODAY_HABIT_TOOL,
)

# 从 app.tools.planning 引入 每日方案业务工具（DAILY_PLAN_TOOL）、等待处理任务列表业务工具（PENDING_TASKS_TOOL）、规划任务列表业务工具（PLANNING_TASKS_TOOL）、任务任务安排业务工具（TASK_ASSIGNMENT_TOOL）、每周方案详情业务工具（WEEKLY_PLAN_DETAIL_TOOL）、每周方案列表业务工具（WEEKLY_PLAN_LIST_TOOL）。
from app.tools.planning import (
    # 继续传入或组合每日方案业务工具（DAILY_PLAN_TOOL），完成上面尚未结束的表达式。
    DAILY_PLAN_TOOL,
    # 继续传入或组合等待处理任务列表业务工具（PENDING_TASKS_TOOL），完成上面尚未结束的表达式。
    PENDING_TASKS_TOOL,
    # 继续传入或组合规划任务列表业务工具（PLANNING_TASKS_TOOL），完成上面尚未结束的表达式。
    PLANNING_TASKS_TOOL,
    # 继续传入或组合任务任务安排业务工具（TASK_ASSIGNMENT_TOOL），完成上面尚未结束的表达式。
    TASK_ASSIGNMENT_TOOL,
    # 继续传入或组合每周方案详情业务工具（WEEKLY_PLAN_DETAIL_TOOL），完成上面尚未结束的表达式。
    WEEKLY_PLAN_DETAIL_TOOL,
    # 继续传入或组合每周方案列表业务工具（WEEKLY_PLAN_LIST_TOOL），完成上面尚未结束的表达式。
    WEEKLY_PLAN_LIST_TOOL,
)

# 从 app.tools.roadmap 引入 目标ROADMAP业务工具（GOAL_ROADMAP_TOOL）。
from app.tools.roadmap import GOAL_ROADMAP_TOOL

# 从 app.tools.stages 引入 阶段详情业务工具（STAGE_DETAIL_TOOL）、阶段列表业务工具（STAGE_LIST_TOOL）。
from app.tools.stages import STAGE_DETAIL_TOOL, STAGE_LIST_TOOL

# 从 app.tools.statistics 引入 成长业务成长时间线业务工具（GROWTH_TIMELINE_TOOL）、习惯统计业务工具（HABIT_STATISTICS_TOOL）、对象统计业务工具（OBJECT_STATISTICS_TOOL）、统计汇总业务工具（STATISTICS_SUMMARY_TOOL）、统计趋势列表业务工具（STATISTICS_TRENDS_TOOL）。
from app.tools.statistics import (
    # 继续传入或组合成长业务成长时间线业务工具（GROWTH_TIMELINE_TOOL），完成上面尚未结束的表达式。
    GROWTH_TIMELINE_TOOL,
    # 继续传入或组合习惯统计业务工具（HABIT_STATISTICS_TOOL），完成上面尚未结束的表达式。
    HABIT_STATISTICS_TOOL,
    # 继续传入或组合对象统计业务工具（OBJECT_STATISTICS_TOOL），完成上面尚未结束的表达式。
    OBJECT_STATISTICS_TOOL,
    # 继续传入或组合统计汇总业务工具（STATISTICS_SUMMARY_TOOL），完成上面尚未结束的表达式。
    STATISTICS_SUMMARY_TOOL,
    # 继续传入或组合统计趋势列表业务工具（STATISTICS_TRENDS_TOOL），完成上面尚未结束的表达式。
    STATISTICS_TRENDS_TOOL,
)

# 从 app.tools.tasks 引入 任务详情业务工具（TASK_DETAIL_TOOL）、任务列表业务工具（TASK_LIST_TOOL）。
from app.tools.tasks import TASK_DETAIL_TOOL, TASK_LIST_TOOL

# 声明 业务工具列表（TOOLS），类型为 tuple[ToolSpec, ...]；其内容为依次包含 今天OVERVIEW业务工具（TODAY_OVERVIEW_TOOL）、任务列表业务工具（TASK_LIST_TOOL）、任务详情业务工具（TASK_DETAIL_TOOL）、目标列表业务工具（GOAL_LIST_TOOL）、目标详情业务工具（GOAL_DETAIL_TOOL）等内容 的集合。
TOOLS: tuple[ToolSpec, ...] = (
    # 继续传入或组合今天OVERVIEW业务工具（TODAY_OVERVIEW_TOOL），完成上面尚未结束的表达式。
    TODAY_OVERVIEW_TOOL,
    # 继续传入或组合任务列表业务工具（TASK_LIST_TOOL），完成上面尚未结束的表达式。
    TASK_LIST_TOOL,
    # 继续传入或组合任务详情业务工具（TASK_DETAIL_TOOL），完成上面尚未结束的表达式。
    TASK_DETAIL_TOOL,
    # 继续传入或组合目标列表业务工具（GOAL_LIST_TOOL），完成上面尚未结束的表达式。
    GOAL_LIST_TOOL,
    # 继续传入或组合目标详情业务工具（GOAL_DETAIL_TOOL），完成上面尚未结束的表达式。
    GOAL_DETAIL_TOOL,
    # 继续传入或组合目标ROADMAP业务工具（GOAL_ROADMAP_TOOL），完成上面尚未结束的表达式。
    GOAL_ROADMAP_TOOL,
    # 继续传入或组合阶段列表业务工具（STAGE_LIST_TOOL），完成上面尚未结束的表达式。
    STAGE_LIST_TOOL,
    # 继续传入或组合阶段详情业务工具（STAGE_DETAIL_TOOL），完成上面尚未结束的表达式。
    STAGE_DETAIL_TOOL,
    # 继续传入或组合每周方案列表业务工具（WEEKLY_PLAN_LIST_TOOL），完成上面尚未结束的表达式。
    WEEKLY_PLAN_LIST_TOOL,
    # 继续传入或组合每周方案详情业务工具（WEEKLY_PLAN_DETAIL_TOOL），完成上面尚未结束的表达式。
    WEEKLY_PLAN_DETAIL_TOOL,
    # 继续传入或组合每日方案业务工具（DAILY_PLAN_TOOL），完成上面尚未结束的表达式。
    DAILY_PLAN_TOOL,
    # 继续传入或组合规划任务列表业务工具（PLANNING_TASKS_TOOL），完成上面尚未结束的表达式。
    PLANNING_TASKS_TOOL,
    # 继续传入或组合等待处理任务列表业务工具（PENDING_TASKS_TOOL），完成上面尚未结束的表达式。
    PENDING_TASKS_TOOL,
    # 继续传入或组合任务任务安排业务工具（TASK_ASSIGNMENT_TOOL），完成上面尚未结束的表达式。
    TASK_ASSIGNMENT_TOOL,
    # 继续传入或组合当前执行计时业务工具（CURRENT_EXECUTION_TOOL），完成上面尚未结束的表达式。
    CURRENT_EXECUTION_TOOL,
    # 继续传入或组合执行计时列表业务工具（EXECUTION_LIST_TOOL），完成上面尚未结束的表达式。
    EXECUTION_LIST_TOOL,
    # 继续传入或组合执行计时详情业务工具（EXECUTION_DETAIL_TOOL），完成上面尚未结束的表达式。
    EXECUTION_DETAIL_TOOL,
    # 继续传入或组合连续计时段列表业务工具（SEGMENT_LIST_TOOL），完成上面尚未结束的表达式。
    SEGMENT_LIST_TOOL,
    # 继续传入或组合执行计时汇总业务工具（EXECUTION_SUMMARY_TOOL），完成上面尚未结束的表达式。
    EXECUTION_SUMMARY_TOOL,
    # 继续传入或组合习惯列表业务工具（HABIT_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_LIST_TOOL,
    # 继续传入或组合习惯详情业务工具（HABIT_DETAIL_TOOL），完成上面尚未结束的表达式。
    HABIT_DETAIL_TOOL,
    # 继续传入或组合习惯分类列表业务工具（HABIT_CATEGORY_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_CATEGORY_LIST_TOOL,
    # 继续传入或组合今天习惯业务工具（TODAY_HABIT_TOOL），完成上面尚未结束的表达式。
    TODAY_HABIT_TOOL,
    # 继续传入或组合习惯打卡列表业务工具（HABIT_CHECKIN_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_CHECKIN_LIST_TOOL,
    # 继续传入或组合习惯修订版本列表业务工具（HABIT_REVISION_LIST_TOOL），完成上面尚未结束的表达式。
    HABIT_REVISION_LIST_TOOL,
    # 继续传入或组合统计汇总业务工具（STATISTICS_SUMMARY_TOOL），完成上面尚未结束的表达式。
    STATISTICS_SUMMARY_TOOL,
    # 继续传入或组合统计趋势列表业务工具（STATISTICS_TRENDS_TOOL），完成上面尚未结束的表达式。
    STATISTICS_TRENDS_TOOL,
    # 继续传入或组合成长业务成长时间线业务工具（GROWTH_TIMELINE_TOOL），完成上面尚未结束的表达式。
    GROWTH_TIMELINE_TOOL,
    # 继续传入或组合习惯统计业务工具（HABIT_STATISTICS_TOOL），完成上面尚未结束的表达式。
    HABIT_STATISTICS_TOOL,
    # 继续传入或组合对象统计业务工具（OBJECT_STATISTICS_TOOL），完成上面尚未结束的表达式。
    OBJECT_STATISTICS_TOOL,
)
# 声明 业务工具对应表（TOOL_MAP），类型为 dict[str, ToolSpec]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
TOOL_MAP: dict[str, ToolSpec] = {tool.name: tool for tool in TOOLS}


# 函数 tool_for：调用 tool_for 完成业务工具for处理；接收 名称（name）。
def tool_for(name: str) -> ToolSpec | None:
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果交还给调用方，本函数到此结束。
    return TOOL_MAP.get(name)


# 函数 label_for：调用 label_for 完成显示名称for处理；接收 名称（name）。
def label_for(name: str) -> str:
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 spec。
    spec = TOOL_MAP.get(name)
    # 把条件 spec is not None 是否成立时使用spec的显示名称（label）属性，否则使用固定文字 '业务数据'交还给调用方，本函数到此结束。
    return spec.label if spec is not None else "业务数据"


# 函数 tool_definitions：OpenAI 兼容的 tools 数组；描述和 Schema 只来自服务端注册表。；接收 权限列表（permissions）。
def tool_definitions(permissions: frozenset[str] | None = None) -> list[dict[str, Any]]:
    # OpenAI 兼容的 tools 数组；描述和 Schema 只来自服务端注册表。
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return [
        {
            # 填写类型（type）字段，值为固定文字 'function'。
            "type": "function",
            # 填写function字段，值为表达式 { 的结果。
            "function": {
                # 填写名称（name）字段，值为表达式 tool.name 的结果。
                "name": tool.name,
                # 填写说明（description）字段，值为表达式 tool.description 的结果。
                "description": tool.description,
                # 填写parameters字段，值为表达式 tool.parameters 的结果。
                "parameters": tool.parameters,
            },
        }
        # 继续传入或组合表达式 for tool in TOOLS 的结果，完成上面尚未结束的表达式。
        for tool in TOOLS
        # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：if permissions is None or tool.allowed(permissions)，完成上面尚未结束的表达式。
        if permissions is None or tool.allowed(permissions)
    ]
