package com.asterway.identity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

// 本人资料查询投影，不读取密码哈希；status 仅供模块内部检查账户状态。
public record AccountProfile(
        UUID accountId,
        String username,
        String nickname,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
