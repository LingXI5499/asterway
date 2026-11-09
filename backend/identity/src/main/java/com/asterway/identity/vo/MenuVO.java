/*
 * 声明菜单返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 目录、页面和按钮树节点，组件键由前端白名单解释。 */
// 定义保存一组固定字段的 record MenuVO；声明菜单返回给页面的字段，不直接把数据库实体全部暴露出去。
public record MenuVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 编码（code），使用 String 类型保存；可为 null，表示尚未提供。
        String code,
        // 上级编号（parentId），使用 String 类型保存；可为 null，表示尚未提供。
        String parentId,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 菜单类型（menuType），使用 String 类型保存；可为 null，表示尚未提供。
        String menuType,
        // route路径（routePath），使用 String 类型保存；可为 null，表示尚未提供。
        String routePath,
        // component键（componentKey），使用 String 类型保存；可为 null，表示尚未提供。
        String componentKey,
        // 权限编号（permissionId），使用 String 类型保存；可为 null，表示尚未提供。
        String permissionId,
        // icon，使用 String 类型保存；可为 null，表示尚未提供。
        String icon,
        // 排序顺序（sortOrder），使用 int 类型保存。
        int sortOrder,
        // 是否显示（visible），使用 boolean 类型保存。
        boolean visible,
        // 是否启用（enabled），使用 boolean 类型保存。
        boolean enabled,
        // builtin，使用 boolean 类型保存。
        boolean builtin,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 下级列表（children），使用 List<MenuVO> 类型保存。
        List<MenuVO> children
) {
}
