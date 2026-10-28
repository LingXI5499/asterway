package com.asterway.identity.api.model;

import java.util.List;

// 账户模块公开数据契约；HTTP 请求和响应另由 dto/vo 明确表达。
public record TimeZonePage(List<TimeZoneItem> items, String nextCursor, boolean hasMore) {
}
