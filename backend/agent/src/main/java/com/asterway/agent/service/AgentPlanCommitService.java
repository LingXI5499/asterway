package com.asterway.agent.service;

import com.asterway.agent.dto.AgentPlanCommitDTO;
import com.asterway.agent.vo.AgentPlanCommitVO;

/** 已确认方案的 Java 原子批量提交；同一方案版本只提交一次。 */
public interface AgentPlanCommitService {
    AgentPlanCommitVO commit(AgentPlanCommitDTO dto);
}
