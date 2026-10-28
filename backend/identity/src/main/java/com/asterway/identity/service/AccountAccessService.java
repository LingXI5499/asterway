package com.asterway.identity.service;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountExecutionPort;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.constant.AccountConstants;
import com.asterway.identity.mapper.AccountMapper;
import com.asterway.identity.model.AccountState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

// 集中处理账户锁、会话资格、版本及未结束执行等业务前置条件。
@Service
public class AccountAccessService {

    private final AccountMapper mapper;
    private final AccountSecurityPort ledger;
    private final AccountExecutionPort executions;

    public AccountAccessService(
            AccountMapper mapper,
            AccountSecurityPort ledger,
            AccountExecutionPort executions
    ) {
        this.mapper = mapper;
        this.ledger = ledger;
        this.executions = executions;
    }

    public AccountState locked(AccountContext context) {
        if (context.userId() == null || ledger.lockAccount(context.userId()) == null) {
            throw new BusinessException(CommonErrorCode.AUTH_REQUIRED);
        }
        AccountState account = mapper.findState(context.userId());
        requireSession(account, context.credentialVersion(), context.securityVersion());
        return account;
    }

    public void requireAvailable(AccountState account) {
        if (account == null) {
            throw new BusinessException(CommonErrorCode.AUTH_REQUIRED);
        }
        if (account.deletedAt() != null || !AccountConstants.ENABLED_STATUS.equals(account.status())
                || !AccountConstants.CLEAR_RECOVERY_STATE.equals(ledger.recoveryState(account.id()))) {
            throw new BusinessException(IdentityErrorCode.ACCOUNT_UNAVAILABLE);
        }
        ledger.verify(account.securityState());
    }

    public void requireSession(AccountState account, long credentialVersion, long securityVersion) {
        requireAvailable(account);
        if (account.credentialVersion() != credentialVersion || account.securityVersion() != securityVersion) {
            throw new BusinessException(CommonErrorCode.AUTH_REQUIRED);
        }
    }

    public void requireVersion(AccountState account, long expectedVersion) {
        if (account.version() != expectedVersion) {
            throw new AccountException(IdentityErrorCode.VERSION_CONFLICT,
                    Map.of("currentVersion", Long.toString(account.version())));
        }
    }

    public void requireNoUnfinished(UUID userId) {
        int count = executions.unfinishedCount(userId);
        if (count > 0) {
            Map<String, Object> data = Map.of(
                    "unfinishedExecutionCount", count,
                    "unfinishedExecutions", executions.unfinishedHints(userId),
                    "unfinishedExecutionsHasMore", count > 100
            );
            throw new AccountException(IdentityErrorCode.UNFINISHED_EXECUTION, data);
        }
    }
}
