package com.asterway.agent.service.impl;

import com.asterway.agent.service.AgentWorkspaceService;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.integration.AgentClient;
import com.asterway.common.integration.AgentRequestContext;
import com.asterway.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 固定资源代理；Python 仍复核有效身份、权限、数据归属和完整业务参数。 */
@Service
public class AgentWorkspaceServiceImpl implements AgentWorkspaceService {
    private static final int MAX_FILE_BYTES = 26_214_400;
    private final AgentClient client;

    public AgentWorkspaceServiceImpl(AgentClient client) {
        this.client = client;
    }

    @Override
    public JsonNode request(String method, String resource, Map<String, String> query, JsonNode body) {
        AgentRequestContext context = context(permission(method, resource));
        JsonNode payload = body == null ? null : body.deepCopy();
        if (payload instanceof ObjectNode object) {
            // 前端 ID 保留字符串精度，内部严格契约使用 JSON 整数。
            for (String field : new String[] {"conversationId", "sourceTurnId", "turnId", "supersedesId", "planVersionId", "sourceRunId"}) {
                JsonNode value = object.get(field);
                if (value != null && value.isTextual()) {
                    try {
                        long id = Long.parseLong(value.asText());
                        if (id <= 0) throw new NumberFormatException();
                        object.put(field, id);
                    } catch (NumberFormatException error) {
                        throw BusinessException.badRequest("业务标识必须为正整数");
                    }
                }
            }
        }
        return client.workspaceRequest(method, "/internal/agent/" + resource, query, payload, context);
    }

    @Override
    public JsonNode upload(String resource, Map<String, String> query, String mimeType, byte[] content) {
        if (content.length == 0 || content.length > MAX_FILE_BYTES) {
            throw new BusinessException(413, "KNOWLEDGE_SIZE_INVALID", "文件为空或超过 25 MB");
        }
        return client.workspaceUpload("/internal/agent/" + resource, query, mimeType, content,
                context("agent:knowledge:manage"));
    }

    private String permission(String method, String resource) {
        if (resource.matches("reviews/[1-9][0-9]*/proposals")) return "agent:workflow:manage";
        if (resource.equals("runs/tasks") || resource.matches("runs/tasks/[1-9][0-9]*/retry")) return "agent:run:read";
        String subject;
        if (resource.startsWith("memories")) subject = "memory";
        else if (resource.startsWith("knowledge")) subject = "knowledge";
        else if (resource.startsWith("planning/proposals")) subject = "workflow";
        else if (resource.startsWith("reviews")) subject = "review";
        else if (resource.equals("runs/calls")) return "agent:run:read";
        else throw BusinessException.notFound();
        return "agent:" + subject + ("GET".equals(method) ? ":read" : ":manage");
    }

    private AgentRequestContext context(String permission) {
        var actor = SecurityUtils.requirePermission(permission);
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            throw BusinessException.unauthorized();
        }
        String authorization = attrs.getRequest().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.unauthorized();
        }
        return new AgentRequestContext(actor.userId(), authorization, MDC.get("traceId"));
    }
}
