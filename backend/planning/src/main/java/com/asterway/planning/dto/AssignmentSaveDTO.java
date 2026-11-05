package com.asterway.planning.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 替换当前安排，预期尚无安排时 assignmentId/version 同时为空。 */
public record AssignmentSaveDTO(
        @NotNull @Positive Long weeklyPlanId,
        @Positive Long dailyPlanId,
        @Positive Long assignmentId,
        @PositiveOrZero Long version,
        @PositiveOrZero Integer sortOrder) {}
