package com.asterway.execution.dto;

import java.time.Instant;

/** Service 内部构造的查询上下文，不作为 HTTP 入参绑定，保证时间和 owner 不受客户端控制。 */
public record ExecutionReadDTO(
        long userId,
        String sessionId,
        Instant now,
        Instant rangeStart,
        Instant rangeEnd,
        int heartbeatIntervalSeconds) {}
