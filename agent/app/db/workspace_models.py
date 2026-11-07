"""知识版本、成长复盘与调用记录；结构通过手工 SQL 初始化。"""

from datetime import datetime
from typing import Any, ClassVar

from sqlalchemy import BigInteger, DateTime, Identity, Integer, String, Text, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import Mapped, mapped_column

from app.db.models import Base


class KnowledgeRevision(Base):
    __tablename__ = "knowledge_revision"
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger)
    document_id: Mapped[int] = mapped_column(BigInteger)
    revision_no: Mapped[int] = mapped_column(Integer)
    file_name: Mapped[str] = mapped_column(String(255))
    mime_type: Mapped[str] = mapped_column(String(100))
    size_bytes: Mapped[int] = mapped_column(BigInteger)
    checksum: Mapped[str] = mapped_column(String(64))
    storage_key: Mapped[str] = mapped_column(String(500))
    status: Mapped[str] = mapped_column(String(16), default="UPLOADED")
    index_status: Mapped[str] = mapped_column(String(16), default="PENDING")
    chunk_count: Mapped[int] = mapped_column(Integer, default=0)
    error_message: Mapped[str | None] = mapped_column(String(1000))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class ReviewReport(Base):
    __tablename__ = "review_report"
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger)
    date_from: Mapped[str] = mapped_column(String(10))
    date_to: Mapped[str] = mapped_column(String(10))
    time_zone: Mapped[str] = mapped_column(String(64))
    status: Mapped[str] = mapped_column(String(16), default="PENDING")
    facts: Mapped[dict[str, Any]] = mapped_column(JSONB, default=dict)
    content: Mapped[str] = mapped_column(Text, default="")
    error_message: Mapped[str | None] = mapped_column(String(1000))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class CallRecord(Base):
    __tablename__ = "call_record"
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}

    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger)
    trace_id: Mapped[str] = mapped_column(String(64))
    kind: Mapped[str] = mapped_column(String(16))
    name: Mapped[str] = mapped_column(String(128))
    status: Mapped[str] = mapped_column(String(16))
    duration_ms: Mapped[int] = mapped_column(BigInteger)
    first_token_ms: Mapped[int | None] = mapped_column(BigInteger)
    prompt_tokens: Mapped[int | None] = mapped_column(BigInteger)
    completion_tokens: Mapped[int | None] = mapped_column(BigInteger)
    error_code: Mapped[str | None] = mapped_column(String(64))
    conversation_id: Mapped[int | None] = mapped_column(BigInteger)
    turn_id: Mapped[int | None] = mapped_column(BigInteger)
    run_id: Mapped[int | None] = mapped_column(BigInteger)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
