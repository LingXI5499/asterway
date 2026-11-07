package com.asterway.common.config;

import com.asterway.common.filter.JwtAuthenticationFilter;
import com.asterway.common.security.AccountSecurityProvider;
import com.asterway.common.security.RedisSessionStore;
import com.asterway.common.security.SecurityErrorHandler;
import com.asterway.common.utils.JwtTokenUtils;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** 公共安全入口，仅接受主动携带的 Bearer 凭证，不建立 Servlet 会话。 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 与本地超管初始化的 Argon2id 参数保持一致；密码不能记录到日志。
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenUtils tokens,
            RedisSessionStore sessions, AccountSecurityProvider accounts, SecurityErrorHandler errors) throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        // Vite 在默认端口被占用时会顺延；仅放行本地开发使用的明确来源。
        cors.setAllowedOrigins(List.of(
                "http://localhost:5173", "http://127.0.0.1:5173",
                "http://localhost:5174", "http://127.0.0.1:5174",
                "http://localhost:5175", "http://127.0.0.1:5175",
                "http://localhost:5176", "http://127.0.0.1:5176"));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("X-Trace-Id", "X-Session-Token", "X-Session-Expires-At"));
        cors.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return http.cors(config -> config.configurationSource(source))
                // 不接受 Cookie、Basic 或表单自动凭证，所以按 Bearer 方式配置 CSRF。
                .csrf(config -> config.disable())
                .httpBasic(config -> config.disable())
                .formLogin(config -> config.disable())
                .logout(config -> config.disable())
                .requestCache(config -> config.disable())
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(config -> config
                        // 异步派发只处理已通过初始认证的 MVC 请求。
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/asterway/sessions", "/asterway/registrations",
                                "/asterway/captchas", "/asterway/captcha-verifications").permitAll()
                        .requestMatchers(HttpMethod.GET, "/asterway/public/account-settings", "/asterway/time-zones").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(config -> config.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .addFilterBefore(new JwtAuthenticationFilter(tokens, sessions, accounts, errors),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
