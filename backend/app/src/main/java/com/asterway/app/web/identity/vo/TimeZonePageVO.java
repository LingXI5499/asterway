package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.TimeZonePage;

import java.util.List;

public record TimeZonePageVO(
        List<TimeZoneItemVO> items,
        String nextCursor,
        boolean hasMore
) {

    public static TimeZonePageVO from(TimeZonePage value) {
        TimeZonePageVO converted = new TimeZonePageVO(value.items().stream().map(TimeZoneItemVO::from).toList(), value.nextCursor(), value.hasMore());
        return converted;
    }
}
