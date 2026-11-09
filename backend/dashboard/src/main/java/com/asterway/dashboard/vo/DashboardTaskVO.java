/*
 * 声明Dashboard任务返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 今日或本周未定日任务，不重复保存任务数据。 */
// 定义保存一组固定字段的 record DashboardTaskVO；声明Dashboard任务返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardTaskVO(
        // 任务编号（taskId），使用 String 类型保存；可为 null，表示尚未提供。
        String taskId,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 版本号（version），使用 long 类型保存。
        long version,
        // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
        String goalId,
        // 阶段编号（stageId），使用 String 类型保存；可为 null，表示尚未提供。
        String stageId,
        // 任务安排编号（assignmentId），使用 String 类型保存；可为 null，表示尚未提供。
        String assignmentId,
        // 方案日期（planDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate planDate,
        // section，使用 String 类型保存；可为 null，表示尚未提供。
        String section,
        // priority，使用 String 类型保存；可为 null，表示尚未提供。
        String priority,
        // 截止日期（dueDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dueDate,
        // 排序顺序（sortOrder），使用 int 类型保存。
        int sortOrder) {}
