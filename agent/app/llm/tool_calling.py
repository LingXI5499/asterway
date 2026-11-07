"""流式工具调用的增量拼装；只构造消息，不执行工具。"""

import json
import re
from dataclasses import dataclass
from typing import Any

from app.core.config import get_settings
from app.core.errors import AgentError

_MAX_ARGUMENT_CHARS = 8192
_NAME = re.compile(r"[a-z][a-z0-9_]{0,63}")
_CALL_ID = re.compile(r"[A-Za-z0-9_-]{1,128}")


def _invalid() -> AgentError:
    return AgentError(502, "LLM_TOOL_CALL_INVALID", "模型查询请求格式不完整，请重新提问")


@dataclass(frozen=True)
class ToolCall:
    id: str
    name: str
    arguments: str


def merge_tool_deltas(accumulator: dict[int, dict[str, str]], deltas: Any) -> None:
    """完整拼装字段分片；超出预算立即拒绝，不截断参数后执行。"""
    for delta in deltas or []:
        index = getattr(delta, "index", None)
        if (
            not isinstance(index, int)
            or isinstance(index, bool)
            or not 0 <= index < get_settings().agent_tool_max_calls
        ):
            raise _invalid()
        entry = accumulator.setdefault(index, {"id": "", "name": "", "arguments": ""})
        call_id = getattr(delta, "id", None)
        if isinstance(call_id, str) and call_id:
            entry["id"] += call_id
            if len(entry["id"]) > 128:
                raise _invalid()
        function = getattr(delta, "function", None)
        if function is None:
            continue
        name = getattr(function, "name", None)
        if isinstance(name, str) and name:
            entry["name"] += name
            if len(entry["name"]) > 64:
                raise _invalid()
        arguments = getattr(function, "arguments", None)
        if isinstance(arguments, str) and arguments:
            if len(entry["arguments"]) + len(arguments) > _MAX_ARGUMENT_CHARS:
                raise _invalid()
            entry["arguments"] += arguments


def build_tool_calls(accumulator: dict[int, dict[str, str]]) -> list[ToolCall]:
    calls: list[ToolCall] = []
    ids: set[str] = set()
    for index in sorted(accumulator):
        entry = accumulator[index]
        name = entry["name"]
        call_id = entry["id"]
        if not _NAME.fullmatch(name) or not _CALL_ID.fullmatch(call_id) or call_id in ids:
            raise _invalid()
        ids.add(call_id)
        calls.append(
            ToolCall(
                id=call_id,
                name=name,
                arguments=entry["arguments"] or "{}",
            )
        )
    payload = assistant_tool_message("", calls)["tool_calls"]
    if len(json.dumps(payload, ensure_ascii=False).encode()) > 262144:
        raise _invalid()
    return calls


def assistant_tool_message(content: str, calls: list[ToolCall]) -> dict[str, Any]:
    return {
        "role": "assistant",
        "content": content or "",
        "tool_calls": [
            {
                "id": call.id,
                "type": "function",
                "function": {"name": call.name, "arguments": call.arguments},
            }
            for call in calls
        ],
    }


def tool_result_message(call_id: str, content: str) -> dict[str, Any]:
    return {"role": "tool", "tool_call_id": call_id, "content": content}
