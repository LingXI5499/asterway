"""从 Java 认证结果建立请求身份，不解码并信任客户端自报的用户信息。"""

import asyncio
import re
import secrets
import time
from dataclasses import dataclass, field
from typing import Annotated

from fastapi import Depends, Request, Security
from fastapi.security import APIKeyHeader, HTTPAuthorizationCredentials, HTTPBearer
from pydantic import SecretStr

from app.core.config import get_settings
from app.core.errors import AgentError, JavaApiError
from app.integrations.java_client import JavaApiClient

bearer = HTTPBearer(auto_error=False, scheme_name="AsterwayJWT")
internal_key = APIKeyHeader(
    name="X-Asterway-Agent-Key", auto_error=False, scheme_name="AgentInternalKey"
)


@dataclass(frozen=True)
class AgentContext:
    user_id: str
    roles: frozenset[str]
    permissions: frozenset[str]
    authorization: SecretStr = field(repr=False)
    trace_id: str
    deadline: float

    def allows(self, permissions: frozenset[str]) -> bool:
        return permissions.issubset(self.permissions)

    def require(self, *permissions: str) -> None:
        """接口级权限检查；身份已由 Java 确认，这里复核有效权限集。"""
        if any(code not in self.permissions for code in permissions):
            raise AgentError(403, "FORBIDDEN", "当前账户没有该操作的权限")


def get_java_client(request: Request) -> JavaApiClient:
    return request.app.state.java_client


async def authenticate(
    client: JavaApiClient,
    authorization: SecretStr,
    trace_id: str,
    deadline: float,
) -> AgentContext:
    result = await client.get("me", authorization=authorization, trace_id=trace_id)
    data = result.data
    if not isinstance(data, dict) or not isinstance(data.get("user"), dict):
        raise JavaApiError(502, "JAVA_IDENTITY_INVALID", "业务服务返回的身份结构不正确")
    user = data["user"]
    user_id, roles, permissions = user.get("id"), data.get("roles"), data.get("permissions")
    if (
        not isinstance(user_id, str)
        or not re.fullmatch(r"[1-9][0-9]{0,18}", user_id)
        or int(user_id) > 9_223_372_036_854_775_807
        or not isinstance(roles, list)
        or not all(isinstance(value, str) for value in roles)
        or not isinstance(permissions, list)
        or not all(isinstance(value, str) for value in permissions)
    ):
        raise JavaApiError(502, "JAVA_IDENTITY_INVALID", "业务服务返回的身份字段不完整")
    if user.get("enabled") is not True:
        raise AgentError(403, "ACCOUNT_DISABLED", "当前账户不可用")
    if user.get("mustChangePassword") is not False:
        raise AgentError(403, "PASSWORD_CHANGE_REQUIRED", "请先完成初始密码修改")
    if "USER" not in roles or {"ADMIN", "SUPER_ADMIN"}.intersection(roles):
        raise AgentError(403, "PERSONAL_AGENT_FORBIDDEN", "管理身份不开放个人成长助手")
    return AgentContext(
        user_id=user_id,
        roles=frozenset(roles),
        permissions=frozenset(permissions),
        authorization=authorization,
        trace_id=trace_id,
        deadline=deadline,
    )


async def refresh_context(context: AgentContext, client: JavaApiClient) -> AgentContext:
    current = await authenticate(client, context.authorization, context.trace_id, context.deadline)
    if current.user_id != context.user_id:
        raise AgentError(401, "IDENTITY_CHANGED", "登录身份已变化，请重新发起请求")
    return current


async def get_agent_context(
    request: Request,
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)],
    service_key: Annotated[str | None, Security(internal_key)],
    client: Annotated[JavaApiClient, Depends(get_java_client)],
) -> AgentContext:
    settings = get_settings()
    expected = settings.agent_internal_key.get_secret_value()
    if not expected:
        raise AgentError(503, "AGENT_INTERNAL_NOT_CONFIGURED", "内部调用认证尚未配置")
    if (
        not service_key
        or len(service_key) > 512
        or not secrets.compare_digest(service_key.encode("utf-8"), expected.encode("utf-8"))
    ):
        raise AgentError(401, "AGENT_INTERNAL_UNAUTHORIZED", "内部调用认证失败")
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise AgentError(401, "UNAUTHORIZED", "请使用 Asterway 登录后取得的 Bearer JWT")
    token = credentials.credentials
    if not token or len(token) > 8192 or not re.fullmatch(r"[A-Za-z0-9._~\-]+", token):
        raise AgentError(401, "UNAUTHORIZED", "登录凭据格式不正确")
    deadline = time.monotonic() + settings.agent_request_timeout_seconds
    try:
        async with asyncio.timeout_at(deadline):
            context = await authenticate(
                client, SecretStr("Bearer " + token), request.state.trace_id, deadline
            )
            from app.services.observation_service import Observation, current_observation
            if getattr(request.app.state, "database", None) is not None:
                current_observation.set(Observation(request.app.state.database, int(context.user_id), context.trace_id))
            return context
    except TimeoutError:
        raise AgentError(504, "AGENT_TIMEOUT", "身份校验超时，请稍后重试") from None





