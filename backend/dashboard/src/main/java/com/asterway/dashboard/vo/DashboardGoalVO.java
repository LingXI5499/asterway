/*
 * 声明Dashboard目标返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;

/* 当前目标精确任务数和权重进度；没有有效任务时进度为空。 */
// 定义保存一组固定字段的 record DashboardGoalVO；声明Dashboard目标返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardGoalVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 完成情况ConditionMet（completionConditionMet），使用 boolean 类型保存。
        boolean completionConditionMet,
        // 任务数量（taskCount），使用 long 类型保存。
        long taskCount,
        // 已完成任务列表（completedTasks），使用 long 类型保存。
        long completedTasks,
        // 当前进度（currentProgress），使用 BigDecimal 类型保存。
        BigDecimal currentProgress) {}
