"""已完成复盘到可确认调整方案的桥接；原任务只改安排，提交仍须单独确认。"""

import asyncio
import json

from sqlalchemy import select

from app.core.errors import AgentError
from app.db.models import WorkflowRun
from app.repositories import conversation_repository, turn_repository, workflow_repository
from app.services import conversation_planning, review_service, workflow_service


async def draft(db, context, client, *, report_id: int, conversation_id: int, message: str, slots):
    context.require("agent:review:read", "agent:workflow:manage", "agent:workflow:read")
    report = await review_service.get(db, int(context.user_id), report_id)
    if report.status != "READY":
        raise AgentError(409, "REVIEW_NOT_READY", "复盘尚未完成，请先等待或重试复盘")
    async with db.session() as session:
        await conversation_repository.require_owned(session, int(context.user_id), conversation_id)
        if await turn_repository.count_running(session, conversation_id):
            raise AgentError(409, "PLAN_CONVERSATION_RUNNING", "对话正在整理，请完成后再根据复盘调整")
        active = await workflow_repository.find_active_run(session, conversation_id)
        if active and (active.graph_state or {}).get("sourceReviewId") != str(report_id):
            raise AgentError(409, "WORKFLOW_ACTIVE_EXISTS", "当前还有待处理方案，请先接受或暂不接受，再整理复盘调整")
        previous = await session.scalar(select(WorkflowRun).where(
            WorkflowRun.user_id == int(context.user_id), WorkflowRun.conversation_id == conversation_id,
            WorkflowRun.graph_state["sourceReviewId"].astext == str(report_id),
            WorkflowRun.status.in_(("WAITING_CONFIRM", "CONFIRMED", "EXECUTING", "SUCCEEDED"))
        ).order_by(WorkflowRun.id.desc()).limit(1))
        previous_id = previous.id if previous is not None else None
    if previous_id:
        return await workflow_service.detail(db, user_id=int(context.user_id), run_id=previous_id)

    async def silent(_event, _data):
        return None
    arguments = json.dumps({"action": "draft", "objective": "根据实际执行反馈调整后续任务安排",
        "summary": message, "intent": "WEEKLY_PLAN", "questions": [], "reviewId": report_id}, ensure_ascii=False)
    async with asyncio.timeout_at(context.deadline):
        result = await conversation_planning.execute(db, context, client, conversation_id=conversation_id,
            turn_id=None, arguments=arguments, messages=[{"role": "user", "content": message}],
            summary="", slots=slots, emit=silent)
    if result.is_error:
        raise AgentError(422, "REVIEW_ADJUSTMENT_FAILED", result.summary)
    return await conversation_planning.current_detail(db, int(context.user_id), conversation_id)
