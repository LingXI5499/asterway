/*
 * 声明统计习惯复盘报告返回给页面的字段，不直接把数据库实体全部暴露出去。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.statistics.vo;

// 引入当前页结果（PageResultVO），下面可以直接使用这个类型或静态成员。
import com.asterway.common.result.PageResultVO;

// 引入不带时区的日历日期（LocalDate），下面可以直接使用这个类型或静态成员。
import java.time.LocalDate;

/* 总／周／月／年／自定义的习惯总计及分组分页，统计不受当前目标筛选影响。 */
// 定义保存一组固定字段的 record StatisticsHabitReportVO；声明统计习惯复盘报告返回给页面的字段，不直接把数据库实体全部暴露出去。
public record StatisticsHabitReportVO(
        // 统计周期（period），使用 String 类型保存；可为 null，表示尚未提供。
        String period,
        // 日期来源（dateFrom），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateFrom,
        // 日期To（dateTo），使用 LocalDate 类型保存；可为 null，表示尚未提供。
        LocalDate dateTo,
        // 用户使用的时区（timeZone），使用 String 类型保存；可为 null，表示尚未提供。
        String timeZone,
        // 分组按（groupBy），使用 String 类型保存；可为 null，表示尚未提供。
        String groupBy,
        // totals，使用 StatisticsHabitTotalsVO 类型保存。
        StatisticsHabitTotalsVO totals,
        // 分组列表（groups），使用 PageResultVO<StatisticsHabitGroupVO> 类型保存。
        PageResultVO<StatisticsHabitGroupVO> groups) {}
