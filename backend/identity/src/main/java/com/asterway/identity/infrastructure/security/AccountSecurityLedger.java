package com.asterway.identity.infrastructure.security;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.infrastructure.serialization.AccountJsonCodec;
import com.asterway.identity.infrastructure.transaction.AccountTransactionExecutor;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.model.AccountState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 独立于数据库备份的本地追加账本：先落意图、同库提交、再确认结果。
// 文件只有受保护摘要；缺失、篡改或未决状态不能通过“重置版本”自动放行。
@Component
public class AccountSecurityLedger implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityLedger.class);

    private final AccountCryptography cryptography;
    private final AccountJsonCodec jsonCodec;
    private final AccountLifecycleMapper mapper;
    private final AccountTransactionExecutor transactionExecutor;
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock fileLock;
    private final Map<UUID, String> expected = new HashMap<>();
    private final Map<UUID, Entry> pending = new HashMap<>();
    private final Set<UUID> inFlight = new HashSet<>();
    private String tail = "ROOT";
    private boolean initialized;
    private boolean ready;

    public AccountSecurityLedger(
            AccountCryptography cryptography,
            AccountKeyStore keyStore,
            AccountJsonCodec jsonCodec,
            AccountLifecycleMapper mapper,
            AccountTransactionExecutor transactionExecutor
    ) {
        this.cryptography = cryptography;
        this.jsonCodec = jsonCodec;
        this.mapper = mapper;
        this.transactionExecutor = transactionExecutor;
        file = keyStore.privateDirectory().resolve("security-ledger.jsonl");
        try {
            lockChannel = FileChannel.open(keyStore.privateDirectory().resolve("identity.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            fileLock = lockChannel.tryLock();
            if (fileLock == null) {
                throw new IllegalStateException("账户私有材料目录正由其他应用实例使用");
            }
            load();
        } catch (IOException exception) {
            throw new IllegalStateException("账户安全账本无法读取", exception);
        }
    }

    public synchronized void initialize() {
        List<AccountState> states = mapper.listSecurityStates();
        if (!initialized) {
            if (mapper.securityHistoryExists()) {
                throw new IllegalStateException("数据库有安全变更历史但独立账本缺失，拒绝自动建立新基线");
            }
            for (AccountState state : states) {
                append(new Entry(UUID.randomUUID(), state.id(), "BASELINE", null, proof(state), tail));
            }
            append(new Entry(UUID.randomUUID(), null, "INITIALIZED", null, null, tail));
        }
        for (Entry entry : List.copyOf(pending.values())) {
            reconcile(entry);
        }
        for (AccountState state : states) {
            if (!matches(expected.get(state.id()), proof(state))) {
                mapper.isolateAccount(state.id());
                log.warn("账户安全状态与独立账本不一致，accountId={}", state.id());
            }
        }
        ready = true;
    }

    public synchronized void verify(AccountState state) {
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
            Context context,
            String type,
            AccountState before,
            AccountState after
    ) {
        UUID operationId = UUID.randomUUID();
        String beforeProof = before == null ? null : proof(before);
        Entry entry = new Entry(operationId, after.id(), "PREPARE", beforeProof, proof(after), tail);
        append(entry);
        inFlight.add(operationId);

        // 提交结果通过数据库重新核实；保存点回滚的意图也不能误记为成功。
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                synchronized (AccountSecurityLedger.this) {
                    reconcile(entry);
                }
            }

            @Override
            public void afterCompletion(int status) {
                synchronized (AccountSecurityLedger.this) {
                    if (status == STATUS_ROLLED_BACK) {
                        resolve(entry, "ABORT");
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
        AccountState current = mapper.findState(entry.userId());
        String currentProof = current == null ? null : proof(current);
        boolean committed = mapper.securityChangeCommitted(entry.operationId());
        if (committed && matches(entry.afterProof(), currentProof)) {
            resolve(entry, "COMMIT");
            transactionExecutor.independent(() -> {
                mapper.markLedgerSynced(entry.operationId());
                return Boolean.TRUE;
            });
        } else if (!committed && matches(entry.beforeProof(), currentProof)) {
            resolve(entry, "ABORT");
        } else {
            throw new BusinessException(IdentityErrorCode.RESULT_UNKNOWN);
        }
    }

    private void resolve(Entry entry, String stage) {
        if (!pending.containsKey(entry.operationId())) {
            return;
        }
        append(new Entry(entry.operationId(), entry.userId(), stage,
                entry.beforeProof(), entry.afterProof(), tail));
    }

    private String proof(AccountState state) {
        SecurityProof proof = new SecurityProof(
                state.id(), state.usernameKey(), state.passwordHash(), state.role(), state.status(),
                state.deletedAt() == null ? null : state.deletedAt().toInstant().toString(),
                state.securityVersion(), state.credentialVersion(), state.loginNameVersion()
        );
        String digest = cryptography.digestHex("security-proof", proof);
        return digest;
    }

    private void load() throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                SignedLine signed = jsonCodec.read(line, SignedLine.class);
                String signature = cryptography.digestHex("ledger", signed.body());
                if (!matches(signature, signed.mac())) {
                    throw new IllegalStateException("账户安全账本签名校验失败");
                }
                Entry entry = jsonCodec.read(signed.body(), Entry.class);
                if (!tail.equals(entry.previousMac())) {
                    throw new IllegalStateException("账户安全账本记录顺序不完整");
                }
                accept(entry);
                tail = signed.mac();
            }
        }
    }

    private void append(Entry entry) {
        String body = jsonCodec.write(entry);
        String signature = cryptography.digestHex("ledger", body);
        String line = jsonCodec.write(new SignedLine(body, signature)) + "\n";
        try (FileChannel channel = FileChannel.open(file,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, exception);
        }
        accept(entry);
        tail = signature;
    }

    private void accept(Entry entry) {
        switch (entry.stage()) {
            case "BASELINE", "COMMIT" -> {
                expected.put(entry.userId(), entry.afterProof());
                pending.remove(entry.operationId());
            }
            case "PREPARE" -> pending.put(entry.operationId(), entry);
            case "ABORT" -> pending.remove(entry.operationId());
            case "INITIALIZED" -> initialized = true;
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
    public void close() throws IOException {
        fileLock.release();
        lockChannel.close();
    }

    public record Entry(
            UUID operationId, UUID userId, String stage, String beforeProof, String afterProof, String previousMac
    ) {
    }

    public record SignedLine(String body, String mac) {
    }

    private record SecurityProof(
            UUID id, String usernameKey, String passwordHash, String role, String status,
            String deletedAt, long securityVersion, long credentialVersion, long loginNameVersion
    ) {
    }
}
