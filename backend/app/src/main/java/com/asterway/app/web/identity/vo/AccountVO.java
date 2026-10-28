package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.AccountSnapshot;

import java.time.Instant;
import java.util.UUID;

public record AccountVO(
        UUID id,
        String username,
        String nickname,
        String role,
        String version,
        Instant createdAt,
        Instant updatedAt,
        AccountTimeZoneVO timeZone
) {

    public static AccountVO from(AccountSnapshot value) {
        AccountVO converted = new AccountVO(
                value.id(),
                value.username(),
                value.nickname(),
                value.role(),
                value.version(),
                value.createdAt(),
                value.updatedAt(),
                AccountTimeZoneVO.from(value.timeZone())
        );
        return converted;
    }
}
