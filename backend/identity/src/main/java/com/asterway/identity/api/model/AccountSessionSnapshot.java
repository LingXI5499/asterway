package com.asterway.identity.api.model;

import java.time.Instant;
import java.time.LocalDate;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record AccountSessionSnapshot(
        AccountSnapshot account,
        Instant serverTime,
        LocalDate businessDate,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        boolean hasUnfinishedExecutions
) {
}
