package com.asterway.task.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.task.entity.TaskEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Task 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface TaskMapper {
    TaskEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<TaskEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(TaskEntity row);

    int update(TaskEntity row);

    int softDelete(TaskEntity row);
}
