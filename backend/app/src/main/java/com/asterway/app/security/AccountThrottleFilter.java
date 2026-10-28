package com.asterway.app.security;

import com.asterway.app.constant.AccountThrottleConstants;
import com.asterway.app.web.error.ApiErrorWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

// 入口流量保护与“五次密码错误锁定”分开；不信任客户端可伪造的转发 IP 头。
public class AccountThrottleFilter extends OncePerRequestFilter {

    private final ApiErrorWriter errorWriter;
    private final Semaphore concurrentRequests = new Semaphore(AccountThrottleConstants.MAX_CONCURRENT_REQUESTS);
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AccountThrottleFilter(ApiErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
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
        long minute = System.currentTimeMillis() / AccountThrottleConstants.WINDOW_MILLIS;
        windows.entrySet().removeIf(entry -> entry.getValue().minute() < minute);
        String address = request.getRemoteAddr();
        if (windows.size() >= AccountThrottleConstants.MAX_TRACKED_ADDRESSES && !windows.containsKey(address)) {
            return false;
        }
        Window window = windows.computeIfAbsent(address, key -> new Window(minute, new AtomicInteger()));
        boolean allowed = window.count().incrementAndGet() <= AccountThrottleConstants.MAX_REQUESTS_PER_WINDOW;
        return allowed;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        errorWriter.writeRateLimited(AccountThrottleConstants.RETRY_AFTER_SECONDS, request, response);
    }

    private record Window(long minute, AtomicInteger count) {
    }
}
