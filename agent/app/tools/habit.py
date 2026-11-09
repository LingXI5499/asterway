# 封装习惯查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 习惯只读工具：习惯列表/详情、分类、今日应做、打卡历史与规则修订。
# 
# 只读取 Java 习惯事实；不打卡、不撤销打卡、不创建/修改/暂停/结束习惯。
# 今日口径使用 Java 账号有效时区。

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
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 ACTIVE（页面是否仍处于活动状态）、固定状态或编码 PAUSED（已暂停）、固定状态或编码 ENDED（已结束） 的集合取出的内容保存到 习惯状态（HabitStatus）。
HabitStatus = Literal["ACTIVE", "PAUSED", "ENDED"]
# 把按字段名保存多个值的字典保存到 状态名称对应表（_STATUS_LABELS）。
_STATUS_LABELS = {"ACTIVE": "进行中", "PAUSED": "已暂停", "ENDED": "已结束"}
# 把按字段名保存多个值的字典保存到 CYCLE名称对应表（_CYCLE_LABELS）。
_CYCLE_LABELS = {"DAILY": "每天", "WEEKLY_DAYS": "指定星期"}
# 把按字段名保存多个值的字典保存到 操作名称对应表（_ACTION_LABELS）。
_ACTION_LABELS = {
    # 填写创建时（CREATED）字段，值为固定文字 '创建'。
    "CREATED": "创建",
    # 填写EDITED字段，值为固定文字 '编辑规则'。
    "EDITED": "编辑规则",
    # 填写已暂停（PAUSED）字段，值为固定文字 '暂停'。
    "PAUSED": "暂停",
    # 填写RESUMED字段，值为固定文字 '恢复'。
    "RESUMED": "恢复",
    # 填写已结束（ENDED）字段，值为固定文字 '结束'。
    "ENDED": "结束",
    # 填写REOPENED字段，值为固定文字 '重新开启'。
    "REOPENED": "重新开启",
    # 填写删除标记（DELETED）字段，值为固定文字 '删除'。
    "DELETED": "删除",
    # 填写系统已暂停（SYSTEM_PAUSED）字段，值为固定文字 '系统暂停（来源目标归档）'。
    "SYSTEM_PAUSED": "系统暂停（来源目标归档）",
}
# 把依次包含 固定文字 '周一'、固定文字 '周二'、固定文字 '周三'、固定文字 '周四'、固定文字 '周五'等内容 的集合保存到 WEEKDAYNAMES（_WEEKDAY_NAMES）。
_WEEKDAY_NAMES = ("周一", "周二", "周三", "周四", "周五", "周六", "周日")


# 定义 HabitListArgs，把习惯列表参数需要的数据与操作放在同一个对象中。
class HabitListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 状态（status），类型为 HabitStatus | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    status: HabitStatus | None = Field(
        # 传入默认值（default），值为表达式 None, description="习惯状态：ACTIVE 进行中、PAUSED 已暂停、ENDED 已结束" 的结果。
        default=None, description="习惯状态：ACTIVE 进行中、PAUSED 已暂停、ENDED 已结束"
    )
    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="习惯名称关键字")
    # 声明 分类编号（categoryId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    categoryId: int | None = Field(default=None, description="只看某个分类下的习惯")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 HabitDetailArgs，把习惯详情参数需要的数据与操作放在同一个对象中。
class HabitDetailArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 习惯编号（habitId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")


# 定义 HabitCategoryListArgs，把习惯分类列表参数需要的数据与操作放在同一个对象中。
class HabitCategoryListArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 keyword，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    keyword: str | None = Field(default=None, max_length=50, description="分类名称关键字")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 TodayHabitArgs，把今天习惯参数需要的数据与操作放在同一个对象中。
class TodayHabitArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 定义 HabitCheckinArgs，把习惯打卡参数需要的数据与操作放在同一个对象中。
class HabitCheckinArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 习惯编号（habitId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")
    # 声明 日期来源（dateFrom），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateFrom: str | None = Field(default=None, description="业务日期下限 YYYY-MM-DD，可省略")
    # 声明 日期To（dateTo），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dateTo: str | None = Field(default=None, description="业务日期上限 YYYY-MM-DD，可省略")
    # 声明 已取消（cancelled），类型为 bool | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    cancelled: bool | None = Field(
        # 传入默认值（default），值为表达式 None, description="按是否已撤销筛选：true 已撤销、false 有效打卡；不传为全部" 的结果。
        default=None, description="按是否已撤销筛选：true 已撤销、false 有效打卡；不传为全部"
    )
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_dates：检查 dates。
    def validate_dates(self) -> "HabitCheckinArgs":
        # 检查self.dateFrom is None) != (self.dateTo is None，即对应内容是否不存在；成立时执行缩进的分支。
        if (self.dateFrom is None) != (self.dateTo is None):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("dateFrom 与 dateTo 必须同时提供或同时省略")
        # 从依次包含 当前实例（self）的日期来源（dateFrom）属性、当前实例（self）的日期To（dateTo）属性 的集合依次取出值（value），每项执行一次下面的处理。
        for value in (self.dateFrom, self.dateTo):
            # 检查条件 value is not None and (not _DATE.fullmatch(value)) 是否成立；成立时执行缩进的分支。
            if value is not None and not _DATE.fullmatch(value):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期必须是 YYYY-MM-DD")
        # 检查条件 self.dateFrom and self.dateTo 是否成立；成立时执行缩进的分支。
        if self.dateFrom and self.dateTo:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 检查条件 date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo) 是否成立；成立时执行缩进的分支。
                if date.fromisoformat(self.dateFrom) > date.fromisoformat(self.dateTo):
                    # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                    raise ValueError("开始日期不能晚于结束日期")
            # 捕获ValueError异常，执行下面的错误处理，异常对象保存为 error。
            except ValueError as error:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日期格式不正确") from error
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 定义 HabitRevisionArgs，把习惯修订版本参数需要的数据与操作放在同一个对象中。
class HabitRevisionArgs(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)

    # 声明 习惯编号（habitId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    habitId: int = Field(description="习惯 ID，来自习惯列表或今日习惯")
    # 声明 上限（limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    limit: int = Field(default=10, ge=1, le=20, description="最多返回条数，1 到 20")


# 函数 _weekdays：调用 _weekdays 完成weekdays处理；接收 值（value）。
def _weekdays(value: Any) -> list[int]:
    # 检查条件 isinstance(value, list) 是否成立；成立时执行缩进的分支。
    if isinstance(value, list):
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 日列表（days）。
        days = [
            # 继续传入或组合日（day），完成上面尚未结束的表达式。
            day
            # 继续传入或组合表达式 for day in value 的结果，完成上面尚未结束的表达式。
            for day in value
            # 继续传入或组合条件判断的结果：if isinstance(day, int) and not isinstance(day, bool) and 1 <= day <= 7，完成上面尚未结束的表达式。
            if isinstance(day, int) and not isinstance(day, bool) and 1 <= day <= 7
        ]
        # 把日列表（days）交还给调用方，本函数到此结束。
        return days
    # 把值（value）保存到 mask。
    mask = value
    # 检查条件 isinstance(mask, int) and (not isinstance(mask, bool)) 是否成立；成立时执行缩进的分支。
    if isinstance(mask, int) and not isinstance(mask, bool):
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
        return [day for day in range(1, 8) if mask & (1 << (day - 1))]
    # 把空列表或集合交还给调用方，本函数到此结束。
    return []


# 函数 _weekdays_text：调用 _weekdays_text 完成weekdays文字处理；接收 日列表（days）。
def _weekdays_text(days: list[int]) -> str | None:
    # 检查条件 not days 是否成立；成立时执行缩进的分支。
    if not days:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“用指定分隔符把多段文字合起来”得到的结果交还给调用方，本函数到此结束。
    return "、".join(_WEEKDAY_NAMES[day - 1] for day in days)


# 函数 _labeled：调用 _labeled 完成labeled处理；接收 值（value）、名称对应表（labels）。
def _labeled(value: Any, labels: dict[str, str]) -> str | None:
    # 检查条件 isinstance(value, str) 是否成立；成立时执行缩进的分支。
    if isinstance(value, str):
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果交还给调用方，本函数到此结束。
        return labels.get(value, value)
    # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return None


# 函数 _habit_item：调用 _habit_item 完成习惯单条数据处理；接收 当前记录（row）、详情（detail）。
def _habit_item(row: Any, *, detail: bool) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None or (not isinstance(row.get('name'), str))，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“调用 _weekdays 完成weekdays处理”得到的结果保存到 日列表（days）。
    days = _weekdays(row.get("weekdays") if "weekdays" in row else row.get("weekdayMask"))
    # 声明 单条数据（item），类型为 dict[str, Any]；其内容为逐项处理数据，并按这里的条件筛选或转换后收集结果。
    item: dict[str, Any] = {
        # 填写键（key）字段，值为表达式 row[key] 的结果。
        key: row[key]
        # 继续传入或组合表达式 for key in ( 的结果，完成上面尚未结束的表达式。
        for key in (
            # 继续传入或组合固定文字 'id'，完成上面尚未结束的表达式。
            "id",
            # 继续传入或组合固定文字 'status'，完成上面尚未结束的表达式。
            "status",
            # 继续传入或组合固定文字 'revisionNo'，完成上面尚未结束的表达式。
            "revisionNo",
            # 继续传入或组合固定文字 'name'，完成上面尚未结束的表达式。
            "name",
            # 继续传入或组合固定文字 'categoryId'，完成上面尚未结束的表达式。
            "categoryId",
            # 继续传入或组合固定文字 'categoryName'，完成上面尚未结束的表达式。
            "categoryName",
            # 继续传入或组合固定文字 'categoryDeleted'，完成上面尚未结束的表达式。
            "categoryDeleted",
            # 继续传入或组合固定文字 'cycleType'，完成上面尚未结束的表达式。
            "cycleType",
            # 继续传入或组合固定文字 'startDate'，完成上面尚未结束的表达式。
            "startDate",
            # 继续传入或组合固定文字 'expectedTime'，完成上面尚未结束的表达式。
            "expectedTime",
            # 继续传入或组合固定文字 'checkedToday'，完成上面尚未结束的表达式。
            "checkedToday",
            # 继续传入或组合固定文字 'historyLockedThrough'，完成上面尚未结束的表达式。
            "historyLockedThrough",
            # 继续传入或组合固定文字 'endedAt'，完成上面尚未结束的表达式。
            "endedAt",
            # 继续传入或组合固定文字 'version'，完成上面尚未结束的表达式。
            "version",
        )
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if row.get(key) is not None
    }
    # 把执行“调用 _labeled 完成labeled处理”得到的结果保存到 单条数据状态显示名称（item['statusLabel']）。
    item["statusLabel"] = _labeled(row.get("status"), _STATUS_LABELS)
    # 把执行“调用 _labeled 完成labeled处理”得到的结果保存到 单条数据cycle类型显示名称（item['cycleTypeLabel']）。
    item["cycleTypeLabel"] = _labeled(row.get("cycleType"), _CYCLE_LABELS)
    # 把日列表（days）保存到 单条数据weekdays（item['weekdays']）。
    item["weekdays"] = days
    # 把执行“调用 _weekdays_text 完成weekdays文字处理”得到的结果保存到 单条数据weekdays文字（item['weekdaysText']）。
    item["weekdaysText"] = _weekdays_text(days)
    # 检查条件 detail 是否成立；成立时执行缩进的分支。
    if detail:
        # 把执行“调用 shared.text 完成文字处理”得到的结果保存到 单条数据说明（item['description']）。
        item["description"] = shared.text(row.get("description"), 300)
        # 把执行“调用 shared.text 完成文字处理”得到的结果保存到 单条数据完成情况判断标准（item['completionCriteria']）。
        item["completionCriteria"] = shared.text(row.get("completionCriteria"), 300)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {key: value for key, value in item.items() if value is not None}


# 函数 _today_item：调用 _today_item 完成今天单条数据处理；接收 当前记录（row）。
def _today_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('habitId') is None or (not isinstance(row.get('name'), str))，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("habitId") is None or not isinstance(
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        row.get("name"), str
    ):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 已完成（done）。
    done = row.get("doneCount")
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 应完成（due）。
    due = row.get("dueCount")
    # 把执行“调用 bool 完成bool处理”得到的结果或者执行“判断是否为 instance”得到的结果并且条件 done > 0 是否成立保存到 已核对（checked）。
    checked = bool(row.get("checkinId")) or (isinstance(done, int) and done > 0)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写习惯编号（habitId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "habitId": row.get("habitId"),
            # 填写名称（name）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "name": row.get("name"),
            # 填写状态（status）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "status": row.get("status"),
            # 填写状态显示名称（statusLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            # 填写预计时间（expectedTime）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "expectedTime": row.get("expectedTime"),
            # 填写分类编号（categoryId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "categoryId": row.get("categoryId"),
            # 填写已核对（checked）字段，值为已核对（checked）。
            "checked": checked,
            # 填写已完成数量（doneCount）字段，值为已完成（done）。
            "doneCount": done,
            # 填写应完成数量（dueCount）字段，值为应完成（due）。
            "dueCount": due,
            # 填写是否可以打卡（canCheckin）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "canCheckin": row.get("canCheckin"),
            # 填写是否可以取消（canCancel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "canCancel": row.get("canCancel"),
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 函数 _checkin_item：调用 _checkin_item 完成打卡单条数据处理；接收 当前记录（row）。
def _checkin_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("id") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把条件 row.get('cancelledAt') is not None 是否成立保存到 已取消（cancelled）。
    cancelled = row.get("cancelledAt") is not None
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写习惯编号（habitId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "habitId": row.get("habitId"),
            # 填写名称确认时的数据快照（nameSnapshot）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "nameSnapshot": row.get("nameSnapshot"),
            # 填写分类编号（categoryId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "categoryId": row.get("categoryId"),
            # 填写按用户时区计算的业务日期（businessDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "businessDate": row.get("businessDate"),
            # 填写用户使用的时区（timeZone）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "timeZone": row.get("timeZone"),
            # 填写已核对At（checkedAt）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "checkedAt": row.get("checkedAt"),
            # 填写已取消At（cancelledAt）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "cancelledAt": row.get("cancelledAt"),
            # 填写已取消（cancelled）字段，值为已取消（cancelled）。
            "cancelled": cancelled,
            # 填写已取消显示名称（cancelledLabel）字段，值为固定文字 '已撤销" if cancelled else "有效'。
            "cancelledLabel": "已撤销" if cancelled else "有效",
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 函数 _revision_item：调用 _revision_item 完成修订版本单条数据处理；接收 当前记录（row）。
def _revision_item(row: Any) -> dict[str, Any] | None:
    # 检查not isinstance(row, dict) or row.get('revisionNo') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(row, dict) or row.get("revisionNo") is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“调用 _weekdays 完成weekdays处理”得到的结果保存到 日列表（days）。
    days = _weekdays(row.get("weekdays") if "weekdays" in row else row.get("weekdayMask"))
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return {
        # 填写键（key）字段，值为值（value）。
        key: value
        # 继续传入或组合表达式 for key, value in { 的结果，完成上面尚未结束的表达式。
        for key, value in {
            # 填写修订版本No（revisionNo）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "revisionNo": row.get("revisionNo"),
            # 填写操作类型（actionType）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "actionType": row.get("actionType"),
            # 填写操作类型显示名称（actionTypeLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "actionTypeLabel": _labeled(row.get("actionType"), _ACTION_LABELS),
            # 填写名称（name）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "name": row.get("name"),
            # 填写说明（description）字段，值为调用 shared.text 完成文字处理。
            "description": shared.text(row.get("description"), 200),
            # 填写完成情况判断标准（completionCriteria）字段，值为调用 shared.text 完成文字处理。
            "completionCriteria": shared.text(row.get("completionCriteria"), 200),
            # 填写分类编号（categoryId）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "categoryId": row.get("categoryId"),
            # 填写cycle类型（cycleType）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "cycleType": row.get("cycleType"),
            # 填写cycle类型显示名称（cycleTypeLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "cycleTypeLabel": _labeled(row.get("cycleType"), _CYCLE_LABELS),
            # 填写weekdays字段，值为日列表（days）。
            "weekdays": days,
            # 填写weekdays文字（weekdaysText）字段，值为调用 _weekdays_text 完成weekdays文字处理。
            "weekdaysText": _weekdays_text(days),
            # 填写开始日期（startDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "startDate": row.get("startDate"),
            # 填写预计时间（expectedTime）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "expectedTime": row.get("expectedTime"),
            # 填写状态（status）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "status": row.get("status"),
            # 填写状态显示名称（statusLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "statusLabel": _labeled(row.get("status"), _STATUS_LABELS),
            # 填写删除标记（deleted）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "deleted": row.get("deleted"),
            # 填写生效日期（effectiveDate）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "effectiveDate": row.get("effectiveDate"),
            # 填写已修改At（changedAt）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "changedAt": row.get("changedAt"),
            # 填写用户使用的时区（timeZone）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
            "timeZone": row.get("timeZone"),
        # 继续传入或组合调用 }.items 完成数据列表处理，完成上面尚未结束的表达式。
        }.items()
        # 继续传入或组合条件判断的结果：if value is not None，完成上面尚未结束的表达式。
        if value is not None
    }


# 异步函数 run_list_habits：调用 run_list_habits 完成方案运行列表习惯列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_habits(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int | bool]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    # 检查条件 args.status 是否成立；成立时执行缩进的分支。
    if args.status:
        # 把参数（args）的状态（status）属性保存到 参数集合状态（params['status']）。
        params["status"] = args.status
    # 检查条件 args.categoryId is not None 是否成立；成立时执行缩进的分支。
    if args.categoryId is not None:
        # 把参数（args）的分类编号（categoryId）属性保存到 参数集合分类编号（params['categoryId']）。
        params["categoryId"] = args.categoryId
    # 把执行“去掉文字两端的空白”得到的结果保存到 keyword。
    keyword = (args.keyword or "").strip()
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把keyword保存到 参数集合keyword（params['keyword']）。
        params["keyword"] = keyword
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "habits", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "习惯查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("习惯返回结构不正确", "习惯返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [
        # 继续传入或组合条件判断的结果：item for row in data["items"] if (item := _habit_item(row, detail=False)) is not None，完成上面尚未结束的表达式。
        item for row in data["items"] if (item := _habit_item(row, detail=False)) is not None
    ]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"习惯 {payload['returned']} 个（共 {payload['total']} 个）" 的结果。
        summary=f"习惯 {payload['returned']} 个（共 {payload['total']} 个）",
    )


# 异步函数 run_get_habit：调用 run_get_habit 完成方案运行读取习惯处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_get_habit(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitDetailArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitDetailArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, f"habits/{args.habitId}")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "习惯详情查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查not isinstance(data, dict) or data.get('id') is None，即对应内容是否不存在；成立时执行缩进的分支。
    if not isinstance(data, dict) or data.get("id") is None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("习惯详情返回结构不正确", "习惯详情返回异常")
    # 把执行“调用 _habit_item 完成习惯单条数据处理”得到的结果保存到 习惯（habit）。
    habit = _habit_item(data, detail=True)
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"habit": habit}, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为优先使用前面的有效值，没有时采用后面的备用值：f"习惯「{(habit or {}).get('name', '')}」详情"。
        summary=f"习惯「{(habit or {}).get('name', '')}」详情",
    )


# 异步函数 run_list_habit_categories：调用 run_list_habit_categories 完成方案运行列表习惯分类列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_list_habit_categories(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitCategoryListArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitCategoryListArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int] = {"page": 1, "size": args.limit}
    # 把执行“去掉文字两端的空白”得到的结果保存到 keyword。
    keyword = (args.keyword or "").strip()
    # 检查条件 keyword 是否成立；成立时执行缩进的分支。
    if keyword:
        # 把keyword保存到 参数集合keyword（params['keyword']）。
        params["keyword"] = keyword
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(context, client, "habit-categories", params)
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "习惯分类查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("习惯分类返回结构不正确", "习惯分类返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [
        {
            # 填写键（key）字段，值为表达式 row[key] 的结果。
            key: row[key]
            # 继续传入或组合表达式 for key in ("id", "name", "sortOrder", "version") 的结果，完成上面尚未结束的表达式。
            for key in ("id", "name", "sortOrder", "version")
            # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
            if isinstance(row, dict) and row.get(key) is not None
        }
        # 继续传入或组合表达式 for row in data["items"] 的结果，完成上面尚未结束的表达式。
        for row in data["items"]
        # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
        if isinstance(row, dict) and row.get("id") is not None
    ]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"习惯分类 {payload['returned']} 个（共 {payload['total']} 个）" 的结果。
        summary=f"习惯分类 {payload['returned']} 个（共 {payload['total']} 个）",
    )


# 异步函数 run_today_habits：调用 run_today_habits 完成方案运行今天习惯列表处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_today_habits(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: TodayHabitArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: TodayHabitArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, "habits/today", {"page": 1, "size": args.limit}，完成上面尚未结束的表达式。
        context, client, "habits/today", {"page": 1, "size": args.limit}
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "今日习惯查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("今日习惯返回结构不正确", "今日习惯返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [item for row in data["items"] if (item := _today_item(row)) is not None]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把固定文字 '该列表是今天实际应做的习惯；canCheckin/canCancel 仅为页面可操作状态，助手不会打卡或撤销'保存到 提交数据说明（payload['note']）。
    payload["note"] = "该列表是今天实际应做的习惯；canCheckin/canCancel 仅为页面可操作状态，助手不会打卡或撤销"
    # 检查当前用户是否具有需要的权限：'dashboard:board:read' in context.permissions；成立时执行缩进的分支。
    if "dashboard:board:read" in context.permissions:
        # 把等待执行“调用 shared.today 完成今天处理”得到的结果完成后的结果保存到 今天值时间时区（(today_value, time_zone, _)）。
        today_value, time_zone, _ = await shared.today(context, client)
        # 检查条件 today_value 是否成立；成立时执行缩进的分支。
        if today_value:
            # 把今天值（today_value）保存到 提交数据日期（payload['date']）。
            payload["date"] = today_value
        # 检查条件 time_zone 是否成立；成立时执行缩进的分支。
        if time_zone:
            # 把用户使用的时区（time_zone）保存到 提交数据时间时区（payload['timeZone']）。
            payload["timeZone"] = time_zone
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"今日习惯 {payload['returned']} 个（共 {payload['total']} 个）" 的结果。
        summary=f"今日习惯 {payload['returned']} 个（共 {payload['total']} 个）",
    )


# 异步函数 run_habit_checkins：调用 run_habit_checkins 完成方案运行习惯打卡记录处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_habit_checkins(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitCheckinArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitCheckinArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 声明 参数集合（params），类型为 dict[str, str | int | bool]；其内容为按字段名保存多个值的字典。
    params: dict[str, str | int | bool] = {"page": 1, "size": args.limit}
    # 检查条件 args.dateFrom 是否成立；成立时执行缩进的分支。
    if args.dateFrom:
        # 把参数（args）的日期来源（dateFrom）属性保存到 参数集合日期来源（params['dateFrom']）。
        params["dateFrom"] = args.dateFrom
    # 检查条件 args.dateTo 是否成立；成立时执行缩进的分支。
    if args.dateTo:
        # 把参数（args）的日期To（dateTo）属性保存到 参数集合日期To（params['dateTo']）。
        params["dateTo"] = args.dateTo
    # 检查条件 args.cancelled is not None 是否成立；成立时执行缩进的分支。
    if args.cancelled is not None:
        # 把参数（args）的已取消（cancelled）属性保存到 参数集合已取消（params['cancelled']）。
        params["cancelled"] = args.cancelled
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, f"habits/{args.habitId}/checkins", params，完成上面尚未结束的表达式。
        context, client, f"habits/{args.habitId}/checkins", params
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "打卡历史查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("打卡历史返回结构不正确", "打卡历史返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [
        # 继续传入或组合条件判断的结果：item for row in data["items"] if (item := _checkin_item(row)) is not None，完成上面尚未结束的表达式。
        item for row in data["items"] if (item := _checkin_item(row)) is not None
    ]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 str 完成str处理”得到的结果保存到 提交数据习惯编号（payload['habitId']）。
    payload["habitId"] = str(args.habitId)
    # 把固定文字 '已撤销的打卡仍保留为事实，cancelled=true 表示该次打卡已撤销'保存到 提交数据说明（payload['note']）。
    payload["note"] = "已撤销的打卡仍保留为事实，cancelled=true 表示该次打卡已撤销"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"打卡记录 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"打卡记录 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# 异步函数 run_habit_revisions：调用 run_habit_revisions 完成方案运行习惯修订记录处理；接收 上下文（context）、客户端（client）、参数（args）。
async def run_habit_revisions(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient, args: HabitRevisionArgs 的结果。
    context: AgentContext, client: JavaApiClient, args: HabitRevisionArgs
# 继续传入或组合表达式 ) -> ToolOutcome: 的结果，完成上面尚未结束的表达式。
) -> ToolOutcome:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await shared.get(
        # 继续传入或组合按表达式计算得到的值：context, client, f"habits/{args.habitId}/revisions", {"page": 1, "size": args.limit}，完成上面尚未结束的表达式。
        context, client, f"habits/{args.habitId}/revisions", {"page": 1, "size": args.limit}
    )
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error(error_message, "习惯规则历史查询失败")
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("items"), list):
        # 把执行“调用 shared.error 完成错误处理”得到的结果交还给调用方，本函数到此结束。
        return shared.error("习惯规则历史返回结构不正确", "习惯规则历史返回异常")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 数据列表（items）。
    items = [
        # 继续传入或组合条件判断的结果：item for row in data["items"] if (item := _revision_item(row)) is not None，完成上面尚未结束的表达式。
        item for row in data["items"] if (item := _revision_item(row)) is not None
    ]
    # 把执行“调用 shared.page 完成当前页处理”得到的结果保存到 提交数据（payload）。
    payload = shared.page(items, data.get("total"))
    # 把执行“调用 str 完成str处理”得到的结果保存到 提交数据习惯编号（payload['habitId']）。
    payload["habitId"] = str(args.habitId)
    # 把固定文字 '修订按版本倒序返回，是规则与生命周期快照，不覆盖历史打卡事实'保存到 提交数据说明（payload['note']）。
    payload["note"] = "修订按版本倒序返回，是规则与生命周期快照，不覆盖历史打卡事实"
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
        # 传入汇总（summary），值为表达式 f"习惯规则修订 {payload['returned']} 条（共 {payload['total']} 条）" 的结果。
        summary=f"习惯规则修订 {payload['returned']} 条（共 {payload['total']} 条）",
    )


# ---------- 工具声明 ----------


# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯列表业务工具（HABIT_LIST_TOOL）。
HABIT_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_habits'。
    name="list_habits",
    # 传入显示名称（label），值为固定文字 '习惯列表'。
    label="习惯列表",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的习惯列表，可按状态（进行中/已暂停/已结束）、名称关键字和分类筛选，'，完成上面尚未结束的表达式。
        "查询用户本人的习惯列表，可按状态（进行中/已暂停/已结束）、名称关键字和分类筛选，"
        "最多返回 20 条并给出总条数。返回习惯 ID、名称、状态、周期（每天/指定星期）、执行星期、"
        "提示时间、分类、开始日期和今日是否已打卡。用户询问自己有哪些习惯、习惯状态时使用；"
        "完整资料用 get_habit，今日应做用 get_today_habits。"
    ),
    # 传入权限（permission），值为固定文字 'habit:habit:list'。
    permission="habit:habit:list",
    # 传入参数模型（args_model），值为习惯列表参数（HabitListArgs）。
    args_model=HabitListArgs,
    # 传入handler，值为方案运行列表习惯列表（run_list_habits）。
    handler=run_list_habits,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯详情业务工具（HABIT_DETAIL_TOOL）。
HABIT_DETAIL_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_habit'。
    name="get_habit",
    # 传入显示名称（label），值为固定文字 '习惯详情'。
    label="习惯详情",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按习惯 ID 查询用户本人的习惯当前资料：名称、描述、完成标准、状态、周期、执行星期、'，完成上面尚未结束的表达式。
        "按习惯 ID 查询用户本人的习惯当前资料：名称、描述、完成标准、状态、周期、执行星期、"
        "提示时间、分类、开始日期、今日是否已打卡和版本。习惯 ID 必须来自本人习惯列表或今日习惯。"
        "用户询问某个习惯的完整规则时使用。"
    ),
    # 传入权限（permission），值为固定文字 'habit:habit:read'。
    permission="habit:habit:read",
    # 传入参数模型（args_model），值为习惯详情参数（HabitDetailArgs）。
    args_model=HabitDetailArgs,
    # 传入handler，值为方案运行读取习惯（run_get_habit）。
    handler=run_get_habit,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯分类列表业务工具（HABIT_CATEGORY_LIST_TOOL）。
HABIT_CATEGORY_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_habit_categories'。
    name="list_habit_categories",
    # 传入显示名称（label），值为固定文字 '习惯分类'。
    label="习惯分类",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人的习惯分类列表，可按名称关键字筛选，最多返回 20 条并给出总条数。'，完成上面尚未结束的表达式。
        "查询用户本人的习惯分类列表，可按名称关键字筛选，最多返回 20 条并给出总条数。"
        "返回分类 ID、名称和排序。用户询问习惯分类、按分类整理习惯时使用。"
    ),
    # 传入权限（permission），值为固定文字 'habit:category:list'。
    permission="habit:category:list",
    # 传入参数模型（args_model），值为习惯分类列表参数（HabitCategoryListArgs）。
    args_model=HabitCategoryListArgs,
    # 传入handler，值为方案运行列表习惯分类列表（run_list_habit_categories）。
    handler=run_list_habit_categories,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 今天习惯业务工具（TODAY_HABIT_TOOL）。
TODAY_HABIT_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'get_today_habits'。
    name="get_today_habits",
    # 传入显示名称（label），值为固定文字 '今日习惯'。
    label="今日习惯",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '查询用户本人今天实际应做的习惯：名称、状态、提示时间、是否已打卡、完成/应做数量，'，完成上面尚未结束的表达式。
        "查询用户本人今天实际应做的习惯：名称、状态、提示时间、是否已打卡、完成/应做数量，"
        "并给出今天日期和账号时区。最多返回 20 条并给出总条数。只读；不会替用户打卡或撤销打卡。"
        "用户询问今天有哪些习惯要做、哪些还没完成时使用。"
    ),
    # 传入权限（permission），值为固定文字 'habit:habit:list'。
    permission="habit:habit:list",
    # 传入参数模型（args_model），值为今天习惯参数（TodayHabitArgs）。
    args_model=TodayHabitArgs,
    # 传入handler，值为方案运行今天习惯列表（run_today_habits）。
    handler=run_today_habits,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯打卡列表业务工具（HABIT_CHECKIN_LIST_TOOL）。
HABIT_CHECKIN_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_habit_checkins'。
    name="list_habit_checkins",
    # 传入显示名称（label），值为固定文字 '打卡历史'。
    label="打卡历史",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按习惯 ID 查询打卡历史：业务日期、打卡时间、时区、是否已撤销及撤销时间；'，完成上面尚未结束的表达式。
        "按习惯 ID 查询打卡历史：业务日期、打卡时间、时区、是否已撤销及撤销时间；"
        "可按业务日期范围和是否撤销筛选，最多返回 20 条并给出总条数。已撤销打卡仍作为事实保留。"
        "习惯 ID 必须来自本人习惯列表。用户询问某习惯打过几次卡、哪天没打、是否撤销过时使用。"
    ),
    # 传入权限（permission），值为固定文字 'habit:checkin:read'。
    permission="habit:checkin:read",
    # 传入参数模型（args_model），值为习惯打卡参数（HabitCheckinArgs）。
    args_model=HabitCheckinArgs,
    # 传入handler，值为方案运行习惯打卡记录（run_habit_checkins）。
    handler=run_habit_checkins,
)

# 把执行“调用 ToolSpec 完成业务工具Spec处理”得到的结果保存到 习惯修订版本列表业务工具（HABIT_REVISION_LIST_TOOL）。
HABIT_REVISION_LIST_TOOL = ToolSpec(
    # 传入名称（name），值为固定文字 'list_habit_revisions'。
    name="list_habit_revisions",
    # 传入显示名称（label），值为固定文字 '习惯规则历史'。
    label="习惯规则历史",
    # 传入说明（description），值为表达式 ( 的结果。
    description=(
        # 继续传入或组合固定文字 '按习惯 ID 查询规则与生命周期修订历史：修订号、动作（创建/编辑/暂停/恢复/结束/重新开启/删除）、'，完成上面尚未结束的表达式。
        "按习惯 ID 查询规则与生命周期修订历史：修订号、动作（创建/编辑/暂停/恢复/结束/重新开启/删除）、"
        "当时的名称、周期、执行星期、提示时间、状态快照、生效日期与变更时间，按修订号倒序，"
        "最多返回 20 条并给出总条数。用于解释习惯规则如何变化；不覆盖历史打卡事实。"
    ),
    # 传入权限（permission），值为固定文字 'habit:habit:history'。
    permission="habit:habit:history",
    # 传入参数模型（args_model），值为习惯修订版本参数（HabitRevisionArgs）。
    args_model=HabitRevisionArgs,
    # 传入handler，值为方案运行习惯修订记录（run_habit_revisions）。
    handler=run_habit_revisions,
)
