package com.asterway.common.exception;

/** 预期业务拒绝，包含 HTTP 状态和可公开数据；不携带密码等敏感信息。 */
public class BusinessException extends RuntimeException {

    private final int status;
    private final String code;
    private final Object data;

    public BusinessException(int status, String code, String message) {
        this(status, code, message, null);
    }

    public BusinessException(int status, String code, String message, Object data) {
        super(message);
        this.status = status;
        this.code = code;
        this.data = data;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object getData() {
        return data;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(400, "INVALID_ARGUMENT", message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(409, "STATE_CONFLICT", message);
    }

    public static BusinessException notFound() {
        return new BusinessException(404, "NOT_FOUND", "资源不存在或不在可访问范围内");
    }

    public static BusinessException unauthorized() {
        return new BusinessException(401, "UNAUTHORIZED", "登录已失效，请重新登录");
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "FORBIDDEN", "没有执行此操作的权限");
    }
}
