package com.asterway.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** P1.1 只接收本次提问；身份与权限由认证上下文取得，不接受客户端指定。 */
public record AgentChatDTO(
        @NotBlank(message = "请先输入提问") @Size(max = 8000, message = "提问不能超过 8000 字")
                String message) {}
