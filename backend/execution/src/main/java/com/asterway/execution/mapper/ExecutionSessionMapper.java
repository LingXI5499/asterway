package com.asterway.execution.mapper;

import com.asterway.common.security.ExecutionAccountData;
import com.asterway.execution.dto.ExecutionQueryDTO;
import com.asterway.execution.dto.ExecutionReadDTO;
import com.asterway.execution.entity.ExecutionSessionEntity;
import com.asterway.execution.vo.ExecutionSessionVO;
import com.asterway.execution.vo.ExecutionSummaryVO;
import com.asterway.execution.vo.ExecutionTaskVO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** 会话持久化与执行范围查询，全部 SQL 位于 XML，不依赖其他业务 Mapper。 */
@Mapper
public interface ExecutionSessionMapper {
    Long lockUser(@Param("userId") long userId);

    Long tryLockUser(@Param("userId") long userId);

    ExecutionAccountData account(@Param("userId") long userId);

    ExecutionTaskVO eligibleTask(
            @Param("userId") long userId,
            @Param("taskId") long taskId,
            @Param("today") LocalDate today);

    Long scopeExists(
            @Param("userId") long userId, @Param("scope") String scope, @Param("id") long id);

    ExecutionSessionEntity find(@Param("userId") long userId, @Param("id") long id);

    ExecutionSessionEntity running(@Param("userId") long userId);

    ExecutionSessionEntity unfinishedTask(
            @Param("userId") long userId, @Param("taskId") long taskId);

    List<ExecutionSessionEntity> unfinished(
            @Param("userId") long userId,
            @Param("scope") String scope,
            @Param("id") long id,
            @Param("limit") int limit);

    boolean runningInScope(
            @Param("userId") long userId, @Param("scope") String scope, @Param("id") long id);

    List<Long> dueUsers(@Param("now") Instant now, @Param("limit") int limit);

    int insert(ExecutionSessionEntity row);

    int changeState(
            @Param("row") ExecutionSessionEntity row,
            @Param("expected") long expected,
            @Param("now") Instant now);

    int heartbeat(
            @Param("userId") long userId,
            @Param("id") long id,
            @Param("segmentId") long segmentId,
            @Param("sessionId") String sessionId,
            @Param("now") Instant now);

    int startTask(
            @Param("userId") long userId,
            @Param("taskId") long taskId,
            @Param("version") long version,
            @Param("now") Instant now);

    ExecutionSessionVO view(@Param("ctx") ExecutionReadDTO context, @Param("id") long id);

    List<ExecutionSessionVO> page(
            @Param("ctx") ExecutionReadDTO context,
            @Param("q") ExecutionQueryDTO query,
            @Param("offset") int offset);

    long count(@Param("ctx") ExecutionReadDTO context, @Param("q") ExecutionQueryDTO query);

    ExecutionSummaryVO summary(
            @Param("ctx") ExecutionReadDTO context, @Param("q") ExecutionQueryDTO query);
}
