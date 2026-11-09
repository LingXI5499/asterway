/*
 * 处理Redis缓存CaptchaStore相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入Captcha结果（CaptchaResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.CaptchaResultVO;
// 引入Captcha图片Utils（CaptchaImageUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.CaptchaImageUtils;
// 引入JSON数据ProcessingException（JsonProcessingException），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.core.JsonProcessingException;
// 引入JSON 与 Java 对象转换器（ObjectMapper），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.ObjectMapper;
// 引入持续时间（Duration），下面可以直接使用这个类型或静态成员。
import java.time.Duration;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;
// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;
// 引入文字Redis缓存Template（StringRedisTemplate），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.StringRedisTemplate;
// 引入默认值Redis缓存Script（DefaultRedisScript），下面可以直接使用这个类型或静态成员。
import org.springframework.data.redis.core.script.DefaultRedisScript;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* 短时挑战及一次性票据；验证、删除挑战、创建票据在 Redis 内原子执行。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 RedisCaptchaStore；处理Redis缓存CaptchaStore相关的当前身份、权限或安全数据。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class RedisCaptchaStore {

    // 准备CHALLENGE前缀（CHALLENGE_PREFIX），类型为 String；初始内容是固定文字 'asterway:identity:captcha:'。
    private static final String CHALLENGE_PREFIX = "asterway:identity:captcha:";
    // 准备本次请求序号前缀（TICKET_PREFIX），类型为 String；初始内容是固定文字 'asterway:identity:captcha-ticket:'。
    private static final String TICKET_PREFIX = "asterway:identity:captcha-ticket:";
    // 准备校验（VERIFY），类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
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
    // 准备CONSUME，类型为 DefaultRedisScript<Long>；初始内容是新建 默认值Redis缓存Script（DefaultRedisScript） 实例。
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local raw=redis.call('get',KEYS[1])
            if not raw then return 0 end
            local c=cjson.decode(raw)
            if c.scene~=ARGV[1] or c.nonce~=ARGV[2] then return 0 end
            redis.call('del',KEYS[1])
            return 1
            """, Long.class);
    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    private final StringRedisTemplate redis;
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    private final ObjectMapper json;

    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // Redis缓存（redis），使用 StringRedisTemplate 类型保存。
    // JSON 编码和解析库（json），使用 ObjectMapper 类型保存。
    public RedisCaptchaStore(StringRedisTemplate redis, ObjectMapper json) {
        // 把Redis缓存（redis）保存到 this.redis。
        this.redis = redis;
        // 把JSON 编码和解析库（json）保存到 this.json。
        this.json = json;
    }

    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // scene，使用 String 类型保存；可为 null，表示尚未提供。
    // nonce，使用 String 类型保存；可为 null，表示尚未提供。
    public CaptchaResultVO create(String scene, String nonce) {
        // 检查条件 !List.of("LOGIN", "REGISTER").contains(scene) 是否成立；成立时执行下面的分支。
        if (!List.of("LOGIN", "REGISTER").contains(scene)) {
            // 中止正常处理并抛出异常：调用 BusinessException.badRequest 完成bad请求处理；由上层按失败处理。
            throw BusinessException.badRequest("验证码场景无效");
        }
        // 准备图片（image），类型为 null；初始内容是调用 CaptchaImageUtils.create 完成创建处理。
        var image = CaptchaImageUtils.create();
        // 准备编号（id），类型为 String；初始内容是调用 UUID.randomUUID 完成randomUUID处理。
        String id = UUID.randomUUID().toString();
        // 开始执行可能失败的处理；异常交给 catch，资源或收尾交给 finally。
        try {
            // 准备数据（data），类型为 String；初始内容是把对象编码为 JSON 文字。
            String data = json.writeValueAsString(Map.of("scene", scene, "nonce", nonce,
                    // 继续传入或组合调用 "answer", image.answer 完成answer处理，完成上面尚未结束的调用或声明。
                    "answer", image.answer(), "attempts", 0));
            // 调用 redis.opsForValue 完成opsFor值处理。
            redis.opsForValue().set(CHALLENGE_PREFIX + id, data, Duration.ofSeconds(120));
        // 错误（error），使用 JsonProcessingException 类型保存。
        // 捕获这里列出的异常，按下面的规则转换错误、回滚或记录失败。
        } catch (JsonProcessingException error) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("验证码数据写入失败");
        }
        // 把新建 Captcha结果（CaptchaResultVO） 实例交还给调用方，本方法到此结束。
        return new CaptchaResultVO(id, image.background(), image.piece(), image.top(), 320, 160, 64, 120);
    }

    // 校验（verify）方法：调用 verify 完成校验处理；参数负责传入本次处理所需的数据。
    // challenge编号（challengeId），使用 String 类型保存；可为 null，表示尚未提供。
    // nonce，使用 String 类型保存；可为 null，表示尚未提供。
    // 分页时跳过的记录数（offset），使用 int 类型保存。
    public String verify(String challengeId, String nonce, int offset) {
        // 准备本次请求的序号（ticket），类型为 String；初始内容是调用 UUID.randomUUID 完成randomUUID处理。
        String ticket = UUID.randomUUID().toString();
        // 准备verified，类型为 Long；初始内容是执行传入的数据库语句并取得结果。
        Long verified = redis.execute(VERIFY, List.of(CHALLENGE_PREFIX + challengeId, TICKET_PREFIX + ticket),
                // 继续传入或组合调用 nonce, Integer.toString 完成to文字处理，完成上面尚未结束的调用或声明。
                nonce, Integer.toString(offset));
        // 检查条件 !Long.valueOf(1).equals(verified) 是否成立；成立时执行下面的分支。
        if (!Long.valueOf(1).equals(verified)) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(400, "CAPTCHA_INVALID", "验证码无效或已过期，请重新验证");
        }
        // 把本次请求的序号（ticket）交还给调用方，本方法到此结束。
        return ticket;
    }

    // consume方法：调用 consume 完成consume处理；参数负责传入本次处理所需的数据。
    // 本次请求的序号（ticket），使用 String 类型保存；可为 null，表示尚未提供。
    // scene，使用 String 类型保存；可为 null，表示尚未提供。
    // nonce，使用 String 类型保存；可为 null，表示尚未提供。
    public void consume(String ticket, String scene, String nonce) {
        // 准备consumed，类型为 Long；初始内容是执行传入的数据库语句并取得结果。
        Long consumed = redis.execute(CONSUME, List.of(TICKET_PREFIX + ticket), scene, nonce);
        // 检查条件 !Long.valueOf(1).equals(consumed) 是否成立；成立时执行下面的分支。
        if (!Long.valueOf(1).equals(consumed)) {
            // 中止正常处理并抛出异常：新建 带业务错误码和可读提示的异常（BusinessException） 实例；由上层按失败处理。
            throw new BusinessException(400, "CAPTCHA_INVALID", "请先完成对应的验证码验证");
        }
    }
}
