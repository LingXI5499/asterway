package com.asterway.dashboard.vo;

import java.time.LocalDate;

/** 今日或本周未定日任务，不重复保存任务数据。 */
public record DashboardTaskVO(
        String taskId,
        String name,
        String status,
        long version,
        String goalId,
        String stageId,
        String assignmentId,
        LocalDate planDate,
        String section,
        String priority,
        LocalDate dueDate,
        int sortOrder) {}
