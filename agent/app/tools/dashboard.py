# 封装dashboard查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 今日看板工具：读取用户本人的今日任务、习惯、执行与当前执行摘要。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 typing 引入 Any。
from typing import Any

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict。
from pydantic import BaseModel, ConfigDict

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 Java后端接口错误（JavaApiError）。
from app.core.errors import JavaApiError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）、Java后端接口结果（JavaApiResult）。
from app.integrations.java_client import JavaApiClient, JavaApiResult

# 从 app.tools.base 引入 业务工具Outcome（ToolOutcome）、业务工具Spec（ToolSpec）。
from app.tools.base import ToolOutcome, ToolSpec

# 把依次包含 固定文字 'todayTasks'、固定文字 'todayCurrentCompletedTasks'、固定文字 'weekUndatedTasks'、固定文字 'actualSeconds'、固定文字 'habitDone'等内容 的集合保存到 METRIC键列表（_METRIC_KEYS）。
_METRIC_KEYS = (
    # 继续传入或组合固定文字 'todayTasks'，完成上面尚未结束的表达式。
    "todayTasks",
    # 继续传入或组合固定文字 'todayCurrentCompletedTasks'，完成上面尚未结束的表达式。
    "todayCurrentCompletedTasks",
    # 继续传入或组合固定文字 'weekUndatedTasks'，完成上面尚未结束的表达式。
    "weekUndatedTasks",
    # 继续传入或组合固定文字 'actualSeconds'，完成上面尚未结束的表达式。
    "actualSeconds",
    # 继续传入或组合固定文字 'habitDone'，完成上面尚未结束的表达式。
    "habitDone",
    # 继续传入或组合固定文字 'habitDue'，完成上面尚未结束的表达式。
    "habitDue",
    # 继续传入或组合固定文字 'habitCompletionRate'，完成上面尚未结束的表达式。
    "habitCompletionRate",
    # 继续传入或组合固定文字 'unreadMessages'，完成上面尚未结束的表达式。
    "unreadMessages",
)


# 定义 TodayOverviewArgs，把今天Overview参数需要的数据与操作放在同一个对象中。
class TodayOverviewArgs(BaseModel):
    # 看板工具不需要参数；用户身份由服务端上下文确定。

    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)


# 异步函数 _get：调用 _get 完成读取处理；接收 客户端（client）、上下文（context）、路径（path）、参数集合（params）。
async def _get(
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写路径（path）字段，值为str。
    path: str,
    # 填写参数集合（params）字段，值为表达式 dict[str, str | int | bool] | None = None 的结果。
    params: dict[str, str | int | bool] | None = None,
# 继续传入或组合表达式 ) -> JavaApiResult: 的结果，完成上面尚未结束的表达式。
) -> JavaApiResult:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await client.get(
        # 继续传入或组合路径（path），完成上面尚未结束的表达式。
        path,
        # 传入授权（authorization），值为表达式 context.authorization 的结果。
        authorization=context.authorization,
        # 传入追踪标识编号（trace_id），值为表达式 context.trace_id 的结果。
        trace_id=context.trace_id,
        # 传入参数集合（params），值为参数集合（params）。
        params=params,
    )


# 函数 _brief_task：调用 _brief_task 完成brief任务处理；接收 当前记录（row）。
def _brief_task(row: Any) -> dict[str, Any] | None:
    # 检查条件 not isinstance(row, dict) or not isinstance(row.get('name'), str) 是否成立；成立时执行缩进的分支。
    if not isinstance(row, dict) or not isinstance(row.get("name"), str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 声明 结果（result），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典，这里先创建空字典。
    result: dict[str, Any] = {}
    # 从依次包含 固定文字 'name'、固定文字 'status'、固定文字 'priority'、固定文字 'dueDate'、固定文字 'section' 的集合依次取出键（key），每项执行一次下面的处理。
    for key in ("name", "status", "priority", "dueDate", "section"):
        # 检查条件 row.get(key) is not None 是否成立；成立时执行缩进的分支。
        if row.get(key) is not None:
            # 把从当前记录（row）中按键（key）取出的内容保存到 结果键（result[key]）。
            result[key] = row[key]
    # 把结果（result）交还给调用方，本函数到此结束。
    return result


# 异步函数 _section：调用 _section 完成section处理；接收 客户端（client）、上下文（context）、section、每页条数（size）。
async def _section(
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写section字段，值为str。
    section: str,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[dict[str, Any]], str | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[dict[str, Any]], str | None]:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 声明 结果（result），类型为 JavaApiResult；其内容为等待执行“调用 _get 完成读取处理”得到的结果完成后的结果。
        result: JavaApiResult = await _get(
            # 继续传入或组合客户端（client），完成上面尚未结束的表达式。
            client,
            # 继续传入或组合上下文（context），完成上面尚未结束的表达式。
            context,
            # 继续传入或组合固定文字 'dashboard/tasks'，完成上面尚未结束的表达式。
            "dashboard/tasks",
            # 继续传入或组合表达式 {"section": section, "page": 1, "size": size} 的结果，完成上面尚未结束的表达式。
            {"section": section, "page": 1, "size": size},
        )
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
    except JavaApiError as error:
        # 检查条件 error.status_code == 401 是否成立；成立时执行缩进的分支。
        if error.status_code == 401:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 把依次包含 空列表或集合、错误（error）的消息（message）属性 的集合交还给调用方，本函数到此结束。
        return [], error.message
    # 把结果（result）的数据（data）属性保存到 数据（data）。
    data = result.data
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把依次包含 空列表或集合、固定文字 '看板任务返回结构不正确' 的集合交还给调用方，本函数到此结束。
        return [], "看板任务返回结构不正确"
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 任务列表（tasks）。
    tasks = [brief for row in data["items"] if (brief := _brief_task(row)) is not None]
    # 把依次包含 任务列表（tasks）、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
    return tasks, None


# 异步函数 run_today_overview：调用 run_today_overview 完成方案运行今天overview处理；接收 上下文（context）、客户端（client）、参数（_args）。
async def run_today_overview(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写参数（_args）字段，值为今天Overview参数（TodayOverviewArgs）。
    _args: TodayOverviewArgs,
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 声明 overview，类型为 JavaApiResult；其内容为等待执行“调用 _get 完成读取处理”得到的结果完成后的结果。
        overview: JavaApiResult = await _get(client, context, "dashboard/overview")
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
    except JavaApiError as error:
        # 检查条件 error.status_code == 401 是否成立；成立时执行缩进的分支。
        if error.status_code == 401:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
        return ToolOutcome(
            # 传入内容（content），值为把对象编码为 JSON 文字。
            content=json.dumps({"error": error.message}, ensure_ascii=False),
            # 传入汇总（summary），值为固定文字 '今日看板查询失败'。
            summary="今日看板查询失败",
            # 传入是否错误（is_error），值为真，表示条件成立或开关打开。
            is_error=True,
        )
    # 把overview的数据（data）属性保存到 数据（data）。
    data = overview.data
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
        return ToolOutcome(
            # 传入内容（content），值为把对象编码为 JSON 文字。
            content=json.dumps({"error": "今日看板返回结构不正确"}, ensure_ascii=False),
            # 传入汇总（summary），值为固定文字 '今日看板返回异常'。
            summary="今日看板返回异常",
            # 传入是否错误（is_error），值为真，表示条件成立或开关打开。
            is_error=True,
        )
    # 把条件 isinstance(data.get('metrics'), dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用按字段名保存多个值的字典，这里先创建空字典保存到 metrics。
    metrics = data.get("metrics") if isinstance(data.get("metrics"), dict) else {}
    # 把条件 isinstance(data.get('currentExecution'), dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 当前（current）。
    current = (
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        data.get("currentExecution") if isinstance(data.get("currentExecution"), dict) else None
    )
    # 把等待执行“调用 _section 完成section处理”得到的结果完成后的结果保存到 今天任务列表今天错误（(today_tasks, today_error)）。
    today_tasks, today_error = await _section(client, context, "TODAY", 5)
    # 把等待执行“调用 _section 完成section处理”得到的结果完成后的结果保存到 周任务列表周错误（(week_tasks, week_error)）。
    week_tasks, week_error = await _section(client, context, "WEEK_UNDATED", 5)
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {
        # 填写日期（date）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "date": data.get("today"),
        # 填写用户使用的时区（timeZone）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "timeZone": data.get("timeZone"),
        # 填写metrics字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "metrics": {key: metrics.get(key) for key in _METRIC_KEYS},
        # 填写当前执行计时（currentExecution）字段，值为表达式 ( 的结果。
        "currentExecution": (
            {
                # 填写任务名称（taskName）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                "taskName": current.get("taskName"),
                # 填写实际秒数（actualSeconds）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                "actualSeconds": current.get("actualSeconds"),
            }
            # 继续传入或组合表达式 if current 的结果，完成上面尚未结束的表达式。
            if current
            # 继续传入或组合表达式 else None 的结果，完成上面尚未结束的表达式。
            else None
        ),
        # 填写今天任务列表（todayTasks）字段，值为今天任务列表（today_tasks）。
        "todayTasks": today_tasks,
        # 填写周Undated任务列表（weekUndatedTasks）字段，值为周任务列表（week_tasks）。
        "weekUndatedTasks": week_tasks,
    }
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 错误处理器（errors）。
    errors = [message for message in (today_error, week_error) if message]
    # 检查条件 errors 是否成立；成立时执行缩进的分支。
    if errors:
        # 把错误处理器（errors）保存到 提交数据错误处理器（payload['errors']）。
        payload["errors"] = errors
    # 把在文字占位处填入变量后组成的字符串保存到 汇总（summary）。
    summary = f"今日任务 {metrics.get('todayTasks', 0)} 项"
    # 检查条件 metrics.get('habitDue') 是否成立；成立时执行缩进的分支。
    if metrics.get("habitDue"):
        # 把 summary 加上 在文字占位处填入变量后组成的字符串，再保存回原变量。
        summary += f"，习惯 {metrics.get('habitDone', 0)}/{metrics.get('habitDue')}"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为汇总（summary）。
        summary=summary,
    )


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 今天OVERVIEW业务工具（TODAY_OVERVIEW_TOOL）。
TODAY_OVERVIEW_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_today_overview'。
    name="get_today_overview",
    # 传入显示名称（label），值为固定文字 '今日看板'。
    label="今日看板",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的今日看板：今日任务数、已完成数、本周未定日任务数、'，完成上面尚未结束的表达式。
        "查询用户本人的今日看板：今日任务数、已完成数、本周未定日任务数、"
        "实际执行秒数、习惯完成情况、当前正在执行的任务，以及最多各 5 条"
        "今日任务和本周未定日任务。用户询问今天该做什么、今日进度、"
        "当前在执行什么时使用。"
    ),
    # 传入权限（permission），值为固定文字 'dashboard:board:read'。
    permission="dashboard:board:read",
    # 传入参数模型（args_model），值为今天Overview参数（TodayOverviewArgs）。
    args_model=TodayOverviewArgs,
    # 传入handler，值为方案运行今天overview（run_today_overview）。
    handler=run_today_overview,
)
