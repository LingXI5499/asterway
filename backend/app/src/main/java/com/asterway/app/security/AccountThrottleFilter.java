package com.asterway.app.security;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// 入口流量保护与“五次密码错误锁定”分开；不信任客户端可伪造的转发 IP 头。
public class AccountThrottleFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Semaphore concurrentRequests = new Semaphore(8);
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AccountThrottleFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!allowed(request) || !concurrentRequests.tryAcquire()) {
            reject(request, response);
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            concurrentRequests.release();
        }
    }

    private boolean allowed(HttpServletRequest request) {
        if ("GET".equals(request.getMethod())) {
            return true;
        }
        long minute = System.currentTimeMillis() / 60_000;
        windows.entrySet().removeIf(entry -> entry.getValue().minute() < minute);
        String address = request.getRemoteAddr();
        if (windows.size() >= 2048 && !windows.containsKey(address)) {
            return false;
        }
        Window window = windows.computeIfAbsent(address, key -> new Window(minute, new AtomicInteger()));
        boolean allowed = window.count().incrementAndGet() <= 120;
        return allowed;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        ApiResponse<Void> payload = ApiResponse.failure("RATE_LIMITED", "操作过于频繁，请稍后重试", null, traceId);
        objectMapper.writeValue(response.getOutputStream(), payload);
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
