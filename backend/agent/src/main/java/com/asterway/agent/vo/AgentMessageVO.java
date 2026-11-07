package com.asterway.agent.vo;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** 历史消息；未完成的助手文本可展示，但不能当作已确认回答。 */
public record AgentMessageVO(
        String id,
        long messageNo,
        String role,
        String content,
        boolean complete,
        String finishReason,
        String model,
        JsonNode usage,
        Instant createdAt,
        JsonNode citations,
        JsonNode usedMemoryIds) {}
