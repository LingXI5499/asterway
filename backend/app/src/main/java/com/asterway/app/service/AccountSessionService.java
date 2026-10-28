package com.asterway.app.service;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.security.AccountPrincipal;
import com.asterway.app.web.identity.vo.AccountSessionVO;
import com.asterway.app.web.identity.vo.CsrfVO;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.exception.BusinessException;
import com.asterway.identity.api.AccountAuthenticationApi;
import com.asterway.identity.api.AccountLogoutApi;
import com.asterway.identity.api.AccountPasswordApi;
import com.asterway.identity.api.AccountProfileApi;
import com.asterway.identity.api.error.IdentityErrorCode;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.AccountSessionSnapshot;
import com.asterway.identity.api.model.LoginGrant;
import com.asterway.identity.api.model.LoginInput;
import com.asterway.identity.api.model.LogoutResult;
import com.asterway.identity.api.model.PasswordChangeResult;
import com.asterway.identity.api.model.PasswordInput;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// 只协调账户能力与框架会话，HTTP 和数据库事务不混在控制器中。
@Service
public class AccountSessionService {

    private final AccountAuthenticationApi authenticationApi;
    private final AccountProfileApi profileApi;
    private final AccountPasswordApi passwordApi;
    private final AccountLogoutApi logoutApi;
    private final AccountContextResolver contexts;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextLogoutHandler logoutHandler;
    private final CsrfTokenRepository csrfTokenRepository;

    public AccountSessionService(
            AccountAuthenticationApi authenticationApi,
            AccountProfileApi profileApi,
            AccountPasswordApi passwordApi,
            AccountLogoutApi logoutApi,
            AccountContextResolver contexts,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            SecurityContextLogoutHandler logoutHandler,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.authenticationApi = authenticationApi;
        this.profileApi = profileApi;
        this.passwordApi = passwordApi;
        this.logoutApi = logoutApi;
        this.contexts = contexts;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.logoutHandler = logoutHandler;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public CsrfVO csrf(HttpServletRequest request, HttpServletResponse response) {
        contexts.context(request, null, true);
        CsrfToken token = csrfTokenRepository.loadDeferredToken(request, response).get();
        CsrfVO result = new CsrfVO(token.getHeaderName(), token.getToken());
        return result;
    }

    public AccountSessionVO login(LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID key = contexts.key(request);
        AccountContext context = contexts.context(request, key, true);
        LoginGrant grant = authenticationApi.login(context, input);
        AccountPrincipal existing = contexts.principal();
        if (grant.replayed()) {
            // 认证结果重放不能重新签发 Cookie；只允许已存在的同一身份核实快照。
            if (existing == null || !existing.accountId().equals(grant.accountId())) {
                throw new BusinessException(CommonErrorCode.AUTH_REQUIRED);
            }
        } else {
            authenticationApi.validateSession(grant.accountId(), grant.credentialVersion(), grant.securityVersion());
            establishSession(grant, request, response);
        }
        AccountSessionVO snapshot = current(request, response);
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

    public AccountSessionVO current(HttpServletRequest request, HttpServletResponse response) {
        AccountContext context = contexts.context(request, null, false);
        try {
            CsrfVO csrf = csrf(request, response);
            AccountSessionSnapshot snapshot = profileApi.current(context);
            return AccountSessionVO.from(snapshot, csrf);
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
        AccountContext context = contexts.context(request, contexts.key(request), false);
        PasswordChangeResult result = passwordApi.changePassword(context, input);
        clearSession(request, response);
        return result;
    }

    public LogoutResult logout(Boolean confirmed, HttpServletRequest request, HttpServletResponse response) {
        AccountContext context = contexts.context(request, contexts.key(request), false);
        String sessionId = request.getSession(false) == null ? null : request.getSession(false).getId();
        LogoutResult result = logoutApi.logout(context, confirmed, sessionId);
        clearSession(request, response);
        return result;
    }

    public void clearSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
    }
}
