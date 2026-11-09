# 让模型生成固定结构的方案，校验内容并在截断或格式错误时有限重试。
# 规划意图识别与方案生成：识别用户意图，调用模型生成结构化方案快照。

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 从 collections.abc 引入 Awaitable、Callable。
from collections.abc import Awaitable, Callable

# 从 typing 引入 Any。
from typing import Any

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）。
from app.core.errors import AgentError

# 从 app.services 引入 规划工作流业务服务（workflow_service）。
from app.services import workflow_service

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 声明 INTENTKEYWORDS（_INTENT_KEYWORDS），类型为 dict[str, tuple[str, ...]]；其内容为按字段名保存多个值的字典。
_INTENT_KEYWORDS: dict[str, tuple[str, ...]] = {
    # 填写习惯方案（HABIT_PLAN）字段，值为表达式 ("习惯", "打卡", "每天", "每周三次", "坚持") 的结果。
    "HABIT_PLAN": ("习惯", "打卡", "每天", "每周三次", "坚持"),
    # 填写每周方案（WEEKLY_PLAN）字段，值为表达式 ("周计划", "这周", "本周", "下周", "一周安排", "周安排") 的结果。
    "WEEKLY_PLAN": ("周计划", "这周", "本周", "下周", "一周安排", "周安排"),
    # 填写每日方案（DAILY_PLAN）字段，值为表达式 ("日计划", "今天安排", "明天安排", "今日安排", "每天安排") 的结果。
    "DAILY_PLAN": ("日计划", "今天安排", "明天安排", "今日安排", "每天安排"),
    # 填写目标方案（GOAL_PLAN）字段，值为表达式 ("目标", "长期计划", "成长规划", "季度目标", "年度目标") 的结果。
    "GOAL_PLAN": ("目标", "长期计划", "成长规划", "季度目标", "年度目标"),
    # 填写阶段方案（STAGE_PLAN）字段，值为表达式 ("阶段", "阶段性", "里程碑") 的结果。
    "STAGE_PLAN": ("阶段", "阶段性", "里程碑"),
    # 填写任务方案（TASK_PLAN）字段，值为表达式 ("任务", "拆解", "待办") 的结果。
    "TASK_PLAN": ("任务", "拆解", "待办"),
}
# 把依次包含 固定文字 '综合'、固定文字 '全面'、固定文字 '整体规划'、固定文字 '成长计划'、固定文字 '帮我规划' 的集合保存到 综合（_COMPREHENSIVE）。
_COMPREHENSIVE = ("综合", "全面", "整体规划", "成长计划", "帮我规划")
# 把依次包含 固定文字 '目标'、固定文字 '阶段'、固定文字 '任务'、固定文字 '计划'、固定文字 '安排' 的集合保存到 成长业务NOUNS（_GROWTH_NOUNS）。
_GROWTH_NOUNS = ("目标", "阶段", "任务", "计划", "安排")


# 函数 detect_intent：调用 detect_intent 完成detectintent处理；接收 消息（message）。
def detect_intent(message: str) -> str | None:
    # 把执行“去掉文字两端的空白”得到的结果保存到 文字（text）。
    text = message.strip()
    # 检查条件 not text 是否成立；成立时执行缩进的分支。
    if not text:
        # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return None
    # 检查条件 any((word in text for word in _COMPREHENSIVE)) 是否成立；成立时执行缩进的分支。
    if any(word in text for word in _COMPREHENSIVE):
        # 把固定状态或编码 COMPREHENSIVE_PLAN（综合方案）交还给调用方，本函数到此结束。
        return "COMPREHENSIVE_PLAN"
    # 把执行“调用 any 完成any处理”得到的结果保存到 是否存在成长业务noun（has_growth_noun）。
    has_growth_noun = any(word in text for word in _GROWTH_NOUNS)
    # 只有“习惯/每天/坚持”类表达且没有目标、阶段、任务、计划等对象时，才判为习惯规划；
    # 带“目标/阶段/任务/计划”的需求优先按成长对象规划，避免“每天投入两小时达目标”被误判成习惯。
    # 检查条件 '习惯' in text and (not has_growth_noun) 是否成立；成立时执行缩进的分支。
    if "习惯" in text and not has_growth_noun:
        # 把固定状态或编码 HABIT_PLAN（习惯方案）交还给调用方，本函数到此结束。
        return "HABIT_PLAN"
    # 声明 scores，类型为 list[tuple[str, int]]；其内容为空列表或集合。
    scores: list[tuple[str, int]] = []
    # 从执行“调用 _INTENT_KEYWORDS.items 完成数据列表处理”得到的结果依次取出intentwords（(intent, words)），每项执行一次下面的处理。
    for intent, words in _INTENT_KEYWORDS.items():
        # 检查条件 has_growth_noun and intent == 'HABIT_PLAN' 是否成立；成立时执行缩进的分支。
        if has_growth_noun and intent == "HABIT_PLAN":
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把执行“调用 sum 完成sum处理”得到的结果保存到 score。
        score = sum(text.count(word) for word in words)
        # 检查条件 score 是否成立；成立时执行缩进的分支。
        if score:
            # 把新条目放到列表末尾。
            scores.append((intent, score))
    # 检查条件 not scores 是否成立；成立时执行缩进的分支。
    if not scores:
        # 把条件 '习惯' in text 是否成立时使用固定状态或编码 HABIT_PLAN（习惯方案），否则使用空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
        return "HABIT_PLAN" if "习惯" in text else None
    # 按指定规则排列数据。
    scores.sort(key=lambda item: item[1], reverse=True)
    # 检查条件 len(scores) >= 3 是否成立；成立时执行缩进的分支。
    if len(scores) >= 3:
        # 把固定状态或编码 COMPREHENSIVE_PLAN（综合方案）交还给调用方，本函数到此结束。
        return "COMPREHENSIVE_PLAN"
    # 把从从scores中按固定数值 0取出的内容中按固定数值 0取出的内容交还给调用方，本函数到此结束。
    return scores[0][0]


# 把表达式 '你是 Asterway 成长规划器。把用户的规划需求整理成严格 JSON 方案快照，只输出一个合法 json 对象，不输出解释或代码块围栏。\n方案快照固定结构（未使用的部分输出空数组或 null）：\n{\n "intent": "GO… 的结果保存到 系统模型提示词（_SYSTEM_PROMPT）。
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


# 声明 INTENTRULES（_INTENT_RULES），类型为 dict[str, str]；其内容为按字段名保存多个值的字典。
_INTENT_RULES: dict[str, str] = {
    # 填写目标方案（GOAL_PLAN）字段，值为固定文字 '本次规划目标、阶段与任务：goal 必填，stages/tasks 按用户要求填写；不要填写计划、安排或习惯。'。
    "GOAL_PLAN": "本次规划目标、阶段与任务：goal 必填，stages/tasks 按用户要求填写；不要填写计划、安排或习惯。",
    # 填写阶段方案（STAGE_PLAN）字段，值为固定文字 '本次在已有目标下规划阶段：goal 必须为 null，阶段用真实 goalId/goalVersion 引用已有目标；可包含阶段内新任务。'。
    "STAGE_PLAN": "本次在已有目标下规划阶段：goal 必须为 null，阶段用真实 goalId/goalVersion 引用已有目标；可包含阶段内新任务。",
    # 填写任务方案（TASK_PLAN）字段，值为固定文字 '本次在已有阶段下拆解任务：goal 为 null，stages 为空；任务用真实 stageId/stageVersion 引用已有阶段。'。
    "TASK_PLAN": "本次在已有阶段下拆解任务：goal 为 null，stages 为空；任务用真实 stageId/stageVersion 引用已有阶段。",
    # 填写每周方案（WEEKLY_PLAN）字段，值为固定文字 '本次只规划周计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务（用户若要求创建任务，先…'。
    "WEEKLY_PLAN": "本次只规划周计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务（用户若要求创建任务，先提示改走任务规划）。",
    # 填写每日方案（DAILY_PLAN）字段，值为固定文字 '本次只规划日计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务。'。
    "DAILY_PLAN": "本次只规划日计划与任务安排：weeklyPlans、dailyPlans、taskAssignments 必填；不创建新目标/阶段/任务。",
    # 填写习惯方案（HABIT_PLAN）字段，值为固定文字 '本次只规划习惯：habits 必填；不要填写目标、阶段、任务或计划。'。
    "HABIT_PLAN": "本次只规划习惯：habits 必填；不要填写目标、阶段、任务或计划。",
    # 填写综合方案（COMPREHENSIVE_PLAN）字段，值为固定文字 '本次做综合规划：可同时包含目标、阶段、任务、周/日计划、任务安排与习惯，引用关系必须完整。'。
    "COMPREHENSIVE_PLAN": "本次做综合规划：可同时包含目标、阶段、任务、周/日计划、任务安排与习惯，引用关系必须完整。",
}


# 函数 _messages：调用 _messages 完成消息列表处理；接收 intent、消息（message）、今天（today）、用户使用的时区（time_zone）、上一次（previous）、business上下文（business_context）。
def _messages(
    # 填写intent字段，值为str。
    intent: str,
    # 填写消息（message）字段，值为str。
    message: str,
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写今天（today）字段，值为表达式 str | None 的结果。
    today: str | None,
    # 填写用户使用的时区（time_zone）字段，值为表达式 str | None 的结果。
    time_zone: str | None,
    # 填写上一次（previous）字段，值为表达式 dict[str, Any] | None 的结果。
    previous: dict[str, Any] | None,
    # 填写business上下文（business_context）字段，值为表达式 str | None = None 的结果。
    business_context: str | None = None,
# 继续传入或组合表达式 ) -> list[dict[str, str]]: 的结果，完成上面尚未结束的表达式。
) -> list[dict[str, str]]:
    # 把空列表或集合保存到 facts。
    facts = []
    # 检查条件 today 是否成立；成立时执行缩进的分支。
    if today:
        # 把新条目放到列表末尾。
        facts.append(f"账号今天：{today}")
    # 检查条件 time_zone 是否成立；成立时执行缩进的分支。
    if time_zone:
        # 把新条目放到列表末尾。
        facts.append(f"账号时区：{time_zone}")
    # 把依次包含 在文字占位处填入变量后组成的字符串 的集合保存到 用户parts（user_parts）。
    user_parts = [f"规划意图：{intent}"]
    # 把新条目放到列表末尾。
    user_parts.append(_INTENT_RULES.get(intent, ""))
    # 把另一组条目依次加入列表。
    user_parts.extend(facts)
    # 检查条件 business_context 是否成立；成立时执行缩进的分支。
    if business_context:
        # 把新条目放到列表末尾。
        user_parts.append(
            # 继续传入或组合固定文字 '当前业务事实（只读；用于避免重复与冲突；不要编造其中没有的 ID 或名称）：'，完成上面尚未结束的表达式。
            "当前业务事实（只读；用于避免重复与冲突；不要编造其中没有的 ID 或名称）："
        )
        # 把新条目放到列表末尾。
        user_parts.append(business_context)
    # 检查条件 previous is not None 是否成立；成立时执行缩进的分支。
    if previous is not None:
        # 把新条目放到列表末尾。
        user_parts.append("上一版方案（需在其基础上修改，不要丢失已确认内容）：")
        # 把新条目放到列表末尾。
        user_parts.append(json.dumps(previous, ensure_ascii=False))
    # 把新条目放到列表末尾。
    user_parts.append("用户需求：")
    # 把新条目放到列表末尾。
    user_parts.append(message.strip())
    # 把依次包含 按字段名保存多个值的字典、按字段名保存多个值的字典 的集合保存到 消息列表（messages）。
    messages = [
        # 继续传入或组合表达式 {"role": "system", "content": _SYSTEM_PROMPT} 的结果，完成上面尚未结束的表达式。
        {"role": "system", "content": _SYSTEM_PROMPT},
        # 继续传入或组合用指定分隔符把多段文字合起来，完成上面尚未结束的表达式。
        {"role": "user", "content": "\n".join(part for part in user_parts if part)},
    ]
    # 检查条件 sum((len(item['content']) for item in messages)) > get_settings().agent_context_max_chars 是否成立；成立时执行缩进的分支。
    if sum(len(item["content"]) for item in messages) > get_settings().agent_context_max_chars:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(409, "CONTEXT_BUDGET_EXCEEDED", "规划上下文超出预算，请缩小规划范围")
    # 把消息列表（messages）交还给调用方，本函数到此结束。
    return messages


# 把固定文字 '\\n请大幅精简方案：最多 2 个阶段、6 个任务；description 与 completionCriteria 各不超过 30 字，标题不超过 20 字；不…'保存到 SLIMHINT（_SLIM_HINT）。
_SLIM_HINT = (
    # 继续传入或组合固定文字 '\\n请大幅精简方案：最多 2 个阶段、6 个任务；description 与 completionCriteria 各不超过 30 字，'，完成上面尚未结束的表达式。
    "\n请大幅精简方案：最多 2 个阶段、6 个任务；description 与 completionCriteria 各不超过 30 字，"
    "标题不超过 20 字；不输出解释、注释或多余空白；只保留结构中已列出的必要字段；"
    "必须输出完整闭合的 json 对象。"
)
# 把表达式 '\n上一次输出未通过结构校验（{reason}）。请严格按固定结构重新输出一个合法 json 对象：只使用结构中列出的字段，不添加额外字段；intent 必须是 {intent}；遵守当前意图的字段范围；新建对象用方案内的 Key 引用，… 的结果保存到 不合法HINT（_INVALID_HINT）。
_INVALID_HINT = (
    # 继续传入或组合固定文字 '\\n上一次输出未通过结构校验（{reason}）。请严格按固定结构重新输出一个合法 json 对象：'，完成上面尚未结束的表达式。
    "\n上一次输出未通过结构校验（{reason}）。请严格按固定结构重新输出一个合法 json 对象："
    "只使用结构中列出的字段，不添加额外字段；intent 必须是 {intent}；"
    "遵守当前意图的字段范围；新建对象用方案内的 Key 引用，已有对象用真实 ID 与版本引用；"
    "同一引用不能同时填写 Key 和 ID；日期使用 YYYY-MM-DD 或 null。"
)


# 函数 _short_reason：把校验错误压缩成可回传模型的一句话，不包含用户原文。；接收 错误（error）。
def _short_reason(error: Exception) -> str:
    # 把校验错误压缩成可回传模型的一句话，不包含用户原文。
    # 把执行“用指定分隔符把多段文字合起来”得到的结果保存到 文字（text）。
    text = " ".join(str(error).split())
    # 检查条件 len(text) > 240 是否成立；成立时执行缩进的分支。
    if len(text) > 240:
        # 把从文字（text）中按表达式 :240 的结果取出的内容保存到 文字（text）。
        text = text[:240]
    # 把文字（text）交还给调用方，本函数到此结束。
    return text


# 异步函数 _generate_once：调用 _generate_once 完成generateonce处理；接收 消息列表（messages）、最大凭据处理器（max_tokens）。
async def _generate_once(messages: list[dict[str, str]], max_tokens: int):

    # 从 app.llm.client 引入 取得复用的模型客户端（get_llm_client）。
    from app.llm.client import get_llm_client

    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把等待执行“调用 get_llm_client().chat.completions.create 完成创建处理”得到的结果完成后的结果交还给调用方，本函数到此结束。
    return await get_llm_client().chat.completions.create(
        # 传入模型（model），值为表达式 settings.llm_model 的结果。
        model=settings.llm_model,
        # 传入消息列表（messages），值为消息列表（messages）。
        messages=messages,
        # 传入temperature，值为固定数值 0.2。
        temperature=0.2,
        # 传入最大凭据处理器（max_tokens），值为最大凭据处理器（max_tokens）。
        max_tokens=max_tokens,
        # 传入响应格式（response_format），值为表达式 {"type": "json_object"} 的结果。
        response_format={"type": "json_object"},
    )


# 异步函数 generate_snapshot：调用 generate_snapshot 完成generate确认时的数据快照处理；接收 intent、消息（message）、今天（today）、用户使用的时区（time_zone）、上一次（previous）、business上下文（business_context）、事件发生时尝试（on_attempt）。
async def generate_snapshot(
    # 继续传入或组合按表达式计算得到的值：*，完成上面尚未结束的表达式。
    *,
    # 填写intent字段，值为str。
    intent: str,
    # 填写消息（message）字段，值为str。
    message: str,
    # 填写今天（today）字段，值为表达式 str | None 的结果。
    today: str | None,
    # 填写用户使用的时区（time_zone）字段，值为表达式 str | None 的结果。
    time_zone: str | None,
    # 填写上一次（previous）字段，值为表达式 dict[str, Any] | None = None 的结果。
    previous: dict[str, Any] | None = None,
    # 填写business上下文（business_context）字段，值为表达式 str | None = None 的结果。
    business_context: str | None = None,
    # 填写事件发生时尝试（on_attempt）字段，值为表达式 Callable[[int], Awaitable[None]] | None = None 的结果。
    on_attempt: Callable[[int], Awaitable[None]] | None = None,
# 继续传入或组合表达式 ) -> dict[str, Any]: 的结果，完成上面尚未结束的表达式。
) -> dict[str, Any]:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 把执行“调用 _messages 完成消息列表处理”得到的结果保存到 基础消息列表（base_messages）。
    base_messages = _messages(
        # 继续传入或组合intent，完成上面尚未结束的表达式。
        intent,
        # 继续传入或组合消息（message），完成上面尚未结束的表达式。
        message,
        # 传入今天（today），值为今天（today）。
        today=today,
        # 传入用户使用的时区（time_zone），值为用户使用的时区（time_zone）。
        time_zone=time_zone,
        # 传入上一次（previous），值为上一次（previous）。
        previous=previous,
        # 传入business上下文（business_context），值为business上下文（business_context）。
        business_context=business_context,
    )
    # 把固定文字 'invalid'保存到 最后一次种类（last_kind）。
    last_kind = "invalid"
    # 把固定文字 ''保存到 hint。
    hint = ""
    # 从执行“调用 range 完成区间处理”得到的结果依次取出尝试（attempt），每项执行一次下面的处理。
    for attempt in range(1, 4):
        # 检查条件 on_attempt is not None 是否成立；成立时执行缩进的分支。
        if on_attempt is not None:
            # 等待调用 on_attempt 完成事件发生时尝试处理完成后再继续。
            await on_attempt(attempt)
        # 把执行“调用 list 完成列表处理”得到的结果保存到 消息列表（messages）。
        messages = list(base_messages)
        # 检查条件 attempt > 1 and hint 是否成立；成立时执行缩进的分支。
        if attempt > 1 and hint:
            # 把新条目放到列表末尾。
            messages.append({"role": "user", "content": hint})
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把等待执行“调用 _generate_once 完成generateonce处理”得到的结果完成后的结果保存到 响应（response）。
            response = await _generate_once(messages, settings.agent_planning_max_output_tokens)
        # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
        except Exception as error:
            # 记录warning级别的运行信息，供定位处理过程和失败原因。
            logger.warning("agent_plan_generation_failed attempt=%s type=%s", attempt,
                # 继续传入或组合调用 type 完成类型处理，完成上面尚未结束的表达式。
                type(error).__name__)
            # 检查条件 attempt >= 3 是否成立；成立时执行缩进的分支。
            if attempt >= 3:
                # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
                raise AgentError(502, "PLAN_GENERATION_FAILED",
                    # 继续传入或组合表达式 "方案生成服务暂时不可用，请稍后重试") from error 的结果，完成上面尚未结束的表达式。
                    "方案生成服务暂时不可用，请稍后重试") from error
            # 把SLIMHINT（_SLIM_HINT）保存到 hint。
            hint = _SLIM_HINT
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把条件 response.choices 是否成立时使用从响应（response）的choices属性中按固定数值 0取出的内容，否则使用空值，表示当前还没有对应内容保存到 choice。
        choice = response.choices[0] if response.choices else None
        # 把条件 choice is not None 是否成立时使用执行“读取 attr 的值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 finish原因（finish_reason）。
        finish_reason = getattr(choice, "finish_reason", None) if choice is not None else None
        # 把执行“读取 attr 的值”得到的结果保存到 用量（usage）。
        usage = getattr(response, "usage", None)
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning("agent_plan_output attempt=%s reason=%s completion_tokens=%s",
            # 继续传入或组合表达式 attempt, finish_reason, getattr(usage, "completion_tokens", None)) 的结果，完成上面尚未结束的表达式。
            attempt, finish_reason, getattr(usage, "completion_tokens", None))
        # 把执行“读取 attr 的值”得到的结果保存到 内容（content）。
        content = getattr(getattr(choice, "message", None), "content", None)
        # 检查条件 finish_reason != 'stop' or not isinstance(content, str) or (not content.strip()) 是否成立；成立时执行缩进的分支。
        if finish_reason != "stop" or not isinstance(content, str) or not content.strip():
            # 输出超预算或被截断：追加精简约束重试，避免可修复的长方案直接失败。
            # 把固定文字 'too_long'保存到 最后一次种类（last_kind）。
            last_kind = "too_long"
            # 把SLIMHINT（_SLIM_HINT）保存到 hint。
            hint = _SLIM_HINT
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把执行“把 JSON 文字解析为对象”得到的结果保存到 确认时的数据快照（snapshot）。
            snapshot = json.loads(content)
        # 捕获json.JSONDecodeError异常，执行下面的错误处理。
        except json.JSONDecodeError:
            # 把固定文字 'invalid'保存到 最后一次种类（last_kind）。
            last_kind = "invalid"
            # 把执行“调用 _INVALID_HINT.format 完成格式处理”得到的结果保存到 hint。
            hint = _INVALID_HINT.format(reason="输出不是合法 json", intent=intent)
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 检查条件 not isinstance(snapshot, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(snapshot, dict):
            # 把固定文字 'invalid'保存到 最后一次种类（last_kind）。
            last_kind = "invalid"
            # 把执行“调用 _INVALID_HINT.format 完成格式处理”得到的结果保存到 hint。
            hint = _INVALID_HINT.format(reason="输出不是 json 对象", intent=intent)
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把intent保存到 确认时的数据快照intent（snapshot['intent']）。
        snapshot["intent"] = intent
        # 检查条件 time_zone and (not snapshot.get('timeZone')) 是否成立；成立时执行缩进的分支。
        if time_zone and not snapshot.get("timeZone"):
            # 把用户使用的时区（time_zone）保存到 确认时的数据快照时间时区（snapshot['timeZone']）。
            snapshot["timeZone"] = time_zone
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 把执行“调用 workflow_service.PlanSnapshot.model_validate 完成模型检查合法性处理”得到的结果保存到 parsed。
            parsed = workflow_service.PlanSnapshot.model_validate(snapshot)
        # 捕获ValueError异常，执行下面的错误处理，异常对象保存为 error。
        except ValueError as error:
            # 把固定文字 'invalid'保存到 最后一次种类（last_kind）。
            last_kind = "invalid"
            # 把执行“调用 _short_reason 完成short原因处理”得到的结果保存到 原因（reason）。
            reason = _short_reason(error)
            # 记录warning级别的运行信息，供定位处理过程和失败原因。
            logger.warning("agent_plan_snapshot_invalid attempt=%s reason=%s", attempt, reason)
            # 把执行“调用 _INVALID_HINT.format 完成格式处理”得到的结果保存到 hint。
            hint = _INVALID_HINT.format(reason=reason, intent=intent)
            # 跳过当前条目，本轮剩余语句不执行，继续下一轮循环。
            continue
        # 把执行“调用 parsed.model_dump 完成模型dump处理”得到的结果交还给调用方，本函数到此结束。
        return parsed.model_dump(mode="json")
    # 检查条件 last_kind == 'too_long' 是否成立；成立时执行缩进的分支。
    if last_kind == "too_long":
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(422, "PLAN_GENERATION_TOO_LONG",
            # 继续传入或组合表达式 "方案内容超出输出上限，请缩小规划范围或分阶段生成") 的结果，完成上面尚未结束的表达式。
            "方案内容超出输出上限，请缩小规划范围或分阶段生成")
    # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
    raise AgentError(422, "PLAN_GENERATION_INVALID",
        # 继续传入或组合按表达式计算得到的值："方案内容未能通过结构校验，请把目标拆成更明确的阶段/任务后重试")，完成上面尚未结束的表达式。
        "方案内容未能通过结构校验，请把目标拆成更明确的阶段/任务后重试")
