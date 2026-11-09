# 校验并执行模型申请的业务工具，记录调用结果并限制查询额度。
# 工具执行边界：权限、参数、超时、结果裁剪和审计，失败转为模型可读结果。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 引入 re，供下面调用其功能。
import re

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 typing 引入 Any。
from typing import Any

# 从 pydantic 引入 Validation错误（ValidationError）。
from pydantic import ValidationError

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）、Java后端接口错误（JavaApiError）。
from app.core.errors import AgentError, JavaApiError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）、业务工具Outcome（ToolOutcome）。
from app.tools.base import ToolExecution, ToolOutcome

# 从 app.tools.registry 引入 显示名称for（label_for）、业务工具for（tool_for）。
from app.tools.registry import label_for, tool_for

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 把执行“调用 re.compile 完成compile处理”得到的结果保存到 SAFE名称（_SAFE_NAME）。
_SAFE_NAME = re.compile(r"[a-z][a-z0-9_]{0,63}")
# 把固定文字 'unknown'保存到 UNKNOWN（_UNKNOWN）。
_UNKNOWN = "unknown"


# 函数 _safe_name：调用 _safe_name 完成safe名称处理；接收 名称（name）。
def _safe_name(name: str) -> str:
    # 把条件 isinstance(name, str) and _SAFE_NAME.fullmatch(name) 是否成立时使用名称（name），否则使用UNKNOWN（_UNKNOWN）交还给调用方，本函数到此结束。
    return name if isinstance(name, str) and _SAFE_NAME.fullmatch(name) else _UNKNOWN


# 函数 _error：调用 _error 完成错误处理；接收 消息（message）。
def _error(message: str) -> ToolOutcome:
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"error": message}, ensure_ascii=False),
        # 传入汇总（summary），值为消息（message）。
        summary=message,
        # 传入是否错误（is_error），值为真，表示条件成立或开关打开。
        is_error=True,
    )


# 函数 _finish：调用 _finish 完成finish处理；接收 名称（name）、显示名称（label）、outcome、开始时（started）。
def _finish(
    # 填写名称（name）字段，值为str。
    name: str,
    # 填写显示名称（label）字段，值为str。
    label: str,
    # 填写outcome字段，值为业务工具Outcome（ToolOutcome）。
    outcome: ToolOutcome,
    # 填写开始时（started）字段，值为浮点小数（float）。
    started: float,
# 继续传入或组合表达式 ) -> ToolExecution: 的结果，完成上面尚未结束的表达式。
) -> ToolExecution:
    # 把执行“调用 max 完成最大处理”得到的结果保存到 持续时间ms（duration_ms）。
    duration_ms = max(0, int((time.monotonic() - started) * 1000))
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“把 JSON 文字解析为对象”得到的结果保存到 数据（data）。
        data = json.loads(outcome.content)
    # 捕获(ValueError, RecursionError)异常，执行下面的错误处理。
    except (ValueError, RecursionError):
        # 把按字段名保存多个值的字典保存到 数据（data）。
        data = {"text": outcome.content}
    # 把执行“把对象编码为 JSON 文字”得到的结果保存到 内容（content）。
    content = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
    # 把执行“读取并复用当前进程的运行配置”得到的结果的成长助手业务工具最大结果chars（agent_tool_max_result_chars）属性保存到 上限（limit）。
    limit = get_settings().agent_tool_max_result_chars
    # 检查条件 len(content) > limit 是否成立；成立时执行缩进的分支。
    if len(content) > limit:
        # 把从内容（content）中按按表达式计算得到的值：:max(0, (limit - 256) // 2)取出的内容保存到 preview。
        preview = content[: max(0, (limit - 256) // 2)]
        # 只要条件 True 是否成立，就继续执行下面的循环。
        while True:
            # 把执行“把对象编码为 JSON 文字”得到的结果保存到 bounded。
            bounded = json.dumps(
                {
                    # 填写truncated字段，值为真，表示条件成立或开关打开。
                    "truncated": True,
                    # 填写消息（message）字段，值为固定文字 '查询结果过长，以下仅为部分预览，不能当作完整列表'。
                    "message": "查询结果过长，以下仅为部分预览，不能当作完整列表",
                    # 填写preview字段，值为preview。
                    "preview": preview,
                },
                # 传入ensureascii（ensure_ascii），值为假，表示条件不成立或开关关闭。
                ensure_ascii=False,
                # 传入separators，值为表达式 (",", ":") 的结果。
                separators=(",", ":"),
            )
            # 检查条件 len(bounded) <= limit 是否成立；成立时执行缩进的分支。
            if len(bounded) <= limit:
                # 把bounded保存到 内容（content）。
                content = bounded
                # 立即退出当前循环，不再处理后续条目。
                break
            # 把从preview中按按表达式计算得到的值：:len(preview) // 2取出的内容保存到 preview。
            preview = preview[: len(preview) // 2]
    # 记录info级别的运行信息，供定位处理过程和失败原因。
    logger.info(
        # 继续传入或组合固定文字 'agent_tool_call tool=%s status=%s duration_ms=%d'，完成上面尚未结束的表达式。
        "agent_tool_call tool=%s status=%s duration_ms=%d",
        # 继续传入或组合名称（name），完成上面尚未结束的表达式。
        name,
        # 继续传入或组合固定文字 'error" if outcome.is_error else "ok'，完成上面尚未结束的表达式。
        "error" if outcome.is_error else "ok",
        # 继续传入或组合持续时间ms（duration_ms），完成上面尚未结束的表达式。
        duration_ms,
    )
    # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果交还给调用方，本函数到此结束。
    return ToolExecution(
        # 传入名称（name），值为名称（name）。
        name=name,
        # 传入显示名称（label），值为显示名称（label）。
        label=label,
        # 传入内容（content），值为内容（content）。
        content=content,
        # 传入汇总（summary），值为表达式 outcome.summary[:200] 的结果。
        summary=outcome.summary[:200],
        # 传入是否错误（is_error），值为表达式 outcome.is_error 的结果。
        is_error=outcome.is_error,
        # 传入持续时间ms（duration_ms），值为持续时间ms（duration_ms）。
        duration_ms=duration_ms,
    )


# 函数 budget_exceeded：超出调用次数时仍为每个 tool_call 返回结果，保证下一轮消息结构完整。；接收 名称（name）。
def budget_exceeded(name: str) -> ToolExecution:
    # 超出调用次数时仍为每个 tool_call 返回结果，保证下一轮消息结构完整。
    # 把执行“调用 _safe_name 完成safe名称处理”得到的结果保存到 safe。
    safe = _safe_name(name)
    # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果交还给调用方，本函数到此结束。
    return ToolExecution(
        # 传入名称（name），值为safe。
        name=safe,
        # 传入显示名称（label），值为调用 label_for 完成显示名称for处理。
        label=label_for(safe),
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps(
            # 继续传入或组合表达式 {"status": "limited", "notice": "本轮已取得部分资料，剩余查询暂缓执行。" 的结果，完成上面尚未结束的表达式。
            {"status": "limited", "notice": "本轮已取得部分资料，剩余查询暂缓执行。"
             "请说明哪些范围尚未核对，基于已取得的事实回答；用户可继续追问。"},
            # 传入ensureascii（ensure_ascii），值为假，表示条件不成立或开关关闭。
            ensure_ascii=False,
        ),
        # 传入汇总（summary），值为固定文字 '本轮先汇总已取得的资料，此项尚未查询，可继续追问'。
        summary="本轮先汇总已取得的资料，此项尚未查询，可继续追问",
        # 传入是否错误（is_error），值为假，表示条件不成立或开关关闭。
        is_error=False,
        # 传入持续时间ms（duration_ms），值为固定数值 0。
        duration_ms=0,
        # 传入是否limited（is_limited），值为真，表示条件成立或开关打开。
        is_limited=True,
    )


# 异步函数 _execute：调用 _execute 完成执行处理；接收 上下文（context）、Java后端客户端（java_client）、名称（name）、arguments。
async def _execute(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写名称（name）字段，值为str。
    name: str,
    # 填写arguments字段，值为str。
    arguments: str,
# 继续传入或组合表达式 ) -> ToolExecution: 的结果，完成上面尚未结束的表达式。
) -> ToolExecution:
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
    started = time.monotonic()
    # 把执行“调用 tool_for 完成业务工具for处理”得到的结果保存到 spec。
    spec = tool_for(name)
    # 检查spec is None，即对应内容是否不存在；成立时执行缩进的分支。
    if spec is None:
        # 把执行“调用 _safe_name 完成safe名称处理”得到的结果保存到 safe。
        safe = _safe_name(name)
        # 把执行“调用 _finish 完成finish处理”得到的结果交还给调用方，本函数到此结束。
        return _finish(safe, label_for(safe), _error("未知的查询工具"), started)
    # 检查当前用户是否具有需要的权限：not spec.allowed(context.permissions)；成立时执行缩进的分支。
    if not spec.allowed(context.permissions):
        # 把执行“调用 _finish 完成finish处理”得到的结果交还给调用方，本函数到此结束。
        return _finish(
            # 继续传入或组合表达式 spec.name 的结果，完成上面尚未结束的表达式。
            spec.name,
            # 继续传入或组合表达式 spec.label 的结果，完成上面尚未结束的表达式。
            spec.label,
            # 继续传入或组合调用 _error 完成错误处理，完成上面尚未结束的表达式。
            _error("当前账户没有该查询的权限，无法读取这类业务数据"),
            # 继续传入或组合开始时（started），完成上面尚未结束的表达式。
            started,
        )
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 声明 parsed，类型为 Any；其内容为执行“调用 spec.args_model.model_validate_json 完成模型检查合法性JSON数据处理”得到的结果。
        parsed: Any = spec.args_model.model_validate_json(arguments or "{}")
    # 捕获ValidationError异常，执行下面的错误处理。
    except ValidationError:
        # 把执行“调用 _finish 完成finish处理”得到的结果交还给调用方，本函数到此结束。
        return _finish(spec.name, spec.label, _error("查询参数不符合要求"), started)
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 asyncio.timeout 完成超时处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        async with asyncio.timeout(get_settings().agent_tool_timeout_seconds):
            # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 当前（current）。
            current = await refresh_context(context, java_client)
            # 检查当前用户是否具有需要的权限：not spec.allowed(current.permissions)；成立时执行缩进的分支。
            if not spec.allowed(current.permissions):
                # 把执行“调用 _finish 完成finish处理”得到的结果交还给调用方，本函数到此结束。
                return _finish(spec.name, spec.label, _error("当前账户已没有该查询权限"), started)
            # 把等待执行“调用 spec.handler 完成handler处理”得到的结果完成后的结果保存到 outcome。
            outcome = await spec.handler(current, java_client, parsed)
    # 捕获TimeoutError异常，执行下面的错误处理。
    except TimeoutError:
        # 把执行“调用 _error 完成错误处理”得到的结果保存到 outcome。
        outcome = _error("查询超时，请缩小查询范围后重试")
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
    except JavaApiError as error:
        # 检查条件 error.status_code == 401 是否成立；成立时执行缩进的分支。
        if error.status_code == 401:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 把执行“调用 _error 完成错误处理”得到的结果保存到 outcome。
        outcome = _error(error.message)
    # 捕获AgentError异常，执行下面的错误处理。
    except AgentError:
        # 登录失效、管理身份或发送权限撤销必须终止生成，不能伪装成普通查询失败。
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 工具失败不能中断整个回答。
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning(
            # 继续传入或组合固定文字 'agent_tool_failed tool=%s type=%s trace=%s'，完成上面尚未结束的表达式。
            "agent_tool_failed tool=%s type=%s trace=%s",
            # 继续传入或组合表达式 spec.name 的结果，完成上面尚未结束的表达式。
            spec.name,
            # 继续传入或组合调用 type 完成类型处理，完成上面尚未结束的表达式。
            type(error).__name__,
            # 继续传入或组合表达式 context.trace_id 的结果，完成上面尚未结束的表达式。
            context.trace_id,
        )
        # 把执行“调用 _error 完成错误处理”得到的结果保存到 outcome。
        outcome = _error("查询暂时不可用，请稍后重试")
    # 把执行“调用 _finish 完成finish处理”得到的结果交还给调用方，本函数到此结束。
    return _finish(spec.name, spec.label, outcome, started)


# 异步函数 execute：执行传入的数据库语句并取得结果；接收 上下文（context）、Java后端客户端（java_client）、名称（name）、arguments。
async def execute(context: AgentContext, java_client: JavaApiClient, name: str, arguments: str) -> ToolExecution:

    # 从 app.services.observation_service 引入 记录（record）。
    from app.services.observation_service import record
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
    started = time.monotonic()
    # 把等待执行“调用 _execute 完成执行处理”得到的结果完成后的结果保存到 结果（result）。
    result = await _execute(context, java_client, name, arguments)
    # 等待调用 record 完成记录处理完成后再继续。
    await record(kind="TOOL", name=result.name, started=started,
        # 传入状态（status），值为固定文字 'FAILED" if result.is_error else "SUCCEEDED'。
        status="FAILED" if result.is_error else "SUCCEEDED",
        # 传入错误编码（error_code），值为表达式 "TOOL_FAILED" if result.is_error else None) 的结果。
        error_code="TOOL_FAILED" if result.is_error else None)
    # 把结果（result）交还给调用方，本函数到此结束。
    return result
