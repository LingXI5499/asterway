# 压缩已经完整结束的历史对话，保留用户约束与尚未完成事项。
# 历史摘要：只在终态轮次之后压缩，失败时保留旧摘要和原文。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 logging，供下面调用其功能。
import logging

# 从 app.core.config 引入 读取运行配置（get_settings）。
from app.core.config import get_settings

# 从 app.db.session 引入 数据库（Database）。
from app.db.session import Database

# 从 app.llm.chat_service 引入 模型选项（model_options）。
from app.llm.chat_service import model_options

# 从 app.llm.client 引入 取得复用的模型客户端（get_llm_client）。
from app.llm.client import get_llm_client

# 从 app.llm.prompts.conversation 引入 汇总系统模型提示词（SUMMARY_SYSTEM_PROMPT）、汇总模型提示词（summary_prompt）。
from app.llm.prompts.conversation import SUMMARY_SYSTEM_PROMPT, summary_prompt

# 从 app.repositories 引入 对话数据仓库（conversation_repository）、消息数据仓库（message_repository）。
from app.repositories import conversation_repository, message_repository

# 从 app.services.context_service 引入 消息列表for本轮对话（messages_for_turn）。
from app.services.context_service import messages_for_turn

# 把执行“读取 日志记录器（Logger） 的值”得到的结果保存到 日志记录器（logger）。
logger = logging.getLogger(__name__)


# 函数 _transcript：只覆盖完整装入输入的轮次，不把因预算未送出的消息标为已摘要。；接收 对话轮次（turns）、额度（budget）。
def _transcript(turns: list[message_repository.HistoryTurn], budget: int) -> tuple[str, int | None]:
    # 只覆盖完整装入输入的轮次，不把因预算未送出的消息标为已摘要。
    # 声明 lines，类型为 list[str]；其内容为空列表或集合。
    lines: list[str] = []
    # 把固定数值 0保存到 used。
    used = 0
    # 把空值，表示当前还没有对应内容保存到 boundary。
    boundary = None
    # 从对话轮次（turns）依次取出本轮对话（turn），每项执行一次下面的处理。
    for turn in turns:
        # 把执行“把对象编码为 JSON 文字”得到的结果保存到 line。
        line = json.dumps(messages_for_turn(turn), ensure_ascii=False, separators=(",", ":"))
        # 把按 len(line) + (2 if lines else 0) 计算的结果保存到 每页条数（size）。
        size = len(line) + (2 if lines else 0)
        # 检查条件 used + size > budget 是否成立；成立时执行缩进的分支。
        if used + size > budget:
            # 立即退出当前循环，不再处理后续条目。
            break
        # 把新条目放到列表末尾。
        lines.append(line)
        # 把 used 加上 每页条数（size），再保存回原变量。
        used += size
        # 把本轮对话（turn）的最后一次消息no（last_message_no）属性保存到 boundary。
        boundary = turn.last_message_no
    # 把依次包含 执行“用指定分隔符把多段文字合起来”得到的结果、boundary 的集合交还给调用方，本函数到此结束。
    return "\n\n".join(lines), boundary


# 异步函数 run_summary_for_conversation：调用 run_summary_for_conversation 完成方案运行汇总for对话处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、slots。
async def run_summary_for_conversation(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None = None 的结果。
    db: Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None = None
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 等待调用 _summarize 完成summarize处理完成后再继续。
        await _summarize(db, user_id, conversation_id, slots)
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:  # noqa: BLE001 - 摘要是派生数据，失败不能影响已完成的回复。
        # 记录warning级别的运行信息，供定位处理过程和失败原因。
        logger.warning(
            # 继续传入或组合固定文字 'agent_summary_failed conversation=%s type=%s'，完成上面尚未结束的表达式。
            "agent_summary_failed conversation=%s type=%s",
            # 继续传入或组合对话编号（conversation_id），完成上面尚未结束的表达式。
            conversation_id,
            # 继续传入或组合调用 type 完成类型处理，完成上面尚未结束的表达式。
            type(error).__name__,
        )


# 异步函数 _summarize：调用 _summarize 完成summarize处理；接收 Agent 数据库访问器（db）、用户编号（user_id）、对话编号（conversation_id）、slots。
async def _summarize(
    # 填写Agent 数据库访问器（db）字段，值为表达式 Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None 的结果。
    db: Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None
# 继续传入或组合表达式 ) -> None: 的结果，完成上面尚未结束的表达式。
) -> None:
    # 把执行“读取并复用当前进程的运行配置”得到的结果保存到 配置（settings）。
    settings = get_settings()
    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“查找 owned”得到的结果完成后的结果保存到 对话（conversation）。
        conversation = await conversation_repository.find_owned(session, user_id, conversation_id)
        # 检查conversation is None or conversation.status != 'ACTIVE'，即对应内容是否不存在；成立时执行缩进的分支。
        if conversation is None or conversation.status != "ACTIVE":
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把等待执行“调用 message_repository.history_turns 完成历史对话轮次处理”得到的结果完成后的结果保存到 对话轮次（turns）。
        turns = await message_repository.history_turns(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
            conversation_id=conversation_id,
            # 传入变化后消息no（after_message_no），值为表达式 conversation.summary_through_message_no 的结果。
            after_message_no=conversation.summary_through_message_no,
            # 传入变化前消息no（before_message_no），值为空值，表示当前还没有对应内容。
            before_message_no=None,
            # 传入上限（limit），值为固定数值 128。
            limit=128,
            # 传入newest，值为假，表示条件不成立或开关关闭。
            newest=False,
        )
        # 检查条件 sum((len(turn.messages) for turn in turns)) <= settings.agent_summary_trigger_messages 是否成立；成立时执行缩进的分支。
        if sum(len(turn.messages) for turn in turns) <= settings.agent_summary_trigger_messages:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把固定数值 0保存到 keep。
        keep = 0
        # 把执行“调用 len 完成len处理”得到的结果保存到 split。
        split = len(turns)
        # 只要条件 split > 0 and keep < settings.agent_summary_keep_messages 是否成立，就继续执行下面的循环。
        while split > 0 and keep < settings.agent_summary_keep_messages:
            # 把 split 减去 固定数值 1，再保存回原变量。
            split -= 1
            # 把 keep 加上 执行“调用 len 完成len处理”得到的结果，再保存回原变量。
            keep += len(turns[split].messages)
        # 把从对话轮次（turns）中按表达式 :split 的结果取出的内容保存到 candidates。
        candidates = turns[:split]
        # 检查条件 not candidates 是否成立；成立时执行缩进的分支。
        if not candidates:
            # 把空值交还给调用方，本函数到此结束。
            return
        # 把对话（conversation）的上下文汇总（context_summary）属性保存到 上一次汇总（previous_summary）。
        previous_summary = conversation.context_summary
        # 把对话（conversation）的版本号（version）属性保存到 预计版本号（expected_version）。
        expected_version = conversation.version
        # 把对话（conversation）的汇总through消息no（summary_through_message_no）属性保存到 预计boundary（expected_boundary）。
        expected_boundary = conversation.summary_through_message_no
        # 把按 settings.agent_context_max_chars - len(previous_summary) - len(SUMMARY_SYSTEM_PROMPT) - 256 计算的结果保存到 额度（budget）。
        budget = (
            # 继续传入或组合表达式 settings.agent_context_max_chars 的结果，完成上面尚未结束的表达式。
            settings.agent_context_max_chars
            # 继续传入或组合表达式 - len(previous_summary) 的结果，完成上面尚未结束的表达式。
            - len(previous_summary)
            # 继续传入或组合表达式 - len(SUMMARY_SYSTEM_PROMPT) 的结果，完成上面尚未结束的表达式。
            - len(SUMMARY_SYSTEM_PROMPT)
            # 继续传入或组合表达式 - 256 的结果，完成上面尚未结束的表达式。
            - 256
        )
        # 把执行“调用 _transcript 完成transcript处理”得到的结果保存到 transcriptboundary（(transcript, boundary)）。
        transcript, boundary = _transcript(candidates, budget)
        # 检查not transcript.strip() or boundary is None，即对应内容是否不存在；成立时执行缩进的分支。
        if not transcript.strip() or boundary is None:
            # 把空值交还给调用方，本函数到此结束。
            return

    # 异步进入执行“调用 asyncio.timeout 完成超时处理”得到的结果、slots或者执行“调用 asyncio.Semaphore 完成并发名额控制器处理”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
    async with (
        # 继续传入或组合调用 asyncio.timeout 完成超时处理，完成上面尚未结束的表达式。
        asyncio.timeout(settings.agent_request_timeout_seconds),
        # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：slots or asyncio.Semaphore(1)，完成上面尚未结束的表达式。
        slots or asyncio.Semaphore(1),
    ):
        # 把等待执行“调用 get_llm_client().chat.completions.create 完成创建处理”得到的结果完成后的结果保存到 完成情况（completion）。
        completion = await get_llm_client().chat.completions.create(
            # 继续传入或组合按表达式计算得到的值：**model_options(，完成上面尚未结束的表达式。
            **model_options(
                [
                    # 继续传入或组合表达式 {"role": "system", "content": SUMMARY_SYSTEM_PROMPT} 的结果，完成上面尚未结束的表达式。
                    {"role": "system", "content": SUMMARY_SYSTEM_PROMPT},
                    {
                        # 填写角色（role）字段，值为固定文字 'user'。
                        "role": "user",
                        # 填写内容（content）字段，值为调用 summary_prompt 完成汇总模型提示词处理。
                        "content": summary_prompt(previous_summary, transcript),
                    },
                ]
            )
        )
    # 检查条件 not completion.choices or completion.choices[0].finish_reason != 'stop' or (not completion.choices[0].message.content) 是否成立；成立时执行缩进的分支。
    if (
        # 继续传入或组合表达式 not completion.choices 的结果，完成上面尚未结束的表达式。
        not completion.choices
        # 继续补充本次调用或判断：条件判断的结果：or completion.choices[0].finish_reason != "stop"。
        or completion.choices[0].finish_reason != "stop"
        # 继续补充本次调用或判断：条件判断的结果：or not completion.choices[0].message.content。
        or not completion.choices[0].message.content
    ):
        # 把空值交还给调用方，本函数到此结束。
        return
    # 把执行“去掉文字两端的空白”得到的结果保存到 汇总（summary）。
    summary = completion.choices[0].message.content.strip()
    # 把执行“调用 min 完成最小处理”得到的结果保存到 上限（limit）。
    limit = min(settings.agent_summary_max_chars, 16_000)
    # 检查条件 not summary or len(summary) > limit 是否成立；成立时执行缩进的分支。
    if not summary or len(summary) > limit:
        # 把空值交还给调用方，本函数到此结束。
        return

    # 异步进入执行“调用 db.session 完成当前会话处理”得到的结果，命名为 session；退出时由上下文管理器提交、回滚或释放资源。
    async with db.session() as session:
        # 把等待执行“更新 汇总（summary）”得到的结果完成后的结果保存到 更新时（updated）。
        updated = await conversation_repository.update_summary(
            # 继续传入或组合当前会话（session），完成上面尚未结束的表达式。
            session,
            # 传入对话编号（conversation_id），值为对话编号（conversation_id）。
            conversation_id=conversation_id,
            # 传入用户编号（user_id），值为用户编号（user_id）。
            user_id=user_id,
            # 传入预计版本号（expected_version），值为预计版本号（expected_version）。
            expected_version=expected_version,
            # 传入预计boundary（expected_boundary），值为预计boundary（expected_boundary）。
            expected_boundary=expected_boundary,
            # 传入汇总（summary），值为汇总（summary）。
            summary=summary,
            # 传入boundary，值为boundary。
            boundary=boundary,
        )
        # 检查条件 not updated 是否成立；成立时执行缩进的分支。
        if not updated:
            # 记录info级别的运行信息，供定位处理过程和失败原因。
            logger.info("agent_summary_skipped conversation=%s reason=stale", conversation_id)
