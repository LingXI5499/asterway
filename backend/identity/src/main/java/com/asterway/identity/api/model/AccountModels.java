package com.asterway.identity.api.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// 账户接口使用的不可变业务对象；版本统一以十进制字符串向页面传输。
public final class AccountModels {

    private AccountModels() {
    }

    // 作用域与身份均由 app 从服务端会话构造，不能从请求体读取。
    public record Context(
            UUID key,
            String scope,
            UUID userId,
            String traceId,
            long credentialVersion,
            long securityVersion
    ) {
    }

    public record RegistrationInput(
            String username,
            String nickname,
            String password,
            String confirmPassword,
            String zoneId,
            UUID captchaChallengeId,
            String captchaVerificationToken
    ) {
        @Override
        public String toString() {
            return "RegistrationInput[protected]";
        }
    }

    public record LoginInput(
            String username,
            String password,
            UUID captchaChallengeId,
            String captchaVerificationToken
    ) {
        @Override
        public String toString() {
            return "LoginInput[protected]";
        }
    }

    public record ProfileInput(String expectedVersion, String username, String nickname) {
    }

    public record PasswordInput(
            String expectedVersion,
            String currentPassword,
            String newPassword,
            String confirmPassword,
            Boolean confirmed
    ) {
        @Override
        public String toString() {
            return "PasswordInput[protected]";
        }
    }

    public record ZoneInput(String detectedZoneId, String expectedVersion) {
    }

    public record CsrfInfo(String headerName, String token) {
    }

    public record TimeZoneState(
            String activeZoneId,
            UUID zonePeriodId,
            String zoneRevision,
            String pendingZoneId,
            Instant pendingEffectiveAt
    ) {
    }

    public record SelfAccount(
            UUID id,
            String username,
            String nickname,
            String role,
            String version,
            Instant createdAt,
            Instant updatedAt,
            TimeZoneState timeZone
    ) {
    }

    // 会话快照不含任何密码、内部安全版本或可用于续专注的许可。
    public record SelfSession(
            SelfAccount account,
            CsrfInfo csrf,
            Instant serverTime,
            LocalDate businessDate,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            boolean hasUnfinishedExecutions
    ) {
    }

    public record ProfileResult(SelfAccount account, boolean changed) {
    }

    public record PasswordChangeResult(boolean passwordChanged, boolean reloginRequired, Instant changedAt) {
    }

    public record DeviceTimeZoneResult(SelfAccount account, boolean changed, Instant serverTime) {
    }

    public record LogoutResult(boolean loggedOut) {
    }

    public record PublicBootstrap(String siteName, boolean registrationOpen, Instant serverTime) {
    }

    public record TimeZoneItem(String zoneId) {
    }

    public record TimeZonePage(List<TimeZoneItem> items, String nextCursor, boolean hasMore) {
    }

    public record CaptchaChallenge(
            UUID challengeId,
            String purpose,
            String backgroundImage,
            String pieceImage,
            int imageWidth,
            int imageHeight,
            int pieceWidth,
            int pieceHeight,
            int pieceTop,
            Instant expiresAt
    ) {
    }

    public record CaptchaProof(UUID challengeId, String verificationToken, Instant expiresAt) {
    }

    // 登录授权仅在应用内部使用，不能直接作为 HTTP 响应或重新签发 Cookie 的凭据。
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

    public record OperationSnapshot(
            String operation,
            UUID idempotencyKey,
            String state,
            int httpStatus,
            String resultCode,
            Object result
    ) {
    }
}
