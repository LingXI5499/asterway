/*
 * 声明通知Handle的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.notification.dto;

// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

/* 处理提醒的消息版本；确认完成还须提供界面读取到的目标版本。 */
// 定义保存一组固定字段的 record NotificationHandleDTO；声明通知Handle的请求字段和校验条件；接口收到数据后按这些限制检查。
public record NotificationHandleDTO(
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        // 目标版本号（goalVersion），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long version, @PositiveOrZero Long goalVersion) {}
