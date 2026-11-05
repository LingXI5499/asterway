package com.asterway.execution.mapper;

import com.asterway.execution.dto.ExecutionReadDTO;
import com.asterway.execution.entity.ExecutionSegmentEntity;
import com.asterway.execution.vo.ExecutionSegmentVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.util.List;

/** 时间段从属于会话，只由会话 Service 在同一事务内写入。 */
@Mapper
public interface ExecutionSegmentMapper {
    ExecutionSegmentEntity open(
            @Param("userId") long userId, @Param("executionId") long executionId);

    int nextNumber(@Param("userId") long userId, @Param("executionId") long executionId);

    int insert(ExecutionSegmentEntity row);

    int close(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("endedAt") Instant endedAt,
            @Param("reason") String reason,
            @Param("now") Instant now);

    int shorten(
            @Param("userId") long userId,
            @Param("executionId") long executionId,
            @Param("deadline") Instant deadline,
            @Param("now") Instant now);

    List<ExecutionSegmentVO> page(
            @Param("ctx") ExecutionReadDTO context,
            @Param("executionId") long executionId,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(@Param("userId") long userId, @Param("executionId") long executionId);
}
