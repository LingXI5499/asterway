# 通过白名单路径调用 Java 接口；用户身份与业务权限由 Java 再次核对。
# Java 只读客户端：固定来源、逐请求认证、连接池与统一错误转换。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 re，供下面调用其功能。
import re

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 typing 引入 Any。
from typing import Any

# 引入 HTTP 请求客户端库（httpx），供下面调用其功能。
import httpx

# 从 pydantic 引入 默认显示时隐藏实际内容的私密文字类型（SecretStr）。
from pydantic import SecretStr

# 从 app.core.config 引入 配置（Settings）。
from app.core.config import Settings

# 从 app.core.errors 引入 Java后端接口错误（JavaApiError）。
from app.core.errors import JavaApiError

# 把执行“调用 frozenset 完成frozenset处理”得到的结果保存到 读取PATHS（READ_PATHS）。
READ_PATHS = frozenset(
    {
        # 继续传入或组合固定文字 'me'，完成上面尚未结束的表达式。
        "me",
        # 继续传入或组合固定文字 'dashboard/overview'，完成上面尚未结束的表达式。
        "dashboard/overview",
        # 继续传入或组合固定文字 'dashboard/tasks'，完成上面尚未结束的表达式。
        "dashboard/tasks",
        # 继续传入或组合固定文字 'tasks'，完成上面尚未结束的表达式。
        "tasks",
        # 继续传入或组合固定文字 'goals'，完成上面尚未结束的表达式。
        "goals",
        # 继续传入或组合固定文字 'weekly-plans'，完成上面尚未结束的表达式。
        "weekly-plans",
        # 继续传入或组合固定文字 'planning/pending-tasks'，完成上面尚未结束的表达式。
        "planning/pending-tasks",
        # 继续传入或组合固定文字 'executions'，完成上面尚未结束的表达式。
        "executions",
        # 继续传入或组合固定文字 'executions/current'，完成上面尚未结束的表达式。
        "executions/current",
        # 继续传入或组合固定文字 'executions/summary'，完成上面尚未结束的表达式。
        "executions/summary",
        # 继续传入或组合固定文字 'habits'，完成上面尚未结束的表达式。
        "habits",
        # 继续传入或组合固定文字 'habits/today'，完成上面尚未结束的表达式。
        "habits/today",
        # 继续传入或组合固定文字 'habit-categories'，完成上面尚未结束的表达式。
        "habit-categories",
        # 继续传入或组合固定文字 'statistics/summary'，完成上面尚未结束的表达式。
        "statistics/summary",
        # 继续传入或组合固定文字 'statistics/trends'，完成上面尚未结束的表达式。
        "statistics/trends",
        # 继续传入或组合固定文字 'statistics/timeline'，完成上面尚未结束的表达式。
        "statistics/timeline",
        # 继续传入或组合固定文字 'statistics/habits'，完成上面尚未结束的表达式。
        "statistics/habits",
        # 继续传入或组合固定文字 'statistics/objects'，完成上面尚未结束的表达式。
        "statistics/objects",
    }
)
# 带资源的只读路径只允许受限模板与纯数字 ID，不开放任意 URL 或查询方法。
# 规划提交是唯一允许的 Java 写调用；只允许固定路径，不接受模型或用户指定。
# 把执行“调用 frozenset 完成frozenset处理”得到的结果保存到 成长助手写入PATHS（AGENT_WRITE_PATHS）。
AGENT_WRITE_PATHS = frozenset({"agent/plans/commit"})
# 把依次包含 执行“调用 re.compile 完成compile处理”得到的结果、执行“调用 re.compile 完成compile处理”得到的结果、执行“调用 re.compile 完成compile处理”得到的结果、执行“调用 re.compile 完成compile处理”得到的结果、执行“调用 re.compile 完成compile处理”得到的结果等内容 的集合保存到 读取路径TEMPLATES（READ_PATH_TEMPLATES）。
READ_PATH_TEMPLATES = (
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"goals/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"goals/\d{1,19}/stages"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"stages/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"tasks/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"tasks/\d{1,19}/assignment"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"weekly-plans/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"weekly-plans/\d{1,19}/tasks"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"weekly-plans/by-week/\d{4}-\d{2}-\d{2}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"weekly-plans/\d{1,19}/daily-plans/\d{4}-\d{2}-\d{2}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"daily-plans/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"daily-plans/\d{1,19}/tasks"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"executions/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"executions/\d{1,19}/segments"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"habits/\d{1,19}"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"habits/\d{1,19}/checkins"),
    # 继续传入或组合调用 re.compile 完成compile处理，完成上面尚未结束的表达式。
    re.compile(r"habits/\d{1,19}/revisions"),
)
# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 SAFE编码（SAFE_CODE）。
SAFE_CODE = re.compile(r"[A-Z][A-Z0-9_:.\-]{0,79}")
# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 CONTROL（_CONTROL）。
_CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")


# 函数 upstream_message：优先透传 Java 业务异常的中文原因，让用户看到可操作信息；异常内容做长度与控制字符过滤。；接收 envelope、备用值（fallback）、上限（limit）。
def upstream_message(envelope: dict, fallback: str, limit: int = 200) -> str:
    # 优先透传 Java 业务异常的中文原因，让用户看到可操作信息；异常内容做长度与控制字符过滤。
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 值（value）。
    value = envelope.get("message")
    # 检查条件 not isinstance(value, str) 是否成立；成立时执行缩进的分支。
    if not isinstance(value, str):
        # 把备用值（fallback）交还给调用方，本函数到此结束。
        return fallback
    # 把执行“用指定分隔符把多段文字合起来”得到的结果保存到 值（value）。
    value = " ".join(value.split())
    # 检查条件 not value or len(value) > limit or _CONTROL.search(value) 是否成立；成立时执行缩进的分支。
    if not value or len(value) > limit or _CONTROL.search(value):
        # 把备用值（fallback）交还给调用方，本函数到此结束。
        return fallback
    # 把值（value）交还给调用方，本函数到此结束。
    return value


# 函数 check_read_path：只读工具路径白名单：固定路径或已声明模板，不能拼接任意段。；接收 路径（path）。
def check_read_path(path: str) -> None:
    # 只读工具路径白名单：固定路径或已声明模板，不能拼接任意段。
    # 检查条件 path in READ_PATHS or any((pattern.fullmatch(path) for pattern in READ_PATH_TEMPLATES)) 是否成立；成立时执行缩进的分支。
    if path in READ_PATHS or any(
        # 继续传入或组合调用 pattern.fullmatch 完成fullmatch处理，完成上面尚未结束的表达式。
        pattern.fullmatch(path) for pattern in READ_PATH_TEMPLATES
    ):
        # 把空值交还给调用方，本函数到此结束。
        return
    # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
    raise JavaApiError(400, "JAVA_PATH_NOT_ALLOWED", "当前客户端只允许已实现的只读接口")



# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 JavaApiResult，把Java后端接口结果需要的数据与操作放在同一个对象中。
class JavaApiResult:
    # 声明 数据（data），类型为 Any。
    data: Any
    # 声明 追踪标识编号（trace_id），类型为 str | None。
    trace_id: str | None


# 定义 JavaApiClient，把Java后端接口客户端需要的数据与操作放在同一个对象中。
class JavaApiClient:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 配置（settings）。
    def __init__(self, settings: Settings) -> None:
        # 把执行“调用 settings.java_api_base_url.rstrip 完成rstrip处理”得到的结果保存到 基础（base）。
        base = settings.java_api_base_url.rstrip("/")
        # 检查条件 not base.endswith('/asterway') 是否成立；成立时执行缩进的分支。
        if not base.endswith("/asterway"):
            # 把 base 加上 固定文字 '/asterway'，再保存回原变量。
            base += "/asterway"
        # 把配置（settings）保存到 当前实例中的配置（self._settings）。
        self._settings = settings
        # 认证头绝不写入共享客户端，避免并发请求串用用户身份。
        # 把执行“调用 httpx.AsyncClient 完成异步客户端处理”得到的结果保存到 当前实例中的客户端（self._client）。
        self._client = httpx.AsyncClient(
            # 传入基础地址（base_url），值为按表达式计算得到的值：base + "/"。
            base_url=base + "/",
            # 传入超时（timeout），值为调用 httpx.Timeout 完成超时处理。
            timeout=httpx.Timeout(settings.java_timeout_seconds, pool=5.0),
            # 传入limits，值为调用 httpx.Limits 完成Limits处理。
            limits=httpx.Limits(max_connections=32, max_keepalive_connections=16),
            # 传入followredirects（follow_redirects），值为假，表示条件不成立或开关关闭。
            follow_redirects=False,
            # 传入trustenv（trust_env），值为假，表示条件不成立或开关关闭。
            trust_env=False,
            # 传入请求头集合（headers），值为按表达式计算得到的值：{"Accept": "application/json", "User-Agent": "asterway-agent/0.1.0"}。
            headers={"Accept": "application/json", "User-Agent": "asterway-agent/0.1.0"},
        )

    # 异步函数 close：释放连接、文件或客户端占用的资源。
    async def close(self) -> None:
        # 等待调用 self._client.aclose 完成aclose处理完成后再继续。
        await self._client.aclose()

    # 异步函数 get：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；接收 路径（path）、授权（authorization）、追踪标识编号（trace_id）、参数集合（params）。
    async def get(
        # 继续传入或组合当前实例（self），完成上面尚未结束的表达式。
        self,
        # 填写路径（path）字段，值为str。
        path: str,
        # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
        *,
        # 填写授权（authorization）字段，值为默认显示时隐藏实际内容的私密文字类型（SecretStr）。
        authorization: SecretStr,
        # 填写追踪标识编号（trace_id）字段，值为str。
        trace_id: str,
        # 填写参数集合（params）字段，值为表达式 dict[str, str | int | bool] | None = None 的结果。
        params: dict[str, str | int | bool] | None = None,
    # 继续传入或组合表达式 ) -> JavaApiResult: 的结果，完成上面尚未结束的表达式。
    ) -> JavaApiResult:
        # 只读工具使用固定路径白名单，不能指定域名、任意路径或 HTTP 方法。
        # 核对 读取路径（read_path）。
        check_read_path(path)
        # 把按字段名保存多个值的字典保存到 请求头集合（headers）。
        headers = {
            # 填写授权（Authorization）字段，值为仅在确实需要连接服务时取出私密配置中的实际值。
            "Authorization": authorization.get_secret_value(),
            # 继续传入或组合表达式 "X-Trace-ID": trace_id 的结果，完成上面尚未结束的表达式。
            "X-Trace-ID": trace_id,
        }
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 异步进入执行“调用 self._client.stream 完成分段回复流处理”得到的结果，命名为 response；退出时由上下文管理器提交、回滚或释放资源。
            async with self._client.stream("GET", path, params=params, headers=headers) as response:
                # 把执行“调用 bytearray 完成bytearray处理”得到的结果保存到 内容（content）。
                content = bytearray()
                # 从执行“调用 response.aiter_bytes 完成aiterbytes处理”得到的结果依次取出文档片段（chunk），每项执行一次下面的处理。
                async for chunk in response.aiter_bytes():
                    # 把另一组条目依次加入列表。
                    content.extend(chunk)
                    # 检查条件 len(content) > self._settings.java_max_response_bytes 是否成立；成立时执行缩进的分支。
                    if len(content) > self._settings.java_max_response_bytes:
                        # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
                        raise JavaApiError(
                            # 继续传入或组合表达式 502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围" 的结果，完成上面尚未结束的表达式。
                            502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围"
                        )
                # 把响应（response）的HTTP 状态码（status_code）属性保存到 状态（status）。
                status = response.status_code
        # 捕获httpx.TimeoutException异常，执行下面的错误处理。
        except httpx.TimeoutException:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(504, "JAVA_TIMEOUT", "业务服务响应超时，请稍后重试") from None
        # 捕获httpx.RequestError异常，执行下面的错误处理。
        except httpx.RequestError:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_UNAVAILABLE", "暂时无法连接业务服务") from None

        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把执行“把 JSON 文字解析为对象”得到的结果保存到 envelope。
            envelope = json.loads(content)
        # 捕获(ValueError, UnicodeDecodeError, RecursionError)异常，执行下面的错误处理。
        except (ValueError, UnicodeDecodeError, RecursionError):
            # 检查条件 status == 401 是否成立；成立时执行缩进的分支。
            if status == 401:
                # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
                raise JavaApiError(401, "UNAUTHORIZED", "登录会话已失效，请重新登录") from None
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回了无法识别的数据") from None
        # 检查条件 not isinstance(envelope, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(envelope, dict):
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回结构不正确")
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 编码（code）。
        code = envelope.get("code")
        # 检查条件 status < 200 or status >= 300 or code != 'OK' 是否成立；成立时执行缩进的分支。
        if status < 200 or status >= 300 or code != "OK":
            # 把条件 status in {400, 401, 403, 404, 409, 422, 429} 是否成立时使用状态（status），否则使用固定数值 502保存到 mapped状态（mapped_status）。
            mapped_status = status if status in {400, 401, 403, 404, 409, 422, 429} else 502
            # 检查条件 status == 200 是否成立；成立时执行缩进的分支。
            if status == 200:
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 mapped状态（mapped_status）。
                mapped_status = {"UNAUTHORIZED": 401, "FORBIDDEN": 403}.get(str(code), 502)
            # 把按字段名保存多个值的字典保存到 消息列表（messages）。
            messages = {
                # 填写400字段，值为固定文字 '业务查询参数不符合要求'。
                400: "业务查询参数不符合要求",
                # 填写401字段，值为固定文字 '登录会话已失效，请重新登录'。
                401: "登录会话已失效，请重新登录",
                # 填写403字段，值为固定文字 '当前账户没有该业务的访问权限'。
                403: "当前账户没有该业务的访问权限",
                # 填写404字段，值为固定文字 '没有找到可访问的业务记录'。
                404: "没有找到可访问的业务记录",
                # 填写409字段，值为固定文字 '业务状态已变化，请重新查询'。
                409: "业务状态已变化，请重新查询",
                # 填写422字段，值为固定文字 '业务查询参数无法处理'。
                422: "业务查询参数无法处理",
                # 填写429字段，值为固定文字 '业务服务请求过于频繁，请稍后重试'。
                429: "业务服务请求过于频繁，请稍后重试",
            }
            # 把条件 isinstance(code, str) and SAFE_CODE.fullmatch(code) 是否成立时使用编码（code），否则使用固定状态或编码 JAVA_REQUEST_FAILED（Java后端请求失败）保存到 safe编码（safe_code）。
            safe_code = (
                # 继续传入或组合编码（code），完成上面尚未结束的表达式。
                code
                # 继续传入或组合调用 if isinstance(code, str) and SAFE_CODE.fullmatch 完成fullmatch处理，完成上面尚未结束的表达式。
                if isinstance(code, str) and SAFE_CODE.fullmatch(code)
                # 继续传入或组合表达式 else "JAVA_REQUEST_FAILED" 的结果，完成上面尚未结束的表达式。
                else "JAVA_REQUEST_FAILED"
            )
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(
                # 继续传入或组合mapped状态（mapped_status），完成上面尚未结束的表达式。
                mapped_status,
                # 继续传入或组合safe编码（safe_code），完成上面尚未结束的表达式。
                safe_code,
                # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                upstream_message(envelope, messages.get(mapped_status, "业务服务暂时不可用")),
            )
        # 检查条件 'data' not in envelope 是否成立；成立时执行缩进的分支。
        if "data" not in envelope:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回缺少结果字段")
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 Java后端追踪标识（java_trace）。
        java_trace = envelope.get("traceId")
        # 检查条件 not isinstance(java_trace, str) or not re.fullmatch('[A-Za-z0-9_-]{1,64}', java_trace) 是否成立；成立时执行缩进的分支。
        if not isinstance(java_trace, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", java_trace):
            # 把空值，表示当前还没有对应内容保存到 Java后端追踪标识（java_trace）。
            java_trace = None
        # 把执行“调用 JavaApiResult 完成Java后端接口结果处理”得到的结果交还给调用方，本函数到此结束。
        return JavaApiResult(data=envelope["data"], trace_id=java_trace)


    # 异步函数 post_agent：受控的 Agent 写调用：只允许固定路径，超时与错误按业务规则映射。；接收 路径（path）、授权（authorization）、追踪标识编号（trace_id）、提交数据（payload）。
    async def post_agent(
        # 继续传入或组合当前实例（self），完成上面尚未结束的表达式。
        self,
        # 填写路径（path）字段，值为str。
        path: str,
        # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
        *,
        # 填写授权（authorization）字段，值为默认显示时隐藏实际内容的私密文字类型（SecretStr）。
        authorization: SecretStr,
        # 填写追踪标识编号（trace_id）字段，值为str。
        trace_id: str,
        # 填写提交数据（payload）字段，值为表达式 dict[str, object] 的结果。
        payload: dict[str, object],
    # 继续传入或组合表达式 ) -> JavaApiResult: 的结果，完成上面尚未结束的表达式。
    ) -> JavaApiResult:
        # 受控的 Agent 写调用：只允许固定路径，超时与错误按业务规则映射。
        # 检查条件 path not in AGENT_WRITE_PATHS 是否成立；成立时执行缩进的分支。
        if path not in AGENT_WRITE_PATHS:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(403, "AGENT_WRITE_FORBIDDEN", "不允许该写操作")
        # 把按字段名保存多个值的字典保存到 请求头集合（headers）。
        headers = {
            # 填写授权（Authorization）字段，值为仅在确实需要连接服务时取出私密配置中的实际值。
            "Authorization": authorization.get_secret_value(),
            # 继续传入或组合表达式 "X-Trace-ID": trace_id 的结果，完成上面尚未结束的表达式。
            "X-Trace-ID": trace_id,
            # 继续传入或组合固定文字 'Content-Type": "application/json'，完成上面尚未结束的表达式。
            "Content-Type": "application/json",
        }
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 异步进入执行“调用 self._client.stream 完成分段回复流处理”得到的结果，命名为 response；退出时由上下文管理器提交、回滚或释放资源。
            async with self._client.stream(
                # 继续传入或组合表达式 "POST", path, json=payload, headers=headers 的结果，完成上面尚未结束的表达式。
                "POST", path, json=payload, headers=headers
            # 继续传入或组合表达式 ) as response: 的结果，完成上面尚未结束的表达式。
            ) as response:
                # 把执行“调用 bytearray 完成bytearray处理”得到的结果保存到 内容（content）。
                content = bytearray()
                # 从执行“调用 response.aiter_bytes 完成aiterbytes处理”得到的结果依次取出文档片段（chunk），每项执行一次下面的处理。
                async for chunk in response.aiter_bytes():
                    # 把另一组条目依次加入列表。
                    content.extend(chunk)
                    # 检查条件 len(content) > self._settings.java_max_response_bytes 是否成立；成立时执行缩进的分支。
                    if len(content) > self._settings.java_max_response_bytes:
                        # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
                        raise JavaApiError(
                            # 继续传入或组合表达式 502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围" 的结果，完成上面尚未结束的表达式。
                            502, "JAVA_RESULT_TOO_LARGE", "业务返回数据过多，请缩小查询范围"
                        )
                # 把响应（response）的HTTP 状态码（status_code）属性保存到 状态（status）。
                status = response.status_code
        # 捕获httpx.TimeoutException异常，执行下面的错误处理。
        except httpx.TimeoutException:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(504, "JAVA_TIMEOUT", "业务服务响应超时，请稍后重试") from None
        # 捕获httpx.RequestError异常，执行下面的错误处理。
        except httpx.RequestError:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_UNAVAILABLE", "暂时无法连接业务服务") from None
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把执行“把 JSON 文字解析为对象”得到的结果保存到 envelope。
            envelope = json.loads(content)
        # 捕获(ValueError, UnicodeDecodeError, RecursionError)异常，执行下面的错误处理。
        except (ValueError, UnicodeDecodeError, RecursionError):
            # 检查条件 status == 401 是否成立；成立时执行缩进的分支。
            if status == 401:
                # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
                raise JavaApiError(401, "UNAUTHORIZED", "登录会话已失效，请重新登录") from None
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回了无法识别的数据") from None
        # 检查条件 not isinstance(envelope, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(envelope, dict):
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回结构不正确")
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 编码（code）。
        code = envelope.get("code")
        # 检查条件 status < 200 or status >= 300 or code != 'OK' 是否成立；成立时执行缩进的分支。
        if status < 200 or status >= 300 or code != "OK":
            # 把条件 status in {400, 401, 403, 404, 409, 422, 429} 是否成立时使用状态（status），否则使用固定数值 502保存到 mapped状态（mapped_status）。
            mapped_status = status if status in {400, 401, 403, 404, 409, 422, 429} else 502
            # 检查条件 status == 200 是否成立；成立时执行缩进的分支。
            if status == 200:
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 mapped状态（mapped_status）。
                mapped_status = {"UNAUTHORIZED": 401, "FORBIDDEN": 403}.get(str(code), 502)
            # 把按字段名保存多个值的字典保存到 消息列表（messages）。
            messages = {
                # 填写400字段，值为固定文字 '提交内容不符合要求'。
                400: "提交内容不符合要求",
                # 填写401字段，值为固定文字 '登录会话已失效，请重新登录'。
                401: "登录会话已失效，请重新登录",
                # 填写403字段，值为固定文字 '当前账户没有执行该操作的权限'。
                403: "当前账户没有执行该操作的权限",
                # 填写404字段，值为固定文字 '提交记录不存在'。
                404: "提交记录不存在",
                # 填写409字段，值为固定文字 '方案状态已变化，请重新生成或刷新后重试'。
                409: "方案状态已变化，请重新生成或刷新后重试",
                # 填写422字段，值为固定文字 '提交内容无法处理'。
                422: "提交内容无法处理",
                # 填写429字段，值为固定文字 '业务服务请求过于频繁，请稍后重试'。
                429: "业务服务请求过于频繁，请稍后重试",
            }
            # 把条件 isinstance(code, str) and SAFE_CODE.fullmatch(code) 是否成立时使用编码（code），否则使用固定状态或编码 PLAN_COMMIT_FAILED（方案提交回执失败）保存到 safe编码（safe_code）。
            safe_code = (
                # 继续传入或组合调用 code if isinstance(code, str) and SAFE_CODE.fullmatch 完成fullmatch处理，完成上面尚未结束的表达式。
                code if isinstance(code, str) and SAFE_CODE.fullmatch(code) else "PLAN_COMMIT_FAILED"
            )
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(
                # 继续传入或组合mapped状态（mapped_status），完成上面尚未结束的表达式。
                mapped_status,
                # 继续传入或组合safe编码（safe_code），完成上面尚未结束的表达式。
                safe_code,
                # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                upstream_message(envelope, messages.get(mapped_status, "规划提交失败")),
            )
        # 检查条件 'data' not in envelope 是否成立；成立时执行缩进的分支。
        if "data" not in envelope:
            # 中止本轮正常处理，抛出 执行“调用 JavaApiError 完成Java后端接口错误处理”得到的结果。
            raise JavaApiError(502, "JAVA_INVALID_RESULT", "业务服务返回缺少结果字段")
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 Java后端追踪标识（java_trace）。
        java_trace = envelope.get("traceId")
        # 检查条件 not isinstance(java_trace, str) or not re.fullmatch('[A-Za-z0-9_-]{1,64}', java_trace) 是否成立；成立时执行缩进的分支。
        if not isinstance(java_trace, str) or not re.fullmatch(r"[A-Za-z0-9_-]{1,64}", java_trace):
            # 把空值，表示当前还没有对应内容保存到 Java后端追踪标识（java_trace）。
            java_trace = None
        # 把执行“调用 JavaApiResult 完成Java后端接口结果处理”得到的结果交还给调用方，本函数到此结束。
        return JavaApiResult(data=envelope["data"], trace_id=java_trace)
