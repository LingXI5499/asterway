# 提供workflows内部 HTTP 路由，校验当前用户后调用对应服务。
# 规划工作流内部接口：生成方案、修订、确认、取消、查看与恢复。

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）、只能从列出的固定值中选择的类型（Literal）。
from typing import Annotated, Literal

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、查询条件（Query）。
from fastapi import APIRouter, Depends, Query

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）。
from pydantic import BaseModel, ConfigDict, Field

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）、读取Java后端客户端（get_java_client）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, get_agent_context, get_java_client, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.session 引入 数据库（Database）、取得本次请求的 Agent 数据库访问器（get_database）。
from app.db.session import Database, get_database

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.repositories 引入 对话数据仓库（conversation_repository）。
from app.repositories import conversation_repository

# 从 app.services 引入 规划上下文业务服务（planning_context_service）、规划业务服务（planning_service）、规划工作流进度业务服务（workflow_progress_service）、规划工作流业务服务（workflow_service）。
from app.services import (
    # 继续传入或组合规划上下文业务服务（planning_context_service），完成上面尚未结束的表达式。
    planning_context_service,
    # 继续传入或组合规划业务服务（planning_service），完成上面尚未结束的表达式。
    planning_service,
    # 继续传入或组合规划工作流进度业务服务（workflow_progress_service），完成上面尚未结束的表达式。
    workflow_progress_service,
    # 继续传入或组合规划工作流业务服务（workflow_service），完成上面尚未结束的表达式。
    workflow_service,
)

# 从 app.services.workflow_graph 引入 require工作流图（require_graph）。
from app.services.workflow_graph import require_graph

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(prefix="/internal/agent/planning/proposals", tags=["Planning"])

# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 成长助手上下文（AgentContext）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 上下文Dep（ContextDep）。
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 Java后端接口客户端（JavaApiClient）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 客户端Dep（ClientDep）。
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 数据库（Database）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 数据库Dep（DbDep）。
DbDep = Annotated[Database, Depends(get_database)]


# 定义 ProposalRequest，把规划草案请求需要的数据与操作放在同一个对象中。
class ProposalRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)
    # 声明 消息（message），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    message: str = Field(min_length=1, max_length=8000)
    # 声明 对话编号（conversationId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    conversationId: int = Field(ge=1)
    # 声明 intent，类型为 str | None；其内容为空值，表示当前还没有对应内容。
    intent: str | None = None
    # 声明 来源本轮对话编号（sourceTurnId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    sourceTurnId: int | None = Field(default=None, ge=1)


# 定义 RevisionRequest，把修订版本请求需要的数据与操作放在同一个对象中。
class RevisionRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)
    # 声明 消息（message），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    message: str = Field(min_length=1, max_length=8000)


# 定义 VersionRequest，把版本号请求需要的数据与操作放在同一个对象中。
class VersionRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", strict=True)
    # 声明 方案版本号编号（planVersionId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    planVersionId: int = Field(ge=1)


# 函数 _user：调用 _user 完成用户处理；接收 上下文（context）。
def _user(context: AgentContext) -> int:
    # 把执行“调用 int 完成int处理”得到的结果交还给调用方，本函数到此结束。
    return int(context.user_id)


# 异步函数 _calendar：调用 _calendar 完成calendar处理；接收 上下文（context）、客户端（client）。
async def _calendar(context: AgentContext, client: JavaApiClient) -> tuple[str | None, str | None]:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果（result）。
    result = await client.get(
        # 继续传入或组合按表达式计算得到的值："dashboard/overview", authorization=context.authorization, trace_id=context.trace_id，完成上面尚未结束的表达式。
        "dashboard/overview", authorization=context.authorization, trace_id=context.trace_id
    )
    # 检查条件 not isinstance(result.data, dict) or not result.data.get('today') or (not result.data.get('timeZone')) 是否成立；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 not isinstance(result.data, dict) 的结果，完成上面尚未结束的表达式。
        not isinstance(result.data, dict)
        # 继续补充本次调用或判断：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        or not result.data.get("today")
        # 继续补充本次调用或判断：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        or not result.data.get("timeZone")
    ):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "无法取得账号日期与时区，请稍后重试")
    # 把依次包含 执行“调用 str 完成str处理”得到的结果、执行“调用 str 完成str处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return str(result.data["today"]), str(result.data["timeZone"])


# 异步函数 _assert_conversation：调用 _assert_conversation 完成assert对话处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def _assert_conversation(db: Database, user_id: int, conversation_id: int) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, user_id, conversation_id)


# 把下面的函数登记为 HTTP 处理入口：router.post("", status_code=201, summary="生成规划方案")。
@router.post("", status_code=201, summary="生成规划方案")
# 异步函数 create_proposal：创建 规划草案（proposal）；接收 提交数据（payload）、上下文（context）、客户端（client）、Agent 数据库访问器（db）。
async def create_proposal(
    # 填写提交数据（payload）字段，值为规划草案请求（ProposalRequest）。
    payload: ProposalRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写客户端（client）字段，值为客户端Dep（ClientDep）。
    client: ClientDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage")
    # 把执行“调用 _user 完成用户处理”得到的结果保存到 用户编号（user_id）。
    user_id = _user(context)
    # 把提交数据（payload）的intent属性或者执行“调用 planning_service.detect_intent 完成detectintent处理”得到的结果保存到 intent。
    intent = payload.intent or planning_service.detect_intent(payload.message)
    # 检查条件 intent not in workflow_service.INTENTS 是否成立；成立时执行缩进的分支。
    if intent not in workflow_service.INTENTS:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(
            # 继续传入或组合固定数值 422，完成上面尚未结束的表达式。
            422,
            # 继续传入或组合固定状态或编码 WORKFLOW_INTENT_REQUIRED（规划工作流INTENT必填），完成上面尚未结束的表达式。
            "WORKFLOW_INTENT_REQUIRED",
            # 继续传入或组合固定文字 '无法识别规划意图，请说明要规划目标、阶段、任务、周计划、日计划还是习惯'，完成上面尚未结束的表达式。
            "无法识别规划意图，请说明要规划目标、阶段、任务、周计划、日计划还是习惯",
        )
    # 等待调用 _assert_conversation 完成assert对话处理完成后再继续。
    await _assert_conversation(db, user_id, payload.conversationId)
    # 把等待执行“调用 _calendar 完成calendar处理”得到的结果完成后的结果保存到 今天时区（(today, zone)）。
    today, zone = await _calendar(context, client)
    # 把等待执行“调用 planning_context_service.collect 完成collect处理”得到的结果完成后的结果保存到 business上下文（business_context）。
    business_context = await planning_context_service.collect(context, client, intent)
    # 把等待执行“调用 planning_service.generate_snapshot 完成generate确认时的数据快照处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
    snapshot = await planning_service.generate_snapshot(
        # 传入intent，值为intent。
        intent=intent,
        # 传入消息（message），值为表达式 payload.message 的结果。
        message=payload.message,
        # 传入今天（today），值为今天（today）。
        today=today,
        # 传入用户使用的时区（time_zone），值为时区（zone）。
        time_zone=zone,
        # 传入business上下文（business_context），值为business上下文（business_context）。
        business_context=business_context,
    )
    # 把等待执行“调用 planning_context_service.verify_assignments 完成校验任务安排列表处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
    snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
    # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 fresh。
    fresh = await refresh_context(context, client)
    # 调用 fresh.require 完成require处理。
    fresh.require("agent:workflow:manage")
    # 把等待执行“调用 workflow_service.start_run 完成开始方案运行处理”得到的结果完成后的结果保存到 方案运行（run）。
    run = await workflow_service.start_run(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入对话编号（conversation_id），值为表达式 payload.conversationId 的结果。
        conversation_id=payload.conversationId,
        # 传入intent，值为intent。
        intent=intent,
        # 传入来源本轮对话编号（source_turn_id），值为表达式 payload.sourceTurnId 的结果。
        source_turn_id=payload.sourceTurnId,
    )
    # 把等待执行“保存 方案（plan）”得到的结果完成后的结果保存到 版本号（version）。
    version = await workflow_service.save_plan(
        # 继续传入或组合表达式 db, user_id=user_id, run_id=run.id, snapshot=snapshot 的结果，完成上面尚未结束的表达式。
        db, user_id=user_id, run_id=run.id, snapshot=snapshot
    )
    # 等待调用 require_graph(db).prepare 完成prepare处理完成后再继续。
    await require_graph(db).prepare(user_id, run.id, version.id)
    # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
    detail = await workflow_service.detail(db, user_id=user_id, run_id=run.id)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"run": detail["run"], "plan": workflow_service.plan_vo(version)}


# 把下面的函数登记为 HTTP 处理入口：router.post("/{run_id}/revisions", summary="修订规划方案")。
@router.post("/{run_id}/revisions", summary="修订规划方案")
# 异步函数 revise_proposal：调用 revise_proposal 完成revise规划草案处理；接收 方案运行编号（run_id）、提交数据（payload）、上下文（context）、客户端（client）、Agent 数据库访问器（db）。
async def revise_proposal(
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
    # 填写提交数据（payload）字段，值为修订版本请求（RevisionRequest）。
    payload: RevisionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写客户端（client）字段，值为客户端Dep（ClientDep）。
    client: ClientDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage")
    # 把执行“调用 _user 完成用户处理”得到的结果保存到 用户编号（user_id）。
    user_id = _user(context)
    # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
    detail = await workflow_service.detail(db, user_id=user_id, run_id=run_id)
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者空列表或集合保存到 方案列表（plans）。
    plans = detail.get("plans") or []
    # 把条件 plans 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 上一次（previous）。
    previous = plans[-1].get("snapshot") if plans else None
    # 检查条件 not isinstance(previous, dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(previous, dict):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "WORKFLOW_PLAN_MISSING", "当前方案没有可修订的版本")
    # 把执行“调用 str 完成str处理”得到的结果保存到 intent。
    intent = str(detail["run"]["intent"])
    # 等待调用 _assert_conversation 完成assert对话处理完成后再继续。
    await _assert_conversation(db, user_id, int(str(detail["run"]["conversationId"])))
    # 把等待执行“调用 _calendar 完成calendar处理”得到的结果完成后的结果保存到 今天时区（(today, zone)）。
    today, zone = await _calendar(context, client)
    # 把等待执行“调用 planning_context_service.collect 完成collect处理”得到的结果完成后的结果保存到 business上下文（business_context）。
    business_context = await planning_context_service.collect(context, client, intent)
    # 把等待执行“调用 planning_service.generate_snapshot 完成generate确认时的数据快照处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
    snapshot = await planning_service.generate_snapshot(
        # 传入intent，值为intent。
        intent=intent,
        # 传入消息（message），值为表达式 payload.message 的结果。
        message=payload.message,
        # 传入今天（today），值为今天（today）。
        today=today,
        # 传入用户使用的时区（time_zone），值为时区（zone）。
        time_zone=zone,
        # 传入上一次（previous），值为上一次（previous）。
        previous=previous,
        # 传入business上下文（business_context），值为business上下文（business_context）。
        business_context=business_context,
    )
    # 把等待执行“调用 planning_context_service.verify_assignments 完成校验任务安排列表处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
    snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
    # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 fresh。
    fresh = await refresh_context(context, client)
    # 调用 fresh.require 完成require处理。
    fresh.require("agent:workflow:manage")
    # 把等待执行“保存 方案（plan）”得到的结果完成后的结果保存到 版本号（version）。
    version = await workflow_service.save_plan(
        # 继续传入或组合表达式 db, user_id=user_id, run_id=run_id, snapshot=snapshot, revision=True 的结果，完成上面尚未结束的表达式。
        db, user_id=user_id, run_id=run_id, snapshot=snapshot, revision=True
    )
    # 等待调用 require_graph(db).prepare 完成prepare处理完成后再继续。
    await require_graph(db).prepare(user_id, run_id, version.id)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"plan": workflow_service.plan_vo(version)}


# 把下面的函数登记为 HTTP 处理入口：router.get("", summary="规划列表")。
@router.get("", summary="规划列表")
# 异步函数 page：调用 page 完成当前页处理；接收 上下文（context）、Agent 数据库访问器（db）、状态（status）、对话编号（conversation_id）、当前页数值（page_number）、每页条数（size）。
async def page(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写状态（status）字段，值为表达式 Annotated[ 的结果。
    status: Annotated[
        # 继续传入或组合表达式 Literal[ 的结果，完成上面尚未结束的表达式。
        Literal[
            # 继续传入或组合固定状态或编码 DRAFT（草案），完成上面尚未结束的表达式。
            "DRAFT",
            # 继续传入或组合固定状态或编码 WAITING_CONFIRM（等待确认确认），完成上面尚未结束的表达式。
            "WAITING_CONFIRM",
            # 继续传入或组合固定状态或编码 CONFIRMED（已确认），完成上面尚未结束的表达式。
            "CONFIRMED",
            # 继续传入或组合固定状态或编码 EXECUTING（正在执行），完成上面尚未结束的表达式。
            "EXECUTING",
            # 继续传入或组合固定状态或编码 SUCCEEDED（已成功），完成上面尚未结束的表达式。
            "SUCCEEDED",
            # 继续传入或组合固定状态或编码 FAILED（失败），完成上面尚未结束的表达式。
            "FAILED",
            # 继续传入或组合固定状态或编码 CANCELLED（已取消），完成上面尚未结束的表达式。
            "CANCELLED",
            # 继续传入或组合固定状态或编码 EXPIRED（已过期），完成上面尚未结束的表达式。
            "EXPIRED",
        ]
        # 继续传入或组合表达式 | None 的结果，完成上面尚未结束的表达式。
        | None,
        # 继续传入或组合调用 Query 完成查询条件处理，完成上面尚未结束的表达式。
        Query(),
    # 继续传入或组合表达式 ] = None 的结果，完成上面尚未结束的表达式。
    ] = None,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int | None, Query(alias="conversationId", ge=1)] = None 的结果。
    conversation_id: Annotated[int | None, Query(alias="conversationId", ge=1)] = None,
    # 填写当前页数值（page_number）字段，值为表达式 Annotated[int, Query(alias="page", ge=1)] = 1 的结果。
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    # 填写每页条数（size）字段，值为表达式 Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    size: Annotated[int, Query(ge=1, le=100)] = 20,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:read")
    # 把等待执行“调用 workflow_service.page_runs 完成当前页运行记录列表处理”得到的结果完成后的结果保存到 运行记录列表总数（(runs, total)）。
    runs, total = await workflow_service.page_runs(
        # 继续传入或组合表达式 db, user_id=_user(context), status=status, page=page_number, size=size 的结果，完成上面尚未结束的表达式。
        db, user_id=_user(context), status=status, page=page_number, size=size,
        # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
        conversation_id=conversation_id,
    )
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写数据列表（items）字段，值为调用 [workflow_service.run_vo 完成方案运行处理。
        "items": [workflow_service.run_vo(row) for row in runs],
        # 填写总数（total）字段，值为总数（total）。
        "total": total,
        # 填写当前页（page）字段，值为当前页数值（page_number）。
        "page": page_number,
        # 填写每页条数（size）字段，值为每页条数（size）。
        "size": size,
    }


# 把下面的函数登记为 HTTP 处理入口：router.get("/{run_id}", summary="规划详情")。
@router.get("/{run_id}", summary="规划详情")
# 异步函数 detail：调用 detail 完成详情处理；接收 方案运行编号（run_id）、上下文（context）、Agent 数据库访问器（db）。
async def detail(run_id: int, context: ContextDep, db: DbDep) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:read")
    # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await workflow_service.detail(db, user_id=_user(context), run_id=run_id)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{run_id}/confirm", summary="确认方案")。
@router.post("/{run_id}/confirm", summary="确认方案")
# 异步函数 confirm：调用 confirm 完成确认处理；接收 方案运行编号（run_id）、提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def confirm(
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
    # 填写提交数据（payload）字段，值为版本号请求（VersionRequest）。
    payload: VersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage")
    # 把等待执行“确认 方案（plan）”得到的结果完成后的结果保存到 版本号（version）。
    version = await workflow_service.confirm_plan(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 传入用户编号（user_id），值为调用 _user 完成用户处理。
        user_id=_user(context),
        # 传入方案运行编号（run_id），值为方案运行编号（run_id）。
        run_id=run_id,
        # 传入方案版本号编号（plan_version_id），值为表达式 payload.planVersionId 的结果。
        plan_version_id=payload.planVersionId,
    )
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"plan": workflow_service.plan_vo(version)}


# 把下面的函数登记为 HTTP 处理入口：router.get("/{run_id}/progress", summary="方案关联对象的真实进度与执行反馈")。
@router.get("/{run_id}/progress", summary="方案关联对象的真实进度与执行反馈")
# 异步函数 progress：调用 progress 完成进度处理；接收 方案运行编号（run_id）、上下文（context）、Agent 数据库访问器（db）、客户端（client）。
async def progress(run_id: int, context: ContextDep, db: DbDep, client: ClientDep):
    # 把等待执行“调用 workflow_progress_service.collect 完成collect处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await workflow_progress_service.collect(db, context, client, run_id)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{run_id}/execute", summary="确认后执行方案提交")。
@router.post("/{run_id}/execute", summary="确认后执行方案提交")
# 异步函数 execute：执行传入的数据库语句并取得结果；接收 方案运行编号（run_id）、上下文（context）、客户端（client）、Agent 数据库访问器（db）。
async def execute(
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写客户端（client）字段，值为客户端Dep（ClientDep）。
    client: ClientDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> dict[str, object]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, object]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage")
    # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
    detail = await workflow_service.detail(db, user_id=_user(context), run_id=run_id)
    # 把从详情（detail）中按固定文字 'run'取出的内容保存到 方案运行（run）。
    run = detail["run"]
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 提交回执（commit）。
    commit = detail.get("commit")
    # 检查条件 commit is not None and commit['status'] == 'SUCCEEDED' 是否成立；成立时执行缩进的分支。
    if commit is not None and commit["status"] == "SUCCEEDED":
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"commit": commit}
    # 检查条件 commit is not None 是否成立；成立时执行缩进的分支。
    if commit is not None:
        # 确认已完成但上次提交未成功：沿用原幂等键直接重试，不重复确认、不重复写入。
        # 把等待执行“调用 workflow_service.execute_commit 完成执行提交回执处理”得到的结果完成后的结果保存到 提交回执（receipt）。
        receipt = await workflow_service.execute_commit(
            # 继续传入或组合表达式 db, context=context, java_client=client, run_id=run_id 的结果，完成上面尚未结束的表达式。
            db, context=context, java_client=client, run_id=run_id
        )
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"commit": workflow_service.commit_vo(receipt)}
    # 检查条件 run['status'] in ('DRAFT', 'WAITING_CONFIRM') 是否成立；成立时执行缩进的分支。
    if run["status"] in ("DRAFT", "WAITING_CONFIRM"):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "WORKFLOW_NOT_CONFIRMED", "请先确认最新方案再执行")
    # 检查记录或凭据是否已经到期：run['status'] in ('CANCELLED', 'EXPIRED')；成立时执行缩进的分支。
    if run["status"] in ("CANCELLED", "EXPIRED"):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "WORKFLOW_STATUS_INVALID", "方案已取消或过期，不能执行")
    # 检查条件 run['status'] == 'FAILED' 是否成立；成立时执行缩进的分支。
    if run["status"] == "FAILED":
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "PLAN_COMMIT_MISSING", "失败状态没有可重试的提交记录，请重新生成方案")
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"commit": (await require_graph(db).execute(context, client, run_id))["commit"]}


# 把下面的函数登记为 HTTP 处理入口：router.post("/{run_id}/cancel", summary="取消规划")。
@router.post("/{run_id}/cancel", summary="取消规划")
# 异步函数 cancel：调用 cancel 完成取消处理；接收 方案运行编号（run_id）、上下文（context）、Agent 数据库访问器（db）。
async def cancel(run_id: int, context: ContextDep, db: DbDep) -> dict[str, bool]:
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage")
    # 等待取消 方案运行（run）完成后再继续。
    await workflow_service.cancel_run(db, user_id=_user(context), run_id=run_id)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"success": True}
