package com.asterway.habit.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

/** 今日实际应做行；已完成后暂停或删除的行仍为 1/1。 */
public record HabitTodayVO(
        String habitId,
        String revisionId,
        String categoryId,
        String name,
        @JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        String status,
        String checkinId,
        int doneCount,
        int dueCount,
        boolean canCancel,
        boolean canCheckin) {}
