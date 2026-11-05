package com.asterway.planning.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.planning.entity.WeeklyPlanEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** WeeklyPlan 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface WeeklyPlanMapper {
    WeeklyPlanEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<WeeklyPlanEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(WeeklyPlanEntity row);

    int update(WeeklyPlanEntity row);

    int softDelete(WeeklyPlanEntity row);

    WeeklyPlanEntity byDate(@Param("userId") long userId, @Param("date") LocalDate date);
}
