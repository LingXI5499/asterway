# 封装shared查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 成长只读工具共用能力：Java 查询错误转换、文本裁剪和分页说明。
# 
# 工具只读取 Java 业务数据，不写库、不扩大当前用户权限；所有资源归属由 Java 校验。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 typing 引入 Any。
from typing import Any

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 Java后端接口错误（JavaApiError）。
from app.core.errors import JavaApiError

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）、Java后端接口结果（JavaApiResult）。
from app.integrations.java_client import JavaApiClient, JavaApiResult

# 从 app.tools.base 引入 业务工具Outcome（ToolOutcome）。
from app.tools.base import ToolOutcome

# 把固定数值 500保存到 文字上限（TEXT_LIMIT）。
TEXT_LIMIT = 500


# 函数 text：可选文本字段：去空白并限制长度，超出时保留可识别截断标记。；接收 值（value）、上限（limit）。
def text(value: Any, limit: int = TEXT_LIMIT) -> str | None:
    # 可选文本字段：去空白并限制长度，超出时保留可识别截断标记。
    # 检查条件 not isinstance(value, str) 是否成立；成立时执行缩进的分支。
    if not isinstance(value, str):
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把执行“去掉文字两端的空白”得到的结果保存到 stripped。
    stripped = value.strip()
    # 检查条件 not stripped 是否成立；成立时执行缩进的分支。
    if not stripped:
        # 把固定文字 ''交还给调用方，本函数到此结束。
        return ""
    # 检查条件 len(stripped) <= limit 是否成立；成立时执行缩进的分支。
    if len(stripped) <= limit:
        # 把stripped交还给调用方，本函数到此结束。
        return stripped
    # 把按 stripped[:limit - 1] + '…' 计算的结果交还给调用方，本函数到此结束。
    return stripped[: limit - 1] + "…"


# 函数 error：调用 error 完成错误处理；接收 消息（message）、汇总（summary）。
def error(message: str, summary: str) -> ToolOutcome:
    # 把执行“调用 ToolOutcome 完成业务工具Outcome处理”得到的结果交还给调用方，本函数到此结束。
    return ToolOutcome(
        # 传入内容（content），值为把对象编码为 JSON 文字。
        content=json.dumps({"error": message}, ensure_ascii=False),
        # 传入汇总（summary），值为汇总（summary）。
        summary=summary,
        # 传入是否错误（is_error），值为真，表示条件成立或开关打开。
        is_error=True,
    )


# 异步函数 get：执行一次只读查询；登录失效继续向上抛出，其余错误转换为工具错误说明。；接收 上下文（context）、客户端（client）、路径（path）、参数集合（params）。
async def get(
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写客户端（client）字段，值为Java后端接口客户端（JavaApiClient）。
    client: JavaApiClient,
    # 填写路径（path）字段，值为str。
    path: str,
    # 填写参数集合（params）字段，值为表达式 dict[str, str | int | bool] | None = None 的结果。
    params: dict[str, str | int | bool] | None = None,
# 继续传入或组合表达式 ) -> tuple[JavaApiResult | None, str | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[JavaApiResult | None, str | None]:
    # 执行一次只读查询；登录失效继续向上抛出，其余错误转换为工具错误说明。
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把依次包含 等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
        return (
            # 继续传入或组合等待 按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值 完成后取得结果，完成上面尚未结束的表达式。
            await client.get(
                # 继续传入或组合路径（path），完成上面尚未结束的表达式。
                path,
                # 传入授权（authorization），值为表达式 context.authorization 的结果。
                authorization=context.authorization,
                # 传入追踪标识编号（trace_id），值为表达式 context.trace_id 的结果。
                trace_id=context.trace_id,
                # 传入参数集合（params），值为参数集合（params）。
                params=params,
            ),
            # 继续传入或组合空值，表示当前还没有对应内容，完成上面尚未结束的表达式。
            None,
        )
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 exc。
    except JavaApiError as exc:
        # 检查条件 exc.status_code == 401 是否成立；成立时执行缩进的分支。
        if exc.status_code == 401:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 把依次包含 空值，表示当前还没有对应内容、捕获到的异常（exc）的消息（message）属性 的集合交还给调用方，本函数到此结束。
        return None, exc.message


# 函数 page：统一分页结果，明确返回“第一页不是全部”，避免模型把首页当成全集。；接收 数据列表（items）、总数（total）。
def page(items: list[dict[str, Any]], total: Any) -> dict[str, Any]:
    # 统一分页结果，明确返回“第一页不是全部”，避免模型把首页当成全集。
    # 把条件 isinstance(total, int) and (not isinstance(total, bool)) 是否成立时使用总数（total），否则使用执行“调用 len 完成len处理”得到的结果保存到 safe总数（safe_total）。
    safe_total = total if isinstance(total, int) and not isinstance(total, bool) else len(items)
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写总数（total）字段，值为safe总数（safe_total）。
        "total": safe_total,
        # 填写已返回（returned）字段，值为集合或文字的长度。
        "returned": len(items),
        # 填写是否存在More（hasMore）字段，值为条件判断的结果：safe_total > len(items)。
        "hasMore": safe_total > len(items),
        # 填写当前页（page）字段，值为固定数值 1。
        "page": 1,
        # 填写数据列表（items）字段，值为数据列表（items）。
        "items": items,
    }


# 函数 overview：目标/阶段返回的轻量子项，只保留可核对的状态与进度。；接收 记录列表（rows）。
def overview(rows: Any) -> list[dict[str, Any]]:
    # 目标/阶段返回的轻量子项，只保留可核对的状态与进度。
    # 检查条件 not isinstance(rows, list) 是否成立；成立时执行缩进的分支。
    if not isinstance(rows, list):
        # 把空列表或集合交还给调用方，本函数到此结束。
        return []
    # 声明 数据列表（items），类型为 list[dict[str, Any]]；其内容为空列表或集合。
    items: list[dict[str, Any]] = []
    # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
    for row in rows:
        # 检查not isinstance(row, dict) or row.get('id') is None or (not isinstance(row.get('name'), str))，即对应内容是否不存在；成立时执行缩进的分支。
        if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把新条目放到列表末尾。
        items.append(
            {
                # 填写键（key）字段，值为表达式 row[key] 的结果。
                key: row[key]
                # 继续传入或组合表达式 for key in ("id", "name", "status", "progress") 的结果，完成上面尚未结束的表达式。
                for key in ("id", "name", "status", "progress")
                # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                if row.get(key) is not None
            }
        )
    # 把数据列表（items）交还给调用方，本函数到此结束。
    return items


# 异步函数 today：读取 Java 账号当前业务日期与有效时区；计划/执行工具用它解析今天、本周与区间。；接收 上下文（context）、客户端（client）。
async def today(
    # 填写上下文（context）字段，值为表达式 AgentContext, client: JavaApiClient 的结果。
    context: AgentContext, client: JavaApiClient
# 继续传入或组合表达式 ) -> tuple[str | None, str | None, str | None]: 的结果，完成上面尚未结束的表达式。
) -> tuple[str | None, str | None, str | None]:
    # 读取 Java 账号当前业务日期与有效时区；计划/执行工具用它解析今天、本周与区间。
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 结果错误消息（(result, error_message)）。
    result, error_message = await get(context, client, "dashboard/overview")
    # 检查条件 error_message is not None 是否成立；成立时执行缩进的分支。
    if error_message is not None:
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、错误消息（error_message） 的集合交还给调用方，本函数到此结束。
        return None, None, error_message
    # 把条件 result is not None 是否成立时使用结果（result）的数据（data）属性，否则使用空值，表示当前还没有对应内容保存到 数据（data）。
    data = result.data if result is not None else None
    # 检查条件 not isinstance(data, dict) or not isinstance(data.get('today'), str) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or not isinstance(data.get("today"), str):
        # 把依次包含 空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、固定文字 '无法读取账号今天的业务日期' 的集合交还给调用方，本函数到此结束。
        return None, None, "无法读取账号今天的业务日期"
    # 把条件 isinstance(data.get('timeZone'), str) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 用户使用的时区（time_zone）。
    time_zone = data.get("timeZone") if isinstance(data.get("timeZone"), str) else None
    # 把依次包含 从数据（data）中按固定文字 'today'取出的内容、用户使用的时区（time_zone）、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
    return data["today"], time_zone, None

