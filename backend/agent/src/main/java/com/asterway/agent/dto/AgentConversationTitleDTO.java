package com.asterway.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** 修改会话标题；版本用于乐观锁。 */
public record AgentConversationTitleDTO(
        @NotBlank(message = "标题不能为空") @Size(max = 100, message = "标题不能超过 100 字")
                String title,
        @NotNull @PositiveOrZero Long version) {}
