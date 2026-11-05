package com.asterway.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;

/** 只由 Service 构造的查询上下文，控制端身份不返回前端。 */
public record DashboardReadDTO(
        long userId, LocalDate today, String timeZone, Instant now, String sessionId) {}
