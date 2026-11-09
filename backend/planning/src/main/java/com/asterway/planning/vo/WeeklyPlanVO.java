/*
 * 声明每周方案返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 安全返回对象；BIGINT 标识统一作为字符串返回。 */
// 定义保存一组固定字段的 record WeeklyPlanVO；声明每周方案返回给页面的字段，不直接把数据库实体全部暴露出去。
public record WeeklyPlanVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        // 所属周的周一日期（weekStartDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        String id, LocalDate weekStartDate, Long version, Instant createdAt, Instant updatedAt) {}
