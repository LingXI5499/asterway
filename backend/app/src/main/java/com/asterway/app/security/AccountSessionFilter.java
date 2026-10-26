package com.asterway.app.security;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.error.ApiErrorFactory;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.error.AccountException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// 每次恢复认证身份后核对最新账户与凭据版本，不能只在 /me 中核对。
public class AccountSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccountSessionFilter.class);

    private final AccountApi accounts;
    private final ObjectMapper objectMapper;
    private final ApiErrorFactory errors;
    private final SecurityContextLogoutHandler logout;
    private final CsrfTokenRepository csrf;

    public AccountSessionFilter(
            AccountApi accounts,
            ObjectMapper objectMapper,
            ApiErrorFactory errors,
            SecurityContextLogoutHandler logout,
            CsrfTokenRepository csrf
    ) {
        this.accounts = accounts;
        this.objectMapper = objectMapper;
        this.errors = errors;
        this.logout = logout;
        this.csrf = csrf;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            try {
                accounts.validateSession(principal.accountId(), principal.credentialVersion(), principal.securityVersion());
            } catch (BusinessException exception) {
                String code = exception.getErrorCode().code();
                if ("AUTH_REQUIRED".equals(code) || "ACCOUNT_UNAVAILABLE".equals(code)) {
                    logout.logout(request, response, authentication);
                    csrf.saveToken(null, request, response);
                }
                writeFailure(exception, request, response);
                return;
            } catch (DataAccessException exception) {
                log.error("核对账户会话时数据库不可用", exception);
                response.setStatus(503);
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
                ApiResponse<Void> payload = ApiResponse.failure("SERVICE_UNAVAILABLE", "服务暂时不可用，请稍后重试", null, traceId);
                objectMapper.writeValue(response.getOutputStream(), payload);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void writeFailure(BusinessException exception, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String traceId = (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE);
        ResponseEntity<?> failure = exception instanceof AccountException accountException
                ? errors.fromAccountException(accountException, traceId)
                : errors.fromBusinessException(exception, traceId);
        response.setStatus(failure.getStatusCode().value());
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), failure.getBody());
    }
}
