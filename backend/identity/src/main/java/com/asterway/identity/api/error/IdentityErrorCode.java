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
    ),

    // 已认证账户被停用，拒绝继续展示资料并清理当前身份。
    ACCOUNT_UNAVAILABLE(
            "ACCOUNT_UNAVAILABLE",
            "账户不可用，请联系管理员"
    ),
    REGISTRATION_CLOSED("REGISTRATION_CLOSED", "当前未开放注册"),
    VERSION_CONFLICT("VERSION_CONFLICT", "资料已更新，请重新读取后再操作"),
    STATE_CONFLICT("STATE_CONFLICT", "当前状态不允许此操作"),
    IDEMPOTENCY_KEY_REUSED("IDEMPOTENCY_KEY_REUSED", "本次操作标识已用于不同的内容"),
    REQUEST_IN_PROGRESS("REQUEST_IN_PROGRESS", "原操作正在处理中，请稍后核实"),
    CAPTCHA_INVALID("CAPTCHA_INVALID", "滑动验证未通过，请重新验证"),
    CAPTCHA_EXPIRED("CAPTCHA_EXPIRED", "滑动验证已过期，请重新验证"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "连续登录失败，账户暂时锁定，请稍后重试"),
    CURRENT_PASSWORD_INVALID("CURRENT_PASSWORD_INVALID", "原密码不正确"),
    RATE_LIMITED("RATE_LIMITED", "操作过于频繁，请稍后重试"),
    UNFINISHED_EXECUTION("UNFINISHED_EXECUTION", "请先结束全部未完成的执行，再进行此操作"),
    RESULT_UNKNOWN("RESULT_UNKNOWN", "操作结果暂时无法确认，请按原操作标识核实"),
    OPERATION_RESULT_NOT_FOUND("OPERATION_RESULT_NOT_FOUND", "未找到可查询的操作结果");

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
