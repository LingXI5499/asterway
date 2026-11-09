# 实现时间与耗时工具库处理；下面逐项说明输入、分支与调用的作用。
# 数据库事务中的业务时间；避免 Windows 与数据库时钟偏差破坏时间约束。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 sqlalchemy 引入 func、选择（select）。
from sqlalchemy import func, select

# 从 sqlalchemy.ext.asyncio 引入 单次异步数据库会话（AsyncSession）。
from sqlalchemy.ext.asyncio import AsyncSession


# 异步函数 database_time：使用数据库当前时间，并保证不会早于参与变更的记录时间。；接收 当前会话（session）。
async def database_time(session: AsyncSession, *rows: object) -> datetime:
    # 使用数据库当前时间，并保证不会早于参与变更的记录时间。
    # 把等待执行“取得查询结果中的一个值”得到的结果完成后的结果保存到 当前（current）。
    current = await session.scalar(select(func.clock_timestamp()))
    # 检查current is None，即对应内容是否不存在；成立时执行缩进的分支。
    if current is None:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("数据库未返回当前时间")
    # 把依次包含 当前（current） 的集合保存到 值列表（values）。
    values = [current]
    # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
    for row in rows:
        # 从依次包含 固定文字 'created_at'、固定文字 'updated_at'、固定文字 'confirmed_at'、固定文字 'started_at' 的集合依次取出名称（name），每项执行一次下面的处理。
        for name in ("created_at", "updated_at", "confirmed_at", "started_at"):
            # 把执行“读取 attr 的值”得到的结果保存到 值（value）。
            value = getattr(row, name, None)
            # 检查条件 isinstance(value, datetime) 是否成立；成立时执行缩进的分支。
            if isinstance(value, datetime):
                # 把新条目放到列表末尾。
                values.append(value)
    # 把执行“调用 max 完成最大处理”得到的结果交还给调用方，本函数到此结束。
    return max(values)
