/*
 * 声明用户返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 账户安全展示对象，不包含密码或安全账本材料。 */
// 定义保存一组固定字段的 record UserVO；声明用户返回给页面的字段，不直接把数据库实体全部暴露出去。
public record UserVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 用户名（username），使用 String 类型保存；可为 null，表示尚未提供。
        String username,
        // 昵称（nickname），使用 String 类型保存；可为 null，表示尚未提供。
        String nickname,
        // 是否启用（enabled），使用 boolean 类型保存。
        boolean enabled,
        // 首次登录是否必须改密码（mustChangePassword），使用 boolean 类型保存。
        boolean mustChangePassword,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 等待处理时间时区（pendingTimeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String pendingTimeZone,
        // 时间时区生效At（timeZoneEffectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant timeZoneEffectiveAt,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 角色列表（roles），使用 List<String> 类型保存。
        List<String> roles,
        // 角色编号列表（roleIds），使用 List<String> 类型保存。
        List<String> roleIds
) {
}
