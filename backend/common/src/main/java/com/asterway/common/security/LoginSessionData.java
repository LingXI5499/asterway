/*
 * 处理登录会话数据相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

/* Redis 中的一次登录，不缓存角色权限；用户标识使用字符串避免 Lua 数字精度损失。 */
// 定义保存一组固定字段的 record LoginSessionData；处理登录会话数据相关的当前身份、权限或安全数据。
public record LoginSessionData(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 用户编号（userId），使用 String 类型保存；可为 null，表示尚未提供。
        String userId,
        // 使旧登录失效的认证版本号（authVersion），使用 long 类型保存。
        long authVersion,
        // 创建时At（createdAt），使用 long 类型保存。
        long createdAt,
        // 最后一次当前有效At（lastActiveAt），使用 long 类型保存。
        long lastActiveAt,
        // 到期At（expiresAt），使用 long 类型保存。
        long expiresAt,
        // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
        String ipAddress,
        // 用户成长助手（userAgent），使用 String 类型保存；可为 null，表示尚未提供。
        String userAgent,
        // 设备显示名称（deviceLabel），使用 String 类型保存；可为 null，表示尚未提供。
        String deviceLabel
) {
}
