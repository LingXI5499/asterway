package com.asterway.app.web.error;

import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.error.AccountException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 安全过滤链的统一错误写出入口，完整保留工厂产生的状态、响应头和响应体。
@Component
public class ApiErrorWriter {

    private final ApiErrorFactory errors;
    private final ObjectMapper objectMapper;

    public ApiErrorWriter(ApiErrorFactory errors, ObjectMapper objectMapper) {
        this.errors = errors;
        this.objectMapper = objectMapper;
    }

    public void writeFailure(BusinessException exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String traceId = traceId(request);
        ResponseEntity<?> failure = exception instanceof AccountException accountException
                ? errors.fromAccountException(accountException, traceId)
                : errors.fromBusinessException(exception, traceId);
        write(failure, response);
    }

    public void writeAuthenticationRequired(HttpServletRequest request, HttpServletResponse response) throws IOException {
        write(errors.authenticationRequired(traceId(request)), response);
    }

    public void writeAccessDenied(boolean csrfFailure, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        write(errors.accessDenied(csrfFailure, traceId(request)), response);
    }

    public void writeRateLimited(int retryAfterSeconds, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        write(errors.rateLimited(retryAfterSeconds, traceId(request)), response);
    }

    public void writeInternalError(Throwable exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        write(errors.internalError(exception, traceId(request)), response);
    }

    private String traceId(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
    }

    private void write(ResponseEntity<?> failure, HttpServletResponse response) throws IOException {
        response.setStatus(failure.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        failure.getHeaders().forEach((name, values) -> {
            if (!values.isEmpty()) {
                response.setHeader(name, values.get(0));
                for (int index = 1; index < values.size(); index++) {
                    response.addHeader(name, values.get(index));
                }
            }
        });
        objectMapper.writeValue(response.getOutputStream(), failure.getBody());
    }
}
