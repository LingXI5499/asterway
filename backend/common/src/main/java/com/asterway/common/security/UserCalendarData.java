package com.asterway.common.security;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** 一次业务操作的服务端日历快照，避免跨午夜时多次取今天发生变化。 */
public record UserCalendarData(long userId, ZoneId zone, Instant now) {
    public LocalDate today() {
        return now.atZone(zone).toLocalDate();
    }

    public Instant start(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }
}
