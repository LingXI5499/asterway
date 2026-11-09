/*
 * 声明任务返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.task.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 安全返回对象；BIGINT 标识统一作为字符串返回。 */
// 定义保存一组固定字段的 record TaskVO；声明任务返回给页面的字段，不直接把数据库实体全部暴露出去。
public record TaskVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 阶段编号（stageId），使用 String 类型保存；可为 null，表示尚未提供。
        String stageId,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        String description,
        // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
        String completionCriteria,
        // priority，使用 String 类型保存；可为 null，表示尚未提供。
        String priority,
        // 权重（weight），使用 Integer 类型保存；可为 null，表示尚未提供。
        Integer weight,
        // estimated分钟数（estimatedMinutes），使用 Integer 类型保存；可为 null，表示尚未提供。
        Integer estimatedMinutes,
        // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate startDate,
        // 截止日期（dueDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dueDate,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 排序顺序（sortOrder），使用 Integer 类型保存；可为 null，表示尚未提供。
        Integer sortOrder,
        // 已完成At（completedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant completedAt,
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        Long version,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant updatedAt,
        // 目标编号（goalId），使用 String 类型保存；可为 null，表示尚未提供。
        String goalId,
        // 目标名称（goalName），使用 String 类型保存；可为 null，表示尚未提供。
        String goalName,
        // 阶段名称（stageName），使用 String 类型保存；可为 null，表示尚未提供。
        String stageName,
        // 任务安排状态（assignmentStatus），使用 String 类型保存；可为 null，表示尚未提供。
        String assignmentStatus,
        // 每周方案编号（weeklyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
        String weeklyPlanId,
        // 每日方案编号（dailyPlanId），使用 String 类型保存；可为 null，表示尚未提供。
        String dailyPlanId,
        // 目标已归档At（goalArchivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant goalArchivedAt) {}
