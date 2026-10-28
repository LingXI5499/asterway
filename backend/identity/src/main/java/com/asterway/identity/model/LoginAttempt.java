package com.asterway.identity.model;

import java.time.OffsetDateTime;

// 按规范化提交名记录失败，不以账户是否存在区分响应。
public record LoginAttempt(int failureCount, OffsetDateTime lockedUntil) {
}
