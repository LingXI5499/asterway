/*
 * 处理Redis缓存会话Store相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入JSON数据ProcessingException（JsonProcessingException），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.core.JsonProcessingException;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入持续时间（Duration），下面可以直接使用这个类型或静态成员。
import java.time.Duration;
// 引入Array列表（ArrayList），下面可以直接使用这个类型或静态成员。
import java.util.ArrayList;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;
// 引入值（Value），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.annotation.Value;
// 引入文字Redis缓存Template（StringRedisTemplate），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.StringRedisTemplate;
// 引入默认值Redis缓存Script（DefaultRedisScript），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.script.DefaultRedisScript;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* Redis 会话基础设施；所有新增、续期、撤销同时维护用户与全局索引。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 RedisSessionStore；处理Redis缓存会话Store相关的当前身份、权限或安全数据。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class RedisSessionStore {

    // 准备会话前缀（SESSION_PREFIX），类型为 String；初始内容是固定文字 'asterway:identity:session:'。
    private static final String SESSION_PREFIX = "asterway:identity:session:";
    // 准备用户前缀（USER_PREFIX），类型为 String；初始内容是固定文字 'asterway:identity:user-sessions:'。
    private static final String USER_PREFIX = "asterway:identity:user-sessions:";
    // 准备全部索引（ALL_INDEX），类型为 String；初始内容是固定文字 'asterway:identity:sessions'。
    private static final String ALL_INDEX = "asterway:identity:sessions";

    // 准备创建（CREATE），类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
    private static final DefaultRedisScript<Long> CREATE = new DefaultRedisScript<>("""
            if redis.call('exists',KEYS[1]) == 1 then return 0 end
            redis.call('set',KEYS[1],ARGV[1],'PX',ARGV[2])
            redis.call('zadd',KEYS[2],ARGV[3],ARGV[4])
            redis.call('pexpire',KEYS[2],ARGV[2])
            redis.call('zremrangebyscore',KEYS[3],'-inf',ARGV[5])
            redis.call('zadd',KEYS[3],ARGV[3],ARGV[4])
            return 1
            """, Long.class);

    // 准备TOUCH，类型为 DefaultRedisScript<String>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
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

    // 准备删除（DELETE），类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
    private static final DefaultRedisScript<Long> DELETE = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then redis.call('zrem',KEYS[2],ARGV[1]); return 0 end
            local s=cjson.decode(raw)
            redis.call('del',KEYS[1])
            redis.call('zrem',KEYS[2],ARGV[1])
            redis.call('zrem',ARGV[2]..s.userId,ARGV[1])
            return 1
            """, Long.class);

    // 准备删除用户（DELETE_USER），类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
    private static final DefaultRedisScript<Long> DELETE_USER = new DefaultRedisScript<>("""
            local ids=redis.call('zrange',KEYS[1],0,-1)
            for _,sid in ipairs(ids) do
                redis.call('del',ARGV[1]..sid)
                redis.call('zrem',KEYS[2],sid)
            end
            redis.call('del',KEYS[1])
            return #ids
            """, Long.class);

    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    private final StringRedisTemplate redis;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;
    // idle超时（idleTimeout），使用 Duration 类型保存。
    private final Duration idleTimeout;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public RedisSessionStore(StringRedisTemplate redis, ObjectMapper json,
            // 使用 Value 注解配置下面的类、方法或参数。
            // idle超时（idleTimeout），使用 Duration 类型保存。
            @Value("${asterway.security.session-idle:7d}") Duration idleTimeout) {
        // 把Redis缓存（redis）保存到 this.redis。
        this.redis = redis;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
        // 把idle超时（idleTimeout）保存到 this.idleTimeout。
        this.idleTimeout = idleTimeout;
        // 检查条件 idleTimeout.isNegative() || idleTimeout.isZero() 是否成立；成立时执行下面的分支。
        if (idleTimeout.isNegative() || idleTimeout.isZero()) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("会话期限必须为正数");
        }
    }

    /* 每次登录创建独立会话，失败不返回令牌。 */
    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 用户（user），使用 SecurityAccountData 类型保存。
    // ip，使用 String 类型保存；可为 null，表示尚未提供。
    // 成长助手（agent），使用 String 类型保存；可为 null，表示尚未提供。
    public LoginSessionData create(SecurityAccountData user, String ip, String agent) {
        // 准备当前时间（now），类型为 long；初始内容是当前时间的毫秒数，用于比较登录或操作是否到期。
        long now = System.currentTimeMillis();
        // 准备编号（id），类型为 String；初始内容是调用 UUID.randomUUID 完成randomUUID处理。
        String id = UUID.randomUUID().toString();
        // 准备设备（device），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：agent.isBlank() ? "U+672aU+77e5U+6d4fU+89c8U+5668" : agent.substring(0, Math.min(128, agent.length()…。
        String device = agent.isBlank() ? "未知浏览器" : agent.substring(0, Math.min(128, agent.length()));
        // 准备数据（data），类型为 LoginSessionData；初始内容是新建 登录会话数据（LoginSessionData） 实例。
        LoginSessionData data = new LoginSessionData(id, Long.toString(user.userId()), user.authVersion(),
                // 继续传入或组合调用 now, now, now + idleTimeout.toMillis 完成toMillis处理，完成上面尚未结束的调用或声明。
                now, now, now + idleTimeout.toMillis(), ip, agent, device);
        // 准备创建时（created），类型为 Long；初始内容是执行传入的数据库语句并取得结果。
        Long created = redis.execute(CREATE,
                // 调用 List.of 完成of处理。
                List.of(SESSION_PREFIX + id, USER_PREFIX + data.userId(), ALL_INDEX),
                // 调用 encode(data), Long.toString 完成to文字处理。
                encode(data), Long.toString(idleTimeout.toMillis()), Long.toString(data.expiresAt()), id, Long.toString(now));
        // 检查条件 !Long.valueOf(1).equals(created) 是否成立；成立时执行下面的分支。
        if (!Long.valueOf(1).equals(created)) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("登录会话创建失败");
        }
        // 把数据（data）交还给调用方，本方法到此结束。
        return data;
    }

    // 读取（get）方法：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；参数负责传入本次处理所需的数据。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public LoginSessionData get(String id) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 调用 UUID.fromString 完成来源文字处理。
            UUID.fromString(id);
        // 错误（error），使用 IllegalArgumentException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (IllegalArgumentException error) {
            // 把空值，表示当前还没有对应内容交还给调用方，本方法到此结束。
            return null;
        }
        // 准备原始值（raw），类型为 String；初始内容是调用 redis.opsForValue 完成opsFor值处理。
        String raw = redis.opsForValue().get(SESSION_PREFIX + id);
        // 把根据前面的条件，从两个候选结果中选择一个：raw == null ? null : decode(raw)交还给调用方，本方法到此结束。
        return raw == null ? null : decode(raw);
    }

    /* 仅更新仍存在且身份版本匹配的会话，绝不重建已撤销记录。 */
    // touch方法：调用 touch 完成touch处理；参数负责传入本次处理所需的数据。
    // 当前会话（session），使用 LoginSessionData 类型保存。
    public LoginSessionData touch(LoginSessionData session) {
        // 准备原始值（raw），类型为 String；初始内容是执行传入的数据库语句并取得结果。
        String raw = redis.execute(TOUCH,
                // 调用 List.of 完成of处理。
                List.of(SESSION_PREFIX + session.id(), USER_PREFIX + session.userId(), ALL_INDEX),
                // 当前时间的毫秒数，用于比较登录或操作是否到期。
                session.userId(), Long.toString(session.authVersion()), Long.toString(System.currentTimeMillis()),
                // 调用 Long.toString 完成to文字处理。
                Long.toString(idleTimeout.toMillis()));
        // 把根据前面的条件，从两个候选结果中选择一个：raw == null ? null : decode(raw)交还给调用方，本方法到此结束。
        return raw == null ? null : decode(raw);
    }

    // revoke方法：调用 revoke 完成revoke处理；参数负责传入本次处理所需的数据。
    // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
    public void revoke(String id) {
        // 执行传入的数据库语句并取得结果。
        redis.execute(DELETE, List.of(SESSION_PREFIX + id, ALL_INDEX), id, USER_PREFIX);
    }

    // revoke用户（revokeUser）方法：调用 revokeUser 完成revoke用户处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 long 类型保存。
    public void revokeUser(long userId) {
        // 执行传入的数据库语句并取得结果。
        redis.execute(DELETE_USER, List.of(USER_PREFIX + userId, ALL_INDEX), SESSION_PREFIX);
    }

    /* 查询时清理失效索引；不会延长任何设备的登录期限。 */
    // 列表（list）方法：调用 list 完成列表处理；参数负责传入本次处理所需的数据。
    // 用户编号（userId），使用 Long 类型保存；可为 null，表示尚未提供。
    public List<LoginSessionData> list(Long userId) {
        // 准备索引（index），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个：userId == null ? ALL_INDEX : USER_PREFIX + userId。
        String index = userId == null ? ALL_INDEX : USER_PREFIX + userId;
        // 准备当前时间（now），类型为 long；初始内容是当前时间的毫秒数，用于比较登录或操作是否到期。
        long now = System.currentTimeMillis();
        // 调用 redis.opsForZSet 完成opsForZ设置处理。
        redis.opsForZSet().removeRangeByScore(index, Double.NEGATIVE_INFINITY, now);
        // 准备编号列表（ids），类型为 Set<String>；初始内容是调用 redis.opsForZSet 完成opsForZ设置处理。
        Set<String> ids = redis.opsForZSet().reverseRangeByScore(index, now + 1, Double.POSITIVE_INFINITY);
        // 准备结果（result），类型为 List<LoginSessionData>；初始内容是一个空列表，用来保存后续多条数据。
        List<LoginSessionData> result = new ArrayList<>();
        // 检查ids == null || ids.isEmpty()，即对应内容是否不存在；成立时执行下面的分支。
        if (ids == null || ids.isEmpty()) {
            // 把结果（result）交还给调用方，本方法到此结束。
            return result;
        }
        // 准备键列表（keys），类型为 List<String>；初始内容是把前面筛选、转换后的结果汇成列表。
        // 编号（id），使用 null 类型保存。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        List<String> keys = ids.stream().map(id -> SESSION_PREFIX + id).toList();
        // 准备值列表（values），类型为 List<String>；初始内容是调用 redis.opsForValue 完成opsFor值处理。
        List<String> values = redis.opsForValue().multiGet(keys);
        // 检查条件 values != null 是否成立；成立时执行下面的分支。
        if (values != null) {
            // 准备位置（position），类型为 int；初始内容是固定数值 0。
            int position = 0;
            // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
            // 逐次执行循环：for (String id : ids)；每轮处理一条数据或推进一次检查。
            for (String id : ids) {
                // 准备值（value），类型为 String；初始内容是按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。
                String value = values.get(position++);
                // 检查value == null，即对应内容是否不存在；成立时执行下面的分支。
                if (value == null) {
                    // 调用 redis.opsForZSet 完成opsForZ设置处理。
                    redis.opsForZSet().remove(index, id);
                // 继续传入或组合表达式 } else { 的结果，完成上面尚未结束的调用或声明。
                } else {
                    // 准备当前会话（session），类型为 LoginSessionData；初始内容是调用 decode 完成decode处理。
                    LoginSessionData session = decode(value);
                    // 检查记录或凭据是否已经到期：session.expiresAt() > now；成立时执行下面的分支。
                    if (session.expiresAt() > now) {
                        // 加入集合或登记一条待保存记录。
                        result.add(session);
                    }
                }
            }
        }
        // 把结果（result）交还给调用方，本方法到此结束。
        return result;
    }

    // encode方法：把原始密码编码成不可直接还原的密码摘要；参数负责传入本次处理所需的数据。
    // 当前会话（session），使用 LoginSessionData 类型保存。
    private String encode(LoginSessionData session) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把把对象编码为 JSON 文字交还给调用方，本方法到此结束。
            return json.writeValueAsString(session);
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("会话数据序列化失败");
        }
    }

    // decode方法：调用 decode 完成decode处理；参数负责传入本次处理所需的数据。
    // 原始值（raw），使用 String 类型保存；可为 null，表示尚未提供。
    private LoginSessionData decode(String raw) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把读取 值（Value）交还给调用方，本方法到此结束。
            return json.readValue(raw, LoginSessionData.class);
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("会话数据格式无效");
        }
    }
}
