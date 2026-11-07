"""长期记忆内部接口；浏览器只通过 Java 代理访问，用户范围由 Java 验证后的上下文决定。"""

from typing import Annotated, Literal

from fastapi import APIRouter, Depends, Query

from app.core.context import AgentContext, get_agent_context
from app.db.session import Database, get_database
from app.schemas.memory import (
    MemoryCandidateRequest,
    MemoryPageVO,
    MemoryUpdateRequest,
    MemoryVersionRequest,
    MemoryVO,
)
from app.services import memory_service

router = APIRouter(prefix="/internal/agent/memories", tags=["Memories"])

ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
DbDep = Annotated[Database, Depends(get_database)]


def _user(context: AgentContext) -> int:
    return int(context.user_id)


@router.get("", response_model=MemoryPageVO, summary="记忆列表")
async def page(
    context: ContextDep,
    db: DbDep,
    status: Annotated[Literal["CANDIDATE", "CONFIRMED", "REJECTED", "ARCHIVED"] | None, Query()] = None,
    kind: Annotated[Literal["PREFERENCE", "FACT", "CONSTRAINT", "GOAL_CONTEXT"] | None, Query()] = None,
    keyword: Annotated[str | None, Query(max_length=100)] = None,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> MemoryPageVO:
    context.require("agent:memory:read")
    rows, total = await memory_service.list_memories(
        db,
        _user(context),
        status=status,
        kind=kind,
        keyword=keyword,
        page=page_number,
        size=size,
    )
    return MemoryPageVO(
        items=[memory_service.memory_vo(row) for row in rows],
        total=total,
        page=page_number,
        size=size,
    )


@router.post("/candidates", status_code=201, response_model=MemoryVO, summary="新建记忆候选")
async def create_candidate(
    payload: MemoryCandidateRequest, context: ContextDep, db: DbDep
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.create_candidate(db, _user(context), payload)
    return memory_service.memory_vo(row)


@router.patch("/{memory_id}", response_model=MemoryVO, summary="修改记忆内容")
async def update_content(
    memory_id: int,
    payload: MemoryUpdateRequest,
    context: ContextDep,
    db: DbDep,
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.update_content(
        db, _user(context), memory_id, payload.content, payload.version
    )
    return memory_service.memory_vo(row)


@router.post("/{memory_id}/confirm", response_model=MemoryVO, summary="确认记忆")
async def confirm(
    memory_id: int,
    payload: MemoryVersionRequest,
    context: ContextDep,
    db: DbDep,
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.confirm(db, _user(context), memory_id, payload.version)
    return memory_service.memory_vo(row)


@router.post("/{memory_id}/reject", response_model=MemoryVO, summary="拒绝记忆")
async def reject(
    memory_id: int,
    payload: MemoryVersionRequest,
    context: ContextDep,
    db: DbDep,
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.reject(db, _user(context), memory_id, payload.version)
    return memory_service.memory_vo(row)


@router.post("/{memory_id}/archive", response_model=MemoryVO, summary="归档记忆")
async def archive(
    memory_id: int,
    payload: MemoryVersionRequest,
    context: ContextDep,
    db: DbDep,
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.archive(db, _user(context), memory_id, payload.version)
    return memory_service.memory_vo(row)


@router.delete("/{memory_id}", response_model=MemoryVO, summary="删除记忆")
async def remove(
    memory_id: int,
    version: Annotated[int, Query(ge=0)],
    context: ContextDep,
    db: DbDep,
) -> MemoryVO:
    context.require("agent:memory:manage")
    row = await memory_service.delete(db, _user(context), memory_id, version)
    return memory_service.memory_vo(row)
