# 创建 Agent 专用异步数据库连接；一个请求内的修改一起提交或回滚。
# Agent 自有 schema 的异步连接；不建表、不迁移，只使用已人工确认的结构。

# 从 collections.abc 引入 可逐项读取的异步迭代结果（AsyncIterator）。
from collections.abc import AsyncIterator

# 从 contextlib 引入 异步资源进入和退出管理器（asynccontextmanager）。
from contextlib import asynccontextmanager

# 从 fastapi 引入 本次 HTTP 请求（Request）。
from fastapi import Request

# 从 sqlalchemy.exc 引入 数据库参数配置异常（ArgumentError）。
from sqlalchemy.exc import ArgumentError

# 从 sqlalchemy.ext.asyncio 引入 异步数据库连接引擎（AsyncEngine）、单次异步数据库会话（AsyncSession）、异步数据库会话创建器（async_sessionmaker）、创建异步数据库连接池的函数（create_async_engine）。
from sqlalchemy.ext.asyncio import (
    # 继续传入或组合异步数据库连接引擎（AsyncEngine），完成上面尚未结束的表达式。
    AsyncEngine,
    # 继续传入或组合单次异步数据库会话（AsyncSession），完成上面尚未结束的表达式。
    AsyncSession,
    # 继续传入或组合异步数据库会话创建器（async_sessionmaker），完成上面尚未结束的表达式。
    async_sessionmaker,
    # 继续传入或组合创建异步数据库连接池的函数（create_async_engine），完成上面尚未结束的表达式。
    create_async_engine,
)

# 从 app.core.config 引入 配置（Settings）。
from app.core.config import Settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError


# 定义 Database，把数据库需要的数据与操作放在同一个对象中。
class Database:
    # 函数 __init__：在创建实例时读取配置并准备后续要复用的成员；接收 配置（settings）。
    def __init__(self, settings: Settings) -> None:
        # 把执行“只在连接或签名时取出密钥实际值”得到的结果保存到 地址（url）。
        url = settings.agent_database_url.get_secret_value()
        # 检查条件 not url 是否成立；成立时执行缩进的分支。
        if not url:
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储尚未配置")
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 声明 当前实例中的数据库连接引擎（self._engine），类型为 AsyncEngine；其内容为执行“按连接地址和连接池限制建立异步数据库访问引擎”得到的结果。
            self._engine: AsyncEngine = create_async_engine(
                # 继续传入或组合地址（url），完成上面尚未结束的表达式。
                url,
                # 传入借用连接前是否先检查连通（pool_pre_ping），值为真，表示条件成立或开关打开。
                pool_pre_ping=True,
                # 传入连接池常驻连接数（pool_size），值为表达式 settings.agent_db_pool_size 的结果。
                pool_size=settings.agent_db_pool_size,
                # 传入高峰时允许增加的连接数（max_overflow），值为表达式 settings.agent_db_max_overflow 的结果。
                max_overflow=settings.agent_db_max_overflow,
                # 传入等待可用连接的秒数上限（pool_timeout），值为固定数值 5.0。
                pool_timeout=5.0,
                # 传入连接使用多少秒后回收重建（pool_recycle），值为固定数值 1800。
                pool_recycle=1800,
                # 传入建立数据库连接时附带的参数（connect_args），值为表达式 {"server_settings": {"application_name": "asterway-agent"}} 的结果。
                connect_args={"server_settings": {"application_name": "asterway-agent"}},
            )
        # 捕获(ArgumentError, ValueError)异常，执行下面的错误处理。
        except (ArgumentError, ValueError):
            # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
            raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储配置不正确") from None
        # 把执行“创建生成数据库会话的工厂，后面的每次事务通过它取得会话”得到的结果保存到 当前实例中的会话创建器（self._session_factory）。
        self._session_factory = async_sessionmaker(self._engine, expire_on_commit=False)

    # 让这个异步生成器管理资源的进入和退出。
    @asynccontextmanager
    # 异步函数 session：一个 HTTP 动作一个事务；异常整体回滚，正常退出提交。
    async def session(self) -> AsyncIterator[AsyncSession]:
        # 一个 HTTP 动作一个事务；异常整体回滚，正常退出提交。
        # 异步进入执行“创建供当前操作使用的异步数据库会话”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
        async with self._session_factory() as session:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 向外逐项交付本次结果，下一次继续迭代时从这里往后执行。
                yield session
                # 等待提交本次数据库事务，使本次修改一起生效完成后再继续。
                await session.commit()
            # 捕获BaseException异常，执行下面的错误处理。
            except BaseException:
                # 等待回滚本次数据库事务，撤销这一轮尚未提交的修改完成后再继续。
                await session.rollback()
                # 中止本轮正常处理，把当前异常继续向上交给调用方。
                raise

    # 异步函数 close：释放连接、文件或客户端占用的资源。
    async def close(self) -> None:
        # 等待关闭连接池并释放数据库连接完成后再继续。
        await self._engine.dispose()


# 函数 get_database：取得本次请求所使用的 Agent 数据库访问器；接收 请求（request）。
def get_database(request: Request) -> Database:
    # 把执行“读取 attr 的值”得到的结果保存到 数据库（database）。
    database = getattr(request.app.state, "database", None)
    # 检查database is None，即对应内容是否不存在；成立时执行缩进的分支。
    if database is None:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "AGENT_DB_NOT_CONFIGURED", "会话存储尚未配置")
    # 把数据库（database）交还给调用方，本函数到此结束。
    return database
