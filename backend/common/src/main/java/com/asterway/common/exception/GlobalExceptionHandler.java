package com.asterway.common.exception;

import com.asterway.common.result.ResultVO;
import com.asterway.common.security.OperationAuditProvider;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Controller 异常出口；安全过滤器中的异常使用同一响应格式单独写出。 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObjectProvider<OperationAuditProvider> audits;

    public GlobalExceptionHandler(ObjectProvider<OperationAuditProvider> audits) {
        this.audits = audits;
    }

    private void audit(int status, String code) {
        try {
            var recorder = audits.getIfAvailable();
            if (recorder != null) {
                recorder.recordFailure(status, code);
            }
        } catch (RuntimeException error) {
            LOGGER.warn("拒绝请求的审计写入失败，类型：{}", error.getClass().getSimpleName());
        }
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultVO<Object>> business(BusinessException error) {
        audit(error.getStatus(), error.getCode());
        return ResponseEntity.status(error.getStatus())
                .body(ResultVO.failure(error.getCode(), error.getMessage(), error.getData()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ResultVO<Object>> validation(BindException error) {
        var fields = error.getBindingResult().getFieldErrors().stream()
                .map(field -> Map.of("field", field.getField(), "message",
                        field.getDefaultMessage() == null ? "字段无效" : field.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ResultVO.failure("INVALID_ARGUMENT", "请检查提交的字段", fields));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, IllegalArgumentException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ResultVO<Object>> invalid(Exception error) {
        return ResponseEntity.badRequest().body(ResultVO.failure("INVALID_ARGUMENT", "请求参数格式无效", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResultVO<Object>> denied() {
        audit(403, "FORBIDDEN");
        return ResponseEntity.status(403).body(ResultVO.failure("FORBIDDEN", "没有执行此操作的权限", null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResultVO<Object>> missing() {
        return ResponseEntity.status(404).body(ResultVO.failure("NOT_FOUND", "请求地址不存在", null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResultVO<Object>> integrity() {
        audit(409, "DATA_CONFLICT");
        return ResponseEntity.status(409).body(ResultVO.failure("DATA_CONFLICT", "数据重复或不满足保存条件", null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ResultVO<Object>> database(DataAccessException error) {
        LOGGER.error("数据服务不可用，异常类型：{}", error.getClass().getSimpleName());
        return ResponseEntity.status(503).body(ResultVO.failure("SERVICE_UNAVAILABLE", "数据服务暂不可用，请稍后重试", null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResultVO<Object>> methodNotAllowed() {
        return ResponseEntity.status(405).body(ResultVO.failure("METHOD_NOT_ALLOWED", "当前地址不支持此请求方法", null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResultVO<Object>> mediaType() {
        return ResponseEntity.status(415).body(ResultVO.failure("UNSUPPORTED_MEDIA_TYPE", "请使用 JSON 请求体", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultVO<Object>> unexpected(Exception error) {
        // 只记录异常类型，避免异常消息包含数据库绑定参数或凭据。
        LOGGER.error("未处理异常，类型：{}", error.getClass().getName());
        return ResponseEntity.internalServerError().body(ResultVO.failure("INTERNAL_ERROR", "服务处理失败，请稍后重试", null));
    }
}
