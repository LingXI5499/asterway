/*
 * 声明执行计时任务返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.execution.vo;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 执行资格的只读投影，供 Service 使用，不直接暴露为公开接口。 */
// 定义保存一组固定字段的 record ExecutionTaskVO；声明执行计时任务返回给页面的字段，不直接把数据库实体全部暴露出去。
public record ExecutionTaskVO(
        // 任务编号（taskId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long taskId,
        // 任务版本号（taskVersion），使用 Long 类型保存；可为 null，表示尚未提供。
        Long taskVersion,
        // 任务状态（taskStatus），使用 String 类型保存；可为 null，表示尚未提供。
        String taskStatus,
        // 任务安排编号（assignmentId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long assignmentId,
        // 所属周的周一日期（weekStartDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate weekStartDate,
        // 方案日期（planDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate planDate,
        // 目标编号（goalId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long goalId,
        // 阶段编号（stageId），使用 Long 类型保存；可为 null，表示尚未提供。
        Long stageId) {}
