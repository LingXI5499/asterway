package com.asterway.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** 请求元数据；当前本地开发不信任客户端伪造的代理 IP 请求头。 */
public final class RequestUtils {

    private RequestUtils() {
    }

    public static String ipAddress() {
        HttpServletRequest request = request();
        return request == null ? null : request.getRemoteAddr();
    }

    public static String userAgent() {
        HttpServletRequest request = request();
        String value = request == null ? "" : request.getHeader("User-Agent");
        return value == null ? "" : value.substring(0, Math.min(512, value.length()));
    }

    private static HttpServletRequest request() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }
}
