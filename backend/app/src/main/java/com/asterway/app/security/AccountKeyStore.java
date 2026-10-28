package com.asterway.app.security;

import com.asterway.common.utils.SecureRandomUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

// 管理稳定密钥及私有目录，沿用既有文件格式，不能随应用重启更换密钥。
@Component
public class AccountKeyStore {

    private static final int KEY_BYTES = 32;
    private static final String KEY_FILE = "identity.key";
    private static final String LEDGER_FILE = "security-ledger.jsonl";

    private final Path privateDirectory;
    private final SecretKey signingKey;
    private final SecretKey encryptionKey;

    public AccountKeyStore(
            @Value("${asterway.identity.private-directory:.asterway-private}") String directory
    ) {
        privateDirectory = Path.of(directory).toAbsolutePath().normalize();
        byte[] key = loadKey();
        signingKey = new SecretKeySpec(key, "HmacSHA256");
        encryptionKey = new SecretKeySpec(key, "AES");
    }

    public Path privateDirectory() {
        return privateDirectory;
    }

    public SecretKey signingKey() {
        return signingKey;
    }

    public SecretKey encryptionKey() {
        return encryptionKey;
    }

    private byte[] loadKey() {
        try {
            Files.createDirectories(privateDirectory);
            Path keyFile = privateDirectory.resolve(KEY_FILE);
            if (!Files.exists(keyFile)) {
                createKey(keyFile);
            }
            byte[] key = Files.readAllBytes(keyFile);
            if (key.length != KEY_BYTES) {
                throw new IllegalStateException("账户私有密钥格式无效");
            }
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("无法初始化账户私有材料目录", exception);
        }
    }

    private void createKey(Path keyFile) throws IOException {
        if (Files.exists(privateDirectory.resolve(LEDGER_FILE))) {
            throw new IllegalStateException("安全账本密钥缺失，禁止自动生成替代密钥");
        }
        byte[] newKey = SecureRandomUtils.randomBytes(KEY_BYTES);
        try (FileChannel channel = FileChannel.open(
                keyFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(newKey);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            channel.force(true);
        }
        if (keyFile.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(keyFile, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
