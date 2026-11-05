package com.asterway.planning.mapper;

import com.asterway.common.dto.GrowthQueryDTO;
import com.asterway.planning.entity.TaskAssignmentEntity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** TaskAssignment 单表持久化链路，SQL 位于对应 XML。 */
@Mapper
public interface TaskAssignmentMapper {
    TaskAssignmentEntity findById(@Param("userId") long userId, @Param("id") long id);

    List<TaskAssignmentEntity> page(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    long count(@Param("userId") long userId, @Param("q") GrowthQueryDTO query);

    int insert(TaskAssignmentEntity row);

    int update(TaskAssignmentEntity row);

    TaskAssignmentEntity current(@Param("userId") long userId, @Param("taskId") long taskId);
}
