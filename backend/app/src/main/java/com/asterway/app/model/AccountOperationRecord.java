package com.asterway.app.model;



// 仅保存安全结果快照；认证会话及 CSRF 不在该对象中持久化。
public record AccountOperationRecord(
        byte[] requestFingerprint,
        String state,
        Integer httpStatus,
        String resultCode,
        String resultSummary
) {
}
