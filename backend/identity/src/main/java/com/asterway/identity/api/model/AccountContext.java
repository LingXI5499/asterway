package com.asterway.identity.api.model;

import java.util.UUID;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record AccountContext(
        UUID key,
        String scope,
        UUID userId,
        String traceId,
        long credentialVersion,
        long securityVersion
) {

    public static final String ANONYMOUS_SCOPE_PREFIX = "ANON:";
    public static final String USER_SCOPE_PREFIX = "USER:";

}
