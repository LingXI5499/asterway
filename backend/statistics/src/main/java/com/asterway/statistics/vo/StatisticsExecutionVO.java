package com.asterway.statistics.vo;

import java.time.Instant;

/** 毫秒先合计再统一转秒；区分累计和查询区间，不以段数代替会话数。 */
public record StatisticsExecutionVO(
        long totalActualSeconds,
        long rangeActualSeconds,
        long totalActualMilliseconds,
        long rangeActualMilliseconds,
        long totalSessions,
        long rangeSessions,
        long totalFinishedSessions,
        long rangeFinishedSessions,
        Instant lastExecutionAt) {}
