package com.asterway.identity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 原子替换同一角色的菜单集合，服务端补齐祖先目录。 */
public record MenuGrantDTO(
        @NotNull @Size(max = 200) List<@Positive Long> menuIds,
        @NotNull @PositiveOrZero Long version
) {
}
