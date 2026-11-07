"""Java 只读客户端：固定来源、逐请求认证、连接池与统一错误转换。"""

import json
import re
from dataclasses import dataclass
from typing import Any

import httpx
from pydantic import SecretStr

from app.core.config import Settings
from app.core.errors import JavaApiError

READ_PATHS = frozenset(
    {
        "me",
        "dashboard/overview",
        "dashboard/tasks",
        "tasks",
        "goals",
        "weekly-plans",
        "planning/pending-tasks",
        "executions",
        "executions/current",
        "executions/summary",
        "habits",
        "habits/today",
        "habit-categories",
        "statistics/summary",
        "statistics/trends",
        "statistics/timeline",
        "statistics/habits",
        "statistics/objects",
    }
)
# 带资源的只读路径只允许受限模板与纯数字 ID，不开放任意 URL 或查询方法。
# 规划提交是唯一允许的 Java 写调用；只允许固定路径，不接受模型或用户指定。
AGENT_WRITE_PATHS = frozenset({"agent/plans/commit"})
READ_PATH_TEMPLATES = (
    re.compile(r"goals/\d{1,19}"),
    re.compile(r"goals/\d{1,19}/stages"),
    re.compile(r"stages/\d{1,19}"),
    re.compile(r"tasks/\d{1,19}"),
    re.compile(r"tasks/\d{1,19}/assignment"),
    re.compile(r"weekly-plans/\d{1,19}"),
    re.compile(r"weekly-plans/\d{1,19}/tasks"),
    re.compile(r"weekly-plans/by-week/\d{4}-\d{2}-\d{2}"),
    re.compile(r"weekly-plans/\d{1,19}/daily-plans/\d{4}-\d{2}-\d{2}"),
    re.compile(r"daily-plans/\d{1,19}"),
    re.compile(r"daily-plans/\d{1,19}/tasks"),
    re.compile(r"executions/\d{1,19}"),
    re.compile(r"executions/\d{1,19}/segments"),
    re.compile(r"habits/\d{1,19}"),
    re.compile(r"habits/\d{1,19}/checkins"),
    re.compile(r"habits/\d{1,19}/revisions"),
)
SAFE_CODE = re.compile(r"[A-Z][A-Z0-9_:.\-]{0,79}")
_CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")


def upstream_message(envelope: dict, fallback: str, limit: int = 200) -> str:
    """优先透传 Java 业务异常的中文原因，让用户看到可操作信息；异常内容做长度与控制字符过滤。"""
    value = envelope.get("message")
    if not isinstance(value, str):
        return fallback
    value = " ".join(value.split())
    if not value or len(value) > limit or _CONTROL.search(value):
        return fallback
    return value


def check_read_path(path: str) -> None:
    """只读工具路径白名单：固定路径或已声明模板，不能拼接任意段。"""
    if path in READ_PATHS or any(
        pattern.fullmatch(path) for pattern in READ_PATH_TEMPLATES
    ):
        return
    raise JavaApiError(400, "JAVA_PATH_NOT_ALLOWED", "当前客户端只允许已实现的只读接口")



@dataclass(frozen=True)
class JavaApiResult:
    data: Any
    trace_id: str | None


class JavaApiClient:
    def __init__(self, settings: Settings) -> None:
        base = settings.java_api_base_url.rstrip("/")
        if not base.endswith("/asterway"):
            base += "/asterway"
        self._settings = settings
        # 认证头绝不写入共享客户端，避免并发请求串用用户身份。
        self._client = httpx.AsyncClient(
            base_url=base + "/",
            timeout=httpx.Timeout(settings.java_timeout_seconds, pool=5.0),
            limits=httpx.Limits(max_connections=32, max_keepalive_connections=16),
            follow_redirects=False,
            trust_env=False,
            headers={"Accept": "application/json", "User-Agent": "asterway-agent/0.1.0"},
        )

    async def close(self) -> None:
        await self._client.aclose()

    async def get(
        self,
        path: str,
        *,
        authorization: SecretStr,
        trace_id: str,
        params: dict[str, str | int | bool] | None = None,
    ) -> JavaApiResult:
        # 只读工具使用固定路径白名单，不能指定域名、任意路径或 HTTP 方法。
        check_read_path(path)
        headers = {
            "Authorization": authorization.get_secret_value(),
            "X-Trace-ID": trace_id,
        }
        try:
            async with self._client.stream("GET", path, params=params, headers=headers) as response:
                content = bytearray()
                async for chunk in response.aiter_bytes():
                    content.extend(chunk)
                    if len(content) > self._settings.java_max_response_bytes:
                        raise JavaApiError(
                            502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围"
                        )
                status = response.status_code
        except httpx.TimeoutException:
            raise JavaApiError(504, "JAVA_TIMEOUT", "业务服务响应超时，请稍后重试") from None
        except httpx.RequestError:
            raise JavaApiError(502, "JAVA_UNAVAILABLE", "暂时无法连接业务服务") from None

        try:
            envelope = json.loads(content)
        except (ValueError, UnicodeDecodeError, RecursionError):
            if status == 401:
                raise JavaApiError(401, "UNAUTHORIZED", "登录会话已失效，请重新登录") from None
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回了无法识别的数据") from None
        if not isinstance(envelope, dict):
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回结构不正确")
        code = envelope.get("code")
        if status < 200 or status >= 300 or code != "OK":
            mapped_status = status if status in {400, 401, 403, 404, 409, 422, 429} else 502
            if status == 200:
                mapped_status = {"UNAUTHORIZED": 401, "FORBIDDEN": 403}.get(str(code), 502)
            messages = {
                400: "业务查询参数不符合要求",
                401: "登录会话已失效，请重新登录",
                403: "当前账户没有该业务的访问权限",
                404: "没有找到可访问的业务记录",
                409: "业务状态已变化，请重新查询",
                422: "业务查询参数无法处理",
                429: "业务服务请求过于频繁，请稍后重试",
            }
            safe_code = (
                code
                if isinstance(code, str) and SAFE_CODE.fullmatch(code)
                else "JAVA_REQUEST_FAILED"
            )
            raise JavaApiError(
                mapped_status,
                safe_code,
                upstream_message(envelope, messages.get(mapped_status, "业务服务暂时不可用")),
            )
        if "data" not in envelope:
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回缺少结果字段")
        java_trace = envelope.get("traceId")
        if not isinstance(java_trace, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", java_trace):
            java_trace = None
        return JavaApiResult(data=envelope["data"], trace_id=java_trace)


    async def post_agent(
        self,
        path: str,
        *,
        authorization: SecretStr,
        trace_id: str,
        payload: dict[str, object],
    ) -> JavaApiResult:
        """受控的 Agent 写调用：只允许固定路径，超时与错误按业务规则映射。"""
        if path not in AGENT_WRITE_PATHS:
            raise JavaApiError(403, "AGENT_WRITE_FORBIDDEN", "不允许该写操作")
        headers = {
            "Authorization": authorization.get_secret_value(),
            "X-Trace-ID": trace_id,
            "Content-Type": "application/json",
        }
        try:
            async with self._client.stream(
                "POST", path, json=payload, headers=headers
            ) as response:
                content = bytearray()
                async for chunk in response.aiter_bytes():
                    content.extend(chunk)
                    if len(content) > self._settings.java_max_response_bytes:
                        raise JavaApiError(
                            502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围"
                        )
                status = response.status_code
        except httpx.TimeoutException:
            raise JavaApiError(504, "JAVA_TIMEOUT", "业务服务响应超时，请稍后重试") from None
        except httpx.RequestError:
            raise JavaApiError(502, "JAVA_UNAVAILABLE", "暂时无法连接业务服务") from None
        try:
            envelope = json.loads(content)
        except (ValueError, UnicodeDecodeError, RecursionError):
            if status == 401:
                raise JavaApiError(401, "UNAUTHORIZED", "登录会话已失效，请重新登录") from None
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回了无法识别的数据") from None
        if not isinstance(envelope, dict):
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回结构不正确")
        code = envelope.get("code")
        if status < 200 or status >= 300 or code != "OK":
            mapped_status = status if status in {400, 401, 403, 404, 409, 422, 429} else 502
            if status == 200:
                mapped_status = {"UNAUTHORIZED": 401, "FORBIDDEN": 403}.get(str(code), 502)
            messages = {
                400: "提交内容不符合要求",
                401: "登录会话已失效，请重新登录",
                403: "当前账户没有执行该操作的权限",
                404: "提交记录不存在",
                409: "方案状态已变化，请重新生成或刷新后重试",
                422: "提交内容无法处理",
                429: "业务服务请求过于频繁，请稍后重试",
            }
            safe_code = (
                code if isinstance(code, str) and SAFE_CODE.fullmatch(code) else "PLAN_COMMIT_FAILED"
            )
            raise JavaApiError(
                mapped_status,
                safe_code,
                upstream_message(envelope, messages.get(mapped_status, "规划提交失败")),
            )
        if "data" not in envelope:
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回缺少结果字段")
        java_trace = envelope.get("traceId")
        if not isinstance(java_trace, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", java_trace):
            java_trace = None
        return JavaApiResult(data=envelope["data"], trace_id=java_trace)
