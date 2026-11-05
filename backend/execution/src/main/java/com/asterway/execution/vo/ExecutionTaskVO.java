package com.asterway.execution.vo;

import java.time.LocalDate;

/** 执行资格的只读投影，供 Service 使用，不直接暴露为公开接口。 */
public record ExecutionTaskVO(
        Long taskId,
        Long taskVersion,
        String taskStatus,
        Long assignmentId,
        LocalDate weekStartDate,
        LocalDate planDate,
        Long goalId,
        Long stageId) {}
