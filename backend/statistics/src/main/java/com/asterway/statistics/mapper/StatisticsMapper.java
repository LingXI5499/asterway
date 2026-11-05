package com.asterway.statistics.mapper;

import com.asterway.common.security.GrowthScopeData;
import com.asterway.statistics.dto.StatisticsRangeDTO;
import com.asterway.statistics.vo.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 只读事实统计，全部语句使用绑定参数并限定当前用户。 */
@Mapper
public interface StatisticsMapper {
    LocalDate firstDate(@Param("userId") long userId, @Param("zone") String zone);

    GrowthScopeData scope(
            @Param("userId") long userId, @Param("type") String type, @Param("id") long id);

    StatisticsExecutionVO execution(@Param("ctx") StatisticsRangeDTO context);

    List<StatisticsExecutionDayVO> executionDays(@Param("ctx") StatisticsRangeDTO context);

    StatisticsHabitTotalsVO habitTotals(@Param("ctx") StatisticsRangeDTO context);

    List<StatisticsHabitDayVO> habitDays(@Param("ctx") StatisticsRangeDTO context);

    List<StatisticsHabitGroupVO> habitGroups(
            @Param("ctx") StatisticsRangeDTO context,
            @Param("groupBy") String groupBy,
            @Param("offset") int offset,
            @Param("size") int size);

    long habitGroupCount(
            @Param("ctx") StatisticsRangeDTO context, @Param("groupBy") String groupBy);

    List<StatisticsStateDayVO> stateDays(@Param("ctx") StatisticsRangeDTO context);

    StatisticsStateTotalsVO stateTotals(@Param("ctx") StatisticsRangeDTO context);

    List<StatisticsTimelineVO> timeline(
            @Param("ctx") StatisticsRangeDTO context,
            @Param("offset") int offset,
            @Param("size") int size);

    long timelineCount(@Param("ctx") StatisticsRangeDTO context);

    List<StatisticsObjectVO> objects(@Param("ctx") StatisticsRangeDTO context);
}
