package com.asterway.identity.api.model;

import java.time.Instant;
import java.util.UUID;

// 注册结果对象。
// 注册成功后返回给上层（app 模块）的结果。
// accountId、createdAt 是数据库生成的，所以注册后需要重新读一次库才能拿到。
public record RegistrationResult(
        UUID accountId,        // 账户 ID（数据库生成的 UUID）
        String username,       // 注册使用的用户名（原始形式）
        boolean loginRequired, // 注册后是否需要登录（当前固定 true）
        Instant createdAt      // 账户创建时间
) {
}
