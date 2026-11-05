package com.asterway.stage.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.stage.entity.StageEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Stage 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface StageMapper {
    StageEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<StageEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(StageEntity row);

    int update(StageEntity row);

    int softDelete(StageEntity row);

    java.math.BigDecimal progress(@Param("userId") long userId, @Param("id") long id);
}
