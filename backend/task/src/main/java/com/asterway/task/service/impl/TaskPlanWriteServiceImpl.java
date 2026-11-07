package com.asterway.task.service.impl;

import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.PlanWritePayloadUtils;
import com.asterway.task.dto.TaskSaveDTO;
import com.asterway.task.service.TaskService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** 已确认方案的task写入适配，业务规则仍由现有 Service 执行。 */
@Service
public class TaskPlanWriteServiceImpl implements PlanWritePort {
    private final TaskService service;
    private final ObjectMapper json;

    public TaskPlanWriteServiceImpl(TaskService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public String domain() { return "task"; }

    @Override
    public JsonNode write(Action action, JsonNode command) {
        if (action != Action.CREATE) throw BusinessException.badRequest("不支持的方案操作");
        var dto = PlanWritePayloadUtils.saveDTO(command.path("data"), TaskSaveDTO.class, json);
        return json.valueToTree(service.create(command.path("parentId").asLong(), dto));
    }
}
