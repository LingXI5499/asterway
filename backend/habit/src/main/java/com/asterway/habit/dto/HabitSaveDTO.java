/*
 * 声明习惯保存的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.dto;

// 引入JSON数据格式（JsonFormat），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.annotation.JsonFormat;

// 引入正数（Positive），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Positive;
// 引入非负数校验规则（PositiveOrZero），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.PositiveOrZero;
// 引入长度或数量校验规则（Size），下面可以直接使用这个类型或静态成员。
import jakarta.validation.constraints.Size;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 习惯资料与规则；不接收状态、日期补录或 userId。PATCH 的空值保留，PUT 的空值清空可选项。 */
// 定义保存一组固定字段的 record HabitSaveDTO；声明习惯保存的请求字段和校验条件；接口收到数据后按这些限制检查。
public record HabitSaveDTO(
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 100) String name,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 10000) String description,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
        @Size(max = 10000) String completionCriteria,
        // 要求数值大于零。
        // 分类编号（categoryId），使用 Long 类型保存；可为 null，表示尚未提供。
        @Positive Long categoryId,
        // cycle类型（cycleType），使用 String 类型保存；可为 null，表示尚未提供。
        String cycleType,
        // 限制集合条数或文字长度，边界以注解中的数值为准。
        // weekdays，使用 List<Integer> 类型保存。
        @Size(max = 7) List<Integer> weekdays,
        // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate startDate,
        // 规定该字段编码成 JSON 时使用的格式。
        // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
        @JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        // 要求数值为零或正数。
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        @PositiveOrZero Long version) {}
