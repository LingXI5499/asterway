package com.asterway.app.application.identity;

import com.asterway.app.security.AccountPrincipal;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountModels.Context;
import com.asterway.identity.api.model.AccountModels.CsrfInfo;
import com.asterway.identity.api.model.AccountModels.LoginGrant;
import com.asterway.identity.api.model.AccountModels.LoginInput;
import com.asterway.identity.api.model.AccountModels.LogoutResult;
import com.asterway.identity.api.model.AccountModels.PasswordChangeResult;
import com.asterway.identity.api.model.AccountModels.PasswordInput;
import com.asterway.identity.api.model.AccountModels.SelfSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

// 只协调账户能力与框架会话，HTTP 和数据库事务不混在控制器中。
@Component
public class AccountUseCase {

    private final AccountApi accountApi;
    private final AccountHttpContext contexts;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler;
    private final CsrfTokenRepository csrfTokenRepository;

    public AccountUseCase(
            AccountApi accountApi,
            AccountHttpContext contexts,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            SecurityContextLogoutHandler logoutHandler,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.accountApi = accountApi;
        this.contexts = contexts;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.logoutHandler = logoutHandler;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public CsrfInfo csrf(HttpServletRequest request, HttpServletResponse response) {
        contexts.context(request, null, true);
        CsrfToken token = csrfTokenRepository.loadDeferredToken(request, response).get();
        CsrfInfo result = new CsrfInfo(token.getHeaderName(), token.getToken());
        return result;
    }

    public SelfSession login(LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID key = contexts.key(request);
        Context context = contexts.context(request, key, true);
        LoginGrant grant = accountApi.login(context, input);
        AccountPrincipal existing = contexts.principal();
        if (grant.replayed()) {
            // 认证结果重放不能重新签发 Cookie；只允许已存在的同一身份核实快照。
            if (existing == null || !existing.accountId().equals(grant.accountId())) {
                throw new BusinessException(CommonErrorCode.AUTH_REQUIRED);
            }
        } else {
            accountApi.validateSession(grant.accountId(), grant.credentialVersion(), grant.securityVersion());
            establishSession(grant, request, response);
        }
        SelfSession snapshot = current(request, response);
        return snapshot;
    }

    private void establishSession(LoginGrant grant, HttpServletRequest request, HttpServletResponse response) {
        AccountPrincipal principal = new AccountPrincipal(
                grant.accountId(), grant.username(), grant.nickname(), grant.role(),
                grant.credentialVersion(), grant.securityVersion()
        );
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + grant.role()));
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    public SelfSession current(HttpServletRequest request, HttpServletResponse response) {
        Context context = contexts.context(request, null, false);
        try {
            SelfSession snapshot = accountApi.current(context, csrf(request, response));
            return snapshot;
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == CommonErrorCode.AUTH_REQUIRED
                    || exception.getErrorCode() == IdentityErrorCode.ACCOUNT_UNAVAILABLE) {
                clearSession(request, response);
            }
            throw exception;
        }
    }

    public PasswordChangeResult changePassword(
            PasswordInput input, HttpServletRequest request, HttpServletResponse response
    ) {
        Context context = contexts.context(request, contexts.key(request), false);
        PasswordChangeResult result = accountApi.changePassword(context, input);
        clearSession(request, response);
        return result;
    }

    public LogoutResult logout(Boolean confirmed, HttpServletRequest request, HttpServletResponse response) {
        Context context = contexts.context(request, contexts.key(request), false);
        String sessionId = request.getSession(false) == null ? null : request.getSession(false).getId();
        LogoutResult result = accountApi.logout(context, confirmed, sessionId);
        clearSession(request, response);
        return result;
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
    }
}
