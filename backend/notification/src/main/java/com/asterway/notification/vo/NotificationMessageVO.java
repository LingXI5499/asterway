package com.asterway.notification.vo;

import java.time.Instant;

/** 消息状态与来源当前版本；不返回收件人内部标识。 */
public record NotificationMessageVO(
        String id,
        String goalId,
        long conditionRound,
        String messageType,
        String title,
        String content,
        Instant readAt,
        String handleStatus,
        Instant handledAt,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Long goalVersion,
        boolean canHandle) {}
