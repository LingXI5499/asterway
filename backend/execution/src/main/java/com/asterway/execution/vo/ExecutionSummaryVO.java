package com.asterway.execution.vo;

import java.time.Instant;

/** 累计和区间耗时分别返回，会话数量与时间段数量不混用。 */
public record ExecutionSummaryVO(
        Long totalActualSeconds,
        Long rangeActualSeconds,
        Long totalSessions,
        Long rangeSessions,
        Long finishedSessions,
        Long runningSessions,
        Long pausedSessions,
        Instant latestExecutionAt) {}
