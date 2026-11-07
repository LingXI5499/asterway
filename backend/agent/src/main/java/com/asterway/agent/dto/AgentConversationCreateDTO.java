package com.asterway.agent.dto;

import jakarta.validation.constraints.Size;

/** 创建会话；标题可省略，服务端使用“新对话”。 */
public record AgentConversationCreateDTO(
        @Size(max = 100, message = "标题不能超过 100 字") String title) {}
