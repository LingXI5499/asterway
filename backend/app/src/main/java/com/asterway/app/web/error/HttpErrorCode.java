package com.asterway.app.web.error;

import com.asterway.common.error.ErrorCode;

// 只描述 HTTP 接入层的错误，业务错误仍由各业务模块定义。
public enum HttpErrorCode implements ErrorCode {

    INTERNAL_ERROR("INTERNAL_ERROR", "服务器内部错误"),
    CSRF_INVALID("CSRF_INVALID", "CSRF Token 无效或已过期"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "请求方法或内容类型不受支持"),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE", "请求方法或内容类型不受支持");

    private final String code;
    private final String message;

    HttpErrorCode(String code, String message) {
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
