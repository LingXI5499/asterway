package com.asterway.identity.api.model;

import java.util.UUID;

// 仅供应用内安全核验使用，禁止作为 HTTP 响应或日志正文。
public record AccountSecurityState(
        UUID id,
        String usernameKey,
        String passwordHash,
        String role,
        String status,
        String deletedAt,
        long securityVersion,
        long credentialVersion,
        long loginNameVersion
) {

    @Override
    public String toString() {
        return "AccountSecurityState[protected]";
    }
}
