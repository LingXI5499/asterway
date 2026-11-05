package com.asterway.dashboard.vo;

import java.math.BigDecimal;

/** 当前目标精确任务数和权重进度；没有有效任务时进度为空。 */
public record DashboardGoalVO(
        String id,
        String name,
        String status,
        long version,
        boolean completionConditionMet,
        long taskCount,
        long completedTasks,
        BigDecimal currentProgress) {}
