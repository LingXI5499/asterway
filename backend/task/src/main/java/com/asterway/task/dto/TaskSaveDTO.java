package com.asterway.task.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** 创建、部分修改或完整替换资料；创建和替换时名称必填，状态单独修改。 */
public record TaskSaveDTO(
        @Size(max = 100) String name,
        @Size(max = 20000) String description,
        @Size(max = 20000) String completionCriteria,
        @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT") String priority,
        @Positive Integer weight,
        @Positive Integer estimatedMinutes,
        LocalDate startDate,
        LocalDate dueDate,
        @PositiveOrZero Integer sortOrder,
        @PositiveOrZero Long version) {}
