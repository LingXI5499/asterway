"""P1.1 单轮对话。流式请求取消时，立即关闭上游流，不自动重试计费请求。"""

import asyncio
import json
import logging
from collections.abc import AsyncIterator
from contextlib import suppress
from typing import Any

from openai import APIStatusError

from app.core.config import get_settings
from app.core.context import AgentContext, refresh_context
from app.core.errors import AgentError
from app.integrations.java_client import JavaApiClient
from app.llm.client import get_llm_client
from app.llm.errors import classify_model_error
from app.llm.prompts.chat import SYSTEM_PROMPT
from app.schemas.chat import ChatRequest, ChatResponse

logger = logging.getLogger(__name__)


def ensure_model_configured() -> None:
    if not get_settings().llm_api_key.get_secret_value():
        raise AgentError(503, "LLM_NOT_CONFIGURED", "模型服务尚未配置，请联系管理员")


def model_options(messages: list[dict[str, Any]]) -> dict[str, Any]:
    """模型的公共参数；会话、摘要和 P1.1 单轮共用同一套配置。"""
    settings = get_settings()
    options: dict[str, Any] = {
        "model": settings.llm_model,
        "messages": messages,
        "max_tokens": settings.llm_max_output_tokens,
    }
    if settings.llm_provider.lower() == "deepseek":
        options["extra_body"] = {"thinking": {"type": "disabled"}}
    return options


def completion_options(request: ChatRequest) -> dict[str, Any]:
    return model_options(
        [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": request.message},
        ]
    )


def safe_error(error: Exception, trace_id: str) -> AgentError:
    if isinstance(error, AgentError):
        return error
    failure = classify_model_error(error)
    # 不记录 SDK 异常字符串、响应正文或请求头；保留可定位的状态码和分类。
    logger.warning(
        "agent_request_failed trace=%s type=%s upstream_status=%s code=%s",
        trace_id, type(error).__name__,
        error.status_code if isinstance(error, APIStatusError) else None,
        failure.code,
    )
    return failure


def event_frame(event: str, data: dict[str, Any]) -> bytes:
    encoded = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    return f"event: {event}\ndata: {encoded}\n\n".encode()


async def generate_reply(
    request: ChatRequest,
    context: AgentContext,
    java_client: JavaApiClient,
    slots: asyncio.Semaphore,
) -> ChatResponse:
    ensure_model_configured()
    try:
        async with asyncio.timeout_at(context.deadline), slots:
            completion = await get_llm_client().chat.completions.create(
                **completion_options(request)
            )
            if not completion.choices or not completion.choices[0].message.content:
                raise AgentError(502, "LLM_EMPTY_REPLY", "模型未返回有效回复，请重试")
            await refresh_context(context, java_client)
            choice = completion.choices[0]
            return ChatResponse(
                reply=choice.message.content,
                model=completion.model or get_settings().llm_model,
                finish_reason=choice.finish_reason,
                trace_id=context.trace_id,
            )
    except Exception as error:  # noqa: BLE001 - 对外边界统一脱敏，不输出 SDK 响应原文。
        raise safe_error(error, context.trace_id) from None


async def stream_reply(
    request: ChatRequest,
    context: AgentContext,
    java_client: JavaApiClient,
    slots: asyncio.Semaphore,
) -> AsyncIterator[bytes]:
    """有界队列防止慢客户端无限积压；心跳让 Java 能及时感知浏览器断开。"""
    queue: asyncio.Queue[tuple[str, dict[str, Any]]] = asyncio.Queue(maxsize=32)

    async def produce() -> None:
        upstream = None
        try:
            async with asyncio.timeout_at(context.deadline), slots:
                upstream = await get_llm_client().chat.completions.create(
                    **completion_options(request),
                    stream=True,
                    stream_options={"include_usage": True},
                )
                model = get_settings().llm_model
                finish_reason = None
                usage = None
                received_text = False
                async for chunk in upstream:
                    model = chunk.model or model
                    if chunk.usage is not None:
                        usage = {
                            "promptTokens": chunk.usage.prompt_tokens,
                            "completionTokens": chunk.usage.completion_tokens,
                            "totalTokens": chunk.usage.total_tokens,
                        }
                    if not chunk.choices:
                        continue
                    choice = chunk.choices[0]
                    if choice.delta.content:
                        received_text = True
                        await queue.put(("delta", {"text": choice.delta.content}))
                    if choice.finish_reason:
                        finish_reason = choice.finish_reason
                if not finish_reason or not received_text:
                    raise AgentError(502, "LLM_INCOMPLETE_REPLY", "回复意外中断，请重试")
                await refresh_context(context, java_client)
                await queue.put(
                    (
                        "message_end",
                        {
                            "model": model,
                            "finishReason": finish_reason,
                            "complete": finish_reason == "stop",
                            "usage": usage,
                            "traceId": context.trace_id,
                        },
                    )
                )
        except Exception as error:  # noqa: BLE001 - SSE 已开始后必须转为终止错误事件。
            failure = safe_error(error, context.trace_id)
            await queue.put(
                (
                    "error",
                    {
                        "code": failure.code,
                        "message": failure.message,
                        "status": failure.status_code,
                        "traceId": context.trace_id,
                    },
                )
            )
        finally:
            if upstream is not None:
                with suppress(Exception):
                    await upstream.close()

    producer = asyncio.create_task(produce(), name="agent-chat-stream")
    try:
        yield event_frame(
            "message_start",
            {
                "traceId": context.trace_id,
                "model": get_settings().llm_model,
            },
        )
        while True:
            try:
                event, data = await asyncio.wait_for(queue.get(), timeout=10.0)
            except TimeoutError:
                yield b": keep-alive\n\n"
                continue
            yield event_frame(event, data)
            if event in {"message_end", "error"}:
                return
    finally:
        producer.cancel()
        with suppress(asyncio.CancelledError):
            await producer
