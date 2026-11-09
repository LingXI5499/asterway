# 记录模型与工具的用量、耗时和失败类型，不记录密钥或私密正文。
# 调用观测仅保存身份范围、用量和耗时，永不保存正文或认证资料。

# 引入 logging，供下面调用其功能。
import logging

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 contextvars 引入 上下文Var（ContextVar）。
from contextvars import ContextVar

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 typing 引入 Any。
from typing import Any

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.workspace_models 引入 调用记录（CallRecord）。
from app.db.workspace_models import CallRecord

# 从 app.llm.errors 引入 classify模型错误（classify_model_error）。
from app.llm.errors import classify_model_error

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 Observation，把调用观测需要的数据与操作放在同一个对象中。
class Observation:
    # 声明 Agent 数据库访问器（db），类型为 Database。
    db: Database
    # 声明 用户编号（user_id），类型为 int。
    user_id: int
    # 声明 追踪标识编号（trace_id），类型为 str。
    trace_id: str


# 声明 当前调用观测（current_observation），类型为 ContextVar[Observation | None]；其内容为执行“调用 ContextVar 完成上下文Var处理”得到的结果。
current_observation: ContextVar[Observation | None] = ContextVar("agent_observation", default=None)


# 异步函数 record：调用 record 完成记录处理；接收 种类（kind）、名称（name）、开始时（started）、状态（status）、用量（usage）、首次身份凭据ms（first_token_ms）、错误编码（error_code）。
async def record(*, kind: str, name: str, started: float, status: str,
                 # 填写用量（usage）字段，值为表达式 Any = None, first_token_ms: int | None = None 的结果。
                 usage: Any = None, first_token_ms: int | None = None,
                 # 填写错误编码（error_code）字段，值为表达式 str | None = None): 的结果。
                 error_code: str | None = None):
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 值（value）。
    value = current_observation.get()
    # 检查value is None，即对应内容是否不存在；成立时执行缩进的分支。
    if value is None:
        # 把空值交还给调用方，本函数到此结束。
        return
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 value.db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with value.db.session() as session:
            # 加入集合或登记一条待保存记录。
            session.add(CallRecord(user_id=value.user_id, trace_id=value.trace_id,
                # 传入种类（kind），值为表达式 kind, name=name[:128], status=status 的结果。
                kind=kind, name=name[:128], status=status,
                # 传入持续时间ms（duration_ms），值为调用 max(0, int((time.monotonic 完成monotonic处理。
                duration_ms=max(0, int((time.monotonic()-started)*1000)),
                # 传入首次身份凭据ms（first_token_ms），值为首次身份凭据ms（first_token_ms）。
                first_token_ms=first_token_ms,
                # 传入模型提示词凭据处理器（prompt_tokens），值为读取 attr 的值。
                prompt_tokens=getattr(usage, "prompt_tokens", None),
                # 传入完成情况凭据处理器（completion_tokens），值为读取 attr 的值。
                completion_tokens=getattr(usage, "completion_tokens", None),
                # 传入错误编码（error_code），值为表达式 error_code)) 的结果。
                error_code=error_code))
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 观测失败不影响主业务
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("observation_write_failed type=%s", type(error).__name__)


# 定义 ObservedStream，把Observed分段回复流需要的数据与操作放在同一个对象中。
class ObservedStream:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 分段回复流（stream）、开始时（started）、模型（model）。
    def __init__(self, stream, *, started: float, model: str):
        # 把依次包含 分段回复流（stream）、开始时（started）、模型（model） 的集合保存到 self分段回复流self开始时self模型（(self.stream, self.started, self.model)）。
        self.stream, self.started, self.model = stream, started, model
        # 把空值，表示当前还没有对应内容保存到 当前实例中的用量（self.usage）。
        self.usage = None
        # 把空值，表示当前还没有对应内容保存到 当前实例中的首次（self.first）。
        self.first = None
        # 把假，表示条件不成立或开关关闭保存到 当前实例中的closed（self.closed）。
        self.closed = False
        # 把固定状态或编码 CANCELLED（已取消）保存到 当前实例中的状态（self.status）。
        self.status = "CANCELLED"
        # 把空值，表示当前还没有对应内容保存到 当前实例中的错误编码（self.error_code）。
        self.error_code = None

    # 函数 __aiter__：调用 __aiter__ 完成aiter处理。
    def __aiter__(self):
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self

    # 异步函数 __anext__：调用 __anext__ 完成anext处理。
    async def __anext__(self):
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把等待执行“调用 self.stream.__anext__ 完成anext处理”得到的结果完成后的结果保存到 文档片段（chunk）。
            chunk = await self.stream.__anext__()
            # 检查条件 chunk.usage is not None 是否成立；成立时执行缩进的分支。
            if chunk.usage is not None:
                # 把文档片段（chunk）的用量（usage）属性保存到 当前实例中的用量（self.usage）。
                self.usage = chunk.usage
            # 检查self.first is None and any((choice.delta.content or choice.delta.tool_calls for choice in chunk.choi…，即对应内容是否不存在；成立时执行缩进的分支。
            if self.first is None and any(choice.delta.content or choice.delta.tool_calls for choice in chunk.choices):
                # 把执行“调用 max 完成最大处理”得到的结果保存到 当前实例中的首次（self.first）。
                self.first = max(0, int((time.monotonic()-self.started)*1000))
            # 把文档片段（chunk）交还给调用方，本函数到此结束。
            return chunk
        # 捕获StopAsyncIteration异常，执行下面的错误处理。
        except StopAsyncIteration:
            # 把固定状态或编码 SUCCEEDED（已成功）保存到 当前实例中的状态（self.status）。
            self.status = "SUCCEEDED"
            # 等待释放连接、文件或客户端占用的资源完成后再继续。
            await self.close()
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 捕获BaseException异常，执行下面的错误处理，异常对象保存为 error。
        except BaseException as error:
            # 把固定状态或编码 FAILED（失败）保存到 当前实例中的状态（self.status）。
            self.status = "FAILED"
            # 把执行“调用 classify_model_error 完成classify模型错误处理”得到的结果的编码（code）属性保存到 当前实例中的错误编码（self.error_code）。
            self.error_code = classify_model_error(error).code
            # 等待释放连接、文件或客户端占用的资源完成后再继续。
            await self.close()
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise

    # 异步函数 close：释放连接、文件或客户端占用的资源。
    async def close(self):
        # 检查条件 self.closed 是否成立；成立时执行缩进的分支。
        if self.closed:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把真，表示条件成立或开关打开保存到 当前实例中的closed（self.closed）。
        self.closed = True
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待释放连接、文件或客户端占用的资源完成后再继续。
            await self.stream.close()
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 等待调用 record 完成记录处理完成后再继续。
            await record(kind="MODEL", name=self.model, started=self.started, status=self.status,
                # 传入用量（usage），值为表达式 self.usage, first_token_ms=self.first 的结果。
                usage=self.usage, first_token_ms=self.first,
                # 传入错误编码（error_code），值为表达式 self.error_code) 的结果。
                error_code=self.error_code)


# 定义 ObservedCompletions，把ObservedCompletions需要的数据与操作放在同一个对象中。
class ObservedCompletions:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 delegate。
    def __init__(self, delegate):
        # 把delegate保存到 当前实例中的delegate（self.delegate）。
        self.delegate = delegate

    # 异步函数 create：调用 create 完成创建处理。
    async def create(self, **kwargs):
        # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
        started = time.monotonic()
        # 把执行“调用 str 完成str处理”得到的结果保存到 模型（model）。
        model = str(kwargs.get("model", "unknown"))
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把等待执行“调用 self.delegate.create 完成创建处理”得到的结果完成后的结果保存到 结果（result）。
            result = await self.delegate.create(**kwargs)
        # 捕获BaseException异常，执行下面的错误处理，异常对象保存为 error。
        except BaseException as error:
            # 等待调用 record 完成记录处理完成后再继续。
            await record(kind="MODEL", name=model, started=started, status="FAILED",
                # 传入错误编码（error_code），值为调用 classify_model_error 完成classify模型错误处理。
                error_code=classify_model_error(error).code)
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 检查条件 kwargs.get('stream') 是否成立；成立时执行缩进的分支。
        if kwargs.get("stream"):
            # 把执行“调用 ObservedStream 完成Observed分段回复流处理”得到的结果交还给调用方，本函数到此结束。
            return ObservedStream(result, started=started, model=model)
        # 等待调用 record 完成记录处理完成后再继续。
        await record(kind="MODEL", name=model, started=started, status="SUCCEEDED", usage=result.usage)
        # 把结果（result）交还给调用方，本函数到此结束。
        return result
