/*
 * 声明成长助手对话消息的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import java.util.List;

// 引入UUID，下面可以直接使用这个类型或静态成员。
import java.util.UUID;

/* 会话内发送；requestId 由浏览器生成，用于重放与幂等。 */
// 定义保存一组固定字段的 record AgentConversationMessageDTO；声明成长助手对话消息的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AgentConversationMessageDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank(message = "请先输入提问") @Size(max = 8000, message = "提问不能超过 8000 字")
                // 继续传入或组合表达式 String message 的结果，完成上面尚未结束的调用或声明。
                String message,
        // 要求该值必须存在，不能为 null。
        // 请求编号（requestId），使用 UUID 类型保存。
        @NotNull(message = "请求标识不能为空") UUID requestId,
        // 空列表表示本轮不引用；缺省时沿用会话保存的选择。
        @Size(max = 100) List<@Positive Long> knowledgeBaseIds) {}
