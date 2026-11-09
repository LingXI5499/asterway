-- 定义规划表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway planning 模块，PostgreSQL 17.11 手工初始化。
-- 一业务模块一份 SQL；仅创建新表，已有同名对象时失败，不覆盖已有业务数据。
-- 无物理外键、触发器、级联、自动迁移；跨表关系由应用事务校验。
-- 日期是用户本地日历日期，timestamptz 是时间点；不使用服务器 CURRENT_DATE 推断用户日期。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 weekly_plan 表；用户维度的自然周计划，可汇集不同目标和阶段的任务。
CREATE TABLE weekly_plan (
    -- id 保存主键，数据库生成的 BIGINT 标识；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，必须取当前认证身份；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- week_start_date 保存自然周周一日期，周日由该日期加 6 天计算；类型为日历日期；必须填写，保存时检查后面的限制。
    week_start_date date NOT NULL CHECK (isfinite(week_start_date) AND EXTRACT(ISODOW FROM week_start_date) = 1),
    -- version 保存乐观锁版本，由应用更新时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，按时间点存储；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用更新时维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deleted_at 保存逻辑删除时间，NULL 表示未删除；类型为带时区的时间点。
    deleted_at timestamptz
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE weekly_plan IS '用户维度的自然周计划，可汇集不同目标和阶段的任务';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.id IS '主键，数据库生成的 BIGINT 标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.user_id IS '所属用户，必须取当前认证身份';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.week_start_date IS '自然周周一日期，周日由该日期加 6 天计算';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.version IS '乐观锁版本，由应用更新时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.created_at IS '创建时间，按时间点存储';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.updated_at IS '更新时间，由应用更新时维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN weekly_plan.deleted_at IS '逻辑删除时间，NULL 表示未删除';

-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_weekly_plan_user_week_active ON weekly_plan (user_id, week_start_date) WHERE deleted_at IS NULL;

-- 创建 daily_plan 表；周计划内按需创建的日计划；日期归属与同用户关系由应用事务校验。
CREATE TABLE daily_plan (
    -- id 保存主键，数据库生成的 BIGINT 标识；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，必须取当前认证身份；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- weekly_plan_id 保存所属周计划逻辑标识，必须是本人未删除的周计划；类型为大整数；必须填写，保存时检查后面的限制。
    weekly_plan_id bigint NOT NULL CHECK (weekly_plan_id > 0),
    -- plan_date 保存用户本地计划日期，必须位于关联周一至周日内；类型为日历日期；必须填写，保存时检查后面的限制。
    plan_date date NOT NULL CHECK (isfinite(plan_date)),
    -- version 保存乐观锁版本，由应用更新时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，按时间点存储；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用更新时维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deleted_at 保存逻辑删除时间，NULL 表示未删除；类型为带时区的时间点。
    deleted_at timestamptz
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE daily_plan IS '周计划内按需创建的日计划；日期归属与同用户关系由应用事务校验';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.id IS '主键，数据库生成的 BIGINT 标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.user_id IS '所属用户，必须取当前认证身份';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.weekly_plan_id IS '所属周计划逻辑标识，必须是本人未删除的周计划';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.plan_date IS '用户本地计划日期，必须位于关联周一至周日内';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.version IS '乐观锁版本，由应用更新时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.created_at IS '创建时间，按时间点存储';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.updated_at IS '更新时间，由应用更新时维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN daily_plan.deleted_at IS '逻辑删除时间，NULL 表示未删除';

-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_daily_plan_user_date_active ON daily_plan (user_id, plan_date) WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_daily_plan_user_week_date ON daily_plan (user_id, weekly_plan_id, plan_date, id) WHERE deleted_at IS NULL;

-- 创建 task_assignment 表；任务当前安排与安排历史；ended_at 为 NULL 表示当前安排，不使用逻辑删除。
CREATE TABLE task_assignment (
    -- id 保存主键，数据库生成的 BIGINT 标识；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，必须取当前认证身份；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- task_id 保存逻辑关联本人任务；历史任务标识永久保留；类型为大整数；必须填写，保存时检查后面的限制。
    task_id bigint NOT NULL CHECK (task_id > 0),
    -- weekly_plan_id 保存安排所属周计划，不关联单一目标；类型为大整数；必须填写，保存时检查后面的限制。
    weekly_plan_id bigint NOT NULL CHECK (weekly_plan_id > 0),
    -- daily_plan_id 保存可空；为空表示只安排到周，非空时须属于同一周计划；类型为大整数；保存时检查后面的限制。
    daily_plan_id bigint CHECK (daily_plan_id > 0),
    -- sort_order 保存显示排序，数值越小越靠前；相同值按主键排序；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    -- assigned_at 保存本次安排建立时间；不是未来的执行日期；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    assigned_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 结束前面的条件表达式、循环或数据库程序块。
    ended_at timestamptz,
    -- 结束前面的条件表达式、循环或数据库程序块。
    end_reason varchar(30),
    -- version 保存乐观锁版本，由应用更新时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，按时间点存储；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用更新时维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_task_assignment_end_pair CHECK ((ended_at IS NULL AND end_reason IS NULL) OR (ended_at IS NOT NULL AND end_reason IS NOT NULL)),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_task_assignment_end_time CHECK (ended_at IS NULL OR ended_at >= assigned_at),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_task_assignment_end_reason CHECK (end_reason IN ('RESCHEDULED', 'RETURNED_TO_POOL', 'TASK_DELETED', 'GOAL_ARCHIVED', 'DAILY_PLAN_DELETED', 'WEEKLY_PLAN_DELETED'))
);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE task_assignment IS '任务当前安排与安排历史；ended_at 为 NULL 表示当前安排，不使用逻辑删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.id IS '主键，数据库生成的 BIGINT 标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.user_id IS '所属用户，必须取当前认证身份';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.task_id IS '逻辑关联本人任务；历史任务标识永久保留';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.weekly_plan_id IS '安排所属周计划，不关联单一目标';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.daily_plan_id IS '可空；为空表示只安排到周，非空时须属于同一周计划';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.sort_order IS '显示排序，数值越小越靠前；相同值按主键排序';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.assigned_at IS '本次安排建立时间；不是未来的执行日期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.ended_at IS '本次安排结束时间，NULL 表示当前安排';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.end_reason IS '结束原因，与 ended_at 同时填写；历史安排不得覆盖改期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.version IS '乐观锁版本，由应用更新时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.created_at IS '创建时间，按时间点存储';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN task_assignment.updated_at IS '更新时间，由应用更新时维护';

-- 对 task_id 全局唯一，而非 (user_id, task_id)，避免错误归属产生第二条当前安排。
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_task_assignment_task_current ON task_assignment (task_id) WHERE ended_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_task_assignment_user_week_current ON task_assignment (user_id, weekly_plan_id, sort_order, id) WHERE ended_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_task_assignment_user_day_current ON task_assignment (user_id, daily_plan_id, sort_order, id) WHERE ended_at IS NULL AND daily_plan_id IS NOT NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_task_assignment_user_task_history ON task_assignment (user_id, task_id, assigned_at DESC, id DESC);

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
