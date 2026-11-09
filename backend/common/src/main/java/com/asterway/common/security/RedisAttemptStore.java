/*
 * 处理Redis缓存尝试Store相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入StandardCharsets，下面可以直接使用这个类型或静态成员。
import java.nio.charset.StandardCharsets;
// 引入消息Digest（MessageDigest），下面可以直接使用这个类型或静态成员。
import java.security.MessageDigest;
// 引入NoSuchAlgorithmException，下面可以直接使用这个类型或静态成员。
import java.security.NoSuchAlgorithmException;
// 引入Hex格式（HexFormat），下面可以直接使用这个类型或静态成员。
import java.util.HexFormat;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Locale，下面可以直接使用这个类型或静态成员。
import java.util.Locale;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入文字Redis缓存Template（StringRedisTemplate），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.StringRedisTemplate;
// 引入默认值Redis缓存Script（DefaultRedisScript），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.script.DefaultRedisScript;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* 短期限流与凭据失败计数；验证码失败不调用凭据计数方法。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 RedisAttemptStore；处理Redis缓存尝试Store相关的当前身份、权限或安全数据。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class RedisAttemptStore {

    // 准备INCREMENT，类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local n=redis.call('incr',KEYS[1])
            if n==1 or n==tonumber(ARGV[2]) then redis.call('expire',KEYS[1],ARGV[1]) end
            return n
            """, Long.class);
    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    private final StringRedisTemplate redis;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    public RedisAttemptStore(StringRedisTemplate redis) {
        // 把Redis缓存（redis）保存到 this.redis。
        this.redis = redis;
    }

    // 上限（limit）方法：调用 limit 完成上限处理；参数负责传入本次处理所需的数据。
    // 操作（action），使用 String 类型保存；可为 null，表示尚未提供。
    // 来源（source），使用 String 类型保存；可为 null，表示尚未提供。
    // maximum，使用 int 类型保存。
    // 秒数（seconds），使用 int 类型保存。
    public void limit(String action, String source, int maximum, int seconds) {
        // 准备键（key），类型为 String；初始内容是根据前面的条件，从两个候选结果中选择一个："asterway:identity:rate:" + action + ":" + digest(source == null ? "unknown" : source)。
        String key = "asterway:identity:rate:" + action + ":" + digest(source == null ? "unknown" : source);
        // 准备数量（count），类型为 Long；初始内容是执行传入的数据库语句并取得结果。
        Long count = redis.execute(INCREMENT, List.of(key), Integer.toString(seconds), Integer.toString(Integer.MAX_VALUE));
        // 检查count == null || count > maximum，即对应内容是否不存在；成立时执行下面的分支。
        if (count == null || count > maximum) {
            // 中止正常处理并抛出异常：调用 limited 完成limited处理；由上层按失败处理。
            throw limited(seconds);
        }
    }

    // requireUnlocked方法：要求满足 Unlocked；参数负责传入本次处理所需的数据。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    public void requireUnlocked(String username) {
        // 准备键（key），类型为 String；初始内容是调用 loginKey 完成登录键处理。
        String key = loginKey(username);
        // 准备数量（count），类型为 String；初始内容是调用 redis.opsForValue 完成opsFor值处理。
        String count = redis.opsForValue().get(key);
        // 检查条件 count != null && Long.parseLong(count) >= 5 是否成立；成立时执行下面的分支。
        if (count != null && Long.parseLong(count) >= 5) {
            // 准备剩余（remaining），类型为 Long；初始内容是读取 过期（Expire） 的值。
            Long remaining = redis.getExpire(key);
            // 中止正常处理并抛出异常：根据前面的条件，从两个候选结果中选择一个：limited(remaining == null ? 300 : Math.max(1, remaining))；由上层按失败处理。
            throw limited(remaining == null ? 300 : Math.max(1, remaining));
        }
    }

    // 失败Credentials（failedCredentials）方法：调用 failedCredentials 完成失败Credentials处理；参数负责传入本次处理所需的数据。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    public void failedCredentials(String username) {
        // 执行传入的数据库语句并取得结果。
        redis.execute(INCREMENT, List.of(loginKey(username)), "300", "5");
    }

    // 清除Failures（clearFailures）方法：调用 clearFailures 完成清除Failures处理；参数负责传入本次处理所需的数据。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    public void clearFailures(String username) {
        // 调用 redis.delete 完成删除处理。
        redis.delete(loginKey(username));
    }

    // 登录键（loginKey）方法：调用 loginKey 完成登录键处理；参数负责传入本次处理所需的数据。
    // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
    private String loginKey(String username) {
        // 把转为小写后统一处理交还给调用方，本方法到此结束。
        return "asterway:identity:login-failure:" + digest(username.toLowerCase(Locale.ROOT));
    }

    // limited方法：调用 limited 完成limited处理；参数负责传入本次处理所需的数据。
    // 秒数（seconds），使用 long 类型保存。
    private BusinessException limited(long seconds) {
        // 把新建 带业务错误码和可读提示的异常（BusinessException） 实例交还给调用方，本方法到此结束。
        return new BusinessException(429, "TOO_MANY_ATTEMPTS", "操作过于频繁，请稍后重试",
                // 调用 Map.of 完成of处理。
                Map.of("retryAfterSeconds", seconds));
    }

    // digest方法：调用 digest 完成digest处理；参数负责传入本次处理所需的数据。
    // 文字（text），使用 String 类型保存；可为 null，表示尚未提供。
    private String digest(String text) {
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 把内容的 SHA-256 摘要，用于核对内容或构造幂等键交还给调用方，本方法到此结束。
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    // 继续对上一步结果执行读取 Bytes 的值。
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        // 错误（error），使用 NoSuchAlgorithmException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (NoSuchAlgorithmException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("系统摘要算法不可用");
        }
    }
}
