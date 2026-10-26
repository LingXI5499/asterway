package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.LoginGrant;
import com.asterway.identity.api.model.AccountModels.LoginInput;
import com.asterway.identity.infrastructure.security.SecureTokenGenerator;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.LoginAttempt;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// 每个有效认证意图只累计一次失败，滑动错误不累计凭据失败。
@Service
public class AccountAuthenticationService {

    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final AccountIdempotencyService idempotencyService;
    private final AccountCaptchaService captchas;
    private final AccountAccessService accessService;
    private final AccountValidator validator;
    private final PasswordHasher passwordHasher;
    private final String dummyHash;

    public AccountAuthenticationService(
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor,
            AccountIdempotencyService idempotencyService,
            AccountCaptchaService captchas,
            AccountAccessService accessService,
            AccountValidator validator,
            PasswordHasher passwordHasher
    ) {
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        this.idempotencyService = idempotencyService;
        this.captchas = captchas;
        this.accessService = accessService;
        this.validator = validator;
        this.passwordHasher = passwordHasher;
        dummyHash = passwordHasher.hash(SecureTokenGenerator.token());
    }

    public LoginGrant login(Context context, LoginInput input) {
        validator.username(input.username());
        validator.password(input.password(), "password", false);
        validator.captcha(input.captchaChallengeId(), input.captchaVerificationToken());
        AccountIdempotencyService.Execution<LoginGrant> previous = idempotencyService.existing(
                context, "API-AUTH-005", input, LoginGrant.class);
        if (previous != null) {
            LoginGrant replay = withReplay(previous.data(), true);
            return replay;
        }
        if (context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        String usernameKey = input.username().toLowerCase(Locale.ROOT);
        transactionExecutor.independent(() -> {
            mapper.lockScope("login:" + usernameKey);
            currentAttempt(usernameKey);
            return Boolean.TRUE;
        });
        captchas.consume(context, "LOGIN", input.captchaChallengeId(), input.captchaVerificationToken(), input);
        AccountState candidate = mapper.findByLoginName(usernameKey);
        String encoded = candidate == null ? dummyHash : candidate.passwordHash();
        boolean matches = passwordHasher.matches(input.password(), encoded);
        AccountIdempotencyService.Execution<LoginGrant> execution = idempotencyService.execute(
                context, "API-AUTH-005", input, LoginGrant.class,
                () -> authenticateLocked(context, usernameKey, candidate, matches)
        );
        LoginGrant result = withReplay(execution.data(), execution.replayed());
        return result;
    }

    private AccountIdempotencyService.Outcome<LoginGrant> authenticateLocked(
            Context context, String usernameKey, AccountState candidate, boolean matches
    ) {
        mapper.lockScope("login:" + usernameKey);
        LoginAttempt attempt = currentAttempt(usernameKey);
        AccountState account = mapper.findByLoginName(usernameKey);
        if (account != null) {
            mapper.lockAccount(account.id());
            account = mapper.findByLoginName(usernameKey);
        }
        boolean stale = candidate == null || account == null
                || !candidate.id().equals(account.id())
                || candidate.credentialVersion() != account.credentialVersion()
                || candidate.loginNameVersion() != account.loginNameVersion();
        if (!matches || stale) {
            int failures = Math.min(5, attempt.failureCount() + 1);
            OffsetDateTime lockedUntil = failures == 5 ? mapper.now().plusMinutes(5) : null;
            mapper.saveLoginAttempt(usernameKey, failures, lockedUntil);
            mapper.audit(context, account == null ? null : account.id(), "ACCOUNT_LOGIN", "REJECTED");
            BusinessException error = failures == 5
                    ? new AccountException(IdentityErrorCode.ACCOUNT_LOCKED, Map.of("retryAfterSeconds", "300"))
                    : new BusinessException(IdentityErrorCode.CREDENTIALS_INVALID);
            AccountIdempotencyService.Outcome<LoginGrant> outcome = AccountIdempotencyService.failure(error);
            return outcome;
        }
        accessService.requireAvailable(account);
        mapper.saveLoginAttempt(usernameKey, 0, null);
        mapper.audit(context, account.id(), "ACCOUNT_LOGIN", "SUCCESS");
        AccountIdempotencyService.Outcome<LoginGrant> outcome = AccountIdempotencyService.success(grant(account));
        return outcome;
    }

    private LoginAttempt currentAttempt(String usernameKey) {
        LoginAttempt attempt = mapper.loginAttempt(usernameKey);
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
            mapper.saveLoginAttempt(usernameKey, 0, null);
            return new LoginAttempt(0, null);
        }
        return attempt;
    }

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
