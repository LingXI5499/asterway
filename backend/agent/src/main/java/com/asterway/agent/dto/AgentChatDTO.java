/*
 * 声明成长助手Chat的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* P1.1 只接收本次提问；身份与权限由认证上下文取得，不接受客户端指定。 */
// 定义保存一组固定字段的 record AgentChatDTO；声明成长助手Chat的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AgentChatDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 消息（message），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank(message = "请先输入提问") @Size(max = 8000, message = "提问不能超过 8000 字")
                // 继续传入或组合表达式 String message) {} 的结果，完成上面尚未结束的调用或声明。
                String message) {}
