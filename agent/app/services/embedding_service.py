"""OpenAI 兼容的文本向量接口；未配置密钥时不发起网络请求。"""

import math
import time
from typing import Any

import httpx

from app.core.config import get_settings
from app.core.errors import AgentError


def _endpoint(base: str) -> str:
    normalized = base.rstrip("/")
    if normalized.endswith("/embeddings"):
        return normalized
    return normalized + "/embeddings"


async def _embed_texts(texts: list[str]) -> list[list[float]]:
    if not texts:
        return []
    settings = get_settings()
    api_key = settings.embedding_api_key.get_secret_value()
    if not api_key:
        raise AgentError(503, "EMBEDDING_NOT_CONFIGURED", "向量服务尚未配置")
    if not settings.embedding_base_url or not settings.embedding_model:
        raise AgentError(503, "EMBEDDING_NOT_CONFIGURED", "向量服务尚未配置")
    if len(texts) > 10:
        raise AgentError(422, "EMBEDDING_BATCH_TOO_LARGE", "单次向量请求最多包含 10 段文本")
    payload: dict[str, Any] = {
        "model": settings.embedding_model,
        "input": texts,
        "dimensions": settings.embedding_dimension,
    }
    try:
        async with httpx.AsyncClient(
            timeout=settings.embedding_timeout_seconds, trust_env=False
        ) as client:
            response = await client.post(
                _endpoint(settings.embedding_base_url),
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
            )
    except httpx.HTTPError as error:
        raise AgentError(502, "EMBEDDING_UNAVAILABLE", "向量服务暂时不可用") from error
    if response.status_code != 200:
        raise AgentError(502, "EMBEDDING_FAILED", "向量服务返回失败")
    try:
        body = response.json()
        items = body["data"]
        if not isinstance(items, list) or len(items) != len(texts):
            raise ValueError("Unexpected embedding count")
        indexed: dict[int, list[float]] = {}
        for item in items:
            index = item["index"]
            if type(index) is not int or index < 0 or index >= len(texts) or index in indexed:
                raise ValueError("Invalid embedding index")
            values = item["embedding"]
            if not isinstance(values, list) or any(
                type(value) not in (int, float) for value in values
            ):
                raise ValueError("Invalid embedding values")
            vector = list(map(float, values))
            if len(vector) != settings.embedding_dimension or not all(map(math.isfinite, vector)):
                raise ValueError("Invalid embedding dimension")
            if not any(vector):
                raise ValueError("Empty direction vector")
            indexed[index] = vector
        vectors = [indexed[index] for index in range(len(texts))]
    except (KeyError, TypeError, ValueError) as error:
        raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量服务返回结构不正确") from error
    if len(vectors) != len(texts) or any(not vector for vector in vectors):
        raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量返回数量不正确")
    return vectors


async def embed_texts(texts: list[str]) -> list[list[float]]:
    from app.services.observation_service import record

    if not texts:
        return []
    started = time.monotonic()
    try:
        result = await _embed_texts(texts)
    except AgentError as error:
        await record(kind="EMBEDDING", name=get_settings().embedding_model, started=started,
            status="FAILED", error_code=error.code)
        raise
    await record(kind="EMBEDDING", name=get_settings().embedding_model, started=started,
        status="SUCCEEDED")
    return result
