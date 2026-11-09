/*
 * 声明成长业务状态的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

/* 独立状态操作，必须携带当前资源版本。 */
// 要求文字既不为空，也不能只有空白。
// 要求该值必须存在，不能为 null。
// 要求数值为零或正数。
// 定义保存一组固定字段的 record GrowthStatusDTO；声明成长业务状态的请求字段和校验条件；接口收到数据后按这些限制检查。
// 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
// 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
public record GrowthStatusDTO(@NotBlank String status, @NotNull @PositiveOrZero Long version) {}
