package com.asterway.agent.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/** Python 规划引擎提交的已确认方案；只接受普通用户本人范围。 */
public record AgentPlanCommitDTO(
    @NotNull @Positive Long runId,
    @NotNull @Positive Long planVersionId,
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "幂等键必须是 sha256 十六进制")
        String idempotencyKey,
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$", message = "请求摘要必须是 sha256 十六进制")
        String requestHash,
    @NotNull JsonNode snapshot) {}
