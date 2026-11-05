package com.asterway.statistics.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 一天内的原始习惯统计口径。 */
public record StatisticsHabitDayVO(
        LocalDate businessDate, long doneCount, long dueCount, BigDecimal completionRate) {}
