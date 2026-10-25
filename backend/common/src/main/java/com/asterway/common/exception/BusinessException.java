package com.asterway.common.exception;

import com.asterway.common.error.ErrorCode;

// 业务异常。
// 当业务规则不满足（如用户名已存在、密码不一致）时，业务层主动抛出这个异常。
// 它携带一个 ErrorCode，最终由全局异常处理器 GlobalExceptionHandler 统一转成 HTTP 错误响应。
// 业务代码里不要直接 throw new RuntimeException(...)，而应抛出携带明确错误码的 BusinessException。
public class BusinessException extends RuntimeException {

    // 本次异常对应的错误码
    private final ErrorCode errorCode;

    // 只带错误码的构造器，异常消息默认取错误码的文案
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    // 同时带错误码和原始异常（cause）的构造器，常用于包装底层异常时保留完整堆栈
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    // 返回本次异常对应的错误码
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
