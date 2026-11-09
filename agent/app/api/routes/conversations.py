# 提供对话列表内部 HTTP 路由，校验当前用户后调用对应服务。
# 会话持久化的内部接口；浏览器只通过 Java 代理访问。

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）、只能从列出的固定值中选择的类型（Literal）。
from typing import Annotated, Literal

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、路径（Path）、查询条件（Query）、本次 HTTP 请求（Request）。
from fastapi import APIRouter, Depends, Path, Query, Request

# 从 fastapi.responses 引入 可以边生成边发送的流式响应（StreamingResponse）。
from fastapi.responses import StreamingResponse

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）、读取Java后端客户端（get_java_client）。
from app.core.context import AgentContext, get_agent_context, get_java_client

# 从 app.db.session 引入 数据库（Database）、取得本次请求的 Agent 数据库访问器（get_database）。
from app.db.session import Database, get_database

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.chat_service 引入 核对模型服务配置已填写（ensure_model_configured）。
from app.llm.chat_service import ensure_model_configured

# 从 app.schemas.conversation 引入 对话创建请求（ConversationCreateRequest）、对话详情（ConversationDetailVO）、对话消息请求（ConversationMessageRequest）、对话当前页（ConversationPageVO）、对话标题请求（ConversationTitleRequest）、对话版本号请求（ConversationVersionRequest）、消息当前页（MessagePageVO）、发送结果（SendResultVO）、本轮对话（TurnVO）。
from app.schemas.conversation import (
    # 继续传入或组合对话创建请求（ConversationCreateRequest），完成上面尚未结束的表达式。
    ConversationCreateRequest,
    # 继续传入或组合对话详情（ConversationDetailVO），完成上面尚未结束的表达式。
    ConversationDetailVO,
    ConversationKnowledgeRequest,
    # 继续传入或组合对话消息请求（ConversationMessageRequest），完成上面尚未结束的表达式。
    ConversationMessageRequest,
    # 继续传入或组合对话当前页（ConversationPageVO），完成上面尚未结束的表达式。
    ConversationPageVO,
    # 继续传入或组合对话标题请求（ConversationTitleRequest），完成上面尚未结束的表达式。
    ConversationTitleRequest,
    # 继续传入或组合对话版本号请求（ConversationVersionRequest），完成上面尚未结束的表达式。
    ConversationVersionRequest,
    # 继续传入或组合消息当前页（MessagePageVO），完成上面尚未结束的表达式。
    MessagePageVO,
    # 继续传入或组合发送结果（SendResultVO），完成上面尚未结束的表达式。
    SendResultVO,
    # 继续传入或组合本轮对话（TurnVO），完成上面尚未结束的表达式。
    TurnVO,
)

# 从 app.services 引入 对话业务服务（conversation_service）、本轮对话业务服务（turn_service）。
from app.services import conversation_service, turn_service

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(prefix="/internal/agent/conversations", tags=["Conversations"])

# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 成长助手上下文（AgentContext）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 上下文Dep（ContextDep）。
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 Java后端接口客户端（JavaApiClient）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 客户端Dep（ClientDep）。
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 数据库（Database）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 数据库Dep（DbDep）。
DbDep = Annotated[Database, Depends(get_database)]

# 把按字段名保存多个值的字典保存到 分段回复流请求头集合（STREAM_HEADERS）。
STREAM_HEADERS = {"Cache-Control": "no-store", "X-Accel-Buffering": "no"}


# 函数 _user：调用 _user 完成用户处理；接收 上下文（context）。
def _user(context: AgentContext) -> int:
    # 把执行“调用 int 完成int处理”得到的结果交还给调用方，本函数到此结束。
    return int(context.user_id)


# 把下面的函数登记为 HTTP 处理入口：router.post("", status_code=201, response_model=ConversationDetailVO, summary="创建会话")。
@router.post("", status_code=201, response_model=ConversationDetailVO, summary="创建会话")
# 异步函数 create：调用 create 完成创建处理；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def create(payload: ConversationCreateRequest, context: ContextDep, db: DbDep) -> ConversationDetailVO:
    context.require('agent:conversation:create')
    if payload.knowledge_base_ids:
        context.require("agent:knowledge:read")
    return await conversation_service.create_conversation(db, _user(context), payload.title, payload.knowledge_base_ids)


# 把下面的函数登记为 HTTP 处理入口：router.get("", response_model=ConversationPageVO, summary="会话列表")。
@router.get("", response_model=ConversationPageVO, summary="会话列表")
# 异步函数 page：调用 page 完成当前页处理；接收 上下文（context）、Agent 数据库访问器（db）、状态（status）、keyword、当前页数值（page_number）、每页条数（size）。
async def page(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写状态（status）字段，值为表达式 Annotated[Literal["ACTIVE", "CLOSED"] | None, Query()] = None 的结果。
    status: Annotated[Literal["ACTIVE", "CLOSED"] | None, Query()] = None,
    # 填写keyword字段，值为表达式 Annotated[str | None, Query(max_length=100)] = None 的结果。
    keyword: Annotated[str | None, Query(max_length=100)] = None,
    # 填写当前页数值（page_number）字段，值为表达式 Annotated[int, Query(alias="page", ge=1)] = 1 的结果。
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    # 填写每页条数（size）字段，值为表达式 Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    size: Annotated[int, Query(ge=1, le=100)] = 20,
# 继续传入或组合表达式 ) -> ConversationPageVO: 的结果，完成上面尚未结束的表达式。
) -> ConversationPageVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:list")
    # 把等待执行“列出 对话列表（conversations）”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_service.list_conversations(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 继续传入或组合调用 _user 完成用户处理，完成上面尚未结束的表达式。
        _user(context),
        # 传入状态（status），值为状态（status）。
        status=status,
        # 传入keyword，值为keyword。
        keyword=keyword,
        # 传入当前页（page），值为当前页数值（page_number）。
        page=page_number,
        # 传入每页条数（size），值为每页条数（size）。
        size=size,
    )


# 把下面的函数登记为 HTTP 处理入口：router.get("/{conversation_id}", response_model=ConversationDetailVO, summary="会话详情")。
@router.get("/{conversation_id}", response_model=ConversationDetailVO, summary="会话详情")
# 异步函数 detail：调用 detail 完成详情处理；接收 上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）。
async def detail(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
# 继续传入或组合表达式 ) -> ConversationDetailVO: 的结果，完成上面尚未结束的表达式。
) -> ConversationDetailVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:read")
    # 把等待执行“读取 对话（_conversation） 的值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_service.get_conversation(
        # 继续传入或组合表达式 db, _user(context), conversation_id 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id
    )


# 把下面的函数登记为 HTTP 处理入口：router.patch("/{conversation_id}", response_model=ConversationDetailVO, summary="修改标题")。
@router.patch("/{conversation_id}", response_model=ConversationDetailVO, summary="修改标题")
# 异步函数 rename：调用 rename 完成重命名处理；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）。
async def rename(
    # 填写提交数据（payload）字段，值为对话标题请求（ConversationTitleRequest）。
    payload: ConversationTitleRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
# 继续传入或组合表达式 ) -> ConversationDetailVO: 的结果，完成上面尚未结束的表达式。
) -> ConversationDetailVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:update")
    # 把等待执行“调用 conversation_service.rename_conversation 完成重命名对话处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_service.rename_conversation(
        # 继续传入或组合表达式 db, _user(context), conversation_id, payload.title, payload.version 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id, payload.title, payload.version
    )


# 把下面的函数登记为 HTTP 处理入口：router.post(。
@router.post(
    # 继续传入或组合固定文字 '/{conversation_id}/close", response_model=ConversationDetailVO, summary="关闭会话'，完成上面尚未结束的表达式。
    "/{conversation_id}/close", response_model=ConversationDetailVO, summary="关闭会话"
)
# 异步函数 close：释放连接、文件或客户端占用的资源；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）。
async def close(
    # 填写提交数据（payload）字段，值为对话版本号请求（ConversationVersionRequest）。
    payload: ConversationVersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
# 继续传入或组合表达式 ) -> ConversationDetailVO: 的结果，完成上面尚未结束的表达式。
) -> ConversationDetailVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:close")
    # 把等待执行“调用 conversation_service.close_conversation 完成关闭对话处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_service.close_conversation(
        # 继续传入或组合表达式 db, _user(context), conversation_id, payload.version 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id, payload.version
    )


# 把下面的函数登记为 HTTP 处理入口：router.delete("/{conversation_id}", summary="逻辑删除会话")。
@router.delete("/{conversation_id}", summary="逻辑删除会话")
# 异步函数 delete：调用 delete 完成删除处理；接收 上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）、版本号（version）。
async def delete(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
    # 填写版本号（version）字段，值为表达式 Annotated[int, Query(ge=0)] 的结果。
    version: Annotated[int, Query(ge=0)],
# 继续传入或组合表达式 ) -> dict[str, bool]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, bool]:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:delete")
    # 等待删除 对话（conversation）完成后再继续。
    await conversation_service.delete_conversation(
        # 继续传入或组合表达式 db, _user(context), conversation_id, version 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id, version
    )
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"deleted": True}


# 把下面的函数登记为 HTTP 处理入口：router.get(。
@router.get(
    # 继续传入或组合固定文字 '/{conversation_id}/messages", response_model=MessagePageVO, summary="历史消息游标分页'，完成上面尚未结束的表达式。
    "/{conversation_id}/messages", response_model=MessagePageVO, summary="历史消息游标分页"
)
# 异步函数 messages：调用 messages 完成消息列表处理；接收 上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）、变化前消息no（before_message_no）、每页条数（size）。
async def messages(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
    # 填写变化前消息no（before_message_no）字段，值为表达式 Annotated[ 的结果。
    before_message_no: Annotated[
        # 继续传入或组合表达式 int | None, Query(alias="beforeMessageNo", ge=1) 的结果，完成上面尚未结束的表达式。
        int | None, Query(alias="beforeMessageNo", ge=1)
    # 继续传入或组合表达式 ] = None 的结果，完成上面尚未结束的表达式。
    ] = None,
    # 填写每页条数（size）字段，值为表达式 Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    size: Annotated[int, Query(ge=1, le=100)] = 20,
# 继续传入或组合表达式 ) -> MessagePageVO: 的结果，完成上面尚未结束的表达式。
) -> MessagePageVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:read")
    # 把等待执行“列出 消息列表（messages）”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await conversation_service.list_messages(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 继续传入或组合调用 _user 完成用户处理，完成上面尚未结束的表达式。
        _user(context),
        # 继续传入或组合对话编号（conversation_id），完成上面尚未结束的表达式。
        conversation_id,
        # 传入变化前消息no（before_message_no），值为变化前消息no（before_message_no）。
        before_message_no=before_message_no,
        # 传入每页条数（size），值为每页条数（size）。
        size=size,
    )


# 把下面的函数登记为 HTTP 处理入口：router.get(。
@router.get(
    # 继续传入或组合固定文字 '/{conversation_id}/turns/{turn_id}'，完成上面尚未结束的表达式。
    "/{conversation_id}/turns/{turn_id}",
    # 传入响应模型（response_model），值为本轮对话（TurnVO）。
    response_model=TurnVO,
    # 传入汇总（summary），值为固定文字 '轮次状态与已保存文本'。
    summary="轮次状态与已保存文本",
)
# 异步函数 turn：调用 turn 完成本轮对话处理；接收 上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）、本轮对话编号（turn_id）。
async def turn(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
    # 填写本轮对话编号（turn_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    turn_id: Annotated[int, Path(ge=1)],
# 继续传入或组合表达式 ) -> TurnVO: 的结果，完成上面尚未结束的表达式。
) -> TurnVO:
    # 调用 context.require 完成require处理。
    context.require("agent:conversation:read")
    # 把等待执行“读取 本轮对话（_turn） 的值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await turn_service.get_turn(
        # 继续传入或组合表达式 db, _user(context), conversation_id, turn_id 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id, turn_id
    )


# 把下面的函数登记为 HTTP 处理入口：router.post(。
@router.post(
    # 继续传入或组合固定文字 '/{conversation_id}/turns/{turn_id}/cancel'，完成上面尚未结束的表达式。
    "/{conversation_id}/turns/{turn_id}/cancel",
    # 传入响应模型（response_model），值为本轮对话（TurnVO）。
    response_model=TurnVO,
    # 传入汇总（summary），值为固定文字 '停止生成'。
    summary="停止生成",
)
# 异步函数 cancel：调用 cancel 完成取消处理；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）、对话编号（conversation_id）、本轮对话编号（turn_id）。
async def cancel(
    # 填写提交数据（payload）字段，值为对话版本号请求（ConversationVersionRequest）。
    payload: ConversationVersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写对话编号（conversation_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    conversation_id: Annotated[int, Path(ge=1)],
    # 填写本轮对话编号（turn_id）字段，值为表达式 Annotated[int, Path(ge=1)] 的结果。
    turn_id: Annotated[int, Path(ge=1)],
# 继续传入或组合表达式 ) -> TurnVO: 的结果，完成上面尚未结束的表达式。
) -> TurnVO:
    # 调用 context.require 完成require处理。
    context.require("agent:chat:send")
    # 把等待执行“取消 本轮对话（turn）”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await turn_service.cancel_turn(
        # 继续传入或组合表达式 db, _user(context), conversation_id, turn_id, payload.version 的结果，完成上面尚未结束的表达式。
        db, _user(context), conversation_id, turn_id, payload.version
    )


# 把下面的函数登记为 HTTP 处理入口：router.post(。
@router.post(
    # 继续传入或组合固定文字 '/{conversation_id}/messages'，完成上面尚未结束的表达式。
    "/{conversation_id}/messages",
    # 传入响应模型（response_model），值为发送结果（SendResultVO）。
    response_model=SendResultVO,
    # 传入汇总（summary），值为固定文字 '发送消息并保存回复'。
    summary="发送消息并保存回复",
)
# 异步函数 send：调用 send 完成发送处理；接收 提交数据（payload）、请求（request）、上下文（context）、Agent 数据库访问器（db）、客户端（client）、对话编号（conversation_id）。
async def send(payload: ConversationMessageRequest, request: Request, context: ContextDep, db: DbDep, client: ClientDep, conversation_id: Annotated[int, Path(ge=1)]) -> SendResultVO:
    context.require('agent:chat:send')
    ensure_model_configured()
    started = await turn_service.start_turn(db, _user(context), conversation_id, payload.message, payload.request_id, payload.knowledge_base_ids, allow_knowledge="agent:knowledge:read" in context.permissions)
    if isinstance(started, turn_service.ReplayedTurn):
        return turn_service.replay_result(started)
    return await turn_service.generate_plain(db, context, started, payload.message, client, request.app.state.chat_slots)


# 把下面的函数登记为 HTTP 处理入口：router.post(。
@router.post(
    # 继续传入或组合固定文字 '/{conversation_id}/messages/stream'，完成上面尚未结束的表达式。
    "/{conversation_id}/messages/stream",
    # 传入汇总（summary），值为固定文字 '发送消息并流式保存'。
    summary="发送消息并流式保存",
)
# 异步函数 stream：调用 stream 完成分段回复流处理；接收 提交数据（payload）、请求（request）、上下文（context）、Agent 数据库访问器（db）、客户端（client）、对话编号（conversation_id）。
async def stream(payload: ConversationMessageRequest, request: Request, context: ContextDep, db: DbDep, client: ClientDep, conversation_id: Annotated[int, Path(ge=1)]) -> StreamingResponse:
    context.require('agent:chat:send')
    ensure_model_configured()
    started = await turn_service.start_turn(db, _user(context), conversation_id, payload.message, payload.request_id, payload.knowledge_base_ids, allow_knowledge="agent:knowledge:read" in context.permissions)
    if isinstance(started, turn_service.ReplayedTurn):
        frames = turn_service.stream_replay(started, context.trace_id)
    else:
        frames = turn_service.stream_turn(db, context, started, payload.message, client, request.app.state.chat_slots)
    return StreamingResponse(frames, media_type='text/event-stream', headers=STREAM_HEADERS)


@router.patch("/{conversation_id}/knowledge-bases", response_model=ConversationDetailVO)
async def update_knowledge_bases(payload: ConversationKnowledgeRequest, context: ContextDep, db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)]):
    context.require("agent:conversation:update")
    if payload.knowledge_base_ids:
        context.require("agent:knowledge:read")
    return await conversation_service.update_knowledge_bases(db, _user(context), conversation_id, payload.knowledge_base_ids, payload.version)
