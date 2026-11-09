# 检查test对话规划相关行为，断言失败时报告具体不符合预期的场景。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 contextlib 引入 异步资源进入和退出管理器（asynccontextmanager）。
from contextlib import asynccontextmanager

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）、两个时刻之间的时间长度（timedelta）。
from datetime import UTC, datetime, timedelta

# 从 types 引入 可以按属性名读取的简单对象（SimpleNamespace）。
from types import SimpleNamespace

# 从 unittest.mock 引入 异步Mock（AsyncMock）。
from unittest.mock import AsyncMock

# 引入 现有测试运行与断言库（pytest），供下面调用其功能。
import pytest

# 从 pydantic 引入 默认显示时隐藏实际内容的私密文字类型（SecretStr）。
from pydantic import SecretStr

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.services 引入 规划（planning）。
from app.services import conversation_planning as planning

# 从 app.services 引入 本轮对话业务服务（turn_service）、规划工作流业务服务（workflow_service）。
from app.services import turn_service, workflow_service

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution


# 函数 identity：调用 identity 完成账户管理处理；接收 权限列表（permissions）。
def identity(permissions=None):
    # 把执行“调用 AgentContext 完成成长助手上下文处理”得到的结果交还给调用方，本函数到此结束。
    return AgentContext("7", frozenset({"USER"}), frozenset(permissions if permissions is not None
        # 继续传入或组合表达式 else {"agent:workflow:read", "agent:workflow:manage", "agent:chat:send"}) 的结果，完成上面尚未结束的表达式。
        else {"agent:workflow:read", "agent:workflow:manage", "agent:chat:send"}),
        # 继续传入或组合调用 SecretStr("test-only"), "test-trace", time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
        SecretStr("test-only"), "test-trace", time.monotonic() + 60)


# 声明供现有检查复用的准备数据或替代对象。
@pytest.fixture
# 函数 flow：调用 flow 完成流程处理；接收 monkeypatch。
def flow(monkeypatch):
    # 把执行“调用 identity 完成账户管理处理”得到的结果保存到 上下文（context）。
    context = identity()
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 工作流图（graph）。
    graph = SimpleNamespace(prepare=AsyncMock())
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 Agent 数据库访问器（db）。
    db = SimpleNamespace(workflow_graph=graph)
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 客户端（client）。
    client = SimpleNamespace(get=AsyncMock(return_value=SimpleNamespace(data={
        # 填写今天（today）字段，值为按表达式计算得到的值："2026-11-06", "timeZone": "Asia/Shanghai"})))。
        "today": "2026-11-06", "timeZone": "Asia/Shanghai"})))
    # 把按字段名保存多个值的字典保存到 当前（current）。
    current = {"run": {"latestPlanVersionId": None}, "plans": []}
    # 设置 attr 的值。
    monkeypatch.setattr(planning, "refresh_context", AsyncMock(return_value=context))
    # 设置 attr 的值。
    monkeypatch.setattr(planning, "_run", AsyncMock(return_value=(41, "COMPREHENSIVE_PLAN")))
    # 设置 attr 的值。
    monkeypatch.setattr(planning, "_state", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "detail", AsyncMock(return_value=current))
    # 设置 attr 的值。
    monkeypatch.setattr(planning.planning_context_service, "collect", AsyncMock(return_value="真实业务背景"))
    # 设置 attr 的值。
    monkeypatch.setattr(planning.planning_service, "generate_snapshot", AsyncMock(return_value={"goal": {"title": "Java 入门"}}))
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "save_plan", AsyncMock(return_value=SimpleNamespace(
        # 传入编号（id），值为表达式 81, version_no=1, summary="一个目标，三个阶段"))) 的结果。
        id=81, version_no=1, summary="一个目标，三个阶段")))
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "confirm_plan", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "execute_commit", AsyncMock())
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果交还给调用方，本函数到此结束。
    return SimpleNamespace(context=context, db=db, client=client, current=current, graph=graph)


# 异步函数 act：调用 act 完成act处理；接收 流程（flow）、操作（action）。
async def act(flow, action="draft", **values):
    # 把按字段名保存多个值的字典保存到 参数（args）。
    args = {"action": action, "objective": "Java 入门", "summary": "按用户提供的时间拆解", **values}
    # 检查条件 action == 'clarify' and 'questions' not in args 是否成立；成立时执行缩进的分支。
    if action == "clarify" and "questions" not in args:
        # 把依次包含 固定文字 '你目前有编程基础吗？'、固定文字 '每周大约能投入多少时间？' 的集合保存到 参数questions（args['questions']）。
        args["questions"] = ["你目前有编程基础吗？", "每周大约能投入多少时间？"]
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await planning.execute(flow.db, flow.context, flow.client, conversation_id=11,
        # 传入本轮对话编号（turn_id），值为把对象编码为 JSON 文字。
        turn_id=31, arguments=json.dumps(args, ensure_ascii=False), messages=[
            # 继续传入或组合表达式 {"role": "user", "content": "我想从零学习 Java，最终做出一个小项目"} 的结果，完成上面尚未结束的表达式。
            {"role": "user", "content": "我想从零学习 Java，最终做出一个小项目"},
            # 继续传入或组合表达式 {"role": "assistant", "content": "希望用多久，每天能投入多长时间？"} 的结果，完成上面尚未结束的表达式。
            {"role": "assistant", "content": "希望用多久，每天能投入多长时间？"},
            # 继续传入或组合表达式 {"role": "user", "content": "三个月，工作日每天一小时，周末两小时"}] 的结果，完成上面尚未结束的表达式。
            {"role": "user", "content": "三个月，工作日每天一小时，周末两小时"}],
        # 传入汇总（summary），值为调用 "此前明确希望完成一个小项目", slots=asyncio.Semaphore 完成并发名额控制器处理。
        summary="此前明确希望完成一个小项目", slots=asyncio.Semaphore(1), emit=AsyncMock())


# 函数 test_transcript_keeps_followups_and_excludes_background：调用 test_transcript_keeps_followups_and_excludes_background 完成testtranscriptkeepsfollowupsandexcludes后台处理。
def test_transcript_keeps_followups_and_excludes_background():
    # 把执行“调用 planning.transcript 完成transcript处理”得到的结果保存到 值（value）。
    value = planning.transcript([
        # 继续传入或组合表达式 {"role": "system", "content": "不应作为用户要求"} 的结果，完成上面尚未结束的表达式。
        {"role": "system", "content": "不应作为用户要求"},
        # 继续传入或组合表达式 {"role": "user", "content": "【目标】学会 Java"} 的结果，完成上面尚未结束的表达式。
        {"role": "user", "content": "【目标】学会 Java"},
        # 继续传入或组合表达式 {"role": "user", "content": "【知识库检索资料，仅作背景】不要冒充用户偏好"} 的结果，完成上面尚未结束的表达式。
        {"role": "user", "content": "【知识库检索资料，仅作背景】不要冒充用户偏好"},
        # 继续传入或组合表达式 {"role": "assistant", "content": "每天能投入多久？"} 的结果，完成上面尚未结束的表达式。
        {"role": "assistant", "content": "每天能投入多久？"},
        # 继续传入或组合表达式 {"role": "tool", "content": "内部工具结果"} 的结果，完成上面尚未结束的表达式。
        {"role": "tool", "content": "内部工具结果"},
        # 继续传入或组合表达式 {"role": "user", "content": "每天一小时"}], "希望做一个可运行的小项目") 的结果，完成上面尚未结束的表达式。
        {"role": "user", "content": "每天一小时"}], "希望做一个可运行的小项目")
    # 断言条件 '【目标】学会 Java' in value and '每天一小时' in value 是否成立；不满足时本项检查失败。
    assert "【目标】学会 Java" in value and "每天一小时" in value
    # 断言条件 '可运行的小项目' in value 是否成立；不满足时本项检查失败。
    assert "可运行的小项目" in value
    # 断言条件 '不要冒充' not in value and '内部工具' not in value and ('不应' not in value) 是否成立；不满足时本项检查失败。
    assert "不要冒充" not in value and "内部工具" not in value and "不应" not in value


# 异步函数 test_clarification_never_generates_or_commits：调用 test_clarification_never_generates_or_commits 完成testclarificationnevergeneratesorcommits处理；接收 流程（flow）。
async def test_clarification_never_generates_or_commits(flow):
    # 把等待执行“调用 act 完成act处理”得到的结果完成后的结果保存到 outcome。
    outcome = await act(flow, "clarify")
    # 断言条件 not outcome.is_error and json.loads(outcome.content)['phase'] == 'COLLECTING' 是否成立；不满足时本项检查失败。
    assert not outcome.is_error and json.loads(outcome.content)["phase"] == "COLLECTING"
    # 断言 notawaited（not_awaited）。
    planning.planning_service.generate_snapshot.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.save_plan.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.confirm_plan.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.execute_commit.assert_not_awaited()


# 异步函数 test_draft_uses_entire_conversation_and_waits_for_acceptance：调用 test_draft_uses_entire_conversation_and_waits_for_acceptance 完成test草案usesentire对话andwaitsforacceptance处理；接收 流程（flow）。
async def test_draft_uses_entire_conversation_and_waits_for_acceptance(flow):
    # 把等待执行“调用 act 完成act处理”得到的结果完成后的结果保存到 outcome。
    outcome = await act(flow)
    # 断言条件 not outcome.is_error 是否成立；不满足时本项检查失败。
    assert not outcome.is_error
    # 把规划（planning）的规划业务服务（planning_service）属性的generate确认时的数据快照（generate_snapshot）属性的await参数（await_args）属性的kwargs属性保存到 kwargs。
    kwargs = planning.planning_service.generate_snapshot.await_args.kwargs
    # 断言条件 '从零学习 Java' in kwargs['message'] and '三个月' in kwargs['message'] 是否成立；不满足时本项检查失败。
    assert "从零学习 Java" in kwargs["message"] and "三个月" in kwargs["message"]
    # 断言条件 '每天一小时' in kwargs['message'] and '小项目' in kwargs['message'] 是否成立；不满足时本项检查失败。
    assert "每天一小时" in kwargs["message"] and "小项目" in kwargs["message"]
    # 断言条件 kwargs['today'] == '2026-11-06' and kwargs['time_zone'] == 'Asia/Shanghai' 是否成立；不满足时本项检查失败。
    assert kwargs["today"] == "2026-11-06" and kwargs["time_zone"] == "Asia/Shanghai"
    # 断言条件 workflow_service.save_plan.await_args.kwargs['revision'] is False 是否成立；不满足时本项检查失败。
    assert workflow_service.save_plan.await_args.kwargs["revision"] is False
    # 断言 awaitedoncewith（awaited_once_with）。
    flow.graph.prepare.assert_awaited_once_with(7, 41, 81)
    # 断言 notawaited（not_awaited）。
    workflow_service.confirm_plan.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.execute_commit.assert_not_awaited()


# 异步函数 test_revision_uses_previous_snapshot_in_same_run：调用 test_revision_uses_previous_snapshot_in_same_run 完成test修订版本uses上一次确认时的数据快照insame方案运行处理；接收 流程（flow）。
async def test_revision_uses_previous_snapshot_in_same_run(flow):
    # 把按字段名保存多个值的字典保存到 旧（old）。
    old = {"goal": {"title": "Java 入门"}, "tasks": [{"clientKey": "t1"}]}
    # 调用 flow.current.update 完成更新处理。
    flow.current.update(run={"latestPlanVersionId": "80"}, plans=[{"id": "80", "snapshot": old}])
    # 等待调用 act 完成act处理完成后再继续。
    await act(flow, summary="将周期调整为四个月")
    # 断言条件 planning.planning_service.generate_snapshot.await_args.kwargs['previous'] == old 是否成立；不满足时本项检查失败。
    assert planning.planning_service.generate_snapshot.await_args.kwargs["previous"] == old
    # 断言条件 workflow_service.save_plan.await_args.kwargs['revision'] is True 是否成立；不满足时本项检查失败。
    assert workflow_service.save_plan.await_args.kwargs["revision"] is True
    # 断言条件 workflow_service.save_plan.await_args.kwargs['run_id'] == 41 是否成立；不满足时本项检查失败。
    assert workflow_service.save_plan.await_args.kwargs["run_id"] == 41


# 异步函数 test_permission_is_rechecked_before_any_planning：调用 test_permission_is_rechecked_before_any_planning 完成test权限是否rechecked变化前any规划处理；接收 流程（flow）。
async def test_permission_is_rechecked_before_any_planning(flow):
    # 把执行“调用 identity 完成账户管理处理”得到的结果保存到 规划中的刷新上下文中的返回值（planning.refresh_context.return_value）。
    planning.refresh_context.return_value = identity({"agent:chat:send"})
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(AgentError) as error:
        # 等待调用 act 完成act处理完成后再继续。
        await act(flow)
    # 断言条件 error.value.status_code == 403 是否成立；不满足时本项检查失败。
    assert error.value.status_code == 403
    # 断言 notawaited（not_awaited）。
    planning._run.assert_not_awaited()


# 异步函数 test_accept_action_is_not_a_model_tool：调用 test_accept_action_is_not_a_model_tool 完成test接受操作是否nota模型业务工具处理；接收 流程（flow）。
async def test_accept_action_is_not_a_model_tool(flow):
    # 把等待执行“调用 act 完成act处理”得到的结果完成后的结果保存到 outcome。
    outcome = await act(flow, action="accept")
    # 断言条件 outcome.is_error 是否成立；不满足时本项检查失败。
    assert outcome.is_error
    # 断言 notawaited（not_awaited）。
    planning._run.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.execute_commit.assert_not_awaited()


# 异步函数 test_generation_failure_keeps_business_untouched：调用 test_generation_failure_keeps_business_untouched 完成testgeneration失败keepsbusinessuntouched处理；接收 流程（flow）。
async def test_generation_failure_keeps_business_untouched(flow):
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果保存到 规划中的规划业务服务中的generate确认时的数据快照中的sideeffect（planning.planning_service.generate_snapshot.side_effect）。
    planning.planning_service.generate_snapshot.side_effect = AgentError(422, "PLAN_INVALID", "请补充完成标准")
    # 把等待执行“调用 act 完成act处理”得到的结果完成后的结果保存到 outcome。
    outcome = await act(flow)
    # 断言条件 outcome.is_error and '完成标准' in outcome.content 是否成立；不满足时本项检查失败。
    assert outcome.is_error and "完成标准" in outcome.content
    # 断言条件 planning._state.await_args.args[5] == 'ERROR' 是否成立；不满足时本项检查失败。
    assert planning._state.await_args.args[5] == "ERROR"
    # 断言 notawaited（not_awaited）。
    workflow_service.save_plan.assert_not_awaited()
    # 断言 notawaited（not_awaited）。
    workflow_service.execute_commit.assert_not_awaited()


# 异步函数 test_cancellation_is_propagated_and_saved_as_interrupted：调用 test_cancellation_is_propagated_and_saved_as_interrupted 完成testcancellation是否propagatedand已保存作为interrupted处理；接收 流程（flow）。
async def test_cancellation_is_propagated_and_saved_as_interrupted(flow):
    # 把执行“取消 led错误（ledError）”得到的结果保存到 规划中的规划业务服务中的generate确认时的数据快照中的sideeffect（planning.planning_service.generate_snapshot.side_effect）。
    planning.planning_service.generate_snapshot.side_effect = asyncio.CancelledError()
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(asyncio.CancelledError):
        # 等待调用 act 完成act处理完成后再继续。
        await act(flow)
    # 断言条件 planning._state.await_args.args[5] == 'INTERRUPTED' 是否成立；不满足时本项检查失败。
    assert planning._state.await_args.args[5] == "INTERRUPTED"
    # 断言 notawaited（not_awaited）。
    workflow_service.save_plan.assert_not_awaited()


# 让这个异步生成器管理资源的进入和退出。
@asynccontextmanager
# 异步函数 empty_session：调用 empty_session 完成空状态会话处理。
async def empty_session():
    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
    yield object()


# 异步函数 test_accepted_run_cannot_be_silently_revised：调用 test_accepted_run_cannot_be_silently_revised 完成testaccepted方案运行cannotbesilentlyrevised处理；接收 monkeypatch。
async def test_accepted_run_cannot_be_silently_revised(monkeypatch):
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "expire_stale", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(planning.conversation_repository, "require_owned", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(planning.workflow_repository, "find_active_run", AsyncMock(return_value=
        # 继续传入或组合调用 SimpleNamespace 完成可以按属性名读取的简单对象处理，完成上面尚未结束的表达式。
        SimpleNamespace(id=41, user_id=7, intent="COMPREHENSIVE_PLAN", status="CONFIRMED")))
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(AgentError) as error:
        # 等待调用 planning._run 完成方案运行处理完成后再继续。
        await planning._run(SimpleNamespace(session=empty_session), identity(), 11, 31, "GOAL_PLAN")
    # 断言条件 error.value.code == 'WORKFLOW_ALREADY_ACCEPTED' 是否成立；不满足时本项检查失败。
    assert error.value.code == "WORKFLOW_ALREADY_ACCEPTED"


# 异步函数 test_new_goal_keeps_room_for_followup_stages_tasks_and_schedule：调用 test_new_goal_keeps_room_for_followup_stages_tasks_and_schedule 完成test新目标keepsroomforfollowup阶段列表任务列表and安排处理；接收 monkeypatch。
async def test_new_goal_keeps_room_for_followup_stages_tasks_and_schedule(monkeypatch):
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "expire_stale", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(planning.conversation_repository, "require_owned", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(planning.workflow_repository, "find_active_run", AsyncMock(return_value=None))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 开始（start）。
    start = AsyncMock(return_value=SimpleNamespace(id=41, intent="COMPREHENSIVE_PLAN"))
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "start_run", start)
    # 断言条件 await planning._run(SimpleNamespace(session=empty_session), identity(), 11, 31, 'GOAL_PLAN') == (41, 'COMPREHENSIVE_PLAN') 是否成立；不满足时本项检查失败。
    assert await planning._run(SimpleNamespace(session=empty_session), identity(), 11, 31, "GOAL_PLAN") == (41, "COMPREHENSIVE_PLAN")
    # 断言条件 start.await_args.kwargs['intent'] == 'COMPREHENSIVE_PLAN' 是否成立；不满足时本项检查失败。
    assert start.await_args.kwargs["intent"] == "COMPREHENSIVE_PLAN"


# 异步函数 test_confirmation_is_blocked_while_conversation_turn_is_running：调用 test_confirmation_is_blocked_while_conversation_turn_is_running 完成testconfirmation是否blockedwhile对话本轮对话是否运行中处理；接收 monkeypatch。
async def test_confirmation_is_blocked_while_conversation_turn_is_running(monkeypatch):
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 方案运行（run）。
    run = SimpleNamespace(id=41, conversation_id=11, status="WAITING_CONFIRM")
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service.repo, "find_run", AsyncMock(return_value=run))
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service.turn_repository, "count_running", AsyncMock(return_value=1))
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(AgentError) as error:
        # 等待确认 方案（plan）完成后再继续。
        await workflow_service.confirm_plan(SimpleNamespace(session=empty_session), user_id=7,
                                            # 传入方案运行编号（run_id），值为表达式 41, plan_version_id=81) 的结果。
                                            run_id=41, plan_version_id=81)
    # 断言条件 error.value.code == 'PLAN_CONVERSATION_RUNNING' 是否成立；不满足时本项检查失败。
    assert error.value.code == "PLAN_CONVERSATION_RUNNING"


# 异步函数 test_stream_planning_is_bounded_to_once_per_turn：调用 test_stream_planning_is_bounded_to_once_per_turn 完成test分段回复流规划是否boundedtoonceper本轮对话处理；接收 monkeypatch。
async def test_stream_planning_is_bounded_to_once_per_turn(monkeypatch):
    # 把执行“调用 identity 完成账户管理处理”得到的结果保存到 上下文（context）。
    context = identity()
    # 把执行“调用 turn_service.StartedTurn 完成开始时本轮对话处理”得到的结果保存到 开始时（started）。
    started = turn_service.StartedTurn(7, 11, 31, "test-request", 0, 1, 2, "test-model", 0,
        # 继续传入或组合当前真实时间，用于记录操作发生的时刻，完成上面尚未结束的表达式。
        "", datetime.now(UTC) + timedelta(seconds=60))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[{"role": "user", "content": "规划一个学习目标"}]))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    # 设置 attr 的值。
    monkeypatch.setattr(planning, "context_hint", AsyncMock(return_value=None))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_prepare_tools", AsyncMock(return_value=SimpleNamespace(
        # 传入下一个assistant编号（next_assistant_id），值为表达式 3, result_message_nos=(4, 5)))) 的结果。
        next_assistant_id=3, result_message_nos=(4, 5))))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_record_tool_result", AsyncMock())
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_finalize", AsyncMock(return_value=True))
    # 设置 attr 的值。
    monkeypatch.setattr(planning, "execute", AsyncMock(return_value=ToolExecution(
        # 继续传入或组合表达式 planning.TOOL_NAME, planning.TOOL_LABEL, '{"phase":"COLLECTING"}', "等待补充", False, 1))) 的结果，完成上面尚未结束的表达式。
        planning.TOOL_NAME, planning.TOOL_LABEL, '{"phase":"COLLECTING"}', "等待补充", False, 1)))

    # 定义 Stream，把分段回复流需要的数据与操作放在同一个对象中。
    class Stream:
        # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 文档片段列表（chunks）。
        # 把执行“调用 iter 完成iter处理”得到的结果保存到 当前实例中的文档片段列表（self.chunks）。
        def __init__(self, chunks): self.chunks = iter(chunks)
        # 函数 __aiter__：调用 __aiter__ 完成aiter处理。
        # 把当前实例（self）交还给调用方，本函数到此结束。
        def __aiter__(self): return self
        # 异步函数 __anext__：调用 __anext__ 完成anext处理。
        async def __anext__(self):
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            # 把执行“调用 next 完成下一个处理”得到的结果交还给调用方，本函数到此结束。
            try: return next(self.chunks)
            # 捕获StopIteration异常，执行下面的错误处理。
            # 中止本轮正常处理，抛出 停止异步Iteration（StopAsyncIteration）。
            except StopIteration: raise StopAsyncIteration from None
        # 异步函数 close：释放连接、文件或客户端占用的资源。
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        async def close(self): return None

    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 调用列表（calls）。
    calls = [SimpleNamespace(index=i, id=f"call_{i}", type="function", function=SimpleNamespace(
        # 传入名称（name），值为表达式 planning.TOOL_NAME, arguments="{}")) for i in range(2)] 的结果。
        name=planning.TOOL_NAME, arguments="{}")) for i in range(2)]
    # 函数 chunk：调用 chunk 完成文档片段处理；接收 内容（content）、业务工具列表（tools）、原因（reason）。
    def chunk(content, tools, reason):
        # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果交还给调用方，本函数到此结束。
        return SimpleNamespace(model="test-model", usage=None, choices=[SimpleNamespace(
            # 传入delta，值为调用 SimpleNamespace 完成可以按属性名读取的简单对象处理。
            delta=SimpleNamespace(content=content, tool_calls=tools), finish_reason=reason)])
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 创建（create）。
    create = AsyncMock(side_effect=[Stream([chunk(None, calls, "tool_calls")]),
                                  # 继续传入或组合调用 Stream 完成分段回复流处理，完成上面尚未结束的表达式。
                                  Stream([chunk("请告诉我每天能投入多久。", [], "stop")])])
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(
        # 传入对话（chat），值为调用 SimpleNamespace 完成可以按属性名读取的简单对象处理。
        chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 事件列表（events）。
    events = AsyncMock()
    # 把等待执行“调用 turn_service._generate 完成generate处理”得到的结果完成后的结果保存到 reply。
    reply = await turn_service._generate(SimpleNamespace(workflow_graph=object()), context,
        # 继续传入或组合调用 started, "规划一个学习目标", object(), asyncio.Semaphore 完成并发名额控制器处理，完成上面尚未结束的表达式。
        started, "规划一个学习目标", object(), asyncio.Semaphore(1), events)
    # 断言条件 reply.finish_reason == 'stop' 是否成立；不满足时本项检查失败。
    assert reply.finish_reason == "stop"
    # 断言 awaitedonce（awaited_once）。
    planning.execute.assert_awaited_once()
    # 断言条件 turn_service._record_tool_result.await_count == 2 是否成立；不满足时本项检查失败。
    assert turn_service._record_tool_result.await_count == 2
    # 断言条件 any((call.args[0] == 'activity' for call in events.await_args_list)) 是否成立；不满足时本项检查失败。
    assert any(call.args[0] == "activity" for call in events.await_args_list)
