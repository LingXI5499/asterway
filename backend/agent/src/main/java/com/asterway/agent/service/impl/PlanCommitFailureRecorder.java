package com.asterway.agent.service.impl;

import com.asterway.agent.dto.AgentPlanCommitDTO;
import com.asterway.agent.mapper.PlanCommitMapper;
import com.asterway.common.utils.GrowthContextUtils;
import java.util.HashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 失败记录使用独立事务，保证业务回滚后仍留下可重试的失败结果。 */
@Service
public class PlanCommitFailureRecorder {
  private final PlanCommitMapper mapper;
  private final GrowthContextUtils context;

  public PlanCommitFailureRecorder(PlanCommitMapper mapper, GrowthContextUtils context) {
    this.mapper = mapper;
    this.context = context;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(AgentPlanCommitDTO dto, long userId, String errorCode, String errorMessage) {
    // 调用方业务事务必须已经结束，再以相同锁顺序记录失败。
    context.lockLifecycleUser(userId);
    var row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    if (row == null) {
      var values = new HashMap<String, Object>();
      values.put("userId", userId);
      values.put("runId", dto.runId());
      values.put("planVersionId", dto.planVersionId());
      values.put("idempotencyKey", dto.idempotencyKey());
      values.put("requestHash", dto.requestHash());
      mapper.insert(values);
      row = mapper.findByKeyForUpdate(dto.idempotencyKey());
    }
    if (row == null
        || ((Number) row.get("user_id")).longValue() != userId
        || !dto.requestHash().equals(row.get("request_hash"))
        || "SUCCEEDED".equals(row.get("status"))) {
      return;
    }
    String code = errorCode == null || errorCode.isBlank() ? "PLAN_COMMIT_FAILED" : errorCode;
    String message = errorMessage == null || errorMessage.isBlank() ? "规划提交失败" : errorMessage;
    mapper.markFailed(
        ((Number) row.get("id")).longValue(),
        code,
        message.length() > 1000 ? message.substring(0, 1000) : message);
  }
}
