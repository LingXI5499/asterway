"""每轮查询预算与结果复用；草案和复盘各自保留独立操作机会。"""

import json
from dataclasses import dataclass, field, replace

from app.tools.base import ToolExecution


@dataclass
class ReadBudget:
    max_calls: int
    max_rounds: int
    calls: int = 0
    rounds: int = 0
    cache: dict[tuple[str, str], ToolExecution] = field(default_factory=dict)

    @property
    def available(self) -> bool:
        return self.calls < self.max_calls and self.rounds < self.max_rounds

    @staticmethod
    def key(name: str, arguments: str) -> tuple[str, str] | None:
        try:
            value = json.loads(arguments or "{}")
        except (ValueError, RecursionError):
            return None
        if not isinstance(value, dict):
            return None
        return name, json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))

    def cached(self, name: str, arguments: str) -> ToolExecution | None:
        value = self.cache.get(self.key(name, arguments))
        return replace(value, summary="复用本轮已核对结果：" + value.summary, duration_ms=0) if value else None

    def remember(self, name: str, arguments: str, result: ToolExecution) -> None:
        self.calls += 1
        key = self.key(name, arguments)
        if key and not result.is_error and not result.is_limited:
            self.cache[key] = result

    def hint(self) -> str:
        return (
            f"本轮只读查询还可执行 {max(0, self.max_calls - self.calls)} 次，"
            f"还可往返 {max(0, self.max_rounds - self.rounds)} 轮。"
            "查询目标的阶段和任务时优先 get_goal_roadmap，避免依次重复读取列表和详情。"
            "先取得回答所需的关键事实，勿为简单问题遍历无关对象。结果有分页或部分失败必须说明范围。"
            "prepare_growth_plan 与 review_growth_progress 各有一次独立机会，不占用上述只读额度。"
            "未提供的工具不可调用；资料不足时明确尚未核对的内容，不能声称查询完整或业务已修改。"
        )
