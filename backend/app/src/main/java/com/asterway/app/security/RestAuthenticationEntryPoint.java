package com.asterway.app.security;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 未认证请求的统一处理（返回 401）。
// 当未登录用户访问受保护接口时，Spring Security 会调这里的 commence 方法。
// 它位于安全过滤链里，和 MVC 层的 GlobalExceptionHandler 是两套机制，
// 所以这里要自己把错误写成 JSON 响应。
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    // JSON 序列化工具
    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {

        // 取请求追踪 ID，写入错误响应便于排查
        String traceId = (String) request.getAttribute(
                TraceIdFilter.TRACE_ID_ATTRIBUTE
        );

        // 设置 401 状态码和 JSON 响应头
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        // 用统一的 ApiResponse 结构返回"请先登录"
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResponse.<Void>failure(
                        "AUTH_REQUIRED",
                        "请先登录",
                        null,
                        traceId
                )
        );
    }
}
