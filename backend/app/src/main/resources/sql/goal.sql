-- 定义目标表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway goal 模块，PostgreSQL 17.11 手工初始化。
-- 一业务模块一份 SQL；仅创建新表，已有同名对象时失败，不覆盖已有业务数据。
-- 无物理外键、触发器、级联、自动迁移；跨表关系由应用事务校验。
-- 日期是用户本地日历日期，timestamptz 是时间点；不使用服务器 CURRENT_DATE 推断用户日期。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 goal 表；个人成长目标；新业务只归档不删除，旧逻辑删除行保留，进度由有效任务权重聚合计算。
CREATE TABLE goal (
    -- id 保存主键，数据库生成的 BIGINT 标识；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，必须取当前认证身份；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- name 保存名称，1～100 字且不能仅为空白；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]'),
    -- description 保存描述，未填写时为空字符串；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值。
    description text NOT NULL DEFAULT '',
    -- completion_criteria 保存完成条件，未填写时为空字符串；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值。
    completion_criteria text NOT NULL DEFAULT '',
    -- start_date 保存计划开始日期，可空；允许未来日期；类型为日历日期；保存时检查后面的限制。
    start_date date CHECK (isfinite(start_date)),
    -- expected_end_date 保存预计结束日期，可空，不作为强制截止；类型为日历日期；保存时检查后面的限制。
    expected_end_date date CHECK (isfinite(expected_end_date)),
    -- status 保存状态：未开始、进行中、已完成；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    status varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    -- archived_at 保存归档时间，NULL 表示未归档；类型为带时区的时间点。
    archived_at timestamptz,
    -- version 保存乐观锁版本，由应用更新时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，按时间点存储；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用更新时维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deleted_at 保存逻辑删除时间，NULL 表示未删除；类型为带时区的时间点。
    deleted_at timestamptz,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_goal_dates CHECK (start_date IS NULL OR expected_end_date IS NULL OR expected_end_date >= start_date)
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE goal IS '个人成长目标；新业务只归档不删除，旧逻辑删除行保留，进度由有效任务权重聚合计算';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.id IS '主键，数据库生成的 BIGINT 标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.user_id IS '所属用户，必须取当前认证身份';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.name IS '名称，1～100 字且不能仅为空白';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.description IS '描述，未填写时为空字符串';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.completion_criteria IS '完成条件，未填写时为空字符串';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.start_date IS '计划开始日期，可空；允许未来日期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.expected_end_date IS '预计结束日期，可空，不作为强制截止';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.status IS '状态：未开始、进行中、已完成';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.archived_at IS '归档时间，NULL 表示未归档';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.version IS '乐观锁版本，由应用更新时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.created_at IS '创建时间，按时间点存储';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.updated_at IS '更新时间，由应用更新时维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.deleted_at IS '逻辑删除时间，NULL 表示未删除';

-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_goal_user_active_page ON goal (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL AND archived_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_goal_user_state_start ON goal (user_id, status, start_date, id) WHERE deleted_at IS NULL AND archived_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_goal_user_archive_page ON goal (user_id, archived_at DESC, id DESC) WHERE deleted_at IS NULL AND archived_at IS NOT NULL;

-- BEGIN GOAL CONDITION UPGRADE
-- 新库随本文件执行；已有库只选取本标记块和下方新历史表块，手工事务内补齐。
-- 不计算或覆盖当前进度，条件缓存只控制达标通知的轮次。
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE goal ADD COLUMN IF NOT EXISTS completion_condition_met boolean NOT NULL DEFAULT false;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE goal ADD COLUMN IF NOT EXISTS completion_round bigint NOT NULL DEFAULT 0;
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $goal_condition_upgrade$
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF (SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='goal'
        -- 再增加一个必须同时满足的筛选条件。
        AND ((column_name='completion_condition_met' AND data_type='boolean' AND is_nullable='NO' AND column_default='false')
          -- 提供另一个可满足的分支条件，任一分支成立即可。
          OR (column_name='completion_round' AND data_type='bigint' AND is_nullable='NO' AND column_default IN ('0','0::bigint')))) <> 2 THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Goal completion condition columns have an incompatible definition';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='goal'::regclass AND conname='ck_goal_completion_condition'
        -- 再增加一个必须同时满足的筛选条件。
        AND (contype<>'c' OR NOT convalidated)) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Goal completion condition constraint has an incompatible definition';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid='goal'::regclass AND conname='ck_goal_completion_condition') THEN
        -- 调整指定表的结构或约束，具体变更按后续子句执行。
        ALTER TABLE goal ADD CONSTRAINT ck_goal_completion_condition CHECK (
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            completion_round >= 0 AND (NOT completion_condition_met OR
                -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
                (completion_round > 0 AND status <> 'COMPLETED' AND archived_at IS NULL AND deleted_at IS NULL))
        );
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$goal_condition_upgrade$;。
$goal_condition_upgrade$;
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.completion_condition_met IS '当前未归档未完成目标是否已达标；精确条件及同事务维护由 Service 保证，不保存进度百分比';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal.completion_round IS '达标轮次，从 0 开始；未达标转达标时加一，同轮通知唯一，回落不清零';
-- END GOAL CONDITION UPGRADE

-- BEGIN GOAL STATE HISTORY
-- 创建 goal_state_history 表；目标状态与归档恢复的追加式事实；只记实际变化，BASELINE 不计完成，历史不可由业务接口修改删除。
CREATE TABLE goal_state_history (
    -- id 保存数据库生成的 BIGINT 标识，对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，写入与查询均由服务端限定本人；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- goal_id 保存所属业务对象逻辑标识；逻辑删除后保留引用；类型为大整数；必须填写，保存时检查后面的限制。
    goal_id bigint NOT NULL CHECK (goal_id > 0),
    -- event_type 保存真实业务事件类型；BASELINE 为上线时当前状态，COMPLETION_IMPORTED 仅用于导入可信任务完成时间；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    event_type varchar(24) NOT NULL CHECK (event_type IN ('CREATED', 'STATUS_CHANGED', 'ARCHIVED', 'RESTORED', 'BASELINE')),
    -- before_status 保存变化前状态；创建、初始基线和可信完成导入时为空；类型为有长度限制的文字；保存时检查后面的限制。
    before_status varchar(20) CHECK (before_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    -- after_status 保存变化后状态，使用当前业务对象既有枚举；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    after_status varchar(20) NOT NULL CHECK (after_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')),
    -- before_archived 保存归档变化前标志；创建或基线时为空；类型为真假值。
    before_archived boolean,
    -- after_archived 保存事件发生后的归档状态，不以当前 archived_at 倒推历史；类型为真假值；必须填写。
    after_archived boolean NOT NULL,
    -- name_snapshot 保存该事件发生时的名称；不是整份业务资料快照；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name_snapshot varchar(100) NOT NULL CHECK (name_snapshot ~ '[^[:space:]]'),
    -- entity_version 保存该事件对应的主体版本，允许无状态变化的资料修改留下版本间隔；类型为大整数；必须填写，保存时检查后面的限制。
    entity_version bigint NOT NULL CHECK (entity_version >= 0),
    -- occurred_at 保存实际发生时间，或旧任务已知的 completed_at；基线采用初始化时间；类型为带时区的时间点；必须填写，保存时检查后面的限制。
    occurred_at timestamptz(3) NOT NULL CHECK (isfinite(occurred_at)),
    -- source_type 保存由服务端填写：用户动作、通知确认、执行开始或系统初始化；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    source_type varchar(24) NOT NULL DEFAULT 'USER_ACTION' CHECK (source_type IN ('USER_ACTION', 'NOTIFICATION', 'SYSTEM_INIT')),
    -- created_at 保存记录入库时间，导入旧完成事实时可以晚于 occurred_at；类型为带时区的时间点；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    created_at timestamptz(3) NOT NULL DEFAULT clock_timestamp() CHECK (isfinite(created_at)),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_goal_state_history_event UNIQUE (goal_id, entity_version, event_type),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_goal_state_history_shape CHECK ((
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (event_type = 'CREATED' AND before_status IS NULL AND after_status = 'NOT_STARTED'
            -- 再增加一个必须同时满足的筛选条件。
            AND before_archived IS NULL AND NOT after_archived)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (event_type = 'BASELINE' AND before_status IS NULL AND before_archived IS NULL AND source_type = 'SYSTEM_INIT')
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (event_type = 'STATUS_CHANGED' AND before_status IS NOT NULL AND before_status <> after_status
            -- 再增加一个必须同时满足的筛选条件。
            AND before_archived IS NOT NULL AND before_archived = after_archived)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (event_type = 'ARCHIVED' AND before_status IS NOT NULL AND before_status = after_status
            -- 再增加一个必须同时满足的筛选条件。
            AND before_archived = false AND after_archived = true)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (event_type = 'RESTORED' AND before_status IS NOT NULL AND before_status = after_status
            -- 再增加一个必须同时满足的筛选条件。
            AND before_archived = true AND after_archived = false)
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) IS TRUE)。
    ) IS TRUE)
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE goal_state_history IS '目标状态与归档恢复的追加式事实；只记实际变化，BASELINE 不计完成，历史不可由业务接口修改删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.id IS '数据库生成的 BIGINT 标识，对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.user_id IS '所属用户，写入与查询均由服务端限定本人';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.goal_id IS '所属业务对象逻辑标识；逻辑删除后保留引用';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.event_type IS '真实业务事件类型；BASELINE 为上线时当前状态，COMPLETION_IMPORTED 仅用于导入可信任务完成时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.before_status IS '变化前状态；创建、初始基线和可信完成导入时为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.after_status IS '变化后状态，使用当前业务对象既有枚举';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.before_archived IS '归档变化前标志；创建或基线时为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.after_archived IS '事件发生后的归档状态，不以当前 archived_at 倒推历史';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.name_snapshot IS '该事件发生时的名称；不是整份业务资料快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.entity_version IS '该事件对应的主体版本，允许无状态变化的资料修改留下版本间隔';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.occurred_at IS '实际发生时间，或旧任务已知的 completed_at；基线采用初始化时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.source_type IS '由服务端填写：用户动作、通知确认、执行开始或系统初始化';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN goal_state_history.created_at IS '记录入库时间，导入旧完成事实时可以晚于 occurred_at';

-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_goal_state_history_baseline ON goal_state_history (goal_id) WHERE event_type = 'BASELINE';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_goal_state_history_user_time ON goal_state_history (user_id, occurred_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_goal_state_history_object ON goal_state_history (user_id, goal_id, occurred_at DESC, id DESC);
-- END GOAL STATE HISTORY

-- BEGIN REMAINING GOAL HISTORY BOOTSTRAP
-- 后端验收后显式开启 remaining_backend_ready 与 remaining_history_bootstrap，默认不回填。
-- 手工维护事务内先锁定相关用户，先基线后开放新后端；已有任何历史的对象都不补基线。
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $goal_history_bootstrap$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- approved 保存approved；类型为真假值。
    approved boolean := COALESCE(NULLIF(current_setting('asterway.remaining_history_bootstrap', true), ''), 'false')::boolean;
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_backend_ready', true), ''), 'false')::boolean;
    -- initialized_at 保存initializedat；类型为带时区的时间点。
    initialized_at timestamptz(3) := clock_timestamp();
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF approved AND NOT backend_ready THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Remaining fact initialization requires the approved backend';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF approved THEN
        -- 向 goal_state_history 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO goal_state_history(user_id,goal_id,event_type,after_status,after_archived,name_snapshot,entity_version,occurred_at,source_type,created_at)
        -- 读取后面列出的字段或计算结果。
        SELECT g.user_id,g.id,'BASELINE',g.status,(g.archived_at IS NOT NULL),g.name,g.version,initialized_at,'SYSTEM_INIT',initialized_at
        -- 指定本次查询主要读取的表或子查询。
        FROM goal g WHERE NOT EXISTS (SELECT 1 FROM goal_state_history h WHERE h.goal_id=g.id)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT DO NOTHING;
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$goal_history_bootstrap$;。
$goal_history_bootstrap$;
-- END REMAINING GOAL HISTORY BOOTSTRAP

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
