package com.asterway.identity.model;

// 仅保存安全结果快照；认证会话及 CSRF 不在该对象中持久化。
public record OperationRecord(
        byte[] requestFingerprint,
        String state,
        Integer httpStatus,
        String resultCode,
        String resultSummary
) {
}
