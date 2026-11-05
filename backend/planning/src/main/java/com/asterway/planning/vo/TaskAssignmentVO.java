package com.asterway.planning.vo;

import java.time.Instant;

/** 安全返回对象；BIGINT 标识统一作为字符串返回。 */
public record TaskAssignmentVO(
        String id,
        String taskId,
        String weeklyPlanId,
        String dailyPlanId,
        Integer sortOrder,
        Instant assignedAt,
        Instant endedAt,
        String endReason,
        Long version,
        Instant createdAt,
        Instant updatedAt) {}
