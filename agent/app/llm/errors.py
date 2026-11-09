# 把失败区分为认证、限流、参数、上游服务、连接或超时，向页面返回可读原因。
# 模型错误分类；只返回固定提示，不传播上游正文或认证资料。

# 从 openai 引入 连接模型接口失败的异常（APIConnectionError）、接口状态错误（APIStatusError）、模型接口等待超时的异常（APITimeoutError）、模型接口凭据被拒绝的异常（AuthenticationError）、模型接口不接受参数的异常（BadRequestError）、调用频率超限的异常（RateLimitError）。
from openai import (
    # 继续传入或组合连接模型接口失败的异常（APIConnectionError），完成上面尚未结束的表达式。
    APIConnectionError,
    # 继续传入或组合接口状态错误（APIStatusError），完成上面尚未结束的表达式。
    APIStatusError,
    # 继续传入或组合模型接口等待超时的异常（APITimeoutError），完成上面尚未结束的表达式。
    APITimeoutError,
    # 继续传入或组合模型接口凭据被拒绝的异常（AuthenticationError），完成上面尚未结束的表达式。
    AuthenticationError,
    # 继续传入或组合模型接口不接受参数的异常（BadRequestError），完成上面尚未结束的表达式。
    BadRequestError,
    # 继续传入或组合调用频率超限的异常（RateLimitError），完成上面尚未结束的表达式。
    RateLimitError,
)

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError


# 函数 classify_model_error：调用 classify_model_error 完成classify模型错误处理；接收 错误（error）。
def classify_model_error(error: BaseException) -> AgentError:
    # 检查条件 isinstance(error, AgentError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, AgentError):
        # 把错误（error）交还给调用方，本函数到此结束。
        return error
    # 检查条件 isinstance(error, AuthenticationError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, AuthenticationError):
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(502, "LLM_AUTH_FAILED", "模型服务认证失败，请联系管理员检查配置")
    # 检查条件 isinstance(error, RateLimitError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, RateLimitError):
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(429, "LLM_RATE_LIMIT", "模型服务繁忙，请稍后手动重试")
    # 检查条件 isinstance(error, BadRequestError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, BadRequestError):
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(502, "LLM_PARAMETERS_REJECTED", "模型服务拒绝了请求参数，请检查模型配置后重试")
    # 检查条件 isinstance(error, APIStatusError) and error.status_code >= 500 是否成立；成立时执行缩进的分支。
    if isinstance(error, APIStatusError) and error.status_code >= 500:
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(502, "LLM_UPSTREAM_ERROR", "模型服务暂时异常，未能完成本轮回复，请稍后重试")
    # 检查条件 isinstance(error, (TimeoutError, APITimeoutError)) 是否成立；成立时执行缩进的分支。
    if isinstance(error, (TimeoutError, APITimeoutError)):
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(504, "AGENT_TIMEOUT", "回复超时，请稍后重试")
    # 检查条件 isinstance(error, APIConnectionError) 是否成立；成立时执行缩进的分支。
    if isinstance(error, APIConnectionError):
        # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
        return AgentError(502, "LLM_UNAVAILABLE", "暂时无法连接模型服务")
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
    return AgentError(502, "AGENT_REQUEST_FAILED", "对话服务暂时不可用，请稍后重试")
