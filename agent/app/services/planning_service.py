"""规划意图识别与方案生成：识别用户意图，调用模型生成结构化方案快照。"""

import json
import logging
from collections.abc import Awaitable, Callable
from typing import Any

from app.core.config import get_settings
from app.core.errors import AgentError
from app.services import workflow_service

logger = logging.getLogger(__name__)


_INTENT_KEYWORDS: dict[str, tuple[str, ...]] = {
    "HABIT_PLAN": ("习惯", "打卡", "每天", "每周三次", "坚持"),
    "WEEKLY_PLAN": ("周计划", "这周", "本周", "下周", "一周安排", "周安排"),
    "DAILY_PLAN": ("日计划", "今天安排", "明天安排", "今日安排", "每天安排"),
    "GOAL_PLAN": ("目标", "长期计划", "成长规划", "季度目标", "年度目标"),
    "STAGE_PLAN": ("阶段", "阶段性", "里程碑"),
    "TASK_PLAN": ("任务", "拆解", "待办"),
}
_COMPREHENSIVE = ("综合", "全面", "整体规划", "成长计划", "帮我规划")
_GROWTH_NOUNS = ("目标", "阶段", "任务", "计划", "安排")


def detect_intent(message: str) -> str | None:
    text = message.strip()
    if not text:
        return None
    if any(word in text for word in _COMPREHENSIVE):
        return "COMPREHENSIVE_PLAN"
    has_growth_noun = any(word in text for word in _GROWTH_NOUNS)
    # 只有“习惯/每天/坚持”类表达且没有目标、阶段、任务、计划等对象时，才判为习惯规划；
    # 带“目标/阶段/任务/计划”的需求优先按成长对象规划，避免“每天投入两小时达目标”被误判成习惯。
    if "习惯" in text and not has_growth_noun:
        return "HABIT_PLAN"
    scores: list[tuple[str, int]] = []
    for intent, words in _INTENT_KEYWORDS.items():
        if has_growth_noun and intent == "HABIT_PLAN":
            continue
        score = sum(text.count(word) for word in words)
        if score:
            scores.append((intent, score))
    if not scores:
        return "HABIT_PLAN" if "习惯" in text else None
    scores.sort(key=lambda item: item[1], reverse=True)
    if len(scores) >= 3:
        return "COMPREHENSIVE_PLAN"
    return scores[0][0]


_SYSTEM_PROMPT = """你是 Asterway 成长规划器。把用户的规划需求整理成严格 JSON 方案快照，只输出一个合法 json 对象，不输出解释或代码块围栏。
方案快照固定结构（未使用的部分输出空数组或 null）：
{
  "intent": "GOAL_PLAN|STAGE_PLAN|TASK_PLAN|WEEKLY_PLAN|DAILY_PLAN|HABIT_PLAN|COMPREHENSIVE_PLAN",
  "timeZone": "账号时区",
  "goal": {"clientKey":"g1","title":"","description":"","completionCriteria":"","startDate":"YYYY-MM-DD","dueDate":null},
  "stages": [{"clientKey":"s1","goalKey":"g1","title":"","description":"","completionCriteria":"","startDate":null,"dueDate":null}],
  "tasks": [{"clientKey":"t1","stageKey":"s1","title":"","description":"","completionCriteria":"","priority":"MEDIUM","weight":1,"estimatedMinutes":30,"startDate":null,"dueDate":null}],
  "weeklyPlans": [{"clientKey":"w1","weekStart":"YYYY-MM-DD"}],
  "dailyPlans": [{"clientKey":"d1","weekKey":"w1","businessDate":"YYYY-MM-DD"}],
  "taskAssignments": [{"clientKey":"a1","taskKey":"t1","weekKey":"w1","businessDate":"YYYY-MM-DD","sortOrder":1}],
  "habits": [{"clientKey":"h1","name":"","description":"","completionCriteria":"","categoryName":"","cycleType":"DAILY|WEEKLY_DAYS","weekdays":[1,2,3,4,5,6,7],"startDate":"","expectedTime":"HH:mm"}]
}
规则：
1. 只按当前意图填写对应部分；例如 HABIT_PLAN 只填 habits，WEEKLY_PLAN 只填 weeklyPlans/dailyPlans/taskAssignments。
2. clientKey 在方案内唯一，只用于本方案内部引用；不要编造已有业务实体 ID。
3. 日期使用 YYYY-MM-DD，时间使用 HH:mm；开始日期不能晚于截止日期。
4. 未提供的信息可给出合理默认，但不得虚构用户的既有目标、任务或习惯。
5. 不包含删除、归档、开始计时、打卡等动作；方案只描述将要创建或安排的内容。
6. 任务必须属于某个阶段。STAGE_PLAN 用真实 goalId/goalVersion 引用已有目标，不创建同名目标；TASK_PLAN 用真实 stageId/stageVersion 引用已有阶段。
7. 周/日计划用 taskId/taskVersion 引用查询到的已有任务，不填写 taskKey，不复制任务；新任务安排才用 taskKey。已有任务 ID 和版本只能来自业务事实。
8. 已有有效安排改期（包括跨周、从一天移到另一天）必须携带真实 assignmentId/assignmentVersion，说明 reason；
   仅移动已有任务，不复制任务。没有安排时不填写安排ID和版本；已有安排不可猜测或省略版本。
9. 缺省日期写 null，不写空字符串。日期所属自然周、引用关系、星期值必须正确；信息不足不能虚构 ID。
10. 输出保持精炼：description、completionCriteria 各不超过 80 字，标题不超过 30 字；不写解释性文字。
11. 每个任务最多安排一次，不得重复创建同名任务或为同一任务生成多条安排；日计划只为用户明确要求或本周必要的日期创建，最多 7 天。"""


_INTENT_RULES: dict[str, str] = {
    "GOAL_PLAN": "本次规划目标、阶段与任务：goal 必填，stages/tasks 按用户要求填写；不要填写计划、安排或习惯。",
    "STAGE_PLAN": "本次在已有目标下规划阶段：goal 必须为 null，阶段用真实 goalId/goalVersion 引用已有目标；可包含阶段内新任务。",
    "TASK_PLAN": "本次在已有阶段下拆解任务：goal 为 null，stages 为空；任务用真实 stageId/stageVersion 引用已有阶段。",
    "WEEKLY_PLAN": "本次只规划周计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务（用户若要求创建任务，先提示改走任务规划）。",
    "DAILY_PLAN": "本次只规划日计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务。",
    "HABIT_PLAN": "本次只规划习惯：habits 必填；不要填写目标、阶段、任务或计划。",
    "COMPREHENSIVE_PLAN": "本次做综合规划：可同时包含目标、阶段、任务、周/日计划、任务安排与习惯，引用关系必须完整。",
}


def _messages(
    intent: str,
    message: str,
    *,
    today: str | None,
    time_zone: str | None,
    previous: dict[str, Any] | None,
    business_context: str | None = None,
) -> list[dict[str, str]]:
    facts = []
    if today:
        facts.append(f"账号今天：{today}")
    if time_zone:
        facts.append(f"账号时区：{time_zone}")
    user_parts = [f"规划意图：{intent}"]
    user_parts.append(_INTENT_RULES.get(intent, ""))
    user_parts.extend(facts)
    if business_context:
        user_parts.append(
            "当前业务事实（只读；用于避免重复与冲突；不要编造其中没有的 ID 或名称）："
        )
        user_parts.append(business_context)
    if previous is not None:
        user_parts.append("上一版方案（需在其基础上修改，不要丢失已确认内容）：")
        user_parts.append(json.dumps(previous, ensure_ascii=False))
    user_parts.append("用户需求：")
    user_parts.append(message.strip())
    messages = [
        {"role": "system", "content": _SYSTEM_PROMPT},
        {"role": "user", "content": "\n".join(part for part in user_parts if part)},
    ]
    if sum(len(item["content"]) for item in messages) > get_settings().agent_context_max_chars:
        raise AgentError(409, "CONTEXT_BUDGET_EXCEEDED", "规划上下文超出预算，请缩小规划范围")
    return messages


_SLIM_HINT = (
    "\n请大幅精简方案：最多 2 个阶段、6 个任务；description 与 completionCriteria 各不超过 30 字，"
    "标题不超过 20 字；不输出解释、注释或多余空白；只保留结构中已列出的必要字段；"
    "必须输出完整闭合的 json 对象。"
)
_INVALID_HINT = (
    "\n上一次输出未通过结构校验（{reason}）。请严格按固定结构重新输出一个合法 json 对象："
    "只使用结构中列出的字段，不添加额外字段；intent 必须是 {intent}；"
    "遵守当前意图的字段范围；新建对象用方案内的 Key 引用，已有对象用真实 ID 与版本引用；"
    "同一引用不能同时填写 Key 和 ID；日期使用 YYYY-MM-DD 或 null。"
)


def _short_reason(error: Exception) -> str:
    """把校验错误压缩成可回传模型的一句话，不包含用户原文。"""
    text = " ".join(str(error).split())
    if len(text) > 240:
        text = text[:240]
    return text


async def _generate_once(messages: list[dict[str, str]], max_tokens: int):
    from app.llm.client import get_llm_client

    settings = get_settings()
    return await get_llm_client().chat.completions.create(
        model=settings.llm_model,
        messages=messages,
        temperature=0.2,
        max_tokens=max_tokens,
        response_format={"type": "json_object"},
    )


async def generate_snapshot(
    *,
    intent: str,
    message: str,
    today: str | None,
    time_zone: str | None,
    previous: dict[str, Any] | None = None,
    business_context: str | None = None,
    on_attempt: Callable[[int], Awaitable[None]] | None = None,
) -> dict[str, Any]:
    settings = get_settings()
    base_messages = _messages(
        intent,
        message,
        today=today,
        time_zone=time_zone,
        previous=previous,
        business_context=business_context,
    )
    last_kind = "invalid"
    hint = ""
    for attempt in range(1, 4):
        if on_attempt is not None:
            await on_attempt(attempt)
        messages = list(base_messages)
        if attempt > 1 and hint:
            messages.append({"role": "user", "content": hint})
        try:
            response = await _generate_once(messages, settings.agent_planning_max_output_tokens)
        except Exception as error:
            logger.warning("agent_plan_generation_failed attempt=%s type=%s", attempt,
                type(error).__name__)
            if attempt >= 3:
                raise AgentError(502, "PLAN_GENERATION_FAILED",
                    "方案生成服务暂时不可用，请稍后重试") from error
            hint = _SLIM_HINT
            continue
        choice = response.choices[0] if response.choices else None
        finish_reason = getattr(choice, "finish_reason", None) if choice is not None else None
        usage = getattr(response, "usage", None)
        logger.warning("agent_plan_output attempt=%s reason=%s completion_tokens=%s",
            attempt, finish_reason, getattr(usage, "completion_tokens", None))
        content = getattr(getattr(choice, "message", None), "content", None)
        if finish_reason != "stop" or not isinstance(content, str) or not content.strip():
            # 输出超预算或被截断：追加精简约束重试，避免可修复的长方案直接失败。
            last_kind = "too_long"
            hint = _SLIM_HINT
            continue
        try:
            snapshot = json.loads(content)
        except json.JSONDecodeError:
            last_kind = "invalid"
            hint = _INVALID_HINT.format(reason="输出不是合法 json", intent=intent)
            continue
        if not isinstance(snapshot, dict):
            last_kind = "invalid"
            hint = _INVALID_HINT.format(reason="输出不是 json 对象", intent=intent)
            continue
        snapshot["intent"] = intent
        if time_zone and not snapshot.get("timeZone"):
            snapshot["timeZone"] = time_zone
        try:
            parsed = workflow_service.PlanSnapshot.model_validate(snapshot)
        except ValueError as error:
            last_kind = "invalid"
            reason = _short_reason(error)
            logger.warning("agent_plan_snapshot_invalid attempt=%s reason=%s", attempt, reason)
            hint = _INVALID_HINT.format(reason=reason, intent=intent)
            continue
        return parsed.model_dump(mode="json")
    if last_kind == "too_long":
        raise AgentError(422, "PLAN_GENERATION_TOO_LONG",
            "方案内容超出输出上限，请缩小规划范围或分阶段生成")
    raise AgentError(422, "PLAN_GENERATION_INVALID",
        "方案内容未能通过结构校验，请把目标拆成更明确的阶段/任务后重试")
