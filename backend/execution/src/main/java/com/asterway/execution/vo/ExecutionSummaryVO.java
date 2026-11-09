/*
 * 声明执行计时汇总返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 累计和区间耗时分别返回，会话数量与时间段数量不混用。 */
// 定义保存一组固定字段的 record ExecutionSummaryVO；声明执行计时汇总返回给页面的字段，不直接把数据库实体全部暴露出去。
public record ExecutionSummaryVO(
        // 总数实际秒数（totalActualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
        Long totalActualSeconds,
        // 区间实际秒数（rangeActualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
        Long rangeActualSeconds,
        // 总数会话列表（totalSessions），使用 Long 类型保存；可为 null，表示尚未提供。
        Long totalSessions,
        // 区间会话列表（rangeSessions），使用 Long 类型保存；可为 null，表示尚未提供。
        Long rangeSessions,
        // finished会话列表（finishedSessions），使用 Long 类型保存；可为 null，表示尚未提供。
        Long finishedSessions,
        // 运行中会话列表（runningSessions），使用 Long 类型保存；可为 null，表示尚未提供。
        Long runningSessions,
        // 已暂停会话列表（pausedSessions），使用 Long 类型保存；可为 null，表示尚未提供。
        Long pausedSessions,
        // 最新执行计时At（latestExecutionAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant latestExecutionAt) {}
