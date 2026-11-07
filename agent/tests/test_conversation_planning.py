import asyncio
import json
import time
from contextlib import asynccontextmanager
from datetime import UTC, datetime, timedelta
from types import SimpleNamespace
from unittest.mock import AsyncMock

import pytest
from pydantic import SecretStr

from app.core.context import AgentContext
from app.core.errors import AgentError
from app.services import conversation_planning as planning
from app.services import turn_service, workflow_service
from app.tools.base import ToolExecution


def identity(permissions=None):
    return AgentContext("7", frozenset({"USER"}), frozenset(permissions if permissions is not None
        else {"agent:workflow:read", "agent:workflow:manage", "agent:chat:send"}),
        SecretStr("test-only"), "test-trace", time.monotonic() + 60)


@pytest.fixture
def flow(monkeypatch):
    context = identity()
    graph = SimpleNamespace(prepare=AsyncMock())
    db = SimpleNamespace(workflow_graph=graph)
    client = SimpleNamespace(get=AsyncMock(return_value=SimpleNamespace(data={
        "today": "2026-11-06", "timeZone": "Asia/Shanghai"})))
    current = {"run": {"latestPlanVersionId": None}, "plans": []}
    monkeypatch.setattr(planning, "refresh_context", AsyncMock(return_value=context))
    monkeypatch.setattr(planning, "_run", AsyncMock(return_value=(41, "COMPREHENSIVE_PLAN")))
    monkeypatch.setattr(planning, "_state", AsyncMock())
    monkeypatch.setattr(workflow_service, "detail", AsyncMock(return_value=current))
    monkeypatch.setattr(planning.planning_context_service, "collect", AsyncMock(return_value="真实业务背景"))
    monkeypatch.setattr(planning.planning_service, "generate_snapshot", AsyncMock(return_value={"goal": {"title": "Java 入门"}}))
    monkeypatch.setattr(workflow_service, "save_plan", AsyncMock(return_value=SimpleNamespace(
        id=81, version_no=1, summary="一个目标，三个阶段")))
    monkeypatch.setattr(workflow_service, "confirm_plan", AsyncMock())
    monkeypatch.setattr(workflow_service, "execute_commit", AsyncMock())
    return SimpleNamespace(context=context, db=db, client=client, current=current, graph=graph)


async def act(flow, action="draft", **values):
    args = {"action": action, "objective": "Java 入门", "summary": "按用户提供的时间拆解", **values}
    if action == "clarify" and "questions" not in args:
        args["questions"] = ["你目前有编程基础吗？", "每周大约能投入多少时间？"]
    return await planning.execute(flow.db, flow.context, flow.client, conversation_id=11,
        turn_id=31, arguments=json.dumps(args, ensure_ascii=False), messages=[
            {"role": "user", "content": "我想从零学习 Java，最终做出一个小项目"},
            {"role": "assistant", "content": "希望用多久，每天能投入多长时间？"},
            {"role": "user", "content": "三个月，工作日每天一小时，周末两小时"}],
        summary="此前明确希望完成一个小项目", slots=asyncio.Semaphore(1), emit=AsyncMock())


def test_transcript_keeps_followups_and_excludes_background():
    value = planning.transcript([
        {"role": "system", "content": "不应作为用户要求"},
        {"role": "user", "content": "【目标】学会 Java"},
        {"role": "user", "content": "【知识库检索资料，仅作背景】不要冒充用户偏好"},
        {"role": "assistant", "content": "每天能投入多久？"},
        {"role": "tool", "content": "内部工具结果"},
        {"role": "user", "content": "每天一小时"}], "希望做一个可运行的小项目")
    assert "【目标】学会 Java" in value and "每天一小时" in value
    assert "可运行的小项目" in value
    assert "不要冒充" not in value and "内部工具" not in value and "不应" not in value


async def test_clarification_never_generates_or_commits(flow):
    outcome = await act(flow, "clarify")
    assert not outcome.is_error and json.loads(outcome.content)["phase"] == "COLLECTING"
    planning.planning_service.generate_snapshot.assert_not_awaited()
    workflow_service.save_plan.assert_not_awaited()
    workflow_service.confirm_plan.assert_not_awaited()
    workflow_service.execute_commit.assert_not_awaited()


async def test_draft_uses_entire_conversation_and_waits_for_acceptance(flow):
    outcome = await act(flow)
    assert not outcome.is_error
    kwargs = planning.planning_service.generate_snapshot.await_args.kwargs
    assert "从零学习 Java" in kwargs["message"] and "三个月" in kwargs["message"]
    assert "每天一小时" in kwargs["message"] and "小项目" in kwargs["message"]
    assert kwargs["today"] == "2026-11-06" and kwargs["time_zone"] == "Asia/Shanghai"
    assert workflow_service.save_plan.await_args.kwargs["revision"] is False
    flow.graph.prepare.assert_awaited_once_with(7, 41, 81)
    workflow_service.confirm_plan.assert_not_awaited()
    workflow_service.execute_commit.assert_not_awaited()


async def test_revision_uses_previous_snapshot_in_same_run(flow):
    old = {"goal": {"title": "Java 入门"}, "tasks": [{"clientKey": "t1"}]}
    flow.current.update(run={"latestPlanVersionId": "80"}, plans=[{"id": "80", "snapshot": old}])
    await act(flow, summary="将周期调整为四个月")
    assert planning.planning_service.generate_snapshot.await_args.kwargs["previous"] == old
    assert workflow_service.save_plan.await_args.kwargs["revision"] is True
    assert workflow_service.save_plan.await_args.kwargs["run_id"] == 41


async def test_permission_is_rechecked_before_any_planning(flow):
    planning.refresh_context.return_value = identity({"agent:chat:send"})
    with pytest.raises(AgentError) as error:
        await act(flow)
    assert error.value.status_code == 403
    planning._run.assert_not_awaited()


async def test_accept_action_is_not_a_model_tool(flow):
    outcome = await act(flow, action="accept")
    assert outcome.is_error
    planning._run.assert_not_awaited()
    workflow_service.execute_commit.assert_not_awaited()


async def test_generation_failure_keeps_business_untouched(flow):
    planning.planning_service.generate_snapshot.side_effect = AgentError(422, "PLAN_INVALID", "请补充完成标准")
    outcome = await act(flow)
    assert outcome.is_error and "完成标准" in outcome.content
    assert planning._state.await_args.args[5] == "ERROR"
    workflow_service.save_plan.assert_not_awaited()
    workflow_service.execute_commit.assert_not_awaited()


async def test_cancellation_is_propagated_and_saved_as_interrupted(flow):
    planning.planning_service.generate_snapshot.side_effect = asyncio.CancelledError()
    with pytest.raises(asyncio.CancelledError):
        await act(flow)
    assert planning._state.await_args.args[5] == "INTERRUPTED"
    workflow_service.save_plan.assert_not_awaited()


@asynccontextmanager
async def empty_session():
    yield object()


async def test_accepted_run_cannot_be_silently_revised(monkeypatch):
    monkeypatch.setattr(workflow_service, "expire_stale", AsyncMock())
    monkeypatch.setattr(planning.conversation_repository, "require_owned", AsyncMock())
    monkeypatch.setattr(planning.workflow_repository, "find_active_run", AsyncMock(return_value=
        SimpleNamespace(id=41, user_id=7, intent="COMPREHENSIVE_PLAN", status="CONFIRMED")))
    with pytest.raises(AgentError) as error:
        await planning._run(SimpleNamespace(session=empty_session), identity(), 11, 31, "GOAL_PLAN")
    assert error.value.code == "WORKFLOW_ALREADY_ACCEPTED"


async def test_new_goal_keeps_room_for_followup_stages_tasks_and_schedule(monkeypatch):
    monkeypatch.setattr(workflow_service, "expire_stale", AsyncMock())
    monkeypatch.setattr(planning.conversation_repository, "require_owned", AsyncMock())
    monkeypatch.setattr(planning.workflow_repository, "find_active_run", AsyncMock(return_value=None))
    start = AsyncMock(return_value=SimpleNamespace(id=41, intent="COMPREHENSIVE_PLAN"))
    monkeypatch.setattr(workflow_service, "start_run", start)
    assert await planning._run(SimpleNamespace(session=empty_session), identity(), 11, 31, "GOAL_PLAN") == (41, "COMPREHENSIVE_PLAN")
    assert start.await_args.kwargs["intent"] == "COMPREHENSIVE_PLAN"


async def test_confirmation_is_blocked_while_conversation_turn_is_running(monkeypatch):
    run = SimpleNamespace(id=41, conversation_id=11, status="WAITING_CONFIRM")
    monkeypatch.setattr(workflow_service.repo, "find_run", AsyncMock(return_value=run))
    monkeypatch.setattr(workflow_service.turn_repository, "count_running", AsyncMock(return_value=1))
    with pytest.raises(AgentError) as error:
        await workflow_service.confirm_plan(SimpleNamespace(session=empty_session), user_id=7,
                                            run_id=41, plan_version_id=81)
    assert error.value.code == "PLAN_CONVERSATION_RUNNING"


async def test_stream_planning_is_bounded_to_once_per_turn(monkeypatch):
    context = identity()
    started = turn_service.StartedTurn(7, 11, 31, "test-request", 0, 1, 2, "test-model", 0,
        "", datetime.now(UTC) + timedelta(seconds=60))
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[{"role": "user", "content": "规划一个学习目标"}]))
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    monkeypatch.setattr(planning, "context_hint", AsyncMock(return_value=None))
    monkeypatch.setattr(turn_service, "_prepare_tools", AsyncMock(return_value=SimpleNamespace(
        next_assistant_id=3, result_message_nos=(4, 5))))
    monkeypatch.setattr(turn_service, "_record_tool_result", AsyncMock())
    monkeypatch.setattr(turn_service, "_finalize", AsyncMock(return_value=True))
    monkeypatch.setattr(planning, "execute", AsyncMock(return_value=ToolExecution(
        planning.TOOL_NAME, planning.TOOL_LABEL, '{"phase":"COLLECTING"}', "等待补充", False, 1)))

    class Stream:
        def __init__(self, chunks): self.chunks = iter(chunks)
        def __aiter__(self): return self
        async def __anext__(self):
            try: return next(self.chunks)
            except StopIteration: raise StopAsyncIteration from None
        async def close(self): return None

    calls = [SimpleNamespace(index=i, id=f"call_{i}", type="function", function=SimpleNamespace(
        name=planning.TOOL_NAME, arguments="{}")) for i in range(2)]
    def chunk(content, tools, reason):
        return SimpleNamespace(model="test-model", usage=None, choices=[SimpleNamespace(
            delta=SimpleNamespace(content=content, tool_calls=tools), finish_reason=reason)])
    create = AsyncMock(side_effect=[Stream([chunk(None, calls, "tool_calls")]),
                                  Stream([chunk("请告诉我每天能投入多久。", [], "stop")])])
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(
        chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    events = AsyncMock()
    reply = await turn_service._generate(SimpleNamespace(workflow_graph=object()), context,
        started, "规划一个学习目标", object(), asyncio.Semaphore(1), events)
    assert reply.finish_reason == "stop"
    planning.execute.assert_awaited_once()
    assert turn_service._record_tool_result.await_count == 2
    assert any(call.args[0] == "activity" for call in events.await_args_list)
