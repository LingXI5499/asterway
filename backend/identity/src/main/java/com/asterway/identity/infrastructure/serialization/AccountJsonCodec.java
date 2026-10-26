package com.asterway.identity.infrastructure.serialization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 统一账户持久化材料的 JSON 编解码，沿用应用 ObjectMapper 的字段及日期配置。
@Component
public class AccountJsonCodec {

    private final ObjectMapper objectMapper;

    public AccountJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json;
        } catch (IOException exception) {
            throw new IllegalStateException("账户对象序列化失败", exception);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            T value = objectMapper.readValue(json, type);
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException("账户持久化材料无法读取", exception);
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            T value = objectMapper.readValue(json, type);
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException("账户持久化材料无法读取", exception);
        }
    }
}
