package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.contract.ErrorData;
import com.asterway.app.web.contract.Violation;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountOperationCode;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.error.IdentityErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

// 统一错误内容、HTTP 状态和响应头；业务服务只提供业务结果或错误码。
@Component
public class ApiErrorFactory {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorFactory.class);

    private static final Map<String, HttpStatus> HTTP_STATUS_BY_ERROR_CODE = Map.ofEntries(
            Map.entry(CommonErrorCode.VALIDATION_ERROR.code(), HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(IdentityErrorCode.PASSWORD_MISMATCH.code(), HttpStatus.UNPROCESSABLE_ENTITY),
            Map.entry(CommonErrorCode.AUTH_REQUIRED.code(), HttpStatus.UNAUTHORIZED),
            Map.entry(IdentityErrorCode.CREDENTIALS_INVALID.code(), HttpStatus.UNAUTHORIZED),
            Map.entry(IdentityErrorCode.USERNAME_TAKEN.code(), HttpStatus.CONFLICT),
            Map.entry(IdentityErrorCode.VERSION_CONFLICT.code(), HttpStatus.CONFLICT),
            Map.entry(IdentityErrorCode.STATE_CONFLICT.code(), HttpStatus.CONFLICT),
            Map.entry(IdentityErrorCode.IDEMPOTENCY_KEY_REUSED.code(), HttpStatus.CONFLICT),
            Map.entry(IdentityErrorCode.REQUEST_IN_PROGRESS.code(), HttpStatus.CONFLICT),
            Map.entry(IdentityErrorCode.UNFINISHED_EXECUTION.code(), HttpStatus.CONFLICT),
            Map.entry(CommonErrorCode.FORBIDDEN.code(), HttpStatus.FORBIDDEN),
            Map.entry(IdentityErrorCode.ACCOUNT_UNAVAILABLE.code(), HttpStatus.FORBIDDEN),
            Map.entry(IdentityErrorCode.REGISTRATION_CLOSED.code(), HttpStatus.FORBIDDEN),
            Map.entry(HttpErrorCode.CSRF_INVALID.code(), HttpStatus.FORBIDDEN),
            Map.entry(CommonErrorCode.NOT_FOUND.code(), HttpStatus.NOT_FOUND),
            Map.entry(IdentityErrorCode.OPERATION_RESULT_NOT_FOUND.code(), HttpStatus.NOT_FOUND),
            Map.entry(CommonErrorCode.SERVICE_UNAVAILABLE.code(), HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(IdentityErrorCode.RESULT_UNKNOWN.code(), HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(IdentityErrorCode.CAPTCHA_INVALID.code(), HttpStatus.BAD_REQUEST),
            Map.entry(IdentityErrorCode.CURRENT_PASSWORD_INVALID.code(), HttpStatus.BAD_REQUEST),
            Map.entry(IdentityErrorCode.CAPTCHA_EXPIRED.code(), HttpStatus.GONE),
            Map.entry(IdentityErrorCode.ACCOUNT_LOCKED.code(), HttpStatus.LOCKED),
            Map.entry(IdentityErrorCode.RATE_LIMITED.code(), HttpStatus.TOO_MANY_REQUESTS),
            Map.entry(HttpErrorCode.METHOD_NOT_ALLOWED.code(), HttpStatus.METHOD_NOT_ALLOWED),
            Map.entry(HttpErrorCode.UNSUPPORTED_MEDIA_TYPE.code(), HttpStatus.UNSUPPORTED_MEDIA_TYPE),
            Map.entry(HttpErrorCode.INTERNAL_ERROR.code(), HttpStatus.INTERNAL_SERVER_ERROR)
    );

    public ResponseEntity<ApiResponse<Void>> fromBusinessException(BusinessException exception, String traceId) {
        HttpStatus status = resolveStatus(exception.getErrorCode().code());
        logSystemFailure(exception, status, traceId);
        ErrorCode errorCode = publicErrorCode(exception.getErrorCode(), status);
        return failure(errorCode, status, null, traceId);
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> fromAccountException(
            AccountException exception, String traceId
    ) {
        HttpStatus status = resolveStatus(exception.getErrorCode().code());
        logSystemFailure(exception, status, traceId);
        ErrorCode errorCode = publicErrorCode(exception.getErrorCode(), status);
        Map<String, Object> data = status == HttpStatus.INTERNAL_SERVER_ERROR || exception.getData().isEmpty()
                ? null : exception.getData();
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status);
        Object retryAfter = data == null ? null : data.get("retryAfterSeconds");
        if (retryAfter != null) {
            builder.header(HttpHeaders.RETRY_AFTER, retryAfter.toString());
        }
        return builder.body(ApiResponse.failure(errorCode.code(), errorCode.message(), data, traceId));
    }

    // 只在最终转换响应时记录系统性业务异常，避免普通业务拒绝产生错误日志。
    private void logSystemFailure(BusinessException exception, HttpStatus status, String traceId) {
        if (status.is5xxServerError() && exception.getCause() != null) {
            log.error("请求处理失败，code={}，traceId={}", exception.getErrorCode().code(), traceId, exception);
        }
    }

    public ResponseEntity<ApiResponse<Void>> internalError(String traceId) {
        return failure(HttpErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, null, traceId);
    }

    public ResponseEntity<ApiResponse<Void>> internalError(Throwable exception, String traceId) {
        log.error("发生未预期的服务器异常，traceId={}", traceId, exception);
        return internalError(traceId);
    }

    public HttpStatus resolveStatus(String code) {
        return code == null ? HttpStatus.INTERNAL_SERVER_ERROR
                : HTTP_STATUS_BY_ERROR_CODE.getOrDefault(code, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public int successStatus(String operation) {
        return AccountOperationCode.REGISTER.code().equals(operation) ? HttpStatus.CREATED.value() : HttpStatus.OK.value();
    }

    public ResponseEntity<ApiResponse<Void>> authenticationRequired(String traceId) {
        ApiResponse<Void> payload = ApiResponse.failure(
                CommonErrorCode.AUTH_REQUIRED.code(), "请先登录", null, traceId
        );
        return ResponseEntity.status(resolveStatus(CommonErrorCode.AUTH_REQUIRED.code())).body(payload);
    }

    public ResponseEntity<ApiResponse<Void>> accessDenied(boolean csrfFailure, String traceId) {
        ErrorCode errorCode = csrfFailure ? HttpErrorCode.CSRF_INVALID : CommonErrorCode.FORBIDDEN;
        return failure(errorCode, resolveStatus(errorCode.code()), null, traceId);
    }

    public ResponseEntity<ApiResponse<Void>> rateLimited(int retryAfterSeconds, String traceId) {
        // 入口限流沿用原有空 data 契约，仅通过响应头告知重试时间。
        ApiResponse<Void> payload = ApiResponse.failure(
                IdentityErrorCode.RATE_LIMITED.code(), IdentityErrorCode.RATE_LIMITED.message(), null, traceId
        );
        return ResponseEntity.status(resolveStatus(IdentityErrorCode.RATE_LIMITED.code()))
                .header(HttpHeaders.RETRY_AFTER, Integer.toString(retryAfterSeconds)).body(payload);
    }

    public ResponseEntity<ApiResponse<Void>> unreadableRequest(boolean unknownField, String traceId) {
        HttpStatus status = unknownField ? resolveStatus(CommonErrorCode.VALIDATION_ERROR.code()) : HttpStatus.BAD_REQUEST;
        ApiResponse<Void> payload = ApiResponse.failure(
                CommonErrorCode.VALIDATION_ERROR.code(), "请求格式或字段不符合接口约定", null, traceId
        );
        return ResponseEntity.status(status).body(payload);
    }

    public ResponseEntity<ApiResponse<Void>> unsupportedRequest(boolean unsupportedMethod, String traceId) {
        ErrorCode errorCode = unsupportedMethod ? HttpErrorCode.METHOD_NOT_ALLOWED : HttpErrorCode.UNSUPPORTED_MEDIA_TYPE;
        return failure(errorCode, resolveStatus(errorCode.code()), null, traceId);
    }

    public ResponseEntity<ApiResponse<ErrorData>> validationError(List<Violation> violations, String traceId) {
        return failure(CommonErrorCode.VALIDATION_ERROR, resolveStatus(CommonErrorCode.VALIDATION_ERROR.code()),
                new ErrorData(violations), traceId);
    }

    private ErrorCode publicErrorCode(ErrorCode errorCode, HttpStatus status) {
        return status == HttpStatus.INTERNAL_SERVER_ERROR ? HttpErrorCode.INTERNAL_ERROR : errorCode;
    }

    private <T> ResponseEntity<ApiResponse<T>> failure(ErrorCode code, HttpStatus status, T data, String traceId) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code.code(), code.message(), data, traceId));
    }
}
