/*
 * 声明成长助手对话标题的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.agent.dto;

// 引入NotBlank，下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotBlank;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

/* 修改会话标题；版本用于乐观锁。 */
// 定义保存一组固定字段的 record AgentConversationTitleDTO；声明成长助手对话标题的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AgentConversationTitleDTO(
        // 要求文字既不为空，也不能只有空白。
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 标题（title），使用 String 类型保存；可为 null，表示尚未提供。
        @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题不能超过 100 字")
                // 继续传入或组合表达式 String title 的结果，完成上面尚未结束的调用或声明。
                String title,
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long version) {}
