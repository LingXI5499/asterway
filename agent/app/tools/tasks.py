# 封装任务列表查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 任务只读工具：任务列表与任务详情；支持状态、优先级、关键字、目标和阶段筛选。

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

# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 NOT_STARTED（NOT开始时）、固定状态或编码 IN_PROGRESS（IN进度）、固定状态或编码 PAUSED（已暂停）、固定状态或编码 COMPLETED（已完成） 的集合取出的内容保存到 任务状态（TaskStatus）。
TaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 LOW（LOW）、固定状态或编码 MEDIUM（MEDIUM）、固定状态或编码 HIGH（HIGH）、固定状态或编码 URGENT（URGENT） 的集合取出的内容保存到 Priority。
Priority = Literal["LOW", "MEDIUM", "HIGH", "URGENT"]

# 把依次包含 固定文字 'id'、固定文字 'name'、固定文字 'status'、固定文字 'priority'、固定文字 'dueDate'等内容 的集合保存到 BRIEFFIELDS（_BRIEF_FIELDS）。
_BRIEF_FIELDS = (
    # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
    "id",
    # 继续传入或组合固定文字 'name'，完成上面尚未结束的表达式。
    "name",
    # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
    "status",
    # 继续传入或组合固定文字 'priority'，完成上面尚未结束的表达式。
    "priority",
    # 继续传入或组合固定文字 'dueDate'，完成上面尚未结束的表达式。
    "dueDate",
    # 继续传入或组合固定文字 'goalId'，完成上面尚未结束的表达式。
    "goalId",
    # 继续传入或组合固定文字 'goalName'，完成上面尚未结束的表达式。
    "goalName",
    # 继续传入或组合固定文字 'stageId'，完成上面尚未结束的表达式。
    "stageId",
    # 继续传入或组合固定文字 'stageName'，完成上面尚未结束的表达式。
    "stageName",
    # 继续传入或组合固定文字 'assignmentStatus'，完成上面尚未结束的表达式。
    "assignmentStatus",
    # 继续传入或组合固定文字 'estimatedMinutes'，完成上面尚未结束的表达式。
    "estimatedMinutes",
)

# 把依次包含 固定文字 'id'、固定文字 'name'、固定文字 'priority'、固定文字 'weight'、固定文字 'estimatedMinutes'等内容 的集合保存到 详情FIELDS（_DETAIL_FIELDS）。
_DETAIL_FIELDS = (
    # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
    "id",
    # 继续传入或组合固定文字 'name'，完成上面尚未结束的表达式。
    "name",
    # 继续传入或组合固定文字 'priority'，完成上面尚未结束的表达式。
    "priority",
    # 继续传入或组合固定文字 'weight'，完成上面尚未结束的表达式。
    "weight",
    # 继续传入或组合固定文字 'estimatedMinutes'，完成上面尚未结束的表达式。
    "estimatedMinutes",
    # 继续传入或组合固定文字 'startDate'，完成上面尚未结束的表达式。
    "startDate",
    # 继续传入或组合固定文字 'dueDate'，完成上面尚未结束的表达式。
    "dueDate",
    # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
    "status",
    # 继续传入或组合固定文字 'completedAt'，完成上面尚未结束的表达式。
    "completedAt",
    # 继续传入或组合固定文字 'version'，完成上面尚未结束的表达式。
    "version",
    # 继续传入或组合固定文字 'goalId'，完成上面尚未结束的表达式。
    "goalId",
    # 继续传入或组合固定文字 'goalName'，完成上面尚未结束的表达式。
    "goalName",
    # 继续传入或组合固定文字 'stageId'，完成上面尚未结束的表达式。
    "stageId",
    # 继续传入或组合固定文字 'stageName'，完成上面尚未结束的表达式。
    "stageName",
    # 继续传入或组合固定文字 'assignmentStatus'，完成上面尚未结束的表达式。
    "assignmentStatus",
    # 继续传入或组合固定文字 'weeklyPlanId'，完成上面尚未结束的表达式。
    "weeklyPlanId",
    # 继续传入或组合固定文字 'dailyPlanId'，完成上面尚未结束的表达式。
    "dailyPlanId",
    # 继续传入或组合固定文字 'goalArchivedAt'，完成上面尚未结束的表达式。
    "goalArchivedAt",
)


# 定义 TaskListArgs，把任务列表参数需要的数据与操作放在同一个对象中。
class TaskListArgs(BaseModel):
    # 任务查询参数；状态和优先级使用系统固定枚举，不接受任意文本。

    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 状态（status），类型为 TaskStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: TaskStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="任务状态：未开始、进行中、暂停或已完成" 的结果。
        default=None, description="任务状态：未开始、进行中、暂停或已完成"
    )
    # 声明 priority，类型为 Priority | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    priority: Priority | None = Field(
        # 传入默认值（default），值为表达式 None, description="优先级：低、中、高或紧急" 的结果。
        default=None, description="优先级：低、中、高或紧急"
    )
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(
        # 传入默认值（default），值为表达式 None, max_length=50, description="任务标题关键字，最多 50 字" 的结果。
        default=None, max_length=50, description="任务标题关键字，最多 50 字"
    )
    # 声明 目标编号（goalId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int | None = Field(
        # 传入默认值（default），值为表达式 None, description="只看某个目标下的任务；ID 必须来自本人目标列表" 的结果。
        default=None, description="只看某个目标下的任务；ID 必须来自本人目标列表"
    )
    # 声明 阶段编号（stageId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int | None = Field(
        # 传入默认值（default），值为表达式 None, description="只看某个阶段下的任务；ID 必须来自本人阶段列表" 的结果。
        default=None, description="只看某个阶段下的任务；ID 必须来自本人阶段列表"
    )
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 TaskDetailArgs，把任务详情参数需要的数据与操作放在同一个对象中。
class TaskDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 任务编号（taskId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskId: int = Field(description="任务 ID，来自任务列表中的 id")


# 函数 _brief_task：调用 _brief_task 完成brief任务处理；接收 当前记录（row）。
def _brief_task(row: Any) -> dict[str, Any] | None:
    # 检查条件 not isinstance(row, dict) or not isinstance(row.get('name'), str) 是否成立；成立时执行缩进的分支。
    if not isinstance(row, dict) or not isinstance(row.get("name"), str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: row[key] for key in _BRIEF_FIELDS if row.get(key) is not None}


# 函数 _detail_task：调用 _detail_task 完成详情任务处理；接收 当前记录（row）。
def _detail_task(row: dict[str, Any]) -> dict[str, Any]:
    # 声明 详情（detail），类型为 dict[str, Any]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
    detail: dict[str, Any] = {key: row[key] for key in _DETAIL_FIELDS if row.get(key) is not None}
    # 把执行“调用 shared.text 完成文字处理”得到的结果保存到 详情说明（detail['description']）。
    detail["description"] = shared.text(row.get("description"), 300)
    # 把执行“调用 shared.text 完成文字处理”得到的结果保存到 详情完成情况判断标准（detail['completionCriteria']）。
    detail["completionCriteria"] = shared.text(row.get("completionCriteria"), 300)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: value for key, value in detail.items() if value is not None}


# 异步函数 run_list_tasks：调用 run_list_tasks 完成方案运行列表任务列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_tasks(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写参数（args）字段，值为任务列表参数（TaskListArgs）。
    args: TaskListArgs,
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    # 检查条件 args.status 是否成立；成立时执行缩进的分支。
    if args.status:
        # 把参数（args）的状态（status）属性保存到 参数集合状态（params['status']）。
        params["status"] = args.status
    # 检查条件 args.priority 是否成立；成立时执行缩进的分支。
    if args.priority:
        # 把参数（args）的priority属性保存到 参数集合priority（params['priority']）。
        params["priority"] = args.priority
    # 检查条件 args.goalId is not None 是否成立；成立时执行缩进的分支。
    if args.goalId is not None:
        # 把参数（args）的目标编号（goalId）属性保存到 参数集合目标编号（params['goalId']）。
        params["goalId"] = args.goalId
    # 检查条件 args.stageId is not None 是否成立；成立时执行缩进的分支。
    if args.stageId is not None:
        # 把参数（args）的阶段编号（stageId）属性保存到 参数集合阶段编号（params['stageId']）。
        params["stageId"] = args.stageId
    # 把执行“去掉文字两端的空白”得到的结果保存到 keyword。
    keyword = (args.keyword or "").strip()
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把keyword保存到 参数集合keyword（params['keyword']）。
        params["keyword"] = keyword
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "tasks", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "任务查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("任务返回结构不正确", "任务返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [brief for row in data["items"] if (brief := _brief_task(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"任务 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_get_task：调用 run_get_task 完成方案运行读取任务处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_task(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: TaskDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: TaskDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, f"tasks/{args.taskId}")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "任务详情查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查not isinstance(data, dict) or data.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(data, dict) or data.get("id") is None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("任务详情返回结构不正确", "任务详情返回异常")
    # 把执行“调用 _detail_task 完成详情任务处理”得到的结果保存到 任务（task）。
    task = _detail_task(data)
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"task": task}, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=f"任务「{task.get('name', '')}」详情",
    )


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 任务列表业务工具（TASK_LIST_TOOL）。
TASK_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_tasks'。
    name="list_tasks",
    # 传入显示名称（label），值为固定文字 '任务列表'。
    label="任务列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的任务列表，可按任务状态、优先级、标题关键字、所属目标或阶段筛选，'，完成上面尚未结束的表达式。
        "查询用户本人的任务列表，可按任务状态、优先级、标题关键字、所属目标或阶段筛选，"
        "最多返回 20 条并给出总条数。返回任务 ID、名称、状态、优先级、截止日期、"
        "所属目标/阶段、安排位置和预计时长。用户查询任务列表、统计任务数量或按条件筛选任务时使用；"
        "需要任务完整信息时再用 get_task 查询详情。"
    ),
    # 传入权限（permission），值为固定文字 'task:task:list'。
    permission="task:task:list",
    # 传入参数模型（args_model），值为任务列表参数（TaskListArgs）。
    args_model=TaskListArgs,
    # 传入handler，值为方案运行列表任务列表（run_list_tasks）。
    handler=run_list_tasks,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 任务详情业务工具（TASK_DETAIL_TOOL）。
TASK_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_task'。
    name="get_task",
    # 传入显示名称（label），值为固定文字 '任务详情'。
    label="任务详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按任务 ID 查询用户本人的任务详情：名称、描述、完成条件、优先级、权重、'，完成上面尚未结束的表达式。
        "按任务 ID 查询用户本人的任务详情：名称、描述、完成条件、优先级、权重、"
        "预计时长、起止日期、状态、完成时间、所属目标/阶段和安排位置。"
        "任务 ID 必须来自本人任务列表。用户询问某个任务的完整信息或完成情况时使用。"
    ),
    # 传入权限（permission），值为固定文字 'task:task:read'。
    permission="task:task:read",
    # 传入参数模型（args_model），值为任务详情参数（TaskDetailArgs）。
    args_model=TaskDetailArgs,
    # 传入handler，值为方案运行读取任务（run_get_task）。
    handler=run_get_task,
)
