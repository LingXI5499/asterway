package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.RegistrationResult;

import java.time.Instant;
import java.util.UUID;

public record RegistrationVO(
        UUID accountId,
        String username,
        boolean loginRequired,
        Instant createdAt
) {

    public static RegistrationVO from(RegistrationResult value) {
        RegistrationVO converted = new RegistrationVO(
                value.accountId(),
                value.username(),
                value.loginRequired(),
                value.createdAt()
        );
        return converted;
    }
}
