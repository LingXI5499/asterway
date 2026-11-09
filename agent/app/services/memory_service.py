# 维护用户自己的候选记忆、确认、拒绝和归档状态。
# 长期记忆业务：候选、确认、修改、拒绝、归档、删除与跨会话上下文。

# 引入 logging，供下面调用其功能。
import logging

# 从 datetime 引入 协调世界时（UTC）、日期时间类型（datetime）。
from datetime import UTC, datetime

# 从 sqlalchemy 引入 sql删除（sql_delete）。
from sqlalchemy import delete as sql_delete

# 从 sqlalchemy 引入 选择（select）。
from sqlalchemy import select

# 从 sqlalchemy.exc 引入 Integrity错误（IntegrityError）、SQLAlchemy错误（SQLAlchemyError）。
from sqlalchemy.exc import IntegrityError, SQLAlchemyError

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.db.models 引入 对话（Conversation）、对话本轮对话（ConversationTurn）、长期记忆（Memory）、长期记忆向量（MemoryEmbedding）。
from app.db.models import Conversation, ConversationTurn, Memory, MemoryEmbedding

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.db.time 引入 数据库时间（database_time）。
from app.db.time import database_time

# 从 app.repositories 引入 任务repo（task_repo）。
from app.repositories import background_task_repository as task_repo

# 从 app.repositories 引入 repo。
from app.repositories import memory_repository as repo

# 从 app.schemas.memory 引入 长期记忆候选请求（MemoryCandidateRequest）、长期记忆（MemoryVO）。
from app.schemas.memory import MemoryCandidateRequest, MemoryVO

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)

# 把按字段名保存多个值的字典保存到 种类名称对应表（_KIND_LABELS）。
_KIND_LABELS = {
    # 填写PREFERENCE字段，值为固定文字 '偏好'。
    "PREFERENCE": "偏好",
    # 填写FACT字段，值为固定文字 '事实'。
    "FACT": "事实",
    # 填写约束（CONSTRAINT）字段，值为固定文字 '约束'。
    "CONSTRAINT": "约束",
    # 填写目标上下文（GOAL_CONTEXT）字段，值为固定文字 '目标背景'。
    "GOAL_CONTEXT": "目标背景",
}
# 把按字段名保存多个值的字典保存到 状态名称对应表（_STATUS_LABELS）。
_STATUS_LABELS = {
    # 填写候选（CANDIDATE）字段，值为固定文字 '待确认'。
    "CANDIDATE": "待确认",
    # 填写已确认（CONFIRMED）字段，值为固定文字 '已确认'。
    "CONFIRMED": "已确认",
    # 填写REJECTED字段，值为固定文字 '已拒绝'。
    "REJECTED": "已拒绝",
    # 填写已归档（ARCHIVED）字段，值为固定文字 '已归档'。
    "ARCHIVED": "已归档",
}


# 函数 memory_vo：调用 memory_vo 完成长期记忆处理；接收 当前记录（row）。
def memory_vo(row: Memory) -> MemoryVO:
    # 把执行“调用 MemoryVO 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return MemoryVO(
        # 传入编号（id），值为把输入转换成文字，统一比较或展示时的格式。
        id=str(row.id),
        # 传入种类（kind），值为表达式 row.kind 的结果。
        kind=row.kind,
        # 传入种类显示名称（kind_label），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        kind_label=_KIND_LABELS.get(row.kind, row.kind),
        # 传入内容（content），值为表达式 row.content 的结果。
        content=row.content,
        # 传入来源（source），值为表达式 row.source 的结果。
        source=row.source,
        # 传入状态（status），值为表达式 row.status 的结果。
        status=row.status,
        # 传入状态显示名称（status_label），值为按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
        status_label=_STATUS_LABELS.get(row.status, row.status),
        # 传入confidence，值为把输入转换成数值，供比较、计算或按编号查询。
        confidence=float(row.confidence),
        # 传入来源对话编号（source_conversation_id），值为把输入转换成文字，统一比较或展示时的格式。
        source_conversation_id=str(row.source_conversation_id)
        # 继续传入或组合表达式 if row.source_conversation_id 的结果，完成上面尚未结束的表达式。
        if row.source_conversation_id
        # 继续传入或组合表达式 else None 的结果，完成上面尚未结束的表达式。
        else None,
        # 传入来源本轮对话编号（source_turn_id），值为把输入转换成文字，统一比较或展示时的格式。
        source_turn_id=str(row.source_turn_id) if row.source_turn_id else None,
        # 传入supersedes编号（supersedes_id），值为把输入转换成文字，统一比较或展示时的格式。
        supersedes_id=str(row.supersedes_id) if row.supersedes_id else None,
        # 传入已确认at（confirmed_at），值为表达式 row.confirmed_at 的结果。
        confirmed_at=row.confirmed_at,
        # 传入rejectedat（rejected_at），值为表达式 row.rejected_at 的结果。
        rejected_at=row.rejected_at,
        # 传入已归档at（archived_at），值为表达式 row.archived_at 的结果。
        archived_at=row.archived_at,
        # 传入创建时at（created_at），值为表达式 row.created_at 的结果。
        created_at=row.created_at,
        # 传入更新时at（updated_at），值为表达式 row.updated_at 的结果。
        updated_at=row.updated_at,
        # 传入版本号（version），值为表达式 row.version 的结果。
        version=row.version,
    )


# 函数 _stamp：时钟回拨保护：状态时间必须落在行创建时间与更新时间之间。；接收 当前记录（row）。
def _stamp(row: Memory) -> datetime:
    # 时钟回拨保护：状态时间必须落在行创建时间与更新时间之间。
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(datetime.now(UTC), row.created_at)


# 函数 _missing：调用 _missing 完成missing处理。
def _missing() -> AgentError:
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
    return AgentError(404, "MEMORY_NOT_FOUND", "没有找到可访问的记忆")


# 函数 _version_conflict：调用 _version_conflict 完成版本号conflict处理。
def _version_conflict() -> AgentError:
    # 把执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果交还给调用方，本函数到此结束。
    return AgentError(409, "MEMORY_VERSION_CONFLICT", "记忆已被修改，请刷新后重试")


# 异步函数 list_memories：列出 长期记忆列表（memories）；接收 Agent 数据库访问器（db）、用户编号（user_id）、状态（status）、种类（kind）、keyword、当前页（page）、每页条数（size）。
async def list_memories(
    # 填写Agent 数据库访问器（db）字段，值为数据库（Database）。
    db: Database,
    # 填写用户编号（user_id）字段，值为int。
    user_id: int,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写状态（status）字段，值为表达式 str | None 的结果。
    status: str | None,
    # 填写种类（kind）字段，值为表达式 str | None 的结果。
    kind: str | None,
    # 填写keyword字段，值为表达式 str | None 的结果。
    keyword: str | None,
    # 填写当前页（page）字段，值为int。
    page: int,
    # 填写每页条数（size）字段，值为int。
    size: int,
# 继续传入或组合表达式 ) -> tuple[list[Memory], int]: 的结果，完成上面尚未结束的表达式。
) -> tuple[list[Memory], int]:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“调用 repo.page 完成当前页处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
        return await repo.page(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入状态（status），值为状态（status）。
            status=status,
            # 传入种类（kind），值为种类（kind）。
            kind=kind,
            # 传入keyword，值为keyword。
            keyword=keyword,
            # 传入当前页数值（page_number），值为当前页（page）。
            page_number=page,
            # 传入每页条数（size），值为每页条数（size）。
            size=size,
        )


# 异步函数 create_candidate：候选按内容幂等：已有候选或已确认的同一内容时直接返回既有记录。；接收 Agent 数据库访问器（db）、用户编号（user_id）、请求（request）。
async def create_candidate(db: Database, user_id: int, request: MemoryCandidateRequest) -> Memory:
    # 候选按内容幂等：已有候选或已确认的同一内容时直接返回既有记录。
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把请求（request）的对话编号（conversationId）属性保存到 对话编号（conversation_id）。
        conversation_id = request.conversationId
        # 检查条件 conversation_id is not None 是否成立；成立时执行缩进的分支。
        if conversation_id is not None:
            # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 对话（conversation）。
            conversation = await session.scalar(
                # 继续传入或组合调用 select(Conversation).where 完成where处理，完成上面尚未结束的表达式。
                select(Conversation).where(
                    # 继续传入或组合条件判断的结果：Conversation.id == conversation_id，完成上面尚未结束的表达式。
                    Conversation.id == conversation_id,
                    # 继续传入或组合条件判断的结果：Conversation.user_id == user_id，完成上面尚未结束的表达式。
                    Conversation.user_id == user_id,
                    # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
                    Conversation.deleted_at.is_(None),
                )
            )
            # 检查conversation is None，即对应内容是否不存在；成立时执行缩进的分支。
            if conversation is None:
                # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
                raise _missing()
        # 检查条件 request.turnId is not None 是否成立；成立时执行缩进的分支。
        if request.turnId is not None:
            # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 本轮对话（turn）。
            turn = await session.scalar(
                # 继续传入或组合调用 select(ConversationTurn).where 完成where处理，完成上面尚未结束的表达式。
                select(ConversationTurn).where(
                    # 继续传入或组合条件判断的结果：ConversationTurn.id == request.turnId, ConversationTurn.user_id == user_id，完成上面尚未结束的表达式。
                    ConversationTurn.id == request.turnId, ConversationTurn.user_id == user_id
                )
            )
            # 检查turn is None or conversation_id not in (None, turn.conversation_id)，即对应内容是否不存在；成立时执行缩进的分支。
            if turn is None or conversation_id not in (None, turn.conversation_id):
                # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
                raise _missing()
            # 把本轮对话（turn）的对话编号（conversation_id）属性保存到 对话编号（conversation_id）。
            conversation_id = turn.conversation_id
        # 检查条件 request.supersedesId is not None 是否成立；成立时执行缩进的分支。
        if request.supersedesId is not None:
            # 把等待执行“查找 owned”得到的结果完成后的结果保存到 上一次（previous）。
            previous = await repo.find_owned(session, user_id, request.supersedesId)
            # 检查previous is None，即对应内容是否不存在；成立时执行缩进的分支。
            if previous is None:
                # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
                raise _missing()
        # 把等待执行“查找 same内容（same_content）”得到的结果完成后的结果保存到 existing。
        existing = await repo.find_same_content(
            # 继续传入或组合表达式 session, user_id, request.content, ("CANDIDATE", "CONFIRMED") 的结果，完成上面尚未结束的表达式。
            session, user_id, request.content, ("CANDIDATE", "CONFIRMED")
        )
        # 检查条件 existing is not None 是否成立；成立时执行缩进的分支。
        if existing is not None:
            # 把existing交还给调用方，本函数到此结束。
            return existing
        # 把等待执行“调用 repo.create 完成创建处理”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.create(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入种类（kind），值为表达式 request.kind 的结果。
            kind=request.kind,
            # 传入内容（content），值为表达式 request.content 的结果。
            content=request.content,
            # 传入来源（source），值为表达式 request.source 的结果。
            source=request.source,
            # 传入confidence，值为表达式 request.confidence 的结果。
            confidence=request.confidence,
            # 传入来源对话编号（source_conversation_id），值为对话编号（conversation_id）。
            source_conversation_id=conversation_id,
            # 传入来源本轮对话编号（source_turn_id），值为表达式 request.turnId 的结果。
            source_turn_id=request.turnId,
            # 传入supersedes编号（supersedes_id），值为表达式 request.supersedesId 的结果。
            supersedes_id=request.supersedesId,
        )
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 update_content：更新 内容（content）；接收 Agent 数据库访问器（db）、用户编号（user_id）、长期记忆编号（memory_id）、内容（content）、版本号（version）。
async def update_content(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, memory_id: int, content: str, version: int 的结果。
    db: Database, user_id: int, memory_id: int, content: str, version: int
# 继续传入或组合表达式 ) -> Memory: 的结果，完成上面尚未结束的表达式。
) -> Memory:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
            raise _missing()
        # 检查当前记录版本是否符合请求要求：row.version != version；成立时执行缩进的分支。
        if row.version != version:
            # 中止本轮正常处理，抛出 执行“调用 _version_conflict 完成版本号conflict处理”得到的结果。
            raise _version_conflict()
        # 检查条件 row.status not in {'CANDIDATE', 'CONFIRMED'} 是否成立；成立时执行缩进的分支。
        if row.status not in {"CANDIDATE", "CONFIRMED"}:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_STATUS_INVALID", "当前状态的记忆不能修改内容")
        # 把等待执行“查找 same内容（same_content）”得到的结果完成后的结果保存到 duplicate。
        duplicate = await repo.find_same_content(
            # 继续传入或组合表达式 session, user_id, content, ("CANDIDATE", "CONFIRMED") 的结果，完成上面尚未结束的表达式。
            session, user_id, content, ("CANDIDATE", "CONFIRMED")
        )
        # 检查条件 duplicate is not None and duplicate.id != row.id 是否成立；成立时执行缩进的分支。
        if duplicate is not None and duplicate.id != row.id:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 把内容（content）保存到 当前记录中的内容（row.content）。
        row.content = content
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
            await session.flush()
        # 捕获IntegrityError异常，执行下面的错误处理，异常对象保存为 error。
        except IntegrityError as error:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆") from error
        # 检查条件 row.status == 'CONFIRMED' 是否成立；成立时执行缩进的分支。
        if row.status == "CONFIRMED":
            # 等待执行传入的数据库语句并取得结果完成后再继续。
            await session.execute(
                # 继续传入或组合调用 sql_delete(MemoryEmbedding).where 完成where处理，完成上面尚未结束的表达式。
                sql_delete(MemoryEmbedding).where(
                    # 继续传入或组合条件判断的结果：MemoryEmbedding.user_id == user_id, MemoryEmbedding.memory_id == row.id，完成上面尚未结束的表达式。
                    MemoryEmbedding.user_id == user_id, MemoryEmbedding.memory_id == row.id
                )
            )
            # 等待调用 task_repo.enqueue 完成enqueue处理完成后再继续。
            await task_repo.enqueue(
                # 继续传入或组合表达式 session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id 的结果，完成上面尚未结束的表达式。
                session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id
            )
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
            await session.flush()
        # 捕获IntegrityError异常，执行下面的错误处理，异常对象保存为 error。
        except IntegrityError as error:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的记忆") from error
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 confirm：调用 confirm 完成确认处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、长期记忆编号（memory_id）、版本号（version）。
async def confirm(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
            raise _missing()
        # 检查当前记录版本是否符合请求要求：row.version != version；成立时执行缩进的分支。
        if row.version != version:
            # 中止本轮正常处理，抛出 执行“调用 _version_conflict 完成版本号conflict处理”得到的结果。
            raise _version_conflict()
        # 检查条件 row.status != 'CANDIDATE' 是否成立；成立时执行缩进的分支。
        if row.status != "CANDIDATE":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有待确认的记忆可以确认")
        # 把等待执行“查找 same内容（same_content）”得到的结果完成后的结果保存到 duplicate。
        duplicate = await repo.find_same_content(session, user_id, row.content, ("CONFIRMED",))
        # 检查条件 duplicate is not None and duplicate.id != row.id 是否成立；成立时执行缩进的分支。
        if duplicate is not None and duplicate.id != row.id:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆")
        # 把空值，表示当前还没有对应内容保存到 上一次（previous）。
        previous = None
        # 检查条件 row.supersedes_id is not None 是否成立；成立时执行缩进的分支。
        if row.supersedes_id is not None:
            # 把等待执行“查找 owned”得到的结果完成后的结果保存到 上一次（previous）。
            previous = await repo.find_owned(session, user_id, row.supersedes_id, for_update=True)
            # 检查previous is None，即对应内容是否不存在；成立时执行缩进的分支。
            if previous is None:
                # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
                raise _missing()
            # 检查条件 previous.status != 'CONFIRMED' 是否成立；成立时执行缩进的分支。
            if previous.status != "CONFIRMED":
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(409, "MEMORY_STATUS_INVALID", "被替代的记忆状态已变化")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row, previous)
        # 检查条件 previous is not None 是否成立；成立时执行缩进的分支。
        if previous is not None:
            # 把固定状态或编码 ARCHIVED（已归档）保存到 上一次中的状态（previous.status）。
            previous.status = "ARCHIVED"
            # 把stamp保存到 上一次中的已归档at（previous.archived_at）。
            previous.archived_at = stamp
            # 把stamp保存到 上一次中的更新时at（previous.updated_at）。
            previous.updated_at = stamp
            # 把 previous.version 加上 固定数值 1，再保存回原变量。
            previous.version += 1
        # 把固定状态或编码 CONFIRMED（已确认）保存到 当前记录中的状态（row.status）。
        row.status = "CONFIRMED"
        # 把固定状态或编码 USER_CONFIRMED（用户已确认）保存到 当前记录中的来源（row.source）。
        row.source = "USER_CONFIRMED"
        # 把stamp保存到 当前记录中的已确认at（row.confirmed_at）。
        row.confirmed_at = stamp
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
            await session.flush()
        # 捕获IntegrityError异常，执行下面的错误处理，异常对象保存为 error。
        except IntegrityError as error:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆") from error
        # 等待调用 task_repo.enqueue 完成enqueue处理完成后再继续。
        await task_repo.enqueue(
            # 继续传入或组合表达式 session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id 的结果，完成上面尚未结束的表达式。
            session, user_id=user_id, kind="MEMORY_EMBEDDING", ref_type="MEMORY", ref_id=row.id
        )
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
            await session.flush()
        # 捕获IntegrityError异常，执行下面的错误处理，异常对象保存为 error。
        except IntegrityError as error:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_DUPLICATE", "已存在相同内容的已确认记忆") from error
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 reject：调用 reject 完成拒绝处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、长期记忆编号（memory_id）、版本号（version）。
async def reject(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
            raise _missing()
        # 检查当前记录版本是否符合请求要求：row.version != version；成立时执行缩进的分支。
        if row.version != version:
            # 中止本轮正常处理，抛出 执行“调用 _version_conflict 完成版本号conflict处理”得到的结果。
            raise _version_conflict()
        # 检查条件 row.status != 'CANDIDATE' 是否成立；成立时执行缩进的分支。
        if row.status != "CANDIDATE":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有待确认的记忆可以拒绝")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 把固定状态或编码 REJECTED（REJECTED）保存到 当前记录中的状态（row.status）。
        row.status = "REJECTED"
        # 把stamp保存到 当前记录中的rejectedat（row.rejected_at）。
        row.rejected_at = stamp
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
        # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
        await session.flush()
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 archive：调用 archive 完成归档处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、长期记忆编号（memory_id）、版本号（version）。
async def archive(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
            raise _missing()
        # 检查当前记录版本是否符合请求要求：row.version != version；成立时执行缩进的分支。
        if row.version != version:
            # 中止本轮正常处理，抛出 执行“调用 _version_conflict 完成版本号conflict处理”得到的结果。
            raise _version_conflict()
        # 检查条件 row.status != 'CONFIRMED' 是否成立；成立时执行缩进的分支。
        if row.status != "CONFIRMED":
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(409, "MEMORY_STATUS_INVALID", "只有已确认的记忆可以归档")
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 把固定状态或编码 ARCHIVED（已归档）保存到 当前记录中的状态（row.status）。
        row.status = "ARCHIVED"
        # 把stamp保存到 当前记录中的已归档at（row.archived_at）。
        row.archived_at = stamp
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
        # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
        await session.flush()
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 delete：调用 delete 完成删除处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、长期记忆编号（memory_id）、版本号（version）。
async def delete(db: Database, user_id: int, memory_id: int, version: int) -> Memory:
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 当前记录（row）。
        row = await repo.find_owned(session, user_id, memory_id, for_update=True)
        # 检查row is None，即对应内容是否不存在；成立时执行缩进的分支。
        if row is None:
            # 中止本轮正常处理，抛出 执行“调用 _missing 完成missing处理”得到的结果。
            raise _missing()
        # 检查当前记录版本是否符合请求要求：row.version != version；成立时执行缩进的分支。
        if row.version != version:
            # 中止本轮正常处理，抛出 执行“调用 _version_conflict 完成版本号conflict处理”得到的结果。
            raise _version_conflict()
        # 把等待执行“调用 database_time 完成数据库时间处理”得到的结果完成后的结果保存到 stamp。
        stamp = await database_time(session, row)
        # 把stamp保存到 当前记录中的删除标记at（row.deleted_at）。
        row.deleted_at = stamp
        # 把stamp保存到 当前记录中的更新时at（row.updated_at）。
        row.updated_at = stamp
        # 把 row.version 加上 固定数值 1，再保存回原变量。
        row.version += 1
        # 等待把待写入的数据库操作发送到数据库，但此时还没有提交事务完成后再继续。
        await session.flush()
        # 等待调用 session.refresh 完成刷新处理完成后再继续。
        await session.refresh(row)
        # 把当前记录（row）交还给调用方，本函数到此结束。
        return row


# 异步函数 context_block：把已确认记忆压缩成上下文背景；表未建立时静默跳过，不影响对话。；接收 Agent 数据库访问器（db）、用户编号（user_id）、最大chars（max_chars）、上限（limit）、查询条件（query）、附加说明（metadata）。
async def context_block(
    # 填写Agent 数据库访问器（db）字段，值为按表达式计算得到的值：Database, user_id: int, *, max_chars: int = 2000, limit: int = 40。
    db: Database, user_id: int, *, max_chars: int = 2000, limit: int = 40,
    # 填写查询条件（query）字段，值为表达式 str | None = None, metadata: dict | None = None 的结果。
    query: str | None = None, metadata: dict | None = None
# 继续传入或组合表达式 ) -> str | None: 的结果，完成上面尚未结束的表达式。
) -> str | None:
    # 把已确认记忆压缩成上下文背景；表未建立时静默跳过，不影响对话。
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with db.session() as session:
            # 把等待执行“确认 edfor上下文（ed_for_context）”得到的结果完成后的结果保存到 记录列表（rows）。
            rows = await repo.confirmed_for_context(session, user_id, limit)
    # 捕获SQLAlchemyError异常，执行下面的错误处理。
    except SQLAlchemyError:
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("agent_memory_context_unavailable user=%s", user_id)
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 检查条件 not rows 是否成立；成立时执行缩进的分支。
    if not rows:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 检查条件 query 是否成立；成立时执行缩进的分支。
    if query:

        # 从 app.core.config 引入 读取运行配置（get_settings）。
        from app.core.config import get_settings

        # 从 app.services.embedding_service 引入 embedtexts（embed_texts）。
        from app.services.embedding_service import embed_texts
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把从等待执行“调用 embed_texts 完成embedtexts处理”得到的结果完成后的结果中按固定数值 0取出的内容保存到 向量（vector）。
            vector = (await embed_texts([query]))[0]
            # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
            async with db.session() as session:
                # 把执行“调用 MemoryEmbedding.vector_value.cosine_distance 完成cosinedistance处理”得到的结果保存到 distance。
                distance = MemoryEmbedding.vector_value.cosine_distance(vector)
                # 把执行“调用 list 完成列表处理”得到的结果保存到 matches。
                matches = list(await session.scalars(select(Memory).join(
                    # 继续传入或组合调用 MemoryEmbedding, MemoryEmbedding.memory_id == Memory.id).where 完成where处理，完成上面尚未结束的表达式。
                    MemoryEmbedding, MemoryEmbedding.memory_id == Memory.id).where(
                    # 继续传入或组合条件判断的结果：Memory.user_id == user_id, MemoryEmbedding.user_id == user_id，完成上面尚未结束的表达式。
                    Memory.user_id == user_id, MemoryEmbedding.user_id == user_id,
                    # 继续传入或组合判断是否为 ，完成上面尚未结束的表达式。
                    Memory.status == "CONFIRMED", Memory.deleted_at.is_(None),
                    # 继续传入或组合条件判断的结果：MemoryEmbedding.embedding_model == get_settings().embedding_model，完成上面尚未结束的表达式。
                    MemoryEmbedding.embedding_model == get_settings().embedding_model,
                    # 继续传入或组合判断是否为 not，完成上面尚未结束的表达式。
                    MemoryEmbedding.vector_value.is_not(None), distance <= 0.8,
                # 继续传入或组合调用 ).order_by 完成顺序按处理，完成上面尚未结束的表达式。
                ).order_by(distance, Memory.id).limit(10)))
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 已读（seen）。
            seen = {row.id for row in matches}
            # 明确约束和偏好在语义不近时仍作为稳定背景；普通事实按相关性选择。
            # 把按 matches + [row for row in rows if row.id not in seen and row.kind in {'CONSTRAINT', 'PREFERENCE'}] 计算的结果保存到 记录列表（rows）。
            rows = matches + [row for row in rows if row.id not in seen and row.kind in {"CONSTRAINT", "PREFERENCE"}]
        # 捕获AgentError异常，执行下面的错误处理。
        except AgentError:
            # 这里不执行额外操作，只保留合法的空分支或占位结构。
            pass
    # 声明 lines，类型为 list[str]；其内容为空列表或集合。
    lines: list[str] = []
    # 把固定数值 0保存到 used。
    used = 0
    # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
    for row in rows:
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 显示名称（label）。
        label = _KIND_LABELS.get(row.kind, row.kind)
        # 把在文字占位处填入变量后组成的字符串保存到 line。
        line = f"- [{label}] {row.content}"
        # 检查条件 used + len(line) + 1 > max_chars 是否成立；成立时执行缩进的分支。
        if used + len(line) + 1 > max_chars:
            # 立即退出当前循环，不再处理后续条目。
            break
        # 把新条目放到列表末尾。
        lines.append(line)
        # 检查条件 metadata is not None 是否成立；成立时执行缩进的分支。
        if metadata is not None:
            # 把新条目放到列表末尾。
            metadata.setdefault("memoryIds", []).append(str(row.id))
        # 把 used 加上 按 len(line) + 1 计算的结果，再保存回原变量。
        used += len(line) + 1
    # 把条件 lines 是否成立时使用执行“用指定分隔符把多段文字合起来”得到的结果，否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
    return "\n".join(lines) if lines else None
