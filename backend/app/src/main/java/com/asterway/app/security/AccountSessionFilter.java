package com.asterway.app.security;

import com.asterway.app.web.error.ApiErrorWriter;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountAuthenticationApi;
import com.asterway.identity.api.error.IdentityErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 每次恢复认证身份后核对最新账户与凭据版本，不能只在 /me 中核对。
public class AccountSessionFilter extends OncePerRequestFilter {

    private final AccountAuthenticationApi accounts;
    private final ApiErrorWriter errorWriter;
    private final SecurityContextLogoutHandler logout;
    private final CsrfTokenRepository csrf;

    public AccountSessionFilter(
            AccountAuthenticationApi accounts,
            ApiErrorWriter errorWriter,
            SecurityContextLogoutHandler logout,
            CsrfTokenRepository csrf
    ) {
        this.accounts = accounts;
        this.errorWriter = errorWriter;
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
                if (CommonErrorCode.AUTH_REQUIRED.code().equals(code)
                        || IdentityErrorCode.ACCOUNT_UNAVAILABLE.code().equals(code)) {
                    logout.logout(request, response, authentication);
                    csrf.saveToken(null, request, response);
                }
                errorWriter.writeFailure(exception, request, response);
                return;
            } catch (DataAccessException exception) {
                BusinessException failure = new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, exception);
                errorWriter.writeFailure(failure, request, response);
                return;
            } catch (RuntimeException exception) {
                errorWriter.writeInternalError(exception, request, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
