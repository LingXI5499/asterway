package com.asterway.goal.vo;

import java.time.Instant;

/** 主体真实历史；BASELINE 仅说明初始化状态，不计为完成动作。 */
public record GoalStateHistoryVO(
        String id,
        String goalId,
        String eventType,
        String beforeStatus,
        String afterStatus,
        Boolean beforeArchived,
        Boolean afterArchived,
        String nameSnapshot,
        long entityVersion,
        Instant occurredAt,
        String sourceType,
        Instant createdAt) {}
