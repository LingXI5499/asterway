package com.asterway.agent.vo;

import java.time.Instant;

/** 会话列表项；摘要边界表示已压缩到的连续消息序号。 */
public record AgentConversationVO(
        String id,
        String title,
        String status,
        long summaryThroughMessageNo,
        Instant lastMessageAt,
        Instant createdAt,
        Instant updatedAt,
        long version) {}
