package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountPasswordApi;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.AccountSessionPort;
import com.asterway.identity.api.PasswordHasher;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.PasswordChangeResult;
import com.asterway.identity.api.model.PasswordInput;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.utils.AccountValidationUtils;
import org.springframework.stereotype.Service;

// 负责密码校验、变更和全部旧会话撤销，密码哈希计算放在账户锁之外。
@Service
public class AccountPasswordService implements AccountPasswordApi {

    private final AccountSessionPort sessions;

    private final AccountAuditPort audit;

    private final AccountMapper mapper;
    private final AccountOperationPort idempotencyService;
    private final AccountAccessService accessService;
    private final AccountTimeZoneService timeZones;
    private final AccountSecurityPort ledger;
    private final PasswordHasher passwordHasher;

    public AccountPasswordService(
            AccountSessionPort sessions,
            AccountAuditPort audit,
            AccountMapper mapper,
            AccountOperationPort idempotencyService,
            AccountAccessService accessService,
            AccountTimeZoneService timeZones,
            AccountSecurityPort ledger,
            PasswordHasher passwordHasher
    ) {
        this.sessions = sessions;
        this.audit = audit;
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.timeZones = timeZones;
        this.ledger = ledger;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public PasswordChangeResult changePassword(AccountContext context, PasswordInput input) {
        long expectedVersion = AccountValidationUtils.version(input.expectedVersion());
        AccountValidationUtils.confirmed(input.confirmed());
        AccountValidationUtils.text(input.currentPassword(), "currentPassword", 1, 1024);
        AccountValidationUtils.password(input.newPassword(), "newPassword", true);
        AccountValidationUtils.matchingPasswords(input.newPassword(), input.confirmPassword());
        if (input.newPassword().equals(input.currentPassword())) {
            AccountValidationUtils.invalid("newPassword", "新密码不能与原密码相同");
        }
        timeZones.applyDue(context);
        AccountState before = mapper.findState(context.userId());
        accessService.requireSession(before, context.credentialVersion(), context.securityVersion());
        if (!passwordHasher.matches(input.currentPassword(), before.passwordHash())) {
            throw new BusinessException(IdentityErrorCode.CURRENT_PASSWORD_INVALID);
        }
        String encoded = passwordHasher.hash(input.newPassword());
        AccountOperationPort.Execution<PasswordChangeResult> execution = idempotencyService.execute(
                context, AccountOperationCode.CHANGE_PASSWORD.code(), input, PasswordChangeResult.class,
                () -> AccountOperationPort.success(changePasswordLocked(context, before, expectedVersion, encoded))
        );
        return execution.data();
    }

    private PasswordChangeResult changePasswordLocked(
            AccountContext context, AccountState checked, long expectedVersion, String encoded
    ) {
        AccountState before = accessService.locked(context);
        accessService.requireVersion(before, expectedVersion);
        if (checked.credentialVersion() != before.credentialVersion()) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        accessService.requireNoUnfinished(before.id());
        ledger.prepare(context, "PASSWORD", before.securityState(), before.withPasswordHash(encoded).securityState());
        mapper.updatePassword(before.id(), encoded);
        sessions.revokeAll(before.id());
        audit.record(context, before.id(), "ACCOUNT_PASSWORD_CHANGE", "SUCCESS");
        PasswordChangeResult result = new PasswordChangeResult(true, true, mapper.now().toInstant());
        return result;
    }
}
