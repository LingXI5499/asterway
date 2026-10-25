package com.asterway.app.security;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 拒绝访问（403）的统一处理。
// 主要处理两类：CSRF 校验失败、已登录但无权限。
// 同样位于安全过滤链里，需要自己写 JSON 响应。
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    // JSON 序列化工具
    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {

        // 取请求追踪 ID
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        // 区分是 CSRF 失败还是普通权限不足，给不同的错误码和文案
        boolean csrfFailure = exception instanceof CsrfException;

        String code = csrfFailure
                ? "CSRF_INVALID"
                : "FORBIDDEN";

        String message = csrfFailure
                ? "CSRF Token 无效或已过期"
                : "没有权限执行此操作";

        // 设置 403 状态码和 JSON 响应头
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        // 用统一结构返回错误
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.<Void>failure(
                        code,
                        message,
                        null,
                        traceId
                )
        );
    }
}
