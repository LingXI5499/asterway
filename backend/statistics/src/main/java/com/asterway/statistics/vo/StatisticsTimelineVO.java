/*
 * 声明统计成长时间线返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 真实事件时间线；事实标识用于稳定分页，不根据当前状态推测过去。 */
// 定义保存一组固定字段的 record StatisticsTimelineVO；声明统计成长时间线返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsTimelineVO(
        // 种类（kind），使用 String 类型保存；可为 null，表示尚未提供。
        String kind,
        // fact编号（factId），使用 String 类型保存；可为 null，表示尚未提供。
        String factId,
        // 对象编号（objectId），使用 String 类型保存；可为 null，表示尚未提供。
        String objectId,
        // 事件类型（eventType），使用 String 类型保存；可为 null，表示尚未提供。
        String eventType,
        // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant occurredAt,
        // 显示名称（label），使用 String 类型保存；可为 null，表示尚未提供。
        String label) {}
