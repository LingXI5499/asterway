# 把 Agent 表结构映射为 Python 对象，统一字段类型与记录关系。
# agent schema 三表映射；列与约束以 agent.sql 为准，不通过 ORM 建表。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 typing 引入 Any、类Var（ClassVar）。
from typing import Any, ClassVar

# 从 uuid 引入 UUID。
from uuid import UUID

# 从 pgvector.sqlalchemy 引入 向量（Vector）。
from pgvector.sqlalchemy import Vector

# 从 sqlalchemy 引入 ARRAY、Big整数（BigInteger）、可为空的真假值（Boolean）、日期时间类型（DateTime）、浮点小数（Float）、账户管理（Identity）、可为空的整数（Integer）、Numeric、可为空的文字（String）、文字（Text）、Uuid、func。
from sqlalchemy import (
    # 继续传入或组合ARRAY，完成上面尚未结束的表达式。
    ARRAY,
    # 继续传入或组合Big整数（BigInteger），完成上面尚未结束的表达式。
    BigInteger,
    # 继续传入或组合可为空的真假值（Boolean），完成上面尚未结束的表达式。
    Boolean,
    # 继续传入或组合日期时间类型（DateTime），完成上面尚未结束的表达式。
    DateTime,
    # 继续传入或组合浮点小数（Float），完成上面尚未结束的表达式。
    Float,
    # 继续传入或组合账户管理（Identity），完成上面尚未结束的表达式。
    Identity,
    # 继续传入或组合可为空的整数（Integer），完成上面尚未结束的表达式。
    Integer,
    # 继续传入或组合Numeric，完成上面尚未结束的表达式。
    Numeric,
    # 继续传入或组合可为空的文字（String），完成上面尚未结束的表达式。
    String,
    # 继续传入或组合文字（Text），完成上面尚未结束的表达式。
    Text,
    # 继续传入或组合Uuid，完成上面尚未结束的表达式。
    Uuid,
    # 继续传入或组合func，完成上面尚未结束的表达式。
    func,
)

# 从 sqlalchemy.dialects.postgresql 引入 JSONB。
from sqlalchemy.dialects.postgresql import JSONB

# 从 sqlalchemy.orm 引入 Declarative基础（DeclarativeBase）、Mapped、mapped字段（mapped_column）。
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


# 定义 Base，把基础需要的数据与操作放在同一个对象中。
class Base(DeclarativeBase):
    # 这里不执行额外操作，只保留合法的空分支或占位结构。
    pass


# 定义 Conversation，把对话需要的数据与操作放在同一个对象中。
class Conversation(Base):
    # 把固定文字 'conversation'保存到 tablename（__tablename__）。
    __tablename__ = "conversation"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 标题（title），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    title: Mapped[str] = mapped_column(String(100), nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False)
    # 声明 上下文汇总（context_summary），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    context_summary: Mapped[str] = mapped_column(Text, nullable=False, default="")
    # 声明 汇总through消息no（summary_through_message_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    summary_through_message_no: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 汇总更新时at（summary_updated_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    summary_updated_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 下一个消息no（next_message_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    next_message_no: Mapped[int] = mapped_column(BigInteger, nullable=False, default=1)
    # 声明 最后一次消息at（last_message_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    last_message_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 closedat（closed_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    closed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 删除标记at（deleted_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# 定义 ConversationTurn，把对话本轮对话需要的数据与操作放在同一个对象中。
class ConversationTurn(Base):
    # 把固定文字 'conversation_turn'保存到 tablename（__tablename__）。
    __tablename__ = "conversation_turn"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 对话编号（conversation_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 请求编号（request_id），类型为 Mapped[UUID]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    request_id: Mapped[UUID] = mapped_column(Uuid, nullable=False)
    # 声明 请求摘要值（request_hash），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    request_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="RUNNING")
    # 声明 finish原因（finish_reason），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finish_reason: Mapped[str | None] = mapped_column(String(64))
    # 声明 错误编码（error_code），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_code: Mapped[str | None] = mapped_column(String(64))
    # 声明 开始时at（started_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    started_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 截止时间at（deadline_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    deadline_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # 声明 finishedat（finished_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    # 本轮创建时固定的资料范围；后续修改会话选择不影响本轮。
    knowledge_base_ids: Mapped[list[str]] = mapped_column(JSONB, nullable=False, default=list)


# 定义 ConversationMessage，把对话消息需要的数据与操作放在同一个对象中。
class ConversationMessage(Base):
    # 把固定文字 'conversation_message'保存到 tablename（__tablename__）。
    __tablename__ = "conversation_message"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 对话编号（conversation_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 本轮对话编号（turn_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    turn_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 消息no（message_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    message_no: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 角色（role），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    role: Mapped[str] = mapped_column(String(16), nullable=False)
    # 声明 内容（content），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    content: Mapped[str] = mapped_column(Text, nullable=False, default="")
    # 声明 是否完成（is_complete），类型为 Mapped[bool]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    is_complete: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    # 声明 业务工具调用列表（tool_calls），类型为 Mapped[list[Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    tool_calls: Mapped[list[Any]] = mapped_column(JSONB, nullable=False, default=list)
    # 声明 业务工具请求消息编号（tool_request_message_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    tool_request_message_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 业务工具调用编号（tool_call_id），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    tool_call_id: Mapped[str | None] = mapped_column(String(128))
    # 声明 业务工具名称（tool_name），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    tool_name: Mapped[str | None] = mapped_column(String(64))
    # 声明 模型（model），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    model: Mapped[str | None] = mapped_column(String(128))
    # 声明 finish原因（finish_reason），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finish_reason: Mapped[str | None] = mapped_column(String(64))
    # 声明 用量（usage），类型为 Mapped[dict[str, Any] | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    usage: Mapped[dict[str, Any] | None] = mapped_column(JSONB(none_as_null=True))
    # 声明 citations，类型为 Mapped[list[dict[str, Any]]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    citations: Mapped[list[dict[str, Any]]] = mapped_column(JSONB, nullable=False, default=list)
    # 声明 used长期记忆编号列表（used_memory_ids），类型为 Mapped[list[str]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    used_memory_ids: Mapped[list[str]] = mapped_column(JSONB, nullable=False, default=list)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# 定义 Memory，把长期记忆需要的数据与操作放在同一个对象中。
class Memory(Base):
    # 把固定文字 'memory'保存到 tablename（__tablename__）。
    __tablename__ = "memory"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 种类（kind），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    kind: Mapped[str] = mapped_column(String(16), nullable=False)
    # 声明 内容（content），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    content: Mapped[str] = mapped_column(Text, nullable=False)
    # 声明 来源（source），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    source: Mapped[str] = mapped_column(String(16), nullable=False, default="INFERRED")
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="CANDIDATE")
    # 声明 confidence，类型为 Mapped[float]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    confidence: Mapped[float] = mapped_column(Numeric(3, 2), nullable=False, default=0.6)
    # 声明 来源对话编号（source_conversation_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    source_conversation_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 来源本轮对话编号（source_turn_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    source_turn_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 supersedes编号（supersedes_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    supersedes_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 已确认at（confirmed_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 rejectedat（rejected_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    rejected_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 已归档at（archived_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    archived_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 删除标记at（deleted_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# 定义 MemoryEmbedding，把长期记忆向量需要的数据与操作放在同一个对象中。
class MemoryEmbedding(Base):
    # 把固定文字 'memory_embedding'保存到 tablename（__tablename__）。
    __tablename__ = "memory_embedding"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 长期记忆编号（memory_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    memory_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 向量模型（embedding_model），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    embedding_model: Mapped[str] = mapped_column(String(100), nullable=False)
    # 声明 dimension，类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    dimension: Mapped[int] = mapped_column(Integer, nullable=False)
    # 声明 向量（embedding），类型为 Mapped[list[float]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    embedding: Mapped[list[float]] = mapped_column(ARRAY(Float(precision=53)), nullable=False)
    # 声明 向量值（vector_value），类型为 Mapped[Any | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    vector_value: Mapped[Any | None] = mapped_column(Vector(1024))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

# 定义 KnowledgeDocument，把知识资料知识文档需要的数据与操作放在同一个对象中。
class KnowledgeDocument(Base):
    # 把固定文字 'knowledge_document'保存到 tablename（__tablename__）。
    __tablename__ = "knowledge_document"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 当前修订版本编号（current_revision_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    current_revision_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 标题（title），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    # 声明 文件名称（file_name），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    # 声明 mime类型（mime_type），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    mime_type: Mapped[str] = mapped_column(String(100), nullable=False)
    # 声明 每页条数bytes（size_bytes），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    size_bytes: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 checksum，类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    checksum: Mapped[str] = mapped_column(String(64), nullable=False)
    # 声明 存储键（storage_key），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    storage_key: Mapped[str] = mapped_column(String(500), nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="UPLOADED")
    # 声明 文档片段数量（chunk_count），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    chunk_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    # 声明 错误消息（error_message），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_message: Mapped[str | None] = mapped_column(String(1000))
    # 声明 删除标记at（deleted_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

    # 资料所属知识库，移动文档不改变其版本编号。
    knowledge_base_id: Mapped[int] = mapped_column(BigInteger, nullable=False)


# 定义 KnowledgeChunk，把知识资料文档片段需要的数据与操作放在同一个对象中。
class KnowledgeChunk(Base):
    # 把固定文字 'knowledge_chunk'保存到 tablename（__tablename__）。
    __tablename__ = "knowledge_chunk"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 知识文档编号（document_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    document_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 修订版本编号（revision_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    revision_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 当前页数值（page_number），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    page_number: Mapped[int | None] = mapped_column(Integer)
    # 声明 heading，类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    heading: Mapped[str | None] = mapped_column(String(300))
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 文档片段no（chunk_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    chunk_no: Mapped[int] = mapped_column(Integer, nullable=False)
    # 声明 内容（content），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    content: Mapped[str] = mapped_column(Text, nullable=False)
    # 声明 身份凭据数量（token_count），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    token_count: Mapped[int] = mapped_column(Integer, nullable=False)
    # 声明 向量模型（embedding_model），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    embedding_model: Mapped[str | None] = mapped_column(String(100))
    # 声明 向量dimension（embedding_dimension），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    embedding_dimension: Mapped[int | None] = mapped_column(Integer)
    # 声明 向量（embedding），类型为 Mapped[list[float] | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    embedding: Mapped[list[float] | None] = mapped_column(ARRAY(Float(precision=53)))
    # 声明 向量值（vector_value），类型为 Mapped[Any | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    vector_value: Mapped[Any | None] = mapped_column(Vector(1024))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# 定义 BackgroundTask，把后台任务需要的数据与操作放在同一个对象中。
class BackgroundTask(Base):
    # 把固定文字 'background_task'保存到 tablename（__tablename__）。
    __tablename__ = "background_task"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 种类（kind），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    kind: Mapped[str] = mapped_column(String(32), nullable=False)
    # 声明 响应式引用类型（ref_type），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    ref_type: Mapped[str] = mapped_column(String(32), nullable=False)
    # 声明 响应式引用编号（ref_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    ref_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    # 声明 尝试次数（attempts），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    # 声明 错误编码（error_code），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_code: Mapped[str | None] = mapped_column(String(64))
    # 声明 错误消息（error_message），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_message: Mapped[str | None] = mapped_column(String(1000))
    # 声明 定时执行at（scheduled_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    scheduled_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 开始时at（started_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 finishedat（finished_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )

# 定义 WorkflowRun，把规划工作流方案运行需要的数据与操作放在同一个对象中。
class WorkflowRun(Base):
    # 把固定文字 'workflow_run'保存到 tablename（__tablename__）。
    __tablename__ = "workflow_run"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 对话编号（conversation_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    conversation_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 来源本轮对话编号（source_turn_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    source_turn_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 intent，类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    intent: Mapped[str] = mapped_column(String(32), nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="DRAFT")
    # 声明 当前状态版本号（state_version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    state_version: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    # 声明 最新方案版本号编号（latest_plan_version_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    latest_plan_version_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 工作流图当前状态（graph_state），类型为 Mapped[dict[str, Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    graph_state: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False, default=dict)
    # 声明 到期at（expires_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # 声明 已确认at（confirmed_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 finishedat（finished_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 错误编码（error_code），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_code: Mapped[str | None] = mapped_column(String(64))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)


# 定义 PlanVersion，把方案版本号需要的数据与操作放在同一个对象中。
class PlanVersion(Base):
    # 把固定文字 'plan_version'保存到 tablename（__tablename__）。
    __tablename__ = "plan_version"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 方案运行编号（run_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    run_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 版本号no（version_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version_no: Mapped[int] = mapped_column(Integer, nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(24), nullable=False, default="DRAFT")
    # 声明 汇总（summary），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    summary: Mapped[str] = mapped_column(String(1000), nullable=False)
    # 声明 确认时的数据快照（snapshot），类型为 Mapped[dict[str, Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    snapshot: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    # 声明 diff汇总（diff_summary），类型为 Mapped[dict[str, Any] | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    diff_summary: Mapped[dict[str, Any] | None] = mapped_column(JSONB(none_as_null=True))
    # 声明 confirmation身份凭据（confirmation_token），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    confirmation_token: Mapped[str | None] = mapped_column(String(64))
    # 声明 已确认at（confirmed_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    confirmed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 supersededat（superseded_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    superseded_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 到期at（expires_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    expires_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)


# 定义 WorkflowEvent，把规划工作流事件需要的数据与操作放在同一个对象中。
class WorkflowEvent(Base):
    # 把固定文字 'workflow_event'保存到 tablename（__tablename__）。
    __tablename__ = "workflow_event"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 方案运行编号（run_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    run_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 事件no（event_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    event_no: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 事件类型（event_type），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    event_type: Mapped[str] = mapped_column(String(32), nullable=False)
    # 声明 提交数据（payload），类型为 Mapped[dict[str, Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    payload: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False, default=dict)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# 定义 PlanCommit，把方案提交回执需要的数据与操作放在同一个对象中。
class PlanCommit(Base):
    # 把固定文字 'plan_commit'保存到 tablename（__tablename__）。
    __tablename__ = "plan_commit"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 方案运行编号（run_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    run_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 方案版本号编号（plan_version_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    plan_version_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    # 声明 幂等键（idempotency_key），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    idempotency_key: Mapped[str] = mapped_column(String(64), nullable=False)
    # 声明 请求摘要值（request_hash），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    request_hash: Mapped[str] = mapped_column(String(64), nullable=False)
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), nullable=False, default="PENDING")
    # 声明 尝试次数（attempts），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    attempts: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    # 声明 结果（result），类型为 Mapped[dict[str, Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    result: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False, default=dict)
    # 声明 错误编码（error_code），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_code: Mapped[str | None] = mapped_column(String(64))
    # 声明 错误消息（error_message），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_message: Mapped[str | None] = mapped_column(String(1000))
    # 声明 开始时at（started_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 finishedat（finished_at），类型为 Mapped[datetime | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(
        # 继续传入或组合调用 DateTime(timezone=True), nullable=False, server_default=func.now 完成当前时间处理，完成上面尚未结束的表达式。
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    # 声明 版本号（version），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
