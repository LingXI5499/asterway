/*
 * 声明权限Grant的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 原子替换同一角色的动作权限集合。 */
// 定义保存一组固定字段的 record PermissionGrantDTO；声明权限Grant的请求字段和校验条件；接口收到数据后按这些限制检查。
public record PermissionGrantDTO(
        // 要求该值必须存在，不能为 null。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 权限编号列表（permissionIds），使用 List<@Positive Long> 类型保存。
        @NotNull @Size(max = 200) List<@Positive Long> permissionIds,
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long version
) {
}
