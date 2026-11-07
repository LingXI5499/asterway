"""LangGraph 持久化确认边界；检查点只保存业务 ID，不保存登录凭据。"""

from dataclasses import dataclass
from typing import TypedDict

from langgraph.checkpoint.postgres.aio import AsyncPostgresSaver
from langgraph.graph import END, START, StateGraph
from langgraph.runtime import Runtime
from langgraph.types import Command, interrupt
from psycopg.conninfo import make_conninfo
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool
from sqlalchemy.engine import make_url

from app.core.config import Settings
from app.core.context import AgentContext, refresh_context
from app.core.errors import AgentError
from app.db.session import Database
from app.integrations.java_client import JavaApiClient
from app.services import workflow_service


class GraphState(TypedDict, total=False):
    user_id: int
    run_id: int
    plan_version_id: int
    commit_id: str
    verified: bool


@dataclass
class GraphContext:
    identity: AgentContext
    java_client: JavaApiClient


class WorkflowGraph:
    def __init__(self, db: Database, settings: Settings):
        self.db = db
        url = make_url(settings.agent_database_url.get_secret_value())
        info = make_conninfo(host=url.host, port=url.port or 5432, dbname=url.database,
            user=url.username, password=url.password, options="-c search_path=agent,public")
        self.pool = AsyncConnectionPool(info, min_size=1, max_size=4, open=False,
            kwargs={"autocommit": True, "prepare_threshold": 0, "row_factory": dict_row})
        builder = StateGraph(GraphState, context_schema=GraphContext)
        builder.add_node("await_confirmation", self._wait)
        builder.add_node("commit_business", self._commit)
        builder.add_node("verify_result", self._verify)
        builder.add_edge(START, "await_confirmation")
        builder.add_edge("await_confirmation", "commit_business")
        builder.add_edge("commit_business", "verify_result")
        builder.add_edge("verify_result", END)
        self.graph = builder.compile(checkpointer=AsyncPostgresSaver(self.pool))

    async def start(self):
        # 最终表由 agent.sql 手工初始化，禁止在运行时调用 saver.setup()。
        await self.pool.open(wait=True, timeout=10)

    async def close(self):
        await self.pool.close()

    @staticmethod
    def config(user_id: int, run_id: int, plan_id: int) -> dict:
        return {"configurable": {"thread_id": f"u{user_id}:run{run_id}:plan{plan_id}"}}

    async def _wait(self, state: GraphState):
        value = interrupt({"type": "CONFIRM_PLAN", "planVersionId": str(state["plan_version_id"])})
        if value != {"confirmedPlanVersionId": str(state["plan_version_id"])}:
            raise AgentError(409, "PLAN_CONFIRMATION_INVALID", "必须确认当前版本后再执行")
        return {}

    async def _commit(self, state: GraphState, runtime: Runtime[GraphContext]):
        identity = await refresh_context(runtime.context.identity, runtime.context.java_client)
        identity.require("agent:workflow:manage")
        if int(identity.user_id) != state["user_id"]:
            raise AgentError(404, "WORKFLOW_NOT_FOUND", "规划不存在")
        detail = await workflow_service.detail(self.db, user_id=state["user_id"], run_id=state["run_id"])
        if str(detail["run"]["latestPlanVersionId"]) != str(state["plan_version_id"]):
            raise AgentError(409, "PLAN_VERSION_CONFLICT", "方案已经修订，请确认最新版本")
        commit = await workflow_service.execute_commit(self.db, context=identity,
            java_client=runtime.context.java_client, run_id=state["run_id"])
        return {"commit_id": str(commit.id)}

    async def _verify(self, state: GraphState):
        detail = await workflow_service.detail(self.db, user_id=state["user_id"], run_id=state["run_id"])
        receipt = detail.get("commit")
        if not receipt or receipt["status"] != "SUCCEEDED" or str(receipt["id"]) != state["commit_id"]:
            raise AgentError(409, "PLAN_COMMIT_PENDING", "提交结果尚未核实，请刷新后重试")
        return {"verified": True}

    async def prepare(self, user_id: int, run_id: int, plan_id: int):
        config = self.config(user_id, run_id, plan_id)
        state = await self.graph.aget_state(config)
        if not state.values:
            await self.graph.ainvoke({"user_id": user_id, "run_id": run_id,
                "plan_version_id": plan_id}, config)

    async def execute(self, identity: AgentContext, client: JavaApiClient, run_id: int):
        user_id = int(identity.user_id)
        detail = await workflow_service.detail(self.db, user_id=user_id, run_id=run_id)
        plan_id = int(detail["run"]["latestPlanVersionId"])
        await self.prepare(user_id, run_id, plan_id)
        config = self.config(user_id, run_id, plan_id)
        state = await self.graph.aget_state(config)
        if not state.values.get("verified"):
            paused = any(task.interrupts for task in state.tasks)
            command = Command(resume={"confirmedPlanVersionId": str(plan_id)}) if paused else None
            await self.graph.ainvoke(command, config, context=GraphContext(identity, client))
        # 权威结果来自 Java 事务回执，浏览器不会收到图内部状态。
        return await workflow_service.detail(self.db, user_id=user_id, run_id=run_id)


def require_graph(db: Database) -> WorkflowGraph:
    graph = getattr(db, "workflow_graph", None)
    if graph is None:
        raise AgentError(503, "WORKFLOW_STORAGE_NOT_READY", "工作流持久化尚未就绪")
    return graph
