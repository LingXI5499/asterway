# 声明长期记忆输入输出结构，以及字段类型、默认值和合法范围。
# 长期记忆请求与响应模型；对外字段统一 camelCase。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 pydantic 引入 字段（Field）、字段validator（field_validator）。
from pydantic import Field, field_validator

# 从 app.schemas.conversation 引入 Camel模型（CamelModel）、StrictCamel模型（StrictCamelModel）。
from app.schemas.conversation import CamelModel, StrictCamelModel

# 把依次包含 固定状态或编码 PREFERENCE（PREFERENCE）、固定状态或编码 FACT（FACT）、固定状态或编码 CONSTRAINT（约束）、固定状态或编码 GOAL_CONTEXT（目标上下文） 的集合保存到 长期记忆KINDS（MEMORY_KINDS）。
MEMORY_KINDS = ("PREFERENCE", "FACT", "CONSTRAINT", "GOAL_CONTEXT")
# 把依次包含 固定状态或编码 CANDIDATE（候选）、固定状态或编码 CONFIRMED（已确认）、固定状态或编码 REJECTED（REJECTED）、固定状态或编码 ARCHIVED（已归档） 的集合保存到 长期记忆STATUSES（MEMORY_STATUSES）。
MEMORY_STATUSES = ("CANDIDATE", "CONFIRMED", "REJECTED", "ARCHIVED")
# 把依次包含 固定状态或编码 USER_CONFIRMED（用户已确认）、固定状态或编码 USER_STATED（用户STATED）、固定状态或编码 INFERRED（INFERRED） 的集合保存到 长期记忆SOURCES（MEMORY_SOURCES）。
MEMORY_SOURCES = ("USER_CONFIRMED", "USER_STATED", "INFERRED")


# 函数 _clean_content：调用 _clean_content 完成clean内容处理；接收 值（value）。
def _clean_content(value: str) -> str:
    # 把执行“去掉文字两端的空白”得到的结果保存到 内容（content）。
    content = value.strip()
    # 检查条件 not content 是否成立；成立时执行缩进的分支。
    if not content:
        # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
        raise ValueError("记忆内容不能为空白")
    # 把内容（content）交还给调用方，本函数到此结束。
    return content


# 定义 MemoryCandidateRequest，把长期记忆候选请求需要的数据与操作放在同一个对象中。
class MemoryCandidateRequest(StrictCamelModel):
    # 声明 种类（kind），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    kind: str = Field(pattern="^(PREFERENCE|FACT|CONSTRAINT|GOAL_CONTEXT)$")
    # 声明 内容（content），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    content: str = Field(min_length=1, max_length=4000)
    # 声明 来源（source），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    source: str = Field(
        # 传入默认值（default），值为固定文字 'USER_STATED", pattern="^(USER_CONFIRMED|USER_STATED|INFERRED)$'。
        default="USER_STATED", pattern="^(USER_CONFIRMED|USER_STATED|INFERRED)$"
    )
    # 声明 confidence，类型为 float；其内容为执行“调用 Field 完成字段处理”得到的结果。
    confidence: float = Field(default=0.8, ge=0, le=1)
    # 声明 对话编号（conversationId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    conversationId: int | None = Field(default=None, ge=1)
    # 声明 本轮对话编号（turnId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    turnId: int | None = Field(default=None, ge=1)
    # 声明 supersedes编号（supersedesId），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    supersedesId: int | None = Field(default=None, ge=1)

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("content")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_content：检查 内容（content）；接收 值（value）。
    def validate_content(cls, value: str) -> str:
        # 把执行“调用 _clean_content 完成clean内容处理”得到的结果交还给调用方，本函数到此结束。
        return _clean_content(value)


# 定义 MemoryUpdateRequest，把长期记忆更新请求需要的数据与操作放在同一个对象中。
class MemoryUpdateRequest(StrictCamelModel):
    # 声明 内容（content），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    content: str = Field(min_length=1, max_length=4000)
    # 声明 版本号（version），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    version: int = Field(ge=0)

    # 在字段值进入对象之前或之后，按这里指定的阶段执行检查。
    @field_validator("content")
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 validate_content：检查 内容（content）；接收 值（value）。
    def validate_content(cls, value: str) -> str:
        # 把执行“调用 _clean_content 完成clean内容处理”得到的结果交还给调用方，本函数到此结束。
        return _clean_content(value)


# 定义 MemoryVersionRequest，把长期记忆版本号请求需要的数据与操作放在同一个对象中。
class MemoryVersionRequest(StrictCamelModel):
    # 声明 版本号（version），类型为 int；其内容为执行“调用 Field 完成字段处理”得到的结果。
    version: int = Field(ge=0)


# 定义 MemoryVO，把长期记忆需要的数据与操作放在同一个对象中。
class MemoryVO(CamelModel):
    # 声明 编号（id），类型为 str。
    id: str
    # 声明 种类（kind），类型为 str。
    kind: str
    # 声明 种类显示名称（kind_label），类型为 str。
    kind_label: str
    # 声明 内容（content），类型为 str。
    content: str
    # 声明 来源（source），类型为 str。
    source: str
    # 声明 状态（status），类型为 str。
    status: str
    # 声明 状态显示名称（status_label），类型为 str。
    status_label: str
    # 声明 confidence，类型为 float。
    confidence: float
    # 声明 来源对话编号（source_conversation_id），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    source_conversation_id: str | None = None
    # 声明 来源本轮对话编号（source_turn_id），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    source_turn_id: str | None = None
    # 声明 supersedes编号（supersedes_id），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    supersedes_id: str | None = None
    # 声明 已确认at（confirmed_at），类型为 datetime | None；其内容为空值，表示当前还没有对应内容。
    confirmed_at: datetime | None = None
    # 声明 rejectedat（rejected_at），类型为 datetime | None；其内容为空值，表示当前还没有对应内容。
    rejected_at: datetime | None = None
    # 声明 已归档at（archived_at），类型为 datetime | None；其内容为空值，表示当前还没有对应内容。
    archived_at: datetime | None = None
    # 声明 创建时at（created_at），类型为 datetime。
    created_at: datetime
    # 声明 更新时at（updated_at），类型为 datetime。
    updated_at: datetime
    # 声明 版本号（version），类型为 int。
    version: int


# 定义 MemoryPageVO，把长期记忆当前页需要的数据与操作放在同一个对象中。
class MemoryPageVO(CamelModel):
    # 声明 数据列表（items），类型为 list[MemoryVO]。
    items: list[MemoryVO]
    # 声明 总数（total），类型为 int。
    total: int
    # 声明 当前页（page），类型为 int。
    page: int
    # 声明 每页条数（size），类型为 int。
    size: int


# 定义 MemoryContextVO，把长期记忆上下文需要的数据与操作放在同一个对象中。
class MemoryContextVO(CamelModel):
    # 供内部调用检查当前会注入哪些已确认记忆。

    # 声明 数据列表（items），类型为 list[MemoryVO]。
    items: list[MemoryVO]
    # 声明 chars，类型为 int。
    chars: int
