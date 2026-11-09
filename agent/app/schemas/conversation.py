# 声明对话输入输出结构，以及字段类型、默认值和合法范围。
# 会话、轮次与消息的请求和响应模型；对外字段统一使用 camelCase。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 typing 引入 Any。
from typing import Annotated, Any

# 从 uuid 引入 UUID。
from uuid import UUID

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、字段validator（field_validator）。
from pydantic import BaseModel, ConfigDict, Field, field_validator

# 从 pydantic.alias_generators 引入 tocamel（to_camel）。
from pydantic.alias_generators import to_camel


# 定义 CamelModel，把Camel模型需要的数据与操作放在同一个对象中。
class CamelModel(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


# 定义 StrictCamelModel，把StrictCamel模型需要的数据与操作放在同一个对象中。
class StrictCamelModel(CamelModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(
        # 传入aliasgenerator（alias_generator），值为表达式 to_camel, populate_by_name=True, extra="forbid" 的结果。
        alias_generator=to_camel, populate_by_name=True, extra="forbid"
    )


# 函数 _clean_title：调用 _clean_title 完成clean标题处理；接收 值（value）。
def _clean_title(value: str) -> str:
    # 把执行“去掉文字两端的空白”得到的结果保存到 标题（title）。
    title = value.strip()
    # 检查条件 not title 是否成立；成立时执行缩进的分支。
    if not title:
        # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
        raise ValueError("标题不能为空白")
    # 把标题（title）交还给调用方，本函数到此结束。
    return title


# 定义 ConversationCreateRequest，把对话创建请求需要的数据与操作放在同一个对象中。
class ConversationCreateRequest(StrictCamelModel):
    # 声明 标题（title），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    title: str | None = Field(default=None, max_length=100)

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("title")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_title：检查 标题（title）；接收 值（value）。
    def validate_title(cls, value: str | None) -> str | None:
        # 把value is None，即对应内容是否不存在时使用空值，表示当前还没有对应内容，否则使用执行“调用 _clean_title 完成clean标题处理”得到的结果交还给调用方，本函数到此结束。
        return None if value is None else _clean_title(value)

    # 显式空列表表示本次不引用知识资料。
    knowledge_base_ids: list[Annotated[int, Field(gt=0)]] = Field(default_factory=list, max_length=100)


# 定义 ConversationTitleRequest，把对话标题请求需要的数据与操作放在同一个对象中。
class ConversationTitleRequest(StrictCamelModel):
    # 声明 标题（title），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    title: str = Field(min_length=1, max_length=100)
    # 声明 版本号（version），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    version: int = Field(ge=0)

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("title")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_title：检查 标题（title）；接收 值（value）。
    def validate_title(cls, value: str) -> str:
        # 把执行“调用 _clean_title 完成clean标题处理”得到的结果交还给调用方，本函数到此结束。
        return _clean_title(value)


# 定义 ConversationVersionRequest，把对话版本号请求需要的数据与操作放在同一个对象中。
class ConversationVersionRequest(StrictCamelModel):
    # 声明 版本号（version），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    version: int = Field(ge=0)


# 定义 ConversationMessageRequest，把对话消息请求需要的数据与操作放在同一个对象中。
class ConversationMessageRequest(StrictCamelModel):
    # 声明 消息（message），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    message: str = Field(min_length=1, max_length=8000)
    # 声明 请求编号（request_id），类型为 UUID。
    request_id: UUID

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("message")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_message：检查 消息（message）；接收 值（value）。
    def validate_message(cls, value: str) -> str:
        # 检查条件 not value.strip() 是否成立；成立时执行缩进的分支。
        if not value.strip():
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("消息不能全部为空白")
        # 把值（value）交还给调用方，本函数到此结束。
        return value

    # 显式空列表表示本次不引用知识资料。
    knowledge_base_ids: list[Annotated[int, Field(gt=0)]] | None = Field(default=None, max_length=100)


# 定义 ConversationVO，把对话需要的数据与操作放在同一个对象中。
class ConversationVO(CamelModel):
    # 声明 编号（id），类型为 str。
    id: str
    # 声明 标题（title），类型为 str。
    title: str
    # 声明 状态（status），类型为 str。
    status: str
    # 声明 汇总through消息no（summary_through_message_no），类型为 int。
    summary_through_message_no: int
    # 声明 最后一次消息at（last_message_at），类型为 datetime | None；其内容为空值，表示当前还没有对应内容。
    last_message_at: datetime | None = None
    # 声明 创建时at（created_at），类型为 datetime。
    created_at: datetime
    # 声明 更新时at（updated_at），类型为 datetime。
    updated_at: datetime
    # 声明 版本号（version），类型为 int。
    version: int

    # 显式空列表表示本次不引用知识资料。
    knowledge_base_ids: list[str] = Field(default_factory=list)


# 定义 TurnVO，把本轮对话需要的数据与操作放在同一个对象中。
class TurnVO(CamelModel):
    # 声明 编号（id），类型为 str。
    id: str
    # 声明 对话编号（conversation_id），类型为 str。
    conversation_id: str
    # 声明 请求编号（request_id），类型为 str。
    request_id: str
    # 声明 状态（status），类型为 str。
    status: str
    # 声明 finish原因（finish_reason），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    finish_reason: str | None = None
    # 声明 错误编码（error_code），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    error_code: str | None = None
    # 声明 开始时at（started_at），类型为 datetime。
    started_at: datetime
    # 声明 finishedat（finished_at），类型为 datetime | None；其内容为空值，表示当前还没有对应内容。
    finished_at: datetime | None = None
    # 声明 版本号（version），类型为 int。
    version: int
    # 声明 reply，类型为 str | None；其内容为空值，表示当前还没有对应内容。
    reply: str | None = None


# 定义 ConversationDetailVO，把对话详情需要的数据与操作放在同一个对象中。
class ConversationDetailVO(ConversationVO):
    # 声明 运行中本轮对话（running_turn），类型为 TurnVO | None；其内容为空值，表示当前还没有对应内容。
    running_turn: TurnVO | None = None


# 定义 MessageVO，把消息需要的数据与操作放在同一个对象中。
class MessageVO(CamelModel):
    # 声明 citations，类型为 list[dict[str, Any]]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    citations: list[dict[str, Any]] = Field(default_factory=list)
    # 声明 used长期记忆编号列表（used_memory_ids），类型为 list[str]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    used_memory_ids: list[str] = Field(default_factory=list)
    # 声明 编号（id），类型为 str。
    id: str
    # 声明 消息no（message_no），类型为 int。
    message_no: int
    # 声明 角色（role），类型为 str。
    role: str
    # 声明 内容（content），类型为 str。
    content: str
    # 声明 是否完成（is_complete），类型为 bool。
    is_complete: bool
    # 声明 finish原因（finish_reason），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    finish_reason: str | None = None
    # 声明 模型（model），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    model: str | None = None
    # 声明 用量（usage），类型为 dict[str, Any] | None；其内容为空值，表示当前还没有对应内容。
    usage: dict[str, Any] | None = None
    # 声明 创建时at（created_at），类型为 datetime。
    created_at: datetime


# 定义 MessagePageVO，把消息当前页需要的数据与操作放在同一个对象中。
class MessagePageVO(CamelModel):
    # 声明 数据列表（items），类型为 list[MessageVO]。
    items: list[MessageVO]
    # 声明 是否存在more（has_more），类型为 bool。
    has_more: bool
    # 声明 下一个变化前消息no（next_before_message_no），类型为 int | None；其内容为空值，表示当前还没有对应内容。
    next_before_message_no: int | None = None


# 定义 ConversationPageVO，把对话当前页需要的数据与操作放在同一个对象中。
class ConversationPageVO(CamelModel):
    # 声明 数据列表（items），类型为 list[ConversationVO]。
    items: list[ConversationVO]
    # 声明 总数（total），类型为 int。
    total: int
    # 声明 当前页（page），类型为 int。
    page: int
    # 声明 每页条数（size），类型为 int。
    size: int


# 定义 SendResultVO，把发送结果需要的数据与操作放在同一个对象中。
class SendResultVO(CamelModel):
    # 声明 reply，类型为 str。
    reply: str
    # 声明 模型（model），类型为 str。
    model: str
    # 声明 finish原因（finish_reason），类型为 str。
    finish_reason: str
    # 声明 对话编号（conversation_id），类型为 str。
    conversation_id: str
    # 声明 本轮对话编号（turn_id），类型为 str。
    turn_id: str
    # 声明 请求编号（request_id），类型为 str。
    request_id: str
    # 声明 本轮对话版本号（turn_version），类型为 int。
    turn_version: int


class ConversationKnowledgeRequest(StrictCamelModel):
    knowledge_base_ids: list[Annotated[int, Field(gt=0)]] = Field(default_factory=list, max_length=100)
    version: int = Field(ge=0)
