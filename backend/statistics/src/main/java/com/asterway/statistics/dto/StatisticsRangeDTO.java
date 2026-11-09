/*
 * 声明统计区间的请求字段和校验条件；接口收到数据后按这些限制检查。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.dto;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* Service 归一化后的统计区间；本地日期含首尾，时间点采用半开区间。 */
// 定义保存一组固定字段的 record StatisticsRangeDTO；声明统计区间的请求字段和校验条件；接口收到数据后按这些限制检查。
public record StatisticsRangeDTO(
        // 用户编号（userId），使用 long 类型保存。
        long userId,
        // 统计周期（period），使用 String 类型保存；可为 null，表示尚未提供。
        String period,
        // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateFrom,
        // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateTo,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 来源Instant（fromInstant），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant fromInstant,
        // toInstant，使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant toInstant,
        // 当前时间（now），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant now,
        // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long goalId,
        // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long stageId,
        // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long taskId) {}
