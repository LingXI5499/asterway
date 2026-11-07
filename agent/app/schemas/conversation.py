"""会话、轮次与消息的请求和响应模型；对外字段统一使用 camelCase。"""

from datetime import datetime
from typing import Any
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


class StrictCamelModel(CamelModel):
    model_config = ConfigDict(
        alias_generator=to_camel, populate_by_name=True, extra="forbid"
    )


def _clean_title(value: str) -> str:
    title = value.strip()
    if not title:
        raise ValueError("标题不能为空白")
    return title


class ConversationCreateRequest(StrictCamelModel):
    title: str | None = Field(default=None, max_length=100)

    @field_validator("title")
    @classmethod
    def validate_title(cls, value: str | None) -> str | None:
        return None if value is None else _clean_title(value)


class ConversationTitleRequest(StrictCamelModel):
    title: str = Field(min_length=1, max_length=100)
    version: int = Field(ge=0)

    @field_validator("title")
    @classmethod
    def validate_title(cls, value: str) -> str:
        return _clean_title(value)


class ConversationVersionRequest(StrictCamelModel):
    version: int = Field(ge=0)


class ConversationMessageRequest(StrictCamelModel):
    message: str = Field(min_length=1, max_length=8000)
    request_id: UUID

    @field_validator("message")
    @classmethod
    def validate_message(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("消息不能全部为空白")
        return value


class ConversationVO(CamelModel):
    id: str
    title: str
    status: str
    summary_through_message_no: int
    last_message_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    version: int


class TurnVO(CamelModel):
    id: str
    conversation_id: str
    request_id: str
    status: str
    finish_reason: str | None = None
    error_code: str | None = None
    started_at: datetime
    finished_at: datetime | None = None
    version: int
    reply: str | None = None


class ConversationDetailVO(ConversationVO):
    running_turn: TurnVO | None = None


class MessageVO(CamelModel):
    citations: list[dict[str, Any]] = Field(default_factory=list)
    used_memory_ids: list[str] = Field(default_factory=list)
    id: str
    message_no: int
    role: str
    content: str
    is_complete: bool
    finish_reason: str | None = None
    model: str | None = None
    usage: dict[str, Any] | None = None
    created_at: datetime


class MessagePageVO(CamelModel):
    items: list[MessageVO]
    has_more: bool
    next_before_message_no: int | None = None


class ConversationPageVO(CamelModel):
    items: list[ConversationVO]
    total: int
    page: int
    size: int


class SendResultVO(CamelModel):
    reply: str
    model: str
    finish_reason: str
    conversation_id: str
    turn_id: str
    request_id: str
    turn_version: int
