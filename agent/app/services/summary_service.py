"""历史摘要：只在终态轮次之后压缩，失败时保留旧摘要和原文。"""

import asyncio
import json
import logging

from app.core.config import get_settings
from app.db.session import Database
from app.llm.chat_service import model_options
from app.llm.client import get_llm_client
from app.llm.prompts.conversation import SUMMARY_SYSTEM_PROMPT, summary_prompt
from app.repositories import conversation_repository, message_repository
from app.services.context_service import messages_for_turn

logger = logging.getLogger(__name__)


def _transcript(turns: list[message_repository.HistoryTurn], budget: int) -> tuple[str, int | None]:
    """只覆盖完整装入输入的轮次，不把因预算未送出的消息标为已摘要。"""
    lines: list[str] = []
    used = 0
    boundary = None
    for turn in turns:
        line = json.dumps(messages_for_turn(turn), ensure_ascii=False, separators=(",", ":"))
        size = len(line) + (2 if lines else 0)
        if used + size > budget:
            break
        lines.append(line)
        used += size
        boundary = turn.last_message_no
    return "\n\n".join(lines), boundary


async def run_summary_for_conversation(
    db: Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None = None
) -> None:
    try:
        await _summarize(db, user_id, conversation_id, slots)
    except Exception as error:  # noqa: BLE001 - 摘要是派生数据，失败不能影响已完成的回复。
        logger.warning(
            "agent_summary_failed conversation=%s type=%s",
            conversation_id,
            type(error).__name__,
        )


async def _summarize(
    db: Database, user_id: int, conversation_id: int, slots: asyncio.Semaphore | None
) -> None:
    settings = get_settings()
    async with db.session() as session:
        conversation = await conversation_repository.find_owned(session, user_id, conversation_id)
        if conversation is None or conversation.status != "ACTIVE":
            return
        turns = await message_repository.history_turns(
            session,
            user_id=user_id,
            conversation_id=conversation_id,
            after_message_no=conversation.summary_through_message_no,
            before_message_no=None,
            limit=128,
            newest=False,
        )
        if sum(len(turn.messages) for turn in turns) <= settings.agent_summary_trigger_messages:
            return
        keep = 0
        split = len(turns)
        while split > 0 and keep < settings.agent_summary_keep_messages:
            split -= 1
            keep += len(turns[split].messages)
        candidates = turns[:split]
        if not candidates:
            return
        previous_summary = conversation.context_summary
        expected_version = conversation.version
        expected_boundary = conversation.summary_through_message_no
        budget = (
            settings.agent_context_max_chars
            - len(previous_summary)
            - len(SUMMARY_SYSTEM_PROMPT)
            - 256
        )
        transcript, boundary = _transcript(candidates, budget)
        if not transcript.strip() or boundary is None:
            return

    async with (
        asyncio.timeout(settings.agent_request_timeout_seconds),
        slots or asyncio.Semaphore(1),
    ):
        completion = await get_llm_client().chat.completions.create(
            **model_options(
                [
                    {"role": "system", "content": SUMMARY_SYSTEM_PROMPT},
                    {
                        "role": "user",
                        "content": summary_prompt(previous_summary, transcript),
                    },
                ]
            )
        )
    if (
        not completion.choices
        or completion.choices[0].finish_reason != "stop"
        or not completion.choices[0].message.content
    ):
        return
    summary = completion.choices[0].message.content.strip()
    limit = min(settings.agent_summary_max_chars, 16_000)
    if not summary or len(summary) > limit:
        return

    async with db.session() as session:
        updated = await conversation_repository.update_summary(
            session,
            conversation_id=conversation_id,
            user_id=user_id,
            expected_version=expected_version,
            expected_boundary=expected_boundary,
            summary=summary,
            boundary=boundary,
        )
        if not updated:
            logger.info("agent_summary_skipped conversation=%s reason=stale", conversation_id)
