package com.asterway.app.web.identity.response;

import java.util.UUID;

// 登录响应 VO（返回给前端的对象）。
// 与 identity 模块的 AuthenticatedAccount 不同，这是 HTTP 层的视图对象，
// 只暴露前端允许读取的字段（不含密码哈希）。
public record LoginResponse(
        UUID accountId,  // 账户 ID
        String username, // 用户名
        String nickname, // 昵称
        String role      // 角色
) {
}
