/*
 * 声明统计习惯分组返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;

/* 按习惯或历史规则分类归属聚合；删除不抹去历史分子分母。 */
// 定义保存一组固定字段的 record StatisticsHabitGroupVO；声明统计习惯分组返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsHabitGroupVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 删除标记（deleted），使用 boolean 类型保存。
        boolean deleted,
        // 已完成数量（doneCount），使用 long 类型保存。
        long doneCount,
        // 应完成数量（dueCount），使用 long 类型保存。
        long dueCount,
        // 完成情况频率（completionRate），使用 BigDecimal 类型保存。
        BigDecimal completionRate) {}
