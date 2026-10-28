package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.DeviceTimeZoneResult;

import java.time.Instant;

public record DeviceTimeZoneVO(
        AccountVO account,
        boolean changed,
        Instant serverTime
) {

    public static DeviceTimeZoneVO from(DeviceTimeZoneResult value) {
        DeviceTimeZoneVO converted = new DeviceTimeZoneVO(AccountVO.from(value.account()), value.changed(), value.serverTime());
        return converted;
    }
}
