package com.asterway.app.web.contract;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        String traceId
) {
    // 成功，并返回数据
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                "OK",
                "操作成功",
                data,
                null
        );
    }
    // 成功，但不需要返回业务数据
    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
                "OK",
                "操作成功",
                null,
                null
        );
    }

    // 成功并返回数据，同时携带请求追踪标识
    public static <T> ApiResponse<T> success(
            T data,
            String traceId
    ) {
        return new ApiResponse<>(
                "OK",
                "操作成功",
                data,
                traceId
        );
    }

    // 成功但不返回业务数据，同时携带请求追踪标识
    public static ApiResponse<Void> successWithTraceId(
            String traceId
    ) {
        return new ApiResponse<>(
                "OK",
                "操作成功",
                null,
                traceId
        );
    }
    // 失败，返回错误编码和提示
    public static ApiResponse<Void> failure(
            String code,
            String message
    ) {
        return new ApiResponse<>(
                code,
                message,
                null,
                null
        );
    }

    // 失败，返回附加数据
    public static <T> ApiResponse<T> failure(
            String code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                code,
                message,
                data,
                null
        );
    }
    // 失败，同时指定错误数据和追踪标识
    public static <T> ApiResponse<T> failure(
            String code,
            String message,
            T data,
            String traceId
    ) {
        return new ApiResponse<>(
                code,
                message,
                data,
                traceId
        );
    }
}
