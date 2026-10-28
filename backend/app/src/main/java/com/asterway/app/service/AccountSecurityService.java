package com.asterway.app.service;

import com.asterway.app.constant.AccountLedgerConstants;
import com.asterway.app.mapper.AccountSecurityMapper;
import com.asterway.app.security.AccountLedgerStore;
import com.asterway.app.security.AccountLedgerStore.Entry;
import com.asterway.common.api.CryptographyApi;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.TransactionUtils;
import com.asterway.identity.api.AccountSecurityPort;
import com.asterway.identity.api.AccountSecurityQueryApi;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSecurityState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 负责账户安全核验、护栏和提交结果协调；文件格式与文件锁由账本存储组件管理。
@Service
public class AccountSecurityService implements AccountSecurityPort {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityService.class);

    private final AccountLedgerStore store;
    private final CryptographyApi cryptography;
    private final AccountSecurityMapper mapper;
    private final AccountSecurityQueryApi securityQueries;
    private final PlatformTransactionManager transactionManager;
    private final Map<UUID, String> expected = new HashMap<>();
    private final Map<UUID, Entry> pending = new HashMap<>();
    private final Set<UUID> inFlight = new HashSet<>();
    private boolean initialized;
    private boolean ready;

    public AccountSecurityService(
            AccountLedgerStore store,
            CryptographyApi cryptography,
            AccountSecurityMapper mapper,
            AccountSecurityQueryApi securityQueries,
            PlatformTransactionManager transactionManager
    ) {
        this.store = store;
        this.cryptography = cryptography;
        this.mapper = mapper;
        this.securityQueries = securityQueries;
        this.transactionManager = transactionManager;
        store.replay(this::accept);
    }

    public synchronized void initialize() {
        List<AccountSecurityState> states = securityQueries.list();
        if (!initialized) {
            if (mapper.securityHistoryExists()) {
                throw new IllegalStateException("数据库有安全变更历史但独立账本缺失，拒绝自动建立新基线");
            }
            for (AccountSecurityState state : states) {
                append(UUID.randomUUID(), state.id(), AccountLedgerConstants.BASELINE, null, proof(state));
            }
            append(UUID.randomUUID(), null, AccountLedgerConstants.INITIALIZED, null, null);
        }
        for (Entry entry : List.copyOf(pending.values())) {
            reconcile(entry);
        }
        for (AccountSecurityState state : states) {
            if (!matches(expected.get(state.id()), proof(state))) {
                mapper.isolateAccount(state.id());
                log.warn("账户安全状态与独立账本不一致，accountId={}", state.id());
            }
        }
        ready = true;
    }

    public synchronized void verify(AccountSecurityState state) {
        if (!ready) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        for (Entry entry : List.copyOf(pending.values())) {
            if (state.id().equals(entry.userId())) {
                if (inFlight.contains(entry.operationId())) {
                    throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
                }
                reconcile(entry);
            }
        }
        if (!matches(expected.get(state.id()), proof(state))) {
            throw new BusinessException(IdentityErrorCode.ACCOUNT_UNAVAILABLE);
        }
    }

    public synchronized void prepare(
            AccountContext context,
            String type,
            AccountSecurityState before,
            AccountSecurityState after
    ) {
        UUID operationId = UUID.randomUUID();
        String beforeProof = before == null ? null : proof(before);
        Entry entry = append(operationId, after.id(), AccountLedgerConstants.PREPARE, beforeProof, proof(after));
        inFlight.add(operationId);

        // 提交结果通过数据库重新核实；保存点回滚的意图也不能误记为成功。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                synchronized (AccountSecurityService.this) {
                    reconcile(entry);
                }
            }

            @Override
            public void afterCompletion(int status) {
                synchronized (AccountSecurityService.this) {
                    if (status == STATUS_ROLLED_BACK) {
                        resolve(entry, AccountLedgerConstants.ABORT);
                    }
                    inFlight.remove(entry.operationId());
                }
            }
        });
        mapper.recordSecurityChange(operationId, context, type, before, after);
    }

    private void reconcile(Entry entry) {
        if (!pending.containsKey(entry.operationId())) {
            return;
        }
        AccountSecurityState current = securityQueries.find(entry.userId());
        String currentProof = current == null ? null : proof(current);
        boolean committed = mapper.securityChangeCommitted(entry.operationId());
        if (committed && matches(entry.afterProof(), currentProof)) {
            resolve(entry, AccountLedgerConstants.COMMIT);
            TransactionUtils.independent(transactionManager, () -> {
                mapper.markLedgerSynced(entry.operationId());
                return Boolean.TRUE;
            });
        } else if (!committed && matches(entry.beforeProof(), currentProof)) {
            resolve(entry, AccountLedgerConstants.ABORT);
        } else {
            throw new BusinessException(IdentityErrorCode.RESULT_UNKNOWN);
        }
    }

    private void resolve(Entry entry, String stage) {
        if (!pending.containsKey(entry.operationId())) {
            return;
        }
        append(entry.operationId(), entry.userId(), stage, entry.beforeProof(), entry.afterProof());
    }

    private String proof(AccountSecurityState state) {
        SecurityProof proof = new SecurityProof(
                state.id(), state.usernameKey(), state.passwordHash(), state.role(), state.status(),
                state.deletedAt(),
                state.securityVersion(), state.credentialVersion(), state.loginNameVersion()
        );
        String digest = cryptography.digestHex("security-proof", proof);
        return digest;
    }

    private Entry append(UUID operationId, UUID userId, String stage, String beforeProof, String afterProof) {
        Entry entry = store.append(operationId, userId, stage, beforeProof, afterProof);
        accept(entry);
        return entry;
    }

    private void accept(Entry entry) {
        switch (entry.stage()) {
            case AccountLedgerConstants.BASELINE, AccountLedgerConstants.COMMIT -> {
                expected.put(entry.userId(), entry.afterProof());
                pending.remove(entry.operationId());
            }
            case AccountLedgerConstants.PREPARE -> pending.put(entry.operationId(), entry);
            case AccountLedgerConstants.ABORT -> pending.remove(entry.operationId());
            case AccountLedgerConstants.INITIALIZED -> initialized = true;
            default -> throw new IllegalStateException("未知的账本记录类型");
        }
    }

    private boolean matches(String expectedValue, String actualValue) {
        if (expectedValue == null || actualValue == null) {
            return expectedValue == null && actualValue == null;
        }
        boolean equal = MessageDigest.isEqual(
                expectedValue.getBytes(StandardCharsets.US_ASCII), actualValue.getBytes(StandardCharsets.US_ASCII));
        return equal;
    }

    @Override
    public UUID lockAccount(UUID userId) {
        return mapper.lockAccount(userId);
    }

    @Override
    public String recoveryState(UUID userId) {
        return mapper.recoveryState(userId);
    }

    @Override
    public void insertGuard(UUID userId) {
        mapper.insertGuard(userId);
    }

    private record SecurityProof(
            UUID id, String usernameKey, String passwordHash, String role, String status,
            String deletedAt, long securityVersion, long credentialVersion, long loginNameVersion
    ) {
    }
}
