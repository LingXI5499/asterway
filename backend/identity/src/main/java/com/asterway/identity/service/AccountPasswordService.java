package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.PasswordChangeResult;
import com.asterway.identity.api.model.AccountModels.PasswordInput;
import com.asterway.identity.infrastructure.security.AccountSecurityLedger;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

// 负责密码校验、变更和全部旧会话撤销，密码哈希计算放在账户锁之外。
@Service
public class AccountPasswordService {

    private final AccountLifecycleMapper mapper;
    private final AccountIdempotencyService idempotencyService;
    private final AccountAccessService accessService;
    private final AccountTimeZoneService timeZones;
    private final AccountSecurityLedger ledger;
    private final AccountValidator validator;
    private final PasswordHasher passwordHasher;

    public AccountPasswordService(
            AccountLifecycleMapper mapper,
            AccountIdempotencyService idempotencyService,
            AccountAccessService accessService,
            AccountTimeZoneService timeZones,
            AccountSecurityLedger ledger,
            AccountValidator validator,
            PasswordHasher passwordHasher
    ) {
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.timeZones = timeZones;
        this.ledger = ledger;
        this.validator = validator;
        this.passwordHasher = passwordHasher;
    }

    public PasswordChangeResult changePassword(Context context, PasswordInput input) {
        long expectedVersion = validator.version(input.expectedVersion());
        validator.confirmed(input.confirmed());
        validator.text(input.currentPassword(), "currentPassword", 1, 1024);
        validator.password(input.newPassword(), "newPassword", true);
        validator.matchingPasswords(input.newPassword(), input.confirmPassword());
        if (input.newPassword().equals(input.currentPassword())) {
            validator.invalid("newPassword", "新密码不能与原密码相同");
        }
        timeZones.applyDue(context);
        AccountState before = mapper.findState(context.userId());
        accessService.requireSession(before, context.credentialVersion(), context.securityVersion());
        if (!passwordHasher.matches(input.currentPassword(), before.passwordHash())) {
            throw new BusinessException(IdentityErrorCode.CURRENT_PASSWORD_INVALID);
        }
        String encoded = passwordHasher.hash(input.newPassword());
        AccountIdempotencyService.Execution<PasswordChangeResult> execution = idempotencyService.execute(
                context, "API-AUTH-008", input, PasswordChangeResult.class,
                () -> AccountIdempotencyService.success(changePasswordLocked(context, before, expectedVersion, encoded))
        );
        return execution.data();
    }

    private PasswordChangeResult changePasswordLocked(
            Context context, AccountState checked, long expectedVersion, String encoded
    ) {
        AccountState before = accessService.locked(context);
        accessService.requireVersion(before, expectedVersion);
        if (checked.credentialVersion() != before.credentialVersion()) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        accessService.requireNoUnfinished(before.id());
        ledger.prepare(context, "PASSWORD", before, before.withPasswordHash(encoded));
        mapper.updatePassword(before.id(), encoded);
        mapper.deleteAccountSessionAttributes(before.id());
        mapper.deleteAccountSessions(before.id());
        mapper.audit(context, before.id(), "ACCOUNT_PASSWORD_CHANGE", "SUCCESS");
        PasswordChangeResult result = new PasswordChangeResult(true, true, mapper.now().toInstant());
        return result;
    }
}
