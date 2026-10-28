package com.asterway.growth.mapper;

import com.asterway.growth.model.TaskName;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

public interface TaskQueryMapper {

    List<TaskName> findNames(@Param("userId") UUID userId, @Param("taskIds") List<UUID> taskIds);
}
