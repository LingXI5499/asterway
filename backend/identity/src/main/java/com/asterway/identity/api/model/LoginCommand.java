package com.asterway.identity.api.model;

// 登录命令对象。
// 登录时从 app 传给 identity 的业务输入，只包含用户名和密码。
public record LoginCommand(
        String username,  // 用户名
        String password   // 密码（明文，仅本次校验用，不持久化）
) {
}
