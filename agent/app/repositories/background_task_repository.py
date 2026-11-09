# 封装后台任务数据的查询与保存，只负责存储访问。
# 后台任务数据访问：单 worker 领取、状态推进与幂等入队。

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）。
from datetime import UTC, datetime

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 后台任务（BackgroundTask）。
from app.db.models import BackgroundTask

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 把依次包含 固定状态或编码 SUCCEEDED（已成功）、固定状态或编码 FAILED（失败）、固定状态或编码 CANCELLED（已取消） 的集合保存到 TERMINAL（_TERMINAL）。
_TERMINAL = {"SUCCEEDED", "FAILED", "CANCELLED"}


# 函数 _stamp：调用 _stamp 完成stamp处理；接收 当前记录（row）。
def _stamp(row: BackgroundTask) -> datetime:
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(datetime.now(UTC), row.created_at)


# 异步函数 enqueue：调用 enqueue 完成enqueue处理；接收 当前会话（session）、用户编号（user_id）、种类（kind）、响应式引用类型（ref_type）、响应式引用编号（ref_id）。
async def enqueue(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 填写种类（kind）字段，值为str。
    kind: str,
    # 填写响应式引用类型（ref_type）字段，值为str。
    ref_type: str,
    # 填写响应式引用编号（ref_id）字段，值为int。
    ref_id: int,
# 继续传入或组合表达式 ) -> BackgroundTask: 的结果，完成上面尚未结束的表达式。
) -> BackgroundTask:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 existing。
    existing = await session.scalar(
        # 继续传入或组合调用 select(BackgroundTask).where 完成where处理，完成上面尚未结束的表达式。
        select(BackgroundTask).where(
            # 继续传入或组合条件判断的结果：BackgroundTask.user_id == user_id，完成上面尚未结束的表达式。
            BackgroundTask.user_id == user_id,
            # 继续传入或组合条件判断的结果：BackgroundTask.kind == kind，完成上面尚未结束的表达式。
            BackgroundTask.kind == kind,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_type == ref_type，完成上面尚未结束的表达式。
            BackgroundTask.ref_type == ref_type,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_id == ref_id，完成上面尚未结束的表达式。
            BackgroundTask.ref_id == ref_id,
            # 继续传入或组合调用 BackgroundTask.status.in_ 完成in处理，完成上面尚未结束的表达式。
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )
    # 检查条件 existing is not None 是否成立；成立时执行缩进的分支。
    if existing is not None:
        # 检查条件 existing.status == 'RUNNING' 是否成立；成立时执行缩进的分支。
        if existing.status == "RUNNING":
            # 内容已改变时重新排队；旧领取者不能覆盖这次更新。
            # 把固定状态或编码 PENDING（等待处理）保存到 existing中的状态（existing.status）。
            existing.status = "PENDING"
            # 把空值，表示当前还没有对应内容保存到 existing中的开始时at（existing.started_at）。
            existing.started_at = None
            # 把空值，表示当前还没有对应内容保存到 existing中的finishedat（existing.finished_at）。
            existing.finished_at = None
            # 把空值，表示当前还没有对应内容保存到 existing中的错误编码（existing.error_code）。
            existing.error_code = None
            # 把空值，表示当前还没有对应内容保存到 existing中的错误消息（existing.error_message）。
            existing.error_message = None
            # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 existing中的更新时at（existing.updated_at）。
            existing.updated_at = await database_time(session, existing)
            # 把 existing.version 加上 固定数值 1，再保存回原变量。
            existing.version += 1
        # 把existing交还给调用方，本函数到此结束。
        return existing
    # 把执行“调用 BackgroundTask 完成后台任务处理”得到的结果保存到 当前记录（row）。
    row = BackgroundTask(
        # 传入用户编号（user_id），值为用户编号（user_id）。
        user_id=user_id,
        # 传入种类（kind），值为种类（kind）。
        kind=kind,
        # 传入响应式引用类型（ref_type），值为响应式引用类型（ref_type）。
        ref_type=ref_type,
        # 传入响应式引用编号（ref_id），值为响应式引用编号（ref_id）。
        ref_id=ref_id,
        # 传入状态（status），值为固定状态或编码 PENDING（等待处理）。
        status="PENDING",
    )
    # 加入集合或登记一条待保存记录。
    session.add(row)
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 claim_next：调用 claim_next 完成claim下一个处理；接收 当前会话（session）。
async def claim_next(session: AsyncSession) -> BackgroundTask | None:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 当前记录（row）。
    row = await session.scalar(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(BackgroundTask)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(
            # 继续传入或组合条件判断的结果：BackgroundTask.status == "PENDING"，完成上面尚未结束的表达式。
            BackgroundTask.status == "PENDING",
            # 继续传入或组合当前真实时间，用于记录操作发生的时刻，完成上面尚未结束的表达式。
            BackgroundTask.scheduled_at <= func.clock_timestamp(),
        )
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(BackgroundTask.scheduled_at, BackgroundTask.id)
        # 继续补充本次调用或判断：调用 with_for_update 完成withfor更新处理。
        .with_for_update(skip_locked=True)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(1)
    )
    # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
    if row is None:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 把固定状态或编码 RUNNING（运行中）保存到 当前记录中的状态（row.status）。
    row.status = "RUNNING"
    # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 当前记录中的开始时at（row.started_at）。
    row.started_at = await database_time(session, row)
    # 把当前记录（row）的开始时at（started_at）属性保存到 当前记录中的更新时at（row.updated_at）。
    row.updated_at = row.started_at
    # 把 row.attempts 加上 固定数值 1，再保存回原变量。
    row.attempts += 1
    # 把 row.version 加上 固定数值 1，再保存回原变量。
    row.version += 1
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 finish：调用 finish 完成finish处理；接收 当前会话（session）、当前记录（row）、状态（status）、错误编码（error_code）、错误消息（error_message）。
async def finish(
    # 填写当前会话（session）字段，值为单次异步数据库会话（AsyncSession）。
    session: AsyncSession,
    # 填写当前记录（row）字段，值为后台任务（BackgroundTask）。
    row: BackgroundTask,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写状态（status）字段，值为str。
    status: str,
    # 填写错误编码（error_code）字段，值为表达式 str | None = None 的结果。
    error_code: str | None = None,
    # 填写错误消息（error_message）字段，值为表达式 str | None = None 的结果。
    error_message: str | None = None,
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
    stamp = await database_time(session, row)
    # 把状态（status）保存到 当前记录中的状态（row.status）。
    row.status = status
    # 把错误编码（error_code）保存到 当前记录中的错误编码（row.error_code）。
    row.error_code = error_code
    # 把错误消息（error_message）保存到 当前记录中的错误消息（row.error_message）。
    row.error_message = error_message
    # 把stamp保存到 当前记录中的finishedat（row.finished_at）。
    row.finished_at = stamp
    # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
    row.updated_at = stamp
    # 把 row.version 加上 固定数值 1，再保存回原变量。
    row.version += 1
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()


# 异步函数 cancel_for_ref：取消 for响应式引用（for_ref）；接收 当前会话（session）、种类（kind）、响应式引用类型（ref_type）、响应式引用编号（ref_id）。
async def cancel_for_ref(session: AsyncSession, *, kind: str, ref_type: str, ref_id: int) -> None:

    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select(BackgroundTask).where 完成where处理，完成上面尚未结束的表达式。
        select(BackgroundTask).where(
            # 继续传入或组合条件判断的结果：BackgroundTask.kind == kind，完成上面尚未结束的表达式。
            BackgroundTask.kind == kind,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_type == ref_type，完成上面尚未结束的表达式。
            BackgroundTask.ref_type == ref_type,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_id == ref_id，完成上面尚未结束的表达式。
            BackgroundTask.ref_id == ref_id,
            # 继续传入或组合调用 BackgroundTask.status.in_ 完成in处理，完成上面尚未结束的表达式。
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )
    # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
    for row in rows:
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 把固定状态或编码 CANCELLED（已取消）保存到 当前记录中的状态（row.status）。
        row.status = "CANCELLED"
        # 把stamp保存到 当前记录中的finishedat（row.finished_at）。
        row.finished_at = stamp
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()


# 异步函数 find_active：查找 页面是否仍处于活动状态（active）；接收 当前会话（session）、种类（kind）、响应式引用类型（ref_type）、响应式引用编号（ref_id）。
async def find_active(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, *, kind: str, ref_type: str, ref_id: int。
    session: AsyncSession, *, kind: str, ref_type: str, ref_id: int
# 继续传入或组合表达式 ) -> BackgroundTask | None: 的结果，完成上面尚未结束的表达式。
) -> BackgroundTask | None:
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await session.scalar(
        # 继续传入或组合调用 select(BackgroundTask).where 完成where处理，完成上面尚未结束的表达式。
        select(BackgroundTask).where(
            # 继续传入或组合条件判断的结果：BackgroundTask.kind == kind，完成上面尚未结束的表达式。
            BackgroundTask.kind == kind,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_type == ref_type，完成上面尚未结束的表达式。
            BackgroundTask.ref_type == ref_type,
            # 继续传入或组合条件判断的结果：BackgroundTask.ref_id == ref_id，完成上面尚未结束的表达式。
            BackgroundTask.ref_id == ref_id,
            # 继续传入或组合调用 BackgroundTask.status.in_ 完成in处理，完成上面尚未结束的表达式。
            BackgroundTask.status.in_(("PENDING", "RUNNING")),
        )
    )


# 异步函数 recover_stale_running：把超时 RUNNING 任务退回 PENDING；达到最大尝试次数时置为 FAILED。 返回本次变更的任务，供后台循环补齐文档、复盘等关联资源状态。；接收 当前会话（session）、超时秒数（timeout_seconds）、最大尝试次数（max_attempts）。
async def recover_stale_running(
    # 填写当前会话（session）字段，值为按表达式计算得到的值：AsyncSession, *, timeout_seconds: float, max_attempts: int。
    session: AsyncSession, *, timeout_seconds: float, max_attempts: int
# 继续传入或组合表达式 ) -> list[BackgroundTask]: 的结果，完成上面尚未结束的表达式。
) -> list[BackgroundTask]:
    # 把超时 RUNNING 任务退回 PENDING；达到最大尝试次数时置为 FAILED。
    # 
    # 返回本次变更的任务，供后台循环补齐文档、复盘等关联资源状态。
    # 

    # 从 datetime 引入 两个时刻之间的时间长度（timedelta）。
    from datetime import timedelta

    # 从 sqlalchemy 引入 选择（select）。
    from sqlalchemy import select

    # 把按 func.clock_timestamp() - timedelta(seconds=timeout_seconds) 计算的结果保存到 cutoff。
    cutoff = func.clock_timestamp() - timedelta(seconds=timeout_seconds)
    # 把执行“调用 list 完成列表处理”得到的结果保存到 记录列表（rows）。
    rows = list(
        # 继续传入或组合等待 取得查询结果的第一列对象 完成后取得结果，完成上面尚未结束的表达式。
        await session.scalars(
            # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
            select(BackgroundTask)
            # 继续补充本次调用或判断：调用 where 完成where处理。
            .where(
                # 继续传入或组合条件判断的结果：BackgroundTask.status == "RUNNING"，完成上面尚未结束的表达式。
                BackgroundTask.status == "RUNNING",
                # 继续传入或组合条件判断的结果：BackgroundTask.started_at < cutoff，完成上面尚未结束的表达式。
                BackgroundTask.started_at < cutoff,
            )
            # 继续补充本次调用或判断：调用 with_for_update 完成withfor更新处理。
            .with_for_update(skip_locked=True)
        )
    )
    # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
    for row in rows:
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 检查条件 row.attempts >= max_attempts 是否成立；成立时执行缩进的分支。
        if row.attempts >= max_attempts:
            # 把固定状态或编码 FAILED（失败）保存到 当前记录中的状态（row.status）。
            row.status = "FAILED"
            # 把固定状态或编码 TASK_TIMEOUT（任务超时）保存到 当前记录中的错误编码（row.error_code）。
            row.error_code = "TASK_TIMEOUT"
            # 把固定文字 '后台任务超时且超过最大尝试次数'保存到 当前记录中的错误消息（row.error_message）。
            row.error_message = "后台任务超时且超过最大尝试次数"
            # 把stamp保存到 当前记录中的finishedat（row.finished_at）。
            row.finished_at = stamp
        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
        else:
            # 把固定状态或编码 PENDING（等待处理）保存到 当前记录中的状态（row.status）。
            row.status = "PENDING"
            # 把空值，表示当前还没有对应内容保存到 当前记录中的开始时at（row.started_at）。
            row.started_at = None
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
    # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
    await session.flush()
    # 把记录列表（rows）交还给调用方，本函数到此结束。
    return rows


# 异步函数 require_claim：写入结果前验证领取代次，防止超时或取消后的旧任务覆盖新结果。；接收 当前会话（session）、任务（task）。
async def require_claim(session: AsyncSession, task: BackgroundTask) -> BackgroundTask:
    # 写入结果前验证领取代次，防止超时或取消后的旧任务覆盖新结果。
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
    row = await session.get(BackgroundTask, task.id, with_for_update=True)
    # 检查当前记录版本是否符合请求要求：row is None or row.status != 'RUNNING' or row.user_id != task.user_id or (row.attempts != task.attempts) or (row.version != task.version)；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 row is None 的结果，完成上面尚未结束的表达式。
        row is None
        # 继续补充本次调用或判断：条件判断的结果：or row.status != "RUNNING"。
        or row.status != "RUNNING"
        # 继续补充本次调用或判断：条件判断的结果：or row.user_id != task.user_id。
        or row.user_id != task.user_id
        # 继续补充本次调用或判断：条件判断的结果：or row.attempts != task.attempts。
        or row.attempts != task.attempts
        # 继续补充本次调用或判断：条件判断的结果：or row.version != task.version。
        or row.version != task.version
    ):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "TASK_CLAIM_LOST", "后台任务已取消或由新的执行接管")
    # 把当前记录（row）交还给调用方，本函数到此结束。
    return row


# 异步函数 page_tasks：调用 page_tasks 完成当前页任务列表处理；接收 当前会话（session）、用户编号（user_id）、状态（status）、当前页数值（page_number）、每页条数（size）。
async def page_tasks(
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
# 继续传入或组合表达式 ) -> tuple[list[BackgroundTask], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[BackgroundTask], int]:

    # 从 sqlalchemy 引入 func。
    from sqlalchemy import func

    # 把依次包含 条件 BackgroundTask.user_id == user_id 是否成立 的集合保存到 conditions。
    conditions = [BackgroundTask.user_id == user_id]
    # 检查条件 status 是否成立；成立时执行缩进的分支。
    if status:
        # 把新条目放到列表末尾。
        conditions.append(BackgroundTask.status == status)
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 总数（total）。
    total = await session.scalar(
        # 继续传入或组合调用 select(func.count 完成数量处理，完成上面尚未结束的表达式。
        select(func.count()).select_from(BackgroundTask).where(*conditions)
    )
    # 把等待执行“取得查询结果的第一列对象”得到的结果完成后的结果保存到 记录列表（rows）。
    rows = await session.scalars(
        # 继续传入或组合调用 select 完成选择处理，完成上面尚未结束的表达式。
        select(BackgroundTask)
        # 继续补充本次调用或判断：调用 where 完成where处理。
        .where(*conditions)
        # 继续补充本次调用或判断：调用 order_by 完成顺序按处理。
        .order_by(BackgroundTask.created_at.desc(), BackgroundTask.id.desc())
        # 继续补充本次调用或判断：调用 offset 完成分页时跳过的记录数处理。
        .offset((page_number - 1) * size)
        # 继续补充本次调用或判断：调用 limit 完成上限处理。
        .limit(size)
    )
    # 把依次包含 执行“调用 list 完成列表处理”得到的结果、执行“调用 int 完成int处理”得到的结果 的集合交还给调用方，本函数到此结束。
    return list(rows), int(total or 0)


# 把依次包含 固定文字 'cancel_for_ref'、固定文字 'claim_next'、固定文字 'enqueue'、固定文字 'find_active'、固定文字 'finish'等内容 的集合保存到 全部（__all__）。
__all__ = [
    # 继续传入或组合固定文字 'cancel_for_ref'，完成上面尚未结束的表达式。
    "cancel_for_ref",
    # 继续传入或组合固定文字 'claim_next'，完成上面尚未结束的表达式。
    "claim_next",
    # 继续传入或组合固定文字 'enqueue'，完成上面尚未结束的表达式。
    "enqueue",
    # 继续传入或组合固定文字 'find_active'，完成上面尚未结束的表达式。
    "find_active",
    # 继续传入或组合固定文字 'finish'，完成上面尚未结束的表达式。
    "finish",
    # 继续传入或组合固定文字 'page_tasks'，完成上面尚未结束的表达式。
    "page_tasks",
    # 继续传入或组合固定文字 'recover_stale_running'，完成上面尚未结束的表达式。
    "recover_stale_running",
]
