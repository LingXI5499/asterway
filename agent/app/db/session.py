"""Agent 自有 schema 的异步连接；不建表、不迁移，只使用已人工确认的结构。"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

from fastapi import Request
from sqlalchemy.exc import ArgumentError
from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.core.config import Settings
from app.core.errors import AgentError


class Database:
    def __init__(self, settings: Settings) -> None:
        url = settings.agent_database_url.get_secret_value()
        if not url:
            raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储尚未配置")
        try:
            self._engine: AsyncEngine = create_async_engine(
                url,
                pool_pre_ping=True,
                pool_size=settings.agent_db_pool_size,
                max_overflow=settings.agent_db_max_overflow,
                pool_timeout=5.0,
                pool_recycle=1800,
                connect_args={"server_settings": {"application_name": "asterway-agent"}},
            )
        except (ArgumentError, ValueError):
            raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储配置不正确") from None
        self._session_factory = async_sessionmaker(self._engine, expire_on_commit=False)

    @asynccontextmanager
    async def session(self) -> AsyncIterator[AsyncSession]:
        """一个 HTTP 动作一个事务；异常整体回滚，正常退出提交。"""
        async with self._session_factory() as session:
            try:
                yield session
                await session.commit()
            except BaseException:
                await session.rollback()
                raise

    async def close(self) -> None:
        await self._engine.dispose()


def get_database(request: Request) -> Database:
    database = getattr(request.app.state, "database", None)
    if database is None:
        raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储尚未配置")
    return database
