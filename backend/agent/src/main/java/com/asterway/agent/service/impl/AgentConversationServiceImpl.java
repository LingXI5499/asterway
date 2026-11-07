package com.asterway.agent.service.impl;

import com.asterway.agent.dto.AgentConversationCreateDTO;
import com.asterway.agent.dto.AgentConversationMessageDTO;
import com.asterway.agent.dto.AgentConversationQueryDTO;
import com.asterway.agent.dto.AgentConversationTitleDTO;
import com.asterway.agent.service.AgentConversationService;
import com.asterway.agent.vo.AgentConversationDetailVO;
import com.asterway.agent.vo.AgentConversationVO;
import com.asterway.agent.vo.AgentMessagePageVO;
import com.asterway.agent.vo.AgentMessageVO;
import com.asterway.agent.vo.AgentSendResultVO;
import com.asterway.agent.vo.AgentTurnVO;
import com.asterway.common.dto.GrowthVersionDTO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.integration.AgentClient;
import com.asterway.common.integration.AgentRequestContext;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 每个操作在同步阶段完成权限检查，异步流只使用已捕获的请求上下文。 */
@Service
public class AgentConversationServiceImpl implements AgentConversationService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AgentConversationServiceImpl.class);
    private final AgentClient client;

    public AgentConversationServiceImpl(AgentClient client) {
        this.client = client;
    }

    @Override
    public AgentConversationDetailVO create(AgentConversationCreateDTO dto) {
        return detail(client.createConversation(dto.title(), context("agent:conversation:create")));
    }

    @Override
    public PageResultVO<AgentConversationVO> page(AgentConversationQueryDTO query) {
        Map<String, String> parameters = new LinkedHashMap<>();
        if (query.getStatus() != null) {
            parameters.put("status", query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            parameters.put("keyword", query.getKeyword().trim());
        }
        parameters.put("page", Integer.toString(query.getPage()));
        parameters.put("size", Integer.toString(query.getSize()));
        JsonNode result =
                client.conversationPage(parameters, context("agent:conversation:list"));
        List<AgentConversationVO> items = new ArrayList<>();
        for (JsonNode node : result.path("items")) {
            items.add(conversation(node));
        }
        return new PageResultVO<>(
                items,
                result.path("total").asLong(),
                result.path("page").asInt(query.getPage()),
                result.path("size").asInt(query.getSize()));
    }

    @Override
    public AgentConversationDetailVO detail(long conversationId) {
        return detail(client.conversation(conversationId, context("agent:conversation:read")));
    }

    @Override
    public AgentConversationDetailVO rename(
            long conversationId, AgentConversationTitleDTO dto) {
        return detail(
                client.renameConversation(
                        conversationId,
                        dto.title().trim(),
                        dto.version(),
                        context("agent:conversation:update")));
    }

    @Override
    public AgentConversationDetailVO close(long conversationId, GrowthVersionDTO dto) {
        return detail(
                client.closeConversation(
                        conversationId, dto.version(), context("agent:conversation:close")));
    }

    @Override
    public void delete(long conversationId, long version) {
        client.deleteConversation(
                conversationId, version, context("agent:conversation:delete"));
    }

    @Override
    public AgentMessagePageVO messages(long conversationId, Long beforeMessageNo, int size) {
        JsonNode result =
                client.conversationMessages(
                        conversationId,
                        beforeMessageNo,
                        size,
                        context("agent:conversation:read"));
        List<AgentMessageVO> items = new ArrayList<>();
        for (JsonNode node : result.path("items")) {
            items.add(message(node));
        }
        Long nextBefore =
                result.hasNonNull("nextBeforeMessageNo")
                        ? result.get("nextBeforeMessageNo").asLong()
                        : null;
        return new AgentMessagePageVO(items, result.path("hasMore").asBoolean(false), nextBefore);
    }

    @Override
    public AgentTurnVO turn(long conversationId, long turnId) {
        return turn(
                client.conversationTurn(
                        conversationId, turnId, context("agent:conversation:read")));
    }

    @Override
    public AgentTurnVO cancel(long conversationId, long turnId, GrowthVersionDTO dto) {
        return turn(
                client.cancelTurn(
                        conversationId,
                        turnId,
                        dto.version(),
                        context("agent:chat:send")));
    }

    @Override
    public AgentSendResultVO send(long conversationId, AgentConversationMessageDTO dto) {
        JsonNode result =
                client.sendMessage(
                        conversationId,
                        dto.message(),
                        dto.requestId().toString(),
                        context("agent:chat:send"));
        return new AgentSendResultVO(
                result.path("reply").asText(),
                result.path("model").asText(),
                result.path("finishReason").asText(),
                result.path("conversationId").asText(),
                result.path("turnId").asText(),
                result.path("requestId").asText(),
                result.path("turnVersion").asLong());
    }

    @Override
    public StreamingResponseBody stream(long conversationId, AgentConversationMessageDTO dto) {
        AgentRequestContext context = context("agent:chat:send");
        client.requireConfigured();
        return output -> {
            try {
                client.streamMessage(
                        conversationId, dto.message(), dto.requestId().toString(), context, output);
            } catch (IOException error) {
                // 浏览器主动停止后无法再写出结果；上游由客户端 try-with-resources 关闭。
                LOGGER.debug("Agent 会话流客户端已断开，traceId={}", context.traceId());
            }
        };
    }

    private AgentRequestContext context(String permission) {
        var actor = SecurityUtils.requirePermission(permission);
        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attrs)) {
            throw BusinessException.unauthorized();
        }
        String authorization = attrs.getRequest().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw BusinessException.unauthorized();
        }
        return new AgentRequestContext(actor.userId(), authorization, MDC.get("traceId"));
    }

    private AgentConversationVO conversation(JsonNode node) {
        return new AgentConversationVO(
                node.path("id").asText(),
                node.path("title").asText(),
                node.path("status").asText(),
                node.path("summaryThroughMessageNo").asLong(),
                instant(node, "lastMessageAt"),
                instant(node, "createdAt"),
                instant(node, "updatedAt"),
                node.path("version").asLong());
    }

    private AgentConversationDetailVO detail(JsonNode node) {
        return new AgentConversationDetailVO(
                node.path("id").asText(),
                node.path("title").asText(),
                node.path("status").asText(),
                node.path("summaryThroughMessageNo").asLong(),
                instant(node, "lastMessageAt"),
                instant(node, "createdAt"),
                instant(node, "updatedAt"),
                node.path("version").asLong(),
                node.hasNonNull("runningTurn") ? turn(node.get("runningTurn")) : null);
    }

    private AgentTurnVO turn(JsonNode node) {
        return new AgentTurnVO(
                node.path("id").asText(),
                node.path("conversationId").asText(),
                node.path("requestId").asText(),
                node.path("status").asText(),
                text(node, "finishReason"),
                text(node, "errorCode"),
                instant(node, "startedAt"),
                instant(node, "finishedAt"),
                text(node, "reply"),
                node.path("version").asLong());
    }

    private AgentMessageVO message(JsonNode node) {
        return new AgentMessageVO(
                node.path("id").asText(),
                node.path("messageNo").asLong(),
                node.path("role").asText(),
                node.path("content").asText(),
                node.path("isComplete").asBoolean(false),
                text(node, "finishReason"),
                text(node, "model"),
                node.hasNonNull("usage") ? node.get("usage") : null,
                instant(node, "createdAt"),
                node.path("citations"),
                node.path("usedMemoryIds"));
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private Instant instant(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException error) {
            return null;
        }
    }
}
