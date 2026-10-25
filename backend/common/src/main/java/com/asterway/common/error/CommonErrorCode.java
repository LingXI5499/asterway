package com.asterway.common.error;

// 公共（跨模块通用）错误码。
// 这里只放不归属任何具体业务模块、整个系统都会用到的错误码。
// 业务专属的错误码（如身份相关的）放在各模块自己的枚举里，比如 identity 模块的 IdentityErrorCode。
public enum CommonErrorCode implements ErrorCode {

    // 请求参数校验未通过
    VALIDATION_ERROR("VALIDATION_ERROR", "请检查填写内容"),

    // 当前会话无效或未登录，需要重新认证
    AUTH_REQUIRED("AUTH_REQUIRED", "当前会话无效，请重新认证"),

    // 已登录但没有权限执行当前操作
    FORBIDDEN("FORBIDDEN", "没有权限执行此操作"),

    // 请求的资源不存在
    NOT_FOUND("NOT_FOUND", "未找到请求的资源");

    // 错误码字符串
    private final String code;

    // 错误提示文案
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
