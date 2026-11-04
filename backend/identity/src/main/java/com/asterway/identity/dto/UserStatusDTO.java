package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 账户启停请求，使用版本避免覆盖其他管理操作。 */
public record UserStatusDTO(
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero Long version
) {
}
