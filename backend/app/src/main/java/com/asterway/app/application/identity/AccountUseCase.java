package com.asterway.app.application.identity;

import com.asterway.app.security.AccountPrincipal;
import com.asterway.app.web.identity.response.LoginResponse;

import com.asterway.identity.api.AccountApi;
import com.asterway.identity.api.model.AuthenticatedAccount;
import com.asterway.identity.api.model.LoginCommand;
import com.asterway.identity.api.model.RegisterCommand;
import com.asterway.identity.api.model.RegistrationResult;

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
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Component;

import java.util.List;

// 账户用例（业务编排层）。
// 负责协调 identity 模块的业务能力与 Spring Security 的认证/会话机制。
// 关键点：手动调用认证不等于走 Spring Security 的认证过滤器，
// 所以必须在这里显式地：调用会话策略 -> 设置 SecurityContext -> 保存上下文，
// 才算真正建立了服务端登录状态，而不是只返回一个"登录成功"的 JSON。
@Component
public class AccountUseCase {

    // identity 模块的账户业务入口
    private final AccountApi accountApi;

    // 会话认证策略：登录时做会话固定攻击防护、刷新 CSRF Token
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    // 安全上下文仓库：把认证状态保存到会话（JDBC 会话表）
    private final SecurityContextRepository securityContextRepository;

    // 注销处理器：退出登录时清理认证状态
    private final SecurityContextLogoutHandler logoutHandler;

    // CSRF Token 仓库：注销后清除旧 token
    private final CsrfTokenRepository csrfTokenRepository;

    public AccountUseCase(
            AccountApi accountApi,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            SecurityContextLogoutHandler logoutHandler,
            CsrfTokenRepository csrfTokenRepository
    ) {
        this.accountApi = accountApi;
        this.sessionAuthenticationStrategy =
                sessionAuthenticationStrategy;
        this.securityContextRepository =
                securityContextRepository;
        this.logoutHandler = logoutHandler;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    // 注册：直接委托给 identity 模块，本身不做额外编排
    public RegistrationResult register(RegisterCommand command) {
        return accountApi.register(command);
    }

    // 登录：校验凭据 -> 构造安全身份 -> 建立服务端会话
    public LoginResponse login(
            LoginCommand command,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1. 调 identity 模块校验用户名密码，拿到安全账户摘要
        AuthenticatedAccount account =
                accountApi.authenticate(command);

        // 2. 把账户摘要包装成 Spring Security 的安全身份对象
        AccountPrincipal principal = new AccountPrincipal(
                account.accountId(),
                account.username(),
                account.nickname(),
                account.role()
        );

        // 3. 构造一个"已认证"的 Authentication，附带角色权限
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + account.role()
                                )
                        )
                );

        // 4. 触发会话认证策略：换会话 ID（防会话固定攻击）+ 刷新 CSRF Token
        sessionAuthenticationStrategy.onAuthentication(
                authentication,
                request,
                response
        );

        // 5. 创建新的安全上下文并放入认证信息
        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 6. 显式保存上下文到会话，使后续请求能恢复登录状态（关键步骤）
        securityContextRepository.saveContext(
                context,
                request,
                response
        );

        // 7. 返回登录响应
        return new LoginResponse(
                account.accountId(),
                account.username(),
                account.nickname(),
                account.role()
        );
    }

    // 注销：清理认证状态 + 清除 CSRF Token
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        logoutHandler.logout(
                request,
                response,
                SecurityContextHolder.getContext().getAuthentication()
        );

        // 注销后主动清空 CSRF Token，下次写请求需要重新获取
        csrfTokenRepository.saveToken(
                null,
                request,
                response
        );
    }
}
