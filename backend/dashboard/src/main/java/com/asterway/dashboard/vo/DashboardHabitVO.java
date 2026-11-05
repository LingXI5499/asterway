package com.asterway.dashboard.vo;

import java.time.Instant;
import java.time.LocalTime;

/** 今日习惯读取应做规则或实际打卡快照，不改变规则和打卡。 */
public record DashboardHabitVO(
        String habitId,
        String revisionId,
        String categoryId,
        String name,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        String status,
        Instant deletedAt,
        String checkinId,
        int doneCount,
        int dueCount,
        boolean canCancel,
        boolean canCheckin) {}
