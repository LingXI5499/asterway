# 知识库路由放在文档编号路由之前，避免把 bases 当成文档编号。
from typing import Annotated

from fastapi import APIRouter, Depends, Path, Query
from pydantic import Field
from sqlalchemy import func, select

from app.core.context import AgentContext, get_agent_context
from app.db.models import KnowledgeDocument
from app.db.session import Database, get_database
from app.schemas.conversation import StrictCamelModel
from app.services import knowledge_base_service as service

ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
DbDep = Annotated[Database, Depends(get_database)]
router = APIRouter(prefix="/bases")


class BaseWrite(StrictCamelModel):
    name: str = Field(min_length=1, max_length=100)
    description: str = Field(default="", max_length=500)
    version: int | None = Field(default=None, ge=0)


class DocumentVersion(StrictCamelModel):
    id: int = Field(gt=0)
    version: int = Field(ge=0)


class MoveRequest(StrictCamelModel):
    knowledge_base_id: int = Field(gt=0)
    documents: list[DocumentVersion] = Field(min_length=1, max_length=100)


@router.get("")
async def page(context: ContextDep, db: DbDep):
    context.require("agent:knowledge:read")
    items = await service.list_bases(db, int(context.user_id))
    return {"items": items, "total": len(items)}


@router.post("", status_code=201)
async def create(payload: BaseWrite, context: ContextDep, db: DbDep):
    context.require("agent:knowledge:manage")
    return await service.save_base(db, int(context.user_id), payload.name, payload.description)


@router.get("/{base_id}")
async def detail(context: ContextDep, db: DbDep, base_id: Annotated[int, Path(gt=0)]):
    context.require("agent:knowledge:read")
    user_id = int(context.user_id)
    async with db.session() as session:
        row = await service.require_base(session, user_id, base_id)
        count = await session.scalar(select(func.count()).select_from(KnowledgeDocument).where(
            KnowledgeDocument.user_id == user_id, KnowledgeDocument.knowledge_base_id == base_id,
            KnowledgeDocument.deleted_at.is_(None)))
        return service.base_vo(row, count)


@router.patch("/{base_id}")
async def update(payload: BaseWrite, context: ContextDep, db: DbDep, base_id: Annotated[int, Path(gt=0)]):
    context.require("agent:knowledge:manage")
    return await service.save_base(db, int(context.user_id), payload.name, payload.description, base_id=base_id, version=payload.version)


@router.delete("/{base_id}")
async def remove(context: ContextDep, db: DbDep, base_id: Annotated[int, Path(gt=0)], version: Annotated[int, Query(ge=0)]):
    context.require("agent:knowledge:manage")
    await service.delete_base(db, int(context.user_id), base_id, version)
    return {"deleted": True}
