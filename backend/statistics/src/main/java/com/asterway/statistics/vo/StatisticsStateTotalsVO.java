package com.asterway.statistics.vo;

/** 区间动作数与区间去重任务数，不能相加每日去重数代替区间去重数。 */
public record StatisticsStateTotalsVO(
        long goalCompletionActions,
        long stageCompletionActions,
        long taskCompletionActions,
        long completedDistinctTasks,
        long goalArchiveActions,
        long goalRestoreActions) {}
