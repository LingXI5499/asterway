package com.asterway.common.error;

public enum CommonErrorCode implements ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", "请检查填写内容"),
    AUTH_REQUIRED("AUTH_REQUIRED", "当前会话无效，请重新认证"),
    FORBIDDEN("FORBIDDEN", "没有权限执行此操作"),
    NOT_FOUND("NOT_FOUND", "未找到请求的资源");

    private final String code;
    private final String message;
    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }
    @Override
    public String message() {
        return message;
    }

}
