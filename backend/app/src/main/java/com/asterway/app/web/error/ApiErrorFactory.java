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

@Component
public class ApiErrorFactory {

    public ResponseEntity<ApiResponse<Void>> fromBusinessException(
            BusinessException exception,
            String traceId
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        HttpStatus status = resolveStatus(errorCode.code());

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

    private HttpStatus resolveStatus(String code) {
        return switch (code) {
            case "VALIDATION_ERROR" ->
                    HttpStatus.UNPROCESSABLE_ENTITY;
            case "AUTH_REQUIRED" ->
                    HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" ->
                    HttpStatus.FORBIDDEN;
            case "NOT_FOUND" ->
                    HttpStatus.NOT_FOUND;
            default ->
                    HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

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
