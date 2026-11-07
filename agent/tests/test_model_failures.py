import asyncio
import json
import time
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace
from unittest.mock import AsyncMock

import httpx
import pytest
from openai import (
    APIConnectionError,
    APITimeoutError,
    AuthenticationError,
    BadRequestError,
    InternalServerError,
    RateLimitError,
)
from pydantic import SecretStr

from app.core.config import Settings
from app.core.context import AgentContext
from app.core.errors import AgentError
from app.llm.chat_service import safe_error
from app.services import observation_service, turn_service

PRIVATE_TEXT = "private-prompt-and-secret-must-not-appear"


def upstream_error(error_type=InternalServerError, status=500):
    request = httpx.Request("POST", "https://model.invalid/chat/completions")
    return error_type(
        PRIVATE_TEXT,
        response=httpx.Response(status, request=request),
        body={"error": {"message": PRIVATE_TEXT}},
    )


@pytest.mark.parametrize(("error", "code", "status"), [
    (upstream_error(), "LLM_UPSTREAM_ERROR", 502),
    (upstream_error(status=503), "LLM_UPSTREAM_ERROR", 502),
    (upstream_error(AuthenticationError, 401), "LLM_AUTH_FAILED", 502),
    (upstream_error(BadRequestError, 400), "LLM_PARAMETERS_REJECTED", 502),
    (upstream_error(RateLimitError, 429), "LLM_RATE_LIMIT", 429),
    (APIConnectionError(request=httpx.Request("POST", "https://model.invalid")),
     "LLM_UNAVAILABLE", 502),
    (APITimeoutError(request=httpx.Request("POST", "https://model.invalid")),
     "AGENT_TIMEOUT", 504),
    (ValueError(PRIVATE_TEXT), "AGENT_REQUEST_FAILED", 502),
])
def test_failures_are_classified_without_exposing_upstream_body(error, code, status, caplog):
    failure = safe_error(error, "test-model-failure")
    assert (failure.code, failure.status_code) == (code, status)
    assert PRIVATE_TEXT not in failure.message + caplog.text
    assert f"code={code}" in caplog.text
    if isinstance(error, InternalServerError):
        assert f"upstream_status={error.status_code}" in caplog.text
        assert "模型服务" in failure.message


def test_business_error_keeps_its_original_identity():
    original = AgentError(409, "PLAN_CHANGED", "方案已变化，请重新查看")
    assert safe_error(original, "test-model-failure") is original


async def test_failed_model_call_is_observed_once_without_hidden_retry(monkeypatch):
    record = AsyncMock()
    monkeypatch.setattr(observation_service, "record", record)
    error = upstream_error()
    create = AsyncMock(side_effect=error)
    observed = observation_service.ObservedCompletions(SimpleNamespace(create=create))
    with pytest.raises(InternalServerError) as caught:
        await observed.create(model="test", stream=True, messages=[])
    assert caught.value is error
    create.assert_awaited_once()
    record.assert_awaited_once()
    assert record.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"
    assert PRIVATE_TEXT not in str(record.call_args)


async def test_stream_failure_retains_received_content_and_records_its_cause(monkeypatch):
    record = AsyncMock()
    monkeypatch.setattr(observation_service, "record", record)
    chunk = SimpleNamespace(usage=None, choices=[SimpleNamespace(
        delta=SimpleNamespace(content="已收到的部分", tool_calls=[]))])
    source = SimpleNamespace(__anext__=AsyncMock(side_effect=[chunk, upstream_error()]),
                             close=AsyncMock())
    stream = observation_service.ObservedStream(source, started=time.monotonic(), model="test")
    assert (await stream.__anext__()).choices[0].delta.content == "已收到的部分"
    with pytest.raises(InternalServerError):
        await stream.__anext__()
    await stream.close()
    source.close.assert_awaited_once()
    record.assert_awaited_once()
    assert record.call_args.kwargs["status"] == "FAILED"
    assert record.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"


async def test_failed_conversation_emits_an_error_and_never_reports_success(monkeypatch):
    context = AgentContext("7", frozenset({"USER"}), frozenset({"agent:chat:send"}),
        SecretStr("test-only"), "test-model-failure", time.monotonic() + 30)
    started = turn_service.StartedTurn(7, 11, 31, "request", 0, 1, 2, "test", 0, "",
                                      datetime.now(UTC) + timedelta(seconds=30))
    settings = Settings(_env_file=None, llm_api_key="test-only", agent_tools_enabled=False)
    monkeypatch.setattr(turn_service, "get_settings", lambda: settings)
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[
        {"role": "system", "content": "test"}, {"role": "user", "content": "test"}]))
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    finalize = AsyncMock(return_value=True)
    monkeypatch.setattr(turn_service, "_finalize", finalize)
    prepare = AsyncMock()
    monkeypatch.setattr(turn_service, "_prepare_tools", prepare)
    create = AsyncMock(side_effect=upstream_error())
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(
        chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    frames = [frame.decode() async for frame in turn_service.stream_turn(
        object(), context, started, "test", object(), asyncio.Semaphore(1))]
    error = json.loads(frames[-1].split("data: ", 1)[1].strip())
    assert error["code"] == "LLM_UPSTREAM_ERROR"
    assert error["traceId"] == context.trace_id
    assert not any("event: message_end" in frame for frame in frames)
    assert PRIVATE_TEXT not in "".join(frames)
    assert finalize.call_args.kwargs["status"] == "FAILED"
    assert finalize.call_args.kwargs["error_code"] == "LLM_UPSTREAM_ERROR"
    assert finalize.call_args.kwargs["text"] == ""
    create.assert_awaited_once()
    prepare.assert_not_awaited()
