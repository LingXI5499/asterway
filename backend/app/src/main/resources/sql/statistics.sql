-- 定义统计表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway statistics 模块：只读 PostgreSQL 查询原型，不建业务表、视图、函数或缓存。
-- 所有参数须先由 Service 校验并绑定；$1 永远取当前认证用户，不允许客户端指定他人。
-- PREPARE 仅建立当前数据库连接内的临时查询计划，供 SQL 隔离校验；后端阶段转为 Mapper/XML。
-- 本文件依赖其他业务表；在所有 DDL 完成后装载，连接关闭即销毁查询计划。
-- 用户日历日期包含起止日；时间点范围为 [from,to)。业务角色仍需由 Service 拒绝管理身份。

-- period：TOTAL/WEEK/MONTH/YEAR/RANGE；参数为 userId,period,anchorDate,dateFrom,dateTo,today。
-- today 由服务端有效账号时区计算；自然周期截到今天，未知周期、倒置或未来自定义范围无结果。
-- 继续列出本次使用的字段：文字（text）、日期（date）、日期（date）、日期（date）。
PREPARE statistics_habit_period(bigint,text,date,date,date,date) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH input AS (
    -- 读取后面列出的字段或计算结果。
    SELECT upper($2) AS period,COALESCE($3,$6) AS anchor,$6 AS today,
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        (SELECT LEAST(COALESCE(min(effective_date),$6),$6) FROM habit_revision WHERE user_id=$1) AS first_date
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), bounds AS (。
), bounds AS (
    -- 读取后面列出的字段或计算结果。
    SELECT period,today,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE period WHEN 'TOTAL' THEN first_date WHEN 'WEEK' THEN date_trunc('week',anchor::timestamp)::date
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN 'MONTH' THEN date_trunc('month',anchor::timestamp)::date
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN 'YEAR' THEN date_trunc('year',anchor::timestamp)::date WHEN 'RANGE' THEN $4 END AS date_from,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE period WHEN 'TOTAL' THEN today WHEN 'WEEK' THEN date_trunc('week',anchor::timestamp)::date+6
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN 'MONTH' THEN (date_trunc('month',anchor::timestamp)+interval '1 month - 1 day')::date
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN 'YEAR' THEN (date_trunc('year',anchor::timestamp)+interval '1 year - 1 day')::date
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN 'RANGE' THEN $5 END AS date_to
    -- 指定本次查询主要读取的表或子查询。
    FROM input WHERE isfinite(today) AND isfinite(anchor) AND anchor<=today
)
-- 读取后面列出的字段或计算结果。
SELECT period,date_from,LEAST(date_to,today) AS date_to FROM bounds
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE date_from IS NOT NULL AND date_to IS NOT NULL AND isfinite(date_from) AND isfinite(date_to)
    -- 再增加一个必须同时满足的筛选条件。
    AND date_from<=date_to AND date_from<=today AND (period<>'RANGE' OR date_to<=today);

-- 每习惯每日事实：已有有效打卡优先为 1/1；否则看该日最后修订，暂停、结束、删除不抹去过去。
-- 继续列出本次使用的字段：日期（date）。
PREPARE statistics_habit_days(bigint,date,date) AS

-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2::date + n)::date AS business_date
    -- 指定本次查询主要读取的表或子查询。
    FROM generate_series(0, $3::date - $2::date) AS n
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), habit_days AS (。
), habit_days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT d.business_date,h.id AS habit_id,h.status,h.deleted_at,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.revision_id ELSE r.id END AS revision_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.category_id ELSE r.category_id END AS category_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.name ELSE r.name END AS name,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.expected_time ELSE r.expected_time END AS expected_time,
        -- 继续列出本次使用的字段：c编号作为打卡编号（c.id AS checkin_id）。
        c.id AS checkin_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS done_count,
        -- 继续列出本次使用的字段：1作为应完成数量（1 AS due_count）。
        1 AS due_count
    -- 指定本次查询主要读取的表或子查询。
    FROM habit h CROSS JOIN days d
    -- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    LEFT JOIN LATERAL (
        -- 读取后面列出的字段或计算结果。
        SELECT ci.id,ci.revision_id,cr.category_id,cr.name,cr.expected_time
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_checkin ci JOIN habit_revision cr
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            ON cr.id=ci.revision_id AND cr.habit_id=h.id AND cr.user_id=h.user_id
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE ci.habit_id=h.id AND ci.user_id=h.user_id
            -- 再增加一个必须同时满足的筛选条件。
            AND ci.business_date=d.business_date AND ci.cancelled_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) c ON true。
    ) c ON true
    -- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    LEFT JOIN LATERAL (
        -- 读取后面列出的字段或计算结果。
        SELECT hr.id,hr.category_id,hr.name,hr.expected_time,hr.status_snapshot,hr.deleted_snapshot,
            -- 继续列出本次使用的字段：hr中的开始日期（hr.start_date）、hr中的weekdaymask（hr.weekday_mask）。
            hr.start_date,hr.weekday_mask
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_revision hr WHERE hr.habit_id=h.id AND hr.user_id=h.user_id
            -- 再增加一个必须同时满足的筛选条件。
            AND hr.effective_date<=d.business_date
        -- 按列出的字段排序，ASC 为升序，DESC 为降序。
        ORDER BY hr.effective_date DESC,hr.revision_no DESC LIMIT 1
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) r ON true。
    ) r ON true
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE h.user_id=$1 AND (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        c.id IS NOT NULL OR (r.status_snapshot='ACTIVE' AND NOT r.deleted_snapshot
            -- 再增加一个必须同时满足的筛选条件。
            AND r.start_date<=d.business_date
            -- 再增加一个必须同时满足的筛选条件。
            AND (r.weekday_mask::integer & (1 << (EXTRACT(ISODOW FROM d.business_date)::integer-1)))<>0)
    )
)
-- 读取后面列出的字段或计算结果。
SELECT business_date,habit_id,revision_id,category_id,done_count,due_count,checkin_id
-- 指定本次查询主要读取的表或子查询。
FROM habit_days ORDER BY business_date,habit_id;

-- 各维度均为 sum(done)/sum(due)，不是对单个习惯的百分比取平均；无分母返回 NULL。
-- 四种维度结果同时返回；category_id NULL 表示“未分类”，由 dimension 区分总计。
-- 继续列出本次使用的字段：日期（date）。
PREPARE statistics_habit_summary(bigint,date,date) AS

-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2::date + n)::date AS business_date
    -- 指定本次查询主要读取的表或子查询。
    FROM generate_series(0, $3::date - $2::date) AS n
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), habit_days AS (。
), habit_days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT d.business_date,h.id AS habit_id,h.status,h.deleted_at,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.revision_id ELSE r.id END AS revision_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.category_id ELSE r.category_id END AS category_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.name ELSE r.name END AS name,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN c.expected_time ELSE r.expected_time END AS expected_time,
        -- 继续列出本次使用的字段：c编号作为打卡编号（c.id AS checkin_id）。
        c.id AS checkin_id,
        -- 根据后续 WHEN 条件选择本字段最终使用的值。
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS done_count,
        -- 继续列出本次使用的字段：1作为应完成数量（1 AS due_count）。
        1 AS due_count
    -- 指定本次查询主要读取的表或子查询。
    FROM habit h CROSS JOIN days d
    -- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    LEFT JOIN LATERAL (
        -- 读取后面列出的字段或计算结果。
        SELECT ci.id,ci.revision_id,cr.category_id,cr.name,cr.expected_time
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_checkin ci JOIN habit_revision cr
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            ON cr.id=ci.revision_id AND cr.habit_id=h.id AND cr.user_id=h.user_id
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE ci.habit_id=h.id AND ci.user_id=h.user_id
            -- 再增加一个必须同时满足的筛选条件。
            AND ci.business_date=d.business_date AND ci.cancelled_at IS NULL
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) c ON true。
    ) c ON true
    -- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    LEFT JOIN LATERAL (
        -- 读取后面列出的字段或计算结果。
        SELECT hr.id,hr.category_id,hr.name,hr.expected_time,hr.status_snapshot,hr.deleted_snapshot,
            -- 继续列出本次使用的字段：hr中的开始日期（hr.start_date）、hr中的weekdaymask（hr.weekday_mask）。
            hr.start_date,hr.weekday_mask
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_revision hr WHERE hr.habit_id=h.id AND hr.user_id=h.user_id
            -- 再增加一个必须同时满足的筛选条件。
            AND hr.effective_date<=d.business_date
        -- 按列出的字段排序，ASC 为升序，DESC 为降序。
        ORDER BY hr.effective_date DESC,hr.revision_no DESC LIMIT 1
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：) r ON true。
    ) r ON true
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE h.user_id=$1 AND (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        c.id IS NOT NULL OR (r.status_snapshot='ACTIVE' AND NOT r.deleted_snapshot
            -- 再增加一个必须同时满足的筛选条件。
            AND r.start_date<=d.business_date
            -- 再增加一个必须同时满足的筛选条件。
            AND (r.weekday_mask::integer & (1 << (EXTRACT(ISODOW FROM d.business_date)::integer-1)))<>0)
    )
)
-- 读取后面列出的字段或计算结果。
SELECT CASE WHEN GROUPING(business_date)=0 THEN 'DAY' WHEN GROUPING(habit_id)=0 THEN 'HABIT'
            -- 当前条件成立时，采用 THEN 后给出的值。
            WHEN GROUPING(category_id)=0 THEN 'CATEGORY' ELSE 'TOTAL' END AS dimension,
    -- 继续列出本次使用的字段：business日期（business_date）、习惯编号（habit_id）、分类编号（category_id）。
    business_date,habit_id,category_id,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：COALESCE(sum(done_count),0)::bigint AS done_count,COALESCE(sum(due_count),0)::bigint AS due_count,。
    COALESCE(sum(done_count),0)::bigint AS done_count,COALESCE(sum(due_count),0)::bigint AS due_count,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：round(100.0*sum(done_count)/NULLIF(sum(due_count),0),2) AS completion_rate。
    round(100.0*sum(done_count)/NULLIF(sum(due_count),0),2) AS completion_rate
-- 指定本次查询主要读取的表或子查询。
FROM habit_days GROUP BY GROUPING SETS ((business_date),(habit_id),(category_id),())
-- 按列出的字段排序，ASC 为升序，DESC 为降序。
ORDER BY dimension,business_date NULLS FIRST,habit_id NULLS FIRST,category_id NULLS FIRST;

-- 每日执行耗时：按查询时区切日，夏令时日长度可为 23/25 小时，不假定一天固定 86400 秒。
-- 参数 userId,dateFrom,dateTo,timeZone,serverNow。查询时不会写心跳或延长登录有效期。
-- 继续列出本次使用的字段：日期（date）、日期（date）、文字（text）。
PREPARE statistics_execution_days(bigint,date,date,text,timestamptz) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2+n)::date AS business_date,($2+n)::timestamp AT TIME ZONE $4 AS starts_at,
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：($2+n+1)::timestamp AT TIME ZONE $4 AS ends_at。
        ($2+n+1)::timestamp AT TIME ZONE $4 AS ends_at
    -- 指定本次查询主要读取的表或子查询。
    FROM generate_series(0,$3-$2) AS n
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), segments AS (。
), segments AS (
    -- 读取后面列出的字段或计算结果。
    SELECT es.id AS execution_id,es.status,sg.started_at,
        -- 继续列出本次使用的字段：es中的最后一次心跳at（es.last_heartbeat_at）。
        LEAST($5,COALESCE(sg.ended_at,LEAST($5,es.last_heartbeat_at
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            +make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))) AS ends_at
    -- 指定本次查询主要读取的表或子查询。
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), clipped AS (。
), clipped AS (
    -- 读取后面列出的字段或计算结果。
    SELECT d.business_date,s.execution_id,s.status,
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：GREATEST(0,EXTRACT(EPOCH FROM (LEAST(d.ends_at,s.ends_at)-GREATEST(d.starts_at,s.started_at)))*1000) AS milliseconds。
        GREATEST(0,EXTRACT(EPOCH FROM (LEAST(d.ends_at,s.ends_at)-GREATEST(d.starts_at,s.started_at)))*1000) AS milliseconds
    -- 指定本次查询主要读取的表或子查询。
    FROM days d LEFT JOIN segments s ON s.started_at<d.ends_at AND s.ends_at>d.starts_at
)
-- 读取后面列出的字段或计算结果。
SELECT business_date,COALESCE(sum(milliseconds),0)::bigint AS actual_milliseconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：floor(COALESCE(sum(milliseconds),0)/1000)::bigint AS actual_seconds,。
    floor(COALESCE(sum(milliseconds),0)/1000)::bigint AS actual_seconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：count(DISTINCT execution_id) FILTER (WHERE milliseconds>0) AS sessions,。
    count(DISTINCT execution_id) FILTER (WHERE milliseconds>0) AS sessions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(DISTINCT execution_id) FILTER (WHERE milliseconds>0 AND status='FINISHED') AS finished_sessions
-- 指定本次查询主要读取的表或子查询。
FROM clipped GROUP BY business_date ORDER BY business_date;

-- 总计及区间耗时各自先合并毫秒再转秒；会话数按会话去重，不以段数替代。
-- 继续列出本次使用的字段：timestamptz、timestamptz。
PREPARE statistics_execution_totals(bigint,timestamptz,timestamptz,timestamptz) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH segments AS (
    -- 读取后面列出的字段或计算结果。
    SELECT es.id AS execution_id,es.status,sg.started_at,
        -- 继续列出本次使用的字段：es中的最后一次心跳at（es.last_heartbeat_at）。
        LEAST($4,COALESCE(sg.ended_at,LEAST($4,es.last_heartbeat_at
            -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
            +make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))) AS ends_at
    -- 指定本次查询主要读取的表或子查询。
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), lengths AS (。
), lengths AS (
    -- 读取后面列出的字段或计算结果。
    SELECT *,GREATEST(0,EXTRACT(EPOCH FROM ends_at-started_at)*1000) AS total_ms,
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：GREATEST(0,EXTRACT(EPOCH FROM LEAST(ends_at,$3)-GREATEST(started_at,$2))*1000) AS range_ms。
        GREATEST(0,EXTRACT(EPOCH FROM LEAST(ends_at,$3)-GREATEST(started_at,$2))*1000) AS range_ms
    -- 指定本次查询主要读取的表或子查询。
    FROM segments
)
-- 读取后面列出的字段或计算结果。
SELECT floor(COALESCE(sum(total_ms),0)/1000)::bigint AS total_actual_seconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：floor(COALESCE(sum(range_ms),0)/1000)::bigint AS range_actual_seconds,。
    floor(COALESCE(sum(range_ms),0)/1000)::bigint AS range_actual_seconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：COALESCE(sum(total_ms),0)::bigint AS total_actual_milliseconds,。
    COALESCE(sum(total_ms),0)::bigint AS total_actual_milliseconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：COALESCE(sum(range_ms),0)::bigint AS range_actual_milliseconds,。
    COALESCE(sum(range_ms),0)::bigint AS range_actual_milliseconds,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：count(DISTINCT execution_id) FILTER (WHERE total_ms>0) AS total_sessions,。
    count(DISTINCT execution_id) FILTER (WHERE total_ms>0) AS total_sessions,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：count(DISTINCT execution_id) FILTER (WHERE range_ms>0) AS range_sessions,。
    count(DISTINCT execution_id) FILTER (WHERE range_ms>0) AS range_sessions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(DISTINCT execution_id) FILTER (WHERE total_ms>0 AND status='FINISHED') AS total_finished_sessions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(DISTINCT execution_id) FILTER (WHERE range_ms>0 AND status='FINISHED') AS range_finished_sessions,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：max(started_at) FILTER (WHERE total_ms>0) AS last_execution_at。
    max(started_at) FILTER (WHERE total_ms>0) AS last_execution_at
-- 指定本次查询主要读取的表或子查询。
FROM lengths;

-- 真实状态事件的每日计数：基线不算完成；可信旧任务完成导入算一次，完成后重开保留每次事件。
-- 继续列出本次使用的字段：日期（date）、日期（date）。
PREPARE statistics_state_days(bigint,date,date,text) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2+n)::date AS business_date FROM generate_series(0,$3-$2) AS n
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), events AS (。
), events AS (
    -- 读取后面列出的字段或计算结果。
    SELECT 'GOAL' AS kind,goal_id AS object_id,event_type,after_status,occurred_at
    -- 指定本次查询主要读取的表或子查询。
    FROM goal_state_history WHERE user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'STAGE',stage_id,event_type,after_status,occurred_at FROM stage_state_history WHERE user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'TASK',task_id,event_type,after_status,occurred_at FROM task_state_history WHERE user_id=$1
)
-- 读取后面列出的字段或计算结果。
SELECT d.business_date,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='STATUS_CHANGED' AND e.after_status='COMPLETED') AS goal_completion_actions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(*) FILTER (WHERE e.kind='STAGE' AND e.event_type='STATUS_CHANGED' AND e.after_status='COMPLETED') AS stage_completion_actions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(*) FILTER (WHERE e.kind='TASK' AND e.event_type IN ('STATUS_CHANGED','COMPLETION_IMPORTED') AND e.after_status='COMPLETED') AS task_completion_actions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(DISTINCT e.object_id) FILTER (WHERE e.kind='TASK' AND e.event_type IN ('STATUS_CHANGED','COMPLETION_IMPORTED') AND e.after_status='COMPLETED') AS completed_distinct_tasks,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='ARCHIVED') AS goal_archive_actions,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    count(*) FILTER (WHERE e.kind='GOAL' AND e.event_type='RESTORED') AS goal_restore_actions
-- 指定本次查询主要读取的表或子查询。
FROM days d LEFT JOIN events e ON e.occurred_at >= $2::timestamp AT TIME ZONE $4
    -- 再增加一个必须同时满足的筛选条件。
    AND e.occurred_at < ($3+1)::timestamp AT TIME ZONE $4
    -- 再增加一个必须同时满足的筛选条件。
    AND (e.occurred_at AT TIME ZONE $4)::date=d.business_date
-- 把分组字段相同的记录放在一起，分别做数量或耗时汇总。
GROUP BY d.business_date ORDER BY d.business_date;

-- 时间线使用真实事实；同一时间点按种类、主键、事件类型排序，分页结果稳定。
-- 继续列出本次使用的字段：timestamptz、timestamptz、整数（integer）。
PREPARE statistics_timeline(bigint,timestamptz,timestamptz,integer,integer) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH events AS (
    -- 读取后面列出的字段或计算结果。
    SELECT 'GOAL' AS kind,id AS fact_id,goal_id AS object_id,event_type,occurred_at,name_snapshot AS label
        -- 指定本次查询主要读取的表或子查询。
        FROM goal_state_history WHERE user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'STAGE',id,stage_id,event_type,occurred_at,name_snapshot FROM stage_state_history WHERE user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'TASK',id,task_id,event_type,occurred_at,name_snapshot FROM task_state_history WHERE user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'HABIT',c.id,c.habit_id,'CHECKIN',c.checked_at,r.name
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_checkin c JOIN habit_revision r ON r.id=c.revision_id AND r.user_id=c.user_id AND r.habit_id=c.habit_id WHERE c.user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'HABIT',c.id,c.habit_id,'CHECKIN_CANCELLED',c.cancelled_at,r.name
        -- 指定本次查询主要读取的表或子查询。
        FROM habit_checkin c JOIN habit_revision r ON r.id=c.revision_id AND r.user_id=c.user_id AND r.habit_id=c.habit_id
        -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        WHERE c.user_id=$1 AND c.cancelled_at IS NOT NULL
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'EXECUTION',sg.id,es.task_id,'SEGMENT_STARTED',sg.started_at,NULL
        -- 指定本次查询主要读取的表或子查询。
        FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'EXECUTION',sg.id,es.task_id,sg.end_reason,sg.ended_at,NULL
        -- 指定本次查询主要读取的表或子查询。
        FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1 AND sg.ended_at IS NOT NULL
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'ASSIGNMENT',a.id,a.task_id,'ASSIGNED',a.assigned_at,NULL FROM task_assignment a WHERE a.user_id=$1
    -- 合并前后两段查询结果，保留重复行。
    UNION ALL SELECT 'ASSIGNMENT',a.id,a.task_id,a.end_reason,a.ended_at,NULL FROM task_assignment a WHERE a.user_id=$1 AND a.ended_at IS NOT NULL
)
-- 读取后面列出的字段或计算结果。
SELECT kind,fact_id,object_id,event_type,occurred_at,label FROM events
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE occurred_at>=$2 AND occurred_at<$3
-- 按列出的字段排序，ASC 为升序，DESC 为降序。
ORDER BY occurred_at DESC,kind,fact_id DESC,event_type
-- 限制本次最多返回的记录数，配合 OFFSET 实现分页。
LIMIT LEAST(100,GREATEST(1,$4)) OFFSET GREATEST(0,$5);

-- 当前对象概况与历史完成动作分开，不通过当前状态倒推某一天的过去。
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：PREPARE statistics_objects(bigint) AS。
PREPARE statistics_objects(bigint) AS
-- 读取后面列出的字段或计算结果。
SELECT 'GOAL' AS kind,status,count(*) AS object_count FROM goal
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE user_id=$1 AND deleted_at IS NULL AND archived_at IS NULL GROUP BY status
-- 合并前后两段查询结果，保留重复行。
UNION ALL SELECT 'STAGE',s.status,count(*) FROM stage s JOIN goal g ON g.id=s.goal_id AND g.user_id=s.user_id
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE s.user_id=$1 AND s.deleted_at IS NULL AND g.deleted_at IS NULL AND g.archived_at IS NULL GROUP BY s.status
-- 合并前后两段查询结果，保留重复行。
UNION ALL SELECT 'TASK',t.status,count(*) FROM task t JOIN stage s ON s.id=t.stage_id AND s.user_id=t.user_id
    -- 按这里的关联条件连接另一张表的记录。
    JOIN goal g ON g.id=s.goal_id AND g.user_id=t.user_id
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE t.user_id=$1 AND t.deleted_at IS NULL AND s.deleted_at IS NULL AND g.deleted_at IS NULL AND g.archived_at IS NULL GROUP BY t.status
-- 按列出的字段排序，ASC 为升序，DESC 为降序。
ORDER BY kind,status;
