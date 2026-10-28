package com.asterway.identity.api.model;

import java.util.UUID;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record OperationSnapshot(
        String operation,
        UUID idempotencyKey,
        String state,
        int httpStatus,
        String resultCode,
        Object result
) {
}
