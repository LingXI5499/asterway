package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.AccountConfigurationPort;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountRegistrationApi;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.RegistrationInput;
import com.asterway.identity.api.model.RegistrationResult;
import com.asterway.identity.constant.AccountConstants;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.mapper.AccountTimeZoneMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.model.ZonePeriod;
import com.asterway.identity.utils.AccountValidationUtils;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

// 注册原子建立普通账户、初始时区及账户控制行，不自动创建认证会话。
@Service
public class AccountRegistrationService implements AccountRegistrationApi {

    private final AccountConfigurationPort configuration;

    private final AccountAuditPort audit;

    private final AccountTimeZoneMapper timeZoneMapper;

    private final AccountMapper mapper;
    private final AccountOperationPort idempotencyService;
    private final AccountCaptchaService captchas;
    private final AccountSecurityPort ledger;
    private final PasswordHasher passwordHasher;

    public AccountRegistrationService(
            AccountConfigurationPort configuration,
            AccountAuditPort audit,
            AccountTimeZoneMapper timeZoneMapper,
            AccountMapper mapper,
            AccountOperationPort idempotencyService,
            AccountCaptchaService captchas,
            AccountSecurityPort ledger,
            PasswordHasher passwordHasher
    ) {
        this.configuration = configuration;
        this.audit = audit;
        this.timeZoneMapper = timeZoneMapper;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.captchas = captchas;
        this.ledger = ledger;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public RegistrationResult register(AccountContext context, RegistrationInput input) {
        if (context.userId() != null) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        AccountValidationUtils.username(input.username());
        if (input.nickname() != null) {
            AccountValidationUtils.nickname(input.nickname());
        }
        AccountValidationUtils.password(input.password(), "password", true);
        AccountValidationUtils.matchingPasswords(input.password(), input.confirmPassword());
        AccountValidationUtils.zone(input.zoneId());
        AccountValidationUtils.captcha(input.captchaChallengeId(), input.captchaVerificationToken());
        AccountOperationPort.Execution<RegistrationResult> existing = idempotencyService.existing(
                context, AccountOperationCode.REGISTER.code(), input, RegistrationResult.class);
        if (existing != null) {
            return existing.data();
        }
        if (!configuration.registrationOpen()) {
            throw new BusinessException(IdentityErrorCode.REGISTRATION_CLOSED);
        }
        captchas.consume(context, "REGISTER", input.captchaChallengeId(), input.captchaVerificationToken(), input);
        String passwordHash = passwordHasher.hash(input.password());
        AccountOperationPort.Execution<RegistrationResult> result = idempotencyService.execute(
                context, AccountOperationCode.REGISTER.code(), input, RegistrationResult.class,
                () -> AccountOperationPort.success(insertLocked(context, input, passwordHash))
        );
        return result.data();
    }

    private RegistrationResult insertLocked(AccountContext context, RegistrationInput input, String passwordHash) {
        if (!configuration.registrationOpen()) {
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
                AccountConstants.USER_ROLE,
                AccountConstants.ENABLED_STATUS,
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
        ledger.prepare(context, "REGISTER", null, account.securityState());
        if (mapper.insertState(account) != 1) {
            throw new IllegalStateException("账户新增影响行数异常");
        }
        timeZoneMapper.insertZone(zone);
        ledger.insertGuard(id);
        audit.record(context, id, "ACCOUNT_REGISTER", "SUCCESS");
        AccountState saved = mapper.findState(id);
        RegistrationResult result = new RegistrationResult(id, saved.username(), true, saved.createdAt().toInstant());
        return result;
    }
}
