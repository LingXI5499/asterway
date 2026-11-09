/*
 * 声明Captcha创建的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Pattern;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 验证码创建请求，浏览器需生成至少16字符的随机绑定标识。 */
// 定义保存一组固定字段的 record CaptchaCreateDTO；声明Captcha创建的请求字段和校验条件；接口收到数据后按这些限制检查。
public record CaptchaCreateDTO(
        // 要求文字既不为空，也不能只有空白。
        // 要求输入文字符合给定的正则规则。
        // scene，使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Pattern(regexp = "LOGIN|REGISTER") String scene,
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 客户端Nonce（clientNonce），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(min = 16, max = 128) String clientNonce
) {
}
