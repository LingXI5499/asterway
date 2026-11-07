"""成长只读工具共用能力：Java 查询错误转换、文本裁剪和分页说明。

工具只读取 Java 业务数据，不写库、不扩大当前用户权限；所有资源归属由 Java 校验。
"""

import json
from typing import Any

from app.core.context import AgentContext
from app.core.errors import JavaApiError
from app.integrations.java_client import JavaApiClient, JavaApiResult
from app.tools.base import ToolOutcome

TEXT_LIMIT = 500


def text(value: Any, limit: int = TEXT_LIMIT) -> str | None:
    """可选文本字段：去空白并限制长度，超出时保留可识别截断标记。"""
    if not isinstance(value, str):
        return None
    stripped = value.strip()
    if not stripped:
        return ""
    if len(stripped) <= limit:
        return stripped
    return stripped[: limit - 1] + "…"


def error(message: str, summary: str) -> ToolOutcome:
    return ToolOutcome(
        content=json.dumps({"error": message}, ensure_ascii=False),
        summary=summary,
        is_error=True,
    )


async def get(
    context: AgentContext,
    client: JavaApiClient,
    path: str,
    params: dict[str, str | int | bool] | None = None,
) -> tuple[JavaApiResult | None, str | None]:
    """执行一次只读查询；登录失效继续向上抛出，其余错误转换为工具错误说明。"""
    try:
        return (
            await client.get(
                path,
                authorization=context.authorization,
                trace_id=context.trace_id,
                params=params,
            ),
            None,
        )
    except JavaApiError as exc:
        if exc.status_code == 401:
            raise
        return None, exc.message


def page(items: list[dict[str, Any]], total: Any) -> dict[str, Any]:
    """统一分页结果，明确返回“第一页不是全部”，避免模型把首页当成全集。"""
    safe_total = total if isinstance(total, int) and not isinstance(total, bool) else len(items)
    return {
        "total": safe_total,
        "returned": len(items),
        "hasMore": safe_total > len(items),
        "page": 1,
        "items": items,
    }


def overview(rows: Any) -> list[dict[str, Any]]:
    """目标/阶段返回的轻量子项，只保留可核对的状态与进度。"""
    if not isinstance(rows, list):
        return []
    items: list[dict[str, Any]] = []
    for row in rows:
        if not isinstance(row, dict) or row.get("id") is None or not isinstance(row.get("name"), str):
            continue
        items.append(
            {
                key: row[key]
                for key in ("id", "name", "status", "progress")
                if row.get(key) is not None
            }
        )
    return items


async def today(
    context: AgentContext, client: JavaApiClient
) -> tuple[str | None, str | None, str | None]:
    """读取 Java 账号当前业务日期与有效时区；计划/执行工具用它解析今天、本周与区间。"""
    result, error_message = await get(context, client, "dashboard/overview")
    if error_message is not None:
        return None, None, error_message
    data = result.data if result is not None else None
    if not isinstance(data, dict) or not isinstance(data.get("today"), str):
        return None, None, "无法读取账号今天的业务日期"
    time_zone = data.get("timeZone") if isinstance(data.get("timeZone"), str) else None
    return data["today"], time_zone, None

