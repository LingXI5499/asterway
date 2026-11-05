-- Asterway dashboard 模块：读取当前计划、习惯、目标和执行事实；不另建看板表或复制任务。
-- PREPARE 为 SQL 校验的连接内只读原型；全部参数由未来 Service 校验及绑定。
-- today/timeZone/serverNow 取账号有效时区和服务端时间，不能信任客户端时间。

PREPARE dashboard_habits(bigint,date) AS

WITH days AS (
    SELECT ($2::date + n)::date AS business_date
    FROM generate_series(0, $2::date - $2::date) AS n
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
SELECT habit_id,revision_id,category_id,name,expected_time,status,deleted_at,checkin_id,
    done_count,due_count,(checkin_id IS NOT NULL AND deleted_at IS NULL) AS can_cancel
FROM habit_days ORDER BY expected_time NULLS LAST,habit_id;

-- 分别返回当天有日期任务和本周未分配日期的任务，不把周待安排误计入今日计划。
PREPARE dashboard_tasks(bigint,date,integer,integer) AS
SELECT t.id AS task_id,t.name,t.status,t.version,g.id AS goal_id,s.id AS stage_id,
    a.id AS assignment_id,d.plan_date,CASE WHEN d.id IS NULL THEN 'WEEK_UNDATED' ELSE 'TODAY' END AS section,
    t.priority,t.due_date,a.sort_order
FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id AND t.deleted_at IS NULL
JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id AND s.deleted_at IS NULL
JOIN goal g ON g.id=s.goal_id AND g.user_id=a.user_id AND g.deleted_at IS NULL AND g.archived_at IS NULL
JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=a.user_id AND w.deleted_at IS NULL
LEFT JOIN daily_plan d ON d.id=a.daily_plan_id AND d.user_id=a.user_id AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL
WHERE a.user_id=$1 AND a.ended_at IS NULL AND w.week_start_date=date_trunc('week',$2::timestamp)::date
    AND ((a.daily_plan_id IS NULL) OR d.plan_date=$2)
ORDER BY section,a.sort_order,a.id
LIMIT LEAST(100,GREATEST(1,$3)) OFFSET GREATEST(0,$4);

-- 先聚合每目标任务，再查询列表；不与一对多执行段直接联结后重复累加权重。
PREPARE dashboard_goals(bigint,integer,integer) AS
WITH task_totals AS (
    SELECT s.goal_id,count(*) AS task_count,count(*) FILTER(WHERE t.status='COMPLETED') AS completed_tasks,
        sum(t.weight::numeric) AS total_weight,COALESCE(sum(t.weight::numeric) FILTER(WHERE t.status='COMPLETED'),0) AS completed_weight
    FROM stage s JOIN task t ON t.stage_id=s.id AND t.user_id=s.user_id AND t.deleted_at IS NULL
    WHERE s.user_id=$1 AND s.deleted_at IS NULL GROUP BY s.goal_id
)
SELECT g.id,g.name,g.status,g.version,g.completion_condition_met,
    COALESCE(tt.task_count,0) AS task_count,COALESCE(tt.completed_tasks,0) AS completed_tasks,
    round(100.0*tt.completed_weight/NULLIF(tt.total_weight,0),2) AS current_progress
FROM goal g LEFT JOIN task_totals tt ON tt.goal_id=g.id WHERE g.user_id=$1 AND g.deleted_at IS NULL AND g.archived_at IS NULL
ORDER BY g.created_at DESC,g.id DESC LIMIT LEAST(100,GREATEST(1,$2)) OFFSET GREATEST(0,$3);

PREPARE dashboard_overview(bigint,date,text,timestamptz) AS

WITH days AS (
    SELECT ($2::date + n)::date AS business_date
    FROM generate_series(0, $2::date - $2::date) AS n
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
), plan_counts AS (
    SELECT count(*) FILTER (WHERE d.plan_date=$2) AS today_tasks,
        count(*) FILTER (WHERE d.plan_date=$2 AND t.status='COMPLETED') AS today_current_completed_tasks,
        count(*) FILTER (WHERE a.daily_plan_id IS NULL) AS week_undated_tasks
    FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id AND t.deleted_at IS NULL
    JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id AND s.deleted_at IS NULL
    JOIN goal g ON g.id=s.goal_id AND g.user_id=a.user_id AND g.deleted_at IS NULL AND g.archived_at IS NULL
    JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=a.user_id AND w.deleted_at IS NULL
    LEFT JOIN daily_plan d ON d.id=a.daily_plan_id AND d.user_id=a.user_id AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL
    WHERE a.user_id=$1 AND a.ended_at IS NULL AND w.week_start_date=date_trunc('week',$2::timestamp)::date
        AND (a.daily_plan_id IS NULL OR d.plan_date=$2)
), execution_today AS (
    SELECT floor(COALESCE(sum(GREATEST(0,EXTRACT(EPOCH FROM (
        LEAST(LEAST($4,COALESCE(sg.ended_at,LEAST($4,es.last_heartbeat_at+make_interval(secs=>sg.heartbeat_timeout_seconds),sg.eligible_until))),
            ($2+1)::timestamp AT TIME ZONE $3)
        -GREATEST(sg.started_at,$2::timestamp AT TIME ZONE $3)))*1000)),0)/1000)::bigint AS actual_seconds
    FROM execution_session es JOIN execution_segment sg ON sg.execution_id=es.id WHERE es.user_id=$1
), habit_counts AS (
    SELECT COALESCE(sum(done_count),0)::bigint AS habit_done,COALESCE(sum(due_count),0)::bigint AS habit_due FROM habit_days
)
SELECT p.*,e.actual_seconds,h.habit_done,h.habit_due,
    round(100.0*h.habit_done/NULLIF(h.habit_due,0),2) AS habit_completion_rate,
    (SELECT id FROM execution_session WHERE user_id=$1 AND status='RUNNING') AS running_execution_id,
    (SELECT count(*) FROM notification_message WHERE user_id=$1 AND read_at IS NULL AND handle_status<>'INVALIDATED') AS unread_messages
FROM plan_counts p CROSS JOIN execution_today e CROSS JOIN habit_counts h;
