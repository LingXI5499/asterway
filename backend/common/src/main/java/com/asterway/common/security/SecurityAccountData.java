/*
 * 处理Security账号数据相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 业务模块提供给安全组件的最小账户快照；不得直接作为 HTTP 返回对象。 */
// 定义保存一组固定字段的 record SecurityAccountData；处理Security账号数据相关的当前身份、权限或安全数据。
public record SecurityAccountData(
        // 用户编号（userId），使用 long 类型保存。
        long userId,
        // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
        String username,
        // 密码摘要值（passwordHash），使用 String 类型保存；可为 null，表示尚未提供。
        String passwordHash,
        // 是否启用（enabled），使用 boolean 类型保存。
        boolean enabled,
        // 首次登录是否必须改密码（mustChangePassword），使用 boolean 类型保存。
        boolean mustChangePassword,
        // 使旧登录失效的认证版本号（authVersion），使用 long 类型保存。
        long authVersion,
        // 角色列表（roles），使用 Set<String> 类型保存。
        Set<String> roles,
        // 权限列表（permissions），使用 Set<String> 类型保存。
        Set<String> permissions
) {
}
