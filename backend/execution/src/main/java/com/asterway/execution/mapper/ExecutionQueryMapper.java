package com.asterway.execution.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExecutionQueryMapper {

    boolean exists();

    int countUnfinished(@Param("userId") UUID userId);

    List<Map<String, Object>> findUnfinishedHints(@Param("userId") UUID userId);
}
