# 提供知识资料内部 HTTP 路由，校验当前用户后调用对应服务。
# 知识库内部接口：原始字节上传（由 Java 代理转发）、列表、详情、分块、任务与删除。

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）、只能从列出的固定值中选择的类型（Literal）。
from typing import Annotated, Literal

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、请求头（Header）、查询条件（Query）、本次 HTTP 请求（Request）。
from fastapi import APIRouter, Depends, Header, Query, Request

from app.api.routes.knowledge_bases import MoveRequest
from app.api.routes.knowledge_bases import router as bases_router

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）。
from app.core.context import AgentContext, get_agent_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.session 引入 数据库（Database）、取得本次请求的 Agent 数据库访问器（get_database）。
from app.db.session import Database, get_database

# 从 app.services 引入 知识资料业务服务（knowledge_service）。
from app.services import knowledge_base_service, knowledge_formats, knowledge_service

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(prefix="/internal/agent/knowledge", tags=["Knowledge"])
router.include_router(bases_router)

# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 成长助手上下文（AgentContext）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 上下文Dep（ContextDep）。
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 数据库（Database）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 数据库Dep（DbDep）。
DbDep = Annotated[Database, Depends(get_database)]


# 函数 _user：调用 _user 完成用户处理；接收 上下文（context）。
def _user(context: AgentContext) -> int:
    # 把执行“调用 int 完成int处理”得到的结果交还给调用方，本函数到此结束。
    return int(context.user_id)


# 把下面的函数登记为 HTTP 处理入口：router.post("", status_code=201, summary="上传知识文档")。
@router.get("/capabilities")
async def capabilities(context: ContextDep):
    if not context.permissions.intersection({"agent:knowledge:read", "agent:knowledge:manage"}):
        context.require("agent:knowledge:read")
    return knowledge_formats.capabilities()


@router.post("/move")
async def move(payload: MoveRequest, context: ContextDep, db: DbDep):
    context.require("agent:knowledge:manage")
    return await knowledge_base_service.move_documents(db, _user(context), payload.knowledge_base_id,
        [item.model_dump() for item in payload.documents])


@router.post("", status_code=201, summary="上传知识文档")
# 异步函数 upload：调用 upload 完成上传处理；接收 请求（request）、上下文（context）、Agent 数据库访问器（db）、文件名称（file_name）、标题（title）、mime类型（mime_type）。
async def upload(request: Request, context: ContextDep, db: DbDep, file_name: Annotated[str, Query(alias='fileName', min_length=1, max_length=255)], title: Annotated[str | None, Query(max_length=200)]=None, mime_type: Annotated[str | None, Header(alias='X-File-Type')]=None, knowledge_base_id: Annotated[int | None, Query(alias='knowledgeBaseId', gt=0)] = None) -> dict[str, object]:
    context.require('agent:knowledge:manage')
    data = bytearray()
    async for chunk in request.stream():
        data.extend(chunk)
        if len(data) > knowledge_formats.capabilities()["maxFileBytes"]:
            raise AgentError(413, 'KNOWLEDGE_TOO_LARGE', '文件超过允许的最大大小')
    (document, created) = await knowledge_service.upload(db, _user(context), file_name=file_name, mime_type=mime_type or 'application/octet-stream', data=bytes(data), title=title, knowledge_base_id=knowledge_base_id)
    return {'created': created, 'document': knowledge_service.document_vo(document)}


# 把下面的函数登记为 HTTP 处理入口：router.get("", summary="知识文档列表")。
@router.get("", summary="知识文档列表")
# 异步函数 page：调用 page 完成当前页处理；接收 上下文（context）、Agent 数据库访问器（db）、状态（status）、keyword、当前页数值（page_number）、每页条数（size）。
async def page(context: ContextDep, db: DbDep, status: Annotated[Literal['UPLOADED', 'PARSING', 'READY', 'FAILED', 'DELETED'] | None, Query()]=None, keyword: Annotated[str | None, Query(max_length=100)]=None, page_number: Annotated[int, Query(alias='page', ge=1)]=1, size: Annotated[int, Query(ge=1, le=100)]=20, knowledge_base_id: Annotated[int | None, Query(alias='knowledgeBaseId', gt=0)] = None) -> dict[str, object]:
    context.require('agent:knowledge:read')
    (rows, total) = await knowledge_service.list_documents(db, _user(context), status=status, keyword=keyword, page=page_number, size=size, knowledge_base_id=knowledge_base_id)
    return {'items': [knowledge_service.document_vo(row) for row in rows], 'total': total, 'page': page_number, 'size': size}


# 把下面的函数登记为 HTTP 处理入口：router.get("/tasks", summary="后台任务列表")。
@router.get("/tasks", summary="后台任务列表")
# 异步函数 tasks：调用 tasks 完成任务列表处理；接收 上下文（context）、Agent 数据库访问器（db）、状态（status）、当前页数值（page_number）、每页条数（size）。
async def tasks(context: ContextDep, db: DbDep, status: Annotated[Literal['PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'] | None, Query()]=None, page_number: Annotated[int, Query(alias='page', ge=1)]=1, size: Annotated[int, Query(ge=1, le=100)]=20, knowledge_base_id: Annotated[int | None, Query(alias='knowledgeBaseId', gt=0)] = None) -> dict[str, object]:
    context.require('agent:knowledge:read')
    (rows, total) = await knowledge_service.page_tasks(db, _user(context), status=status, page=page_number, size=size, knowledge_base_id=knowledge_base_id)
    return {'items': [knowledge_service.task_vo(row) for row in rows], 'total': total, 'page': page_number, 'size': size}


# 把下面的函数登记为 HTTP 处理入口：router.get("/{document_id}", summary="知识文档详情")。
@router.get("/{document_id}", summary="知识文档详情")
# 异步函数 detail：调用 detail 完成详情处理；接收 知识文档编号（document_id）、上下文（context）、Agent 数据库访问器（db）。
async def detail(document_id: int, context: ContextDep, db: DbDep) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:read")
    # 把等待执行“读取 知识文档（_document） 的值”得到的结果完成后的结果保存到 当前记录（row）。
    row = await knowledge_service.get_document(db, _user(context), document_id)
    # 把执行“调用 knowledge_service.document_vo 完成知识文档处理”得到的结果交还给调用方，本函数到此结束。
    return knowledge_service.document_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.get("/{document_id}/chunks", summary="知识文档分块列表")。
@router.get("/{document_id}/chunks", summary="知识文档分块列表")
# 异步函数 chunks：调用 chunks 完成文档片段列表处理；接收 知识文档编号（document_id）、上下文（context）、Agent 数据库访问器（db）、当前页数值（page_number）、每页条数（size）。
async def chunks(
    # 填写知识文档编号（document_id）字段，值为int。
    document_id: int,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写当前页数值（page_number）字段，值为表达式 Annotated[int, Query(alias="page", ge=1)] = 1 的结果。
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    # 填写每页条数（size）字段，值为表达式 Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    size: Annotated[int, Query(ge=1, le=100)] = 20,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:read")
    # 把等待执行“调用 knowledge_service.page_chunks 完成当前页文档片段列表处理”得到的结果完成后的结果保存到 记录列表总数（(rows, total)）。
    rows, total = await knowledge_service.page_chunks(
        # 继续传入或组合表达式 db, _user(context), document_id, page=page_number, size=size 的结果，完成上面尚未结束的表达式。
        db, _user(context), document_id, page=page_number, size=size
    )
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写数据列表（items）字段，值为调用 [knowledge_service.chunk_vo 完成文档片段处理。
        "items": [knowledge_service.chunk_vo(row) for row in rows],
        # 填写总数（total）字段，值为总数（total）。
        "total": total,
        # 填写当前页（page）字段，值为当前页数值（page_number）。
        "page": page_number,
        # 填写每页条数（size）字段，值为每页条数（size）。
        "size": size,
    }


# 把下面的函数登记为 HTTP 处理入口：router.delete("/{document_id}", summary="删除知识文档")。
@router.delete("/{document_id}", summary="删除知识文档")
# 异步函数 remove：调用 remove 完成移除处理；接收 知识文档编号（document_id）、版本号（version）、上下文（context）、Agent 数据库访问器（db）。
async def remove(
    # 填写知识文档编号（document_id）字段，值为int。
    document_id: int,
    # 填写版本号（version）字段，值为表达式 Annotated[int, Query(ge=0)] 的结果。
    version: Annotated[int, Query(ge=0)],
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> dict[str, bool]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, bool]:
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:manage")
    # 等待删除 知识文档（document）完成后再继续。
    await knowledge_service.delete_document(db, _user(context), document_id, version)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"success": True}


# 把下面的函数登记为 HTTP 处理入口：router.get("/{document_id}/revisions", summary="文档版本历史")。
@router.get("/{document_id}/revisions", summary="文档版本历史")
# 异步函数 revisions：调用 revisions 完成修订记录处理；接收 知识文档编号（document_id）、上下文（context）、Agent 数据库访问器（db）。
async def revisions(document_id: int, context: ContextDep, db: DbDep) -> dict:

    # 从 app.services 引入 修订记录（revisions）。
    from app.services import knowledge_revision_service as revisions
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:read")
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"items": await revisions.revisions(db, _user(context), document_id)}


# 把下面的函数登记为 HTTP 处理入口：router.put("/{document_id}/content", summary="上传文档新版本")。
@router.put("/{document_id}/content", summary="上传文档新版本")
# 异步函数 replace：生成保留原对象其余字段的新副本，只替换明确指定的值；接收 知识文档编号（document_id）、请求（request）、上下文（context）、Agent 数据库访问器（db）、版本号（version）、文件名称（file_name）、mime类型（mime_type）。
async def replace(document_id: int, request: Request, context: ContextDep, db: DbDep,
                  # 填写版本号（version）字段，值为表达式 Annotated[int, Query(ge=0)] 的结果。
                  version: Annotated[int, Query(ge=0)],
                  # 填写文件名称（file_name）字段，值为表达式 Annotated[str, Query(alias="fileName", min_length=1, max_length=255)] 的结果。
                  file_name: Annotated[str, Query(alias="fileName", min_length=1, max_length=255)],
                  # 填写mime类型（mime_type）字段，值为按表达式计算得到的值：Annotated[str, Header(alias="X-File-Type")] = "application/octet-stream") -> dict:。
                  mime_type: Annotated[str, Header(alias="X-File-Type")] = "application/octet-stream") -> dict:

    # 从 app.services 引入 修订记录（revisions）。
    from app.services import knowledge_revision_service as revisions
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:manage")
    # 把执行“调用 bytearray 完成bytearray处理”得到的结果保存到 数据（data）。
    data = bytearray()
    # 从执行“调用 request.stream 完成分段回复流处理”得到的结果依次取出piece，每项执行一次下面的处理。
    async for piece in request.stream():
        # 把另一组条目依次加入列表。
        data.extend(piece)
        # 新版本也使用能力接口给出的同一份有效大小上限。
        if len(data) > knowledge_formats.capabilities()["maxFileBytes"]:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的最大大小")
    # 把等待执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果完成后的结果保存到 当前记录（row）。
    row = await revisions.replace(db, _user(context), document_id, version=version,
        # 传入文件名称（file_name），值为表达式 file_name, mime_type=mime_type, data=bytes(data)) 的结果。
        file_name=file_name, mime_type=mime_type, data=bytes(data))
    # 把执行“调用 knowledge_service.document_vo 完成知识文档处理”得到的结果交还给调用方，本函数到此结束。
    return knowledge_service.document_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{document_id}/retry", summary="重试文档处理")。
@router.post("/{document_id}/retry", summary="重试文档处理")
# 异步函数 retry：调用 retry 完成重试处理；接收 知识文档编号（document_id）、上下文（context）、Agent 数据库访问器（db）、版本号（version）。
async def retry(document_id: int, context: ContextDep, db: DbDep,
                # 填写版本号（version）字段，值为表达式 Annotated[int, Query(ge=0)]) -> dict: 的结果。
                version: Annotated[int, Query(ge=0)]) -> dict:

    # 从 app.services 引入 修订记录（revisions）。
    from app.services import knowledge_revision_service as revisions
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:manage")
    # 等待调用 revisions.retry 完成重试处理完成后再继续。
    await revisions.retry(db, _user(context), document_id, version)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"accepted": True}


# 把下面的函数登记为 HTTP 处理入口：router.get("/{document_id}/revisions/{revision_id}/chunks/{chunk_id}", summary="引用原文")。
@router.get("/{document_id}/revisions/{revision_id}/chunks/{chunk_id}", summary="引用原文")
# 异步函数 source：调用 source 完成来源处理；接收 知识文档编号（document_id）、修订版本编号（revision_id）、文档片段编号（chunk_id）、上下文（context）、Agent 数据库访问器（db）。
async def source(document_id: int, revision_id: int, chunk_id: int,
                 # 填写上下文（context）字段，值为表达式 ContextDep, db: DbDep) -> dict: 的结果。
                 context: ContextDep, db: DbDep) -> dict:

    # 从 app.services 引入 修订记录（revisions）。
    from app.services import knowledge_revision_service as revisions
    # 调用 context.require 完成require处理。
    context.require("agent:knowledge:read")
    # 把等待执行“调用 revisions.source 完成来源处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await revisions.source(db, _user(context), document_id, revision_id, chunk_id)
