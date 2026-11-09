/*
 * 声明任务安排保存的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.planning.dto;

// 引入Not空值（NotNull），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.NotNull;
// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;

/* 替换当前安排，预期尚无安排时 assignmentId/version 同时为空。 */
// 定义保存一组固定字段的 record AssignmentSaveDTO；声明任务安排保存的请求字段和校验条件；接口收到数据后按这些限制检查。
public record AssignmentSaveDTO(
        // 要求该值必须存在，不能为 null。
        // 要求数值大于零。
        // 每周方案编号（weeklyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
        @NotNull @Positive Long weeklyPlanId,
        // 要求数值大于零。
        // 每日方案编号（dailyPlanId），使用 Long 类型保存；可为 null，表示尚未提供。
        @Positive Long dailyPlanId,
        // 要求数值大于零。
        // 任务安排编号（assignmentId），使用 Long 类型保存；可为 null，表示尚未提供。
        @Positive Long assignmentId,
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Long version,
        // 要求数值为零或正数。
        // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Integer sortOrder) {}
