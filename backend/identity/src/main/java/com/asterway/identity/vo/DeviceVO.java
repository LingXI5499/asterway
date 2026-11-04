package com.asterway.identity.vo;

import java.time.Instant;

/** 以会话为单位的设备展示对象，不返回 JWT。 */
public record DeviceVO(
        String id,
        String userId,
        String username,
        String deviceLabel,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant lastActiveAt,
        Instant expiresAt,
        boolean current
) {
}
