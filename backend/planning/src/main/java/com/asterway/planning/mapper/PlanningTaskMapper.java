package com.asterway.planning.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.planning.vo.PlanningTaskVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 仅负责计划任务及待处理的跨表查询，不承担单表写操作。 */
@Mapper
public interface PlanningTaskMapper {
    List<PlanningTaskVO> page(
            @Param("userId") long userId,
            @Param("q") GrowthQueryDTO query,
            @Param("pending") boolean pending);

    long count(
            @Param("userId") long userId,
            @Param("q") GrowthQueryDTO query,
            @Param("pending") boolean pending);
}
