package com.asterway.app.web.contract;

// 单个字段的校验错误。
public record Violation(
        String field,    // 出错的字段名
        String message   // 错误提示
) {
}
