package com.asterway.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;

/** JSONB 字符串与明确类型之间的转换，不提供未知类型的动态反序列化。 */
@Component
public class JsonUtils {

    private final ObjectMapper mapper;

    public JsonUtils(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("JSON 数据转换失败");
        }
    }

    public Map<String, Object> object(String value) {
        try {
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("JSON 对象格式无效");
        }
    }

    public String text(String value) {
        try {
            return mapper.readValue(value, String.class);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("JSON 字符串格式无效");
        }
    }
}
