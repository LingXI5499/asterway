package com.asterway.habit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 习惯资料与规则；不接收状态、日期补录或 userId。PATCH 的空值保留，PUT 的空值清空可选项。 */
public record HabitSaveDTO(
        @Size(max = 100) String name,
        @Size(max = 10000) String description,
        @Size(max = 10000) String completionCriteria,
        @Positive Long categoryId,
        String cycleType,
        @Size(max = 7) List<Integer> weekdays,
        LocalDate startDate,
        @JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        @PositiveOrZero Long version) {}
