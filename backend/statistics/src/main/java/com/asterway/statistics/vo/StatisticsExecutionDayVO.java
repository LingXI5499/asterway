package com.asterway.statistics.vo;

import java.time.LocalDate;

/** 按当前账号时区与自然日交集计算的耗时。 */
public record StatisticsExecutionDayVO(
        LocalDate businessDate,
        long actualMilliseconds,
        long actualSeconds,
        long sessions,
        long finishedSessions) {}
