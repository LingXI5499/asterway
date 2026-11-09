/*
 * 声明目标当前状态历史返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 主体真实历史；BASELINE 仅说明初始化状态，不计为完成动作。 */
// 定义保存一组固定字段的 record GoalStateHistoryVO；声明目标当前状态历史返回给页面的字段，不直接把数据库实体全部暴露出去。
public record GoalStateHistoryVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
        String goalId,
        // 事件类型（eventType），使用 String 类型保存；可为 null，表示尚未提供。
        String eventType,
        // 变化前状态（beforeStatus），使用 String 类型保存；可为 null，表示尚未提供。
        String beforeStatus,
        // 变化后状态（afterStatus），使用 String 类型保存；可为 null，表示尚未提供。
        String afterStatus,
        // 变化前已归档（beforeArchived），使用 Boolean 类型保存；可为 null，表示尚未提供。
        Boolean beforeArchived,
        // 变化后已归档（afterArchived），使用 Boolean 类型保存；可为 null，表示尚未提供。
        Boolean afterArchived,
        // 名称确认时的数据快照（nameSnapshot），使用 String 类型保存；可为 null，表示尚未提供。
        String nameSnapshot,
        // 版本号（entityVersion），使用 long 类型保存。
        long entityVersion,
        // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant occurredAt,
        // 来源类型（sourceType），使用 String 类型保存；可为 null，表示尚未提供。
        String sourceType,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt) {}
