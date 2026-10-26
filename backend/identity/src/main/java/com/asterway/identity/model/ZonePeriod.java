package com.asterway.identity.model;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.zone.ZoneRulesProvider;
import java.util.UUID;

// 已生效的历史时区区间，只允许关闭开放区间，不改写旧时区和起点。
public record ZonePeriod(
        UUID id,
        UUID userId,
        long revision,
        String zoneId,
        String tzdbVersion,
        OffsetDateTime validFrom,
        OffsetDateTime validTo,
        String source
) {

    // 使用当前时区规则创建开放区间；数据库时间传入后统一转换为 UTC。
    public static ZonePeriod create(UUID userId, long revision, String zoneId, OffsetDateTime at, String source) {
        String tzdbVersion = ZoneRulesProvider.getVersions(zoneId).lastKey();
        ZonePeriod period = new ZonePeriod(
                UUID.randomUUID(),
                userId,
                revision,
                zoneId,
                tzdbVersion,
                at.withOffsetSameInstant(ZoneOffset.UTC),
                null,
                source
        );
        return period;
    }
}
