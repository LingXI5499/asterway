package com.asterway.identity.vo;

import java.time.Instant;

/** 登录成功结果，令牌只能保存在客户端登录状态中，不写日志。 */
public record LoginResultVO(
        String token,
        String sessionId,
        Instant expiresAt,
        UserVO user
) {
}
