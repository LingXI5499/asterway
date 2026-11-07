package com.asterway.habit.service.impl;

import com.asterway.common.integration.PlanWritePort;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.utils.PlanWritePayloadUtils;
import com.asterway.habit.dto.HabitSaveDTO;
import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.dto.HabitCategorySaveDTO;
import com.asterway.habit.service.HabitService;
import com.asterway.habit.service.HabitCategoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/** 习惯独立创建；分类按当前用户的真实数据复用。 */
@Service
public class HabitPlanWriteServiceImpl implements PlanWritePort {
    private final HabitService habits;
    private final HabitCategoryService categories;
    private final ObjectMapper json;

    public HabitPlanWriteServiceImpl(HabitService habits, HabitCategoryService categories, ObjectMapper json) {
        this.habits = habits;
        this.categories = categories;
        this.json = json;
    }

    @Override
    public String domain() { return "habit"; }

    @Override
    public JsonNode write(Action action, JsonNode command) {
        if (action == Action.CREATE) {
            var data = (com.fasterxml.jackson.databind.node.ObjectNode) command.path("data").deepCopy();
            if (data.hasNonNull("expectedTime")) {
                data.put("expectedTime", java.time.LocalTime.parse(data.path("expectedTime").asText())
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
            }
            return json.valueToTree(habits.create(PlanWritePayloadUtils.saveDTO(data, HabitSaveDTO.class, json)));
        }
        if (action != Action.CATEGORY) throw BusinessException.badRequest("不支持的习惯操作");
        String name = command.path("name").asText();
        HabitQueryDTO query = new HabitQueryDTO();
        query.setSize(100);
        query.setKeyword(name);
        for (int page = 1; ; page++) {
            query.setPage(page);
            var result = categories.page(query);
            for (var item : result.items()) {
                if (name.equals(item.name())) return json.valueToTree(item);
            }
            if ((long) page * 100 >= result.total()) break;
        }
        return json.valueToTree(categories.create(new HabitCategorySaveDTO(name, null, null)));
    }
}
