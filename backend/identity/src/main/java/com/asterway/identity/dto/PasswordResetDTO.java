package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 管理员重置密码输入，成功后要求用户改密并撤销全部旧会话。 */
public record PasswordResetDTO(
        @NotNull @Size(max = 100) String newPassword,
        @NotNull @Size(max = 100) String confirmPassword,
        @NotNull @PositiveOrZero Long version
) {
}
