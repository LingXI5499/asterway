"""规划工作流内部接口：生成方案、修订、确认、取消、查看与恢复。"""

from typing import Annotated, Literal

from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel, ConfigDict, Field

from app.core.context import AgentContext, get_agent_context, get_java_client, refresh_context
from app.core.errors import AgentError
from app.db.session import Database, get_database
from app.integrations.java_client import JavaApiClient
from app.repositories import conversation_repository
from app.services import (
    planning_context_service,
    planning_service,
    workflow_progress_service,
    workflow_service,
)
from app.services.workflow_graph import require_graph

router = APIRouter(prefix="/internal/agent/planning/proposals", tags=["Planning"])

ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]
DbDep = Annotated[Database, Depends(get_database)]


class ProposalRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    message: str = Field(min_length=1, max_length=8000)
    conversationId: int = Field(ge=1)
    intent: str | None = None
    sourceTurnId: int | None = Field(default=None, ge=1)


class RevisionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    message: str = Field(min_length=1, max_length=8000)


class VersionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    planVersionId: int = Field(ge=1)


def _user(context: AgentContext) -> int:
    return int(context.user_id)


async def _calendar(context: AgentContext, client: JavaApiClient) -> tuple[str | None, str | None]:
    result = await client.get(
        "dashboard/overview", authorization=context.authorization, trace_id=context.trace_id
    )
    if (
        not isinstance(result.data, dict)
        or not result.data.get("today")
        or not result.data.get("timeZone")
    ):
        raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "无法取得账号日期与时区，请稍后重试")
    return str(result.data["today"]), str(result.data["timeZone"])


async def _assert_conversation(db: Database, user_id: int, conversation_id: int) -> None:
    async with db.session() as session:
        await conversation_repository.require_owned(session, user_id, conversation_id)


@router.post("", status_code=201, summary="生成规划方案")
async def create_proposal(
    payload: ProposalRequest,
    context: ContextDep,
    client: ClientDep,
    db: DbDep,
) -> dict[str, object]:
    context.require("agent:workflow:manage")
    user_id = _user(context)
    intent = payload.intent or planning_service.detect_intent(payload.message)
    if intent not in workflow_service.INTENTS:
        raise AgentError(
            422,
            "WORKFLOW_INTENT_REQUIRED",
            "无法识别规划意图，请说明要规划目标、阶段、任务、周计划、日计划还是习惯",
        )
    await _assert_conversation(db, user_id, payload.conversationId)
    today, zone = await _calendar(context, client)
    business_context = await planning_context_service.collect(context, client, intent)
    snapshot = await planning_service.generate_snapshot(
        intent=intent,
        message=payload.message,
        today=today,
        time_zone=zone,
        business_context=business_context,
    )
    snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
    fresh = await refresh_context(context, client)
    fresh.require("agent:workflow:manage")
    run = await workflow_service.start_run(
        db,
        user_id=user_id,
        conversation_id=payload.conversationId,
        intent=intent,
        source_turn_id=payload.sourceTurnId,
    )
    version = await workflow_service.save_plan(
        db, user_id=user_id, run_id=run.id, snapshot=snapshot
    )
    await require_graph(db).prepare(user_id, run.id, version.id)
    detail = await workflow_service.detail(db, user_id=user_id, run_id=run.id)
    return {"run": detail["run"], "plan": workflow_service.plan_vo(version)}


@router.post("/{run_id}/revisions", summary="修订规划方案")
async def revise_proposal(
    run_id: int,
    payload: RevisionRequest,
    context: ContextDep,
    client: ClientDep,
    db: DbDep,
) -> dict[str, object]:
    context.require("agent:workflow:manage")
    user_id = _user(context)
    detail = await workflow_service.detail(db, user_id=user_id, run_id=run_id)
    plans = detail.get("plans") or []
    previous = plans[-1].get("snapshot") if plans else None
    if not isinstance(previous, dict):
        raise AgentError(422, "WORKFLOW_PLAN_MISSING", "当前方案没有可修订的版本")
    intent = str(detail["run"]["intent"])
    await _assert_conversation(db, user_id, int(str(detail["run"]["conversationId"])))
    today, zone = await _calendar(context, client)
    business_context = await planning_context_service.collect(context, client, intent)
    snapshot = await planning_service.generate_snapshot(
        intent=intent,
        message=payload.message,
        today=today,
        time_zone=zone,
        previous=previous,
        business_context=business_context,
    )
    snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
    fresh = await refresh_context(context, client)
    fresh.require("agent:workflow:manage")
    version = await workflow_service.save_plan(
        db, user_id=user_id, run_id=run_id, snapshot=snapshot, revision=True
    )
    await require_graph(db).prepare(user_id, run_id, version.id)
    return {"plan": workflow_service.plan_vo(version)}


@router.get("", summary="规划列表")
async def page(
    context: ContextDep,
    db: DbDep,
    status: Annotated[
        Literal[
            "DRAFT",
            "WAITING_CONFIRM",
            "CONFIRMED",
            "EXECUTING",
            "SUCCEEDED",
            "FAILED",
            "CANCELLED",
            "EXPIRED",
        ]
        | None,
        Query(),
    ] = None,
    conversation_id: Annotated[int | None, Query(alias="conversationId", ge=1)] = None,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> dict[str, object]:
    context.require("agent:workflow:read")
    runs, total = await workflow_service.page_runs(
        db, user_id=_user(context), status=status, page=page_number, size=size,
        conversation_id=conversation_id,
    )
    return {
        "items": [workflow_service.run_vo(row) for row in runs],
        "total": total,
        "page": page_number,
        "size": size,
    }


@router.get("/{run_id}", summary="规划详情")
async def detail(run_id: int, context: ContextDep, db: DbDep) -> dict[str, object]:
    context.require("agent:workflow:read")
    return await workflow_service.detail(db, user_id=_user(context), run_id=run_id)


@router.post("/{run_id}/confirm", summary="确认方案")
async def confirm(
    run_id: int,
    payload: VersionRequest,
    context: ContextDep,
    db: DbDep,
) -> dict[str, object]:
    context.require("agent:workflow:manage")
    version = await workflow_service.confirm_plan(
        db,
        user_id=_user(context),
        run_id=run_id,
        plan_version_id=payload.planVersionId,
    )
    return {"plan": workflow_service.plan_vo(version)}


@router.get("/{run_id}/progress", summary="方案关联对象的真实进度与执行反馈")
async def progress(run_id: int, context: ContextDep, db: DbDep, client: ClientDep):
    return await workflow_progress_service.collect(db, context, client, run_id)


@router.post("/{run_id}/execute", summary="确认后执行方案提交")
async def execute(
    run_id: int,
    context: ContextDep,
    client: ClientDep,
    db: DbDep,
) -> dict[str, object]:
    context.require("agent:workflow:manage")
    detail = await workflow_service.detail(db, user_id=_user(context), run_id=run_id)
    run = detail["run"]
    commit = detail.get("commit")
    if commit is not None and commit["status"] == "SUCCEEDED":
        return {"commit": commit}
    if commit is not None:
        # 确认已完成但上次提交未成功：沿用原幂等键直接重试，不重复确认、不重复写入。
        receipt = await workflow_service.execute_commit(
            db, context=context, java_client=client, run_id=run_id
        )
        return {"commit": workflow_service.commit_vo(receipt)}
    if run["status"] in ("DRAFT", "WAITING_CONFIRM"):
        raise AgentError(409, "WORKFLOW_NOT_CONFIRMED", "请先确认最新方案再执行")
    if run["status"] in ("CANCELLED", "EXPIRED"):
        raise AgentError(409, "WORKFLOW_STATUS_INVALID", "方案已取消或过期，不能执行")
    if run["status"] == "FAILED":
        raise AgentError(409, "PLAN_COMMIT_MISSING", "失败状态没有可重试的提交记录，请重新生成方案")
    return {"commit": (await require_graph(db).execute(context, client, run_id))["commit"]}


@router.post("/{run_id}/cancel", summary="取消规划")
async def cancel(run_id: int, context: ContextDep, db: DbDep) -> dict[str, bool]:
    context.require("agent:workflow:manage")
    await workflow_service.cancel_run(db, user_id=_user(context), run_id=run_id)
    return {"success": True}
