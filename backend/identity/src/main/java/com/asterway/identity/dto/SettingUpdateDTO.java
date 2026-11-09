/*
 * 声明Setting更新的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 同时更新两项账户配置；每项配置独立比较版本，在同一事务保存。 */
// 定义保存一组固定字段的 record SettingUpdateDTO；声明Setting更新的请求字段和校验条件；接口收到数据后按这些限制检查。
public record SettingUpdateDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // site名称（siteName），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(max = 30) String siteName,
        // 要求该值必须存在，不能为 null。
        // registration是否启用（registrationEnabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
        @NotNull Boolean registrationEnabled,
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // site名称版本号（siteNameVersion），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long siteNameVersion,
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // registration版本号（registrationVersion），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long registrationVersion
) {
}
