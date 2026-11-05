-- Asterway statistics 模块：只读 PostgreSQL 查询原型，不建业务表、视图、函数或缓存。
-- 所有参数须先由 Service 校验并绑定；$1 永远取当前认证用户，不允许客户端指定他人。
-- PREPARE 仅建立当前数据库连接内的临时查询计划，供 SQL 隔离校验；后端阶段转为 Mapper/XML。
-- 本文件依赖其他业务表；在所有 DDL 完成后装载，连接关闭即销毁查询计划。
-- 用户日历日期包含起止日；时间点范围为 [from,to)。业务角色仍需由 Service 拒绝管理身份。

-- period：TOTAL/WEEK/MONTH/YEAR/RANGE；参数为 userId,period,anchorDate,dateFrom,dateTo,today。
-- today 由服务端有效账号时区计算；自然周期截到今天，未知周期、倒置或未来自定义范围无结果。
PREPARE statistics_habit_period(bigint,text,date,date,date,date) AS
WITH input AS (
    SELECT upper($2) AS period,COALESCE($3,$6) AS anchor,$6 AS today,
        (SELECT LEAST(COALESCE(min(effective_date),$6),$6) FROM habit_revision WHERE user_id=$1) AS first_date
), bounds AS (
    SELECT period,today,
        CASE period WHEN 'TOTAL' THEN first_date WHEN 'WEEK' THEN date_trunc('week',anchor::timestamp)::date
            WHEN 'MONTH' THEN date_trunc('month',anchor::timestamp)::date
            WHEN 'YEAR' THEN date_trunc('year',anchor::timestamp)::date WHEN 'RANGE' THEN $4 END AS date_from,
        CASE period WHEN 'TOTAL' THEN today WHEN 'WEEK' THEN date_trunc('week',anchor::timestamp)::date+6
            WHEN 'MONTH' THEN (date_trunc('month',anchor::timestamp)+interval '1 month - 1 day')::date
            WHEN 'YEAR' THEN (date_trunc('year',anchor::timestamp)+interval '1 year - 1 day')::date
            WHEN 'RANGE' THEN $5 END AS date_to
    FROM input WHERE isfinite(today) AND isfinite(anchor) AND anchor<=today
)
SELECT period,date_from,LEAST(date_to,today) AS date_to FROM bounds
WHERE date_from IS NOT NULL AND date_to IS NOT NULL AND isfinite(date_from) AND isfinite(date_to)
    AND date_from<=date_to AND date_from<=today AND (period<>'RANGE' OR date_to<=today);

-- 每习惯每日事实：已有有效打卡优先为 1/1；否则看该日最后修订，暂停、结束、删除不抹去过去。
PREPARE statistics_habit_days(bigint,date,date) AS

WITH days AS (
    SELECT ($2::date + n)::date AS business_date
    FROM generate_series(0, $3::date - $2::date) AS n
), habit_days AS (
    SELECT d.business_date,h.id AS habit_id,h.status,h.deleted_at,
        CASE WHEN c.id IS NOT NULL THEN c.revision_id ELSE r.id END AS revision_id,
        CASE WHEN c.id IS NOT NULL THEN c.category_id ELSE r.category_id END AS category_id,
        CASE WHEN c.id IS NOT NULL THEN c.name ELSE r.name END AS name,
        CASE WHEN c.id IS NOT NULL THEN c.expected_time ELSE r.expected_time END AS expected_time,
        c.id AS checkin_id,
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS done_count,
        1 AS due_count
    FROM habit h CROSS JOIN days d
    LEFT JOIN LATERAL (
        SELECT ci.id,ci.revision_id,cr.category_id,cr.name,cr.expected_time
        FROM habit_checkin ci JOIN habit_revision cr
            ON cr.id=ci.revision_id AND cr.habit_id=h.id AND cr.user_id=h.user_id
        WHERE ci.habit_id=h.id AND ci.user_id=h.user_id
            AND ci.business_date=d.business_date AND ci.cancelled_at IS NULL
    ) c ON true
    LEFT JOIN LATERAL (
        SELECT hr.id,hr.category_id,hr.name,hr.expected_time,hr.status_snapshot,hr.deleted_snapshot,
            hr.start_date,hr.weekday_mask
        FROM habit_revision hr WHERE hr.habit_id=h.id AND hr.user_id=h.user_id
            AND hr.effective_date<=d.business_date
        ORDER BY hr.effective_date DESC,hr.revision_no DESC LIMIT 1
    ) r ON true
    WHERE h.user_id=$1 AND (
        c.id IS NOT NULL OR (r.status_snapshot='ACTIVE' AND NOT r.deleted_snapshot
            AND r.start_date<=d.business_date
            AND (r.weekday_mask::integer & (1 << (EXTRACT(ISODOW FROM d.business_date)::integer-1)))<>0)
    )
)
SELECT business_date,habit_id,revision_id,category_id,done_count,due_count,checkin_id
FROM habit_days ORDER BY business_date,habit_id;

-- 各维度均为 sum(done)/sum(due)，不是对单个习惯的百分比取平均；无分母返回 NULL。
-- 四种维度结果同时返回；category_id NULL 表示“未分类”，由 dimension 区分总计。
PREPARE statistics_habit_summary(bigint,date,date) AS

WITH days AS (
    SELECT ($2::date + n)::date AS business_date
    FROM generate_series(0, $3::date - $2::date) AS n
), habit_days AS (
    SELECT d.business_date,h.id AS habit_id,h.status,h.deleted_at,
        CASE WHEN c.id IS NOT NULL THEN c.revision_id ELSE r.id END AS revision_id,
        CASE WHEN c.id IS NOT NULL THEN c.category_id ELSE r.category_id END AS category_id,
        CASE WHEN c.id IS NOT NULL THEN c.name ELSE r.name END AS name,
        CASE WHEN c.id IS NOT NULL THEN c.expected_time ELSE r.expected_time END AS expected_time,
        c.id AS checkin_id,
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS done_count,
        1 AS due_count
    FROM habit h CROSS JOIN days d
    LEFT JOIN LATERAL (
        SELECT ci.id,ci.revision_id,cr.category_id,cr.name,cr.expected_time
        FROM habit_checkin ci JOIN habit_revision cr
            ON cr.id=ci.revision_id AND cr.habit_id=h.id AND cr.user_id=h.user_id
        WHERE ci.habit_id=h.id AND ci.user_id=h.user_id
            AND ci.business_date=d.business_date AND ci.cancelled_at IS NULL
    ) c ON true
    LEFT JOIN LATERAL (
        SELECT hr.id,hr.category_id,hr.name,hr.expected_time,hr.status_snapshot,hr.deleted_snapshot,
            hr.start_date,hr.weekday_mask
        FROM habit_revision hr WHERE hr.habit_id=h.id AND hr.user_id=h.user_id
            AND hr.effective_date<=d.business_date
        ORDER BY hr.effective_date DESC,hr.revision_no DESC LIMIT 1
    ) r ON true
    WHERE h.user_id=$1 AND (
        c.id IS NOT NULL OR (r.status_snapshot='ACTIVE' AND NOT r.deleted_snapshot
            AND r.start_date<=d.business_date
            AND (r.weekday_mask::integer & (1 << (EXTRACT(ISODOW FROM d.business_date)::integer-1)))<>0)
    )
)
SELECT CASE WHEN GROUPING(business_date)=0 THEN 'DAY' WHEN GROUPING(habit_id)=0 THEN 'HABIT'
            WHEN GROUPING(category_id)=0 THEN 'CATEGORY' ELSE 'TOTAL' END AS dimension,
    business_date,habit_id,category_id,
    COALESCE(sum(done_count),0)::bigint AS done_count,COALESCE(sum(due_count),0)::bigint AS due_count,
    round(100.0*sum(done_count)/NULLIF(sum(due_count),0),2) AS completion_rate
FROM habit_days GROUP BY GROUPING SETS ((business_date),(habit_id),(category_id),())
ORDER BY dimension,business_date NULLS FIRST,habit_id NULLS FIRST,category_id NULLS FIRST;

-- 每日执行耗时：按查询时区切日，夏令时日长度可为 23/25 小时，不假定一天固定 86400 秒。
-- 参数 userId,dateFrom,dateTo,timeZone,serverNow。查询时不会写心跳或延长登录有效期。
PREPARE statistics_execution_days(bigint,date,date,text,timestamptz) AS
WITH days AS (
    SELECT ($2+n)::date AS business_date,($2+n)::timestamp AT TIME ZONE $4 AS starts_at,
        ($2+n+1)::timestamp AT TIME ZONE $4 AS ends_at
    FROM generate_series(0,$3-$2) AS n
), segments AS (
    SELECT es.id AS execution_id,es.status,sg.started_at,
        LEAST($5,COALESCE(sg.ended_at,LEAST($5,es.last_heartbeat_at
            +make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))) AS ends_at
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
), clipped AS (
    SELECT d.business_date,s.execution_id,s.status,
        GREATEST(0,EXTRACT(EPOCH FROM (LEAST(d.ends_at,s.ends_at)-GREATEST(d.starts_at,s.started_at)))*1000) AS milliseconds
    FROM days d LEFT JOIN segments s ON s.started_at<d.ends_at AND s.ends_at>d.starts_at
)
SELECT business_date,COALESCE(sum(milliseconds),0)::bigint AS actual_milliseconds,
    floor(COALESCE(sum(milliseconds),0)/1000)::bigint AS actual_seconds,
    count(DISTINCT execution_id) FILTER (WHERE milliseconds>0) AS sessions,
    count(DISTINCT execution_id) FILTER (WHERE milliseconds>0 AND status='FINISHED') AS finished_sessions
FROM clipped GROUP BY business_date ORDER BY business_date;

-- 总计及区间耗时各自先合并毫秒再转秒；会话数按会话去重，不以段数替代。
PREPARE statistics_execution_totals(bigint,timestamptz,timestamptz,timestamptz) AS
WITH segments AS (
    SELECT es.id AS execution_id,es.status,sg.started_at,
        LEAST($4,COALESCE(sg.ended_at,LEAST($4,es.last_heartbeat_at
            +make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))) AS ends_at
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
), lengths AS (
    SELECT *,GREATEST(0,EXTRACT(EPOCH FROM ends_at-started_at)*1000) AS total_ms,
        GREATEST(0,EXTRACT(EPOCH FROM LEAST(ends_at,$3)-GREATEST(started_at,$2))*1000) AS range_ms
    FROM segments
)
SELECT floor(COALESCE(sum(total_ms),0)/1000)::bigint AS total_actual_seconds,
    floor(COALESCE(sum(range_ms),0)/1000)::bigint AS range_actual_seconds,
    COALESCE(sum(total_ms),0)::bigint AS total_actual_milliseconds,
    COALESCE(sum(range_ms),0)::bigint AS range_actual_milliseconds,
    count(DISTINCT execution_id) FILTER (WHERE total_ms>0) AS total_sessions,
    count(DISTINCT execution_id) FILTER (WHERE range_ms>0) AS range_sessions,
    count(DISTINCT execution_id) FILTER (WHERE total_ms>0 AND status='FINISHED') AS total_finished_sessions,
    count(DISTINCT execution_id) FILTER (WHERE range_ms>0 AND status='FINISHED') AS range_finished_sessions,
    max(started_at) FILTER (WHERE total_ms>0) AS last_execution_at
FROM lengths;

-- 真实状态事件的每日计数：基线不算完成；可信旧任务完成导入算一次，完成后重开保留每次事件。
PREPARE statistics_state_days(bigint,date,date,text) AS
WITH days AS (
    SELECT ($2+n)::date AS business_date FROM generate_series(0,$3-$2) AS n
), events AS (
    SELECT 'GOAL' AS kind,goal_id AS object_id,event_type,after_status,occurred_at
    FROM goal_state_history WHERE user_id=$1
    UNION ALL SELECT 'STAGE',stage_id,event_type,after_status,occurred_at FROM stage_state_history WHERE user_id=$1
    UNION ALL SELECT 'TASK',task_id,event_type,after_status,occurred_at FROM task_state_history WHERE user_id=$1
)
SELECT d.business_date,
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='STATUS_CHANGED' AND e.after_status='COMPLETED') AS goal_completion_actions,
    count(*) FILTER (WHERE e.kind='STAGE' AND e.event_type='STATUS_CHANGED' AND e.after_status='COMPLETED') AS stage_completion_actions,
    count(*) FILTER (WHERE e.kind='TASK' AND e.event_type IN ('STATUS_CHANGED','COMPLETION_IMPORTED') AND e.after_status='COMPLETED') AS task_completion_actions,
    count(DISTINCT e.object_id) FILTER (WHERE e.kind='TASK' AND e.event_type IN ('STATUS_CHANGED','COMPLETION_IMPORTED') AND e.after_status='COMPLETED') AS completed_distinct_tasks,
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='ARCHIVED') AS goal_archive_actions,
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='RESTORED') AS goal_restore_actions
FROM days d LEFT JOIN events e ON e.occurred_at >= $2::timestamp AT TIME ZONE $4
    AND e.occurred_at < ($3+1)::timestamp AT TIME ZONE $4
    AND (e.occurred_at AT TIME ZONE $4)::date=d.business_date
GROUP BY d.business_date ORDER BY d.business_date;

-- 时间线使用真实事实；同一时间点按种类、主键、事件类型排序，分页结果稳定。
PREPARE statistics_timeline(bigint,timestamptz,timestamptz,integer,integer) AS
WITH events AS (
    SELECT 'GOAL' AS kind,id AS fact_id,goal_id AS object_id,event_type,occurred_at,name_snapshot AS label
        FROM goal_state_history WHERE user_id=$1
    UNION ALL SELECT 'STAGE',id,stage_id,event_type,occurred_at,name_snapshot FROM stage_state_history WHERE user_id=$1
    UNION ALL SELECT 'TASK',id,task_id,event_type,occurred_at,name_snapshot FROM task_state_history WHERE user_id=$1
    UNION ALL SELECT 'HABIT',c.id,c.habit_id,'CHECKIN',c.checked_at,r.name
        FROM habit_checkin c JOIN habit_revision r ON r.id=c.revision_id AND r.user_id=c.user_id AND r.habit_id=c.habit_id WHERE c.user_id=$1
    UNION ALL SELECT 'HABIT',c.id,c.habit_id,'CHECKIN_CANCELLED',c.cancelled_at,r.name
        FROM habit_checkin c JOIN habit_revision r ON r.id=c.revision_id AND r.user_id=c.user_id AND r.habit_id=c.habit_id
        WHERE c.user_id=$1 AND c.cancelled_at IS NOT NULL
    UNION ALL SELECT 'EXECUTION',sg.id,es.task_id,'SEGMENT_STARTED',sg.started_at,NULL
        FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
    UNION ALL SELECT 'EXECUTION',sg.id,es.task_id,sg.end_reason,sg.ended_at,NULL
        FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1 AND sg.ended_at IS NOT NULL
    UNION ALL SELECT 'ASSIGNMENT',a.id,a.task_id,'ASSIGNED',a.assigned_at,NULL FROM task_assignment a WHERE a.user_id=$1
    UNION ALL SELECT 'ASSIGNMENT',a.id,a.task_id,a.end_reason,a.ended_at,NULL FROM task_assignment a WHERE a.user_id=$1 AND a.ended_at IS NOT NULL
)
SELECT kind,fact_id,object_id,event_type,occurred_at,label FROM events
WHERE occurred_at>=$2 AND occurred_at<$3
ORDER BY occurred_at DESC,kind,fact_id DESC,event_type
LIMIT LEAST(100,GREATEST(1,$4)) OFFSET GREATEST(0,$5);

-- 当前对象概况与历史完成动作分开，不通过当前状态倒推某一天的过去。
PREPARE statistics_objects(bigint) AS
SELECT 'GOAL' AS kind,status,count(*) AS object_count FROM goal
    WHERE user_id=$1 AND deleted_at IS NULL AND archived_at IS NULL GROUP BY status
UNION ALL SELECT 'STAGE',s.status,count(*) FROM stage s JOIN goal g ON g.id=s.goal_id AND g.user_id=s.user_id
    WHERE s.user_id=$1 AND s.deleted_at IS NULL AND g.deleted_at IS NULL AND g.archived_at IS NULL GROUP BY s.status
UNION ALL SELECT 'TASK',t.status,count(*) FROM task t JOIN stage s ON s.id=t.stage_id AND s.user_id=t.user_id
    JOIN goal g ON g.id=s.goal_id AND g.user_id=t.user_id
    WHERE t.user_id=$1 AND t.deleted_at IS NULL AND s.deleted_at IS NULL AND g.deleted_at IS NULL AND g.archived_at IS NULL GROUP BY t.status
ORDER BY kind,status;
