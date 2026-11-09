# 知识库只保存组织信息，文件、不可变版本和片段仍由原来的表管理。
from datetime import datetime
from typing import ClassVar

from sqlalchemy import BigInteger, Boolean, DateTime, Identity, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.db.models import Base


class KnowledgeBase(Base):
    __tablename__ = "knowledge_base"
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}
    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    user_id: Mapped[int] = mapped_column(BigInteger, nullable=False)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    description: Mapped[str] = mapped_column(String(500), nullable=False, default="")
    is_default: Mapped[bool] = mapped_column(Boolean, nullable=False, default=False)
    deleted_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    version: Mapped[int] = mapped_column(BigInteger, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())


class ConversationKnowledgeBase(Base):
    __tablename__ = "conversation_knowledge_base"
    __table_args__: ClassVar[dict[str, str]] = {"schema": "agent"}
    user_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    conversation_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    knowledge_base_id: Mapped[int] = mapped_column(BigInteger, primary_key=True)
