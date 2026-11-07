package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** 中立命令到模块 DTO 的受控转换；模型逻辑引用不会混入业务 DTO。 */
public final class PlanWritePayloadUtils {
    private PlanWritePayloadUtils() {}

    public static <T> T saveDTO(JsonNode source, Class<T> type, ObjectMapper mapper) {
        ObjectNode fields = mapper.createObjectNode();
        for (var component : type.getRecordComponents()) {
            String name = component.getName();
            String input = name;
            if (!source.has(input)) {
                input = switch (name) {
                    case "name" -> "title";
                    case "expectedEndDate", "endDate" -> "dueDate";
                    default -> name;
                };
            }
            if (source.has(input)) {
                fields.set(name, source.get(input));
            }
        }
        try {
            return mapper.convertValue(fields, type);
        } catch (IllegalArgumentException error) {
            throw BusinessException.badRequest("方案中的业务字段格式不正确");
        }
    }
}
