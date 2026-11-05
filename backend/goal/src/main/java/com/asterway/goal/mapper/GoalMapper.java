package com.asterway.goal.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.goal.entity.GoalEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Goal 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface GoalMapper {
    GoalEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<GoalEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(GoalEntity row);

    int update(GoalEntity row);

    java.math.BigDecimal progress(@Param("userId") long userId, @Param("id") long id);
}
