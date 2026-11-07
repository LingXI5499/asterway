package com.asterway.agent.controller;

import com.asterway.agent.dto.AgentPlanCommitDTO;
import com.asterway.agent.service.AgentPlanCommitService;
import com.asterway.agent.vo.AgentPlanCommitVO;
import com.asterway.common.result.ResultVO;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 已确认规划方案的批量提交入口；只允许普通用户本人、需要 agent:workflow:manage。 */
@RestController
@RequestMapping("/asterway/agent/plans")
public class AgentPlanCommitController {
    private final AgentPlanCommitService service;

    public AgentPlanCommitController(AgentPlanCommitService service) {
        this.service = service;
    }

    @PostMapping("/commit")
    public ResultVO<AgentPlanCommitVO> commit(@Valid @RequestBody AgentPlanCommitDTO dto) {
        return ResultVO.success(service.commit(dto));
    }
}
