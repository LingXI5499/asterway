package com.asterway.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 独立状态操作，必须携带当前资源版本。 */
public record GrowthStatusDTO(@NotBlank String status, @NotNull @PositiveOrZero Long version) {}
