-- 定义账户管理表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway 账户与 RBAC，全新 PostgreSQL 17.11 初始化。
-- 在专用开发库或测试库中执行。已有对象会导致事务失败，不覆盖或清理旧对象。
-- 无物理外键、级联、触发器和迁移版本表。关联有效性和授权边界由应用事务校验。
-- 超级管理员由本地初始化步骤单独建立，不在本文件保存默认密码。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 identity_user 表；账户主体；用户名全历史大小写无关唯一；Argon2id 密码；时区状态由应用校验。
CREATE TABLE identity_user (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- username 保存登录名；大小写无关且全历史唯一，保留展示形式；类型为有长度限制的文字；必须填写。
    username varchar(30) NOT NULL,
    -- password_hash 保存Argon2id 编码摘要；数据库只做格式前缀检查，完整哈希与密码规则由应用验证；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    password_hash varchar(255) NOT NULL CHECK (password_hash LIKE '$argon2id$%'),
    -- nickname 保存显示昵称，可重复，注册未填时由应用取用户名；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    nickname varchar(30) NOT NULL CHECK (char_length(nickname) BETWEEN 1 AND 30 AND nickname ~ '[^[:space:]]'),
    -- is_enabled 保存是否启用；不等同于逻辑删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_enabled boolean NOT NULL DEFAULT true,
    -- must_change_password 保存初始或重置密码后要求修改密码；类型为真假值；必须填写，未传值时使用这里的默认值。
    must_change_password boolean NOT NULL DEFAULT false,
    -- auth_version 保存安全版本；密码与安全状态变化时递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    auth_version bigint NOT NULL DEFAULT 0 CHECK (auth_version >= 0),
    -- time_zone 保存有效 IANA 时区；应用通过可用时区列表验证；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    time_zone varchar(64) NOT NULL CHECK (btrim(time_zone) <> ''),
    -- pending_time_zone 保存待生效时区；没有待切换时为空；类型为有长度限制的文字。
    pending_time_zone varchar(64),
    -- time_zone_effective_at 保存待切换时刻，与待生效时区同时设置或清空；类型为带时区的时间点。
    time_zone_effective_at timestamptz,
    -- version 保存乐观锁版本；应用更新时比较并递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存最近修改时刻；由应用写入，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- updated_by 保存最近修改者账户标识；初始化时可空；无物理外键；类型为大整数。
    updated_by bigint,
    -- deleted_at 保存逻辑删除时刻；为空表示未删除；类型为带时区的时间点。
    deleted_at timestamptz,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_identity_user_username CHECK (char_length(username) BETWEEN 1 AND 30 AND
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：username COLLATE "C" ~ U&'^[A-Za-z0-9\3400-\4DBF\4E00-\9FFF\F900-\FAFF\+020000-\+02FA1F]+$'),。
        username COLLATE "C" ~ U&'^[A-Za-z0-9\3400-\4DBF\4E00-\9FFF\F900-\FAFF\+020000-\+02FA1F]+$'),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_identity_user_pending_timezone CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：(pending_time_zone IS NULL AND time_zone_effective_at IS NULL) OR。
        (pending_time_zone IS NULL AND time_zone_effective_at IS NULL) OR
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        (pending_time_zone IS NOT NULL AND btrim(pending_time_zone) <> '' AND
         -- 继续补齐上面的 SQL 表达式、字段值或约束组合：pending_time_zone <> time_zone AND time_zone_effective_at IS NOT NULL)),。
         pending_time_zone <> time_zone AND time_zone_effective_at IS NOT NULL)),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);
-- 全历史唯一，逻辑删除后不复用用户名。
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_identity_user_username ON identity_user (lower(username COLLATE "C"));
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_user_active_page ON identity_user (created_at DESC, id DESC) WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_user_pending_timezone ON identity_user (time_zone_effective_at, id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE deleted_at IS NULL AND pending_time_zone IS NOT NULL;

-- 创建 identity_role 表；内置三角色；结构可扩展，本轮不开放自定义角色新增删除。
CREATE TABLE identity_role (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- code 保存全历史唯一业务编码；内置编码由应用保护；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    code varchar(64) NOT NULL UNIQUE CHECK (code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
    -- name 保存界面展示名称；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    -- description 保存用途说明；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    description varchar(255) NOT NULL DEFAULT '',
    -- is_builtin 保存内置对象标志；核心对象不能由管理接口删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_builtin boolean NOT NULL DEFAULT false,
    -- is_enabled 保存是否启用；不等同于逻辑删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_enabled boolean NOT NULL DEFAULT true,
    -- version 保存乐观锁版本；应用更新时比较并递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存最近修改时刻；由应用写入，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- updated_by 保存最近修改者账户标识；初始化时可空；无物理外键；类型为大整数。
    updated_by bigint,
    -- deleted_at 保存逻辑删除时刻；为空表示未删除；类型为带时区的时间点。
    deleted_at timestamptz,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (NOT is_builtin OR code IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);

-- 创建 identity_permission 表；服务端动作权限；新增目录行不会自动创建接口能力。
CREATE TABLE identity_permission (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- code 保存全历史唯一业务编码；内置编码由应用保护；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    code varchar(100) NOT NULL UNIQUE CHECK (code ~ '^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$'),
    -- name 保存界面展示名称；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    -- description 保存用途说明；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    description varchar(255) NOT NULL DEFAULT '',
    -- is_builtin 保存内置对象标志；核心对象不能由管理接口删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_builtin boolean NOT NULL DEFAULT false,
    -- is_enabled 保存是否启用；不等同于逻辑删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_enabled boolean NOT NULL DEFAULT true,
    -- version 保存乐观锁版本；应用更新时比较并递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存最近修改时刻；由应用写入，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- updated_by 保存最近修改者账户标识；初始化时可空；无物理外键；类型为大整数。
    updated_by bigint,
    -- deleted_at 保存逻辑删除时刻；为空表示未删除；类型为带时区的时间点。
    deleted_at timestamptz,

    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);

-- 创建 identity_menu 表；目录、页面与按钮；菜单可见性不代替接口授权；父链由应用校验。
CREATE TABLE identity_menu (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- code 保存全历史唯一业务编码；内置编码由应用保护；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    code varchar(64) NOT NULL UNIQUE CHECK (code ~ '^[a-z][a-z0-9_]{1,63}$'),
    -- parent_id 保存父菜单逻辑标识；根节点为空；更长循环及悬空父节点由应用拒绝；类型为大整数；保存时检查后面的限制。
    parent_id bigint CHECK (parent_id > 0 AND parent_id <> id),
    -- name 保存界面展示名称；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    -- menu_type 保存DIRECTORY 目录、PAGE 页面、BUTTON 按钮；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    menu_type varchar(16) NOT NULL CHECK (menu_type IN ('DIRECTORY', 'PAGE', 'BUTTON')),
    -- route_path 保存目录或页面路径，必须以斜杠开头；按钮为空；类型为有长度限制的文字。
    route_path varchar(255),
    -- component_key 保存页面组件白名单键，不存任意文件路径；类型为有长度限制的文字。
    component_key varchar(100),
    -- permission_id 保存逻辑引用动作权限，关联有效性由应用检查；类型为大整数；保存时检查后面的限制。
    permission_id bigint CHECK (permission_id > 0),
    -- icon 保存图标键，由前端白名单解释；类型为有长度限制的文字。
    icon varchar(64),
    -- sort_order 保存同级非负排序值；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    -- is_visible 保存导航展示状态，不代替接口授权；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_visible boolean NOT NULL DEFAULT true,
    -- is_builtin 保存内置对象标志；核心对象不能由管理接口删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_builtin boolean NOT NULL DEFAULT false,
    -- is_enabled 保存是否启用；不等同于逻辑删除；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_enabled boolean NOT NULL DEFAULT true,
    -- version 保存乐观锁版本；应用更新时比较并递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存最近修改时刻；由应用写入，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- updated_by 保存最近修改者账户标识；初始化时可空；无物理外键；类型为大整数。
    updated_by bigint,
    -- deleted_at 保存逻辑删除时刻；为空表示未删除；类型为带时区的时间点。
    deleted_at timestamptz,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_identity_menu_shape CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (menu_type = 'DIRECTORY' AND route_path IS NOT NULL AND route_path LIKE '/%' AND component_key IS NULL AND permission_id IS NULL) OR
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (menu_type = 'PAGE' AND route_path IS NOT NULL AND route_path LIKE '/%' AND component_key IS NOT NULL AND
         -- 继续补齐上面的 SQL 表达式、字段值或约束组合：btrim(component_key) <> '' AND permission_id IS NOT NULL) OR。
         btrim(component_key) <> '' AND permission_id IS NOT NULL) OR
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (menu_type = 'BUTTON' AND route_path IS NULL AND component_key IS NULL AND permission_id IS NOT NULL)),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_menu_parent ON identity_menu (parent_id, sort_order, id) WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_menu_permission ON identity_menu (permission_id) WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_identity_menu_route ON identity_menu (route_path) WHERE deleted_at IS NULL AND route_path IS NOT NULL;

-- 创建 identity_user_role 表；基础 USER 角色与附加管理身份，无物理外键。
CREATE TABLE identity_user_role (
    -- user_id 保存逻辑引用账户，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- role_id 保存逻辑引用角色，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    role_id bigint NOT NULL CHECK (role_id > 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：PRIMARY KEY (user_id, role_id)。
    PRIMARY KEY (user_id, role_id)
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_user_role_reverse ON identity_user_role (role_id, user_id);

-- 创建 identity_role_permission 表；有效角色动作权限，多角色取并集。
CREATE TABLE identity_role_permission (
    -- role_id 保存逻辑引用角色，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    role_id bigint NOT NULL CHECK (role_id > 0),
    -- permission_id 保存逻辑引用动作权限，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    permission_id bigint NOT NULL CHECK (permission_id > 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：PRIMARY KEY (role_id, permission_id)。
    PRIMARY KEY (role_id, permission_id)
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_role_permission_reverse ON identity_role_permission (permission_id, role_id);

-- 创建 identity_role_menu 表；角色菜单分配，祖先目录补齐但不额外授予动作权限。
CREATE TABLE identity_role_menu (
    -- role_id 保存逻辑引用角色，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    role_id bigint NOT NULL CHECK (role_id > 0),
    -- menu_id 保存逻辑引用菜单，关联有效性由应用检查；类型为大整数；必须填写，保存时检查后面的限制。
    menu_id bigint NOT NULL CHECK (menu_id > 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- created_by 保存创建者账户标识；初始化时可空；无物理外键；类型为大整数。
    created_by bigint,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：PRIMARY KEY (role_id, menu_id)。
    PRIMARY KEY (role_id, menu_id)
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_role_menu_reverse ON identity_role_menu (menu_id, role_id);

-- 创建 identity_login_log 表；追加式登录日志；未知账户 user_id 为空；不保存凭据。
CREATE TABLE identity_login_log (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存逻辑引用账户，关联有效性由应用检查；类型为大整数；保存时检查后面的限制。
    user_id bigint CHECK (user_id > 0),
    -- username_snapshot 保存尝试登录时的用户名快照；未知用户也能记录；类型为有长度限制的文字；必须填写。
    username_snapshot varchar(128) NOT NULL,
    -- outcome 保存SUCCESS 或 FAILURE；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    -- reason_code 保存结果或拒绝原因编码；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    reason_code varchar(64) NOT NULL CHECK (reason_code ~ '^[A-Z][A-Z0-9_]*$'),
    -- session_id 保存登录成功关联的 UUID 会话标识，不是 JWT；类型为唯一标识。
    session_id uuid,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ip_address inet,。
    ip_address inet,
    -- user_agent 保存截断后的客户端描述，不参与授权；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    user_agent varchar(512) NOT NULL DEFAULT '',
    -- device_label 保存设备展示描述，不参与授权；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    device_label varchar(128) NOT NULL DEFAULT '',
    -- trace_id 保存请求追踪标识，不存认证凭据；类型为有长度限制的文字；必须填写。
    trace_id varchar(64) NOT NULL,
    -- occurred_at 保存事件发生时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (outcome <> 'SUCCESS' OR (user_id IS NOT NULL AND session_id IS NOT NULL AND reason_code = 'SUCCESS'))
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_login_log_user_time ON identity_login_log (user_id, occurred_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_login_log_time ON identity_login_log (occurred_at DESC, id DESC);
-- 创建 identity_operation_log 表；追加式操作审计；变更摘要由应用按白名单脱敏。
CREATE TABLE identity_operation_log (
    -- id 保存数据库自增主键；对外作为字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- actor_id 保存操作者账户标识，匿名失败场景可空；类型为大整数；保存时检查后面的限制。
    actor_id bigint CHECK (actor_id > 0),
    -- actor_username 保存操作时用户名快照；类型为有长度限制的文字。
    actor_username varchar(30),
    -- target_user_id 保存被管理账户标识，用于管理员日志范围筛选；类型为大整数；保存时检查后面的限制。
    target_user_id bigint CHECK (target_user_id > 0),
    -- resource_type 保存被操作资源类别；类型为有长度限制的文字；必须填写。
    resource_type varchar(64) NOT NULL,
    -- resource_id 保存被操作资源标识快照；类型为有长度限制的文字。
    resource_id varchar(100),
    -- action_code 保存动作权限或审计动作编码；类型为有长度限制的文字；必须填写。
    action_code varchar(100) NOT NULL,
    -- outcome 保存SUCCESS 或 FAILURE；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    -- reason_code 保存结果或拒绝原因编码；类型为有长度限制的文字。
    reason_code varchar(64),
    -- change_summary 保存仅安全字段的 JSON 对象，应用负责拒绝密码、JWT 和验证码答案；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    change_summary jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(change_summary) = 'object'),
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ip_address inet,。
    ip_address inet,
    -- trace_id 保存请求追踪标识，不存认证凭据；类型为有长度限制的文字；必须填写。
    trace_id varchar(64) NOT NULL,
    -- occurred_at 保存事件发生时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_operation_log_actor_time ON identity_operation_log (actor_id, occurred_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_operation_log_target_time ON identity_operation_log (target_user_id, occurred_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_identity_operation_log_time ON identity_operation_log (occurred_at DESC, id DESC);
-- 创建 identity_setting 表；仅站点名称、公开注册开关，保存后影响后续请求。
CREATE TABLE identity_setting (
    -- setting_key 保存仅 site_name 与 registration_enabled，不允许任意扩展配置键；类型为有长度限制的文字；用于唯一定位记录。
    setting_key varchar(64) PRIMARY KEY,
    -- setting_value 保存站点名称为 JSON 字符串，注册开关为 JSON 布尔值；类型为可按字段查询的 JSON；必须填写。
    setting_value jsonb NOT NULL,
    -- version 保存乐观锁版本；应用更新时比较并递增；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时刻；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存最近修改时刻；由应用写入，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_by 保存最近修改者账户标识；初始化时可空；无物理外键；类型为大整数。
    updated_by bigint,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_identity_setting_value CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (setting_key = 'registration_enabled' AND jsonb_typeof(setting_value) = 'boolean') OR
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (setting_key = 'site_name' AND jsonb_typeof(setting_value) = 'string' AND
         -- 继续补齐上面的 SQL 表达式、字段值或约束组合：char_length(setting_value #>> '{}') BETWEEN 1 AND 30 AND (setting_value #>> '{}') ~ '[^[:space:]]'))。
         char_length(setting_value #>> '{}') BETWEEN 1 AND 30 AND (setting_value #>> '{}') ~ '[^[:space:]]'))
);
-- 向 identity_role 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_role (code, name, description, is_builtin) VALUES
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('USER', '用户', '所有账户的基础角色，仅操作本人资源', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('ADMIN', '管理员', '附加管理身份，仅管理不含管理角色的普通用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('SUPER_ADMIN', '超级管理员', '本地初始化，负责授权与系统配置', true);

-- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_permission (code, name, is_builtin) VALUES
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:profile:read', '查看本人资料', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:profile:update', '修改本人资料', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:password:update', '修改本人密码', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:timezone:read', '查看本人时区', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:timezone:update', '修改本人时区', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:device:list', '查看本人登录设备', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:device:revoke', '退出本人登录设备', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:count', '查看用户数量', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:list', '查询用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:read', '查看用户详情', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:create', '新增普通用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:update', '修改用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:delete', '逻辑删除用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:status', '启停用户', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:reset_password', '重置用户密码', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:user:assign_role', '分配角色（仅超管）', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:role:list', '查询角色', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:role:read', '查看角色授权', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:role:update', '修改角色展示信息', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:role:grant_permission', '分配角色权限', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:role:grant_menu', '分配角色菜单', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:permission:list', '查询权限', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:permission:read', '查看权限详情', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:permission:create', '新增扩展权限', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:permission:update', '修改权限', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:permission:delete', '删除未引用的扩展权限', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:menu:list', '查询菜单树', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:menu:read', '查看菜单详情', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:menu:create', '新增菜单', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:menu:update', '修改菜单', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:menu:delete', '删除未引用的扩展菜单', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:session:list', '查询管理范围内会话', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:session:revoke', '强退管理范围内会话', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:login_log:list', '查询管理范围内登录日志', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:operation_log:list', '查询管理范围内操作日志', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:setting:read', '读取账户配置', true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('identity:setting:update', '修改账户配置', true);

-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu (code, name, menu_type, route_path, sort_order, is_builtin) VALUES
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('account_root', '账户中心', 'DIRECTORY', '/account', 10, true),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('system_root', '系统管理', 'DIRECTORY', '/system', 20, true);

-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu (code, parent_id, name, menu_type, route_path, component_key, permission_id, sort_order, is_builtin)
-- 读取后面列出的字段或计算结果。
SELECT v.code, root.id, v.name, 'PAGE', v.path, v.component, p.id, v.order_no, true
-- 指定本次查询主要读取的表或子查询。
FROM (VALUES
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('profile', 'account_root', '个人资料', '/account/profile', 'account/profile', 'identity:profile:read', 10),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('devices', 'account_root', '登录设备', '/account/devices', 'account/devices', 'identity:device:list', 20),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('users', 'system_root', '用户管理', '/system/users', 'system/users', 'identity:user:list', 30),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('roles', 'system_root', '角色授权', '/system/roles', 'system/roles', 'identity:role:list', 40),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('permissions', 'system_root', '权限管理', '/system/permissions', 'system/permissions', 'identity:permission:list', 50),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('menus', 'system_root', '菜单管理', '/system/menus', 'system/menus', 'identity:menu:list', 60),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('sessions', 'system_root', '登录会话', '/system/sessions', 'system/sessions', 'identity:session:list', 70),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('login_logs', 'system_root', '登录日志', '/system/login-logs', 'system/login_logs', 'identity:login_log:list', 80),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('operation_logs', 'system_root', '操作日志', '/system/operation-logs', 'system/operation_logs', 'identity:operation_log:list', 90),
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('account_settings', 'system_root', '账户配置', '/system/account-settings', 'system/account_settings', 'identity:setting:read', 100)
-- 继续列出本次使用的字段：根编码（root_code）、名称（name）、路径（path）、component、权限编码（permission_code）。
) AS v(code, root_code, name, path, component, permission_code, order_no)
-- 按这里的关联条件连接另一张表的记录。
JOIN identity_menu root ON root.code = v.root_code
-- 按这里的关联条件连接另一张表的记录。
JOIN identity_permission p ON p.code = v.permission_code;

-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu (code, parent_id, name, menu_type, permission_id, sort_order, is_builtin)
-- 读取后面列出的字段或计算结果。
SELECT replace(p.code, ':', '_'), m.id, p.name, 'BUTTON', p.id,
       -- 继续列出本次使用的字段：成立（true）。
       row_number() OVER (PARTITION BY m.id ORDER BY p.id)::integer, true
-- 指定本次查询主要读取的表或子查询。
FROM identity_permission p JOIN identity_menu m ON m.code = CASE split_part(p.code, ':', 2)
    -- 当前条件成立时，采用 THEN 后给出的值。
    WHEN 'profile' THEN 'profile' WHEN 'password' THEN 'profile' WHEN 'timezone' THEN 'profile'
    -- 当前条件成立时，采用 THEN 后给出的值。
    WHEN 'device' THEN 'devices' WHEN 'user' THEN 'users' WHEN 'role' THEN 'roles'
    -- 当前条件成立时，采用 THEN 后给出的值。
    WHEN 'permission' THEN 'permissions' WHEN 'menu' THEN 'menus' WHEN 'session' THEN 'sessions'
    -- 当前条件成立时，采用 THEN 后给出的值。
    WHEN 'login_log' THEN 'login_logs' WHEN 'operation_log' THEN 'operation_logs' WHEN 'setting' THEN 'account_settings' END
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE p.id NOT IN (SELECT permission_id FROM identity_menu WHERE permission_id IS NOT NULL);
-- 向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_role_permission (role_id, permission_id)
-- 读取后面列出的字段或计算结果。
SELECT r.id, p.id FROM identity_role r CROSS JOIN identity_permission p
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE r.code = 'SUPER_ADMIN'
   -- 提供另一个可满足的分支条件，任一分支成立即可。
   OR (r.code = 'USER' AND split_part(p.code, ':', 2) IN ('profile', 'password', 'timezone', 'device'))
   -- 提供另一个可满足的分支条件，任一分支成立即可。
   OR (r.code = 'ADMIN' AND
       -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
       (p.code LIKE 'identity:user:%' AND p.code <> 'identity:user:assign_role'
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR split_part(p.code, ':', 2) IN ('session', 'login_log', 'operation_log')));
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH RECURSIVE permitted(role_id, menu_id, parent_id) AS (
    -- 读取后面列出的字段或计算结果。
    SELECT rp.role_id, m.id, m.parent_id FROM identity_role_permission rp
    -- 按这里的关联条件连接另一张表的记录。
    JOIN identity_menu m ON m.permission_id = rp.permission_id
    -- 合并前后两段查询结果，去掉重复行。
    UNION
    -- 读取后面列出的字段或计算结果。
    SELECT c.role_id, parent.id, parent.parent_id FROM permitted c JOIN identity_menu parent ON parent.id = c.parent_id
)
-- 向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_role_menu (role_id, menu_id) SELECT DISTINCT role_id, menu_id FROM permitted;
-- 向 identity_setting 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_setting (setting_key, setting_value) VALUES
    -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
    ('site_name', '"Asterway"'::jsonb), ('registration_enabled', 'true'::jsonb);

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_user IS '账户主体；用户名全历史大小写无关唯一；Argon2id 密码；时区状态由应用校验';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_role IS '内置三角色；结构可扩展，本轮不开放自定义角色新增删除';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_permission IS '服务端动作权限；新增目录行不会自动创建接口能力';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_menu IS '目录、页面与按钮；菜单可见性不代替接口授权；父链由应用校验';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_user_role IS '基础 USER 角色与附加管理身份，无物理外键';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_role_permission IS '有效角色动作权限，多角色取并集';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_role_menu IS '角色菜单分配，祖先目录补齐但不额外授予动作权限';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_login_log IS '追加式登录日志；未知账户 user_id 为空；不保存凭据';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_operation_log IS '追加式操作审计；变更摘要由应用按白名单脱敏';

-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE identity_setting IS '仅站点名称、公开注册开关，保存后影响后续请求';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.auth_version IS '密码或安全状态变化时递增，旧会话安全版本不匹配即失效';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.version IS '乐观锁版本；更新必须比较旧版本并递增';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.time_zone IS '应用验证 IANA 时区；不以服务器默认值代替用户选择';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.time_zone_effective_at IS '待生效时区的切换时刻，与 pending_time_zone 同时设置或清空';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.session_id IS '只存会话标识，不保存 JWT';

-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.change_summary IS '仅允许安全字段白名单，不写密码、JWT 或验证码答案';

-- 字段职责说明。
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.username IS '登录名；大小写无关且全历史唯一，保留展示形式';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.password_hash IS 'Argon2id 编码摘要；数据库只做格式前缀检查，完整哈希与密码规则由应用验证';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.nickname IS '显示昵称，可重复，注册未填时由应用取用户名';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.is_enabled IS '是否启用；不等同于逻辑删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.must_change_password IS '初始或重置密码后要求修改密码';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.auth_version IS '安全版本；密码与安全状态变化时递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.time_zone IS '有效 IANA 时区；应用通过可用时区列表验证';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.pending_time_zone IS '待生效时区；没有待切换时为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.time_zone_effective_at IS '待切换时刻，与待生效时区同时设置或清空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.version IS '乐观锁版本；应用更新时比较并递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user.deleted_at IS '逻辑删除时刻；为空表示未删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.code IS '全历史唯一业务编码；内置编码由应用保护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.name IS '界面展示名称';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.description IS '用途说明';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.is_enabled IS '是否启用；不等同于逻辑删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.version IS '乐观锁版本；应用更新时比较并递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role.deleted_at IS '逻辑删除时刻；为空表示未删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.code IS '全历史唯一业务编码；内置编码由应用保护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.name IS '界面展示名称';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.description IS '用途说明';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.is_enabled IS '是否启用；不等同于逻辑删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.version IS '乐观锁版本；应用更新时比较并递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_permission.deleted_at IS '逻辑删除时刻；为空表示未删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.code IS '全历史唯一业务编码；内置编码由应用保护';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.parent_id IS '父菜单逻辑标识；根节点为空；更长循环及悬空父节点由应用拒绝';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.name IS '界面展示名称';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.menu_type IS 'DIRECTORY 目录、PAGE 页面、BUTTON 按钮';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.route_path IS '目录或页面路径，必须以斜杠开头；按钮为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.component_key IS '页面组件白名单键，不存任意文件路径';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.permission_id IS '逻辑引用动作权限，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.icon IS '图标键，由前端白名单解释';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.sort_order IS '同级非负排序值';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.is_visible IS '导航展示状态，不代替接口授权';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.is_enabled IS '是否启用；不等同于逻辑删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.version IS '乐观锁版本；应用更新时比较并递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_menu.deleted_at IS '逻辑删除时刻；为空表示未删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user_role.user_id IS '逻辑引用账户，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user_role.role_id IS '逻辑引用角色，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user_role.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_user_role.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_permission.role_id IS '逻辑引用角色，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_permission.permission_id IS '逻辑引用动作权限，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_permission.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_permission.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_menu.role_id IS '逻辑引用角色，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_menu.menu_id IS '逻辑引用菜单，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_menu.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_role_menu.created_by IS '创建者账户标识；初始化时可空；无物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.user_id IS '逻辑引用账户，关联有效性由应用检查';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.username_snapshot IS '尝试登录时的用户名快照；未知用户也能记录';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.outcome IS 'SUCCESS 或 FAILURE';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.reason_code IS '结果或拒绝原因编码';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.session_id IS '登录成功关联的 UUID 会话标识，不是 JWT';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.ip_address IS '请求来源 IP；代理来源必须经过可信配置';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.user_agent IS '截断后的客户端描述，不参与授权';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.device_label IS '设备展示描述，不参与授权';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.trace_id IS '请求追踪标识，不存认证凭据';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_login_log.occurred_at IS '事件发生时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.id IS '数据库自增主键；对外作为字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.actor_id IS '操作者账户标识，匿名失败场景可空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.actor_username IS '操作时用户名快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.target_user_id IS '被管理账户标识，用于管理员日志范围筛选';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.resource_type IS '被操作资源类别';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.resource_id IS '被操作资源标识快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.action_code IS '动作权限或审计动作编码';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.outcome IS 'SUCCESS 或 FAILURE';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.reason_code IS '结果或拒绝原因编码';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.change_summary IS '仅安全字段的 JSON 对象，应用负责拒绝密码、JWT 和验证码答案';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.ip_address IS '请求来源 IP；代理来源必须经过可信配置';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.trace_id IS '请求追踪标识，不存认证凭据';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_operation_log.occurred_at IS '事件发生时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.setting_key IS '仅 site_name 与 registration_enabled，不允许任意扩展配置键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.setting_value IS '站点名称为 JSON 字符串，注册开关为 JSON 布尔值';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.version IS '乐观锁版本；应用更新时比较并递增';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.created_at IS '创建时刻';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN identity_setting.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';

-- BEGIN GROWTH AUTHORIZATION
-- 核心成长权限初始化，可从现有库手工独立提取执行，不重建 Identity 表。
-- 仅给 USER；管理身份即使同时持有 USER，也不能使用成长业务。
-- 读取后面列出的字段或计算结果；FOR UPDATE 会同时锁住对应记录。
SELECT id FROM identity_role WHERE code='SUPER_ADMIN' FOR UPDATE;
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $$ BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF (SELECT count(*) FROM identity_role WHERE code IN ('USER','ADMIN','SUPER_ADMIN') AND is_builtin AND is_enabled AND deleted_at IS NULL) <> 3 THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'The three active built-in roles are required';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END $$;
-- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled) VALUES
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:list','目标列表','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:read','目标详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:create','目标创建','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:update','目标修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:status','目标状态修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:delete','目标删除','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:archive','目标归档','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('goal:goal:restore','目标恢复','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:list','阶段列表','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:read','阶段详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:create','阶段创建','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:update','阶段修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:status','阶段状态修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:delete','阶段删除','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('stage:stage:sort','阶段排序','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:list','任务列表','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:read','任务详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:create','任务创建','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:update','任务修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:status','任务状态修改','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:delete','任务删除','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('task:task:sort','任务排序','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:weekly:list','周计划列表','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:weekly:read','周计划详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:weekly:manage','周计划管理','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:daily:list','日计划列表','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:daily:read','日计划详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:daily:manage','日计划管理','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:assignment:read','任务安排详情','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:assignment:manage','任务安排管理','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:assignment:remove','任务安排移出计划','当前用户本人的成长业务',true,true),
 -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
 ('planning:pending:list','待处理任务列表','当前用户本人的成长业务',true,true)
-- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
ON CONFLICT (code) DO NOTHING;
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $$ BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code IN ('goal:goal:list','goal:goal:read','goal:goal:create','goal:goal:update','goal:goal:status','goal:goal:delete','goal:goal:archive','goal:goal:restore','stage:stage:list','stage:stage:read','stage:stage:create','stage:stage:update','stage:stage:status','stage:stage:delete','stage:stage:sort','task:task:list','task:task:read','task:task:create','task:task:update','task:task:status','task:task:delete','task:task:sort','planning:weekly:list','planning:weekly:read','planning:weekly:manage','planning:daily:list','planning:daily:read','planning:daily:manage','planning:assignment:read','planning:assignment:manage','planning:assignment:remove','planning:pending:list') AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Growth permission code conflicts with an existing custom or deleted permission';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END $$;
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH inserted AS (
 -- 向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
 INSERT INTO identity_role_permission(role_id,permission_id)
 -- 读取后面列出的字段或计算结果。
 SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
 -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
 WHERE r.code = 'USER' AND p.code IN ('goal:goal:list','goal:goal:read','goal:goal:create','goal:goal:update','goal:goal:status','goal:goal:delete','goal:goal:archive','goal:goal:restore','stage:stage:list','stage:stage:read','stage:stage:create','stage:stage:update','stage:stage:status','stage:stage:delete','stage:stage:sort','task:task:list','task:task:read','task:task:create','task:task:update','task:task:status','task:task:delete','task:task:sort','planning:weekly:list','planning:weekly:read','planning:weekly:manage','planning:daily:list','planning:daily:read','planning:daily:manage','planning:assignment:read','planning:assignment:manage','planning:assignment:remove','planning:pending:list')
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT DO NOTHING RETURNING role_id
)
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);
-- 注册普通用户的成长菜单；页面显示由本标记块末尾的显式启用语句维护。
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,name,menu_type,route_path,sort_order,is_visible,is_builtin,is_enabled)
-- 按 INSERT 声明的字段顺序提供要写入的值。
VALUES ('growth_root','成长规划','DIRECTORY','/growth',20,false,true,true) ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goals',m.id,'目标','PAGE','/growth/goals','growth/goals',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='goal:goal:list'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stages',m.id,'阶段','PAGE','/growth/stages','growth/stages',p.id,20,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='stage:stage:list'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_tasks',m.id,'任务','PAGE','/growth/tasks','growth/tasks',p.id,30,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='task:task:list'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_weekly',m.id,'周计划','PAGE','/planning/weekly-plans','planning/weekly',p.id,40,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:weekly:read'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_daily',m.id,'日计划','PAGE','/planning/daily-plans','planning/daily',p.id,50,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:daily:read'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_pending',m.id,'待处理任务','PAGE','/planning/pending-tasks','planning/pending',p.id,60,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:pending:list'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_create',m.id,'目标创建','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:create'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_update',m.id,'目标修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:update'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_status',m.id,'目标状态修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:status'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_delete',m.id,'目标删除','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:delete'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_archive',m.id,'目标归档','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:archive'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_goal_goal_restore',m.id,'目标恢复','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:restore'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stage_stage_create',m.id,'阶段创建','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:create'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stage_stage_update',m.id,'阶段修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:update'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stage_stage_status',m.id,'阶段状态修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:status'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stage_stage_delete',m.id,'阶段删除','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:delete'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_stage_stage_sort',m.id,'阶段排序','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:sort'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_task_task_create',m.id,'任务创建','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:create'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_task_task_update',m.id,'任务修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:update'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_task_task_status',m.id,'任务状态修改','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:status'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_task_task_delete',m.id,'任务删除','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:delete'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_task_task_sort',m.id,'任务排序','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:sort'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_planning_weekly_manage',m.id,'周计划管理','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:weekly:manage'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_planning_daily_manage',m.id,'日计划管理','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_daily' AND p.code='planning:daily:manage'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_planning_assignment_manage',m.id,'任务安排管理','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:assignment:manage'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 -- 读取后面列出的字段或计算结果。
 SELECT 'growth_planning_assignment_remove',m.id,'任务安排移出计划','BUTTON',p.id,10,false,true,true
 -- 指定本次查询主要读取的表或子查询。
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:assignment:remove'
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT(code) DO NOTHING;
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $$ BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF (SELECT count(*) FROM identity_menu WHERE code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending','growth_goal_goal_create','growth_goal_goal_update','growth_goal_goal_status','growth_goal_goal_delete','growth_goal_goal_archive','growth_goal_goal_restore','growth_stage_stage_create','growth_stage_stage_update','growth_stage_stage_status','growth_stage_stage_delete','growth_stage_stage_sort','growth_task_task_create','growth_task_task_update','growth_task_task_status','growth_task_task_delete','growth_task_task_sort','growth_planning_weekly_manage','growth_planning_daily_manage','growth_planning_assignment_manage','growth_planning_assignment_remove') AND is_builtin AND deleted_at IS NULL) <> 27 THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Growth menu code conflicts with an existing custom or deleted menu';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END $$;
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH inserted AS (
 -- 向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
 INSERT INTO identity_role_menu(role_id,menu_id)
 -- 读取后面列出的字段或计算结果。
 SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
 -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
 WHERE r.code = 'USER' AND m.code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending','growth_goal_goal_create','growth_goal_goal_update','growth_goal_goal_status','growth_goal_goal_delete','growth_goal_goal_archive','growth_goal_goal_restore','growth_stage_stage_create','growth_stage_stage_update','growth_stage_stage_status','growth_stage_stage_delete','growth_stage_stage_sort','growth_task_task_create','growth_task_task_update','growth_task_task_status','growth_task_task_delete','growth_task_task_sort','growth_planning_weekly_manage','growth_planning_daily_manage','growth_planning_assignment_manage','growth_planning_assignment_remove')
 -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
 ON CONFLICT DO NOTHING RETURNING role_id
)
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);

-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH removed AS (
 -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
 DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
 -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
 WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
 -- 再增加一个必须同时满足的筛选条件。
 AND p.code ~ '^(goal|stage|task|planning):' RETURNING rp.role_id
-- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH removed AS (
 -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
 DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
 -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
 WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
 -- 再增加一个必须同时满足的筛选条件。
 AND (m.code LIKE 'growth_%' OR m.component_key LIKE 'growth/%' OR m.component_key LIKE 'planning/%' OR m.permission_id IN (SELECT id FROM identity_permission WHERE code ~ '^(goal|stage|task|planning):')) RETURNING rm.role_id
-- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
-- 周/日主页面展示当前计划，入口依赖 read，历史列表另依赖 list。
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE identity_menu m SET permission_id=p.id,version=m.version+1,updated_at=CURRENT_TIMESTAMP
-- 指定本次查询主要读取的表或子查询。
FROM identity_permission p
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE ((m.code='growth_weekly' AND p.code='planning:weekly:read') OR (m.code='growth_daily' AND p.code='planning:daily:read'))
-- 再增加一个必须同时满足的筛选条件。
AND m.is_builtin AND m.deleted_at IS NULL AND m.permission_id IS DISTINCT FROM p.id;
-- 成长页面已完成；普通用户入口启用，按钮节点仍隐藏。
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE identity_menu SET is_visible=true,version=version+1,updated_at=CURRENT_TIMESTAMP
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending')
-- 再增加一个必须同时满足的筛选条件。
AND is_builtin AND deleted_at IS NULL AND NOT is_visible;
-- END GROWTH AUTHORIZATION

-- BEGIN EXECUTION AUTHORIZATION
-- 可在已有库中只选取本标记块，放入手工事务执行，不重跑前面的建表语句。
-- 默认只登记禁用权限和隐藏禁用菜单，不修改目标旧规则，不启用尚未实现的接口。
-- 后端验收后，在同一事务 SET LOCAL asterway.execution_backend_ready = 'true'；
-- 前端验收后，再同时 SET LOCAL asterway.execution_frontend_ready = 'true'。
-- 这两个标记仅供本段手工 SQL 使用，不是应用环境变量或系统配置项。
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $execution_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.execution_backend_ready', true), ''), 'false')::boolean;
    -- frontend_ready 保存frontend已就绪；类型为真假值。
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.execution_frontend_ready', true), ''), 'false')::boolean;
    -- execution_codes 保存执行计时codes；类型为不限固定长度的文字。
    execution_codes text[] := ARRAY[
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'execution:session:list', 'execution:session:read', 'execution:session:start',。
        'execution:session:list', 'execution:session:read', 'execution:session:start',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'execution:session:pause', 'execution:session:resume', 'execution:session:finish',。
        'execution:session:pause', 'execution:session:resume', 'execution:session:finish',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'execution:session:heartbeat'。
        'execution:session:heartbeat'
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：];。
    ];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY[
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'growth_executions', 'growth_execution_read', 'growth_execution_start',。
        'growth_executions', 'growth_execution_read', 'growth_execution_start',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'growth_execution_pause', 'growth_execution_resume', 'growth_execution_finish'。
        'growth_execution_pause', 'growth_execution_resume', 'growth_execution_finish'
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：];。
    ];
    -- root_id 保存根编号；类型为大整数。
    root_id bigint;
    -- page_id 保存当前页编号；类型为大整数。
    page_id bigint;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF frontend_ready AND NOT backend_ready THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Execution frontend requires the approved backend';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready AND (to_regclass('execution_session') IS NULL OR to_regclass('execution_segment') IS NULL) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Execution tables must be initialized before backend authorization';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO root_id FROM identity_menu
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE code = 'growth_root' AND menu_type = 'DIRECTORY' AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF root_id IS NULL OR NOT EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_role WHERE code = 'USER' AND is_builtin AND deleted_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Built-in USER role and growth directory are required';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_permission WHERE code = ANY(execution_codes)
          -- 再增加一个必须同时满足的筛选条件。
          AND (NOT is_builtin OR deleted_at IS NOT NULL)
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) OR EXISTS (。
    ) OR EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_menu WHERE code = ANY(menu_codes)
          -- 再增加一个必须同时满足的筛选条件。
          AND (NOT is_builtin OR deleted_at IS NOT NULL)
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Execution code conflicts with an existing custom or deleted object';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
    INSERT INTO identity_permission(code, name, description, is_builtin, is_enabled)
    -- 读取后面列出的字段或计算结果。
    SELECT v.code, v.name, '普通用户本人的任务执行；管理身份不开放个人核心业务', true, false
    -- 指定本次查询主要读取的表或子查询。
    FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:list', '执行记录列表'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:read', '执行详情与耗时汇总'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:start', '开始任务执行'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:pause', '暂停任务执行'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:resume', '恢复任务执行'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:finish', '结束任务执行'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('execution:session:heartbeat', '执行心跳')
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) AS v(code, name)。
    ) AS v(code, name)
    -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
    ON CONFLICT(code) DO NOTHING;

    -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
    INSERT INTO identity_menu(
        -- 继续列出本次使用的字段：编码（code）、上级编号（parent_id）、名称（name）、菜单类型（menu_type）、route路径（route_path）、component键（component_key）、权限编号（permission_id）。
        code, parent_id, name, menu_type, route_path, component_key, permission_id,
        -- 继续列出本次使用的字段：排序顺序（sort_order）、是否是否显示（is_visible）、是否builtin（is_builtin）、是否是否启用（is_enabled）。
        sort_order, is_visible, is_builtin, is_enabled
    )
    -- 读取后面列出的字段或计算结果。
    SELECT 'growth_executions', root_id, '执行计时', 'PAGE', '/execution', 'execution/index', p.id,
           -- 继续列出本次使用的字段：70、不成立（false）、成立（true）、不成立（false）。
           70, false, true, false
    -- 指定本次查询主要读取的表或子查询。
    FROM identity_permission p WHERE p.code = 'execution:session:list'
    -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
    ON CONFLICT(code) DO NOTHING;
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO page_id FROM identity_menu WHERE code = 'growth_executions';

    -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
    INSERT INTO identity_menu(
        -- 继续列出本次使用的字段：编码（code）、上级编号（parent_id）、名称（name）、菜单类型（menu_type）、权限编号（permission_id）、排序顺序（sort_order）、是否是否显示（is_visible）、是否builtin（is_builtin）、是否是否启用（is_enabled）。
        code, parent_id, name, menu_type, permission_id, sort_order, is_visible, is_builtin, is_enabled
    )
    -- 读取后面列出的字段或计算结果。
    SELECT v.code, page_id, v.name, 'BUTTON', p.id, v.sort_order, false, true, false
    -- 指定本次查询主要读取的表或子查询。
    FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_execution_read', '查看执行详情', 'execution:session:read', 10),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_execution_start', '开始执行', 'execution:session:start', 20),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_execution_pause', '暂停执行', 'execution:session:pause', 30),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_execution_resume', '继续执行', 'execution:session:resume', 40),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_execution_finish', '结束执行', 'execution:session:finish', 50)
    -- 继续列出本次使用的字段：名称（name）、权限编码（permission_code）。
    ) AS v(code, name, permission_code, sort_order)
    -- 按这里的关联条件连接另一张表的记录。
    JOIN identity_permission p ON p.code = v.permission_code
    -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
    ON CONFLICT(code) DO NOTHING;

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_menu m LEFT JOIN identity_permission p ON p.id = m.permission_id
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE m.code = ANY(menu_codes) AND (p.id IS NULL OR
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            (m.code = 'growth_executions' AND (m.menu_type <> 'PAGE' OR m.parent_id IS DISTINCT FROM root_id
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR m.route_path IS DISTINCT FROM '/execution'
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR m.component_key IS DISTINCT FROM 'execution/index'
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR p.code IS DISTINCT FROM 'execution:session:list'))
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR (m.code <> 'growth_executions' AND (m.menu_type <> 'BUTTON' OR m.parent_id IS DISTINCT FROM page_id
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR p.code IS DISTINCT FROM ('execution:session:' || substring(m.code FROM length('growth_execution_') + 1))))
        )
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) OR (SELECT count(*) FROM identity_menu WHERE code = ANY(menu_codes)) <> 6 THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Existing execution menu does not match the reserved contract';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH inserted AS (
        -- 向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_role_permission(role_id, permission_id)
        -- 读取后面列出的字段或计算结果。
        SELECT r.id, p.id FROM identity_role r CROSS JOIN identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE r.code = 'USER' AND p.code = ANY(execution_codes)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE id IN (SELECT role_id FROM inserted);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH inserted AS (
        -- 向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_role_menu(role_id, menu_id)
        -- 读取后面列出的字段或计算结果。
        SELECT r.id, m.id FROM identity_role r CROSS JOIN identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE r.code = 'USER' AND m.code = ANY(menu_codes)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE id IN (SELECT role_id FROM inserted);

    -- 管理身份不能通过角色关系获得个人执行权限；后端仍须先做身份限制再做超级管理员放行。
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r, identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id = r.id AND rp.permission_id = p.id AND r.code IN ('ADMIN', 'SUPER_ADMIN')
          -- 再增加一个必须同时满足的筛选条件。
          AND p.code LIKE 'execution:%' RETURNING rp.role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r, identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id = r.id AND rm.menu_id = m.id AND r.code IN ('ADMIN', 'SUPER_ADMIN')
          -- 再增加一个必须同时满足的筛选条件。
          AND (m.code = ANY(menu_codes) OR m.component_key LIKE 'execution/%') RETURNING rm.role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled = true, version = version + 1, updated_at = CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code = ANY(execution_codes) AND NOT is_enabled RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id = rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled = true,
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                is_visible = (code = 'growth_executions' AND frontend_ready),
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                version = version + 1, updated_at = CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code = ANY(menu_codes) AND (NOT is_enabled
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR is_visible IS DISTINCT FROM (code = 'growth_executions' AND frontend_ready))
            -- 把刚写入或修改的记录中这些字段返回给调用方。
            RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id = rm.menu_id);

        -- 与目标只归档的新后端同批启用；只停用旧动作和移除授权，保留字典及历史审计。
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code = 'goal:goal:delete' AND NOT is_builtin)
           -- 提供另一个可满足的分支条件，任一分支成立即可。
           OR EXISTS (SELECT 1 FROM identity_menu WHERE code = 'growth_goal_goal_delete' AND NOT is_builtin) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Legacy goal delete code belongs to a custom object';
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled = false, version = version + 1, updated_at = CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code = 'goal:goal:delete' AND is_builtin AND is_enabled RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id = rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled = false, is_visible = false,
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                version = version + 1, updated_at = CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code = 'growth_goal_goal_delete' AND is_builtin AND (is_enabled OR is_visible) RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id = rm.menu_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH removed AS (
            -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
            DELETE FROM identity_role_permission rp USING identity_permission p
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE rp.permission_id = p.id AND p.code = 'goal:goal:delete' AND p.is_builtin RETURNING rp.role_id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH removed AS (
            -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
            DELETE FROM identity_role_menu rm USING identity_menu m
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE rm.menu_id = m.id AND m.code = 'growth_goal_goal_delete' AND m.is_builtin RETURNING rm.role_id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$execution_authorization$;。
$execution_authorization$;
-- END EXECUTION AUTHORIZATION

-- BEGIN REMAINING AUTHORIZATION
-- 新库默认只登记禁用权限和隐藏禁用菜单；已有库可单独选取本标记块在维护事务内执行。
-- 后端验收后 SET LOCAL asterway.remaining_backend_ready = 'true';
-- 前端将来验收后再同时 SET LOCAL asterway.remaining_frontend_ready = 'true'。
-- 这些为手工 SQL 的事务级开关，不是应用环境变量，不写入 application.yml。
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $remaining_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_backend_ready', true), ''), 'false')::boolean;
    -- frontend_ready 保存frontend已就绪；类型为真假值。
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_frontend_ready', true), ''), 'false')::boolean;
    -- root_id 保存根编号；类型为大整数。
    root_id bigint;
    -- parent_menu_id 保存上级菜单编号；类型为大整数。
    parent_menu_id bigint;
    -- reserved_permission_id 保存reserved权限编号；类型为大整数。
    reserved_permission_id bigint;
    -- permission_codes 保存权限codes；类型为不限固定长度的文字。
    permission_codes text[] := ARRAY[]::text[];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY[]::text[];
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：item record;。
    item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF frontend_ready AND NOT backend_ready THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Remaining frontend requires the approved backend';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready AND EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM unnest(ARRAY['habit_category','habit','habit_revision','habit_checkin','notification_message',
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'goal_state_history','stage_state_history','task_state_history']) AS required(table_name)。
            'goal_state_history','stage_state_history','task_state_history']) AS required(table_name)
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE to_regclass(required.table_name) IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Remaining tables must be initialized before backend authorization';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO root_id FROM identity_menu
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE code='growth_root' AND menu_type='DIRECTORY' AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF root_id IS NULL OR NOT EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Built-in USER role and growth directory are required';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 页面先登记，后续按钮才能引用稳定的父菜单；每条权限对应一个预留菜单节点。
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:category:list', '习惯分类列表', 'growth_habit_categories', '习惯分类', 'PAGE', NULL::text, '/habits/categories', 'habit/categories', 85),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:list', '习惯列表与今日习惯', 'growth_habits', '习惯管理', 'PAGE', NULL::text, '/habits', 'habit/index', 80),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('notification:message:list', '站内消息列表与未读数', 'growth_notifications', '站内消息', 'PAGE', NULL::text, '/notifications', 'notification/index', 90),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('dashboard:board:read', '今日看板', 'growth_dashboard', '今日看板', 'PAGE', NULL::text, '/dashboard', 'dashboard/index', 5),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('statistics:report:read', '历史统计', 'growth_statistics', '历史统计', 'PAGE', NULL::text, '/statistics', 'statistics/index', 100),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:category:create', '创建习惯分类', 'growth_habit_category_create', '创建分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 10),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:category:update', '修改习惯分类', 'growth_habit_category_update', '修改分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 20),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:category:delete', '删除习惯分类', 'growth_habit_category_delete', '删除分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 30),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:read', '查看习惯', 'growth_habit_read', '查看习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 10),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:create', '创建习惯', 'growth_habit_create', '创建习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 20),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:update', '修改习惯', 'growth_habit_update', '修改习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 30),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:pause', '暂停习惯', 'growth_habit_pause', '暂停习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 40),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:resume', '恢复习惯', 'growth_habit_resume', '恢复习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 50),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:end', '结束归档习惯', 'growth_habit_end', '结束归档', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 60),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:reopen', '重新开启习惯', 'growth_habit_reopen', '重新开启', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 70),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:delete', '删除习惯', 'growth_habit_delete', '删除习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 80),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:habit:history', '查看规则和生命周期历史', 'growth_habit_history', '规则与生命周期', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 90),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:checkin:create', '今日打卡', 'growth_habit_checkin_create', '今日打卡', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 100),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:checkin:cancel', '撤销今日打卡', 'growth_habit_checkin_cancel', '撤销今日打卡', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 110),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('habit:checkin:read', '打卡历史', 'growth_habit_checkin_read', '打卡历史', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 120),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('notification:message:read', '消息详情', 'growth_notification_read', '消息详情', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 10),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('notification:message:mark_read', '标记已读', 'growth_notification_mark_read', '标记已读', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 20),
            -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
            ('notification:message:handle', '处理达标提醒', 'growth_notification_handle', '处理提醒', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 30)
    -- 继续列出本次使用的字段：权限名称（permission_name）、菜单编码（menu_code）、菜单名称（menu_name）、菜单类型（menu_type）、上级编码（parent_code）、route路径（route_path）、component键（component_key）。
    ) AS v(permission_code,permission_name,menu_code,menu_name,menu_type,parent_code,route_path,component_key,sort_order)
    -- 继续列出本次使用的字段：LOOP。
    LOOP
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code=item.permission_code
                -- 再增加一个必须同时满足的筛选条件。
                AND (NOT is_builtin OR deleted_at IS NOT NULL))
           -- 提供另一个可满足的分支条件，任一分支成立即可。
           OR EXISTS (SELECT 1 FROM identity_menu WHERE code=item.menu_code
                -- 再增加一个必须同时满足的筛选条件。
                AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Remaining code conflicts with a custom or deleted object: %', item.permission_code;
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        permission_codes := array_append(permission_codes,item.permission_code);
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        menu_codes := array_append(menu_codes,item.menu_code);
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.permission_code,item.permission_name,'仅普通用户本人的核心业务；管理身份优先且不开放',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO reserved_permission_id FROM identity_permission WHERE code=item.permission_code;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF item.parent_code IS NULL THEN
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            parent_menu_id := root_id;
        -- 前面条件都不成立时，采用这里的备用值。
        ELSE
            -- 读取后面列出的字段或计算结果。
            SELECT id INTO parent_menu_id FROM identity_menu WHERE code=item.parent_code AND deleted_at IS NULL;
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF parent_menu_id IS NULL THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Missing remaining parent menu: %',item.parent_code;
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,
            -- 继续列出本次使用的字段：排序顺序（sort_order）、是否是否显示（is_visible）、是否builtin（is_builtin）。
            sort_order,is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.menu_code,parent_menu_id,item.menu_name,item.menu_type,item.route_path,item.component_key,
            -- 继续列出本次使用的字段：reserved权限编号（reserved_permission_id）、单条数据中的排序顺序（item.sort_order）、不成立（false）、成立（true）。
            reserved_permission_id,item.sort_order,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code=item.menu_code AND (
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type。
            m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR m.route_path IS DISTINCT FROM item.route_path OR m.component_key IS DISTINCT FROM item.component_key
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR m.permission_id IS DISTINCT FROM reserved_permission_id
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：)) THEN。
        )) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Existing remaining menu does not match the reserved contract: %',item.menu_code;
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
    -- 结束前面的条件表达式、循环或数据库程序块。
    END LOOP;

    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH inserted AS (
        -- 向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_role_permission(role_id,permission_id)
        -- 读取后面列出的字段或计算结果。
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH inserted AS (
        -- 向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_role_menu(role_id,menu_id)
        -- 读取后面列出的字段或计算结果。
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);

    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
        -- 把刚写入或修改的记录中这些字段返回给调用方。
        RETURNING rp.role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    )
    -- 按后续条件修改指定记录；没有被条件选中的记录不变。
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled=true,
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                is_visible=(menu_type='PAGE' AND (frontend_ready OR is_visible)),
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(menu_codes) AND (NOT is_enabled
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR is_visible IS DISTINCT FROM (menu_type='PAGE' AND (frontend_ready OR is_visible)))
            -- 把刚写入或修改的记录中这些字段返回给调用方。
            RETURNING id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$remaining_authorization$;。
$remaining_authorization$;
-- END REMAINING AUTHORIZATION

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
