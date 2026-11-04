package com.asterway.identity.vo;

import java.time.Instant;
import java.util.Map;

/** 操作审计展示对象，摘要仅包含已筛选的安全字段。 */
public record OperationLogVO(
        String id,
        String actorId,
        String actorUsername,
        String targetUserId,
        String resourceType,
        String resourceId,
        String actionCode,
        String outcome,
        String reasonCode,
        Map<String, Object> changeSummary,
        String ipAddress,
        String traceId,
        Instant occurredAt
) {
}
