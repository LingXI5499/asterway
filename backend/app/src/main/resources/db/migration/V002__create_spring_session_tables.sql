-- ============================================================
-- V002：创建 Spring Session JDBC 会话表
-- 数据库：PostgreSQL
-- Schema：session_store（与业务表 app 分离，避免污染业务 schema）
-- ============================================================
--
-- 背景：
--   spring.session.jdbc.initialize-schema = never，
--   Spring Boot 不会自动建表，必须交由受控迁移建立。
--
-- 说明：
--   1. Flyway 的 schemas 列表包含 migration_meta / app / session_store，
--      迁移会在每个 schema 上各执行一次，因此这里显式写死 schema 前缀，
--      并用 IF NOT EXISTS 保证幂等。
--   2. 表名为大写无引号标识符，PostgreSQL 会折叠为小写 spring_session，
--      与 Spring Session 默认表名一致。
--   3. 不建立物理外键，符合本项目"无物理外键"的约定。
-- ============================================================

CREATE TABLE IF NOT EXISTS session_store.SPRING_SESSION (
    PRIMARY_ID CHAR(36) NOT NULL,
    SESSION_ID CHAR(36) NOT NULL,
    CREATION_TIME BIGINT NOT NULL,
    LAST_ACCESS_TIME BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME BIGINT NOT NULL,
    PRINCIPAL_NAME VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS SPRING_SESSION_IX1
    ON session_store.SPRING_SESSION (SESSION_ID);

CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX2
    ON session_store.SPRING_SESSION (EXPIRY_TIME);

CREATE INDEX IF NOT EXISTS SPRING_SESSION_IX3
    ON session_store.SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS session_store.SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK
        PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
);

-- ============================================================
-- 表注释
-- ============================================================

COMMENT ON TABLE session_store.SPRING_SESSION IS
    '服务端会话主表，由 Spring Session JDBC 管理，支持服务端可撤销登录状态。';

COMMENT ON TABLE session_store.SPRING_SESSION_ATTRIBUTES IS
    '服务端会话属性表，存放序列化后的 SecurityContext 等会话属性。';

-- ============================================================
-- 字段注释
-- ============================================================

COMMENT ON COLUMN session_store.SPRING_SESSION.PRIMARY_ID IS
    '会话主键，使用 UUID 字符串。';

COMMENT ON COLUMN session_store.SPRING_SESSION.SESSION_ID IS
    '对外暴露的会话标识，写入浏览器 Cookie。';

COMMENT ON COLUMN session_store.SPRING_SESSION.CREATION_TIME IS
    '会话创建时间，毫秒时间戳。';

COMMENT ON COLUMN session_store.SPRING_SESSION.LAST_ACCESS_TIME IS
    '会话最后访问时间，毫秒时间戳。';

COMMENT ON COLUMN session_store.SPRING_SESSION.MAX_INACTIVE_INTERVAL IS
    '会话最大不活动间隔，单位为秒。';

COMMENT ON COLUMN session_store.SPRING_SESSION.EXPIRY_TIME IS
    '会话过期时间，毫秒时间戳，用于清理过期会话。';

COMMENT ON COLUMN session_store.SPRING_SESSION.PRINCIPAL_NAME IS
    '会话关联的登录主体名称，便于按用户查询会话。';
