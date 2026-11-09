/*
 * 声明Dashboard执行计时返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 当前执行只读摘要，不返回登录 SID，不刷新心跳。 */
// 定义保存一组固定字段的 record DashboardExecutionVO；声明Dashboard执行计时返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardExecutionVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
        String taskId,
        // 任务名称（taskName），使用 String 类型保存；可为 null，表示尚未提供。
        String taskName,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 连续计时段编号（segmentId），使用 String 类型保存；可为 null，表示尚未提供。
        String segmentId,
        // 实际秒数（actualSeconds），使用 long 类型保存。
        long actualSeconds,
        // 心跳截止时间（heartbeatDeadline），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant heartbeatDeadline,
        // eligibleUntil，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant eligibleUntil,
        // 是否可以心跳（canHeartbeat），使用 boolean 类型保存。
        boolean canHeartbeat) {}
