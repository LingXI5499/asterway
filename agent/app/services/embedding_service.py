# 把文字交给已配置的向量接口，检查返回数量和向量维度。
# OpenAI 兼容的文本向量接口；未配置密钥时不发起网络请求。

# 引入 math，供下面调用其功能。
import math

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 从 typing 引入 Any。
from typing import Any

# 引入 HTTP 请求客户端库（httpx），供下面调用其功能。
import httpx

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError


# 函数 _endpoint：调用 _endpoint 完成endpoint处理；接收 基础（base）。
def _endpoint(base: str) -> str:
    # 把执行“调用 base.rstrip 完成rstrip处理”得到的结果保存到 normalized。
    normalized = base.rstrip("/")
    # 检查条件 normalized.endswith('/embeddings') 是否成立；成立时执行缩进的分支。
    if normalized.endswith("/embeddings"):
        # 把normalized交还给调用方，本函数到此结束。
        return normalized
    # 把按 normalized + '/embeddings' 计算的结果交还给调用方，本函数到此结束。
    return normalized + "/embeddings"


# 异步函数 _embed_texts：调用 _embed_texts 完成embedtexts处理；接收 texts。
async def _embed_texts(texts: list[str]) -> list[list[float]]:
    # 检查条件 not texts 是否成立；成立时执行缩进的分支。
    if not texts:
        # 把空列表或集合交还给调用方，本函数到此结束。
        return []
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把执行“只在连接或签名时取出密钥实际值”得到的结果保存到 接口键（api_key）。
    api_key = settings.embedding_api_key.get_secret_value()
    # 检查条件 not api_key 是否成立；成立时执行缩进的分支。
    if not api_key:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "EMBEDDING_NOT_CONFIGURED", "向量服务尚未配置")
    # 检查条件 not settings.embedding_base_url or not settings.embedding_model 是否成立；成立时执行缩进的分支。
    if not settings.embedding_base_url or not settings.embedding_model:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(503, "EMBEDDING_NOT_CONFIGURED", "向量服务尚未配置")
    # 检查条件 len(texts) > 10 是否成立；成立时执行缩进的分支。
    if len(texts) > 10:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "EMBEDDING_BATCH_TOO_LARGE", "单次向量请求最多包含 10 段文本")
    # 声明 提交数据（payload），类型为 dict[str, Any]；其内容为按字段名保存多个值的字典。
    payload: dict[str, Any] = {
        # 填写模型（model）字段，值为表达式 settings.embedding_model 的结果。
        "model": settings.embedding_model,
        # 填写输入（input）字段，值为texts。
        "input": texts,
        # 填写dimensions字段，值为表达式 settings.embedding_dimension 的结果。
        "dimensions": settings.embedding_dimension,
    }
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 异步进入执行“调用 httpx.AsyncClient 完成异步客户端处理”得到的结果，命名为 client；退出时由上下文管理器提交、回滚或释放资源。
        async with httpx.AsyncClient(
            # 传入超时（timeout），值为表达式 settings.embedding_timeout_seconds, trust_env=False 的结果。
            timeout=settings.embedding_timeout_seconds, trust_env=False
        # 继续传入或组合表达式 ) as client: 的结果，完成上面尚未结束的表达式。
        ) as client:
            # 把等待执行“调用 client.post 完成post处理”得到的结果完成后的结果保存到 响应（response）。
            response = await client.post(
                # 继续传入或组合调用 _endpoint 完成endpoint处理，完成上面尚未结束的表达式。
                _endpoint(settings.embedding_base_url),
                # 传入请求头集合（headers），值为表达式 { 的结果。
                headers={
                    # 填写授权（Authorization）字段，值为表达式 f"Bearer {api_key}" 的结果。
                    "Authorization": f"Bearer {api_key}",
                    # 继续传入或组合固定文字 'Content-Type": "application/json'，完成上面尚未结束的表达式。
                    "Content-Type": "application/json",
                },
                # 传入JSON 编码和解析库（json），值为提交数据（payload）。
                json=payload,
            )
    # 捕获httpx.HTTPError异常，执行下面的错误处理，异常对象保存为 error。
    except httpx.HTTPError as error:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "EMBEDDING_UNAVAILABLE", "向量服务暂时不可用") from error
    # 检查条件 response.status_code != 200 是否成立；成立时执行缩进的分支。
    if response.status_code != 200:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "EMBEDDING_FAILED", "向量服务返回失败")
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 response.json 完成JSON 编码和解析库处理”得到的结果保存到 请求正文（body）。
        body = response.json()
        # 把从请求正文（body）中按固定文字 'data'取出的内容保存到 数据列表（items）。
        items = body["data"]
        # 检查条件 not isinstance(items, list) or len(items) != len(texts) 是否成立；成立时执行缩进的分支。
        if not isinstance(items, list) or len(items) != len(texts):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("Unexpected embedding count")
        # 声明 indexed，类型为 dict[int, list[float]]；其内容为按字段名保存多个值的字典，这里先创建空字典。
        indexed: dict[int, list[float]] = {}
        # 从数据列表（items）依次取出单条数据（item），每项执行一次下面的处理。
        for item in items:
            # 把从单条数据（item）中按固定文字 'index'取出的内容保存到 索引（index）。
            index = item["index"]
            # 检查条件 type(index) is not int or index < 0 or index >= len(texts) or (index in indexed) 是否成立；成立时执行缩进的分支。
            if type(index) is not int or index < 0 or index >= len(texts) or index in indexed:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("Invalid embedding index")
            # 把从单条数据（item）中按固定文字 'embedding'取出的内容保存到 值列表（values）。
            values = item["embedding"]
            # 检查条件 not isinstance(values, list) or any((type(value) not in (int, float) for value in values)) 是否成立；成立时执行缩进的分支。
            if not isinstance(values, list) or any(
                # 继续传入或组合调用 type 完成类型处理，完成上面尚未结束的表达式。
                type(value) not in (int, float) for value in values
            ):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("Invalid embedding values")
            # 把执行“调用 list 完成列表处理”得到的结果保存到 向量（vector）。
            vector = list(map(float, values))
            # 检查条件 len(vector) != settings.embedding_dimension or not all(map(math.isfinite, vector)) 是否成立；成立时执行缩进的分支。
            if len(vector) != settings.embedding_dimension or not all(map(math.isfinite, vector)):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("Invalid embedding dimension")
            # 检查条件 not any(vector) 是否成立；成立时执行缩进的分支。
            if not any(vector):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("Empty direction vector")
            # 把向量（vector）保存到 indexed索引（indexed[index]）。
            indexed[index] = vector
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 vectors。
        vectors = [indexed[index] for index in range(len(texts))]
    # 捕获(KeyError, TypeError, ValueError)异常，执行下面的错误处理，异常对象保存为 error。
    except (KeyError, TypeError, ValueError) as error:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量服务返回结构不正确") from error
    # 检查条件 len(vectors) != len(texts) or any((not vector for vector in vectors)) 是否成立；成立时执行缩进的分支。
    if len(vectors) != len(texts) or any(not vector for vector in vectors):
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "EMBEDDING_RESULT_INVALID", "向量返回数量不正确")
    # 把vectors交还给调用方，本函数到此结束。
    return vectors


# 异步函数 embed_texts：调用 embed_texts 完成embedtexts处理；接收 texts。
async def embed_texts(texts: list[str]) -> list[list[float]]:

    # 从 app.services.observation_service 引入 记录（record）。
    from app.services.observation_service import record

    # 检查条件 not texts 是否成立；成立时执行缩进的分支。
    if not texts:
        # 把空列表或集合交还给调用方，本函数到此结束。
        return []
    # 把执行“调用 time.monotonic 完成monotonic处理”得到的结果保存到 开始时（started）。
    started = time.monotonic()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把等待执行“调用 _embed_texts 完成embedtexts处理”得到的结果完成后的结果保存到 结果（result）。
        result = await _embed_texts(texts)
    # 捕获AgentError异常，执行下面的错误处理，异常对象保存为 error。
    except AgentError as error:
        # 等待调用 record 完成记录处理完成后再继续。
        await record(kind="EMBEDDING", name=get_settings().embedding_model, started=started,
            # 传入状态（status），值为表达式 "FAILED", error_code=error.code) 的结果。
            status="FAILED", error_code=error.code)
        # 中止本轮正常处理，把当前异常继续向上交给调用方。
        raise
    # 等待调用 record 完成记录处理完成后再继续。
    await record(kind="EMBEDDING", name=get_settings().embedding_model, started=started,
        # 传入状态（status），值为表达式 "SUCCEEDED") 的结果。
        status="SUCCEEDED")
    # 把结果（result）交还给调用方，本函数到此结束。
    return result
