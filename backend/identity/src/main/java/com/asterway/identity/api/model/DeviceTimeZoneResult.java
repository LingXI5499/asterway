package com.asterway.identity.api.model;

import java.time.Instant;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record DeviceTimeZoneResult(AccountSnapshot account, boolean changed, Instant serverTime) {
}
