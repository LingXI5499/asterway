package com.asterway.agent.service;

import com.asterway.agent.dto.AgentChatDTO;
import com.asterway.agent.vo.AgentChatResultVO;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 单轮对话入口，普通结果和流式结果复用相同的身份及权限边界。 */
public interface AgentChatService {
    AgentChatResultVO chat(AgentChatDTO dto);

    StreamingResponseBody stream(AgentChatDTO dto);
}
