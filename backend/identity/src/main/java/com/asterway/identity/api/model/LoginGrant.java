package com.asterway.identity.api.model;

import java.util.UUID;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record LoginGrant(
        UUID accountId,
        String username,
        String nickname,
        String role,
        long credentialVersion,
        long securityVersion,
        boolean replayed
) {
}
