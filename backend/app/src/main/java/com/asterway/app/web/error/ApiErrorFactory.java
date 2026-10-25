package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.contract.ErrorData;
import com.asterway.app.web.contract.Violation;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
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

        return ResponseEntity
                .status(status)
                .body(ApiResponse.<Void>failure(
                        errorCode.code(),
                        errorCode.message(),
                        null,
                        traceId
                ));
    }

    // 服务器内部错误（500）
    public ResponseEntity<ApiResponse<Void>> internalError(
            String traceId
    ) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>failure(
                        "INTERNAL_ERROR",
                        "服务器内部错误",
                        null,
                        traceId
                ));
    }

    // 错误码 -> HTTP 状态码映射表
    private HttpStatus resolveStatus(String code) {
        return switch (code) {
            // 参数校验失败、密码不一致 -> 422
            case "VALIDATION_ERROR", "PASSWORD_MISMATCH" ->
                    HttpStatus.UNPROCESSABLE_ENTITY;

            // 未认证、凭据错误 -> 401
            case "AUTH_REQUIRED", "CREDENTIALS_INVALID" ->
                    HttpStatus.UNAUTHORIZED;

            // 用户名重复 -> 409
            case "USERNAME_TAKEN" ->
                    HttpStatus.CONFLICT;

            // 无权限 -> 403
            case "FORBIDDEN" ->
                    HttpStatus.FORBIDDEN;

            // 资源不存在 -> 404
            case "NOT_FOUND" ->
                    HttpStatus.NOT_FOUND;

            // 未知错误码 -> 500
            default ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    // 参数校验失败 -> 422（附带各字段错误明细）
    public ResponseEntity<ApiResponse<ErrorData>> validationError(
            List<Violation> violations,
            String traceId
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.<ErrorData>failure(
                        "VALIDATION_ERROR",
                        "请检查填写内容",
                        new ErrorData(violations),
                        traceId
                ));
    }
}
