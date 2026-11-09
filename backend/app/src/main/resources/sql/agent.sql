-- 定义成长助手表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Agent：会话、记忆、知识版本、规划运行、复盘与调用记录的最终初始化 SQL。
-- 已有库按标记段补齐；先在隔离库验证。不得重建或覆盖已有业务事实。
-- 新库先执行 identity.sql；本文件手工创建 agent schema 的三张表，不使用自动建表或迁移工具。
-- 已有 P1.1 库也无需重建 Identity；本文件的两个授权块可单独置于维护事务执行。
-- 默认准备态：权限禁用、页面隐藏。后端完成后在同一维护事务执行标记块：
-- SET LOCAL asterway.agent_backend_ready='true';
-- 前端完成后再同时 SET LOCAL asterway.agent_frontend_ready='true';
-- 标记块可单独置于 BEGIN/COMMIT 内执行，禁止覆盖自定义或已删除的同码记录。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT CONVERSATION SCHEMA
-- 三表只存 Agent 自身数据。user_id 与 Identity 用户为逻辑关联，无物理外键或级联删除。
-- 不使用 IF NOT EXISTS 掩盖已有结构偏差；若 schema 已存在，先核对结构，不直接重跑。
-- 创建独立的数据库命名空间，隔离这组表和访问授权。
CREATE SCHEMA agent;
-- 撤销这里列出的访问权限，限制角色可以读写的范围。
REVOKE ALL ON SCHEMA agent FROM PUBLIC;

-- 创建 agent.conversation 表；Agent 会话；用户自有，关闭后只读，删除为逻辑删除，原始消息与摘要分开保留。
CREATE TABLE agent.conversation (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- title 保存可修改的会话标题，1～100 字，空白标题无效；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    title varchar(100) NOT NULL DEFAULT '新对话' CHECK (title ~ '[^[:space:]]'),
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    status varchar(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','CLOSED')),
    -- context_summary 保存派生历史摘要，最多 16000 字；不替代原始消息，不作为认证或业务授权依据；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    context_summary text NOT NULL DEFAULT '' CHECK (char_length(context_summary) <= 16000),
    -- summary_through_message_no 保存摘要已覆盖的连续历史边界；Service 只在终态轮次边界推进，保留原始消息；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    summary_through_message_no bigint NOT NULL DEFAULT 0 CHECK (summary_through_message_no >= 0),
    -- summary_updated_at 保存摘要最后提交时间；无摘要时为空；类型为带时区的时间点。
    summary_updated_at timestamptz(3),
    -- next_message_no 保存分配下条会话内消息序号；事务内锁会话后递增，不用时间戳排序；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    next_message_no bigint NOT NULL DEFAULT 1 CHECK (next_message_no > 0),
    -- last_message_at 保存最近消息写入时间；没有消息时为空；类型为带时区的时间点。
    last_message_at timestamptz(3),
    -- closed_at 保存关闭时间；ACTIVE 为空，CLOSED 必填，关闭后不再发起生成；类型为带时区的时间点。
    closed_at timestamptz(3),
    -- deleted_at 保存逻辑删除时间；删除后所有接口按不存在处理，不清空历史事实；类型为带时区的时间点。
    deleted_at timestamptz(3),
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_conversation_summary CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：summary_through_message_no < next_message_no。
        summary_through_message_no < next_message_no
        -- 再增加一个必须同时满足的筛选条件。
        AND ((context_summary = '' AND summary_through_message_no = 0 AND summary_updated_at IS NULL)
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR (context_summary ~ '[^[:space:]]' AND summary_through_message_no > 0
                -- 再增加一个必须同时满足的筛选条件。
                AND summary_updated_at IS NOT NULL))
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_conversation_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ((status = 'ACTIVE' AND closed_at IS NULL) OR (status = 'CLOSED' AND closed_at IS NOT NULL))
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR status = 'CLOSED')
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_conversation_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (last_message_at IS NULL OR (isfinite(last_message_at)
            -- 再增加一个必须同时满足的筛选条件。
            AND last_message_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (summary_updated_at IS NULL OR (isfinite(summary_updated_at)
            -- 再增加一个必须同时满足的筛选条件。
            AND summary_updated_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (closed_at IS NULL OR (isfinite(closed_at) AND closed_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at BETWEEN closed_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.conversation IS 'Agent 会话；用户自有，关闭后只读，删除为逻辑删除，原始消息与摘要分开保留';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.id IS '会话主键；对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.user_id IS 'Java 验证得到的所属用户，不从客户端请求体或模型参数读取';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.title IS '可修改的会话标题，1～100 字，空白标题无效';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.context_summary IS '派生历史摘要，最多 16000 字；不替代原始消息，不作为认证或业务授权依据';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.summary_through_message_no IS '摘要已覆盖的连续历史边界；Service 只在终态轮次边界推进，保留原始消息';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.summary_updated_at IS '摘要最后提交时间；无摘要时为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.next_message_no IS '分配下条会话内消息序号；事务内锁会话后递增，不用时间戳排序';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.last_message_at IS '最近消息写入时间；没有消息时为空';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.closed_at IS '关闭时间；ACTIVE 为空，CLOSED 必填，关闭后不再发起生成';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.deleted_at IS '逻辑删除时间；删除后所有接口按不存在处理，不清空历史事实';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation.version IS '标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_conversation_user_page ON agent.conversation (user_id, updated_at DESC, id DESC)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE deleted_at IS NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_conversation_user_status ON agent.conversation (user_id, status, updated_at DESC, id DESC)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE deleted_at IS NULL;

-- 创建 agent.conversation_turn 表；一次用户提问及其生成生命周期；支持幂等、取消、失败和超时恢复，不充当消息内容表。
CREATE TABLE agent.conversation_turn (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- conversation_id 保存所属会话逻辑标识；必须与 user_id 同属当前用户，由 Service 同事务核对；类型为大整数；必须填写，保存时检查后面的限制。
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    -- request_id 保存客户端为一次发送生成的 UUID；同用户重放同请求只返回原轮次，不重复调用模型；类型为唯一标识；必须填写，保存时检查后面的限制。
    request_id uuid NOT NULL CHECK (request_id <> '00000000-0000-0000-0000-000000000000'::uuid),
    -- request_hash 保存规范化请求的 SHA-256；重用 request_id 但会话或内容不同必须返回冲突；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(16) NOT NULL DEFAULT 'RUNNING'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('RUNNING','COMPLETED','FAILED','CANCELLED','INCOMPLETE')),
    -- finish_reason 保存模型真实结束原因，不把 length 或其他截断伪装为 stop；类型为有长度限制的文字；保存时检查后面的限制。
    finish_reason varchar(64) CHECK (finish_reason ~ '^[a-z][a-z0-9_]{0,63}$'),
    -- error_code 保存可公开错误码，不保存上游异常全文、认证头或密钥；类型为有长度限制的文字；保存时检查后面的限制。
    error_code varchar(64) CHECK (error_code ~ '^[A-Z][A-Z0-9_]{0,63}$'),
    -- started_at 保存会话首次开始的服务端时间，恢复时不覆盖；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    started_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- deadline_at 保存服务端生成的本轮截止时间；过期 RUNNING 先归一为失败，再接受下一轮；类型为带时区的时间点；必须填写。
    deadline_at timestamptz(3) NOT NULL,
    -- finished_at 保存最终结束的服务端时间；暂停后结束不会增加执行时间段；类型为带时区的时间点。
    finished_at timestamptz(3),
    -- version 保存轮次状态和终态提交的乐观锁，禁止旧生成任务覆盖已取消或已结束结果；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_turn_user_request UNIQUE (user_id, request_id),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_turn_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'RUNNING' AND finished_at IS NULL AND finish_reason IS NULL AND error_code IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'COMPLETED' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND finish_reason = 'stop' AND error_code IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'INCOMPLETE' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND finish_reason <> 'stop' AND error_code IS NOT NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('FAILED','CANCELLED') AND finished_at IS NOT NULL AND error_code IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_turn_time CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：isfinite(created_at) AND isfinite(updated_at) AND isfinite(started_at) AND isfinite(deadline_at)。
        isfinite(created_at) AND isfinite(updated_at) AND isfinite(started_at) AND isfinite(deadline_at)
        -- 再增加一个必须同时满足的筛选条件。
        AND updated_at >= created_at AND started_at BETWEEN created_at AND updated_at
        -- 再增加一个必须同时满足的筛选条件。
        AND deadline_at > started_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN started_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.conversation_turn IS '一次用户提问及其生成生命周期；支持幂等、取消、失败和超时恢复，不充当消息内容表';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.conversation_id IS '所属会话逻辑标识；必须与 user_id 同属当前用户，由 Service 同事务核对';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.request_id IS '客户端为一次发送生成的 UUID；同用户重放同请求只返回原轮次，不重复调用模型';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.request_hash IS '规范化请求的 SHA-256；重用 request_id 但会话或内容不同必须返回冲突';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.status IS 'RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.finish_reason IS '模型真实结束原因，不把 length 或其他截断伪装为 stop';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.error_code IS '可公开错误码，不保存上游异常全文、认证头或密钥';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.deadline_at IS '服务端生成的本轮截止时间；过期 RUNNING 先归一为失败，再接受下一轮';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_turn.version IS '轮次状态和终态提交的乐观锁，禁止旧生成任务覆盖已取消或已结束结果';
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_turn_running_conversation ON agent.conversation_turn (conversation_id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE status = 'RUNNING';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_turn_user_conversation ON agent.conversation_turn
    -- 继续列出本次使用的字段：对话编号（conversation_id）。
    (user_id, conversation_id, started_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_turn_deadline ON agent.conversation_turn (deadline_at, id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE status = 'RUNNING';

-- 创建 agent.conversation_message 表；会话内顺序消息事实；保留用户、助手、工具结果和中断的部分文本，不保存系统密钥或客户端自报身份。
CREATE TABLE agent.conversation_message (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- conversation_id 保存所属会话逻辑标识；必须与 user_id 同属当前用户，由 Service 同事务核对；类型为大整数；必须填写，保存时检查后面的限制。
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    -- turn_id 保存所属提问轮次逻辑标识；同会话同用户必须由 Service 核对，不伪装成行内 CHECK；类型为大整数；必须填写，保存时检查后面的限制。
    turn_id bigint NOT NULL CHECK (turn_id > 0),
    -- message_no 保存会话内严格递增序号；并发分配由会话行锁和 next_message_no 负责；类型为大整数；必须填写，保存时检查后面的限制。
    message_no bigint NOT NULL CHECK (message_no > 0),
    -- role 保存角色；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    role varchar(16) NOT NULL CHECK (role IN ('USER','ASSISTANT','TOOL')),
    -- content 保存用户最多 8000 字，助手和工具最多 65536 字；工具结果只保存业务允许返回的脱敏内容；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    content text NOT NULL DEFAULT '' CHECK (char_length(content) <= 65536),
    -- is_complete 保存助手部分输出为 false；失败、取消、截断的文本可展示，但不能作为已确认回答进入后续推理；类型为真假值；必须填写，未传值时使用这里的默认值。
    is_complete boolean NOT NULL DEFAULT true,
    -- tool_calls 保存助手原始工具调用数组，结构上限 32 项／256 KiB；实际注册白名单及更小预算由后续工具模块执行；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值。
    tool_calls jsonb NOT NULL DEFAULT '[]'::jsonb,
    -- tool_request_message_id 保存工具结果对应的助手消息逻辑标识；Service 核对同用户、同会话、同轮次及调用标识；类型为大整数；保存时检查后面的限制。
    tool_request_message_id bigint CHECK (tool_request_message_id > 0 AND tool_request_message_id <> id),
    -- tool_call_id 保存对应助手 tool_calls 内的调用标识；不同助手消息允许使用相同调用标识；类型为有长度限制的文字；保存时检查后面的限制。
    tool_call_id varchar(128) CHECK (tool_call_id ~ '^[A-Za-z0-9_-]{1,128}$'),
    -- tool_name 保存已注册工具名称；P1.2 只具备存储结构，P1.3 验收前不开放工具执行；类型为有长度限制的文字；保存时检查后面的限制。
    tool_name varchar(64) CHECK (tool_name ~ '^[a-z][a-z0-9_]{0,63}$'),
    -- model 保存模型；类型为有长度限制的文字；保存时检查后面的限制。
    model varchar(128) CHECK (model ~ '[^[:space:]]'),
    -- finish_reason 保存模型真实结束原因，不把 length 或其他截断伪装为 stop；类型为有长度限制的文字；保存时检查后面的限制。
    finish_reason varchar(64) CHECK (finish_reason ~ '^[a-z][a-z0-9_]{0,63}$'),
    -- usage 保存助手调用的实际 token 用量快照；只接受允许的数字字段，不存请求头或 SDK 原始响应；类型为可按字段查询的 JSON。
    usage jsonb,
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_message_conversation_no UNIQUE (conversation_id, message_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_message_json CHECK (
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN jsonb_typeof(tool_calls) = 'array' THEN jsonb_array_length(tool_calls) <= 32 ELSE false END
        -- 再增加一个必须同时满足的筛选条件。
        AND octet_length(tool_calls::text) <= 262144
        -- 再增加一个必须同时满足的筛选条件。
        AND (usage IS NULL OR (jsonb_typeof(usage) = 'object' AND octet_length(usage::text) <= 4096))
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_message_role CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (role = 'USER' AND is_complete AND char_length(content) BETWEEN 1 AND 8000
            -- 再增加一个必须同时满足的筛选条件。
            AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb
            -- 再增加一个必须同时满足的筛选条件。
            AND tool_request_message_id IS NULL AND tool_call_id IS NULL AND tool_name IS NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND model IS NULL AND finish_reason IS NULL AND usage IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (role = 'ASSISTANT' AND tool_request_message_id IS NULL AND tool_call_id IS NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND tool_name IS NULL AND model IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_complete OR (finish_reason IS NOT NULL AND (
                -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
                (finish_reason = 'stop' AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb)
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR (finish_reason = 'tool_calls' AND tool_calls <> '[]'::jsonb)))))
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (role = 'TOOL' AND is_complete AND content ~ '[^[:space:]]' AND tool_calls = '[]'::jsonb
            -- 再增加一个必须同时满足的筛选条件。
            AND tool_request_message_id IS NOT NULL AND tool_call_id IS NOT NULL AND tool_name IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND model IS NULL AND finish_reason IS NULL AND usage IS NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_message_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.conversation_message IS '会话内顺序消息事实；保留用户、助手、工具结果和中断的部分文本，不保存系统密钥或客户端自报身份';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.message_no IS '会话内严格递增序号；并发分配由会话行锁和 next_message_no 负责';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.turn_id IS '所属提问轮次逻辑标识；同会话同用户必须由 Service 核对，不伪装成行内 CHECK';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.content IS '用户最多 8000 字，助手和工具最多 65536 字；工具结果只保存业务允许返回的脱敏内容';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.is_complete IS '助手部分输出为 false；失败、取消、截断的文本可展示，但不能作为已确认回答进入后续推理';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.tool_calls IS '助手原始工具调用数组，结构上限 32 项／256 KiB；实际注册白名单及更小预算由后续工具模块执行';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.tool_request_message_id IS '工具结果对应的助手消息逻辑标识；Service 核对同用户、同会话、同轮次及调用标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.tool_call_id IS '对应助手 tool_calls 内的调用标识；不同助手消息允许使用相同调用标识';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.tool_name IS '已注册工具名称；P1.2 只具备存储结构，P1.3 验收前不开放工具执行';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.conversation_message.usage IS '助手调用的实际 token 用量快照；只接受允许的数字字段，不存请求头或 SDK 原始响应';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_message_user_page ON agent.conversation_message (user_id, conversation_id, message_no DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_message_turn ON agent.conversation_message (turn_id, message_no);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_message_turn_user ON agent.conversation_message (turn_id) WHERE role = 'USER';
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_message_tool_result ON agent.conversation_message (tool_request_message_id, tool_call_id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE role = 'TOOL';

-- 撤销这里列出的访问权限，限制角色可以读写的范围。
REVOKE ALL ON ALL TABLES IN SCHEMA agent FROM PUBLIC;
-- 撤销这里列出的访问权限，限制角色可以读写的范围。
REVOKE ALL ON ALL SEQUENCES IN SCHEMA agent FROM PUBLIC;
-- Python 以后使用独立受限数据库角色，仅授予 agent schema 的 USAGE、三表 DML 与序列 USAGE。
-- 不在公开 SQL 建立含真实密码的角色，不授予 Java 业务表读写权限。
-- 跨表归属、轮次终态、工具调用与结果配对、摘要边界、已完成消息不可改由事务和 Service 负责。
-- 本期关闭／逻辑删除只改会话，不删除消息；存在未结束轮次时先停止生成再操作。
-- END AGENT CONVERSATION SCHEMA

-- 读取后面列出的字段或计算结果。
SELECT pg_advisory_xact_lock(hashtext('asterway-agent-authorization'));

-- BEGIN AGENT AUTHORIZATION
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $agent_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_backend_ready',true),''),'false')::boolean;
    -- frontend_ready 保存frontend已就绪；类型为真假值。
    frontend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_frontend_ready',true),''),'false')::boolean;
    -- root_id 保存根编号；类型为大整数。
    root_id bigint;
    -- parent_menu_id 保存上级菜单编号；类型为大整数。
    parent_menu_id bigint;
    -- permission_id_value 保存权限编号值；类型为大整数。
    permission_id_value bigint;
    -- permission_codes 保存权限codes；类型为不限固定长度的文字。
    permission_codes text[] := ARRAY['agent:chat:read','agent:chat:send'];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY['growth_agent','growth_agent_chat_send'];
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：item record;。
    item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF frontend_ready AND NOT backend_ready THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Agent frontend requires the implemented backend';
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
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        -- 再增加一个必须同时满足的筛选条件。
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
        -- 再增加一个必须同时满足的筛选条件。
        AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Reserved Agent code conflicts with a custom or deleted object';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('agent:chat:read','访问成长助手','growth_agent','成长助手','PAGE',
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：NULL::text,'/agent','agent/index',110),。
            NULL::text,'/agent','agent/index',110),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('agent:chat:send','发送成长助手提问','growth_agent_chat_send','发送提问','BUTTON',
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'growth_agent',NULL::text,NULL::text,10)。
            'growth_agent',NULL::text,NULL::text,10)
    -- 继续列出本次使用的字段：权限名称（permission_name）、菜单编码（menu_code）、菜单名称（menu_name）、菜单类型（menu_type）。
    ) AS entries(permission_code,permission_name,menu_code,menu_name,menu_type,
        -- 继续列出本次使用的字段：上级编码（parent_code）、route路径（route_path）、component键（component_key）。
        parent_code,route_path,component_key,sort_order)
    -- 继续列出本次使用的字段：LOOP。
    LOOP
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.permission_code,item.permission_name,
            -- 继续列出本次使用的字段：成立（true）。
            '仅普通用户本人的成长助手；管理身份优先且不开放',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code=item.permission_code;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF item.parent_code IS NULL THEN
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            parent_menu_id := root_id;
        -- 前面条件都不成立时，采用这里的备用值。
        ELSE
            -- 读取后面列出的字段或计算结果。
            SELECT id INTO parent_menu_id FROM identity_menu WHERE code=item.parent_code;
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,
            -- 继续列出本次使用的字段：权限编号（permission_id）、排序顺序（sort_order）、是否是否显示（is_visible）、是否builtin（is_builtin）。
            permission_id,sort_order,is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.menu_code,parent_menu_id,item.menu_name,item.menu_type,item.route_path,
            -- 继续列出本次使用的字段：单条数据中的component键（item.component_key）、权限编号值（permission_id_value）、单条数据中的排序顺序（item.sort_order）、不成立（false）、成立（true）。
            item.component_key,permission_id_value,item.sort_order,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code=item.menu_code AND (
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type。
            m.parent_id IS DISTINCT FROM parent_menu_id OR m.menu_type IS DISTINCT FROM item.menu_type
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR m.route_path IS DISTINCT FROM item.route_path OR m.component_key IS DISTINCT FROM item.component_key
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR m.permission_id IS DISTINCT FROM permission_id_value
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：)) THEN。
        )) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Existing Agent menu does not match the reserved contract';
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);

    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
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
                OR is_visible IS DISTINCT FROM (menu_type='PAGE' AND (frontend_ready OR is_visible))) RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$agent_authorization$;。
$agent_authorization$;
-- END AGENT AUTHORIZATION

-- BEGIN AGENT CONVERSATION AUTHORIZATION
-- P1.2 独立准备态；P1.1 的 ready 开关绝不提前启用尚未实现的会话操作。
-- SQL 与后端验收后，才在维护事务设置 asterway.agent_conversation_backend_ready='true'。
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $agent_conversation_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.agent_conversation_backend_ready',true),''),'false')::boolean;
    -- page_id 保存当前页编号；类型为大整数。
    page_id bigint;
    -- permission_id_value 保存权限编号值；类型为大整数。
    permission_id_value bigint;
    -- permission_codes 保存权限codes；类型为不限固定长度的文字。
    permission_codes text[] := ARRAY[]::text[];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY[]::text[];
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：item record;。
    item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO page_id FROM identity_menu
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        -- 再增加一个必须同时满足的筛选条件。
        AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF page_id IS NULL OR NOT EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Built-in USER role and implemented Agent page are required';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready AND (to_regclass('agent.conversation') IS NULL
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR to_regclass('agent.conversation_turn') IS NULL OR to_regclass('agent.conversation_message') IS NULL) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Agent conversation tables must exist before backend authorization';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('list','查询会话列表',10),('read','读取会话与消息',20),('create','创建会话',30),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('update','修改会话标题',40),('close','关闭会话',50),('delete','逻辑删除会话',60)
    -- 继续列出本次使用的字段：名称（name）。
    ) AS entries(action,name,sort_order)
    -- 继续列出本次使用的字段：LOOP。
    LOOP
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        permission_codes := array_append(permission_codes,'agent:conversation:' || item.action);
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        menu_codes := array_append(menu_codes,'growth_agent_conversation_' || item.action);
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code='agent:conversation:' || item.action
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_builtin OR deleted_at IS NOT NULL))
            -- 提供另一个可满足的分支条件，任一分支成立即可。
            OR EXISTS (SELECT 1 FROM identity_menu WHERE code='growth_agent_conversation_' || item.action
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Reserved conversation code conflicts with a custom or deleted object';
        -- 结束前面的条件表达式、循环或数据库程序块。
        END IF;
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('agent:conversation:' || item.action,item.name,'仅普通用户本人会话；P1.2 后端验收后启用',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:conversation:' || item.action;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            -- 继续列出本次使用的字段：是否是否显示（is_visible）、是否builtin（is_builtin）。
            is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('growth_agent_conversation_' || item.action,page_id,item.name,'BUTTON',
            -- 继续列出本次使用的字段：权限编号值（permission_id_value）、单条数据中的排序顺序（item.sort_order）、不成立（false）、成立（true）。
            permission_id_value,item.sort_order,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_menu m WHERE m.code='growth_agent_conversation_' || item.action
            -- 再增加一个必须同时满足的筛选条件。
            AND (m.parent_id IS DISTINCT FROM page_id OR m.menu_type<>'BUTTON'
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR m.permission_id IS DISTINCT FROM permission_id_value
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR m.route_path IS NOT NULL OR m.component_key IS NOT NULL)) THEN
            -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
            RAISE EXCEPTION 'Existing conversation menu does not match the reserved contract';
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$agent_conversation_authorization$;。
$agent_conversation_authorization$;
-- END AGENT CONVERSATION AUTHORIZATION

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;

-- =====================================================================
-- P1.4.0 记忆、知识库与后台任务 SQL
-- 新库：在会话三表段之后顺序执行本段；已有 P1.2/P1.3 库：只单独执行本段。
-- 本段保留历史数组向量列；后续 COMPLETION 段增加 pgvector(1024) 及不可变文档版本。
-- =====================================================================
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT P1.4 MEMORY SCHEMA
-- 创建 agent.memory 表；长期记忆；候选需用户确认后才跨会话使用，逻辑删除保留审计，不存密码或令牌。
CREATE TABLE agent.memory (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不接受客户端或模型指定；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- kind 保存偏好、事实、约束或目标背景；仅作为对话上下文，不作为业务授权依据；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    kind varchar(16) NOT NULL CHECK (kind IN ('PREFERENCE','FACT','CONSTRAINT','GOAL_CONTEXT')),
    -- content 保存用户最多 8000 字，助手和工具最多 65536 字；工具结果只保存业务允许返回的脱敏内容；类型为不限固定长度的文字；必须填写，保存时检查后面的限制。
    content text NOT NULL CHECK (content ~ '[^[:space:]]' AND char_length(content) <= 4000),
    -- source 保存来源；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    source varchar(16) NOT NULL DEFAULT 'INFERRED'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (source IN ('USER_CONFIRMED','USER_STATED','INFERRED')),
    -- status 保存CANDIDATE 候选、CONFIRMED 已确认、REJECTED 已拒绝、ARCHIVED 已归档；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(16) NOT NULL DEFAULT 'CANDIDATE'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('CANDIDATE','CONFIRMED','REJECTED','ARCHIVED')),
    -- confidence 保存0～1 置信度；仅辅助排序，不替代用户确认；类型为精确小数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    confidence numeric(3,2) NOT NULL DEFAULT 0.60 CHECK (confidence >= 0 AND confidence <= 1),
    -- source_conversation_id 保存来源对话编号；类型为大整数。
    source_conversation_id bigint,
    -- source_turn_id 保存来源本轮对话编号；类型为大整数。
    source_turn_id bigint,
    -- supersedes_id 保存本条替代的旧记忆逻辑引用；不级联删除；类型为大整数。
    supersedes_id bigint,
    -- confirmed_at 保存已确认at；类型为带时区的时间点。
    confirmed_at timestamptz(3),
    -- rejected_at 保存rejectedat；类型为带时区的时间点。
    rejected_at timestamptz(3),
    -- archived_at 保存归档时间，NULL 表示未归档；类型为带时区的时间点。
    archived_at timestamptz(3),
    -- deleted_at 保存逻辑删除时间；删除后所有接口按不存在处理，不清空历史事实；类型为带时区的时间点。
    deleted_at timestamptz(3),
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_memory_supersedes CHECK (supersedes_id IS NULL OR supersedes_id <> id),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_memory_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'CANDIDATE' AND confirmed_at IS NULL AND rejected_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND rejected_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'REJECTED' AND rejected_at IS NOT NULL AND confirmed_at IS NULL AND archived_at IS NULL AND deleted_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL AND deleted_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR deleted_at IS NOT NULL
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_memory_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (rejected_at IS NULL OR (isfinite(rejected_at) AND rejected_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (archived_at IS NULL OR (isfinite(archived_at) AND archived_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at >= created_at AND deleted_at <= updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.memory IS '长期记忆；候选需用户确认后才跨会话使用，逻辑删除保留审计，不存密码或令牌';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory.user_id IS 'Java 验证得到的所属用户，不接受客户端或模型指定';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory.kind IS '偏好、事实、约束或目标背景；仅作为对话上下文，不作为业务授权依据';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory.status IS 'CANDIDATE 候选、CONFIRMED 已确认、REJECTED 已拒绝、ARCHIVED 已归档';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory.confidence IS '0～1 置信度；仅辅助排序，不替代用户确认';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory.supersedes_id IS '本条替代的旧记忆逻辑引用；不级联删除';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_memory_user_status ON agent.memory(user_id, status, updated_at DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_memory_user_kind ON agent.memory(user_id, kind, updated_at DESC);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_memory_confirmed_content
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON agent.memory(user_id, md5(content))。
    ON agent.memory(user_id, md5(content))
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE status = 'CONFIRMED' AND deleted_at IS NULL;

-- 创建 agent.memory_embedding 表；已确认记忆的向量；一条记忆当前一条向量，模型变更时更新该行。
CREATE TABLE agent.memory_embedding (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- memory_id 保存长期记忆编号；类型为大整数；必须填写。
    memory_id bigint NOT NULL,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- embedding_model 保存向量模型；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    embedding_model varchar(100) NOT NULL CHECK (embedding_model ~ '[^[:space:]]'),
    -- dimension 保存dimension；类型为整数；必须填写，保存时检查后面的限制。
    dimension integer NOT NULL CHECK (dimension > 0 AND dimension <= 8192),
    -- embedding 保存double precision 数组，维度与 dimension 一致；不依赖 pgvector 扩展；类型为double；必须填写。
    embedding double precision[] NOT NULL,
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_memory_embedding_memory UNIQUE (memory_id),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_memory_embedding_shape CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        cardinality(embedding) = dimension AND array_ndims(embedding) = 1
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_memory_embedding_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.memory_embedding IS '已确认记忆的向量；一条记忆当前一条向量，模型变更时更新该行';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.memory_embedding.embedding IS 'double precision 数组，维度与 dimension 一致；不依赖 pgvector 扩展';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_memory_embedding_user ON agent.memory_embedding(user_id);

-- 创建 agent.knowledge_document 表；用户上传的知识文档元数据；文件本体不入库，只存受控存储键与校验和。
CREATE TABLE agent.knowledge_document (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- title 保存可修改的会话标题，1～100 字，空白标题无效；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    title varchar(200) NOT NULL CHECK (title ~ '[^[:space:]]'),
    -- file_name 保存文件名称；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    file_name varchar(255) NOT NULL CHECK (file_name ~ '[^[:space:]]'),
    -- mime_type 保存mime类型；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    mime_type varchar(100) NOT NULL CHECK (mime_type ~ '[^[:space:]]'),
    -- size_bytes 保存每页条数bytes；类型为大整数；必须填写，保存时检查后面的限制。
    size_bytes bigint NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 26214400),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checksum varchar(64) NOT NULL CHECK (checksum ~ '^[0-9a-f]{64}$'),
    -- storage_key 保存服务端生成的文件标识，不直接暴露给用户或模型；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    storage_key varchar(500) NOT NULL CHECK (storage_key ~ '[^[:space:]]'),
    -- status 保存UPLOADED 已上传、PARSING 解析中、READY 可检索、FAILED 失败、DELETED 已删除；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(16) NOT NULL DEFAULT 'UPLOADED'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('UPLOADED','PARSING','READY','FAILED','DELETED')),
    -- chunk_count 保存文档片段数量；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    chunk_count integer NOT NULL DEFAULT 0 CHECK (chunk_count >= 0),
    -- error_message 保存错误消息；类型为有长度限制的文字。
    error_message varchar(1000),
    -- deleted_at 保存逻辑删除时间；删除后所有接口按不存在处理，不清空历史事实；类型为带时区的时间点。
    deleted_at timestamptz(3),
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_knowledge_document_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'FAILED' AND error_message ~ '[^[:space:]]' AND deleted_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'DELETED' AND deleted_at IS NOT NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('UPLOADED','PARSING','READY') AND deleted_at IS NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_knowledge_document_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (deleted_at IS NULL OR (isfinite(deleted_at) AND deleted_at BETWEEN created_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.knowledge_document IS '用户上传的知识文档元数据；文件本体不入库，只存受控存储键与校验和';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.knowledge_document.storage_key IS '服务端生成的文件标识，不直接暴露给用户或模型';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.knowledge_document.status IS 'UPLOADED 已上传、PARSING 解析中、READY 可检索、FAILED 失败、DELETED 已删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.knowledge_document.checksum IS 'sha256 十六进制，用于同用户去重和重复上传识别';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_knowledge_document_user_status ON agent.knowledge_document(user_id, status, created_at DESC);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_knowledge_document_user_checksum
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON agent.knowledge_document(user_id, checksum) WHERE deleted_at IS NULL;。
    ON agent.knowledge_document(user_id, checksum) WHERE deleted_at IS NULL;

-- 创建 agent.knowledge_chunk 表；文档分块与向量；按文档顺序保留，删除文档时为逻辑删除而不物理丢弃历史。
CREATE TABLE agent.knowledge_chunk (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- document_id 保存知识文档编号；类型为大整数；必须填写。
    document_id bigint NOT NULL,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- chunk_no 保存文档片段no；类型为整数；必须填写，保存时检查后面的限制。
    chunk_no integer NOT NULL CHECK (chunk_no >= 0),
    -- content 保存分块正文，最多 8000 字；原文只用于检索和引用；类型为不限固定长度的文字；必须填写，保存时检查后面的限制。
    content text NOT NULL CHECK (content ~ '[^[:space:]]' AND char_length(content) <= 8000),
    -- token_count 保存身份凭据数量；类型为整数；必须填写，保存时检查后面的限制。
    token_count integer NOT NULL CHECK (token_count > 0 AND token_count <= 8192),
    -- embedding_model 保存向量模型；类型为有长度限制的文字。
    embedding_model varchar(100),
    -- embedding_dimension 保存向量dimension；类型为整数；保存时检查后面的限制。
    embedding_dimension integer CHECK (embedding_dimension > 0 AND embedding_dimension <= 8192),
    -- embedding 保存double precision 数组，未完成向量化时为空；类型为double。
    embedding double precision[],
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_knowledge_chunk_no UNIQUE (document_id, chunk_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_knowledge_chunk_embedding CHECK (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：(embedding IS NULL AND embedding_model IS NULL AND embedding_dimension IS NULL)。
        (embedding IS NULL AND embedding_model IS NULL AND embedding_dimension IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (embedding IS NOT NULL AND embedding_model IS NOT NULL AND embedding_dimension IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND cardinality(embedding) = embedding_dimension AND array_ndims(embedding) = 1)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_knowledge_chunk_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.knowledge_chunk IS '文档分块与向量；按文档顺序保留，删除文档时为逻辑删除而不物理丢弃历史';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.knowledge_chunk.content IS '分块正文，最多 8000 字；原文只用于检索和引用';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.knowledge_chunk.embedding IS 'double precision 数组，未完成向量化时为空';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_knowledge_chunk_user_document ON agent.knowledge_chunk(user_id, document_id, chunk_no);

-- 创建 agent.background_task 表；文档解析/向量化等后台任务；仅记录调度与结果，不存业务事实。
CREATE TABLE agent.background_task (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- kind 保存偏好、事实、约束或目标背景；仅作为对话上下文，不作为业务授权依据；类型为有长度限制的文字；必须填写。
    kind varchar(32) NOT NULL
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING','MEMORY_EMBEDDING')),
    -- ref_type 保存响应式引用类型；类型为有长度限制的文字；必须填写。
    ref_type varchar(32) NOT NULL
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (ref_type IN ('KNOWLEDGE_DOCUMENT','MEMORY')),
    -- ref_id 保存关联文档或记忆的逻辑引用，不建物理外键；类型为大整数；必须填写，保存时检查后面的限制。
    ref_id bigint NOT NULL CHECK (ref_id > 0),
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    -- attempts 保存已尝试次数，最多 20 次，超过后由服务置为失败；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0 AND attempts <= 20),
    -- error_code 保存可公开错误码，不保存上游异常全文、认证头或密钥；类型为有长度限制的文字。
    error_code varchar(64),
    -- error_message 保存错误消息；类型为有长度限制的文字。
    error_message varchar(1000),
    -- scheduled_at 保存定时执行at；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    scheduled_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- started_at 保存会话首次开始的服务端时间，恢复时不覆盖；类型为带时区的时间点。
    started_at timestamptz(3),
    -- finished_at 保存最终结束的服务端时间；暂停后结束不会增加执行时间段；类型为带时区的时间点。
    finished_at timestamptz(3),
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_background_task_ref CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING') AND ref_type = 'KNOWLEDGE_DOCUMENT')
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (kind = 'MEMORY_EMBEDDING' AND ref_type = 'MEMORY')
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_background_task_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL AND error_code IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'SUCCEEDED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_code IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'FAILED' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND error_message ~ '[^[:space:]]')
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'CANCELLED' AND finished_at IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_background_task_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (started_at IS NULL OR (isfinite(started_at) AND started_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.background_task IS '文档解析/向量化等后台任务；仅记录调度与结果，不存业务事实';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.background_task.ref_id IS '关联文档或记忆的逻辑引用，不建物理外键';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.background_task.attempts IS '已尝试次数，最多 20 次，超过后由服务置为失败';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_background_task_due ON agent.background_task(status, scheduled_at, id);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_background_task_active
    -- 继续列出本次使用的字段：响应式引用类型（ref_type）。
    ON agent.background_task(kind, ref_type, ref_id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE status IN ('PENDING','RUNNING');
-- END AGENT P1.4 MEMORY SCHEMA

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;

-- =====================================================================
-- P1.4.0 记忆与知识库权限（准备态，默认禁用隐藏）
-- P1.4.1/P1.4.2 后端实现并通过验收后，才在维护事务设置：
-- SET LOCAL asterway.agent_memory_backend_ready='true';
-- =====================================================================
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT MEMORY AUTHORIZATION
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $agent_memory_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- page_id 保存当前页编号；类型为大整数。
    page_id bigint;
    -- permission_id_value 保存权限编号值；类型为大整数。
    permission_id_value bigint;
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(current_setting('asterway.agent_memory_backend_ready', true) = 'true', false);
    -- permission_codes 保存权限codes；类型为不限固定长度的文字。
    permission_codes text[] := ARRAY[
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'agent:memory:read','agent:memory:manage',。
        'agent:memory:read','agent:memory:manage',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'agent:knowledge:read','agent:knowledge:manage'。
        'agent:knowledge:read','agent:knowledge:manage'
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：];。
    ];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY[
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'growth_agent_memory_read','growth_agent_memory_manage',。
        'growth_agent_memory_read','growth_agent_memory_manage',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'growth_agent_knowledge_read','growth_agent_knowledge_manage'。
        'growth_agent_knowledge_read','growth_agent_knowledge_manage'
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：];。
    ];
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：item record;。
    item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO page_id FROM identity_menu
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        -- 再增加一个必须同时满足的筛选条件。
        AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF page_id IS NULL OR NOT EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Agent page or USER role is missing; run P1.1 authorization first';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        -- 再增加一个必须同时满足的筛选条件。
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Reserved memory/knowledge code conflicts with a custom or deleted object';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('read','查看长期记忆与知识库'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('manage','确认、编辑或删除长期记忆，上传或删除知识文档')
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) AS entries(action,name)。
    ) AS entries(action,name)
    -- 继续列出本次使用的字段：LOOP。
    LOOP
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('agent:memory:' || item.action,item.name,'仅普通用户本人记忆；P1.4 后端验收后启用',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:memory:' || item.action;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            -- 继续列出本次使用的字段：是否是否显示（is_visible）、是否builtin（is_builtin）。
            is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('growth_agent_memory_' || item.action,page_id,item.name,'BUTTON',
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            permission_id_value,CASE WHEN item.action = 'manage' THEN 20 ELSE 19 END,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;

        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('agent:knowledge:' || item.action,item.name,'仅普通用户本人知识库；P1.4 后端验收后启用',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:knowledge:' || item.action;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            -- 继续列出本次使用的字段：是否是否显示（is_visible）、是否builtin（is_builtin）。
            is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('growth_agent_knowledge_' || item.action,page_id,item.name,'BUTTON',
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            permission_id_value,CASE WHEN item.action = 'manage' THEN 22 ELSE 21 END,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$agent_memory_authorization$;。
$agent_memory_authorization$;
-- END AGENT MEMORY AUTHORIZATION

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;

-- =====================================================================
-- P1.5.0 工作流、方案版本、事件与 Java 幂等提交 SQL
-- 新库：在 P1.4 段之后顺序执行；已有库：只单独执行本段。
-- 方案快照存 JSONB，由 Python 工作流引擎生成；Java 只在确认后按快照原子提交业务。
-- =====================================================================
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT P1.5 WORKFLOW SCHEMA
-- 创建 agent.workflow_run 表；一次对话内的工作流运行；方案先确认后执行，过期或取消后不再提交业务。
CREATE TABLE agent.workflow_run (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不接受客户端或模型指定；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- conversation_id 保存所属会话逻辑标识；必须与 user_id 同属当前用户，由 Service 同事务核对；类型为大整数；必须填写，保存时检查后面的限制。
    conversation_id bigint NOT NULL CHECK (conversation_id > 0),
    -- source_turn_id 保存来源本轮对话编号；类型为大整数。
    source_turn_id bigint,
    -- intent 保存intent；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    intent varchar(32) NOT NULL CHECK (intent IN (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'GOAL_PLAN','STAGE_PLAN','TASK_PLAN','WEEKLY_PLAN','DAILY_PLAN','HABIT_PLAN','COMPREHENSIVE_PLAN'。
        'GOAL_PLAN','STAGE_PLAN','TASK_PLAN','WEEKLY_PLAN','DAILY_PLAN','HABIT_PLAN','COMPREHENSIVE_PLAN'
    )),
    -- status 保存草稿、待确认、已确认、执行中、成功、失败、取消、过期；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    status varchar(24) NOT NULL DEFAULT 'DRAFT' CHECK (status IN (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'DRAFT','WAITING_CONFIRM','CONFIRMED','EXECUTING','SUCCEEDED','FAILED','CANCELLED','EXPIRED'。
        'DRAFT','WAITING_CONFIRM','CONFIRMED','EXECUTING','SUCCEEDED','FAILED','CANCELLED','EXPIRED'
    )),
    -- state_version 保存当前状态版本号；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    state_version integer NOT NULL DEFAULT 0 CHECK (state_version >= 0),
    -- latest_plan_version_id 保存最新方案版本号编号；类型为大整数。
    latest_plan_version_id bigint,
    -- graph_state 保存工作流中间状态，只存编排数据，不存业务事实或令牌；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    graph_state jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(graph_state) = 'object'),
    -- expires_at 保存方案确认与提交的失效时间；过期后不执行提交；类型为带时区的时间点；必须填写。
    expires_at timestamptz(3) NOT NULL,
    -- confirmed_at 保存已确认at；类型为带时区的时间点。
    confirmed_at timestamptz(3),
    -- finished_at 保存最终结束的服务端时间；暂停后结束不会增加执行时间段；类型为带时区的时间点。
    finished_at timestamptz(3),
    -- error_code 保存可公开错误码，不保存上游异常全文、认证头或密钥；类型为有长度限制的文字。
    error_code varchar(64),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_workflow_run_status CHECK (
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        (status IN ('DRAFT','WAITING_CONFIRM') AND confirmed_at IS NULL AND finished_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('CONFIRMED','EXECUTING') AND confirmed_at IS NOT NULL AND finished_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status IN ('SUCCEEDED','FAILED') AND confirmed_at IS NOT NULL AND finished_at IS NOT NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'CANCELLED' AND finished_at IS NOT NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'EXPIRED' AND finished_at IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_workflow_run_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND isfinite(expires_at) AND expires_at > created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.workflow_run IS '一次对话内的工作流运行；方案先确认后执行，过期或取消后不再提交业务';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.workflow_run.user_id IS 'Java 验证得到的所属用户，不接受客户端或模型指定';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.workflow_run.status IS '草稿、待确认、已确认、执行中、成功、失败、取消、过期';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.workflow_run.graph_state IS '工作流中间状态，只存编排数据，不存业务事实或令牌';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.workflow_run.expires_at IS '方案确认与提交的失效时间；过期后不执行提交';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_workflow_run_user ON agent.workflow_run(user_id, status, updated_at DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_workflow_run_conversation ON agent.workflow_run(conversation_id, updated_at DESC);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_workflow_run_active
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ON agent.workflow_run(conversation_id)。
    ON agent.workflow_run(conversation_id)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE status IN ('DRAFT','WAITING_CONFIRM','CONFIRMED','EXECUTING');

-- 创建 agent.plan_version 表；方案不可变版本；修订生成新版本，旧版本标记被替代，确认后快照不再修改。
CREATE TABLE agent.plan_version (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- run_id 保存方案运行编号；类型为大整数；必须填写，保存时检查后面的限制。
    run_id bigint NOT NULL CHECK (run_id > 0),
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- version_no 保存版本号no；类型为整数；必须填写，保存时检查后面的限制。
    version_no integer NOT NULL CHECK (version_no > 0),
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(24) NOT NULL DEFAULT 'DRAFT'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('DRAFT','WAITING_CONFIRM','CONFIRMED','SUPERSEDED','REJECTED','EXPIRED')),
    -- summary 保存汇总；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    summary varchar(1000) NOT NULL CHECK (summary ~ '[^[:space:]]'),
    -- snapshot 保存结构化方案快照（目标/阶段/任务/计划/习惯），Java 提交只读取已确认快照；类型为可按字段查询的 JSON；必须填写，保存时检查后面的限制。
    snapshot jsonb NOT NULL CHECK (jsonb_typeof(snapshot) = 'object'),
    -- diff_summary 保存diff汇总；类型为可按字段查询的 JSON。
    diff_summary jsonb,
    -- confirmation_token 保存确认令牌 sha256，仅内部传递，不写入日志或返回浏览器；类型为有长度限制的文字；保存时检查后面的限制。
    confirmation_token varchar(64) CHECK (confirmation_token IS NULL OR confirmation_token ~ '^[0-9a-f]{64}$'),
    -- confirmed_at 保存已确认at；类型为带时区的时间点。
    confirmed_at timestamptz(3),
    -- superseded_at 保存supersededat；类型为带时区的时间点。
    superseded_at timestamptz(3),
    -- expires_at 保存方案确认与提交的失效时间；过期后不执行提交；类型为带时区的时间点；必须填写。
    expires_at timestamptz(3) NOT NULL,
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_plan_version_no UNIQUE (run_id, version_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_plan_version_confirm CHECK (
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        (status IN ('DRAFT','WAITING_CONFIRM','REJECTED','EXPIRED') AND confirmed_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'SUPERSEDED' AND superseded_at IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND ((confirmed_at IS NULL AND confirmation_token IS NULL)
                -- 提供另一个可满足的分支条件，任一分支成立即可。
                OR (confirmed_at IS NOT NULL AND confirmation_token IS NOT NULL)))
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND confirmation_token IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_plan_version_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND isfinite(expires_at) AND expires_at > created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (confirmed_at IS NULL OR (isfinite(confirmed_at) AND confirmed_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (superseded_at IS NULL OR (isfinite(superseded_at) AND superseded_at BETWEEN created_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.plan_version IS '方案不可变版本；修订生成新版本，旧版本标记被替代，确认后快照不再修改';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.plan_version.snapshot IS '结构化方案快照（目标/阶段/任务/计划/习惯），Java 提交只读取已确认快照';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.plan_version.confirmation_token IS '确认令牌 sha256，仅内部传递，不写入日志或返回浏览器';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_plan_version_run ON agent.plan_version(run_id, version_no DESC);
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX uk_agent_plan_version_one_confirmed ON agent.plan_version(run_id) WHERE status = 'CONFIRMED';

-- 创建 agent.workflow_event 表；工作流事件流；只追加不修改，用于恢复、展示与审计，不存敏感令牌。
CREATE TABLE agent.workflow_event (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- run_id 保存方案运行编号；类型为大整数；必须填写，保存时检查后面的限制。
    run_id bigint NOT NULL CHECK (run_id > 0),
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- event_no 保存事件no；类型为大整数；必须填写，保存时检查后面的限制。
    event_no bigint NOT NULL CHECK (event_no > 0),
    -- event_type 保存真实业务事件类型；BASELINE 为上线时当前状态，COMPLETION_IMPORTED 仅用于导入可信任务完成时间；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    event_type varchar(32) NOT NULL CHECK (event_type IN (
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'RUN_STARTED','PLAN_DRAFTED','PLAN_REVISED','CONFIRMED','COMMIT_STARTED',。
        'RUN_STARTED','PLAN_DRAFTED','PLAN_REVISED','CONFIRMED','COMMIT_STARTED',
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'COMMIT_SUCCEEDED','COMMIT_FAILED','CANCELLED','EXPIRED'。
        'COMMIT_SUCCEEDED','COMMIT_FAILED','CANCELLED','EXPIRED'
    )),
    -- payload 保存提交数据；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    payload jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(payload) = 'object'),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_workflow_event_no UNIQUE (run_id, event_no),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_workflow_event_time CHECK (isfinite(created_at))
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.workflow_event IS '工作流事件流；只追加不修改，用于恢复、展示与审计，不存敏感令牌';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_workflow_event_run ON agent.workflow_event(run_id, event_no);

-- 创建 agent.plan_commit 表；Java 幂等提交记录；同一方案版本只提交一次，重试返回同一结果，成功/失败均留审计。
CREATE TABLE agent.plan_commit (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- run_id 保存方案运行编号；类型为大整数；必须填写，保存时检查后面的限制。
    run_id bigint NOT NULL CHECK (run_id > 0),
    -- plan_version_id 保存方案版本号编号；类型为大整数；必须填写，保存时检查后面的限制。
    plan_version_id bigint NOT NULL CHECK (plan_version_id > 0),
    -- idempotency_key 保存Java 与 Python 共享的幂等键 sha256；重复请求不得重复创建业务；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    idempotency_key varchar(64) NOT NULL CHECK (idempotency_key ~ '^[0-9a-f]{64}$'),
    -- request_hash 保存规范化请求的 SHA-256；重用 request_id 但会话或内容不同必须返回冲突；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    request_hash varchar(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    status varchar(16) NOT NULL DEFAULT 'PENDING'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (status IN ('PENDING','RUNNING','SUCCEEDED','FAILED')),
    -- attempts 保存已尝试次数，最多 20 次，超过后由服务置为失败；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    attempts integer NOT NULL DEFAULT 0 CHECK (attempts >= 0 AND attempts <= 10),
    -- result 保存Java 返回的创建结果摘要（实体标识与状态），不存敏感信息；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    result jsonb NOT NULL DEFAULT '{}'::jsonb CHECK (jsonb_typeof(result) = 'object'),
    -- error_code 保存可公开错误码，不保存上游异常全文、认证头或密钥；类型为有长度限制的文字。
    error_code varchar(64),
    -- error_message 保存错误消息；类型为有长度限制的文字。
    error_message varchar(1000),
    -- started_at 保存会话首次开始的服务端时间，恢复时不覆盖；类型为带时区的时间点。
    started_at timestamptz(3),
    -- finished_at 保存最终结束的服务端时间；暂停后结束不会增加执行时间段；类型为带时区的时间点。
    finished_at timestamptz(3),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_plan_commit_key UNIQUE (idempotency_key),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_agent_plan_commit_version UNIQUE (plan_version_id),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_plan_commit_status CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (status = 'PENDING' AND started_at IS NULL AND finished_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'RUNNING' AND started_at IS NOT NULL AND finished_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'SUCCEEDED' AND started_at IS NOT NULL AND finished_at IS NOT NULL AND error_code IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (status = 'FAILED' AND started_at IS NOT NULL AND finished_at IS NOT NULL
            -- 再增加一个必须同时满足的筛选条件。
            AND error_message ~ '[^[:space:]]')
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_agent_plan_commit_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (started_at IS NULL OR (isfinite(started_at) AND started_at BETWEEN created_at AND updated_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (finished_at IS NULL OR (isfinite(finished_at) AND finished_at BETWEEN created_at AND updated_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.plan_commit IS 'Java 幂等提交记录；同一方案版本只提交一次，重试返回同一结果，成功/失败均留审计';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.plan_commit.idempotency_key IS 'Java 与 Python 共享的幂等键 sha256；重复请求不得重复创建业务';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN agent.plan_commit.result IS 'Java 返回的创建结果摘要（实体标识与状态），不存敏感信息';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_agent_plan_commit_user ON agent.plan_commit(user_id, created_at DESC);
-- END AGENT P1.5 WORKFLOW SCHEMA

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;

-- =====================================================================
-- P1.5.0 工作流权限（准备态，默认禁用隐藏）
-- P1.5.1—P1.5.5 实现并通过验收后，才在维护事务设置：
-- SET LOCAL asterway.agent_workflow_backend_ready='true';
-- =====================================================================
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';

-- BEGIN AGENT WORKFLOW AUTHORIZATION
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $agent_workflow_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- page_id 保存当前页编号；类型为大整数。
    page_id bigint;
    -- permission_id_value 保存权限编号值；类型为大整数。
    permission_id_value bigint;
    -- backend_ready 保存backend已就绪；类型为真假值。
    backend_ready boolean := COALESCE(current_setting('asterway.agent_workflow_backend_ready', true) = 'true', false);
    -- permission_codes 保存权限codes；类型为不限固定长度的文字。
    permission_codes text[] := ARRAY['agent:workflow:read','agent:workflow:manage'];
    -- menu_codes 保存菜单codes；类型为不限固定长度的文字。
    menu_codes text[] := ARRAY['growth_agent_workflow_read','growth_agent_workflow_manage'];
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：item record;。
    item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO page_id FROM identity_menu
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE code='growth_agent' AND menu_type='PAGE' AND component_key='agent/index'
        -- 再增加一个必须同时满足的筛选条件。
        AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF page_id IS NULL OR NOT EXISTS (
        -- 读取后面列出的字段或计算结果。
        SELECT 1 FROM identity_role WHERE code='USER' AND is_builtin AND deleted_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) THEN。
    ) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Agent page or USER role is missing; run P1.1 authorization first';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF EXISTS (SELECT 1 FROM identity_permission WHERE code=ANY(permission_codes)
        -- 再增加一个必须同时满足的筛选条件。
        AND (NOT is_builtin OR deleted_at IS NOT NULL))
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR EXISTS (SELECT 1 FROM identity_menu WHERE code=ANY(menu_codes)
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_builtin OR deleted_at IS NOT NULL)) THEN
        -- 由数据库明确报告当前检查结果或拒绝继续执行的原因。
        RAISE EXCEPTION 'Reserved workflow code conflicts with a custom or deleted object';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;

    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('read','查看规划方案与工作流状态'),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('manage','确认、取消或重新生成规划方案')
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) AS entries(action,name)。
    ) AS entries(action,name)
    -- 继续列出本次使用的字段：LOOP。
    LOOP
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('agent:workflow:' || item.action,item.name,'仅普通用户本人规划；P1.5 后端验收后启用',true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code='agent:workflow:' || item.action;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,permission_id,sort_order,
            -- 继续列出本次使用的字段：是否是否显示（is_visible）、是否builtin（is_builtin）。
            is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES('growth_agent_workflow_' || item.action,page_id,item.name,'BUTTON',
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            permission_id_value,CASE WHEN item.action = 'manage' THEN 24 ELSE 23 END,false,true,false)
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(code) DO NOTHING;
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
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
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM inserted);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_permission rp USING identity_role r,identity_permission p
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND p.code=ANY(permission_codes) RETURNING rp.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);
    -- 先定义可复用的临时查询结果，后续语句继续引用它。
    WITH removed AS (
        -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
        DELETE FROM identity_role_menu rm USING identity_role r,identity_menu m
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN')
            -- 再增加一个必须同时满足的筛选条件。
            AND m.code=ANY(menu_codes) RETURNING rm.role_id
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE id IN (SELECT role_id FROM removed);

    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF backend_ready THEN
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_permission SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(permission_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rp.role_id FROM identity_role_permission rp JOIN changed c ON c.id=rp.permission_id);
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH changed AS (
            -- 按后续条件修改指定记录；没有被条件选中的记录不变。
            UPDATE identity_menu SET is_enabled=true,version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE code=ANY(menu_codes) AND NOT is_enabled RETURNING id
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        ) UPDATE identity_role SET version=version+1,updated_at=CURRENT_TIMESTAMP
            -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            WHERE id IN (SELECT rm.role_id FROM identity_role_menu rm JOIN changed c ON c.id=rm.menu_id);
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$agent_workflow_authorization$;。
$agent_workflow_authorization$;
-- END AGENT WORKFLOW AUTHORIZATION

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;


-- BEGIN AGENT COMPLETION SCHEMA
-- 手工执行的最终结构补齐段；保留既有数据，不建立迁移版本链。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 设置本次数据库会话或事务使用的参数。
SET LOCAL lock_timeout='5s';
-- 启用这段结构需要的 PostgreSQL 扩展能力。
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
-- 创建 agent.embedding_profile 表；成长助手中的向量个人资料相关记录存放在这张表中。
CREATE TABLE IF NOT EXISTS agent.embedding_profile (
    -- code 保存全历史唯一业务编码；内置编码由应用保护；类型为有长度限制的文字；用于唯一定位记录。
    code varchar(100) PRIMARY KEY,
    -- provider 保存提供器；类型为有长度限制的文字；必须填写。
    provider varchar(32) NOT NULL,
    -- model 保存模型；类型为有长度限制的文字；必须填写。
    model varchar(100) NOT NULL,
    -- dimension 保存dimension；类型为整数；必须填写，保存时检查后面的限制。
    dimension integer NOT NULL CHECK (dimension=1024),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 向 agent.embedding_profile 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO agent.embedding_profile(code,provider,model,dimension)
-- 按 INSERT 声明的字段顺序提供要写入的值。
VALUES ('dashscope:text-embedding-v4:1024','DASHSCOPE','text-embedding-v4',1024)
-- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
ON CONFLICT (code) DO NOTHING;

-- 创建 agent.knowledge_revision 表；不可变原文版本；仅解析和索引状态可推进，文件正文不覆盖。
CREATE TABLE IF NOT EXISTS agent.knowledge_revision (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id>0),
    -- document_id 保存知识文档编号；类型为大整数；必须填写，保存时检查后面的限制。
    document_id bigint NOT NULL CHECK (document_id>0),
    -- revision_no 保存同习惯内从 1 连续递增，唯一；不重叠及连续性由用户锁内维护；类型为整数；必须填写，保存时检查后面的限制。
    revision_no integer NOT NULL CHECK (revision_no>0),
    -- file_name 保存文件名称；类型为有长度限制的文字；必须填写。
    file_name varchar(255) NOT NULL,
    -- mime_type 保存mime类型；类型为有长度限制的文字；必须填写。
    mime_type varchar(100) NOT NULL,
    -- size_bytes 保存每页条数bytes；类型为大整数；必须填写，保存时检查后面的限制。
    size_bytes bigint NOT NULL CHECK (size_bytes>0),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checksum varchar(64) NOT NULL CHECK (checksum ~ '^[a-f0-9]{64}$'),
    -- storage_key 保存服务端生成的文件标识，不直接暴露给用户或模型；类型为有长度限制的文字；必须填写。
    storage_key varchar(500) NOT NULL,
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    status varchar(16) NOT NULL CHECK (status IN ('UPLOADED','PARSING','READY','FAILED')),
    -- index_status 保存索引状态；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    index_status varchar(16) NOT NULL DEFAULT 'PENDING'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (index_status IN ('PENDING','READY','FAILED')),
    -- chunk_count 保存文档片段数量；类型为整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    chunk_count integer NOT NULL DEFAULT 0 CHECK (chunk_count>=0),
    -- error_message 保存错误消息；类型为有长度限制的文字。
    error_message varchar(1000),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：UNIQUE(document_id,revision_no),。
    UNIQUE(document_id,revision_no),
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (isfinite(created_at) AND isfinite(updated_at) AND updated_at>=created_at)
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.knowledge_revision IS '不可变原文版本；仅解析和索引状态可推进，文件正文不覆盖';
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_document ADD COLUMN IF NOT EXISTS current_revision_id bigint;
-- 向 agent.knowledge_revision 写入新记录，字段顺序与后面的值一一对应。
INSERT INTO agent.knowledge_revision(user_id,document_id,revision_no,file_name,mime_type,
    -- 继续列出本次使用的字段：每页条数bytes（size_bytes）、checksum、存储键（storage_key）、状态（status）、文档片段数量（chunk_count）、错误消息（error_message）、创建时at（created_at）。
    size_bytes,checksum,storage_key,status,chunk_count,error_message,created_at,updated_at)
-- 读取后面列出的字段或计算结果。
SELECT user_id,id,1,file_name,mime_type,size_bytes,checksum,storage_key,
    -- 根据后续 WHEN 条件选择本字段最终使用的值。
    CASE WHEN status='DELETED' THEN 'READY' ELSE status END,chunk_count,error_message,created_at,updated_at
-- 指定本次查询主要读取的表或子查询。
FROM agent.knowledge_document d WHERE NOT EXISTS (
    -- 读取后面列出的字段或计算结果。
    SELECT 1 FROM agent.knowledge_revision r WHERE r.document_id=d.id);
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE agent.knowledge_document d SET current_revision_id=r.id
-- 指定本次查询主要读取的表或子查询。
FROM agent.knowledge_revision r WHERE r.document_id=d.id AND r.revision_no=1
    -- 再增加一个必须同时满足的筛选条件。
    AND d.current_revision_id IS NULL;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS revision_id bigint;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS page_number integer CHECK (page_number>0);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS heading varchar(300);
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE agent.knowledge_chunk c SET revision_id=d.current_revision_id
-- 指定本次查询主要读取的表或子查询。
FROM agent.knowledge_document d WHERE d.id=c.document_id AND c.revision_id IS NULL;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk ALTER COLUMN revision_id SET NOT NULL;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk DROP CONSTRAINT IF EXISTS uk_agent_knowledge_chunk_no;
-- 建立这里指定的查询索引，同时阻止指定键值重复。
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_revision_chunk_no ON agent.knowledge_chunk(revision_id,chunk_no);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.knowledge_chunk ADD COLUMN IF NOT EXISTS vector_value public.vector(1024);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.memory_embedding ADD COLUMN IF NOT EXISTS vector_value public.vector(1024);
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE agent.knowledge_chunk SET vector_value=embedding::public.vector(1024)
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE vector_value IS NULL AND embedding_dimension=1024;
-- 按后续条件修改指定记录；没有被条件选中的记录不变。
UPDATE agent.memory_embedding SET vector_value=embedding::public.vector(1024)
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE vector_value IS NULL AND dimension=1024;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX IF NOT EXISTS ix_agent_chunk_vector ON agent.knowledge_chunk
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;。
    USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX IF NOT EXISTS ix_agent_memory_vector ON agent.memory_embedding
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;。
    USING hnsw (vector_value public.vector_cosine_ops) WHERE vector_value IS NOT NULL;
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX IF NOT EXISTS ix_agent_chunk_owner_revision ON agent.knowledge_chunk(user_id,revision_id,id);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.conversation_message ADD COLUMN IF NOT EXISTS citations jsonb NOT NULL DEFAULT '[]';
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.conversation_message ADD COLUMN IF NOT EXISTS used_memory_ids jsonb NOT NULL DEFAULT '[]';

-- 创建 agent.review_report 表；成长助手中的复盘复盘报告相关记录存放在这张表中。
CREATE TABLE IF NOT EXISTS agent.review_report (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id>0),
    -- date_from 保存日期来源；类型为有长度限制的文字；必须填写。
    date_from varchar(10) NOT NULL, date_to varchar(10) NOT NULL,
    -- time_zone 保存本段开始时使用的时区；IANA 有效性由应用校验，历史查询按查询时有效时区切日；类型为有长度限制的文字；必须填写。
    time_zone varchar(64) NOT NULL,
    -- status 保存RUNNING 生成中，COMPLETED 完整结束，FAILED 失败，CANCELLED 取消，INCOMPLETE 截断；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    status varchar(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','RUNNING','READY','FAILED')),
    -- facts 保存facts；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    facts jsonb NOT NULL DEFAULT '{}' CHECK (jsonb_typeof(facts)='object'),
    -- content 保存用户最多 8000 字，助手和工具最多 65536 字；工具结果只保存业务允许返回的脱敏内容；类型为不限固定长度的文字；必须填写，未传值时使用这里的默认值。
    content text NOT NULL DEFAULT '', error_message varchar(1000),
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间，由应用维护，无自动更新时间触发器；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    CHECK (date_from<=date_to AND updated_at>=created_at)
);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX IF NOT EXISTS ix_agent_review_owner ON agent.review_report(user_id,id DESC);
-- 创建 agent.call_record 表；调用用量与耗时；不记录提问正文、令牌或密钥，未知用量为 NULL。
CREATE TABLE IF NOT EXISTS agent.call_record (
    -- id 保存会话主键；对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存Java 验证得到的所属用户，不从客户端请求体或模型参数读取；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id>0), trace_id varchar(64) NOT NULL,
    -- kind 保存偏好、事实、约束或目标背景；仅作为对话上下文，不作为业务授权依据；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    kind varchar(16) NOT NULL CHECK (kind IN ('MODEL','TOOL','EMBEDDING')),
    -- name 保存名称，1～100 字且不能仅为空白；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    name varchar(128) NOT NULL, status varchar(16) NOT NULL CHECK (status IN ('SUCCEEDED','FAILED','CANCELLED')),
    -- duration_ms 保存持续时间ms；类型为大整数；必须填写，保存时检查后面的限制。
    duration_ms bigint NOT NULL CHECK (duration_ms>=0),
    -- first_token_ms 保存首次身份凭据ms；类型为大整数；保存时检查后面的限制。
    first_token_ms bigint CHECK (first_token_ms>=0),
    -- prompt_tokens 保存模型提示词凭据处理器；类型为大整数；保存时检查后面的限制。
    prompt_tokens bigint CHECK (prompt_tokens>=0), completion_tokens bigint CHECK (completion_tokens>=0),
    -- error_code 保存可公开错误码，不保存上游异常全文、认证头或密钥；类型为有长度限制的文字。
    error_code varchar(64), conversation_id bigint, turn_id bigint, run_id bigint,
    -- created_at 保存创建时间，毫秒精度；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE agent.call_record IS '调用用量与耗时；不记录提问正文、令牌或密钥，未知用量为 NULL';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX IF NOT EXISTS ix_agent_call_owner_date ON agent.call_record(user_id,created_at DESC,id DESC);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS background_task_kind_check;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS background_task_ref_type_check;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task DROP CONSTRAINT IF EXISTS ck_agent_background_task_ref;
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task ADD CONSTRAINT background_task_kind_check CHECK (
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING','MEMORY_EMBEDDING','MEMORY_EXTRACT','REVIEW_GENERATE'));。
    kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING','MEMORY_EMBEDDING','MEMORY_EXTRACT','REVIEW_GENERATE'));
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task ADD CONSTRAINT background_task_ref_type_check CHECK (
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：ref_type IN ('KNOWLEDGE_DOCUMENT','MEMORY','CONVERSATION_TURN','REVIEW_REPORT'));。
    ref_type IN ('KNOWLEDGE_DOCUMENT','MEMORY','CONVERSATION_TURN','REVIEW_REPORT'));
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.background_task ADD CONSTRAINT ck_agent_background_task_ref CHECK (
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (kind IN ('DOCUMENT_PARSE','DOCUMENT_EMBEDDING') AND ref_type='KNOWLEDGE_DOCUMENT') OR
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (kind='MEMORY_EMBEDDING' AND ref_type='MEMORY') OR
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (kind='MEMORY_EXTRACT' AND ref_type='CONVERSATION_TURN') OR
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (kind='REVIEW_GENERATE' AND ref_type='REVIEW_REPORT'));
-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
-- END AGENT COMPLETION SCHEMA

-- BEGIN AGENT CHECKPOINT SCHEMA
-- langgraph-checkpoint-postgres 3.2.0 最终运行表；不调用 setup，不建立迁移表。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;
-- 创建 agent.checkpoints 表；成长助手中的checkpoints相关记录存放在这张表中。
CREATE TABLE IF NOT EXISTS agent.checkpoints (
    -- thread_id 保存thread编号；类型为不限固定长度的文字；必须填写。
    thread_id TEXT NOT NULL,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint_id TEXT NOT NULL,
    -- parent_checkpoint_id 保存上级恢复检查点编号；类型为不限固定长度的文字；保存时检查后面的限制。
    parent_checkpoint_id TEXT,
    -- type 保存类型；类型为不限固定长度的文字。
    type TEXT,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint JSONB NOT NULL,
    -- metadata 保存附加说明；类型为可按字段查询的 JSON；必须填写，未传值时使用这里的默认值。
    metadata JSONB NOT NULL DEFAULT '{}',
    -- 继续列出本次使用的字段：恢复检查点ns（checkpoint_ns）。
    PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id)
);
-- 创建 agent.checkpoint_blobs 表；成长助手中的恢复检查点blobs相关记录存放在这张表中。
CREATE TABLE IF NOT EXISTS agent.checkpoint_blobs (
    -- thread_id 保存thread编号；类型为不限固定长度的文字；必须填写。
    thread_id TEXT NOT NULL,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    -- channel 保存channel；类型为不限固定长度的文字；必须填写。
    channel TEXT NOT NULL,
    -- version 保存标题、关闭、删除、摘要及序号分配等会话更新的乐观锁版本；类型为不限固定长度的文字；必须填写。
    version TEXT NOT NULL,
    -- type 保存类型；类型为不限固定长度的文字；必须填写。
    type TEXT NOT NULL,
    -- blob 保存blob；类型为原始字节。
    blob BYTEA,
    -- 继续列出本次使用的字段：恢复检查点ns（checkpoint_ns）、channel。
    PRIMARY KEY (thread_id, checkpoint_ns, channel, version)
);
-- 创建 agent.checkpoint_writes 表；成长助手中的恢复检查点writes相关记录存放在这张表中。
CREATE TABLE IF NOT EXISTS agent.checkpoint_writes (
    -- thread_id 保存thread编号；类型为不限固定长度的文字；必须填写。
    thread_id TEXT NOT NULL,
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint_ns TEXT NOT NULL DEFAULT '',
    -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
    checkpoint_id TEXT NOT NULL,
    -- task_id 保存任务逻辑标识；任务逻辑删除后保留历史引用；类型为不限固定长度的文字；必须填写。
    task_id TEXT NOT NULL,
    -- idx 保存idx；类型为整数；必须填写。
    idx INTEGER NOT NULL,
    -- channel 保存channel；类型为不限固定长度的文字；必须填写。
    channel TEXT NOT NULL,
    -- type 保存类型；类型为不限固定长度的文字。
    type TEXT,
    -- blob 保存blob；类型为原始字节；必须填写。
    blob BYTEA NOT NULL,
    -- 继续列出本次使用的字段：恢复检查点ns（checkpoint_ns）、恢复检查点编号（checkpoint_id）、任务编号（task_id）。
    PRIMARY KEY (thread_id, checkpoint_ns, checkpoint_id, task_id, idx)
);
-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.checkpoint_blobs ALTER COLUMN blob DROP not null;

    -- 建立这里指定的查询索引，加快对应筛选或排序。
    CREATE INDEX IF NOT EXISTS checkpoints_thread_id_idx ON agent.checkpoints(thread_id);


    -- 建立这里指定的查询索引，加快对应筛选或排序。
    CREATE INDEX IF NOT EXISTS checkpoint_blobs_thread_id_idx ON agent.checkpoint_blobs(thread_id);


    -- 建立这里指定的查询索引，加快对应筛选或排序。
    CREATE INDEX IF NOT EXISTS checkpoint_writes_thread_id_idx ON agent.checkpoint_writes(thread_id);

-- 调整指定表的结构或约束，具体变更按后续子句执行。
ALTER TABLE agent.checkpoint_writes ADD COLUMN IF NOT EXISTS task_path TEXT NOT NULL DEFAULT '';
-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
-- END AGENT CHECKPOINT SCHEMA

-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- BEGIN AGENT WORKSPACE AUTHORIZATION
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $agent_workspace_authorization$
-- 继续列出本次使用的字段：DECLARE。
DECLARE
    -- ready 保存已就绪；类型为真假值。
    ready boolean := COALESCE(current_setting('asterway.agent_workspace_ready',true)='true',false);
    -- root_id 保存根编号；类型为大整数。
    root_id bigint; permission_id_value bigint; item record;
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN
    -- 读取后面列出的字段或计算结果。
    SELECT id INTO root_id FROM identity_menu WHERE code='growth_root' AND is_builtin AND deleted_at IS NULL;
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF root_id IS NULL THEN RAISE EXCEPTION 'Growth directory is missing'; END IF;
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('agent:review:read','查看成长复盘'),('agent:review:manage','生成成长复盘'),('agent:run:read','查看本人运行记录')
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) AS entries(code,name) LOOP。
    ) AS entries(code,name) LOOP
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_permission WHERE code=item.code AND (NOT is_builtin OR deleted_at IS NOT NULL))
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：THEN RAISE EXCEPTION 'Reserved permission conflicts with custom or deleted object'; END IF;。
        THEN RAISE EXCEPTION 'Reserved permission conflicts with custom or deleted object'; END IF;
        -- 向 identity_permission 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_permission(code,name,description,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.code,item.name,'仅普通用户本人使用',true,false) ON CONFLICT(code) DO NOTHING;
    -- 结束前面的条件表达式、循环或数据库程序块。
    END LOOP;
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：FOR item IN SELECT * FROM (VALUES。
    FOR item IN SELECT * FROM (VALUES
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_agent_memories','长期记忆','/agent/memories','agent/memories','agent:memory:read',111),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_agent_knowledge','知识资料','/agent/knowledge','agent/knowledge','agent:knowledge:read',112),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_agent_runs','运行记录','/agent/runs','agent/runs','agent:run:read',113),
        -- 这一行提供一条初始化记录或一个允许的组合，值的顺序对应前面声明的字段。
        ('growth_agent_reviews','成长复盘','/agent/reviews','agent/reviews','agent:review:read',114)
    -- 继续列出本次使用的字段：名称（name）、路径（path）、component、权限（permission）。
    ) AS entries(code,name,path,component,permission,sort_order) LOOP
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF EXISTS (SELECT 1 FROM identity_menu WHERE code=item.code AND (NOT is_builtin OR deleted_at IS NOT NULL))
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：THEN RAISE EXCEPTION 'Reserved menu conflicts with custom or deleted object'; END IF;。
        THEN RAISE EXCEPTION 'Reserved menu conflicts with custom or deleted object'; END IF;
        -- 读取后面列出的字段或计算结果。
        SELECT id INTO permission_id_value FROM identity_permission WHERE code=item.permission AND is_builtin AND deleted_at IS NULL;
        -- 在数据库端按这个条件决定是否继续执行下面的语句。
        IF permission_id_value IS NULL THEN RAISE EXCEPTION 'Workspace permission missing'; END IF;
        -- 向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO identity_menu(code,parent_id,name,menu_type,route_path,component_key,permission_id,
            -- 继续列出本次使用的字段：排序顺序（sort_order）、是否是否显示（is_visible）、是否builtin（is_builtin）。
            sort_order,is_visible,is_builtin,is_enabled)
        -- 按 INSERT 声明的字段顺序提供要写入的值。
        VALUES(item.code,root_id,item.name,'PAGE',item.path,item.component,permission_id_value,
            -- 继续列出本次使用的字段：单条数据中的排序顺序（item.sort_order）、不成立（false）、成立（true）。
            item.sort_order,false,true,false) ON CONFLICT(code) DO NOTHING;
    -- 结束前面的条件表达式、循环或数据库程序块。
    END LOOP;
    -- 向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
    INSERT INTO identity_role_permission(role_id,permission_id)
    -- 读取后面列出的字段或计算结果。
    SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE r.code='USER' AND r.deleted_at IS NULL AND p.code IN ('agent:review:read','agent:review:manage','agent:run:read')
    -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
    ON CONFLICT DO NOTHING;
    -- 向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
    INSERT INTO identity_role_menu(role_id,menu_id)
    -- 读取后面列出的字段或计算结果。
    SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE r.code='USER' AND r.deleted_at IS NULL AND m.code IN ('growth_agent_memories','growth_agent_knowledge','growth_agent_runs','growth_agent_reviews')
    -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
    ON CONFLICT DO NOTHING;
    -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
    DELETE FROM identity_role_permission rp USING identity_role r, identity_permission p
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE rp.role_id=r.id AND rp.permission_id=p.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'agent:%';
    -- 按这里的条件物理删除记录；被删除行不会保留在查询结果中。
    DELETE FROM identity_role_menu rm USING identity_role r, identity_menu m
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE rm.role_id=r.id AND rm.menu_id=m.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND m.code LIKE 'growth_agent%';
    -- 在数据库端按这个条件决定是否继续执行下面的语句。
    IF ready THEN
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_permission SET is_enabled=true, version=version+1, updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE is_builtin AND deleted_at IS NULL AND code LIKE 'agent:%' AND NOT is_enabled;
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_menu SET is_enabled=true, is_visible=(menu_type='PAGE'), version=version+1, updated_at=CURRENT_TIMESTAMP
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE is_builtin AND deleted_at IS NULL AND code LIKE 'growth_agent%'
            -- 再增加一个必须同时满足的筛选条件。
            AND (NOT is_enabled OR (menu_type='PAGE' AND NOT is_visible));
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE identity_role SET version=version+1, updated_at=CURRENT_TIMESTAMP WHERE code='USER';
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END $agent_workspace_authorization$;
-- END AGENT WORKSPACE AUTHORIZATION

-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;

-- 结构阅读补充：完整说明 Agent 表与字段。
-- 补齐 Agent 表和字段的普通结构说明；可以重复执行，不读取或更改用户记录。
BEGIN;
-- 说明 agent.background_task.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.background_task.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.background_task.kind 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.kind IS '后台任务类型，用来选择文档解析、向量化、记忆抽取或复盘处理器';
-- 说明 agent.background_task.ref_type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.ref_type IS '后台任务关联的对象种类，用来确定 ref_id 指向文档、记忆还是其他受控对象';
-- 说明 agent.background_task.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.status IS '当前处理状态；允许值由本列对应的 CHECK 约束限制';
-- 说明 agent.background_task.error_code 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.error_code IS '最近一次失败的稳定错误编码；页面和日志据此区分失败原因';
-- 说明 agent.background_task.error_message 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.error_message IS '供用户阅读的失败原因；不保存密钥、完整上游响应或内部堆栈';
-- 说明 agent.background_task.scheduled_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.scheduled_at IS '允许后台任务被领取执行的最早时间点';
-- 说明 agent.background_task.started_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.started_at IS '本次处理实际开始的时间点；尚未开始时可以为空';
-- 说明 agent.background_task.finished_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.finished_at IS '本次处理进入成功、失败或取消终态的时间点；进行中可以为空';
-- 说明 agent.background_task.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.background_task.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.created_at IS '记录首次创建的时间点';
-- 说明 agent.background_task.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.background_task.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.call_record.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.call_record.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.call_record.trace_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.trace_id IS '贯穿同一次请求的追踪标识，用于把相关调用和错误记录对应起来';
-- 说明 agent.call_record.kind 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.kind IS '调用种类，区分聊天、业务工具、向量化或其他模型处理';
-- 说明 agent.call_record.name 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.name IS '本次调用的模型或操作名称，供运行记录页面区分处理对象';
-- 说明 agent.call_record.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.status IS '当前处理状态；允许值由本列对应的 CHECK 约束限制';
-- 说明 agent.call_record.duration_ms 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.duration_ms IS '本次调用从开始到结束的耗时，单位为毫秒';
-- 说明 agent.call_record.first_token_ms 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.first_token_ms IS '从发起调用到收到第一个输出片段的耗时，单位为毫秒；没有输出时可为空';
-- 说明 agent.call_record.prompt_tokens 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.prompt_tokens IS '上游报告的输入计量数量；没有返回用量信息时可为空';
-- 说明 agent.call_record.completion_tokens 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.completion_tokens IS '上游报告的输出计量数量；没有返回用量信息时可为空';
-- 说明 agent.call_record.error_code 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.error_code IS '最近一次失败的稳定错误编码；页面和日志据此区分失败原因';
-- 说明 agent.call_record.conversation_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.conversation_id IS '所属对话编号，必须与当前用户的数据范围一致';
-- 说明 agent.call_record.turn_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.turn_id IS '所属对话轮次编号；用于把消息或调用归入同一轮处理';
-- 说明 agent.call_record.run_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.run_id IS '所属规划工作流运行编号';
-- 说明 agent.call_record.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.call_record.created_at IS '记录首次创建的时间点';
-- 说明 agent.checkpoint_blobs 保存的内容及其用途。
COMMENT ON TABLE agent.checkpoint_blobs IS '工作流检查点使用的通道值；较大的序列化内容单独保存，避免每次重复存储';
-- 说明 agent.checkpoint_blobs.thread_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.thread_id IS '工作流恢复使用的逻辑线程标识；由用户、运行和方案版本组成';
-- 说明 agent.checkpoint_blobs.checkpoint_ns 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.checkpoint_ns IS '检查点的命名空间，用于隔离主流程与子流程';
-- 说明 agent.checkpoint_blobs.channel 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.channel IS '检查点里某一类状态值的通道名称';
-- 说明 agent.checkpoint_blobs.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.checkpoint_blobs.type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.type IS '该段状态内容的序列化类型，恢复时按相同方式解码';
-- 说明 agent.checkpoint_blobs.blob 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_blobs.blob IS '按 type 指定方式编码的原始字节内容，恢复时由工作流库解码';
-- 说明 agent.checkpoint_writes 保存的内容及其用途。
COMMENT ON TABLE agent.checkpoint_writes IS '工作流节点已经产生但尚待合并的写入；按任务和通道保存，支持中断后的恢复';
-- 说明 agent.checkpoint_writes.thread_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.thread_id IS '工作流恢复使用的逻辑线程标识；由用户、运行和方案版本组成';
-- 说明 agent.checkpoint_writes.checkpoint_ns 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.checkpoint_ns IS '检查点的命名空间，用于隔离主流程与子流程';
-- 说明 agent.checkpoint_writes.checkpoint_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.checkpoint_id IS '本次检查点的唯一标识';
-- 说明 agent.checkpoint_writes.task_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.task_id IS '产生这条检查点写入的工作流节点任务标识，不是成长业务 task 表编号';
-- 说明 agent.checkpoint_writes.idx 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.idx IS '同一个任务产生的写入条目的顺序号';
-- 说明 agent.checkpoint_writes.channel 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.channel IS '检查点里某一类状态值的通道名称';
-- 说明 agent.checkpoint_writes.type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.type IS '该段状态内容的序列化类型，恢复时按相同方式解码';
-- 说明 agent.checkpoint_writes.blob 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.blob IS '按 type 指定方式编码的原始字节内容，恢复时由工作流库解码';
-- 说明 agent.checkpoint_writes.task_path 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoint_writes.task_path IS '产生本条写入的工作流任务路径，供恢复和定位节点使用';
-- 说明 agent.checkpoints 保存的内容及其用途。
COMMENT ON TABLE agent.checkpoints IS '工作流恢复检查点；保存某次运行的状态、前一个检查点和附加信息，重启后从这里继续';
-- 说明 agent.checkpoints.thread_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.thread_id IS '工作流恢复使用的逻辑线程标识；由用户、运行和方案版本组成';
-- 说明 agent.checkpoints.checkpoint_ns 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.checkpoint_ns IS '检查点的命名空间，用于隔离主流程与子流程';
-- 说明 agent.checkpoints.checkpoint_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.checkpoint_id IS '本次检查点的唯一标识';
-- 说明 agent.checkpoints.parent_checkpoint_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.parent_checkpoint_id IS '前一个检查点的标识；没有前置检查点时为空';
-- 说明 agent.checkpoints.type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.type IS '该段状态内容的序列化类型，恢复时按相同方式解码';
-- 说明 agent.checkpoints.checkpoint 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.checkpoint IS '本次检查点的结构化状态，供中断恢复使用';
-- 说明 agent.checkpoints.metadata 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.checkpoints.metadata IS '检查点的附加恢复信息，例如来源步骤；不用于保存登录凭据';
-- 说明 agent.conversation.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation.status IS '对话生命周期状态；历史关闭接口保留，当前界面主要使用活跃与删除流程';
-- 说明 agent.conversation.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation.created_at IS '记录首次创建的时间点';
-- 说明 agent.conversation.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.conversation_message.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.conversation_message.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.conversation_message.conversation_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.conversation_id IS '所属对话编号，必须与当前用户的数据范围一致';
-- 说明 agent.conversation_message.role 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.role IS '消息发送者种类，区分用户、助手和工具结果';
-- 说明 agent.conversation_message.model 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.model IS '本条消息使用的模型名称，或固定向量配置中声明的模型名称';
-- 说明 agent.conversation_message.finish_reason 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.finish_reason IS '模型结束本次输出的原因，例如正常结束、工具调用或达到输出上限';
-- 说明 agent.conversation_message.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.created_at IS '记录首次创建的时间点';
-- 说明 agent.conversation_message.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.conversation_message.citations 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.citations IS '本条回复实际引用的知识资料定位信息，支持按原版本回看';
-- 说明 agent.conversation_message.used_memory_ids 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_message.used_memory_ids IS '本轮实际使用的已确认记忆编号列表，便于解释回答依据';
-- 说明 agent.conversation_turn.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.conversation_turn.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.conversation_turn.started_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.started_at IS '本次处理实际开始的时间点；尚未开始时可以为空';
-- 说明 agent.conversation_turn.finished_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.finished_at IS '本次处理进入成功、失败或取消终态的时间点；进行中可以为空';
-- 说明 agent.conversation_turn.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.created_at IS '记录首次创建的时间点';
-- 说明 agent.conversation_turn.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.conversation_turn.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.embedding_profile 保存的内容及其用途。
COMMENT ON TABLE agent.embedding_profile IS '系统固定的向量接口配置定义；记录提供方、模型和维度，不保存用户文字或向量内容';
-- 说明 agent.embedding_profile.code 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.embedding_profile.code IS '固定向量配置的唯一编码，供系统识别这组模型与维度约定';
-- 说明 agent.embedding_profile.provider 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.embedding_profile.provider IS '向量接口的提供方标识，不包含访问密钥';
-- 说明 agent.embedding_profile.model 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.embedding_profile.model IS '本条消息使用的模型名称，或固定向量配置中声明的模型名称';
-- 说明 agent.embedding_profile.dimension 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.embedding_profile.dimension IS '向量中数字的数量，必须与当前固定配置和向量列维度一致';
-- 说明 agent.embedding_profile.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.embedding_profile.created_at IS '记录首次创建的时间点';
-- 说明 agent.knowledge_chunk.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.knowledge_chunk.document_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.document_id IS '所属知识文档编号；必须与片段或版本的所属用户相符';
-- 说明 agent.knowledge_chunk.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.knowledge_chunk.chunk_no 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.chunk_no IS '片段在同一文档版本中的顺序号，便于按原文顺序阅读';
-- 说明 agent.knowledge_chunk.token_count 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.token_count IS '估算的模型输入计量数量，供检索和上下文预算控制使用';
-- 说明 agent.knowledge_chunk.embedding_model 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.embedding_model IS '生成该向量时实际使用的模型名称，不能与其他模型生成的向量混用';
-- 说明 agent.knowledge_chunk.embedding_dimension 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.embedding_dimension IS '该片段向量的实际维度，写入时必须通过维度校验';
-- 说明 agent.knowledge_chunk.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.created_at IS '记录首次创建的时间点';
-- 说明 agent.knowledge_chunk.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.knowledge_chunk.revision_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.revision_id IS '片段来自的文档修订版本编号；引用回看时据此定位原版本';
-- 说明 agent.knowledge_chunk.page_number 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.page_number IS '片段来源的文档页码；无法确定页码的文本可以为空';
-- 说明 agent.knowledge_chunk.heading 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.heading IS '片段所属的小节标题，供引用定位和用户回看';
-- 说明 agent.knowledge_chunk.vector_value 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_chunk.vector_value IS '可以参与 pgvector 相似度检索的向量值，不是原文内容';
-- 说明 agent.knowledge_document.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.knowledge_document.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.knowledge_document.title 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.title IS '文档展示标题，供列表、检索结果和引用说明使用';
-- 说明 agent.knowledge_document.file_name 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.file_name IS '用户上传时的文件名称，文件内容由受控存储路径管理';
-- 说明 agent.knowledge_document.mime_type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.mime_type IS '文件内容类型，用于选择 PDF、Word 或文本解析方式';
-- 说明 agent.knowledge_document.size_bytes 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.size_bytes IS '文件原始字节数，上传时按配置检查大小上限';
-- 说明 agent.knowledge_document.chunk_count 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.chunk_count IS '当前版本已经解析或索引的片段数量';
-- 说明 agent.knowledge_document.error_message 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.error_message IS '供用户阅读的失败原因；不保存密钥、完整上游响应或内部堆栈';
-- 说明 agent.knowledge_document.deleted_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.deleted_at IS '逻辑删除时间点，未删除时为空；本次从零重置使用物理清空而不是填写此字段';
-- 说明 agent.knowledge_document.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.knowledge_document.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.created_at IS '记录首次创建的时间点';
-- 说明 agent.knowledge_document.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.knowledge_document.current_revision_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_document.current_revision_id IS '文档当前版本编号；替换资料时指向新版本，旧版本仍可回看';
-- 说明 agent.knowledge_revision.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.knowledge_revision.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.knowledge_revision.document_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.document_id IS '所属知识文档编号；必须与片段或版本的所属用户相符';
-- 说明 agent.knowledge_revision.revision_no 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.revision_no IS '同一文档内的修订次序，替换文件时递增';
-- 说明 agent.knowledge_revision.file_name 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.file_name IS '用户上传时的文件名称，文件内容由受控存储路径管理';
-- 说明 agent.knowledge_revision.mime_type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.mime_type IS '文件内容类型，用于选择 PDF、Word 或文本解析方式';
-- 说明 agent.knowledge_revision.size_bytes 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.size_bytes IS '文件原始字节数，上传时按配置检查大小上限';
-- 说明 agent.knowledge_revision.checksum 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.checksum IS '文件内容的校验摘要，用于识别内容是否变化';
-- 说明 agent.knowledge_revision.storage_key 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.storage_key IS '文件在受控知识存储目录里的相对定位键，不接受任意外部文件路径';
-- 说明 agent.knowledge_revision.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.status IS '这个不可变文档版本的解析状态；旧任务不能覆盖新版本结果';
-- 说明 agent.knowledge_revision.index_status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.index_status IS '文档版本的向量索引状态；与文件解析状态分别记录';
-- 说明 agent.knowledge_revision.chunk_count 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.chunk_count IS '当前版本已经解析或索引的片段数量';
-- 说明 agent.knowledge_revision.error_message 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.error_message IS '供用户阅读的失败原因；不保存密钥、完整上游响应或内部堆栈';
-- 说明 agent.knowledge_revision.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.created_at IS '记录首次创建的时间点';
-- 说明 agent.knowledge_revision.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.knowledge_revision.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.memory.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.memory.content 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.content IS '该用户的长期记忆文字；只有确认且未归档、未删除的内容才能作为已认可事实使用';
-- 说明 agent.memory.source 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.source IS '记忆的来源种类，区分用户主动提供与从对话提取';
-- 说明 agent.memory.source_conversation_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.source_conversation_id IS '产生这条记忆或草案的来源对话编号';
-- 说明 agent.memory.source_turn_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.source_turn_id IS '产生这条记忆或草案的来源对话轮次编号';
-- 说明 agent.memory.confirmed_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.confirmed_at IS '用户明确确认这条记忆或方案的时间点，未确认时为空';
-- 说明 agent.memory.rejected_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.rejected_at IS '用户拒绝候选记忆的时间点，未拒绝时为空';
-- 说明 agent.memory.archived_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.archived_at IS '用户归档这条记忆的时间点；归档后不作为活跃记忆注入';
-- 说明 agent.memory.deleted_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.deleted_at IS '逻辑删除时间点，未删除时为空；本次从零重置使用物理清空而不是填写此字段';
-- 说明 agent.memory.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.memory.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.created_at IS '记录首次创建的时间点';
-- 说明 agent.memory.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.memory_embedding.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.memory_embedding.memory_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.memory_id IS '所属长期记忆编号；仅已确认且仍有效的记忆可参与检索';
-- 说明 agent.memory_embedding.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.memory_embedding.embedding_model 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.embedding_model IS '生成该向量时实际使用的模型名称，不能与其他模型生成的向量混用';
-- 说明 agent.memory_embedding.dimension 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.dimension IS '向量中数字的数量，必须与当前固定配置和向量列维度一致';
-- 说明 agent.memory_embedding.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.created_at IS '记录首次创建的时间点';
-- 说明 agent.memory_embedding.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.memory_embedding.vector_value 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.memory_embedding.vector_value IS '可以参与 pgvector 相似度检索的向量值，不是原文内容';
-- 说明 agent.plan_commit.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.plan_commit.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.plan_commit.run_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.run_id IS '所属规划工作流运行编号';
-- 说明 agent.plan_commit.plan_version_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.plan_version_id IS '本次确认或提交所针对的不可变方案版本编号';
-- 说明 agent.plan_commit.request_hash 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.request_hash IS '确认快照的内容摘要；重复提交时必须对应同一份请求内容';
-- 说明 agent.plan_commit.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.status IS '原子提交状态；成功回执复用，失败时按尝试次数与有效期限制重试';
-- 说明 agent.plan_commit.attempts 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.attempts IS '已经尝试提交或执行的次数；最大允许次数由该表约束和业务配置共同限制';
-- 说明 agent.plan_commit.error_code 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.error_code IS '最近一次失败的稳定错误编码；页面和日志据此区分失败原因';
-- 说明 agent.plan_commit.error_message 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.error_message IS '供用户阅读的失败原因；不保存密钥、完整上游响应或内部堆栈';
-- 说明 agent.plan_commit.started_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.started_at IS '本次处理实际开始的时间点；尚未开始时可以为空';
-- 说明 agent.plan_commit.finished_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.finished_at IS '本次处理进入成功、失败或取消终态的时间点；进行中可以为空';
-- 说明 agent.plan_commit.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.created_at IS '记录首次创建的时间点';
-- 说明 agent.plan_commit.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.plan_commit.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_commit.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.plan_version.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.plan_version.run_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.run_id IS '所属规划工作流运行编号';
-- 说明 agent.plan_version.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.plan_version.version_no 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.version_no IS '同一工作流内的方案版本次序，修订时产生新版本而不覆盖旧快照';
-- 说明 agent.plan_version.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.status IS '方案版本状态，区分当前草案、已确认版本以及被后续修订替代的旧版本';
-- 说明 agent.plan_version.summary 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.summary IS '供用户审阅的方案概要，不作为业务提交成功的证明';
-- 说明 agent.plan_version.diff_summary 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.diff_summary IS '本版相对前版的修改概要，帮助用户在确认前核对变化';
-- 说明 agent.plan_version.confirmed_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.confirmed_at IS '用户明确确认这条记忆或方案的时间点，未确认时为空';
-- 说明 agent.plan_version.superseded_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.superseded_at IS '旧方案版本被新版本替代的时间点';
-- 说明 agent.plan_version.expires_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.expires_at IS '方案的确认或提交有效期截止时间，超过后不能继续按原草案写入业务数据';
-- 说明 agent.plan_version.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.created_at IS '记录首次创建的时间点';
-- 说明 agent.plan_version.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.plan_version.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.plan_version.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
-- 说明 agent.review_report 保存的内容及其用途。
COMMENT ON TABLE agent.review_report IS '用户执行复盘；保留统计区间、真实事实快照、生成结果和处理状态，调整草案以此为依据';
-- 说明 agent.review_report.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.review_report.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.review_report.date_from 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.date_from IS '复盘统计区间的开始业务日期，包含当天';
-- 说明 agent.review_report.date_to 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.date_to IS '复盘统计区间的结束业务日期，包含当天';
-- 说明 agent.review_report.time_zone 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.time_zone IS '生成本次复盘时采用的用户时区，保证日期区间有一致含义';
-- 说明 agent.review_report.status 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.status IS '复盘生成状态；只有完成的复盘才能产生待确认的调整草案';
-- 说明 agent.review_report.facts 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.facts IS '生成复盘时固定下来的真实统计和被跟踪方案快照，复盘与调整以这些事实为依据';
-- 说明 agent.review_report.content 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.content IS '已经生成的复盘正文；统计事实单独保存在 facts 中';
-- 说明 agent.review_report.error_message 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.error_message IS '供用户阅读的失败原因；不保存密钥、完整上游响应或内部堆栈';
-- 说明 agent.review_report.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.created_at IS '记录首次创建的时间点';
-- 说明 agent.review_report.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.review_report.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.workflow_event.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.workflow_event.run_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.run_id IS '所属规划工作流运行编号';
-- 说明 agent.workflow_event.user_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.user_id IS '记录所属用户编号；读取和修改时必须核对当前登录用户';
-- 说明 agent.workflow_event.event_no 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.event_no IS '同一工作流内递增的事件序号，用于恢复正确的处理顺序';
-- 说明 agent.workflow_event.event_type 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.event_type IS '本次工作流事件的类型，例如生成、修订、确认、取消或提交完成';
-- 说明 agent.workflow_event.payload 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.payload IS '该事件附带的结构化数据；只解释本次事件，不代替业务表事实';
-- 说明 agent.workflow_event.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_event.created_at IS '记录首次创建的时间点';
-- 说明 agent.workflow_run.id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.id IS '本表记录编号，由数据库生成，用于唯一定位这一行';
-- 说明 agent.workflow_run.conversation_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.conversation_id IS '所属对话编号，必须与当前用户的数据范围一致';
-- 说明 agent.workflow_run.source_turn_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.source_turn_id IS '产生这条记忆或草案的来源对话轮次编号';
-- 说明 agent.workflow_run.intent 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.intent IS '规划种类，例如目标、阶段、任务、周计划、日计划、习惯或综合规划';
-- 说明 agent.workflow_run.state_version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.state_version IS '工作流状态的变化版本，更新时用于防止并发覆盖';
-- 说明 agent.workflow_run.latest_plan_version_id 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.latest_plan_version_id IS '这次工作流目前使用的最新方案版本编号';
-- 说明 agent.workflow_run.confirmed_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.confirmed_at IS '用户明确确认这条记忆或方案的时间点，未确认时为空';
-- 说明 agent.workflow_run.finished_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.finished_at IS '本次处理进入成功、失败或取消终态的时间点；进行中可以为空';
-- 说明 agent.workflow_run.error_code 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.error_code IS '最近一次失败的稳定错误编码；页面和日志据此区分失败原因';
-- 说明 agent.workflow_run.created_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.created_at IS '记录首次创建的时间点';
-- 说明 agent.workflow_run.updated_at 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.updated_at IS '记录最后更新的时间点，由应用写入时维护';
-- 说明 agent.workflow_run.version 的含义；只修改结构说明，不改字段或记录值。
COMMENT ON COLUMN agent.workflow_run.version IS '乐观锁版本号；更新时递增，旧版本请求不能覆盖新结果';
COMMIT;


BEGIN;
-- ASTERWAY_KNOWLEDGE_BASES_BEGIN
-- 多知识库增量；由维护脚本显式执行，应用启动不会自动修改数据库。
SELECT pg_advisory_xact_lock(72150920261109);
CREATE TABLE IF NOT EXISTS agent.knowledge_base (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    name varchar(100) NOT NULL CHECK (length(btrim(name)) > 0),
    description varchar(500) NOT NULL DEFAULT '',
    is_default boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_base_owner ON agent.knowledge_base(id,user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_base_name
    ON agent.knowledge_base(user_id, lower(btrim(name))) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_base_default
    ON agent.knowledge_base(user_id) WHERE is_default AND deleted_at IS NULL;
ALTER TABLE agent.knowledge_document ADD COLUMN IF NOT EXISTS knowledge_base_id bigint;
-- 只迁移尚无归属的资料，旧文件、版本和引用编号保持不变。
INSERT INTO agent.knowledge_base(user_id, name, description, is_default)
SELECT DISTINCT user_id, '默认知识库', '原有资料，可移动到自己创建的知识库。', true
FROM agent.knowledge_document WHERE knowledge_base_id IS NULL
ON CONFLICT (user_id) WHERE is_default AND deleted_at IS NULL DO NOTHING;
UPDATE agent.knowledge_document d SET knowledge_base_id = b.id
FROM agent.knowledge_base b
WHERE d.knowledge_base_id IS NULL AND b.user_id = d.user_id AND b.is_default AND b.deleted_at IS NULL;
ALTER TABLE agent.knowledge_document ALTER COLUMN knowledge_base_id SET NOT NULL;
DO $ownership$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname='fk_agent_document_base_owner' AND conrelid='agent.knowledge_document'::regclass) THEN
        ALTER TABLE agent.knowledge_document ADD CONSTRAINT fk_agent_document_base_owner
            FOREIGN KEY (knowledge_base_id,user_id) REFERENCES agent.knowledge_base(id,user_id);
    END IF;
END $ownership$;
DROP INDEX IF EXISTS agent.uk_agent_knowledge_document_user_checksum;
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_knowledge_document_base_checksum
    ON agent.knowledge_document(user_id,knowledge_base_id,checksum) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_agent_document_base_status
    ON agent.knowledge_document(user_id,knowledge_base_id,status,created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_agent_conversation_owner ON agent.conversation(id,user_id);
DO $scope$
DECLARE
    first_binding boolean := to_regclass('agent.conversation_knowledge_base') IS NULL;
    first_snapshot boolean := NOT EXISTS (
        SELECT 1 FROM information_schema.columns WHERE table_schema='agent'
        AND table_name='conversation_turn' AND column_name='knowledge_base_ids');
BEGIN
    CREATE TABLE IF NOT EXISTS agent.conversation_knowledge_base (
        user_id bigint NOT NULL,
        conversation_id bigint NOT NULL,
        knowledge_base_id bigint NOT NULL,
        PRIMARY KEY(user_id,conversation_id,knowledge_base_id),
        FOREIGN KEY(conversation_id,user_id) REFERENCES agent.conversation(id,user_id),
        FOREIGN KEY(knowledge_base_id,user_id) REFERENCES agent.knowledge_base(id,user_id)
    );
    -- 只在首次创建关联表时延续旧会话范围，后续执行不会覆盖用户主动清空的选择。
    IF first_binding THEN
        INSERT INTO agent.conversation_knowledge_base(user_id,conversation_id,knowledge_base_id)
        SELECT c.user_id,c.id,b.id FROM agent.conversation c JOIN agent.knowledge_base b ON b.user_id=c.user_id
        WHERE b.is_default AND b.deleted_at IS NULL AND c.deleted_at IS NULL
        ON CONFLICT DO NOTHING;
    END IF;
    IF first_snapshot THEN
        ALTER TABLE agent.conversation_turn ADD COLUMN knowledge_base_ids jsonb NOT NULL DEFAULT '[]'::jsonb
            CHECK (jsonb_typeof(knowledge_base_ids)='array');
        UPDATE agent.conversation_turn t SET knowledge_base_ids=jsonb_build_array(b.id::text)
        FROM agent.knowledge_base b WHERE b.user_id=t.user_id AND b.is_default AND b.deleted_at IS NULL;
    END IF;
END $scope$;
COMMENT ON TABLE agent.knowledge_base IS '个人知识库；文档、会话选择和检索均限定所属用户';
COMMENT ON TABLE agent.conversation_knowledge_base IS '会话保存的知识库选择；新会话默认无选择';
COMMENT ON COLUMN agent.knowledge_document.knowledge_base_id IS '所属知识库，移动只修改归属，不重建文档版本和索引';
COMMENT ON COLUMN agent.conversation_turn.knowledge_base_ids IS '本轮固定的检索范围，空数组表示不检索知识资料';
-- ASTERWAY_KNOWLEDGE_BASES_END
COMMIT;
