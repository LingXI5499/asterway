"""方案创建后的真实业务进度；分开统计目标、任务、安排和今日习惯。"""

import asyncio

from app.core.errors import AgentError, JavaApiError
from app.services import workflow_service

_KINDS = {
    "goals": ("goalIds", "goals", "goal:goal:read"),
    "stages": ("stageIds", "stages", "stage:stage:read"),
    "tasks": ("taskIds", "tasks", "task:task:read"),
    "weeks": ("weeklyPlanIds", "weekly-plans", "planning:weekly:read"),
    "days": ("dailyPlanIds", "daily-plans", "planning:daily:read"),
    "habits": ("habitIds", "habits", "habit:habit:read"),
}


async def collect(db, context, client, run_id):
    context.require("agent:workflow:read")
    detail = await workflow_service.detail(db, user_id=int(context.user_id), run_id=run_id)
    receipt = detail.get("commit") or {}
    if receipt.get("status") != "SUCCEEDED":
        return {"ready": False, "groups": {}, "signals": [], "partial": False}
    plan = next((item for item in detail["plans"] if item["id"] == detail["run"]["latestPlanVersionId"]), {})
    snapshot = plan.get("snapshot") or {}
    result = receipt.get("result") or {}
    ids = {kind: list(dict.fromkeys(str(value) for value in result.get(field, [])))
           for kind, (field, _, _) in _KINDS.items()}
    for source, field, kind in (("stages", "goalId", "goals"), ("tasks", "stageId", "stages"),
                                ("taskAssignments", "taskId", "tasks")):
        ids[kind] = list(dict.fromkeys([*ids[kind], *(str(row[field]) for row in snapshot.get(source, []) if row.get(field))]))
    calendar = await client.get("dashboard/overview", authorization=context.authorization, trace_id=context.trace_id)
    today = calendar.data.get("today") if isinstance(calendar.data, dict) else None
    if not today:
        raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "暂时无法取得账号业务日期")
    slots = asyncio.Semaphore(4)
    errors = []
    partial = any(len(values) > 20 for values in ids.values())

    async def get(path, params=None):
        try:
            async with slots:
                value = await client.get(path, authorization=context.authorization,
                                          trace_id=context.trace_id, params=params)
            return value.data
        except JavaApiError as error:
            if error.status_code == 401:
                raise
            errors.append("部分业务进度暂未读取成功，请刷新重试")
            return None

    async def row(kind, entity_id):
        _, path, _ = _KINDS[kind]
        value = await get(f"{path}/{entity_id}")
        if not isinstance(value, dict):
            return None
        fields = ("id", "name", "title", "status", "progress", "goalId", "stageId", "dueDate", "expectedEndDate",
                  "weekStartDate", "weekEndDate", "planDate", "checkedToday")
        item = {key: value[key] for key in fields if value.get(key) is not None}
        item["id"] = str(value.get("id", entity_id))
        item["kind"] = kind
        if kind in {"weeks", "days"}:
            data = await get(f"{path}/{entity_id}/tasks", {"page": 1, "size": 100})
            if isinstance(data, dict) and isinstance(data.get("items"), list):
                items = data["items"]
                total = data.get("total", len(items))
                item["taskTotal"] = total
                item["taskReturned"] = len(items)
                item["taskCompleted"] = sum(task.get("status") == "COMPLETED" for task in items)
                item["partial"] = total > len(items)
                item["overdue"] = sum(task.get("status") != "COMPLETED" and
                    (value.get("planDate") or value.get("weekEndDate") or today) < today for task in items)
        if kind == "tasks":
            due = value.get("dueDate") or value.get("expectedEndDate")
            item["overdue"] = bool(due and due < today and value.get("status") != "COMPLETED")
        return item

    async def group(kind):
        if _KINDS[kind][2] not in context.permissions:
            if ids[kind]:
                errors.append("部分对象没有读取权限")
            return []
        return [value for value in await asyncio.gather(*(row(kind, value) for value in ids[kind][:20])) if value]

    groups = dict(zip(_KINDS, await asyncio.gather(*(group(kind) for kind in _KINDS)), strict=True))
    # 既有阶段/任务方案也能跟踪所属目标，不限于本次新建的 goalIds。
    for kind, parent_field, parent_kind in (("tasks", "stageId", "stages"), ("stages", "goalId", "goals")):
        known = {item["id"] for item in groups[parent_kind]}
        parents = list(dict.fromkeys(str(item[parent_field]) for item in groups[kind]
                                    if item.get(parent_field) and str(item[parent_field]) not in known))[:20]
        if _KINDS[parent_kind][2] in context.permissions:
            groups[parent_kind].extend(value for value in await asyncio.gather(*(row(parent_kind, value) for value in parents)) if value)
    if groups["habits"] and "habit:habit:list" in context.permissions:
        data = await get("habits/today", {"page": 1, "size": 100})
        if isinstance(data, dict) and isinstance(data.get("items"), list):
            due = {str(item.get("id") or item.get("habitId")): item for item in data["items"]}
            complete_list = data.get("total", len(due)) <= len(data["items"])
            for item in groups["habits"]:
                actual = due.get(item["id"])
                item["todayKnown"] = actual is not None or complete_list
                item["dueToday"] = actual is not None
                item["checkedToday"] = bool(actual and (actual.get("checkinId") or actual.get("checkedToday")
                                                         or (actual.get("doneCount") or 0) > 0))
    overdue = sum(bool(item.get("overdue")) for item in groups["tasks"])
    plan_overdue = sum(item.get("overdue", 0) for kind in ("weeks", "days") for item in groups[kind])
    completed = sum(item.get("status") == "COMPLETED" for item in groups["tasks"])
    signals = []
    if overdue or plan_overdue:
        signals.append({"code": "OVERDUE", "message": "有任务超过截止或原安排时间，可以复盘后调整后续安排"})
    if completed:
        signals.append({"code": "PROGRESS", "message": f"已完成 {completed} 个跟踪任务，可以根据实际投入安排下一步"})
    return {"ready": True, "asOf": today, "groups": groups, "signals": signals,
            "partial": partial or bool(errors) or any(item.get("partial") for values in groups.values() for item in values),
            "notes": list(dict.fromkeys(errors)), "trackedCounts": {kind: len(values) for kind, values in groups.items()}}
