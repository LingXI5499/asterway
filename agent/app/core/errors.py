"""可对外返回的错误，不携带请求头、凭据或上游响应原文。"""

from typing import Any


class AgentError(Exception):
    def __init__(
        self,
        status_code: int,
        code: str,
        message: str,
        data: Any | None = None,
    ) -> None:
        super().__init__(message)
        self.status_code = status_code
        self.code = code
        self.message = message
        self.data = data


class JavaApiError(AgentError):
    """Java 认证、业务或通信失败；保留可安全识别的状态。"""
