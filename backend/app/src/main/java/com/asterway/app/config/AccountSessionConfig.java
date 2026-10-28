package com.asterway.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.io.IOException;

// 装配账户会话的 Cookie 策略和 JDBC 仓库适配。
@Configuration
public class AccountSessionConfig {

    @Bean
    public CookieSerializer accountCookieSerializer(
            @Value("${asterway.identity.secure-cookie:true}") boolean secureCookie
    ) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secureCookie);
        return serializer;
    }

    @Bean
    public SessionRepositoryCustomizer<JdbcIndexedSessionRepository> noForeignKeySessionQueries()
            throws IOException {
        NoForeignKeySessionRepositoryCustomizer customizer =
                new NoForeignKeySessionRepositoryCustomizer();
        return customizer;
    }
}
