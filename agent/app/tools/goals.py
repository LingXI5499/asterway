# 封装目标列表查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 目标只读工具：查询本人目标列表与目标详情（含阶段概览）。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 typing 引入 Any、只能从列出的固定值中选择的类型（Literal）。
from typing import Any, Literal

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）。
from pydantic import BaseModel, ConfigDict, Field

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.tools 引入 shared。
from app.tools import shared

# 从 app.tools.base 引入 业务工具Outcome（ToolOutcome）、业务工具Spec（ToolSpec）。
from app.tools.base import ToolOutcome, ToolSpec

# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 NOT_STARTED（NOT开始时）、固定状态或编码 IN_PROGRESS（IN进度）、固定状态或编码 COMPLETED（已完成） 的集合取出的内容保存到 目标状态（GoalStatus）。
GoalStatus = Literal["NOT_STARTED", "IN_PROGRESS", "COMPLETED"]

# 把依次包含 固定文字 'id'、固定文字 'name'、固定文字 'status'、固定文字 'startDate'、固定文字 'expectedEndDate'等内容 的集合保存到 目标汇总FIELDS（_GOAL_SUMMARY_FIELDS）。
_GOAL_SUMMARY_FIELDS = (
    # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
    "id",
    # 继续传入或组合固定文字 'name'，完成上面尚未结束的表达式。
    "name",
    # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
    "status",
    # 继续传入或组合固定文字 'startDate'，完成上面尚未结束的表达式。
    "startDate",
    # 继续传入或组合固定文字 'expectedEndDate'，完成上面尚未结束的表达式。
    "expectedEndDate",
    # 继续传入或组合固定文字 'progress'，完成上面尚未结束的表达式。
    "progress",
    # 继续传入或组合固定文字 'archivedAt'，完成上面尚未结束的表达式。
    "archivedAt",
    # 继续传入或组合固定文字 'completionConditionMet'，完成上面尚未结束的表达式。
    "completionConditionMet",
)


# 定义 GoalListArgs，把目标列表参数需要的数据与操作放在同一个对象中。
class GoalListArgs(BaseModel):
    # 目标列表条件；状态使用固定枚举，标识只接受合法数字。

    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 状态（status），类型为 GoalStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: GoalStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="目标状态：未开始、进行中或已完成" 的结果。
        default=None, description="目标状态：未开始、进行中或已完成"
    )
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="目标名称关键字")
    # 声明 已归档（archived），类型为 bool；其内容为执行“调用 Field 完成字段处理”得到的结果。
    archived: bool = Field(default=False, description="是否查询已归档目标，默认 false")
    # 声明 timing，类型为 Literal['ALL', 'CURRENT', 'FUTURE'] | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    timing: Literal["ALL", "CURRENT", "FUTURE"] | None = Field(
        # 传入默认值（default），值为空值，表示当前还没有对应内容。
        default=None,
        # 传入说明（description），值为固定文字 '目标时间范围：CURRENT 当前进行中、FUTURE 未来、ALL 全部；不传按工作区默认'。
        description="目标时间范围：CURRENT 当前进行中、FUTURE 未来、ALL 全部；不传按工作区默认",
    )
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 GoalDetailArgs，把目标详情参数需要的数据与操作放在同一个对象中。
class GoalDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 目标编号（goalId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int = Field(description="目标 ID，来自目标列表中的 id")


# 函数 _list_item：调用 _list_item 完成列表单条数据处理；接收 当前记录（row）。
def _list_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None or (not isinstance(row.get('name'), str))，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: row[key] for key in _GOAL_SUMMARY_FIELDS if row.get(key) is not None}


# 函数 _detail：调用 _detail 完成详情处理；接收 当前记录（row）。
def _detail(row: dict[str, Any]) -> dict[str, Any]:
    # 把执行“调用 shared.overview 完成overview处理”得到的结果保存到 阶段数据列表（stage_items）。
    stage_items = shared.overview(row.get("stages"))
    # 声明 详情（detail），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    detail: dict[str, Any] = {
        # 填写编号（id）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "id": row.get("id"),
        # 填写名称（name）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "name": row.get("name"),
        # 填写说明（description）字段，值为调用 shared.text 完成文字处理。
        "description": shared.text(row.get("description"), 300),
        # 填写完成情况判断标准（completionCriteria）字段，值为调用 shared.text 完成文字处理。
        "completionCriteria": shared.text(row.get("completionCriteria"), 300),
        # 填写开始日期（startDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "startDate": row.get("startDate"),
        # 填写预计结束日期（expectedEndDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "expectedEndDate": row.get("expectedEndDate"),
        # 填写状态（status）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "status": row.get("status"),
        # 填写已归档At（archivedAt）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "archivedAt": row.get("archivedAt"),
        # 填写进度（progress）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "progress": row.get("progress"),
        # 填写完成情况ConditionMet（completionConditionMet）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "completionConditionMet": row.get("completionConditionMet"),
        # 填写完成情况轮（completionRound）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "completionRound": row.get("completionRound"),
        # 填写版本号（version）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "version": row.get("version"),
        # 填写阶段数量（stageCount）字段，值为集合或文字的长度。
        "stageCount": len(stage_items),
        # 填写阶段列表（stages）字段，值为阶段数据列表（stage_items）。
        "stages": stage_items,
    }
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: value for key, value in detail.items() if value is not None}


# 异步函数 run_list_goals：调用 run_list_goals 完成方案运行列表目标列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_goals(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: GoalListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: GoalListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int | bool]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    # 检查条件 args.status 是否成立；成立时执行缩进的分支。
    if args.status:
        # 把参数（args）的状态（status）属性保存到 参数集合状态（params['status']）。
        params["status"] = args.status
    # 检查条件 args.keyword 是否成立；成立时执行缩进的分支。
    if args.keyword:
        # 把执行“去掉文字两端的空白”得到的结果保存到 keyword。
        keyword = args.keyword.strip()
        # 检查条件 keyword 是否成立；成立时执行缩进的分支。
        if keyword:
            # 把keyword保存到 参数集合keyword（params['keyword']）。
            params["keyword"] = keyword
    # 检查条件 args.archived 是否成立；成立时执行缩进的分支。
    if args.archived:
        # 把真，表示条件成立或开关打开保存到 参数集合已归档（params['archived']）。
        params["archived"] = True
    # 检查条件 args.timing 是否成立；成立时执行缩进的分支。
    if args.timing:
        # 把参数（args）的timing属性保存到 参数集合timing（params['timing']）。
        params["timing"] = args.timing
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "goals", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "目标查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("目标返回结构不正确", "目标返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _list_item(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"目标 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"目标 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_get_goal：调用 run_get_goal 完成方案运行读取目标处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_goal(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: GoalDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: GoalDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, f"goals/{args.goalId}")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "目标详情查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查not isinstance(data, dict) or data.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(data, dict) or data.get("id") is None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("目标详情返回结构不正确", "目标详情返回异常")
    # 把执行“调用 _detail 完成详情处理”得到的结果保存到 目标（goal）。
    goal = _detail(data)
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"goal": goal}, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=f"目标「{goal.get('name', '')}」详情",
    )


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 目标列表业务工具（GOAL_LIST_TOOL）。
GOAL_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_goals'。
    name="list_goals",
    # 传入显示名称（label），值为固定文字 '目标列表'。
    label="目标列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的成长目标列表，可按目标状态、名称关键字、是否归档和时间范围筛选，'，完成上面尚未结束的表达式。
        "查询用户本人的成长目标列表，可按目标状态、名称关键字、是否归档和时间范围筛选，"
        "最多返回 20 条并给出总条数。返回目标 ID、名称、状态、起止日期和进度。"
        "用户询问自己有哪些目标、目标进度或查找目标时使用；"
        "需要阶段概览等完整信息时再用 get_goal 查询详情。"
    ),
    # 传入权限（permission），值为固定文字 'goal:goal:list'。
    permission="goal:goal:list",
    # 传入参数模型（args_model），值为目标列表参数（GoalListArgs）。
    args_model=GoalListArgs,
    # 传入handler，值为方案运行列表目标列表（run_list_goals）。
    handler=run_list_goals,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 目标详情业务工具（GOAL_DETAIL_TOOL）。
GOAL_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_goal'。
    name="get_goal",
    # 传入显示名称（label），值为固定文字 '目标详情'。
    label="目标详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按目标 ID 查询用户本人的目标详情：名称、描述、完成条件、状态、起止日期、'，完成上面尚未结束的表达式。
        "按目标 ID 查询用户本人的目标详情：名称、描述、完成条件、状态、起止日期、"
        "进度及阶段概览。目标 ID 必须来自本人目标列表，不能猜测或使用他人 ID。"
        "用户询问某个目标的完整信息或目标下有哪些阶段时使用。"
    ),
    # 传入权限（permission），值为固定文字 'goal:goal:read'。
    permission="goal:goal:read",
    # 传入参数模型（args_model），值为目标详情参数（GoalDetailArgs）。
    args_model=GoalDetailArgs,
    # 传入handler，值为方案运行读取目标（run_get_goal）。
    handler=run_get_goal,
)
