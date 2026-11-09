# 实现程序入口处理；下面逐项说明输入、分支与调用的作用。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 logging，供下面调用其功能。
import logging

# 引入 re，供下面调用其功能。
import re

# 从 contextlib 引入 异步资源进入和退出管理器（asynccontextmanager）。
from contextlib import asynccontextmanager

# 从 uuid 引入 uuid4。
from uuid import uuid4

# 从 fastapi 引入 Fast接口（FastAPI）、本次 HTTP 请求（Request）。
from fastapi import FastAPI, Request

# 从 fastapi.exceptions 引入 请求Validation错误（RequestValidationError）。
from fastapi.exceptions import RequestValidationError

# 从 fastapi.responses 引入 JSON 格式的 HTTP 响应（JSONResponse）。
from fastapi.responses import JSONResponse

# 从 sqlalchemy 引入 文字（text）。
from sqlalchemy import text

# 从 sqlalchemy.exc 引入 SQLAlchemy错误（SQLAlchemyError）。
from sqlalchemy.exc import SQLAlchemyError

# 从 app.api.routes.chat 引入 chatrouter（chat_router）。
from app.api.routes.chat import router as chat_router

# 从 app.api.routes.conversations 引入 对话router（conversation_router）。
from app.api.routes.conversations import router as conversation_router

# 从 app.api.routes.knowledge 引入 知识资料router（knowledge_router）。
from app.api.routes.knowledge import router as knowledge_router

# 从 app.api.routes.memories 引入 长期记忆router（memory_router）。
from app.api.routes.memories import router as memory_router

# 从 app.api.routes.workflows 引入 规划工作流router（workflow_router）。
from app.api.routes.workflows import router as workflow_router

# 从 app.api.routes.workspace 引入 工作区router（workspace_router）。
from app.api.routes.workspace import router as workspace_router

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.client 引入 关闭语言模型客户端（close_llm_client）。
from app.llm.client import close_llm_client

# 从 app.services.background_worker 引入 后台后台处理器（BackgroundWorker）。
from app.services.background_worker import BackgroundWorker

# 从 app.services.turn_service 引入 shutdowngeneration（shutdown_generation）。
from app.services.turn_service import shutdown_generation

# 从 app.services.workflow_graph 引入 规划工作流工作流图（WorkflowGraph）。
from app.services.workflow_graph import WorkflowGraph

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 浏览器通过 Java 网关访问；本服务不开放跨域浏览器调用或数据库自动初始化。


# 异步函数 storage_readiness：只检查已部署表，不把代码已导入当成数据库已经初始化。；接收 数据库（database）。
async def storage_readiness(database: Database | None) -> dict[str, bool]:
    # 只检查已部署表，不把代码已导入当成数据库已经初始化。
    # 把按字段名保存多个值的字典保存到 分组列表（groups）。
    groups = {
        # 填写对话（conversation）字段，值为表达式 ("conversation", "conversation_turn", "conversation_message") 的结果。
        "conversation": ("conversation", "conversation_turn", "conversation_message"),
        # 填写长期记忆（memory）字段，值为表达式 ("memory", "memory_embedding") 的结果。
        "memory": ("memory", "memory_embedding"),
        # 填写知识资料（knowledge）字段，值为表达式 ("knowledge_document", "knowledge_revision", "knowledge_chunk", "background_task") 的结果。
        "knowledge": ("knowledge_document", "knowledge_revision", "knowledge_chunk", "background_task"),
        # 继续传入或组合表达式 "planning-workflow": ("workflow_run", "plan_version", "workflow_event", "plan_commit", "checkpoints", "checkpoint_blobs"… 的结果，完成上面尚未结束的表达式。
        "planning-workflow": ("workflow_run", "plan_version", "workflow_event", "plan_commit", "checkpoints", "checkpoint_blobs", "checkpoint_writes"),
    }
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 已就绪（ready）。
    ready = {name: False for name in groups}
    # 检查database is None，即对应内容是否不存在；成立时执行缩进的分支。
    if database is None:
        # 把已就绪（ready）交还给调用方，本函数到此结束。
        return ready
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 database.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with database.session() as session:
            # 从执行“调用 groups.items 完成数据列表处理”得到的结果依次取出名称tables（(name, tables)），每项执行一次下面的处理。
            for name, tables in groups.items():
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 found。
                found = [
                    # 继续传入或组合等待 取得查询结果中的一个值 完成后取得结果，完成上面尚未结束的表达式。
                    await session.scalar(
                        # 继续传入或组合调用 text 完成文字处理，完成上面尚未结束的表达式。
                        text("SELECT to_regclass(:table)"), {"table": "agent." + table}
                    )
                    # 继续传入或组合表达式 for table in tables 的结果，完成上面尚未结束的表达式。
                    for table in tables
                ]
                # 把执行“调用 all 完成全部处理”得到的结果保存到 已就绪名称（ready[name]）。
                ready[name] = all(value is not None for value in found)
    # 捕获(SQLAlchemyError, OSError, TimeoutError)异常，执行下面的错误处理。
    except (SQLAlchemyError, OSError, TimeoutError):
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("agent_storage_not_ready")
    # 把已就绪（ready）交还给调用方，本函数到此结束。
    return ready


# 让这个异步生成器管理资源的进入和退出。
@asynccontextmanager
# 异步函数 lifespan：调用 lifespan 完成lifespan处理；接收 应用（app）。
async def lifespan(app: FastAPI):
    # 把执行“调用 JavaApiClient 完成Java后端接口客户端处理”得到的结果保存到 应用中的当前状态中的Java后端客户端（app.state.java_client）。
    app.state.java_client = JavaApiClient(get_settings())
    # 把执行“调用 asyncio.Semaphore 完成并发名额控制器处理”得到的结果保存到 应用中的当前状态中的chatslots（app.state.chat_slots）。
    app.state.chat_slots = asyncio.Semaphore(get_settings().agent_max_concurrent_requests)
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 Database 完成数据库处理”得到的结果保存到 应用中的当前状态中的数据库（app.state.database）。
        app.state.database = Database(get_settings())
    # 捕获AgentError异常，执行下面的错误处理。
    except AgentError:
        # 未配置会话存储时 P1.1 单轮对话仍可用，会话接口返回 503。
        # 把空值，表示当前还没有对应内容保存到 应用中的当前状态中的数据库（app.state.database）。
        app.state.database = None
    # 把空值，表示当前还没有对应内容保存到 应用中的当前状态中的后台后台处理器（app.state.background_worker）。
    app.state.background_worker = None
    # 把等待执行“调用 storage_readiness 完成存储readiness处理”得到的结果完成后的结果保存到 应用中的当前状态中的存储readiness（app.state.storage_readiness）。
    app.state.storage_readiness = await storage_readiness(app.state.database)
    # 检查条件 app.state.database is not None and app.state.storage_readiness['planning-workflow'] 是否成立；成立时执行缩进的分支。
    if app.state.database is not None and app.state.storage_readiness["planning-workflow"]:
        # 把执行“调用 WorkflowGraph 完成规划工作流工作流图处理”得到的结果保存到 工作流图（graph）。
        graph = WorkflowGraph(app.state.database, get_settings())
        # 等待调用 graph.start 完成开始处理完成后再继续。
        await graph.start()
        # 把工作流图（graph）保存到 应用中的当前状态中的数据库中的规划工作流工作流图（app.state.database.workflow_graph）。
        app.state.database.workflow_graph = graph
    # 检查条件 app.state.database is not None and app.state.storage_readiness['knowledge'] 是否成立；成立时执行缩进的分支。
    if app.state.database is not None and app.state.storage_readiness["knowledge"]:
        # 把执行“调用 BackgroundWorker 完成后台后台处理器处理”得到的结果保存到 应用中的当前状态中的后台后台处理器（app.state.background_worker）。
        app.state.background_worker = BackgroundWorker(app.state.database)
        # 等待调用 app.state.background_worker.start 完成开始处理完成后再继续。
        await app.state.background_worker.start()
        # 把应用（app）的当前状态（state）属性的后台后台处理器（background_worker）属性保存到 应用中的当前状态中的数据库中的后台后台处理器（app.state.database.background_worker）。
        app.state.database.background_worker = app.state.background_worker
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
        yield
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 检查条件 app.state.background_worker is not None 是否成立；成立时执行缩进的分支。
        if app.state.background_worker is not None:
            # 等待调用 app.state.background_worker.stop 完成停止处理完成后再继续。
            await app.state.background_worker.stop()
        # 等待调用 shutdown_generation 完成shutdowngeneration处理完成后再继续。
        await shutdown_generation()
        # 检查条件 app.state.database is not None 是否成立；成立时执行缩进的分支。
        if app.state.database is not None:
            # 检查条件 getattr(app.state.database, 'workflow_graph', None) 是否成立；成立时执行缩进的分支。
            if getattr(app.state.database, "workflow_graph", None):
                # 等待释放连接、文件或客户端占用的资源完成后再继续。
                await app.state.database.workflow_graph.close()
            # 等待释放连接、文件或客户端占用的资源完成后再继续。
            await app.state.database.close()
        # 等待释放连接、文件或客户端占用的资源完成后再继续。
        await app.state.java_client.close()
        # 等待调用 close_llm_client 完成关闭语言模型客户端处理完成后再继续。
        await close_llm_client()


# 把执行“调用 FastAPI 完成Fast接口处理”得到的结果保存到 应用（app）。
app = FastAPI(
    # 传入标题（title），值为固定文字 'Asterway Agent'。
    title="Asterway Agent",
    # 传入说明（description），值为固定文字 'Asterway AI Growth Assistant'。
    description="Asterway AI Growth Assistant",
    # 传入版本号（version），值为固定文字 '0.1.0'。
    version="0.1.0",
    # 传入lifespan，值为lifespan。
    lifespan=lifespan,
)


# 把下面的函数登记为 HTTP 处理入口：app.middleware("http")。
@app.middleware("http")
# 异步函数 attach_trace：调用 attach_trace 完成attach追踪标识处理；接收 请求（request）、调用下一个（call_next）。
async def attach_trace(request: Request, call_next):
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 supplied。
    supplied = request.headers.get("X-Trace-ID", "")
    # 把条件 re.fullmatch('[A-Za-z0-9_-]{1,64}', supplied) 是否成立时使用supplied，否则使用执行“调用 uuid4 完成uuid4处理”得到的结果的hex属性保存到 请求中的当前状态中的追踪标识编号（request.state.trace_id）。
    request.state.trace_id = (
        # 继续传入或组合调用 supplied if re.fullmatch 完成fullmatch处理，完成上面尚未结束的表达式。
        supplied if re.fullmatch(r"[A-Za-z0-9_-]{1,64}", supplied) else uuid4().hex
    )
    # 把等待执行“调用 call_next 完成调用下一个处理”得到的结果完成后的结果保存到 响应（response）。
    response = await call_next(request)
    # 把请求（request）的当前状态（state）属性的追踪标识编号（trace_id）属性保存到 响应请求头集合X追踪标识编号（response.headers['X-Trace-ID']）。
    response.headers["X-Trace-ID"] = request.state.trace_id
    # 把响应（response）交还给调用方，本函数到此结束。
    return response


# 把下面的函数登记为 HTTP 处理入口：app.exception_handler(AgentError)。
@app.exception_handler(AgentError)
# 异步函数 agent_error：调用 agent_error 完成成长助手错误处理；接收 请求（request）、错误（error）。
async def agent_error(request: Request, error: AgentError):
    # 把按字段名保存多个值的字典保存到 内容（content）。
    content = {
        # 填写编码（code）字段，值为表达式 error.code 的结果。
        "code": error.code,
        # 填写消息（message）字段，值为表达式 error.message 的结果。
        "message": error.message,
        # 填写本次请求的追踪编号（traceId）字段，值为表达式 request.state.trace_id 的结果。
        "traceId": request.state.trace_id,
    }
    # 检查条件 error.data is not None 是否成立；成立时执行缩进的分支。
    if error.data is not None:
        # 把错误（error）的数据（data）属性保存到 内容数据（content['data']）。
        content["data"] = error.data
    # 把执行“调用 JSONResponse 完成JSON 格式的 HTTP 响应处理”得到的结果交还给调用方，本函数到此结束。
    return JSONResponse(
        # 传入HTTP 状态码（status_code），值为表达式 error.status_code 的结果。
        status_code=error.status_code,
        # 传入内容（content），值为内容（content）。
        content=content,
        # 传入请求头集合（headers），值为条件判断的结果：{"WWW-Authenticate": "Bearer"} if error.status_code == 401 else None。
        headers={"WWW-Authenticate": "Bearer"} if error.status_code == 401 else None,
    )


# 把下面的函数登记为 HTTP 处理入口：app.exception_handler(RequestValidationError)。
@app.exception_handler(RequestValidationError)
# 异步函数 invalid_request：调用 invalid_request 完成不合法请求处理；接收 请求（request）、错误（error）。
async def invalid_request(request: Request, error: RequestValidationError):
    # 不把输入内容原样写回错误结果，避免额外字段携带凭据或私密内容。
    # 把执行“调用 JSONResponse 完成JSON 格式的 HTTP 响应处理”得到的结果交还给调用方，本函数到此结束。
    return JSONResponse(
        # 传入HTTP 状态码（status_code），值为固定数值 422。
        status_code=422,
        # 传入内容（content），值为表达式 { 的结果。
        content={
            # 填写编码（code）字段，值为固定状态或编码 INVALID_REQUEST（不合法请求）。
            "code": "INVALID_REQUEST",
            # 填写消息（message）字段，值为固定文字 '请求参数不符合接口要求，请检查必填字段、格式和取值范围'。
            "message": "请求参数不符合接口要求，请检查必填字段、格式和取值范围",
            # 填写本次请求的追踪编号（traceId）字段，值为表达式 request.state.trace_id 的结果。
            "traceId": request.state.trace_id,
        },
    )


# 把下面的函数登记为 HTTP 处理入口：app.exception_handler(Exception)。
@app.exception_handler(Exception)
# 异步函数 unexpected_error：调用 unexpected_error 完成unexpected错误处理；接收 请求（request）、错误（error）。
async def unexpected_error(request: Request, error: Exception):
    # 把执行“读取 attr 的值”得到的结果保存到 追踪标识编号（trace_id）。
    trace_id = getattr(request.state, "trace_id", uuid4().hex)
    # 记录error级别的运行信息，供定位处理过程和失败原因。
    logger.error("agent_unexpected_error trace=%s type=%s", trace_id, type(error).__name__)
    # 把执行“调用 JSONResponse 完成JSON 格式的 HTTP 响应处理”得到的结果交还给调用方，本函数到此结束。
    return JSONResponse(
        # 传入HTTP 状态码（status_code），值为固定数值 500。
        status_code=500,
        # 传入内容（content），值为表达式 { 的结果。
        content={
            # 填写编码（code）字段，值为固定状态或编码 AGENT_INTERNAL_ERROR（成长助手内部错误）。
            "code": "AGENT_INTERNAL_ERROR",
            # 填写消息（message）字段，值为固定文字 '对话服务暂时不可用'。
            "message": "对话服务暂时不可用",
            # 填写本次请求的追踪编号（traceId）字段，值为追踪标识编号（trace_id）。
            "traceId": trace_id,
        },
    )


# 把下面的函数登记为 HTTP 处理入口：app.get("/health")。
@app.get("/health")
# 异步函数 health：调用 health 完成health处理；接收 请求（request）。
async def health(request: Request):
    # 把执行“读取 attr 的值”得到的结果保存到 已就绪（ready）。
    ready = getattr(request.app.state, "storage_readiness", {})
    # 把依次包含 固定文字 'chat'、固定文字 'sse'、固定文字 'tools' 的集合保存到 capabilities。
    capabilities = ["chat", "sse", "tools"]
    # 检查条件 getattr(getattr(request.app.state, 'database', None), 'workflow_graph', None) and get_settings().agent_tools_enabled 是否成立；成立时执行缩进的分支。
    if getattr(getattr(request.app.state, "database", None), "workflow_graph", None) \
            and get_settings().agent_tools_enabled:
        # 把新条目放到列表末尾。
        capabilities.append("conversation-planning")
    # 把另一组条目依次加入列表。
    capabilities.extend(name for name, available in ready.items() if available)
    # 检查条件 ready.get('knowledge') 是否成立；成立时执行缩进的分支。
    if ready.get("knowledge"):
        # 把新条目放到列表末尾。
        capabilities.append("background-tasks")
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写成功（success）字段，值为真，表示条件成立或开关打开。
        "success": True,
        # 填写业务服务（service）字段，值为固定文字 'asterway-agent'。
        "service": "asterway-agent",
        # 填写状态（status）字段，值为固定文字 'running'。
        "status": "running",
        # 填写当前步骤（phase）字段，值为固定文字 'P1.6'。
        "phase": "P1.6",
        # 填写capabilities字段，值为capabilities。
        "capabilities": capabilities,
        # 填写存储已就绪（storageReady）字段，值为已就绪（ready）。
        "storageReady": ready,
        # 填写向量Configured（embeddingConfigured）字段，值为仅在确实需要连接服务时取出私密配置中的实际值。
        "embeddingConfigured": bool(get_settings().embedding_api_key.get_secret_value()),
    }


# 调用 app.include_router 完成includerouter处理。
app.include_router(chat_router)
# 调用 app.include_router 完成includerouter处理。
app.include_router(conversation_router)
# 调用 app.include_router 完成includerouter处理。
app.include_router(memory_router)
# 调用 app.include_router 完成includerouter处理。
app.include_router(knowledge_router)
# 调用 app.include_router 完成includerouter处理。
app.include_router(workflow_router)

# 调用 app.include_router 完成includerouter处理。
app.include_router(workspace_router)
