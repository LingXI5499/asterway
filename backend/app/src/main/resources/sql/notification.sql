-- Asterway notification 模块，PostgreSQL 17.11 手工初始化。
-- MVP 仅个人目标达标后的站内确认提醒，不实现公告、群发、邮件、短信或 WebSocket。
BEGIN;

CREATE TABLE notification_message (
    id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id bigint NOT NULL CHECK (user_id > 0),
    goal_id bigint NOT NULL CHECK (goal_id > 0),
    condition_round bigint NOT NULL CHECK (condition_round > 0),
    message_type varchar(32) NOT NULL DEFAULT 'GOAL_COMPLETION_CONFIRM'
        CHECK (message_type = 'GOAL_COMPLETION_CONFIRM'),
    title varchar(100) NOT NULL CHECK (title ~ '[^[:space:]]'),
    content varchar(2000) NOT NULL CHECK (content ~ '[^[:space:]]'),
    read_at timestamptz(3),
    handle_status varchar(16) NOT NULL DEFAULT 'PENDING'
        CHECK (handle_status IN ('PENDING', 'CONFIRMED', 'CONTINUED', 'INVALIDATED')),
    handled_at timestamptz(3),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_goal_round UNIQUE (user_id, goal_id, condition_round),
    CONSTRAINT ck_notification_handle_pair CHECK (
        (handle_status = 'PENDING' AND handled_at IS NULL)
        OR (handle_status <> 'PENDING' AND handled_at IS NOT NULL)
    ),
    CONSTRAINT ck_notification_time CHECK (
        isfinite(created_at) AND isfinite(updated_at) AND updated_at >= created_at
        AND (read_at IS NULL OR (isfinite(read_at) AND read_at >= created_at))
        AND (handled_at IS NULL OR (isfinite(handled_at) AND handled_at >= created_at))
    )
);
COMMENT ON TABLE notification_message IS '目标达标站内消息；一位用户一个目标一轮只一条，已读与业务处理独立，历史不删除';
COMMENT ON COLUMN notification_message.id IS '数据库生成的站内消息标识，对外按字符串传输';
COMMENT ON COLUMN notification_message.user_id IS '收件人，必须与目标同属本人普通用户；不支持客户端任意发送';
COMMENT ON COLUMN notification_message.goal_id IS '提醒来源目标逻辑标识；归档、完成或条件失效后仍保留消息';
COMMENT ON COLUMN notification_message.condition_round IS '目标达标轮次；回落再达标才产生下一轮，不按每次查询重复提醒';
COMMENT ON COLUMN notification_message.message_type IS '本期仅 GOAL_COMPLETION_CONFIRM；不预建没有业务的类型';
COMMENT ON COLUMN notification_message.title IS '发送时的标题快照，1～100 字';
COMMENT ON COLUMN notification_message.content IS '发送时的正文快照，1～2000 字，避免来源后改名造成历史歧义';
COMMENT ON COLUMN notification_message.read_at IS '首次已读时间，可空；已读不表示确认完成，失效不伪造已读';
COMMENT ON COLUMN notification_message.handle_status IS 'PENDING 待处理、CONFIRMED 确认目标完成、CONTINUED 继续保持原状态、INVALIDATED 来源或条件失效';
COMMENT ON COLUMN notification_message.handled_at IS '真实处理或失效时间，仅非 PENDING 有值；重复操作不改写时间';
COMMENT ON COLUMN notification_message.version IS '乐观锁版本；确认必须同时检查目标版本和达标轮次';
COMMENT ON COLUMN notification_message.created_at IS '发送的服务端时间；旧目标首轮提醒也取初始化当前时间';
COMMENT ON COLUMN notification_message.updated_at IS '更新时间点，由应用维护';
CREATE INDEX ix_notification_user_page ON notification_message (user_id, created_at DESC, id DESC);
CREATE INDEX ix_notification_user_unread ON notification_message (user_id, created_at DESC, id DESC)
    WHERE read_at IS NULL AND handle_status <> 'INVALIDATED';
CREATE INDEX ix_notification_user_pending_goal ON notification_message (user_id, goal_id, condition_round)
    WHERE handle_status = 'PENDING';

-- BEGIN REMAINING NOTIFICATION BOOTSTRAP
-- 后端验收后，在维护事务中同时 SET LOCAL asterway.remaining_backend_ready = 'true';
-- SET LOCAL asterway.remaining_history_bootstrap = 'true'; 默认完全不改业务数据。
-- 维护时停止业务写入，或在同一事务先按 id 锁定全部待处理 identity_user 行。
DO $notification_bootstrap$
DECLARE
    approved boolean := COALESCE(NULLIF(current_setting('asterway.remaining_history_bootstrap', true), ''), 'false')::boolean;
    backend_ready boolean := COALESCE(NULLIF(current_setting('asterway.remaining_backend_ready', true), ''), 'false')::boolean;
    initialized_at timestamptz(3) := clock_timestamp();
BEGIN
    IF approved AND NOT backend_ready THEN
        RAISE EXCEPTION 'Remaining fact initialization requires the approved backend';
    END IF;
    IF approved THEN
        -- 精确条件：有效任务非空且全部完成。管理身份优先于保留的 USER。
        WITH eligible AS (
            SELECT g.id, (g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
                AND COALESCE(u.is_enabled,false) AND u.deleted_at IS NULL
                AND EXISTS (SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
                    WHERE ur.user_id=u.id AND r.code='USER' AND r.is_enabled AND r.deleted_at IS NULL)
                AND NOT EXISTS (SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
                    WHERE ur.user_id=u.id AND r.code IN ('ADMIN','SUPER_ADMIN') AND r.is_enabled AND r.deleted_at IS NULL)
                AND EXISTS (SELECT 1 FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=g.user_id
                    WHERE s.goal_id=g.id AND s.user_id=g.user_id AND s.deleted_at IS NULL AND t.deleted_at IS NULL)
                AND NOT EXISTS (SELECT 1 FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=g.user_id
                    WHERE s.goal_id=g.id AND s.user_id=g.user_id AND s.deleted_at IS NULL AND t.deleted_at IS NULL
                        AND t.status <> 'COMPLETED')) AS qualifies
            FROM goal g LEFT JOIN identity_user u ON u.id=g.user_id
        )
        UPDATE goal g SET completion_condition_met=e.qualifies,
            completion_round=g.completion_round + CASE WHEN e.qualifies THEN 1 ELSE 0 END,
            version=g.version+1, updated_at=initialized_at
        FROM eligible e WHERE g.id=e.id AND g.completion_condition_met IS DISTINCT FROM e.qualifies;

        UPDATE notification_message n SET handle_status='INVALIDATED', handled_at=initialized_at,
            version=n.version+1, updated_at=initialized_at
        WHERE n.handle_status='PENDING' AND NOT EXISTS (
            SELECT 1 FROM goal g WHERE g.id=n.goal_id AND g.user_id=n.user_id
                AND g.completion_condition_met AND g.completion_round=n.condition_round
                AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
        );
        INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,created_at,updated_at)
        SELECT g.user_id,g.id,g.completion_round,'目标已达标，请确认完成',
            '目标「' || g.name || '」的有效任务已全部完成，请确认目标完成或选择继续保持当前状态。',
            initialized_at,initialized_at
        FROM goal g
        WHERE g.completion_condition_met AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
        ON CONFLICT(user_id,goal_id,condition_round) DO NOTHING;
    END IF;
END
$notification_bootstrap$;
-- END REMAINING NOTIFICATION BOOTSTRAP

-- 后续 Service：先用户锁；任务状态、状态历史、精确达标缓存和消息同事务，消息写入失败则整体回滚。
-- 不使用四舍五入后的 100.00% 判断；继续保持后同轮不重发；标记已读不自动处理目标。
-- 确认时重查本人、通知版本、目标版本、轮次、有效来源；失效消息不能完成目标。
-- 未读数排除 INVALIDATED；应用不提供任意消息发送接口。
COMMIT;
