/*
 * 声明DashboardMetrics返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;

/* 今日事实概览；计划完成数为当前计划状态，不伪装成历史完成动作。 */
// 定义保存一组固定字段的 record DashboardMetricsVO；声明DashboardMetrics返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardMetricsVO(
        // 今天任务列表（todayTasks），使用 long 类型保存。
        long todayTasks,
        // 今天当前已完成任务列表（todayCurrentCompletedTasks），使用 long 类型保存。
        long todayCurrentCompletedTasks,
        // 周Undated任务列表（weekUndatedTasks），使用 long 类型保存。
        long weekUndatedTasks,
        // 实际秒数（actualSeconds），使用 long 类型保存。
        long actualSeconds,
        // 习惯已完成（habitDone），使用 long 类型保存。
        long habitDone,
        // 习惯应完成（habitDue），使用 long 类型保存。
        long habitDue,
        // 习惯完成情况频率（habitCompletionRate），使用 BigDecimal 类型保存。
        BigDecimal habitCompletionRate,
        // 运行中执行计时编号（runningExecutionId），使用 String 类型保存；可为 null，表示尚未提供。
        String runningExecutionId,
        // unread消息列表（unreadMessages），使用 long 类型保存。
        long unreadMessages) {}
