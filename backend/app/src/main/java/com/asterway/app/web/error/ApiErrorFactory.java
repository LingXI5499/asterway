package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.contract.ErrorData;
import com.asterway.app.web.contract.Violation;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.AccountException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

// 把业务异常/错误码转换成 HTTP 响应。
// 集中管理"错误码 -> HTTP 状态码"的映射，业务层只管抛 BusinessException，
// 不需要关心最终返回什么状态码。
@Component
public class ApiErrorFactory {

    // 业务异常 -> 错误响应
    public ResponseEntity<ApiResponse<Void>> fromBusinessException(
            BusinessException exception,
            String traceId
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        // 根据错误码解析出对应的 HTTP 状态码
        HttpStatus status = resolveStatus(errorCode.code());

        // 未映射到的错误码按 500 处理，但对外不暴露内部细节
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            return internalError(traceId);
        }

        ApiResponse<Void> payload = ApiResponse.failure(
                errorCode.code(), errorCode.message(), null, traceId
        );
        ResponseEntity<ApiResponse<Void>> response = ResponseEntity.status(status).body(payload);
        return response;
    }

    // 服务器内部错误（500）
    public ResponseEntity<ApiResponse<Void>> internalError(
            String traceId
    ) {
        ApiResponse<Void> payload = ApiResponse.failure(
                "INTERNAL_ERROR", "服务器内部错误", null, traceId
        );
        ResponseEntity<ApiResponse<Void>> response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(payload);
        return response;
    }

    // 错误码 -> HTTP 状态码映射表
    public HttpStatus resolveStatus(String code) {
        HttpStatus status = switch (code) {
            // 参数校验失败、密码不一致 -> 422
            case "VALIDATION_ERROR", "PASSWORD_MISMATCH" ->
                    HttpStatus.UNPROCESSABLE_ENTITY;

            // 未认证、凭据错误 -> 401
            case "AUTH_REQUIRED", "CREDENTIALS_INVALID" ->
                    HttpStatus.UNAUTHORIZED;

            // 用户名重复 -> 409
            case "USERNAME_TAKEN", "VERSION_CONFLICT", "STATE_CONFLICT", "IDEMPOTENCY_KEY_REUSED",
                    "REQUEST_IN_PROGRESS", "UNFINISHED_EXECUTION" ->
                    HttpStatus.CONFLICT;

            // 无权限 -> 403
            case "FORBIDDEN", "ACCOUNT_UNAVAILABLE", "REGISTRATION_CLOSED" ->
                    HttpStatus.FORBIDDEN;

            // 资源不存在 -> 404
            case "NOT_FOUND", "OPERATION_RESULT_NOT_FOUND" ->
                    HttpStatus.NOT_FOUND;

            // 数据库连接等依赖暂时不可用 -> 503。
            case "SERVICE_UNAVAILABLE", "RESULT_UNKNOWN" ->
                    HttpStatus.SERVICE_UNAVAILABLE;

            case "CAPTCHA_INVALID", "CURRENT_PASSWORD_INVALID" -> HttpStatus.BAD_REQUEST;
            case "CAPTCHA_EXPIRED" -> HttpStatus.GONE;
            case "ACCOUNT_LOCKED" -> HttpStatus.LOCKED;
            case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;

            // 未知错误码 -> 500
            default ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return status;
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> fromAccountException(
            AccountException exception, String traceId
    ) {
        HttpStatus status = resolveStatus(exception.getErrorCode().code());
        Map<String, Object> data = exception.getData().isEmpty() ? null : exception.getData();
        ApiResponse<Map<String, Object>> payload = ApiResponse.failure(
                exception.getErrorCode().code(), exception.getErrorCode().message(), data, traceId
        );
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        Object retryAfter = exception.getData().get("retryAfterSeconds");
        if (retryAfter != null) {
            builder.header("Retry-After", retryAfter.toString());
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> response = builder.body(payload);
        return response;
    }

    // 参数校验失败 -> 422（附带各字段错误明细）
    public ResponseEntity<ApiResponse<ErrorData>> validationError(
            List<Violation> violations,
            String traceId
    ) {
        ErrorData data = new ErrorData(violations);
        ApiResponse<ErrorData> payload = ApiResponse.failure(
                "VALIDATION_ERROR", "请检查填写内容", data, traceId
        );
        ResponseEntity<ApiResponse<ErrorData>> response =
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(payload);
        return response;
    }
}
