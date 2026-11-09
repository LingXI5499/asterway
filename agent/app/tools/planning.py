# 封装规划查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 计划只读工具：周计划、日计划、计划任务、待重新安排任务与任务当前安排。
# 
# 所有工具只读取 Java 本人业务数据；今天/本周由 Java 看板返回的账号日期与有效时区解析。

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
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 NOT_STARTED（NOT开始时）、固定状态或编码 IN_PROGRESS（IN进度）、固定状态或编码 PAUSED（已暂停）、固定状态或编码 COMPLETED（已完成） 的集合取出的内容保存到 任务状态（TaskStatus）。
TaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED", "COMPLETED"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 NOT_STARTED（NOT开始时）、固定状态或编码 IN_PROGRESS（IN进度）、固定状态或编码 PAUSED（已暂停） 的集合取出的内容保存到 是否展开任务状态（OpenTaskStatus）。
OpenTaskStatus = Literal["NOT_STARTED", "IN_PROGRESS", "PAUSED"]
# 把依次包含 固定文字 'assignmentId'、固定文字 'taskId'、固定文字 'taskName'、固定文字 'status'、固定文字 'priority'等内容 的集合保存到 规划任务FIELDS（_PLANNING_TASK_FIELDS）。
_PLANNING_TASK_FIELDS = (
    # 继续传入或组合固定文字 'assignmentId'，完成上面尚未结束的表达式。
    "assignmentId",
    # 继续传入或组合固定文字 'taskId'，完成上面尚未结束的表达式。
    "taskId",
    # 继续传入或组合固定文字 'taskName'，完成上面尚未结束的表达式。
    "taskName",
    # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
    "status",
    # 继续传入或组合固定文字 'priority'，完成上面尚未结束的表达式。
    "priority",
    # 继续传入或组合固定文字 'weight'，完成上面尚未结束的表达式。
    "weight",
    # 继续传入或组合固定文字 'goalId'，完成上面尚未结束的表达式。
    "goalId",
    # 继续传入或组合固定文字 'goalName'，完成上面尚未结束的表达式。
    "goalName",
    # 继续传入或组合固定文字 'stageId'，完成上面尚未结束的表达式。
    "stageId",
    # 继续传入或组合固定文字 'stageName'，完成上面尚未结束的表达式。
    "stageName",
    # 继续传入或组合固定文字 'weeklyPlanId'，完成上面尚未结束的表达式。
    "weeklyPlanId",
    # 继续传入或组合固定文字 'dailyPlanId'，完成上面尚未结束的表达式。
    "dailyPlanId",
    # 继续传入或组合固定文字 'weekStartDate'，完成上面尚未结束的表达式。
    "weekStartDate",
    # 继续传入或组合固定文字 'planDate'，完成上面尚未结束的表达式。
    "planDate",
    # 继续传入或组合固定文字 'sortOrder'，完成上面尚未结束的表达式。
    "sortOrder",
    # 继续传入或组合固定文字 'archivedAt'，完成上面尚未结束的表达式。
    "archivedAt",
)


# ---------- 参数模型 ----------


# 定义 WeeklyPlanListArgs，把每周方案列表参数需要的数据与操作放在同一个对象中。
class WeeklyPlanListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 统计周期（period），类型为 Literal['ALL', 'CURRENT', 'PREVIOUS', 'NEXT']；其内容为执行“调用 Field 完成字段处理”得到的结果。
    period: Literal["ALL", "CURRENT", "PREVIOUS", "NEXT"] = Field(
        # 传入默认值（default），值为固定状态或编码 ALL（全部）。
        default="ALL",
        # 传入说明（description），值为固定文字 '周范围：CURRENT 本周、PREVIOUS 上周、NEXT 下周、ALL 全部；不传为 ALL'。
        description="周范围：CURRENT 本周、PREVIOUS 上周、NEXT 下周、ALL 全部；不传为 ALL",
    )
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 WeeklyPlanDetailArgs，把每周方案详情参数需要的数据与操作放在同一个对象中。
class WeeklyPlanDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 每周方案编号（weeklyPlanId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weeklyPlanId: int | None = Field(
        # 传入默认值（default），值为表达式 None, description="周计划 ID；与 period 二选一" 的结果。
        default=None, description="周计划 ID；与 period 二选一"
    )
    # 声明 统计周期（period），类型为 Literal['CURRENT', 'PREVIOUS', 'NEXT'] | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    period: Literal["CURRENT", "PREVIOUS", "NEXT"] | None = Field(
        # 传入默认值（default），值为按表达式计算得到的值：None, description="按相对周查询：本周/上周/下周；与 weeklyPlanId 二选一"。
        default=None, description="按相对周查询：本周/上周/下周；与 weeklyPlanId 二选一"
    )

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_mode：检查 mode。
    def validate_mode(self) -> "WeeklyPlanDetailArgs":
        # 检查self.weeklyPlanId is None) == (self.period is None，即对应内容是否不存在；成立时执行缩进的分支。
        if (self.weeklyPlanId is None) == (self.period is None):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("weeklyPlanId 与 period 必须且只能提供一个")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 DailyPlanArgs，把每日方案参数需要的数据与操作放在同一个对象中。
class DailyPlanArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 每日方案编号（dailyPlanId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dailyPlanId: int | None = Field(
        # 传入默认值（default），值为表达式 None, description="日计划 ID，来自周计划任务列表或日计划结果" 的结果。
        default=None, description="日计划 ID，来自周计划任务列表或日计划结果"
    )
    # 声明 每周方案编号（weeklyPlanId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weeklyPlanId: int | None = Field(
        # 传入默认值（default），值为表达式 None, description="周计划 ID；指定后按该周内日期查询日计划" 的结果。
        default=None, description="周计划 ID；指定后按该周内日期查询日计划"
    )
    # 声明 方案日期（planDate），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    planDate: str | None = Field(
        # 传入默认值（default），值为表达式 None, description="日期 YYYY-MM-DD；不传且未给 relativeDay 时默认今天" 的结果。
        default=None, description="日期 YYYY-MM-DD；不传且未给 relativeDay 时默认今天"
    )
    # 声明 relative日（relativeDay），类型为 Literal['TODAY', 'TOMORROW', 'YESTERDAY'] | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    relativeDay: Literal["TODAY", "TOMORROW", "YESTERDAY"] | None = Field(
        # 传入默认值（default），值为按表达式计算得到的值：None, description="相对今天：今天/明天/昨天；与 planDate 二选一"。
        default=None, description="相对今天：今天/明天/昨天；与 planDate 二选一"
    )

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_mode：检查 mode。
    def validate_mode(self) -> "DailyPlanArgs":
        # 检查条件 self.dailyPlanId is not None 是否成立；成立时执行缩进的分支。
        if self.dailyPlanId is not None:
            # 检查条件 self.weeklyPlanId is not None or self.planDate is not None or self.relativeDay is not None 是否成立；成立时执行缩进的分支。
            if (
                # 继续传入或组合条件判断的结果：self.weeklyPlanId is not None，完成上面尚未结束的表达式。
                self.weeklyPlanId is not None
                # 继续补充本次调用或判断：条件判断的结果：or self.planDate is not None。
                or self.planDate is not None
                # 继续补充本次调用或判断：条件判断的结果：or self.relativeDay is not None。
                or self.relativeDay is not None
            ):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("dailyPlanId 不能与日期或周计划参数同时使用")
            # 把当前实例（self）交还给调用方，本函数到此结束。
            return self
        # 检查条件 self.planDate is not None and self.relativeDay is not None 是否成立；成立时执行缩进的分支。
        if self.planDate is not None and self.relativeDay is not None:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("planDate 与 relativeDay 只能提供一个")
        # 检查条件 self.planDate is not None and (not _DATE.fullmatch(self.planDate)) 是否成立；成立时执行缩进的分支。
        if self.planDate is not None and not _DATE.fullmatch(self.planDate):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("planDate 必须是 YYYY-MM-DD")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 PlanningTasksArgs，把规划任务列表参数需要的数据与操作放在同一个对象中。
class PlanningTasksArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 每周方案编号（weeklyPlanId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weeklyPlanId: int | None = Field(default=None, description="周计划 ID；与日计划二选一")
    # 声明 每日方案编号（dailyPlanId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dailyPlanId: int | None = Field(default=None, description="日计划 ID；与周计划二选一")
    # 声明 状态（status），类型为 TaskStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: TaskStatus | None = Field(default=None, description="任务状态筛选")
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="任务名称关键字")
    # 声明 目标编号（goalId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int | None = Field(default=None, description="只看某个目标下的计划任务")
    # 声明 阶段编号（stageId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int | None = Field(default=None, description="只看某个阶段下的计划任务")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_mode：检查 mode。
    def validate_mode(self) -> "PlanningTasksArgs":
        # 检查self.weeklyPlanId is None) == (self.dailyPlanId is None，即对应内容是否不存在；成立时执行缩进的分支。
        if (self.weeklyPlanId is None) == (self.dailyPlanId is None):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("weeklyPlanId 与 dailyPlanId 必须且只能提供一个")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 PendingTasksArgs，把等待处理任务列表参数需要的数据与操作放在同一个对象中。
class PendingTasksArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 状态（status），类型为 OpenTaskStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: OpenTaskStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="任务状态筛选：未开始、进行中或暂停" 的结果。
        default=None, description="任务状态筛选：未开始、进行中或暂停"
    )
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="任务名称关键字")
    # 声明 目标编号（goalId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int | None = Field(default=None, description="只看某个目标下的任务")
    # 声明 阶段编号（stageId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int | None = Field(default=None, description="只看某个阶段下的任务")
    # 声明 日期来源（dateFrom），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateFrom: str | None = Field(
        # 传入默认值（default），值为表达式 None, description="原安排日期下限 YYYY-MM-DD，可省略" 的结果。
        default=None, description="原安排日期下限 YYYY-MM-DD，可省略"
    )
    # 声明 日期To（dateTo），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateTo: str | None = Field(
        # 传入默认值（default），值为表达式 None, description="原安排日期上限 YYYY-MM-DD，可省略" 的结果。
        default=None, description="原安排日期上限 YYYY-MM-DD，可省略"
    )
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_dates：检查 dates。
    def validate_dates(self) -> "PendingTasksArgs":
        # 从依次包含 当前实例（self）的日期来源（dateFrom）属性、当前实例（self）的日期To（dateTo）属性 的集合依次取出值（value），每项执行一次下面的处理。
        for value in (self.dateFrom, self.dateTo):
            # 检查条件 value is not None and (not _DATE.fullmatch(value)) 是否成立；成立时执行缩进的分支。
            if value is not None and not _DATE.fullmatch(value):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期必须是 YYYY-MM-DD")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 TaskAssignmentArgs，把任务任务安排参数需要的数据与操作放在同一个对象中。
class TaskAssignmentArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 任务编号（taskId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskId: int = Field(description="任务 ID，来自本人任务列表")


# ---------- 公共辅助 ----------


# 函数 _parse_date：调用 _parse_date 完成解析日期处理；接收 值（value）。
def _parse_date(value: str | None) -> date | None:
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


# 函数 _week_end：调用 _week_end 完成周结束处理；接收 周开始（week_start）。
def _week_end(week_start: Any) -> str | None:
    # 把条件 isinstance(week_start, str) 是否成立时使用执行“调用 _parse_date 完成解析日期处理”得到的结果，否则使用空值，表示当前还没有对应内容保存到 parsed。
    parsed = _parse_date(week_start) if isinstance(week_start, str) else None
    # 把条件 parsed 是否成立时使用执行“判断是否为 oformat”得到的结果，否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return (parsed + timedelta(days=6)).isoformat() if parsed else None


# 函数 _weekly_brief：周计划列表只返回定位信息；版本、创建时间等完整字段在详情工具返回。；接收 当前记录（row）。
def _weekly_brief(row: Any) -> dict[str, Any] | None:
    # 周计划列表只返回定位信息；版本、创建时间等完整字段在详情工具返回。
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把按 {key: row[key] for key in ('id', 'weekStartDate') if row.get(key) is not None} | {'weekEndDate': _week_end(row.get('weekStartDate'))} 计算的结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ("id", "weekStartDate") 的结果，完成上面尚未结束的表达式。
        for key in ("id", "weekStartDate")
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
    } | {"weekEndDate": _week_end(row.get("weekStartDate"))}


# 函数 _weekly_item：调用 _weekly_item 完成每周单条数据处理；接收 当前记录（row）。
def _weekly_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 声明 单条数据（item），类型为 dict[str, Any]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
    item: dict[str, Any] = {
        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        key: row[key] for key in ("id", "weekStartDate", "version", "createdAt", "updatedAt") if row.get(key) is not None
    }
    # 把执行“调用 _week_end 完成周结束处理”得到的结果保存到 单条数据周结束日期（item['weekEndDate']）。
    item["weekEndDate"] = _week_end(row.get("weekStartDate"))
    # 把单条数据（item）交还给调用方，本函数到此结束。
    return item


# 函数 _daily_item：调用 _daily_item 完成每日单条数据处理；接收 当前记录（row）。
def _daily_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ("id", "weeklyPlanId", "planDate", "version", "createdAt", "updatedAt") 的结果，完成上面尚未结束的表达式。
        for key in ("id", "weeklyPlanId", "planDate", "version", "createdAt", "updatedAt")
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    }


# 函数 _planning_task：调用 _planning_task 完成规划任务处理；接收 当前记录（row）。
def _planning_task(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('taskId') is None or (not isinstance(row.get('taskName'), str))，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("taskId") is None or not isinstance(
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        row.get("taskName"), str
    ):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: row[key] for key in _PLANNING_TASK_FIELDS if row.get(key) is not None}


# 函数 _has：调用 _has 完成是否存在处理；接收 上下文（context）、权限（permission）。
def _has(context: AgentContext, permission: str) -> bool:
    # 把当前用户是否具有需要的权限：permission in context.permissions交还给调用方，本函数到此结束。
    return permission in context.permissions


# 异步函数 _today：通过 Java 看板解析账号当前业务日期；无看板权限时返回可读错误。；接收 上下文（context）、客户端（client）。
async def _today(context: AgentContext, client: JavaApiClient) -> tuple[date | None, str | None]:
    # 通过 Java 看板解析账号当前业务日期；无看板权限时返回可读错误。
    # 检查条件 not _has(context, 'dashboard:board:read') 是否成立；成立时执行缩进的分支。
    if not _has(context, "dashboard:board:read"):
        # 把依次包含 空值，表示当前还没有对应内容、固定文字 '当前账户无法读取账号今天日期，请改用明确日期或 ALL 范围查询' 的集合交还给调用方，本函数到此结束。
        return None, "当前账户无法读取账号今天日期，请改用明确日期或 ALL 范围查询"
    # 把等待执行“调用 shared.today 完成今天处理”得到的结果完成后的结果保存到 值时间时区错误消息（(value, _time_zone, error_message)）。
    value, _time_zone, error_message = await shared.today(context, client)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把依次包含 空值，表示当前还没有对应内容、错误消息（error_message） 的集合交还给调用方，本函数到此结束。
        return None, error_message
    # 把条件 value 是否成立时使用执行“调用 _parse_date 完成解析日期处理”得到的结果，否则使用空值，表示当前还没有对应内容保存到 parsed。
    parsed = _parse_date(value) if value else None
    # 检查parsed is None，即对应内容是否不存在；成立时执行缩进的分支。
    if parsed is None:
        # 把依次包含 空值，表示当前还没有对应内容、固定文字 '账号今天日期格式不正确' 的集合交还给调用方，本函数到此结束。
        return None, "账号今天日期格式不正确"
    # 把依次包含 parsed、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
    return parsed, None


# 异步函数 _relative_week_start：调用 _relative_week_start 完成relative周开始处理；接收 上下文（context）、客户端（client）、统计周期（period）。
async def _relative_week_start(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, period: str 的结果。
    context: AgentContext, client: JavaApiClient, period: str
# 继续传入或组合表达式 ) -> tuple[date | None, str | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[date | None, str | None]:
    # 把等待执行“调用 _today 完成今天处理”得到的结果完成后的结果保存到 今天值错误消息（(today_value, error_message)）。
    today_value, error_message = await _today(context, client)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把依次包含 空值，表示当前还没有对应内容、错误消息（error_message） 的集合交还给调用方，本函数到此结束。
        return None, error_message
    # 断言条件 today_value is not None 是否成立；不满足时本项检查失败。
    assert today_value is not None
    # 把执行“调用 _monday 完成monday处理”得到的结果保存到 monday。
    monday = _monday(today_value)
    # 检查条件 period == 'PREVIOUS' 是否成立；成立时执行缩进的分支。
    if period == "PREVIOUS":
        # 把 monday 减去 执行“调用 timedelta 完成两个时刻之间的时间长度处理”得到的结果，再保存回原变量。
        monday -= timedelta(days=7)
    # 检查条件 period == 'NEXT' 是否成立；成立时执行缩进的分支。
    elif period == "NEXT":
        # 把 monday 加上 执行“调用 timedelta 完成两个时刻之间的时间长度处理”得到的结果，再保存回原变量。
        monday += timedelta(days=7)
    # 把依次包含 monday、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
    return monday, None


# ---------- 周计划 ----------


# 异步函数 run_list_weekly_plans：调用 run_list_weekly_plans 完成方案运行列表每周方案列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_weekly_plans(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: WeeklyPlanListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: WeeklyPlanListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    # 声明 今天值（today_value），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    today_value: str | None = None
    # 检查条件 args.period != 'ALL' 是否成立；成立时执行缩进的分支。
    if args.period != "ALL":
        # 把等待执行“调用 _relative_week_start 完成relative周开始处理”得到的结果完成后的结果保存到 monday错误消息（(monday, error_message)）。
        monday, error_message = await _relative_week_start(context, client, args.period)
        # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
        if error_message is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(error_message, "周计划查询失败")
        # 断言条件 monday is not None 是否成立；不满足时本项检查失败。
        assert monday is not None
        # 把执行“判断是否为 oformat”得到的结果保存到 今天值（today_value）。
        today_value = monday.isoformat()
        # 把执行“判断是否为 oformat”得到的结果保存到 参数集合日期来源（params['dateFrom']）。
        params["dateFrom"] = monday.isoformat()
        # 把执行“判断是否为 oformat”得到的结果保存到 参数集合日期To（params['dateTo']）。
        params["dateTo"] = monday.isoformat()
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "weekly-plans", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "周计划查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("周计划返回结构不正确", "周计划返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _weekly_brief(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把参数（args）的统计周期（period）属性保存到 提交数据period（payload['period']）。
    payload["period"] = args.period
    # 检查条件 args.period != 'ALL' and items 是否成立；成立时执行缩进的分支。
    if args.period != "ALL" and items:
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交数据周开始日期（payload['weekStartDate']）。
        payload["weekStartDate"] = items[0].get("weekStartDate")
    # 检查条件 args.period != 'ALL' 是否成立；成立时执行缩进的分支。
    elif args.period != "ALL":
        # 把今天值（today_value）保存到 提交数据查询条件周开始日期（payload['queryWeekStartDate']）。
        payload["queryWeekStartDate"] = today_value
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"周计划 {payload['returned']} 个（共 {payload['total']} 个）" 的结果。
        summary=f"周计划 {payload['returned']} 个（共 {payload['total']} 个）",
    )


# 异步函数 run_get_weekly_plan：调用 run_get_weekly_plan 完成方案运行读取每周方案处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_weekly_plan(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: WeeklyPlanDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: WeeklyPlanDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 resolved周（resolved_week），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    resolved_week: str | None = None
    # 把条件 args.weeklyPlanId is not None 是否成立时使用固定文字 'id'，否则使用固定文字 'period'保存到 mode。
    mode = "id" if args.weeklyPlanId is not None else "period"
    # 检查条件 args.weeklyPlanId is not None 是否成立；成立时执行缩进的分支。
    if args.weeklyPlanId is not None:
        # 把在文字占位处填入变量后组成的字符串保存到 路径（path）。
        path = f"weekly-plans/{args.weeklyPlanId}"
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 把等待执行“调用 _relative_week_start 完成relative周开始处理”得到的结果完成后的结果保存到 monday错误消息（(monday, error_message)）。
        monday, error_message = await _relative_week_start(context, client, args.period or "CURRENT")
        # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
        if error_message is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(error_message, "周计划查询失败")
        # 断言条件 monday is not None 是否成立；不满足时本项检查失败。
        assert monday is not None
        # 把执行“判断是否为 oformat”得到的结果保存到 resolved周（resolved_week）。
        resolved_week = monday.isoformat()
        # 把在文字占位处填入变量后组成的字符串保存到 路径（path）。
        path = f"weekly-plans/by-week/{monday.isoformat()}"
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, path)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "周计划查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {"resolvedBy": mode}
    # 检查条件 resolved_week is not None 是否成立；成立时执行缩进的分支。
    if resolved_week is not None:
        # 把resolved周（resolved_week）保存到 提交数据查询条件周开始日期（payload['queryWeekStartDate']）。
        payload["queryWeekStartDate"] = resolved_week
    # 把执行“调用 _weekly_item 完成每周单条数据处理”得到的结果保存到 方案（plan）。
    plan = _weekly_item(data)
    # 把方案（plan）保存到 提交数据每周方案（payload['weeklyPlan']）。
    payload["weeklyPlan"] = plan
    # 检查plan is None，即对应内容是否不存在；成立时执行缩进的分支。
    if plan is None:
        # 把固定文字 '查询的周没有有效周计划'保存到 提交数据说明（payload['note']）。
        payload["note"] = "查询的周没有有效周计划"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=("周计划「" + str(plan.get("weekStartDate")) + "」详情") if plan else "该周没有周计划",
    )


# ---------- 日计划 ----------


# 异步函数 _resolve_daily_plan：调用 _resolve_daily_plan 完成resolve每日方案处理；接收 上下文（context）、客户端（client）、参数（args）。
async def _resolve_daily_plan(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: DailyPlanArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: DailyPlanArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 检查条件 args.dailyPlanId is not None 是否成立；成立时执行缩进的分支。
    if args.dailyPlanId is not None:
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
        result, error_message = await shared.get(
            # 继续传入或组合按表达式计算得到的值：context, client, f"daily-plans/{args.dailyPlanId}"，完成上面尚未结束的表达式。
            context, client, f"daily-plans/{args.dailyPlanId}"
        )
        # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
        if error_message is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(error_message, "日计划查询失败")
        # 把执行“调用 _daily_item 完成每日单条数据处理”得到的结果保存到 方案（plan）。
        plan = _daily_item(result.data if result is not None else None)
        # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
        return ToolOutcome(
            # 传入内容（content），值为把对象编码为 JSON 文字。
            content=json.dumps(
                # 继续传入或组合表达式 {"resolvedBy": "id", "dailyPlan": plan} 的结果，完成上面尚未结束的表达式。
                {"resolvedBy": "id", "dailyPlan": plan}
                # 继续传入或组合表达式 | ({"note": "该日计划不存在或已删除"} if plan is None else {}) 的结果，完成上面尚未结束的表达式。
                | ({"note": "该日计划不存在或已删除"} if plan is None else {}),
                # 传入ensureascii（ensure_ascii），值为假，表示条件不成立或开关关闭。
                ensure_ascii=False,
                # 传入separators，值为表达式 (",", ":") 的结果。
                separators=(",", ":"),
            ),
            # 传入汇总（summary），值为固定文字 '日计划详情" if plan else "该日计划不存在'。
            summary="日计划详情" if plan else "该日计划不存在",
        )

    # 日期模式：today/相对日期/指定日期
    # 声明 今天值（today_value），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    today_value: date | None = None
    # 检查args.relativeDay is not None or args.planDate is None，即对应内容是否不存在；成立时执行缩进的分支。
    if args.relativeDay is not None or args.planDate is None:
        # 把等待执行“调用 _today 完成今天处理”得到的结果完成后的结果保存到 今天值错误消息（(today_value, error_message)）。
        today_value, error_message = await _today(context, client)
        # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
        if error_message is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(error_message, "日计划查询失败")
    # 检查条件 args.planDate is not None 是否成立；成立时执行缩进的分支。
    if args.planDate is not None:
        # 把执行“调用 _parse_date 完成解析日期处理”得到的结果保存到 目标值（target）。
        target = _parse_date(args.planDate)
        # 检查target is None，即对应内容是否不存在；成立时执行缩进的分支。
        if target is None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error("日期格式不正确，请使用 YYYY-MM-DD", "日计划查询失败")
    # 检查条件 args.relativeDay is not None 是否成立；成立时执行缩进的分支。
    elif args.relativeDay is not None:
        # 断言条件 today_value is not None 是否成立；不满足时本项检查失败。
        assert today_value is not None
        # 把从按字段名保存多个值的字典中按参数（args）的relative日（relativeDay）属性取出的内容保存到 分页时跳过的记录数（offset）。
        offset = {"TODAY": 0, "TOMORROW": 1, "YESTERDAY": -1}[args.relativeDay]
        # 把按 today_value + timedelta(days=offset) 计算的结果保存到 目标值（target）。
        target = today_value + timedelta(days=offset)
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 断言条件 today_value is not None 是否成立；不满足时本项检查失败。
        assert today_value is not None
        # 把今天值（today_value）保存到 目标值（target）。
        target = today_value
    # 把执行“判断是否为 oformat”得到的结果保存到 目标值文字（target_text）。
    target_text = target.isoformat()
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {"resolvedBy": "date", "date": target_text}

    # 把参数（args）的每周方案编号（weeklyPlanId）属性保存到 每周编号（weekly_id）。
    weekly_id = args.weeklyPlanId
    # 检查weekly_id is None，即对应内容是否不存在；成立时执行缩进的分支。
    if weekly_id is None:
        # 检查条件 not _has(context, 'planning:weekly:read') 是否成立；成立时执行缩进的分支。
        if not _has(context, "planning:weekly:read"):
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(
                # 继续传入或组合固定文字 '当前账户无法读取周计划，请提供 weeklyPlanId 或 dailyPlanId", "日计划查询失败'，完成上面尚未结束的表达式。
                "当前账户无法读取周计划，请提供 weeklyPlanId 或 dailyPlanId", "日计划查询失败"
            )
        # 把执行“调用 _monday 完成monday处理”得到的结果保存到 monday。
        monday = _monday(target)
        # 把执行“判断是否为 oformat”得到的结果保存到 提交数据查询条件周开始日期（payload['queryWeekStartDate']）。
        payload["queryWeekStartDate"] = monday.isoformat()
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 周结果周错误（(week_result, week_error)）。
        week_result, week_error = await shared.get(
            # 继续传入或组合判断是否为 oformat，完成上面尚未结束的表达式。
            context, client, f"weekly-plans/by-week/{monday.isoformat()}"
        )
        # 检查条件 week_error is not None 是否成立；成立时执行缩进的分支。
        if week_error is not None:
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error(week_error, "日计划查询失败")
        # 把执行“调用 _weekly_item 完成每周单条数据处理”得到的结果保存到 周（week）。
        week = _weekly_item(week_result.data if week_result is not None else None)
        # 检查week is None，即对应内容是否不存在；成立时执行缩进的分支。
        if week is None:
            # 把空值，表示当前还没有对应内容保存到 提交数据每日方案（payload['dailyPlan']）。
            payload["dailyPlan"] = None
            # 把固定文字 '该日期所在自然周没有有效周计划，因此没有日计划'保存到 提交数据说明（payload['note']）。
            payload["note"] = "该日期所在自然周没有有效周计划，因此没有日计划"
            # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
            return ToolOutcome(
                # 传入内容（content），值为把对象编码为 JSON 文字。
                content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                # 传入汇总（summary），值为固定文字 '该日期没有日计划'。
                summary="该日期没有日计划",
            )
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 每周编号（weekly_id）。
        weekly_id = week.get("id")
        # 把每周编号（weekly_id）保存到 提交数据每周方案编号（payload['weeklyPlanId']）。
        payload["weeklyPlanId"] = weekly_id

    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, f"weekly-plans/{weekly_id}/daily-plans/{target_text}"，完成上面尚未结束的表达式。
        context, client, f"weekly-plans/{weekly_id}/daily-plans/{target_text}"
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "日计划查询失败")
    # 把执行“调用 _daily_item 完成每日单条数据处理”得到的结果保存到 方案（plan）。
    plan = _daily_item(result.data if result is not None else None)
    # 把方案（plan）保存到 提交数据每日方案（payload['dailyPlan']）。
    payload["dailyPlan"] = plan
    # 检查plan is None，即对应内容是否不存在；成立时执行缩进的分支。
    if plan is None:
        # 把固定文字 '该日期没有创建日计划'保存到 提交数据说明（payload['note']）。
        payload["note"] = "该日期没有创建日计划"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为固定文字 '该日期有日计划" if plan else "该日期没有日计划'。
        summary="该日期有日计划" if plan else "该日期没有日计划",
    )


# 异步函数 run_get_daily_plan：调用 run_get_daily_plan 完成方案运行读取每日方案处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_daily_plan(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: DailyPlanArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: DailyPlanArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“调用 _resolve_daily_plan 完成resolve每日方案处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await _resolve_daily_plan(context, client, args)


# ---------- 计划任务 / 待安排任务 ----------


# 函数 _task_params：调用 _task_params 完成任务参数集合处理；接收 上限（limit）、状态（status）、keyword。
def _task_params(
    # 填写上限（limit）字段，值为int。
    limit: int,
    # 填写状态（status）字段，值为表达式 str | None = None 的结果。
    status: str | None = None,
    # 填写keyword字段，值为表达式 str | None = None 的结果。
    keyword: str | None = None,
# 继续传入或组合表达式 ) -> dict[str, str | int]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, str | int]:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": limit}
    # 检查条件 status 是否成立；成立时执行缩进的分支。
    if status:
        # 把状态（status）保存到 参数集合状态（params['status']）。
        params["status"] = status
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把执行“去掉文字两端的空白”得到的结果保存到 cleaned。
        cleaned = keyword.strip()
        # 检查条件 cleaned 是否成立；成立时执行缩进的分支。
        if cleaned:
            # 把cleaned保存到 参数集合keyword（params['keyword']）。
            params["keyword"] = cleaned
    # 把参数集合（params）交还给调用方，本函数到此结束。
    return params


# 异步函数 run_list_planning_tasks：调用 run_list_planning_tasks 完成方案运行列表规划任务列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_planning_tasks(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: PlanningTasksArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: PlanningTasksArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把执行“调用 _task_params 完成任务参数集合处理”得到的结果保存到 参数集合（params）。
    params = _task_params(args.limit, args.status, args.keyword)
    # 检查条件 args.goalId is not None 是否成立；成立时执行缩进的分支。
    if args.goalId is not None:
        # 把参数（args）的目标编号（goalId）属性保存到 参数集合目标编号（params['goalId']）。
        params["goalId"] = args.goalId
    # 检查条件 args.stageId is not None 是否成立；成立时执行缩进的分支。
    if args.stageId is not None:
        # 把参数（args）的阶段编号（stageId）属性保存到 参数集合阶段编号（params['stageId']）。
        params["stageId"] = args.stageId
    # 检查条件 args.weeklyPlanId is not None 是否成立；成立时执行缩进的分支。
    if args.weeklyPlanId is not None:
        # 检查条件 not _has(context, 'planning:weekly:read') 是否成立；成立时执行缩进的分支。
        if not _has(context, "planning:weekly:read"):
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error("当前账户没有读取周计划任务的权限", "计划任务查询失败")
        # 把在文字占位处填入变量后组成的字符串保存到 路径（path）。
        path = f"weekly-plans/{args.weeklyPlanId}/tasks"
        # 把依次包含 固定文字 'weekly'、参数（args）的每周方案编号（weeklyPlanId）属性 的集合保存到 mode方案编号（(mode, plan_id)）。
        mode, plan_id = "weekly", args.weeklyPlanId
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 检查条件 not _has(context, 'planning:daily:read') 是否成立；成立时执行缩进的分支。
        if not _has(context, "planning:daily:read"):
            # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
            return shared.error("当前账户没有读取日计划任务的权限", "计划任务查询失败")
        # 把在文字占位处填入变量后组成的字符串保存到 路径（path）。
        path = f"daily-plans/{args.dailyPlanId}/tasks"
        # 把依次包含 固定文字 'daily'、参数（args）的每日方案编号（dailyPlanId）属性 的集合保存到 mode方案编号（(mode, plan_id)）。
        mode, plan_id = "daily", args.dailyPlanId
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, path, params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "计划任务查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("计划任务返回结构不正确", "计划任务返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _planning_task(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把mode保存到 提交数据mode（payload['mode']）。
    payload["mode"] = mode
    # 把方案编号（plan_id）保存到 提交数据mode方案编号（payload[mode + 'PlanId']）。
    payload[mode + "PlanId"] = plan_id
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"计划任务 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"计划任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_list_pending_tasks：调用 run_list_pending_tasks 完成方案运行列表等待处理任务列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_pending_tasks(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: PendingTasksArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: PendingTasksArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把执行“调用 _task_params 完成任务参数集合处理”得到的结果保存到 参数集合（params）。
    params = _task_params(args.limit, args.status, args.keyword)
    # 检查条件 args.goalId is not None 是否成立；成立时执行缩进的分支。
    if args.goalId is not None:
        # 把参数（args）的目标编号（goalId）属性保存到 参数集合目标编号（params['goalId']）。
        params["goalId"] = args.goalId
    # 检查条件 args.stageId is not None 是否成立；成立时执行缩进的分支。
    if args.stageId is not None:
        # 把参数（args）的阶段编号（stageId）属性保存到 参数集合阶段编号（params['stageId']）。
        params["stageId"] = args.stageId
    # 检查条件 args.dateFrom 是否成立；成立时执行缩进的分支。
    if args.dateFrom:
        # 把参数（args）的日期来源（dateFrom）属性保存到 参数集合日期来源（params['dateFrom']）。
        params["dateFrom"] = args.dateFrom
    # 检查条件 args.dateTo 是否成立；成立时执行缩进的分支。
    if args.dateTo:
        # 把参数（args）的日期To（dateTo）属性保存到 参数集合日期To（params['dateTo']）。
        params["dateTo"] = args.dateTo
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, "planning/pending-tasks", params，完成上面尚未结束的表达式。
        context, client, "planning/pending-tasks", params
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "待安排任务查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("待安排任务返回结构不正确", "待安排任务返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _planning_task(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"待安排任务 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"待安排任务 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# ---------- 任务当前安排 ----------


# 异步函数 run_get_task_assignment：调用 run_get_task_assignment 完成方案运行读取任务任务安排处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_task_assignment(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: TaskAssignmentArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: TaskAssignmentArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, f"tasks/{args.taskId}/assignment"，完成上面尚未结束的表达式。
        context, client, f"tasks/{args.taskId}/assignment"
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "任务安排查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 任务安排（assignment）。
    assignment = result.data if result is not None else None
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {"taskId": str(args.taskId)}
    # 检查not isinstance(assignment, dict) or assignment.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(assignment, dict) or assignment.get("id") is None:
        # 把空值，表示当前还没有对应内容保存到 提交数据任务安排（payload['assignment']）。
        payload["assignment"] = None
        # 把固定文字 '该任务当前没有有效安排（在任务池中），可安排到周计划或日计划'保存到 提交数据说明（payload['note']）。
        payload["note"] = "该任务当前没有有效安排（在任务池中），可安排到周计划或日计划"
        # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
        return ToolOutcome(
            # 传入内容（content），值为把对象编码为 JSON 文字。
            content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
            # 传入汇总（summary），值为固定文字 '任务当前未安排'。
            summary="任务当前未安排",
        )
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 单条数据（item）。
    item = {
        # 填写键（key）字段，值为表达式 assignment[key] 的结果。
        key: assignment[key]
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
            "id",
            # 继续传入或组合固定文字 'taskId'，完成上面尚未结束的表达式。
            "taskId",
            # 继续传入或组合固定文字 'weeklyPlanId'，完成上面尚未结束的表达式。
            "weeklyPlanId",
            # 继续传入或组合固定文字 'dailyPlanId'，完成上面尚未结束的表达式。
            "dailyPlanId",
            # 继续传入或组合固定文字 'sortOrder'，完成上面尚未结束的表达式。
            "sortOrder",
            # 继续传入或组合固定文字 'assignedAt'，完成上面尚未结束的表达式。
            "assignedAt",
            # 继续传入或组合固定文字 'endedAt'，完成上面尚未结束的表达式。
            "endedAt",
            # 继续传入或组合固定文字 'endReason'，完成上面尚未结束的表达式。
            "endReason",
            # 继续传入或组合固定文字 'version'，完成上面尚未结束的表达式。
            "version",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if assignment.get(key) is not None
    }
    # 把单条数据（item）保存到 提交数据任务安排（payload['assignment']）。
    payload["assignment"] = item
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 每周编号（weekly_id）。
    weekly_id = assignment.get("weeklyPlanId")
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 每日编号（daily_id）。
    daily_id = assignment.get("dailyPlanId")
    # 检查条件 isinstance(weekly_id, str) and _has(context, 'planning:weekly:read') 是否成立；成立时执行缩进的分支。
    if isinstance(weekly_id, str) and _has(context, "planning:weekly:read"):
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 周结果周错误（(week_result, week_error)）。
        week_result, week_error = await shared.get(context, client, f"weekly-plans/{weekly_id}")
        # 检查week_error is None and week_result is not None，即对应内容是否不存在；成立时执行缩进的分支。
        if week_error is None and week_result is not None:
            # 把执行“调用 _weekly_item 完成每周单条数据处理”得到的结果保存到 周（week）。
            week = _weekly_item(week_result.data)
            # 检查条件 week is not None 是否成立；成立时执行缩进的分支。
            if week is not None:
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交数据周开始日期（payload['weekStartDate']）。
                payload["weekStartDate"] = week.get("weekStartDate")
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交数据周结束日期（payload['weekEndDate']）。
                payload["weekEndDate"] = week.get("weekEndDate")
    # 检查条件 isinstance(daily_id, str) and _has(context, 'planning:daily:read') 是否成立；成立时执行缩进的分支。
    if isinstance(daily_id, str) and _has(context, "planning:daily:read"):
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 日结果日错误（(day_result, day_error)）。
        day_result, day_error = await shared.get(context, client, f"daily-plans/{daily_id}")
        # 检查day_error is None and day_result is not None，即对应内容是否不存在；成立时执行缩进的分支。
        if day_error is None and day_result is not None:
            # 把执行“调用 _daily_item 完成每日单条数据处理”得到的结果保存到 日（day）。
            day = _daily_item(day_result.data)
            # 检查条件 day is not None 是否成立；成立时执行缩进的分支。
            if day is not None:
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交数据方案日期（payload['planDate']）。
                payload["planDate"] = day.get("planDate")
    # 检查条件 daily_id is not None 是否成立；成立时执行缩进的分支。
    if daily_id is not None:
        # 把固定状态或编码 DAY（日）保存到 提交数据placement（payload['placement']）。
        payload["placement"] = "DAY"
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 把固定状态或编码 WEEK（周）保存到 提交数据placement（payload['placement']）。
        payload["placement"] = "WEEK"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为固定文字 '任务当前安排：'。
        summary="任务当前安排："
        # 继续传入或组合条件判断的结果：+ ("日计划" if daily_id is not None else "周计划")，完成上面尚未结束的表达式。
        + ("日计划" if daily_id is not None else "周计划"),
    )


# ---------- 工具声明 ----------


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 每周方案列表业务工具（WEEKLY_PLAN_LIST_TOOL）。
WEEKLY_PLAN_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_weekly_plans'。
    name="list_weekly_plans",
    # 传入显示名称（label），值为固定文字 '周计划列表'。
    label="周计划列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的周计划列表，可按相对周筛选：CURRENT 本周、PREVIOUS 上周、NEXT 下周、'，完成上面尚未结束的表达式。
        "查询用户本人的周计划列表，可按相对周筛选：CURRENT 本周、PREVIOUS 上周、NEXT 下周、"
        "ALL 全部；最多返回 20 条并给出总条数。今天/本周取自 Java 账号有效时区。"
        "返回周计划 ID、周一日期和周日日期。需要具体任务时再用 list_planning_tasks(weeklyPlanId)。"
        "用户询问本周/上周/下周计划或有哪些周计划时使用。"
    ),
    # 传入权限（permission），值为固定文字 'planning:weekly:list'。
    permission="planning:weekly:list",
    # 传入参数模型（args_model），值为每周方案列表参数（WeeklyPlanListArgs）。
    args_model=WeeklyPlanListArgs,
    # 传入handler，值为方案运行列表每周方案列表（run_list_weekly_plans）。
    handler=run_list_weekly_plans,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 每周方案详情业务工具（WEEKLY_PLAN_DETAIL_TOOL）。
WEEKLY_PLAN_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_weekly_plan'。
    name="get_weekly_plan",
    # 传入显示名称（label），值为固定文字 '周计划详情'。
    label="周计划详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按周计划 ID 或相对周（CURRENT/PREVIOUS/NEXT）查询本人的周计划：周一日期、周日日期和版本。'，完成上面尚未结束的表达式。
        "按周计划 ID 或相对周（CURRENT/PREVIOUS/NEXT）查询本人的周计划：周一日期、周日日期和版本。"
        "相对周以 Java 账号有效时区的今天解析。该周没有周计划时返回空并说明。"
        "用户询问某一周计划的完整信息时使用；任务列表请用 list_planning_tasks。"
    ),
    # 传入权限（permission），值为固定文字 'planning:weekly:read'。
    permission="planning:weekly:read",
    # 传入参数模型（args_model），值为每周方案详情参数（WeeklyPlanDetailArgs）。
    args_model=WeeklyPlanDetailArgs,
    # 传入handler，值为方案运行读取每周方案（run_get_weekly_plan）。
    handler=run_get_weekly_plan,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 每日方案业务工具（DAILY_PLAN_TOOL）。
DAILY_PLAN_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_daily_plan'。
    name="get_daily_plan",
    # 传入显示名称（label），值为固定文字 '日计划详情'。
    label="日计划详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的日计划：可按日计划 ID 查询，或按日期/相对日期（TODAY 今天、TOMORROW 明天、'，完成上面尚未结束的表达式。
        "查询用户本人的日计划：可按日计划 ID 查询，或按日期/相对日期（TODAY 今天、TOMORROW 明天、"
        "YESTERDAY 昨天、或不传表示今天）查询；不传 weeklyPlanId 时自动按账号时区定位所在自然周。"
        "该日期没有周计划或日计划时返回空并说明。需要日期内的任务时再用 list_planning_tasks(dailyPlanId)。"
    ),
    # 传入权限（permission），值为固定文字 'planning:daily:read'。
    permission="planning:daily:read",
    # 传入参数模型（args_model），值为每日方案参数（DailyPlanArgs）。
    args_model=DailyPlanArgs,
    # 传入handler，值为方案运行读取每日方案（run_get_daily_plan）。
    handler=run_get_daily_plan,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 规划任务列表业务工具（PLANNING_TASKS_TOOL）。
PLANNING_TASKS_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_planning_tasks'。
    name="list_planning_tasks",
    # 传入显示名称（label），值为固定文字 '计划任务列表'。
    label="计划任务列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询某个周计划或日计划下的任务安排，周计划与日计划 ID 二选一，最多返回 20 条并给出总条数。'，完成上面尚未结束的表达式。
        "查询某个周计划或日计划下的任务安排，周计划与日计划 ID 二选一，最多返回 20 条并给出总条数。"
        "返回安排 ID、任务 ID/名称、状态、优先级、所属目标/阶段、周一日期、安排日期和排序。"
        "用户询问本周/某天计划中有哪些任务时使用。"
    ),
    # 传入权限（permission），值为固定文字 'planning:weekly:read'。
    permission="planning:weekly:read",
    # 传入参数模型（args_model），值为规划任务列表参数（PlanningTasksArgs）。
    args_model=PlanningTasksArgs,
    # 传入handler，值为方案运行列表规划任务列表（run_list_planning_tasks）。
    handler=run_list_planning_tasks,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 等待处理任务列表业务工具（PENDING_TASKS_TOOL）。
PENDING_TASKS_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_pending_tasks'。
    name="list_pending_tasks",
    # 传入显示名称（label），值为固定文字 '待安排任务'。
    label="待安排任务",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人已过原计划日期或所在周已结束、但仍未完成的任务，用于回答哪些任务需要重新安排。'，完成上面尚未结束的表达式。
        "查询用户本人已过原计划日期或所在周已结束、但仍未完成的任务，用于回答哪些任务需要重新安排。"
        "可按状态、名称关键字、目标、阶段和原安排日期范围筛选，最多返回 20 条并给出总条数。"
        "返回任务、原安排日期、所属目标/阶段和原安排 ID。不会自动延期、复制或修改任务。"
    ),
    # 传入权限（permission），值为固定文字 'planning:pending:list'。
    permission="planning:pending:list",
    # 传入参数模型（args_model），值为等待处理任务列表参数（PendingTasksArgs）。
    args_model=PendingTasksArgs,
    # 传入handler，值为方案运行列表等待处理任务列表（run_list_pending_tasks）。
    handler=run_list_pending_tasks,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 任务任务安排业务工具（TASK_ASSIGNMENT_TOOL）。
TASK_ASSIGNMENT_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_task_assignment'。
    name="get_task_assignment",
    # 传入显示名称（label），值为固定文字 '任务安排'。
    label="任务安排",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按任务 ID 查询用户本人的任务当前安排：安排在哪个周计划/日计划、周一日期、安排日期、排序和状态；'，完成上面尚未结束的表达式。
        "按任务 ID 查询用户本人的任务当前安排：安排在哪个周计划/日计划、周一日期、安排日期、排序和状态；"
        "任务未安排时返回空并说明在任务池中。任务 ID 必须来自本人任务列表。"
        "用户询问某个任务安排在哪里或是否已安排时使用。"
    ),
    # 传入权限（permission），值为固定文字 'planning:assignment:read'。
    permission="planning:assignment:read",
    # 传入参数模型（args_model），值为任务任务安排参数（TaskAssignmentArgs）。
    args_model=TaskAssignmentArgs,
    # 传入handler，值为方案运行读取任务任务安排（run_get_task_assignment）。
    handler=run_get_task_assignment,
)
