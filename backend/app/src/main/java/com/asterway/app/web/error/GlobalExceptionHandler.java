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

// 全局异常处理器（MVC 层）。
// 统一捕获控制器抛出的异常，转成标准 ApiResponse 响应。
// 注意：它只管 MVC 层，Spring Security 过滤链里的异常由
// RestAuthenticationEntryPoint / RestAccessDeniedHandler 单独处理。
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 负责把异常转换成响应
    private final ApiErrorFactory apiErrorFactory;

    public GlobalExceptionHandler(ApiErrorFactory apiErrorFactory) {
        this.apiErrorFactory = apiErrorFactory;
    }

    // 业务异常：交给 ApiErrorFactory 按错误码映射状态码
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

    // 兜底异常：记录日志，对外统一返回 500，不暴露内部细节
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

    // 参数校验失败：提取各字段错误，返回 422
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorData>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        // 把每个字段的校验错误转成 Violation 列表
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
