"""知识库内部接口：原始字节上传（由 Java 代理转发）、列表、详情、分块、任务与删除。"""

from typing import Annotated, Literal

from fastapi import APIRouter, Depends, Header, Query, Request

from app.core.config import get_settings
from app.core.context import AgentContext, get_agent_context
from app.core.errors import AgentError
from app.db.session import Database, get_database
from app.services import knowledge_service

router = APIRouter(prefix="/internal/agent/knowledge", tags=["Knowledge"])

ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
DbDep = Annotated[Database, Depends(get_database)]


def _user(context: AgentContext) -> int:
    return int(context.user_id)


@router.post("", status_code=201, summary="上传知识文档")
async def upload(
    request: Request,
    context: ContextDep,
    db: DbDep,
    file_name: Annotated[str, Query(alias="fileName", min_length=1, max_length=255)],
    title: Annotated[str | None, Query(max_length=200)] = None,
    mime_type: Annotated[str | None, Header(alias="X-File-Type")] = None,
) -> dict[str, object]:
    context.require("agent:knowledge:manage")
    data = bytearray()
    async for chunk in request.stream():
        data.extend(chunk)
        if len(data) > get_settings().agent_knowledge_max_bytes:
            raise AgentError(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的最大大小")
    document, created = await knowledge_service.upload(
        db,
        _user(context),
        file_name=file_name,
        mime_type=mime_type or "application/octet-stream",
        data=bytes(data),
        title=title,
    )
    return {
        "created": created,
        "document": knowledge_service.document_vo(document),
    }


@router.get("", summary="知识文档列表")
async def page(
    context: ContextDep,
    db: DbDep,
    status: Annotated[
        Literal["UPLOADED", "PARSING", "READY", "FAILED", "DELETED"] | None, Query()
    ] = None,
    keyword: Annotated[str | None, Query(max_length=100)] = None,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> dict[str, object]:
    context.require("agent:knowledge:read")
    rows, total = await knowledge_service.list_documents(
        db,
        _user(context),
        status=status,
        keyword=keyword,
        page=page_number,
        size=size,
    )
    return {
        "items": [knowledge_service.document_vo(row) for row in rows],
        "total": total,
        "page": page_number,
        "size": size,
    }


@router.get("/tasks", summary="后台任务列表")
async def tasks(
    context: ContextDep,
    db: DbDep,
    status: Annotated[
        Literal["PENDING", "RUNNING", "SUCCEEDED", "FAILED", "CANCELLED"] | None, Query()
    ] = None,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> dict[str, object]:
    context.require("agent:knowledge:read")
    rows, total = await knowledge_service.page_tasks(
        db, _user(context), status=status, page=page_number, size=size
    )
    return {
        "items": [knowledge_service.task_vo(row) for row in rows],
        "total": total,
        "page": page_number,
        "size": size,
    }


@router.get("/{document_id}", summary="知识文档详情")
async def detail(document_id: int, context: ContextDep, db: DbDep) -> dict[str, object]:
    context.require("agent:knowledge:read")
    row = await knowledge_service.get_document(db, _user(context), document_id)
    return knowledge_service.document_vo(row)


@router.get("/{document_id}/chunks", summary="知识文档分块列表")
async def chunks(
    document_id: int,
    context: ContextDep,
    db: DbDep,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> dict[str, object]:
    context.require("agent:knowledge:read")
    rows, total = await knowledge_service.page_chunks(
        db, _user(context), document_id, page=page_number, size=size
    )
    return {
        "items": [knowledge_service.chunk_vo(row) for row in rows],
        "total": total,
        "page": page_number,
        "size": size,
    }


@router.delete("/{document_id}", summary="删除知识文档")
async def remove(
    document_id: int,
    version: Annotated[int, Query(ge=0)],
    context: ContextDep,
    db: DbDep,
) -> dict[str, bool]:
    context.require("agent:knowledge:manage")
    await knowledge_service.delete_document(db, _user(context), document_id, version)
    return {"success": True}


@router.get("/{document_id}/revisions", summary="文档版本历史")
async def revisions(document_id: int, context: ContextDep, db: DbDep) -> dict:
    from app.services import knowledge_revision_service as revisions
    context.require("agent:knowledge:read")
    return {"items": await revisions.revisions(db, _user(context), document_id)}


@router.put("/{document_id}/content", summary="上传文档新版本")
async def replace(document_id: int, request: Request, context: ContextDep, db: DbDep,
                  version: Annotated[int, Query(ge=0)],
                  file_name: Annotated[str, Query(alias="fileName", min_length=1, max_length=255)],
                  mime_type: Annotated[str, Header(alias="X-File-Type")] = "application/octet-stream") -> dict:
    from app.services import knowledge_revision_service as revisions
    context.require("agent:knowledge:manage")
    data = bytearray()
    async for piece in request.stream():
        data.extend(piece)
        if len(data) > get_settings().agent_knowledge_max_bytes:
            raise AgentError(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的最大大小")
    row = await revisions.replace(db, _user(context), document_id, version=version,
        file_name=file_name, mime_type=mime_type, data=bytes(data))
    return knowledge_service.document_vo(row)


@router.post("/{document_id}/retry", summary="重试文档处理")
async def retry(document_id: int, context: ContextDep, db: DbDep,
                version: Annotated[int, Query(ge=0)]) -> dict:
    from app.services import knowledge_revision_service as revisions
    context.require("agent:knowledge:manage")
    await revisions.retry(db, _user(context), document_id, version)
    return {"accepted": True}


@router.get("/{document_id}/revisions/{revision_id}/chunks/{chunk_id}", summary="引用原文")
async def source(document_id: int, revision_id: int, chunk_id: int,
                 context: ContextDep, db: DbDep) -> dict:
    from app.services import knowledge_revision_service as revisions
    context.require("agent:knowledge:read")
    return await revisions.source(db, _user(context), document_id, revision_id, chunk_id)
