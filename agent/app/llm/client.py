# 创建和复用模型客户端连接，不把真实密钥写进源文件。

# 从 types 引入 可以按属性名读取的简单对象（SimpleNamespace）。
from types import SimpleNamespace

# 从 openai 引入 兼容模型接口的异步客户端（AsyncOpenAI）。
from openai import AsyncOpenAI

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.services.observation_service 引入 ObservedCompletions。
from app.services.observation_service import ObservedCompletions

# 声明 客户端（_client），类型为 AsyncOpenAI | None；其内容为空值，表示当前还没有对应内容。
_client: AsyncOpenAI | None = None


# 函数 get_llm_client：读取 语言模型客户端（_llm_client） 的值。
def get_llm_client():
    # 下面对 _client 的赋值使用模块共享变量。
    global _client
    # 检查_client is None，即对应内容是否不存在；成立时执行缩进的分支。
    if _client is None:
        # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
        settings = get_settings()
        # 把执行“调用 AsyncOpenAI 完成兼容模型接口的异步客户端处理”得到的结果保存到 客户端（_client）。
        _client = AsyncOpenAI(
            # 传入接口键（api_key），值为仅在确实需要连接服务时取出私密配置中的实际值。
            api_key=settings.llm_api_key.get_secret_value(),
            # 传入基础地址（base_url），值为表达式 settings.llm_base_url 的结果。
            base_url=settings.llm_base_url,
            # 传入超时（timeout），值为表达式 settings.llm_timeout_seconds 的结果。
            timeout=settings.llm_timeout_seconds,
            # 传入最大retries（max_retries），值为固定数值 0。
            max_retries=0,
        )
    # 把执行“调用 SimpleNamespace 完成可以按属性名读取的简单对象处理”得到的结果交还给调用方，本函数到此结束。
    return SimpleNamespace(chat=SimpleNamespace(completions=ObservedCompletions(_client.chat.completions)))


# 异步函数 close_llm_client：调用 close_llm_client 完成关闭语言模型客户端处理。
async def close_llm_client() -> None:
    # 下面对 _client 的赋值使用模块共享变量。
    global _client
    # 检查条件 _client is not None 是否成立；成立时执行缩进的分支。
    if _client is not None:
        # 等待释放连接、文件或客户端占用的资源完成后再继续。
        await _client.close()
        # 把空值，表示当前还没有对应内容保存到 客户端（_client）。
        _client = None
