package com.asterway.identity.mapper;

import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.CaptchaState;
import com.asterway.identity.model.LoginAttempt;
import com.asterway.identity.model.OperationRecord;
import com.asterway.identity.model.ZonePeriod;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// 账户剩余业务的数据访问契约，全部 SQL 保留在 XML 中。
public interface AccountLifecycleMapper {

    OffsetDateTime now();

    void lockScope(@Param("scope") String scope);

    UUID lockAccount(@Param("userId") UUID userId);

    AccountState findState(@Param("userId") UUID userId);

    AccountState findByLoginName(@Param("usernameKey") String usernameKey);

    List<AccountState> listSecurityStates();

    List<AccountState> dueZoneAccounts();

    String recoveryState(@Param("userId") UUID userId);

    void isolateAccount(@Param("userId") UUID userId);

    int insertState(AccountState account);

    void insertGuard(@Param("userId") UUID userId);

    ZonePeriod findZone(@Param("id") UUID id, @Param("userId") UUID userId);

    int insertZone(ZonePeriod period);

    int closeZone(@Param("id") UUID id, @Param("at") OffsetDateTime at);

    int applyZone(@Param("userId") UUID userId, @Param("periodId") UUID periodId);

    int updatePendingZone(
            @Param("userId") UUID userId,
            @Param("zoneId") String zoneId,
            @Param("effectiveAt") OffsetDateTime effectiveAt
    );

    int updateProfile(
            @Param("userId") UUID userId,
            @Param("username") String username,
            @Param("nickname") String nickname,
            @Param("renamed") boolean renamed
    );

    int updatePassword(@Param("userId") UUID userId, @Param("passwordHash") String passwordHash);

    boolean registrationOpen();

    String siteName();

    LoginAttempt loginAttempt(@Param("usernameKey") String usernameKey);

    void saveLoginAttempt(
            @Param("usernameKey") String usernameKey,
            @Param("failures") int failures,
            @Param("lockedUntil") OffsetDateTime lockedUntil
    );

    CaptchaState findCaptcha(@Param("id") UUID id);

    CaptchaState findCreatedCaptcha(@Param("binding") byte[] binding, @Param("key") UUID key);

    long countRecentCaptchas(@Param("binding") byte[] binding);

    void insertCaptcha(CaptchaState state);

    void verifyCaptcha(
            @Param("id") UUID id,
            @Param("key") UUID key,
            @Param("fingerprint") byte[] fingerprint,
            @Param("state") String state,
            @Param("material") byte[] material
    );

    void consumeCaptcha(
            @Param("id") UUID id,
            @Param("key") UUID key,
            @Param("fingerprint") byte[] fingerprint
    );

    void cleanShortLivedData();

    OperationRecord findOperation(@Param("context") Context context, @Param("operation") String operation);

    void insertOperation(
            @Param("context") Context context,
            @Param("operation") String operation,
            @Param("fingerprint") byte[] fingerprint
    );

    void finishOperation(
            @Param("context") Context context,
            @Param("operation") String operation,
            @Param("code") String code,
            @Param("status") int status,
            @Param("summary") String summary
    );

    void audit(
            @Param("context") Context context,
            @Param("targetId") UUID targetId,
            @Param("action") String action,
            @Param("outcome") String outcome
    );

    void recordSecurityChange(
            @Param("operationId") UUID operationId,
            @Param("context") Context context,
            @Param("type") String type,
            @Param("before") AccountState before,
            @Param("after") AccountState after
    );

    boolean securityChangeCommitted(@Param("operationId") UUID operationId);

    boolean securityHistoryExists();

    void markLedgerSynced(@Param("operationId") UUID operationId);

    void deleteCurrentSessionAttributes(@Param("sessionId") String sessionId);

    void deleteCurrentSession(@Param("sessionId") String sessionId);

    void deleteAccountSessionAttributes(@Param("userId") UUID userId);

    void deleteAccountSessions(@Param("userId") UUID userId);

    boolean logoutKeyUsedElsewhere(@Param("context") Context context);
}
