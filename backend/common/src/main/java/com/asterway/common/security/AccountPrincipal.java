/*
 * 处理账号当前登录身份相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入设置（Set），下面可以直接使用这个类型或静态成员。
import java.util.Set;

/* 本次请求的已验证身份，不携带密码哈希。 */
// 定义保存一组固定字段的 record AccountPrincipal；处理账号当前登录身份相关的当前身份、权限或安全数据。
public record AccountPrincipal(
        // 用户编号（userId），使用 long 类型保存。
        long userId,
        // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
        String username,
        // 首次登录是否必须改密码（mustChangePassword），使用 boolean 类型保存。
        boolean mustChangePassword,
        // 使旧登录失效的认证版本号（authVersion），使用 long 类型保存。
        long authVersion,
        // 角色列表（roles），使用 Set<String> 类型保存。
        Set<String> roles,
        // 权限列表（permissions），使用 Set<String> 类型保存。
        Set<String> permissions,
        // 会话编号（sessionId），使用 String 类型保存；可为 null，表示尚未提供。
        String sessionId
) {

    // superAdmin方法：调用 superAdmin 完成superAdmin处理；参数负责传入本次处理所需的数据。
    public boolean superAdmin() {
        // 把判断集合是否包含指定内容交还给调用方，本方法到此结束。
        return roles.contains("SUPER_ADMIN");
    }
}
