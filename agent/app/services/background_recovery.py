# 校验失败记忆任务的来源和归属，再幂等地安排重试。
# 本人记忆后台任务的受控恢复；保留失败审计，重放返回后续任务。

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 后台任务（BackgroundTask）、对话本轮对话（ConversationTurn）、长期记忆（Memory）。
from app.db.models import BackgroundTask, ConversationTurn, Memory

# 从 app.repositories 引入 任务列表（tasks）。
from app.repositories import background_task_repository as tasks

# 从 app.repositories 引入 对话数据仓库（conversation_repository）。
from app.repositories import conversation_repository


# 异步函数 retry_memory：重试 长期记忆（memory）；接收 Agent 数据库访问器（db）、上下文（context）、任务编号（task_id）。
async def retry_memory(db, context, task_id):
    # 调用 context.require 完成require处理。
    context.require("agent:run:read", "agent:memory:manage")
    # 把执行“调用 int 完成int处理”得到的结果保存到 用户编号（user_id）。
    user_id = int(context.user_id)
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 当前记录（row）。
        row = await session.get(BackgroundTask, task_id, with_for_update=True)
        # 检查row is None or row.user_id != user_id，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None or row.user_id != user_id:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(404, "TASK_NOT_FOUND", "未找到可恢复的后台任务")
        # 检查条件 row.kind not in {'MEMORY_EXTRACT', 'MEMORY_EMBEDDING'} or row.status != 'FAILED' 是否成立；成立时执行缩进的分支。
        if row.kind not in {"MEMORY_EXTRACT", "MEMORY_EMBEDDING"} or row.status != "FAILED":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "TASK_RETRY_INVALID", "只有失败的记忆任务可在这里重试")
        # 检查条件 row.kind == 'MEMORY_EXTRACT' 是否成立；成立时执行缩进的分支。
        if row.kind == "MEMORY_EXTRACT":
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 来源（source）。
            source = await session.get(ConversationTurn, row.ref_id)
            # 检查source is None or source.user_id != user_id or source.status != 'COMPLETED'，即对应内容是否不存在；成立时执行缩进的分支。
            if source is None or source.user_id != user_id or source.status != "COMPLETED":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "MEMORY_SOURCE_UNAVAILABLE", "原对话轮次已不可用于提取记忆")
            # 等待要求满足 owned完成后再继续。
            await conversation_repository.require_owned(session, user_id, source.conversation_id)
        # 前面的条件不满足时，按这里的条件或备用分支继续处理。
        else:
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 来源（source）。
            source = await session.get(Memory, row.ref_id)
            # 检查source is None or source.user_id != user_id or source.status != 'CONFIRMED'，即对应内容是否不存在；成立时执行缩进的分支。
            if source is None or source.user_id != user_id or source.status != "CONFIRMED":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "MEMORY_SOURCE_UNAVAILABLE", "记忆已变化，请从当前记忆重新操作")
        # 把依次包含 条件 BackgroundTask.user_id == user_id 是否成立、条件 BackgroundTask.kind == row.kind 是否成立、条件 BackgroundTask.ref_type == row.ref_type 是否成立、条件 BackgroundTask.ref_id == row.ref_id 是否成立 的集合保存到 数据范围（scope）。
        scope = (BackgroundTask.user_id == user_id, BackgroundTask.kind == row.kind,
                 # 继续传入或组合条件判断的结果：BackgroundTask.ref_type == row.ref_type, BackgroundTask.ref_id == row.ref_id)，完成上面尚未结束的表达式。
                 BackgroundTask.ref_type == row.ref_type, BackgroundTask.ref_id == row.ref_id)
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 最新（latest）。
        latest = await session.scalar(select(BackgroundTask).where(*scope).order_by(BackgroundTask.id.desc()).limit(1))
        # 检查条件 latest is not None and latest.id > row.id and (latest.status != 'FAILED') 是否成立；成立时执行缩进的分支。
        if latest is not None and latest.id > row.id and latest.status != "FAILED":
            # 把最新（latest）交还给调用方，本函数到此结束。
            return latest
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 最后一次成功（last_success）。
        last_success = await session.scalar(select(func.max(BackgroundTask.id)).where(*scope,
                                                                                   # 继续传入或组合条件判断的结果：BackgroundTask.status == "SUCCEEDED"))，完成上面尚未结束的表达式。
                                                                                   BackgroundTask.status == "SUCCEEDED"))
        # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 failures。
        failures = await session.scalar(select(func.count()).select_from(BackgroundTask).where(*scope,
            # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：BackgroundTask.status == "FAILED", BackgroundTask.id > (last_success or 0)))，完成上面尚未结束的表达式。
            BackgroundTask.status == "FAILED", BackgroundTask.id > (last_success or 0)))
        # 检查条件 int(failures or 0) >= 10 是否成立；成立时执行缩进的分支。
        if int(failures or 0) >= 10:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "TASK_RETRY_EXHAUSTED", "连续重试仍失败，请先检查服务配置后再处理")
        # 把等待执行“调用 tasks.enqueue 完成enqueue处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await tasks.enqueue(session, user_id=user_id, kind=row.kind, ref_type=row.ref_type, ref_id=row.ref_id)
