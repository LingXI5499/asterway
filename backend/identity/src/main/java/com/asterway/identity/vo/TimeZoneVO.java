/*
 * 声明时间时区返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 本人时区状态，待生效值不会提前替换当前业务时区。 */
// 定义保存一组固定字段的 record TimeZoneVO；声明时间时区返回给页面的字段，不直接把数据库实体全部暴露出去。
public record TimeZoneVO(
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 等待处理时间时区（pendingTimeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String pendingTimeZone,
        // 生效At（effectiveAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant effectiveAt,
        // 版本号（version），使用 long 类型保存。
        long version
) {
}
