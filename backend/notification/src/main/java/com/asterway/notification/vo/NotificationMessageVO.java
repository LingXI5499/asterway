/*
 * 声明通知消息返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 消息状态与来源当前版本；不返回收件人内部标识。 */
// 定义保存一组固定字段的 record NotificationMessageVO；声明通知消息返回给页面的字段，不直接把数据库实体全部暴露出去。
public record NotificationMessageVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
        String goalId,
        // condition轮（conditionRound），使用 long 类型保存。
        long conditionRound,
        // 消息类型（messageType），使用 String 类型保存；可为 null，表示尚未提供。
        String messageType,
        // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
        String title,
        // 内容（content），使用 String 类型保存；可为 null，表示尚未提供。
        String content,
        // 读取At（readAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant readAt,
        // handle状态（handleStatus），使用 String 类型保存；可为 null，表示尚未提供。
        String handleStatus,
        // handledAt，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant handledAt,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant updatedAt,
        // 目标版本号（goalVersion），使用 Long 类型保存；可为 null，表示尚未提供。
        Long goalVersion,
        // 是否可以Handle（canHandle），使用 boolean 类型保存。
        boolean canHandle) {}
