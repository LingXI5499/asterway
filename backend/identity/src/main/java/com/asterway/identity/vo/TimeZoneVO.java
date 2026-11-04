package com.asterway.identity.vo;

import java.time.Instant;

/** 本人时区状态，待生效值不会提前替换当前业务时区。 */
public record TimeZoneVO(
        String timeZone,
        String pendingTimeZone,
        Instant effectiveAt,
        long version
) {
}
