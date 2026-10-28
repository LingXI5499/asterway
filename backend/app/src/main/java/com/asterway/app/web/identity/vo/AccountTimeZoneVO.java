package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.TimeZoneState;

import java.time.Instant;
import java.util.UUID;

public record AccountTimeZoneVO(
        String activeZoneId,
        UUID zonePeriodId,
        String zoneRevision,
        String pendingZoneId,
        Instant pendingEffectiveAt
) {

    public static AccountTimeZoneVO from(TimeZoneState value) {
        AccountTimeZoneVO converted = new AccountTimeZoneVO(
                value.activeZoneId(),
                value.zonePeriodId(),
                value.zoneRevision(),
                value.pendingZoneId(),
                value.pendingEffectiveAt()
        );
        return converted;
    }
}
