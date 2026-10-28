package com.asterway.app.security;

import com.asterway.app.web.error.ApiErrorWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;

// 拒绝访问（403）的统一处理。
// 主要处理两类：CSRF 校验失败、已登录但无权限。
// 同样位于安全过滤链里，通过统一错误写出组件返回 JSON 响应。
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final ApiErrorWriter errorWriter;

    public RestAccessDeniedHandler(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {
        errorWriter.writeAccessDenied(exception instanceof CsrfException, request, response);
    }
}
