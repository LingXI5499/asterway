package com.asterway.app.security;

import com.asterway.identity.api.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Argon2 密码哈希器实现。
// 这是 identity 模块定义的 PasswordHasher 接口在 app 模块里的具体实现。
// 实际密码算法由 Spring Security 的 PasswordEncoder（Argon2PasswordEncoder）提供，
// 算法参数在 SecurityConfig 里统一配置，业务代码不重复配置。
@Component
public class Argon2PasswordHasher implements PasswordHasher {

    // Spring Security 的密码编码器（具体是 Argon2）
    private final PasswordEncoder passwordEncoder;

    public Argon2PasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // 哈希：把明文密码编码成 Argon2 哈希
    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // 校验：比对明文密码与已存哈希是否匹配
    @Override
    public boolean matches(
            String rawPassword,
            String passwordHash
    ) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }
}
