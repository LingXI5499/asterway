/*
 * 声明成长助手发送结果返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

/* 会话内普通回复结果；流式回复通过 SSE 返回。 */
// 定义保存一组固定字段的 record AgentSendResultVO；声明成长助手发送结果返回给页面的字段，不直接把数据库实体全部暴露出去。
public record AgentSendResultVO(
        // reply，使用 String 类型保存；可为 null，表示尚未提供。
        String reply,
        // 模型（model），使用 String 类型保存；可为 null，表示尚未提供。
        String model,
        // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
        String finishReason,
        // 对话编号（conversationId），使用 String 类型保存；可为 null，表示尚未提供。
        String conversationId,
        // 本轮对话编号（turnId），使用 String 类型保存；可为 null，表示尚未提供。
        String turnId,
        // 请求编号（requestId），使用 String 类型保存；可为 null，表示尚未提供。
        String requestId,
        // 本轮对话版本号（turnVersion），使用 long 类型保存。
        long turnVersion) {}
