"""只读业务工具的类型定义；工具只查询，不写数据库、不改变业务事实。"""

from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import Any

from pydantic import BaseModel

from app.core.context import AgentContext
from app.integrations.java_client import JavaApiClient

ToolHandler = Callable[[AgentContext, JavaApiClient, BaseModel], Awaitable["ToolOutcome"]]


@dataclass(frozen=True)
class ToolOutcome:
    """工具返回给模型的内容；content 必须是可直接放入 tool 消息的文本。"""

    content: str
    summary: str
    is_error: bool = False


@dataclass(frozen=True)
class ToolSpec:
    name: str
    label: str
    description: str
    permission: str
    args_model: type[BaseModel]
    handler: ToolHandler
    extra_permissions: tuple[str, ...] = ()

    def allowed(self, permissions: frozenset[str]) -> bool:
        return all(value in permissions for value in (self.permission, *self.extra_permissions))

    @property
    def parameters(self) -> dict[str, Any]:
        return self.args_model.model_json_schema()


@dataclass(frozen=True)
class ToolExecution:
    """一次工具调用的最终结果，供轮次服务和 SSE 使用。"""

    name: str
    label: str
    content: str
    summary: str
    is_error: bool
    duration_ms: int
    is_limited: bool = False
