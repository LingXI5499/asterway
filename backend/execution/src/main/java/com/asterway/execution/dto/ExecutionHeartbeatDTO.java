package com.asterway.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 心跳只确认当前段；控制端身份直接来自已认证登录 SID。 */
public record ExecutionHeartbeatDTO(@NotNull @Positive Long segmentId) {}
