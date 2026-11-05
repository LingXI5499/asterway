package com.asterway.statistics.vo;

import java.math.BigDecimal;

/** 按习惯或历史规则分类归属聚合；删除不抹去历史分子分母。 */
public record StatisticsHabitGroupVO(
        String id,
        String name,
        boolean deleted,
        long doneCount,
        long dueCount,
        BigDecimal completionRate) {}
