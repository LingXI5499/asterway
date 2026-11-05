package com.asterway.dashboard.mapper;

import com.asterway.dashboard.dto.DashboardReadDTO;
import com.asterway.dashboard.vo.*;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 只读看板 Mapper；各类事实独立聚合，避免多表一对多联结重复计数。 */
@Mapper
public interface DashboardMapper {
    DashboardMetricsVO overview(@Param("ctx") DashboardReadDTO context);

    DashboardExecutionVO current(@Param("ctx") DashboardReadDTO context);

    List<DashboardTaskVO> tasks(
            @Param("ctx") DashboardReadDTO context,
            @Param("section") String section,
            @Param("offset") int offset,
            @Param("size") int size);

    long tasksCount(@Param("ctx") DashboardReadDTO context, @Param("section") String section);

    List<DashboardGoalVO> goals(
            @Param("ctx") DashboardReadDTO context,
            @Param("offset") int offset,
            @Param("size") int size);

    long goalsCount(@Param("ctx") DashboardReadDTO context);

    List<DashboardHabitVO> habits(
            @Param("ctx") DashboardReadDTO context,
            @Param("offset") int offset,
            @Param("size") int size);

    long habitsCount(@Param("ctx") DashboardReadDTO context);
}
