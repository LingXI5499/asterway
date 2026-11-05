package com.asterway.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 只携带乐观锁版本的业务操作。 */
public record GrowthVersionDTO(@NotNull @PositiveOrZero Long version) {}
