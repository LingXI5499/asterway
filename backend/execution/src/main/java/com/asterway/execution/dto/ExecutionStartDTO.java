package com.asterway.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** 开始或恢复任务执行；任务版本用于阻止延迟开始请求产生新会话。 */
public record ExecutionStartDTO(
        @NotNull @Positive Long taskId, @NotNull @PositiveOrZero Long taskVersion) {}
