package com.asterway.app.config;

import com.asterway.app.security.RestAccessDeniedHandler;
import com.asterway.app.security.AccountSessionFilter;
import com.asterway.app.security.AccountThrottleFilter;
import com.asterway.app.web.error.ApiErrorFactory;
import com.asterway.identity.api.AccountApi;
import com.asterway.app.security.RestAuthenticationEntryPoint;
import com.asterway.app.security.SpaCsrfTokenRequestHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.List;

// Spring Security 配置。
// 集中配置：密码算法、CSRF、会话、认证入口、访问控制规则。
// 注意：这里没有关闭 CSRF，写请求仍需合法 CSRF Token，
// 前端先调 /api/v1/auth/csrf 拿到 token 再通过 X-CSRF-TOKEN 请求头提交。
@Configuration
public class SecurityConfig {

    // 密码编码器：使用 Argon2 算法（Spring Security 推荐的默认参数）
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    // CSRF Token 仓库：把 token 存进 Cookie（httpOnly=false，前端才能读）
    @Bean
    public CsrfTokenRepository csrfTokenRepository(
            @Value("${asterway.identity.secure-cookie:true}") boolean secureCookie
    ) {

        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        // 指定前端提交 token 用的请求头名
        repository.setHeaderName("X-CSRF-TOKEN");
        repository.setCookieCustomizer(cookie -> cookie.secure(secureCookie).sameSite("Lax").path("/"));

        return repository;
    }

    // 安全上下文仓库：认证状态存到 HTTP 会话（配合 Spring Session JDBC 持久化）
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    // 会话认证策略：登录成功后做两件事
    //   1. 换会话 ID（防会话固定攻击）
    //   2. 刷新 CSRF Token（防旧 token 复用）
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository
    ) {
        List<SessionAuthenticationStrategy> strategies = List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)
        );
        SessionAuthenticationStrategy strategy = new CompositeSessionAuthenticationStrategy(strategies);
        return strategy;
    }

    // 注销处理器：退出登录时清理认证状态
    @Bean
    public SecurityContextLogoutHandler securityContextLogoutHandler() {
        return new SecurityContextLogoutHandler();
    }

    // 核心安全过滤链
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository,
            ObjectMapper objectMapper,
            AccountApi accounts,
            ApiErrorFactory errors,
            SecurityContextLogoutHandler logoutHandler
    ) throws Exception {

        http
                // CSRF：用 Cookie 存 token，SPA 专用解析器
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(
                                new SpaCsrfTokenRequestHandler()
                        )
                )
                // 指定安全上下文仓库
                .securityContext(context -> context
                        .securityContextRepository(
                                securityContextRepository
                        )
                )
                // 会话策略：按需创建会话（登录时才建）
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )
                // 未认证/无权限时，用自定义的 REST 风格错误响应
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                new RestAuthenticationEntryPoint(
                                        objectMapper
                                )
                        )
                        .accessDeniedHandler(
                                new RestAccessDeniedHandler(
                                        objectMapper
                                )
                        )
                )
                // 访问控制规则
                .authorizeHttpRequests(auth -> auth
                        // GET csrf 接口放行（任何人都要先拿 token）
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/csrf",
                                "/api/v1/public/**"
                        ).permitAll()
                        // 文档端点由配置决定是否启用，不绕过业务接口的认证。
                        .requestMatchers(
                                HttpMethod.GET,
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // 注册、登录放行
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/captcha-challenges",
                                "/api/v1/auth/captcha-challenges/*/verify"
                        ).permitAll()
                        // 其余接口都需要认证
                        .anyRequest().authenticated()
                )
                // 关闭表单登录和 HTTP Basic，因为走的是自定义 REST 登录
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 所有注销必须通过账户用例，不能走框架默认入口绕过未结束执行检查。
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .addFilterBefore(new AccountThrottleFilter(objectMapper), AuthorizationFilter.class)
                .addFilterBefore(new AccountSessionFilter(
                        accounts, objectMapper, errors, logoutHandler, csrfTokenRepository
                ), AuthorizationFilter.class);

        return http.build();
    }
}
