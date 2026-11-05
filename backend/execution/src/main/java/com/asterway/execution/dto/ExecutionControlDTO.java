package com.asterway.execution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 暂停、恢复、结束共用的控制请求，不接收客户端时间。 */
public record ExecutionControlDTO(@NotNull @PositiveOrZero Long version) {}
