"""成长复盘与本人运行记录，均由 Java 认证代理转发。"""

from datetime import date
from typing import Annotated

from fastapi import APIRouter, Depends, Query, Request
from pydantic import BaseModel, ConfigDict, Field, model_validator
from sqlalchemy import func, select

from app.core.context import AgentContext, get_agent_context, get_java_client
from app.db.session import Database, get_database
from app.db.workspace_models import CallRecord, ReviewReport
from app.integrations.java_client import JavaApiClient
from app.repositories import background_task_repository as tasks
from app.services import (
    background_recovery,
    knowledge_service,
    review_planning_service,
    review_service,
)

router = APIRouter(prefix="/internal/agent", tags=["Workspace"])
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
DbDep = Annotated[Database, Depends(get_database)]
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]


class ReviewRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    dateFrom: date
    dateTo: date
    conversationId: int | None = Field(default=None, ge=1)
    sourceRunId: int | None = Field(default=None, ge=1)

    @model_validator(mode="after")
    def validate_range(self):
        if not 0 <= (self.dateTo-self.dateFrom).days <= 365:
            raise ValueError("复盘区间应为1至366天")
        return self


@router.post("/reviews", status_code=201)
async def create_review(payload: ReviewRequest, context: ContextDep, db: DbDep, client: ClientDep):
    row = await review_service.create(db, context, client, payload.dateFrom, payload.dateTo,
        conversation_id=payload.conversationId, source_run_id=payload.sourceRunId)
    return review_service.vo(row)


@router.post("/reviews/{report_id}/retry")
async def retry_review(report_id: int, context: ContextDep, db: DbDep):
    context.require("agent:review:manage", "statistics:report:read")
    row = await review_service.retry(db, int(context.user_id), report_id)
    return review_service.vo(row)


@router.get("/reviews")
async def list_reviews(context: ContextDep, db: DbDep,
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20):
    context.require("agent:review:read")
    async with db.session() as session:
        query = select(ReviewReport).where(ReviewReport.user_id == int(context.user_id))
        total = await session.scalar(select(func.count()).select_from(query.subquery()))
        rows = await session.scalars(query.order_by(ReviewReport.id.desc()).offset((page-1)*size).limit(size))
        return {"items": [review_service.vo(row) for row in rows], "total": total, "page": page, "size": size}


@router.get("/reviews/{report_id}")
async def get_review(report_id: int, context: ContextDep, db: DbDep):
    context.require("agent:review:read")
    return review_service.vo(await review_service.get(db, int(context.user_id), report_id))


@router.get("/reviews/latest/conversation/{conversation_id}")
async def conversation_review(conversation_id: int, context: ContextDep, db: DbDep):
    from app.repositories import conversation_repository
    context.require("agent:review:read")
    async with db.session() as session:
        await conversation_repository.require_owned(session, int(context.user_id), conversation_id)
    row = await review_service.latest_for_conversation(db, int(context.user_id), conversation_id)
    return {"review": review_service.vo(row) if row else None}


class ReviewProposalRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    conversationId: int = Field(ge=1)
    message: str = Field(default="根据这次复盘调整接下来两周的任务安排，保留已有目标和任务，说明每项改期原因。", min_length=1, max_length=2000)


@router.post("/reviews/{report_id}/proposals", status_code=201)
async def review_proposal(report_id: int, payload: ReviewProposalRequest, request: Request,
                          context: ContextDep, db: DbDep, client: ClientDep):
    return await review_planning_service.draft(db, context, client, report_id=report_id,
        conversation_id=payload.conversationId, message=payload.message, slots=request.app.state.chat_slots)


@router.get("/runs/calls")
async def calls(context: ContextDep, db: DbDep,
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20):
    context.require("agent:run:read")
    async with db.session() as session:
        query = select(CallRecord).where(CallRecord.user_id == int(context.user_id))
        total = await session.scalar(select(func.count()).select_from(query.subquery()))
        rows = list(await session.scalars(query.order_by(CallRecord.id.desc()).offset((page-1)*size).limit(size)))
        aggregate = (await session.execute(select(func.count(), func.sum(CallRecord.prompt_tokens),
            func.sum(CallRecord.completion_tokens), func.avg(CallRecord.duration_ms)).where(
                CallRecord.user_id == int(context.user_id)))).one()
        return {"items": [{"id": str(row.id), "traceId": row.trace_id, "kind": row.kind,
            "name": row.name, "status": row.status, "durationMs": row.duration_ms,
            "firstTokenMs": row.first_token_ms, "promptTokens": row.prompt_tokens,
            "completionTokens": row.completion_tokens, "errorCode": row.error_code,
            "createdAt": row.created_at} for row in rows], "total": total, "page": page, "size": size,
            "summary": {"calls": aggregate[0], "promptTokens": aggregate[1],
                "completionTokens": aggregate[2], "averageDurationMs": float(aggregate[3] or 0)}}


@router.get("/runs/tasks")
async def background_tasks(context: ContextDep, db: DbDep,
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20,
    status: Annotated[str | None, Query(pattern="^(PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED)$")] = None):
    context.require("agent:run:read")
    async with db.session() as session:
        rows, total = await tasks.page_tasks(session, user_id=int(context.user_id), status=status, page_number=page, size=size)
    return {"items": [knowledge_service.task_vo(row) for row in rows], "total": total, "page": page, "size": size}


@router.post("/runs/tasks/{task_id}/retry")
async def retry_background_task(task_id: int, context: ContextDep, db: DbDep):
    return knowledge_service.task_vo(await background_recovery.retry_memory(db, context, task_id))
