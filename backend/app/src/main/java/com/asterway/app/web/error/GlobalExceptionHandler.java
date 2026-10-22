package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.contract.ErrorData;
import com.asterway.app.web.contract.Violation;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ApiErrorFactory apiErrorFactory;

    public GlobalExceptionHandler(ApiErrorFactory apiErrorFactory) {
        this.apiErrorFactory = apiErrorFactory;
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        return apiErrorFactory.fromBusinessException(
                exception,
                traceId
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error("发生未预期的服务器异常", exception);

        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        return apiErrorFactory.internalError(traceId);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorData>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        List<Violation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new Violation(
                        error.getField(),
                        error.getDefaultMessage() == null
                                ? "字段校验失败"
                                : error.getDefaultMessage()
                ))
                .toList();

        return apiErrorFactory.validationError(
                violations,
                traceId
        );
    }
}
