package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.RegistrationInput;
import com.asterway.identity.api.model.RegistrationResult;
import com.asterway.identity.infrastructure.security.AccountSecurityLedger;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

// 注册原子建立普通账户、初始时区及账户控制行，不自动创建认证会话。
@Service
public class AccountRegistrationService {

    private final AccountLifecycleMapper mapper;
    private final AccountIdempotencyService idempotencyService;
    private final AccountCaptchaService captchas;
    private final AccountValidator validator;
    private final AccountSecurityLedger ledger;
    private final PasswordHasher passwordHasher;

    public AccountRegistrationService(
            AccountLifecycleMapper mapper,
            AccountIdempotencyService idempotencyService,
            AccountCaptchaService captchas,
            AccountValidator validator,
            AccountSecurityLedger ledger,
            PasswordHasher passwordHasher
    ) {
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.captchas = captchas;
        this.validator = validator;
        this.ledger = ledger;
        this.passwordHasher = passwordHasher;
    }

    public RegistrationResult register(Context context, RegistrationInput input) {
        if (context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        validator.username(input.username());
        if (input.nickname() != null) {
            validator.nickname(input.nickname());
        }
        validator.password(input.password(), "password", true);
        validator.matchingPasswords(input.password(), input.confirmPassword());
        validator.zone(input.zoneId());
        validator.captcha(input.captchaChallengeId(), input.captchaVerificationToken());
        AccountIdempotencyService.Execution<RegistrationResult> existing = idempotencyService.existing(
                context, "API-AUTH-004", input, RegistrationResult.class);
        if (existing != null) {
            return existing.data();
        }
        if (!mapper.registrationOpen()) {
            throw new BusinessException(IdentityErrorCode.REGISTRATION_CLOSED);
        }
        captchas.consume(context, "REGISTER", input.captchaChallengeId(), input.captchaVerificationToken(), input);
        String passwordHash = passwordHasher.hash(input.password());
        AccountIdempotencyService.Execution<RegistrationResult> result = idempotencyService.execute(
                context, "API-AUTH-004", input, RegistrationResult.class,
                () -> AccountIdempotencyService.success(insertLocked(context, input, passwordHash))
        );
        return result.data();
    }

    private RegistrationResult insertLocked(Context context, RegistrationInput input, String passwordHash) {
        if (!mapper.registrationOpen()) {
            throw new BusinessException(IdentityErrorCode.REGISTRATION_CLOSED);
        }
        if (mapper.findByLoginName(input.username().toLowerCase(Locale.ROOT)) != null) {
            throw new BusinessException(IdentityErrorCode.USERNAME_TAKEN);
        }
        OffsetDateTime now = mapper.now();
        UUID id = UUID.randomUUID();
        ZonePeriod zone = ZonePeriod.create(id, 1, input.zoneId(), now, "INITIAL_DEVICE");
        String nickname = input.nickname() == null ? input.username() : input.nickname();
        AccountState account = new AccountState(
                id,
                input.username(),
                input.username().toLowerCase(Locale.ROOT),
                nickname,
                passwordHash,
                "USER",
                "ENABLED",
                now,
                now,
                0,
                null,
                1,
                1,
                1,
                now,
                zone.id(),
                null,
                null,
                null
        );
        ledger.prepare(context, "REGISTER", null, account);
        if (mapper.insertState(account) != 1) {
            throw new IllegalStateException("账户新增影响行数异常");
        }
        mapper.insertZone(zone);
        mapper.insertGuard(id);
        mapper.audit(context, id, "ACCOUNT_REGISTER", "SUCCESS");
        AccountState saved = mapper.findState(id);
        RegistrationResult result = new RegistrationResult(id, saved.username(), true, saved.createdAt().toInstant());
        return result;
    }
}
