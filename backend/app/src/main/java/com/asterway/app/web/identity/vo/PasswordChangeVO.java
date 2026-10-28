package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.PasswordChangeResult;

import java.time.Instant;

public record PasswordChangeVO(
        boolean passwordChanged,
        boolean reloginRequired,
        Instant changedAt
) {

    public static PasswordChangeVO from(PasswordChangeResult value) {
        PasswordChangeVO converted = new PasswordChangeVO(value.passwordChanged(), value.reloginRequired(), value.changedAt());
        return converted;
    }
}
