# 映射复盘、调用记录等工作区表，供后台处理与页面查询。
# 知识版本、成长复盘与调用记录；结构通过手工 SQL 初始化。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 typing 引入 Any、类Var（ClassVar）。
from typing import Any, ClassVar

# 从 sqlalchemy 引入 Big整数（BigInteger）、日期时间类型（DateTime）、账户管理（Identity）、可为空的整数（Integer）、可为空的文字（String）、文字（Text）、func。
from sqlalchemy import BigInteger, DateTime, Identity, Integer, String, Text, func

# 从 sqlalchemy.dialects.postgresql 引入 JSONB。
from sqlalchemy.dialects.postgresql import JSONB

# 从 sqlalchemy.orm 引入 Mapped、mapped字段（mapped_column）。
from sqlalchemy.orm import Mapped, mapped_column

# 从 app.db.models 引入 基础（Base）。
from app.db.models import Base


# 定义 KnowledgeRevision，把知识资料修订版本需要的数据与操作放在同一个对象中。
class KnowledgeRevision(Base):
    # 把固定文字 'knowledge_revision'保存到 tablename（__tablename__）。
    __tablename__ = "knowledge_revision"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 声明 知识文档编号（document_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    document_id: Mapped[int] = mapped_column(BigInteger)
    # 声明 修订版本no（revision_no），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    revision_no: Mapped[int] = mapped_column(Integer)
    # 声明 文件名称（file_name），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    file_name: Mapped[str] = mapped_column(String(255))
    # 声明 mime类型（mime_type），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    mime_type: Mapped[str] = mapped_column(String(100))
    # 声明 每页条数bytes（size_bytes），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    size_bytes: Mapped[int] = mapped_column(BigInteger)
    # 声明 checksum，类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    checksum: Mapped[str] = mapped_column(String(64))
    # 声明 存储键（storage_key），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    storage_key: Mapped[str] = mapped_column(String(500))
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), default="UPLOADED")
    # 声明 索引状态（index_status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    index_status: Mapped[str] = mapped_column(String(16), default="PENDING")
    # 声明 文档片段数量（chunk_count），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    # 声明 错误消息（error_message），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_message: Mapped[str | None] = mapped_column(String(1000))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


# 定义 ReviewReport，把复盘复盘报告需要的数据与操作放在同一个对象中。
class ReviewReport(Base):
    # 把固定文字 'review_report'保存到 tablename（__tablename__）。
    __tablename__ = "review_report"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 声明 日期来源（date_from），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    date_from: Mapped[str] = mapped_column(String(10))
    # 声明 日期to（date_to），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    date_to: Mapped[str] = mapped_column(String(10))
    # 声明 用户使用的时区（time_zone），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    time_zone: Mapped[str] = mapped_column(String(64))
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16), default="PENDING")
    # 声明 facts，类型为 Mapped[dict[str, Any]]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    facts: Mapped[dict[str, Any]] = mapped_column(JSONB, default=dict)
    # 声明 内容（content），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    content: Mapped[str] = mapped_column(Text, default="")
    # 声明 错误消息（error_message），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_message: Mapped[str | None] = mapped_column(String(1000))
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    # 声明 更新时at（updated_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


# 定义 CallRecord，把调用记录需要的数据与操作放在同一个对象中。
class CallRecord(Base):
    # 把固定文字 'call_record'保存到 tablename（__tablename__）。
    __tablename__ = "call_record"
    # 声明 表参数（__table_args__），类型为 ClassVar[dict[str, str]]；其内容为按字段名保存多个值的字典。
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    # 声明 编号（id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    # 声明 用户编号（user_id），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    user_id: Mapped[int] = mapped_column(BigInteger)
    # 声明 追踪标识编号（trace_id），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    trace_id: Mapped[str] = mapped_column(String(64))
    # 声明 种类（kind），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    kind: Mapped[str] = mapped_column(String(16))
    # 声明 名称（name），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    name: Mapped[str] = mapped_column(String(128))
    # 声明 状态（status），类型为 Mapped[str]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    status: Mapped[str] = mapped_column(String(16))
    # 声明 持续时间ms（duration_ms），类型为 Mapped[int]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    duration_ms: Mapped[int] = mapped_column(BigInteger)
    # 声明 首次身份凭据ms（first_token_ms），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    first_token_ms: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 模型提示词凭据处理器（prompt_tokens），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    prompt_tokens: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 完成情况凭据处理器（completion_tokens），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    completion_tokens: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 错误编码（error_code），类型为 Mapped[str | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    error_code: Mapped[str | None] = mapped_column(String(64))
    # 声明 对话编号（conversation_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    conversation_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 本轮对话编号（turn_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    turn_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 方案运行编号（run_id），类型为 Mapped[int | None]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    run_id: Mapped[int | None] = mapped_column(BigInteger)
    # 声明 创建时at（created_at），类型为 Mapped[datetime]；其内容为执行“调用 mapped_column 完成mapped字段处理”得到的结果。
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
