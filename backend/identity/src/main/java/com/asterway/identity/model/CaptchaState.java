package com.asterway.identity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

// verificationData 是带认证的加密材料，不能写入普通日志或长期幂等结果。
public record CaptchaState(
        UUID id,
        String purpose,
        byte[] bindingDigest,
        byte[] verificationData,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime consumedAt,
        UUID createKey,
        UUID verifyKey,
        byte[] verifyFingerprint,
        UUID consumedKey,
        byte[] consumedFingerprint,
        String state
) {
}
