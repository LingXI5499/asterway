import asyncio
import logging
import re
from contextlib import asynccontextmanager
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.api.routes.chat import router as chat_router
from app.api.routes.conversations import router as conversation_router
from app.api.routes.knowledge import router as knowledge_router
from app.api.routes.memories import router as memory_router
from app.api.routes.workflows import router as workflow_router
from app.api.routes.workspace import router as workspace_router
from app.core.config import get_settings
from app.core.errors import AgentError
from app.db.session import Database
from app.integrations.java_client import JavaApiClient
from app.llm.client import close_llm_client
from app.services.background_worker import BackgroundWorker
from app.services.turn_service import shutdown_generation
from app.services.workflow_graph import WorkflowGraph

logger = logging.getLogger(__name__)

# 浏览器通过 Java 网关访问；本服务不开放跨域浏览器调用或数据库自动初始化。


async def storage_readiness(database: Database | None) -> dict[str, bool]:
    """只检查已部署表，不把代码已导入当成数据库已经初始化。"""
    groups = {
        "conversation": ("conversation", "conversation_turn", "conversation_message"),
        "memory": ("memory", "memory_embedding"),
        "knowledge": ("knowledge_document", "knowledge_revision", "knowledge_chunk", "background_task"),
        "planning-workflow": ("workflow_run", "plan_version", "workflow_event", "plan_commit", "checkpoints", "checkpoint_blobs", "checkpoint_writes"),
    }
    ready = {name: False for name in groups}
    if database is None:
        return ready
    try:
        async with database.session() as session:
            for name, tables in groups.items():
                found = [
                    await session.scalar(
                        text("SELECT to_regclass(:table)"), {"table": "agent." + table}
                    )
                    for table in tables
                ]
                ready[name] = all(value is not None for value in found)
    except (SQLAlchemyError, OSError, TimeoutError):
        logger.warning("agent_storage_not_ready")
    return ready


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.java_client = JavaApiClient(get_settings())
    app.state.chat_slots = asyncio.Semaphore(get_settings().agent_max_concurrent_requests)
    try:
        app.state.database = Database(get_settings())
    except AgentError:
        # 未配置会话存储时 P1.1 单轮对话仍可用，会话接口返回 503。
        app.state.database = None
    app.state.background_worker = None
    app.state.storage_readiness = await storage_readiness(app.state.database)
    if app.state.database is not None and app.state.storage_readiness["planning-workflow"]:
        graph = WorkflowGraph(app.state.database, get_settings())
        await graph.start()
        app.state.database.workflow_graph = graph
    if app.state.database is not None and app.state.storage_readiness["knowledge"]:
        app.state.background_worker = BackgroundWorker(app.state.database)
        await app.state.background_worker.start()
        app.state.database.background_worker = app.state.background_worker
    try:
        yield
    finally:
        if app.state.background_worker is not None:
            await app.state.background_worker.stop()
        await shutdown_generation()
        if app.state.database is not None:
            if getattr(app.state.database, "workflow_graph", None):
                await app.state.database.workflow_graph.close()
            await app.state.database.close()
        await app.state.java_client.close()
        await close_llm_client()


app = FastAPI(
    title="Asterway Agent",
    description="Asterway AI Growth Assistant",
    version="0.1.0",
    lifespan=lifespan,
)


@app.middleware("http")
async def attach_trace(request: Request, call_next):
    supplied = request.headers.get("X-Trace-ID", "")
    request.state.trace_id = (
        supplied if re.fullmatch(r"[A-Za-z0-9_-]{1,64}", supplied) else uuid4().hex
    )
    response = await call_next(request)
    response.headers["X-Trace-ID"] = request.state.trace_id
    return response


@app.exception_handler(AgentError)
async def agent_error(request: Request, error: AgentError):
    content = {
        "code": error.code,
        "message": error.message,
        "traceId": request.state.trace_id,
    }
    if error.data is not None:
        content["data"] = error.data
    return JSONResponse(
        status_code=error.status_code,
        content=content,
        headers={"WWW-Authenticate": "Bearer"} if error.status_code == 401 else None,
    )


@app.exception_handler(RequestValidationError)
async def invalid_request(request: Request, error: RequestValidationError):
    # 不把输入内容原样写回错误结果，避免额外字段携带凭据或私密内容。
    return JSONResponse(
        status_code=422,
        content={
            "code": "INVALID_REQUEST",
            "message": "请求参数不符合接口要求，请检查必填字段、格式和取值范围",
            "traceId": request.state.trace_id,
        },
    )


@app.exception_handler(Exception)
async def unexpected_error(request: Request, error: Exception):
    trace_id = getattr(request.state, "trace_id", uuid4().hex)
    logger.error("agent_unexpected_error trace=%s type=%s", trace_id, type(error).__name__)
    return JSONResponse(
        status_code=500,
        content={
            "code": "AGENT_INTERNAL_ERROR",
            "message": "对话服务暂时不可用",
            "traceId": trace_id,
        },
    )


@app.get("/health")
async def health(request: Request):
    ready = getattr(request.app.state, "storage_readiness", {})
    capabilities = ["chat", "sse", "tools"]
    if getattr(getattr(request.app.state, "database", None), "workflow_graph", None) \
            and get_settings().agent_tools_enabled:
        capabilities.append("conversation-planning")
    capabilities.extend(name for name, available in ready.items() if available)
    if ready.get("knowledge"):
        capabilities.append("background-tasks")
    return {
        "success": True,
        "service": "asterway-agent",
        "status": "running",
        "phase": "P1.6",
        "capabilities": capabilities,
        "storageReady": ready,
        "embeddingConfigured": bool(get_settings().embedding_api_key.get_secret_value()),
    }


app.include_router(chat_router)
app.include_router(conversation_router)
app.include_router(memory_router)
app.include_router(knowledge_router)
app.include_router(workflow_router)

app.include_router(workspace_router)
