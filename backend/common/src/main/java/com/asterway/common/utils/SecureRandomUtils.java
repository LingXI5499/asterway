package com.asterway.common.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

// 生成验证码随机位置、私有密钥和验证令牌，不承担文件存储或加解密职责。
public final class SecureRandomUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private SecureRandomUtils() {
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
