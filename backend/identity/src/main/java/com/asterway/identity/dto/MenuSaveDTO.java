package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 目录、页面和按钮共用保存请求，具体字段组合由 Service 校验。 */
public record MenuSaveDTO(
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String code,
        @Positive Long parentId,
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Pattern(regexp = "DIRECTORY|PAGE|BUTTON") String menuType,
        @Size(max = 255) String routePath,
        @Size(max = 100) String componentKey,
        @Positive Long permissionId,
        @Size(max = 64) String icon,
        @NotNull @PositiveOrZero Integer sortOrder,
        @NotNull Boolean visible,
        @NotNull Boolean enabled,
        @PositiveOrZero Long version
) {
}
