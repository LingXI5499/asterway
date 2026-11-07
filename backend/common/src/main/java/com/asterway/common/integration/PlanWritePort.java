package com.asterway.common.integration;

import com.fasterxml.jackson.databind.JsonNode;

/** 已确认方案的中立写入契约；只在 Java 父事务内调用，业务模块保留自身校验。 */
public interface PlanWritePort {
    enum Action { CREATE, WEEK, DAY, CURRENT_ASSIGNMENT, ASSIGN, CATEGORY }

    String domain();

    JsonNode write(Action action, JsonNode command);
}
