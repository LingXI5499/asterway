package com.asterway.notification.vo;

/** 未读数排除已失效消息，不将失效伪装成已读。 */
public record NotificationUnreadVO(long unreadCount) {}
