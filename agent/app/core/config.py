"""Agent 本地配置；不维护业务表连接或 Java JWT 签名密钥。"""

from functools import lru_cache
from pathlib import Path
from urllib.parse import urlsplit

from pydantic import Field, SecretStr, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict

ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    app_name: str = "Asterway Agent"
    app_env: str = "local"
    agent_port: int = Field(default=8001, ge=1, le=65535)
    java_api_base_url: str = "http://127.0.0.1:8089"
    agent_internal_key: SecretStr = SecretStr("")
    java_timeout_seconds: float = Field(default=15.0, gt=0, le=60)
    java_max_response_bytes: int = Field(default=2_000_000, ge=1024, le=10_000_000)

    llm_provider: str = "deepseek"
    llm_base_url: str = "https://api.deepseek.com"
    llm_api_key: SecretStr = SecretStr("")
    llm_model: str = "deepseek-flash"
    llm_timeout_seconds: float = Field(default=60.0, gt=0, le=180)
    llm_max_output_tokens: int = Field(default=2048, ge=256, le=8192)

    agent_request_timeout_seconds: float = Field(default=240.0, gt=0, le=300)
    agent_max_concurrent_requests: int = Field(default=8, ge=1, le=32)

    # P1.2 会话持久化；数据库使用独立受限角色，只访问 agent schema。
    agent_database_url: SecretStr = SecretStr("")
    agent_db_pool_size: int = Field(default=5, ge=1, le=20)
    agent_db_max_overflow: int = Field(default=5, ge=0, le=20)
    # 规划/复盘工具会在一次对话轮次内串联多次模型调用，轮次预算需覆盖完整工具链。
    agent_turn_timeout_seconds: float = Field(default=240.0, gt=0, le=300)
    agent_partial_save_interval_seconds: float = Field(default=2.0, ge=0.5, le=10)
    agent_context_recent_messages: int = Field(default=20, ge=2, le=100)
    agent_context_max_chars: int = Field(default=24000, ge=2000, le=100_000)
    agent_summary_trigger_messages: int = Field(default=16, ge=4, le=100)
    agent_summary_keep_messages: int = Field(default=8, ge=2, le=50)
    agent_summary_max_chars: int = Field(default=8000, ge=500, le=16_000)

    # P1.4 长期记忆：只注入已确认记忆，预算与条数均可配置。
    agent_memory_context_max_chars: int = Field(default=2000, ge=200, le=8000)
    agent_memory_context_limit: int = Field(default=40, ge=1, le=100)

    # P1.4 知识库：文件仅存本地受控目录，解析/向量化由单进程后台任务执行。
    agent_knowledge_dir: str = "data/knowledge"
    agent_knowledge_max_bytes: int = Field(default=26_214_400, ge=1024, le=52_428_800)
    agent_knowledge_parse_max_chars: int = Field(default=2_000_000, ge=10_000, le=10_000_000)
    agent_knowledge_chunk_chars: int = Field(default=1200, ge=200, le=4000)
    agent_knowledge_chunk_overlap_chars: int = Field(default=150, ge=0, le=800)
    agent_knowledge_max_chunks: int = Field(default=4000, ge=1, le=20_000)
    agent_knowledge_embedding_batch: int = Field(default=10, ge=1, le=10)
    agent_background_poll_seconds: float = Field(default=2.0, ge=0.5, le=30.0)
    agent_background_task_timeout_seconds: float = Field(default=300.0, ge=30.0, le=1800.0)
    agent_background_max_attempts: int = Field(default=3, ge=1, le=10)

    # P1.4 向量服务（OpenAI 兼容 /embeddings）；未配置密钥时向量化任务失败但不影响文本检索。
    embedding_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v4"
    embedding_dimension: int = Field(default=1024, ge=1024, le=1024)
    embedding_api_key: SecretStr = SecretStr("")
    embedding_timeout_seconds: float = Field(default=30.0, gt=0, le=120)
    agent_retrieval_max_chunks: int = Field(default=6, ge=1, le=20)
    agent_retrieval_max_chars: int = Field(default=3000, ge=200, le=12_000)

    # P1.5 规划工作流：方案确认与提交的有效期。
    agent_workflow_ttl_seconds: int = Field(default=900, ge=120, le=7200)
    # 规划方案是结构化 JSON，单独预算；长方案截断时仍会追加精简约束重试。
    agent_planning_max_output_tokens: int = Field(default=8192, ge=1024, le=8192)

    # P1.6 成长复盘：单独的输出预算、任务内重试与按用户的生成频率上限。
    agent_review_max_output_tokens: int = Field(default=4096, ge=512, le=8192)
    agent_review_max_attempts: int = Field(default=2, ge=1, le=3)
    agent_review_rate_limit_count: int = Field(default=6, ge=1, le=100)
    agent_review_rate_limit_seconds: int = Field(default=3600, ge=60, le=86400)

    # P1.3 只读业务工具；关闭后模型只使用对话上下文，不调用工具。
    agent_tools_enabled: bool = True
    agent_tool_max_rounds: int = Field(default=5, ge=1, le=8)
    agent_tool_max_calls: int = Field(default=12, ge=1, le=24)
    agent_review_wait_seconds: float = Field(default=35.0, ge=0, le=60)
    agent_tool_timeout_seconds: float = Field(default=8.0, gt=0, le=30)
    agent_tool_max_result_chars: int = Field(default=12_000, ge=1000, le=32_000)

    model_config = SettingsConfigDict(
        env_file=ENV_FILE,
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    @field_validator("java_api_base_url", "llm_base_url", "embedding_base_url")
    @classmethod
    def validate_service_url(cls, value: str) -> str:
        url = urlsplit(value)
        if (
            url.scheme not in {"http", "https"}
            or not url.hostname
            or url.username
            or url.password
            or url.query
            or url.fragment
        ):
            raise ValueError("服务地址必须是无内嵌凭据的 HTTP(S) 地址")
        return value.rstrip("/")

    @field_validator("java_api_base_url")
    @classmethod
    def validate_java_prefix(cls, value: str) -> str:
        if urlsplit(value).path.rstrip("/") not in {"", "/asterway"}:
            raise ValueError("Java 服务地址只支持服务根地址或 /asterway 前缀")
        return value

    @field_validator("agent_database_url")
    @classmethod
    def validate_database_url(cls, value: SecretStr) -> SecretStr:
        raw = value.get_secret_value()
        if not raw:
            return value
        url = urlsplit(raw)
        if url.scheme != "postgresql+asyncpg" or not url.hostname or not url.path.strip("/"):
            raise ValueError("Agent 数据库地址必须使用 postgresql+asyncpg 并包含数据库名")
        return value

    @model_validator(mode="after")
    def validate_summary_budget(self) -> "Settings":
        if self.agent_summary_keep_messages >= self.agent_summary_trigger_messages:
            raise ValueError("摘要保留条数必须小于触发条数")
        return self


@lru_cache
def get_settings() -> Settings:
    return Settings()
