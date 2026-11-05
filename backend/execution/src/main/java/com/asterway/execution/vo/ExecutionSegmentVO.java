package com.asterway.execution.vo;

import java.time.Instant;

/** 时间线视图：落库结束时间和开放段的展示截止时间分开表达。 */
public record ExecutionSegmentVO(
        String id,
        Integer segmentNo,
        String assignmentId,
        Instant startedAt,
        Instant endedAt,
        String endReason,
        Instant effectiveEndedAt,
        Long actualSeconds,
        String timeZone) {}
