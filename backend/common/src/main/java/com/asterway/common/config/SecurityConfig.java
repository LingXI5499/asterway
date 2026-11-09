/*
 * 声明公共入口与受保护接口，装配身份过滤器和跨域规则。
 * 普通注释按包、字段、方法和语句说明作用；业务顺序与原实现一致。
 */
// 指定当前文件所属的 Java 包，防止同名类冲突并按业务组织代码。
package com.asterway.common.config;

// 引入Jwt身份认证筛选（JwtAuthenticationFilter），下面可以直接使用这个类型或静态成员。
import com.asterway.common.filter.JwtAuthenticationFilter;
// 引入账号Security提供器（AccountSecurityProvider），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.AccountSecurityProvider;
// 引入Redis缓存会话Store（RedisSessionStore），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.RedisSessionStore;
// 引入Security错误Handler（SecurityErrorHandler），下面可以直接使用这个类型或静态成员。
import com.asterway.common.security.SecurityErrorHandler;
// 引入Jwt身份凭据Utils（JwtTokenUtils），下面可以直接使用这个类型或静态成员。
import com.asterway.common.utils.JwtTokenUtils;
// 引入Dispatcher类型（DispatcherType），下面可以直接使用这个类型或静态成员。
import jakarta.servlet.DispatcherType;
// 引入有顺序的数据列表（List），下面可以直接使用这个类型或静态成员。
import java.util.List;
// 引入Bean，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Bean;
// 引入Configuration，下面可以直接使用这个类型或静态成员。
import org.springframework.context.annotation.Configuration;
// 引入HTTP请求方法（HttpMethod），下面可以直接使用这个类型或静态成员。
import org.springframework.http.HttpMethod;
// 引入Enable方法Security（EnableMethodSecurity），下面可以直接使用这个类型或静态成员。
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
// 引入HTTP请求Security（HttpSecurity），下面可以直接使用这个类型或静态成员。
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// 引入会话CreationPolicy（SessionCreationPolicy），下面可以直接使用这个类型或静态成员。
import org.springframework.security.config.http.SessionCreationPolicy;
// 引入Argon2密码Encoder（Argon2PasswordEncoder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
// 引入密码Encoder（PasswordEncoder），下面可以直接使用这个类型或静态成员。
import org.springframework.security.crypto.password.PasswordEncoder;
// 引入Security筛选Chain（SecurityFilterChain），下面可以直接使用这个类型或静态成员。
import org.springframework.security.web.SecurityFilterChain;
// 引入用户名密码身份认证筛选（UsernamePasswordAuthenticationFilter），下面可以直接使用这个类型或静态成员。
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// 引入CorsConfiguration，下面可以直接使用这个类型或静态成员。
import org.springframework.web.cors.CorsConfiguration;
// 引入地址BasedCorsConfiguration来源（UrlBasedCorsConfigurationSource），下面可以直接使用这个类型或静态成员。
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/* 公共安全入口，仅接受主动携带的 Bearer 凭证，不建立 Servlet 会话。 */
// 声明这里提供应用启动时需要的组件与配置。
// 定义类 SecurityConfig；声明公共入口与受保护接口，装配身份过滤器和跨域规则。
@Configuration
// 使用 EnableMethodSecurity 注解配置下面的类、方法或参数。
@EnableMethodSecurity
// 声明下面成员的可见范围、类型与名称，供本类或外部调用。
public class SecurityConfig {

    // 把此方法返回的对象登记为可被注入的共享组件。
    // 密码Encoder（passwordEncoder）方法：调用 passwordEncoder 完成密码Encoder处理；参数负责传入本次处理所需的数据。
    @Bean
    // 声明下面成员的可见范围、类型与名称，供本类或外部调用。
    public PasswordEncoder passwordEncoder() {
        // 与本地超管初始化的 Argon2id 参数保持一致；密码不能记录到日志。
        // 把新建 Argon2密码Encoder（Argon2PasswordEncoder） 实例交还给调用方，本方法到此结束。
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }

    // 把此方法返回的对象登记为可被注入的共享组件。
    // security筛选Chain（securityFilterChain）方法：调用 securityFilterChain 完成security筛选Chain处理；参数负责传入本次处理所需的数据。
    @Bean
    // HTTP请求（http），使用 HttpSecurity 类型保存。
    // 凭据处理器（tokens），使用 JwtTokenUtils 类型保存。
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenUtils tokens,
            // 会话列表（sessions），使用 RedisSessionStore 类型保存。
            // accounts，使用 AccountSecurityProvider 类型保存。
            // 错误处理器（errors），使用 SecurityErrorHandler 类型保存。
            RedisSessionStore sessions, AccountSecurityProvider accounts, SecurityErrorHandler errors) throws Exception {
        // 准备cors，类型为 CorsConfiguration；初始内容是新建 CorsConfiguration 实例。
        CorsConfiguration cors = new CorsConfiguration();
        // Vite 在默认端口被占用时会顺延；仅放行本地开发使用的明确来源。
        // 设置 AllowedOrigins 的值。
        cors.setAllowedOrigins(List.of(
                // 继续传入或组合固定文字 'http://localhost:5173", "http://127.0.0.1:5173'，完成上面尚未结束的调用或声明。
                "http://localhost:5173", "http://127.0.0.1:5173",
                // 继续传入或组合固定文字 'http://localhost:5174", "http://127.0.0.1:5174'，完成上面尚未结束的调用或声明。
                "http://localhost:5174", "http://127.0.0.1:5174",
                // 继续传入或组合固定文字 'http://localhost:5175", "http://127.0.0.1:5175'，完成上面尚未结束的调用或声明。
                "http://localhost:5175", "http://127.0.0.1:5175",
                // 继续传入或组合按表达式计算得到的值："http://localhost:5176", "http://127.0.0.1:5176"))，完成上面尚未结束的调用或声明。
                "http://localhost:5176", "http://127.0.0.1:5176"));
        // 设置 AllowedMethods 的值。
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // 设置 Allowed请求头集合（AllowedHeaders） 的值。
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // 设置 Exposed请求头集合（ExposedHeaders） 的值。
        cors.setExposedHeaders(List.of("X-Trace-Id", "X-Session-Token", "X-Session-Expires-At"));
        // 设置 允许Credentials（AllowCredentials） 的值。
        cors.setAllowCredentials(false);
        // 准备来源（source），类型为 null；初始内容是新建 地址BasedCorsConfiguration来源（UrlBasedCorsConfigurationSource） 实例。
        var source = new UrlBasedCorsConfigurationSource();
        // 调用 source.registerCorsConfiguration 完成registerCorsConfiguration处理。
        source.registerCorsConfiguration("/**", cors);
        // config，使用 null 类型保存。
        // 把调用 http.cors 完成cors处理交还给调用方，本方法到此结束。
        // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
        return http.cors(config -> config.configurationSource(source))
                // 不接受 Cookie、Basic 或表单自动凭证，所以按 Bearer 方式配置 CSRF。
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .csrf(config -> config.disable())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .httpBasic(config -> config.disable())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .formLogin(config -> config.disable())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .logout(config -> config.disable())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .requestCache(config -> config.disable())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .authorizeHttpRequests(config -> config
                        // 异步派发只处理已通过初始认证的 MVC 请求。
                        // 继续对上一步结果执行调用 dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll 完成permit全部处理。
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        // 继续对上一步结果执行调用 requestMatchers(HttpMethod.OPTIONS, "/**").permitAll 完成permit全部处理。
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 继续对上一步结果执行调用 requestMatchers 完成请求Matchers处理。
                        .requestMatchers(HttpMethod.POST, "/asterway/sessions", "/asterway/registrations",
                                // 继续传入或组合调用 "/asterway/captchas", "/asterway/captcha-verifications").permitAll 完成permit全部处理，完成上面尚未结束的调用或声明。
                                "/asterway/captchas", "/asterway/captcha-verifications").permitAll()
                        // 继续对上一步结果执行调用 requestMatchers(HttpMethod.GET, "/asterway/public/account-settings", "/asterway/time-zones").permitAll 完成permit全部处理。
                        .requestMatchers(HttpMethod.GET, "/asterway/public/account-settings", "/asterway/time-zones").permitAll()
                        // 继续对上一步结果执行调用 requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll 完成permit全部处理。
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 继续对上一步结果执行调用 anyRequest().authenticated 完成authenticated处理。
                        .anyRequest().authenticated())
                // config，使用 null 类型保存。
                // 定义一段交给集合或框架稍后执行的处理，输入左侧参数并计算右侧结果。
                .exceptionHandling(config -> config.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                // 继续对上一步结果执行调用 addFilterBefore 完成加入筛选变化前处理。
                .addFilterBefore(new JwtAuthenticationFilter(tokens, sessions, accounts, errors),
                        // 继续传入或组合表达式 UsernamePasswordAuthenticationFilter.class) 的结果，完成上面尚未结束的调用或声明。
                        UsernamePasswordAuthenticationFilter.class)
                // 继续对上一步结果执行调用 build 完成构造处理。
                .build();
    }
}
