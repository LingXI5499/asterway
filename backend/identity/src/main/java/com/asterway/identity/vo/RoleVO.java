/*
 * 声明角色返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 角色及当前授权标识，用于同一角色的授权回显。 */
// 定义保存一组固定字段的 record RoleVO；声明角色返回给页面的字段，不直接把数据库实体全部暴露出去。
public record RoleVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
        String code,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        String description,
        // builtin，使用 boolean 类型保存。
        boolean builtin,
        // 是否启用（enabled），使用 boolean 类型保存。
        boolean enabled,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 权限编号列表（permissionIds），使用 List<String> 类型保存。
        List<String> permissionIds,
        // 菜单编号列表（menuIds），使用 List<String> 类型保存。
        List<String> menuIds
) {
}
