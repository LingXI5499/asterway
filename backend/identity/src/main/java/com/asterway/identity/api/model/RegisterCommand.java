package com.asterway.identity.api.model;

// 注册命令对象。
// 这是 app 模块调用 identity 模块注册能力时传入的业务输入，
// 由前端注册请求 DTO（RegisterRequest）转换而来，不是直接暴露给前端的对象。
// 注意：这里不包含 role/status/id/passwordHash 等字段，这些由服务端决定，防止客户端越权指定。
public record RegisterCommand(
        String username,        // 用户名（原始形式，未规范化）
        String nickname,        // 昵称（可为空，为空时服务端回退为用户名）
        String password,        // 密码
        String confirmPassword  // 确认密码（校验两次输入一致）
) {
}
