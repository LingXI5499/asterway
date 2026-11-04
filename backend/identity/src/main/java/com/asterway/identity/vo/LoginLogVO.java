package com.asterway.identity.vo;

import java.time.Instant;

/** 登录审计展示对象，不返回密码、令牌或验证码答案。 */
public record LoginLogVO(
        String id,
        String userId,
        String username,
        String outcome,
        String reasonCode,
        String sessionId,
        String ipAddress,
        String userAgent,
        String deviceLabel,
        String traceId,
        Instant occurredAt
) {
}
