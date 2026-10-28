-- ============================================================
-- V001：创建最小账户表
-- 数据库：PostgreSQL
-- Schema：app
-- ============================================================

CREATE TABLE IF NOT EXISTS app.app_user (
    -- 账户唯一标识
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 用户原始用户名
                                            username VARCHAR(30) NOT NULL,

    -- 用户名标准化比较值：自动转为小写，用于唯一性校验
                                            username_key VARCHAR(30) COLLATE "C"
                                                GENERATED ALWAYS AS (
                                                   translate(
                                                           username,
                                                           'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
                                                           'abcdefghijklmnopqrstuvwxyz'
                                                   )
                                                   ) STORED,

    -- 用户昵称
                                            nickname VARCHAR(30) NOT NULL,

    -- 密码哈希值
                                            password_hash VARCHAR(255) NOT NULL,

    -- 用户角色
                                            role VARCHAR(16) NOT NULL DEFAULT 'USER',

    -- 账户状态
                                            status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',

    -- 账户创建时间
                                            created_at TIMESTAMPTZ(6)
                                                NOT NULL DEFAULT clock_timestamp(),

    -- 账户最后更新时间
                                            updated_at TIMESTAMPTZ(6)
                                                NOT NULL DEFAULT clock_timestamp(),

    -- 用户名唯一约束
                                            CONSTRAINT uq_app_user_username_key
                                                UNIQUE (username_key),

    -- 用户名长度约束
                                            CONSTRAINT ck_app_user_username_length
                                                CHECK (char_length(username) BETWEEN 1 AND 30),

    -- 昵称长度约束
                                            CONSTRAINT ck_app_user_nickname_length
                                                CHECK (char_length(nickname) BETWEEN 1 AND 30),

    -- 角色合法性约束
                                            CONSTRAINT ck_app_user_role
                                                CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN')),

    -- 账户状态约束
                                            CONSTRAINT ck_app_user_status
                                                CHECK (status IN ('ENABLED', 'DISABLED'))
);

-- ============================================================
-- 表注释
-- ============================================================

COMMENT ON TABLE app.app_user IS
    '用户账户主表，存储账户身份、登录凭据、角色及账户状态等基础信息。';

-- ============================================================
-- 字段注释
-- ============================================================

COMMENT ON COLUMN app.app_user.id IS
    '账户唯一标识，使用 UUID 作为主键。';

COMMENT ON COLUMN app.app_user.username IS
    '用户原始用户名，用于展示和账户识别。';

COMMENT ON COLUMN app.app_user.username_key IS
    '用户名标准化比较值，由数据库自动生成，用于用户名唯一性校验。';

COMMENT ON COLUMN app.app_user.nickname IS
    '用户昵称，用于页面展示。';

COMMENT ON COLUMN app.app_user.password_hash IS
    '密码哈希值，仅存储经过安全密码哈希算法处理的结果，不得存储明文密码。';

COMMENT ON COLUMN app.app_user.role IS
    '账户角色，用于权限控制，允许的角色为 USER、ADMIN、SUPER_ADMIN。';

COMMENT ON COLUMN app.app_user.status IS
    '账户状态，用于控制账户是否允许正常使用，允许的状态为 ENABLED、DISABLED。';

COMMENT ON COLUMN app.app_user.created_at IS
    '账户创建时间，使用带时区的时间戳记录。';

COMMENT ON COLUMN app.app_user.updated_at IS
    '账户最后更新时间，使用带时区的时间戳记录。';














