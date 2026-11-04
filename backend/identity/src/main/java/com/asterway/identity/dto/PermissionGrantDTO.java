package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 原子替换同一角色的动作权限集合。 */
public record PermissionGrantDTO(
        @NotNull @Size(max = 200) List<@Positive Long> permissionIds,
        @NotNull @PositiveOrZero Long version
) {
}
