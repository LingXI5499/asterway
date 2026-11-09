# 把失败区分为认证、限流、参数、上游服务、连接或超时，向页面返回可读原因。
# 可对外返回的错误，不携带请求头、凭据或上游响应原文。

# 从 typing 引入 Any。
from typing import Any


# 定义 AgentError，把带状态码和可读提示的 Agent 异常需要的数据与操作放在同一个对象中。
class AgentError(Exception):
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 HTTP 状态码（status_code）、编码（code）、消息（message）、数据（data）。
    def __init__(
        # 继续传入或组合当前实例（self），完成上面尚未结束的表达式。
        self,
        # 填写HTTP 状态码（status_code）字段，值为int。
        status_code: int,
        # 填写编码（code）字段，值为str。
        code: str,
        # 填写消息（message）字段，值为str。
        message: str,
        # 填写数据（data）字段，值为表达式 Any | None = None 的结果。
        data: Any | None = None,
    # 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
    ) -> None:
        # 在创建实例时读取配置并准备后续要复用的成员。
        super().__init__(message)
        # 把HTTP 状态码（status_code）保存到 当前实例中的HTTP 状态码（self.status_code）。
        self.status_code = status_code
        # 把编码（code）保存到 当前实例中的编码（self.code）。
        self.code = code
        # 把消息（message）保存到 当前实例中的消息（self.message）。
        self.message = message
        # 把数据（data）保存到 当前实例中的数据（self.data）。
        self.data = data


# 定义 JavaApiError，把Java后端接口错误需要的数据与操作放在同一个对象中。
class JavaApiError(AgentError):
    # Java 认证、业务或通信失败；保留可安全识别的状态。
    pass
