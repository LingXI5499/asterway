package com.asterway.common.security;

import java.time.Instant;
import java.time.ZoneId;

/** 计时所需的账户时区与安全投影，不包含凭据，不作为 HTTP 返回。 */
public record ExecutionAccountData(
        long userId,
        boolean enabled,
        long authVersion,
        String timeZone,
        String pendingTimeZone,
        Instant timeZoneEffectiveAt) {

    /** 与账户延后生效规则一致，按本次服务端时间求有效时区。 */
    public ZoneId effectiveZone(Instant now) {
        return ZoneId.of(
                pendingTimeZone != null && !timeZoneEffectiveAt.isAfter(now)
                        ? pendingTimeZone
                        : timeZone);
    }

    /** 只返回仍在未来的切换点，已经生效的时区不成为下一段的过去截止。 */
    public Instant pendingBoundary(Instant now) {
        return pendingTimeZone != null && timeZoneEffectiveAt.isAfter(now)
                ? timeZoneEffectiveAt
                : null;
    }
}
