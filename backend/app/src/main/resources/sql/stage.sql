-- Asterway stage 模块，PostgreSQL 17.11 手工初始化。
-- 一业务模块一份 SQL；仅创建新表，已有同名对象时失败，不覆盖已有业务数据。
-- 无物理外键、触发器、级联、自动迁移；跨表关系由应用事务校验。
-- 日期是用户本地日历日期，timestamptz 是时间点；不使用服务器 CURRENT_DATE 推断用户日期。
BEGIN;

CREATE TABLE stage (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    goal_id bigint NOT NULL CHECK (goal_id > 0),
    name varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]'),
    description text NOT NULL DEFAULT '',
    completion_criteria text NOT NULL DEFAULT '',
    start_date date CHECK (isfinite(start_date)),
    end_date date CHECK (isfinite(end_date)),
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    status varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at timestamptz,
    CONSTRAINT ck_stage_dates CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

COMMENT ON TABLE stage IS '目标下的阶段；不得脱离所属用户与目标，进度不重复存储';
COMMENT ON COLUMN stage.id IS '主键，数据库生成的 BIGINT 标识';
COMMENT ON COLUMN stage.user_id IS '所属用户，必须取当前认证身份';
COMMENT ON COLUMN stage.goal_id IS '所属目标逻辑标识，须与 user_id 同属当前用户';
COMMENT ON COLUMN stage.name IS '名称，1～100 字且不能仅为空白';
COMMENT ON COLUMN stage.description IS '描述，未填写时为空字符串';
COMMENT ON COLUMN stage.completion_criteria IS '完成条件，未填写时为空字符串';
COMMENT ON COLUMN stage.start_date IS '阶段开始日期，可空，不强制限制在目标日期内';
COMMENT ON COLUMN stage.end_date IS '阶段结束日期，可空';
COMMENT ON COLUMN stage.sort_order IS '显示排序，数值越小越靠前；相同值按主键排序';
COMMENT ON COLUMN stage.status IS '状态：未开始、进行中、已完成';
COMMENT ON COLUMN stage.version IS '乐观锁版本，由应用更新时递增';
COMMENT ON COLUMN stage.created_at IS '创建时间，按时间点存储';
COMMENT ON COLUMN stage.updated_at IS '更新时间，由应用更新时维护';
COMMENT ON COLUMN stage.deleted_at IS '逻辑删除时间，NULL 表示未删除';

CREATE INDEX ix_stage_user_goal_order ON stage (user_id, goal_id, sort_order, id) WHERE deleted_at IS NULL;

-- BEGIN STAGE STATE HISTORY
CREATE TABLE stage_state_history (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    stage_id bigint NOT NULL CHECK (stage_id > 0),
    event_type varchar(24) NOT NULL CHECK (event_type IN ('CREATED', 'STATUS_CHANGED', 'DELETED', 'BASELINE')),
    before_status varchar(20) CHECK (before_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    after_status varchar(20) NOT NULL CHECK (after_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    name_snapshot varchar(100) NOT NULL CHECK (name_snapshot ~ '[^[:space:]]'),
    entity_version bigint NOT NULL CHECK (entity_version >= 0),
    occurred_at timestamptz(3) NOT NULL CHECK (isfinite(occurred_at)),
    source_type varchar(24) NOT NULL DEFAULT 'USER_ACTION' CHECK (source_type IN ('USER_ACTION', 'SYSTEM_INIT')),
    created_at timestamptz(3) NOT NULL DEFAULT clock_timestamp() CHECK (isfinite(created_at)),
    CONSTRAINT uk_stage_state_history_event UNIQUE (stage_id, entity_version, event_type),
    CONSTRAINT ck_stage_state_history_shape CHECK ((
        (event_type = 'CREATED' AND before_status IS NULL AND after_status = 'NOT_STARTED')
        OR (event_type = 'BASELINE' AND before_status IS NULL AND source_type = 'SYSTEM_INIT')
        OR (event_type = 'STATUS_CHANGED' AND before_status IS NOT NULL AND before_status <> after_status)
        OR (event_type = 'DELETED' AND before_status IS NOT NULL AND before_status = after_status)
    ) IS TRUE)
);

COMMENT ON TABLE stage_state_history IS '阶段状态的追加式事实；只记实际变化，BASELINE 不计完成，历史不可由业务接口修改删除';
COMMENT ON COLUMN stage_state_history.id IS '数据库生成的 BIGINT 标识，对外按字符串传输';
COMMENT ON COLUMN stage_state_history.user_id IS '所属用户，写入与查询均由服务端限定本人';
COMMENT ON COLUMN stage_state_history.stage_id IS '所属业务对象逻辑标识；逻辑删除后保留引用';
COMMENT ON COLUMN stage_state_history.event_type IS '真实业务事件类型；BASELINE 为上线时当前状态，COMPLETION_IMPORTED 仅用于导入可信任务完成时间';
COMMENT ON COLUMN stage_state_history.before_status IS '变化前状态；创建、初始基线和可信完成导入时为空';
COMMENT ON COLUMN stage_state_history.after_status IS '变化后状态，使用当前业务对象既有枚举';
COMMENT ON COLUMN stage_state_history.name_snapshot IS '该事件发生时的名称；不是整份业务资料快照';
COMMENT ON COLUMN stage_state_history.entity_version IS '该事件对应的主体版本，允许无状态变化的资料修改留下版本间隔';
COMMENT ON COLUMN stage_state_history.occurred_at IS '实际发生时间，或旧任务已知的 completed_at；基线采用初始化时间';
COMMENT ON COLUMN stage_state_history.source_type IS '由服务端填写：用户动作、通知确认、执行开始或系统初始化';
COMMENT ON COLUMN stage_state_history.created_at IS '记录入库时间，导入旧完成事实时可以晚于 occurred_at';

CREATE UNIQUE INDEX uk_stage_state_history_baseline ON stage_state_history (stage_id) WHERE event_type = 'BASELINE';
CREATE INDEX ix_stage_state_history_user_time ON stage_state_history (user_id, occurred_at DESC, id DESC);
CREATE INDEX ix_stage_state_history_object ON stage_state_history (user_id, stage_id, occurred_at DESC, id DESC);
-- END STAGE STATE HISTORY

-- BEGIN REMAINING STAGE HISTORY BOOTSTRAP
-- 后端验收后显式开启 remaining_backend_ready 与 remaining_history_bootstrap，默认不回填。
-- 手工维护事务内先锁定相关用户，先基线后开放新后端；已有任何历史的对象都不补基线。
DO $stage_history_bootstrap$
DECLARE
    approved boolean := COALESCE(NULLIF(current_setting('asterway.remaining_history_bootstrap', true), ''), 'false')::boolean;
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_backend_ready', true), ''), 'false')::boolean;
    initialized_at timestamptz(3) := clock_timestamp();
BEGIN
    IF approved AND NOT backend_ready THEN
        RAISE EXCEPTION 'Remaining fact initialization requires the approved backend';
    END IF;
    IF approved THEN
        INSERT INTO stage_state_history(user_id,stage_id,event_type,after_status,name_snapshot,entity_version,occurred_at,source_type,created_at)
        SELECT s.user_id,s.id,'BASELINE',s.status,s.name,s.version,initialized_at,'SYSTEM_INIT',initialized_at
        FROM stage s WHERE NOT EXISTS (SELECT 1 FROM stage_state_history h WHERE h.stage_id=s.id)
        ON CONFLICT DO NOTHING;
    END IF;
END
$stage_history_bootstrap$;
-- END REMAINING STAGE HISTORY BOOTSTRAP

COMMIT;
