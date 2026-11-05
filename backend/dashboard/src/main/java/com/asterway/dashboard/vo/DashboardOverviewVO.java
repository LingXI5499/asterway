package com.asterway.dashboard.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 服务端日历快照和今日概览，所有指标都读取各业务事实。 */
public record DashboardOverviewVO(
        LocalDate today,
        String timeZone,
        Instant serverTime,
        DashboardMetricsVO metrics,
        DashboardExecutionVO currentExecution) {}
