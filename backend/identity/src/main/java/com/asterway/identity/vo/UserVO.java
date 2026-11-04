package com.asterway.identity.vo;

import java.time.Instant;
import java.util.List;

/** 账户安全展示对象，不包含密码或安全账本材料。 */
public record UserVO(
        String id,
        String username,
        String nickname,
        boolean enabled,
        boolean mustChangePassword,
        long version,
        String timeZone,
        String pendingTimeZone,
        Instant timeZoneEffectiveAt,
        Instant createdAt,
        List<String> roles,
        List<String> roleIds
) {
}
