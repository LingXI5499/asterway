package com.asterway.identity.service;

import com.asterway.common.api.CryptographyApi;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountAuditPort;
import com.asterway.identity.api.AccountLogoutApi;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.AccountOperationPort;
import com.asterway.identity.api.AccountSessionPort;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.LogoutResult;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.utils.AccountValidationUtils;
import org.springframework.stereotype.Service;

// 退出只处理当前会话，幂等意图绑定原会话，避免迟到请求注销新登录身份。
@Service
public class AccountLogoutService implements AccountLogoutApi {

    private final AccountSessionPort sessions;

    private final AccountAuditPort audit;

    private final AccountOperationPort idempotencyService;
    private final AccountAccessService accessService;
    private final CryptographyApi cryptography;

    public AccountLogoutService(
            AccountSessionPort sessions,
            AccountAuditPort audit,
            AccountOperationPort idempotencyService,
            AccountAccessService accessService,
            CryptographyApi cryptography
    ) {
        this.sessions = sessions;
        this.audit = audit;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.cryptography = cryptography;
    }

    @Override
    public LogoutResult logout(AccountContext context, Boolean confirmed, String sessionId) {
        AccountValidationUtils.confirmed(confirmed);
        String sessionBinding = cryptography.digestHex("logout-session", sessionId == null ? "anonymous" : sessionId);
        LogoutIntent intent = new LogoutIntent(confirmed, sessionBinding);
        AccountOperationPort.Execution<LogoutResult> execution = idempotencyService.execute(
                context, AccountOperationCode.LOGOUT.code(), intent, LogoutResult.class,
                () -> AccountOperationPort.success(logoutLocked(context, sessionId))
        );
        return execution.data();
    }

    private LogoutResult logoutLocked(AccountContext context, String sessionId) {
        if (idempotencyService.logoutKeyUsedElsewhere(context)) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        if (context.userId() != null) {
            AccountState account = accessService.locked(context);
            accessService.requireNoUnfinished(account.id());
            audit.record(context, account.id(), "ACCOUNT_LOGOUT", "SUCCESS");
        }
        if (sessionId != null) {
            sessions.revokeCurrent(sessionId);
        }
        LogoutResult result = new LogoutResult(true);
        return result;
    }

    private record LogoutIntent(Boolean confirmed, String sessionBinding) {
    }
}
