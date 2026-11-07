"""工具执行边界：权限、参数、超时、结果裁剪和审计，失败转为模型可读结果。"""

import asyncio
import json
import logging
import re
import time
from typing import Any

from pydantic import ValidationError

from app.core.config import get_settings
from app.core.context import AgentContext, refresh_context
from app.core.errors import AgentError, JavaApiError
from app.integrations.java_client import JavaApiClient
from app.tools.base import ToolExecution, ToolOutcome
from app.tools.registry import label_for, tool_for

logger = logging.getLogger(__name__)

_SAFE_NAME = re.compile(r"[a-z][a-z0-9_]{0,63}")
_UNKNOWN = "unknown"


def _safe_name(name: str) -> str:
    return name if isinstance(name, str) and _SAFE_NAME.fullmatch(name) else _UNKNOWN


def _error(message: str) -> ToolOutcome:
    return ToolOutcome(
        content=json.dumps({"error": message}, ensure_ascii=False),
        summary=message,
        is_error=True,
    )


def _finish(
    name: str,
    label: str,
    outcome: ToolOutcome,
    started: float,
) -> ToolExecution:
    duration_ms = max(0, int((time.monotonic() - started) * 1000))
    try:
        data = json.loads(outcome.content)
    except (ValueError, RecursionError):
        data = {"text": outcome.content}
    content = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    limit = get_settings().agent_tool_max_result_chars
    if len(content) > limit:
        preview = content[: max(0, (limit - 256) // 2)]
        while True:
            bounded = json.dumps(
                {
                    "truncated": True,
                    "message": "查询结果过长，以下仅为部分预览，不能当作完整列表",
                    "preview": preview,
                },
                ensure_ascii=False,
                separators=(",", ":"),
            )
            if len(bounded) <= limit:
                content = bounded
                break
            preview = preview[: len(preview) // 2]
    logger.info(
        "agent_tool_call tool=%s status=%s duration_ms=%d",
        name,
        "error" if outcome.is_error else "ok",
        duration_ms,
    )
    return ToolExecution(
        name=name,
        label=label,
        content=content,
        summary=outcome.summary[:200],
        is_error=outcome.is_error,
        duration_ms=duration_ms,
    )


def budget_exceeded(name: str) -> ToolExecution:
    """超出调用次数时仍为每个 tool_call 返回结果，保证下一轮消息结构完整。"""
    safe = _safe_name(name)
    return ToolExecution(
        name=safe,
        label=label_for(safe),
        content=json.dumps(
            {"status": "limited", "notice": "本轮已取得部分资料，剩余查询暂缓执行。"
             "请说明哪些范围尚未核对，基于已取得的事实回答；用户可继续追问。"},
            ensure_ascii=False,
        ),
        summary="本轮先汇总已取得的资料，此项尚未查询，可继续追问",
        is_error=False,
        duration_ms=0,
        is_limited=True,
    )


async def _execute(
    context: AgentContext,
    java_client: JavaApiClient,
    name: str,
    arguments: str,
) -> ToolExecution:
    started = time.monotonic()
    spec = tool_for(name)
    if spec is None:
        safe = _safe_name(name)
        return _finish(safe, label_for(safe), _error("未知的查询工具"), started)
    if not spec.allowed(context.permissions):
        return _finish(
            spec.name,
            spec.label,
            _error("当前账户没有该查询的权限，无法读取这类业务数据"),
            started,
        )
    try:
        parsed: Any = spec.args_model.model_validate_json(arguments or "{}")
    except ValidationError:
        return _finish(spec.name, spec.label, _error("查询参数不符合要求"), started)
    try:
        async with asyncio.timeout(get_settings().agent_tool_timeout_seconds):
            current = await refresh_context(context, java_client)
            if not spec.allowed(current.permissions):
                return _finish(spec.name, spec.label, _error("当前账户已没有该查询权限"), started)
            outcome = await spec.handler(current, java_client, parsed)
    except TimeoutError:
        outcome = _error("查询超时，请缩小查询范围后重试")
    except JavaApiError as error:
        if error.status_code == 401:
            raise
        outcome = _error(error.message)
    except AgentError:
        # 登录失效、管理身份或发送权限撤销必须终止生成，不能伪装成普通查询失败。
        raise
    except Exception as error:  # noqa: BLE001 - 工具失败不能中断整个回答。
        logger.warning(
            "agent_tool_failed tool=%s type=%s trace=%s",
            spec.name,
            type(error).__name__,
            context.trace_id,
        )
        outcome = _error("查询暂时不可用，请稍后重试")
    return _finish(spec.name, spec.label, outcome, started)


async def execute(context: AgentContext, java_client: JavaApiClient, name: str, arguments: str) -> ToolExecution:
    from app.services.observation_service import record
    started = time.monotonic()
    result = await _execute(context, java_client, name, arguments)
    await record(kind="TOOL", name=result.name, started=started,
        status="FAILED" if result.is_error else "SUCCEEDED",
        error_code="TOOL_FAILED" if result.is_error else None)
    return result
