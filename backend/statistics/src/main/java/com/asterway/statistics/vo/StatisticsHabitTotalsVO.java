/*
 * 声明统计习惯Totals返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;

/* 完成率为所有实际完成除以所有应做，分母为零时为空。 */
// 定义保存一组固定字段的 record StatisticsHabitTotalsVO；声明统计习惯Totals返回给页面的字段，不直接把数据库实体全部暴露出去。
// 已完成数量（doneCount），使用 long 类型保存。
// 应完成数量（dueCount），使用 long 类型保存。
// 完成情况频率（completionRate），使用 BigDecimal 类型保存。
public record StatisticsHabitTotalsVO(long doneCount, long dueCount, BigDecimal completionRate) {}
