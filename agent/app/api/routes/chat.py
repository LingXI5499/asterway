from typing import Annotated

from fastapi import APIRouter, Depends, Request
from fastapi.responses import StreamingResponse

from app.core.context import AgentContext, get_agent_context, get_java_client
from app.integrations.java_client import JavaApiClient
from app.llm.chat_service import ensure_model_configured, generate_reply, stream_reply
from app.schemas.chat import ChatRequest, ChatResponse

router = APIRouter(tags=["Chat"])


@router.post("/api/v1/chat", response_model=ChatResponse, include_in_schema=False)
@router.post("/internal/agent/chat", response_model=ChatResponse, summary="内部单轮对话")
async def chat(
    payload: ChatRequest,
    request: Request,
    context: Annotated[AgentContext, Depends(get_agent_context)],
    client: Annotated[JavaApiClient, Depends(get_java_client)],
) -> ChatResponse:
    context.require("agent:chat:send")
    return await generate_reply(payload, context, client, request.app.state.chat_slots)


@router.post("/internal/agent/chat/stream", summary="内部 SSE 单轮对话")
async def chat_stream(
    payload: ChatRequest,
    request: Request,
    context: Annotated[AgentContext, Depends(get_agent_context)],
    client: Annotated[JavaApiClient, Depends(get_java_client)],
) -> StreamingResponse:
    context.require("agent:chat:send")
    ensure_model_configured()
    return StreamingResponse(
        stream_reply(payload, context, client, request.app.state.chat_slots),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-store", "X-Accel-Buffering": "no"},
    )
