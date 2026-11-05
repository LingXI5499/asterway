package com.asterway.goal.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 归档固定结束全部未结束执行并移出计划，必须明确确认，不接受旧保留安排策略。 */
public record GoalArchiveDTO(
        @NotNull @PositiveOrZero Long version,
        @NotNull @AssertTrue Boolean confirmed) {
}
