-- 定义执行计时表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway execution 模块，PostgreSQL 17.11 手工初始化。
-- 一业务模块一份 SQL；同名对象已存在时失败，不覆盖历史数据。
-- 仅建立执行会话和连续时间段；无物理外键、级联、触发器或自动迁移。
-- 时间均为服务端时间点，精度为毫秒；应用不能接受客户端补录时间。
-- 当前为 SQL 验收阶段，不在开发库执行；权限初始化在 identity.sql 独立标记块内。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 execution_session 表；任务的一次完整执行；可暂停和恢复，结束后保留，不提供删除或补时功能。
CREATE TABLE execution_session (
    -- id 保存数据库生成的 BIGINT 主键，对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，由认证上下文取得；跨表同用户关系由 Service 校验；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- task_id 保存任务逻辑标识；任务逻辑删除后保留历史引用；类型为大整数；必须填写，保存时检查后面的限制。
    task_id bigint NOT NULL CHECK (task_id > 0),
    -- status 保存RUNNING 运行、PAUSED 暂停、FINISHED 已结束；终态不可恢复由 Service 保证；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(20) NOT NULL DEFAULT 'RUNNING',
    -- started_at 保存会话首次开始的服务端时间，恢复时不覆盖；类型为带时区的时间点；必须填写。
    started_at timestamptz(3) NOT NULL,
    -- finished_at 保存最终结束的服务端时间；暂停后结束不会增加执行时间段；类型为带时区的时间点。
    finished_at timestamptz(3),
    -- last_heartbeat_at 保存最后有效心跳；开始和恢复时初始化，不随普通查询更新；类型为带时区的时间点；必须填写。
    last_heartbeat_at timestamptz(3) NOT NULL,
    -- controller_session_id 保存当前控制端的登录 SID，只在 RUNNING 时保存；不是 JWT，不返回前端；类型为有长度限制的文字。
    controller_session_id varchar(64),
    -- finish_reason 保存USER_FINISH 主动结束、TASK_DELETED 删除任务、GOAL_ARCHIVED 归档目标；类型为有长度限制的文字。
    finish_reason varchar(24),
    -- version 保存控制版本，由状态变更递增；心跳不递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_status CHECK (status IN ('RUNNING', 'PAUSED', 'FINISHED')),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_finish CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'FINISHED' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('RUNNING', 'PAUSED') AND finished_at IS NULL AND finish_reason IS NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_finish_reason CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：finish_reason IN ('USER_FINISH', 'TASK_DELETED', 'GOAL_ARCHIVED')。
        finish_reason IN ('USER_FINISH', 'TASK_DELETED', 'GOAL_ARCHIVED')
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_controller CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'RUNNING' AND controller_session_id IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND controller_session_id ~ '[^[:space:]]')
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('PAUSED', 'FINISHED') AND controller_session_id IS NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_time CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：isfinite(started_at) AND isfinite(last_heartbeat_at)。
        isfinite(started_at) AND isfinite(last_heartbeat_at)
        -- 再增加一个必须同时满足的筛选条件。
        AND last_heartbeat_at >= started_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at >= last_heartbeat_at))
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_session_audit_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE execution_session IS '任务的一次完整执行；可暂停和恢复，结束后保留，不提供删除或补时功能';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.id IS '数据库生成的 BIGINT 主键，对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.user_id IS '所属用户，由认证上下文取得；跨表同用户关系由 Service 校验';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.task_id IS '任务逻辑标识；任务逻辑删除后保留历史引用';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.status IS 'RUNNING 运行、PAUSED 暂停、FINISHED 已结束；终态不可恢复由 Service 保证';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.started_at IS '会话首次开始的服务端时间，恢复时不覆盖';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.finished_at IS '最终结束的服务端时间；暂停后结束不会增加执行时间段';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.last_heartbeat_at IS '最后有效心跳；开始和恢复时初始化，不随普通查询更新';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.controller_session_id IS '当前控制端的登录 SID，只在 RUNNING 时保存；不是 JWT，不返回前端';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.finish_reason IS 'USER_FINISH 主动结束、TASK_DELETED 删除任务、GOAL_ARCHIVED 归档目标';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.version IS '控制版本，由状态变更递增；心跳不递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.created_at IS '创建时间，毫秒精度';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_session.updated_at IS '更新时间，由应用维护，无自动更新时间触发器';

-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_execution_session_user_running
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ON execution_session (user_id) WHERE status = 'RUNNING';
-- 同一任务的 RUNNING 与 PAUSED 一并互斥，比只限制 PAUSED 更完整。
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_execution_session_user_task_unfinished
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON execution_session (user_id, task_id) WHERE status IN ('RUNNING', 'PAUSED');。
    ON execution_session (user_id, task_id) WHERE status IN ('RUNNING', 'PAUSED');
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_execution_session_user_page
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON execution_session (user_id, started_at DESC, id DESC);。
    ON execution_session (user_id, started_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_execution_session_user_task_page
    -- 继续列出本次使用的字段：任务编号（task_id）。
    ON execution_session (user_id, task_id, started_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_execution_session_running_heartbeat
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ON execution_session (last_heartbeat_at, id) WHERE status = 'RUNNING';

-- 创建 execution_segment 表；每次开始或继续形成一个连续执行时间段；暂停间隔不计时，历史段不改期。
CREATE TABLE execution_segment (
    -- id 保存数据库生成的 BIGINT 主键；心跳携带当前时间段标识，阻止旧段心跳刷新新段；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- execution_id 保存所属执行会话逻辑标识；归属和状态一致性由 Service 校验；类型为大整数；必须填写，保存时检查后面的限制。
    execution_id bigint NOT NULL CHECK (execution_id > 0),
    -- assignment_id 保存本段开始时的任务安排标识；恢复时可引用新安排，已结束段保留旧安排；类型为大整数；必须填写，保存时检查后面的限制。
    assignment_id bigint NOT NULL CHECK (assignment_id > 0),
    -- segment_no 保存会话内连续序号，从 1 开始，由用户锁保护下的应用分配；类型为整数；必须填写，保存时检查后面的限制。
    segment_no integer NOT NULL CHECK (segment_no > 0),
    -- started_at 保存首次开始或本次继续的服务端时间；类型为带时区的时间点；必须填写。
    started_at timestamptz(3) NOT NULL,
    -- 结束前面的条件表达式、循环或数据库程序块。
    ended_at timestamptz(3),
    -- 结束前面的条件表达式、循环或数据库程序块。
    end_reason varchar(24),
    -- eligible_until 保存当前日结束或当前周结束，与待生效时区边界取较早值；时区调整需同步缩短开放段截止时间；类型为带时区的时间点；必须填写。
    eligible_until timestamptz(3) NOT NULL,
    -- time_zone 保存本段开始时使用的时区；IANA 有效性由应用校验，历史查询按查询时有效时区切日；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    time_zone varchar(64) NOT NULL CHECK (time_zone ~ '[^[:space:]]'),
    -- heartbeat_timeout_seconds 保存本段的超时秒数快照，默认 90；配置变化不追溯改变已有段；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    heartbeat_timeout_seconds integer NOT NULL DEFAULT 90 CHECK (heartbeat_timeout_seconds > 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护；已结束时间段不对外开放编辑；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_execution_segment_sequence UNIQUE (execution_id, segment_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_segment_end_pair CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：(ended_at IS NULL AND end_reason IS NULL)。
        (ended_at IS NULL AND end_reason IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_segment_end_reason CHECK (
        -- 结束前面的条件表达式、循环或数据库程序块。
        end_reason IN ('USER_PAUSE', 'AUTO_PAUSE', 'PLAN_BOUNDARY', 'AUTH_REVOKED', 'FINISH')
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_segment_time CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：isfinite(started_at) AND isfinite(eligible_until) AND eligible_until > started_at。
        isfinite(started_at) AND isfinite(eligible_until) AND eligible_until > started_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (ended_at IS NULL OR (isfinite(ended_at) AND ended_at >= started_at AND ended_at <= eligible_until))
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_execution_segment_audit_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE execution_segment IS '每次开始或继续形成一个连续执行时间段；暂停间隔不计时，历史段不改期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.id IS '数据库生成的 BIGINT 主键；心跳携带当前时间段标识，阻止旧段心跳刷新新段';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.execution_id IS '所属执行会话逻辑标识；归属和状态一致性由 Service 校验';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.assignment_id IS '本段开始时的任务安排标识；恢复时可引用新安排，已结束段保留旧安排';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.segment_no IS '会话内连续序号，从 1 开始，由用户锁保护下的应用分配';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.started_at IS '首次开始或本次继续的服务端时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.ended_at IS '暂停或结束的有效截止时间；为空表示当前运行段';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.end_reason IS 'USER_PAUSE 主动暂停、AUTO_PAUSE 超时、PLAN_BOUNDARY 边界、AUTH_REVOKED 登录撤销、FINISH 结束';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.eligible_until IS '当前日结束或当前周结束，与待生效时区边界取较早值；时区调整需同步缩短开放段截止时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.time_zone IS '本段开始时使用的时区；IANA 有效性由应用校验，历史查询按查询时有效时区切日';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.heartbeat_timeout_seconds IS '本段的超时秒数快照，默认 90；配置变化不追溯改变已有段';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.created_at IS '创建时间，毫秒精度';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN execution_segment.updated_at IS '更新时间，由应用维护；已结束时间段不对外开放编辑';

-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_execution_segment_open
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON execution_segment (execution_id) WHERE ended_at IS NULL;。
    ON execution_segment (execution_id) WHERE ended_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_execution_segment_open_deadline
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON execution_segment (eligible_until, execution_id) WHERE ended_at IS NULL;。
    ON execution_segment (eligible_until, execution_id) WHERE ended_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_execution_segment_assignment
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON execution_segment (assignment_id, execution_id);。
    ON execution_segment (assignment_id, execution_id);

-- 跨表约束由后续 Service 事务保证：先锁 identity_user，再检查会话与时间段。
-- RUNNING 恰有一个开放段；PAUSED/FINISHED 无开放段；段不重叠、首段与会话首次开始一致。
-- 任务、父链、安排及用户关系校验不放进普通 CHECK，不用触发器代替 Service。
-- 耗时按段求和，开放段封顶到 min(服务端当前时间, 最后心跳+超时快照, eligible_until)。
-- 本文件没有写入任何执行示例、账号、默认密码或其他业务初始化数据。
-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
