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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    public static final String TRACE_ID_ATTRIBUTE =
            TraceIdFilter.class.getName() + ".traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString();

        String previousTraceId = MDC.get(TRACE_ID_KEY);

        request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        response.setHeader("X-Trace-Id", traceId);

        MDC.put(TRACE_ID_KEY, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previousTraceId == null) {
                MDC.remove(TRACE_ID_KEY);
            } else {
                MDC.put(TRACE_ID_KEY, previousTraceId);
            }
        }
    }
}
