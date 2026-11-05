package com.asterway.planning.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 安全返回对象；BIGINT 标识统一作为字符串返回。 */
public record WeeklyPlanVO(
        String id, LocalDate weekStartDate, Long version, Instant createdAt, Instant updatedAt) {}
