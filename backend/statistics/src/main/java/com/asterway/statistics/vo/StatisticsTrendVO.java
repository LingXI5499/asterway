package com.asterway.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 每日趋势，耗时、打卡和真实完成动作独立汇总后组合。 */
public record StatisticsTrendVO(
        LocalDate businessDate,
        long actualMilliseconds,
        long actualSeconds,
        long sessions,
        long habitDone,
        long habitDue,
        BigDecimal habitCompletionRate,
        long goalCompletionActions,
        long stageCompletionActions,
        long taskCompletionActions,
        long completedDistinctTasks,
        long goalArchiveActions,
        long goalRestoreActions) {}
