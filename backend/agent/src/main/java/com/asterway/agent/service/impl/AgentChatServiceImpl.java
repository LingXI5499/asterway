package com.asterway.agent.service.impl;

import com.asterway.agent.dto.AgentChatDTO;
import com.asterway.agent.service.AgentChatService;
import com.asterway.agent.vo.AgentChatResultVO;
import com.asterway.common.exception.BusinessException;
import com.asterway.common.integration.AgentClient;
import com.asterway.common.integration.AgentRequestContext;
import com.asterway.common.utils.SecurityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;

/** 同步阶段完成授权，异步阶段只使用已捕获的当前请求上下文。 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentChatServiceImpl.class);
    private final AgentClient client;

    public AgentChatServiceImpl(AgentClient client) {
        this.client = client;
    }

    @Override
    public AgentChatResultVO chat(AgentChatDTO dto) {
        var result = client.chat(dto.message(), context());
        return new AgentChatResultVO(
                result.path("reply").asText(),
                result.path("model").asText(),
                result.path("finish_reason").asText());
    }

    @Override
    public StreamingResponseBody stream(AgentChatDTO dto) {
        AgentRequestContext context = context();
        client.requireConfigured();
        return output -> {
            try {
                client.stream(dto.message(), context, output);
            } catch (IOException error) {
                // 浏览器主动停止后无法再写出任何结果；上游已由客户端 try-with-resources 关闭。
                // 不交给 JSON 异常处理器，避免把正常取消变成 500 或二次写出错误。
                LOGGER.debug("Agent 流式客户端已断开，traceId={}", context.traceId());
            }
        };
    }

    private AgentRequestContext context() {
        var actor = SecurityUtils.requirePermission("agent:chat:send");
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
}
