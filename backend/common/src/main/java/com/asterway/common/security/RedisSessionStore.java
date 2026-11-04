package com.asterway.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Redis 会话基础设施；所有新增、续期、撤销同时维护用户与全局索引。 */
@Component
public class RedisSessionStore {

    private static final String SESSION_PREFIX = "asterway:identity:session:";
    private static final String USER_PREFIX = "asterway:identity:user-sessions:";
    private static final String ALL_INDEX = "asterway:identity:sessions";

    private static final DefaultRedisScript<Long> CREATE = new DefaultRedisScript<>("""
            if redis.call('exists',KEYS[1]) == 1 then return 0 end
            redis.call('set',KEYS[1],ARGV[1],'PX',ARGV[2])
            redis.call('zadd',KEYS[2],ARGV[3],ARGV[4])
            redis.call('pexpire',KEYS[2],ARGV[2])
            redis.call('zremrangebyscore',KEYS[3],'-inf',ARGV[5])
            redis.call('zadd',KEYS[3],ARGV[3],ARGV[4])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<String> TOUCH = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then return nil end
            local s=cjson.decode(raw)
            if tostring(s.userId)~=ARGV[1] or tostring(s.authVersion)~=ARGV[2] then return nil end
            local now=tonumber(ARGV[3])
            if tonumber(s.expiresAt)<=now then return nil end
            now=math.max(now,tonumber(s.lastActiveAt))
            s.lastActiveAt=now
            s.expiresAt=now+tonumber(ARGV[4])
            local updated=cjson.encode(s)
            redis.call('set',KEYS[1],updated,'PX',ARGV[4])
            redis.call('zadd',KEYS[2],s.expiresAt,s.id)
            redis.call('pexpire',KEYS[2],ARGV[4])
            redis.call('zadd',KEYS[3],s.expiresAt,s.id)
            return updated
            """, String.class);

    private static final DefaultRedisScript<Long> DELETE = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then redis.call('zrem',KEYS[2],ARGV[1]); return 0 end
            local s=cjson.decode(raw)
            redis.call('del',KEYS[1])
            redis.call('zrem',KEYS[2],ARGV[1])
            redis.call('zrem',ARGV[2]..s.userId,ARGV[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> DELETE_USER = new DefaultRedisScript<>("""
            local ids=redis.call('zrange',KEYS[1],0,-1)
            for _,sid in ipairs(ids) do
                redis.call('del',ARGV[1]..sid)
                redis.call('zrem',KEYS[2],sid)
            end
            redis.call('del',KEYS[1])
            return #ids
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final Duration idleTimeout;

    public RedisSessionStore(StringRedisTemplate redis, ObjectMapper json,
            @Value("${asterway.security.session-idle:7d}") Duration idleTimeout) {
        this.redis = redis;
        this.json = json;
        this.idleTimeout = idleTimeout;
        if (idleTimeout.isNegative() || idleTimeout.isZero()) {
            throw new IllegalStateException("会话期限必须为正数");
        }
    }

    /** 每次登录创建独立会话，失败不返回令牌。 */
    public LoginSessionData create(SecurityAccountData user, String ip, String agent) {
        long now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        String device = agent.isBlank() ? "未知浏览器" : agent.substring(0, Math.min(128, agent.length()));
        LoginSessionData data = new LoginSessionData(id, Long.toString(user.userId()), user.authVersion(),
                now, now, now + idleTimeout.toMillis(), ip, agent, device);
        Long created = redis.execute(CREATE,
                List.of(SESSION_PREFIX + id, USER_PREFIX + data.userId(), ALL_INDEX),
                encode(data), Long.toString(idleTimeout.toMillis()), Long.toString(data.expiresAt()), id, Long.toString(now));
        if (!Long.valueOf(1).equals(created)) {
            throw new IllegalStateException("登录会话创建失败");
        }
        return data;
    }

    public LoginSessionData get(String id) {
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException error) {
            return null;
        }
        String raw = redis.opsForValue().get(SESSION_PREFIX + id);
        return raw == null ? null : decode(raw);
    }

    /** 仅更新仍存在且身份版本匹配的会话，绝不重建已撤销记录。 */
    public LoginSessionData touch(LoginSessionData session) {
        String raw = redis.execute(TOUCH,
                List.of(SESSION_PREFIX + session.id(), USER_PREFIX + session.userId(), ALL_INDEX),
                session.userId(), Long.toString(session.authVersion()), Long.toString(System.currentTimeMillis()),
                Long.toString(idleTimeout.toMillis()));
        return raw == null ? null : decode(raw);
    }

    public void revoke(String id) {
        redis.execute(DELETE, List.of(SESSION_PREFIX + id, ALL_INDEX), id, USER_PREFIX);
    }

    public void revokeUser(long userId) {
        redis.execute(DELETE_USER, List.of(USER_PREFIX + userId, ALL_INDEX), SESSION_PREFIX);
    }

    /** 查询时清理失效索引；不会延长任何设备的登录期限。 */
    public List<LoginSessionData> list(Long userId) {
        String index = userId == null ? ALL_INDEX : USER_PREFIX + userId;
        long now = System.currentTimeMillis();
        redis.opsForZSet().removeRangeByScore(index, Double.NEGATIVE_INFINITY, now);
        Set<String> ids = redis.opsForZSet().reverseRangeByScore(index, now + 1, Double.POSITIVE_INFINITY);
        List<LoginSessionData> result = new ArrayList<>();
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        List<String> keys = ids.stream().map(id -> SESSION_PREFIX + id).toList();
        List<String> values = redis.opsForValue().multiGet(keys);
        if (values != null) {
            int position = 0;
            for (String id : ids) {
                String value = values.get(position++);
                if (value == null) {
                    redis.opsForZSet().remove(index, id);
                } else {
                    LoginSessionData session = decode(value);
                    if (session.expiresAt() > now) {
                        result.add(session);
                    }
                }
            }
        }
        return result;
    }

    private String encode(LoginSessionData session) {
        try {
            return json.writeValueAsString(session);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("会话数据序列化失败");
        }
    }

    private LoginSessionData decode(String raw) {
        try {
            return json.readValue(raw, LoginSessionData.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("会话数据格式无效");
        }
    }
}
