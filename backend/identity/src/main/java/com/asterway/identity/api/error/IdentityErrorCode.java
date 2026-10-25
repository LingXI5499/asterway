package com.asterway.identity.api.error;

import com.asterway.common.error.ErrorCode;

// 账户模块专属的业务错误码。
// 只放"账户"这个业务域特有的错误（用户名重复、密码不一致、凭据错误）。
// 通用错误（校验失败、未认证、无权限等）由 common 模块的 CommonErrorCode 负责。
public enum IdentityErrorCode implements ErrorCode {

    // 用户名已被其他账户占用
    USERNAME_TAKEN(
            "USERNAME_TAKEN",
            "用户名已被使用"
    ),

    // 两次输入的密码不一致
    PASSWORD_MISMATCH(
            "PASSWORD_MISMATCH",
            "两次输入的密码不一致"
    ),

    // 登录凭据（用户名或密码）错误
    CREDENTIALS_INVALID(
            "CREDENTIALS_INVALID",
            "用户名或密码错误"
    );

    // 错误码字符串
    private final String code;

    // 错误提示文案
    private final String message;

    IdentityErrorCode(String code, String message) {
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
