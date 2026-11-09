# 把规划确认流程接入可恢复的工作流检查点，重启后仍能识别待确认状态。
# LangGraph 持久化确认边界；检查点只保存业务 ID，不保存登录凭据。

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 typing 引入 按字段名约定类型的字典结构（TypedDict）。
from typing import TypedDict

# 从 langgraph.checkpoint.postgres.aio 引入 异步PostgresSaver（AsyncPostgresSaver）。
from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver

# 从 langgraph.graph 引入 结束（END）、开始（START）、当前状态工作流图（StateGraph）。
from langgraph.graph import END, START, StateGraph

# 从 langgraph.runtime 引入 Runtime。
from langgraph.runtime import Runtime

# 从 langgraph.types 引入 Command、interrupt。
from langgraph.types import Command, interrupt

# 从 psycopg.conninfo 引入 makeconninfo（make_conninfo）。
from psycopg.conninfo import make_conninfo

# 从 psycopg.rows 引入 dict当前记录（dict_row）。
from psycopg.rows import dict_row

# 从 psycopg_pool 引入 异步数据库连接连接池（AsyncConnectionPool）。
from psycopg_pool import AsyncConnectionPool

# 从 sqlalchemy.engine 引入 make地址（make_url）。
from sqlalchemy.engine import make_url

# 从 app.core.config 引入 配置（Settings）。
from app.core.config import Settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.services 引入 规划工作流业务服务（workflow_service）。
from app.services import workflow_service


# 定义 GraphState，把工作流图当前状态需要的数据与操作放在同一个对象中。
class GraphState(TypedDict, total=False):
    # 声明 用户编号（user_id），类型为 int。
    user_id: int
    # 声明 方案运行编号（run_id），类型为 int。
    run_id: int
    # 声明 方案版本号编号（plan_version_id），类型为 int。
    plan_version_id: int
    # 声明 提交回执编号（commit_id），类型为 str。
    commit_id: str
    # 声明 verified，类型为 bool。
    verified: bool


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass
# 定义 GraphContext，把工作流图上下文需要的数据与操作放在同一个对象中。
class GraphContext:
    # 声明 账户管理（identity），类型为 AgentContext。
    identity: AgentContext
    # 声明 Java后端客户端（java_client），类型为 JavaApiClient。
    java_client: JavaApiClient


# 定义 WorkflowGraph，把规划工作流工作流图需要的数据与操作放在同一个对象中。
class WorkflowGraph:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 Agent 数据库访问器（db）、配置（settings）。
    def __init__(self, db: Database, settings: Settings):
        # 把Agent 数据库访问器（db）保存到 当前实例中的Agent 数据库访问器（self.db）。
        self.db = db
        # 把执行“调用 make_url 完成make地址处理”得到的结果保存到 地址（url）。
        url = make_url(settings.agent_database_url.get_secret_value())
        # 把执行“调用 make_conninfo 完成makeconninfo处理”得到的结果保存到 信息（info）。
        info = make_conninfo(host=url.host, port=url.port or 5432, dbname=url.database,
            # 传入用户（user），值为表达式 url.username, password=url.password, options="-c search_path=agent,public") 的结果。
            user=url.username, password=url.password, options="-c search_path=agent,public")
        # 把执行“调用 AsyncConnectionPool 完成异步数据库连接连接池处理”得到的结果保存到 当前实例中的连接池（self.pool）。
        self.pool = AsyncConnectionPool(info, min_size=1, max_size=4, open=False,
            # 传入kwargs，值为表达式 {"autocommit": True, "prepare_threshold": 0, "row_factory": dict_row}) 的结果。
            kwargs={"autocommit": True, "prepare_threshold": 0, "row_factory": dict_row})
        # 把执行“调用 StateGraph 完成当前状态工作流图处理”得到的结果保存到 builder。
        builder = StateGraph(GraphState, context_schema=GraphContext)
        # 调用 builder.add_node 完成加入节点处理。
        builder.add_node("await_confirmation", self._wait)
        # 调用 builder.add_node 完成加入节点处理。
        builder.add_node("commit_business", self._commit)
        # 调用 builder.add_node 完成加入节点处理。
        builder.add_node("verify_result", self._verify)
        # 调用 builder.add_edge 完成加入edge处理。
        builder.add_edge(START, "await_confirmation")
        # 调用 builder.add_edge 完成加入edge处理。
        builder.add_edge("await_confirmation", "commit_business")
        # 调用 builder.add_edge 完成加入edge处理。
        builder.add_edge("commit_business", "verify_result")
        # 调用 builder.add_edge 完成加入edge处理。
        builder.add_edge("verify_result", END)
        # 把执行“调用 builder.compile 完成compile处理”得到的结果保存到 当前实例中的工作流图（self.graph）。
        self.graph = builder.compile(checkpointer=AsyncPostgresSaver(self.pool))

    # 异步函数 start：调用 start 完成开始处理。
    async def start(self):
        # 最终表由 agent.sql 手工初始化，禁止在运行时调用 saver.setup()。
        # 等待调用 self.pool.open 完成是否展开处理完成后再继续。
        await self.pool.open(wait=True, timeout=10)

    # 异步函数 close：释放连接、文件或客户端占用的资源。
    async def close(self):
        # 等待释放连接、文件或客户端占用的资源完成后再继续。
        await self.pool.close()

    # 这个方法不依赖具体实例，可以直接通过类调用。
    @staticmethod
    # 函数 config：调用 config 完成config处理；接收 用户编号（user_id）、方案运行编号（run_id）、方案编号（plan_id）。
    def config(user_id: int, run_id: int, plan_id: int) -> dict:
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"configurable": {"thread_id": f"u{user_id}:run{run_id}:plan{plan_id}"}}

    # 异步函数 _wait：调用 _wait 完成等待处理；接收 当前状态（state）。
    async def _wait(self, state: GraphState):
        # 把执行“调用 interrupt 完成interrupt处理”得到的结果保存到 值（value）。
        value = interrupt({"type": "CONFIRM_PLAN", "planVersionId": str(state["plan_version_id"])})
        # 检查当前记录版本是否符合请求要求：value != {'confirmedPlanVersionId': str(state['plan_version_id'])}；成立时执行缩进的分支。
        if value != {"confirmedPlanVersionId": str(state["plan_version_id"])}:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "PLAN_CONFIRMATION_INVALID", "必须确认当前版本后再执行")
        # 把按字段名保存多个值的字典，这里先创建空字典交还给调用方，本函数到此结束。
        return {}

    # 异步函数 _commit：调用 _commit 完成提交回执处理；接收 当前状态（state）、runtime。
    async def _commit(self, state: GraphState, runtime: Runtime[GraphContext]):
        # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 账户管理（identity）。
        identity = await refresh_context(runtime.context.identity, runtime.context.java_client)
        # 调用 identity.require 完成require处理。
        identity.require("agent:workflow:manage")
        # 检查条件 int(identity.user_id) != state['user_id'] 是否成立；成立时执行缩进的分支。
        if int(identity.user_id) != state["user_id"]:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "WORKFLOW_NOT_FOUND", "规划不存在")
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
        detail = await workflow_service.detail(self.db, user_id=state["user_id"], run_id=state["run_id"])
        # 检查当前记录版本是否符合请求要求：str(detail['run']['latestPlanVersionId']) != str(state['plan_version_id'])；成立时执行缩进的分支。
        if str(detail["run"]["latestPlanVersionId"]) != str(state["plan_version_id"]):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "PLAN_VERSION_CONFLICT", "方案已经修订，请确认最新版本")
        # 把等待执行“调用 workflow_service.execute_commit 完成执行提交回执处理”得到的结果完成后的结果保存到 提交回执（commit）。
        commit = await workflow_service.execute_commit(self.db, context=identity,
            # 传入Java后端客户端（java_client），值为表达式 runtime.context.java_client, run_id=state["run_id"]) 的结果。
            java_client=runtime.context.java_client, run_id=state["run_id"])
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"commit_id": str(commit.id)}

    # 异步函数 _verify：调用 _verify 完成校验处理；接收 当前状态（state）。
    async def _verify(self, state: GraphState):
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
        detail = await workflow_service.detail(self.db, user_id=state["user_id"], run_id=state["run_id"])
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交回执（receipt）。
        receipt = detail.get("commit")
        # 检查条件 not receipt or receipt['status'] != 'SUCCEEDED' or str(receipt['id']) != state['commit_id'] 是否成立；成立时执行缩进的分支。
        if not receipt or receipt["status"] != "SUCCEEDED" or str(receipt["id"]) != state["commit_id"]:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "PLAN_COMMIT_PENDING", "提交结果尚未核实，请刷新后重试")
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"verified": True}

    # 异步函数 prepare：调用 prepare 完成prepare处理；接收 用户编号（user_id）、方案运行编号（run_id）、方案编号（plan_id）。
    async def prepare(self, user_id: int, run_id: int, plan_id: int):
        # 把执行“调用 self.config 完成config处理”得到的结果保存到 config。
        config = self.config(user_id, run_id, plan_id)
        # 把等待执行“调用 self.graph.aget_state 完成aget当前状态处理”得到的结果完成后的结果保存到 当前状态（state）。
        state = await self.graph.aget_state(config)
        # 检查条件 not state.values 是否成立；成立时执行缩进的分支。
        if not state.values:
            # 等待调用 self.graph.ainvoke 完成ainvoke处理完成后再继续。
            await self.graph.ainvoke({"user_id": user_id, "run_id": run_id,
                # 填写方案版本号编号（plan_version_id）字段，值为表达式 plan_id}, config) 的结果。
                "plan_version_id": plan_id}, config)

    # 异步函数 execute：执行传入的数据库语句并取得结果；接收 账户管理（identity）、客户端（client）、方案运行编号（run_id）。
    async def execute(self, identity: AgentContext, client: JavaApiClient, run_id: int):
        # 把执行“调用 int 完成int处理”得到的结果保存到 用户编号（user_id）。
        user_id = int(identity.user_id)
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
        detail = await workflow_service.detail(self.db, user_id=user_id, run_id=run_id)
        # 把执行“调用 int 完成int处理”得到的结果保存到 方案编号（plan_id）。
        plan_id = int(detail["run"]["latestPlanVersionId"])
        # 等待调用 self.prepare 完成prepare处理完成后再继续。
        await self.prepare(user_id, run_id, plan_id)
        # 把执行“调用 self.config 完成config处理”得到的结果保存到 config。
        config = self.config(user_id, run_id, plan_id)
        # 把等待执行“调用 self.graph.aget_state 完成aget当前状态处理”得到的结果完成后的结果保存到 当前状态（state）。
        state = await self.graph.aget_state(config)
        # 检查条件 not state.values.get('verified') 是否成立；成立时执行缩进的分支。
        if not state.values.get("verified"):
            # 把执行“调用 any 完成any处理”得到的结果保存到 已暂停（paused）。
            paused = any(task.interrupts for task in state.tasks)
            # 把条件 paused 是否成立时使用执行“调用 Command 完成Command处理”得到的结果，否则使用空值，表示当前还没有对应内容保存到 command。
            command = Command(resume={"confirmedPlanVersionId": str(plan_id)}) if paused else None
            # 等待调用 self.graph.ainvoke 完成ainvoke处理完成后再继续。
            await self.graph.ainvoke(command, config, context=GraphContext(identity, client))
        # 权威结果来自 Java 事务回执，浏览器不会收到图内部状态。
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await workflow_service.detail(self.db, user_id=user_id, run_id=run_id)


# 函数 require_graph：要求满足 工作流图（graph）；接收 Agent 数据库访问器（db）。
def require_graph(db: Database) -> WorkflowGraph:
    # 把执行“读取 attr 的值”得到的结果保存到 工作流图（graph）。
    graph = getattr(db, "workflow_graph", None)
    # 检查graph is None，即对应内容是否不存在；成立时执行缩进的分支。
    if graph is None:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "WORKFLOW_STORAGE_NOT_READY", "工作流持久化尚未就绪")
    # 把工作流图（graph）交还给调用方，本函数到此结束。
    return graph
