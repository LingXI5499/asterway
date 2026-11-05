package com.asterway.goal.vo;

import com.asterway.common.result.GrowthOverviewVO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 安全返回对象；BIGINT 标识统一作为字符串返回。 */
public record GoalVO(
        String id,
        String name,
        String description,
        String completionCriteria,
        LocalDate startDate,
        LocalDate expectedEndDate,
        String status,
        Instant archivedAt,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        BigDecimal progress,
        List<GrowthOverviewVO> stages,
        boolean completionConditionMet,
        long completionRound) {}
