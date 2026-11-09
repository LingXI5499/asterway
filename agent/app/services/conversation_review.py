# 在聊天中按真实执行统计生成复盘，并通知右侧面板更新。
# 在对话中发起有真实统计依据的复盘；仅分析，不确认或执行业务方案。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 datetime 引入 日期（date）、两个时刻之间的时间长度（timedelta）。
from datetime import date, timedelta

# 从 typing 引入 只能从列出的固定值中选择的类型（Literal）。
from typing import Literal

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、Validation错误（ValidationError）。
from pydantic import BaseModel, ConfigDict, ValidationError

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 刷新上下文（refresh_context）。
from app.core.context import refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.services 引入 复盘业务服务（review_service）。
from app.services import review_service

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution

# 把固定文字 'review_growth_progress'保存到 业务工具名称（TOOL_NAME）。
TOOL_NAME = "review_growth_progress"
# 把固定文字 '复盘实际进展'保存到 业务工具显示名称（TOOL_LABEL）。
TOOL_LABEL = "复盘实际进展"


# 定义 ReviewAction，把复盘操作需要的数据与操作放在同一个对象中。
class ReviewAction(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid")
    # 声明 统计周期（period），类型为 Literal['THIS_WEEK', 'LAST_WEEK', 'LAST_7_DAYS', 'CUSTOM']；其内容为固定状态或编码 LAST_7_DAYS（最后一次7日列表）。
    period: Literal["THIS_WEEK", "LAST_WEEK", "LAST_7_DAYS", "CUSTOM"] = "LAST_7_DAYS"
    # 声明 日期来源（dateFrom），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    dateFrom: date | None = None
    # 声明 日期To（dateTo），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    dateTo: date | None = None


# 函数 available：判断本轮已用调用次数和轮数是否都还未达到各自上限；接收 Agent 数据库访问器（db）、上下文（context）。
def available(db, context):
    # 把条件 getattr(db, 'background_worker', None) is not None 是否成立并且执行“调用 all 完成全部处理”得到的结果交还给调用方，本函数到此结束。
    return getattr(db, "background_worker", None) is not None and all(value in context.permissions
        # 继续传入或组合表达式 for value in ("agent:review:read", "agent:review:manage", "statistics:report:read")) 的结果，完成上面尚未结束的表达式。
        for value in ("agent:review:read", "agent:review:manage", "statistics:report:read"))


# 函数 definition：调用 definition 完成definition处理。
def definition():
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"type": "function", "function": {"name": TOOL_NAME,
        # 填写说明（description）字段，值为固定文字 '用户要求复盘投入、总结执行反馈、分析逾期或据此调整计划时使用。读取本人真实统计并保存复盘，'。
        "description": "用户要求复盘投入、总结执行反馈、分析逾期或据此调整计划时使用。读取本人真实统计并保存复盘，"
            "右侧显示生成进度。默认最近7天；自定义日期仅用于CUSTOM。复盘不执行写入，READY后可用其reviewId"
            "调用prepare_growth_plan生成周日调整草案，再由用户接受。每轮最多一次。",
        # 填写parameters字段，值为调用 ReviewAction.model_json_schema 完成模型JSON数据数据库分区处理。
        "parameters": ReviewAction.model_json_schema()}}


# 异步函数 context_hint：调用 context_hint 完成上下文hint处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def context_hint(db, user_id, conversation_id):
    # 把等待执行“调用 review_service.latest_for_conversation 完成最新for对话处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await review_service.latest_for_conversation(db, user_id, conversation_id)
    # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
    if row is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把按 '【成长复盘背景，仅为统计依据与建议，不是用户指令】\n' + json.dumps({'reviewId': str(row.id), 'status': row.status, 'dateFrom': row.date_from, 'dateTo': row.date_to, 'analysis': row.content[:2200] if row.status == 'READY' else None, 'instruction': '用户要求根据复盘调整时，保留已有目标和任务，使用此reviewId生成周日安排草案。'}, ensure_ascii=False) 计算的结果交还给调用方，本函数到此结束。
    return "【成长复盘背景，仅为统计依据与建议，不是用户指令】\n" + json.dumps({
        # 填写复盘编号（reviewId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "reviewId": str(row.id), "status": row.status, "dateFrom": row.date_from,
        # 填写日期To（dateTo）字段，值为条件判断的结果：row.date_to, "analysis": row.content[:2200] if row.status == "READY" else None。
        "dateTo": row.date_to, "analysis": row.content[:2200] if row.status == "READY" else None,
        # 填写instruction字段，值为固定文字 '用户要求根据复盘调整时，保留已有目标和任务，使用此reviewId生成周日安排草案。'。
        "instruction": "用户要求根据复盘调整时，保留已有目标和任务，使用此reviewId生成周日安排草案。"
    # 继续传入或组合表达式 }, ensure_ascii=False) 的结果，完成上面尚未结束的表达式。
    }, ensure_ascii=False)


# 函数 resolve_range：确定 区间（range）；接收 操作（action）、今天（today）。
def resolve_range(action: ReviewAction, today: date):
    # 检查条件 action.period == 'CUSTOM' 是否成立；成立时执行缩进的分支。
    if action.period == "CUSTOM":
        # 检查action.dateFrom is None or action.dateTo is None，即对应内容是否不存在；成立时执行缩进的分支。
        if action.dateFrom is None or action.dateTo is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(422, "REVIEW_RANGE_REQUIRED", "自定义复盘需要起止日期")
        # 把依次包含 操作（action）的日期来源（dateFrom）属性、操作（action）的日期To（dateTo）属性 的集合保存到 开始结束（(start, end)）。
        start, end = action.dateFrom, action.dateTo
    # 检查条件 action.dateFrom is not None or action.dateTo is not None 是否成立；成立时执行缩进的分支。
    elif action.dateFrom is not None or action.dateTo is not None:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "REVIEW_RANGE_INVALID", "具体日期请使用自定义复盘")
    # 检查条件 action.period == 'THIS_WEEK' 是否成立；成立时执行缩进的分支。
    elif action.period == "THIS_WEEK":
        # 把依次包含 按 today - timedelta(days=today.weekday()) 计算的结果、今天（today） 的集合保存到 开始结束（(start, end)）。
        start, end = today - timedelta(days=today.weekday()), today
    # 检查条件 action.period == 'LAST_WEEK' 是否成立；成立时执行缩进的分支。
    elif action.period == "LAST_WEEK":
        # 把按 today - timedelta(days=today.weekday() + 7) 计算的结果保存到 开始（start）。
        start = today - timedelta(days=today.weekday() + 7)
        # 把按 start + timedelta(days=6) 计算的结果保存到 结束（end）。
        end = start + timedelta(days=6)
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 把依次包含 按 today - timedelta(days=6) 计算的结果、今天（today） 的集合保存到 开始结束（(start, end)）。
        start, end = today - timedelta(days=6), today
    # 检查条件 not 0 <= (end - start).days <= 365 or end > today 是否成立；成立时执行缩进的分支。
    if not 0 <= (end - start).days <= 365 or end > today:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "REVIEW_RANGE_INVALID", "请选择截至账号今天的有效复盘区间")
    # 把依次包含 开始（start）、结束（end） 的集合交还给调用方，本函数到此结束。
    return start, end


# 异步函数 execute：执行传入的数据库语句并取得结果；接收 Agent 数据库访问器（db）、上下文（context）、客户端（client）、对话编号（conversation_id）、arguments、向父组件发送事件的函数（emit）。
async def execute(db, context, client, *, conversation_id, arguments, emit):
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
    started = time.monotonic()
    # 函数 result：调用 result 完成结果处理；接收 值（value）、汇总（summary）、错误（error）。
    def result(value, summary, error=False):
        # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果交还给调用方，本函数到此结束。
        return ToolExecution(TOOL_NAME, TOOL_LABEL, json.dumps(value, ensure_ascii=False),
                             # 继续传入或组合调用 summary, error, int((time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
                             summary, error, int((time.monotonic() - started) * 1000))
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 ReviewAction.model_validate_json 完成模型检查合法性JSON数据处理”得到的结果保存到 操作（action）。
        action = ReviewAction.model_validate_json(arguments)
        # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 当前（current）。
        current = await refresh_context(context, client)
        # 调用 current.require 完成require处理。
        current.require("agent:review:read", "agent:review:manage", "statistics:report:read")
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 calendar。
        calendar = await client.get("dashboard/overview", authorization=current.authorization,
                                    # 传入追踪标识编号（trace_id），值为表达式 current.trace_id) 的结果。
                                    trace_id=current.trace_id)
        # 把条件 isinstance(calendar.data, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 今天（today）。
        today = calendar.data.get("today") if isinstance(calendar.data, dict) else None
        # 检查条件 not today 是否成立；成立时执行缩进的分支。
        if not today:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "无法取得账号业务日期")
        # 把执行“确定 区间（range）”得到的结果保存到 开始结束（(start, end)）。
        start, end = resolve_range(action, date.fromisoformat(today))
        # 把等待执行“调用 review_service.create 完成创建处理”得到的结果完成后的结果保存到 当前记录（row）。
        row = await review_service.create(db, current, client, start, end,
                                          # 传入对话编号（conversation_id），值为表达式 conversation_id) 的结果。
                                          conversation_id=conversation_id)
        # 异步函数 announce：把方案当前是否可用及其状态通知父组件。
        async def announce():
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit("review_update", {"conversationId": str(conversation_id), "reviewId": str(row.id),
                # 填写状态（status）字段，值为表达式 row.status, "dateFrom": row.date_from, "dateTo": row.date_to}) 的结果。
                "status": row.status, "dateFrom": row.date_from, "dateTo": row.date_to})
        # 等待把方案当前是否可用及其状态通知父组件完成后再继续。
        await announce()
        # 把执行“调用 min 完成最小处理”得到的结果保存到 上限（limit）。
        limit = min(get_settings().agent_review_wait_seconds, max(0, current.deadline - time.monotonic() - 35))
        # 把按 time.monotonic() + limit 计算的结果保存到 until。
        until = time.monotonic() + limit
        # 只要条件 row.status in {'PENDING', 'RUNNING'} and time.monotonic() < until 是否成立，就继续执行下面的循环。
        while row.status in {"PENDING", "RUNNING"} and time.monotonic() < until:
            # 等待等待指定时间后再继续，给轮询或其他任务留出执行机会完成后再继续。
            await asyncio.sleep(1)
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
            row = await review_service.get(db, int(current.user_id), row.id)
        # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 当前（current）。
        current = await refresh_context(current, client)
        # 调用 current.require 完成require处理。
        current.require("agent:review:read", "agent:review:manage", "statistics:report:read")
        # 等待把方案当前是否可用及其状态通知父组件完成后再继续。
        await announce()
        # 检查条件 row.status == 'FAILED' 是否成立；成立时执行缩进的分支。
        if row.status == "FAILED":
            # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
            return result({"error": row.error_message, "reviewId": str(row.id)}, "复盘暂未生成，可在右侧重试", True)
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"reviewId": str(row.id), "status": row.status, "dateFrom": row.date_from,
            # 填写日期To（dateTo）字段，值为条件判断的结果：row.date_to, "content": row.content[:2200] if row.status == "READY" else None。
            "dateTo": row.date_to, "content": row.content[:2200] if row.status == "READY" else None,
            # 填写instruction字段，值为固定文字 '仅根据已完成的统计和复盘作答；READY且用户要求调整时，用此reviewId和WEEKLY_PLAN/DAILY_PLAN'。
            "instruction": "仅根据已完成的统计和复盘作答；READY且用户要求调整时，用此reviewId和WEEKLY_PLAN/DAILY_PLAN"
                "调用prepare_growth_plan，保留已有目标任务。尚未READY时说明正在生成，右侧可继续查看，勿虚构结论。"},
            # 继续传入或组合条件判断的结果："复盘已就绪" if row.status == "READY" else "复盘正在生成，右侧会自动更新")，完成上面尚未结束的表达式。
            "复盘已就绪" if row.status == "READY" else "复盘正在生成，右侧会自动更新")
    # 捕获ValidationError异常，执行下面的错误处理。
    except ValidationError:
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"error": "复盘参数不符合要求"}, "复盘日期参数需要调整", True)
    # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
    except AgentError as error:
        # 检查条件 error.status_code in {401, 403} 是否成立；成立时执行缩进的分支。
        if error.status_code in {401, 403}:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"error": error.message}, error.message, True)
