package com.asterway.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** 会话内发送；requestId 由浏览器生成，用于重放与幂等。 */
public record AgentConversationMessageDTO(
        @NotBlank(message = "请先输入提问") @Size(max = 8000, message = "提问不能超过 8000 字")
                String message,
        @NotNull(message = "请求标识不能为空") UUID requestId) {}
