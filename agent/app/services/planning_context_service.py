# 只读收集用户已有目标、任务、安排和习惯，供规划时避免重复创建。
# 规划业务上下文：只读读取现有目标、任务、计划、习惯，供方案生成避免重复与冲突。
# 
# 只读收集本人业务事实；读取失败明确终止，避免基于缺失事实生成可执行方案。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 从 typing 引入 Any。
from typing import Any

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）、Java后端接口错误（JavaApiError）。
from app.integrations.java_client import JavaApiClient, JavaApiError

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 把固定数值 10000保存到 最大CHARS（_MAX_CHARS）。
_MAX_CHARS = 10000


# 异步函数 _get：调用 _get 完成读取处理；接收 上下文（context）、客户端（client）、路径（path）、参数集合（params）。
async def _get(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写路径（path）字段，值为str。
    path: str,
    # 填写参数集合（params）字段，值为表达式 dict[str, str | int | bool] | None = None 的结果。
    params: dict[str, str | int | bool] | None = None,
# 继续传入或组合表达式 ) -> Any: 的结果，完成上面尚未结束的表达式。
) -> Any:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果（result）。
    result = await client.get(
        # 继续传入或组合路径（path），完成上面尚未结束的表达式。
        path,
        # 传入授权（authorization），值为表达式 context.authorization 的结果。
        authorization=context.authorization,
        # 传入追踪标识编号（trace_id），值为表达式 context.trace_id 的结果。
        trace_id=context.trace_id,
        # 传入参数集合（params），值为参数集合（params）。
        params=params,
    )
    # 把结果（result）的数据（data）属性交还给调用方，本函数到此结束。
    return result.data


# 函数 _short：调用 _short 完成short处理；接收 值（value）、上限（limit）。
def _short(value: Any, limit: int = 60) -> str:
    # 把执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果保存到 文字（text）。
    text = str(value or "").strip().replace("\n", " ")
    # 把从文字（text）中按表达式 :limit 的结果取出的内容交还给调用方，本函数到此结束。
    return text[:limit]


# 异步函数 collect：按意图收集最相关的现有业务事实，压缩成模型可读文本。；接收 上下文（context）、客户端（client）、intent。
async def collect(context: AgentContext, client: JavaApiClient, intent: str) -> str | None:
    # 按意图收集最相关的现有业务事实，压缩成模型可读文本。
    # 声明 blocks，类型为 list[str]；其内容为空列表或集合。
    blocks: list[str] = []
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 dashboard。
        dashboard = await _get(context, client, "dashboard/overview")
        # 检查条件 isinstance(dashboard, dict) 是否成立；成立时执行缩进的分支。
        if isinstance(dashboard, dict):
            # 把空列表或集合保存到 parts。
            parts = []
            # 检查条件 dashboard.get('today') 是否成立；成立时执行缩进的分支。
            if dashboard.get("today"):
                # 把新条目放到列表末尾。
                parts.append(f"账号今天 {dashboard['today']}")
            # 检查条件 dashboard.get('timeZone') 是否成立；成立时执行缩进的分支。
            if dashboard.get("timeZone"):
                # 把新条目放到列表末尾。
                parts.append(f"时区 {dashboard['timeZone']}")
            # 检查条件 parts 是否成立；成立时执行缩进的分支。
            if parts:
                # 把新条目放到列表末尾。
                blocks.append("；".join(parts))

        # 把条件 intent in {'GOAL_PLAN', 'STAGE_PLAN', 'TASK_PLAN', 'COMPREHENSIVE_PLAN'} 是否成立保存到 needs目标（needs_goal）。
        needs_goal = intent in {
            # 继续传入或组合固定状态或编码 GOAL_PLAN（目标方案），完成上面尚未结束的表达式。
            "GOAL_PLAN",
            # 继续传入或组合固定状态或编码 STAGE_PLAN（阶段方案），完成上面尚未结束的表达式。
            "STAGE_PLAN",
            # 继续传入或组合固定状态或编码 TASK_PLAN（任务方案），完成上面尚未结束的表达式。
            "TASK_PLAN",
            # 继续传入或组合固定状态或编码 COMPREHENSIVE_PLAN（综合方案），完成上面尚未结束的表达式。
            "COMPREHENSIVE_PLAN",
        }
        # 把条件 intent in {'WEEKLY_PLAN', 'DAILY_PLAN', 'COMPREHENSIVE_PLAN'} 是否成立保存到 needs方案（needs_plan）。
        needs_plan = intent in {
            # 继续传入或组合固定状态或编码 WEEKLY_PLAN（每周方案），完成上面尚未结束的表达式。
            "WEEKLY_PLAN",
            # 继续传入或组合固定状态或编码 DAILY_PLAN（每日方案），完成上面尚未结束的表达式。
            "DAILY_PLAN",
            # 继续传入或组合固定状态或编码 COMPREHENSIVE_PLAN（综合方案），完成上面尚未结束的表达式。
            "COMPREHENSIVE_PLAN",
        }
        # 把条件 intent in {'HABIT_PLAN', 'COMPREHENSIVE_PLAN'} 是否成立保存到 needs习惯（needs_habit）。
        needs_habit = intent in {"HABIT_PLAN", "COMPREHENSIVE_PLAN"}

        # 检查条件 needs_goal 是否成立；成立时执行缩进的分支。
        if needs_goal:
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 目标列表（goals）。
            goals = await _get(context, client, "goals", {"page": 1, "size": 20})
            # 把条件 isinstance(goals, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 数据列表（items）。
            items = goals.get("items") if isinstance(goals, dict) else None
            # 检查条件 isinstance(items, list) and items 是否成立；成立时执行缩进的分支。
            if isinstance(items, list) and items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 lines。
                lines = [
                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                    f"- ID={item.get('id')} version={item.get('version')} {_short(item.get('name'))}（{item.get('status')}，{item.get('startDate')}~{item.get('expectedEndDate')}）"
                    # 继续传入或组合表达式 for item in items[:20] 的结果，完成上面尚未结束的表达式。
                    for item in items[:20]
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("现有目标（不要重复创建同名目标）：\n" + "\n".join(lines))
                # 检查条件 intent in {'TASK_PLAN', 'COMPREHENSIVE_PLAN'} 是否成立；成立时执行缩进的分支。
                if intent in {"TASK_PLAN", "COMPREHENSIVE_PLAN"}:
                    # 从从数据列表（items）中按表达式 :5 的结果取出的内容依次取出目标（goal），每项执行一次下面的处理。
                    for goal in items[:5]:
                        # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 阶段列表（stages）。
                        stages = await _get(
                            # 继续传入或组合按表达式计算得到的值：context, client, f"goals/{goal['id']}/stages", {"page": 1, "size": 20}，完成上面尚未结束的表达式。
                            context, client, f"goals/{goal['id']}/stages", {"page": 1, "size": 20}
                        )
                        # 把新条目放到列表末尾。
                        blocks.append(
                            # 继续传入或组合固定文字 '已有阶段（可通过 stageId/stageVersion 引用）：'，完成上面尚未结束的表达式。
                            "已有阶段（可通过 stageId/stageVersion 引用）："
                            # 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的表达式。
                            + json.dumps(
                                [
                                    {
                                        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                                        key: stage.get(key)
                                        # 继续传入或组合表达式 for key in ("id", "version", "name", "goalId", "status") 的结果，完成上面尚未结束的表达式。
                                        for key in ("id", "version", "name", "goalId", "status")
                                    }
                                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                                    for stage in stages.get("items", [])
                                ],
                                # 传入ensureascii（ensure_ascii），值为假，表示条件不成立或开关关闭。
                                ensure_ascii=False,
                            )
                        )
            # 前面的条件不满足时，按这里的条件或备用分支继续处理。
            else:
                # 把新条目放到列表末尾。
                blocks.append("现有目标：无")
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 任务列表（tasks）。
            tasks = await _get(context, client, "tasks", {"page": 1, "size": 50})
            # 把条件 isinstance(tasks, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 任务数据列表（task_items）。
            task_items = tasks.get("items") if isinstance(tasks, dict) else None
            # 检查条件 isinstance(task_items, list) and task_items 是否成立；成立时执行缩进的分支。
            if isinstance(task_items, list) and task_items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 任务lines（task_lines）。
                task_lines = [
                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                    f"- ID={item.get('id')} version={item.get('version')} stageId={item.get('stageId')} {_short(item.get('name'))}（{item.get('status')}，优先级 {item.get('priority')}，截止 {item.get('dueDate')}）"
                    # 继续传入或组合表达式 for item in task_items[:30] 的结果，完成上面尚未结束的表达式。
                    for item in task_items[:30]
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("现有任务（不要重复创建同名任务）：\n" + "\n".join(task_lines))
            # 前面的条件不满足时，按这里的条件或备用分支继续处理。
            else:
                # 把新条目放到列表末尾。
                blocks.append("现有任务：无")

        # 检查条件 needs_plan 是否成立；成立时执行缩进的分支。
        if needs_plan:
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 是否可用（available）。
            available = await _get(context, client, "tasks", {"page": 1, "size": 20})
            # 把条件 isinstance(available, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空列表或集合保存到 是否可用数据列表（available_items）。
            available_items = available.get("items", []) if isinstance(available, dict) else []
            # 把新条目放到列表末尾。
            blocks.append(
                # 继续传入或组合固定文字 '已有任务（taskId/taskVersion；仅展示首屏，未列出不代表不存在）：'，完成上面尚未结束的表达式。
                "已有任务（taskId/taskVersion；仅展示首屏，未列出不代表不存在）："
                # 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的表达式。
                + json.dumps(
                    [
                        {
                            # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                            key: item.get(key)
                            # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
                            for key in (
                                # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
                                "id",
                                # 继续传入或组合固定文字 'version'，完成上面尚未结束的表达式。
                                "version",
                                # 继续传入或组合固定文字 'name'，完成上面尚未结束的表达式。
                                "name",
                                # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
                                "status",
                                # 继续传入或组合固定文字 'priority'，完成上面尚未结束的表达式。
                                "priority",
                                # 继续传入或组合固定文字 'estimatedMinutes'，完成上面尚未结束的表达式。
                                "estimatedMinutes",
                                # 继续传入或组合固定文字 'assignmentStatus'，完成上面尚未结束的表达式。
                                "assignmentStatus",
                                # 继续传入或组合固定文字 'weeklyPlanId'，完成上面尚未结束的表达式。
                                "weeklyPlanId",
                                # 继续传入或组合固定文字 'dailyPlanId'，完成上面尚未结束的表达式。
                                "dailyPlanId",
                            )
                        }
                        # 继续传入或组合表达式 for item in available_items 的结果，完成上面尚未结束的表达式。
                        for item in available_items
                        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                        if item.get("status") != "COMPLETED"
                    ],
                    # 传入ensureascii（ensure_ascii），值为假，表示条件不成立或开关关闭。
                    ensure_ascii=False,
                )
            )
            # 从从逐项处理数据，并按这里的条件筛选或转换后收集结果中按表达式 :12 的结果取出的内容依次取出单条数据（item），每项执行一次下面的处理。
            for item in [row for row in available_items if row.get("assignmentStatus") in {"WEEK", "DAY"}][
                # 继续传入或组合表达式 :12 的结果，完成上面尚未结束的表达式。
                :12
            ]:
                # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 任务安排（assignment）。
                assignment = await _get(context, client, f"tasks/{item['id']}/assignment")
                # 把新条目放到列表末尾。
                blocks.append(
                    # 继续传入或组合固定文字 '已有任务安排（跨周或改期必须保留 assignmentId/assignmentVersion）：'，完成上面尚未结束的表达式。
                    "已有任务安排（跨周或改期必须保留 assignmentId/assignmentVersion）："
                    # 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的表达式。
                    + json.dumps(
                        # 继续传入或组合表达式 {"taskId": item["id"], "assignment": assignment}, ensure_ascii=False 的结果，完成上面尚未结束的表达式。
                        {"taskId": item["id"], "assignment": assignment}, ensure_ascii=False
                    )
                )
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 周计划列表（weeks）。
            weeks = await _get(context, client, "weekly-plans", {"page": 1, "size": 10})
            # 把条件 isinstance(weeks, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 周数据列表（week_items）。
            week_items = weeks.get("items") if isinstance(weeks, dict) else None
            # 检查条件 isinstance(week_items, list) and week_items 是否成立；成立时执行缩进的分支。
            if isinstance(week_items, list) and week_items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 周lines（week_lines）。
                week_lines = [
                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                    f"- 周计划 {_short(item.get('weekStartDate'))}（ID {item.get('id')}）"
                    # 继续传入或组合表达式 for item in week_items[:10] 的结果，完成上面尚未结束的表达式。
                    for item in week_items[:10]
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("现有周计划：\n" + "\n".join(week_lines))
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 等待处理（pending）。
            pending = await _get(context, client, "planning/pending-tasks", {"page": 1, "size": 20})
            # 把条件 isinstance(pending, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 等待处理数据列表（pending_items）。
            pending_items = pending.get("items") if isinstance(pending, dict) else None
            # 检查条件 isinstance(pending_items, list) and pending_items 是否成立；成立时执行缩进的分支。
            if isinstance(pending_items, list) and pending_items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 等待处理lines（pending_lines）。
                pending_lines = [
                    # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：f"- {_short(item.get('name') or item.get('title'))}（原安排 {item.get('businessDate') or item.get('originalDate')}…，完成上面尚未结束的表达式。
                    f"- {_short(item.get('name') or item.get('title'))}（原安排 {item.get('businessDate') or item.get('originalDate')}）"
                    # 继续传入或组合表达式 for item in pending_items[:20] 的结果，完成上面尚未结束的表达式。
                    for item in pending_items[:20]
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("待重新安排任务：\n" + "\n".join(pending_lines))

        # 检查条件 needs_habit 是否成立；成立时执行缩进的分支。
        if needs_habit:
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 习惯列表（habits）。
            habits = await _get(context, client, "habits", {"page": 1, "size": 20})
            # 把条件 isinstance(habits, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 习惯数据列表（habit_items）。
            habit_items = habits.get("items") if isinstance(habits, dict) else None
            # 检查条件 isinstance(habit_items, list) and habit_items 是否成立；成立时执行缩进的分支。
            if isinstance(habit_items, list) and habit_items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 习惯lines（habit_lines）。
                habit_lines = [
                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                    f"- {_short(item.get('name'))}（{item.get('status')}，{item.get('cycleType')}）"
                    # 继续传入或组合表达式 for item in habit_items[:20] 的结果，完成上面尚未结束的表达式。
                    for item in habit_items[:20]
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("现有习惯（不要重复创建同名习惯）：\n" + "\n".join(habit_lines))
            # 前面的条件不满足时，按这里的条件或备用分支继续处理。
            else:
                # 把新条目放到列表末尾。
                blocks.append("现有习惯：无")
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 分类列表（categories）。
            categories = await _get(context, client, "habit-categories", {"page": 1, "size": 50})
            # 把条件 isinstance(categories, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 分类数据列表（category_items）。
            category_items = categories.get("items") if isinstance(categories, dict) else None
            # 检查条件 isinstance(category_items, list) and category_items 是否成立；成立时执行缩进的分支。
            if isinstance(category_items, list) and category_items:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 names。
                names = [
                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                    _short(item.get("name"), 20)
                    # 继续传入或组合表达式 for item in category_items 的结果，完成上面尚未结束的表达式。
                    for item in category_items
                    # 继续传入或组合表达式 if isinstance(item, dict) 的结果，完成上面尚未结束的表达式。
                    if isinstance(item, dict)
                ]
                # 把新条目放到列表末尾。
                blocks.append("现有习惯分类：" + "、".join(names))
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
    except JavaApiError as error:
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("agent_planning_context_unavailable code=%s", error.code)
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(error.status_code, error.code, error.message) from error
    # 捕获(TypeError, ValueError)异常，执行下面的错误处理，异常对象保存为 error。
    except (TypeError, ValueError) as error:
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("agent_planning_context_invalid type=%s", type(error).__name__)
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(
            # 继续传入或组合表达式 502, "PLANNING_CONTEXT_INVALID", "业务上下文返回不完整，请稍后重试" 的结果，完成上面尚未结束的表达式。
            502, "PLANNING_CONTEXT_INVALID", "业务上下文返回不完整，请稍后重试"
        # 继续传入或组合表达式 ) from error 的结果，完成上面尚未结束的表达式。
        ) from error
    # 检查条件 not blocks 是否成立；成立时执行缩进的分支。
    if not blocks:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 只要条件 blocks and len(json.dumps(blocks, ensure_ascii=False)) > _MAX_CHARS 是否成立，就继续执行下面的循环。
    while blocks and len(json.dumps(blocks, ensure_ascii=False)) > _MAX_CHARS:
        # 调用 blocks.pop 完成pop处理。
        blocks.pop()
    # 把条件 blocks 是否成立时使用执行“把对象编码为 JSON 文字”得到的结果，否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return json.dumps(blocks, ensure_ascii=False) if blocks else None


# 异步函数 verify_assignments：确认前填入权威的改期前信息；版本不一致时要求重新整理，不能静默覆盖。；接收 上下文（context）、客户端（client）、确认时的数据快照（snapshot）。
async def verify_assignments(context: AgentContext, client: JavaApiClient, snapshot: dict) -> dict:
    # 确认前填入权威的改期前信息；版本不一致时要求重新整理，不能静默覆盖。
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 existing。
    existing = [item for item in snapshot.get("taskAssignments", []) if item.get("taskId")]
    # 检查条件 len(existing) > 20 是否成立；成立时执行缩进的分支。
    if len(existing) > 20:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "PLAN_ADJUSTMENT_TOO_LARGE", "每次最多调整20个已有任务，请分批整理")
    # 从existing依次取出单条数据（item），每项执行一次下面的处理。
    for item in existing:
        # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 任务（task）。
        task = await _get(context, client, f"tasks/{item['taskId']}")
        # 检查条件 not isinstance(task, dict) or task.get('status') == 'COMPLETED' 是否成立；成立时执行缩进的分支。
        if not isinstance(task, dict) or task.get("status") == "COMPLETED":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "PLAN_TASK_CHANGED", "待安排任务已变化或已完成，请重新整理")
        # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 当前（current）。
        current = await _get(context, client, f"tasks/{item['taskId']}/assignment")
        # 检查条件 isinstance(current, dict) and current.get('id') 是否成立；成立时执行缩进的分支。
        if isinstance(current, dict) and current.get("id"):
            # 检查当前记录版本是否符合请求要求：str(item.get('assignmentId')) != str(current['id']) or item.get('assignmentVersion') != current.get('version')；成立时执行缩进的分支。
            if str(item.get("assignmentId")) != str(current["id"]) \
                    or item.get("assignmentVersion") != current.get("version"):
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "PLAN_ASSIGNMENT_CHANGED", "原任务安排已变化或缺少版本，请重新整理调整方案")
            # 把等待执行“调用 _get 完成读取处理”得到的结果完成后的结果保存到 周（week）。
            week = await _get(context, client, f"weekly-plans/{current['weeklyPlanId']}")
            # 把条件 current.get('dailyPlanId') 是否成立时使用等待执行“调用 _get 完成读取处理”得到的结果完成后的结果，否则使用空值，表示当前还没有对应内容保存到 日（day）。
            day = await _get(context, client, f"daily-plans/{current['dailyPlanId']}") if current.get("dailyPlanId") else None
            # 把条件 isinstance(week, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 单条数据上一次周开始（item['previousWeekStart']）。
            item["previousWeekStart"] = week.get("weekStartDate") if isinstance(week, dict) else None
            # 把条件 isinstance(day, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 单条数据上一次Business日期（item['previousBusinessDate']）。
            item["previousBusinessDate"] = day.get("planDate") if isinstance(day, dict) else None
        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
        else:
            # 检查当前记录版本是否符合请求要求：item.get('assignmentId') is not None or item.get('assignmentVersion') is not None；成立时执行缩进的分支。
            if item.get("assignmentId") is not None or item.get("assignmentVersion") is not None:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "PLAN_ASSIGNMENT_CHANGED", "原任务安排已失效，请重新整理")
            # 把空值，表示当前还没有对应内容保存到 单条数据上一次周开始（item['previousWeekStart']）、单条数据上一次Business日期（item['previousBusinessDate']）。
            item["previousWeekStart"] = item["previousBusinessDate"] = None
        # 把从执行“调用 str 完成str处理”得到的结果中按表达式 :100 的结果取出的内容保存到 单条数据任务标题（item['taskTitle']）。
        item["taskTitle"] = str(task.get("name") or task.get("title") or "已有任务")[:100]
    # 把确认时的数据快照（snapshot）交还给调用方，本函数到此结束。
    return snapshot
