/*
 * 声明统计趋势返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 每日趋势，耗时、打卡和真实完成动作独立汇总后组合。 */
// 定义保存一组固定字段的 record StatisticsTrendVO；声明统计趋势返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsTrendVO(
        // 按用户时区计算的业务日期（businessDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate businessDate,
        // 实际Milliseconds（actualMilliseconds），使用 long 类型保存。
        long actualMilliseconds,
        // 实际秒数（actualSeconds），使用 long 类型保存。
        long actualSeconds,
        // 会话列表（sessions），使用 long 类型保存。
        long sessions,
        // 习惯已完成（habitDone），使用 long 类型保存。
        long habitDone,
        // 习惯应完成（habitDue），使用 long 类型保存。
        long habitDue,
        // 习惯完成情况频率（habitCompletionRate），使用 BigDecimal 类型保存。
        BigDecimal habitCompletionRate,
        // 目标完成情况Actions（goalCompletionActions），使用 long 类型保存。
        long goalCompletionActions,
        // 阶段完成情况Actions（stageCompletionActions），使用 long 类型保存。
        long stageCompletionActions,
        // 任务完成情况Actions（taskCompletionActions），使用 long 类型保存。
        long taskCompletionActions,
        // 已完成Distinct任务列表（completedDistinctTasks），使用 long 类型保存。
        long completedDistinctTasks,
        // 目标归档Actions（goalArchiveActions），使用 long 类型保存。
        long goalArchiveActions,
        // 目标恢复Actions（goalRestoreActions），使用 long 类型保存。
        long goalRestoreActions) {}
