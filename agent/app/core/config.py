# 读取运行配置、校验地址和范围；实际密钥从私密配置读取。
# Agent 本地配置；不维护业务表连接或 Java JWT 签名密钥。

# 从 functools 引入 函数结果缓存（lru_cache）。
from functools import lru_cache

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 从 urllib.parse 引入 urlsplit。
from urllib.parse import urlsplit

# 从 pydantic 引入 字段（Field）、默认显示时隐藏实际内容的私密文字类型（SecretStr）、字段validator（field_validator）、模型validator（model_validator）。
from pydantic import Field, SecretStr, field_validator, model_validator

# 从 pydantic_settings 引入 基础配置（BaseSettings）、配置ConfigDict（SettingsConfigDict）。
from pydantic_settings import BaseSettings, SettingsConfigDict

# 把按 Path(__file__).resolve().parents[2] / '.env' 计算的结果保存到 ENV文件（ENV_FILE）。
ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


# 定义 Settings，把配置需要的数据与操作放在同一个对象中。
class Settings(BaseSettings):
    # 声明 应用名称（app_name），类型为 str；其内容为固定文字 'Asterway Agent'。
    app_name: str = "Asterway Agent"
    # 声明 应用env（app_env），类型为 str；其内容为固定文字 'local'。
    app_env: str = "local"
    # 声明 成长助手port（agent_port），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_port: int = Field(default=8001, ge=1, le=65535)
    # 声明 Java后端接口基础地址（java_api_base_url），类型为 str；其内容为固定文字 'http://127.0.0.1:8089'。
    java_api_base_url: str = "http://127.0.0.1:8089"
    # 声明 成长助手内部键（agent_internal_key），类型为 SecretStr；其内容为执行“调用 SecretStr 完成默认显示时隐藏实际内容的私密文字类型处理”得到的结果。
    agent_internal_key: SecretStr = SecretStr("")
    # 声明 Java后端超时秒数（java_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    java_timeout_seconds: float = Field(default=15.0, gt=0, le=60)
    # 声明 Java后端最大响应bytes（java_max_response_bytes），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    java_max_response_bytes: int = Field(default=2_000_000, ge=1024, le=10_000_000)

    # 声明 语言模型提供器（llm_provider），类型为 str；其内容为固定文字 'deepseek'。
    llm_provider: str = "deepseek"
    # 声明 语言模型基础地址（llm_base_url），类型为 str；其内容为固定文字 'https://api.deepseek.com'。
    llm_base_url: str = "https://api.deepseek.com"
    # 声明 语言模型接口键（llm_api_key），类型为 SecretStr；其内容为执行“调用 SecretStr 完成默认显示时隐藏实际内容的私密文字类型处理”得到的结果。
    llm_api_key: SecretStr = SecretStr("")
    # 声明 语言模型模型（llm_model），类型为 str；其内容为固定文字 'deepseek-flash'。
    llm_model: str = "deepseek-flash"
    # 声明 语言模型超时秒数（llm_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    llm_timeout_seconds: float = Field(default=60.0, gt=0, le=180)
    # 声明 语言模型最大输出凭据处理器（llm_max_output_tokens），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    llm_max_output_tokens: int = Field(default=2048, ge=256, le=8192)

    # 声明 成长助手请求超时秒数（agent_request_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_request_timeout_seconds: float = Field(default=240.0, gt=0, le=300)
    # 声明 成长助手最大concurrentrequests（agent_max_concurrent_requests），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_max_concurrent_requests: int = Field(default=8, ge=1, le=32)

    # P1.2 会话持久化；数据库使用独立受限角色，只访问 agent schema。
    # 声明 成长助手数据库地址（agent_database_url），类型为 SecretStr；其内容为执行“调用 SecretStr 完成默认显示时隐藏实际内容的私密文字类型处理”得到的结果。
    agent_database_url: SecretStr = SecretStr("")
    # 声明 成长助手数据库连接池每页条数（agent_db_pool_size），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_db_pool_size: int = Field(default=5, ge=1, le=20)
    # 声明 成长助手数据库最大额外连接数（agent_db_max_overflow），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_db_max_overflow: int = Field(default=5, ge=0, le=20)
    # 规划/复盘工具会在一次对话轮次内串联多次模型调用，轮次预算需覆盖完整工具链。
    # 声明 成长助手本轮对话超时秒数（agent_turn_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_turn_timeout_seconds: float = Field(default=240.0, gt=0, le=300)
    # 声明 成长助手部分结果保存间隔秒数（agent_partial_save_interval_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_partial_save_interval_seconds: float = Field(default=2.0, ge=0.5, le=10)
    # 声明 成长助手上下文最近消息列表（agent_context_recent_messages），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_context_recent_messages: int = Field(default=20, ge=2, le=100)
    # 声明 成长助手上下文最大chars（agent_context_max_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_context_max_chars: int = Field(default=24000, ge=2000, le=100_000)
    # 声明 成长助手汇总trigger消息列表（agent_summary_trigger_messages），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_summary_trigger_messages: int = Field(default=16, ge=4, le=100)
    # 声明 成长助手汇总keep消息列表（agent_summary_keep_messages），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_summary_keep_messages: int = Field(default=8, ge=2, le=50)
    # 声明 成长助手汇总最大chars（agent_summary_max_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_summary_max_chars: int = Field(default=8000, ge=500, le=16_000)

    # P1.4 长期记忆：只注入已确认记忆，预算与条数均可配置。
    # 声明 成长助手长期记忆上下文最大chars（agent_memory_context_max_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_memory_context_max_chars: int = Field(default=2000, ge=200, le=8000)
    # 声明 成长助手长期记忆上下文上限（agent_memory_context_limit），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_memory_context_limit: int = Field(default=40, ge=1, le=100)

    # P1.4 知识库：文件仅存本地受控目录，解析/向量化由单进程后台任务执行。
    # 声明 成长助手知识资料dir（agent_knowledge_dir），类型为 str；其内容为固定文字 'data/knowledge'。
    agent_knowledge_dir: str = "data/knowledge"
    # 声明 成长助手知识资料最大bytes（agent_knowledge_max_bytes），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_max_bytes: int = Field(default=26_214_400, ge=1024, le=52_428_800)
    # 声明 成长助手知识资料解析最大chars（agent_knowledge_parse_max_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_parse_max_chars: int = Field(default=2_000_000, ge=10_000, le=10_000_000)
    # 声明 成长助手知识资料文档片段chars（agent_knowledge_chunk_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_chunk_chars: int = Field(default=1200, ge=200, le=4000)
    # 声明 成长助手知识资料文档片段overlapchars（agent_knowledge_chunk_overlap_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_chunk_overlap_chars: int = Field(default=150, ge=0, le=800)
    # 声明 成长助手知识资料最大文档片段列表（agent_knowledge_max_chunks），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_max_chunks: int = Field(default=4000, ge=1, le=20_000)
    # 声明 成长助手知识资料向量批量（agent_knowledge_embedding_batch），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_knowledge_embedding_batch: int = Field(default=10, ge=1, le=10)
    # 声明 成长助手后台轮询秒数（agent_background_poll_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_background_poll_seconds: float = Field(default=2.0, ge=0.5, le=30.0)
    # 声明 成长助手后台任务超时秒数（agent_background_task_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_background_task_timeout_seconds: float = Field(default=300.0, ge=30.0, le=1800.0)
    # 声明 成长助手后台最大尝试次数（agent_background_max_attempts），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_background_max_attempts: int = Field(default=3, ge=1, le=10)

    # P1.4 向量服务（OpenAI 兼容 /embeddings）；未配置密钥时向量化任务失败但不影响文本检索。
    # 声明 向量基础地址（embedding_base_url），类型为 str；其内容为固定文字 'https://dashscope.aliyuncs.com/compatible-mode/v1'。
    embedding_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    # 声明 向量模型（embedding_model），类型为 str；其内容为固定文字 'text-embedding-v4'。
    embedding_model: str = "text-embedding-v4"
    # 声明 向量dimension（embedding_dimension），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    embedding_dimension: int = Field(default=1024, ge=1024, le=1024)
    # 声明 向量接口键（embedding_api_key），类型为 SecretStr；其内容为执行“调用 SecretStr 完成默认显示时隐藏实际内容的私密文字类型处理”得到的结果。
    embedding_api_key: SecretStr = SecretStr("")
    # 声明 向量超时秒数（embedding_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    embedding_timeout_seconds: float = Field(default=30.0, gt=0, le=120)
    # 声明 成长助手retrieval最大文档片段列表（agent_retrieval_max_chunks），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_retrieval_max_chunks: int = Field(default=6, ge=1, le=20)
    # 声明 成长助手retrieval最大chars（agent_retrieval_max_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_retrieval_max_chars: int = Field(default=3000, ge=200, le=12_000)

    # P1.5 规划工作流：方案确认与提交的有效期。
    # 声明 成长助手规划工作流ttl秒数（agent_workflow_ttl_seconds），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_workflow_ttl_seconds: int = Field(default=900, ge=120, le=7200)
    # 规划方案是结构化 JSON，单独预算；长方案截断时仍会追加精简约束重试。
    # 声明 成长助手规划最大输出凭据处理器（agent_planning_max_output_tokens），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_planning_max_output_tokens: int = Field(default=8192, ge=1024, le=8192)

    # P1.6 成长复盘：单独的输出预算、任务内重试与按用户的生成频率上限。
    # 声明 成长助手复盘最大输出凭据处理器（agent_review_max_output_tokens），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_review_max_output_tokens: int = Field(default=4096, ge=512, le=8192)
    # 声明 成长助手复盘最大尝试次数（agent_review_max_attempts），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_review_max_attempts: int = Field(default=2, ge=1, le=3)
    # 声明 成长助手复盘频率上限数量（agent_review_rate_limit_count），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_review_rate_limit_count: int = Field(default=6, ge=1, le=100)
    # 声明 成长助手复盘频率上限秒数（agent_review_rate_limit_seconds），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_review_rate_limit_seconds: int = Field(default=3600, ge=60, le=86400)

    # P1.3 只读业务工具；关闭后模型只使用对话上下文，不调用工具。
    # 声明 成长助手业务工具列表是否启用（agent_tools_enabled），类型为 bool；其内容为真，表示条件成立或开关打开。
    agent_tools_enabled: bool = True
    # 声明 成长助手业务工具最大rounds（agent_tool_max_rounds），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_tool_max_rounds: int = Field(default=5, ge=1, le=8)
    # 声明 成长助手业务工具最大调用列表（agent_tool_max_calls），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_tool_max_calls: int = Field(default=12, ge=1, le=24)
    # 声明 成长助手复盘等待秒数（agent_review_wait_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_review_wait_seconds: float = Field(default=35.0, ge=0, le=60)
    # 声明 成长助手业务工具超时秒数（agent_tool_timeout_seconds），类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_tool_timeout_seconds: float = Field(default=8.0, gt=0, le=30)
    # 声明 成长助手业务工具最大结果chars（agent_tool_max_result_chars），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    agent_tool_max_result_chars: int = Field(default=12_000, ge=1000, le=32_000)

    # 把执行“调用 SettingsConfigDict 完成配置ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = SettingsConfigDict(
        # 传入env文件（env_file），值为ENV文件（ENV_FILE）。
        env_file=ENV_FILE,
        # 传入env文件encoding（env_file_encoding），值为固定文字 'utf-8'。
        env_file_encoding="utf-8",
        # 传入casesensitive（case_sensitive），值为假，表示条件不成立或开关关闭。
        case_sensitive=False,
        # 传入extra，值为固定文字 'ignore'。
        extra="ignore",
    )

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("java_api_base_url", "llm_base_url", "embedding_base_url")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_service_url：检查 业务服务地址（service_url）；接收 值（value）。
    def validate_service_url(cls, value: str) -> str:
        # 把执行“调用 urlsplit 完成urlsplit处理”得到的结果保存到 地址（url）。
        url = urlsplit(value)
        # 检查条件 url.scheme not in {'http', 'https'} or not url.hostname or url.username or url.password or url.query or url.fragment 是否成立；成立时执行缩进的分支。
        if (
            # 继续传入或组合条件判断的结果：url.scheme not in {"http", "https"}，完成上面尚未结束的表达式。
            url.scheme not in {"http", "https"}
            # 继续补充本次调用或判断：条件判断的结果：or not url.hostname。
            or not url.hostname
            # 继续补充本次调用或判断：表达式 or url.username 的结果。
            or url.username
            # 继续补充本次调用或判断：表达式 or url.password 的结果。
            or url.password
            # 继续补充本次调用或判断：表达式 or url.query 的结果。
            or url.query
            # 继续补充本次调用或判断：表达式 or url.fragment 的结果。
            or url.fragment
        ):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("服务地址必须是无内嵌凭据的 HTTP(S) 地址")
        # 把执行“调用 value.rstrip 完成rstrip处理”得到的结果交还给调用方，本函数到此结束。
        return value.rstrip("/")

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("java_api_base_url")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_java_prefix：检查 Java后端前缀（java_prefix）；接收 值（value）。
    def validate_java_prefix(cls, value: str) -> str:
        # 检查条件 urlsplit(value).path.rstrip('/') not in {'', '/asterway'} 是否成立；成立时执行缩进的分支。
        if urlsplit(value).path.rstrip("/") not in {"", "/asterway"}:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("Java 服务地址只支持服务根地址或 /asterway 前缀")
        # 把值（value）交还给调用方，本函数到此结束。
        return value

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("agent_database_url")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_database_url：检查 数据库地址（database_url）；接收 值（value）。
    def validate_database_url(cls, value: SecretStr) -> SecretStr:
        # 把执行“只在连接或签名时取出密钥实际值”得到的结果保存到 原始值（raw）。
        raw = value.get_secret_value()
        # 检查条件 not raw 是否成立；成立时执行缩进的分支。
        if not raw:
            # 把值（value）交还给调用方，本函数到此结束。
            return value
        # 把执行“调用 urlsplit 完成urlsplit处理”得到的结果保存到 地址（url）。
        url = urlsplit(raw)
        # 检查条件 url.scheme != 'postgresql+asyncpg' or not url.hostname or (not url.path.strip('/')) 是否成立；成立时执行缩进的分支。
        if url.scheme != "postgresql+asyncpg" or not url.hostname or not url.path.strip("/"):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("Agent 数据库地址必须使用 postgresql+asyncpg 并包含数据库名")
        # 把值（value）交还给调用方，本函数到此结束。
        return value

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_summary_budget：检查 汇总额度（summary_budget）。
    def validate_summary_budget(self) -> "Settings":
        # 检查条件 self.agent_summary_keep_messages >= self.agent_summary_trigger_messages 是否成立；成立时执行缩进的分支。
        if self.agent_summary_keep_messages >= self.agent_summary_trigger_messages:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("摘要保留条数必须小于触发条数")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self


# 缓存函数结果，避免反复创建相同配置或对象。
@lru_cache
# 函数 get_settings：读取并复用当前进程的运行配置。
def get_settings() -> Settings:
    # 把执行“调用 Settings 完成配置处理”得到的结果交还给调用方，本函数到此结束。
    return Settings()
