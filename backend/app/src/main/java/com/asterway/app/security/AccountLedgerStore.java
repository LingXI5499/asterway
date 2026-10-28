package com.asterway.app.security;

import com.asterway.app.constant.AccountLedgerConstants;
import com.asterway.common.api.CryptographyApi;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

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
import java.util.UUID;
import java.util.function.Consumer;

// 仅负责文件锁、签名链读取和追加落盘，不查询账户或判断数据库提交结果。
@Component
public class AccountLedgerStore implements AutoCloseable {

    private final CryptographyApi cryptography;
    private final ObjectMapper objectMapper;
    private final Path file;
    private final FileChannel lockChannel;
    private final FileLock fileLock;
    private String tail = AccountLedgerConstants.ROOT_MAC;
    private boolean replayed;

    public AccountLedgerStore(
            CryptographyApi cryptography,
            AccountKeyStore keyStore,
            ObjectMapper objectMapper
    ) {
        this.cryptography = cryptography;
        this.objectMapper = objectMapper;
        file = keyStore.privateDirectory().resolve("security-ledger.jsonl");
        FileChannel channel = null;
        FileLock lock;
        try {
            channel = FileChannel.open(keyStore.privateDirectory().resolve("identity.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = channel.tryLock();
            if (lock == null) {
                throw new IllegalStateException("账户私有材料目录正由其他应用实例使用");
            }
        } catch (IOException | RuntimeException exception) {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            throw new IllegalStateException("账户安全账本无法读取", exception);
        }
        lockChannel = channel;
        fileLock = lock;
    }

    public synchronized Entry append(
            UUID operationId, UUID userId, String stage, String beforeProof, String afterProof
    ) {
        if (!replayed) {
            throw new IllegalStateException("账本尚未完成初始重放");
        }
        Entry entry = new Entry(operationId, userId, stage, beforeProof, afterProof, tail);
        String body = JsonUtils.write(objectMapper, entry);
        String signature = cryptography.digestHex(AccountLedgerConstants.SIGNATURE_PURPOSE, body);
        String line = JsonUtils.write(objectMapper, new SignedLine(body, signature)) + "\n";
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
        tail = signature;
        return entry;
    }

    // 逐条重放给业务服务，存储组件不长期缓存全部历史记录。
    public synchronized void replay(Consumer<Entry> consumer) {
        if (replayed) {
            throw new IllegalStateException("账本初始重放不能重复执行");
        }
        if (!Files.exists(file)) {
            replayed = true;
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                SignedLine signed = JsonUtils.read(objectMapper, line, SignedLine.class);
                String signature = cryptography.digestHex(AccountLedgerConstants.SIGNATURE_PURPOSE, signed.body());
                if (!matches(signature, signed.mac())) {
                    throw new IllegalStateException("账户安全账本签名校验失败");
                }
                Entry entry = JsonUtils.read(objectMapper, signed.body(), Entry.class);
                if (!tail.equals(entry.previousMac())) {
                    throw new IllegalStateException("账户安全账本记录顺序不完整");
                }
                consumer.accept(entry);
                tail = signed.mac();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("账户安全账本无法读取", exception);
        }
        replayed = true;
    }

    private boolean matches(String expected, String actual) {
        if (expected == null || actual == null) {
            return expected == null && actual == null;
        }
        boolean equal = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII), actual.getBytes(StandardCharsets.US_ASCII));
        return equal;
    }

    @Override
    public void close() throws IOException {
        try {
            fileLock.release();
        } finally {
            lockChannel.close();
        }
    }

    // 字段名和字段顺序沿用已有账本格式，禁止随包名整理改变持久化编码。
    public record Entry(
            UUID operationId, UUID userId, String stage, String beforeProof, String afterProof, String previousMac
    ) {
    }

    public record SignedLine(String body, String mac) {
    }
}
