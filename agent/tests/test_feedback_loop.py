import asyncio
import json
import time
from contextlib import asynccontextmanager
from datetime import UTC, date, datetime, timedelta
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest
from pydantic import SecretStr

from app.core.config import Settings
from app.core.context import AgentContext
from app.core.errors import AgentError
from app.services import (
    background_recovery,
    conversation_planning,
    conversation_review,
    planning_context_service,
    turn_service,
    workflow_progress_service,
    workflow_service,
)
from app.services.tool_budget import ReadBudget
from app.services.tool_service import budget_exceeded
from app.tools import roadmap
from app.tools.base import ToolExecution
from app.tools.registry import TOOLS


def identity(permissions=None):
    values = {tool.permission for tool in TOOLS} | {"agent:workflow:read", "agent:workflow:manage",
        "agent:review:read", "agent:review:manage", "agent:run:read", "agent:memory:manage", "agent:chat:send"}
    return AgentContext("7", frozenset({"USER"}), frozenset(values if permissions is None else permissions),
        SecretStr("test-only"), "test-loop", time.monotonic() + 60)


def test_budget_reuses_equivalent_arguments_and_does_not_cache_errors():
    budget = ReadBudget(2, 2)
    result = ToolExecution("get_goal", "目标", '{"goal":1}', "已核对", False, 12)
    budget.remember("get_goal", '{"goalId":1,"extra":null}', result)
    cached = budget.cached("get_goal", '{ "extra":null, "goalId":1 }')
    assert cached and cached.duration_ms == 0 and "复用" in cached.summary
    assert budget.calls == 1 and budget.cached("get_goal", '{"goalId":2}') is None
    budget.remember("get_goal", '{"goalId":2}', ToolExecution("get_goal", "目标", "{}", "失败", True, 1))
    assert not budget.available and budget.cached("get_goal", '{"goalId":2}') is None


def test_default_request_deadline_allows_a_complete_turn(monkeypatch):
    monkeypatch.delenv("AGENT_REQUEST_TIMEOUT_SECONDS", raising=False)
    monkeypatch.delenv("AGENT_TURN_TIMEOUT_SECONDS", raising=False)
    settings = Settings(_env_file=None)
    assert settings.agent_request_timeout_seconds >= settings.agent_turn_timeout_seconds


def test_budget_limit_is_an_honest_partial_result_not_a_failed_query():
    result = budget_exceeded("get_stage")
    assert result.is_limited and not result.is_error
    assert json.loads(result.content)["status"] == "limited"
    assert "尚未查询" in result.summary


@pytest.mark.asyncio
async def test_roadmap_reads_all_three_stages_without_separate_list_calls():
    async def get(_context, _client, path, params=None):
        if path == "goals/1":
            return SimpleNamespace(data={"id": "1", "name": "学习", "stages": [
                {"id": str(value), "name": f"阶段{value}"} for value in range(2, 5)]}), None
        return SimpleNamespace(data={"id": path.split("/")[1], "name": "阶段", "tasks": [
            {"id": "9", "name": "练习", "status": "NOT_STARTED"}]}), None
    from unittest.mock import patch

    from app.tools.goals import GoalDetailArgs
    with patch.object(roadmap.shared, "get", new=AsyncMock(side_effect=get)) as query:
        value = await roadmap.run(identity(), object(), GoalDetailArgs(goalId=1))
    payload = json.loads(value.content)
    assert query.await_count == 4
    assert len(payload["stages"]) == 3 and not payload["partial"]
    assert all(row["taskCount"] == 1 for row in payload["stages"])


@pytest.mark.asyncio
async def test_roadmap_does_not_describe_unread_stage_as_empty(monkeypatch):
    query = AsyncMock(side_effect=[(SimpleNamespace(data={"id": "1", "name": "目标", "stages": [
        {"id": "2", "name": "阶段"}]}), None), (None, "暂不可用")])
    monkeypatch.setattr(roadmap.shared, "get", query)
    from app.tools.goals import GoalDetailArgs
    payload = json.loads((await roadmap.run(identity(), object(), GoalDetailArgs(goalId=1))).content)
    assert payload["partial"] and payload["stages"][0]["unavailable"]


@pytest.mark.parametrize(("period", "start", "end"), [
    ("THIS_WEEK", "2026-11-02", "2026-11-06"),
    ("LAST_WEEK", "2026-10-26", "2026-11-01"),
    ("LAST_7_DAYS", "2026-10-31", "2026-11-06"),
])
def test_review_uses_account_business_calendar(period, start, end):
    resolved = conversation_review.resolve_range(conversation_review.ReviewAction(period=period), date(2026, 11, 6))
    assert tuple(value.isoformat() for value in resolved) == (start, end)


def test_review_rejects_future_and_ambiguous_ranges():
    for action in [conversation_review.ReviewAction(period="CUSTOM", dateFrom="2026-11-01", dateTo="2026-11-07"),
                   conversation_review.ReviewAction(period="THIS_WEEK", dateFrom="2026-11-01")]:
        with pytest.raises(AgentError):
            conversation_review.resolve_range(action, date(2026, 11, 6))


@pytest.mark.asyncio
async def test_read_limit_cannot_take_away_the_planning_operation(monkeypatch):
    context = identity()
    started = turn_service.StartedTurn(7, 11, 31, "test-loop", 0, 1, 2, "test", 0, "", datetime.now(UTC) + timedelta(seconds=60))
    monkeypatch.setattr(turn_service, "get_settings", lambda: SimpleNamespace(agent_tool_max_calls=1,
        agent_tool_max_rounds=1, agent_tools_enabled=True, agent_partial_save_interval_seconds=10))
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[{"role": "user", "content": "查看并规划"}]))
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    monkeypatch.setattr(conversation_planning, "context_hint", AsyncMock(return_value=None))
    monkeypatch.setattr(turn_service, "_prepare_tools", AsyncMock(return_value=SimpleNamespace(next_assistant_id=3, result_message_nos=(4, 5, 6))))
    recorded = AsyncMock()
    monkeypatch.setattr(turn_service, "_record_tool_result", recorded)
    monkeypatch.setattr(turn_service, "_finalize", AsyncMock(return_value=True))
    read = AsyncMock(return_value=ToolExecution("get_goal", "目标", "{}", "已核对", False, 1))
    plan = AsyncMock(return_value=ToolExecution(conversation_planning.TOOL_NAME, "方案", "{}", "草案", False, 1))
    monkeypatch.setattr(turn_service.tool_service, "execute", read)
    monkeypatch.setattr(conversation_planning, "execute", plan)
    calls = [SimpleNamespace(index=index, id=f"c{index}", type="function", function=SimpleNamespace(name=name, arguments=args))
        for index, (name, args) in enumerate([("get_goal", '{"goalId":1}'), ("get_goal", '{"goalId":2}'), ("prepare_growth_plan", "{}")])]
    def chunk(content, tools, reason):
        return SimpleNamespace(model="test", usage=None, choices=[SimpleNamespace(delta=SimpleNamespace(content=content, tool_calls=tools), finish_reason=reason)])
    class Stream:
        def __init__(self, item): self.item = iter([item])
        def __aiter__(self): return self
        async def __anext__(self):
            try: return next(self.item)
            except StopIteration: raise StopAsyncIteration from None
        async def close(self): pass
    create = AsyncMock(side_effect=[Stream(chunk(None, calls, "tool_calls")), Stream(chunk("请核对右侧方案。", [], "stop"))])
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    events = AsyncMock()
    await turn_service._generate(SimpleNamespace(workflow_graph=object()), context, started, "查看并规划", object(), asyncio.Semaphore(1), events)
    read.assert_awaited_once()
    plan.assert_awaited_once()
    assert recorded.await_count == 3
    assert any(item.args[0] == "tool_end" and item.args[1]["status"] == "limited" for item in events.await_args_list)


@pytest.mark.asyncio
async def test_adjustment_preview_uses_authoritative_original_dates(monkeypatch):
    values = {"tasks/3": {"name": "真实任务", "status": "IN_PROGRESS"},
        "tasks/3/assignment": {"id": "8", "version": 2, "weeklyPlanId": "4", "dailyPlanId": "5"},
        "weekly-plans/4": {"weekStartDate": "2026-11-02"}, "daily-plans/5": {"planDate": "2026-11-06"}}
    monkeypatch.setattr(planning_context_service, "_get", AsyncMock(side_effect=lambda _ctx, _client, path: values[path]))
    snapshot = {"taskAssignments": [{"taskId": "3", "assignmentId": "8", "assignmentVersion": 2,
                                    "previousBusinessDate": "1999-01-01"}]}
    result = await planning_context_service.verify_assignments(identity(), object(), snapshot)
    row = result["taskAssignments"][0]
    assert row["previousBusinessDate"] == "2026-11-06" and row["taskTitle"] == "真实任务"


@pytest.mark.asyncio
async def test_adjustment_rejects_stale_assignment_before_it_can_be_accepted(monkeypatch):
    monkeypatch.setattr(planning_context_service, "_get", AsyncMock(side_effect=[
        {"name": "任务", "status": "IN_PROGRESS"}, {"id": "8", "version": 3}]))
    with pytest.raises(AgentError) as error:
        await planning_context_service.verify_assignments(identity(), object(), {"taskAssignments": [
            {"taskId": "3", "assignmentId": "8", "assignmentVersion": 2}]})
    assert error.value.code == "PLAN_ASSIGNMENT_CHANGED"


@pytest.mark.asyncio
async def test_task_only_plan_tracks_existing_goal_and_today_habit(monkeypatch):
    monkeypatch.setattr(workflow_service, "detail", AsyncMock(return_value={"run": {"latestPlanVersionId": "81"},
        "plans": [{"id": "81", "snapshot": {"taskAssignments": [{"taskId": "3"}]}}],
        "commit": {"status": "SUCCEEDED", "result": {"weeklyPlanIds": ["4"], "dailyPlanIds": ["5"], "habitIds": ["6"]}}}))
    values = {"dashboard/overview": {"today": "2026-11-06"},
        "tasks/3": {"id": "3", "name": "原任务", "stageId": "2", "status": "IN_PROGRESS", "dueDate": "2026-11-04"},
        "stages/2": {"id": "2", "goalId": "1", "status": "IN_PROGRESS", "progress": 25},
        "goals/1": {"id": "1", "name": "原目标", "progress": 25},
        "weekly-plans/4": {"id": "4", "weekStartDate": "2026-11-02", "weekEndDate": "2026-11-08"},
        "daily-plans/5": {"id": "5", "planDate": "2026-11-05"}, "habits/6": {"id": "6", "name": "练习", "status": "ACTIVE"},
        "weekly-plans/4/tasks": {"total": 2, "items": [{"status": "COMPLETED"}, {"status": "IN_PROGRESS"}]},
        "daily-plans/5/tasks": {"total": 1, "items": [{"status": "IN_PROGRESS"}]},
        "habits/today": {"total": 1, "items": [{"habitId": "6", "doneCount": 1}]}}
    client = SimpleNamespace(get=AsyncMock(side_effect=lambda path, **_kwargs: SimpleNamespace(data=values[path])))
    result = await workflow_progress_service.collect(object(), identity(), client, 41)
    assert result["groups"]["goals"][0]["name"] == "原目标"
    assert result["groups"]["habits"][0]["checkedToday"]
    assert result["groups"]["weeks"][0]["taskCompleted"] == 1
    assert result["signals"][0]["code"] == "OVERDUE" and not result["partial"]


@pytest.mark.asyncio
async def test_nonready_review_never_creates_or_accepts_an_adjustment(monkeypatch):
    monkeypatch.setattr(conversation_planning, "refresh_context", AsyncMock(return_value=identity()))
    monkeypatch.setattr(conversation_planning.review_service, "get", AsyncMock(return_value=SimpleNamespace(status="RUNNING")))
    run = AsyncMock()
    monkeypatch.setattr(conversation_planning, "_run", run)
    result = await conversation_planning.execute(object(), identity(), object(), conversation_id=11, turn_id=31,
        arguments=json.dumps({"action": "draft", "objective": "调整", "summary": "保留原任务", "intent": "WEEKLY_PLAN", "reviewId": 3}),
        messages=[], summary="", slots=asyncio.Semaphore(1), emit=AsyncMock())
    assert result.is_error and "尚未完成" in result.summary
    run.assert_not_awaited()


@pytest.mark.asyncio
async def test_memory_retry_returns_existing_later_task_instead_of_resetting_it(monkeypatch):
    old = SimpleNamespace(id=1, user_id=7, kind="MEMORY_EXTRACT", status="FAILED", ref_id=31, ref_type="CONVERSATION_TURN")
    turn = SimpleNamespace(user_id=7, status="COMPLETED", conversation_id=11)
    later = SimpleNamespace(id=2, status="RUNNING")
    session = SimpleNamespace(get=AsyncMock(side_effect=[old, turn]), scalar=AsyncMock(return_value=later))
    @asynccontextmanager
    async def sessions(): yield session
    monkeypatch.setattr(background_recovery.conversation_repository, "require_owned", AsyncMock())
    enqueue = AsyncMock()
    monkeypatch.setattr(background_recovery.tasks, "enqueue", enqueue)
    result = await background_recovery.retry_memory(SimpleNamespace(session=sessions), identity(), 1)
    assert result is later and later.status == "RUNNING"
    enqueue.assert_not_awaited()


@pytest.mark.asyncio
async def test_memory_retry_cannot_access_another_users_task():
    session = SimpleNamespace(get=AsyncMock(return_value=SimpleNamespace(user_id=8)))
    @asynccontextmanager
    async def sessions(): yield session
    with pytest.raises(AgentError) as error:
        await background_recovery.retry_memory(SimpleNamespace(session=sessions), identity(), 1)
    assert error.value.status_code == 404
