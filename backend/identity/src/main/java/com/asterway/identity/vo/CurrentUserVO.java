/*
 * 声明当前用户返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 应用启动时确认的当前身份和授权菜单。 */
// 定义保存一组固定字段的 record CurrentUserVO；声明当前用户返回给页面的字段，不直接把数据库实体全部暴露出去。
public record CurrentUserVO(
        // 用户（user），使用 UserVO 类型保存。
        UserVO user,
        // 角色列表（roles），使用 Set<String> 类型保存。
        Set<String> roles,
        // 权限列表（permissions），使用 Set<String> 类型保存。
        Set<String> permissions,
        // 菜单列表（menus），使用 List<MenuVO> 类型保存。
        List<MenuVO> menus
) {
}
