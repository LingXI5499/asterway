/*
 * 声明设备返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 以会话为单位的设备展示对象，不返回 JWT。 */
// 定义保存一组固定字段的 record DeviceVO；声明设备返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DeviceVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 用户编号（userId），使用 String 类型保存；可为 null，表示尚未提供。
        String userId,
        // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
        String username,
        // 设备显示名称（deviceLabel），使用 String 类型保存；可为 null，表示尚未提供。
        String deviceLabel,
        // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
        String ipAddress,
        // 用户成长助手（userAgent），使用 String 类型保存；可为 null，表示尚未提供。
        String userAgent,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 最后一次当前有效At（lastActiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant lastActiveAt,
        // 到期At（expiresAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant expiresAt,
        // 当前（current），使用 boolean 类型保存。
        boolean current
) {
}
