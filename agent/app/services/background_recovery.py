"""本人记忆后台任务的受控恢复；保留失败审计，重放返回后续任务。"""

from sqlalchemy import func, select

from app.core.errors import AgentError
from app.db.models import BackgroundTask, ConversationTurn, Memory
from app.repositories import background_task_repository as tasks
from app.repositories import conversation_repository


async def retry_memory(db, context, task_id):
    context.require("agent:run:read", "agent:memory:manage")
    user_id = int(context.user_id)
    async with db.session() as session:
        row = await session.get(BackgroundTask, task_id, with_for_update=True)
        if row is None or row.user_id != user_id:
            raise AgentError(404, "TASK_NOT_FOUND", "未找到可恢复的后台任务")
        if row.kind not in {"MEMORY_EXTRACT", "MEMORY_EMBEDDING"} or row.status != "FAILED":
            raise AgentError(409, "TASK_RETRY_INVALID", "只有失败的记忆任务可在这里重试")
        if row.kind == "MEMORY_EXTRACT":
            source = await session.get(ConversationTurn, row.ref_id)
            if source is None or source.user_id != user_id or source.status != "COMPLETED":
                raise AgentError(409, "MEMORY_SOURCE_UNAVAILABLE", "原对话轮次已不可用于提取记忆")
            await conversation_repository.require_owned(session, user_id, source.conversation_id)
        else:
            source = await session.get(Memory, row.ref_id)
            if source is None or source.user_id != user_id or source.status != "CONFIRMED":
                raise AgentError(409, "MEMORY_SOURCE_UNAVAILABLE", "记忆已变化，请从当前记忆重新操作")
        scope = (BackgroundTask.user_id == user_id, BackgroundTask.kind == row.kind,
                 BackgroundTask.ref_type == row.ref_type, BackgroundTask.ref_id == row.ref_id)
        latest = await session.scalar(select(BackgroundTask).where(*scope).order_by(BackgroundTask.id.desc()).limit(1))
        if latest is not None and latest.id > row.id and latest.status != "FAILED":
            return latest
        last_success = await session.scalar(select(func.max(BackgroundTask.id)).where(*scope,
                                                                                   BackgroundTask.status == "SUCCEEDED"))
        failures = await session.scalar(select(func.count()).select_from(BackgroundTask).where(*scope,
            BackgroundTask.status == "FAILED", BackgroundTask.id > (last_success or 0)))
        if int(failures or 0) >= 10:
            raise AgentError(409, "TASK_RETRY_EXHAUSTED", "连续重试仍失败，请先检查服务配置后再处理")
        return await tasks.enqueue(session, user_id=user_id, kind=row.kind, ref_type=row.ref_type, ref_id=row.ref_id)
