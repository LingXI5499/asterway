/*
 * 声明DashboardOverview返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 服务端日历快照和今日概览，所有指标都读取各业务事实。 */
// 定义保存一组固定字段的 record DashboardOverviewVO；声明DashboardOverview返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardOverviewVO(
        // 今天（today），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate today,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // server时间（serverTime），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant serverTime,
        // metrics，使用 DashboardMetricsVO 类型保存。
        DashboardMetricsVO metrics,
        // 当前执行计时（currentExecution），使用 DashboardExecutionVO 类型保存。
        DashboardExecutionVO currentExecution) {}
