/*
 * 声明Captcha校验的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入最大（Max），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Max;
// 引入最小（Min），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Min;
// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 拼图校验请求，offset 是透明拼图左上角的横坐标。 */
// 定义保存一组固定字段的 record CaptchaVerifyDTO；声明Captcha校验的请求字段和校验条件；接口收到数据后按这些限制检查。
public record CaptchaVerifyDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // challenge编号（challengeId），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(max = 64) String challengeId,
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 客户端Nonce（clientNonce），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(min = 16, max = 128) String clientNonce,
        // 要求该值必须存在，不能为 null。
        // 要求数值不小于注解给出的下限。
        // 要求数值不大于注解给出的上限。
        // 分页时跳过的记录数（offset），使用 Integer 类型保存；可为 null，表示尚未提供。
        @NotNull @Min(0) @Max(256) Integer offset
) {
}
