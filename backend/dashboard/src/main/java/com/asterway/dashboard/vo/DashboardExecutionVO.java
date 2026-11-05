package com.asterway.dashboard.vo;

import java.time.Instant;

/** 当前执行只读摘要，不返回登录 SID，不刷新心跳。 */
public record DashboardExecutionVO(
        String id,
        String taskId,
        String taskName,
        long version,
        String segmentId,
        long actualSeconds,
        Instant heartbeatDeadline,
        Instant eligibleUntil,
        boolean canHeartbeat) {}
