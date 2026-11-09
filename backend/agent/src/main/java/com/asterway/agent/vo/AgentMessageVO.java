/*
 * 声明成长助手消息返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

// 引入JSON数据节点（JsonNode），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.databind.JsonNode;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;

/* 历史消息；未完成的助手文本可展示，但不能当作已确认回答。 */
// 定义保存一组固定字段的 record AgentMessageVO；声明成长助手消息返回给页面的字段，不直接把数据库实体全部暴露出去。
public record AgentMessageVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 消息No（messageNo），使用 long 类型保存。
        long messageNo,
        // 角色（role），使用 String 类型保存；可为 null，表示尚未提供。
        String role,
        // 内容（content），使用 String 类型保存；可为 null，表示尚未提供。
        String content,
        // 完成（complete），使用 boolean 类型保存。
        boolean complete,
        // finish原因（finishReason），使用 String 类型保存；可为 null，表示尚未提供。
        String finishReason,
        // 模型（model），使用 String 类型保存；可为 null，表示尚未提供。
        String model,
        // 用量（usage），使用 JsonNode 类型保存。
        JsonNode usage,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // citations，使用 JsonNode 类型保存。
        JsonNode citations,
        // used长期记忆编号列表（usedMemoryIds），使用 JsonNode 类型保存。
        JsonNode usedMemoryIds) {}
