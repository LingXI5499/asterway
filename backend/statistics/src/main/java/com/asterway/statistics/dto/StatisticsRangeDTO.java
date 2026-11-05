package com.asterway.statistics.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Service 归一化后的统计区间；本地日期含首尾，时间点采用半开区间。 */
public record StatisticsRangeDTO(
        long userId,
        String period,
        LocalDate dateFrom,
        LocalDate dateTo,
        String timeZone,
        Instant fromInstant,
        Instant toInstant,
        Instant now,
        Long goalId,
        Long stageId,
        Long taskId) {}
