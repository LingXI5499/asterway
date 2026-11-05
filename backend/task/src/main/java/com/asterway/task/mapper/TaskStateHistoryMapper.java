package com.asterway.task.mapper;

import com.asterway.task.entity.TaskStateHistoryEntity;
import com.asterway.task.vo.TaskStateHistoryVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 主体从属历史，只允许追加和本人范围读取。 */
@Mapper
public interface TaskStateHistoryMapper {
    int insert(TaskStateHistoryEntity row);

    Long historicalSubject(@Param("userId") long userId, @Param("id") long id);

    long count(@Param("userId") long userId, @Param("id") long id);

    List<TaskStateHistoryVO> page(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("offset") int offset,
            @Param("size") int size);
}
