package com.asterway.identity.model;

import java.time.OffsetDateTime;
import java.util.UUID;

// 账户实体（数据库表 app.app_user 的一行记录）。
// 它是 identity 模块内部使用的数据对象，字段直接对应数据库列。
// 使用 record 表示，字段不可变，所以 MyBatis 映射时要按构造器参数顺序显式配置（见 AccountMapper.xml）。
// 重要：passwordHash 是敏感字段，只允许在 identity 内部用于登录校验，绝不能直接返回给前端。
public record AccountEntity(
        UUID id,                 // 账户唯一标识（数据库生成 UUID）
        String username,         // 用户原始用户名（用于展示）
        String usernameKey,      // 用户名标准化值（小写，数据库生成，用于唯一性比较）
        String nickname,         // 用户昵称
        String passwordHash,     // 密码哈希值（非明文）
        String role,             // 角色（USER / ADMIN / SUPER_ADMIN）
        String status,           // 账户状态（ENABLED / DISABLED）
        OffsetDateTime createdAt,// 创建时间（数据库默认 clock_timestamp）
        OffsetDateTime updatedAt // 最后更新时间（数据库默认 clock_timestamp）
) {
}
