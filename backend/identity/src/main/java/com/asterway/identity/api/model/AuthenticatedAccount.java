package com.asterway.identity.api.model;

import java.util.UUID;

// 已认证账户摘要。
// 登录凭据校验通过后返回给上层的安全账户信息。
// 刻意不包含 passwordHash，避免密码哈希泄露到业务层之外。
// 上层（app 模块）据此建立 Spring Security 的认证状态。
public record AuthenticatedAccount(
        UUID accountId,  // 账户 ID
        String username, // 用户名
        String nickname, // 昵称
        String role      // 角色（如 USER、ADMIN）
) {
}
