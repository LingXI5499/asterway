# 检查test执行反馈loop相关行为，断言失败时报告具体不符合预期的场景。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 contextlib 引入 异步资源进入和退出管理器（asynccontextmanager）。
from contextlib import asynccontextmanager

# 从 datetime 引入 协调世界时（UTC）、日期（date）、日期时间类型（datetime）、两个时刻之间的时间长度（timedelta）。
from datetime import UTC, date, datetime, timedelta

# 从 types 引入 可以按属性名读取的简单对象（SimpleNamespace）。
from types import SimpleNamespace

# 从 unittest.mock 引入 异步Mock（AsyncMock）。
from unittest.mock import AsyncMock

# 引入 现有测试运行与断言库（pytest），供下面调用其功能。
import pytest

# 从 pydantic 引入 默认显示时隐藏实际内容的私密文字类型（SecretStr）。
from pydantic import SecretStr

# 从 app.core.config 引入 配置（Settings）。
from app.core.config import Settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.services 引入 后台recovery（background_recovery）、对话规划（conversation_planning）、对话复盘（conversation_review）、规划上下文业务服务（planning_context_service）、本轮对话业务服务（turn_service）、规划工作流进度业务服务（workflow_progress_service）、规划工作流业务服务（workflow_service）。
from app.services import (
    # 继续传入或组合后台recovery（background_recovery），完成上面尚未结束的表达式。
    background_recovery,
    # 继续传入或组合对话规划（conversation_planning），完成上面尚未结束的表达式。
    conversation_planning,
    # 继续传入或组合对话复盘（conversation_review），完成上面尚未结束的表达式。
    conversation_review,
    # 继续传入或组合规划上下文业务服务（planning_context_service），完成上面尚未结束的表达式。
    planning_context_service,
    # 继续传入或组合本轮对话业务服务（turn_service），完成上面尚未结束的表达式。
    turn_service,
    # 继续传入或组合规划工作流进度业务服务（workflow_progress_service），完成上面尚未结束的表达式。
    workflow_progress_service,
    # 继续传入或组合规划工作流业务服务（workflow_service），完成上面尚未结束的表达式。
    workflow_service,
)

# 从 app.services.tool_budget 引入 读取额度（ReadBudget）。
from app.services.tool_budget import ReadBudget

# 从 app.services.tool_service 引入 额度exceeded（budget_exceeded）。
from app.services.tool_service import budget_exceeded

# 从 app.tools 引入 roadmap。
from app.tools import roadmap

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution

# 从 app.tools.registry 引入 业务工具列表（TOOLS）。
from app.tools.registry import TOOLS


# 函数 identity：调用 identity 完成账户管理处理；接收 权限列表（permissions）。
def identity(permissions=None):
    # 把按 {tool.permission for tool in TOOLS} | {'agent:workflow:read', 'agent:workflow:manage', 'agent:review:read', 'agent:review:manage', 'agent:run:read', 'agent:memory:manage', 'agent:chat:send'} 计算的结果保存到 值列表（values）。
    values = {tool.permission for tool in TOOLS} | {"agent:workflow:read", "agent:workflow:manage",
        # 填写成长助手（agent）字段，值为表达式 review:read", "agent:review:manage", "agent:run:read", "agent:memory:manage", "agent:chat:send"} 的结果。
        "agent:review:read", "agent:review:manage", "agent:run:read", "agent:memory:manage", "agent:chat:send"}
    # 把执行“调用 AgentContext 完成成长助手上下文处理”得到的结果交还给调用方，本函数到此结束。
    return AgentContext("7", frozenset({"USER"}), frozenset(values if permissions is None else permissions),
        # 继续传入或组合调用 SecretStr("test-only"), "test-loop", time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
        SecretStr("test-only"), "test-loop", time.monotonic() + 60)


# 函数 test_budget_reuses_equivalent_arguments_and_does_not_cache_errors：调用 test_budget_reuses_equivalent_arguments_and_does_not_cache_errors 完成test额度reusesequivalentargumentsanddoesnot缓存错误处理器处理。
def test_budget_reuses_equivalent_arguments_and_does_not_cache_errors():
    # 把执行“读取 额度（Budget）”得到的结果保存到 额度（budget）。
    budget = ReadBudget(2, 2)
    # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果保存到 结果（result）。
    result = ToolExecution("get_goal", "目标", '{"goal":1}', "已核对", False, 12)
    # 累计一次查询；只有成功且未受限的结果才写入本轮缓存。
    budget.remember("get_goal", '{"goalId":1,"extra":null}', result)
    # 把执行“按工具名与标准化参数读取本轮缓存；命中后返回可复用的结果副本”得到的结果保存到 缓存结果（cached）。
    cached = budget.cached("get_goal", '{ "extra":null, "goalId":1 }')
    # 断言条件 cached and cached.duration_ms == 0 and ('复用' in cached.summary) 是否成立；不满足时本项检查失败。
    assert cached and cached.duration_ms == 0 and "复用" in cached.summary
    # 断言budget.calls == 1 and budget.cached('get_goal', '{"goalId":2}') is None，即对应内容是否不存在；不满足时本项检查失败。
    assert budget.calls == 1 and budget.cached("get_goal", '{"goalId":2}') is None
    # 累计一次查询；只有成功且未受限的结果才写入本轮缓存。
    budget.remember("get_goal", '{"goalId":2}', ToolExecution("get_goal", "目标", "{}", "失败", True, 1))
    # 断言not budget.available and budget.cached('get_goal', '{"goalId":2}') is None，即对应内容是否不存在；不满足时本项检查失败。
    assert not budget.available and budget.cached("get_goal", '{"goalId":2}') is None


# 函数 test_default_request_deadline_allows_a_complete_turn：调用 test_default_request_deadline_allows_a_complete_turn 完成test默认值请求截止时间allowsa完成本轮对话处理；接收 monkeypatch。
def test_default_request_deadline_allows_a_complete_turn(monkeypatch):
    # 调用 monkeypatch.delenv 完成delenv处理。
    monkeypatch.delenv("AGENT_REQUEST_TIMEOUT_SECONDS", raising=False)
    # 调用 monkeypatch.delenv 完成delenv处理。
    monkeypatch.delenv("AGENT_TURN_TIMEOUT_SECONDS", raising=False)
    # 把执行“调用 Settings 完成配置处理”得到的结果保存到 配置（settings）。
    settings = Settings(_env_file=None)
    # 断言条件 settings.agent_request_timeout_seconds >= settings.agent_turn_timeout_seconds 是否成立；不满足时本项检查失败。
    assert settings.agent_request_timeout_seconds >= settings.agent_turn_timeout_seconds


# 函数 test_budget_limit_is_an_honest_partial_result_not_a_failed_query：调用 test_budget_limit_is_an_honest_partial_result_not_a_failed_query 完成test额度上限是否anhonest部分结果结果nota失败查询条件处理。
def test_budget_limit_is_an_honest_partial_result_not_a_failed_query():
    # 把执行“调用 budget_exceeded 完成额度exceeded处理”得到的结果保存到 结果（result）。
    result = budget_exceeded("get_stage")
    # 断言条件 result.is_limited and (not result.is_error) 是否成立；不满足时本项检查失败。
    assert result.is_limited and not result.is_error
    # 断言条件 json.loads(result.content)['status'] == 'limited' 是否成立；不满足时本项检查失败。
    assert json.loads(result.content)["status"] == "limited"
    # 断言条件 '尚未查询' in result.summary 是否成立；不满足时本项检查失败。
    assert "尚未查询" in result.summary


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_roadmap_reads_all_three_stages_without_separate_list_calls：调用 test_roadmap_reads_all_three_stages_without_separate_list_calls 完成testroadmapreads全部three阶段列表withoutseparate列表调用列表处理。
async def test_roadmap_reads_all_three_stages_without_separate_list_calls():
    # 异步函数 get：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；接收 上下文（_context）、客户端（_client）、路径（path）、参数集合（params）。
    async def get(_context, _client, path, params=None):
        # 检查条件 path == 'goals/1' 是否成立；成立时执行缩进的分支。
        if path == "goals/1":
            # 把依次包含 执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
            return SimpleNamespace(data={"id": "1", "name": "学习", "stages": [
                # 继续传入或组合表达式 {"id": str(value), "name": f"阶段{value}"} for value in range(2, 5)]}), None 的结果，完成上面尚未结束的表达式。
                {"id": str(value), "name": f"阶段{value}"} for value in range(2, 5)]}), None
        # 把依次包含 执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
        return SimpleNamespace(data={"id": path.split("/")[1], "name": "阶段", "tasks": [
            # 继续传入或组合表达式 {"id": "9", "name": "练习", "status": "NOT_STARTED"}]}), None 的结果，完成上面尚未结束的表达式。
            {"id": "9", "name": "练习", "status": "NOT_STARTED"}]}), None

    # 从 unittest.mock 引入 patch。
    from unittest.mock import patch

    # 从 app.tools.goals 引入 目标详情参数（GoalDetailArgs）。
    from app.tools.goals import GoalDetailArgs
    # 进入执行“调用 patch.object 完成对象处理”得到的结果，命名为 query；退出时由上下文管理器提交、回滚或释放资源。
    with patch.object(roadmap.shared, "get", new=AsyncMock(side_effect=get)) as query:
        # 把等待执行“调用 roadmap.run 完成方案运行处理”得到的结果完成后的结果保存到 值（value）。
        value = await roadmap.run(identity(), object(), GoalDetailArgs(goalId=1))
    # 把执行“把 JSON 文字解析为对象”得到的结果保存到 提交数据（payload）。
    payload = json.loads(value.content)
    # 断言条件 query.await_count == 4 是否成立；不满足时本项检查失败。
    assert query.await_count == 4
    # 断言条件 len(payload['stages']) == 3 and (not payload['partial']) 是否成立；不满足时本项检查失败。
    assert len(payload["stages"]) == 3 and not payload["partial"]
    # 断言条件 all((row['taskCount'] == 1 for row in payload['stages'])) 是否成立；不满足时本项检查失败。
    assert all(row["taskCount"] == 1 for row in payload["stages"])


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_roadmap_does_not_describe_unread_stage_as_empty：调用 test_roadmap_does_not_describe_unread_stage_as_empty 完成testroadmapdoesnotdescribeunread阶段作为空状态处理；接收 monkeypatch。
async def test_roadmap_does_not_describe_unread_stage_as_empty(monkeypatch):
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 查询条件（query）。
    query = AsyncMock(side_effect=[(SimpleNamespace(data={"id": "1", "name": "目标", "stages": [
        # 继续传入或组合表达式 {"id": "2", "name": "阶段"}]}), None), (None, "暂不可用")]) 的结果，完成上面尚未结束的表达式。
        {"id": "2", "name": "阶段"}]}), None), (None, "暂不可用")])
    # 设置 attr 的值。
    monkeypatch.setattr(roadmap.shared, "get", query)

    # 从 app.tools.goals 引入 目标详情参数（GoalDetailArgs）。
    from app.tools.goals import GoalDetailArgs
    # 把执行“把 JSON 文字解析为对象”得到的结果保存到 提交数据（payload）。
    payload = json.loads((await roadmap.run(identity(), object(), GoalDetailArgs(goalId=1))).content)
    # 断言条件 payload['partial'] and payload['stages'][0]['unavailable'] 是否成立；不满足时本项检查失败。
    assert payload["partial"] and payload["stages"][0]["unavailable"]


# 用这里列出的多组输入分别执行同一个检查。
@pytest.mark.parametrize(("period", "start", "end"), [
    # 继续传入或组合表达式 ("THIS_WEEK", "2026-11-02", "2026-11-06") 的结果，完成上面尚未结束的表达式。
    ("THIS_WEEK", "2026-11-02", "2026-11-06"),
    # 继续传入或组合表达式 ("LAST_WEEK", "2026-10-26", "2026-11-01") 的结果，完成上面尚未结束的表达式。
    ("LAST_WEEK", "2026-10-26", "2026-11-01"),
    # 继续传入或组合表达式 ("LAST_7_DAYS", "2026-10-31", "2026-11-06") 的结果，完成上面尚未结束的表达式。
    ("LAST_7_DAYS", "2026-10-31", "2026-11-06"),
])
# 函数 test_review_uses_account_business_calendar：调用 test_review_uses_account_business_calendar 完成test复盘uses账号businesscalendar处理；接收 统计周期（period）、开始（start）、结束（end）。
def test_review_uses_account_business_calendar(period, start, end):
    # 把执行“确定 区间（range）”得到的结果保存到 resolved。
    resolved = conversation_review.resolve_range(conversation_review.ReviewAction(period=period), date(2026, 11, 6))
    # 断言条件 tuple((value.isoformat() for value in resolved)) == (start, end) 是否成立；不满足时本项检查失败。
    assert tuple(value.isoformat() for value in resolved) == (start, end)


# 函数 test_review_rejects_future_and_ambiguous_ranges：调用 test_review_rejects_future_and_ambiguous_ranges 完成test复盘rejectsfutureandambiguousranges处理。
def test_review_rejects_future_and_ambiguous_ranges():
    # 从依次包含 执行“调用 conversation_review.ReviewAction 完成复盘操作处理”得到的结果、执行“调用 conversation_review.ReviewAction 完成复盘操作处理”得到的结果 的集合依次取出操作（action），每项执行一次下面的处理。
    for action in [conversation_review.ReviewAction(period="CUSTOM", dateFrom="2026-11-01", dateTo="2026-11-07"),
                   # 继续传入或组合调用 conversation_review.ReviewAction 完成复盘操作处理，完成上面尚未结束的表达式。
                   conversation_review.ReviewAction(period="THIS_WEEK", dateFrom="2026-11-01")]:
        # 进入执行“调用 pytest.raises 完成raises处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
        with pytest.raises(AgentError):
            # 确定 区间（range）。
            conversation_review.resolve_range(action, date(2026, 11, 6))


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_read_limit_cannot_take_away_the_planning_operation：调用 test_read_limit_cannot_take_away_the_planning_operation 完成test读取上限cannottakeawaythe规划操作处理；接收 monkeypatch。
async def test_read_limit_cannot_take_away_the_planning_operation(monkeypatch):
    # 把执行“调用 identity 完成账户管理处理”得到的结果保存到 上下文（context）。
    context = identity()
    # 把执行“调用 turn_service.StartedTurn 完成开始时本轮对话处理”得到的结果保存到 开始时（started）。
    started = turn_service.StartedTurn(7, 11, 31, "test-loop", 0, 1, 2, "test", 0, "", datetime.now(UTC) + timedelta(seconds=60))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "get_settings", lambda: SimpleNamespace(agent_tool_max_calls=1,
        # 传入成长助手业务工具最大rounds（agent_tool_max_rounds），值为表达式 1, agent_tools_enabled=True, agent_partial_save_interval_seconds=10)) 的结果。
        agent_tool_max_rounds=1, agent_tools_enabled=True, agent_partial_save_interval_seconds=10))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_messages", AsyncMock(return_value=[{"role": "user", "content": "查看并规划"}]))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "refresh_context", AsyncMock(return_value=context))
    # 设置 attr 的值。
    monkeypatch.setattr(conversation_planning, "context_hint", AsyncMock(return_value=None))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_prepare_tools", AsyncMock(return_value=SimpleNamespace(next_assistant_id=3, result_message_nos=(4, 5, 6))))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 recorded。
    recorded = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_record_tool_result", recorded)
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "_finalize", AsyncMock(return_value=True))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 读取（read）。
    read = AsyncMock(return_value=ToolExecution("get_goal", "目标", "{}", "已核对", False, 1))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 方案（plan）。
    plan = AsyncMock(return_value=ToolExecution(conversation_planning.TOOL_NAME, "方案", "{}", "草案", False, 1))
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service.tool_service, "execute", read)
    # 设置 attr 的值。
    monkeypatch.setattr(conversation_planning, "execute", plan)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 调用列表（calls）。
    calls = [SimpleNamespace(index=index, id=f"c{index}", type="function", function=SimpleNamespace(name=name, arguments=args))
        # 继续传入或组合表达式 for index, (name, args) in enumerate([("get_goal", '{"goalId":1}'), ("get_goal", '{"goalId":2}'), ("prepare_growth_plan"… 的结果，完成上面尚未结束的表达式。
        for index, (name, args) in enumerate([("get_goal", '{"goalId":1}'), ("get_goal", '{"goalId":2}'), ("prepare_growth_plan", "{}")])]
    # 函数 chunk：调用 chunk 完成文档片段处理；接收 内容（content）、业务工具列表（tools）、原因（reason）。
    def chunk(content, tools, reason):
        # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果交还给调用方，本函数到此结束。
        return SimpleNamespace(model="test", usage=None, choices=[SimpleNamespace(delta=SimpleNamespace(content=content, tool_calls=tools), finish_reason=reason)])
    # 定义 Stream，把分段回复流需要的数据与操作放在同一个对象中。
    class Stream:
        # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 单条数据（item）。
        # 把执行“调用 iter 完成iter处理”得到的结果保存到 当前实例中的单条数据（self.item）。
        def __init__(self, item): self.item = iter([item])
        # 函数 __aiter__：调用 __aiter__ 完成aiter处理。
        # 把当前实例（self）交还给调用方，本函数到此结束。
        def __aiter__(self): return self
        # 异步函数 __anext__：调用 __anext__ 完成anext处理。
        async def __anext__(self):
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            # 把执行“调用 next 完成下一个处理”得到的结果交还给调用方，本函数到此结束。
            try: return next(self.item)
            # 捕获StopIteration异常，执行下面的错误处理。
            # 中止本轮正常处理，抛出 停止异步Iteration（StopAsyncIteration）。
            except StopIteration: raise StopAsyncIteration from None
        # 异步函数 close：释放连接、文件或客户端占用的资源。
        # 这里不执行额外操作，只保留合法的空分支或占位结构。
        async def close(self): pass
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 创建（create）。
    create = AsyncMock(side_effect=[Stream(chunk(None, calls, "tool_calls")), Stream(chunk("请核对右侧方案。", [], "stop"))])
    # 设置 attr 的值。
    monkeypatch.setattr(turn_service, "get_llm_client", lambda: SimpleNamespace(chat=SimpleNamespace(completions=SimpleNamespace(create=create))))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 事件列表（events）。
    events = AsyncMock()
    # 等待调用 turn_service._generate 完成generate处理完成后再继续。
    await turn_service._generate(SimpleNamespace(workflow_graph=object()), context, started, "查看并规划", object(), asyncio.Semaphore(1), events)
    # 断言 awaitedonce（awaited_once）。
    read.assert_awaited_once()
    # 断言 awaitedonce（awaited_once）。
    plan.assert_awaited_once()
    # 断言条件 recorded.await_count == 3 是否成立；不满足时本项检查失败。
    assert recorded.await_count == 3
    # 断言条件 any((item.args[0] == 'tool_end' and item.args[1]['status'] == 'limited' for item in events.await_args_list)) 是否成立；不满足时本项检查失败。
    assert any(item.args[0] == "tool_end" and item.args[1]["status"] == "limited" for item in events.await_args_list)


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_adjustment_preview_uses_authoritative_original_dates：调用 test_adjustment_preview_uses_authoritative_original_dates 完成testadjustmentpreviewusesauthoritative原始dates处理；接收 monkeypatch。
async def test_adjustment_preview_uses_authoritative_original_dates(monkeypatch):
    # 把按字段名保存多个值的字典保存到 值列表（values）。
    values = {"tasks/3": {"name": "真实任务", "status": "IN_PROGRESS"},
        # 继续传入或组合按表达式计算得到的值："tasks/3/assignment": {"id": "8", "version": 2, "weeklyPlanId": "4", "dailyPlanId": "5"}，完成上面尚未结束的表达式。
        "tasks/3/assignment": {"id": "8", "version": 2, "weeklyPlanId": "4", "dailyPlanId": "5"},
        # 继续传入或组合按表达式计算得到的值："weekly-plans/4": {"weekStartDate": "2026-11-02"}, "daily-plans/5": {"planDate": "2026-11-06"}}，完成上面尚未结束的表达式。
        "weekly-plans/4": {"weekStartDate": "2026-11-02"}, "daily-plans/5": {"planDate": "2026-11-06"}}
    # 设置 attr 的值。
    monkeypatch.setattr(planning_context_service, "_get", AsyncMock(side_effect=lambda _ctx, _client, path: values[path]))
    # 把按字段名保存多个值的字典保存到 确认时的数据快照（snapshot）。
    snapshot = {"taskAssignments": [{"taskId": "3", "assignmentId": "8", "assignmentVersion": 2,
                                    # 填写上一次Business日期（previousBusinessDate）字段，值为表达式 "1999-01-01"}]} 的结果。
                                    "previousBusinessDate": "1999-01-01"}]}
    # 把等待执行“调用 planning_context_service.verify_assignments 完成校验任务安排列表处理”得到的结果完成后的结果保存到 结果（result）。
    result = await planning_context_service.verify_assignments(identity(), object(), snapshot)
    # 把从从结果（result）中按固定文字 'taskAssignments'取出的内容中按固定数值 0取出的内容保存到 当前记录（row）。
    row = result["taskAssignments"][0]
    # 断言条件 row['previousBusinessDate'] == '2026-11-06' and row['taskTitle'] == '真实任务' 是否成立；不满足时本项检查失败。
    assert row["previousBusinessDate"] == "2026-11-06" and row["taskTitle"] == "真实任务"


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_adjustment_rejects_stale_assignment_before_it_can_be_accepted：调用 test_adjustment_rejects_stale_assignment_before_it_can_be_accepted 完成testadjustmentrejectsstale任务安排变化前it是否可以beaccepted处理；接收 monkeypatch。
async def test_adjustment_rejects_stale_assignment_before_it_can_be_accepted(monkeypatch):
    # 设置 attr 的值。
    monkeypatch.setattr(planning_context_service, "_get", AsyncMock(side_effect=[
        # 继续传入或组合表达式 {"name": "任务", "status": "IN_PROGRESS"}, {"id": "8", "version": 3}])) 的结果，完成上面尚未结束的表达式。
        {"name": "任务", "status": "IN_PROGRESS"}, {"id": "8", "version": 3}]))
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(AgentError) as error:
        # 等待调用 planning_context_service.verify_assignments 完成校验任务安排列表处理完成后再继续。
        await planning_context_service.verify_assignments(identity(), object(), {"taskAssignments": [
            # 继续传入或组合表达式 {"taskId": "3", "assignmentId": "8", "assignmentVersion": 2}]}) 的结果，完成上面尚未结束的表达式。
            {"taskId": "3", "assignmentId": "8", "assignmentVersion": 2}]})
    # 断言条件 error.value.code == 'PLAN_ASSIGNMENT_CHANGED' 是否成立；不满足时本项检查失败。
    assert error.value.code == "PLAN_ASSIGNMENT_CHANGED"


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_task_only_plan_tracks_existing_goal_and_today_habit：调用 test_task_only_plan_tracks_existing_goal_and_today_habit 完成test任务仅方案tracksexisting目标and今天习惯处理；接收 monkeypatch。
async def test_task_only_plan_tracks_existing_goal_and_today_habit(monkeypatch):
    # 设置 attr 的值。
    monkeypatch.setattr(workflow_service, "detail", AsyncMock(return_value={"run": {"latestPlanVersionId": "81"},
        # 填写方案列表（plans）字段，值为表达式 [{"id": "81", "snapshot": {"taskAssignments": [{"taskId": "3"}]}}] 的结果。
        "plans": [{"id": "81", "snapshot": {"taskAssignments": [{"taskId": "3"}]}}],
        # 填写提交回执（commit）字段，值为表达式 {"status": "SUCCEEDED", "result": {"weeklyPlanIds": ["4"], "dailyPlanIds": ["5"], "habitIds": ["6"]}}})) 的结果。
        "commit": {"status": "SUCCEEDED", "result": {"weeklyPlanIds": ["4"], "dailyPlanIds": ["5"], "habitIds": ["6"]}}}))
    # 把按字段名保存多个值的字典保存到 值列表（values）。
    values = {"dashboard/overview": {"today": "2026-11-06"},
        # 继续传入或组合按表达式计算得到的值："tasks/3": {"id": "3", "name": "原任务", "stageId": "2", "status": "IN_PROGRESS", "dueDate": "2026-11-04"}，完成上面尚未结束的表达式。
        "tasks/3": {"id": "3", "name": "原任务", "stageId": "2", "status": "IN_PROGRESS", "dueDate": "2026-11-04"},
        # 继续传入或组合按表达式计算得到的值："stages/2": {"id": "2", "goalId": "1", "status": "IN_PROGRESS", "progress": 25}，完成上面尚未结束的表达式。
        "stages/2": {"id": "2", "goalId": "1", "status": "IN_PROGRESS", "progress": 25},
        # 继续传入或组合按表达式计算得到的值："goals/1": {"id": "1", "name": "原目标", "progress": 25}，完成上面尚未结束的表达式。
        "goals/1": {"id": "1", "name": "原目标", "progress": 25},
        # 继续传入或组合按表达式计算得到的值："weekly-plans/4": {"id": "4", "weekStartDate": "2026-11-02", "weekEndDate": "2026-11-08"}，完成上面尚未结束的表达式。
        "weekly-plans/4": {"id": "4", "weekStartDate": "2026-11-02", "weekEndDate": "2026-11-08"},
        # 继续传入或组合按表达式计算得到的值："daily-plans/5": {"id": "5", "planDate": "2026-11-05"}, "habits/6": {"id": "6", "name": "练习", "status": "ACTIVE"}，完成上面尚未结束的表达式。
        "daily-plans/5": {"id": "5", "planDate": "2026-11-05"}, "habits/6": {"id": "6", "name": "练习", "status": "ACTIVE"},
        # 继续传入或组合按表达式计算得到的值："weekly-plans/4/tasks": {"total": 2, "items": [{"status": "COMPLETED"}, {"status": "IN_PROGRESS"}]}，完成上面尚未结束的表达式。
        "weekly-plans/4/tasks": {"total": 2, "items": [{"status": "COMPLETED"}, {"status": "IN_PROGRESS"}]},
        # 继续传入或组合按表达式计算得到的值："daily-plans/5/tasks": {"total": 1, "items": [{"status": "IN_PROGRESS"}]}，完成上面尚未结束的表达式。
        "daily-plans/5/tasks": {"total": 1, "items": [{"status": "IN_PROGRESS"}]},
        # 继续传入或组合按表达式计算得到的值："habits/today": {"total": 1, "items": [{"habitId": "6", "doneCount": 1}]}}，完成上面尚未结束的表达式。
        "habits/today": {"total": 1, "items": [{"habitId": "6", "doneCount": 1}]}}
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 客户端（client）。
    client = SimpleNamespace(get=AsyncMock(side_effect=lambda path, **_kwargs: SimpleNamespace(data=values[path])))
    # 把等待执行“调用 workflow_progress_service.collect 完成collect处理”得到的结果完成后的结果保存到 结果（result）。
    result = await workflow_progress_service.collect(object(), identity(), client, 41)
    # 断言条件 result['groups']['goals'][0]['name'] == '原目标' 是否成立；不满足时本项检查失败。
    assert result["groups"]["goals"][0]["name"] == "原目标"
    # 断言条件 result['groups']['habits'][0]['checkedToday'] 是否成立；不满足时本项检查失败。
    assert result["groups"]["habits"][0]["checkedToday"]
    # 断言条件 result['groups']['weeks'][0]['taskCompleted'] == 1 是否成立；不满足时本项检查失败。
    assert result["groups"]["weeks"][0]["taskCompleted"] == 1
    # 断言条件 result['signals'][0]['code'] == 'OVERDUE' and (not result['partial']) 是否成立；不满足时本项检查失败。
    assert result["signals"][0]["code"] == "OVERDUE" and not result["partial"]


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_nonready_review_never_creates_or_accepts_an_adjustment：调用 test_nonready_review_never_creates_or_accepts_an_adjustment 完成testnonready复盘nevercreatesoracceptsanadjustment处理；接收 monkeypatch。
async def test_nonready_review_never_creates_or_accepts_an_adjustment(monkeypatch):
    # 设置 attr 的值。
    monkeypatch.setattr(conversation_planning, "refresh_context", AsyncMock(return_value=identity()))
    # 设置 attr 的值。
    monkeypatch.setattr(conversation_planning.review_service, "get", AsyncMock(return_value=SimpleNamespace(status="RUNNING")))
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 方案运行（run）。
    run = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(conversation_planning, "_run", run)
    # 把等待执行“执行传入的数据库语句并取得结果”得到的结果完成后的结果保存到 结果（result）。
    result = await conversation_planning.execute(object(), identity(), object(), conversation_id=11, turn_id=31,
        # 传入arguments，值为把对象编码为 JSON 文字。
        arguments=json.dumps({"action": "draft", "objective": "调整", "summary": "保留原任务", "intent": "WEEKLY_PLAN", "reviewId": 3}),
        # 传入消息列表（messages），值为调用 [], summary="", slots=asyncio.Semaphore 完成并发名额控制器处理。
        messages=[], summary="", slots=asyncio.Semaphore(1), emit=AsyncMock())
    # 断言条件 result.is_error and '尚未完成' in result.summary 是否成立；不满足时本项检查失败。
    assert result.is_error and "尚未完成" in result.summary
    # 断言 notawaited（not_awaited）。
    run.assert_not_awaited()


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_memory_retry_returns_existing_later_task_instead_of_resetting_it：调用 test_memory_retry_returns_existing_later_task_instead_of_resetting_it 完成test长期记忆重试returnsexistinglater任务insteadofresettingit处理；接收 monkeypatch。
async def test_memory_retry_returns_existing_later_task_instead_of_resetting_it(monkeypatch):
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 旧（old）。
    old = SimpleNamespace(id=1, user_id=7, kind="MEMORY_EXTRACT", status="FAILED", ref_id=31, ref_type="CONVERSATION_TURN")
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 本轮对话（turn）。
    turn = SimpleNamespace(user_id=7, status="COMPLETED", conversation_id=11)
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 later。
    later = SimpleNamespace(id=2, status="RUNNING")
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 当前会话（session）。
    session = SimpleNamespace(get=AsyncMock(side_effect=[old, turn]), scalar=AsyncMock(return_value=later))
    # 让这个异步生成器管理资源的进入和退出。
    @asynccontextmanager
    # 异步函数 sessions：调用 sessions 完成会话列表处理。
    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
    async def sessions(): yield session
    # 设置 attr 的值。
    monkeypatch.setattr(background_recovery.conversation_repository, "require_owned", AsyncMock())
    # 把执行“调用 AsyncMock 完成异步Mock处理”得到的结果保存到 enqueue。
    enqueue = AsyncMock()
    # 设置 attr 的值。
    monkeypatch.setattr(background_recovery.tasks, "enqueue", enqueue)
    # 把等待执行“重试 长期记忆（memory）”得到的结果完成后的结果保存到 结果（result）。
    result = await background_recovery.retry_memory(SimpleNamespace(session=sessions), identity(), 1)
    # 断言条件 result is later and later.status == 'RUNNING' 是否成立；不满足时本项检查失败。
    assert result is later and later.status == "RUNNING"
    # 断言 notawaited（not_awaited）。
    enqueue.assert_not_awaited()


# 让现有检查运行器等待下面的异步测试函数完成。
@pytest.mark.asyncio
# 异步函数 test_memory_retry_cannot_access_another_users_task：调用 test_memory_retry_cannot_access_another_users_task 完成test长期记忆重试cannot访问权限another用户列表任务处理。
async def test_memory_retry_cannot_access_another_users_task():
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果保存到 当前会话（session）。
    session = SimpleNamespace(get=AsyncMock(return_value=SimpleNamespace(user_id=8)))
    # 让这个异步生成器管理资源的进入和退出。
    @asynccontextmanager
    # 异步函数 sessions：调用 sessions 完成会话列表处理。
    # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
    async def sessions(): yield session
    # 进入执行“调用 pytest.raises 完成raises处理”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
    with pytest.raises(AgentError) as error:
        # 等待重试 长期记忆（memory）完成后再继续。
        await background_recovery.retry_memory(SimpleNamespace(session=sessions), identity(), 1)
    # 断言条件 error.value.status_code == 404 是否成立；不满足时本项检查失败。
    assert error.value.status_code == 404
