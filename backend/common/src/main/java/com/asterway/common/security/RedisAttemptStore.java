package com.asterway.common.security;

import com.asterway.common.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 短期限流与凭据失败计数；验证码失败不调用凭据计数方法。 */
@Component
public class RedisAttemptStore {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local n=redis.call('incr',KEYS[1])
            if n==1 or n==tonumber(ARGV[2]) then redis.call('expire',KEYS[1],ARGV[1]) end
            return n
            """, Long.class);
    private final StringRedisTemplate redis;

    public RedisAttemptStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void limit(String action, String source, int maximum, int seconds) {
        String key = "asterway:identity:rate:" + action + ":" + digest(source == null ? "unknown" : source);
        Long count = redis.execute(INCREMENT, List.of(key), Integer.toString(seconds), Integer.toString(Integer.MAX_VALUE));
        if (count == null || count > maximum) {
            throw limited(seconds);
        }
    }

    public void requireUnlocked(String username) {
        String key = loginKey(username);
        String count = redis.opsForValue().get(key);
        if (count != null && Long.parseLong(count) >= 5) {
            Long remaining = redis.getExpire(key);
            throw limited(remaining == null ? 300 : Math.max(1, remaining));
        }
    }

    public void failedCredentials(String username) {
        redis.execute(INCREMENT, List.of(loginKey(username)), "300", "5");
    }

    public void clearFailures(String username) {
        redis.delete(loginKey(username));
    }

    private String loginKey(String username) {
        return "asterway:identity:login-failure:" + digest(username.toLowerCase(Locale.ROOT));
    }

    private BusinessException limited(long seconds) {
        return new BusinessException(429, "TOO_MANY_ATTEMPTS", "操作过于频繁，请稍后重试",
                Map.of("retryAfterSeconds", seconds));
    }

    private String digest(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("系统摘要算法不可用");
        }
    }
}
