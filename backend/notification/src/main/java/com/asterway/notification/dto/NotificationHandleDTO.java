package com.asterway.notification.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 处理提醒的消息版本；确认完成还须提供界面读取到的目标版本。 */
public record NotificationHandleDTO(
        @NotNull @PositiveOrZero Long version, @PositiveOrZero Long goalVersion) {}
