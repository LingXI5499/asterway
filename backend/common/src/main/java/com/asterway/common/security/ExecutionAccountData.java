/*
 * 处理执行计时账号数据相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;

/* 计时所需的账户时区与安全投影，不包含凭据，不作为 HTTP 返回。 */
// 定义保存一组固定字段的 record ExecutionAccountData；处理执行计时账号数据相关的当前身份、权限或安全数据。
public record ExecutionAccountData(
        // 用户编号（userId），使用 long 类型保存。
        long userId,
        // 是否启用（enabled），使用 boolean 类型保存。
        boolean enabled,
        // 使旧登录失效的认证版本号（authVersion），使用 long 类型保存。
        long authVersion,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 等待处理时间时区（pendingTimeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String pendingTimeZone,
        // 时间时区生效At（timeZoneEffectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant timeZoneEffectiveAt) {

    /* 与账户延后生效规则一致，按本次服务端时间求有效时区。 */
    // 生效时区（effectiveZone）方法：调用 effectiveZone 完成生效时区处理；参数负责传入本次处理所需的数据。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    public ZoneId effectiveZone(Instant now) {
        // 把根据前面的条件，从两个候选结果中选择一个：ZoneId.of(pendingTimeZone != null && !timeZoneEffectiveAt.isAfter(now) ? pendingTimeZone : timeZone)交还给调用方，本方法到此结束。
        return ZoneId.of(
                // 继续传入或组合判断是否为 变化后（After），完成上面尚未结束的调用或声明。
                pendingTimeZone != null && !timeZoneEffectiveAt.isAfter(now)
                        // 继续补充同一个判断或备用结果：条件 ? pendingTimeZone 是否成立。
                        ? pendingTimeZone
                        // 继续补充同一个判断或备用结果：条件 : timeZone); 是否成立。
                        : timeZone);
    }

    /* 只返回仍在未来的切换点，已经生效的时区不成为下一段的过去截止。 */
    // 等待处理Boundary（pendingBoundary）方法：调用 pendingBoundary 完成等待处理Boundary处理；参数负责传入本次处理所需的数据。
    // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
    public Instant pendingBoundary(Instant now) {
        // 把根据前面的条件，从两个候选结果中选择一个：pendingTimeZone != null && timeZoneEffectiveAt.isAfter(now) ? timeZoneEffectiveAt : null交还给调用方，本方法到此结束。
        return pendingTimeZone != null && timeZoneEffectiveAt.isAfter(now)
                // 继续补充同一个判断或备用结果：条件 ? timeZoneEffectiveAt 是否成立。
                ? timeZoneEffectiveAt
                // 继续补充同一个判断或备用结果：条件 : null; 是否成立。
                : null;
    }
}
