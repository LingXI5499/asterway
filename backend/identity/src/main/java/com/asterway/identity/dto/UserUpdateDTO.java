package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 本人及后台资料更新；未提供的资料字段保持原值。 */
public record UserUpdateDTO(
        @Size(max = 60) String username,
        @Size(max = 60) String nickname,
        @NotNull @PositiveOrZero Long version
) {
}
