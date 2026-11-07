package com.asterway.planning.service.impl;

import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.PlanWritePayloadUtils;
import com.asterway.planning.dto.AssignmentSaveDTO;
import com.asterway.planning.service.WeeklyPlanService;
import com.asterway.planning.service.DailyPlanService;
import com.asterway.planning.service.TaskAssignmentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/** 计划始终复用真实任务与现有计划，不复制任务实体。 */
@Service
public class PlanningPlanWriteServiceImpl implements PlanWritePort {
    private final WeeklyPlanService weeks;
    private final DailyPlanService days;
    private final TaskAssignmentService assignments;
    private final ObjectMapper json;

    public PlanningPlanWriteServiceImpl(WeeklyPlanService weeks, DailyPlanService days,
            TaskAssignmentService assignments, ObjectMapper json) {
        this.weeks = weeks;
        this.days = days;
        this.assignments = assignments;
        this.json = json;
    }

    @Override
    public String domain() { return "planning"; }

    @Override
    public JsonNode write(Action action, JsonNode command) {
        Object value = switch (action) {
            case WEEK -> weeks.byWeek(LocalDate.parse(command.path("date").asText()), true);
            case DAY -> days.byDate(command.path("parentId").asLong(), LocalDate.parse(command.path("date").asText()));
            case CURRENT_ASSIGNMENT -> assignments.get(command.path("parentId").asLong());
            case ASSIGN -> assignments.assign(command.path("parentId").asLong(),
                    PlanWritePayloadUtils.saveDTO(command.path("data"), AssignmentSaveDTO.class, json));
            default -> throw BusinessException.badRequest("不支持的计划操作");
        };
        return json.valueToTree(value);
    }
}
