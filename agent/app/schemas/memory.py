"""长期记忆请求与响应模型；对外字段统一 camelCase。"""

from datetime import datetime

from pydantic import Field, field_validator

from app.schemas.conversation import CamelModel, StrictCamelModel

MEMORY_KINDS = ("PREFERENCE", "FACT", "CONSTRAINT", "GOAL_CONTEXT")
MEMORY_STATUSES = ("CANDIDATE", "CONFIRMED", "REJECTED", "ARCHIVED")
MEMORY_SOURCES = ("USER_CONFIRMED", "USER_STATED", "INFERRED")


def _clean_content(value: str) -> str:
    content = value.strip()
    if not content:
        raise ValueError("记忆内容不能为空白")
    return content


class MemoryCandidateRequest(StrictCamelModel):
    kind: str = Field(pattern="^(PREFERENCE|FACT|CONSTRAINT|GOAL_CONTEXT)$")
    content: str = Field(min_length=1, max_length=4000)
    source: str = Field(
        default="USER_STATED", pattern="^(USER_CONFIRMED|USER_STATED|INFERRED)$"
    )
    confidence: float = Field(default=0.8, ge=0, le=1)
    conversationId: int | None = Field(default=None, ge=1)
    turnId: int | None = Field(default=None, ge=1)
    supersedesId: int | None = Field(default=None, ge=1)

    @field_validator("content")
    @classmethod
    def validate_content(cls, value: str) -> str:
        return _clean_content(value)


class MemoryUpdateRequest(StrictCamelModel):
    content: str = Field(min_length=1, max_length=4000)
    version: int = Field(ge=0)

    @field_validator("content")
    @classmethod
    def validate_content(cls, value: str) -> str:
        return _clean_content(value)


class MemoryVersionRequest(StrictCamelModel):
    version: int = Field(ge=0)


class MemoryVO(CamelModel):
    id: str
    kind: str
    kind_label: str
    content: str
    source: str
    status: str
    status_label: str
    confidence: float
    source_conversation_id: str | None = None
    source_turn_id: str | None = None
    supersedes_id: str | None = None
    confirmed_at: datetime | None = None
    rejected_at: datetime | None = None
    archived_at: datetime | None = None
    created_at: datetime
    updated_at: datetime
    version: int


class MemoryPageVO(CamelModel):
    items: list[MemoryVO]
    total: int
    page: int
    size: int


class MemoryContextVO(CamelModel):
    """供内部调用检查当前会注入哪些已确认记忆。"""

    items: list[MemoryVO]
    chars: int
