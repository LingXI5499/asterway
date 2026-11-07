"""本地启动入口；私密配置从 agent/.env 读取。"""

import uvicorn

from app.core.config import get_settings

if __name__ == "__main__":
    uvicorn.run("app.main:app", host="127.0.0.1", port=get_settings().agent_port,
        loop="app.core.event_loop:selector_event_loop")
