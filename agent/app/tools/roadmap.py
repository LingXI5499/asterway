# 把一个目标的阶段和任务合并查询，减少重复请求。
# 有界地聚合一个目标的阶段与任务，避免逐层查询耗尽对话预算。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 app.tools 引入 shared。
from app.tools import shared

# 从 app.tools.base 引入 业务工具Outcome（ToolOutcome）、业务工具Spec（ToolSpec）。
from app.tools.base import ToolOutcome, ToolSpec

# 从 app.tools.goals 引入 目标详情参数（GoalDetailArgs）。
from app.tools.goals import GoalDetailArgs

# 从 app.tools.goals 引入 目标详情（goal_detail）。
from app.tools.goals import _detail as goal_detail

# 从 app.tools.stages 引入 阶段详情（stage_detail）。
from app.tools.stages import _detail as stage_detail


# 异步函数 run：调用 run 完成方案运行处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run(context, client, args: GoalDetailArgs) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 响应错误（(response, error)）。
    response, error = await shared.get(context, client, f"goals/{args.goalId}")
    # 检查条件 error 是否成立；成立时执行缩进的分支。
    if error:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error, "目标全貌暂未读取成功")
    # 把条件 response 是否成立时使用响应（response）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = response.data if response else None
    # 检查not isinstance(data, dict) or data.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(data, dict) or data.get("id") is None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("目标详情返回不完整", "目标全貌返回异常")
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者空列表或集合保存到 阶段列表（stages）。
    stages = data.get("stages") or []
    # 把从逐项处理数据，并按这里的条件筛选或转换后收集结果中按表达式 :8 的结果取出的内容保存到 已选（selected）。
    selected = [row for row in stages if isinstance(row, dict) and row.get("id") is not None][:8]
    # 把执行“调用 asyncio.Semaphore 完成并发名额控制器处理”得到的结果保存到 slots。
    slots = asyncio.Semaphore(3)

    # 异步函数 read：调用 read 完成读取处理；接收 当前记录（row）。
    async def read(row):
        # 异步进入slots；退出时由上下文管理器提交、回滚或释放资源。
        async with slots:
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 值失败（(value, failure)）。
            value, failure = await shared.get(context, client, f"stages/{row['id']}")
        # 检查条件 failure or not value or (not isinstance(value.data, dict)) 是否成立；成立时执行缩进的分支。
        if failure or not value or not isinstance(value.data, dict):
            # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
            return {"id": row["id"], "name": row.get("name"), "unavailable": True}
        # 把执行“调用 stage_detail 完成阶段详情处理”得到的结果交还给调用方，本函数到此结束。
        return stage_detail(value.data)

    # 把等待执行“等待多个异步任务完成并收集结果”得到的结果完成后的结果保存到 详情（details）。
    details = await asyncio.gather(*(read(row) for row in selected))
    # 把条件 len(selected) < len(stages) 是否成立或者执行“调用 any 完成any处理”得到的结果保存到 是否只有部分结果（partial）。
    partial = len(selected) < len(stages) or any(row.get("unavailable") for row in details)
    # 把按字段名保存多个值的字典保存到 提交数据（payload）。
    payload = {"goal": goal_detail(data), "stages": details, "stageTotal": len(stages),
               # 填写阶段已返回（stageReturned）字段，值为集合或文字的长度。
               "stageReturned": len(details), "partial": partial,
               # 填写数据范围（scope）字段，值为表达式 "包含最多8个阶段的任务概览；未读取的阶段或详细任务内容不能视为空"} 的结果。
               "scope": "包含最多8个阶段的任务概览；未读取的阶段或详细任务内容不能视为空"}
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(json.dumps(payload, ensure_ascii=False),
        # 继续传入或组合按表达式计算得到的值：f"目标及 {len(details)} 个阶段的任务概览" + ("（部分资料）" if partial else ""))，完成上面尚未结束的表达式。
        f"目标及 {len(details)} 个阶段的任务概览" + ("（部分资料）" if partial else ""))


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 目标ROADMAP业务工具（GOAL_ROADMAP_TOOL）。
GOAL_ROADMAP_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_goal_roadmap", label="目标全貌'。
    name="get_goal_roadmap", label="目标全貌",
    # 传入说明（description），值为固定文字 '一次查询本人目标的详情、阶段及阶段下的任务概览。询问整体进度、完整路线或目标有哪些任务时优先使用，'。
    description="一次查询本人目标的详情、阶段及阶段下的任务概览。询问整体进度、完整路线或目标有哪些任务时优先使用，"
                "无需再逐项重复 list_stages/get_stage。最多聚合8个阶段并明确部分结果。目标ID来自本人目标列表。",
    # 传入权限（permission），值为表达式 "goal:goal:read", extra_permissions=("stage:stage:read",) 的结果。
    permission="goal:goal:read", extra_permissions=("stage:stage:read",),
    # 传入参数模型（args_model），值为表达式 GoalDetailArgs, handler=run 的结果。
    args_model=GoalDetailArgs, handler=run,
)
