package com.asterway.habit.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 分类保存；更新时须提供版本，名称及排序在 Service 统一校验。 */
public record HabitCategorySaveDTO(
        @Size(max = 50) String name,
        @PositiveOrZero Integer sortOrder,
        @PositiveOrZero Long version) {}
