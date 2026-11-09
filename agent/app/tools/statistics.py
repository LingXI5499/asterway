# 封装统计查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 统计只读工具：周期汇总、逐日趋势、成长时间线、习惯统计与对象状态分布。
# 
# 全部读取既有 Java 统计接口，不修改历史、不补写耗时；周期与今天口径由 Java 账号时区决定。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 re，供下面调用其功能。
import re

# 从 datetime 引入 日期（date）。
from datetime import date

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
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 TOTAL（总数）、固定状态或编码 WEEK（周）、固定状态或编码 MONTH（月）、固定状态或编码 YEAR（年）、固定状态或编码 RANGE（区间） 的集合取出的内容保存到 统计Period（StatisticsPeriod）。
StatisticsPeriod = Literal["TOTAL", "WEEK", "MONTH", "YEAR", "RANGE"]
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 HABIT（习惯）、固定状态或编码 CATEGORY（分类） 的集合取出的内容保存到 习惯分组按（HabitGroupBy）。
HabitGroupBy = Literal["HABIT", "CATEGORY"]
# 把按字段名保存多个值的字典保存到 PERIOD名称对应表（_PERIOD_LABELS）。
_PERIOD_LABELS = {
    # 填写总数（TOTAL）字段，值为固定文字 '全部历史'。
    "TOTAL": "全部历史",
    # 填写周（WEEK）字段，值为固定文字 '本周'。
    "WEEK": "本周",
    # 填写月（MONTH）字段，值为固定文字 '本月'。
    "MONTH": "本月",
    # 填写年（YEAR）字段，值为固定文字 '今年'。
    "YEAR": "今年",
    # 填写区间（RANGE）字段，值为固定文字 '自定义区间'。
    "RANGE": "自定义区间",
}
# 把按字段名保存多个值的字典保存到 种类名称对应表（_KIND_LABELS）。
_KIND_LABELS = {
    # 填写目标（GOAL）字段，值为固定文字 '目标'。
    "GOAL": "目标",
    # 填写阶段（STAGE）字段，值为固定文字 '阶段'。
    "STAGE": "阶段",
    # 填写任务（TASK）字段，值为固定文字 '任务'。
    "TASK": "任务",
    # 填写习惯（HABIT）字段，值为固定文字 '习惯'。
    "HABIT": "习惯",
    # 填写执行计时（EXECUTION）字段，值为固定文字 '执行计时'。
    "EXECUTION": "执行计时",
    # 填写规划（PLANNING）字段，值为固定文字 '计划安排'。
    "PLANNING": "计划安排",
    # 填写任务安排（ASSIGNMENT）字段，值为固定文字 '计划安排'。
    "ASSIGNMENT": "计划安排",
}
# 把按字段名保存多个值的字典保存到 事件名称对应表（_EVENT_LABELS）。
_EVENT_LABELS = {
    # 填写状态已修改（STATUS_CHANGED）字段，值为固定文字 '状态变更'。
    "STATUS_CHANGED": "状态变更",
    # 填写已完成（COMPLETED）字段，值为固定文字 '完成'。
    "COMPLETED": "完成",
    # 填写已归档（ARCHIVED）字段，值为固定文字 '归档'。
    "ARCHIVED": "归档",
    # 填写RESTORED字段，值为固定文字 '恢复'。
    "RESTORED": "恢复",
    # 填写打卡（CHECKIN）字段，值为固定文字 '打卡'。
    "CHECKIN": "打卡",
    # 填写打卡已取消（CHECKIN_CANCELLED）字段，值为固定文字 '撤销打卡'。
    "CHECKIN_CANCELLED": "撤销打卡",
    # 填写连续计时段开始时（SEGMENT_STARTED）字段，值为固定文字 '开始计时'。
    "SEGMENT_STARTED": "开始计时",
    # 填写用户暂停（USER_PAUSE）字段，值为固定文字 '用户暂停'。
    "USER_PAUSE": "用户暂停",
    # 填写AUTO暂停（AUTO_PAUSE）字段，值为固定文字 '自动暂停'。
    "AUTO_PAUSE": "自动暂停",
    # 填写方案BOUNDARY（PLAN_BOUNDARY）字段，值为固定文字 '计划边界暂停'。
    "PLAN_BOUNDARY": "计划边界暂停",
    # 填写认证REVOKED（AUTH_REVOKED）字段，值为固定文字 '登录失效暂停'。
    "AUTH_REVOKED": "登录失效暂停",
    # 填写用户FINISH（USER_FINISH）字段，值为固定文字 '用户结束'。
    "USER_FINISH": "用户结束",
    # 填写任务删除标记（TASK_DELETED）字段，值为固定文字 '任务删除结束'。
    "TASK_DELETED": "任务删除结束",
    # 填写目标已归档（GOAL_ARCHIVED）字段，值为固定文字 '目标归档结束'。
    "GOAL_ARCHIVED": "目标归档结束",
    # 填写FINISH字段，值为固定文字 '结束'。
    "FINISH": "结束",
}
# 把按字段名保存多个值的字典保存到 状态名称对应表（_STATUS_LABELS）。
_STATUS_LABELS = {
    # 填写NOT开始时（NOT_STARTED）字段，值为固定文字 '未开始'。
    "NOT_STARTED": "未开始",
    # 填写TODO字段，值为固定文字 '待开始'。
    "TODO": "待开始",
    # 填写IN进度（IN_PROGRESS）字段，值为固定文字 '进行中'。
    "IN_PROGRESS": "进行中",
    # 填写已暂停（PAUSED）字段，值为固定文字 '已暂停'。
    "PAUSED": "已暂停",
    # 填写页面是否仍处于活动状态（ACTIVE）字段，值为固定文字 '进行中'。
    "ACTIVE": "进行中",
    # 填写已完成（COMPLETED）字段，值为固定文字 '已完成'。
    "COMPLETED": "已完成",
    # 填写已完成（DONE）字段，值为固定文字 '已完成'。
    "DONE": "已完成",
    # 填写已结束（ENDED）字段，值为固定文字 '已结束'。
    "ENDED": "已结束",
    # 填写已归档（ARCHIVED）字段，值为固定文字 '已归档'。
    "ARCHIVED": "已归档",
    # 填写已取消（CANCELLED）字段，值为固定文字 '已取消'。
    "CANCELLED": "已取消",
}


# 定义 StatisticsArgs，把统计参数需要的数据与操作放在同一个对象中。
class StatisticsArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 统计周期（period），类型为 StatisticsPeriod；其内容为执行“调用 Field 完成字段处理”得到的结果。
    period: StatisticsPeriod = Field(
        # 传入默认值（default），值为固定状态或编码 MONTH（月）。
        default="MONTH",
        # 传入说明（description），值为固定文字 '统计周期：TOTAL 全部历史、WEEK 本周、MONTH 本月、YEAR 今年、RANGE 自定义区间'。
        description="统计周期：TOTAL 全部历史、WEEK 本周、MONTH 本月、YEAR 今年、RANGE 自定义区间",
    )
    # 声明 anchor日期（anchorDate），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    anchorDate: str | None = Field(
        # 传入默认值（default），值为表达式 None, description="周期参照日期 YYYY-MM-DD；省略时使用账号今天" 的结果。
        default=None, description="周期参照日期 YYYY-MM-DD；省略时使用账号今天"
    )
    # 声明 日期来源（dateFrom），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateFrom: str | None = Field(default=None, description="自定义区间开始日期，仅 period=RANGE")
    # 声明 日期To（dateTo），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateTo: str | None = Field(default=None, description="自定义区间结束日期，仅 period=RANGE")
    # 声明 目标编号（goalId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalId: int | None = Field(default=None, description="只看某个目标相关统计")
    # 声明 阶段编号（stageId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageId: int | None = Field(default=None, description="只看某个阶段相关统计")
    # 声明 任务编号（taskId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskId: int | None = Field(default=None, description="只看某个任务相关统计")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_dates：检查 dates。
    def validate_dates(self) -> "StatisticsArgs":
        # 检查条件 self.period == 'RANGE' 是否成立；成立时执行缩进的分支。
        if self.period == "RANGE":
            # 检查条件 not self.dateFrom or not self.dateTo 是否成立；成立时执行缩进的分支。
            if not self.dateFrom or not self.dateTo:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("period=RANGE 时必须同时提供 dateFrom 与 dateTo")
        # 检查条件 self.dateFrom is not None or self.dateTo is not None 是否成立；成立时执行缩进的分支。
        elif self.dateFrom is not None or self.dateTo is not None:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("只有 period=RANGE 才能指定 dateFrom/dateTo")
        # 从依次包含 当前实例（self）的anchor日期（anchorDate）属性、当前实例（self）的日期来源（dateFrom）属性、当前实例（self）的日期To（dateTo）属性 的集合依次取出值（value），每项执行一次下面的处理。
        for value in (self.anchorDate, self.dateFrom, self.dateTo):
            # 检查条件 value is not None and (not _DATE.fullmatch(value)) 是否成立；成立时执行缩进的分支。
            if value is not None and not _DATE.fullmatch(value):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期必须是 YYYY-MM-DD")
        # 检查条件 self.dateFrom and self.dateTo and (date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo)) 是否成立；成立时执行缩进的分支。
        if (
            # 继续传入或组合表达式 self.dateFrom 的结果，完成上面尚未结束的表达式。
            self.dateFrom
            # 继续补充本次调用或判断：表达式 and self.dateTo 的结果。
            and self.dateTo
            # 继续补充本次调用或判断：调用 and date.fromisoformat 完成fromisoformat处理。
            and date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo)
        ):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("开始日期不能晚于结束日期")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 StatisticsPageArgs，把统计当前页参数需要的数据与操作放在同一个对象中。
class StatisticsPageArgs(StatisticsArgs):
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 HabitStatisticsArgs，把习惯统计参数需要的数据与操作放在同一个对象中。
class HabitStatisticsArgs(StatisticsPageArgs):
    # 声明 分组按（groupBy），类型为 HabitGroupBy；其内容为执行“调用 Field 完成字段处理”得到的结果。
    groupBy: HabitGroupBy = Field(
        # 传入默认值（default），值为固定文字 'HABIT", description="分组方式：HABIT 按习惯、CATEGORY 按历史分类归属'。
        default="HABIT", description="分组方式：HABIT 按习惯、CATEGORY 按历史分类归属"
    )


# 函数 _labeled：调用 _labeled 完成labeled处理；接收 值（value）、名称对应表（labels）。
def _labeled(value: Any, labels: dict[str, str]) -> str | None:
    # 检查条件 isinstance(value, str) 是否成立；成立时执行缩进的分支。
    if isinstance(value, str):
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果交还给调用方，本函数到此结束。
        return labels.get(value, value)
    # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return None


# 函数 _statistics_params：调用 _statistics_params 完成统计参数集合处理；接收 参数（args）。
def _statistics_params(args: StatisticsArgs) -> dict[str, str | int]:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"period": args.period}
    # 检查条件 args.anchorDate 是否成立；成立时执行缩进的分支。
    if args.anchorDate:
        # 把参数（args）的anchor日期（anchorDate）属性保存到 参数集合anchor日期（params['anchorDate']）。
        params["anchorDate"] = args.anchorDate
    # 检查条件 args.period == 'RANGE' 是否成立；成立时执行缩进的分支。
    if args.period == "RANGE":
        # 把参数（args）的日期来源（dateFrom）属性或者固定文字 ''保存到 参数集合日期来源（params['dateFrom']）。
        params["dateFrom"] = args.dateFrom or ""
        # 把参数（args）的日期To（dateTo）属性或者固定文字 ''保存到 参数集合日期To（params['dateTo']）。
        params["dateTo"] = args.dateTo or ""
    # 从依次包含 固定文字 'goalId'、固定文字 'stageId'、固定文字 'taskId' 的集合依次取出键（key），每项执行一次下面的处理。
    for key in ("goalId", "stageId", "taskId"):
        # 把执行“读取 attr 的值”得到的结果保存到 值（value）。
        value = getattr(args, key)
        # 检查条件 value is not None 是否成立；成立时执行缩进的分支。
        if value is not None:
            # 把值（value）保存到 参数集合键（params[key]）。
            params[key] = value
    # 把参数集合（params）交还给调用方，本函数到此结束。
    return params


# 函数 _range_fields：调用 _range_fields 完成区间fields处理；接收 数据（data）。
def _range_fields(data: dict[str, Any]) -> dict[str, Any]:
    # 把按字段名保存多个值的字典保存到 结果（result）。
    result = {
        # 填写统计周期（period）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "period": data.get("period"),
        # 填写period显示名称（periodLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "periodLabel": _labeled(data.get("period"), _PERIOD_LABELS),
        # 填写日期来源（dateFrom）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "dateFrom": data.get("dateFrom"),
        # 填写日期To（dateTo）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "dateTo": data.get("dateTo"),
        # 填写用户使用的时区（timeZone）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "timeZone": data.get("timeZone"),
    }
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: value for key, value in result.items() if value is not None}


# 函数 _execution：调用 _execution 完成执行计时处理；接收 数据（data）。
def _execution(data: Any) -> dict[str, Any] | None:
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 结果（result）。
    result = {
        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        key: data.get(key)
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'totalActualSeconds'，完成上面尚未结束的表达式。
            "totalActualSeconds",
            # 继续传入或组合固定文字 'rangeActualSeconds'，完成上面尚未结束的表达式。
            "rangeActualSeconds",
            # 继续传入或组合固定文字 'totalSessions'，完成上面尚未结束的表达式。
            "totalSessions",
            # 继续传入或组合固定文字 'rangeSessions'，完成上面尚未结束的表达式。
            "rangeSessions",
            # 继续传入或组合固定文字 'totalFinishedSessions'，完成上面尚未结束的表达式。
            "totalFinishedSessions",
            # 继续传入或组合固定文字 'rangeFinishedSessions'，完成上面尚未结束的表达式。
            "rangeFinishedSessions",
            # 继续传入或组合固定文字 'lastExecutionAt'，完成上面尚未结束的表达式。
            "lastExecutionAt",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if data.get(key) is not None
    }
    # 检查条件 result.get('totalActualSeconds') is not None 是否成立；成立时执行缩进的分支。
    if result.get("totalActualSeconds") is not None:
        # 把执行“调用 round 完成轮处理”得到的结果保存到 结果总数实际分钟数（result['totalActualMinutes']）。
        result["totalActualMinutes"] = round(result["totalActualSeconds"] / 60, 2)
    # 检查条件 result.get('rangeActualSeconds') is not None 是否成立；成立时执行缩进的分支。
    if result.get("rangeActualSeconds") is not None:
        # 把执行“调用 round 完成轮处理”得到的结果保存到 结果区间实际分钟数（result['rangeActualMinutes']）。
        result["rangeActualMinutes"] = round(result["rangeActualSeconds"] / 60, 2)
    # 把结果（result）交还给调用方，本函数到此结束。
    return result


# 函数 _habit_totals：调用 _habit_totals 完成习惯totals处理；接收 数据（data）。
def _habit_totals(data: Any) -> dict[str, Any] | None:
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        key: data.get(key)
        # 继续传入或组合表达式 for key in ("doneCount", "dueCount", "completionRate") 的结果，完成上面尚未结束的表达式。
        for key in ("doneCount", "dueCount", "completionRate")
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if data.get(key) is not None
    }


# 函数 _state_totals：调用 _state_totals 完成当前状态totals处理；接收 数据（data）。
def _state_totals(data: Any) -> dict[str, Any] | None:
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        key: data.get(key)
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'goalCompletionActions'，完成上面尚未结束的表达式。
            "goalCompletionActions",
            # 继续传入或组合固定文字 'stageCompletionActions'，完成上面尚未结束的表达式。
            "stageCompletionActions",
            # 继续传入或组合固定文字 'taskCompletionActions'，完成上面尚未结束的表达式。
            "taskCompletionActions",
            # 继续传入或组合固定文字 'completedDistinctTasks'，完成上面尚未结束的表达式。
            "completedDistinctTasks",
            # 继续传入或组合固定文字 'goalArchiveActions'，完成上面尚未结束的表达式。
            "goalArchiveActions",
            # 继续传入或组合固定文字 'goalRestoreActions'，完成上面尚未结束的表达式。
            "goalRestoreActions",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if data.get(key) is not None
    }


# 函数 _trend_item：调用 _trend_item 完成趋势单条数据处理；接收 当前记录（row）。
def _trend_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('businessDate') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("businessDate") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'businessDate'，完成上面尚未结束的表达式。
            "businessDate",
            # 继续传入或组合固定文字 'actualSeconds'，完成上面尚未结束的表达式。
            "actualSeconds",
            # 继续传入或组合固定文字 'actualMilliseconds'，完成上面尚未结束的表达式。
            "actualMilliseconds",
            # 继续传入或组合固定文字 'sessions'，完成上面尚未结束的表达式。
            "sessions",
            # 继续传入或组合固定文字 'habitDone'，完成上面尚未结束的表达式。
            "habitDone",
            # 继续传入或组合固定文字 'habitDue'，完成上面尚未结束的表达式。
            "habitDue",
            # 继续传入或组合固定文字 'habitCompletionRate'，完成上面尚未结束的表达式。
            "habitCompletionRate",
            # 继续传入或组合固定文字 'goalCompletionActions'，完成上面尚未结束的表达式。
            "goalCompletionActions",
            # 继续传入或组合固定文字 'stageCompletionActions'，完成上面尚未结束的表达式。
            "stageCompletionActions",
            # 继续传入或组合固定文字 'taskCompletionActions'，完成上面尚未结束的表达式。
            "taskCompletionActions",
            # 继续传入或组合固定文字 'completedDistinctTasks'，完成上面尚未结束的表达式。
            "completedDistinctTasks",
            # 继续传入或组合固定文字 'goalArchiveActions'，完成上面尚未结束的表达式。
            "goalArchiveActions",
            # 继续传入或组合固定文字 'goalRestoreActions'，完成上面尚未结束的表达式。
            "goalRestoreActions",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    }


# 函数 _timeline_item：调用 _timeline_item 完成成长时间线单条数据处理；接收 当前记录（row）。
def _timeline_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('occurredAt') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("occurredAt") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写种类（kind）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "kind": row.get("kind"),
            # 填写种类显示名称（kindLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "kindLabel": _labeled(row.get("kind"), _KIND_LABELS),
            # 填写事件类型（eventType）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "eventType": row.get("eventType"),
            # 填写事件显示名称（eventLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "eventLabel": _labeled(row.get("eventType"), _EVENT_LABELS),
            # 填写occurredAt字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "occurredAt": row.get("occurredAt"),
            # 填写显示名称（label）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "label": row.get("label"),
            # 填写对象编号（objectId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "objectId": row.get("objectId"),
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 函数 _habit_group_item：调用 _habit_group_item 完成习惯分组单条数据处理；接收 当前记录（row）。
def _habit_group_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('name') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("name") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写编号（id）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "id": row.get("id"),
            # 填写名称（name）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "name": row.get("name"),
            # 填写删除标记（deleted）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "deleted": row.get("deleted"),
            # 填写已完成数量（doneCount）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "doneCount": row.get("doneCount"),
            # 填写应完成数量（dueCount）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "dueCount": row.get("dueCount"),
            # 填写完成情况频率（completionRate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "completionRate": row.get("completionRate"),
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 函数 _object_item：调用 _object_item 完成对象单条数据处理；接收 当前记录（row）。
def _object_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('kind') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("kind") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写种类（kind）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "kind": row.get("kind"),
            # 填写种类显示名称（kindLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "kindLabel": _labeled(row.get("kind"), _KIND_LABELS),
            # 填写状态（status）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "status": row.get("status"),
            # 填写状态显示名称（statusLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            # 填写对象数量（objectCount）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "objectCount": row.get("objectCount"),
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 异步函数 run_statistics_summary：调用 run_statistics_summary 完成方案运行统计汇总处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_statistics_summary(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StatisticsArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StatisticsArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, "statistics/summary", _statistics_params(args)，完成上面尚未结束的表达式。
        context, client, "statistics/summary", _statistics_params(args)
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "统计汇总查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("统计汇总返回结构不正确", "统计汇总返回异常")
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为执行“调用 _range_fields 完成区间fields处理”得到的结果。
    payload: dict[str, Any] = _range_fields(data)
    # 把执行“调用 _execution 完成执行计时处理”得到的结果保存到 提交数据执行计时（payload['execution']）。
    payload["execution"] = _execution(data.get("execution"))
    # 把执行“调用 _habit_totals 完成习惯totals处理”得到的结果保存到 提交数据习惯列表（payload['habits']）。
    payload["habits"] = _habit_totals(data.get("habits"))
    # 把执行“调用 _state_totals 完成当前状态totals处理”得到的结果保存到 提交数据states（payload['states']）。
    payload["states"] = _state_totals(data.get("states"))
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 筛选条件（filters）。
    filters = {
        # 填写键（key）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        key: data.get(key)
        # 继续传入或组合表达式 for key in ("goalId", "stageId", "taskId") 的结果，完成上面尚未结束的表达式。
        for key in ("goalId", "stageId", "taskId")
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if data.get(key) is not None
    }
    # 检查条件 filters 是否成立；成立时执行缩进的分支。
    if filters:
        # 把筛选条件（filters）保存到 提交数据筛选条件（payload['filters']）。
        payload["filters"] = filters
    # 把固定文字 'total* 为账号全部历史累计，range* 为所选统计周期；习惯统计独立于目标筛选'保存到 提交数据说明（payload['note']）。
    payload["note"] = "total* 为账号全部历史累计，range* 为所选统计周期；习惯统计独立于目标筛选"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为优先使用前面的有效值，没有时采用后面的备用值：f"统计汇总（{payload.get('periodLabel') or payload.get('period')}）"。
        summary=f"统计汇总（{payload.get('periodLabel') or payload.get('period')}）",
    )


# 异步函数 run_statistics_trends：调用 run_statistics_trends 完成方案运行统计趋势列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_statistics_trends(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StatisticsPageArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StatisticsPageArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把执行“调用 _statistics_params 完成统计参数集合处理”得到的结果保存到 参数集合（params）。
    params = _statistics_params(args)
    # 把固定数值 1保存到 参数集合当前页（params['page']）。
    params["page"] = 1
    # 把参数（args）的上限（limit）属性保存到 参数集合每页条数（params['size']）。
    params["size"] = args.limit
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "statistics/trends", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "逐日趋势查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("逐日趋势返回结构不正确", "逐日趋势返回异常")
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(
        # 继续传入或组合条件判断的结果：[item for row in data["items"] if (item := _trend_item(row)) is not None]，完成上面尚未结束的表达式。
        [item for row in data["items"] if (item := _trend_item(row)) is not None],
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        data.get("total"),
    )
    # 把固定文字 '逐日趋势来自真实业务事实，不从当前状态反推过去完成'保存到 提交数据说明（payload['note']）。
    payload["note"] = "逐日趋势来自真实业务事实，不从当前状态反推过去完成"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"逐日趋势 {payload['returned']} 天（共 {payload['total']} 天）" 的结果。
        summary=f"逐日趋势 {payload['returned']} 天（共 {payload['total']} 天）",
    )


# 异步函数 run_growth_timeline：调用 run_growth_timeline 完成方案运行成长业务成长时间线处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_growth_timeline(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StatisticsPageArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StatisticsPageArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把执行“调用 _statistics_params 完成统计参数集合处理”得到的结果保存到 参数集合（params）。
    params = _statistics_params(args)
    # 把固定数值 1保存到 参数集合当前页（params['page']）。
    params["page"] = 1
    # 把参数（args）的上限（limit）属性保存到 参数集合每页条数（params['size']）。
    params["size"] = args.limit
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "statistics/timeline", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "成长时间线查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("成长时间线返回结构不正确", "成长时间线返回异常")
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(
        # 继续传入或组合条件判断的结果：[item for row in data["items"] if (item := _timeline_item(row)) is not None]，完成上面尚未结束的表达式。
        [item for row in data["items"] if (item := _timeline_item(row)) is not None],
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        data.get("total"),
    )
    # 把固定文字 '时间线是真实状态、打卡、执行与计划事件，按发生时间倒序，不按当前状态推测'保存到 提交数据说明（payload['note']）。
    payload["note"] = "时间线是真实状态、打卡、执行与计划事件，按发生时间倒序，不按当前状态推测"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"成长时间线 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"成长时间线 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_habit_statistics：调用 run_habit_statistics 完成方案运行习惯统计处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_habit_statistics(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitStatisticsArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitStatisticsArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把执行“调用 _statistics_params 完成统计参数集合处理”得到的结果保存到 参数集合（params）。
    params = _statistics_params(args)
    # 把固定数值 1保存到 参数集合当前页（params['page']）。
    params["page"] = 1
    # 把参数（args）的上限（limit）属性保存到 参数集合每页条数（params['size']）。
    params["size"] = args.limit
    # 把参数（args）的分组按（groupBy）属性保存到 参数集合分组按（params['groupBy']）。
    params["groupBy"] = args.groupBy
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "statistics/habits", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "习惯统计查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("习惯统计返回结构不正确", "习惯统计返回异常")
    # 把条件 isinstance(data.get('groups'), dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用按字段名保存多个值的字典，这里先创建空字典保存到 分组列表（groups）。
    groups = data.get("groups") if isinstance(data.get("groups"), dict) else {}
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为执行“调用 _range_fields 完成区间fields处理”得到的结果。
    payload: dict[str, Any] = _range_fields(data)
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交数据分组按（payload['groupBy']）。
    payload["groupBy"] = data.get("groupBy")
    # 把执行“调用 _habit_totals 完成习惯totals处理”得到的结果保存到 提交数据totals（payload['totals']）。
    payload["totals"] = _habit_totals(data.get("totals"))
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据分组列表（payload['groups']）。
    payload["groups"] = shared.page(
        [
            # 继续传入或组合单条数据（item），完成上面尚未结束的表达式。
            item
            # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
            for row in groups.get("items", []) if isinstance(row, dict)
            # 继续传入或组合条件判断的结果：if (item := _habit_group_item(row)) is not None，完成上面尚未结束的表达式。
            if (item := _habit_group_item(row)) is not None
        ],
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        groups.get("total"),
    )
    # 把固定文字 '完成率=实际完成/应做；分母为零时为空；习惯统计不受目标筛选影响'保存到 提交数据说明（payload['note']）。
    payload["note"] = "完成率=实际完成/应做；分母为零时为空；习惯统计不受目标筛选影响"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        summary=f"习惯统计 {payload['groups'].get('returned', 0)} 组",
    )


# 异步函数 run_object_statistics：调用 run_object_statistics 完成方案运行对象统计处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_object_statistics(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: StatisticsArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: StatisticsArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, "statistics/objects", _statistics_params(args)，完成上面尚未结束的表达式。
        context, client, "statistics/objects", _statistics_params(args)
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "对象状态统计查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("对象状态统计返回结构不正确", "对象状态统计返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data if (item := _object_item(row)) is not None]
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {"items": items, "returned": len(items), "total": len(items)}
    # 把固定文字 '只统计当前有效未归档对象，与历史完成动作分开'保存到 提交数据说明（payload['note']）。
    payload["note"] = "只统计当前有效未归档对象，与历史完成动作分开"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"对象状态分布 {len(items)} 组" 的结果。
        summary=f"对象状态分布 {len(items)} 组",
    )


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 统计汇总业务工具（STATISTICS_SUMMARY_TOOL）。
STATISTICS_SUMMARY_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_statistics_summary'。
    name="get_statistics_summary",
    # 传入显示名称（label），值为固定文字 '统计汇总'。
    label="统计汇总",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按全部历史/本周/本月/今年/自定义区间汇总成长数据：实际投入（累计与区间、会话数）、'，完成上面尚未结束的表达式。
        "按全部历史/本周/本月/今年/自定义区间汇总成长数据：实际投入（累计与区间、会话数）、"
        "习惯完成与应做数量、目标/阶段/任务完成动作、归档与恢复动作；可按目标/阶段/任务限定范围。"
        "用户询问投入了多少时间、完成了多少任务、习惯完成率、某目标/任务统计时使用。"
    ),
    # 传入权限（permission），值为固定文字 'statistics:report:read'。
    permission="statistics:report:read",
    # 传入参数模型（args_model），值为统计参数（StatisticsArgs）。
    args_model=StatisticsArgs,
    # 传入handler，值为方案运行统计汇总（run_statistics_summary）。
    handler=run_statistics_summary,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 统计趋势列表业务工具（STATISTICS_TRENDS_TOOL）。
STATISTICS_TRENDS_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_statistics_trends'。
    name="get_statistics_trends",
    # 传入显示名称（label），值为固定文字 '逐日趋势'。
    label="逐日趋势",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按日期返回逐日趋势：当天实际秒数与会话数、习惯完成/应做与完成率、各类完成动作数、'，完成上面尚未结束的表达式。
        "按日期返回逐日趋势：当天实际秒数与会话数、习惯完成/应做与完成率、各类完成动作数、"
        "去重完成任务数、归档与恢复动作。最多 20 天并给出总天数。"
        "用户询问最近每天/每周投入变化、趋势时使用；周期与今天口径以账号时区为准。"
    ),
    # 传入权限（permission），值为固定文字 'statistics:report:read'。
    permission="statistics:report:read",
    # 传入参数模型（args_model），值为统计当前页参数（StatisticsPageArgs）。
    args_model=StatisticsPageArgs,
    # 传入handler，值为方案运行统计趋势列表（run_statistics_trends）。
    handler=run_statistics_trends,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 成长业务成长时间线业务工具（GROWTH_TIMELINE_TOOL）。
GROWTH_TIMELINE_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_growth_timeline'。
    name="get_growth_timeline",
    # 传入显示名称（label），值为固定文字 '成长时间线'。
    label="成长时间线",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按发生时间倒序返回真实成长事件：目标/阶段/任务状态变更与完成、习惯打卡与撤销、'，完成上面尚未结束的表达式。
        "按发生时间倒序返回真实成长事件：目标/阶段/任务状态变更与完成、习惯打卡与撤销、"
        "执行开始与结束、计划安排事件，包含对象名称和发生时间。最多 20 条并给出总条数。"
        "用户询问最近发生了什么、成长轨迹、某段时间完成了哪些事时使用。"
    ),
    # 传入权限（permission），值为固定文字 'statistics:report:read'。
    permission="statistics:report:read",
    # 传入参数模型（args_model），值为统计当前页参数（StatisticsPageArgs）。
    args_model=StatisticsPageArgs,
    # 传入handler，值为方案运行成长业务成长时间线（run_growth_timeline）。
    handler=run_growth_timeline,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯统计业务工具（HABIT_STATISTICS_TOOL）。
HABIT_STATISTICS_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_habit_statistics'。
    name="get_habit_statistics",
    # 传入显示名称（label），值为固定文字 '习惯统计'。
    label="习惯统计",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按周期统计习惯完成情况：总完成/应做与完成率，并按习惯或历史分类归属分组返回每组'，完成上面尚未结束的表达式。
        "按周期统计习惯完成情况：总完成/应做与完成率，并按习惯或历史分类归属分组返回每组"
        "完成数、应做数与完成率，最多 20 组。习惯统计独立于目标/阶段/任务筛选。"
        "用户询问某段时间习惯完成率、哪个习惯完成得好、分类维度习惯统计时使用。"
    ),
    # 传入权限（permission），值为固定文字 'statistics:report:read'。
    permission="statistics:report:read",
    # 传入参数模型（args_model），值为习惯统计参数（HabitStatisticsArgs）。
    args_model=HabitStatisticsArgs,
    # 传入handler，值为方案运行习惯统计（run_habit_statistics）。
    handler=run_habit_statistics,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 对象统计业务工具（OBJECT_STATISTICS_TOOL）。
OBJECT_STATISTICS_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_object_statistics'。
    name="get_object_statistics",
    # 传入显示名称（label），值为固定文字 '对象状态分布'。
    label="对象状态分布",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询当前有效未归档对象的状态分布：目标、阶段、任务各自按状态的数量。'，完成上面尚未结束的表达式。
        "查询当前有效未归档对象的状态分布：目标、阶段、任务各自按状态的数量。"
        "这是当前状态快照，不等于历史完成动作。用户询问现在有多少未完成/进行中的目标或任务时使用。"
    ),
    # 传入权限（permission），值为固定文字 'statistics:report:read'。
    permission="statistics:report:read",
    # 传入参数模型（args_model），值为统计参数（StatisticsArgs）。
    args_model=StatisticsArgs,
    # 传入handler，值为方案运行对象统计（run_object_statistics）。
    handler=run_object_statistics,
)
