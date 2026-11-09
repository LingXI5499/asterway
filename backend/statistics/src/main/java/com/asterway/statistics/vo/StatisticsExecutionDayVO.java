/*
 * 声明统计执行计时日返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 按当前账号时区与自然日交集计算的耗时。 */
// 定义保存一组固定字段的 record StatisticsExecutionDayVO；声明统计执行计时日返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsExecutionDayVO(
        // 按用户时区计算的业务日期（businessDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate businessDate,
        // 实际Milliseconds（actualMilliseconds），使用 long 类型保存。
        long actualMilliseconds,
        // 实际秒数（actualSeconds），使用 long 类型保存。
        long actualSeconds,
        // 会话列表（sessions），使用 long 类型保存。
        long sessions,
        // finished会话列表（finishedSessions），使用 long 类型保存。
        long finishedSessions) {}
