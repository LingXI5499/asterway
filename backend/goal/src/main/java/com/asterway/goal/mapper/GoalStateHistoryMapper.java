package com.asterway.goal.mapper;

import com.asterway.goal.entity.GoalStateHistoryEntity;
import com.asterway.goal.vo.GoalStateHistoryVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 主体从属历史，只允许追加和本人范围读取。 */
@Mapper
public interface GoalStateHistoryMapper {
    int insert(GoalStateHistoryEntity row);

    Long historicalSubject(@Param("userId") long userId, @Param("id") long id);

    long count(@Param("userId") long userId, @Param("id") long id);

    List<GoalStateHistoryVO> page(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("offset") int offset,
            @Param("size") int size);
}
