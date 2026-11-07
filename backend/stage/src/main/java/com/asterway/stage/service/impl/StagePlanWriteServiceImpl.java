package com.asterway.stage.service.impl;

import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.PlanWritePayloadUtils;
import com.asterway.stage.dto.StageSaveDTO;
import com.asterway.stage.service.StageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** 已确认方案的stage写入适配，业务规则仍由现有 Service 执行。 */
@Service
public class StagePlanWriteServiceImpl implements PlanWritePort {
    private final StageService service;
    private final ObjectMapper json;

    public StagePlanWriteServiceImpl(StageService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public String domain() { return "stage"; }

    @Override
    public JsonNode write(Action action, JsonNode command) {
        if (action != Action.CREATE) throw BusinessException.badRequest("不支持的方案操作");
        var dto = PlanWritePayloadUtils.saveDTO(command.path("data"), StageSaveDTO.class, json);
        return json.valueToTree(service.create(command.path("parentId").asLong(), dto));
    }
}
