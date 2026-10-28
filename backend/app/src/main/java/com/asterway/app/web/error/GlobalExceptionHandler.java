package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.contract.ErrorData;
import com.asterway.app.web.contract.Violation;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.AccountException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

// 全局异常处理器（MVC 层）。
// 统一捕获控制器抛出的异常，转成标准 ApiResponse 响应。
// 注意：它只管 MVC 层，Spring Security 过滤链里的异常由
// RestAuthenticationEntryPoint / RestAccessDeniedHandler 单独处理。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 负责把异常转换成响应
    private final ApiErrorFactory apiErrorFactory;

    public GlobalExceptionHandler(ApiErrorFactory apiErrorFactory) {
        this.apiErrorFactory = apiErrorFactory;
    }

    @ExceptionHandler(AccountException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccountException(
            AccountException exception, HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        ResponseEntity<ApiResponse<Map<String, Object>>> response =
                apiErrorFactory.fromAccountException(exception, traceId);
        return response;
    }

    // JSON 语法及基础类型错误返回 400，未知契约字段返回 422，不回显请求正文。
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<?> handleUnreadableRequest(Exception exception, HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        boolean unknownField = false;
        Throwable cause = exception;
        while (cause != null) {
            // 严格请求反序列化器抛出的业务校验异常保持原有错误数据和状态。
            if (cause instanceof AccountException accountException) {
                return apiErrorFactory.fromAccountException(accountException, traceId);
            }
            if (cause instanceof BusinessException businessException) {
                return apiErrorFactory.fromBusinessException(businessException, traceId);
            }
            if (cause instanceof UnrecognizedPropertyException) {
                unknownField = true;
            }
            cause = cause.getCause();
        }
        return apiErrorFactory.unreadableRequest(unknownField, traceId);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedRequest(Exception exception, HttpServletRequest request) {
        boolean unsupportedMethod = exception instanceof HttpRequestMethodNotSupportedException;
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        return apiErrorFactory.unsupportedRequest(unsupportedMethod, traceId);
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

    // 未启用的文档或不存在的静态资源保持 404，不误报为服务器故障。
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingResource(HttpServletRequest request) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND);
        ResponseEntity<ApiResponse<Void>> response =
                apiErrorFactory.fromBusinessException(exception, traceId);
        return response;
    }

    // 数据库连接失败按暂时不可用反馈，不能误判为账户或会话失效。
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseUnavailable(
            DataAccessResourceFailureException exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        BusinessException businessException = new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, exception);
        ResponseEntity<ApiResponse<Void>> response =
                apiErrorFactory.fromBusinessException(businessException, traceId);
        return response;
    }

    // 兜底异常：记录日志，对外统一返回 500，不暴露内部细节
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        return apiErrorFactory.internalError(exception, traceId);
    }

    // 参数校验失败：提取各字段错误，返回 422
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<? extends ApiResponse<?>> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        // 查询 DTO 的类型转换错误沿用原请求参数的 400，不当作业务字段校验失败。
        boolean bindingFailure = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.isBindingFailure());
        if (bindingFailure) {
            ResponseEntity<ApiResponse<Void>> response = apiErrorFactory.unreadableRequest(false, traceId);
            return response;
        }

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
