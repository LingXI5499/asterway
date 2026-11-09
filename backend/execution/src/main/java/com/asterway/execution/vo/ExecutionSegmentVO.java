/*
 * 声明执行计时连续计时段返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 时间线视图：落库结束时间和开放段的展示截止时间分开表达。 */
// 定义保存一组固定字段的 record ExecutionSegmentVO；声明执行计时连续计时段返回给页面的字段，不直接把数据库实体全部暴露出去。
public record ExecutionSegmentVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 连续计时段No（segmentNo），使用 Integer 类型保存；可为 null，表示尚未提供。
        Integer segmentNo,
        // 任务安排编号（assignmentId），使用 String 类型保存；可为 null，表示尚未提供。
        String assignmentId,
        // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant startedAt,
        // 已结束At（endedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant endedAt,
        // 结束原因（endReason），使用 String 类型保存；可为 null，表示尚未提供。
        String endReason,
        // 生效已结束At（effectiveEndedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant effectiveEndedAt,
        // 实际秒数（actualSeconds），使用 Long 类型保存；可为 null，表示尚未提供。
        Long actualSeconds,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone) {}
