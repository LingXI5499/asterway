# 封装规划工作流数据的查询与保存，只负责存储访问。
# 工作流运行、方案版本、事件与提交记录的数据访问。

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.db.models 引入 方案提交回执（PlanCommit）、方案版本号（PlanVersion）、规划工作流事件（WorkflowEvent）、规划工作流方案运行（WorkflowRun）。
from app.db.models import PlanCommit, PlanVersion, WorkflowEvent, WorkflowRun


# 异步函数 create_run：创建 方案运行（run）；接收 当前会话（session）。
async def create_run(session: AsyncSession, **values: object) -> WorkflowRun:
    # 把执行“调用 WorkflowRun 完成规划工作流方案运行处理”得到的结果保存到 当前记录（row）。
    row = WorkflowRun(**values)
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 find_run：查找 方案运行（run）；接收 当前会话（session）、用户编号（user_id）、方案运行编号（run_id）、for更新（for_update）。
async def find_run(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, user_id: int, run_id: int, *, for_update: bool = False。
    session: AsyncSession, user_id: int, run_id: int, *, for_update: bool = False
# 继续传入或组合表达式 ) -> WorkflowRun | None: 的结果，完成上面尚未结束的表达式。
) -> WorkflowRun | None:
    # 把执行“调用 select(WorkflowRun).where 完成where处理”得到的结果保存到 查询条件（query）。
    query = select(WorkflowRun).where(WorkflowRun.id == run_id, WorkflowRun.user_id == user_id)
    # 检查条件 for_update 是否成立；成立时执行缩进的分支。
    if for_update:
        # 把执行“调用 query.with_for_update 完成withfor更新处理”得到的结果保存到 查询条件（query）。
        query = query.with_for_update()
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(query)


# 异步函数 find_active_run：查找 当前有效方案运行（active_run）；接收 当前会话（session）、对话编号（conversation_id）。
async def find_active_run(session: AsyncSession, conversation_id: int) -> WorkflowRun | None:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(
        # 继续传入或组合调用 select(WorkflowRun).where 完成where处理，完成上面尚未结束的表达式。
        select(WorkflowRun).where(
            # 继续传入或组合条件判断的结果：WorkflowRun.conversation_id == conversation_id，完成上面尚未结束的表达式。
            WorkflowRun.conversation_id == conversation_id,
            # 继续传入或组合调用 WorkflowRun.status.in_ 完成in处理，完成上面尚未结束的表达式。
            WorkflowRun.status.in_(("DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING")),
        )
    )


# 异步函数 next_event_no：调用 next_event_no 完成下一个事件no处理；接收 当前会话（session）、方案运行编号（run_id）。
async def next_event_no(session: AsyncSession, run_id: int) -> int:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 值（value）。
    value = await session.scalar(
        # 继续传入或组合调用 select(func.coalesce 完成coalesce处理，完成上面尚未结束的表达式。
        select(func.coalesce(func.max(WorkflowEvent.event_no), 0)).where(
            # 继续传入或组合条件判断的结果：WorkflowEvent.run_id == run_id，完成上面尚未结束的表达式。
            WorkflowEvent.run_id == run_id
        )
    )
    # 把按 int(value or 0) + 1 计算的结果交还给调用方，本函数到此结束。
    return int(value or 0) + 1


# 异步函数 add_event：调用 add_event 完成加入事件处理；接收 当前会话（session）、方案运行编号（run_id）、用户编号（user_id）、事件no（event_no）、事件类型（event_type）、提交数据（payload）。
async def add_event(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写方案运行编号（run_id）字段，值为int。
    run_id: int,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写事件no（event_no）字段，值为int。
    event_no: int,
    # 填写事件类型（event_type）字段，值为str。
    event_type: str,
    # 填写提交数据（payload）字段，值为表达式 dict[str, object] | None = None 的结果。
    payload: dict[str, object] | None = None,
# 继续传入或组合表达式 ) -> WorkflowEvent: 的结果，完成上面尚未结束的表达式。
) -> WorkflowEvent:
    # 把执行“调用 WorkflowEvent 完成规划工作流事件处理”得到的结果保存到 当前记录（row）。
    row = WorkflowEvent(
        # 传入方案运行编号（run_id），值为方案运行编号（run_id）。
        run_id=run_id,
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入事件no（event_no），值为事件no（event_no）。
        event_no=event_no,
        # 传入事件类型（event_type），值为事件类型（event_type）。
        event_type=event_type,
        # 传入提交数据（payload），值为优先使用前面的有效值，没有时采用后面的备用值：payload or {}。
        payload=payload or {},
    )
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 next_version_no：调用 next_version_no 完成下一个版本号no处理；接收 当前会话（session）、方案运行编号（run_id）。
async def next_version_no(session: AsyncSession, run_id: int) -> int:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 值（value）。
    value = await session.scalar(
        # 继续传入或组合调用 select(func.coalesce 完成coalesce处理，完成上面尚未结束的表达式。
        select(func.coalesce(func.max(PlanVersion.version_no), 0)).where(
            # 继续传入或组合条件判断的结果：PlanVersion.run_id == run_id，完成上面尚未结束的表达式。
            PlanVersion.run_id == run_id
        )
    )
    # 把按 int(value or 0) + 1 计算的结果交还给调用方，本函数到此结束。
    return int(value or 0) + 1


# 异步函数 create_plan_version：创建 方案版本号（plan_version）；接收 当前会话（session）。
async def create_plan_version(session: AsyncSession, **values: object) -> PlanVersion:
    # 把执行“调用 PlanVersion 完成方案版本号处理”得到的结果保存到 当前记录（row）。
    row = PlanVersion(**values)
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 find_plan_version：查找 方案版本号（plan_version）；接收 当前会话（session）、用户编号（user_id）、方案版本号编号（plan_version_id）、for更新（for_update）。
async def find_plan_version(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写方案版本号编号（plan_version_id）字段，值为int。
    plan_version_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写for更新（for_update）字段，值为表达式 bool = False 的结果。
    for_update: bool = False,
# 继续传入或组合表达式 ) -> PlanVersion | None: 的结果，完成上面尚未结束的表达式。
) -> PlanVersion | None:
    # 把执行“调用 select(PlanVersion).where 完成where处理”得到的结果保存到 查询条件（query）。
    query = select(PlanVersion).where(
        # 继续传入或组合条件判断的结果：PlanVersion.id == plan_version_id, PlanVersion.user_id == user_id，完成上面尚未结束的表达式。
        PlanVersion.id == plan_version_id, PlanVersion.user_id == user_id
    )
    # 检查条件 for_update 是否成立；成立时执行缩进的分支。
    if for_update:
        # 把执行“调用 query.with_for_update 完成withfor更新处理”得到的结果保存到 查询条件（query）。
        query = query.with_for_update()
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(query)


# 异步函数 list_versions：列出 versions；接收 当前会话（session）、方案运行编号（run_id）。
async def list_versions(session: AsyncSession, run_id: int) -> list[PlanVersion]:
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select(PlanVersion).where 完成where处理，完成上面尚未结束的表达式。
        select(PlanVersion).where(PlanVersion.run_id == run_id).order_by(PlanVersion.version_no)
    )
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)


# 异步函数 list_events：列出 事件列表（events）；接收 当前会话（session）、方案运行编号（run_id）。
async def list_events(session: AsyncSession, run_id: int) -> list[WorkflowEvent]:
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select(WorkflowEvent).where 完成where处理，完成上面尚未结束的表达式。
        select(WorkflowEvent).where(WorkflowEvent.run_id == run_id).order_by(WorkflowEvent.event_no)
    )
    # 把执行“调用 list 完成列表处理”得到的结果交还给调用方，本函数到此结束。
    return list(rows)


# 异步函数 create_commit：创建 提交回执（commit）；接收 当前会话（session）。
async def create_commit(session: AsyncSession, **values: object) -> PlanCommit:
    # 把执行“调用 PlanCommit 完成方案提交回执处理”得到的结果保存到 当前记录（row）。
    row = PlanCommit(**values)
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 find_commit_by_version：查找 提交回执按版本号（commit_by_version）；接收 当前会话（session）、方案版本号编号（plan_version_id）。
async def find_commit_by_version(
    # 填写当前会话（session）字段，值为表达式 AsyncSession, plan_version_id: int 的结果。
    session: AsyncSession, plan_version_id: int
# 继续传入或组合表达式 ) -> PlanCommit | None: 的结果，完成上面尚未结束的表达式。
) -> PlanCommit | None:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(
        # 继续传入或组合调用 select(PlanCommit).where 完成where处理，完成上面尚未结束的表达式。
        select(PlanCommit).where(PlanCommit.plan_version_id == plan_version_id)
    )


# 异步函数 page_runs：调用 page_runs 完成当前页运行记录列表处理；接收 当前会话（session）、用户编号（user_id）、状态（status）、当前页数值（page_number）、每页条数（size）、对话编号（conversation_id）。
async def page_runs(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写状态（status）字段，值为表达式 str | None 的结果。
    status: str | None,
    # 填写当前页数值（page_number）字段，值为int。
    page_number: int,
    # 填写每页条数（size）字段，值为int。
    size: int,
    # 填写对话编号（conversation_id）字段，值为表达式 int | None = None 的结果。
    conversation_id: int | None = None,
# 继续传入或组合表达式 ) -> tuple[list[WorkflowRun], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[WorkflowRun], int]:
    # 把依次包含 条件 WorkflowRun.user_id == user_id 是否成立 的集合保存到 conditions。
    conditions = [WorkflowRun.user_id == user_id]
    # 检查条件 conversation_id is not None 是否成立；成立时执行缩进的分支。
    if conversation_id is not None:
        # 把新条目放到列表末尾。
        conditions.append(WorkflowRun.conversation_id == conversation_id)
    # 检查条件 status 是否成立；成立时执行缩进的分支。
    if status:
        # 把新条目放到列表末尾。
        conditions.append(WorkflowRun.status == status)
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
    total = await session.scalar(
        # 继续传入或组合调用 select(func.count 完成数量处理，完成上面尚未结束的表达式。
        select(func.count()).select_from(WorkflowRun).where(*conditions)
    )
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(WorkflowRun)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(*conditions)
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(WorkflowRun.updated_at.desc(), WorkflowRun.id.desc())
        # 继续补充本次调用或判断：调用 offset 完成分页时跳过的记录数处理。
        .offset((page_number - 1) * size)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(size)
    )
    # 把依次包含 执行“调用 list 完成列表处理”得到的结果、执行“调用 int 完成int处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return list(rows), int(total or 0)
