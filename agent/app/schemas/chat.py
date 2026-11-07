from pydantic import BaseModel, ConfigDict, Field, field_validator


class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    message: str = Field(
        min_length=1,
        max_length=8000,
        description="用户发送的消息",
    )

    @field_validator("message")
    @classmethod
    def validate_message(cls, value: str) -> str:
        if not value.strip():
            raise ValueError("消息不能全部为空白")
        return value


class ChatResponse(BaseModel):
    reply: str
    model: str
    finish_reason: str = "stop"
    trace_id: str | None = None
