-- Asterway execution 模块，PostgreSQL 17.11 手工初始化。
-- 一业务模块一份 SQL；同名对象已存在时失败，不覆盖历史数据。
-- 仅建立执行会话和连续时间段；无物理外键、级联、触发器或自动迁移。
-- 时间均为服务端时间点，精度为毫秒；应用不能接受客户端补录时间。
-- 当前为 SQL 验收阶段，不在开发库执行；权限初始化在 identity.sql 独立标记块内。
BEGIN;

CREATE TABLE execution_session (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    task_id bigint NOT NULL CHECK (task_id > 0),
    status varchar(20) NOT NULL DEFAULT 'RUNNING',
    started_at timestamptz(3) NOT NULL,
    finished_at timestamptz(3),
    last_heartbeat_at timestamptz(3) NOT NULL,
    controller_session_id varchar(64),
    finish_reason varchar(24),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_execution_session_status CHECK (status IN ('RUNNING', 'PAUSED', 'FINISHED')),
    CONSTRAINT ck_execution_session_finish CHECK (
        (status = 'FINISHED' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL)
        OR (status IN ('RUNNING', 'PAUSED') AND finished_at IS NULL AND finish_reason IS NULL)
    ),
    CONSTRAINT ck_execution_session_finish_reason CHECK (
        finish_reason IN ('USER_FINISH', 'TASK_DELETED', 'GOAL_ARCHIVED')
    ),
    CONSTRAINT ck_execution_session_controller CHECK (
        (status = 'RUNNING' AND controller_session_id IS NOT NULL
            AND controller_session_id ~ '[^[:space:]]')
        OR (status IN ('PAUSED', 'FINISHED') AND controller_session_id IS NULL)
    ),
    CONSTRAINT ck_execution_session_time CHECK (
        isfinite(started_at) AND isfinite(last_heartbeat_at)
        AND last_heartbeat_at >= started_at
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at >= last_heartbeat_at))
    ),
    CONSTRAINT ck_execution_session_audit_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);

COMMENT ON TABLE execution_session IS '任务的一次完整执行；可暂停和恢复，结束后保留，不提供删除或补时功能';
COMMENT ON COLUMN execution_session.id IS '数据库生成的 BIGINT 主键，对外按字符串传输';
COMMENT ON COLUMN execution_session.user_id IS '所属用户，由认证上下文取得；跨表同用户关系由 Service 校验';
COMMENT ON COLUMN execution_session.task_id IS '任务逻辑标识；任务逻辑删除后保留历史引用';
COMMENT ON COLUMN execution_session.status IS 'RUNNING 运行、PAUSED 暂停、FINISHED 已结束；终态不可恢复由 Service 保证';
COMMENT ON COLUMN execution_session.started_at IS '会话首次开始的服务端时间，恢复时不覆盖';
COMMENT ON COLUMN execution_session.finished_at IS '最终结束的服务端时间；暂停后结束不会增加执行时间段';
COMMENT ON COLUMN execution_session.last_heartbeat_at IS '最后有效心跳；开始和恢复时初始化，不随普通查询更新';
COMMENT ON COLUMN execution_session.controller_session_id IS '当前控制端的登录 SID，只在 RUNNING 时保存；不是 JWT，不返回前端';
COMMENT ON COLUMN execution_session.finish_reason IS 'USER_FINISH 主动结束、TASK_DELETED 删除任务、GOAL_ARCHIVED 归档目标';
COMMENT ON COLUMN execution_session.version IS '控制版本，由状态变更递增；心跳不递增';
COMMENT ON COLUMN execution_session.created_at IS '创建时间，毫秒精度';
COMMENT ON COLUMN execution_session.updated_at IS '更新时间，由应用维护，无自动更新时间触发器';

CREATE UNIQUE INDEX uk_execution_session_user_running
    ON execution_session (user_id) WHERE status = 'RUNNING';
-- 同一任务的 RUNNING 与 PAUSED 一并互斥，比只限制 PAUSED 更完整。
CREATE UNIQUE INDEX uk_execution_session_user_task_unfinished
    ON execution_session (user_id, task_id) WHERE status IN ('RUNNING', 'PAUSED');
CREATE INDEX ix_execution_session_user_page
    ON execution_session (user_id, started_at DESC, id DESC);
CREATE INDEX ix_execution_session_user_task_page
    ON execution_session (user_id, task_id, started_at DESC, id DESC);
CREATE INDEX ix_execution_session_running_heartbeat
    ON execution_session (last_heartbeat_at, id) WHERE status = 'RUNNING';

CREATE TABLE execution_segment (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id bigint NOT NULL CHECK (execution_id > 0),
    assignment_id bigint NOT NULL CHECK (assignment_id > 0),
    segment_no integer NOT NULL CHECK (segment_no > 0),
    started_at timestamptz(3) NOT NULL,
    ended_at timestamptz(3),
    end_reason varchar(24),
    eligible_until timestamptz(3) NOT NULL,
    time_zone varchar(64) NOT NULL CHECK (time_zone ~ '[^[:space:]]'),
    heartbeat_timeout_seconds integer NOT NULL DEFAULT 90 CHECK (heartbeat_timeout_seconds > 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_execution_segment_sequence UNIQUE (execution_id, segment_no),
    CONSTRAINT ck_execution_segment_end_pair CHECK (
        (ended_at IS NULL AND end_reason IS NULL)
        OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)
    ),
    CONSTRAINT ck_execution_segment_end_reason CHECK (
        end_reason IN ('USER_PAUSE', 'AUTO_PAUSE', 'PLAN_BOUNDARY', 'AUTH_REVOKED', 'FINISH')
    ),
    CONSTRAINT ck_execution_segment_time CHECK (
        isfinite(started_at) AND isfinite(eligible_until) AND eligible_until > started_at
        AND (ended_at IS NULL OR (isfinite(ended_at) AND ended_at >= started_at AND ended_at <= eligible_until))
    ),
    CONSTRAINT ck_execution_segment_audit_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);

COMMENT ON TABLE execution_segment IS '每次开始或继续形成一个连续执行时间段；暂停间隔不计时，历史段不改期';
COMMENT ON COLUMN execution_segment.id IS '数据库生成的 BIGINT 主键；心跳携带当前时间段标识，阻止旧段心跳刷新新段';
COMMENT ON COLUMN execution_segment.execution_id IS '所属执行会话逻辑标识；归属和状态一致性由 Service 校验';
COMMENT ON COLUMN execution_segment.assignment_id IS '本段开始时的任务安排标识；恢复时可引用新安排，已结束段保留旧安排';
COMMENT ON COLUMN execution_segment.segment_no IS '会话内连续序号，从 1 开始，由用户锁保护下的应用分配';
COMMENT ON COLUMN execution_segment.started_at IS '首次开始或本次继续的服务端时间';
COMMENT ON COLUMN execution_segment.ended_at IS '暂停或结束的有效截止时间；为空表示当前运行段';
COMMENT ON COLUMN execution_segment.end_reason IS 'USER_PAUSE 主动暂停、AUTO_PAUSE 超时、PLAN_BOUNDARY 边界、AUTH_REVOKED 登录撤销、FINISH 结束';
COMMENT ON COLUMN execution_segment.eligible_until IS '当前日结束或当前周结束，与待生效时区边界取较早值；时区调整需同步缩短开放段截止时间';
COMMENT ON COLUMN execution_segment.time_zone IS '本段开始时使用的时区；IANA 有效性由应用校验，历史查询按查询时有效时区切日';
COMMENT ON COLUMN execution_segment.heartbeat_timeout_seconds IS '本段的超时秒数快照，默认 90；配置变化不追溯改变已有段';
COMMENT ON COLUMN execution_segment.created_at IS '创建时间，毫秒精度';
COMMENT ON COLUMN execution_segment.updated_at IS '更新时间，由应用维护；已结束时间段不对外开放编辑';

CREATE UNIQUE INDEX uk_execution_segment_open
    ON execution_segment (execution_id) WHERE ended_at IS NULL;
CREATE INDEX ix_execution_segment_open_deadline
    ON execution_segment (eligible_until, execution_id) WHERE ended_at IS NULL;
CREATE INDEX ix_execution_segment_assignment
    ON execution_segment (assignment_id, execution_id);

-- 跨表约束由后续 Service 事务保证：先锁 identity_user，再检查会话与时间段。
-- RUNNING 恰有一个开放段；PAUSED/FINISHED 无开放段；段不重叠、首段与会话首次开始一致。
-- 任务、父链、安排及用户关系校验不放进普通 CHECK，不用触发器代替 Service。
-- 耗时按段求和，开放段封顶到 min(服务端当前时间, 最后心跳+超时快照, eligible_until)。
-- 本文件没有写入任何执行示例、账号、默认密码或其他业务初始化数据。
COMMIT;
