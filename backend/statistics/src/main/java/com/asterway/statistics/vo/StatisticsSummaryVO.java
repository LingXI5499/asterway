/*
 * 声明统计汇总返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 统一周期统计，目标筛选只作用于成长／执行，习惯始终是独立账户维度。 */
// 定义保存一组固定字段的 record StatisticsSummaryVO；声明统计汇总返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsSummaryVO(
        // 统计周期（period），使用 String 类型保存；可为 null，表示尚未提供。
        String period,
        // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateFrom,
        // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateTo,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // server时间（serverTime），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant serverTime,
        // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
        String goalId,
        // 阶段编号（stageId），使用 String 类型保存；可为 null，表示尚未提供。
        String stageId,
        // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
        String taskId,
        // 执行计时（execution），使用 StatisticsExecutionVO 类型保存。
        StatisticsExecutionVO execution,
        // 习惯列表（habits），使用 StatisticsHabitTotalsVO 类型保存。
        StatisticsHabitTotalsVO habits,
        // states，使用 StatisticsStateTotalsVO 类型保存。
        StatisticsStateTotalsVO states) {}
