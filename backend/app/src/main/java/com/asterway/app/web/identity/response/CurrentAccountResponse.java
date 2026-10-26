package com.asterway.app.web.identity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

// 本人基础资料响应；沿用初版 accountId 字段，避免破坏现有页面。
@Schema(description = "当前登录账户的最新基础资料")
public record CurrentAccountResponse(
        @Schema(description = "稳定账户 ID") UUID accountId,
        @Schema(description = "用户名") String username,
        @Schema(description = "昵称") String nickname,
        @Schema(description = "账户角色") String role,
        @Schema(description = "创建时间，UTC") Instant createdAt,
        @Schema(description = "最后更新时间，UTC") Instant updatedAt
) {
}
