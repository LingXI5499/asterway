package com.asterway.agent.vo;

/** 会话内普通回复结果；流式回复通过 SSE 返回。 */
public record AgentSendResultVO(
        String reply,
        String model,
        String finishReason,
        String conversationId,
        String turnId,
        String requestId,
        long turnVersion) {}
