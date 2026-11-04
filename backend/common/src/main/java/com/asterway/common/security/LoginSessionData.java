package com.asterway.common.security;

/** Redis 中的一次登录，不缓存角色权限；用户标识使用字符串避免 Lua 数字精度损失。 */
public record LoginSessionData(
        String id,
        String userId,
        long authVersion,
        long createdAt,
        long lastActiveAt,
        long expiresAt,
        String ipAddress,
        String userAgent,
        String deviceLabel
) {
}
