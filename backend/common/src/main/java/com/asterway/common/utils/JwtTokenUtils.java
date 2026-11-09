/*
 * 集中处理Jwt身份凭据Utils的重复规则，避免各业务模块分别实现。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.utils;

// 引入带业务错误码和可读提示的异常（BusinessException），下面可以直接使用这个类型或静态成员。
import com.asterway.common.exception.BusinessException;
// 引入登录会话数据（LoginSessionData），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.LoginSessionData;
// 引入不可修改Secret（ImmutableSecret），下面可以直接使用这个类型或静态成员。
import com.nimbusds.jose.jwk.source.ImmutableSecret;
// 引入StandardCharsets，下面可以直接使用这个类型或静态成员。
import java.nio.charset.StandardCharsets;
// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Secret键Spec（SecretKeySpec），下面可以直接使用这个类型或静态成员。
import javax.crypto.spec.SecretKeySpec;
// 引入值（Value），下面可以直接使用这个类型或静态成员。
import org.springframework.beans.factory.annotation.Value;
// 引入MacAlgorithm，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
// 引入Jws请求头（JwsHeader），下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwsHeader;
// 引入Jwt，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.Jwt;
// 引入JwtClaims设置（JwtClaimsSet），下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
// 引入JwtDecoder，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtDecoder;
// 引入JwtEncoder，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtEncoder;
// 引入JwtEncoderParameters，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
// 引入JwtValidators，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.JwtValidators;
// 引入NimbusJwtDecoder，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
// 引入NimbusJwtEncoder，下面可以直接使用这个类型或静态成员。
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
// 引入Component，下面可以直接使用这个类型或静态成员。
import org.springframework.stereotype.Component;

/* JWT 签发与验签；固定算法和签发者，不把令牌中的权限作为授权依据。 */
// 把该共享组件交给 Spring 创建和管理。
// 定义类 JwtTokenUtils；集中处理Jwt身份凭据Utils的重复规则，避免各业务模块分别实现。
@Component
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class JwtTokenUtils {

    // 准备ISSUER，类型为 String；初始内容是固定文字 'asterway'。
    private static final String ISSUER = "asterway";
    // encoder，使用 JwtEncoder 类型保存。
    private final JwtEncoder encoder;
    // decoder，使用 JwtDecoder 类型保存。
    private final JwtDecoder decoder;

    // 使用 Value 注解配置下面的类、方法或参数。
    // 构造当前对象，把需要协作的组件保存下来，后面的业务方法会复用它们。
    // secret，使用 String 类型保存；可为 null，表示尚未提供。
    public JwtTokenUtils(@Value("${asterway.security.jwt-secret}") String secret) {
        // 准备bytes，类型为 byte[]；初始内容是读取 Bytes 的值。
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // 检查条件 bytes.length < 32 是否成立；成立时执行下面的分支。
        if (bytes.length < 32) {
            // 中止正常处理并抛出异常：新建 Illegal当前状态Exception（IllegalStateException） 实例；由上层按失败处理。
            throw new IllegalStateException("本地 JWT 签名配置至少需要32字节");
        }
        // 准备键（key），类型为 null；初始内容是新建 Secret键Spec（SecretKeySpec） 实例。
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        // 把新建 NimbusJwtEncoder 实例保存到 encoder。
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        // 准备verifier，类型为 NimbusJwtDecoder；初始内容是调用 NimbusJwtDecoder.withSecretKey 完成withSecret键处理。
        NimbusJwtDecoder verifier = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // 设置 JwtValidator 的值。
        verifier.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        // 把verifier保存到 decoder。
        decoder = verifier;
    }

    // 创建（create）方法：调用 create 完成创建处理；参数负责传入本次处理所需的数据。
    // 当前会话（session），使用 LoginSessionData 类型保存。
    public String create(LoginSessionData session) {
        // 准备claims，类型为 JwtClaimsSet；初始内容是组装 er。
        JwtClaimsSet claims = JwtClaimsSet.builder()
                // 继续对上一步结果执行判断是否为 suer。
                .issuer(ISSUER)
                // 继续对上一步结果执行调用 audience(List.of 完成of处理。
                .audience(List.of("asterway-web"))
                // 继续对上一步结果执行调用 subject(session.userId 完成用户编号处理。
                .subject(session.userId())
                // 继续对上一步结果执行调用 issuedAt(Instant.ofEpochMilli 完成ofEpochMilli处理。
                .issuedAt(Instant.ofEpochMilli(session.lastActiveAt()))
                // 继续对上一步结果执行调用 expiresAt(Instant.ofEpochMilli 完成ofEpochMilli处理。
                .expiresAt(Instant.ofEpochMilli(session.expiresAt()))
                // 继续对上一步结果执行调用 claim("sid", session.id 完成编号处理。
                .claim("sid", session.id())
                // 继续对上一步结果执行调用 build 完成构造处理。
                .build();
        // 把把原始密码编码成不可直接还原的密码摘要交还给调用方，本方法到此结束。
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                // 继续对上一步结果执行读取 身份凭据值（TokenValue） 的值。
                .getTokenValue();
    }

    // 解析（parse）方法：调用 parse 完成解析处理；参数负责传入本次处理所需的数据。
    // 身份凭据（token），使用 String 类型保存；可为 null，表示尚未提供。
    public Jwt parse(String token) {
        // 准备jwt，类型为 Jwt；初始内容是调用 decoder.decode 完成decode处理。
        Jwt jwt = decoder.decode(token);
        // 检查记录或凭据是否已经到期：!jwt.getAudience().contains("asterway-web") || jwt.getClaimAsString("sid") == null || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(I…；成立时执行下面的分支。
        if (!jwt.getAudience().contains("asterway-web") || jwt.getClaimAsString("sid") == null
                // 继续补充同一个判断或备用结果：记录或凭据是否已经到期：|| jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {。
                || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            // 中止正常处理并抛出异常：调用 BusinessException.unauthorized 完成unauthorized处理；由上层按失败处理。
            throw BusinessException.unauthorized();
        }
        // 把jwt交还给调用方，本方法到此结束。
        return jwt;
    }
}
