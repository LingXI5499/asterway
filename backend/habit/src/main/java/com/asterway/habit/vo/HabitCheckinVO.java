/*
 * 声明习惯打卡返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 原始打卡事实；取消后仍可读取，不覆盖原打卡时间或修订。 */
// 定义保存一组固定字段的 record HabitCheckinVO；声明习惯打卡返回给页面的字段，不直接把数据库实体全部暴露出去。
public record HabitCheckinVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 习惯编号（habitId），使用 String 类型保存；可为 null，表示尚未提供。
        String habitId,
        // 修订版本编号（revisionId），使用 String 类型保存；可为 null，表示尚未提供。
        String revisionId,
        // 名称确认时的数据快照（nameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
        String nameSnapshot,
        // 分类编号（categoryId），使用 String 类型保存；可为 null，表示尚未提供。
        String categoryId,
        // 按用户时区计算的业务日期（businessDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate businessDate,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 已核对At（checkedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant checkedAt,
        // 已取消At（cancelledAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant cancelledAt,
        // 版本号（version），使用 long 类型保存。
        long version) {}
