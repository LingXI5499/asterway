/*
 * 声明Registration的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 注册只填写用户名和两次密码；验证码凭据由客户端验证流程提供。 */
// 定义保存一组固定字段的 record RegistrationDTO；声明Registration的请求字段和校验条件；接口收到数据后按这些限制检查。
public record RegistrationDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(max = 60) String username,
        // 要求该值必须存在，不能为 null。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 密码（password），使用 String 类型保存；可为 null，表示尚未提供。
        @NotNull @Size(max = 100) String password,
        // 要求该值必须存在，不能为 null。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 确认密码（confirmPassword），使用 String 类型保存；可为 null，表示尚未提供。
        @NotNull @Size(max = 100) String confirmPassword,
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // captcha本次请求序号（captchaTicket），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(max = 64) String captchaTicket,
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 客户端Nonce（clientNonce），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(min = 16, max = 128) String clientNonce
) {
}
