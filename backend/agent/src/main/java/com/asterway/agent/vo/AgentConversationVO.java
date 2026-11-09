/*
 * 声明成长助手对话返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
import java.util.List;

/* 会话列表项；摘要边界表示已压缩到的连续消息序号。 */
// 定义保存一组固定字段的 record AgentConversationVO；声明成长助手对话返回给页面的字段，不直接把数据库实体全部暴露出去。
public record AgentConversationVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
        String title,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 汇总Through消息No（summaryThroughMessageNo），使用 long 类型保存。
        long summaryThroughMessageNo,
        // 最后一次消息At（lastMessageAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant lastMessageAt,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant updatedAt,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 会话当前保存的知识库范围。
        List<String> knowledgeBaseIds) {}
