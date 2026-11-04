package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 后台新增普通账户，管理员身份必须通过独立角色分配接口赋予。 */
public record UserCreateDTO(
        @NotBlank @Size(max = 60) String username,
        @Size(max = 60) String nickname,
        @NotNull @Size(max = 100) String initialPassword,
        @NotNull @Size(max = 100) String confirmPassword,
        @NotBlank @Size(max = 64) String timeZone
) {
}
