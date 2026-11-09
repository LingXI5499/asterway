/*
 * 声明成长助手对话创建的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.util.List;

/* 创建会话；标题可省略，服务端使用“新对话”。 */
// 定义保存一组固定字段的 record AgentConversationCreateDTO；声明成长助手对话创建的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AgentConversationCreateDTO(
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 100, message = "标题不能超过 100 字") String title,
        // 新会话默认不选资料，传入时逐个核验所属用户。
        @Size(max = 100) List<@Positive Long> knowledgeBaseIds) {}
