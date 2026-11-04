package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 本人改密输入；不自动裁剪密码的首尾字符。 */
public record PasswordUpdateDTO(
        @NotNull @Size(max = 100) String currentPassword,
        @NotNull @Size(max = 100) String newPassword,
        @NotNull @Size(max = 100) String confirmPassword,
        @NotNull @PositiveOrZero Long version
) {
}
