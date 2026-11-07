-- Agent：会话、记忆、知识版本、规划运行、复盘与调用记录的最终初始化 SQL。
-- 已有库按标记段补齐；先在隔离库验证。不得重建或覆盖已有业务事实。
-- 新库先执行 identity.sql；本文件手工创建 agent schema 的三张表，不使用自动建表或迁移工具。
-- 已有 P1.1 库也无需重建 Identity；本文件的两个授权块可单独置于维护事务执行。
-- 默认准备态：权限禁用、页面隐藏。后端完成后在同一维护事务执行标记块：
-- SET LOCAL asterway.agent_backend_ready='true';
-- 前端完成后再同时 SET LOCAL asterway.agent_frontend_ready='true';
-- 标记块可单独置于 BEGIN/COMMIT 内执行，禁止覆盖自定义或已删除的同码记录。
BEGIN;
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT CONVERSATION SCHEMA
-- 三表只存 Agent 自身数据。user_id 与 Identity 用户为逻辑关联，无物理外键或级联删除。
-- 不使用 IF NOT EXISTS 掩盖已有结构偏差；若 schema 已存在，先核对结构，不直接重跑。
CREATE SCHEMA agent;
REVOKE ALL ON SCHEMA agent FROM PUBLIC;

CREATE TABLE agent.conversation (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    title varchar(100) NOT NULL DEFAULT '新对话' CHECK (title ~ '[^[:space:]]'),
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CLOSED')),
    context_summary text NOT NULL DEFAULT '' CHECK (char_length(context_summary) <= 16000),
    summary_through_message_no bigint NOT NULL DEFAULT 0 CHECK (summary_through_message_no >= 0),
    summary_updated_at timestamptz(3),
    next_message_no bigint NOT NULL DEFAULT 1 CHECK (next_message_no > 0),
    last_message_at timestamptz(3),
    closed_at timestamptz(3),
    deleted_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agent_conversation_summary CHECK (
        summary_through_message_no < next_message_no
        AND ((context_summary = '' AND summary_through_message_no = 0 AND summary_updated_at IS NULL)
            OR (context_summary ~ '[^[:space:]]' AND summary_through_message_no > 0
                AND summary_updated_at IS NOT NULL))
    ),
    CONSTRAINT ck_agent_conversation_status CHECK (
        ((status = 'ACTIVE' AND closed_at IS NULL) OR (status = 'CLOSED' AND closed_at IS NOT NULL))
        AND (deleted_at IS NULL OR status = 'CLOSED')
    ),
    CONSTRAINT ck_agent_conversation_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (last_message_at IS NULL OR (isfinite(last_message_at)
            AND last_message_at BETWEEN created_at AND updated_at))
        AND (summary_updated_at IS NULL OR (isfinite(summary_updated_at)
            AND summary_updated_at BETWEEN created_at AND updated_at))
        AND (closed_at IS NULL OR (isfinite(closed_at) AND closed_at BETWEEN created_at AND updated_at))
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at BETWEEN closed_at AND updated_at))
    )
);
COMMENT ON TABLE agent.conversation IS 'Agent 会话；用户自有，关闭后只读，删除为逻辑删除，原始消息与摘要分开保留';
COMMENT ON COLUMN agent.conversation.id IS '会话主键；对外按字符串传输';
COMMENT ON COLUMN agent.conversation.user_id IS 'Java 验证得到的所属用户，不从客户端请求体或模型参数读取';
COMMENT ON COLUMN agent.conversation.title IS '可修改的会话标题，1～100 字，空白标题无效';
COMMENT ON COLUMN agent.conversation.context_summary IS '派生历史摘要，最多 16000 字；不替代原始消息，不作为认证或业务授权依据';
COMMENT ON COLUMN agent.conversation.summary_through_message_no IS '摘要已覆盖的连续历史边界；Service 只在终态轮次边界推进，保留原始消息';
COMMENT ON COLUMN agent.conversation.summary_updated_at IS '摘要最后提交时间；无摘要时为空';
COMMENT ON COLUMN agent.conversation.next_message_no IS '分配下条会话内消息序号；事务内锁会话后递增，不用时间戳排序';
COMMENT ON COLUMN agent.conversation.last_message_at IS '最近消息写入时间；没有消息时为空';
COMMENT ON COLUMN agent.conversation.closed_at IS '关闭时间；ACTIVE 为空，CLOSED 必填，关闭后不再发起生成';
COMMENT ON COLUMN agent.conversation.deleted_at IS '逻辑删除时间；删除后所有接口按不存在处理，不清空历史事实';
COMMENT ON COLUMN agent.conversation.version IS '标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本';
CREATE INDEX ix_agent_conversation_user_page ON agent.conversation (user_id, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_agent_conversation_user_status ON agent.conversation (user_id, status, updated_at DESC, id DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE agent.conversation_turn (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    request_id uuid NOT NULL CHECK (request_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    status varchar(16) NOT NULL DEFAULT 'RUNNING'
        CHECK (status IN ('RUNNING','COMPLETED','FAILED','CANCELLED','INCOMPLETE')),
    finish_reason varchar(64) CHECK (finish_reason ~ '^[a-z][a-z0-9_]{0,63}$'),
    error_code varchar(64) CHECK (error_code ~ '^[A-Z][A-Z0-9_]{0,63}$'),
    started_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deadline_at timestamptz(3) NOT NULL,
    finished_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_turn_user_request UNIQUE (user_id, request_id),
    CONSTRAINT ck_agent_turn_status CHECK (
        (status = 'RUNNING' AND finished_at IS NULL AND finish_reason IS NULL AND error_code IS NULL)
        OR (status = 'COMPLETED' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL
            AND finish_reason = 'stop' AND error_code IS NULL)
        OR (status = 'INCOMPLETE' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL
            AND finish_reason <> 'stop' AND error_code IS NOT NULL)
        OR (status IN ('FAILED','CANCELLED') AND finished_at IS NOT NULL AND error_code IS NOT NULL)
    ),
    CONSTRAINT ck_agent_turn_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND isfinite(started_at) AND isfinite(deadline_at)
        AND updated_at >= created_at AND started_at BETWEEN created_at AND updated_at
        AND deadline_at > started_at
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN started_at AND updated_at))
    )
);
COMMENT ON TABLE agent.conversation_turn IS '一次用户提问及其生成生命周期；支持幂等、取消、失败和超时恢复，不充当消息内容表';
COMMENT ON COLUMN agent.conversation_turn.conversation_id IS '所属会话逻辑标识；必须与 user_id 同属当前用户，由 Service 同事务核对';
COMMENT ON COLUMN agent.conversation_turn.request_id IS '客户端为一次发送生成的 UUID；同用户重放同请求只返回原轮次，不重复调用模型';
COMMENT ON COLUMN agent.conversation_turn.request_hash IS '规范化请求的 SHA-256；重用 request_id 但会话或内容不同必须返回冲突';
COMMENT ON COLUMN agent.conversation_turn.status IS 'RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断';
COMMENT ON COLUMN agent.conversation_turn.finish_reason IS '模型真实结束原因，不把 length 或其他截断伪装为 stop';
COMMENT ON COLUMN agent.conversation_turn.error_code IS '可公开错误码，不保存上游异常全文、认证头或密钥';
COMMENT ON COLUMN agent.conversation_turn.deadline_at IS '服务端生成的本轮截止时间；过期 RUNNING 先归一为失败，再接受下一轮';
COMMENT ON COLUMN agent.conversation_turn.version IS '轮次状态和终态提交的乐观锁，禁止旧生成任务覆盖已取消或已结束结果';
CREATE UNIQUE INDEX uk_agent_turn_running_conversation ON agent.conversation_turn (conversation_id)
    WHERE status = 'RUNNING';
CREATE INDEX ix_agent_turn_user_conversation ON agent.conversation_turn
    (user_id, conversation_id, started_at DESC, id DESC);
CREATE INDEX ix_agent_turn_deadline ON agent.conversation_turn (deadline_at, id)
    WHERE status = 'RUNNING';

CREATE TABLE agent.conversation_message (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    turn_id bigint NOT NULL CHECK (turn_id > 0),
    message_no bigint NOT NULL CHECK (message_no > 0),
    role varchar(16) NOT NULL CHECK (role IN ('USER','ASSISTANT','TOOL')),
    content text NOT NULL DEFAULT '' CHECK (char_length(content) <= 65536),
    is_complete boolean NOT NULL DEFAULT true,
    tool_calls jsonb NOT NULL DEFAULT '[]'::jsonb,
    tool_request_message_id bigint CHECK (tool_request_message_id > 0 AND tool_request_message_id <> id),
    tool_call_id varchar(128) CHECK (tool_call_id ~ '^[A-Za-z0-9_-]{1,128}$'),
    tool_name varchar(64) CHECK (tool_name ~ '^[a-z][a-z0-9_]{0,63}$'),
    model varchar(128) CHECK (model ~ '[^[:space:]]'),
    finish_reason varchar(64) CHECK (finish_reason ~ '^[a-z][a-z0-9_]{0,63}$'),
    usage jsonb,
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_message_conversation_no UNIQUE (conversation_id, message_no),
    CONSTRAINT ck_agent_message_json CHECK (
        CASE WHEN jsonb_typeof(tool_calls) = 'array' THEN jsonb_array_length(tool_calls) <= 32 ELSE false END
        AND octet_length(tool_calls::text) <= 262144
        AND (usage IS NULL OR (jsonb_typeof(usage) = 'object' AND octet_length(usage::text) <= 4096))
    ),
    CONSTRAINT ck_agent_message_role CHECK (
        (role = 'USER' AND is_complete AND char_length(content) BETWEEN 1 AND 8000
            AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb
            AND tool_request_message_id IS NULL AND tool_call_id IS NULL AND tool_name IS NULL
            AND model IS NULL AND finish_reason IS NULL AND usage IS NULL)
        OR (role = 'ASSISTANT' AND tool_request_message_id IS NULL AND tool_call_id IS NULL
            AND tool_name IS NULL AND model IS NOT NULL
            AND (NOT is_complete OR (finish_reason IS NOT NULL AND (
                (finish_reason = 'stop' AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb)
                OR (finish_reason = 'tool_calls' AND tool_calls <> '[]'::jsonb)))))
        OR (role = 'TOOL' AND is_complete AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb
            AND tool_request_message_id IS NOT NULL AND tool_call_id IS NOT NULL AND tool_name IS NOT NULL
            AND model IS NULL AND finish_reason IS NULL AND usage IS NULL)
    ),
    CONSTRAINT ck_agent_message_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
COMMENT ON TABLE agent.conversation_message IS '会话内顺序消息事实；保留用户、助手、工具结果和中断的部分文本，不保存系统密钥或客户端自报身份';
COMMENT ON COLUMN agent.conversation_message.message_no IS '会话内严格递增序号；并发分配由会话行锁和 next_message_no 负责';
COMMENT ON COLUMN agent.conversation_message.turn_id IS '所属提问轮次逻辑标识；同会话同用户必须由 Service 核对，不伪装成行内 CHECK';
COMMENT ON COLUMN agent.conversation_message.content IS '用户最多 8000 字，助手和工具最多 65536 字；工具结果只保存业务允许返回的脱敏内容';
COMMENT ON COLUMN agent.conversation_message.is_complete IS '助手部分输出为 false；失败、取消、截断的文本可展示，但不能作为已确认回答进入后续推理';
COMMENT ON COLUMN agent.conversation_message.tool_calls IS '助手原始工具调用数组，结构上限 32 项／256 KiB；实际注册白名单及更小预算由后续工具模块执行';
COMMENT ON COLUMN agent.conversation_message.tool_request_message_id IS '工具结果对应的助手消息逻辑标识；Service 核对同用户、同会话、同轮次及调用标识';
COMMENT ON COLUMN agent.conversation_message.tool_call_id IS '对应助手 tool_calls 内的调用标识；不同助手消息允许使用相同调用标识';
COMMENT ON COLUMN agent.conversation_message.tool_name IS '已注册工具名称；P1.2 只具备存储结构，P1.3 验收前不开放工具执行';
COMMENT ON COLUMN agent.conversation_message.usage IS '助手调用的实际 token 用量快照；只接受允许的数字字段，不存请求头或 SDK 原始响应';
CREATE INDEX ix_agent_message_user_page ON agent.conversation_message (user_id, conversation_id, message_no DESC);
CREATE INDEX ix_agent_message_turn ON agent.conversation_message (turn_id, message_no);
CREATE UNIQUE INDEX uk_agent_message_turn_user ON agent.conversation_message (turn_id) WHERE role = 'USER';
CREATE UNIQUE INDEX uk_agent_message_tool_result ON agent.conversation_message (tool_request_message_id, tool_call_id)
    WHERE role = 'TOOL';

REVOKE ALL ON ALL TABLES IN SCHEMA agent FROM PUBLIC;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA agent FROM PUBLIC;
-- Python 以后使用独立受限数据库角色，仅授予 agent schema 的 USAGE、三表 DML 与序列 USAGE。
-- 不在公开 SQL 建立含真实密码的角色，不授予 Java 业务表读写权限。
-- 跨表归属、轮次终态、工具调用与结果配对、摘要边界、已完成消息不可改由事务和 Service 负责。
-- 本期关闭／逻辑删除只改会话，不删除消息；存在未结束轮次时先停止生成再操作。
-- END AGENT CONVERSATION SCHEMA

SELECT pg_advisory_xact_lock(hashtext('asterway-agent-authorization'));

-- BEGIN AGENT AUTHORIZATION
DO $agent_authorization$
DECLARE
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_backend_ready',true),''),'false')::boolean;
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_frontend_ready',true),''),'false')::boolean;
    root_id bigint;
    parent_menu_id bigint;
    permission_id_value bigint;
    permission_codes text[] := ARRAY['agent:chat:read','agent:chat:send'];
    menu_codes text[] := ARRAY['growth_agent','growth_agent_chat_send'];
    item record;
BEGIN
    IF frontend_ready AND NOT backend_ready THEN
        RAISE EXCEPTION 'Agent frontend requires the implemented backend';
    END IF;
    SELECT id INTO root_id FROM identity_menu
    WHERE code='growth_root' AND menu_type='DIRECTORY' AND is_builtin AND deleted_at IS NULL;
    IF root_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Built-in USER role and growth directory are required';
    END IF;
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
        AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        RAISE EXCEPTION 'Reserved Agent code conflicts with a custom or deleted object';
    END IF;

    FOR item IN SELECT * FROM (VALUES
        ('agent:chat:read','访问成长助手','growth_agent','成长助手','PAGE',
            NULL::text,'/agent','agent/index',110),
        ('agent:chat:send','发送成长助手提问','growth_agent_chat_send','发送提问','BUTTON',
            'growth_agent',NULL::text,NULL::text,10)
    ) AS entries(permission_code,permission_name,menu_code,menu_name,menu_type,
        parent_code,route_path,component_key,sort_order)
    LOOP
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES(item.permission_code,item.permission_name,
            '仅普通用户本人的成长助手；管理身份优先且不开放',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code=item.permission_code;
        IF item.parent_code IS NULL THEN
            parent_menu_id := root_id;
        ELSE
            SELECT id INTO parent_menu_id FROM identity_menu WHERE code=item.parent_code;
        END IF;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,
            permission_id,sort_order,is_visible,is_builtin,is_enabled)
        VALUES(item.menu_code,parent_menu_id,item.menu_name,item.menu_type,item.route_path,
            item.component_key,permission_id_value,item.sort_order,false,true,false)
        ON CONFLICT(code) DO NOTHING;
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code=item.menu_code AND (
            m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type
            OR m.route_path IS DISTINCT FROM item.route_path OR m.component_key IS DISTINCT FROM item.component_key
            OR m.permission_id IS DISTINCT FROM permission_id_value
        )) THEN
            RAISE EXCEPTION 'Existing Agent menu does not match the reserved contract';
        END IF;
    END LOOP;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);

    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled=true,
                is_visible=(menu_type='PAGE' AND (frontend_ready OR is_visible)),
                version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(menu_codes) AND (NOT is_enabled
                OR is_visible IS DISTINCT FROM (menu_type='PAGE' AND (frontend_ready OR is_visible))) RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    END IF;
END
$agent_authorization$;
-- END AGENT AUTHORIZATION

-- BEGIN AGENT CONVERSATION AUTHORIZATION
-- P1.2 独立准备态；P1.1 的 ready 开关绝不提前启用尚未实现的会话操作。
-- SQL 与后端验收后，才在维护事务设置 asterway.agent_conversation_backend_ready='true'。
DO $agent_conversation_authorization$
DECLARE
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_conversation_backend_ready',true),''),'false')::boolean;
    page_id bigint;
    permission_id_value bigint;
    permission_codes text[] := ARRAY[]::text[];
    menu_codes text[] := ARRAY[]::text[];
    item record;
BEGIN
    SELECT id INTO page_id FROM identity_menu
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        AND is_builtin AND deleted_at IS NULL;
    IF page_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Built-in USER role and implemented Agent page are required';
    END IF;
    IF backend_ready AND (to_regclass('agent.conversation') IS NULL
        OR to_regclass('agent.conversation_turn') IS NULL OR to_regclass('agent.conversation_message') IS NULL) THEN
        RAISE EXCEPTION 'Agent conversation tables must exist before backend authorization';
    END IF;
    FOR item IN SELECT * FROM (VALUES
        ('list','查询会话列表',10),('read','读取会话与消息',20),('create','创建会话',30),
        ('update','修改会话标题',40),('close','关闭会话',50),('delete','逻辑删除会话',60)
    ) AS entries(action,name,sort_order)
    LOOP
        permission_codes := array_append(permission_codes,'agent:conversation:' || item.action);
        menu_codes := array_append(menu_codes,'growth_agent_conversation_' || item.action);
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code='agent:conversation:' || item.action
            AND (NOT is_builtin OR deleted_at IS NOT NULL))
            OR EXISTS (SELECT 1 FROM identity_menu WHERE code='growth_agent_conversation_' || item.action
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
            RAISE EXCEPTION 'Reserved conversation code conflicts with a custom or deleted object';
        END IF;
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES('agent:conversation:' || item.action,item.name,'仅普通用户本人会话；P1.2 后端验收后启用',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:conversation:' || item.action;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            is_visible,is_builtin,is_enabled)
        VALUES('growth_agent_conversation_' || item.action,page_id,item.name,'BUTTON',
            permission_id_value,item.sort_order,false,true,false)
        ON CONFLICT(code) DO NOTHING;
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code='growth_agent_conversation_' || item.action
            AND (m.parent_id IS DISTINCT FROM page_id OR m.menu_type<>'BUTTON'
                OR m.permission_id IS DISTINCT FROM permission_id_value
                OR m.route_path IS NOT NULL OR m.component_key IS NOT NULL)) THEN
            RAISE EXCEPTION 'Existing conversation menu does not match the reserved contract';
        END IF;
    END LOOP;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    END IF;
END
$agent_conversation_authorization$;
-- END AGENT CONVERSATION AUTHORIZATION

COMMIT;

-- =====================================================================
-- P1.4.0 记忆、知识库与后台任务 SQL
-- 新库：在会话三表段之后顺序执行本段；已有 P1.2/P1.3 库：只单独执行本段。
-- 本段保留历史数组向量列；后续 COMPLETION 段增加 pgvector(1024) 及不可变文档版本。
-- =====================================================================
BEGIN;
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT P1.4 MEMORY SCHEMA
CREATE TABLE agent.memory (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    kind varchar(16) NOT NULL CHECK (kind IN ('PREFERENCE','FACT','CONSTRAINT','GOAL_CONTEXT')),
    content text NOT NULL CHECK (content ~ '[^[:space:]]' AND char_length(content) <= 4000),
    source varchar(16) NOT NULL DEFAULT 'INFERRED'
        CHECK (source IN ('USER_CONFIRMED','USER_STATED','INFERRED')),
    status varchar(16) NOT NULL DEFAULT 'CANDIDATE'
        CHECK (status IN ('CANDIDATE','CONFIRMED','REJECTED','ARCHIVED')),
    confidence numeric(3,2) NOT NULL DEFAULT 0.60 CHECK (confidence >= 0 AND confidence <= 1),
    source_conversation_id bigint,
    source_turn_id bigint,
    supersedes_id bigint,
    confirmed_at timestamptz(3),
    rejected_at timestamptz(3),
    archived_at timestamptz(3),
    deleted_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agent_memory_supersedes CHECK (supersedes_id IS NULL OR supersedes_id <> id),
    CONSTRAINT ck_agent_memory_status CHECK (
        (status = 'CANDIDATE' AND confirmed_at IS NULL AND rejected_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND rejected_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        OR (status = 'REJECTED' AND rejected_at IS NOT NULL AND confirmed_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL AND deleted_at IS NULL)
        OR deleted_at IS NOT NULL
    ),
    CONSTRAINT ck_agent_memory_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        AND (rejected_at IS NULL OR (isfinite(rejected_at) AND rejected_at BETWEEN created_at AND updated_at))
        AND (archived_at IS NULL OR (isfinite(archived_at) AND archived_at BETWEEN created_at AND updated_at))
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at >= created_at AND deleted_at <= updated_at))
    )
);
COMMENT ON TABLE agent.memory IS '长期记忆；候选需用户确认后才跨会话使用，逻辑删除保留审计，不存密码或令牌';
COMMENT ON COLUMN agent.memory.user_id IS 'Java 验证得到的所属用户，不接受客户端或模型指定';
COMMENT ON COLUMN agent.memory.kind IS '偏好、事实、约束或目标背景；仅作为对话上下文，不作为业务授权依据';
COMMENT ON COLUMN agent.memory.status IS 'CANDIDATE 候选、CONFIRMED 已确认、REJECTED 已拒绝、ARCHIVED 已归档';
COMMENT ON COLUMN agent.memory.confidence IS '0～1 置信度；仅辅助排序，不替代用户确认';
COMMENT ON COLUMN agent.memory.supersedes_id IS '本条替代的旧记忆逻辑引用；不级联删除';
CREATE INDEX ix_agent_memory_user_status ON agent.memory(user_id, status, updated_at DESC);
CREATE INDEX ix_agent_memory_user_kind ON agent.memory(user_id, kind, updated_at DESC);
CREATE UNIQUE INDEX uk_agent_memory_confirmed_content
    ON agent.memory(user_id, md5(content))
    WHERE status = 'CONFIRMED' AND deleted_at IS NULL;

CREATE TABLE agent.memory_embedding (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    memory_id bigint NOT NULL,
    user_id bigint NOT NULL CHECK (user_id > 0),
    embedding_model varchar(100) NOT NULL CHECK (embedding_model ~ '[^[:space:]]'),
    dimension integer NOT NULL CHECK (dimension > 0 AND dimension <= 8192),
    embedding double precision[] NOT NULL,
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_memory_embedding_memory UNIQUE (memory_id),
    CONSTRAINT ck_agent_memory_embedding_shape CHECK (
        cardinality(embedding) = dimension AND array_ndims(embedding) = 1
    ),
    CONSTRAINT ck_agent_memory_embedding_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
COMMENT ON TABLE agent.memory_embedding IS '已确认记忆的向量；一条记忆当前一条向量，模型变更时更新该行';
COMMENT ON COLUMN agent.memory_embedding.embedding IS 'double precision 数组，维度与 dimension 一致；不依赖 pgvector 扩展';
CREATE INDEX ix_agent_memory_embedding_user ON agent.memory_embedding(user_id);

CREATE TABLE agent.knowledge_document (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    title varchar(200) NOT NULL CHECK (title ~ '[^[:space:]]'),
    file_name varchar(255) NOT NULL CHECK (file_name ~ '[^[:space:]]'),
    mime_type varchar(100) NOT NULL CHECK (mime_type ~ '[^[:space:]]'),
    size_bytes bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 26214400),
    checksum varchar(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    storage_key varchar(500) NOT NULL CHECK (storage_key ~ '[^[:space:]]'),
    status varchar(16) NOT NULL DEFAULT 'UPLOADED'
        CHECK (status IN ('UPLOADED','PARSING','READY','FAILED','DELETED')),
    chunk_count integer NOT NULL DEFAULT 0 CHECK (chunk_count >= 0),
    error_message varchar(1000),
    deleted_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agent_knowledge_document_status CHECK (
        (status = 'FAILED' AND error_message ~ '[^[:space:]]' AND deleted_at IS NULL)
        OR (status = 'DELETED' AND deleted_at IS NOT NULL)
        OR (status IN ('UPLOADED','PARSING','READY') AND deleted_at IS NULL)
    ),
    CONSTRAINT ck_agent_knowledge_document_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at BETWEEN created_at AND updated_at))
    )
);
COMMENT ON TABLE agent.knowledge_document IS '用户上传的知识文档元数据；文件本体不入库，只存受控存储键与校验和';
COMMENT ON COLUMN agent.knowledge_document.storage_key IS '服务端生成的文件标识，不直接暴露给用户或模型';
COMMENT ON COLUMN agent.knowledge_document.status IS 'UPLOADED 已上传、PARSING 解析中、READY 可检索、FAILED 失败、DELETED 已删除';
COMMENT ON COLUMN agent.knowledge_document.checksum IS 'sha256 十六进制，用于同用户去重和重复上传识别';
CREATE INDEX ix_agent_knowledge_document_user_status ON agent.knowledge_document(user_id, status, created_at DESC);
CREATE UNIQUE INDEX uk_agent_knowledge_document_user_checksum
    ON agent.knowledge_document(user_id, checksum) WHERE deleted_at IS NULL;

CREATE TABLE agent.knowledge_chunk (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id bigint NOT NULL,
    user_id bigint NOT NULL CHECK (user_id > 0),
    chunk_no integer NOT NULL CHECK (chunk_no >= 0),
    content text NOT NULL CHECK (content ~ '[^[:space:]]' AND char_length(content) <= 8000),
    token_count integer NOT NULL CHECK (token_count > 0 AND token_count <= 8192),
    embedding_model varchar(100),
    embedding_dimension integer CHECK (embedding_dimension > 0 AND embedding_dimension <= 8192),
    embedding double precision[],
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_knowledge_chunk_no UNIQUE (document_id, chunk_no),
    CONSTRAINT ck_agent_knowledge_chunk_embedding CHECK (
        (embedding IS NULL AND embedding_model IS NULL AND embedding_dimension IS NULL)
        OR (embedding IS NOT NULL AND embedding_model IS NOT NULL AND embedding_dimension IS NOT NULL
            AND cardinality(embedding) = embedding_dimension AND array_ndims(embedding) = 1)
    ),
    CONSTRAINT ck_agent_knowledge_chunk_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
COMMENT ON TABLE agent.knowledge_chunk IS '文档分块与向量；按文档顺序保留，删除文档时为逻辑删除而不物理丢弃历史';
COMMENT ON COLUMN agent.knowledge_chunk.content IS '分块正文，最多 8000 字；原文只用于检索和引用';
COMMENT ON COLUMN agent.knowledge_chunk.embedding IS 'double precision 数组，未完成向量化时为空';
CREATE INDEX ix_agent_knowledge_chunk_user_document ON agent.knowledge_chunk(user_id, document_id, chunk_no);

CREATE TABLE agent.background_task (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    kind varchar(32) NOT NULL
        CHECK (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING','MEMORY_EMBEDDING')),
    ref_type varchar(32) NOT NULL
        CHECK (ref_type IN ('KNOWLEDGE_DOCUMENT','MEMORY')),
    ref_id bigint NOT NULL CHECK (ref_id > 0),
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0 AND attempts <= 20),
    error_code varchar(64),
    error_message varchar(1000),
    scheduled_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at timestamptz(3),
    finished_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_agent_background_task_ref CHECK (
        (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING') AND ref_type = 'KNOWLEDGE_DOCUMENT')
        OR (kind = 'MEMORY_EMBEDDING' AND ref_type = 'MEMORY')
    ),
    CONSTRAINT ck_agent_background_task_status CHECK (
        (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL AND error_code IS NULL)
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL)
        OR (status = 'SUCCEEDED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_code IS NULL)
        OR (status = 'FAILED' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            AND error_message ~ '[^[:space:]]')
        OR (status = 'CANCELLED' AND finished_at IS NOT NULL)
    ),
    CONSTRAINT ck_agent_background_task_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (started_at IS NULL OR (isfinite(started_at) AND started_at BETWEEN created_at AND updated_at))
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
COMMENT ON TABLE agent.background_task IS '文档解析/向量化等后台任务；仅记录调度与结果，不存业务事实';
COMMENT ON COLUMN agent.background_task.ref_id IS '关联文档或记忆的逻辑引用，不建物理外键';
COMMENT ON COLUMN agent.background_task.attempts IS '已尝试次数，最多 20 次，超过后由服务置为失败';
CREATE INDEX ix_agent_background_task_due ON agent.background_task(status, scheduled_at, id);
CREATE UNIQUE INDEX uk_agent_background_task_active
    ON agent.background_task(kind, ref_type, ref_id)
    WHERE status IN ('PENDING','RUNNING');
-- END AGENT P1.4 MEMORY SCHEMA

COMMIT;

-- =====================================================================
-- P1.4.0 记忆与知识库权限（准备态，默认禁用隐藏）
-- P1.4.1/P1.4.2 后端实现并通过验收后，才在维护事务设置：
-- SET LOCAL asterway.agent_memory_backend_ready='true';
-- =====================================================================
BEGIN;
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT MEMORY AUTHORIZATION
DO $agent_memory_authorization$
DECLARE
    page_id bigint;
    permission_id_value bigint;
    backend_ready boolean := COALESCE(current_setting('asterway.agent_memory_backend_ready', true) = 'true', false);
    permission_codes text[] := ARRAY[
        'agent:memory:read','agent:memory:manage',
        'agent:knowledge:read','agent:knowledge:manage'
    ];
    menu_codes text[] := ARRAY[
        'growth_agent_memory_read','growth_agent_memory_manage',
        'growth_agent_knowledge_read','growth_agent_knowledge_manage'
    ];
    item record;
BEGIN
    SELECT id INTO page_id FROM identity_menu
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        AND is_builtin AND deleted_at IS NULL;
    IF page_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Agent page or USER role is missing; run P1.1 authorization first';
    END IF;

    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        RAISE EXCEPTION 'Reserved memory/knowledge code conflicts with a custom or deleted object';
    END IF;

    FOR item IN SELECT * FROM (VALUES
        ('read','查看长期记忆与知识库'),
        ('manage','确认、编辑或删除长期记忆，上传或删除知识文档')
    ) AS entries(action,name)
    LOOP
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES('agent:memory:' || item.action,item.name,'仅普通用户本人记忆；P1.4 后端验收后启用',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:memory:' || item.action;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            is_visible,is_builtin,is_enabled)
        VALUES('growth_agent_memory_' || item.action,page_id,item.name,'BUTTON',
            permission_id_value,CASE WHEN item.action = 'manage' THEN 20 ELSE 19 END,false,true,false)
        ON CONFLICT(code) DO NOTHING;

        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES('agent:knowledge:' || item.action,item.name,'仅普通用户本人知识库；P1.4 后端验收后启用',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:knowledge:' || item.action;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            is_visible,is_builtin,is_enabled)
        VALUES('growth_agent_knowledge_' || item.action,page_id,item.name,'BUTTON',
            permission_id_value,CASE WHEN item.action = 'manage' THEN 22 ELSE 21 END,false,true,false)
        ON CONFLICT(code) DO NOTHING;
    END LOOP;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    END IF;
END
$agent_memory_authorization$;
-- END AGENT MEMORY AUTHORIZATION

COMMIT;

-- =====================================================================
-- P1.5.0 工作流、方案版本、事件与 Java 幂等提交 SQL
-- 新库：在 P1.4 段之后顺序执行；已有库：只单独执行本段。
-- 方案快照存 JSONB，由 Python 工作流引擎生成；Java 只在确认后按快照原子提交业务。
-- =====================================================================
BEGIN;
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT P1.5 WORKFLOW SCHEMA
CREATE TABLE agent.workflow_run (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    source_turn_id bigint,
    intent varchar(32) NOT NULL CHECK (intent IN (
        'GOAL_PLAN','STAGE_PLAN','TASK_PLAN','WEEKLY_PLAN','DAILY_PLAN','HABIT_PLAN','COMPREHENSIVE_PLAN'
    )),
    status varchar(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        'DRAFT','WAITING_CONFIRM','CONFIRMED','EXECUTING','SUCCEEDED','FAILED','CANCELLED','EXPIRED'
    )),
    state_version integer NOT NULL DEFAULT 0 CHECK (state_version >= 0),
    latest_plan_version_id bigint,
    graph_state jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(graph_state) = 'object'),
    expires_at timestamptz(3) NOT NULL,
    confirmed_at timestamptz(3),
    finished_at timestamptz(3),
    error_code varchar(64),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT ck_agent_workflow_run_status CHECK (
        (status IN ('DRAFT','WAITING_CONFIRM') AND confirmed_at IS NULL AND finished_at IS NULL)
        OR (status IN ('CONFIRMED','EXECUTING') AND confirmed_at IS NOT NULL AND finished_at IS NULL)
        OR (status IN ('SUCCEEDED','FAILED') AND confirmed_at IS NOT NULL AND finished_at IS NOT NULL)
        OR (status = 'CANCELLED' AND finished_at IS NOT NULL)
        OR (status = 'EXPIRED' AND finished_at IS NOT NULL)
    ),
    CONSTRAINT ck_agent_workflow_run_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND isfinite(expires_at) AND expires_at > created_at
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
COMMENT ON TABLE agent.workflow_run IS '一次对话内的工作流运行；方案先确认后执行，过期或取消后不再提交业务';
COMMENT ON COLUMN agent.workflow_run.user_id IS 'Java 验证得到的所属用户，不接受客户端或模型指定';
COMMENT ON COLUMN agent.workflow_run.status IS '草稿、待确认、已确认、执行中、成功、失败、取消、过期';
COMMENT ON COLUMN agent.workflow_run.graph_state IS '工作流中间状态，只存编排数据，不存业务事实或令牌';
COMMENT ON COLUMN agent.workflow_run.expires_at IS '方案确认与提交的失效时间；过期后不执行提交';
CREATE INDEX ix_agent_workflow_run_user ON agent.workflow_run(user_id, status, updated_at DESC);
CREATE INDEX ix_agent_workflow_run_conversation ON agent.workflow_run(conversation_id, updated_at DESC);
CREATE UNIQUE INDEX uk_agent_workflow_run_active
    ON agent.workflow_run(conversation_id)
    WHERE status IN ('DRAFT','WAITING_CONFIRM','CONFIRMED','EXECUTING');

CREATE TABLE agent.plan_version (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id bigint NOT NULL CHECK (run_id > 0),
    user_id bigint NOT NULL CHECK (user_id > 0),
    version_no integer NOT NULL CHECK (version_no > 0),
    status varchar(24) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT','WAITING_CONFIRM','CONFIRMED','SUPERSEDED','REJECTED','EXPIRED')),
    summary varchar(1000) NOT NULL CHECK (summary ~ '[^[:space:]]'),
    snapshot jsonb NOT NULL CHECK (jsonb_typeof(snapshot) = 'object'),
    diff_summary jsonb,
    confirmation_token varchar(64) CHECK (confirmation_token IS NULL OR confirmation_token ~ '^[0-9a-f]{64}$'),
    confirmed_at timestamptz(3),
    superseded_at timestamptz(3),
    expires_at timestamptz(3) NOT NULL,
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uk_agent_plan_version_no UNIQUE (run_id, version_no),
    CONSTRAINT ck_agent_plan_version_confirm CHECK (
        (status IN ('DRAFT','WAITING_CONFIRM','REJECTED','EXPIRED') AND confirmed_at IS NULL)
        OR (status = 'SUPERSEDED' AND superseded_at IS NOT NULL
            AND ((confirmed_at IS NULL AND confirmation_token IS NULL)
                OR (confirmed_at IS NOT NULL AND confirmation_token IS NOT NULL)))
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND confirmation_token IS NOT NULL)
    ),
    CONSTRAINT ck_agent_plan_version_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND isfinite(expires_at) AND expires_at > created_at
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        AND (superseded_at IS NULL OR (isfinite(superseded_at) AND superseded_at BETWEEN created_at AND updated_at))
    )
);
COMMENT ON TABLE agent.plan_version IS '方案不可变版本；修订生成新版本，旧版本标记被替代，确认后快照不再修改';
COMMENT ON COLUMN agent.plan_version.snapshot IS '结构化方案快照（目标/阶段/任务/计划/习惯），Java 提交只读取已确认快照';
COMMENT ON COLUMN agent.plan_version.confirmation_token IS '确认令牌 sha256，仅内部传递，不写入日志或返回浏览器';
CREATE INDEX ix_agent_plan_version_run ON agent.plan_version(run_id, version_no DESC);
CREATE UNIQUE INDEX uk_agent_plan_version_one_confirmed ON agent.plan_version(run_id) WHERE status = 'CONFIRMED';

CREATE TABLE agent.workflow_event (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    run_id bigint NOT NULL CHECK (run_id > 0),
    user_id bigint NOT NULL CHECK (user_id > 0),
    event_no bigint NOT NULL CHECK (event_no > 0),
    event_type varchar(32) NOT NULL CHECK (event_type IN (
        'RUN_STARTED','PLAN_DRAFTED','PLAN_REVISED','CONFIRMED','COMMIT_STARTED',
        'COMMIT_SUCCEEDED','COMMIT_FAILED','CANCELLED','EXPIRED'
    )),
    payload jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(payload) = 'object'),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agent_workflow_event_no UNIQUE (run_id, event_no),
    CONSTRAINT ck_agent_workflow_event_time CHECK (isfinite(created_at))
);
COMMENT ON TABLE agent.workflow_event IS '工作流事件流；只追加不修改，用于恢复、展示与审计，不存敏感令牌';
CREATE INDEX ix_agent_workflow_event_run ON agent.workflow_event(run_id, event_no);

CREATE TABLE agent.plan_commit (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    run_id bigint NOT NULL CHECK (run_id > 0),
    plan_version_id bigint NOT NULL CHECK (plan_version_id > 0),
    idempotency_key varchar(64) NOT NULL CHECK (idempotency_key ~ '^[0-9a-f]{64}$'),
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0 AND attempts <= 10),
    result jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(result) = 'object'),
    error_code varchar(64),
    error_message varchar(1000),
    started_at timestamptz(3),
    finished_at timestamptz(3),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    CONSTRAINT uk_agent_plan_commit_key UNIQUE (idempotency_key),
    CONSTRAINT uk_agent_plan_commit_version UNIQUE (plan_version_id),
    CONSTRAINT ck_agent_plan_commit_status CHECK (
        (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL)
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL)
        OR (status = 'SUCCEEDED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_code IS NULL)
        OR (status = 'FAILED' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            AND error_message ~ '[^[:space:]]')
    ),
    CONSTRAINT ck_agent_plan_commit_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (started_at IS NULL OR (isfinite(started_at) AND started_at BETWEEN created_at AND updated_at))
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
COMMENT ON TABLE agent.plan_commit IS 'Java 幂等提交记录；同一方案版本只提交一次，重试返回同一结果，成功/失败均留审计';
COMMENT ON COLUMN agent.plan_commit.idempotency_key IS 'Java 与 Python 共享的幂等键 sha256；重复请求不得重复创建业务';
COMMENT ON COLUMN agent.plan_commit.result IS 'Java 返回的创建结果摘要（实体标识与状态），不存敏感信息';
CREATE INDEX ix_agent_plan_commit_user ON agent.plan_commit(user_id, created_at DESC);
-- END AGENT P1.5 WORKFLOW SCHEMA

COMMIT;

-- =====================================================================
-- P1.5.0 工作流权限（准备态，默认禁用隐藏）
-- P1.5.1—P1.5.5 实现并通过验收后，才在维护事务设置：
-- SET LOCAL asterway.agent_workflow_backend_ready='true';
-- =====================================================================
BEGIN;
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT WORKFLOW AUTHORIZATION
DO $agent_workflow_authorization$
DECLARE
    page_id bigint;
    permission_id_value bigint;
    backend_ready boolean := COALESCE(current_setting('asterway.agent_workflow_backend_ready', true) = 'true', false);
    permission_codes text[] := ARRAY['agent:workflow:read','agent:workflow:manage'];
    menu_codes text[] := ARRAY['growth_agent_workflow_read','growth_agent_workflow_manage'];
    item record;
BEGIN
    SELECT id INTO page_id FROM identity_menu
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        AND is_builtin AND deleted_at IS NULL;
    IF page_id IS NULL OR NOT EXISTS (
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    ) THEN
        RAISE EXCEPTION 'Agent page or USER role is missing; run P1.1 authorization first';
    END IF;
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        RAISE EXCEPTION 'Reserved workflow code conflicts with a custom or deleted object';
    END IF;

    FOR item IN SELECT * FROM (VALUES
        ('read','查看规划方案与工作流状态'),
        ('manage','确认、取消或重新生成规划方案')
    ) AS entries(action,name)
    LOOP
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES('agent:workflow:' || item.action,item.name,'仅普通用户本人规划；P1.5 后端验收后启用',true,false)
        ON CONFLICT(code) DO NOTHING;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:workflow:' || item.action;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            is_visible,is_builtin,is_enabled)
        VALUES('growth_agent_workflow_' || item.action,page_id,item.name,'BUTTON',
            permission_id_value,CASE WHEN item.action = 'manage' THEN 24 ELSE 23 END,false,true,false)
        ON CONFLICT(code) DO NOTHING;
    END LOOP;

    WITH inserted AS (
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='USER' AND p.code=ANY(permission_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH inserted AS (
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='USER' AND m.code=ANY(menu_codes)
        ON CONFLICT DO NOTHING RETURNING role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM inserted);
    WITH removed AS (
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);
    WITH removed AS (
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        WHERE id IN (SELECT role_id FROM removed);

    IF backend_ready THEN
        WITH changed AS (
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        WITH changed AS (
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    END IF;
END
$agent_workflow_authorization$;
-- END AGENT WORKFLOW AUTHORIZATION

COMMIT;


-- BEGIN AGENT COMPLETION SCHEMA
-- 手工执行的最终结构补齐段；保留既有数据，不建立迁移版本链。
BEGIN;
SET LOCAL lock_timeout='5s';
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
CREATE TABLE IF NOT EXISTS agent.embedding_profile (
    code varchar(100) PRIMARY KEY,
    provider varchar(32) NOT NULL,
    model varchar(100) NOT NULL,
    dimension integer NOT NULL CHECK (dimension=1024),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO agent.embedding_profile(code,provider,model,dimension)
VALUES ('dashscope:text-embedding-v4:1024','DASHSCOPE','text-embedding-v4',1024)
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS agent.knowledge_revision (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id>0),
    document_id bigint NOT NULL CHECK (document_id>0),
    revision_no integer NOT NULL CHECK (revision_no>0),
    file_name varchar(255) NOT NULL,
    mime_type varchar(100) NOT NULL,
    size_bytes bigint NOT NULL CHECK (size_bytes>0),
    checksum varchar(64) NOT NULL CHECK (checksum ~ '^[a-f0-9]{64}$'),
    storage_key varchar(500) NOT NULL,
    status varchar(16) NOT NULL CHECK (status IN ('UPLOADED','PARSING','READY','FAILED')),
    index_status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (index_status IN ('PENDING','READY','FAILED')),
    chunk_count integer NOT NULL DEFAULT 0 CHECK (chunk_count>=0),
    error_message varchar(1000),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id,revision_no),
    CHECK (isfinite(created_at) AND isfinite(updated_at) AND updated_at>=created_at)
);
COMMENT ON TABLE agent.knowledge_revision IS '不可变原文版本；仅解析和索引状态可推进，文件正文不覆盖';
ALTER TABLE agent.knowledge_document ADD COLUMN IF NOT EXISTS current_revision_id bigint;
INSERT INTO agent.knowledge_revision(user_id,document_id,revision_no,file_name,mime_type,
    size_bytes,checksum,storage_key,status,chunk_count,error_message,created_at,updated_at)
SELECT user_id,id,1,file_name,mime_type,size_bytes,checksum,storage_key,
    CASE WHEN status='DELETED' THEN 'READY' ELSE status END,chunk_count,error_message,created_at,updated_at
FROM agent.knowledge_document d WHERE NOT EXISTS (
    SELECT 1 FROM agent.knowledge_revision r WHERE r.document_id=d.id);
UPDATE agent.knowledge_document d SET current_revision_id=r.id
FROM agent.knowledge_revision r WHERE r.document_id=d.id AND r.revision_no=1
    AND d.current_revision_id IS NULL;
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS revision_id bigint;
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS page_number integer CHECK (page_number>0);
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS heading varchar(300);
UPDATE agent.knowledge_chunk c SET revision_id=d.current_revision_id
FROM agent.knowledge_document d WHERE d.id=c.document_id AND c.revision_id IS NULL;
ALTER TABLE agent.knowledge_chunk ALTER COLUMN revision_id SET NOT NULL;
ALTER TABLE agent.knowledge_chunk DROP CONSTRAINT IF EXISTS uk_agent_knowledge_chunk_no;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_revision_chunk_no ON agent.knowledge_chunk(revision_id,chunk_no);
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS vector_value public.vector(1024);
ALTER TABLE agent.memory_embedding ADD COLUMN IF NOT EXISTS vector_value public.vector(1024);
UPDATE agent.knowledge_chunk SET vector_value=embedding::public.vector(1024)
WHERE vector_value IS NULL AND embedding_dimension=1024;
UPDATE agent.memory_embedding SET vector_value=embedding::public.vector(1024)
WHERE vector_value IS NULL AND dimension=1024;
CREATE INDEX IF NOT EXISTS ix_agent_chunk_vector ON agent.knowledge_chunk
    USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_agent_memory_vector ON agent.memory_embedding
    USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;
CREATE INDEX IF NOT EXISTS ix_agent_chunk_owner_revision ON agent.knowledge_chunk(user_id,revision_id,id);
ALTER TABLE agent.conversation_message ADD COLUMN IF NOT EXISTS citations jsonb NOT NULL DEFAULT '[]';
ALTER TABLE agent.conversation_message ADD COLUMN IF NOT EXISTS used_memory_ids jsonb NOT NULL DEFAULT '[]';

CREATE TABLE IF NOT EXISTS agent.review_report (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id>0),
    date_from varchar(10) NOT NULL, date_to varchar(10) NOT NULL,
    time_zone varchar(64) NOT NULL,
    status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','READY','FAILED')),
    facts jsonb NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(facts)='object'),
    content text NOT NULL DEFAULT '', error_message varchar(1000),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (date_from<=date_to AND updated_at>=created_at)
);
CREATE INDEX IF NOT EXISTS ix_agent_review_owner ON agent.review_report(user_id,id DESC);
CREATE TABLE IF NOT EXISTS agent.call_record (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id>0), trace_id varchar(64) NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('MODEL','TOOL','EMBEDDING')),
    name varchar(128) NOT NULL, status varchar(16) NOT NULL CHECK (status IN ('SUCCEEDED','FAILED','CANCELLED')),
    duration_ms bigint NOT NULL CHECK (duration_ms>=0),
    first_token_ms bigint CHECK (first_token_ms>=0),
    prompt_tokens bigint CHECK (prompt_tokens>=0), completion_tokens bigint CHECK (completion_tokens>=0),
    error_code varchar(64), conversation_id bigint, turn_id bigint, run_id bigint,
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE agent.call_record IS '调用用量与耗时；不记录提问正文、令牌或密钥，未知用量为 NULL';
CREATE INDEX IF NOT EXISTS ix_agent_call_owner_date ON agent.call_record(user_id,created_at DESC,id DESC);
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS background_task_kind_check;
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS background_task_ref_type_check;
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS ck_agent_background_task_ref;
ALTER TABLE agent.background_task ADD CONSTRAINT background_task_kind_check CHECK (
    kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING','MEMORY_EMBEDDING','MEMORY_EXTRACT','REVIEW_GENERATE'));
ALTER TABLE agent.background_task ADD CONSTRAINT background_task_ref_type_check CHECK (
    ref_type IN ('KNOWLEDGE_DOCUMENT','MEMORY','CONVERSATION_TURN','REVIEW_REPORT'));
ALTER TABLE agent.background_task ADD CONSTRAINT ck_agent_background_task_ref CHECK (
    (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING') AND ref_type='KNOWLEDGE_DOCUMENT') OR
    (kind='MEMORY_EMBEDDING' AND ref_type='MEMORY') OR
    (kind='MEMORY_EXTRACT' AND ref_type='CONVERSATION_TURN') OR
    (kind='REVIEW_GENERATE' AND ref_type='REVIEW_REPORT'));
COMMIT;
-- END AGENT COMPLETION SCHEMA

-- BEGIN AGENT CHECKPOINT SCHEMA
-- langgraph-checkpoint-postgres 3.2.0 最终运行表；不调用 setup，不建立迁移表。
BEGIN;
CREATE TABLE IF NOT EXISTS agent.checkpoints (
    thread_id TEXT NOT NULL,
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    checkpoint_id TEXT NOT NULL,
    parent_checkpoint_id TEXT,
    type TEXT,
    checkpoint JSONB NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id)
);
CREATE TABLE IF NOT EXISTS agent.checkpoint_blobs (
    thread_id TEXT NOT NULL,
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    channel TEXT NOT NULL,
    version TEXT NOT NULL,
    type TEXT NOT NULL,
    blob BYTEA,
    PRIMARY KEY (thread_id, checkpoint_ns, channel, version)
);
CREATE TABLE IF NOT EXISTS agent.checkpoint_writes (
    thread_id TEXT NOT NULL,
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    checkpoint_id TEXT NOT NULL,
    task_id TEXT NOT NULL,
    idx INTEGER NOT NULL,
    channel TEXT NOT NULL,
    type TEXT,
    blob BYTEA NOT NULL,
    PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id, task_id, idx)
);
ALTER TABLE agent.checkpoint_blobs ALTER COLUMN blob DROP not null;

    CREATE INDEX IF NOT EXISTS checkpoints_thread_id_idx ON agent.checkpoints(thread_id);
    

    CREATE INDEX IF NOT EXISTS checkpoint_blobs_thread_id_idx ON agent.checkpoint_blobs(thread_id);
    

    CREATE INDEX IF NOT EXISTS checkpoint_writes_thread_id_idx ON agent.checkpoint_writes(thread_id);
    
ALTER TABLE agent.checkpoint_writes ADD COLUMN IF NOT EXISTS task_path TEXT NOT NULL DEFAULT '';
COMMIT;
-- END AGENT CHECKPOINT SCHEMA

BEGIN;

-- BEGIN AGENT WORKSPACE AUTHORIZATION
DO $agent_workspace_authorization$
DECLARE
    ready boolean := COALESCE(current_setting('asterway.agent_workspace_ready',true)='true',false);
    root_id bigint; permission_id_value bigint; item record;
BEGIN
    SELECT id INTO root_id FROM identity_menu WHERE code='growth_root' AND is_builtin AND deleted_at IS NULL;
    IF root_id IS NULL THEN RAISE EXCEPTION 'Growth directory is missing'; END IF;
    FOR item IN SELECT * FROM (VALUES
        ('agent:review:read','查看成长复盘'),('agent:review:manage','生成成长复盘'),('agent:run:read','查看本人运行记录')
    ) AS entries(code,name) LOOP
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code=item.code AND (NOT is_builtin OR deleted_at IS NOT NULL))
        THEN RAISE EXCEPTION 'Reserved permission conflicts with custom or deleted object'; END IF;
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        VALUES(item.code,item.name,'仅普通用户本人使用',true,false) ON CONFLICT(code) DO NOTHING;
    END LOOP;
    FOR item IN SELECT * FROM (VALUES
        ('growth_agent_memories','长期记忆','/agent/memories','agent/memories','agent:memory:read',111),
        ('growth_agent_knowledge','知识资料','/agent/knowledge','agent/knowledge','agent:knowledge:read',112),
        ('growth_agent_runs','运行记录','/agent/runs','agent/runs','agent:run:read',113),
        ('growth_agent_reviews','成长复盘','/agent/reviews','agent/reviews','agent:review:read',114)
    ) AS entries(code,name,path,component,permission,sort_order) LOOP
        IF EXISTS (SELECT 1 FROM identity_menu WHERE code=item.code AND (NOT is_builtin OR deleted_at IS NOT NULL))
        THEN RAISE EXCEPTION 'Reserved menu conflicts with custom or deleted object'; END IF;
        SELECT id INTO permission_id_value FROM identity_permission WHERE code=item.permission AND is_builtin AND deleted_at IS NULL;
        IF permission_id_value IS NULL THEN RAISE EXCEPTION 'Workspace permission missing'; END IF;
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,
            sort_order,is_visible,is_builtin,is_enabled)
        VALUES(item.code,root_id,item.name,'PAGE',item.path,item.component,permission_id_value,
            item.sort_order,false,true,false) ON CONFLICT(code) DO NOTHING;
    END LOOP;
    INSERT INTO identity_role_permission(role_id,permission_id)
    SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
    WHERE r.code='USER' AND r.deleted_at IS NULL AND p.code IN ('agent:review:read','agent:review:manage','agent:run:read')
    ON CONFLICT DO NOTHING;
    INSERT INTO identity_role_menu(role_id,menu_id)
    SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
    WHERE r.code='USER' AND r.deleted_at IS NULL AND m.code IN ('growth_agent_memories','growth_agent_knowledge','growth_agent_runs','growth_agent_reviews')
    ON CONFLICT DO NOTHING;
    DELETE FROM identity_role_permission rp USING identity_role r, identity_permission p
    WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'agent:%';
    DELETE FROM identity_role_menu rm USING identity_role r, identity_menu m
    WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND m.code LIKE 'growth_agent%';
    IF ready THEN
        UPDATE identity_permission SET is_enabled=true, version=version+1, updated_at=CURRENT_TIMESTAMP
        WHERE is_builtin AND deleted_at IS NULL AND code LIKE 'agent:%' AND NOT is_enabled;
        UPDATE identity_menu SET is_enabled=true, is_visible=(menu_type='PAGE'), version=version+1, updated_at=CURRENT_TIMESTAMP
        WHERE is_builtin AND deleted_at IS NULL AND code LIKE 'growth_agent%'
            AND (NOT is_enabled OR (menu_type='PAGE' AND NOT is_visible));
        UPDATE identity_role SET version=version+1, updated_at=CURRENT_TIMESTAMP WHERE code='USER';
    END IF;
END $agent_workspace_authorization$;
-- END AGENT WORKSPACE AUTHORIZATION

COMMIT;
