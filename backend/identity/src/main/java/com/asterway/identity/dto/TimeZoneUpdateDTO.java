package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 设备时区变更请求，按当前时区的次日午夜生效。 */
public record TimeZoneUpdateDTO(
        @NotBlank @Size(max = 64) String timeZone,
        @NotNull @PositiveOrZero Long version
) {
}
