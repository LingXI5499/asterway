# 保存方案版本、确认、取消和执行状态；业务落库仍交给 Java 原子提交。
# 工作流运行时：运行、方案版本、确认、取消、过期与幂等提交记录。
# 
# 说明：为避免新增未安装的 LangGraph 依赖导致无法编译，P1.5.1 使用等价的内置状态机
# （显式状态迁移 + 不可变方案版本 + 持久化事件表），后续可在保持契约不变的前提下替换为 LangGraph。

# 引入 hashlib，供下面调用其功能。
import hashlib

# 引入 安全随机值（secrets），供下面调用其功能。
import secrets

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）、两个时刻之间的时间长度（timedelta）。
from datetime import UTC, datetime, timedelta

# 从 typing 引入 Any。
from typing import Any

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 方案提交回执（PlanCommit）、方案版本号（PlanVersion）、规划工作流事件（WorkflowEvent）、规划工作流方案运行（WorkflowRun）。
from app.db.models import PlanCommit, PlanVersion, WorkflowEvent, WorkflowRun

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）、Java后端接口错误（JavaApiError）。
from app.integrations.java_client import JavaApiClient, JavaApiError

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、本轮对话数据仓库（turn_repository）。
from app.repositories import conversation_repository, turn_repository

# 从 app.repositories 引入 repo。
from app.repositories import workflow_repository as repo

# 从 app.schemas.planning 引入 INTENTS、方案确认时的数据快照（PlanSnapshot）。
from app.schemas.planning import INTENTS, PlanSnapshot

# 把按字段名保存多个值的字典保存到 INTENT名称对应表（INTENT_LABELS）。
INTENT_LABELS = {
    # 填写目标方案（GOAL_PLAN）字段，值为固定文字 '目标规划'。
    "GOAL_PLAN": "目标规划",
    # 填写阶段方案（STAGE_PLAN）字段，值为固定文字 '阶段规划'。
    "STAGE_PLAN": "阶段规划",
    # 填写任务方案（TASK_PLAN）字段，值为固定文字 '任务规划'。
    "TASK_PLAN": "任务规划",
    # 填写每周方案（WEEKLY_PLAN）字段，值为固定文字 '周计划'。
    "WEEKLY_PLAN": "周计划",
    # 填写每日方案（DAILY_PLAN）字段，值为固定文字 '日计划'。
    "DAILY_PLAN": "日计划",
    # 填写习惯方案（HABIT_PLAN）字段，值为固定文字 '习惯规划'。
    "HABIT_PLAN": "习惯规划",
    # 填写综合方案（COMPREHENSIVE_PLAN）字段，值为固定文字 '综合成长规划'。
    "COMPREHENSIVE_PLAN": "综合成长规划",
}
# 把按字段名保存多个值的字典保存到 方案运行状态名称对应表（RUN_STATUS_LABELS）。
RUN_STATUS_LABELS = {
    # 填写草案（DRAFT）字段，值为固定文字 '草稿'。
    "DRAFT": "草稿",
    # 填写等待确认确认（WAITING_CONFIRM）字段，值为固定文字 '待确认'。
    "WAITING_CONFIRM": "待确认",
    # 填写已确认（CONFIRMED）字段，值为固定文字 '已确认'。
    "CONFIRMED": "已确认",
    # 填写正在执行（EXECUTING）字段，值为固定文字 '执行中'。
    "EXECUTING": "执行中",
    # 填写已成功（SUCCEEDED）字段，值为固定文字 '已完成'。
    "SUCCEEDED": "已完成",
    # 填写失败（FAILED）字段，值为固定文字 '失败'。
    "FAILED": "失败",
    # 填写已取消（CANCELLED）字段，值为固定文字 '已取消'。
    "CANCELLED": "已取消",
    # 填写已过期（EXPIRED）字段，值为固定文字 '已过期'。
    "EXPIRED": "已过期",
}
# 把按字段名保存多个值的字典保存到 方案状态名称对应表（PLAN_STATUS_LABELS）。
PLAN_STATUS_LABELS = {
    # 填写草案（DRAFT）字段，值为固定文字 '草稿'。
    "DRAFT": "草稿",
    # 填写等待确认确认（WAITING_CONFIRM）字段，值为固定文字 '待确认'。
    "WAITING_CONFIRM": "待确认",
    # 填写已确认（CONFIRMED）字段，值为固定文字 '已确认'。
    "CONFIRMED": "已确认",
    # 填写SUPERSEDED字段，值为固定文字 '已被替代'。
    "SUPERSEDED": "已被替代",
    # 填写REJECTED字段，值为固定文字 '已拒绝'。
    "REJECTED": "已拒绝",
    # 填写已过期（EXPIRED）字段，值为固定文字 '已过期'。
    "EXPIRED": "已过期",
}
# 把依次包含 固定状态或编码 DRAFT（草案）、固定状态或编码 WAITING_CONFIRM（等待确认确认）、固定状态或编码 CONFIRMED（已确认）、固定状态或编码 EXECUTING（正在执行） 的集合保存到 当前有效方案运行STATUSES（ACTIVE_RUN_STATUSES）。
ACTIVE_RUN_STATUSES = ("DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING")
# 与 Java AgentPlanCommitServiceImpl 的 MAX_ATTEMPTS 保持一致。
# 把固定数值 10保存到 最大提交回执尝试次数（MAX_COMMIT_ATTEMPTS）。
MAX_COMMIT_ATTEMPTS = 10


# 函数 snapshot_hash：调用 snapshot_hash 完成确认时的数据快照摘要值处理；接收 确认时的数据快照（snapshot）。
def snapshot_hash(snapshot: dict[str, Any]) -> str:

    # 引入 JSON 编码和解析库（json），供下面调用其功能。
    import json

    # 把执行“把对象编码为 JSON 文字”得到的结果保存到 canonical。
    canonical = json.dumps(snapshot, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    # 把执行“调用 hashlib.sha256(canonical.encode('utf-8')).hexdigest 完成hexdigest处理”得到的结果交还给调用方，本函数到此结束。
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


# 函数 _summary：调用 _summary 完成汇总处理；接收 intent、确认时的数据快照（snapshot）。
def _summary(intent: str, snapshot: PlanSnapshot) -> str:
    # 把依次包含 执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果 的集合保存到 parts。
    parts = [INTENT_LABELS.get(intent, intent)]
    # 检查条件 snapshot.goal is not None 是否成立；成立时执行缩进的分支。
    if snapshot.goal is not None:
        # 把新条目放到列表末尾。
        parts.append(f"目标「{snapshot.goal.title}」")
    # 检查条件 snapshot.stages 是否成立；成立时执行缩进的分支。
    if snapshot.stages:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.stages)} 个阶段")
    # 检查条件 snapshot.tasks 是否成立；成立时执行缩进的分支。
    if snapshot.tasks:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.tasks)} 个任务")
    # 检查条件 snapshot.weeklyPlans 是否成立；成立时执行缩进的分支。
    if snapshot.weeklyPlans:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.weeklyPlans)} 个周计划")
    # 检查条件 snapshot.dailyPlans 是否成立；成立时执行缩进的分支。
    if snapshot.dailyPlans:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.dailyPlans)} 个日计划")
    # 检查条件 snapshot.taskAssignments 是否成立；成立时执行缩进的分支。
    if snapshot.taskAssignments:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.taskAssignments)} 条任务安排")
    # 检查条件 snapshot.habits 是否成立；成立时执行缩进的分支。
    if snapshot.habits:
        # 把新条目放到列表末尾。
        parts.append(f"{len(snapshot.habits)} 个习惯")
    # 把从执行“用指定分隔符把多段文字合起来”得到的结果中按表达式 :1000 的结果取出的内容交还给调用方，本函数到此结束。
    return "，".join(parts)[:1000]


# 函数 _stamp：调用 _stamp 完成stamp处理；接收 当前记录（row）。
def _stamp(row: WorkflowRun | PlanVersion) -> datetime:
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(datetime.now(UTC), row.created_at)


# 函数 _not_found：调用 _not_found 完成notfound处理。
def _not_found() -> AgentError:
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
    return AgentError(404, "WORKFLOW_NOT_FOUND", "没有找到可访问的规划方案")


# 函数 _conflict：调用 _conflict 完成conflict处理；接收 编码（code）、消息（message）。
def _conflict(code: str, message: str) -> AgentError:
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
    return AgentError(409, code, message)


# 函数 run_vo：调用 run_vo 完成方案运行处理；接收 当前记录（row）。
def run_vo(row: WorkflowRun) -> dict[str, Any]:
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 进度（progress）。
    progress = dict((row.graph_state or {}).get("planning", {}))
    # 检查条件 row.status not in ('DRAFT', 'WAITING_CONFIRM') or not progress.get('phase') 是否成立；成立时执行缩进的分支。
    if row.status not in ("DRAFT", "WAITING_CONFIRM") or not progress.get("phase"):
        # 把当前记录（row）的状态（status）属性保存到 进度当前步骤（progress['phase']）。
        progress["phase"] = row.status
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id),
        # 填写对话编号（conversationId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "conversationId": str(row.conversation_id),
        # 填写intent字段，值为表达式 row.intent 的结果。
        "intent": row.intent,
        # 填写intent显示名称（intentLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "intentLabel": INTENT_LABELS.get(row.intent),
        # 填写状态（status）字段，值为表达式 row.status 的结果。
        "status": row.status,
        # 填写状态显示名称（statusLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "statusLabel": RUN_STATUS_LABELS.get(row.status),
        # 填写最新方案版本号编号（latestPlanVersionId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "latestPlanVersionId": str(row.latest_plan_version_id)
        # 继续传入或组合表达式 if row.latest_plan_version_id 的结果，完成上面尚未结束的表达式。
        if row.latest_plan_version_id
        # 继续传入或组合表达式 else None 的结果，完成上面尚未结束的表达式。
        else None,
        # 填写到期At（expiresAt）字段，值为表达式 row.expires_at 的结果。
        "expiresAt": row.expires_at,
        # 填写已确认At（confirmedAt）字段，值为表达式 row.confirmed_at 的结果。
        "confirmedAt": row.confirmed_at,
        # 填写finishedAt字段，值为表达式 row.finished_at 的结果。
        "finishedAt": row.finished_at,
        # 填写错误编码（errorCode）字段，值为表达式 row.error_code 的结果。
        "errorCode": row.error_code,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at 的结果。
        "createdAt": row.created_at,
        # 填写更新时At（updatedAt）字段，值为表达式 row.updated_at 的结果。
        "updatedAt": row.updated_at,
        # 填写版本号（version）字段，值为表达式 row.version 的结果。
        "version": row.version,
        # 填写进度（progress）字段，值为进度（progress）。
        "progress": progress,
        # 填写来源复盘编号（sourceReviewId）字段，值为优先使用前面的有效值，没有时采用后面的备用值：(row.graph_state or {}).get("sourceReviewId")。
        "sourceReviewId": (row.graph_state or {}).get("sourceReviewId"),
    }


# 函数 plan_vo：调用 plan_vo 完成方案处理；接收 当前记录（row）。
def plan_vo(row: PlanVersion) -> dict[str, Any]:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id),
        # 填写工作流运行编号（runId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "runId": str(row.run_id),
        # 填写版本号No（versionNo）字段，值为表达式 row.version_no 的结果。
        "versionNo": row.version_no,
        # 填写状态（status）字段，值为表达式 row.status 的结果。
        "status": row.status,
        # 填写状态显示名称（statusLabel）字段，值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        "statusLabel": PLAN_STATUS_LABELS.get(row.status),
        # 填写汇总（summary）字段，值为表达式 row.summary 的结果。
        "summary": row.summary,
        # 填写确认时的数据快照（snapshot）字段，值为表达式 row.snapshot 的结果。
        "snapshot": row.snapshot,
        # 填写diff汇总（diffSummary）字段，值为表达式 row.diff_summary 的结果。
        "diffSummary": row.diff_summary,
        # 填写到期At（expiresAt）字段，值为表达式 row.expires_at 的结果。
        "expiresAt": row.expires_at,
        # 填写已确认At（confirmedAt）字段，值为表达式 row.confirmed_at 的结果。
        "confirmedAt": row.confirmed_at,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at 的结果。
        "createdAt": row.created_at,
    }


# 函数 event_vo：调用 event_vo 完成事件处理；接收 当前记录（row）。
def event_vo(row: WorkflowEvent) -> dict[str, Any]:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id),
        # 填写事件No（eventNo）字段，值为表达式 row.event_no 的结果。
        "eventNo": row.event_no,
        # 填写事件类型（eventType）字段，值为表达式 row.event_type 的结果。
        "eventType": row.event_type,
        # 填写提交数据（payload）字段，值为表达式 row.payload 的结果。
        "payload": row.payload,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at 的结果。
        "createdAt": row.created_at,
    }


# 函数 commit_vo：调用 commit_vo 完成提交回执处理；接收 当前记录（row）。
def commit_vo(row: PlanCommit) -> dict[str, Any]:
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {
        # 填写编号（id）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "id": str(row.id),
        # 填写工作流运行编号（runId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "runId": str(row.run_id),
        # 填写方案版本号编号（planVersionId）字段，值为把输入转换成文字，统一比较或展示时的格式。
        "planVersionId": str(row.plan_version_id),
        # 填写状态（status）字段，值为表达式 row.status 的结果。
        "status": row.status,
        # 填写尝试次数（attempts）字段，值为表达式 row.attempts 的结果。
        "attempts": row.attempts,
        # 填写结果（result）字段，值为表达式 row.result 的结果。
        "result": row.result,
        # 填写错误编码（errorCode）字段，值为表达式 row.error_code 的结果。
        "errorCode": row.error_code,
        # 填写错误消息（errorMessage）字段，值为表达式 row.error_message 的结果。
        "errorMessage": row.error_message,
        # 填写创建时At（createdAt）字段，值为表达式 row.created_at 的结果。
        "createdAt": row.created_at,
        # 填写finishedAt字段，值为表达式 row.finished_at 的结果。
        "finishedAt": row.finished_at,
    }


# 异步函数 expire_stale：把本人超时未确认/未执行的工作流标记为过期。；接收 Agent 数据库访问器（db）、用户编号（user_id）。
async def expire_stale(db: Database, user_id: int) -> int:
    # 把本人超时未确认/未执行的工作流标记为过期。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:

        # 从 sqlalchemy 引入 选择（select）。
        from sqlalchemy import select

        # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 运行记录列表（runs）。
        runs = await session.scalars(
            # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
            select(WorkflowRun)
            # 继续补充本次调用或判断：调用 where 完成where处理。
            .where(
                # 继续传入或组合条件判断的结果：WorkflowRun.user_id == user_id，完成上面尚未结束的表达式。
                WorkflowRun.user_id == user_id,
                # 继续传入或组合调用 WorkflowRun.status.in_ 完成in处理，完成上面尚未结束的表达式。
                WorkflowRun.status.in_(("DRAFT", "WAITING_CONFIRM", "CONFIRMED")),
                # 继续传入或组合当前真实时间，用于记录操作发生的时刻，完成上面尚未结束的表达式。
                WorkflowRun.expires_at <= datetime.now(UTC),
            )
            # 继续补充本次调用或判断：调用 with_for_update 完成withfor更新处理。
            .with_for_update(skip_locked=True)
        )
        # 把固定数值 0保存到 已修改（changed）。
        changed = 0
        # 从运行记录列表（runs）依次取出方案运行（run），每项执行一次下面的处理。
        for run in runs:
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
            stamp = await database_time(session, run)
            # 把固定状态或编码 EXPIRED（已过期）保存到 方案运行中的状态（run.status）。
            run.status = "EXPIRED"
            # 把stamp保存到 方案运行中的finishedat（run.finished_at）。
            run.finished_at = stamp
            # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
            run.updated_at = stamp
            # 把 run.version 加上 固定数值 1，再保存回原变量。
            run.version += 1
            # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
            event_no = await repo.next_event_no(session, run.id)
            # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
            await repo.add_event(
                # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
                session,
                # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
                run_id=run.id,
                # 传入用户编号（user_id），值为用户编号（user_id）。
                user_id=user_id,
                # 传入事件no（event_no），值为事件no（event_no）。
                event_no=event_no,
                # 传入事件类型（event_type），值为固定状态或编码 EXPIRED（已过期）。
                event_type="EXPIRED",
            )
            # 把 changed 加上 固定数值 1，再保存回原变量。
            changed += 1
        # 把已修改（changed）交还给调用方，本函数到此结束。
        return changed


# 异步函数 start_run：调用 start_run 完成开始方案运行处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、intent、来源本轮对话编号（source_turn_id）。
async def start_run(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写对话编号（conversation_id）字段，值为int。
    conversation_id: int,
    # 填写intent字段，值为str。
    intent: str,
    # 填写来源本轮对话编号（source_turn_id）字段，值为表达式 int | None = None 的结果。
    source_turn_id: int | None = None,
# 继续传入或组合表达式 ) -> WorkflowRun: 的结果，完成上面尚未结束的表达式。
) -> WorkflowRun:
    # 检查条件 intent not in INTENTS 是否成立；成立时执行缩进的分支。
    if intent not in INTENTS:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "WORKFLOW_INTENT_INVALID", "规划意图无效")
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 等待调用 expire_stale 完成过期stale处理完成后再继续。
    await expire_stale(db, user_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 等待要求满足 owned完成后再继续。
        await conversation_repository.require_owned(
            # 继续传入或组合表达式 session, user_id, conversation_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, user_id, conversation_id, for_update=True
        )
        # 检查source_turn_id is None and await turn_repository.count_running(session, conversation_id)，即对应内容是否不存在；成立时执行缩进的分支。
        if source_turn_id is None and await turn_repository.count_running(session, conversation_id):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_CONVERSATION_RUNNING", "对话正在生成，请完成后再整理调整方案")
        # 检查条件 source_turn_id is not None 是否成立；成立时执行缩进的分支。
        if source_turn_id is not None:
            # 把等待执行“查找 owned”得到的结果完成后的结果保存到 本轮对话（turn）。
            turn = await turn_repository.find_owned(
                # 继续传入或组合表达式 session, user_id, conversation_id, source_turn_id 的结果，完成上面尚未结束的表达式。
                session, user_id, conversation_id, source_turn_id
            )
            # 检查turn is None，即对应内容是否不存在；成立时执行缩进的分支。
            if turn is None:
                # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
                raise _not_found()
        # 把等待执行“查找 当前有效方案运行（active_run）”得到的结果完成后的结果保存到 页面是否仍处于活动状态（active）。
        active = await repo.find_active_run(session, conversation_id)
        # 检查条件 active is not None 是否成立；成立时执行缩进的分支。
        if active is not None:
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_ACTIVE_EXISTS", "当前会话已有进行中的规划，请先完成或取消")
        # 把等待执行“创建 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.create_run(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
            conversation_id=conversation_id,
            # 传入来源本轮对话编号（source_turn_id），值为来源本轮对话编号（source_turn_id）。
            source_turn_id=source_turn_id,
            # 传入intent，值为intent。
            intent=intent,
            # 传入状态（status），值为固定状态或编码 DRAFT（草案）。
            status="DRAFT",
            # 传入到期at（expires_at），值为当前真实时间，用于记录操作发生的时刻。
            expires_at=datetime.now(UTC) + timedelta(seconds=settings.agent_workflow_ttl_seconds),
            # 传入工作流图当前状态（graph_state），值为一个空的键值对应表。
            graph_state={},
        )
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入事件no（event_no），值为固定数值 1。
            event_no=1,
            # 传入事件类型（event_type），值为固定状态或编码 RUN_STARTED（方案运行开始时）。
            event_type="RUN_STARTED",
            # 传入提交数据（payload），值为表达式 {"intent": intent} 的结果。
            payload={"intent": intent},
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(run)
        # 把方案运行（run）交还给调用方，本函数到此结束。
        return run


# 异步函数 save_plan：保存 方案（plan）；接收 Agent 数据库访问器（db）、用户编号（user_id）、方案运行编号（run_id）、确认时的数据快照（snapshot）、修订版本（revision）。
async def save_plan(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
    # 填写确认时的数据快照（snapshot）字段，值为表达式 dict[str, Any] 的结果。
    snapshot: dict[str, Any],
    # 填写修订版本（revision）字段，值为表达式 bool = False 的结果。
    revision: bool = False,
# 继续传入或组合表达式 ) -> PlanVersion: 的结果，完成上面尚未结束的表达式。
) -> PlanVersion:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 PlanSnapshot.model_validate 完成模型检查合法性处理”得到的结果保存到 parsed。
        parsed = PlanSnapshot.model_validate(snapshot)
    # 捕获ValueError异常，执行下面的错误处理，异常对象保存为 error。
    except ValueError as error:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "WORKFLOW_SNAPSHOT_INVALID", "方案内容不符合约定结构") from error
    # 检查条件 parsed.intent not in INTENTS 是否成立；成立时执行缩进的分支。
    if parsed.intent not in INTENTS:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "WORKFLOW_INTENT_INVALID", "规划意图无效")
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 检查条件 run.status not in ('DRAFT', 'WAITING_CONFIRM', 'CONFIRMED') 是否成立；成立时执行缩进的分支。
        if run.status not in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前方案不能再修改，请重新生成")
        # 检查条件 parsed.intent != run.intent 是否成立；成立时执行缩进的分支。
        if parsed.intent != run.intent:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(422, "WORKFLOW_INTENT_INVALID", "修订不能改变当前规划类型")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run)
        # 检查记录或凭据是否已经到期：run.expires_at <= datetime.now(UTC)；成立时执行缩进的分支。
        if run.expires_at <= datetime.now(UTC):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        # 把等待执行“列出 versions”得到的结果完成后的结果保存到 上一次（previous）。
        previous = await repo.list_versions(session, run.id)
        # 从上一次（previous）依次取出当前记录（row），每项执行一次下面的处理。
        for row in previous:
            # 检查条件 row.status in ('DRAFT', 'WAITING_CONFIRM', 'CONFIRMED') 是否成立；成立时执行缩进的分支。
            if row.status in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
                # 把固定状态或编码 SUPERSEDED（SUPERSEDED）保存到 当前记录中的状态（row.status）。
                row.status = "SUPERSEDED"
                # 把stamp保存到 当前记录中的supersededat（row.superseded_at）。
                row.superseded_at = stamp
                # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
                row.updated_at = stamp
                # 把 row.version 加上 固定数值 1，再保存回原变量。
                row.version += 1
        # 把等待执行“调用 repo.next_version_no 完成下一个版本号no处理”得到的结果完成后的结果保存到 版本号no（version_no）。
        version_no = await repo.next_version_no(session, run.id)
        # 把等待执行“创建 方案版本号（plan_version）”得到的结果完成后的结果保存到 版本号（version）。
        version = await repo.create_plan_version(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入版本号no（version_no），值为版本号no（version_no）。
            version_no=version_no,
            # 传入状态（status），值为固定状态或编码 WAITING_CONFIRM（等待确认确认）。
            status="WAITING_CONFIRM",
            # 传入汇总（summary），值为调用 _summary 完成汇总处理。
            summary=_summary(parsed.intent, parsed),
            # 传入确认时的数据快照（snapshot），值为调用 parsed.model_dump 完成模型dump处理。
            snapshot=parsed.model_dump(mode="json"),
            # 传入diff汇总（diff_summary），值为表达式 { 的结果。
            diff_summary={
                # 填写修订版本（revision）字段，值为修订版本（revision）。
                "revision": revision,
                # 填写上一次版本号No（previousVersionNo）字段，值为按表达式计算得到的值：version_no - 1 if revision else None。
                "previousVersionNo": version_no - 1 if revision else None,
            }
            # 继续传入或组合表达式 if revision 的结果，完成上面尚未结束的表达式。
            if revision
            # 继续传入或组合表达式 else None 的结果，完成上面尚未结束的表达式。
            else None,
            # 传入到期at（expires_at），值为表达式 run.expires_at 的结果。
            expires_at=run.expires_at,
        )
        # 把固定状态或编码 WAITING_CONFIRM（等待确认确认）保存到 方案运行中的状态（run.status）。
        run.status = "WAITING_CONFIRM"
        # 把空值，表示当前还没有对应内容保存到 方案运行中的已确认at（run.confirmed_at）。
        run.confirmed_at = None
        # 把版本号（version）的编号（id）属性保存到 方案运行中的最新方案版本号编号（run.latest_plan_version_id）。
        run.latest_plan_version_id = version.id
        # 把 run.state_version 加上 固定数值 1，再保存回原变量。
        run.state_version += 1
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
        event_no = await repo.next_event_no(session, run.id)
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入事件no（event_no），值为事件no（event_no）。
            event_no=event_no,
            # 传入事件类型（event_type），值为固定文字 'PLAN_REVISED" if revision else "PLAN_DRAFTED'。
            event_type="PLAN_REVISED" if revision else "PLAN_DRAFTED",
            # 传入提交数据（payload），值为表达式 {"planVersionId": str(version.id), "versionNo": version_no} 的结果。
            payload={"planVersionId": str(version.id), "versionNo": version_no},
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(version)
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(run)
        # 把版本号（version）交还给调用方，本函数到此结束。
        return version


# 异步函数 confirm_plan：确认 方案（plan）；接收 Agent 数据库访问器（db）、用户编号（user_id）、方案运行编号（run_id）、方案版本号编号（plan_version_id）。
async def confirm_plan(
    # 填写Agent 数据库访问器（db）字段，值为按表达式计算得到的值：Database, *, user_id: int, run_id: int, plan_version_id: int。
    db: Database, *, user_id: int, run_id: int, plan_version_id: int
# 继续传入或组合表达式 ) -> PlanVersion: 的结果，完成上面尚未结束的表达式。
) -> PlanVersion:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 已经确认的同一最新版本直接返回，重复点击或确认成功后执行失败都能继续执行。
        # 检查当前记录版本是否符合请求要求：run.status in ('CONFIRMED', 'EXECUTING', 'SUCCEEDED') and run.latest_plan_version_id is not None and (run.latest_plan_version_id == plan_version_…；成立时执行缩进的分支。
        if run.status in ("CONFIRMED", "EXECUTING", "SUCCEEDED") \
                and run.latest_plan_version_id is not None \
                and run.latest_plan_version_id == plan_version_id:
            # 把等待执行“查找 方案版本号（plan_version）”得到的结果完成后的结果保存到 已确认（confirmed）。
            confirmed = await repo.find_plan_version(
                # 继续传入或组合表达式 session, user_id, plan_version_id, for_update=True 的结果，完成上面尚未结束的表达式。
                session, user_id, plan_version_id, for_update=True
            )
            # 检查条件 confirmed is not None and confirmed.run_id == run.id and (confirmed.status == 'CONFIRMED') 是否成立；成立时执行缩进的分支。
            if (
                # 继续传入或组合条件判断的结果：confirmed is not None，完成上面尚未结束的表达式。
                confirmed is not None
                # 继续补充本次调用或判断：条件判断的结果：and confirmed.run_id == run.id。
                and confirmed.run_id == run.id
                # 继续补充本次调用或判断：条件判断的结果：and confirmed.status == "CONFIRMED"。
                and confirmed.status == "CONFIRMED"
            ):
                # 把已确认（confirmed）交还给调用方，本函数到此结束。
                return confirmed
        # 检查条件 run.status != 'WAITING_CONFIRM' 是否成立；成立时执行缩进的分支。
        if run.status != "WAITING_CONFIRM":
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前方案不处于待确认状态")
        # 检查条件 await turn_repository.count_running(session, run.conversation_id) > 0 是否成立；成立时执行缩进的分支。
        if await turn_repository.count_running(session, run.conversation_id) > 0:
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_CONVERSATION_RUNNING", "请等待本轮对话处理完成后再接受方案")
        # 检查记录或凭据是否已经到期：run.expires_at <= datetime.now(UTC)；成立时执行缩进的分支。
        if run.expires_at <= datetime.now(UTC):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        # 把等待执行“查找 方案版本号（plan_version）”得到的结果完成后的结果保存到 版本号（version）。
        version = await repo.find_plan_version(session, user_id, plan_version_id, for_update=True)
        # 检查当前记录版本是否符合请求要求：version is None or version.run_id != run.id；成立时执行缩进的分支。
        if version is None or version.run_id != run.id:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 检查当前记录版本是否符合请求要求：run.latest_plan_version_id != version.id；成立时执行缩进的分支。
        if run.latest_plan_version_id != version.id:
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_STATUS_INVALID", "只能确认当前最新方案")
        # 检查当前记录版本是否符合请求要求：version.status != 'WAITING_CONFIRM'；成立时执行缩进的分支。
        if version.status != "WAITING_CONFIRM":
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_STATUS_INVALID", "该方案版本不处于待确认状态")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run, version)
        # 把执行“调用 hashlib.sha256(secrets.token_bytes(32)).hexdigest 完成hexdigest处理”得到的结果保存到 身份凭据（token）。
        token = hashlib.sha256(secrets.token_bytes(32)).hexdigest()
        # 把固定状态或编码 CONFIRMED（已确认）保存到 版本号中的状态（version.status）。
        version.status = "CONFIRMED"
        # 把身份凭据（token）保存到 版本号中的confirmation身份凭据（version.confirmation_token）。
        version.confirmation_token = token
        # 把stamp保存到 版本号中的已确认at（version.confirmed_at）。
        version.confirmed_at = stamp
        # 把stamp保存到 版本号中的更新时at（version.updated_at）。
        version.updated_at = stamp
        # 把 version.version 加上 固定数值 1，再保存回原变量。
        version.version += 1
        # 把固定状态或编码 CONFIRMED（已确认）保存到 方案运行中的状态（run.status）。
        run.status = "CONFIRMED"
        # 把stamp保存到 方案运行中的已确认at（run.confirmed_at）。
        run.confirmed_at = stamp
        # 把 run.state_version 加上 固定数值 1，再保存回原变量。
        run.state_version += 1
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
        event_no = await repo.next_event_no(session, run.id)
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入事件no（event_no），值为事件no（event_no）。
            event_no=event_no,
            # 传入事件类型（event_type），值为固定状态或编码 CONFIRMED（已确认）。
            event_type="CONFIRMED",
            # 传入提交数据（payload），值为表达式 {"planVersionId": str(version.id)} 的结果。
            payload={"planVersionId": str(version.id)},
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(version)
        # 把版本号（version）交还给调用方，本函数到此结束。
        return version


# 异步函数 cancel_run：取消 方案运行（run）；接收 Agent 数据库访问器（db）、用户编号（user_id）、方案运行编号（run_id）。
async def cancel_run(db: Database, *, user_id: int, run_id: int) -> None:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 检查条件 run.status not in ('DRAFT', 'WAITING_CONFIRM', 'CONFIRMED') 是否成立；成立时执行缩进的分支。
        if run.status not in ("DRAFT", "WAITING_CONFIRM", "CONFIRMED"):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_STATUS_INVALID", "当前状态不能取消")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run)
        # 把固定状态或编码 CANCELLED（已取消）保存到 方案运行中的状态（run.status）。
        run.status = "CANCELLED"
        # 把stamp保存到 方案运行中的finishedat（run.finished_at）。
        run.finished_at = stamp
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 从等待执行“列出 versions”得到的结果完成后的结果依次取出版本号（version），每项执行一次下面的处理。
        for version in await repo.list_versions(session, run.id):
            # 检查当前记录版本是否符合请求要求：version.status in ('DRAFT', 'WAITING_CONFIRM')；成立时执行缩进的分支。
            if version.status in ("DRAFT", "WAITING_CONFIRM"):
                # 把固定状态或编码 REJECTED（REJECTED）保存到 版本号中的状态（version.status）。
                version.status = "REJECTED"
                # 把stamp保存到 版本号中的supersededat（version.superseded_at）。
                version.superseded_at = stamp
                # 把stamp保存到 版本号中的更新时at（version.updated_at）。
                version.updated_at = stamp
                # 把 version.version 加上 固定数值 1，再保存回原变量。
                version.version += 1
        # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
        event_no = await repo.next_event_no(session, run.id)
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合表达式 session, run_id=run.id, user_id=user_id, event_no=event_no, event_type="CANCELLED" 的结果，完成上面尚未结束的表达式。
            session, run_id=run.id, user_id=user_id, event_no=event_no, event_type="CANCELLED"
        )


# 异步函数 begin_commit：调用 begin_commit 完成begin提交回执处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、方案运行编号（run_id）。
async def begin_commit(db: Database, *, user_id: int, run_id: int) -> PlanCommit:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id, for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 检查当前记录版本是否符合请求要求：run.latest_plan_version_id is not None；成立时执行缩进的分支。
        if run.latest_plan_version_id is not None:
            # 把等待执行“查找 提交回执按版本号（commit_by_version）”得到的结果完成后的结果保存到 existing。
            existing = await repo.find_commit_by_version(session, run.latest_plan_version_id)
            # 检查条件 existing is not None and existing.status == 'SUCCEEDED' 是否成立；成立时执行缩进的分支。
            if existing is not None and existing.status == "SUCCEEDED":
                # 把existing交还给调用方，本函数到此结束。
                return existing
        # 检查条件 run.status not in ('CONFIRMED', 'EXECUTING', 'FAILED') 是否成立；成立时执行缩进的分支。
        if run.status not in ("CONFIRMED", "EXECUTING", "FAILED"):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_STATUS_INVALID", "方案尚未确认，不能提交")
        # 检查当前记录版本是否符合请求要求：run.latest_plan_version_id is None；成立时执行缩进的分支。
        if run.latest_plan_version_id is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把等待执行“查找 方案版本号（plan_version）”得到的结果完成后的结果保存到 版本号（version）。
        version = await repo.find_plan_version(
            # 继续传入或组合表达式 session, user_id, run.latest_plan_version_id, for_update=True 的结果，完成上面尚未结束的表达式。
            session, user_id, run.latest_plan_version_id, for_update=True
        )
        # 检查当前记录版本是否符合请求要求：version is None or version.status != 'CONFIRMED'；成立时执行缩进的分支。
        if version is None or version.status != "CONFIRMED":
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_STATUS_INVALID", "方案版本未确认")
        # 把等待执行“查找 提交回执按版本号（commit_by_version）”得到的结果完成后的结果保存到 existing。
        existing = await repo.find_commit_by_version(session, version.id)
        # 检查条件 existing is not None 是否成立；成立时执行缩进的分支。
        if existing is not None:
            # 检查条件 existing.status == 'FAILED' and existing.attempts >= MAX_COMMIT_ATTEMPTS 是否成立；成立时执行缩进的分支。
            if existing.status == "FAILED" and existing.attempts >= MAX_COMMIT_ATTEMPTS:
                # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
                raise _conflict("PLAN_COMMIT_EXHAUSTED", "提交尝试次数已用完，请重新生成方案")
            # 把固定状态或编码 EXECUTING（正在执行）保存到 方案运行中的状态（run.status）。
            run.status = "EXECUTING"
            # 把空值，表示当前还没有对应内容保存到 方案运行中的finishedat（run.finished_at）。
            run.finished_at = None
            # 把空值，表示当前还没有对应内容保存到 方案运行中的错误编码（run.error_code）。
            run.error_code = None
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 方案运行中的更新时at（run.updated_at）。
            run.updated_at = await database_time(session, run)
            # 把 run.version 加上 固定数值 1，再保存回原变量。
            run.version += 1
            # 把existing交还给调用方，本函数到此结束。
            return existing
        # 检查条件 run.status == 'FAILED' 是否成立；成立时执行缩进的分支。
        if run.status == "FAILED":
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("PLAN_COMMIT_MISSING", "失败状态没有可重试的提交记录，请重新生成方案")
        # 检查记录或凭据是否已经到期：run.expires_at <= datetime.now(UTC)；成立时执行缩进的分支。
        if run.expires_at <= datetime.now(UTC):
            # 中止本轮正常处理，抛出 执行“调用 _conflict 完成conflict处理”得到的结果。
            raise _conflict("WORKFLOW_EXPIRED", "方案已过期，请重新生成")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run)
        # 把执行“调用 hashlib.sha256(f'{run.id}:{version.id}:{snapshot_hash(version.snapshot)}'.encode()).hexdigest 完成hexdigest处理”得到的结果保存到 键（key）。
        key = hashlib.sha256(
            # 继续传入或组合把原始密码编码成不可直接还原的密码摘要，完成上面尚未结束的表达式。
            f"{run.id}:{version.id}:{snapshot_hash(version.snapshot)}".encode()
        # 继续传入或组合调用 ).hexdigest 完成hexdigest处理，完成上面尚未结束的表达式。
        ).hexdigest()
        # 把等待执行“创建 提交回执（commit）”得到的结果完成后的结果保存到 提交回执（commit）。
        commit = await repo.create_commit(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入方案版本号编号（plan_version_id），值为表达式 version.id 的结果。
            plan_version_id=version.id,
            # 传入幂等键（idempotency_key），值为键（key）。
            idempotency_key=key,
            # 传入请求摘要值（request_hash），值为调用 snapshot_hash 完成确认时的数据快照摘要值处理。
            request_hash=snapshot_hash(version.snapshot),
            # 传入状态（status），值为固定状态或编码 PENDING（等待处理）。
            status="PENDING",
        )
        # 把固定状态或编码 EXECUTING（正在执行）保存到 方案运行中的状态（run.status）。
        run.status = "EXECUTING"
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
        event_no = await repo.next_event_no(session, run.id)
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入事件no（event_no），值为事件no（event_no）。
            event_no=event_no,
            # 传入事件类型（event_type），值为固定状态或编码 COMMIT_STARTED（提交回执开始时）。
            event_type="COMMIT_STARTED",
            # 传入提交数据（payload），值为表达式 {"planCommitId": str(commit.id), "idempotencyKey": key} 的结果。
            payload={"planCommitId": str(commit.id), "idempotencyKey": key},
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(commit)
        # 把提交回执（commit）交还给调用方，本函数到此结束。
        return commit


# 异步函数 finish_commit：调用 finish_commit 完成finish提交回执处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、提交回执编号（commit_id）、成功（success）、结果（result）、错误编码（error_code）、错误消息（error_message）。
async def finish_commit(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写提交回执编号（commit_id）字段，值为int。
    commit_id: int,
    # 填写成功（success）字段，值为bool。
    success: bool,
    # 填写结果（result）字段，值为表达式 dict[str, Any] | None = None 的结果。
    result: dict[str, Any] | None = None,
    # 填写错误编码（error_code）字段，值为表达式 str | None = None 的结果。
    error_code: str | None = None,
    # 填写错误消息（error_message）字段，值为表达式 str | None = None 的结果。
    error_message: str | None = None,
# 继续传入或组合表达式 ) -> PlanCommit: 的结果，完成上面尚未结束的表达式。
) -> PlanCommit:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 提交回执（commit）。
        commit = await session.get(PlanCommit, commit_id)
        # 检查commit is None or commit.user_id != user_id，即对应内容是否不存在；成立时执行缩进的分支。
        if commit is None or commit.user_id != user_id:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, commit.run_id, for_update=True)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 提交记录由 Java 与业务事务一起写；Python 只投影工作流状态。
        # 网络超时不能覆盖 Java 的成功回执，也不能把未知结果伪装成失败。
        # 检查条件 commit.status not in ('SUCCEEDED', 'FAILED') 是否成立；成立时执行缩进的分支。
        if commit.status not in ("SUCCEEDED", "FAILED"):
            # 把错误编码（error_code）保存到 方案运行中的错误编码（run.error_code）。
            run.error_code = error_code
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 方案运行中的更新时at（run.updated_at）。
            run.updated_at = await database_time(session, run, commit)
            # 把提交回执（commit）交还给调用方，本函数到此结束。
            return commit
        # 把条件 commit.status == 'SUCCEEDED' 是否成立时使用固定状态或编码 SUCCEEDED（已成功），否则使用固定状态或编码 FAILED（失败）保存到 目标值（target）。
        target = "SUCCEEDED" if commit.status == "SUCCEEDED" else "FAILED"
        # 检查条件 run.status == target 是否成立；成立时执行缩进的分支。
        if run.status == target:
            # 把提交回执（commit）交还给调用方，本函数到此结束。
            return commit
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, run, commit)
        # 把目标值（target）保存到 方案运行中的状态（run.status）。
        run.status = target
        # 把提交回执（commit）的错误编码（error_code）属性保存到 方案运行中的错误编码（run.error_code）。
        run.error_code = commit.error_code
        # 把stamp保存到 方案运行中的finishedat（run.finished_at）。
        run.finished_at = stamp
        # 把stamp保存到 方案运行中的更新时at（run.updated_at）。
        run.updated_at = stamp
        # 把 run.version 加上 固定数值 1，再保存回原变量。
        run.version += 1
        # 把等待执行“调用 repo.next_event_no 完成下一个事件no处理”得到的结果完成后的结果保存到 事件no（event_no）。
        event_no = await repo.next_event_no(session, run.id)
        # 等待调用 repo.add_event 完成加入事件处理完成后再继续。
        await repo.add_event(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入方案运行编号（run_id），值为表达式 run.id 的结果。
            run_id=run.id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入事件no（event_no），值为事件no（event_no）。
            event_no=event_no,
            # 传入事件类型（event_type），值为固定文字 'COMMIT_SUCCEEDED" if target == "SUCCEEDED" else "COMMIT_FAILED'。
            event_type="COMMIT_SUCCEEDED" if target == "SUCCEEDED" else "COMMIT_FAILED",
            # 传入提交数据（payload），值为表达式 {"planCommitId": str(commit.id), "errorCode": commit.error_code} 的结果。
            payload={"planCommitId": str(commit.id), "errorCode": commit.error_code},
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(commit)
        # 把提交回执（commit）交还给调用方，本函数到此结束。
        return commit


# 异步函数 detail：调用 detail 完成详情处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、方案运行编号（run_id）。
async def detail(db: Database, *, user_id: int, run_id: int) -> dict[str, Any]:
    # 等待调用 expire_stale 完成过期stale处理完成后再继续。
    await expire_stale(db, user_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把当前记录版本是否符合请求要求：run.latest_plan_version_id时使用等待执行“查找 提交回执按版本号（commit_by_version）”得到的结果完成后的结果，否则使用空值，表示当前还没有对应内容保存到 提交回执（commit）。
        commit = (
            # 继续传入或组合等待 查找 提交回执按版本号（commit_by_version） 完成后取得结果，完成上面尚未结束的表达式。
            await repo.find_commit_by_version(session, run.latest_plan_version_id)
            # 继续传入或组合表达式 if run.latest_plan_version_id 的结果，完成上面尚未结束的表达式。
            if run.latest_plan_version_id
            # 继续传入或组合表达式 else None 的结果，完成上面尚未结束的表达式。
            else None
        )
        # 检查条件 commit is not None and commit.user_id != user_id 是否成立；成立时执行缩进的分支。
        if commit is not None and commit.user_id != user_id:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
    # 检查条件 commit is not None and commit.status in ('SUCCEEDED', 'FAILED') and (run.status == 'EXECUTING') 是否成立；成立时执行缩进的分支。
    if (
        # 继续传入或组合条件判断的结果：commit is not None，完成上面尚未结束的表达式。
        commit is not None
        # 继续补充本次调用或判断：表达式 and commit.status in ("SUCCEEDED", "FAILED") 的结果。
        and commit.status in ("SUCCEEDED", "FAILED")
        # 继续补充本次调用或判断：条件判断的结果：and run.status == "EXECUTING"。
        and run.status == "EXECUTING"
    ):
        # 等待调用 finish_commit 完成finish提交回执处理完成后再继续。
        await finish_commit(
            # 继续传入或组合条件判断的结果：db, user_id=user_id, commit_id=commit.id, success=commit.status == "SUCCEEDED"，完成上面尚未结束的表达式。
            db, user_id=user_id, commit_id=commit.id, success=commit.status == "SUCCEEDED"
        )
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id)
        # 检查run is None，即对应内容是否不存在；成立时执行缩进的分支。
        if run is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把等待执行“列出 versions”得到的结果完成后的结果保存到 versions。
        versions = await repo.list_versions(session, run.id)
        # 把等待执行“列出 事件列表（events）”得到的结果完成后的结果保存到 事件列表（events）。
        events = await repo.list_events(session, run.id)
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {
            # 填写方案运行（run）字段，值为调用 run_vo 完成方案运行处理。
            "run": run_vo(run),
            # 填写方案列表（plans）字段，值为表达式 [plan_vo(row) for row in versions] 的结果。
            "plans": [plan_vo(row) for row in versions],
            # 填写事件列表（events）字段，值为表达式 [event_vo(row) for row in events] 的结果。
            "events": [event_vo(row) for row in events],
            # 填写提交回执（commit）字段，值为调用 commit_vo 完成提交回执处理。
            "commit": commit_vo(commit) if commit is not None else None,
        }


# 异步函数 page_runs：调用 page_runs 完成当前页运行记录列表处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、状态（status）、当前页（page）、每页条数（size）、对话编号（conversation_id）。
async def page_runs(
    # 填写Agent 数据库访问器（db）字段，值为按表达式计算得到的值：Database, *, user_id: int, status: str | None, page: int, size: int。
    db: Database, *, user_id: int, status: str | None, page: int, size: int,
    # 填写对话编号（conversation_id）字段，值为表达式 int | None = None 的结果。
    conversation_id: int | None = None,
# 继续传入或组合表达式 ) -> tuple[list[WorkflowRun], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[WorkflowRun], int]:
    # 等待调用 expire_stale 完成过期stale处理完成后再继续。
    await expire_stale(db, user_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“调用 repo.page_runs 完成当前页运行记录列表处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await repo.page_runs(
            # 继续传入或组合表达式 session, user_id=user_id, status=status, page_number=page, size=size 的结果，完成上面尚未结束的表达式。
            session, user_id=user_id, status=status, page_number=page, size=size,
            # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
            conversation_id=conversation_id,
        )


# 异步函数 execute_commit：调用 Java 原子提交接口；已成功的幂等提交直接返回既有结果。；接收 Agent 数据库访问器（db）、上下文（context）、Java后端客户端（java_client）、方案运行编号（run_id）。
async def execute_commit(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写上下文（context）字段，值为成长助手上下文（AgentContext）。
    context: AgentContext,
    # 填写Java后端客户端（java_client）字段，值为Java后端接口客户端（JavaApiClient）。
    java_client: JavaApiClient,
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
# 继续传入或组合表达式 ) -> PlanCommit: 的结果，完成上面尚未结束的表达式。
) -> PlanCommit:
    # 调用 Java 原子提交接口；已成功的幂等提交直接返回既有结果。
    # 把执行“调用 int 完成int处理”得到的结果保存到 用户编号（user_id）。
    user_id = int(context.user_id)
    # 把等待执行“调用 begin_commit 完成begin提交回执处理”得到的结果完成后的结果保存到 提交回执（commit）。
    commit = await begin_commit(db, user_id=user_id, run_id=run_id)
    # 检查条件 commit.status == 'SUCCEEDED' 是否成立；成立时执行缩进的分支。
    if commit.status == "SUCCEEDED":
        # 把等待执行“调用 finish_commit 完成finish提交回执处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await finish_commit(db, user_id=user_id, commit_id=commit.id, success=True)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 方案运行（run）”得到的结果完成后的结果保存到 方案运行（run）。
        run = await repo.find_run(session, user_id, run_id)
        # 检查当前记录版本是否符合请求要求：run is None or run.latest_plan_version_id is None；成立时执行缩进的分支。
        if run is None or run.latest_plan_version_id is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把等待执行“查找 方案版本号（plan_version）”得到的结果完成后的结果保存到 版本号（version）。
        version = await repo.find_plan_version(session, user_id, run.latest_plan_version_id)
        # 检查当前记录版本是否符合请求要求：version is None；成立时执行缩进的分支。
        if version is None:
            # 中止本轮正常处理，抛出 执行“调用 _not_found 完成notfound处理”得到的结果。
            raise _not_found()
        # 把按字段名保存多个值的字典保存到 提交数据（payload）。
        payload = {
            # 填写工作流运行编号（runId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "runId": str(run.id),
            # 填写方案版本号编号（planVersionId）字段，值为把输入转换成文字，统一比较或展示时的格式。
            "planVersionId": str(version.id),
            # 填写防止重复提交的唯一键（idempotencyKey）字段，值为表达式 commit.idempotency_key 的结果。
            "idempotencyKey": commit.idempotency_key,
            # 填写请求内容的校验摘要（requestHash）字段，值为表达式 commit.request_hash 的结果。
            "requestHash": commit.request_hash,
            # 填写确认时的数据快照（snapshot）字段，值为表达式 version.snapshot 的结果。
            "snapshot": version.snapshot,
        }
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 java_client.post_agent 完成post成长助手处理”得到的结果完成后的结果保存到 结果（result）。
        result = await java_client.post_agent(
            # 继续传入或组合固定文字 'agent/plans/commit'，完成上面尚未结束的表达式。
            "agent/plans/commit",
            # 传入授权（authorization），值为表达式 context.authorization 的结果。
            authorization=context.authorization,
            # 传入追踪标识编号（trace_id），值为表达式 context.trace_id 的结果。
            trace_id=context.trace_id,
            # 传入提交数据（payload），值为提交数据（payload）。
            payload=payload,
        )
    # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
    except JavaApiError as error:
        # 把等待执行“调用 finish_commit 完成finish提交回执处理”得到的结果完成后的结果保存到 reconciled。
        reconciled = await finish_commit(
            # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
            db,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入提交回执编号（commit_id），值为表达式 commit.id 的结果。
            commit_id=commit.id,
            # 传入成功（success），值为假，表示条件不成立或开关关闭。
            success=False,
            # 传入错误编码（error_code），值为表达式 error.code 的结果。
            error_code=error.code,
            # 传入错误消息（error_message），值为表达式 error.message 的结果。
            error_message=error.message,
        )
        # 检查条件 reconciled.status == 'SUCCEEDED' 是否成立；成立时执行缩进的分支。
        if reconciled.status == "SUCCEEDED":
            # 把reconciled交还给调用方，本函数到此结束。
            return reconciled
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(error.status_code, error.code, error.message) from error
    # 把条件 isinstance(result.data, dict) 是否成立时使用结果（result）的数据（data）属性，否则使用按字段名保存多个值的字典，这里先创建空字典保存到 数据（data）。
    data = result.data if isinstance(result.data, dict) else {}
    # 检查条件 data.get('status') != 'SUCCEEDED' or str(data.get('commitId')) != str(commit.id) 是否成立；成立时执行缩进的分支。
    if data.get("status") != "SUCCEEDED" or str(data.get("commitId")) != str(commit.id):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "PLAN_COMMIT_RESULT_INVALID", "提交结果尚未核实，请刷新规划状态")
    # 把等待执行“调用 finish_commit 完成finish提交回执处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await finish_commit(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入提交回执编号（commit_id），值为表达式 commit.id 的结果。
        commit_id=commit.id,
        # 传入成功（success），值为真，表示条件成立或开关打开。
        success=True,
        # 传入结果（result），值为数据（data）。
        result=data,
    )
