package com.asterway.identity.api.model;

import java.time.Instant;
import java.util.UUID;

// 账户模块对外提供的本人基础资料，不携带凭据或内部状态字段。
public record CurrentAccount(
        UUID accountId,
        String username,
        String nickname,
        String role,
        Instant createdAt,
        Instant updatedAt
) {
}
