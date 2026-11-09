# 声明对话输入输出结构，以及字段类型、默认值和合法范围。

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、字段validator（field_validator）。
from pydantic import BaseModel, ConfigDict, Field, field_validator


# 定义 ChatRequest，把Chat请求需要的数据与操作放在同一个对象中。
class ChatRequest(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid")
    # 声明 消息（message），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    message: str = Field(
        # 传入最小长度（min_length），值为固定数值 1。
        min_length=1,
        # 传入最大长度（max_length），值为固定数值 8000。
        max_length=8000,
        # 传入说明（description），值为固定文字 '用户发送的消息'。
        description="用户发送的消息",
    )

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


# 定义 ChatResponse，把Chat响应需要的数据与操作放在同一个对象中。
class ChatResponse(BaseModel):
    # 声明 reply，类型为 str。
    reply: str
    # 声明 模型（model），类型为 str。
    model: str
    # 声明 finish原因（finish_reason），类型为 str；其内容为固定文字 'stop'。
    finish_reason: str = "stop"
    # 声明 追踪标识编号（trace_id），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    trace_id: str | None = None
