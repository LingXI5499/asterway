package com.asterway.identity.api;

// 密码哈希器契约。
// identity 模块只需要知道"如何生成哈希"和"如何校验密码"，不需要知道具体算法（Argon2/BCrypt/scrypt...）。
// 具体实现由 app 模块提供（当前是 Argon2PasswordHasher）。
// 这样设计的好处：将来换算法时只需在 app 模块换实现，identity 业务代码完全不用改。
public interface PasswordHasher {

    // 对明文密码进行哈希
    // rawPassword：明文密码
    // 返回：密码哈希值（应直接存库，绝不存明文）
    String hash(String rawPassword);

    // 校验明文密码是否与已存储的哈希匹配
    // rawPassword：用户输入的明文密码
    // passwordHash：库里存的密码哈希
    // 返回：匹配返回 true，否则 false
    boolean matches(String rawPassword, String passwordHash);
}
