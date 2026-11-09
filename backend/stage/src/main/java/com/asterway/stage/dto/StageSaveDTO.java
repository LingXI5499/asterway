/*
 * 声明阶段保存的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.stage.dto;

// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 创建、部分修改或完整替换资料；创建和替换时名称必填，状态单独修改。 */
// 定义保存一组固定字段的 record StageSaveDTO；声明阶段保存的请求字段和校验条件；接口收到数据后按这些限制检查。
public record StageSaveDTO(
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 100) String name,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 20000) String description,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 20000) String completionCriteria,
        // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate startDate,
        // 结束日期（endDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate endDate,
        // 要求数值为零或正数。
        // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Integer sortOrder,
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Long version) {}
