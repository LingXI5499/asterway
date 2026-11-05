package com.asterway.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 单次最多调整 100 个同父资源；事务内校验全部版本后更新。 */
public record GrowthReorderDTO(@NotEmpty @Size(max = 100) List<@Valid ItemDTO> items) {
    public record ItemDTO(
            @NotNull @Positive Long id,
            @NotNull @PositiveOrZero Long version,
            @NotNull @PositiveOrZero Integer sortOrder) {}
}
