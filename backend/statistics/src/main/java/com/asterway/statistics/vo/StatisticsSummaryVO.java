package com.asterway.statistics.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 统一周期统计，目标筛选只作用于成长／执行，习惯始终是独立账户维度。 */
public record StatisticsSummaryVO(
        String period,
        LocalDate dateFrom,
        LocalDate dateTo,
        String timeZone,
        Instant serverTime,
        String goalId,
        String stageId,
        String taskId,
        StatisticsExecutionVO execution,
        StatisticsHabitTotalsVO habits,
        StatisticsStateTotalsVO states) {}
