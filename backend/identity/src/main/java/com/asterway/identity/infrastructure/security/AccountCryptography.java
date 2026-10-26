package com.asterway.identity.infrastructure.security;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.infrastructure.serialization.AccountJsonCodec;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;

// 提供账户摘要和挑战材料加解密，保留既有用途前缀、JSON 编码及密文布局。
@Component
public class AccountCryptography {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int MIN_ENCRYPTED_BYTES = NONCE_BYTES + TAG_BITS / Byte.SIZE;

    private final AccountKeyStore keyStore;
    private final AccountJsonCodec jsonCodec;

    public AccountCryptography(AccountKeyStore keyStore, AccountJsonCodec jsonCodec) {
        this.keyStore = keyStore;
        this.jsonCodec = jsonCodec;
    }

    public byte[] digest(String purpose, Object value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(keyStore.signingKey());
            mac.update(purpose.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            byte[] digest = mac.doFinal(jsonCodec.write(value).getBytes(StandardCharsets.UTF_8));
            return digest;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("安全摘要初始化失败", exception);
        }
    }

    public String digestHex(String purpose, Object value) {
        String digest = HexFormat.of().formatHex(digest(purpose, value));
        return digest;
    }

    public byte[] encrypt(Object value) {
        try {
            byte[] nonce = SecureTokenGenerator.randomBytes(NONCE_BYTES);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.encryptionKey(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] content = cipher.doFinal(jsonCodec.write(value).getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = ByteBuffer.allocate(nonce.length + content.length)
                    .put(nonce)
                    .put(content)
                    .array();
            return encrypted;
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("挑战材料加密失败", exception);
        }
    }

    public <T> T decrypt(byte[] encrypted, Class<T> type) {
        if (encrypted == null || encrypted.length < MIN_ENCRYPTED_BYTES) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            byte[] nonce = Arrays.copyOfRange(encrypted, 0, NONCE_BYTES);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keyStore.encryptionKey(), new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(encrypted, NONCE_BYTES, encrypted.length - NONCE_BYTES);
            T value = jsonCodec.read(new String(plain, StandardCharsets.UTF_8), type);
            return value;
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, exception);
        }
    }
}
