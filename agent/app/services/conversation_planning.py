# 把聊天中的规划意图整理成可审阅草案，未经用户确认不创建业务数据。
# 对话内的规划工具：持续澄清、生成和修订草案；从不确认或提交业务数据。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 collections.abc 引入 Awaitable、Callable。
from collections.abc import Awaitable, Callable

# 从 datetime 引入 两个时刻之间的时间长度（timedelta）。
from datetime import timedelta

# 从 typing 引入 Any、只能从列出的固定值中选择的类型（Literal）。
from typing import Any, Literal

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、Validation错误（ValidationError）、模型validator（model_validator）。
from pydantic import BaseModel, ConfigDict, Field, ValidationError, model_validator

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）、刷新上下文（refresh_context）。
from app.core.context import AgentContext, refresh_context

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 从 app.llm.chat_service 引入 把底层异常转换成脱敏的可读错误（safe_error）。
from app.llm.chat_service import safe_error

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、规划工作流数据仓库（workflow_repository）。
from app.repositories import conversation_repository, workflow_repository

# 从 app.services 引入 规划上下文业务服务（planning_context_service）、规划业务服务（planning_service）、复盘业务服务（review_service）、规划工作流业务服务（workflow_service）。
from app.services import (
    # 继续传入或组合规划上下文业务服务（planning_context_service），完成上面尚未结束的表达式。
    planning_context_service,
    # 继续传入或组合规划业务服务（planning_service），完成上面尚未结束的表达式。
    planning_service,
    # 继续传入或组合复盘业务服务（review_service），完成上面尚未结束的表达式。
    review_service,
    # 继续传入或组合规划工作流业务服务（workflow_service），完成上面尚未结束的表达式。
    workflow_service,
)

# 从 app.services.workflow_graph 引入 require工作流图（require_graph）。
from app.services.workflow_graph import require_graph

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution

# 把固定文字 'prepare_growth_plan'保存到 业务工具名称（TOOL_NAME）。
TOOL_NAME = "prepare_growth_plan"
# 把固定文字 '整理成长方案'保存到 业务工具显示名称（TOOL_LABEL）。
TOOL_LABEL = "整理成长方案"
# 把从Callable中按依次包含 依次包含 str、从dict中按依次包含 str、Any 的集合取出的内容 的集合、从Awaitable中按空值，表示当前还没有对应内容取出的内容 的集合取出的内容保存到 向父组件发送事件的函数（Emit）。
Emit = Callable[[str, dict[str, Any]], Awaitable[None]]
# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)
# 把从只能从列出的固定值中选择的类型（Literal）中按依次包含 固定状态或编码 GOAL_PLAN（目标方案）、固定状态或编码 STAGE_PLAN（阶段方案）、固定状态或编码 TASK_PLAN（任务方案）、固定状态或编码 WEEKLY_PLAN（每周方案）、固定状态或编码 DAILY_PLAN（每日方案）等内容 的集合取出的内容保存到 Intent。
Intent = Literal[
    # 继续传入或组合固定文字 'GOAL_PLAN", "STAGE_PLAN", "TASK_PLAN", "WEEKLY_PLAN", "DAILY_PLAN", "HABIT_PLAN'，完成上面尚未结束的表达式。
    "GOAL_PLAN", "STAGE_PLAN", "TASK_PLAN", "WEEKLY_PLAN", "DAILY_PLAN", "HABIT_PLAN",
    # 继续传入或组合固定状态或编码 COMPREHENSIVE_PLAN（综合方案），完成上面尚未结束的表达式。
    "COMPREHENSIVE_PLAN",
]


# 定义 PlanningAction，把规划操作需要的数据与操作放在同一个对象中。
class PlanningAction(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)
    # 声明 操作（action），类型为 Literal['clarify', 'draft']。
    action: Literal["clarify", "draft"]
    # 声明 objective，类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    objective: str = Field(min_length=1, max_length=200)
    # 声明 intent，类型为 Intent；其内容为固定状态或编码 COMPREHENSIVE_PLAN（综合方案）。
    intent: Intent = "COMPREHENSIVE_PLAN"
    # 声明 汇总（summary），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    summary: str = Field(min_length=1, max_length=1000)
    # 声明 questions，类型为 list[str]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    questions: list[str] = Field(default_factory=list, max_length=3)
    # 声明 复盘编号（reviewId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    reviewId: int | None = Field(default=None, ge=1, description="据已完成复盘调整安排时填写真实复盘ID")

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 check_questions：核对 questions。
    def check_questions(self):
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 当前实例中的questions（self.questions）。
        self.questions = [question.strip() for question in self.questions]
        # 检查条件 any((not question or len(question) > 300 for question in self.questions)) 是否成立；成立时执行缩进的分支。
        if any(not question or len(question) > 300 for question in self.questions):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("澄清问题必须为 1 至 300 字")
        # 检查条件 self.action == 'clarify' and (not self.questions) 是否成立；成立时执行缩进的分支。
        if self.action == "clarify" and not self.questions:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("需要澄清时必须给出具体问题")
        # 检查条件 self.action == 'draft' and self.questions 是否成立；成立时执行缩进的分支。
        if self.action == "draft" and self.questions:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("尚有关键问题时先澄清，不生成最终草案")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 函数 definition：调用 definition 完成definition处理。
def definition() -> dict[str, Any]:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"type": "function", "function": {
        # 填写名称（name）字段，值为业务工具名称（TOOL_NAME）。
        "name": TOOL_NAME,
        # 填写说明（description）字段，值为表达式 ( 的结果。
        "description": (
            # 继续传入或组合固定文字 '在连续对话中整理用户的成长规划。用户提出新目标或补充、修改当前规划要求时使用。'，完成上面尚未结束的表达式。
            "在连续对话中整理用户的成长规划。用户提出新目标或补充、修改当前规划要求时使用。"
            "缺少目标成果、基础、时间或投入约束时 action=clarify，每次询问一到两个关键问题；"
            "信息足够或用户明确让你采用默认安排时 action=draft，系统会根据完整对话生成可核对的结构化方案。"
            "新目标及其阶段、任务和日程通常使用 COMPREHENSIVE_PLAN；只有明确针对已有对象时选对应意图。"
            "据复盘调整时填写reviewId且使用WEEKLY_PLAN或DAILY_PLAN，复用已有任务与安排版本。"
            "普通闲聊、只读查询、查看进度、用户拒绝规划时不要调用。每轮最多调用一次。"
            "该工具只能整理草案，不能接受、同意、确认、创建或执行任何业务内容。"
        ),
        # 填写parameters字段，值为调用 PlanningAction.model_json_schema 完成模型JSON数据数据库分区处理。
        "parameters": PlanningAction.model_json_schema(),
    }}


# 函数 available：判断本轮已用调用次数和轮数是否都还未达到各自上限；接收 Agent 数据库访问器（db）、上下文（context）。
def available(db: Database, context: AgentContext) -> bool:
    # 把执行“调用 context.allows 完成allows处理”得到的结果并且条件 getattr(db, 'workflow_graph', None) is not None 是否成立交还给调用方，本函数到此结束。
    return context.allows(frozenset({"agent:workflow:read", "agent:workflow:manage"})) \
        and getattr(db, "workflow_graph", None) is not None


# 函数 transcript：只携带已装入上下文的对话；查询结果和检索资料不冒充用户的规划要求。；接收 消息列表（messages）、汇总（summary）、上限（limit）。
def transcript(messages: list[dict[str, Any]], summary: str = "", limit: int = 9000) -> str:
    # 只携带已装入上下文的对话；查询结果和检索资料不冒充用户的规划要求。
    # 把空列表或集合保存到 parts。
    parts = []
    # 把固定数值 0保存到 used。
    used = 0
    # 从执行“调用 reversed 完成reversed处理”得到的结果依次取出消息（message），每项执行一次下面的处理。
    for message in reversed(messages):
        # 把依次包含 执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果、执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果 的集合保存到 角色内容（(role, content)）。
        role, content = message.get("role"), message.get("content")
        # 检查条件 role not in {'user', 'assistant'} or not isinstance(content, str) 是否成立；成立时执行缩进的分支。
        if role not in {"user", "assistant"} or not isinstance(content, str):
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 检查条件 not content.strip() or content.startswith(('【自动生成的历史摘要', '【已确认的长期记忆', '【知识库检索资料', '【历史加载说明', '【当前规划状态', '【成长复盘背景')) or message.get('tool_calls') 是否成立；成立时执行缩进的分支。
        if not content.strip() or content.startswith(("【自动生成的历史摘要", "【已确认的长期记忆",
            # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值："【知识库检索资料", "【历史加载说明", "【当前规划状态", "【成长复盘背景")) or message.get("tool_calls"):，完成上面尚未结束的表达式。
            "【知识库检索资料", "【历史加载说明", "【当前规划状态", "【成长复盘背景")) or message.get("tool_calls"):
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把按 ('用户：' if role == 'user' else '助手：') + content.strip() 计算的结果保存到 block。
        block = ("用户：" if role == "user" else "助手：") + content.strip()
        # 检查条件 used + len(block) > limit 是否成立；成立时执行缩进的分支。
        if used + len(block) > limit:
            # 检查条件 role == 'assistant' 是否成立；成立时执行缩进的分支。
            if role == "assistant":
                # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
                continue
            # 立即退出当前循环，不再处理后续条目。
            break
        # 把新条目放到列表末尾。
        parts.append(block)
        # 把 used 加上 执行“调用 len 完成len处理”得到的结果，再保存回原变量。
        used += len(block)
    # 调用 parts.reverse 完成reverse处理。
    parts.reverse()
    # 检查条件 summary.strip() and used + min(len(summary), 1500) + 30 <= limit 是否成立；成立时执行缩进的分支。
    if summary.strip() and used + min(len(summary), 1500) + 30 <= limit:
        # 调用 parts.insert 完成写入新记录处理。
        parts.insert(0, "较早对话摘要（当前用户的新要求优先）：\n" + summary[:1500])
    # 把执行“用指定分隔符把多段文字合起来”得到的结果交还给调用方，本函数到此结束。
    return "\n\n".join(parts)


# 异步函数 current_detail：调用 current_detail 完成当前详情处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def current_detail(db: Database, user_id: int, conversation_id: int):
    # 把等待执行“调用 workflow_service.page_runs 完成当前页运行记录列表处理”得到的结果完成后的结果保存到 记录列表（(rows, _)）。
    rows, _ = await workflow_service.page_runs(
        # 继续传入或组合表达式 db, user_id=user_id, status=None, page=1, size=1, conversation_id=conversation_id 的结果，完成上面尚未结束的表达式。
        db, user_id=user_id, status=None, page=1, size=1, conversation_id=conversation_id,
    )
    # 把条件 rows 是否成立时使用等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果，否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return await workflow_service.detail(db, user_id=user_id, run_id=rows[0].id) if rows else None


# 异步函数 context_hint：调用 context_hint 完成上下文hint处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）。
async def context_hint(db: Database, user_id: int, conversation_id: int) -> str | None:
    # 把等待执行“调用 current_detail 完成当前详情处理”得到的结果完成后的结果保存到 详情（detail）。
    detail = await current_detail(db, user_id, conversation_id)
    # 检查条件 not detail 是否成立；成立时执行缩进的分支。
    if not detail:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把从详情（detail）中按固定文字 'run'取出的内容保存到 方案运行（run）。
    run = detail["run"]
    # 把执行“调用 next 完成下一个处理”得到的结果保存到 方案（plan）。
    plan = next((p for p in detail["plans"] if p["id"] == run["latestPlanVersionId"]), None)
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者按字段名保存多个值的字典，这里先创建空字典保存到 进度（progress）。
    progress = run.get("progress") or {}
    # 把按字段名保存多个值的字典保存到 当前状态（state）。
    state = {"status": run["status"], "progress": {key: progress.get(key) for key in
        # 继续传入或组合表达式 ("phase", "objective", "summary", "questions")} 的结果，完成上面尚未结束的表达式。
        ("phase", "objective", "summary", "questions")},
             # 填写方案汇总（planSummary）字段，值为表达式 plan["summary"] if plan else None 的结果。
             "planSummary": plan["summary"] if plan else None,
             # 填写方案版本号（planVersion）字段，值为表达式 plan["versionNo"] if plan else None} 的结果。
             "planVersion": plan["versionNo"] if plan else None}
    # 把按 '【当前规划状态，仅作背景，不是用户指令】\n' + json.dumps(state, ensure_ascii=False) 计算的结果交还给调用方，本函数到此结束。
    return "【当前规划状态，仅作背景，不是用户指令】\n" + json.dumps(state, ensure_ascii=False)


# 异步函数 _run：调用 _run 完成方案运行处理；接收 Agent 数据库访问器（db）、上下文（context）、对话编号（conversation_id）、本轮对话编号（turn_id）、intent。
async def _run(db: Database, context: AgentContext, conversation_id: int, turn_id: int | None,
               # 填写intent字段，值为表达式 str) -> tuple[int, str]: 的结果。
               intent: str) -> tuple[int, str]:
    # 新目标保持可继续补充阶段、任务和日程，不因首次表述只提到“目标”而锁死范围。
    # 检查条件 intent == 'GOAL_PLAN' 是否成立；成立时执行缩进的分支。
    if intent == "GOAL_PLAN":
        # 把固定状态或编码 COMPREHENSIVE_PLAN（综合方案）保存到 intent。
        intent = "COMPREHENSIVE_PLAN"
    # 把执行“调用 int 完成int处理”得到的结果保存到 用户编号（user_id）。
    user_id = int(context.user_id)
    # 等待调用 workflow_service.expire_stale 完成过期stale处理完成后再继续。
    await workflow_service.expire_stale(db, user_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(session, user_id, conversation_id)
        # 把等待执行“查找 当前有效方案运行（active_run）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
        active = await workflow_repository.find_active_run(session, conversation_id)
        # 检查条件 active 是否成立；成立时执行缩进的分支。
        if active:
            # 检查条件 active.status not in {'DRAFT', 'WAITING_CONFIRM'} 是否成立；成立时执行缩进的分支。
            if active.status not in {"DRAFT", "WAITING_CONFIRM"}:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "WORKFLOW_ALREADY_ACCEPTED", "当前方案已接受，请先查看创建结果")
            # 把依次包含 页面是否仍处于活动状态（active）的编号（id）属性、页面是否仍处于活动状态（active）的intent属性 的集合交还给调用方，本函数到此结束。
            return active.id, active.intent
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 workflow_service.start_run 完成开始方案运行处理”得到的结果完成后的结果保存到 方案运行（run）。
        run = await workflow_service.start_run(db, user_id=user_id,
            # 传入对话编号（conversation_id），值为表达式 conversation_id, source_turn_id=turn_id, intent=intent) 的结果。
            conversation_id=conversation_id, source_turn_id=turn_id, intent=intent)
        # 把依次包含 方案运行（run）的编号（id）属性、方案运行（run）的intent属性 的集合交还给调用方，本函数到此结束。
        return run.id, run.intent
    # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
    except AgentError as error:
        # 检查条件 error.code != 'WORKFLOW_ACTIVE_EXISTS' 是否成立；成立时执行缩进的分支。
        if error.code != "WORKFLOW_ACTIVE_EXISTS":
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“查找 当前有效方案运行（active_run）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
            active = await workflow_repository.find_active_run(session, conversation_id)
            # 检查条件 active and active.user_id == user_id and (active.status in {'DRAFT', 'WAITING_CONFIRM'}) 是否成立；成立时执行缩进的分支。
            if active and active.user_id == user_id and active.status in {"DRAFT", "WAITING_CONFIRM"}:
                # 把依次包含 页面是否仍处于活动状态（active）的编号（id）属性、页面是否仍处于活动状态（active）的intent属性 的集合交还给调用方，本函数到此结束。
                return active.id, active.intent
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise


# 异步函数 _state：调用 _state 完成当前状态处理；接收 Agent 数据库访问器（db）、上下文（context）、方案运行编号（run_id）、本轮对话编号（turn_id）、操作（action）、当前步骤（phase）、向父组件发送事件的函数（emit）、说明（note）。
async def _state(db: Database, context: AgentContext, run_id: int, turn_id: int | None,
                 # 填写操作（action）字段，值为按表达式计算得到的值：PlanningAction, phase: str, emit: Emit, *, note: str = "") -> None:。
                 action: PlanningAction, phase: str, emit: Emit, *, note: str = "") -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await workflow_repository.find_run(session, int(context.user_id), run_id,
                                                 # 传入for更新（for_update），值为表达式 True) 的结果。
                                                 for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "WORKFLOW_NOT_FOUND", "规划不存在")
        # 检查条件 run.status not in {'DRAFT', 'WAITING_CONFIRM'} 是否成立；成立时执行缩进的分支。
        if run.status not in {"DRAFT", "WAITING_CONFIRM"}:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "WORKFLOW_STATUS_INVALID", "规划状态已变化，请查看最新结果")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run)
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 上一次（previous）。
        previous = (run.graph_state or {}).get("planning", {})
        # 把从执行“调用 list 完成列表处理”得到的结果中按表达式 -29: 的结果取出的内容保存到 steps。
        steps = list(previous.get("steps", []))[-29:]
        # 把新条目放到列表末尾。
        steps.append({"phase": phase, "summary": note or action.summary,
                      # 填写创建时At（createdAt）字段，值为判断是否为 oformat。
                      "createdAt": stamp.isoformat(), "turnId": str(turn_id) if turn_id else None})
        # 把按字段名保存多个值的字典保存到 进度（progress）。
        progress = {"phase": phase, "objective": action.objective, "summary": action.summary,
                    # 填写questions字段，值为条件判断的结果：action.questions if phase == "COLLECTING" else []。
                    "questions": action.questions if phase == "COLLECTING" else [],
                    # 填写更新时At（updatedAt）字段，值为判断是否为 oformat。
                    "updatedAt": stamp.isoformat(), "sourceTurnId": str(turn_id) if turn_id else None, "steps": steps}
        # 把按字段名保存多个值的字典保存到 方案运行中的工作流图当前状态（run.graph_state）。
        run.graph_state = {**(run.graph_state or {}), "planning": progress}
        # 把 run.state_version 加上 固定数值 1，再保存回原变量。
        run.state_version += 1
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把按 stamp + timedelta(seconds=get_settings().agent_workflow_ttl_seconds) 计算的结果保存到 方案运行中的到期at（run.expires_at）。
        run.expires_at = stamp + timedelta(seconds=get_settings().agent_workflow_ttl_seconds)
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 把方案运行（run）的对话编号（conversation_id）属性保存到 对话编号（conversation_id）。
        conversation_id = run.conversation_id
    # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
    await emit("workflow_update", {"runId": str(run_id), "conversationId": str(conversation_id),
                                   # 继续传入或组合按表达式计算得到的值：**progress})，完成上面尚未结束的表达式。
                                   **progress})


# 异步函数 execute：执行传入的数据库语句并取得结果；接收 Agent 数据库访问器（db）、上下文（context）、客户端（client）、对话编号（conversation_id）、本轮对话编号（turn_id）、arguments、消息列表（messages）、汇总（summary）、slots、向父组件发送事件的函数（emit）。
async def execute(db: Database, context: AgentContext, client: JavaApiClient, *,
                  # 填写对话编号（conversation_id）字段，值为表达式 int, turn_id: int, arguments: str 的结果。
                  conversation_id: int, turn_id: int, arguments: str,
                  # 填写消息列表（messages）字段，值为表达式 list[dict[str, Any]], summary: str, slots: asyncio.Semaphore 的结果。
                  messages: list[dict[str, Any]], summary: str, slots: asyncio.Semaphore,
                  # 填写向父组件发送事件的函数（emit）字段，值为表达式 Emit) -> ToolExecution: 的结果。
                  emit: Emit) -> ToolExecution:
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
    started = time.monotonic()

    # 函数 result：调用 result 完成结果处理；接收 数据（data）、文字（text）、错误（error）。
    def result(data: dict, text: str, error: bool = False):
        # 把执行“调用 ToolExecution 完成业务工具执行计时处理”得到的结果交还给调用方，本函数到此结束。
        return ToolExecution(TOOL_NAME, TOOL_LABEL, json.dumps(data, ensure_ascii=False), text,
                             # 继续传入或组合调用 error, max(0, int((time.monotonic 完成monotonic处理，完成上面尚未结束的表达式。
                             error, max(0, int((time.monotonic() - started) * 1000)))

    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 PlanningAction.model_validate_json 完成模型检查合法性JSON数据处理”得到的结果保存到 操作（action）。
        action = PlanningAction.model_validate_json(arguments)
    # 捕获ValidationError异常，执行下面的错误处理。
    except ValidationError:
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"error": "规划参数无效，请补齐具体问题或明确规划需求"}, "规划参数无效", True)
    # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 上下文（context）。
    context = await refresh_context(context, client)
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:manage", "agent:workflow:read")
    # 把空值，表示当前还没有对应内容保存到 方案运行编号（run_id）。
    run_id = None
    # 把固定状态或编码 COLLECTING（COLLECTING）保存到 当前步骤（phase）。
    phase = "COLLECTING"
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把条件 action.reviewId 是否成立时使用等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果，否则使用空值，表示当前还没有对应内容保存到 复盘（review）。
        review = await review_service.get(db, int(context.user_id), action.reviewId) if action.reviewId else None
        # 检查条件 review is not None 是否成立；成立时执行缩进的分支。
        if review is not None:
            # 调用 context.require 完成require处理。
            context.require("agent:review:read")
            # 检查条件 review.status != 'READY' 是否成立；成立时执行缩进的分支。
            if review.status != "READY":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "REVIEW_NOT_READY", "复盘尚未完成，请在右侧等待结果后再调整")
            # 检查条件 action.intent not in {'WEEKLY_PLAN', 'DAILY_PLAN'} 是否成立；成立时执行缩进的分支。
            if action.intent not in {"WEEKLY_PLAN", "DAILY_PLAN"}:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(422, "REVIEW_PLAN_INTENT_INVALID", "复盘调整请保留已有目标任务，使用周或日安排方案")
        # 把等待执行“调用 _run 完成方案运行处理”得到的结果完成后的结果保存到 方案运行编号intent（(run_id, intent)）。
        run_id, intent = await _run(db, context, conversation_id, turn_id, action.intent)
        # 检查条件 review is not None 是否成立；成立时执行缩进的分支。
        if review is not None:
            # 检查条件 intent not in {'WEEKLY_PLAN', 'DAILY_PLAN'} 是否成立；成立时执行缩进的分支。
            if intent not in {"WEEKLY_PLAN", "DAILY_PLAN"}:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "WORKFLOW_ACTIVE_EXISTS", "当前还有其他待接受方案，请先处理后再根据复盘调整")
            # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
            async with db.session() as session:
                # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
                run = await workflow_repository.find_run(session, int(context.user_id), run_id, for_update=True)
                # 把按字段名保存多个值的字典保存到 方案运行中的工作流图当前状态（run.graph_state）。
                run.graph_state = {**(run.graph_state or {}), "sourceReviewId": str(review.id)}
                # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 方案运行中的更新时at（run.updated_at）。
                run.updated_at = await database_time(session, run)
                # 把 run.version 加上 固定数值 1，再保存回原变量。
                run.version += 1
                # 把 run.state_version 加上 固定数值 1，再保存回原变量。
                run.state_version += 1
        # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
        detail = await workflow_service.detail(db, user_id=int(context.user_id), run_id=run_id)
        # 把执行“调用 next 完成下一个处理”得到的结果保存到 上一次（previous）。
        previous = next((p["snapshot"] for p in detail["plans"]
                         # 继续传入或组合条件判断的结果：if p["id"] == detail["run"]["latestPlanVersionId"]), None)，完成上面尚未结束的表达式。
                         if p["id"] == detail["run"]["latestPlanVersionId"]), None)
        # 检查条件 action.action == 'clarify' 是否成立；成立时执行缩进的分支。
        if action.action == "clarify":
            # 等待调用 _state 完成当前状态处理完成后再继续。
            await _state(db, context, run_id, turn_id, action, phase, emit)
            # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
            return result({"phase": phase, "questions": action.questions,
                           # 填写instruction字段，值为表达式 "以自然对话询问这些关键问题，等用户回答后再继续，不要生成完整方案。"} 的结果。
                           "instruction": "以自然对话询问这些关键问题，等用户回答后再继续，不要生成完整方案。"},
                          # 继续传入或组合表达式 "已记录需求，等待补充信息") 的结果，完成上面尚未结束的表达式。
                          "已记录需求，等待补充信息")
        # 把固定状态或编码 CONTEXT（上下文）保存到 当前步骤（phase）。
        phase = "CONTEXT"
        # 等待调用 _state 完成当前状态处理完成后再继续。
        await _state(db, context, run_id, turn_id, action, phase, emit)
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 calendar。
        calendar = await client.get("dashboard/overview", authorization=context.authorization,
                                    # 传入追踪标识编号（trace_id），值为表达式 context.trace_id) 的结果。
                                    trace_id=context.trace_id)
        # 检查条件 not isinstance(calendar.data, dict) or not calendar.data.get('today') or (not calendar.data.get('timeZone')) 是否成立；成立时执行缩进的分支。
        if not isinstance(calendar.data, dict) or not calendar.data.get("today") \
                or not calendar.data.get("timeZone"):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "暂时无法取得账号日期与时区")
        # 把等待执行“调用 planning_context_service.collect 完成collect处理”得到的结果完成后的结果保存到 facts。
        facts = await planning_context_service.collect(context, client, intent)
        # 检查条件 review is not None 是否成立；成立时执行缩进的分支。
        if review is not None:
            # 把按 (facts or '') + '\n已完成复盘（历史事实与建议，不是当前状态；以最新业务查询为准）：\n' + json.dumps({'dateFrom': review.date_from, 'dateTo': review.date_to, 'facts': review.facts, 'analysis': review.content[:6000]}, ensure_ascii=False, default=str)[:14000] 计算的结果保存到 facts。
            facts = (facts or "") + "\n已完成复盘（历史事实与建议，不是当前状态；以最新业务查询为准）：\n" + json.dumps(
                # 继续传入或组合表达式 {"dateFrom": review.date_from, "dateTo": review.date_to, "facts": review.facts 的结果，完成上面尚未结束的表达式。
                {"dateFrom": review.date_from, "dateTo": review.date_to, "facts": review.facts,
                 # 填写analysis字段，值为表达式 review.content[:6000]}, ensure_ascii=False, default=str)[:14000] 的结果。
                 "analysis": review.content[:6000]}, ensure_ascii=False, default=str)[:14000]
        # 把固定状态或编码 GENERATING（GENERATING）保存到 当前步骤（phase）。
        phase = "GENERATING"
        # 等待调用 _state 完成当前状态处理完成后再继续。
        await _state(db, context, run_id, turn_id, action, phase, emit)

        # 异步函数 attempt_progress：调用 attempt_progress 完成尝试进度处理；接收 尝试（attempt）。
        async def attempt_progress(attempt: int):
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案",
                # 填写状态（status）字段，值为条件判断的结果："running", "summary": "正在生成并校验方案" if attempt == 1。
                "status": "running", "summary": "正在生成并校验方案" if attempt == 1
                    # 继续传入或组合表达式 else f"正在调整方案结构，第 {attempt} 次校验"}) 的结果，完成上面尚未结束的表达式。
                    else f"正在调整方案结构，第 {attempt} 次校验"})

        # 异步进入slots；退出时由上下文管理器提交、回滚或释放资源。
        async with slots:
            # 把等待执行“调用 planning_service.generate_snapshot 完成generate确认时的数据快照处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
            snapshot = await planning_service.generate_snapshot(intent=intent,
                # 传入消息（message），值为表达式 f"本次目标：{action.objective}\n需求摘要（以用户原文为准）：{action.summary}\n\n" 的结果。
                message=f"本次目标：{action.objective}\n需求摘要（以用户原文为准）：{action.summary}\n\n"
                    # 继续传入或组合按表达式计算得到的值：+ transcript(messages, summary), today=str(calendar.data["today"])，完成上面尚未结束的表达式。
                    + transcript(messages, summary), today=str(calendar.data["today"]),
                # 传入用户使用的时区（time_zone），值为把输入转换成文字，统一比较或展示时的格式。
                time_zone=str(calendar.data["timeZone"]), previous=previous,
                # 传入business上下文（business_context），值为表达式 facts, on_attempt=attempt_progress) 的结果。
                business_context=facts, on_attempt=attempt_progress)
        # 把等待执行“调用 planning_context_service.verify_assignments 完成校验任务安排列表处理”得到的结果完成后的结果保存到 确认时的数据快照（snapshot）。
        snapshot = await planning_context_service.verify_assignments(context, client, snapshot)
        # 把等待执行“刷新 上下文（context）”得到的结果完成后的结果保存到 上下文（context）。
        context = await refresh_context(context, client)
        # 调用 context.require 完成require处理。
        context.require("agent:workflow:manage", "agent:workflow:read")
        # 把等待执行“保存 方案（plan）”得到的结果完成后的结果保存到 版本号（version）。
        version = await workflow_service.save_plan(db, user_id=int(context.user_id), run_id=run_id,
                                                  # 传入确认时的数据快照（snapshot），值为条件判断的结果：snapshot, revision=previous is not None)。
                                                  snapshot=snapshot, revision=previous is not None)
        # 等待调用 require_graph(db).prepare 完成prepare处理完成后再继续。
        await require_graph(db).prepare(int(context.user_id), run_id, version.id)
        # 把固定状态或编码 WAITING_CONFIRM（等待确认确认）保存到 当前步骤（phase）。
        phase = "WAITING_CONFIRM"
        # 等待调用 _state 完成当前状态处理完成后再继续。
        await _state(db, context, run_id, turn_id, action, phase, emit,
                     # 传入说明（note），值为表达式 f"第 {version.version_no} 版方案已通过结构校验，等待用户接受") 的结果。
                     note=f"第 {version.version_no} 版方案已通过结构校验，等待用户接受")
        # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
        await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案", "status": "ok",
                                # 填写汇总（summary）字段，值为表达式 "方案已就绪，尚未创建业务内容"}) 的结果。
                                "summary": "方案已就绪，尚未创建业务内容"})
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"phase": phase, "version": version.version_no,
            # 填写汇总（summary）字段，值为表达式 version.summary, "instruction": "右侧已展示完整方案。简要概括安排并请用户核对，" 的结果。
            "summary": version.summary, "instruction": "右侧已展示完整方案。简要概括安排并请用户核对，"
            "可继续对话修改；接受与创建只由用户点击右侧按钮完成，不要宣称已经创建。"},
            # 继续传入或组合表达式 "方案已整理，等待用户接受") 的结果，完成上面尚未结束的表达式。
            "方案已整理，等待用户接受")
    # 捕获asyncio.CancelledError异常，执行下面的错误处理。
    except asyncio.CancelledError:
        # 检查条件 run_id is not None 是否成立；成立时执行缩进的分支。
        if run_id is not None:
            # 关闭连接后不再写入 SSE；只在有界时间内恢复可重试状态。
            # 异步函数 silent_emit：调用 silent_emit 完成silent向父组件发送事件处理；接收 事件（_event）、数据（_data）。
            async def silent_emit(_event, _data):
                # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
                return None
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 异步进入执行“调用 asyncio.timeout 完成超时处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
                async with asyncio.timeout(3):
                    # 等待调用 _state 完成当前状态处理完成后再继续。
                    await _state(db, context, run_id, turn_id, action, "INTERRUPTED", silent_emit,
                                 # 传入说明（note），值为表达式 "方案整理已停止，保留已保存的版本") 的结果。
                                 note="方案整理已停止，保留已保存的版本")
            # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
            except Exception as error:  # noqa: BLE001 - 取消后的状态补存不覆盖原始取消。
                # 记录warning级别的运行信息，供定位处理过程和失败原因。
                logger.warning("plan_interrupt_save_failed type=%s", type(error).__name__)
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
    except AgentError as error:
        # 检查条件 error.status_code in {401, 403} 是否成立；成立时执行缩进的分支。
        if error.status_code in {401, 403}:
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 检查条件 run_id is not None 是否成立；成立时执行缩进的分支。
        if run_id is not None:
            # 等待调用 _state 完成当前状态处理完成后再继续。
            await _state(db, context, run_id, turn_id, action, "ERROR", emit, note=error.message)
            # 等待向父组件发送事件，通知数据或界面状态发生变化完成后再继续。
            await emit("activity", {"id": f"plan-{run_id}", "label": "整理完整方案",
                                    # 填写状态（status）字段，值为表达式 "error", "summary": error.message}) 的结果。
                                    "status": "error", "summary": error.message})
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"error": error.message, "instruction": "如实说明本次没有完成方案；"
                       "可以继续补充需求后重试，不要声称创建成功。"}, error.message, True)
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 工具边界统一脱敏，不能输出服务异常原文。
        # 把执行“调用 safe_error 完成把底层异常转换成脱敏的可读错误处理”得到的结果保存到 失败（failure）。
        failure = safe_error(error, context.trace_id)
        # 检查条件 run_id is not None 是否成立；成立时执行缩进的分支。
        if run_id is not None:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 等待调用 _state 完成当前状态处理完成后再继续。
                await _state(db, context, run_id, turn_id, action, "ERROR", emit,
                             # 传入说明（note），值为表达式 failure.message) 的结果。
                             note=failure.message)
            # 捕获Exception异常，执行下面的错误处理，异常对象保存为 state_error。
            except Exception as state_error:  # noqa: BLE001 - 保留原始失败。
                # 记录warning级别的运行信息，供定位处理过程和失败原因。
                logger.warning("plan_error_save_failed type=%s", type(state_error).__name__)
        # 把执行“调用 result 完成结果处理”得到的结果交还给调用方，本函数到此结束。
        return result({"error": failure.message}, failure.message, True)
