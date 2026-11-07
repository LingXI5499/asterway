package com.asterway.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** 记忆、知识、规划与复盘公开入口；身份始终来自当前登录会话。 */
public interface AgentWorkspaceService {
    JsonNode request(String method, String resource, Map<String, String> query, JsonNode body);

    JsonNode upload(String resource, Map<String, String> query, String mimeType, byte[] content);
}
