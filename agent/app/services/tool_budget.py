# 分别计算只读查询、规划与复盘机会，复用本轮已查询的结果。
# 每轮查询预算与结果复用；草案和复盘各自保留独立操作机会。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 dataclasses 引入 自动补齐字段初始化等方法的数据类标记（dataclass）、字段（field）、整体替换（replace）。
from dataclasses import dataclass, field, replace

# 从 app.tools.base 引入 业务工具执行计时（ToolExecution）。
from app.tools.base import ToolExecution


# 自动补齐这个数据类的初始化与比较方法，字段名和类型仍以类内声明为准。
@dataclass
# 定义 ReadBudget，把读取额度需要的数据与操作放在同一个对象中。
class ReadBudget:
    # 声明 本轮允许的最大工具调用次数（max_calls），类型为 int。
    max_calls: int
    # 声明 本轮允许的最大查询轮数（max_rounds），类型为 int。
    max_rounds: int
    # 声明 本轮已经使用的工具调用次数（calls），类型为 int；其内容为固定数值 0。
    calls: int = 0
    # 声明 本轮已经使用的查询轮数（rounds），类型为 int；其内容为固定数值 0。
    rounds: int = 0
    # 声明 缓存（cache），类型为 dict[tuple[str, str], ToolExecution]；其内容为执行“声明数据类字段的默认值或创建规则，避免实例之间共用同一个可变对象”得到的结果。
    cache: dict[tuple[str, str], ToolExecution] = field(default_factory=dict)

    # 把这个方法作为只读属性访问。
    @property
    # 函数 available：判断本轮已用调用次数和轮数是否都还未达到各自上限。
    def available(self) -> bool:
        # 把条件 self.calls < self.max_calls 是否成立并且条件 self.rounds < self.max_rounds 是否成立交还给调用方，本函数到此结束。
        return self.calls < self.max_calls and self.rounds < self.max_rounds

    # 这个方法不依赖具体实例，可以直接通过类调用。
    @staticmethod
    # 函数 key：把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键；接收 名称（name）、arguments。
    def key(name: str, arguments: str) -> tuple[str, str] | None:
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把执行“把 JSON 文字解析为对象”得到的结果保存到 值（value）。
            value = json.loads(arguments or "{}")
        # 捕获(ValueError, RecursionError)异常，执行下面的错误处理。
        except (ValueError, RecursionError):
            # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
            return None
        # 检查条件 not isinstance(value, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(value, dict):
            # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
            return None
        # 把依次包含 名称（name）、执行“把对象编码为 JSON 文字”得到的结果 的集合交还给调用方，本函数到此结束。
        return name, json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))

    # 函数 cached：按工具名与标准化参数读取本轮缓存；命中后返回可复用的结果副本；接收 名称（name）、arguments。
    def cached(self, name: str, arguments: str) -> ToolExecution | None:
        # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 值（value）。
        value = self.cache.get(self.key(name, arguments))
        # 把条件 value 是否成立时使用执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果，否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return replace(value, summary="复用本轮已核对结果：" + value.summary, duration_ms=0) if value else None

    # 函数 remember：累计一次查询；只有成功且未受限的结果才写入本轮缓存；接收 名称（name）、arguments、结果（result）。
    def remember(self, name: str, arguments: str, result: ToolExecution) -> None:
        # 把 self.calls 加上 固定数值 1，再保存回原变量。
        self.calls += 1
        # 把执行“把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键”得到的结果保存到 键（key）。
        key = self.key(name, arguments)
        # 检查条件 key and (not result.is_error) and (not result.is_limited) 是否成立；成立时执行缩进的分支。
        if key and not result.is_error and not result.is_limited:
            # 把结果（result）保存到 self缓存键（self.cache[key]）。
            self.cache[key] = result

    # 函数 hint：把当前剩余次数、轮数和查询约束整理成提示文字。
    def hint(self) -> str:
        # 把在文字占位处填入变量后组成的字符串交还给调用方，本函数到此结束。
        return (
            # 继续传入或组合按表达式计算得到的值：f"本轮只读查询还可执行 {max(0, self.max_calls - self.calls)} 次，"，完成上面尚未结束的表达式。
            f"本轮只读查询还可执行 {max(0, self.max_calls - self.calls)} 次，"
            f"还可往返 {max(0, self.max_rounds - self.rounds)} 轮。"
            "查询目标的阶段和任务时优先 get_goal_roadmap，避免依次重复读取列表和详情。"
            "先取得回答所需的关键事实，勿为简单问题遍历无关对象。结果有分页或部分失败必须说明范围。"
            "prepare_growth_plan 与 review_growth_progress 各有一次独立机会，不占用上述只读额度。"
            "未提供的工具不可调用；资料不足时明确尚未核对的内容，不能声称查询完整或业务已修改。"
        )
