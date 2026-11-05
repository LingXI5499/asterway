package com.asterway.dashboard.vo;

import java.math.BigDecimal;

/** 今日事实概览；计划完成数为当前计划状态，不伪装成历史完成动作。 */
public record DashboardMetricsVO(
        long todayTasks,
        long todayCurrentCompletedTasks,
        long weekUndatedTasks,
        long actualSeconds,
        long habitDone,
        long habitDue,
        BigDecimal habitCompletionRate,
        String runningExecutionId,
        long unreadMessages) {}
