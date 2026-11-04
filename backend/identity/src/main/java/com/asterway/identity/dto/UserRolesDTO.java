package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 用户角色集合；USER 必须保留，SUPER_ADMIN 不允许通过接口调整。 */
public record UserRolesDTO(
        @NotNull @Size(max = 3) List<@Positive Long> roleIds,
        @NotNull @PositiveOrZero Long version
) {
}
