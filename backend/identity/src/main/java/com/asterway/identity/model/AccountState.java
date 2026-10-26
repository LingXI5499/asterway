package com.asterway.identity.model;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

// 模块内部账户状态，密码哈希仅在验密和受保护的安全核验中读取。
public record AccountState(
        UUID id,
        String username,
        String usernameKey,
        String nickname,
        String passwordHash,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long version,
        OffsetDateTime deletedAt,
        long securityVersion,
        long credentialVersion,
        long loginNameVersion,
        OffsetDateTime passwordChangedAt,
        UUID currentZonePeriodId,
        String pendingZoneId,
        OffsetDateTime pendingZoneEffectiveAt,
        OffsetDateTime pendingZoneRequestedAt
) {

    // 构造资料变更后的待核验状态；实际持久化及更新时间仍由业务事务负责。
    public AccountState withProfile(String newUsername, String newNickname) {
        long nextLoginNameVersion = loginNameVersion;
        if (!newUsername.equals(username)) {
            nextLoginNameVersion++;
        }
        AccountState changed = new AccountState(
                id,
                newUsername,
                newUsername.toLowerCase(Locale.ROOT),
                newNickname,
                passwordHash,
                role,
                status,
                createdAt,
                updatedAt,
                version + 1,
                deletedAt,
                securityVersion,
                credentialVersion,
                nextLoginNameVersion,
                passwordChangedAt,
                currentZonePeriodId,
                pendingZoneId,
                pendingZoneEffectiveAt,
                pendingZoneRequestedAt
        );
        return changed;
    }

    // 密码变更同步提升凭据版本和安全版本，用于独立账本的提交前核验。
    public AccountState withPasswordHash(String newPasswordHash) {
        AccountState changed = new AccountState(
                id,
                username,
                usernameKey,
                nickname,
                newPasswordHash,
                role,
                status,
                createdAt,
                updatedAt,
                version + 1,
                deletedAt,
                securityVersion + 1,
                credentialVersion + 1,
                loginNameVersion,
                passwordChangedAt,
                currentZonePeriodId,
                pendingZoneId,
                pendingZoneEffectiveAt,
                pendingZoneRequestedAt
        );
        return changed;
    }

    @Override
    public String toString() {
        return "AccountState[protected]";
    }
}
