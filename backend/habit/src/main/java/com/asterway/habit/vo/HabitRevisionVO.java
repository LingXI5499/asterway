/*
 * 声明习惯修订版本返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.habit.vo;

// 引入JSON数据格式（JsonFormat），下面可以直接使用这个类型或静态成员。
import com.fasterxml.jackson.annotation.JsonFormat;

// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入本地时间（LocalTime），下面可以直接使用这个类型或静态成员。
import java.time.LocalTime;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 完整规则及生命周期快照，不包含内部用户标识。 */
// 定义保存一组固定字段的 record HabitRevisionVO；声明习惯修订版本返回给页面的字段，不直接把数据库实体全部暴露出去。
public record HabitRevisionVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 修订版本No（revisionNo），使用 long 类型保存。
        long revisionNo,
        // 操作类型（actionType），使用 String 类型保存；可为 null，表示尚未提供。
        String actionType,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        String description,
        // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
        String completionCriteria,
        // 分类编号（categoryId），使用 String 类型保存；可为 null，表示尚未提供。
        String categoryId,
        // cycle类型（cycleType），使用 String 类型保存；可为 null，表示尚未提供。
        String cycleType,
        // weekdays，使用 List<Integer> 类型保存。
        List<Integer> weekdays,
        // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate startDate,
        // 规定该字段编码成 JSON 时使用的格式。
        // 预计时间（expectedTime），使用 LocalTime 类型保存；可为 null，表示尚未提供。
        @JsonFormat(pattern = "HH:mm") LocalTime expectedTime,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 删除标记（deleted），使用 boolean 类型保存。
        boolean deleted,
        // 生效日期（effectiveDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate effectiveDate,
        // 已修改At（changedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant changedAt,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone) {}
