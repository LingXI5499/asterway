package com.asterway.planning.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.planning.entity.DailyPlanEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** DailyPlan 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface DailyPlanMapper {
    DailyPlanEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<DailyPlanEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(DailyPlanEntity row);

    int update(DailyPlanEntity row);

    int softDelete(DailyPlanEntity row);

    DailyPlanEntity byDate(@Param("userId") long userId, @Param("date") LocalDate date);
}
