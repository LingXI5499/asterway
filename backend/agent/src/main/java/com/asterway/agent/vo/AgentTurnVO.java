/*
 * 声明成长助手本轮对话返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 轮次状态；reply 为已保存文本，供刷新和幂等请求恢复。 */
// 定义保存一组固定字段的 record AgentTurnVO；声明成长助手本轮对话返回给页面的字段，不直接把数据库实体全部暴露出去。
public record AgentTurnVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 对话编号（conversationId），使用 String 类型保存；可为 null，表示尚未提供。
        String conversationId,
        // 请求编号（requestId），使用 String 类型保存；可为 null，表示尚未提供。
        String requestId,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
        String finishReason,
        // 错误编码（errorCode），使用 String 类型保存；可为 null，表示尚未提供。
        String errorCode,
        // 开始时At（startedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant startedAt,
        // finishedAt，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant finishedAt,
        // reply，使用 String 类型保存；可为 null，表示尚未提供。
        String reply,
        // 版本号（version），使用 long 类型保存。
        long version) {}
