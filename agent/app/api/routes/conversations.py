"""会话持久化的内部接口；浏览器只通过 Java 代理访问。"""

from typing import Annotated, Literal

from fastapi import APIRouter, Depends, Path, Query, Request
from fastapi.responses import StreamingResponse

from app.core.context import AgentContext, get_agent_context, get_java_client
from app.db.session import Database, get_database
from app.integrations.java_client import JavaApiClient
from app.llm.chat_service import ensure_model_configured
from app.schemas.conversation import (
    ConversationCreateRequest,
    ConversationDetailVO,
    ConversationMessageRequest,
    ConversationPageVO,
    ConversationTitleRequest,
    ConversationVersionRequest,
    MessagePageVO,
    SendResultVO,
    TurnVO,
)
from app.services import conversation_service, turn_service

router = APIRouter(prefix="/internal/agent/conversations", tags=["Conversations"])

ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
ClientDep = Annotated[JavaApiClient, Depends(get_java_client)]
DbDep = Annotated[Database, Depends(get_database)]

STREAM_HEADERS = {"Cache-Control": "no-store", "X-Accel-Buffering": "no"}


def _user(context: AgentContext) -> int:
    return int(context.user_id)


@router.post("", status_code=201, response_model=ConversationDetailVO, summary="创建会话")
async def create(
    payload: ConversationCreateRequest,
    context: ContextDep,
    db: DbDep,
) -> ConversationDetailVO:
    context.require("agent:conversation:create")
    return await conversation_service.create_conversation(db, _user(context), payload.title)


@router.get("", response_model=ConversationPageVO, summary="会话列表")
async def page(
    context: ContextDep,
    db: DbDep,
    status: Annotated[Literal["ACTIVE", "CLOSED"] | None, Query()] = None,
    keyword: Annotated[str | None, Query(max_length=100)] = None,
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> ConversationPageVO:
    context.require("agent:conversation:list")
    return await conversation_service.list_conversations(
        db,
        _user(context),
        status=status,
        keyword=keyword,
        page=page_number,
        size=size,
    )


@router.get("/{conversation_id}", response_model=ConversationDetailVO, summary="会话详情")
async def detail(
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
) -> ConversationDetailVO:
    context.require("agent:conversation:read")
    return await conversation_service.get_conversation(
        db, _user(context), conversation_id
    )


@router.patch("/{conversation_id}", response_model=ConversationDetailVO, summary="修改标题")
async def rename(
    payload: ConversationTitleRequest,
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
) -> ConversationDetailVO:
    context.require("agent:conversation:update")
    return await conversation_service.rename_conversation(
        db, _user(context), conversation_id, payload.title, payload.version
    )


@router.post(
    "/{conversation_id}/close", response_model=ConversationDetailVO, summary="关闭会话"
)
async def close(
    payload: ConversationVersionRequest,
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
) -> ConversationDetailVO:
    context.require("agent:conversation:close")
    return await conversation_service.close_conversation(
        db, _user(context), conversation_id, payload.version
    )


@router.delete("/{conversation_id}", summary="逻辑删除会话")
async def delete(
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
    version: Annotated[int, Query(ge=0)],
) -> dict[str, bool]:
    context.require("agent:conversation:delete")
    await conversation_service.delete_conversation(
        db, _user(context), conversation_id, version
    )
    return {"deleted": True}


@router.get(
    "/{conversation_id}/messages", response_model=MessagePageVO, summary="历史消息游标分页"
)
async def messages(
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
    before_message_no: Annotated[
        int | None, Query(alias="beforeMessageNo", ge=1)
    ] = None,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> MessagePageVO:
    context.require("agent:conversation:read")
    return await conversation_service.list_messages(
        db,
        _user(context),
        conversation_id,
        before_message_no=before_message_no,
        size=size,
    )


@router.get(
    "/{conversation_id}/turns/{turn_id}",
    response_model=TurnVO,
    summary="轮次状态与已保存文本",
)
async def turn(
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
    turn_id: Annotated[int, Path(ge=1)],
) -> TurnVO:
    context.require("agent:conversation:read")
    return await turn_service.get_turn(
        db, _user(context), conversation_id, turn_id
    )


@router.post(
    "/{conversation_id}/turns/{turn_id}/cancel",
    response_model=TurnVO,
    summary="停止生成",
)
async def cancel(
    payload: ConversationVersionRequest,
    context: ContextDep,
    db: DbDep,
    conversation_id: Annotated[int, Path(ge=1)],
    turn_id: Annotated[int, Path(ge=1)],
) -> TurnVO:
    context.require("agent:chat:send")
    return await turn_service.cancel_turn(
        db, _user(context), conversation_id, turn_id, payload.version
    )


@router.post(
    "/{conversation_id}/messages",
    response_model=SendResultVO,
    summary="发送消息并保存回复",
)
async def send(
    payload: ConversationMessageRequest,
    request: Request,
    context: ContextDep,
    db: DbDep,
    client: ClientDep,
    conversation_id: Annotated[int, Path(ge=1)],
) -> SendResultVO:
    context.require("agent:chat:send")
    ensure_model_configured()
    started = await turn_service.start_turn(
        db,
        _user(context),
        conversation_id,
        payload.message,
        payload.request_id,
    )
    if isinstance(started, turn_service.ReplayedTurn):
        return turn_service.replay_result(started)
    return await turn_service.generate_plain(
        db, context, started, payload.message, client, request.app.state.chat_slots
    )


@router.post(
    "/{conversation_id}/messages/stream",
    summary="发送消息并流式保存",
)
async def stream(
    payload: ConversationMessageRequest,
    request: Request,
    context: ContextDep,
    db: DbDep,
    client: ClientDep,
    conversation_id: Annotated[int, Path(ge=1)],
) -> StreamingResponse:
    context.require("agent:chat:send")
    ensure_model_configured()
    started = await turn_service.start_turn(
        db,
        _user(context),
        conversation_id,
        payload.message,
        payload.request_id,
    )
    if isinstance(started, turn_service.ReplayedTurn):
        frames = turn_service.stream_replay(started, context.trace_id)
    else:
        frames = turn_service.stream_turn(
            db,
            context,
            started,
            payload.message,
            client,
            request.app.state.chat_slots,
        )
    return StreamingResponse(
        frames,
        media_type="text/event-stream",
        headers=STREAM_HEADERS,
    )
