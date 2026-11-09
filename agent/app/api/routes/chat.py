# 提供对话内部 HTTP 路由，校验当前用户后调用对应服务。

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）。
from typing import Annotated

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、本次 HTTP 请求（Request）。
from fastapi import APIRouter, Depends, Request

# 从 fastapi.responses 引入 可以边生成边发送的流式响应（StreamingResponse）。
from fastapi.responses import StreamingResponse

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）、读取Java后端客户端（get_java_client）。
from app.core.context import AgentContext, get_agent_context, get_java_client

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.chat_service 引入 核对模型服务配置已填写（ensure_model_configured）、生成完整模型回复（generate_reply）、逐片段生成模型回复（stream_reply）。
from app.llm.chat_service import ensure_model_configured, generate_reply, stream_reply

# 从 app.schemas.chat 引入 Chat请求（ChatRequest）、Chat响应（ChatResponse）。
from app.schemas.chat import ChatRequest, ChatResponse

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(tags=["Chat"])


# 把下面的函数登记为 HTTP 处理入口：router.post("/api/v1/chat", response_model=ChatResponse, include_in_schema=False)。
@router.post("/api/v1/chat", response_model=ChatResponse, include_in_schema=False)
# 把下面的函数登记为 HTTP 处理入口：router.post("/internal/agent/chat", response_model=ChatResponse, summary="内部单轮对话")。
@router.post("/internal/agent/chat", response_model=ChatResponse, summary="内部单轮对话")
# 异步函数 chat：调用 chat 完成对话处理；接收 提交数据（payload）、请求（request）、上下文（context）、客户端（client）。
async def chat(
    # 填写提交数据（payload）字段，值为Chat请求（ChatRequest）。
    payload: ChatRequest,
    # 填写请求（request）字段，值为本次 HTTP 请求（Request）。
    request: Request,
    # 填写上下文（context）字段，值为表达式 Annotated[AgentContext, Depends(get_agent_context)] 的结果。
    context: Annotated[AgentContext, Depends(get_agent_context)],
    # 填写客户端（client）字段，值为表达式 Annotated[JavaApiClient, Depends(get_java_client)] 的结果。
    client: Annotated[JavaApiClient, Depends(get_java_client)],
# 继续传入或组合表达式 ) -> ChatResponse: 的结果，完成上面尚未结束的表达式。
) -> ChatResponse:
    # 调用 context.require 完成require处理。
    context.require("agent:chat:send")
    # 把等待执行“调用 generate_reply 完成生成完整模型回复处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await generate_reply(payload, context, client, request.app.state.chat_slots)


# 把下面的函数登记为 HTTP 处理入口：router.post("/internal/agent/chat/stream", summary="内部 SSE 单轮对话")。
@router.post("/internal/agent/chat/stream", summary="内部 SSE 单轮对话")
# 异步函数 chat_stream：调用 chat_stream 完成chat分段回复流处理；接收 提交数据（payload）、请求（request）、上下文（context）、客户端（client）。
async def chat_stream(
    # 填写提交数据（payload）字段，值为Chat请求（ChatRequest）。
    payload: ChatRequest,
    # 填写请求（request）字段，值为本次 HTTP 请求（Request）。
    request: Request,
    # 填写上下文（context）字段，值为表达式 Annotated[AgentContext, Depends(get_agent_context)] 的结果。
    context: Annotated[AgentContext, Depends(get_agent_context)],
    # 填写客户端（client）字段，值为表达式 Annotated[JavaApiClient, Depends(get_java_client)] 的结果。
    client: Annotated[JavaApiClient, Depends(get_java_client)],
# 继续传入或组合表达式 ) -> StreamingResponse: 的结果，完成上面尚未结束的表达式。
) -> StreamingResponse:
    # 调用 context.require 完成require处理。
    context.require("agent:chat:send")
    # 确认满足 模型configured（model_configured）。
    ensure_model_configured()
    # 把执行“调用 StreamingResponse 完成可以边生成边发送的流式响应处理”得到的结果交还给调用方，本函数到此结束。
    return StreamingResponse(
        # 继续传入或组合调用 stream_reply 完成逐片段生成模型回复处理，完成上面尚未结束的表达式。
        stream_reply(payload, context, client, request.app.state.chat_slots),
        # 传入media类型（media_type），值为固定文字 'text/event-stream'。
        media_type="text/event-stream",
        # 传入请求头集合（headers），值为表达式 {"Cache-Control": "no-store", "X-Accel-Buffering": "no"} 的结果。
        headers={"Cache-Control": "no-store", "X-Accel-Buffering": "no"},
    )
