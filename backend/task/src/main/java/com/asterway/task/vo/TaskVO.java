package com.asterway.task.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 安全返回对象；BIGINT 标识统一作为字符串返回。 */
public record TaskVO(
        String id,
        String stageId,
        String name,
        String description,
        String completionCriteria,
        String priority,
        Integer weight,
        Integer estimatedMinutes,
        LocalDate startDate,
        LocalDate dueDate,
        String status,
        Integer sortOrder,
        Instant completedAt,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        String goalId,
        String goalName,
        String stageName,
        String assignmentStatus,
        String weeklyPlanId,
        String dailyPlanId,
        Instant goalArchivedAt) {}
