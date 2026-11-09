# 实现方案运行处理；下面逐项说明输入、分支与调用的作用。
# 本地启动入口；私密配置从 agent/.env 读取。

# 引入 uvicorn，供下面调用其功能。
import uvicorn

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == "__main__":
    # 调用 uvicorn.run 完成方案运行处理。
    uvicorn.run("app.main:app", host="127.0.0.1", port=get_settings().agent_port,
        # 传入loop，值为表达式 "app.core.event_loop:selector_event_loop") 的结果。
        loop="app.core.event_loop:selector_event_loop")
