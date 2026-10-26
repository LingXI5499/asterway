package com.asterway.identity.service;

import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.LogoutResult;
import com.asterway.identity.infrastructure.security.AccountCryptography;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

// 退出只处理当前会话，幂等意图绑定原会话，避免迟到请求注销新登录身份。
@Service
public class AccountLogoutService {

    private final AccountLifecycleMapper mapper;
    private final AccountIdempotencyService idempotencyService;
    private final AccountAccessService accessService;
    private final AccountValidator validator;
    private final AccountCryptography cryptography;

    public AccountLogoutService(
            AccountLifecycleMapper mapper,
            AccountIdempotencyService idempotencyService,
            AccountAccessService accessService,
            AccountValidator validator,
            AccountCryptography cryptography
    ) {
        this.mapper = mapper;
        this.idempotencyService = idempotencyService;
        this.accessService = accessService;
        this.validator = validator;
        this.cryptography = cryptography;
    }

    public LogoutResult logout(Context context, Boolean confirmed, String sessionId) {
        validator.confirmed(confirmed);
        String sessionBinding = cryptography.digestHex("logout-session", sessionId == null ? "anonymous" : sessionId);
        LogoutIntent intent = new LogoutIntent(confirmed, sessionBinding);
        AccountIdempotencyService.Execution<LogoutResult> execution = idempotencyService.execute(
                context, "API-AUTH-009", intent, LogoutResult.class,
                () -> AccountIdempotencyService.success(logoutLocked(context, sessionId))
        );
        return execution.data();
    }

    private LogoutResult logoutLocked(Context context, String sessionId) {
        if (mapper.logoutKeyUsedElsewhere(context)) {
            throw new BusinessException(IdentityErrorCode.STATE_CONFLICT);
        }
        if (context.userId() != null) {
            AccountState account = accessService.locked(context);
            accessService.requireNoUnfinished(account.id());
            mapper.audit(context, account.id(), "ACCOUNT_LOGOUT", "SUCCESS");
        }
        if (sessionId != null) {
            mapper.deleteCurrentSessionAttributes(sessionId);
            mapper.deleteCurrentSession(sessionId);
        }
        LogoutResult result = new LogoutResult(true);
        return result;
    }

    private record LogoutIntent(Boolean confirmed, String sessionBinding) {
    }
}
