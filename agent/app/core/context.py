# 核验 Java 转发的内部密钥和当前用户，建立本次请求的身份与权限上下文。
# 从 Java 认证结果建立请求身份，不解码并信任客户端自报的用户信息。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 re，供下面调用其功能。
import re

# 引入 安全随机值（secrets），供下面调用其功能。
import secrets

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）、字段（field）。
from dataclasses import dataclass, field

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）。
from typing import Annotated

# 从 fastapi 引入 由 FastAPI 提供参数和前置校验的依赖声明（Depends）、本次 HTTP 请求（Request）、Security。
from fastapi import Depends, Request, Security

# 从 fastapi.security 引入 接口键请求头（APIKeyHeader）、HTTP请求授权Credentials（HTTPAuthorizationCredentials）、HTTP请求Bearer（HTTPBearer）。
from fastapi.security import APIKeyHeader, HTTPAuthorizationCredentials, HTTPBearer

# 从 pydantic 引入 默认显示时隐藏实际内容的私密文字类型（SecretStr）。
from pydantic import SecretStr

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）、Java后端接口错误（JavaApiError）。
from app.core.errors import AgentError, JavaApiError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 把执行“调用 HTTPBearer 完成HTTP请求Bearer处理”得到的结果保存到 bearer。
bearer = HTTPBearer(auto_error=False, scheme_name="AsterwayJWT")
# 把执行“调用 APIKeyHeader 完成接口键请求头处理”得到的结果保存到 内部键（internal_key）。
internal_key = APIKeyHeader(
    # 传入名称（name），值为固定文字 'X-Asterway-Agent-Key", auto_error=False, scheme_name="AgentInternalKey'。
    name="X-Asterway-Agent-Key", auto_error=False, scheme_name="AgentInternalKey"
)


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 AgentContext，把成长助手上下文需要的数据与操作放在同一个对象中。
class AgentContext:
    # 声明 用户编号（user_id），类型为 str。
    user_id: str
    # 声明 角色列表（roles），类型为 frozenset[str]。
    roles: frozenset[str]
    # 声明 权限列表（permissions），类型为 frozenset[str]。
    permissions: frozenset[str]
    # 声明 授权（authorization），类型为 SecretStr；其内容为执行“声明数据类字段的默认值或创建规则，避免实例之间共用同一个可变对象”得到的结果。
    authorization: SecretStr = field(repr=False)
    # 声明 追踪标识编号（trace_id），类型为 str。
    trace_id: str
    # 声明 截止时间（deadline），类型为 float。
    deadline: float

    # 函数 allows：调用 allows 完成allows处理；接收 权限列表（permissions）。
    def allows(self, permissions: frozenset[str]) -> bool:
        # 把执行“判断是否为 subset”得到的结果交还给调用方，本函数到此结束。
        return permissions.issubset(self.permissions)

    # 函数 require：接口级权限检查；身份已由 Java 确认，这里复核有效权限集。
    def require(self, *permissions: str) -> None:
        # 接口级权限检查；身份已由 Java 确认，这里复核有效权限集。
        # 检查当前用户是否具有需要的权限：any((code not in self.permissions for code in permissions))；成立时执行缩进的分支。
        if any(code not in self.permissions for code in permissions):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(403, "FORBIDDEN", "当前账户没有该操作的权限")


# 函数 get_java_client：读取 Java后端客户端（_java_client） 的值；接收 请求（request）。
def get_java_client(request: Request) -> JavaApiClient:
    # 把请求（request）的应用（app）属性的当前状态（state）属性的Java后端客户端（java_client）属性交还给调用方，本函数到此结束。
    return request.app.state.java_client


# 异步函数 authenticate：调用 authenticate 完成authenticate处理；接收 客户端（client）、授权（authorization）、追踪标识编号（trace_id）、截止时间（deadline）。
async def authenticate(
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写授权（authorization）字段，值为默认显示时隐藏实际内容的私密文字类型（SecretStr）。
    authorization: SecretStr,
    # 填写追踪标识编号（trace_id）字段，值为str。
    trace_id: str,
    # 填写截止时间（deadline）字段，值为浮点小数（float）。
    deadline: float,
# 继续传入或组合表达式 ) -> AgentContext: 的结果，完成上面尚未结束的表达式。
) -> AgentContext:
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果（result）。
    result = await client.get("me", authorization=authorization, trace_id=trace_id)
    # 把结果（result）的数据（data）属性保存到 数据（data）。
    data = result.data
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('user'), dict) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("user"), dict):
        # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
        raise JavaApiError(502, "JAVA_IDENTITY_INVALID", "业务服务返回的身份结构不正确")
    # 把从数据（data）中按固定文字 'user'取出的内容保存到 用户（user）。
    user = data["user"]
    # 把依次包含 执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果、执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果、执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果 的集合保存到 用户编号角色列表权限列表（(user_id, roles, permissions)）。
    user_id, roles, permissions = user.get("id"), data.get("roles"), data.get("permissions")
    # 检查条件 not isinstance(user_id, str) or not re.fullmatch('[1-9][0-9]{0,18}', user_id) or int(user_id) > 9223372036854775807 or (not isinstance(roles, lis… 是否成立；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 not isinstance(user_id, str) 的结果，完成上面尚未结束的表达式。
        not isinstance(user_id, str)
        # 继续补充本次调用或判断：调用 or not re.fullmatch 完成fullmatch处理。
        or not re.fullmatch(r"[1-9][0-9]{0,18}", user_id)
        # 继续补充本次调用或判断：条件判断的结果：or int(user_id) > 9_223_372_036_854_775_807。
        or int(user_id) > 9_223_372_036_854_775_807
        # 继续补充本次调用或判断：条件判断的结果：or not isinstance(roles, list)。
        or not isinstance(roles, list)
        # 继续补充本次调用或判断：条件判断的结果：or not all(isinstance(value, str) for value in roles)。
        or not all(isinstance(value, str) for value in roles)
        # 继续补充本次调用或判断：条件判断的结果：or not isinstance(permissions, list)。
        or not isinstance(permissions, list)
        # 继续补充本次调用或判断：条件判断的结果：or not all(isinstance(value, str) for value in permissions)。
        or not all(isinstance(value, str) for value in permissions)
    ):
        # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
        raise JavaApiError(502, "JAVA_IDENTITY_INVALID", "业务服务返回的身份字段不完整")
    # 检查条件 user.get('enabled') is not True 是否成立；成立时执行缩进的分支。
    if user.get("enabled") is not True:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(403, "ACCOUNT_DISABLED", "当前账户不可用")
    # 检查条件 user.get('mustChangePassword') is not False 是否成立；成立时执行缩进的分支。
    if user.get("mustChangePassword") is not False:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(403, "PASSWORD_CHANGE_REQUIRED", "请先完成初始密码修改")
    # 检查条件 'USER' not in roles or {'ADMIN', 'SUPER_ADMIN'}.intersection(roles) 是否成立；成立时执行缩进的分支。
    if "USER" not in roles or {"ADMIN", "SUPER_ADMIN"}.intersection(roles):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(403, "PERSONAL_AGENT_FORBIDDEN", "管理身份不开放个人成长助手")
    # 把执行“调用 AgentContext 完成成长助手上下文处理”得到的结果交还给调用方，本函数到此结束。
    return AgentContext(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入角色列表（roles），值为调用 frozenset 完成frozenset处理。
        roles=frozenset(roles),
        # 传入权限列表（permissions），值为调用 frozenset 完成frozenset处理。
        permissions=frozenset(permissions),
        # 传入授权（authorization），值为授权（authorization）。
        authorization=authorization,
        # 传入追踪标识编号（trace_id），值为追踪标识编号（trace_id）。
        trace_id=trace_id,
        # 传入截止时间（deadline），值为截止时间（deadline）。
        deadline=deadline,
    )


# 异步函数 refresh_context：刷新 上下文（context）；接收 上下文（context）、客户端（client）。
async def refresh_context(context: AgentContext, client: JavaApiClient) -> AgentContext:
    # 把等待执行“调用 authenticate 完成authenticate处理”得到的结果完成后的结果保存到 当前（current）。
    current = await authenticate(client, context.authorization, context.trace_id, context.deadline)
    # 检查条件 current.user_id != context.user_id 是否成立；成立时执行缩进的分支。
    if current.user_id != context.user_id:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(401, "IDENTITY_CHANGED", "登录身份已变化，请重新发起请求")
    # 把当前（current）交还给调用方，本函数到此结束。
    return current


# 异步函数 get_agent_context：读取 成长助手上下文（_agent_context） 的值；接收 请求（request）、credentials、业务服务键（service_key）、客户端（client）。
async def get_agent_context(
    # 填写请求（request）字段，值为本次 HTTP 请求（Request）。
    request: Request,
    # 填写credentials字段，值为表达式 Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)] 的结果。
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
    # 填写业务服务键（service_key）字段，值为表达式 Annotated[str | None, Security(internal_key)] 的结果。
    service_key: Annotated[str | None, Security(internal_key)],
    # 填写客户端（client）字段，值为表达式 Annotated[JavaApiClient, Depends(get_java_client)] 的结果。
    client: Annotated[JavaApiClient, Depends(get_java_client)],
# 继续传入或组合表达式 ) -> AgentContext: 的结果，完成上面尚未结束的表达式。
) -> AgentContext:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把执行“只在连接或签名时取出密钥实际值”得到的结果保存到 预计（expected）。
    expected = settings.agent_internal_key.get_secret_value()
    # 检查条件 not expected 是否成立；成立时执行缩进的分支。
    if not expected:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "AGENT_INTERNAL_NOT_CONFIGURED", "内部调用认证尚未配置")
    # 检查条件 not service_key or len(service_key) > 512 or (not secrets.compare_digest(service_key.encode('utf-8'), expected.encode('utf-8'))) 是否成立；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 not service_key 的结果，完成上面尚未结束的表达式。
        not service_key
        # 继续补充本次调用或判断：条件判断的结果：or len(service_key) > 512。
        or len(service_key) > 512
        # 继续补充本次调用或判断：调用 or not secrets.compare_digest 完成comparedigest处理。
        or not secrets.compare_digest(service_key.encode("utf-8"), expected.encode("utf-8"))
    ):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(401, "AGENT_INTERNAL_UNAUTHORIZED", "内部调用认证失败")
    # 检查credentials is None or credentials.scheme.lower() != 'bearer'，即对应内容是否不存在；成立时执行缩进的分支。
    if credentials is None or credentials.scheme.lower() != "bearer":
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(401, "UNAUTHORIZED", "请使用 Asterway 登录后取得的 Bearer JWT")
    # 把credentials的credentials属性保存到 身份凭据（token）。
    token = credentials.credentials
    # 检查条件 not token or len(token) > 8192 or (not re.fullmatch('[A-Za-z0-9._~\\-]+', token)) 是否成立；成立时执行缩进的分支。
    if not token or len(token) > 8192 or not re.fullmatch(r"[A-Za-z0-9._~\-]+", token):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(401, "UNAUTHORIZED", "登录凭据格式不正确")
    # 把按 time.monotonic() + settings.agent_request_timeout_seconds 计算的结果保存到 截止时间（deadline）。
    deadline = time.monotonic() + settings.agent_request_timeout_seconds
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 asyncio.timeout_at 完成超时at处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        async with asyncio.timeout_at(deadline):
            # 把等待执行“调用 authenticate 完成authenticate处理”得到的结果完成后的结果保存到 上下文（context）。
            context = await authenticate(
                # 继续传入或组合按表达式计算得到的值：client, SecretStr("Bearer " + token), request.state.trace_id, deadline，完成上面尚未结束的表达式。
                client, SecretStr("Bearer " + token), request.state.trace_id, deadline
            )

            # 从 app.services.observation_service 引入 调用观测（Observation）、当前调用观测（current_observation）。
            from app.services.observation_service import Observation, current_observation
            # 检查条件 getattr(request.app.state, 'database', None) is not None 是否成立；成立时执行缩进的分支。
            if getattr(request.app.state, "database", None) is not None:
                # 调用 current_observation.set 完成设置处理。
                current_observation.set(Observation(request.app.state.database, int(context.user_id), context.trace_id))
            # 把上下文（context）交还给调用方，本函数到此结束。
            return context
    # 捕获TimeoutError异常，执行下面的错误处理。
    except TimeoutError:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(504, "AGENT_TIMEOUT", "身份校验超时，请稍后重试") from None
