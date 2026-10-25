package com.asterway.app.security;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;
import java.util.UUID;

// 当前登录身份对象（安全 principal）。
// 登录成功后，Spring Security 保存的是这个对象，而不是密码或密码哈希。
// 实现 Serializable 是必须的：Spring Session JDBC 存会话时要把 principal 序列化到数据库，
// 否则会抛 NotSerializableException。
public record AccountPrincipal(
        UUID accountId,  // 账户 ID
        String username, // 用户名
        String nickname, // 昵称
        String role      // 角色
) implements AuthenticatedPrincipal, Serializable {

    // 序列化版本号，保证反序列化兼容
    private static final long serialVersionUID = 1L;

    // 返回主体名，Spring Security 用它标识当前用户
    @Override
    public String getName() {
        return username;
    }
}
