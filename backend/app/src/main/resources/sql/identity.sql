-- Asterway 账户与 RBAC，全新 PostgreSQL 17.11 初始化。
-- 在专用开发库或测试库中执行。已有对象会导致事务失败，不覆盖或清理旧对象。
-- 无物理外键、级联、触发器和迁移版本表。关联有效性和授权边界由应用事务校验。
-- 超级管理员由本地初始化步骤单独建立，不在本文件保存默认密码。
BEGIN;

CREATE TABLE identity_user (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username varchar(30) NOT NULL,
    password_hash varchar(255) NOT NULL CHECK (password_hash LIKE '$argon2id$%'),
    nickname varchar(30) NOT NULL CHECK (char_length(nickname) BETWEEN 1 AND 30 AND nickname ~ '[^[:space:]]'),
    is_enabled boolean NOT NULL DEFAULT true,
    must_change_password boolean NOT NULL DEFAULT false,
    auth_version bigint NOT NULL DEFAULT 0 CHECK (auth_version >= 0),
    time_zone varchar(64) NOT NULL CHECK (btrim(time_zone) <> ''),
    pending_time_zone varchar(64),
    time_zone_effective_at timestamptz,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamptz,
    CONSTRAINT ck_identity_user_username CHECK (char_length(username) BETWEEN 1 AND 30 AND
        username COLLATE "C" ~ U&'^[A-Za-z0-9\3400-\4DBF\4E00-\9FFF\F900-\FAFF\+020000-\+02FA1F]+$'),
    CONSTRAINT ck_identity_user_pending_timezone CHECK (
        (pending_time_zone IS NULL AND time_zone_effective_at IS NULL) OR
        (pending_time_zone IS NOT NULL AND btrim(pending_time_zone) <> '' AND
         pending_time_zone <> time_zone AND time_zone_effective_at IS NOT NULL)),
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);
-- 全历史唯一，逻辑删除后不复用用户名。
CREATE UNIQUE INDEX uk_identity_user_username ON identity_user (lower(username COLLATE "C"));
CREATE INDEX ix_identity_user_active_page ON identity_user (created_at DESC, id DESC) WHERE deleted_at IS NULL;
CREATE INDEX ix_identity_user_pending_timezone ON identity_user (time_zone_effective_at, id)
    WHERE deleted_at IS NULL AND pending_time_zone IS NOT NULL;

CREATE TABLE identity_role (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE CHECK (code ~ '^[A-Z][A-Z0-9_]{1,63}$'),
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    description varchar(255) NOT NULL DEFAULT '',
    is_builtin boolean NOT NULL DEFAULT false,
    is_enabled boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamptz,
    CHECK (NOT is_builtin OR code IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);

CREATE TABLE identity_permission (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(100) NOT NULL UNIQUE CHECK (code ~ '^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){2,3}$'),
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    description varchar(255) NOT NULL DEFAULT '',
    is_builtin boolean NOT NULL DEFAULT false,
    is_enabled boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamptz,

    CHECK (deleted_at IS NULL OR NOT is_enabled)
);

CREATE TABLE identity_menu (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code varchar(64) NOT NULL UNIQUE CHECK (code ~ '^[a-z][a-z0-9_]{1,63}$'),
    parent_id bigint CHECK (parent_id > 0 AND parent_id <> id),
    name varchar(64) NOT NULL CHECK (name ~ '[^[:space:]]'),
    menu_type varchar(16) NOT NULL CHECK (menu_type IN ('DIRECTORY', 'PAGE', 'BUTTON')),
    route_path varchar(255),
    component_key varchar(100),
    permission_id bigint CHECK (permission_id > 0),
    icon varchar(64),
    sort_order integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0),
    is_visible boolean NOT NULL DEFAULT true,
    is_builtin boolean NOT NULL DEFAULT false,
    is_enabled boolean NOT NULL DEFAULT true,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    updated_by bigint,
    deleted_at timestamptz,
    CONSTRAINT ck_identity_menu_shape CHECK (
        (menu_type = 'DIRECTORY' AND route_path IS NOT NULL AND route_path LIKE '/%' AND component_key IS NULL AND permission_id IS NULL) OR
        (menu_type = 'PAGE' AND route_path IS NOT NULL AND route_path LIKE '/%' AND component_key IS NOT NULL AND
         btrim(component_key) <> '' AND permission_id IS NOT NULL) OR
        (menu_type = 'BUTTON' AND route_path IS NULL AND component_key IS NULL AND permission_id IS NOT NULL)),
    CHECK (deleted_at IS NULL OR NOT is_enabled)
);
CREATE INDEX ix_identity_menu_parent ON identity_menu (parent_id, sort_order, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_identity_menu_permission ON identity_menu (permission_id) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_identity_menu_route ON identity_menu (route_path) WHERE deleted_at IS NULL AND route_path IS NOT NULL;

CREATE TABLE identity_user_role (
    user_id bigint NOT NULL CHECK (user_id > 0),
    role_id bigint NOT NULL CHECK (role_id > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    PRIMARY KEY (user_id, role_id)
);
CREATE INDEX ix_identity_user_role_reverse ON identity_user_role (role_id, user_id);

CREATE TABLE identity_role_permission (
    role_id bigint NOT NULL CHECK (role_id > 0),
    permission_id bigint NOT NULL CHECK (permission_id > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX ix_identity_role_permission_reverse ON identity_role_permission (permission_id, role_id);

CREATE TABLE identity_role_menu (
    role_id bigint NOT NULL CHECK (role_id > 0),
    menu_id bigint NOT NULL CHECK (menu_id > 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by bigint,
    PRIMARY KEY (role_id, menu_id)
);
CREATE INDEX ix_identity_role_menu_reverse ON identity_role_menu (menu_id, role_id);

CREATE TABLE identity_login_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint CHECK (user_id > 0),
    username_snapshot varchar(128) NOT NULL,
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    reason_code varchar(64) NOT NULL CHECK (reason_code ~ '^[A-Z][A-Z0-9_]*$'),
    session_id uuid,
    ip_address inet,
    user_agent varchar(512) NOT NULL DEFAULT '',
    device_label varchar(128) NOT NULL DEFAULT '',
    trace_id varchar(64) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (outcome <> 'SUCCESS' OR (user_id IS NOT NULL AND session_id IS NOT NULL AND reason_code = 'SUCCESS'))
);
CREATE INDEX ix_identity_login_log_user_time ON identity_login_log (user_id, occurred_at DESC, id DESC);
CREATE INDEX ix_identity_login_log_time ON identity_login_log (occurred_at DESC, id DESC);
CREATE TABLE identity_operation_log (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id bigint CHECK (actor_id > 0),
    actor_username varchar(30),
    target_user_id bigint CHECK (target_user_id > 0),
    resource_type varchar(64) NOT NULL,
    resource_id varchar(100),
    action_code varchar(100) NOT NULL,
    outcome varchar(16) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    reason_code varchar(64),
    change_summary jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(change_summary) = 'object'),
    ip_address inet,
    trace_id varchar(64) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_identity_operation_log_actor_time ON identity_operation_log (actor_id, occurred_at DESC, id DESC);
CREATE INDEX ix_identity_operation_log_target_time ON identity_operation_log (target_user_id, occurred_at DESC, id DESC);
CREATE INDEX ix_identity_operation_log_time ON identity_operation_log (occurred_at DESC, id DESC);
CREATE TABLE identity_setting (
    setting_key varchar(64) PRIMARY KEY,
    setting_value jsonb NOT NULL,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint,
    CONSTRAINT ck_identity_setting_value CHECK (
        (setting_key = 'registration_enabled' AND jsonb_typeof(setting_value) = 'boolean') OR
        (setting_key = 'site_name' AND jsonb_typeof(setting_value) = 'string' AND
         char_length(setting_value #>> '{}') BETWEEN 1 AND 30 AND (setting_value #>> '{}') ~ '[^[:space:]]'))
);
INSERT INTO identity_role (code, name, description, is_builtin) VALUES
    ('USER', '用户', '所有账户的基础角色，仅操作本人资源', true),
    ('ADMIN', '管理员', '附加管理身份，仅管理不含管理角色的普通用户', true),
    ('SUPER_ADMIN', '超级管理员', '本地初始化，负责授权与系统配置', true);

INSERT INTO identity_permission (code, name, is_builtin) VALUES
    ('identity:profile:read', '查看本人资料', true),
    ('identity:profile:update', '修改本人资料', true),
    ('identity:password:update', '修改本人密码', true),
    ('identity:timezone:read', '查看本人时区', true),
    ('identity:timezone:update', '修改本人时区', true),
    ('identity:device:list', '查看本人登录设备', true),
    ('identity:device:revoke', '退出本人登录设备', true),
    ('identity:user:count', '查看用户数量', true),
    ('identity:user:list', '查询用户', true),
    ('identity:user:read', '查看用户详情', true),
    ('identity:user:create', '新增普通用户', true),
    ('identity:user:update', '修改用户', true),
    ('identity:user:delete', '逻辑删除用户', true),
    ('identity:user:status', '启停用户', true),
    ('identity:user:reset_password', '重置用户密码', true),
    ('identity:user:assign_role', '分配角色（仅超管）', true),
    ('identity:role:list', '查询角色', true),
    ('identity:role:read', '查看角色授权', true),
    ('identity:role:update', '修改角色展示信息', true),
    ('identity:role:grant_permission', '分配角色权限', true),
    ('identity:role:grant_menu', '分配角色菜单', true),
    ('identity:permission:list', '查询权限', true),
    ('identity:permission:read', '查看权限详情', true),
    ('identity:permission:create', '新增扩展权限', true),
    ('identity:permission:update', '修改权限', true),
    ('identity:permission:delete', '删除未引用的扩展权限', true),
    ('identity:menu:list', '查询菜单树', true),
    ('identity:menu:read', '查看菜单详情', true),
    ('identity:menu:create', '新增菜单', true),
    ('identity:menu:update', '修改菜单', true),
    ('identity:menu:delete', '删除未引用的扩展菜单', true),
    ('identity:session:list', '查询管理范围内会话', true),
    ('identity:session:revoke', '强退管理范围内会话', true),
    ('identity:login_log:list', '查询管理范围内登录日志', true),
    ('identity:operation_log:list', '查询管理范围内操作日志', true),
    ('identity:setting:read', '读取账户配置', true),
    ('identity:setting:update', '修改账户配置', true);

INSERT INTO identity_menu (code, name, menu_type, route_path, sort_order, is_builtin) VALUES
    ('account_root', '账户中心', 'DIRECTORY', '/account', 10, true),
    ('system_root', '系统管理', 'DIRECTORY', '/system', 20, true);

INSERT INTO identity_menu (code, parent_id, name, menu_type, route_path, component_key, permission_id, sort_order, is_builtin)
SELECT v.code, root.id, v.name, 'PAGE', v.path, v.component, p.id, v.order_no, true
FROM (VALUES
    ('profile', 'account_root', '个人资料', '/account/profile', 'account/profile', 'identity:profile:read', 10),
    ('devices', 'account_root', '登录设备', '/account/devices', 'account/devices', 'identity:device:list', 20),
    ('users', 'system_root', '用户管理', '/system/users', 'system/users', 'identity:user:list', 30),
    ('roles', 'system_root', '角色授权', '/system/roles', 'system/roles', 'identity:role:list', 40),
    ('permissions', 'system_root', '权限管理', '/system/permissions', 'system/permissions', 'identity:permission:list', 50),
    ('menus', 'system_root', '菜单管理', '/system/menus', 'system/menus', 'identity:menu:list', 60),
    ('sessions', 'system_root', '登录会话', '/system/sessions', 'system/sessions', 'identity:session:list', 70),
    ('login_logs', 'system_root', '登录日志', '/system/login-logs', 'system/login_logs', 'identity:login_log:list', 80),
    ('operation_logs', 'system_root', '操作日志', '/system/operation-logs', 'system/operation_logs', 'identity:operation_log:list', 90),
    ('account_settings', 'system_root', '账户配置', '/system/account-settings', 'system/account_settings', 'identity:setting:read', 100)
) AS v(code, root_code, name, path, component, permission_code, order_no)
JOIN identity_menu root ON root.code = v.root_code
JOIN identity_permission p ON p.code = v.permission_code;

INSERT INTO identity_menu (code, parent_id, name, menu_type, permission_id, sort_order, is_builtin)
SELECT replace(p.code, ':', '_'), m.id, p.name, 'BUTTON', p.id,
       row_number() OVER (PARTITION BY m.id ORDER BY p.id)::integer, true
FROM identity_permission p JOIN identity_menu m ON m.code = CASE split_part(p.code, ':', 2)
    WHEN 'profile' THEN 'profile' WHEN 'password' THEN 'profile' WHEN 'timezone' THEN 'profile'
    WHEN 'device' THEN 'devices' WHEN 'user' THEN 'users' WHEN 'role' THEN 'roles'
    WHEN 'permission' THEN 'permissions' WHEN 'menu' THEN 'menus' WHEN 'session' THEN 'sessions'
    WHEN 'login_log' THEN 'login_logs' WHEN 'operation_log' THEN 'operation_logs' WHEN 'setting' THEN 'account_settings' END
WHERE p.id NOT IN (SELECT permission_id FROM identity_menu WHERE permission_id IS NOT NULL);
INSERT INTO identity_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM identity_role r CROSS JOIN identity_permission p
WHERE r.code = 'SUPER_ADMIN'
   OR (r.code = 'USER' AND split_part(p.code, ':', 2) IN ('profile', 'password', 'timezone', 'device'))
   OR (r.code = 'ADMIN' AND
       (p.code LIKE 'identity:user:%' AND p.code <> 'identity:user:assign_role'
        OR split_part(p.code, ':', 2) IN ('session', 'login_log', 'operation_log')));
WITH RECURSIVE permitted(role_id, menu_id, parent_id) AS (
    SELECT rp.role_id, m.id, m.parent_id FROM identity_role_permission rp
    JOIN identity_menu m ON m.permission_id = rp.permission_id
    UNION
    SELECT c.role_id, parent.id, parent.parent_id FROM permitted c JOIN identity_menu parent ON parent.id = c.parent_id
)
INSERT INTO identity_role_menu (role_id, menu_id) SELECT DISTINCT role_id, menu_id FROM permitted;
INSERT INTO identity_setting (setting_key, setting_value) VALUES
    ('site_name', '"Asterway"'::jsonb), ('registration_enabled', 'true'::jsonb);

COMMENT ON TABLE identity_user IS '账户主体；用户名全历史大小写无关唯一；Argon2id 密码；时区状态由应用校验';

COMMENT ON TABLE identity_role IS '内置三角色；结构可扩展，本轮不开放自定义角色新增删除';

COMMENT ON TABLE identity_permission IS '服务端动作权限；新增目录行不会自动创建接口能力';

COMMENT ON TABLE identity_menu IS '目录、页面与按钮；菜单可见性不代替接口授权；父链由应用校验';

COMMENT ON TABLE identity_user_role IS '基础 USER 角色与附加管理身份，无物理外键';

COMMENT ON TABLE identity_role_permission IS '有效角色动作权限，多角色取并集';

COMMENT ON TABLE identity_role_menu IS '角色菜单分配，祖先目录补齐但不额外授予动作权限';

COMMENT ON TABLE identity_login_log IS '追加式登录日志；未知账户 user_id 为空；不保存凭据';

COMMENT ON TABLE identity_operation_log IS '追加式操作审计；变更摘要由应用按白名单脱敏';

COMMENT ON TABLE identity_setting IS '仅站点名称、公开注册开关，保存后影响后续请求';

COMMENT ON COLUMN identity_user.auth_version IS '密码或安全状态变化时递增，旧会话安全版本不匹配即失效';

COMMENT ON COLUMN identity_user.version IS '乐观锁版本；更新必须比较旧版本并递增';

COMMENT ON COLUMN identity_user.time_zone IS '应用验证 IANA 时区；不以服务器默认值代替用户选择';

COMMENT ON COLUMN identity_user.time_zone_effective_at IS '待生效时区的切换时刻，与 pending_time_zone 同时设置或清空';

COMMENT ON COLUMN identity_login_log.session_id IS '只存会话标识，不保存 JWT';

COMMENT ON COLUMN identity_operation_log.change_summary IS '仅允许安全字段白名单，不写密码、JWT 或验证码答案';

-- 字段职责说明。
COMMENT ON COLUMN identity_user.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_user.username IS '登录名；大小写无关且全历史唯一，保留展示形式';
COMMENT ON COLUMN identity_user.password_hash IS 'Argon2id 编码摘要；数据库只做格式前缀检查，完整哈希与密码规则由应用验证';
COMMENT ON COLUMN identity_user.nickname IS '显示昵称，可重复，注册未填时由应用取用户名';
COMMENT ON COLUMN identity_user.is_enabled IS '是否启用；不等同于逻辑删除';
COMMENT ON COLUMN identity_user.must_change_password IS '初始或重置密码后要求修改密码';
COMMENT ON COLUMN identity_user.auth_version IS '安全版本；密码与安全状态变化时递增';
COMMENT ON COLUMN identity_user.time_zone IS '有效 IANA 时区；应用通过可用时区列表验证';
COMMENT ON COLUMN identity_user.pending_time_zone IS '待生效时区；没有待切换时为空';
COMMENT ON COLUMN identity_user.time_zone_effective_at IS '待切换时刻，与待生效时区同时设置或清空';
COMMENT ON COLUMN identity_user.version IS '乐观锁版本；应用更新时比较并递增';
COMMENT ON COLUMN identity_user.created_at IS '创建时刻';
COMMENT ON COLUMN identity_user.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
COMMENT ON COLUMN identity_user.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_user.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_user.deleted_at IS '逻辑删除时刻；为空表示未删除';
COMMENT ON COLUMN identity_role.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_role.code IS '全历史唯一业务编码；内置编码由应用保护';
COMMENT ON COLUMN identity_role.name IS '界面展示名称';
COMMENT ON COLUMN identity_role.description IS '用途说明';
COMMENT ON COLUMN identity_role.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
COMMENT ON COLUMN identity_role.is_enabled IS '是否启用；不等同于逻辑删除';
COMMENT ON COLUMN identity_role.version IS '乐观锁版本；应用更新时比较并递增';
COMMENT ON COLUMN identity_role.created_at IS '创建时刻';
COMMENT ON COLUMN identity_role.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
COMMENT ON COLUMN identity_role.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_role.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_role.deleted_at IS '逻辑删除时刻；为空表示未删除';
COMMENT ON COLUMN identity_permission.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_permission.code IS '全历史唯一业务编码；内置编码由应用保护';
COMMENT ON COLUMN identity_permission.name IS '界面展示名称';
COMMENT ON COLUMN identity_permission.description IS '用途说明';
COMMENT ON COLUMN identity_permission.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
COMMENT ON COLUMN identity_permission.is_enabled IS '是否启用；不等同于逻辑删除';
COMMENT ON COLUMN identity_permission.version IS '乐观锁版本；应用更新时比较并递增';
COMMENT ON COLUMN identity_permission.created_at IS '创建时刻';
COMMENT ON COLUMN identity_permission.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
COMMENT ON COLUMN identity_permission.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_permission.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_permission.deleted_at IS '逻辑删除时刻；为空表示未删除';
COMMENT ON COLUMN identity_menu.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_menu.code IS '全历史唯一业务编码；内置编码由应用保护';
COMMENT ON COLUMN identity_menu.parent_id IS '父菜单逻辑标识；根节点为空；更长循环及悬空父节点由应用拒绝';
COMMENT ON COLUMN identity_menu.name IS '界面展示名称';
COMMENT ON COLUMN identity_menu.menu_type IS 'DIRECTORY 目录、PAGE 页面、BUTTON 按钮';
COMMENT ON COLUMN identity_menu.route_path IS '目录或页面路径，必须以斜杠开头；按钮为空';
COMMENT ON COLUMN identity_menu.component_key IS '页面组件白名单键，不存任意文件路径';
COMMENT ON COLUMN identity_menu.permission_id IS '逻辑引用动作权限，关联有效性由应用检查';
COMMENT ON COLUMN identity_menu.icon IS '图标键，由前端白名单解释';
COMMENT ON COLUMN identity_menu.sort_order IS '同级非负排序值';
COMMENT ON COLUMN identity_menu.is_visible IS '导航展示状态，不代替接口授权';
COMMENT ON COLUMN identity_menu.is_builtin IS '内置对象标志；核心对象不能由管理接口删除';
COMMENT ON COLUMN identity_menu.is_enabled IS '是否启用；不等同于逻辑删除';
COMMENT ON COLUMN identity_menu.version IS '乐观锁版本；应用更新时比较并递增';
COMMENT ON COLUMN identity_menu.created_at IS '创建时刻';
COMMENT ON COLUMN identity_menu.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
COMMENT ON COLUMN identity_menu.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_menu.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_menu.deleted_at IS '逻辑删除时刻；为空表示未删除';
COMMENT ON COLUMN identity_user_role.user_id IS '逻辑引用账户，关联有效性由应用检查';
COMMENT ON COLUMN identity_user_role.role_id IS '逻辑引用角色，关联有效性由应用检查';
COMMENT ON COLUMN identity_user_role.created_at IS '创建时刻';
COMMENT ON COLUMN identity_user_role.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_role_permission.role_id IS '逻辑引用角色，关联有效性由应用检查';
COMMENT ON COLUMN identity_role_permission.permission_id IS '逻辑引用动作权限，关联有效性由应用检查';
COMMENT ON COLUMN identity_role_permission.created_at IS '创建时刻';
COMMENT ON COLUMN identity_role_permission.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_role_menu.role_id IS '逻辑引用角色，关联有效性由应用检查';
COMMENT ON COLUMN identity_role_menu.menu_id IS '逻辑引用菜单，关联有效性由应用检查';
COMMENT ON COLUMN identity_role_menu.created_at IS '创建时刻';
COMMENT ON COLUMN identity_role_menu.created_by IS '创建者账户标识；初始化时可空；无物理外键';
COMMENT ON COLUMN identity_login_log.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_login_log.user_id IS '逻辑引用账户，关联有效性由应用检查';
COMMENT ON COLUMN identity_login_log.username_snapshot IS '尝试登录时的用户名快照；未知用户也能记录';
COMMENT ON COLUMN identity_login_log.outcome IS 'SUCCESS 或 FAILURE';
COMMENT ON COLUMN identity_login_log.reason_code IS '结果或拒绝原因编码';
COMMENT ON COLUMN identity_login_log.session_id IS '登录成功关联的 UUID 会话标识，不是 JWT';
COMMENT ON COLUMN identity_login_log.ip_address IS '请求来源 IP；代理来源必须经过可信配置';
COMMENT ON COLUMN identity_login_log.user_agent IS '截断后的客户端描述，不参与授权';
COMMENT ON COLUMN identity_login_log.device_label IS '设备展示描述，不参与授权';
COMMENT ON COLUMN identity_login_log.trace_id IS '请求追踪标识，不存认证凭据';
COMMENT ON COLUMN identity_login_log.occurred_at IS '事件发生时刻';
COMMENT ON COLUMN identity_operation_log.id IS '数据库自增主键；对外作为字符串传输';
COMMENT ON COLUMN identity_operation_log.actor_id IS '操作者账户标识，匿名失败场景可空';
COMMENT ON COLUMN identity_operation_log.actor_username IS '操作时用户名快照';
COMMENT ON COLUMN identity_operation_log.target_user_id IS '被管理账户标识，用于管理员日志范围筛选';
COMMENT ON COLUMN identity_operation_log.resource_type IS '被操作资源类别';
COMMENT ON COLUMN identity_operation_log.resource_id IS '被操作资源标识快照';
COMMENT ON COLUMN identity_operation_log.action_code IS '动作权限或审计动作编码';
COMMENT ON COLUMN identity_operation_log.outcome IS 'SUCCESS 或 FAILURE';
COMMENT ON COLUMN identity_operation_log.reason_code IS '结果或拒绝原因编码';
COMMENT ON COLUMN identity_operation_log.change_summary IS '仅安全字段的 JSON 对象，应用负责拒绝密码、JWT 和验证码答案';
COMMENT ON COLUMN identity_operation_log.ip_address IS '请求来源 IP；代理来源必须经过可信配置';
COMMENT ON COLUMN identity_operation_log.trace_id IS '请求追踪标识，不存认证凭据';
COMMENT ON COLUMN identity_operation_log.occurred_at IS '事件发生时刻';
COMMENT ON COLUMN identity_setting.setting_key IS '仅 site_name 与 registration_enabled，不允许任意扩展配置键';
COMMENT ON COLUMN identity_setting.setting_value IS '站点名称为 JSON 字符串，注册开关为 JSON 布尔值';
COMMENT ON COLUMN identity_setting.version IS '乐观锁版本；应用更新时比较并递增';
COMMENT ON COLUMN identity_setting.created_at IS '创建时刻';
COMMENT ON COLUMN identity_setting.updated_at IS '最近修改时刻；由应用写入，无自动更新时间触发器';
COMMENT ON COLUMN identity_setting.updated_by IS '最近修改者账户标识；初始化时可空；无物理外键';

-- BEGIN GROWTH AUTHORIZATION
-- 核心成长权限初始化，可从现有库手工独立提取执行，不重建 Identity 表。
-- 仅给 USER；管理身份即使同时持有 USER，也不能使用成长业务。
SELECT id FROM identity_role WHERE code='SUPER_ADMIN' FOR UPDATE;
DO $$ BEGIN
    IF (SELECT count(*) FROM identity_role WHERE code IN ('USER','ADMIN','SUPER_ADMIN') AND is_builtin AND is_enabled AND deleted_at IS NULL) <> 3 THEN
        RAISE EXCEPTION 'The three active built-in roles are required';
    END IF;
END $$;
INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled) VALUES
 ('goal:goal:list','目标列表','当前用户本人的成长业务',true,true),
 ('goal:goal:read','目标详情','当前用户本人的成长业务',true,true),
 ('goal:goal:create','目标创建','当前用户本人的成长业务',true,true),
 ('goal:goal:update','目标修改','当前用户本人的成长业务',true,true),
 ('goal:goal:status','目标状态修改','当前用户本人的成长业务',true,true),
 ('goal:goal:delete','目标删除','当前用户本人的成长业务',true,true),
 ('goal:goal:archive','目标归档','当前用户本人的成长业务',true,true),
 ('goal:goal:restore','目标恢复','当前用户本人的成长业务',true,true),
 ('stage:stage:list','阶段列表','当前用户本人的成长业务',true,true),
 ('stage:stage:read','阶段详情','当前用户本人的成长业务',true,true),
 ('stage:stage:create','阶段创建','当前用户本人的成长业务',true,true),
 ('stage:stage:update','阶段修改','当前用户本人的成长业务',true,true),
 ('stage:stage:status','阶段状态修改','当前用户本人的成长业务',true,true),
 ('stage:stage:delete','阶段删除','当前用户本人的成长业务',true,true),
 ('stage:stage:sort','阶段排序','当前用户本人的成长业务',true,true),
 ('task:task:list','任务列表','当前用户本人的成长业务',true,true),
 ('task:task:read','任务详情','当前用户本人的成长业务',true,true),
 ('task:task:create','任务创建','当前用户本人的成长业务',true,true),
 ('task:task:update','任务修改','当前用户本人的成长业务',true,true),
 ('task:task:status','任务状态修改','当前用户本人的成长业务',true,true),
 ('task:task:delete','任务删除','当前用户本人的成长业务',true,true),
 ('task:task:sort','任务排序','当前用户本人的成长业务',true,true),
 ('planning:weekly:list','周计划列表','当前用户本人的成长业务',true,true),
 ('planning:weekly:read','周计划详情','当前用户本人的成长业务',true,true),
 ('planning:weekly:manage','周计划管理','当前用户本人的成长业务',true,true),
 ('planning:daily:list','日计划列表','当前用户本人的成长业务',true,true),
 ('planning:daily:read','日计划详情','当前用户本人的成长业务',true,true),
 ('planning:daily:manage','日计划管理','当前用户本人的成长业务',true,true),
 ('planning:assignment:read','任务安排详情','当前用户本人的成长业务',true,true),
 ('planning:assignment:manage','任务安排管理','当前用户本人的成长业务',true,true),
 ('planning:assignment:remove','任务安排移出计划','当前用户本人的成长业务',true,true),
 ('planning:pending:list','待处理任务列表','当前用户本人的成长业务',true,true)
ON CONFLICT (code) DO NOTHING;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code IN ('goal:goal:list','goal:goal:read','goal:goal:create','goal:goal:update','goal:goal:status','goal:goal:delete','goal:goal:archive','goal:goal:restore','stage:stage:list','stage:stage:read','stage:stage:create','stage:stage:update','stage:stage:status','stage:stage:delete','stage:stage:sort','task:task:list','task:task:read','task:task:create','task:task:update','task:task:status','task:task:delete','task:task:sort','planning:weekly:list','planning:weekly:read','planning:weekly:manage','planning:daily:list','planning:daily:read','planning:daily:manage','planning:assignment:read','planning:assignment:manage','planning:assignment:remove','planning:pending:list') AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        RAISE EXCEPTION 'Growth permission code conflicts with an existing custom or deleted permission';
    END IF;
END $$;
WITH inserted AS (
 INSERT INTO identity_role_permission(role_id,permission_id)
 SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
 WHERE r.code = 'USER' AND p.code IN ('goal:goal:list','goal:goal:read','goal:goal:create','goal:goal:update','goal:goal:status','goal:goal:delete','goal:goal:archive','goal:goal:restore','stage:stage:list','stage:stage:read','stage:stage:create','stage:stage:update','stage:stage:status','stage:stage:delete','stage:stage:sort','task:task:list','task:task:read','task:task:create','task:task:update','task:task:status','task:task:delete','task:task:sort','planning:weekly:list','planning:weekly:read','planning:weekly:manage','planning:daily:list','planning:daily:read','planning:daily:manage','planning:assignment:read','planning:assignment:manage','planning:assignment:remove','planning:pending:list')
 ON CONFLICT DO NOTHING RETURNING role_id
)
UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);
-- 注册普通用户的成长菜单；页面显示由本标记块末尾的显式启用语句维护。
INSERT INTO identity_menu(code,name,menu_type,route_path,sort_order,is_visible,is_builtin,is_enabled)
VALUES ('growth_root','成长规划','DIRECTORY','/growth',20,false,true,true) ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goals',m.id,'目标','PAGE','/growth/goals','growth/goals',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='goal:goal:list'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stages',m.id,'阶段','PAGE','/growth/stages','growth/stages',p.id,20,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='stage:stage:list'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_tasks',m.id,'任务','PAGE','/growth/tasks','growth/tasks',p.id,30,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='task:task:list'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_weekly',m.id,'周计划','PAGE','/planning/weekly-plans','planning/weekly',p.id,40,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:weekly:read'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_daily',m.id,'日计划','PAGE','/planning/daily-plans','planning/daily',p.id,50,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:daily:read'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_pending',m.id,'待处理任务','PAGE','/planning/pending-tasks','planning/pending',p.id,60,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_root' AND p.code='planning:pending:list'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_create',m.id,'目标创建','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:create'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_update',m.id,'目标修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:update'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_status',m.id,'目标状态修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:status'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_delete',m.id,'目标删除','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:delete'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_archive',m.id,'目标归档','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:archive'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_goal_goal_restore',m.id,'目标恢复','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_goals' AND p.code='goal:goal:restore'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stage_stage_create',m.id,'阶段创建','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:create'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stage_stage_update',m.id,'阶段修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:update'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stage_stage_status',m.id,'阶段状态修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:status'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stage_stage_delete',m.id,'阶段删除','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:delete'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_stage_stage_sort',m.id,'阶段排序','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_stages' AND p.code='stage:stage:sort'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_task_task_create',m.id,'任务创建','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:create'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_task_task_update',m.id,'任务修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:update'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_task_task_status',m.id,'任务状态修改','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:status'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_task_task_delete',m.id,'任务删除','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:delete'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_task_task_sort',m.id,'任务排序','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_tasks' AND p.code='task:task:sort'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_planning_weekly_manage',m.id,'周计划管理','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:weekly:manage'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_planning_daily_manage',m.id,'日计划管理','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_daily' AND p.code='planning:daily:manage'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_planning_assignment_manage',m.id,'任务安排管理','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:assignment:manage'
 ON CONFLICT(code) DO NOTHING;
INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,is_visible,is_builtin,is_enabled)
 SELECT 'growth_planning_assignment_remove',m.id,'任务安排移出计划','BUTTON',p.id,10,false,true,true
 FROM identity_menu m CROSS JOIN identity_permission p WHERE m.code='growth_weekly' AND p.code='planning:assignment:remove'
 ON CONFLICT(code) DO NOTHING;
DO $$ BEGIN
    IF (SELECT count(*) FROM identity_menu WHERE code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending','growth_goal_goal_create','growth_goal_goal_update','growth_goal_goal_status','growth_goal_goal_delete','growth_goal_goal_archive','growth_goal_goal_restore','growth_stage_stage_create','growth_stage_stage_update','growth_stage_stage_status','growth_stage_stage_delete','growth_stage_stage_sort','growth_task_task_create','growth_task_task_update','growth_task_task_status','growth_task_task_delete','growth_task_task_sort','growth_planning_weekly_manage','growth_planning_daily_manage','growth_planning_assignment_manage','growth_planning_assignment_remove') AND is_builtin AND deleted_at IS NULL) <> 27 THEN
        RAISE EXCEPTION 'Growth menu code conflicts with an existing custom or deleted menu';
    END IF;
END $$;
WITH inserted AS (
 INSERT INTO identity_role_menu(role_id,menu_id)
 SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
 WHERE r.code = 'USER' AND m.code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending','growth_goal_goal_create','growth_goal_goal_update','growth_goal_goal_status','growth_goal_goal_delete','growth_goal_goal_archive','growth_goal_goal_restore','growth_stage_stage_create','growth_stage_stage_update','growth_stage_stage_status','growth_stage_stage_delete','growth_stage_stage_sort','growth_task_task_create','growth_task_task_update','growth_task_task_status','growth_task_task_delete','growth_task_task_sort','growth_planning_weekly_manage','growth_planning_daily_manage','growth_planning_assignment_manage','growth_planning_assignment_remove')
 ON CONFLICT DO NOTHING RETURNING role_id
)
UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);

WITH removed AS (
 DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
 WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
 AND p.code ~ '^(goal|stage|task|planning):' RETURNING rp.role_id
) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
WITH removed AS (
 DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
 WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
 AND (m.code LIKE 'growth_%' OR m.component_key LIKE 'growth/%' OR m.component_key LIKE 'planning/%' OR m.permission_id IN (SELECT id FROM identity_permission WHERE code ~ '^(goal|stage|task|planning):')) RETURNING rm.role_id
) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
-- 周/日主页面展示当前计划，入口依赖 read，历史列表另依赖 list。
UPDATE identity_menu m SET permission_id=p.id,version=m.version+1,updated_at=CURRENT_TIMESTAMP
FROM identity_permission p
WHERE ((m.code='growth_weekly' AND p.code='planning:weekly:read') OR (m.code='growth_daily' AND p.code='planning:daily:read'))
AND m.is_builtin AND m.deleted_at IS NULL AND m.permission_id IS DISTINCT FROM p.id;
-- 成长页面已完成；普通用户入口启用，按钮节点仍隐藏。
UPDATE identity_menu SET is_visible=true,version=version+1,updated_at=CURRENT_TIMESTAMP
WHERE code IN ('growth_root','growth_goals','growth_stages','growth_tasks','growth_weekly','growth_daily','growth_pending')
AND is_builtin AND deleted_at IS NULL AND NOT is_visible;
-- END GROWTH AUTHORIZATION

-- BEGIN EXECUTION AUTHORIZATION
-- 可在已有库中只选取本标记块，放入手工事务执行，不重跑前面的建表语句。
-- 默认只登记禁用权限和隐藏禁用菜单，不修改目标旧规则，不启用尚未实现的接口。
-- 后端验收后，在同一事务 SET LOCAL asterway.execution_backend_ready = 'true'；
-- 前端验收后，再同时 SET LOCAL asterway.execution_frontend_ready = 'true'。
-- 这两个标记仅供本段手工 SQL 使用，不是应用环境变量或系统配置项。
DO $execution_authorization$
DECLARE
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.execution_backend_ready', true), ''), 'false')::boolean;
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.execution_frontend_ready', true), ''), 'false')::boolean;
    execution_codes text[] := ARRAY[
        'execution:session:list', 'execution:session:read', 'execution:session:start',
        'execution:session:pause', 'execution:session:resume', 'execution:session:finish',
        'execution:session:heartbeat'
    ];
    menu_codes text[] := ARRAY[
        'growth_executions', 'growth_execution_read', 'growth_execution_start',
        'growth_execution_pause', 'growth_execution_resume', 'growth_execution_finish'
    ];
    root_id bigint;
    page_id bigint;
BEGIN
    IF frontend_ready AND NOT backend_ready THEN
        RAISE EXCEPTION 'Execution frontend requires the approved backend';
    END IF;
    IF backend_ready AND (to_regclass('execution_session') IS NULL OR to_regclass('execution_segment') IS NULL) THEN
        RAISE EXCEPTION 'Execution tables must be initialized before backend authorization';
    END IF;
    SELECT id INTO root_id FROM identity_menu
    WHERE code = 'growth_root' AND menu_type = 'DIRECTORY' AND is_builtin AND deleted_at IS NULL;
    IF root_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code = 'USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Built-in USER role and growth directory are required';
    END IF;
    IF EXISTS (
        SELECT 1 FROM identity_permission WHERE code = ANY(execution_codes)
          AND (NOT is_builtin OR deleted_at IS NOT NULL)
    ) OR EXISTS (
        SELECT 1 FROM identity_menu WHERE code = ANY(menu_codes)
          AND (NOT is_builtin OR deleted_at IS NOT NULL)
    ) THEN
        RAISE EXCEPTION 'Execution code conflicts with an existing custom or deleted object';
    END IF;

    INSERT INTO identity_permission(code, name, description, is_builtin, is_enabled)
    SELECT v.code, v.name, '普通用户本人的任务执行；管理身份不开放个人核心业务', true, false
    FROM (VALUES
        ('execution:session:list', '执行记录列表'),
        ('execution:session:read', '执行详情与耗时汇总'),
        ('execution:session:start', '开始任务执行'),
        ('execution:session:pause', '暂停任务执行'),
        ('execution:session:resume', '恢复任务执行'),
        ('execution:session:finish', '结束任务执行'),
        ('execution:session:heartbeat', '执行心跳')
    ) AS v(code, name)
    ON CONFLICT(code) DO NOTHING;

    INSERT INTO identity_menu(
        code, parent_id, name, menu_type, route_path, component_key, permission_id,
        sort_order, is_visible, is_builtin, is_enabled
    )
    SELECT 'growth_executions', root_id, '执行计时', 'PAGE', '/execution', 'execution/index', p.id,
           70, false, true, false
    FROM identity_permission p WHERE p.code = 'execution:session:list'
    ON CONFLICT(code) DO NOTHING;
    SELECT id INTO page_id FROM identity_menu WHERE code = 'growth_executions';

    INSERT INTO identity_menu(
        code, parent_id, name, menu_type, permission_id, sort_order, is_visible, is_builtin, is_enabled
    )
    SELECT v.code, page_id, v.name, 'BUTTON', p.id, v.sort_order, false, true, false
    FROM (VALUES
        ('growth_execution_read', '查看执行详情', 'execution:session:read', 10),
        ('growth_execution_start', '开始执行', 'execution:session:start', 20),
        ('growth_execution_pause', '暂停执行', 'execution:session:pause', 30),
        ('growth_execution_resume', '继续执行', 'execution:session:resume', 40),
        ('growth_execution_finish', '结束执行', 'execution:session:finish', 50)
    ) AS v(code, name, permission_code, sort_order)
    JOIN identity_permission p ON p.code = v.permission_code
    ON CONFLICT(code) DO NOTHING;

    IF EXISTS (
        SELECT 1 FROM identity_menu m LEFT JOIN identity_permission p ON p.id = m.permission_id
        WHERE m.code = ANY(menu_codes) AND (p.id IS NULL OR
            (m.code = 'growth_executions' AND (m.menu_type <> 'PAGE' OR m.parent_id IS DISTINCT FROM root_id
                OR m.route_path IS DISTINCT FROM '/execution'
                OR m.component_key IS DISTINCT FROM 'execution/index'
                OR p.code IS DISTINCT FROM 'execution:session:list'))
            OR (m.code <> 'growth_executions' AND (m.menu_type <> 'BUTTON' OR m.parent_id IS DISTINCT FROM page_id
                OR p.code IS DISTINCT FROM ('execution:session:' || substring(m.code FROM length('growth_execution_') + 1))))
        )
    ) OR (SELECT count(*) FROM identity_menu WHERE code = ANY(menu_codes)) <> 6 THEN
        RAISE EXCEPTION 'Existing execution menu does not match the reserved contract';
    END IF;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id, permission_id)
        SELECT r.id, p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code = 'USER' AND p.code = ANY(execution_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id, menu_id)
        SELECT r.id, m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code = 'USER' AND m.code = ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    WHERE id IN (SELECT role_id FROM inserted);

    -- 管理身份不能通过角色关系获得个人执行权限；后端仍须先做身份限制再做超级管理员放行。
    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r, identity_permission p
        WHERE rp.role_id = r.id AND rp.permission_id = p.id AND r.code IN ('ADMIN', 'SUPER_ADMIN')
          AND p.code LIKE 'execution:%' RETURNING rp.role_id
    )
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r, identity_menu m
        WHERE rm.role_id = r.id AND rm.menu_id = m.id AND r.code IN ('ADMIN', 'SUPER_ADMIN')
          AND (m.code = ANY(menu_codes) OR m.component_key LIKE 'execution/%') RETURNING rm.role_id
    )
    UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
    WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled = true, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE code = ANY(execution_codes) AND NOT is_enabled RETURNING id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id = rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled = true,
                is_visible = (code = 'growth_executions' AND frontend_ready),
                version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE code = ANY(menu_codes) AND (NOT is_enabled
                OR is_visible IS DISTINCT FROM (code = 'growth_executions' AND frontend_ready))
            RETURNING id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id = rm.menu_id);

        -- 与目标只归档的新后端同批启用；只停用旧动作和移除授权，保留字典及历史审计。
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code = 'goal:goal:delete' AND NOT is_builtin)
           OR EXISTS (SELECT 1 FROM identity_menu WHERE code = 'growth_goal_goal_delete' AND NOT is_builtin) THEN
            RAISE EXCEPTION 'Legacy goal delete code belongs to a custom object';
        END IF;
        WITH changed AS (
            UPDATE identity_permission SET is_enabled = false, version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE code = 'goal:goal:delete' AND is_builtin AND is_enabled RETURNING id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id = rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled = false, is_visible = false,
                version = version + 1, updated_at = CURRENT_TIMESTAMP
            WHERE code = 'growth_goal_goal_delete' AND is_builtin AND (is_enabled OR is_visible) RETURNING id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id = rm.menu_id);
        WITH removed AS (
            DELETE FROM identity_role_permission rp USING identity_permission p
            WHERE rp.permission_id = p.id AND p.code = 'goal:goal:delete' AND p.is_builtin RETURNING rp.role_id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
        WITH removed AS (
            DELETE FROM identity_role_menu rm USING identity_menu m
            WHERE rm.menu_id = m.id AND m.code = 'growth_goal_goal_delete' AND m.is_builtin RETURNING rm.role_id
        )
        UPDATE identity_role SET version = version + 1, updated_at = CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
    END IF;
END
$execution_authorization$;
-- END EXECUTION AUTHORIZATION

-- BEGIN REMAINING AUTHORIZATION
-- 新库默认只登记禁用权限和隐藏禁用菜单；已有库可单独选取本标记块在维护事务内执行。
-- 后端验收后 SET LOCAL asterway.remaining_backend_ready = 'true';
-- 前端将来验收后再同时 SET LOCAL asterway.remaining_frontend_ready = 'true'。
-- 这些为手工 SQL 的事务级开关，不是应用环境变量，不写入 application.yml。
DO $remaining_authorization$
DECLARE
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_backend_ready', true), ''), 'false')::boolean;
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_frontend_ready', true), ''), 'false')::boolean;
    root_id bigint;
    parent_menu_id bigint;
    reserved_permission_id bigint;
    permission_codes text[] := ARRAY[]::text[];
    menu_codes text[] := ARRAY[]::text[];
    item record;
BEGIN
    IF frontend_ready AND NOT backend_ready THEN
        RAISE EXCEPTION 'Remaining frontend requires the approved backend';
    END IF;
    IF backend_ready AND EXISTS (
        SELECT 1 FROM unnest(ARRAY['habit_category','habit','habit_revision','habit_checkin','notification_message',
            'goal_state_history','stage_state_history','task_state_history']) AS required(table_name)
        WHERE to_regclass(required.table_name) IS NULL
    ) THEN
        RAISE EXCEPTION 'Remaining tables must be initialized before backend authorization';
    END IF;
    SELECT id INTO root_id FROM identity_menu
    WHERE code='growth_root' AND menu_type='DIRECTORY' AND is_builtin AND deleted_at IS NULL;
    IF root_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Built-in USER role and growth directory are required';
    END IF;

    -- 页面先登记，后续按钮才能引用稳定的父菜单；每条权限对应一个预留菜单节点。
    FOR item IN SELECT * FROM (VALUES
            ('habit:category:list', '习惯分类列表', 'growth_habit_categories', '习惯分类', 'PAGE', NULL::text, '/habits/categories', 'habit/categories', 85),
            ('habit:habit:list', '习惯列表与今日习惯', 'growth_habits', '习惯管理', 'PAGE', NULL::text, '/habits', 'habit/index', 80),
            ('notification:message:list', '站内消息列表与未读数', 'growth_notifications', '站内消息', 'PAGE', NULL::text, '/notifications', 'notification/index', 90),
            ('dashboard:board:read', '今日看板', 'growth_dashboard', '今日看板', 'PAGE', NULL::text, '/dashboard', 'dashboard/index', 5),
            ('statistics:report:read', '历史统计', 'growth_statistics', '历史统计', 'PAGE', NULL::text, '/statistics', 'statistics/index', 100),
            ('habit:category:create', '创建习惯分类', 'growth_habit_category_create', '创建分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 10),
            ('habit:category:update', '修改习惯分类', 'growth_habit_category_update', '修改分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 20),
            ('habit:category:delete', '删除习惯分类', 'growth_habit_category_delete', '删除分类', 'BUTTON', 'growth_habit_categories', NULL::text, NULL::text, 30),
            ('habit:habit:read', '查看习惯', 'growth_habit_read', '查看习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 10),
            ('habit:habit:create', '创建习惯', 'growth_habit_create', '创建习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 20),
            ('habit:habit:update', '修改习惯', 'growth_habit_update', '修改习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 30),
            ('habit:habit:pause', '暂停习惯', 'growth_habit_pause', '暂停习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 40),
            ('habit:habit:resume', '恢复习惯', 'growth_habit_resume', '恢复习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 50),
            ('habit:habit:end', '结束归档习惯', 'growth_habit_end', '结束归档', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 60),
            ('habit:habit:reopen', '重新开启习惯', 'growth_habit_reopen', '重新开启', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 70),
            ('habit:habit:delete', '删除习惯', 'growth_habit_delete', '删除习惯', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 80),
            ('habit:habit:history', '查看规则和生命周期历史', 'growth_habit_history', '规则与生命周期', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 90),
            ('habit:checkin:create', '今日打卡', 'growth_habit_checkin_create', '今日打卡', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 100),
            ('habit:checkin:cancel', '撤销今日打卡', 'growth_habit_checkin_cancel', '撤销今日打卡', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 110),
            ('habit:checkin:read', '打卡历史', 'growth_habit_checkin_read', '打卡历史', 'BUTTON', 'growth_habits', NULL::text, NULL::text, 120),
            ('notification:message:read', '消息详情', 'growth_notification_read', '消息详情', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 10),
            ('notification:message:mark_read', '标记已读', 'growth_notification_mark_read', '标记已读', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 20),
            ('notification:message:handle', '处理达标提醒', 'growth_notification_handle', '处理提醒', 'BUTTON', 'growth_notifications', NULL::text, NULL::text, 30)
    ) AS v(permission_code,permission_name,menu_code,menu_name,menu_type,parent_code,route_path,component_key,sort_order)
    LOOP
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code=item.permission_code
                AND (NOT is_builtin OR deleted_at IS NOT NULL))
           OR EXISTS (SELECT 1 FROM identity_menu WHERE code=item.menu_code
                AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
            RAISE EXCEPTION 'Remaining code conflicts with a custom or deleted object: %', item.permission_code;
        END IF;
        permission_codes := array_append(permission_codes,item.permission_code);
        menu_codes := array_append(menu_codes,item.menu_code);
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES(item.permission_code,item.permission_name,'仅普通用户本人的核心业务；管理身份优先且不开放',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO reserved_permission_id FROM identity_permission WHERE code=item.permission_code;
        IF item.parent_code IS NULL THEN
            parent_menu_id := root_id;
        ELSE
            SELECT id INTO parent_menu_id FROM identity_menu WHERE code=item.parent_code AND deleted_at IS NULL;
        END IF;
        IF parent_menu_id IS NULL THEN
            RAISE EXCEPTION 'Missing remaining parent menu: %',item.parent_code;
        END IF;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,
            sort_order,is_visible,is_builtin,is_enabled)
        VALUES(item.menu_code,parent_menu_id,item.menu_name,item.menu_type,item.route_path,item.component_key,
            reserved_permission_id,item.sort_order,false,true,false)
        ON CONFLICT(code) DO NOTHING;
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code=item.menu_code AND (
            m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type
            OR m.route_path IS DISTINCT FROM item.route_path OR m.component_key IS DISTINCT FROM item.component_key
            OR m.permission_id IS DISTINCT FROM reserved_permission_id
        )) THEN
            RAISE EXCEPTION 'Existing remaining menu does not match the reserved contract: %',item.menu_code;
        END IF;
    END LOOP;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    )
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM inserted);

    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
        RETURNING rp.role_id
    )
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    )
    UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        )
        UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled=true,
                is_visible=(menu_type='PAGE' AND (frontend_ready OR is_visible)),
                version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(menu_codes) AND (NOT is_enabled
                OR is_visible IS DISTINCT FROM (menu_type='PAGE' AND (frontend_ready OR is_visible)))
            RETURNING id
        )
        UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    END IF;
END
$remaining_authorization$;
-- END REMAINING AUTHORIZATION

COMMIT;
