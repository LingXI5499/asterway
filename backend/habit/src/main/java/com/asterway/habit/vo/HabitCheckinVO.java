package com.asterway.habit.vo;

import java.time.Instant;
import java.time.LocalDate;

/** 原始打卡事实；取消后仍可读取，不覆盖原打卡时间或修订。 */
public record HabitCheckinVO(
        String id,
        String habitId,
        String revisionId,
        String nameSnapshot,
        String categoryId,
        LocalDate businessDate,
        String timeZone,
        Instant checkedAt,
        Instant cancelledAt,
        long version) {}
