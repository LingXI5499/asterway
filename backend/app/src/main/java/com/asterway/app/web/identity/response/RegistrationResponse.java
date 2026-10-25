package com.asterway.app.web.identity.response;

import java.time.Instant;
import java.util.UUID;

// 注册响应 VO（返回给前端的对象）。
// 由 identity 模块的 RegistrationResult 转换而来，只暴露前端允许读取的字段。
public record RegistrationResponse(
        UUID accountId,        // 账户 ID
        String username,       // 用户名
        boolean loginRequired, // 注册后是否需要登录
        Instant createdAt      // 创建时间
) {
}
