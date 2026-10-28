package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.SecureRandomUtils;
import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.AccountAuthenticationApi;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.LoginGrant;
import com.asterway.identity.api.model.LoginInput;
import com.asterway.identity.constant.LoginConstants;
import com.asterway.identity.mapper.AccountLoginAttemptMapper;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.LoginAttempt;
import com.asterway.identity.utils.AccountValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// 每个有效认证意图只累计一次失败，滑动错误不累计凭据失败。
@Service
public class AccountAuthenticationService implements AccountAuthenticationApi {

    private final AccountAuditPort audit;

    private final AccountSecurityPort security;

    private final AccountLoginAttemptMapper loginAttemptMapper;

    private final AccountMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final AccountOperationPort idempotencyService;
    private final AccountCaptchaService captchas;
    private final AccountAccessService accessService;
    private final PasswordHasher passwordHasher;
    private final String dummyHash;

    public AccountAuthenticationService(
            AccountAuditPort audit,
            AccountSecurityPort security,
            AccountLoginAttemptMapper loginAttemptMapper,
            AccountMapper mapper,
            PlatformTransactionManager transactionManager,
            AccountOperationPort idempotencyService,
            AccountCaptchaService captchas,
            AccountAccessService accessService,
            PasswordHasher passwordHasher
    ) {
        this.audit = audit;
        this.security = security;
        this.loginAttemptMapper = loginAttemptMapper;
        this.mapper = mapper;
        this.transactionManager = transactionManager;
        this.idempotencyService = idempotencyService;
        this.captchas = captchas;
        this.accessService = accessService;
        this.passwordHasher = passwordHasher;
        dummyHash = passwordHasher.hash(SecureRandomUtils.token());
    }

    @Override
    public LoginGrant login(AccountContext context, LoginInput input) {
        AccountValidationUtils.username(input.username());
        AccountValidationUtils.password(input.password(), "password", false);
        AccountValidationUtils.captcha(input.captchaChallengeId(), input.captchaVerificationToken());
        AccountOperationPort.Execution<LoginGrant> previous = idempotencyService.existing(
                context, AccountOperationCode.LOGIN.code(), input, LoginGrant.class);
        if (previous != null) {
            LoginGrant replay = withReplay(previous.data(), true);
            return replay;
        }
        if (context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        String usernameKey = input.username().toLowerCase(Locale.ROOT);
        TransactionUtils.independent(transactionManager, () -> {
            mapper.lockScope("login:" + usernameKey);
            currentAttempt(usernameKey);
            return Boolean.TRUE;
        });
        captchas.consume(context, "LOGIN", input.captchaChallengeId(), input.captchaVerificationToken(), input);
        AccountState candidate = mapper.findByLoginName(usernameKey);
        String encoded = candidate == null ? dummyHash : candidate.passwordHash();
        boolean matches = passwordHasher.matches(input.password(), encoded);
        AccountOperationPort.Execution<LoginGrant> execution = idempotencyService.execute(
                context, AccountOperationCode.LOGIN.code(), input, LoginGrant.class,
                () -> authenticateLocked(context, usernameKey, candidate, matches)
        );
        LoginGrant result = withReplay(execution.data(), execution.replayed());
        return result;
    }

    private AccountOperationPort.Outcome<LoginGrant> authenticateLocked(
            AccountContext context, String usernameKey, AccountState candidate, boolean matches
    ) {
        mapper.lockScope("login:" + usernameKey);
        LoginAttempt attempt = currentAttempt(usernameKey);
        AccountState account = mapper.findByLoginName(usernameKey);
        if (account != null) {
            security.lockAccount(account.id());
            account = mapper.findByLoginName(usernameKey);
        }
        boolean stale = candidate == null || account == null
                || !candidate.id().equals(account.id())
                || candidate.credentialVersion() != account.credentialVersion()
                || candidate.loginNameVersion() != account.loginNameVersion();
        if (!matches || stale) {
            int failures = Math.min(LoginConstants.MAX_FAILURES, attempt.failureCount() + 1);
            OffsetDateTime lockedUntil = failures == LoginConstants.MAX_FAILURES ? mapper.now().plusMinutes(LoginConstants.LOCK_MINUTES) : null;
            loginAttemptMapper.saveLoginAttempt(usernameKey, failures, lockedUntil);
            audit.record(context, account == null ? null : account.id(), "ACCOUNT_LOGIN", "REJECTED");
            BusinessException error = failures == LoginConstants.MAX_FAILURES
                    ? new AccountException(IdentityErrorCode.ACCOUNT_LOCKED, Map.of("retryAfterSeconds", Integer.toString(LoginConstants.LOCK_SECONDS)))
                    : new BusinessException(IdentityErrorCode.CREDENTIALS_INVALID);
            AccountOperationPort.Outcome<LoginGrant> outcome = AccountOperationPort.failure(error);
            return outcome;
        }
        accessService.requireAvailable(account);
        loginAttemptMapper.saveLoginAttempt(usernameKey, 0, null);
        audit.record(context, account.id(), "ACCOUNT_LOGIN", "SUCCESS");
        AccountOperationPort.Outcome<LoginGrant> outcome = AccountOperationPort.success(grant(account));
        return outcome;
    }

    private LoginAttempt currentAttempt(String usernameKey) {
        LoginAttempt attempt = loginAttemptMapper.loginAttempt(usernameKey);
        if (attempt == null) {
            return new LoginAttempt(0, null);
        }
        if (attempt.lockedUntil() != null) {
            OffsetDateTime now = mapper.now();
            if (now.isBefore(attempt.lockedUntil())) {
                long seconds = Math.max(1, Duration.between(now, attempt.lockedUntil()).toSeconds() + 1);
                throw new AccountException(IdentityErrorCode.ACCOUNT_LOCKED,
                        Map.of("retryAfterSeconds", Long.toString(seconds)));
            }
            loginAttemptMapper.saveLoginAttempt(usernameKey, 0, null);
            return new LoginAttempt(0, null);
        }
        return attempt;
    }

    @Override
    public LoginGrant validateSession(UUID userId, long credentialVersion, long securityVersion) {
        AccountState state = mapper.findState(userId);
        accessService.requireSession(state, credentialVersion, securityVersion);
        LoginGrant result = grant(state);
        return result;
    }

    private LoginGrant grant(AccountState state) {
        LoginGrant grant = new LoginGrant(
                state.id(), state.username(), state.nickname(), state.role(),
                state.credentialVersion(), state.securityVersion(), false
        );
        return grant;
    }

    private LoginGrant withReplay(LoginGrant grant, boolean replayed) {
        LoginGrant result = new LoginGrant(
                grant.accountId(), grant.username(), grant.nickname(), grant.role(),
                grant.credentialVersion(), grant.securityVersion(), replayed
        );
        return result;
    }
}
