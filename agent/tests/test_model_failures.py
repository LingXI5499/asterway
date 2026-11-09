# 检查test模型failures相关行为，断言失败时报告具体不符合预期的场景。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）、两个时刻之间的时间长度（timedelta）。
from datetime import UTC, datetime, timedelta

# 从 types 引入 可以按属性名读取的简单对象（SimpleNamespace）。
from types import SimpleNamespace

# 从 unittest.mock 引入 异步Mock（AsyncMock）。
from unittest.mock import AsyncMock

# 引入 HTTP 请求客户端库（httpx），供下面调用其功能。
import httpx

# 引入 现有测试运行与断言库（pytest），供下面调用其功能。
import pytest

# 从 openai 引入 连接模型接口失败的异常（APIConnectionError）、模型接口等待超时的异常（APITimeoutError）、模型接口凭据被拒绝的异常（AuthenticationError）、模型接口不接受参数的异常（BadRequestError）、上游服务内部失败的异常（InternalServerError）、调用频率超限的异常（RateLimitError）。
from openai import (
    # 继续传入或组合连接模型接口失败的异常（APIConnectionError），完成上面尚未结束的表达式。
    APIConnectionError,
    # 继续传入或组合模型接口等待超时的异常（APITimeoutError），完成上面尚未结束的表达式。
    APITimeoutError,
    # 继续传入或组合模型接口凭据被拒绝的异常（AuthenticationError），完成上面尚未结束的表达式。
    AuthenticationError,
    # 继续传入或组合模型接口不接受参数的异常（BadRequestError），完成上面尚未结束的表达式。
    BadRequestError,
    # 继续传入或组合上游服务内部失败的异常（InternalServerError），完成上面尚未结束的表达式。
    InternalServerError,
    # 继续传入或组合调用频率超限的异常（RateLimitError），完成上面尚未结束的表达式。
    RateLimitError,
)

# 从 pydantic 引入 默认显示时隐藏实际内容的私密文字类型（SecretStr）。
from pydantic import SecretStr

# 从 app.core.config 引入 配置（Settings）。
from app.core.config import Settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.llm.chat_service 引入 把底层异常转换成脱敏的可读错误（safe_error）。
from app.llm.chat_service import safe_error

# 从 app.services 引入 调用观测业务服务（observation_service）、本轮对话业务服务（turn_service）。
from app.services import observation_service, turn_service

# 把固定文字 'private-prompt-and-secret-must-not-appear'保存到 PRIVATE文字（PRIVATE_TEXT）。
PRIVATE_TEXT = "private-prompt-and-secret-must-not-appear"


# 函数 upstream_error：调用 upstream_error 完成upstream错误处理；接收 错误类型（error_type）、状态（status）。
def upstream_error(error_type=InternalServerError, status=500):
    # 把执行“调用 httpx.Request 完成本次 HTTP 请求处理”得到的结果保存到 请求（request）。
    request = httpx.Request("POST", "https://model.invalid/chat/completions")
    # 把执行“调用 error_type 完成错误类型处理”得到的结果交还给调用方，本函数到此结束。
    return error_type(
        # 继续传入或组合PRIVATE文字（PRIVATE_TEXT），完成上面尚未结束的表达式。
        PRIVATE_TEXT,
        # 传入响应（response），值为调用 httpx.Response 完成响应处理。
        response=httpx.Response(status, request=request),
        # 传入请求正文（body），值为表达式 {"error": {"message": PRIVATE_TEXT}} 的结果。
        body={"error": {"message": PRIVATE_TEXT}},
    )


# 用这里列出的多组输入分别执行同一个检查。
@pytest.mark.parametrize(("error", "code", "status"), [
    # 继续传入或组合表达式 (upstream_error(), "LLM_UPSTREAM_ERROR", 502) 的结果，完成上面尚未结束的表达式。
    (upstream_error(), "LLM_UPSTREAM_ERROR", 502),
    # 继续传入或组合表达式 (upstream_error(status=503), "LLM_UPSTREAM_ERROR", 502) 的结果，完成上面尚未结束的表达式。
    (upstream_error(status=503), "LLM_UPSTREAM_ERROR", 502),
    # 继续传入或组合表达式 (upstream_error(AuthenticationError, 401), "LLM_AUTH_FAILED", 502) 的结果，完成上面尚未结束的表达式。
    (upstream_error(AuthenticationError, 401), "LLM_AUTH_FAILED", 502),
    # 继续传入或组合表达式 (upstream_error(BadRequestError, 400), "LLM_PARAMETERS_REJECTED", 502) 的结果，完成上面尚未结束的表达式。
    (upstream_error(BadRequestError, 400), "LLM_PARAMETERS_REJECTED", 502),
    # 继续传入或组合表达式 (upstream_error(RateLimitError, 429), "LLM_RATE_LIMIT", 429) 的结果，完成上面尚未结束的表达式。
    (upstream_error(RateLimitError, 429), "LLM_RATE_LIMIT", 429),
    # 继续传入或组合调用 (APIConnectionError(request=httpx.Request 完成本次 HTTP 请求处理，完成上面尚未结束的表达式。
    (APIConnectionError(request=httpx.Request("POST", "https://model.invalid")),
     # 继续传入或组合表达式 "LLM_UNAVAILABLE", 502) 的结果，完成上面尚未结束的表达式。
     "LLM_UNAVAILABLE", 502),
    # 继续传入或组合调用 (APITimeoutError(request=httpx.Request 完成本次 HTTP 请求处理，完成上面尚未结束的表达式。
    (APITimeoutError(request=httpx.Request("POST", "https://model.invalid")),
     # 继续传入或组合表达式 "AGENT_TIMEOUT", 504) 的结果，完成上面尚未结束的表达式。
     "AGENT_TIMEOUT", 504),
    # 继续传入或组合表达式 (ValueError(PRIVATE_TEXT), "AGENT_REQUEST_FAILED", 502) 的结果，完成上面尚未结束的表达式。
    (ValueError(PRIVATE_TEXT), "AGENT_REQUEST_FAILED", 502),
])
# 函数 test_failures_are_classified_without_exposing_upstream_body：调用 test_failures_are_classified_without_exposing_upstream_body 完成testfailuresareclassifiedwithoutexposingupstream请求正文处理；接收 错误（error）、编码（code）、状态（status）、caplog。
def test_failures_are_classified_without_exposing_upstream_body(error, code, status, caplog):
    # 把执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果保存到 失败（failure）。
    failure = safe_error(error, "test-model-failure")
    # 断言条件 failure.code, failure.status_code) == (code, status 是否成立；不满足时本项检查失败。
    assert (failure.code, failure.status_code) == (code, status)
    # 断言条件 PRIVATE_TEXT not in failure.message + caplog.text 是否成立；不满足时本项检查失败。
    assert PRIVATE_TEXT not in failure.message + caplog.text
    # 断言条件 f'code={code}' in caplog.text 是否成立；不满足时本项检查失败。
    assert f"code={code}" in caplog.text
    # 检查条件 isinstance(error, InternalServerError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, InternalServerError):
        # 断言条件 f'upstream_status={error.status_code}' in caplog.text 是否成立；不满足时本项检查失败。
        assert f"upstream_status={error.status_code}" in caplog.text
        # 断言条件 '模型服务' in failure.message 是否成立；不满足时本项检查失败。
        assert "模型服务" in failure.message


# 函数 test_business_error_keeps_its_original_identity：调用 test_business_error_keeps_its_original_identity 完成testbusiness错误keepsits原始账户管理处理。
def test_business_error_keeps_its_original_identity():
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果保存到 原始（original）。
    original = AgentError(409, "PLAN_CHANGED", "方案已变化，请重新查看")
    # 断言条件 safe_error(original, 'test-model-failure') is original 是否成立；不满足时本项检查失败。
    assert safe_error(original, "test-model-failure") is original


# 异步函数 test_failed_model_call_is_observed_once_without_hidden_retry：调用 test_failed_model_call_is_observed_once_without_hidden_retry 完成test失败模型调用是否observedoncewithouthidden重试处理；接收 monkeypatch。
async def test_failed_model_call_is_observed_once_without_hidden_retry(monkeypatch):
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 记录（record）。
    record = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(observation_service, "record", record)
    # 把执行“调用 upstream_error 完成upstream错误处理”得到的结果保存到 错误（error）。
    error = upstream_error()
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 创建（create）。
    create = AsyncMock(side_effect=error)
    # 把执行“调用 observation_service.ObservedCompletions 完成ObservedCompletions处理”得到的结果保存到 observed。
    observed = observation_service.ObservedCompletions(SimpleNamespace(create=create))
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 caught；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(InternalServerError) as caught:
        # 等待调用 observed.create 完成创建处理完成后再继续。
        await observed.create(model="test", stream=True, messages=[])
    # 断言条件 caught.value is error 是否成立；不满足时本项检查失败。
    assert caught.value is error
    # 断言 awaitedonce（awaited_once）。
    create.assert_awaited_once()
    # 断言 awaitedonce（awaited_once）。
    record.assert_awaited_once()
    # 断言条件 record.call_args.kwargs['error_code'] == 'LLM_UPSTREAM_ERROR' 是否成立；不满足时本项检查失败。
    assert record.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"
    # 断言条件 PRIVATE_TEXT not in str(record.call_args) 是否成立；不满足时本项检查失败。
    assert PRIVATE_TEXT not in str(record.call_args)


# 异步函数 test_stream_failure_retains_received_content_and_records_its_cause：调用 test_stream_failure_retains_received_content_and_records_its_cause 完成test分段回复流失败retainsreceived内容and记录列表itscause处理；接收 monkeypatch。
async def test_stream_failure_retains_received_content_and_records_its_cause(monkeypatch):
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 记录（record）。
    record = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(observation_service, "record", record)
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 文档片段（chunk）。
    chunk = SimpleNamespace(usage=None, choices=[SimpleNamespace(
        # 传入delta，值为调用 SimpleNamespace 完成可以按属性名读取的简单对象处理。
        delta=SimpleNamespace(content="已收到的部分", tool_calls=[]))])
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 来源（source）。
    source = SimpleNamespace(__anext__=AsyncMock(side_effect=[chunk, upstream_error()]),
                             # 传入关闭（close），值为调用 AsyncMock 完成异步Mock处理。
                             close=AsyncMock())
    # 把执行“调用 observation_service.ObservedStream 完成Observed分段回复流处理”得到的结果保存到 分段回复流（stream）。
    stream = observation_service.ObservedStream(source, started=time.monotonic(), model="test")
    # 断言条件 (await stream.__anext__()).choices[0].delta.content == '已收到的部分' 是否成立；不满足时本项检查失败。
    assert (await stream.__anext__()).choices[0].delta.content == "已收到的部分"
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(InternalServerError):
        # 等待调用 stream.__anext__ 完成anext处理完成后再继续。
        await stream.__anext__()
    # 等待释放连接、文件或客户端占用的资源完成后再继续。
    await stream.close()
    # 断言 awaitedonce（awaited_once）。
    source.close.assert_awaited_once()
    # 断言 awaitedonce（awaited_once）。
    record.assert_awaited_once()
    # 断言条件 record.call_args.kwargs['status'] == 'FAILED' 是否成立；不满足时本项检查失败。
    assert record.call_args.kwargs["status"] == "FAILED"
    # 断言条件 record.call_args.kwargs['error_code'] == 'LLM_UPSTREAM_ERROR' 是否成立；不满足时本项检查失败。
    assert record.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"


# 异步函数 test_failed_conversation_emits_an_error_and_never_reports_success：调用 test_failed_conversation_emits_an_error_and_never_reports_success 完成test失败对话emitsan错误andnever复盘报告列表成功处理；接收 monkeypatch。
async def test_failed_conversation_emits_an_error_and_never_reports_success(monkeypatch):
    # 把执行“调用 AgentContext 完成成长助手上下文处理”得到的结果保存到 上下文（context）。
    context = AgentContext("7", frozenset({"USER"}), frozenset({"agent:chat:send"}),
        # 继续传入或组合调用 SecretStr("test-only"), "test-model-failure", time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
        SecretStr("test-only"), "test-model-failure", time.monotonic() + 30)
    # 把执行“调用 turn_service.StartedTurn 完成开始时本轮对话处理”得到的结果保存到 开始时（started）。
    started = turn_service.StartedTurn(7, 11, 31, "request", 0, 1, 2, "test", 0, "",
                                      # 继续传入或组合当前真实时间，用于记录操作发生的时刻，完成上面尚未结束的表达式。
                                      datetime.now(UTC) + timedelta(seconds=30))
    # 把执行“调用 Settings 完成配置处理”得到的结果保存到 配置（settings）。
    settings = Settings(_env_file=None, llm_api_key="test-only", agent_tools_enabled=False)
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "get_settings", lambda: settings)
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[
        # 继续传入或组合表达式 {"role": "system", "content": "test"}, {"role": "user", "content": "test"}])) 的结果，完成上面尚未结束的表达式。
        {"role": "system", "content": "test"}, {"role": "user", "content": "test"}]))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 finalize。
    finalize = AsyncMock(return_value=True)
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_finalize", finalize)
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 prepare。
    prepare = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_prepare_tools", prepare)
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 创建（create）。
    create = AsyncMock(side_effect=upstream_error())
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(
        # 传入对话（chat），值为调用 SimpleNamespace 完成可以按属性名读取的简单对象处理。
        chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 frames。
    frames = [frame.decode() async for frame in turn_service.stream_turn(
        # 继续传入或组合调用 object(), context, started, "test", object(), asyncio.Semaphore 完成并发名额控制器处理，完成上面尚未结束的表达式。
        object(), context, started, "test", object(), asyncio.Semaphore(1))]
    # 把执行“把 JSON 文字解析为对象”得到的结果保存到 错误（error）。
    error = json.loads(frames[-1].split("data: ", 1)[1].strip())
    # 断言条件 error['code'] == 'LLM_UPSTREAM_ERROR' 是否成立；不满足时本项检查失败。
    assert error["code"] == "LLM_UPSTREAM_ERROR"
    # 断言条件 error['traceId'] == context.trace_id 是否成立；不满足时本项检查失败。
    assert error["traceId"] == context.trace_id
    # 断言条件 not any(('event: message_end' in frame for frame in frames)) 是否成立；不满足时本项检查失败。
    assert not any("event: message_end" in frame for frame in frames)
    # 断言条件 PRIVATE_TEXT not in ''.join(frames) 是否成立；不满足时本项检查失败。
    assert PRIVATE_TEXT not in "".join(frames)
    # 断言条件 finalize.call_args.kwargs['status'] == 'FAILED' 是否成立；不满足时本项检查失败。
    assert finalize.call_args.kwargs["status"] == "FAILED"
    # 断言条件 finalize.call_args.kwargs['error_code'] == 'LLM_UPSTREAM_ERROR' 是否成立；不满足时本项检查失败。
    assert finalize.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"
    # 断言条件 finalize.call_args.kwargs['text'] == '' 是否成立；不满足时本项检查失败。
    assert finalize.call_args.kwargs["text"] == ""
    # 断言 awaitedonce（awaited_once）。
    create.assert_awaited_once()
    # 断言 notawaited（not_awaited）。
    prepare.assert_not_awaited()
