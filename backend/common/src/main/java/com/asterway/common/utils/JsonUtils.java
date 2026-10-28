package com.asterway.common.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

// 显式使用调用方的 ObjectMapper，避免静态全局配置改变既有安全材料编码。
public final class JsonUtils {

    private JsonUtils() {
    }

    public static String write(ObjectMapper objectMapper, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            return json;
        } catch (IOException exception) {
            throw new IllegalStateException("对象序列化失败", exception);
        }
    }

    public static <T> T read(ObjectMapper objectMapper, String json, Class<T> type) {
        try {
            T value = objectMapper.readValue(json, type);
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException("持久化材料无法读取", exception);
        }
    }

    public static <T> T read(ObjectMapper objectMapper, String json, TypeReference<T> type) {
        try {
            T value = objectMapper.readValue(json, type);
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException("持久化材料无法读取", exception);
        }
    }
}
