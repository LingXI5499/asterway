package com.asterway.common.security;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.CaptchaResultVO;
import com.asterway.common.utils.CaptchaImageUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** 短时挑战及一次性票据；验证、删除挑战、创建票据在 Redis 内原子执行。 */
@Component
public class RedisCaptchaStore {

    private static final String CHALLENGE_PREFIX = "asterway:identity:captcha:";
    private static final String TICKET_PREFIX = "asterway:identity:captcha-ticket:";
    private static final DefaultRedisScript<Long> VERIFY = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then return 0 end
            local c=cjson.decode(raw)
            if c.nonce~=ARGV[1] then return 0 end
            if math.abs(tonumber(c.answer)-tonumber(ARGV[2]))>3 then
                c.attempts=c.attempts+1
                if c.attempts>=5 then redis.call('del',KEYS[1])
                else redis.call('set',KEYS[1],cjson.encode(c),'KEEPTTL') end
                return 0
            end
            redis.call('del',KEYS[1])
            redis.call('set',KEYS[2],cjson.encode({scene=c.scene,nonce=c.nonce}),'EX',60)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then return 0 end
            local c=cjson.decode(raw)
            if c.scene~=ARGV[1] or c.nonce~=ARGV[2] then return 0 end
            redis.call('del',KEYS[1])
            return 1
            """, Long.class);
    private final StringRedisTemplate redis;
    private final ObjectMapper json;

    public RedisCaptchaStore(StringRedisTemplate redis, ObjectMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public CaptchaResultVO create(String scene, String nonce) {
        if (!List.of("LOGIN", "REGISTER").contains(scene)) {
            throw BusinessException.badRequest("验证码场景无效");
        }
        var image = CaptchaImageUtils.create();
        String id = UUID.randomUUID().toString();
        try {
            String data = json.writeValueAsString(Map.of("scene", scene, "nonce", nonce,
                    "answer", image.answer(), "attempts", 0));
            redis.opsForValue().set(CHALLENGE_PREFIX + id, data, Duration.ofSeconds(120));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("验证码数据写入失败");
        }
        return new CaptchaResultVO(id, image.background(), image.piece(), image.top(), 320, 160, 64, 120);
    }

    public String verify(String challengeId, String nonce, int offset) {
        String ticket = UUID.randomUUID().toString();
        Long verified = redis.execute(VERIFY, List.of(CHALLENGE_PREFIX + challengeId, TICKET_PREFIX + ticket),
                nonce, Integer.toString(offset));
        if (!Long.valueOf(1).equals(verified)) {
            throw new BusinessException(400, "CAPTCHA_INVALID", "验证码无效或已过期，请重新验证");
        }
        return ticket;
    }

    public void consume(String ticket, String scene, String nonce) {
        Long consumed = redis.execute(CONSUME, List.of(TICKET_PREFIX + ticket), scene, nonce);
        if (!Long.valueOf(1).equals(consumed)) {
            throw new BusinessException(400, "CAPTCHA_INVALID", "请先完成对应的验证码验证");
        }
    }
}
