/*
 * 声明目标返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.goal.vo;

// 引入成长业务Overview（GrowthOverviewVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.GrowthOverviewVO;

// 引入Big精确小数（BigDecimal），下面可以直接使用这个类型或静态成员。
import java.math.BigDecimal;
// 引入表示绝对时刻的时间点（Instant），下面可以直接使用这个类型或静态成员。
import java.time.Instant;
// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;

/* 安全返回对象；BIGINT 标识统一作为字符串返回。 */
// 定义保存一组固定字段的 record GoalVO；声明目标返回给页面的字段，不直接把数据库实体全部暴露出去。
public record GoalVO(
        // 编号（id），使用 String 类型保存；可为 null，表示尚未提供。
        String id,
        // 名称（name），使用 String 类型保存；可为 null，表示尚未提供。
        String name,
        // 说明（description），使用 String 类型保存；可为 null，表示尚未提供。
        String description,
        // 完成情况判断标准（completionCriteria），使用 String 类型保存；可为 null，表示尚未提供。
        String completionCriteria,
        // 开始日期（startDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate startDate,
        // 预计结束日期（expectedEndDate），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate expectedEndDate,
        // 状态（status），使用 String 类型保存；可为 null，表示尚未提供。
        String status,
        // 已归档At（archivedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant archivedAt,
        // 版本号（version），使用 Long 类型保存；可为 null，表示尚未提供。
        Long version,
        // 创建时At（createdAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant createdAt,
        // 更新时At（updatedAt），使用 Instant 类型保存；可为 null，表示尚未提供。
        Instant updatedAt,
        // 进度（progress），使用 BigDecimal 类型保存。
        BigDecimal progress,
        // 阶段列表（stages），使用 List<GrowthOverviewVO> 类型保存。
        List<GrowthOverviewVO> stages,
        // 完成情况ConditionMet（completionConditionMet），使用 boolean 类型保存。
        boolean completionConditionMet,
        // 完成情况轮（completionRound），使用 long 类型保存。
        long completionRound) {}
