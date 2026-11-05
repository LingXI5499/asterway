package com.asterway.statistics.vo;

import com.asterway.common.result.PageResultVO;

import java.time.LocalDate;

/** 总／周／月／年／自定义的习惯总计及分组分页，统计不受当前目标筛选影响。 */
public record StatisticsHabitReportVO(
        String period,
        LocalDate dateFrom,
        LocalDate dateTo,
        String timeZone,
        String groupBy,
        StatisticsHabitTotalsVO totals,
        PageResultVO<StatisticsHabitGroupVO> groups) {}
