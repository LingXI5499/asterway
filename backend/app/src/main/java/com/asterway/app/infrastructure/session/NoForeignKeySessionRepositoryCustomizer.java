package com.asterway.app.infrastructure.session;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

// 为无外键的会话表注册专用 SQL，保留 Spring Session 原有的参数绑定和事务管理。
public class NoForeignKeySessionRepositoryCustomizer
        implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

    private static final String SQL_DIRECTORY = "sql/session/";

    private final String createSessionAttributeQuery;
    private final String updateSessionAttributeQuery;
    private final String deleteSessionQuery;
    private final String deleteSessionsByExpiryTimeQuery;

    public NoForeignKeySessionRepositoryCustomizer() throws IOException {
        createSessionAttributeQuery = loadSql("create-session-attribute.sql");
        updateSessionAttributeQuery = loadSql("update-session-attribute.sql");
        deleteSessionQuery = loadSql("delete-session.sql");
        deleteSessionsByExpiryTimeQuery = loadSql("delete-expired-sessions.sql");
    }

    @Override
    public void customize(JdbcIndexedSessionRepository repository) {
        repository.setCreateSessionAttributeQuery(createSessionAttributeQuery);
        repository.setUpdateSessionAttributeQuery(updateSessionAttributeQuery);
        repository.setDeleteSessionQuery(deleteSessionQuery);
        repository.setDeleteSessionsByExpiryTimeQuery(deleteSessionsByExpiryTimeQuery);
    }

    // 从类路径读取，兼容可执行 JAR；缺失或空文件直接阻止启动，避免回退到默认 SQL。
    private static String loadSql(String fileName) throws IOException {
        String resourcePath = SQL_DIRECTORY + fileName;
        ClassPathResource resource = new ClassPathResource(resourcePath);
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        if (sql.isBlank()) {
            throw new IllegalStateException("会话 SQL 资源不能为空：" + resourcePath);
        }
        return sql;
    }
}
