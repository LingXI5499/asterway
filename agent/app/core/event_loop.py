# 实现事件loop处理；下面逐项说明输入、分支与调用的作用。
# Windows 下 psycopg 异步连接需要 SelectorEventLoop。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio


# 函数 selector_event_loop：调用 selector_event_loop 完成selector事件loop处理。
def selector_event_loop():
    # 把执行“调用 asyncio.SelectorEventLoop 完成Selector事件Loop处理”得到的结果交还给调用方，本函数到此结束。
    return asyncio.SelectorEventLoop()
