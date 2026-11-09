/*
 * 声明菜单保存的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入匹配规则（Pattern），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Pattern;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 目录、页面和按钮共用保存请求，具体字段组合由 Service 校验。 */
// 定义保存一组固定字段的 record MenuSaveDTO；声明菜单保存的请求字段和校验条件；接口收到数据后按这些限制检查。
public record MenuSaveDTO(
        // 要求文字既不为空，也不能只有空白。
        // 要求输入文字符合给定的正则规则。
        // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$") String code,
        // 要求数值大于零。
        // 上级编号（parentId），使用 Long 类型保存；可为 null，表示尚未提供。
        @Positive Long parentId,
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Size(max = 64) String name,
        // 要求文字既不为空，也不能只有空白。
        // 要求输入文字符合给定的正则规则。
        // 菜单类型（menuType），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank @Pattern(regexp = "DIRECTORY|PAGE|BUTTON") String menuType,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // route路径（routePath），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 255) String routePath,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // component键（componentKey），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 100) String componentKey,
        // 要求数值大于零。
        // 权限编号（permissionId），使用 Long 类型保存；可为 null，表示尚未提供。
        @Positive Long permissionId,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // icon，使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 64) String icon,
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Integer sortOrder,
        // 要求该值必须存在，不能为 null。
        // 是否显示（visible），使用 Boolean 类型保存；可为 null，表示尚未提供。
        @NotNull Boolean visible,
        // 要求该值必须存在，不能为 null。
        // 是否启用（enabled），使用 Boolean 类型保存；可为 null，表示尚未提供。
        @NotNull Boolean enabled,
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Long version
) {
}
