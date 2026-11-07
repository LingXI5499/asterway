"""Windows 下 psycopg 异步连接需要 SelectorEventLoop。"""

import asyncio


def selector_event_loop():
    return asyncio.SelectorEventLoop()
