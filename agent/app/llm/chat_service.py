# 调用模型并把普通或流式回复转换为统一结果，分类处理上游错误。
# P1.1 单轮对话。流式请求取消时，立即关闭上游流，不自动重试计费请求。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 从 collections.abc 引入 可逐项读取的异步迭代结果（AsyncIterator）。
from collections.abc import AsyncIterator

# 从 contextlib 引入 suppress。
from contextlib import suppress

# 从 typing 引入 Any。
from typing import Any

# 从 openai 引入 接口状态错误（APIStatusError）。
from openai import APIStatusError

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.client 引入 取得复用的模型客户端（get_llm_client）。
from app.llm.client import get_llm_client

# 从 app.llm.errors 引入 classify模型错误（classify_model_error）。
from app.llm.errors import classify_model_error

# 从 app.llm.prompts.chat 引入 系统模型提示词（SYSTEM_PROMPT）。
from app.llm.prompts.chat import SYSTEM_PROMPT

# 从 app.schemas.chat 引入 Chat请求（ChatRequest）、Chat响应（ChatResponse）。
from app.schemas.chat import ChatRequest, ChatResponse

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 函数 ensure_model_configured：确认满足 模型configured（model_configured）。
def ensure_model_configured() -> None:
    # 检查条件 not get_settings().llm_api_key.get_secret_value() 是否成立；成立时执行缩进的分支。
    if not get_settings().llm_api_key.get_secret_value():
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "LLM_NOT_CONFIGURED", "模型服务尚未配置，请联系管理员")


# 函数 model_options：模型的公共参数；会话、摘要和 P1.1 单轮共用同一套配置。；接收 消息列表（messages）。
def model_options(messages: list[dict[str, Any]]) -> dict[str, Any]:
    # 模型的公共参数；会话、摘要和 P1.1 单轮共用同一套配置。
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 声明 选项（options），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    options: dict[str, Any] = {
        # 填写模型（model）字段，值为表达式 settings.llm_model 的结果。
        "model": settings.llm_model,
        # 填写消息列表（messages）字段，值为消息列表（messages）。
        "messages": messages,
        # 填写最大凭据处理器（max_tokens）字段，值为表达式 settings.llm_max_output_tokens 的结果。
        "max_tokens": settings.llm_max_output_tokens,
    }
    # 检查条件 settings.llm_provider.lower() == 'deepseek' 是否成立；成立时执行缩进的分支。
    if settings.llm_provider.lower() == "deepseek":
        # 把按字段名保存多个值的字典保存到 选项extra请求正文（options['extra_body']）。
        options["extra_body"] = {"thinking": {"type": "disabled"}}
    # 把选项（options）交还给调用方，本函数到此结束。
    return options


# 函数 completion_options：调用 completion_options 完成完成情况选项处理；接收 请求（request）。
def completion_options(request: ChatRequest) -> dict[str, Any]:
    # 把执行“调用 model_options 完成模型选项处理”得到的结果交还给调用方，本函数到此结束。
    return model_options(
        [
            # 继续传入或组合表达式 {"role": "system", "content": SYSTEM_PROMPT} 的结果，完成上面尚未结束的表达式。
            {"role": "system", "content": SYSTEM_PROMPT},
            # 继续传入或组合表达式 {"role": "user", "content": request.message} 的结果，完成上面尚未结束的表达式。
            {"role": "user", "content": request.message},
        ]
    )


# 函数 safe_error：调用 safe_error 完成把底层异常转换成脱敏的可读错误处理；接收 错误（error）、追踪标识编号（trace_id）。
def safe_error(error: Exception, trace_id: str) -> AgentError:
    # 检查条件 isinstance(error, AgentError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, AgentError):
        # 把错误（error）交还给调用方，本函数到此结束。
        return error
    # 把执行“调用 classify_model_error 完成classify模型错误处理”得到的结果保存到 失败（failure）。
    failure = classify_model_error(error)
    # 不记录 SDK 异常字符串、响应正文或请求头；保留可定位的状态码和分类。
    # 记录warning级别的运行信息，供定位处理过程和失败原因。
    logger.warning(
        # 继续传入或组合固定文字 'agent_request_failed trace=%s type=%s upstream_status=%s code=%s'，完成上面尚未结束的表达式。
        "agent_request_failed trace=%s type=%s upstream_status=%s code=%s",
        # 继续传入或组合表达式 trace_id, type(error).__name__ 的结果，完成上面尚未结束的表达式。
        trace_id, type(error).__name__,
        # 继续传入或组合表达式 error.status_code if isinstance(error, APIStatusError) else None 的结果，完成上面尚未结束的表达式。
        error.status_code if isinstance(error, APIStatusError) else None,
        # 继续传入或组合表达式 failure.code 的结果，完成上面尚未结束的表达式。
        failure.code,
    )
    # 把失败（failure）交还给调用方，本函数到此结束。
    return failure


# 函数 event_frame：调用 event_frame 完成事件frame处理；接收 事件（event）、数据（data）。
def event_frame(event: str, data: dict[str, Any]) -> bytes:
    # 把执行“把对象编码为 JSON 文字”得到的结果保存到 encoded。
    encoded = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    # 把执行“把原始密码编码成不可直接还原的密码摘要”得到的结果交还给调用方，本函数到此结束。
    return f"event: {event}\ndata: {encoded}\n\n".encode()


# 异步函数 generate_reply：调用 generate_reply 完成生成完整模型回复处理；接收 请求（request）、上下文（context）、Java后端客户端（java_client）、slots。
async def generate_reply(
    # 填写请求（request）字段，值为Chat请求（ChatRequest）。
    request: ChatRequest,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写slots字段，值为表达式 asyncio.Semaphore 的结果。
    slots: asyncio.Semaphore,
# 继续传入或组合表达式 ) -> ChatResponse: 的结果，完成上面尚未结束的表达式。
) -> ChatResponse:
    # 确认满足 模型configured（model_configured）。
    ensure_model_configured()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 asyncio.timeout_at 完成超时at处理”得到的结果、slots；退出时由上下文管理器提交、回滚或释放资源。
        async with asyncio.timeout_at(context.deadline), slots:
            # 把等待执行“调用 get_llm_client().chat.completions.create 完成创建处理”得到的结果完成后的结果保存到 完成情况（completion）。
            completion = await get_llm_client().chat.completions.create(
                # 继续传入或组合按表达式计算得到的值：**completion_options(request)，完成上面尚未结束的表达式。
                **completion_options(request)
            )
            # 检查条件 not completion.choices or not completion.choices[0].message.content 是否成立；成立时执行缩进的分支。
            if not completion.choices or not completion.choices[0].message.content:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(502, "LLM_EMPTY_REPLY", "模型未返回有效回复，请重试")
            # 等待刷新 上下文（context）完成后再继续。
            await refresh_context(context, java_client)
            # 把从完成情况（completion）的choices属性中按固定数值 0取出的内容保存到 choice。
            choice = completion.choices[0]
            # 把执行“调用 ChatResponse 完成Chat响应处理”得到的结果交还给调用方，本函数到此结束。
            return ChatResponse(
                # 传入reply，值为表达式 choice.message.content 的结果。
                reply=choice.message.content,
                # 传入模型（model），值为优先使用前面的有效值，没有时采用后面的备用值：completion.model or get_settings().llm_model。
                model=completion.model or get_settings().llm_model,
                # 传入finish原因（finish_reason），值为表达式 choice.finish_reason 的结果。
                finish_reason=choice.finish_reason,
                # 传入追踪标识编号（trace_id），值为表达式 context.trace_id 的结果。
                trace_id=context.trace_id,
            )
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 对外边界统一脱敏，不输出 SDK 响应原文。
        # 中止本轮正常处理，抛出 执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果。
        raise safe_error(error, context.trace_id) from None


# 异步函数 stream_reply：有界队列防止慢客户端无限积压；心跳让 Java 能及时感知浏览器断开。；接收 请求（request）、上下文（context）、Java后端客户端（java_client）、slots。
async def stream_reply(
    # 填写请求（request）字段，值为Chat请求（ChatRequest）。
    request: ChatRequest,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写slots字段，值为表达式 asyncio.Semaphore 的结果。
    slots: asyncio.Semaphore,
# 继续传入或组合表达式 ) -> AsyncIterator[bytes]: 的结果，完成上面尚未结束的表达式。
) -> AsyncIterator[bytes]:
    # 有界队列防止慢客户端无限积压；心跳让 Java 能及时感知浏览器断开。
    # 声明 queue，类型为 asyncio.Queue[tuple[str, dict[str, Any]]]；其内容为执行“调用 asyncio.Queue 完成Queue处理”得到的结果。
    queue: asyncio.Queue[tuple[str, dict[str, Any]]] = asyncio.Queue(maxsize=32)

    # 异步函数 produce：调用 produce 完成produce处理。
    async def produce() -> None:
        # 把空值，表示当前还没有对应内容保存到 upstream。
        upstream = None
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 异步进入执行“调用 asyncio.timeout_at 完成超时at处理”得到的结果、slots；退出时由上下文管理器提交、回滚或释放资源。
            async with asyncio.timeout_at(context.deadline), slots:
                # 把等待执行“调用 get_llm_client().chat.completions.create 完成创建处理”得到的结果完成后的结果保存到 upstream。
                upstream = await get_llm_client().chat.completions.create(
                    # 继续传入或组合按表达式计算得到的值：**completion_options(request)，完成上面尚未结束的表达式。
                    **completion_options(request),
                    # 传入分段回复流（stream），值为真，表示条件成立或开关打开。
                    stream=True,
                    # 传入分段回复流选项（stream_options），值为表达式 {"include_usage": True} 的结果。
                    stream_options={"include_usage": True},
                )
                # 把执行“读取并复用当前进程的运行配置”得到的结果的语言模型模型（llm_model）属性保存到 模型（model）。
                model = get_settings().llm_model
                # 把空值，表示当前还没有对应内容保存到 finish原因（finish_reason）。
                finish_reason = None
                # 把空值，表示当前还没有对应内容保存到 用量（usage）。
                usage = None
                # 把假，表示条件不成立或开关关闭保存到 received文字（received_text）。
                received_text = False
                # 从upstream依次取出文档片段（chunk），每项执行一次下面的处理。
                async for chunk in upstream:
                    # 把文档片段（chunk）的模型（model）属性或者模型（model）保存到 模型（model）。
                    model = chunk.model or model
                    # 检查条件 chunk.usage is not None 是否成立；成立时执行缩进的分支。
                    if chunk.usage is not None:
                        # 把按字段名保存多个值的字典保存到 用量（usage）。
                        usage = {
                            # 填写模型提示词凭据处理器（promptTokens）字段，值为表达式 chunk.usage.prompt_tokens 的结果。
                            "promptTokens": chunk.usage.prompt_tokens,
                            # 填写完成情况凭据处理器（completionTokens）字段，值为表达式 chunk.usage.completion_tokens 的结果。
                            "completionTokens": chunk.usage.completion_tokens,
                            # 填写总数凭据处理器（totalTokens）字段，值为表达式 chunk.usage.total_tokens 的结果。
                            "totalTokens": chunk.usage.total_tokens,
                        }
                    # 检查条件 not chunk.choices 是否成立；成立时执行缩进的分支。
                    if not chunk.choices:
                        # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                        continue
                    # 把从文档片段（chunk）的choices属性中按固定数值 0取出的内容保存到 choice。
                    choice = chunk.choices[0]
                    # 检查条件 choice.delta.content 是否成立；成立时执行缩进的分支。
                    if choice.delta.content:
                        # 把真，表示条件成立或开关打开保存到 received文字（received_text）。
                        received_text = True
                        # 等待保存指定键对应的值完成后再继续。
                        await queue.put(("delta", {"text": choice.delta.content}))
                    # 检查条件 choice.finish_reason 是否成立；成立时执行缩进的分支。
                    if choice.finish_reason:
                        # 把choice的finish原因（finish_reason）属性保存到 finish原因（finish_reason）。
                        finish_reason = choice.finish_reason
                # 检查条件 not finish_reason or not received_text 是否成立；成立时执行缩进的分支。
                if not finish_reason or not received_text:
                    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                    raise AgentError(502, "LLM_INCOMPLETE_REPLY", "回复意外中断，请重试")
                # 等待刷新 上下文（context）完成后再继续。
                await refresh_context(context, java_client)
                # 等待保存指定键对应的值完成后再继续。
                await queue.put(
                    (
                        # 继续传入或组合固定文字 'message_end'，完成上面尚未结束的表达式。
                        "message_end",
                        {
                            # 填写模型（model）字段，值为模型（model）。
                            "model": model,
                            # 填写finish原因（finishReason）字段，值为finish原因（finish_reason）。
                            "finishReason": finish_reason,
                            # 填写完成（complete）字段，值为条件判断的结果：finish_reason == "stop"。
                            "complete": finish_reason == "stop",
                            # 填写用量（usage）字段，值为用量（usage）。
                            "usage": usage,
                            # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                            "traceId": context.trace_id,
                        },
                    )
                )
        # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
        except Exception as error:  # noqa: BLE001 - SSE 已开始后必须转为终止错误事件。
            # 把执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果保存到 失败（failure）。
            failure = safe_error(error, context.trace_id)
            # 等待保存指定键对应的值完成后再继续。
            await queue.put(
                (
                    # 继续传入或组合固定文字 'error'，完成上面尚未结束的表达式。
                    "error",
                    {
                        # 填写编码（code）字段，值为表达式 failure.code 的结果。
                        "code": failure.code,
                        # 填写消息（message）字段，值为表达式 failure.message 的结果。
                        "message": failure.message,
                        # 填写状态（status）字段，值为表达式 failure.status_code 的结果。
                        "status": failure.status_code,
                        # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                        "traceId": context.trace_id,
                    },
                )
            )
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 检查条件 upstream is not None 是否成立；成立时执行缩进的分支。
            if upstream is not None:
                # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
                with suppress(Exception):
                    # 等待释放连接、文件或客户端占用的资源完成后再继续。
                    await upstream.close()

    # 把执行“启动可同时推进的异步任务，并保存任务句柄”得到的结果保存到 producer。
    producer = asyncio.create_task(produce(), name="agent-chat-stream")
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
        yield event_frame(
            # 继续传入或组合固定文字 'message_start'，完成上面尚未结束的表达式。
            "message_start",
            {
                # 填写本次请求的追踪编号（traceId）字段，值为表达式 context.trace_id 的结果。
                "traceId": context.trace_id,
                # 填写模型（model）字段，值为读取并复用当前进程的运行配置。
                "model": get_settings().llm_model,
            },
        )
        # 只要条件 True 是否成立，就继续执行下面的循环。
        while True:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 把等待执行“等待异步处理，但不超过指定的超时时间”得到的结果完成后的结果保存到 事件数据（(event, data)）。
                event, data = await asyncio.wait_for(queue.get(), timeout=10.0)
            # 捕获TimeoutError异常，执行下面的错误处理。
            except TimeoutError:
                # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
                yield b": keep-alive\n\n"
                # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                continue
            # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
            yield event_frame(event, data)
            # 检查条件 event in {'message_end', 'error'} 是否成立；成立时执行缩进的分支。
            if event in {"message_end", "error"}:
                # 把空值交还给调用方，本函数到此结束。
                return
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 调用 producer.cancel 完成取消处理。
        producer.cancel()
        # 进入执行“调用 suppress 完成suppress处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with suppress(asyncio.CancelledError):
            # 等待producer完成后的结果。
            await producer
