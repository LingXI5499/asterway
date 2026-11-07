package com.asterway.agent.vo;

import java.time.Instant;

/** 轮次状态；reply 为已保存文本，供刷新和幂等请求恢复。 */
public record AgentTurnVO(
        String id,
        String conversationId,
        String requestId,
        String status,
        String finishReason,
        String errorCode,
        Instant startedAt,
        Instant finishedAt,
        String reply,
        long version) {}
