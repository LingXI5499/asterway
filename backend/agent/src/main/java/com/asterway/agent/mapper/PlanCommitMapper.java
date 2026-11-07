package com.asterway.agent.mapper;

import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** agent.plan_commit 幂等提交记录；跨 schema 只读写提交记录，不修改会话表。 */
@Mapper
public interface PlanCommitMapper {
  Map<String, Object> findByKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

  Map<String, Object> findConfirmedPlan(
      @Param("userId") long userId,
      @Param("runId") long runId,
      @Param("planVersionId") long planVersionId);

  int insert(Map<String, Object> row);

  int markRunning(@Param("id") long id, @Param("maxAttempts") int maxAttempts);

  int markSucceeded(@Param("id") long id, @Param("resultJson") String resultJson);

  int markFailed(
      @Param("id") long id,
      @Param("errorCode") String errorCode,
      @Param("errorMessage") String errorMessage);
}
