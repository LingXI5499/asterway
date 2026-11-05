package com.asterway.statistics.vo;

import java.time.LocalDate;

/** 每日真实状态动作与去重完成任务数，基线不计完成。 */
public record StatisticsStateDayVO(
        LocalDate businessDate,
        long goalCompletionActions,
        long stageCompletionActions,
        long taskCompletionActions,
        long completedDistinctTasks,
        long goalArchiveActions,
        long goalRestoreActions) {}
