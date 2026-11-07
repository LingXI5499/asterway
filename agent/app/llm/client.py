from types import SimpleNamespace

from openai import AsyncOpenAI

from app.core.config import get_settings
from app.services.observation_service import ObservedCompletions

_client: AsyncOpenAI | None = None


def get_llm_client():
    global _client
    if _client is None:
        settings = get_settings()
        _client = AsyncOpenAI(
            api_key=settings.llm_api_key.get_secret_value(),
            base_url=settings.llm_base_url,
            timeout=settings.llm_timeout_seconds,
            max_retries=0,
        )
    return SimpleNamespace(chat=SimpleNamespace(completions=ObservedCompletions(_client.chat.completions)))


async def close_llm_client() -> None:
    global _client
    if _client is not None:
        await _client.close()
        _client = None
