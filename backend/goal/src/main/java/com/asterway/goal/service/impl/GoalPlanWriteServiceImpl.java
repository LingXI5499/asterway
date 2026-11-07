package com.asterway.goal.service.impl;

import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.PlanWritePayloadUtils;
import com.asterway.goal.dto.GoalSaveDTO;
import com.asterway.goal.service.GoalService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** 已确认方案的goal写入适配，业务规则仍由现有 Service 执行。 */
@Service
public class GoalPlanWriteServiceImpl implements PlanWritePort {
    private final GoalService service;
    private final ObjectMapper json;

    public GoalPlanWriteServiceImpl(GoalService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public String domain() { return "goal"; }

    @Override
    public JsonNode write(Action action, JsonNode command) {
        if (action != Action.CREATE) throw BusinessException.badRequest("不支持的方案操作");
        var dto = PlanWritePayloadUtils.saveDTO(command.path("data"), GoalSaveDTO.class, json);
        return json.valueToTree(service.create(dto));
    }
}
