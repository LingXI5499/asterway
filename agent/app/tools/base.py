# 封装基础查询工具的参数与结果，读取仍经 Java 核对权限和数据归属。
# 只读业务工具的类型定义；工具只查询，不写数据库、不改变业务事实。

# 从 collections.abc 引入 Awaitable、Callable。
from collections.abc import Awaitable, Callable

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）。
from dataclasses import dataclass

# 从 typing 引入 Any。
from typing import Any

# 从 pydantic 引入 基础模型（BaseModel）。
from pydantic import BaseModel

# 从 app.core.context 引入 成长助手上下文（AgentContext）。
from app.core.context import AgentContext

# 从 app.integrations.java_client 引入 Java后端接口客户端（JavaApiClient）。
from app.integrations.java_client import JavaApiClient

# 把从Callable中按依次包含 依次包含 成长助手上下文（AgentContext）、Java后端接口客户端（JavaApiClient）、基础模型（BaseModel） 的集合、从Awaitable中按固定文字 'ToolOutcome'取出的内容 的集合取出的内容保存到 业务工具Handler（ToolHandler）。
ToolHandler = Callable[[AgentContext, JavaApiClient, BaseModel], Awaitable["ToolOutcome"]]


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 ToolOutcome，把业务工具Outcome需要的数据与操作放在同一个对象中。
class ToolOutcome:
    # 工具返回给模型的内容；content 必须是可直接放入 tool 消息的文本。

    # 声明 内容（content），类型为 str。
    content: str
    # 声明 汇总（summary），类型为 str。
    summary: str
    # 声明 是否错误（is_error），类型为 bool；其内容为假，表示条件不成立或开关关闭。
    is_error: bool = False


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 ToolSpec，把业务工具Spec需要的数据与操作放在同一个对象中。
class ToolSpec:
    # 声明 名称（name），类型为 str。
    name: str
    # 声明 显示名称（label），类型为 str。
    label: str
    # 声明 说明（description），类型为 str。
    description: str
    # 声明 权限（permission），类型为 str。
    permission: str
    # 声明 参数模型（args_model），类型为 type[BaseModel]。
    args_model: type[BaseModel]
    # 声明 handler，类型为 ToolHandler。
    handler: ToolHandler
    # 声明 extra权限列表（extra_permissions），类型为 tuple[str, ...]；其内容为空列表或集合。
    extra_permissions: tuple[str, ...] = ()

    # 函数 allowed：调用 allowed 完成allowed处理；接收 权限列表（permissions）。
    def allowed(self, permissions: frozenset[str]) -> bool:
        # 把执行“调用 all 完成全部处理”得到的结果交还给调用方，本函数到此结束。
        return all(value in permissions for value in (self.permission, *self.extra_permissions))

    # 把这个方法作为只读属性访问。
    @property
    # 函数 parameters：调用 parameters 完成parameters处理。
    def parameters(self) -> dict[str, Any]:
        # 把执行“调用 self.args_model.model_json_schema 完成模型JSON数据数据库分区处理”得到的结果交还给调用方，本函数到此结束。
        return self.args_model.model_json_schema()


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass(frozen=True)
# 定义 ToolExecution，把业务工具执行计时需要的数据与操作放在同一个对象中。
class ToolExecution:
    # 一次工具调用的最终结果，供轮次服务和 SSE 使用。

    # 声明 名称（name），类型为 str。
    name: str
    # 声明 显示名称（label），类型为 str。
    label: str
    # 声明 内容（content），类型为 str。
    content: str
    # 声明 汇总（summary），类型为 str。
    summary: str
    # 声明 是否错误（is_error），类型为 bool。
    is_error: bool
    # 声明 持续时间ms（duration_ms），类型为 int。
    duration_ms: int
    # 声明 是否limited（is_limited），类型为 bool；其内容为假，表示条件不成立或开关关闭。
    is_limited: bool = False
