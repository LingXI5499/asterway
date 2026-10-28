package com.asterway.app.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

// 请求追踪过滤器。
// 每次请求进来都生成一个唯一 traceId，写进响应头和 MDC（日志上下文），
// 这样通过 traceId 就能把一次请求的所有日志、错误响应串起来排查。
// @Order(HIGHEST_PRECEDENCE) 保证它在最前面执行。
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    // MDC 里存放 traceId 用的 key
    private static final String TRACE_ID_KEY = "traceId";

    // 请求属性里存放 traceId 用的 key（供控制器、异常处理器读取）
    public static final String TRACE_ID_ATTRIBUTE =
            TraceIdFilter.class.getName() + ".traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 生成本次请求的唯一追踪 ID
        String traceId = UUID.randomUUID().toString();

        // 保存上游可能已存在的 traceId，便于请求结束后恢复
        String previousTraceId = MDC.get(TRACE_ID_KEY);

        // 写入请求属性和响应头，同时放进 MDC 供日志打印
        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader("X-Trace-Id", traceId);
        MDC.put(TRACE_ID_KEY, traceId);

        try {
            // 放行给后续过滤链处理
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束，恢复 MDC 原值，避免污染线程池里的其他请求
            if (previousTraceId == null) {
                MDC.remove(TRACE_ID_KEY);
            } else {
                MDC.put(TRACE_ID_KEY, previousTraceId);
            }
        }
    }
}
