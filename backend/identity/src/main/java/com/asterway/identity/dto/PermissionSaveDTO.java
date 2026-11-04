package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 权限新增与修改共用输入；新增不要求 version，修改必须携带。 */
public record PermissionSaveDTO(
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$") String code,
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String description,
        @NotNull Boolean enabled,
        @PositiveOrZero Long version
) {
}
