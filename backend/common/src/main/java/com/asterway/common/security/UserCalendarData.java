/*
 * 处理用户Calendar数据相关的当前身份、权限或安全数据。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.security;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入时区编号（ZoneId），下面可以直接使用这个类型或静态成员。
import java.time.ZoneId;

/* 一次业务操作的服务端日历快照，避免跨午夜时多次取今天发生变化。 */
// 定义保存一组固定字段的 record UserCalendarData；处理用户Calendar数据相关的当前身份、权限或安全数据。
// 用户编号（userId），使用 long 类型保存。
// 时区（zone），使用 ZoneId 类型保存。
// 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
public record UserCalendarData(long userId, ZoneId zone, Instant now) {
    // 今天（today）方法：调用 today 完成今天处理；参数负责传入本次处理所需的数据。
    public LocalDate today() {
        // 把调用 now.atZone 完成at时区处理交还给调用方，本方法到此结束。
        return now.atZone(zone).toLocalDate();
    }

    // 开始（start）方法：调用 start 完成开始处理；参数负责传入本次处理所需的数据。
    // 日期（date），使用 LocalDate 类型保存；可为 null，表示尚未提供。
    public Instant start(LocalDate date) {
        // 把调用 date.atStartOfDay 完成at开始Of日处理交还给调用方，本方法到此结束。
        return date.atStartOfDay(zone).toInstant();
    }
}
