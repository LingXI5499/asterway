package com.asterway.app.security;

import com.asterway.app.web.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// 未认证请求的统一处理（返回 401）。
// 当未登录用户访问受保护接口时，Spring Security 会调这里的 commence 方法。
// 它位于安全过滤链里，和 MVC 层的 GlobalExceptionHandler 是两套机制，
// 这里交给统一错误写出组件返回 JSON 响应。
public class RestAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ApiErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        errorWriter.writeAuthenticationRequired(request, response);
    }
}
