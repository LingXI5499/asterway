"""规划业务上下文：只读读取现有目标、任务、计划、习惯，供方案生成避免重复与冲突。

只读收集本人业务事实；读取失败明确终止，避免基于缺失事实生成可执行方案。
"""

import json
import logging
from typing import Any

from app.core.context import AgentContext
from app.core.errors import AgentError
from app.integrations.java_client import JavaApiClient, JavaApiError

logger = logging.getLogger(__name__)

_MAX_CHARS = 10000


async def _get(
    context: AgentContext,
    client: JavaApiClient,
    path: str,
    params: dict[str, str | int | bool] | None = None,
) -> Any:
    result = await client.get(
        path,
        authorization=context.authorization,
        trace_id=context.trace_id,
        params=params,
    )
    return result.data


def _short(value: Any, limit: int = 60) -> str:
    text = str(value or "").strip().replace("\n", " ")
    return text[:limit]


async def collect(context: AgentContext, client: JavaApiClient, intent: str) -> str | None:
    """按意图收集最相关的现有业务事实，压缩成模型可读文本。"""
    blocks: list[str] = []
    try:
        dashboard = await _get(context, client, "dashboard/overview")
        if isinstance(dashboard, dict):
            parts = []
            if dashboard.get("today"):
                parts.append(f"账号今天 {dashboard['today']}")
            if dashboard.get("timeZone"):
                parts.append(f"时区 {dashboard['timeZone']}")
            if parts:
                blocks.append("；".join(parts))

        needs_goal = intent in {
            "GOAL_PLAN",
            "STAGE_PLAN",
            "TASK_PLAN",
            "COMPREHENSIVE_PLAN",
        }
        needs_plan = intent in {
            "WEEKLY_PLAN",
            "DAILY_PLAN",
            "COMPREHENSIVE_PLAN",
        }
        needs_habit = intent in {"HABIT_PLAN", "COMPREHENSIVE_PLAN"}

        if needs_goal:
            goals = await _get(context, client, "goals", {"page": 1, "size": 20})
            items = goals.get("items") if isinstance(goals, dict) else None
            if isinstance(items, list) and items:
                lines = [
                    f"- ID={item.get('id')} version={item.get('version')} {_short(item.get('name'))}（{item.get('status')}，{item.get('startDate')}~{item.get('expectedEndDate')}）"
                    for item in items[:20]
                    if isinstance(item, dict)
                ]
                blocks.append("现有目标（不要重复创建同名目标）：\n" + "\n".join(lines))
                if intent in {"TASK_PLAN", "COMPREHENSIVE_PLAN"}:
                    for goal in items[:5]:
                        stages = await _get(
                            context, client, f"goals/{goal['id']}/stages", {"page": 1, "size": 20}
                        )
                        blocks.append(
                            "已有阶段（可通过 stageId/stageVersion 引用）："
                            + json.dumps(
                                [
                                    {
                                        key: stage.get(key)
                                        for key in ("id", "version", "name", "goalId", "status")
                                    }
                                    for stage in stages.get("items", [])
                                ],
                                ensure_ascii=False,
                            )
                        )
            else:
                blocks.append("现有目标：无")
            tasks = await _get(context, client, "tasks", {"page": 1, "size": 50})
            task_items = tasks.get("items") if isinstance(tasks, dict) else None
            if isinstance(task_items, list) and task_items:
                task_lines = [
                    f"- ID={item.get('id')} version={item.get('version')} stageId={item.get('stageId')} {_short(item.get('name'))}（{item.get('status')}，优先级 {item.get('priority')}，截止 {item.get('dueDate')}）"
                    for item in task_items[:30]
                    if isinstance(item, dict)
                ]
                blocks.append("现有任务（不要重复创建同名任务）：\n" + "\n".join(task_lines))
            else:
                blocks.append("现有任务：无")

        if needs_plan:
            available = await _get(context, client, "tasks", {"page": 1, "size": 20})
            available_items = available.get("items", []) if isinstance(available, dict) else []
            blocks.append(
                "已有任务（taskId/taskVersion；仅展示首屏，未列出不代表不存在）："
                + json.dumps(
                    [
                        {
                            key: item.get(key)
                            for key in (
                                "id",
                                "version",
                                "name",
                                "status",
                                "priority",
                                "estimatedMinutes",
                                "assignmentStatus",
                                "weeklyPlanId",
                                "dailyPlanId",
                            )
                        }
                        for item in available_items
                        if item.get("status") != "COMPLETED"
                    ],
                    ensure_ascii=False,
                )
            )
            for item in [row for row in available_items if row.get("assignmentStatus") in {"WEEK", "DAY"}][
                :12
            ]:
                assignment = await _get(context, client, f"tasks/{item['id']}/assignment")
                blocks.append(
                    "已有任务安排（跨周或改期必须保留 assignmentId/assignmentVersion）："
                    + json.dumps(
                        {"taskId": item["id"], "assignment": assignment}, ensure_ascii=False
                    )
                )
            weeks = await _get(context, client, "weekly-plans", {"page": 1, "size": 10})
            week_items = weeks.get("items") if isinstance(weeks, dict) else None
            if isinstance(week_items, list) and week_items:
                week_lines = [
                    f"- 周计划 {_short(item.get('weekStartDate'))}（ID {item.get('id')}）"
                    for item in week_items[:10]
                    if isinstance(item, dict)
                ]
                blocks.append("现有周计划：\n" + "\n".join(week_lines))
            pending = await _get(context, client, "planning/pending-tasks", {"page": 1, "size": 20})
            pending_items = pending.get("items") if isinstance(pending, dict) else None
            if isinstance(pending_items, list) and pending_items:
                pending_lines = [
                    f"- {_short(item.get('name') or item.get('title'))}（原安排 {item.get('businessDate') or item.get('originalDate')}）"
                    for item in pending_items[:20]
                    if isinstance(item, dict)
                ]
                blocks.append("待重新安排任务：\n" + "\n".join(pending_lines))

        if needs_habit:
            habits = await _get(context, client, "habits", {"page": 1, "size": 20})
            habit_items = habits.get("items") if isinstance(habits, dict) else None
            if isinstance(habit_items, list) and habit_items:
                habit_lines = [
                    f"- {_short(item.get('name'))}（{item.get('status')}，{item.get('cycleType')}）"
                    for item in habit_items[:20]
                    if isinstance(item, dict)
                ]
                blocks.append("现有习惯（不要重复创建同名习惯）：\n" + "\n".join(habit_lines))
            else:
                blocks.append("现有习惯：无")
            categories = await _get(context, client, "habit-categories", {"page": 1, "size": 50})
            category_items = categories.get("items") if isinstance(categories, dict) else None
            if isinstance(category_items, list) and category_items:
                names = [
                    _short(item.get("name"), 20)
                    for item in category_items
                    if isinstance(item, dict)
                ]
                blocks.append("现有习惯分类：" + "、".join(names))
    except JavaApiError as error:
        logger.warning("agent_planning_context_unavailable code=%s", error.code)
        raise AgentError(error.status_code, error.code, error.message) from error
    except (TypeError, ValueError) as error:
        logger.warning("agent_planning_context_invalid type=%s", type(error).__name__)
        raise AgentError(
            502, "PLANNING_CONTEXT_INVALID", "业务上下文返回不完整，请稍后重试"
        ) from error
    if not blocks:
        return None
    while blocks and len(json.dumps(blocks, ensure_ascii=False)) > _MAX_CHARS:
        blocks.pop()
    return json.dumps(blocks, ensure_ascii=False) if blocks else None


async def verify_assignments(context: AgentContext, client: JavaApiClient, snapshot: dict) -> dict:
    """确认前填入权威的改期前信息；版本不一致时要求重新整理，不能静默覆盖。"""
    existing = [item for item in snapshot.get("taskAssignments", []) if item.get("taskId")]
    if len(existing) > 20:
        raise AgentError(422, "PLAN_ADJUSTMENT_TOO_LARGE", "每次最多调整20个已有任务，请分批整理")
    for item in existing:
        task = await _get(context, client, f"tasks/{item['taskId']}")
        if not isinstance(task, dict) or task.get("status") == "COMPLETED":
            raise AgentError(409, "PLAN_TASK_CHANGED", "待安排任务已变化或已完成，请重新整理")
        current = await _get(context, client, f"tasks/{item['taskId']}/assignment")
        if isinstance(current, dict) and current.get("id"):
            if str(item.get("assignmentId")) != str(current["id"]) \
                    or item.get("assignmentVersion") != current.get("version"):
                raise AgentError(409, "PLAN_ASSIGNMENT_CHANGED", "原任务安排已变化或缺少版本，请重新整理调整方案")
            week = await _get(context, client, f"weekly-plans/{current['weeklyPlanId']}")
            day = await _get(context, client, f"daily-plans/{current['dailyPlanId']}") if current.get("dailyPlanId") else None
            item["previousWeekStart"] = week.get("weekStartDate") if isinstance(week, dict) else None
            item["previousBusinessDate"] = day.get("planDate") if isinstance(day, dict) else None
        else:
            if item.get("assignmentId") is not None or item.get("assignmentVersion") is not None:
                raise AgentError(409, "PLAN_ASSIGNMENT_CHANGED", "原任务安排已失效，请重新整理")
            item["previousWeekStart"] = item["previousBusinessDate"] = None
        item["taskTitle"] = str(task.get("name") or task.get("title") or "已有任务")[:100]
    return snapshot
