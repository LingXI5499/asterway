/*
 * 声明统计当前状态Totals返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

/* 区间动作数与区间去重任务数，不能相加每日去重数代替区间去重数。 */
// 定义保存一组固定字段的 record StatisticsStateTotalsVO；声明统计当前状态Totals返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsStateTotalsVO(
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
