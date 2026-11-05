package com.asterway.stage.mapper;

import com.asterway.stage.entity.StageStateHistoryEntity;
import com.asterway.stage.vo.StageStateHistoryVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 主体从属历史，只允许追加和本人范围读取。 */
@Mapper
public interface StageStateHistoryMapper {
    int insert(StageStateHistoryEntity row);

    Long historicalSubject(@Param("userId") long userId, @Param("id") long id);

    long count(@Param("userId") long userId, @Param("id") long id);

    List<StageStateHistoryVO> page(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("offset") int offset,
            @Param("size") int size);
}
