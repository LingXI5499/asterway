package com.asterway.common.filter;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.security.AccountPrincipal;
import com.asterway.common.security.AccountSecurityProvider;
import com.asterway.common.security.LoginSessionData;
import com.asterway.common.security.RedisSessionStore;
import com.asterway.common.security.SecurityAccountData;
import com.asterway.common.security.SecurityErrorHandler;
import com.asterway.common.utils.JwtTokenUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/** 请求认证与七天滑动续签；仅成功业务请求续期，设备列表轮询和退出不会保活。 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenUtils tokens;
    private final RedisSessionStore sessions;
    private final AccountSecurityProvider accounts;
    private final SecurityErrorHandler errors;

    public JwtAuthenticationFilter(
            JwtTokenUtils tokens,
            RedisSessionStore sessions,
            AccountSecurityProvider accounts,
            SecurityErrorHandler errors) {
        this.tokens = tokens;
        this.sessions = sessions;
        this.accounts = accounts;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/asterway/")
                || "OPTIONS".equals(request.getMethod())
                || path.startsWith("/asterway/public/")
                || path.equals("/asterway/time-zones")
                || path.equals("/asterway/captchas")
                || path.equals("/asterway/captcha-verifications")
                || path.equals("/asterway/registrations")
                || path.equals("/asterway/sessions") && "POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LoginSessionData session;
        SecurityAccountData account;
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ") || header.length() <= 7) {
                throw BusinessException.unauthorized();
            }
            var jwt = tokens.parse(header.substring(7));
            session = sessions.get(jwt.getClaimAsString("sid"));
            if (session == null
                    || session.expiresAt() <= System.currentTimeMillis()
                    || !session.userId().equals(jwt.getSubject())) {
                throw BusinessException.unauthorized();
            }
            account = accounts.loadSecurityAccount(Long.parseLong(session.userId()));
            if (account == null
                    || !account.enabled()
                    || account.authVersion() != session.authVersion()) {
                throw BusinessException.unauthorized();
            }
            if (account.mustChangePassword() && !passwordChangeAllowed(request)) {
                errors.write(response, 403, "PASSWORD_CHANGE_REQUIRED", "请先修改初始密码");
                return;
            }
        } catch (BusinessException error) {
            errors.write(response, error.getStatus(), error.getCode(), error.getMessage());
            return;
        } catch (JwtException | IllegalArgumentException error) {
            errors.write(response, 401, "UNAUTHORIZED", "登录已失效，请重新登录");
            return;
        } catch (DataAccessException | IllegalStateException error) {
            errors.write(response, 503, "SERVICE_UNAVAILABLE", "认证服务暂不可用，请稍后重试");
            return;
        }
        AccountPrincipal principal =
                new AccountPrincipal(
                        account.userId(),
                        account.username(),
                        account.mustChangePassword(),
                        account.authVersion(),
                        account.roles(),
                        account.permissions(),
                        session.id());
        var authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        account.permissions().stream().map(SimpleGrantedAuthority::new).toList());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // 缓存本模块的 JSON 响应，业务完成后、写出前才能安全增加续签响应头。
        ContentCachingResponseWrapper cached = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, cached);
            if (cached.getStatus() < 400 && renewable(request)) {
                try {
                    LoginSessionData renewed = sessions.touch(session);
                    if (renewed != null) {
                        cached.setHeader("X-Session-Token", tokens.create(renewed));
                        cached.setHeader(
                                "X-Session-Expires-At", Long.toString(renewed.expiresAt()));
                    }
                } catch (RuntimeException error) {
                    // 此时业务可能已经提交，保持真实业务结果，不把保存成功伪装成未保存。
                    LOGGER.warn("业务响应完成后续签不可用，类型：{}", error.getClass().getSimpleName());
                }
            }
        } finally {
            cached.copyBodyToResponse();
            SecurityContextHolder.clearContext();
        }
    }

    private boolean passwordChangeAllowed(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/asterway/me") && request.getMethod().equals("GET")
                || path.equals("/asterway/me/password") && request.getMethod().equals("PUT")
                || path.startsWith("/asterway/me/sessions") && request.getMethod().equals("DELETE");
    }

    private boolean renewable(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/asterway/me/sessions")
                && !path.equals("/asterway/sessions")
                && !(path.startsWith("/asterway/sessions/") && request.getMethod().equals("DELETE"))
                && !(path.equals("/asterway/executions/current")
                        && request.getMethod().equals("GET"))
                && !(request.getMethod().equals("GET")
                        && (path.equals("/asterway/dashboard/overview")
                                || path.equals("/asterway/notifications/unread-count")))
                && !(path.startsWith("/asterway/executions/")
                        && path.endsWith("/heartbeat")
                        && request.getMethod().equals("POST"))
                && !path.equals("/asterway/me/password");
    }
}
