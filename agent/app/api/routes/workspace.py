# 提供工作区内部 HTTP 路由，校验当前用户后调用对应服务。
# 成长复盘与本人运行记录，均由 Java 认证代理转发。

# 从 datetime 引入 日期（date）。
from datetime import date

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）。
from typing import Annotated

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、查询条件（Query）、本次 HTTP 请求（Request）。
from fastapi import APIRouter, Depends, Query, Request

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、模型validator（model_validator）。
from pydantic import BaseModel, ConfigDict, Field, model_validator

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）、读取Java后端客户端（get_java_client）。
from app.core.context import AgentContext, get_agent_context, get_java_client

# 从 app.db.session 引入 数据库（Database）、取得本次请求的 Agent 数据库访问器（get_database）。
from app.db.session import Database, get_database

# 从 app.db.workspace_models 引入 调用记录（CallRecord）、复盘复盘报告（ReviewReport）。
from app.db.workspace_models import CallRecord, ReviewReport

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.repositories 引入 任务列表（tasks）。
from app.repositories import background_task_repository as tasks

# 从 app.services 引入 后台recovery（background_recovery）、知识资料业务服务（knowledge_service）、复盘规划业务服务（review_planning_service）、复盘业务服务（review_service）。
from app.services import (
    # 继续传入或组合后台recovery（background_recovery），完成上面尚未结束的表达式。
    background_recovery,
    # 继续传入或组合知识资料业务服务（knowledge_service），完成上面尚未结束的表达式。
    knowledge_service,
    # 继续传入或组合复盘规划业务服务（review_planning_service），完成上面尚未结束的表达式。
    review_planning_service,
    # 继续传入或组合复盘业务服务（review_service），完成上面尚未结束的表达式。
    review_service,
)

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(prefix="/internal/agent", tags=["Workspace"])
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 成长助手上下文（AgentContext）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 上下文Dep（ContextDep）。
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 数据库（Database）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 数据库Dep（DbDep）。
DbDep = Annotated[Database, Depends(get_database)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 Java后端接口客户端（JavaApiClient）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 客户端Dep（ClientDep）。
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]


# 定义 ReviewRequest，把复盘请求需要的数据与操作放在同一个对象中。
class ReviewRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid")
    # 声明 日期来源（dateFrom），类型为 date。
    dateFrom: date
    # 声明 日期To（dateTo），类型为 date。
    dateTo: date
    # 声明 对话编号（conversationId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    conversationId: int | None = Field(default=None, ge=1)
    # 声明 来源方案运行编号（sourceRunId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    sourceRunId: int | None = Field(default=None, ge=1)

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_range：检查 区间（range）。
    def validate_range(self):
        # 检查条件 not 0 <= (self.dateTo - self.dateFrom).days <= 365 是否成立；成立时执行缩进的分支。
        if not 0 <= (self.dateTo-self.dateFrom).days <= 365:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("复盘区间应为1至366天")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 把下面的函数登记为 HTTP 处理入口：router.post("/reviews", status_code=201)。
@router.post("/reviews", status_code=201)
# 异步函数 create_review：创建 复盘（review）；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）、客户端（client）。
async def create_review(payload: ReviewRequest, context: ContextDep, db: DbDep, client: ClientDep):
    # 把等待执行“调用 review_service.create 完成创建处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await review_service.create(db, context, client, payload.dateFrom, payload.dateTo,
        # 传入对话编号（conversation_id），值为表达式 payload.conversationId, source_run_id=payload.sourceRunId) 的结果。
        conversation_id=payload.conversationId, source_run_id=payload.sourceRunId)
    # 把执行“调用 review_service.vo 完成返回页面的数据处理”得到的结果交还给调用方，本函数到此结束。
    return review_service.vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.post("/reviews/{report_id}/retry")。
@router.post("/reviews/{report_id}/retry")
# 异步函数 retry_review：重试 复盘（review）；接收 复盘报告编号（report_id）、上下文（context）、Agent 数据库访问器（db）。
async def retry_review(report_id: int, context: ContextDep, db: DbDep):
    # 调用 context.require 完成require处理。
    context.require("agent:review:manage", "statistics:report:read")
    # 把等待执行“调用 review_service.retry 完成重试处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await review_service.retry(db, int(context.user_id), report_id)
    # 把执行“调用 review_service.vo 完成返回页面的数据处理”得到的结果交还给调用方，本函数到此结束。
    return review_service.vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.get("/reviews")。
@router.get("/reviews")
# 异步函数 list_reviews：列出 复盘列表（reviews）；接收 上下文（context）、Agent 数据库访问器（db）、当前页（page）、每页条数（size）。
async def list_reviews(context: ContextDep, db: DbDep,
    # 填写当前页（page）字段，值为表达式 Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20): 的结果。
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20):
    # 调用 context.require 完成require处理。
    context.require("agent:review:read")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把执行“调用 select(ReviewReport).where 完成where处理”得到的结果保存到 查询条件（query）。
        query = select(ReviewReport).where(ReviewReport.user_id == int(context.user_id))
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
        total = await session.scalar(select(func.count()).select_from(query.subquery()))
        # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
        rows = await session.scalars(query.order_by(ReviewReport.id.desc()).offset((page-1)*size).limit(size))
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"items": [review_service.vo(row) for row in rows], "total": total, "page": page, "size": size}


# 把下面的函数登记为 HTTP 处理入口：router.get("/reviews/{report_id}")。
@router.get("/reviews/{report_id}")
# 异步函数 get_review：读取 复盘（_review） 的值；接收 复盘报告编号（report_id）、上下文（context）、Agent 数据库访问器（db）。
async def get_review(report_id: int, context: ContextDep, db: DbDep):
    # 调用 context.require 完成require处理。
    context.require("agent:review:read")
    # 把执行“调用 review_service.vo 完成返回页面的数据处理”得到的结果交还给调用方，本函数到此结束。
    return review_service.vo(await review_service.get(db, int(context.user_id), report_id))


# 把下面的函数登记为 HTTP 处理入口：router.get("/reviews/latest/conversation/{conversation_id}")。
@router.get("/reviews/latest/conversation/{conversation_id}")
# 异步函数 conversation_review：调用 conversation_review 完成对话复盘处理；接收 对话编号（conversation_id）、上下文（context）、Agent 数据库访问器（db）。
async def conversation_review(conversation_id: int, context: ContextDep, db: DbDep):

    # 从 app.repositories 引入 对话数据仓库（conversation_repository）。
    from app.repositories import conversation_repository
    # 调用 context.require 完成require处理。
    context.require("agent:review:read")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, int(context.user_id), conversation_id)
    # 把等待执行“调用 review_service.latest_for_conversation 完成最新for对话处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await review_service.latest_for_conversation(db, int(context.user_id), conversation_id)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"review": review_service.vo(row) if row else None}


# 定义 ReviewProposalRequest，把复盘规划草案请求需要的数据与操作放在同一个对象中。
class ReviewProposalRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid")
    # 声明 对话编号（conversationId），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    conversationId: int = Field(ge=1)
    # 声明 消息（message），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    message: str = Field(default="根据这次复盘调整接下来两周的任务安排，保留已有目标和任务，说明每项改期原因。", min_length=1, max_length=2000)


# 把下面的函数登记为 HTTP 处理入口：router.post("/reviews/{report_id}/proposals", status_code=201)。
@router.post("/reviews/{report_id}/proposals", status_code=201)
# 异步函数 review_proposal：调用 review_proposal 完成复盘规划草案处理；接收 复盘报告编号（report_id）、提交数据（payload）、请求（request）、上下文（context）、Agent 数据库访问器（db）、客户端（client）。
async def review_proposal(report_id: int, payload: ReviewProposalRequest, request: Request,
                          # 填写上下文（context）字段，值为表达式 ContextDep, db: DbDep, client: ClientDep): 的结果。
                          context: ContextDep, db: DbDep, client: ClientDep):
    # 把等待执行“调用 review_planning_service.draft 完成草案处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await review_planning_service.draft(db, context, client, report_id=report_id,
        # 传入对话编号（conversation_id），值为表达式 payload.conversationId, message=payload.message, slots=request.app.state.chat_slots) 的结果。
        conversation_id=payload.conversationId, message=payload.message, slots=request.app.state.chat_slots)


# 把下面的函数登记为 HTTP 处理入口：router.get("/runs/calls")。
@router.get("/runs/calls")
# 异步函数 calls：调用 calls 完成调用列表处理；接收 上下文（context）、Agent 数据库访问器（db）、当前页（page）、每页条数（size）。
async def calls(context: ContextDep, db: DbDep,
    # 填写当前页（page）字段，值为表达式 Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20): 的结果。
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20):
    # 调用 context.require 完成require处理。
    context.require("agent:run:read")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把执行“调用 select(CallRecord).where 完成where处理”得到的结果保存到 查询条件（query）。
        query = select(CallRecord).where(CallRecord.user_id == int(context.user_id))
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
        total = await session.scalar(select(func.count()).select_from(query.subquery()))
        # 把执行“调用 list 完成列表处理”得到的结果保存到 记录列表（rows）。
        rows = list(await session.scalars(query.order_by(CallRecord.id.desc()).offset((page-1)*size).limit(size)))
        # 把执行“调用 (await session.execute(select(func.count(), func.sum(CallRecord.prompt_tokens), func.sum(CallRecord.completion_tokens), func.avg(CallRecord.duration_ms)).where(CallRecord.user_id == int(context.user_id)))).one 完成one处理”得到的结果保存到 aggregate。
        aggregate = (await session.execute(select(func.count(), func.sum(CallRecord.prompt_tokens),
            # 继续传入或组合调用 func.sum 完成sum处理，完成上面尚未结束的表达式。
            func.sum(CallRecord.completion_tokens), func.avg(CallRecord.duration_ms)).where(
                # 继续传入或组合调用 CallRecord.user_id == int(context.user_id)))).one 完成one处理，完成上面尚未结束的表达式。
                CallRecord.user_id == int(context.user_id)))).one()
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"items": [{"id": str(row.id), "traceId": row.trace_id, "kind": row.kind,
            # 填写名称（name）字段，值为表达式 row.name, "status": row.status, "durationMs": row.duration_ms 的结果。
            "name": row.name, "status": row.status, "durationMs": row.duration_ms,
            # 填写首次身份凭据Ms（firstTokenMs）字段，值为表达式 row.first_token_ms, "promptTokens": row.prompt_tokens 的结果。
            "firstTokenMs": row.first_token_ms, "promptTokens": row.prompt_tokens,
            # 填写完成情况凭据处理器（completionTokens）字段，值为表达式 row.completion_tokens, "errorCode": row.error_code 的结果。
            "completionTokens": row.completion_tokens, "errorCode": row.error_code,
            # 填写创建时At（createdAt）字段，值为表达式 row.created_at} for row in rows], "total": total, "page": page, "size": size 的结果。
            "createdAt": row.created_at} for row in rows], "total": total, "page": page, "size": size,
            # 填写汇总（summary）字段，值为表达式 {"calls": aggregate[0], "promptTokens": aggregate[1] 的结果。
            "summary": {"calls": aggregate[0], "promptTokens": aggregate[1],
                # 填写完成情况凭据处理器（completionTokens）字段，值为优先使用前面的有效值，没有时采用后面的备用值：aggregate[2], "averageDurationMs": float(aggregate[3] or 0)}}。
                "completionTokens": aggregate[2], "averageDurationMs": float(aggregate[3] or 0)}}


# 把下面的函数登记为 HTTP 处理入口：router.get("/runs/tasks")。
@router.get("/runs/tasks")
# 异步函数 background_tasks：调用 background_tasks 完成后台任务列表处理；接收 上下文（context）、Agent 数据库访问器（db）、当前页（page）、每页条数（size）、状态（status）。
async def background_tasks(context: ContextDep, db: DbDep,
    # 填写当前页（page）字段，值为表达式 Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    page: Annotated[int, Query(ge=1)] = 1, size: Annotated[int, Query(ge=1, le=100)] = 20,
    # 填写状态（status）字段，值为表达式 Annotated[str | None, Query(pattern="^(PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED)$")] = None): 的结果。
    status: Annotated[str | None, Query(pattern="^(PENDING|RUNNING|SUCCEEDED|FAILED|CANCELLED)$")] = None):
    # 调用 context.require 完成require处理。
    context.require("agent:run:read")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“调用 tasks.page_tasks 完成当前页任务列表处理”得到的结果完成后的结果保存到 记录列表总数（(rows, total)）。
        rows, total = await tasks.page_tasks(session, user_id=int(context.user_id), status=status, page_number=page, size=size)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"items": [knowledge_service.task_vo(row) for row in rows], "total": total, "page": page, "size": size}


# 把下面的函数登记为 HTTP 处理入口：router.post("/runs/tasks/{task_id}/retry")。
@router.post("/runs/tasks/{task_id}/retry")
# 异步函数 retry_background_task：重试 后台任务（background_task）；接收 任务编号（task_id）、上下文（context）、Agent 数据库访问器（db）。
async def retry_background_task(task_id: int, context: ContextDep, db: DbDep):
    # 把执行“调用 knowledge_service.task_vo 完成任务处理”得到的结果交还给调用方，本函数到此结束。
    return knowledge_service.task_vo(await background_recovery.retry_memory(db, context, task_id))
