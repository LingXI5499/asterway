"""数据库事务中的业务时间；避免 Windows 与数据库时钟偏差破坏时间约束。"""

from datetime import datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession


async def database_time(session: AsyncSession, *rows: object) -> datetime:
    """使用数据库当前时间，并保证不会早于参与变更的记录时间。"""
    current = await session.scalar(select(func.clock_timestamp()))
    if current is None:
        raise RuntimeError("数据库未返回当前时间")
    values = [current]
    for row in rows:
        for name in ("created_at", "updated_at", "confirmed_at", "started_at"):
            value = getattr(row, name, None)
            if isinstance(value, datetime):
                values.append(value)
    return max(values)
