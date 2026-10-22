package com.asterway.app.web;

import com.asterway.app.web.contract.ApiResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseContractTest {

    @Test
    void shouldIncludeNullableFields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // 模拟项目的全局配置：默认忽略 null 字段
        objectMapper.setSerializationInclusion(
                JsonInclude.Include.NON_NULL
        );

        String json = objectMapper.writeValueAsString(
                ApiResponse.success()
        );

        JsonNode root = objectMapper.readTree(json);

        assertEquals("OK", root.get("code").asText());
        assertEquals("操作成功", root.get("message").asText());

        assertTrue(root.has("data"));
        assertTrue(root.get("data").isNull());

        assertTrue(root.has("traceId"));
        assertTrue(root.get("traceId").isNull());
    }
}