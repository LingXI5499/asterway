# 封装阶段列表查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 阶段只读工具：按目标查询阶段列表与阶段详情（含任务概览）。

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

# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 NOT_STARTED（NOT开始时）、固定状态或编码 IN_PROGRESS（IN进度）、固定状态或编码 COMPLETED（已完成） 的集合取出的内容保存到 阶段状态（StageStatus）。
StageStatus = Literal["NOT_STARTED", "IN_PROGRESS", "COMPLETED"]


# 定义 StageListArgs，把阶段列表参数需要的数据与操作放在同一个对象中。
class StageListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 目标编号（goalId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int = Field(description="目标 ID，来自本人目标列表中的 id")
    # 声明 状态（status），类型为 StageStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: StageStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="阶段状态：未开始、进行中或已完成" 的结果。
        default=None, description="阶段状态：未开始、进行中或已完成"
    )
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="阶段名称关键字")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 StageDetailArgs，把阶段详情参数需要的数据与操作放在同一个对象中。
class StageDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 阶段编号（stageId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int = Field(description="阶段 ID，来自阶段列表中的 id")


# 函数 _list_item：调用 _list_item 完成列表单条数据处理；接收 当前记录（row）。
def _list_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None or (not isinstance(row.get('name'), str))，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 声明 单条数据（item），类型为 dict[str, Any]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
    item: dict[str, Any] = {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ("id", "goalId", "name", "status", "startDate", "endDate", "sortOrder", "progress") 的结果，完成上面尚未结束的表达式。
        for key in ("id", "goalId", "name", "status", "startDate", "endDate", "sortOrder", "progress")
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    }
    # 把单条数据（item）交还给调用方，本函数到此结束。
    return item


# 函数 _detail：调用 _detail 完成详情处理；接收 当前记录（row）。
def _detail(row: dict[str, Any]) -> dict[str, Any]:
    # 把执行“调用 shared.overview 完成overview处理”得到的结果保存到 任务数据列表（task_items）。
    task_items = shared.overview(row.get("tasks"))
    # 声明 详情（detail），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    detail: dict[str, Any] = {
        # 填写编号（id）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "id": row.get("id"),
        # 填写目标编号（goalId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "goalId": row.get("goalId"),
        # 填写名称（name）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "name": row.get("name"),
        # 填写说明（description）字段，值为调用 shared.text 完成文字处理。
        "description": shared.text(row.get("description"), 300),
        # 填写完成情况判断标准（completionCriteria）字段，值为调用 shared.text 完成文字处理。
        "completionCriteria": shared.text(row.get("completionCriteria"), 300),
        # 填写开始日期（startDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "startDate": row.get("startDate"),
        # 填写结束日期（endDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "endDate": row.get("endDate"),
        # 填写排序顺序（sortOrder）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "sortOrder": row.get("sortOrder"),
        # 填写状态（status）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "status": row.get("status"),
        # 填写进度（progress）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "progress": row.get("progress"),
        # 填写版本号（version）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "version": row.get("version"),
        # 填写任务数量（taskCount）字段，值为集合或文字的长度。
        "taskCount": len(task_items),
        # 填写任务列表（tasks）字段，值为任务数据列表（task_items）。
        "tasks": task_items,
    }
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: value for key, value in detail.items() if value is not None}


# 异步函数 run_list_stages：调用 run_list_stages 完成方案运行列表阶段列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_stages(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StageListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StageListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
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
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, f"goals/{args.goalId}/stages", params，完成上面尚未结束的表达式。
        context, client, f"goals/{args.goalId}/stages", params
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "阶段查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("阶段返回结构不正确", "阶段返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _list_item(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"阶段 {payload['returned']} 个（共 {payload['total']} 个）" 的结果。
        summary=f"阶段 {payload['returned']} 个（共 {payload['total']} 个）",
    )


# 异步函数 run_get_stage：调用 run_get_stage 完成方案运行读取阶段处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_stage(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StageDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StageDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, f"stages/{args.stageId}")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "阶段详情查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查not isinstance(data, dict) or data.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(data, dict) or data.get("id") is None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("阶段详情返回结构不正确", "阶段详情返回异常")
    # 把执行“调用 _detail 完成详情处理”得到的结果保存到 阶段（stage）。
    stage = _detail(data)
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"stage": stage}, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=f"阶段「{stage.get('name', '')}」详情",
    )


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 阶段列表业务工具（STAGE_LIST_TOOL）。
STAGE_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_stages'。
    name="list_stages",
    # 传入显示名称（label），值为固定文字 '阶段列表'。
    label="阶段列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人某个目标下的阶段列表，可按阶段状态和名称关键字筛选，最多返回 20 条'，完成上面尚未结束的表达式。
        "查询用户本人某个目标下的阶段列表，可按阶段状态和名称关键字筛选，最多返回 20 条"
        "并给出总条数。返回阶段 ID、名称、状态、起止日期和进度；列表不含任务数量。"
        "目标 ID 必须来自本人目标列表。用户询问某个目标分几个阶段、阶段进度时使用；"
        "需要阶段下的任务或完整信息时，再用 get_stage 查询阶段详情。"
    ),
    # 传入权限（permission），值为固定文字 'stage:stage:list'。
    permission="stage:stage:list",
    # 传入参数模型（args_model），值为阶段列表参数（StageListArgs）。
    args_model=StageListArgs,
    # 传入handler，值为方案运行列表阶段列表（run_list_stages）。
    handler=run_list_stages,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 阶段详情业务工具（STAGE_DETAIL_TOOL）。
STAGE_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_stage'。
    name="get_stage",
    # 传入显示名称（label），值为固定文字 '阶段详情'。
    label="阶段详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按阶段 ID 查询用户本人的阶段详情：所属目标、名称、描述、完成条件、起止日期、'，完成上面尚未结束的表达式。
        "按阶段 ID 查询用户本人的阶段详情：所属目标、名称、描述、完成条件、起止日期、"
        "状态、进度及任务概览。阶段 ID 必须来自本人阶段列表。"
        "用户询问某个阶段的完整信息或该阶段有哪些任务时使用。"
    ),
    # 传入权限（permission），值为固定文字 'stage:stage:read'。
    permission="stage:stage:read",
    # 传入参数模型（args_model），值为阶段详情参数（StageDetailArgs）。
    args_model=StageDetailArgs,
    # 传入handler，值为方案运行读取阶段（run_get_stage）。
    handler=run_get_stage,
)
