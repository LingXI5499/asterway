"""调用观测仅保存身份范围、用量和耗时，永不保存正文或认证资料。"""

import logging
import time
from contextvars import ContextVar
from dataclasses import dataclass
from typing import Any

from app.db.session import Database
from app.db.workspace_models import CallRecord
from app.llm.errors import classify_model_error

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class Observation:
    db: Database
    user_id: int
    trace_id: str


current_observation: ContextVar[Observation | None] = ContextVar("agent_observation", default=None)


async def record(*, kind: str, name: str, started: float, status: str,
                 usage: Any = None, first_token_ms: int | None = None,
                 error_code: str | None = None):
    value = current_observation.get()
    if value is None:
        return
    try:
        async with value.db.session() as session:
            session.add(CallRecord(user_id=value.user_id, trace_id=value.trace_id,
                kind=kind, name=name[:128], status=status,
                duration_ms=max(0, int((time.monotonic()-started)*1000)),
                first_token_ms=first_token_ms,
                prompt_tokens=getattr(usage, "prompt_tokens", None),
                completion_tokens=getattr(usage, "completion_tokens", None),
                error_code=error_code))
    except Exception as error:  # noqa: BLE001 - 观测失败不影响主业务
        logger.warning("observation_write_failed type=%s", type(error).__name__)


class ObservedStream:
    def __init__(self, stream, *, started: float, model: str):
        self.stream, self.started, self.model = stream, started, model
        self.usage = None
        self.first = None
        self.closed = False
        self.status = "CANCELLED"
        self.error_code = None

    def __aiter__(self):
        return self

    async def __anext__(self):
        try:
            chunk = await self.stream.__anext__()
            if chunk.usage is not None:
                self.usage = chunk.usage
            if self.first is None and any(choice.delta.content or choice.delta.tool_calls for choice in chunk.choices):
                self.first = max(0, int((time.monotonic()-self.started)*1000))
            return chunk
        except StopAsyncIteration:
            self.status = "SUCCEEDED"
            await self.close()
            raise
        except BaseException as error:
            self.status = "FAILED"
            self.error_code = classify_model_error(error).code
            await self.close()
            raise

    async def close(self):
        if self.closed:
            return
        self.closed = True
        try:
            await self.stream.close()
        finally:
            await record(kind="MODEL", name=self.model, started=self.started, status=self.status,
                usage=self.usage, first_token_ms=self.first,
                error_code=self.error_code)


class ObservedCompletions:
    def __init__(self, delegate):
        self.delegate = delegate

    async def create(self, **kwargs):
        started = time.monotonic()
        model = str(kwargs.get("model", "unknown"))
        try:
            result = await self.delegate.create(**kwargs)
        except BaseException as error:
            await record(kind="MODEL", name=model, started=started, status="FAILED",
                error_code=classify_model_error(error).code)
            raise
        if kwargs.get("stream"):
            return ObservedStream(result, started=started, model=model)
        await record(kind="MODEL", name=model, started=started, status="SUCCEEDED", usage=result.usage)
        return result
