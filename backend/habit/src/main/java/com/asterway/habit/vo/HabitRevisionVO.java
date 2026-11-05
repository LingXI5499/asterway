package com.asterway.habit.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** 完整规则及生命周期快照，不包含内部用户标识。 */
public record HabitRevisionVO(
        String id,
        long revisionNo,
        String actionType,
        String name,
        String description,
        String completionCriteria,
        String categoryId,
        String cycleType,
        List<Integer> weekdays,
        LocalDate startDate,
        @JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        String status,
        boolean deleted,
        LocalDate effectiveDate,
        Instant changedAt,
        String timeZone) {}
