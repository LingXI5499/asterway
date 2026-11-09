/*
 * 声明目标归档的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.dto;

// 引入Assert成立（AssertTrue），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.AssertTrue;
// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

/* 归档固定结束全部未结束执行并移出计划，必须明确确认，不接受旧保留安排策略。 */
// 定义保存一组固定字段的 record GoalArchiveDTO；声明目标归档的请求字段和校验条件；接口收到数据后按这些限制检查。
public record GoalArchiveDTO(
        // 要求该值必须存在，不能为 null。
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @PositiveOrZero Long version,
        // 要求该值必须存在，不能为 null。
        // 使用 AssertTrue 注解配置下面的类、方法或参数。
        // 已确认（confirmed），使用 Boolean 类型保存；可为 null，表示尚未提供。
        @NotNull @AssertTrue Boolean confirmed) {
}
