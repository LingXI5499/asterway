# 封装执行计时查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 执行只读工具：当前执行、执行历史、时间段与耗时汇总。
# 
# 只读取 Java 执行事实；不开始、暂停、继续、结束或补写任何执行记录。
# 今天/本周与日期区间使用 Java 账号有效时区解析。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 re，供下面调用其功能。
import re

# 从 datetime 引入 日期（date）、两个时刻之间的时间长度（timedelta）。
from datetime import date, timedelta

# 从 typing 引入 Any、只能从列出的固定值中选择的类型（Literal）。
from typing import Any, Literal

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、模型validator（model_validator）。
from pydantic import BaseModel, ConfigDict, Field, model_validator

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.tools 引入 shared。
from app.tools import shared

# 从 app.tools.base 引入 业务工具Outcome（ToolOutcome）、业务工具Spec（ToolSpec）。
from app.tools.base import ToolOutcome, ToolSpec

# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 日期（_DATE）。
_DATE = re.compile(r"^\d{4}-\d{2}-\d{2}$")
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 RUNNING（运行中）、固定状态或编码 PAUSED（已暂停）、固定状态或编码 FINISHED（FINISHED） 的集合取出的内容保存到 方案运行状态（RunStatus）。
RunStatus = Literal["RUNNING", "PAUSED", "FINISHED"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 TODAY（今天）、固定状态或编码 YESTERDAY（YESTERDAY）、固定状态或编码 THIS_WEEK（THIS周）、固定状态或编码 LAST_WEEK（最后一次周） 的集合取出的内容保存到 RelativePeriod。
RelativePeriod = Literal["TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 ALL（全部）、固定状态或编码 TODAY（今天）、固定状态或编码 YESTERDAY（YESTERDAY）、固定状态或编码 THIS_WEEK（THIS周）、固定状态或编码 LAST_WEEK（最后一次周）等内容 的集合取出的内容保存到 列表Period（ListPeriod）。
ListPeriod = Literal["ALL", "TODAY", "YESTERDAY", "THIS_WEEK", "LAST_WEEK", "CUSTOM"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 TASK（任务）、固定状态或编码 STAGE（阶段）、固定状态或编码 GOAL（目标） 的集合取出的内容保存到 数据范围（Scope）。
Scope = Literal["TASK", "STAGE", "GOAL"]

# 把按字段名保存多个值的字典保存到 状态名称对应表（_STATUS_LABELS）。
_STATUS_LABELS = {
    # 填写运行中（RUNNING）字段，值为固定文字 '进行中'。
    "RUNNING": "进行中",
    # 填写已暂停（PAUSED）字段，值为固定文字 '已暂停'。
    "PAUSED": "已暂停",
    # 填写FINISHED字段，值为固定文字 '已结束'。
    "FINISHED": "已结束",
}
# 把按字段名保存多个值的字典保存到 原因名称对应表（_REASON_LABELS）。
_REASON_LABELS = {
    # 填写用户暂停（USER_PAUSE）字段，值为固定文字 '用户暂停'。
    "USER_PAUSE": "用户暂停",
    # 填写AUTO暂停（AUTO_PAUSE）字段，值为固定文字 '心跳超时自动暂停'。
    "AUTO_PAUSE": "心跳超时自动暂停",
    # 填写方案BOUNDARY（PLAN_BOUNDARY）字段，值为固定文字 '到计划边界自动暂停'。
    "PLAN_BOUNDARY": "到计划边界自动暂停",
    # 填写认证REVOKED（AUTH_REVOKED）字段，值为固定文字 '登录失效自动暂停'。
    "AUTH_REVOKED": "登录失效自动暂停",
    # 填写FINISH字段，值为固定文字 '用户结束'。
    "FINISH": "用户结束",
    # 填写用户FINISH（USER_FINISH）字段，值为固定文字 '用户结束'。
    "USER_FINISH": "用户结束",
    # 填写任务删除标记（TASK_DELETED）字段，值为固定文字 '任务删除后结束'。
    "TASK_DELETED": "任务删除后结束",
    # 填写目标已归档（GOAL_ARCHIVED）字段，值为固定文字 '目标归档后结束'。
    "GOAL_ARCHIVED": "目标归档后结束",
}
# 把依次包含 固定文字 'id'、固定文字 'taskId'、固定文字 'taskName'、固定文字 'status'、固定文字 'startedAt'等内容 的集合保存到 会话FIELDS（_SESSION_FIELDS）。
_SESSION_FIELDS = (
    # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
    "id",
    # 继续传入或组合固定文字 'taskId'，完成上面尚未结束的表达式。
    "taskId",
    # 继续传入或组合固定文字 'taskName'，完成上面尚未结束的表达式。
    "taskName",
    # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
    "status",
    # 继续传入或组合固定文字 'startedAt'，完成上面尚未结束的表达式。
    "startedAt",
    # 继续传入或组合固定文字 'finishedAt'，完成上面尚未结束的表达式。
    "finishedAt",
    # 继续传入或组合固定文字 'finishReason'，完成上面尚未结束的表达式。
    "finishReason",
    # 继续传入或组合固定文字 'actualSeconds'，完成上面尚未结束的表达式。
    "actualSeconds",
    # 继续传入或组合固定文字 'rangeActualSeconds'，完成上面尚未结束的表达式。
    "rangeActualSeconds",
    # 继续传入或组合固定文字 'version'，完成上面尚未结束的表达式。
    "version",
)
# 把依次包含 固定文字 'lastHeartbeatAt'、固定文字 'activeSegmentId'、固定文字 'activeSegmentStartedAt'、固定文字 'heartbeatDeadline'、固定文字 'eligibleUntil'等内容 的集合保存到 当前有效FIELDS（_ACTIVE_FIELDS）。
_ACTIVE_FIELDS = (
    # 继续传入或组合固定文字 'lastHeartbeatAt'，完成上面尚未结束的表达式。
    "lastHeartbeatAt",
    # 继续传入或组合固定文字 'activeSegmentId'，完成上面尚未结束的表达式。
    "activeSegmentId",
    # 继续传入或组合固定文字 'activeSegmentStartedAt'，完成上面尚未结束的表达式。
    "activeSegmentStartedAt",
    # 继续传入或组合固定文字 'heartbeatDeadline'，完成上面尚未结束的表达式。
    "heartbeatDeadline",
    # 继续传入或组合固定文字 'eligibleUntil'，完成上面尚未结束的表达式。
    "eligibleUntil",
    # 继续传入或组合固定文字 'canHeartbeat'，完成上面尚未结束的表达式。
    "canHeartbeat",
    # 继续传入或组合固定文字 'serverNow'，完成上面尚未结束的表达式。
    "serverNow",
)


# ---------- 参数模型 ----------


# 定义 CurrentExecutionArgs，把当前执行计时参数需要的数据与操作放在同一个对象中。
class CurrentExecutionArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)


# 定义 ExecutionListArgs，把执行计时列表参数需要的数据与操作放在同一个对象中。
class ExecutionListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 状态（status），类型为 RunStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: RunStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="执行状态：RUNNING 进行中、PAUSED 已暂停、FINISHED 已结束" 的结果。
        default=None, description="执行状态：RUNNING 进行中、PAUSED 已暂停、FINISHED 已结束"
    )
    # 声明 任务编号（taskId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskId: int | None = Field(default=None, description="只看某个任务的执行")
    # 声明 目标编号（goalId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int | None = Field(default=None, description="只看某个目标下的执行")
    # 声明 阶段编号（stageId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int | None = Field(default=None, description="只看某个阶段下的执行")
    # 声明 统计周期（period），类型为 ListPeriod；其内容为执行“调用 Field 完成字段处理”得到的结果。
    period: ListPeriod = Field(
        # 传入默认值（default），值为固定状态或编码 ALL（全部）。
        default="ALL",
        # 传入说明（description），值为固定文字 '日期范围：ALL 全部、TODAY 今天、YESTERDAY 昨天、THIS_WEEK 本周、LAST_WEEK 上周、CUSTOM 自定义'。
        description="日期范围：ALL 全部、TODAY 今天、YESTERDAY 昨天、THIS_WEEK 本周、LAST_WEEK 上周、CUSTOM 自定义",
    )
    # 声明 日期来源（dateFrom），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateFrom: str | None = Field(default=None, description="CUSTOM 时必填，开始日期 YYYY-MM-DD")
    # 声明 日期To（dateTo），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateTo: str | None = Field(default=None, description="CUSTOM 时必填，结束日期 YYYY-MM-DD")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_period：检查 统计周期（period）。
    def validate_period(self) -> "ExecutionListArgs":
        # 检查条件 self.period == 'CUSTOM' 是否成立；成立时执行缩进的分支。
        if self.period == "CUSTOM":
            # 检查self.dateFrom is None or self.dateTo is None，即对应内容是否不存在；成立时执行缩进的分支。
            if self.dateFrom is None or self.dateTo is None:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("period=CUSTOM 时必须同时提供 dateFrom 与 dateTo")
        # 检查条件 self.dateFrom is not None or self.dateTo is not None 是否成立；成立时执行缩进的分支。
        elif self.dateFrom is not None or self.dateTo is not None:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("只有 period=CUSTOM 时才能提供 dateFrom/dateTo")
        # 从依次包含 当前实例（self）的日期来源（dateFrom）属性、当前实例（self）的日期To（dateTo）属性 的集合依次取出值（value），每项执行一次下面的处理。
        for value in (self.dateFrom, self.dateTo):
            # 检查条件 value is not None and (not _DATE.fullmatch(value)) 是否成立；成立时执行缩进的分支。
            if value is not None and not _DATE.fullmatch(value):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期必须是 YYYY-MM-DD")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 ExecutionDetailArgs，把执行计时详情参数需要的数据与操作放在同一个对象中。
class ExecutionDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 执行计时编号（executionId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    executionId: int = Field(description="执行记录 ID，来自执行历史列表")


# 定义 SegmentListArgs，把连续计时段列表参数需要的数据与操作放在同一个对象中。
class SegmentListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 执行计时编号（executionId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    executionId: int = Field(description="执行记录 ID，来自执行历史列表")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 ExecutionSummaryArgs，把执行计时汇总参数需要的数据与操作放在同一个对象中。
class ExecutionSummaryArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 数据范围（scope），类型为 Scope | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    scope: Scope | None = Field(
        # 传入默认值（default），值为表达式 None, description="汇总范围：TASK 任务、STAGE 阶段、GOAL 目标；不传为本人全部执行" 的结果。
        default=None, description="汇总范围：TASK 任务、STAGE 阶段、GOAL 目标；不传为本人全部执行"
    )
    # 声明 数据范围编号（scopeId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    scopeId: int | None = Field(default=None, description="scope 对应的任务/阶段/目标 ID")
    # 声明 统计周期（period），类型为 RelativePeriod | Literal['CUSTOM']；其内容为执行“调用 Field 完成字段处理”得到的结果。
    period: RelativePeriod | Literal["CUSTOM"] = Field(
        # 传入默认值（default），值为固定文字 'TODAY", description="统计区间：今天/昨天/本周/上周/自定义'。
        default="TODAY", description="统计区间：今天/昨天/本周/上周/自定义"
    )
    # 声明 日期来源（dateFrom），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateFrom: str | None = Field(default=None, description="CUSTOM 时必填，开始日期 YYYY-MM-DD")
    # 声明 日期To（dateTo），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateTo: str | None = Field(default=None, description="CUSTOM 时必填，结束日期 YYYY-MM-DD")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_mode：检查 mode。
    def validate_mode(self) -> "ExecutionSummaryArgs":
        # 检查self.scope is None) != (self.scopeId is None，即对应内容是否不存在；成立时执行缩进的分支。
        if (self.scope is None) != (self.scopeId is None):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("scope 与 scopeId 必须同时提供或同时省略")
        # 检查条件 self.period == 'CUSTOM' 是否成立；成立时执行缩进的分支。
        if self.period == "CUSTOM":
            # 检查self.dateFrom is None or self.dateTo is None，即对应内容是否不存在；成立时执行缩进的分支。
            if self.dateFrom is None or self.dateTo is None:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("period=CUSTOM 时必须同时提供 dateFrom 与 dateTo")
        # 检查条件 self.dateFrom is not None or self.dateTo is not None 是否成立；成立时执行缩进的分支。
        elif self.dateFrom is not None or self.dateTo is not None:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("只有 period=CUSTOM 时才能提供 dateFrom/dateTo")
        # 从依次包含 当前实例（self）的日期来源（dateFrom）属性、当前实例（self）的日期To（dateTo）属性 的集合依次取出值（value），每项执行一次下面的处理。
        for value in (self.dateFrom, self.dateTo):
            # 检查条件 value is not None and (not _DATE.fullmatch(value)) 是否成立；成立时执行缩进的分支。
            if value is not None and not _DATE.fullmatch(value):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期必须是 YYYY-MM-DD")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# ---------- 公共辅助 ----------


# 函数 _parse：调用 _parse 完成解析处理；接收 值（value）。
def _parse(value: str | None) -> date | None:
    # 检查条件 not value or not _DATE.fullmatch(value) 是否成立；成立时执行缩进的分支。
    if not value or not _DATE.fullmatch(value):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 date.fromisoformat 完成fromisoformat处理”得到的结果交还给调用方，本函数到此结束。
        return date.fromisoformat(value)
    # 捕获ValueError异常，执行下面的错误处理。
    except ValueError:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None


# 函数 _monday：调用 _monday 完成monday处理；接收 值（value）。
def _monday(value: date) -> date:
    # 把按 value - timedelta(days=value.weekday()) 计算的结果交还给调用方，本函数到此结束。
    return value - timedelta(days=value.weekday())


# 异步函数 _range：解析统计区间；返回 (dateFrom/dateTo 参数, 有效时区, 错误说明)。；接收 上下文（context）、客户端（client）、统计周期（period）、日期来源（date_from）、日期to（date_to）。
async def _range(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写统计周期（period）字段，值为str。
    period: str,
    # 填写日期来源（date_from）字段，值为表达式 str | None 的结果。
    date_from: str | None,
    # 填写日期to（date_to）字段，值为表达式 str | None 的结果。
    date_to: str | None,
# 继续传入或组合表达式 ) -> tuple[dict[str, str] | None, str | None, str | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[dict[str, str] | None, str | None, str | None]:
    # 解析统计区间；返回 (dateFrom/dateTo 参数, 有效时区, 错误说明)。
    # 检查条件 period == 'CUSTOM' 是否成立；成立时执行缩进的分支。
    if period == "CUSTOM":
        # 把执行“调用 _parse 完成解析处理”得到的结果保存到 开始（start）。
        start = _parse(date_from)
        # 把执行“调用 _parse 完成解析处理”得到的结果保存到 结束（end）。
        end = _parse(date_to)
        # 检查start is None or end is None，即对应内容是否不存在；成立时执行缩进的分支。
        if start is None or end is None:
            # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '自定义区间日期格式不正确，请使用 YYYY-MM-DD' 的集合交还给调用方，本函数到此结束。
            return None, None, "自定义区间日期格式不正确，请使用 YYYY-MM-DD"
        # 检查条件 start > end 是否成立；成立时执行缩进的分支。
        if start > end:
            # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '自定义区间开始日期不能晚于结束日期' 的集合交还给调用方，本函数到此结束。
            return None, None, "自定义区间开始日期不能晚于结束日期"
        # 把依次包含 按字段名保存多个值的字典、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
        return {"dateFrom": start.isoformat(), "dateTo": end.isoformat()}, None, None
    # 检查条件 period == 'ALL' 是否成立；成立时执行缩进的分支。
    if period == "ALL":
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
        return None, None, None
    # 检查当前用户是否具有需要的权限：'dashboard:board:read' not in context.permissions；成立时执行缩进的分支。
    if "dashboard:board:read" not in context.permissions:
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '当前账户无法读取账号今天日期，请改用 CUSTOM 明确日期' 的集合交还给调用方，本函数到此结束。
        return None, None, "当前账户无法读取账号今天日期，请改用 CUSTOM 明确日期"
    # 把等待执行“调用 shared.today 完成今天处理”得到的结果完成后的结果保存到 今天值时间时区错误消息（(today_value, time_zone, error_message)）。
    today_value, time_zone, error_message = await shared.today(context, client)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、错误消息（error_message） 的集合交还给调用方，本函数到此结束。
        return None, None, error_message
    # 把条件 today_value 是否成立时使用执行“调用 _parse 完成解析处理”得到的结果，否则使用空值，表示当前还没有对应内容保存到 今天（today）。
    today = _parse(today_value) if today_value else None
    # 检查today is None，即对应内容是否不存在；成立时执行缩进的分支。
    if today is None:
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '账号今天日期格式不正确' 的集合交还给调用方，本函数到此结束。
        return None, None, "账号今天日期格式不正确"
    # 把执行“调用 _monday 完成monday处理”得到的结果保存到 monday。
    monday = _monday(today)
    # 检查条件 period == 'TODAY' 是否成立；成立时执行缩进的分支。
    if period == "TODAY":
        # 把今天（today）保存到 开始（start）、结束（end）。
        start = end = today
    # 检查条件 period == 'YESTERDAY' 是否成立；成立时执行缩进的分支。
    elif period == "YESTERDAY":
        # 把按 today - timedelta(days=1) 计算的结果保存到 开始（start）、结束（end）。
        start = end = today - timedelta(days=1)
    # 检查条件 period == 'THIS_WEEK' 是否成立；成立时执行缩进的分支。
    elif period == "THIS_WEEK":
        # 把依次包含 monday、按 monday + timedelta(days=6) 计算的结果 的集合保存到 开始结束（(start, end)）。
        start, end = monday, monday + timedelta(days=6)
    # 检查条件 period == 'LAST_WEEK' 是否成立；成立时执行缩进的分支。
    elif period == "LAST_WEEK":
        # 把依次包含 按 monday - timedelta(days=7) 计算的结果、按 monday - timedelta(days=1) 计算的结果 的集合保存到 开始结束（(start, end)）。
        start, end = monday - timedelta(days=7), monday - timedelta(days=1)
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '不支持的统计区间' 的集合交还给调用方，本函数到此结束。
        return None, None, "不支持的统计区间"
    # 把依次包含 按字段名保存多个值的字典、用户使用的时区（time_zone）、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
    return {"dateFrom": start.isoformat(), "dateTo": end.isoformat()}, time_zone, None


# 函数 _session_item：调用 _session_item 完成会话单条数据处理；接收 当前记录（row）、页面是否仍处于活动状态（active）。
def _session_item(row: Any, *, active: bool = False) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 单条数据（item）。
    item = {key: row[key] for key in _SESSION_FIELDS if row.get(key) is not None}
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 状态（status）。
    status = row.get("status")
    # 检查条件 isinstance(status, str) 是否成立；成立时执行缩进的分支。
    if isinstance(status, str):
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 单条数据状态显示名称（item['statusLabel']）。
        item["statusLabel"] = _STATUS_LABELS.get(status, status)
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 原因（reason）。
    reason = row.get("finishReason")
    # 检查条件 isinstance(reason, str) and reason 是否成立；成立时执行缩进的分支。
    if isinstance(reason, str) and reason:
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 单条数据finish原因显示名称（item['finishReasonLabel']）。
        item["finishReasonLabel"] = _REASON_LABELS.get(reason, reason)
    # 检查条件 active 是否成立；成立时执行缩进的分支。
    if active:
        # 从当前有效FIELDS（_ACTIVE_FIELDS）依次取出键（key），每项执行一次下面的处理。
        for key in _ACTIVE_FIELDS:
            # 检查条件 row.get(key) is not None 是否成立；成立时执行缩进的分支。
            if row.get(key) is not None:
                # 把从当前记录（row）中按键（key）取出的内容保存到 单条数据键（item[key]）。
                item[key] = row[key]
    # 把单条数据（item）交还给调用方，本函数到此结束。
    return item


# 函数 _segment_item：调用 _segment_item 完成连续计时段单条数据处理；接收 当前记录（row）。
def _segment_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 声明 单条数据（item），类型为 dict[str, Any]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
    item: dict[str, Any] = {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
            "id",
            # 继续传入或组合固定文字 'segmentNo'，完成上面尚未结束的表达式。
            "segmentNo",
            # 继续传入或组合固定文字 'startedAt'，完成上面尚未结束的表达式。
            "startedAt",
            # 继续传入或组合固定文字 'endedAt'，完成上面尚未结束的表达式。
            "endedAt",
            # 继续传入或组合固定文字 'effectiveEndedAt'，完成上面尚未结束的表达式。
            "effectiveEndedAt",
            # 继续传入或组合固定文字 'actualSeconds'，完成上面尚未结束的表达式。
            "actualSeconds",
            # 继续传入或组合固定文字 'endReason'，完成上面尚未结束的表达式。
            "endReason",
            # 继续传入或组合固定文字 'timeZone'，完成上面尚未结束的表达式。
            "timeZone",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    }
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 原因（reason）。
    reason = row.get("endReason")
    # 检查条件 isinstance(reason, str) and reason 是否成立；成立时执行缩进的分支。
    if isinstance(reason, str) and reason:
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 单条数据结束原因显示名称（item['endReasonLabel']）。
        item["endReasonLabel"] = _REASON_LABELS.get(reason, reason)
    # 把row.get('endedAt') is None，即对应内容是否不存在保存到 单条数据是否展开（item['open']）。
    item["open"] = row.get("endedAt") is None
    # 把单条数据（item）交还给调用方，本函数到此结束。
    return item


# 函数 _minutes：调用 _minutes 完成分钟数处理；接收 秒数（seconds）。
def _minutes(seconds: Any) -> float | None:
    # 检查条件 isinstance(seconds, int) and (not isinstance(seconds, bool)) 是否成立；成立时执行缩进的分支。
    if isinstance(seconds, int) and not isinstance(seconds, bool):
        # 把执行“调用 round 完成轮处理”得到的结果交还给调用方，本函数到此结束。
        return round(seconds / 60, 2)
    # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return None


# ---------- 工具实现 ----------


# 异步函数 run_current_execution：调用 run_current_execution 完成方案运行当前执行计时处理；接收 上下文（context）、客户端（client）、参数（_args）。
async def run_current_execution(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, _args: CurrentExecutionArgs 的结果。
    context: AgentContext, client: JavaApiClient, _args: CurrentExecutionArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "executions/current")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "当前执行查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 把执行“调用 _session_item 完成会话单条数据处理”得到的结果保存到 当前（current）。
    current = _session_item(data, active=True)
    # 检查current is None，即对应内容是否不存在；成立时执行缩进的分支。
    if current is None:
        # 把按字段名保存多个值的字典保存到 提交数据（payload）。
        payload = {"currentExecution": None, "note": "当前没有正在进行的执行（可能已暂停或尚未开始）"}
        # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
        return ToolOutcome(
            # 传入内容（content），值为把对象编码为 JSON 文字。
            content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            # 传入汇总（summary），值为固定文字 '当前没有正在执行的任务'。
            summary="当前没有正在执行的任务",
        )
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"currentExecution": current}, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=f"正在执行「{current.get('taskName', '')}」",
    )


# 异步函数 run_list_executions：调用 run_list_executions 完成方案运行列表执行记录处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_executions(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: ExecutionListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: ExecutionListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    # 检查条件 args.status 是否成立；成立时执行缩进的分支。
    if args.status:
        # 把参数（args）的状态（status）属性保存到 参数集合状态（params['status']）。
        params["status"] = args.status
    # 检查条件 args.taskId is not None 是否成立；成立时执行缩进的分支。
    if args.taskId is not None:
        # 把参数（args）的任务编号（taskId）属性保存到 参数集合任务编号（params['taskId']）。
        params["taskId"] = args.taskId
    # 检查条件 args.goalId is not None 是否成立；成立时执行缩进的分支。
    if args.goalId is not None:
        # 把参数（args）的目标编号（goalId）属性保存到 参数集合目标编号（params['goalId']）。
        params["goalId"] = args.goalId
    # 检查条件 args.stageId is not None 是否成立；成立时执行缩进的分支。
    if args.stageId is not None:
        # 把参数（args）的阶段编号（stageId）属性保存到 参数集合阶段编号（params['stageId']）。
        params["stageId"] = args.stageId
    # 把空值，表示当前还没有对应内容保存到 用户使用的时区（time_zone）。
    time_zone = None
    # 检查条件 args.period != 'ALL' 是否成立；成立时执行缩进的分支。
    if args.period != "ALL":
        # 把等待执行“调用 _range 完成区间处理”得到的结果完成后的结果保存到 日期区间时间时区错误消息（(date_range, time_zone, error_message)）。
        date_range, time_zone, error_message = await _range(
            # 继续传入或组合表达式 context, client, args.period, args.dateFrom, args.dateTo 的结果，完成上面尚未结束的表达式。
            context, client, args.period, args.dateFrom, args.dateTo
        )
        # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
        if error_message is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(error_message, "执行历史查询失败")
        # 断言条件 date_range is not None 是否成立；不满足时本项检查失败。
        assert date_range is not None
        # 调用 params.update 完成更新处理。
        params.update(date_range)
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "executions", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "执行历史查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("执行历史返回结构不正确", "执行历史返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _session_item(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把参数（args）的统计周期（period）属性保存到 提交数据period（payload['period']）。
    payload["period"] = args.period
    # 检查条件 args.period != 'ALL' 是否成立；成立时执行缩进的分支。
    if args.period != "ALL":
        # 把按字段名保存多个值的字典保存到 提交数据日期区间（payload['dateRange']）。
        payload["dateRange"] = {"dateFrom": params.get("dateFrom"), "dateTo": params.get("dateTo")}
    # 检查条件 time_zone is not None 是否成立；成立时执行缩进的分支。
    if time_zone is not None:
        # 把用户使用的时区（time_zone）保存到 提交数据时间时区（payload['timeZone']）。
        payload["timeZone"] = time_zone
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"执行记录 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"执行记录 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_get_execution：调用 run_get_execution 完成方案运行读取执行计时处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_execution(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: ExecutionDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: ExecutionDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, f"executions/{args.executionId}")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "执行详情查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('execution'), dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("execution"), dict):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("执行详情返回结构不正确", "执行详情返回异常")
    # 把执行“调用 _session_item 完成会话单条数据处理”得到的结果保存到 执行计时（execution）。
    execution = _session_item(data["execution"], active=True)
    # 把条件 isinstance(data.get('segments'), dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用按字段名保存多个值的字典，这里先创建空字典保存到 连续计时段当前页（segment_page）。
    segment_page = data.get("segments") if isinstance(data.get("segments"), dict) else {}
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 连续计时段数据列表（segment_items）。
    segment_items = [
        # 继续传入或组合单条数据（item），完成上面尚未结束的表达式。
        item
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        for row in segment_page.get("items", [])
        # 继续传入或组合条件判断的结果：if (item := _segment_item(row)) is not None，完成上面尚未结束的表达式。
        if (item := _segment_item(row)) is not None
    ]
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 总数（total）。
    total = segment_page.get("total")
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {
        # 填写执行计时（execution）字段，值为执行计时（execution）。
        "execution": execution,
        # 填写计时段列表（segments）字段，值为表达式 { 的结果。
        "segments": {
            # 填写总数（total）字段，值为表达式 total if isinstance(total, int) else len(segment_items) 的结果。
            "total": total if isinstance(total, int) else len(segment_items),
            # 填写已返回（returned）字段，值为集合或文字的长度。
            "returned": len(segment_items),
            # 填写是否存在More（hasMore）字段，值为判断是否为 instance。
            "hasMore": isinstance(total, int) and total > len(segment_items),
            # 填写当前页（page）字段，值为固定数值 1。
            "page": 1,
            # 填写数据列表（items）字段，值为连续计时段数据列表（segment_items）。
            "items": segment_items,
        },
    }
    # 检查条件 isinstance(total, int) and total > len(segment_items) 是否成立；成立时执行缩进的分支。
    if isinstance(total, int) and total > len(segment_items):
        # 把固定文字 '仅返回首屏时间段，更多时间段请用 list_execution_segments'保存到 提交数据计时段列表说明（payload['segments']['note']）。
        payload["segments"]["note"] = "仅返回首屏时间段，更多时间段请用 list_execution_segments"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为优先使用前面的有效值，没有时采用后面的备用值：f"执行记录「{(execution or {}).get('taskName', '')}」详情与时间段"。
        summary=f"执行记录「{(execution or {}).get('taskName', '')}」详情与时间段",
    )


# 异步函数 run_list_segments：调用 run_list_segments 完成方案运行列表计时段列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_segments(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: SegmentListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: SegmentListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合上下文（context），完成上面尚未结束的表达式。
        context,
        # 继续传入或组合客户端（client），完成上面尚未结束的表达式。
        client,
        # 继续传入或组合按表达式计算得到的值：f"executions/{args.executionId}/segments"，完成上面尚未结束的表达式。
        f"executions/{args.executionId}/segments",
        # 继续传入或组合表达式 {"page": 1, "size": args.limit} 的结果，完成上面尚未结束的表达式。
        {"page": 1, "size": args.limit},
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "执行时间段查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("执行时间段返回结构不正确", "执行时间段返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _segment_item(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 str 完成str处理”得到的结果保存到 提交数据执行计时编号（payload['executionId']）。
    payload["executionId"] = str(args.executionId)
    # 检查条件 any((item.get('open') for item in items)) 是否成立；成立时执行缩进的分支。
    if any(item.get("open") for item in items):
        # 把固定文字 '存在未结束时间段，实际秒数按服务端有效截止时间计算'保存到 提交数据说明（payload['note']）。
        payload["note"] = "存在未结束时间段，实际秒数按服务端有效截止时间计算"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"执行时间段 {payload['returned']} 段（共 {payload['total']} 段）" 的结果。
        summary=f"执行时间段 {payload['returned']} 段（共 {payload['total']} 段）",
    )


# 异步函数 run_execution_summary：调用 run_execution_summary 完成方案运行执行计时汇总处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_execution_summary(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: ExecutionSummaryArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: ExecutionSummaryArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“调用 _range 完成区间处理”得到的结果完成后的结果保存到 日期区间时间时区错误消息（(date_range, time_zone, error_message)）。
    date_range, time_zone, error_message = await _range(
        # 继续传入或组合表达式 context, client, args.period, args.dateFrom, args.dateTo 的结果，完成上面尚未结束的表达式。
        context, client, args.period, args.dateFrom, args.dateTo
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "执行耗时查询失败")
    # 断言条件 date_range is not None 是否成立；不满足时本项检查失败。
    assert date_range is not None
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为执行“调用 dict 完成dict处理”得到的结果。
    params: dict[str, str | int] = dict(date_range)
    # 检查条件 args.scope == 'TASK' 是否成立；成立时执行缩进的分支。
    if args.scope == "TASK":
        # 把参数（args）的数据范围编号（scopeId）属性保存到 参数集合任务编号（params['taskId']）。
        params["taskId"] = args.scopeId
    # 检查条件 args.scope == 'STAGE' 是否成立；成立时执行缩进的分支。
    elif args.scope == "STAGE":
        # 把参数（args）的数据范围编号（scopeId）属性保存到 参数集合阶段编号（params['stageId']）。
        params["stageId"] = args.scopeId
    # 检查条件 args.scope == 'GOAL' 是否成立；成立时执行缩进的分支。
    elif args.scope == "GOAL":
        # 把参数（args）的数据范围编号（scopeId）属性保存到 参数集合目标编号（params['goalId']）。
        params["goalId"] = args.scopeId
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "executions/summary", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "执行耗时查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("执行汇总返回结构不正确", "执行汇总返回异常")
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {
        # 填写数据范围（scope）字段，值为优先使用前面的有效值，没有时采用后面的备用值：args.scope or "ALL"。
        "scope": args.scope or "ALL",
        # 填写数据范围编号（scopeId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "scopeId": str(args.scopeId) if args.scopeId is not None else None,
        # 填写统计周期（period）字段，值为表达式 args.period 的结果。
        "period": args.period,
        # 填写日期区间（dateRange）字段，值为表达式 { 的结果。
        "dateRange": {
            # 填写日期来源（dateFrom）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "dateFrom": date_range.get("dateFrom"),
            # 填写日期To（dateTo）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "dateTo": date_range.get("dateTo"),
        },
        # 填写总数实际秒数（totalActualSeconds）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "totalActualSeconds": data.get("totalActualSeconds"),
        # 填写总数实际分钟数（totalActualMinutes）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "totalActualMinutes": _minutes(data.get("totalActualSeconds")),
        # 填写区间实际秒数（rangeActualSeconds）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "rangeActualSeconds": data.get("rangeActualSeconds"),
        # 填写区间实际分钟数（rangeActualMinutes）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "rangeActualMinutes": _minutes(data.get("rangeActualSeconds")),
        # 填写总数会话列表（totalSessions）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "totalSessions": data.get("totalSessions"),
        # 填写区间会话列表（rangeSessions）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "rangeSessions": data.get("rangeSessions"),
        # 填写finished会话列表（finishedSessions）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "finishedSessions": data.get("finishedSessions"),
        # 填写运行中会话列表（runningSessions）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "runningSessions": data.get("runningSessions"),
        # 填写已暂停会话列表（pausedSessions）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "pausedSessions": data.get("pausedSessions"),
        # 填写最新执行计时At（latestExecutionAt）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "latestExecutionAt": data.get("latestExecutionAt"),
        # 填写说明（note）字段，值为表达式 ( 的结果。
        "note": (
            # 继续传入或组合固定文字 'totalActualSeconds/totalSessions/finishedSessions/runningSessions/pausedSessions…'，完成上面尚未结束的表达式。
            "totalActualSeconds/totalSessions/finishedSessions/runningSessions/pausedSessions 为 scope 内全部历史累计；"
            "rangeActualSeconds/rangeSessions 为所选日期区间内。totalSessions 是执行会话数，不是时间段数量。"
        ),
    }
    # 检查条件 time_zone is not None 是否成立；成立时执行缩进的分支。
    if time_zone is not None:
        # 把用户使用的时区（time_zone）保存到 提交数据时间时区（payload['timeZone']）。
        payload["timeZone"] = time_zone
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"{args.period} 范围内实际投入 {payload['rangeActualSeconds']} 秒" 的结果。
        summary=f"{args.period} 范围内实际投入 {payload['rangeActualSeconds']} 秒",
    )


# ---------- 工具声明 ----------


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 当前执行计时业务工具（CURRENT_EXECUTION_TOOL）。
CURRENT_EXECUTION_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_current_execution'。
    name="get_current_execution",
    # 传入显示名称（label），值为固定文字 '当前执行'。
    label="当前执行",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人当前正在进行的执行计时：任务名称、开始时间、累计实际秒数、当前时间段与心跳截止时间；'，完成上面尚未结束的表达式。
        "查询用户本人当前正在进行的执行计时：任务名称、开始时间、累计实际秒数、当前时间段与心跳截止时间；"
        "没有正在执行时返回空并说明可能已暂停或未开始。工具只读，不会开始、暂停或结束计时。"
        "用户询问现在在做什么、当前计时情况时使用。"
    ),
    # 传入权限（permission），值为固定文字 'execution:session:read'。
    permission="execution:session:read",
    # 传入参数模型（args_model），值为当前执行计时参数（CurrentExecutionArgs）。
    args_model=CurrentExecutionArgs,
    # 传入handler，值为方案运行当前执行计时（run_current_execution）。
    handler=run_current_execution,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 执行计时列表业务工具（EXECUTION_LIST_TOOL）。
EXECUTION_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_executions'。
    name="list_executions",
    # 传入显示名称（label），值为固定文字 '执行历史'。
    label="执行历史",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的执行记录列表，可按状态（进行中/已暂停/已结束）、任务、目标、阶段和执行日期范围筛选，'，完成上面尚未结束的表达式。
        "查询用户本人的执行记录列表，可按状态（进行中/已暂停/已结束）、任务、目标、阶段和执行日期范围筛选，"
        "最多返回 20 条并给出总条数。返回任务名称、状态、开始/结束时间、实际秒数；使用日期范围时同时返回区间内秒数。"
        "日期范围支持今天/昨天/本周/上周或自定义；今天/本周取自 Java 账号有效时区。"
        "用户询问今天/本周执行记录、某任务执行过几次、执行历史时使用。"
    ),
    # 传入权限（permission），值为固定文字 'execution:session:list'。
    permission="execution:session:list",
    # 传入参数模型（args_model），值为执行计时列表参数（ExecutionListArgs）。
    args_model=ExecutionListArgs,
    # 传入handler，值为方案运行列表执行记录（run_list_executions）。
    handler=run_list_executions,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 执行计时详情业务工具（EXECUTION_DETAIL_TOOL）。
EXECUTION_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_execution'。
    name="get_execution",
    # 传入显示名称（label），值为固定文字 '执行详情'。
    label="执行详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按执行记录 ID 查询详情：任务、状态、开始/结束时间、累计与区间实际秒数、当前时间段，以及首屏执行时间段。'，完成上面尚未结束的表达式。
        "按执行记录 ID 查询详情：任务、状态、开始/结束时间、累计与区间实际秒数、当前时间段，以及首屏执行时间段。"
        "执行记录 ID 必须来自本人执行历史。时间段超过首屏时用 list_execution_segments 继续查询。"
        "用户询问某次执行的详细情况时使用。"
    ),
    # 传入权限（permission），值为固定文字 'execution:session:read'。
    permission="execution:session:read",
    # 传入参数模型（args_model），值为执行计时详情参数（ExecutionDetailArgs）。
    args_model=ExecutionDetailArgs,
    # 传入handler，值为方案运行读取执行计时（run_get_execution）。
    handler=run_get_execution,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 连续计时段列表业务工具（SEGMENT_LIST_TOOL）。
SEGMENT_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_execution_segments'。
    name="list_execution_segments",
    # 传入显示名称（label），值为固定文字 '执行时间段'。
    label="执行时间段",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按执行记录 ID 查询时间段（开始/结束时间、实际秒数、结束原因和时区），最多返回 20 段并给出总秒数。'，完成上面尚未结束的表达式。
        "按执行记录 ID 查询时间段（开始/结束时间、实际秒数、结束原因和时区），最多返回 20 段并给出总秒数。"
        "结束原因区分用户暂停、心跳超时自动暂停、到计划边界暂停、登录失效暂停、用户结束、任务删除或目标归档。"
        "未结束时间段显示 open=true，实际秒数按服务端有效截止时间计算。"
        "用户询问某次执行暂停过几次、为什么暂停、每段实际投入时使用。"
    ),
    # 传入权限（permission），值为固定文字 'execution:session:read'。
    permission="execution:session:read",
    # 传入参数模型（args_model），值为连续计时段列表参数（SegmentListArgs）。
    args_model=SegmentListArgs,
    # 传入handler，值为方案运行列表计时段列表（run_list_segments）。
    handler=run_list_segments,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 执行计时汇总业务工具（EXECUTION_SUMMARY_TOOL）。
EXECUTION_SUMMARY_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_execution_summary'。
    name="get_execution_summary",
    # 传入显示名称（label），值为固定文字 '执行耗时汇总'。
    label="执行耗时汇总",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询本人执行耗时汇总，可按任务/阶段/目标限定范围，并按今天/昨天/本周/上周或自定义日期区间统计。'，完成上面尚未结束的表达式。
        "查询本人执行耗时汇总，可按任务/阶段/目标限定范围，并按今天/昨天/本周/上周或自定义日期区间统计。"
        "同时返回总累计（scope 内全部历史）与所选区间内实际秒数、会话数量、已结束/进行中/已暂停数量，"
        "并区分执行会话数与时间段数。耗时只读取执行事实，不用任务当前状态倒推历史完成情况。"
        "用户询问累计投入、今天/本周耗时、某任务/目标花了多少时间时使用。"
    ),
    # 传入权限（permission），值为固定文字 'execution:session:read'。
    permission="execution:session:read",
    # 传入参数模型（args_model），值为执行计时汇总参数（ExecutionSummaryArgs）。
    args_model=ExecutionSummaryArgs,
    # 传入handler，值为方案运行执行计时汇总（run_execution_summary）。
    handler=run_execution_summary,
)
