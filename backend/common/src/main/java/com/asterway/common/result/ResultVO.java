package com.asterway.common.result;

import org.slf4j.MDC;

/** HTTP 统一响应。业务错误同时保留真实 HTTP 状态，不以 200 掩盖失败。 */
public record ResultVO<T>(String code, String message, T data, String traceId) {

    /** 包装成功结果；允许 data 为空，保持响应字段稳定。 */
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>("OK", "操作成功", data, MDC.get("traceId"));
    }

    /** 包装经过筛选的业务错误，不向客户端暴露异常堆栈。 */
    public static ResultVO<Object> failure(String code, String message, Object data) {
        return new ResultVO<>(code, message, data, MDC.get("traceId"));
    }
}
