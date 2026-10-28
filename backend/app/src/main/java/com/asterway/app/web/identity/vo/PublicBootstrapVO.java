package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.PublicBootstrap;

import java.time.Instant;

public record PublicBootstrapVO(
        String siteName,
        boolean registrationOpen,
        Instant serverTime
) {

    public static PublicBootstrapVO from(PublicBootstrap value) {
        PublicBootstrapVO converted = new PublicBootstrapVO(value.siteName(), value.registrationOpen(), value.serverTime());
        return converted;
    }
}
