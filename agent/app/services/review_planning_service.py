# 把已完成的复盘转换成调整草案，复用原任务并等待用户确认。
# 已完成复盘到可确认调整方案的桥接；原任务只改安排，提交仍须单独确认。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 sqlalchemy 引入 选择（select）。
from sqlalchemy import select

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 规划工作流方案运行（WorkflowRun）。
from app.db.models import WorkflowRun

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、本轮对话数据仓库（turn_repository）、规划工作流数据仓库（workflow_repository）。
from app.repositories import conversation_repository, turn_repository, workflow_repository

# 从 app.services 引入 对话规划（conversation_planning）、复盘业务服务（review_service）、规划工作流业务服务（workflow_service）。
from app.services import conversation_planning, review_service, workflow_service


# 异步函数 draft：调用 draft 完成草案处理；接收 Agent 数据库访问器（db）、上下文（context）、客户端（client）、复盘报告编号（report_id）、对话编号（conversation_id）、消息（message）、slots。
async def draft(db, context, client, *, report_id: int, conversation_id: int, message: str, slots):
    # 调用 context.require 完成require处理。
    context.require("agent:review:read", "agent:workflow:manage", "agent:workflow:read")
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 复盘报告（report）。
    report = await review_service.get(db, int(context.user_id), report_id)
    # 检查条件 report.status != 'READY' 是否成立；成立时执行缩进的分支。
    if report.status != "READY":
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "REVIEW_NOT_READY", "复盘尚未完成，请先等待或重试复盘")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, int(context.user_id), conversation_id)
        # 检查条件 await turn_repository.count_running(session, conversation_id) 是否成立；成立时执行缩进的分支。
        if await turn_repository.count_running(session, conversation_id):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "PLAN_CONVERSATION_RUNNING", "对话正在整理，请完成后再根据复盘调整")
        # 把等待执行“查找 当前有效方案运行（active_run）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
        active = await workflow_repository.find_active_run(session, conversation_id)
        # 检查条件 active and (active.graph_state or {}).get('sourceReviewId') != str(report_id) 是否成立；成立时执行缩进的分支。
        if active and (active.graph_state or {}).get("sourceReviewId") != str(report_id):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "WORKFLOW_ACTIVE_EXISTS", "当前还有待处理方案，请先接受或暂不接受，再整理复盘调整")
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 上一次（previous）。
        previous = await session.scalar(select(WorkflowRun).where(
            # 继续传入或组合条件判断的结果：WorkflowRun.user_id == int(context.user_id), WorkflowRun.conversation_id == conversation_id，完成上面尚未结束的表达式。
            WorkflowRun.user_id == int(context.user_id), WorkflowRun.conversation_id == conversation_id,
            # 继续传入或组合条件判断的结果：WorkflowRun.graph_state["sourceReviewId"].astext == str(report_id)，完成上面尚未结束的表达式。
            WorkflowRun.graph_state["sourceReviewId"].astext == str(report_id),
            # 继续传入或组合调用 WorkflowRun.status.in_ 完成in处理，完成上面尚未结束的表达式。
            WorkflowRun.status.in_(("WAITING_CONFIRM", "CONFIRMED", "EXECUTING", "SUCCEEDED"))
        # 继续传入或组合调用 ).order_by 完成顺序按处理，完成上面尚未结束的表达式。
        ).order_by(WorkflowRun.id.desc()).limit(1))
        # 把条件 previous is not None 是否成立时使用上一次（previous）的编号（id）属性，否则使用空值，表示当前还没有对应内容保存到 上一次编号（previous_id）。
        previous_id = previous.id if previous is not None else None
    # 检查条件 previous_id 是否成立；成立时执行缩进的分支。
    if previous_id:
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await workflow_service.detail(db, user_id=int(context.user_id), run_id=previous_id)

    # 异步函数 silent：调用 silent 完成silent处理；接收 事件（_event）、数据（_data）。
    async def silent(_event, _data):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“把对象编码为 JSON 文字”得到的结果保存到 arguments。
    arguments = json.dumps({"action": "draft", "objective": "根据实际执行反馈调整后续任务安排",
        # 填写汇总（summary）字段，值为表达式 message, "intent": "WEEKLY_PLAN", "questions": [], "reviewId": report_id}, ensure_ascii=False) 的结果。
        "summary": message, "intent": "WEEKLY_PLAN", "questions": [], "reviewId": report_id}, ensure_ascii=False)
    # 异步进入执行“调用 asyncio.timeout_at 完成超时at处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
    async with asyncio.timeout_at(context.deadline):
        # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
        result = await conversation_planning.execute(db, context, client, conversation_id=conversation_id,
            # 传入本轮对话编号（turn_id），值为表达式 None, arguments=arguments, messages=[{"role": "user", "content": message}] 的结果。
            turn_id=None, arguments=arguments, messages=[{"role": "user", "content": message}],
            # 传入汇总（summary），值为表达式 "", slots=slots, emit=silent) 的结果。
            summary="", slots=slots, emit=silent)
    # 检查条件 result.is_error 是否成立；成立时执行缩进的分支。
    if result.is_error:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "REVIEW_ADJUSTMENT_FAILED", result.summary)
    # 把等待执行“调用 conversation_planning.current_detail 完成当前详情处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_planning.current_detail(db, int(context.user_id), conversation_id)
