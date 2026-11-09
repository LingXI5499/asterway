-- 定义dashboard表、约束、索引、初始化片段和相关查询；由维护者手工执行。
-- Asterway dashboard 模块：读取当前计划、习惯、目标和执行事实；不另建看板表或复制任务。
-- PREPARE 为 SQL 校验的连接内只读原型；全部参数由未来 Service 校验及绑定。
-- today/timeZone/serverNow 取账号有效时区和服务端时间，不能信任客户端时间。

-- 继续补齐上面的 SQL 表达式、字段值或约束组合：PREPARE dashboard_habits(bigint,date) AS。
PREPARE dashboard_habits(bigint,date) AS

-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2::date + n)::date AS business_date
    -- 指定本次查询主要读取的表或子查询。
    FROM generate_series(0, $2::date - $2::date) AS n
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
SELECT habit_id,revision_id,category_id,name,expected_time,status,deleted_at,checkin_id,
    -- 继续列出本次使用的字段：已完成数量（done_count）、应完成数量（due_count）。
    done_count,due_count,(checkin_id IS NOT NULL AND deleted_at IS NULL) AS can_cancel
-- 指定本次查询主要读取的表或子查询。
FROM habit_days ORDER BY expected_time NULLS LAST,habit_id;

-- 分别返回当天有日期任务和本周未分配日期的任务，不把周待安排误计入今日计划。
-- 继续列出本次使用的字段：日期（date）、整数（integer）。
PREPARE dashboard_tasks(bigint,date,integer,integer) AS
-- 读取后面列出的字段或计算结果。
SELECT t.id AS task_id,t.name,t.status,t.version,g.id AS goal_id,s.id AS stage_id,
    -- 继续列出本次使用的字段：a编号作为任务安排编号（a.id AS assignment_id）、d中的方案日期（d.plan_date）。
    a.id AS assignment_id,d.plan_date,CASE WHEN d.id IS NULL THEN 'WEEK_UNDATED' ELSE 'TODAY' END AS section,
    -- 继续列出本次使用的字段：t中的priority（t.priority）、t中的应完成日期（t.due_date）、a中的排序顺序（a.sort_order）。
    t.priority,t.due_date,a.sort_order
-- 指定本次查询主要读取的表或子查询。
FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id AND t.deleted_at IS NULL
-- 按这里的关联条件连接另一张表的记录。
JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id AND s.deleted_at IS NULL
-- 按这里的关联条件连接另一张表的记录。
JOIN goal g ON g.id=s.goal_id AND g.user_id=a.user_id AND g.deleted_at IS NULL AND g.archived_at IS NULL
-- 按这里的关联条件连接另一张表的记录。
JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=a.user_id AND w.deleted_at IS NULL
-- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
LEFT JOIN daily_plan d ON d.id=a.daily_plan_id AND d.user_id=a.user_id AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL
-- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
WHERE a.user_id=$1 AND a.ended_at IS NULL AND w.week_start_date=date_trunc('week',$2::timestamp)::date
    -- 再增加一个必须同时满足的筛选条件。
    AND ((a.daily_plan_id IS NULL) OR d.plan_date=$2)
-- 按列出的字段排序，ASC 为升序，DESC 为降序。
ORDER BY section,a.sort_order,a.id
-- 限制本次最多返回的记录数，配合 OFFSET 实现分页。
LIMIT LEAST(100,GREATEST(1,$3)) OFFSET GREATEST(0,$4);

-- 先聚合每目标任务，再查询列表；不与一对多执行段直接联结后重复累加权重。
-- 继续列出本次使用的字段：整数（integer）。
PREPARE dashboard_goals(bigint,integer,integer) AS
-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH task_totals AS (
    -- 读取后面列出的字段或计算结果。
    SELECT s.goal_id,count(*) AS task_count,count(*) FILTER(WHERE t.status='COMPLETED') AS completed_tasks,
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        sum(t.weight::numeric) AS total_weight,COALESCE(sum(t.weight::numeric) FILTER(WHERE t.status='COMPLETED'),0) AS completed_weight
    -- 指定本次查询主要读取的表或子查询。
    FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=s.user_id AND t.deleted_at IS NULL
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE s.user_id=$1 AND s.deleted_at IS NULL GROUP BY s.goal_id
)
-- 读取后面列出的字段或计算结果。
SELECT g.id,g.name,g.status,g.version,g.completion_condition_met,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：COALESCE(tt.task_count,0) AS task_count,COALESCE(tt.completed_tasks,0) AS completed_tasks,。
    COALESCE(tt.task_count,0) AS task_count,COALESCE(tt.completed_tasks,0) AS completed_tasks,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：round(100.0*tt.completed_weight/NULLIF(tt.total_weight,0),2) AS current_progress。
    round(100.0*tt.completed_weight/NULLIF(tt.total_weight,0),2) AS current_progress
-- 指定本次查询主要读取的表或子查询。
FROM goal g LEFT JOIN task_totals tt ON tt.goal_id=g.id WHERE g.user_id=$1 AND g.deleted_at IS NULL AND g.archived_at IS NULL
-- 按列出的字段排序，ASC 为升序，DESC 为降序。
ORDER BY g.created_at DESC,g.id DESC LIMIT LEAST(100,GREATEST(1,$2)) OFFSET GREATEST(0,$3);

-- 继续列出本次使用的字段：日期（date）、文字（text）。
PREPARE dashboard_overview(bigint,date,text,timestamptz) AS

-- 先定义可复用的临时查询结果，后续语句继续引用它。
WITH days AS (
    -- 读取后面列出的字段或计算结果。
    SELECT ($2::date + n)::date AS business_date
    -- 指定本次查询主要读取的表或子查询。
    FROM generate_series(0, $2::date - $2::date) AS n
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
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), plan_counts AS (。
), plan_counts AS (
    -- 读取后面列出的字段或计算结果。
    SELECT count(*) FILTER (WHERE d.plan_date=$2) AS today_tasks,
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        count(*) FILTER (WHERE d.plan_date=$2 AND t.status='COMPLETED') AS today_current_completed_tasks,
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：count(*) FILTER (WHERE a.daily_plan_id IS NULL) AS week_undated_tasks。
        count(*) FILTER (WHERE a.daily_plan_id IS NULL) AS week_undated_tasks
    -- 指定本次查询主要读取的表或子查询。
    FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id AND t.deleted_at IS NULL
    -- 按这里的关联条件连接另一张表的记录。
    JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id AND s.deleted_at IS NULL
    -- 按这里的关联条件连接另一张表的记录。
    JOIN goal g ON g.id=s.goal_id AND g.user_id=a.user_id AND g.deleted_at IS NULL AND g.archived_at IS NULL
    -- 按这里的关联条件连接另一张表的记录。
    JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=a.user_id AND w.deleted_at IS NULL
    -- 按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    LEFT JOIN daily_plan d ON d.id=a.daily_plan_id AND d.user_id=a.user_id AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL
    -- 只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    WHERE a.user_id=$1 AND a.ended_at IS NULL AND w.week_start_date=date_trunc('week',$2::timestamp)::date
        -- 再增加一个必须同时满足的筛选条件。
        AND (a.daily_plan_id IS NULL OR d.plan_date=$2)
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), execution_today AS (。
), execution_today AS (
    -- 读取后面列出的字段或计算结果。
    SELECT floor(COALESCE(sum(GREATEST(0,EXTRACT(EPOCH FROM (
        -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        LEAST(LEAST($4,COALESCE(sg.ended_at,LEAST($4,es.last_heartbeat_at+make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))),
            -- 继续补齐上面的 SQL 表达式、字段值或约束组合：($2+1)::timestamp AT TIME ZONE $3)。
            ($2+1)::timestamp AT TIME ZONE $3)
        -- 继续补齐上面的 SQL 表达式、字段值或约束组合：-GREATEST(sg.started_at,$2::timestamp AT TIME ZONE $3)))*1000)),0)/1000)::bigint AS actual_seconds。
        -GREATEST(sg.started_at,$2::timestamp AT TIME ZONE $3)))*1000)),0)/1000)::bigint AS actual_seconds
    -- 指定本次查询主要读取的表或子查询。
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
-- 继续补齐上面的 SQL 表达式、字段值或约束组合：), habit_counts AS (。
), habit_counts AS (
    -- 读取后面列出的字段或计算结果。
    SELECT COALESCE(sum(done_count),0)::bigint AS habit_done,COALESCE(sum(due_count),0)::bigint AS habit_due FROM habit_days
)
-- 读取后面列出的字段或计算结果。
SELECT p.*,e.actual_seconds,h.habit_done,h.habit_due,
    -- 继续补齐上面的 SQL 表达式、字段值或约束组合：round(100.0*h.habit_done/NULLIF(h.habit_due,0),2) AS habit_completion_rate,。
    round(100.0*h.habit_done/NULLIF(h.habit_due,0),2) AS habit_completion_rate,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (SELECT id FROM execution_session WHERE user_id=$1 AND status='RUNNING') AS running_execution_id,
    -- 给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
    (SELECT count(*) FROM notification_message WHERE user_id=$1 AND read_at IS NULL AND handle_status<>'INVALIDATED') AS unread_messages
-- 指定本次查询主要读取的表或子查询。
FROM plan_counts p CROSS JOIN execution_today e CROSS JOIN habit_counts h;
