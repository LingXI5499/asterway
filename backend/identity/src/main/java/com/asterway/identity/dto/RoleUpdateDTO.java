package com.asterway.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 内置角色只修改名称与说明，不修改编码或管理级别。 */
public record RoleUpdateDTO(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 255) String description,
        @NotNull @PositiveOrZero Long version
) {
}
