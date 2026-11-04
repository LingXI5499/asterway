package com.asterway.common.security;

import java.util.Set;

/** 本次请求的已验证身份，不携带密码哈希。 */
public record AccountPrincipal(
        long userId,
        String username,
        boolean mustChangePassword,
        long authVersion,
        Set<String> roles,
        Set<String> permissions,
        String sessionId
) {

    public boolean superAdmin() {
        return roles.contains("SUPER_ADMIN");
    }
}
