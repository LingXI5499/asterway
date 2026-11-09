/*
 * 声明统计执行计时返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 毫秒先合计再统一转秒；区分累计和查询区间，不以段数代替会话数。 */
// 定义保存一组固定字段的 record StatisticsExecutionVO；声明统计执行计时返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsExecutionVO(
        // 总数实际秒数（totalActualSeconds），使用 long 类型保存。
        long totalActualSeconds,
        // 区间实际秒数（rangeActualSeconds），使用 long 类型保存。
        long rangeActualSeconds,
        // 总数实际Milliseconds（totalActualMilliseconds），使用 long 类型保存。
        long totalActualMilliseconds,
        // 区间实际Milliseconds（rangeActualMilliseconds），使用 long 类型保存。
        long rangeActualMilliseconds,
        // 总数会话列表（totalSessions），使用 long 类型保存。
        long totalSessions,
        // 区间会话列表（rangeSessions），使用 long 类型保存。
        long rangeSessions,
        // 总数Finished会话列表（totalFinishedSessions），使用 long 类型保存。
        long totalFinishedSessions,
        // 区间Finished会话列表（rangeFinishedSessions），使用 long 类型保存。
        long rangeFinishedSessions,
        // 最后一次执行计时At（lastExecutionAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant lastExecutionAt) {}
