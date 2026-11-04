package com.asterway.common.security;

import java.util.Set;

/** 业务模块提供给安全组件的最小账户快照；不得直接作为 HTTP 返回对象。 */
public record SecurityAccountData(
        long userId,
        String username,
        String passwordHash,
        boolean enabled,
        boolean mustChangePassword,
        long authVersion,
        Set<String> roles,
        Set<String> permissions
) {
}
