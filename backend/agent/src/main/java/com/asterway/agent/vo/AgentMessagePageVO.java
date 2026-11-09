/*
 * 声明成长助手消息当前页返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.vo;

// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 历史消息游标页；items 按消息序号升序返回。 */
// 定义保存一组固定字段的 record AgentMessagePageVO；声明成长助手消息当前页返回给页面的字段，不直接把数据库实体全部暴露出去。
public record AgentMessagePageVO(
        // 数据列表（items），使用 List<AgentMessageVO> 类型保存。
        // 是否存在More（hasMore），使用 boolean 类型保存。
        // 下一个变化前消息No（nextBeforeMessageNo），使用 Long 类型保存；可为 null，表示尚未提供。
        List<AgentMessageVO> items, boolean hasMore, Long nextBeforeMessageNo) {}
