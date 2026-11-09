-- 定义通知表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway notification 模块，PostgreSQL 17.11 手工初始化。
-- MVP 仅个人目标达标后的站内确认提醒，不实现公告、群发、邮件、短信或 WebSocket。
-- 开始事务，后面的定义或修改需要一起成功才提交。
BEGIN;

-- 创建 notification_message 表；目标达标站内消息；一位用户一个目标一轮只一条，已读与业务处理独立，历史不删除。
CREATE TABLE notification_message (
    -- id 保存数据库生成的站内消息标识，对外按字符串传输；类型为大整数；编号由数据库生成，用于唯一定位记录。
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- user_id 保存收件人，必须与目标同属本人普通用户；不支持客户端任意发送；类型为大整数；必须填写，保存时检查后面的限制。
    user_id bigint NOT NULL CHECK (user_id > 0),
    -- goal_id 保存提醒来源目标逻辑标识；归档、完成或条件失效后仍保留消息；类型为大整数；必须填写，保存时检查后面的限制。
    goal_id bigint NOT NULL CHECK (goal_id > 0),
    -- condition_round 保存目标达标轮次；回落再达标才产生下一轮，不按每次查询重复提醒；类型为大整数；必须填写，保存时检查后面的限制。
    condition_round bigint NOT NULL CHECK (condition_round > 0),
    -- message_type 保存本期仅 GOAL_COMPLETION_CONFIRM；不预建没有业务的类型；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    message_type varchar(32) NOT NULL DEFAULT 'GOAL_COMPLETION_CONFIRM'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (message_type = 'GOAL_COMPLETION_CONFIRM'),
    -- title 保存发送时的标题快照，1～100 字；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    title varchar(100) NOT NULL CHECK (title ~ '[^[:space:]]'),
    -- content 保存发送时的正文快照，1～2000 字，避免来源后改名造成历史歧义；类型为有长度限制的文字；必须填写，保存时检查后面的限制。
    content varchar(2000) NOT NULL CHECK (content ~ '[^[:space:]]'),
    -- read_at 保存首次已读时间，可空；已读不表示确认完成，失效不伪造已读；类型为带时区的时间点。
    read_at timestamptz(3),
    -- handle_status 保存PENDING 待处理、CONFIRMED 确认目标完成、CONTINUED 继续保持原状态、INVALIDATED 来源或条件失效；类型为有长度限制的文字；必须填写，未传值时使用这里的默认值。
    handle_status varchar(16) NOT NULL DEFAULT 'PENDING'
        -- 限定允许写入的数据形状或取值，条件不成立时拒绝保存。
        CHECK (handle_status IN ('PENDING', 'CONFIRMED', 'CONTINUED', 'INVALIDATED')),
    -- handled_at 保存真实处理或失效时间，仅非 PENDING 有值；重复操作不改写时间；类型为带时区的时间点。
    handled_at timestamptz(3),
    -- version 保存乐观锁版本；确认必须同时检查目标版本和达标轮次；类型为大整数；必须填写，未传值时使用这里的默认值，保存时检查后面的限制。
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    -- created_at 保存发送的服务端时间；旧目标首轮提醒也取初始化当前时间；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- updated_at 保存更新时间点，由应用维护；类型为带时区的时间点；必须填写，未传值时使用这里的默认值。
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT uk_notification_goal_round UNIQUE (user_id, goal_id, condition_round),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_notification_handle_pair CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (handle_status = 'PENDING' AND handled_at IS NULL)
        -- 提供另一个可满足的分支条件，任一分支成立即可。
        OR (handle_status <> 'PENDING' AND handled_at IS NOT NULL)
    ),
    -- 给这条校验或唯一规则起名，写入不满足规则的数据会被数据库拒绝。
    CONSTRAINT ck_notification_time CHECK (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        -- 再增加一个必须同时满足的筛选条件。
        AND (read_at IS NULL OR (isfinite(read_at) AND read_at >= created_at))
        -- 再增加一个必须同时满足的筛选条件。
        AND (handled_at IS NULL OR (isfinite(handled_at) AND handled_at >= created_at))
    )
);
-- 把这张表的业务用途保存为数据库说明，查看结构时可以直接读到。
COMMENT ON TABLE notification_message IS '目标达标站内消息；一位用户一个目标一轮只一条，已读与业务处理独立，历史不删除';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.id IS '数据库生成的站内消息标识，对外按字符串传输';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.user_id IS '收件人，必须与目标同属本人普通用户；不支持客户端任意发送';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.goal_id IS '提醒来源目标逻辑标识；归档、完成或条件失效后仍保留消息';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.condition_round IS '目标达标轮次；回落再达标才产生下一轮，不按每次查询重复提醒';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.message_type IS '本期仅 GOAL_COMPLETION_CONFIRM；不预建没有业务的类型';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.title IS '发送时的标题快照，1～100 字';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.content IS '发送时的正文快照，1～2000 字，避免来源后改名造成历史歧义';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.read_at IS '首次已读时间，可空；已读不表示确认完成，失效不伪造已读';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.handle_status IS 'PENDING 待处理、CONFIRMED 确认目标完成、CONTINUED 继续保持原状态、INVALIDATED 来源或条件失效';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.handled_at IS '真实处理或失效时间，仅非 PENDING 有值；重复操作不改写时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.version IS '乐观锁版本；确认必须同时检查目标版本和达标轮次';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.created_at IS '发送的服务端时间；旧目标首轮提醒也取初始化当前时间';
-- 把这个字段的含义和取值规则保存为数据库说明。
COMMENT ON COLUMN notification_message.updated_at IS '更新时间点，由应用维护';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_notification_user_page ON notification_message (user_id, created_at DESC, id DESC);
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_notification_user_unread ON notification_message (user_id, created_at DESC, id DESC)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE read_at IS NULL AND handle_status <> 'INVALIDATED';
-- 建立这里指定的查询索引，加快对应筛选或排序。
CREATE INDEX ix_notification_user_pending_goal ON notification_message (user_id, goal_id, condition_round)
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE handle_status = 'PENDING';

-- BEGIN REMAINING NOTIFICATION BOOTSTRAP
-- 后端验收后，在维护事务中同时 SET LOCAL asterway.remaining_backend_ready = 'true';
-- SET LOCAL asterway.remaining_history_bootstrap = 'true'; 默认完全不改业务数据。
-- 维护时停止业务写入，或在同一事务先按 id 锁定全部待处理 identity_user 行。
-- 执行一次数据库端的过程代码，按下面的判断补齐结构或授权。
DO $notification_bootstrap$
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
        -- 精确条件：有效任务非空且全部完成。管理身份优先于保留的 USER。
        -- 先定义可复用的临时查询结果，后续语句继续引用它。
        WITH eligible AS (
            -- 读取后面列出的字段或计算结果。
            SELECT g.id, (g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
                -- 再增加一个必须同时满足的筛选条件。
                AND COALESCE(u.is_enabled,false) AND u.deleted_at IS NULL
                -- 再增加一个必须同时满足的筛选条件。
                AND EXISTS (SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
                    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
                    WHERE ur.user_id=u.id AND r.code='USER' AND r.is_enabled AND r.deleted_at IS NULL)
                -- 再增加一个必须同时满足的筛选条件。
                AND NOT EXISTS (SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
                    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
                    WHERE ur.user_id=u.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND r.is_enabled AND r.deleted_at IS NULL)
                -- 再增加一个必须同时满足的筛选条件。
                AND EXISTS (SELECT 1 FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=g.user_id
                    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
                    WHERE s.goal_id=g.id AND s.user_id=g.user_id AND s.deleted_at IS NULL AND t.deleted_at IS NULL)
                -- 再增加一个必须同时满足的筛选条件。
                AND NOT EXISTS (SELECT 1 FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=g.user_id
                    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
                    WHERE s.goal_id=g.id AND s.user_id=g.user_id AND s.deleted_at IS NULL AND t.deleted_at IS NULL
                        -- 再增加一个必须同时满足的筛选条件。
                        AND t.status <> 'COMPLETED')) AS qualifies
            -- 指定本次查询主要读取的表或子查询。
            FROM goal g LEFT JOIN identity_user u ON u.id=g.user_id
        )
        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE goal g SET completion_condition_met=e.qualifies,
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            completion_round=g.completion_round + CASE WHEN e.qualifies THEN 1 ELSE 0 END,
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            version=g.version+1, updated_at=initialized_at
        -- 指定本次查询主要读取的表或子查询。
        FROM eligible e WHERE g.id=e.id AND g.completion_condition_met IS DISTINCT FROM e.qualifies;

        -- 按后续条件修改指定记录；没有被条件选中的记录不变。
        UPDATE notification_message n SET handle_status='INVALIDATED', handled_at=initialized_at,
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            version=n.version+1, updated_at=initialized_at
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE n.handle_status='PENDING' AND NOT EXISTS (
            -- 读取后面列出的字段或计算结果。
            SELECT 1 FROM goal g WHERE g.id=n.goal_id AND g.user_id=n.user_id
                -- 再增加一个必须同时满足的筛选条件。
                AND g.completion_condition_met AND g.completion_round=n.condition_round
                -- 再增加一个必须同时满足的筛选条件。
                AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
        );
        -- 向 notification_message 写入新记录，字段顺序与后面的值一一对应。
        INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,created_at,updated_at)
        -- 读取后面列出的字段或计算结果。
        SELECT g.user_id,g.id,g.completion_round,'目标已达标，请确认完成',
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：'目标「' || g.name || '」的有效任务已全部完成，请确认目标完成或选择继续保持当前状态。',。
            '目标「' || g.name || '」的有效任务已全部完成，请确认目标完成或选择继续保持当前状态。',
            -- 继续列出本次使用的字段：initializedat（initialized_at）、initializedat（initialized_at）。
            initialized_at,initialized_at
        -- 指定本次查询主要读取的表或子查询。
        FROM goal g
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE g.completion_condition_met AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
        -- 遇到相同唯一键时按后面的规则复用或更新，不重复插入。
        ON CONFLICT(user_id,goal_id,condition_round) DO NOTHING;
    -- 结束前面的条件表达式、循环或数据库程序块。
    END IF;
-- 结束前面的条件表达式、循环或数据库程序块。
END
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：$notification_bootstrap$;。
$notification_bootstrap$;
-- END REMAINING NOTIFICATION BOOTSTRAP

-- 后续 Service：先用户锁；任务状态、状态历史、精确达标缓存和消息同事务，消息写入失败则整体回滚。
-- 不使用四舍五入后的 100.00% 判断；继续保持后同轮不重发；标记已读不自动处理目标。
-- 确认时重查本人、通知版本、目标版本、轮次、有效来源；失效消息不能完成目标。
-- 未读数排除 INVALIDATED；应用不提供任意消息发送接口。
-- 提交这一组事务操作，使前面已经完成的修改正式生效。
COMMIT;
