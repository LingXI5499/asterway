package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.TimeZoneItem;

public record TimeZoneItemVO(
        String zoneId
) {

    public static TimeZoneItemVO from(TimeZoneItem value) {
        TimeZoneItemVO converted = new TimeZoneItemVO(value.zoneId());
        return converted;
    }
}
