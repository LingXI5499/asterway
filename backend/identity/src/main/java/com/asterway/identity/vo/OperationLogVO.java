/*
 * 声明操作日志返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.identity.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入按键保存值的对应表（Map），下面可以直接使用这个类型或静态成员。
import java.util.Map;

/* 操作审计展示对象，摘要仅包含已筛选的安全字段。 */
// 定义保存一组固定字段的 record OperationLogVO；声明操作日志返回给页面的字段，不直接把数据库实体全部暴露出去。
public record OperationLogVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // actor编号（actorId），使用 String 类型保存；可为 null，表示尚未提供。
        String actorId,
        // actor用户名（actorUsername），使用 String 类型保存；可为 null，表示尚未提供。
        String actorUsername,
        // 目标值用户编号（targetUserId），使用 String 类型保存；可为 null，表示尚未提供。
        String targetUserId,
        // resource类型（resourceType），使用 String 类型保存；可为 null，表示尚未提供。
        String resourceType,
        // resource编号（resourceId），使用 String 类型保存；可为 null，表示尚未提供。
        String resourceId,
        // 操作编码（actionCode），使用 String 类型保存；可为 null，表示尚未提供。
        String actionCode,
        // outcome，使用 String 类型保存；可为 null，表示尚未提供。
        String outcome,
        // 原因编码（reasonCode），使用 String 类型保存；可为 null，表示尚未提供。
        String reasonCode,
        // 修改汇总（changeSummary），使用 Map<String, Object> 类型保存。
        Map<String, Object> changeSummary,
        // ipAddress，使用 String 类型保存；可为 null，表示尚未提供。
        String ipAddress,
        // 本次请求的追踪编号（traceId），使用 String 类型保存；可为 null，表示尚未提供。
        String traceId,
        // occurredAt，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant occurredAt
) {
}
