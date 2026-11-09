/*
 * 声明Dashboard习惯返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.dashboard.vo;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;

/* 今日习惯读取应做规则或实际打卡快照，不改变规则和打卡。 */
// 定义保存一组固定字段的 record DashboardHabitVO；声明Dashboard习惯返回给页面的字段，不直接把数据库实体全部暴露出去。
public record DashboardHabitVO(
        // 习惯编号（habitId），使用 String 类型保存；可为 null，表示尚未提供。
        String habitId,
        // 修订版本编号（revisionId），使用 String 类型保存；可为 null，表示尚未提供。
        String revisionId,
        // 分类编号（categoryId），使用 String 类型保存；可为 null，表示尚未提供。
        String categoryId,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 规定该字段编码成 JSON 时使用的格式。
        // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 删除标记At（deletedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant deletedAt,
        // 打卡编号（checkinId），使用 String 类型保存；可为 null，表示尚未提供。
        String checkinId,
        // 已完成数量（doneCount），使用 int 类型保存。
        int doneCount,
        // 应完成数量（dueCount），使用 int 类型保存。
        int dueCount,
        // 是否可以取消（canCancel），使用 boolean 类型保存。
        boolean canCancel,
        // 是否可以打卡（canCheckin），使用 boolean 类型保存。
        boolean canCheckin) {}
