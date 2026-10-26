package com.asterway.identity.infrastructure.security;

import java.security.SecureRandom;
import java.util.HexFormat;

// 生成验证码随机位置、私有密钥和验证令牌，不承担文件存储或加解密职责。
public final class SecureTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SecureTokenGenerator() {
    }

    public static byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static int randomInt(int bound) {
        int value = RANDOM.nextInt(bound);
        return value;
    }

    public static String token() {
        String token = HexFormat.of().formatHex(randomBytes(TOKEN_BYTES));
        return token;
    }
}
