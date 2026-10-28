package com.asterway.app.web.identity.dto;

import com.asterway.identity.api.model.ZoneInput;

public record DeviceTimeZoneDTO(String detectedZoneId, String expectedVersion) {

    public ZoneInput toInput() {
        ZoneInput converted = new ZoneInput(detectedZoneId, expectedVersion);
        return converted;
    }
}
