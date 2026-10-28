package com.asterway.identity.api.model;

import java.time.Instant;
import java.util.UUID;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record TimeZoneState(
        String activeZoneId,
        UUID zonePeriodId,
        String zoneRevision,
        String pendingZoneId,
        Instant pendingEffectiveAt
) {
}
