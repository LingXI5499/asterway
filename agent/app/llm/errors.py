"""模型错误分类；只返回固定提示，不传播上游正文或认证资料。"""

from openai import (
    APIConnectionError,
    APIStatusError,
    APITimeoutError,
    AuthenticationError,
    BadRequestError,
    RateLimitError,
)

from app.core.errors import AgentError


def classify_model_error(error: BaseException) -> AgentError:
    if isinstance(error, AgentError):
        return error
    if isinstance(error, AuthenticationError):
        return AgentError(502, "LLM_AUTH_FAILED", "模型服务认证失败，请联系管理员检查配置")
    if isinstance(error, RateLimitError):
        return AgentError(429, "LLM_RATE_LIMIT", "模型服务繁忙，请稍后手动重试")
    if isinstance(error, BadRequestError):
        return AgentError(502, "LLM_PARAMETERS_REJECTED", "模型服务拒绝了请求参数，请检查模型配置后重试")
    if isinstance(error, APIStatusError) and error.status_code >= 500:
        return AgentError(502, "LLM_UPSTREAM_ERROR", "模型服务暂时异常，未能完成本轮回复，请稍后重试")
    if isinstance(error, (TimeoutError, APITimeoutError)):
        return AgentError(504, "AGENT_TIMEOUT", "回复超时，请稍后重试")
    if isinstance(error, APIConnectionError):
        return AgentError(502, "LLM_UNAVAILABLE", "暂时无法连接模型服务")
    return AgentError(502, "AGENT_REQUEST_FAILED", "对话服务暂时不可用，请稍后重试")
