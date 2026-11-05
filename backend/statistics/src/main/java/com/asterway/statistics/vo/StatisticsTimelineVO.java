package com.asterway.statistics.vo;

import java.time.Instant;

/** 真实事件时间线；事实标识用于稳定分页，不根据当前状态推测过去。 */
public record StatisticsTimelineVO(
        String kind,
        String factId,
        String objectId,
        String eventType,
        Instant occurredAt,
        String label) {}
