-- 兼容扩展既有账户表；不改写已执行的 V001、V002。
ALTER TABLE app.app_user
    ALTER COLUMN password_hash TYPE TEXT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    ADD COLUMN deleted_at TIMESTAMPTZ(6),
    ADD COLUMN security_version BIGINT NOT NULL DEFAULT 1 CHECK (security_version > 0),
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 1 CHECK (credential_version > 0),
    ADD COLUMN login_name_version BIGINT NOT NULL DEFAULT 1 CHECK (login_name_version > 0),
    ADD COLUMN password_changed_at TIMESTAMPTZ(6),
    ADD COLUMN current_zone_period_id UUID,
    ADD COLUMN pending_zone_id VARCHAR(64),
    ADD COLUMN pending_zone_effective_at TIMESTAMPTZ(6),
    ADD COLUMN pending_zone_requested_at TIMESTAMPTZ(6);

CREATE TABLE app.user_zone_period (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    user_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK (revision > 0),
    zone_id VARCHAR(64) NOT NULL,
    tzdb_version VARCHAR(32) NOT NULL,
    valid_from TIMESTAMPTZ(6) NOT NULL,
    valid_to TIMESTAMPTZ(6),
    source VARCHAR(24) NOT NULL CHECK (source IN ('INITIAL_DEVICE', 'DEVICE_CHANGE', 'RECOVERY')),
    CONSTRAINT uq_zone_period_revision UNIQUE (user_id, revision),
    CONSTRAINT ck_zone_period_range CHECK (valid_to IS NULL OR valid_to > valid_from)
);
CREATE UNIQUE INDEX uq_zone_period_open ON app.user_zone_period (user_id) WHERE valid_to IS NULL;

-- 旧账户此前没有时区，采用明确的迁移参数；新注册必须提供本人选择的 IANA 时区。
INSERT INTO app.user_zone_period (user_id, revision, zone_id, tzdb_version, valid_from, source)
SELECT id, 1, '${legacyAccountZone}', 'legacy-unknown', created_at, 'RECOVERY'
FROM app.app_user;

UPDATE app.app_user u
SET current_zone_period_id = z.id, password_changed_at = u.created_at
FROM app.user_zone_period z
WHERE z.user_id = u.id AND z.valid_to IS NULL;

ALTER TABLE app.app_user
    ALTER COLUMN current_zone_period_id SET NOT NULL,
    ALTER COLUMN password_changed_at SET NOT NULL,
    ALTER COLUMN username_key SET NOT NULL,
    ADD CONSTRAINT ck_account_pending_zone CHECK (
        (pending_zone_id IS NULL AND pending_zone_effective_at IS NULL AND pending_zone_requested_at IS NULL)
        OR
        (pending_zone_id IS NOT NULL AND pending_zone_effective_at IS NOT NULL AND pending_zone_requested_at IS NOT NULL)
    );

CREATE TABLE app.account_guard (
    user_id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    recovered_generation BIGINT NOT NULL DEFAULT 0,
    recovery_state VARCHAR(16) NOT NULL DEFAULT 'CLEAR'
        CHECK (recovery_state IN ('CLEAR', 'ISOLATED', 'REVIEW')),
    isolation_reason VARCHAR(64)
);
INSERT INTO app.account_guard (user_id) SELECT id FROM app.app_user;

CREATE TABLE app.login_attempt (
    username_key VARCHAR(30) COLLATE "C" PRIMARY KEY,
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0,
    failure_count SMALLINT NOT NULL DEFAULT 0 CHECK (failure_count BETWEEN 0 AND 5),
    locked_until TIMESTAMPTZ(6),
    last_failed_at TIMESTAMPTZ(6),
    retain_until TIMESTAMPTZ(6) NOT NULL
);

CREATE TABLE app.captcha_challenge (
    id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0,
    purpose VARCHAR(16) NOT NULL CHECK (purpose IN ('REGISTER', 'LOGIN')),
    binding_digest BYTEA NOT NULL,
    verification_data BYTEA NOT NULL,
    expires_at TIMESTAMPTZ(6) NOT NULL,
    consumed_at TIMESTAMPTZ(6),
    create_key UUID NOT NULL,
    verify_key UUID,
    verify_fingerprint BYTEA,
    consumed_key UUID,
    consumed_fingerprint BYTEA,
    state VARCHAR(16) NOT NULL DEFAULT 'ISSUED'
        CHECK (state IN ('ISSUED', 'VERIFIED', 'FAILED', 'CONSUMED')),
    CONSTRAINT uq_captcha_create UNIQUE (binding_digest, create_key)
);
CREATE INDEX ix_captcha_expiry ON app.captcha_challenge (expires_at);

-- 本轮只使用固定公开配置，不增加管理后台功能。
CREATE TABLE app.system_config (
    config_key VARCHAR(32) PRIMARY KEY CHECK (config_key IN ('SITE_NAME', 'REGISTRATION_OPEN')),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0,
    value_text TEXT,
    value_boolean BOOLEAN,
    updated_by UUID,
    CHECK ((config_key = 'SITE_NAME' AND value_text IS NOT NULL AND value_boolean IS NULL)
        OR (config_key = 'REGISTRATION_OPEN' AND value_boolean IS NOT NULL AND value_text IS NULL))
);
INSERT INTO app.system_config (config_key, value_text) VALUES ('SITE_NAME', 'Asterway');
INSERT INTO app.system_config (config_key, value_boolean) VALUES ('REGISTRATION_OPEN', TRUE);

CREATE TABLE app.request_dedup (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0,
    scope_key VARCHAR(100) NOT NULL,
    user_id UUID,
    operation VARCHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL,
    request_fingerprint BYTEA NOT NULL,
    state VARCHAR(16) NOT NULL CHECK (state IN ('PROCESSING', 'SUCCEEDED', 'REJECTED')),
    http_status SMALLINT,
    result_code VARCHAR(64),
    result_type VARCHAR(32),
    result_id UUID,
    schema_version SMALLINT NOT NULL DEFAULT 1,
    result_summary JSONB NOT NULL DEFAULT '{}',
    expires_at TIMESTAMPTZ(6) NOT NULL,
    CONSTRAINT uq_request_dedup UNIQUE (scope_key, operation, idempotency_key)
);
CREATE INDEX ix_request_dedup_expiry ON app.request_dedup (expires_at);

CREATE TABLE app.audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    actor_user_id UUID,
    target_user_id UUID,
    action VARCHAR(64) NOT NULL,
    object_type VARCHAR(32) NOT NULL DEFAULT 'ACCOUNT',
    object_id VARCHAR(100),
    occurred_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    outcome VARCHAR(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'REJECTED', 'FAILED')),
    trace_id VARCHAR(64) NOT NULL,
    schema_version SMALLINT NOT NULL DEFAULT 1,
    details JSONB NOT NULL DEFAULT '{}'
);

CREATE TABLE app.security_change (
    operation_id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    version BIGINT NOT NULL DEFAULT 0,
    target_user_id UUID,
    actor_user_id UUID,
    change_type VARCHAR(32) NOT NULL,
    expected_security_version BIGINT,
    before_login_name_version BIGINT,
    after_security_version BIGINT,
    after_credential_version BIGINT,
    after_login_name_version BIGINT,
    db_outcome VARCHAR(16) NOT NULL DEFAULT 'COMMITTED',
    external_state VARCHAR(16) NOT NULL DEFAULT 'INTENT_SAVED',
    external_reference TEXT,
    committed_at TIMESTAMPTZ(6) NOT NULL DEFAULT clock_timestamp(),
    last_sync_at TIMESTAMPTZ(6),
    schema_version SMALLINT NOT NULL DEFAULT 1,
    safe_summary JSONB NOT NULL DEFAULT '{}'
);
CREATE INDEX ix_security_change_account ON app.security_change (target_user_id, committed_at);

COMMENT ON TABLE app.user_zone_period IS '账户有效时区区间，历史不重排，无物理外键。';
COMMENT ON TABLE app.account_guard IS '账户并发锁及安全核验隔离状态。';
COMMENT ON TABLE app.login_attempt IS '规范化登录名的短期凭据失败计数。';
COMMENT ON TABLE app.captcha_challenge IS '绑定匿名作用域的一次性拼图挑战，验证材料加密保存。';
COMMENT ON TABLE app.request_dedup IS '账户操作幂等结果，不保存密码、令牌或可重放的会话。';
COMMENT ON TABLE app.audit_event IS '账户操作审计，仅保存允许的安全元数据。';
COMMENT ON TABLE app.security_change IS '账户安全变更与独立本地账本的提交协调记录。';
