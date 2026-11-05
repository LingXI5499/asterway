package com.asterway.statistics.vo;

import java.math.BigDecimal;

/** 完成率为所有实际完成除以所有应做，分母为零时为空。 */
public record StatisticsHabitTotalsVO(long doneCount, long dueCount, BigDecimal completionRate) {}
