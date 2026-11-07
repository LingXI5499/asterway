package com.asterway.agent.vo;

import java.time.Instant;

/** 会话详情；runningTurn 用于刷新后恢复生成中的一轮。 */
public record AgentConversationDetailVO(
        String id,
        String title,
        String status,
        long summaryThroughMessageNo,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt,
        long version,
        AgentTurnVO runningTurn) {}
