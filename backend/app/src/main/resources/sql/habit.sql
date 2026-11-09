-- 定义习惯表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway habit 模块，PostgreSQL 17.11 手工初始化。
-- 独立习惯：分类、主体、规则生命周期修订、实际打卡四表；无每日占位表或结算表。
-- 无物理外键、触发器、级联或自动迁移；跨表归属、日历封存和动作资格由 Service 事务保证。
-- 一日一次有效打卡，不补录、不提前打卡；时间点由服务端生成，本地日期使用账号有效时区。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 habit_category 表；用户自己的习惯分类；删除仅停用分类，不删除习惯，也不改写历史分类归属。
CREATE TABLE habit_category (
    -- id 保存数据库生成的 BIGINT 标识，对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，必须来自认证上下文；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- name 保存分类名称，1～50 字，去除首尾空格后同用户有效分类不重名；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(50) NOT NULL CHECK (name ~ '[^[:space:]]' AND name = btrim(name)),
    -- sort_order 保存显示顺序，非负整数，相同值按主键排序；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    -- version 保存乐观锁版本，实际修改时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间点，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间点，由应用维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deleted_at 保存逻辑删除时间；已引用的分类继续作为历史标识存在；类型为带时区的时间点。
    deleted_at timestamptz(3),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_category_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at >= created_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE habit_category IS '用户自己的习惯分类；删除仅停用分类，不删除习惯，也不改写历史分类归属';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.id IS '数据库生成的 BIGINT 标识，对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.user_id IS '所属用户，必须来自认证上下文';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.name IS '分类名称，1～50 字，去除首尾空格后同用户有效分类不重名';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.sort_order IS '显示顺序，非负整数，相同值按主键排序';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.version IS '乐观锁版本，实际修改时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.created_at IS '创建时间点，毫秒精度';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.updated_at IS '更新时间点，由应用维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_category.deleted_at IS '逻辑删除时间；已引用的分类继续作为历史标识存在';
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_habit_category_user_name_active
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON habit_category (user_id, lower(name COLLATE "C")) WHERE deleted_at IS NULL;。
    ON habit_category (user_id, lower(name COLLATE "C")) WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_category_user_order ON habit_category (user_id, sort_order, id) WHERE deleted_at IS NULL;

-- 创建 habit 表；独立习惯主体；ENDED 即结束归档，PAUSED 可恢复，ENDED 须重新开启，逻辑删除不恢复。
CREATE TABLE habit (
    -- id 保存数据库生成的习惯标识；不关联目标、阶段或任务；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属普通用户；跨表同用户及管理身份限制由 Service 保证；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- status 保存ACTIVE 进行、PAUSED 暂停、ENDED 结束归档；不能与逻辑删除混为一谈；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PAUSED', 'ENDED')),
    -- current_revision_no 保存当前完整快照的修订号，从 1 递增；与对应修订同事务维护；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    current_revision_no bigint NOT NULL DEFAULT 1 CHECK (current_revision_no > 0),
    -- 结束前面的条件表达式、循环或数据库程序块。
    ended_at timestamptz(3),
    -- history_locked_through 保存已封存到的本地日期，只能前进；时区回拨不能重开或修改已封存日期；类型为日历日期；保存时检查后面的限制。
    history_locked_through date CHECK (isfinite(history_locked_through)),
    -- version 保存乐观锁版本，实际写操作时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间点；每日应做从首个修订生效日开始判定；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间点，由应用维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deleted_at 保存逻辑删除时间；保留主体、修订和打卡事实，不允许继续打卡或撤销；类型为带时区的时间点。
    deleted_at timestamptz(3),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_end_pair CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'ENDED' AND ended_at IS NOT NULL) OR (status <> 'ENDED' AND ended_at IS NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (ended_at IS NULL OR (isfinite(ended_at) AND ended_at >= created_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at >= created_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE habit IS '独立习惯主体；ENDED 即结束归档，PAUSED 可恢复，ENDED 须重新开启，逻辑删除不恢复';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.id IS '数据库生成的习惯标识；不关联目标、阶段或任务';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.user_id IS '所属普通用户；跨表同用户及管理身份限制由 Service 保证';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.status IS 'ACTIVE 进行、PAUSED 暂停、ENDED 结束归档；不能与逻辑删除混为一谈';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.current_revision_no IS '当前完整快照的修订号，从 1 递增；与对应修订同事务维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.ended_at IS '本次结束时间，仅 ENDED 有值；重新开启后清空，旧结束仍在修订表';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.history_locked_through IS '已封存到的本地日期，只能前进；时区回拨不能重开或修改已封存日期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.version IS '乐观锁版本，实际写操作时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.created_at IS '创建时间点；每日应做从首个修订生效日开始判定';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.updated_at IS '更新时间点，由应用维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit.deleted_at IS '逻辑删除时间；保留主体、修订和打卡事实，不允许继续打卡或撤销';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_user_page ON habit (user_id, status, created_at DESC, id DESC) WHERE deleted_at IS NULL;

-- 创建 habit_revision 表；规则和生命周期的完整追加快照；同日可多次修订，过去不回写，无变化不新增。
CREATE TABLE habit_revision (
    -- id 保存数据库生成的修订标识，打卡引用本标识；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，与习惯及分类同用户由事务校验；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- habit_id 保存所属习惯逻辑标识，删除后保留；类型为大整数；必须填写，保存时检查后面的限制。
    habit_id bigint NOT NULL CHECK (habit_id > 0),
    -- revision_no 保存同习惯内从 1 连续递增，唯一；不重叠及连续性由用户锁内维护；类型为大整数；必须填写，保存时检查后面的限制。
    revision_no bigint NOT NULL CHECK (revision_no > 0),
    -- action_type 保存创建、资料规则变更、暂停、恢复、结束、重新开启、删除或系统暂停；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    action_type varchar(24) NOT NULL CHECK (action_type IN (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'CREATED', 'EDITED', 'PAUSED', 'RESUMED', 'ENDED', 'REOPENED', 'DELETED', 'SYSTEM_PAUSED'。
        'CREATED', 'EDITED', 'PAUSED', 'RESUMED', 'ENDED', 'REOPENED', 'DELETED', 'SYSTEM_PAUSED'
    )),
    -- name 保存本修订生效时的完整名称快照；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]'),
    -- description 保存描述快照，未填写为空字符串；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值。
    description text NOT NULL DEFAULT '',
    -- completion_criteria 保存完成标准快照，描述性内容，不增加每日次数或计量门槛；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值。
    completion_criteria text NOT NULL DEFAULT '',
    -- category_id 保存可空的分类逻辑标识；分类删除不改写该引用，未分类使用 NULL；类型为大整数；保存时检查后面的限制。
    category_id bigint CHECK (category_id > 0),
    -- cycle_type 保存DAILY 每天；WEEKLY_DAYS 指定星期，不引入每周次数或独立星期表；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    cycle_type varchar(16) NOT NULL CHECK (cycle_type IN ('DAILY', 'WEEKLY_DAYS')),
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：weekday_mask smallint NOT NULL CHECK (weekday_mask BETWEEN 1 AND 127),。
    weekday_mask smallint NOT NULL CHECK (weekday_mask BETWEEN 1 AND 127),
    -- start_date 保存本地开始日期；创建缺省由 Service 取今天，不使用数据库 CURRENT_DATE；类型为日历日期；必须填写，保存时检查后面的限制。
    start_date date NOT NULL CHECK (isfinite(start_date)),
    -- expected_time 保存可空的本地 HH:mm，仅提示；不触发自动提醒、超时或补卡；类型为时刻。
    expected_time time(0),
    -- status_snapshot 保存本次动作后的主体状态快照；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    status_snapshot varchar(16) NOT NULL CHECK (status_snapshot IN ('ACTIVE', 'PAUSED', 'ENDED')),
    -- deleted_snapshot 保存本次动作后的逻辑删除标志；保留历史应做和完成事实；类型为真假值；必须填写，未传值时使用这里的默认值。
    deleted_snapshot boolean NOT NULL DEFAULT false,
    -- effective_date 保存按当时有效账号时区计算的生效日；只允许今天，非递减且不得改写封存日由 Service 保证；类型为日历日期；必须填写，保存时检查后面的限制。
    effective_date date NOT NULL CHECK (isfinite(effective_date)),
    -- changed_at 保存本次真实动作的服务端时间点；类型为带时区的时间点；必须填写，保存时检查后面的限制。
    changed_at timestamptz(3) NOT NULL CHECK (isfinite(changed_at)),
    -- time_zone 保存动作时生效的 IANA 时区，用于解释历史本地日期；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    time_zone varchar(64) NOT NULL CHECK (time_zone ~ '[^[:space:]]' AND time_zone = btrim(time_zone)),
    -- created_at 保存快照入库时间；业务数据库角色只授予 SELECT、INSERT，不开放 UPDATE、DELETE；类型为带时区的时间点；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    created_at timestamptz(3) NOT NULL DEFAULT clock_timestamp() CHECK (isfinite(created_at)),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_habit_revision_number UNIQUE (habit_id, revision_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_revision_cycle CHECK (cycle_type <> 'DAILY' OR weekday_mask = 127),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_revision_expected_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        expected_time IS NULL OR (expected_time < time '24:00' AND EXTRACT(SECOND FROM expected_time) = 0)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_revision_action CHECK ((
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (action_type = 'CREATED' AND revision_no = 1 AND status_snapshot = 'ACTIVE' AND NOT deleted_snapshot)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (action_type = 'EDITED' AND revision_no > 1 AND NOT deleted_snapshot)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (action_type IN ('PAUSED', 'SYSTEM_PAUSED') AND revision_no > 1 AND status_snapshot = 'PAUSED' AND NOT deleted_snapshot)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (action_type IN ('RESUMED', 'REOPENED') AND revision_no > 1 AND status_snapshot = 'ACTIVE' AND NOT deleted_snapshot)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (action_type = 'ENDED' AND revision_no > 1 AND status_snapshot = 'ENDED' AND NOT deleted_snapshot)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (action_type = 'DELETED' AND revision_no > 1 AND deleted_snapshot)
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) IS TRUE)。
    ) IS TRUE)
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE habit_revision IS '规则和生命周期的完整追加快照；同日可多次修订，过去不回写，无变化不新增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.id IS '数据库生成的修订标识，打卡引用本标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.user_id IS '所属用户，与习惯及分类同用户由事务校验';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.habit_id IS '所属习惯逻辑标识，删除后保留';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.revision_no IS '同习惯内从 1 连续递增，唯一；不重叠及连续性由用户锁内维护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.action_type IS '创建、资料规则变更、暂停、恢复、结束、重新开启、删除或系统暂停';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.name IS '本修订生效时的完整名称快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.description IS '描述快照，未填写为空字符串';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.completion_criteria IS '完成标准快照，描述性内容，不增加每日次数或计量门槛';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.category_id IS '可空的分类逻辑标识；分类删除不改写该引用，未分类使用 NULL';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.cycle_type IS 'DAILY 每天；WEEKLY_DAYS 指定星期，不引入每周次数或独立星期表';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.weekday_mask IS '周一为最低位，周日为第 6 位；DAILY 固定 127，接口仍使用 1～7 数组';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.start_date IS '本地开始日期；创建缺省由 Service 取今天，不使用数据库 CURRENT_DATE';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.expected_time IS '可空的本地 HH:mm，仅提示；不触发自动提醒、超时或补卡';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.status_snapshot IS '本次动作后的主体状态快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.deleted_snapshot IS '本次动作后的逻辑删除标志；保留历史应做和完成事实';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.effective_date IS '按当时有效账号时区计算的生效日；只允许今天，非递减且不得改写封存日由 Service 保证';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.changed_at IS '本次真实动作的服务端时间点';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.time_zone IS '动作时生效的 IANA 时区，用于解释历史本地日期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_revision.created_at IS '快照入库时间；业务数据库角色只授予 SELECT、INSERT，不开放 UPDATE、DELETE';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_revision_user_effective
    -- 继续列出本次使用的字段：习惯编号（habit_id）。
    ON habit_revision (user_id, habit_id, effective_date DESC, revision_no DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_revision_user_category ON habit_revision (user_id, category_id, effective_date, habit_id);

-- 创建 habit_checkin 表；实际打卡事实；撤销保留原行，重新打卡新增行，同习惯同日期仅一条有效记录。
CREATE TABLE habit_checkin (
    -- id 保存数据库生成的打卡标识，对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存所属用户，不能信任请求中的用户标识；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- habit_id 保存所属习惯逻辑标识，习惯删除后仍参与历史统计；类型为大整数；必须填写，保存时检查后面的限制。
    habit_id bigint NOT NULL CHECK (habit_id > 0),
    -- revision_id 保存打卡时的完整修订；已完成的 1/1 及分类始终归该修订，不随之后修改重算；类型为大整数；必须填写，保存时检查后面的限制。
    revision_id bigint NOT NULL CHECK (revision_id > 0),
    -- business_date 保存实际打卡的本地日期，历史永久保留；只允许当前未封存日期，不允许客户端补录；类型为日历日期；必须填写，保存时检查后面的限制。
    business_date date NOT NULL CHECK (isfinite(business_date)),
    -- time_zone 保存打卡时有效时区，时区变更不挪动原 business_date；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    time_zone varchar(64) NOT NULL CHECK (time_zone ~ '[^[:space:]]' AND time_zone = btrim(time_zone)),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checked_at timestamptz(3) NOT NULL CHECK (isfinite(checked_at)),
    -- cancelled_at 保存撤销服务端时间；只允许当天，暂停或结束后仍可撤销，删除后拒绝；类型为带时区的时间点。
    cancelled_at timestamptz(3),
    -- version 保存撤销等并发控制版本，实际写操作时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存入库时间点，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间点，撤销时维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_habit_checkin_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (cancelled_at IS NULL OR (isfinite(cancelled_at) AND cancelled_at >= checked_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE habit_checkin IS '实际打卡事实；撤销保留原行，重新打卡新增行，同习惯同日期仅一条有效记录';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.id IS '数据库生成的打卡标识，对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.user_id IS '所属用户，不能信任请求中的用户标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.habit_id IS '所属习惯逻辑标识，习惯删除后仍参与历史统计';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.revision_id IS '打卡时的完整修订；已完成的 1/1 及分类始终归该修订，不随之后修改重算';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.business_date IS '实际打卡的本地日期，历史永久保留；只允许当前未封存日期，不允许客户端补录';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.time_zone IS '打卡时有效时区，时区变更不挪动原 business_date';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.checked_at IS '打卡服务端时间；重复打卡直接返回有效行，不覆盖该时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.cancelled_at IS '撤销服务端时间；只允许当天，暂停或结束后仍可撤销，删除后拒绝';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.version IS '撤销等并发控制版本，实际写操作时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.created_at IS '入库时间点，毫秒精度';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN habit_checkin.updated_at IS '更新时间点，撤销时维护';
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_habit_checkin_valid_date
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON habit_checkin (habit_id, business_date) WHERE cancelled_at IS NULL;。
    ON habit_checkin (habit_id, business_date) WHERE cancelled_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_checkin_user_date ON habit_checkin (user_id, business_date, habit_id, id) WHERE cancelled_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_checkin_user_timeline ON habit_checkin (user_id, habit_id, checked_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_habit_checkin_revision ON habit_checkin (revision_id, id);

-- 主体及当前修订、有效分类归属、当天动作、封存日、时区生效、状态转换及乐观锁由同用户行锁内的 Service 保证。
-- 原始事实不保存累计数、完成率、每日应做占位行；详见 statistics.sql 的逐日口径查询原型。
-- 应用角色不得 UPDATE/DELETE/TRUNCATE habit_revision；此处不创建数据库账号或更改现有角色权限。
-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
