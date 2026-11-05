-- 仅由 scripts/review_execution_sql.ps1 装载到专用校验库的随机 schema，最后回滚。
-- SQL 检查不代表 Service、HTTP、登录认证或前端已经实现。
DO $$ BEGIN
    IF current_database() <> 'asterway_identity_test'
       OR current_schema() NOT LIKE 'asterway_execution_review_%' THEN
        RAISE EXCEPTION 'Refusing to run outside the dedicated execution review schema';
    END IF;
END $$;

CREATE TEMP TABLE execution_validation_result (name text PRIMARY KEY, passed boolean NOT NULL);
CREATE FUNCTION pg_temp.expect_true(label text, condition boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM true THEN RAISE EXCEPTION '%: assertion failed', label; END IF;
    INSERT INTO execution_validation_result VALUES (label, true);
END $$;
CREATE FUNCTION pg_temp.expect_rejection(label text, statement text, expected_state text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE actual_state text;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual_state = RETURNED_SQLSTATE;
        IF actual_state <> expected_state THEN
            RAISE EXCEPTION '%: expected %, got %', label, expected_state, actual_state;
        END IF;
        INSERT INTO execution_validation_result VALUES (label, true);
        RETURN;
    END;
    RAISE EXCEPTION '%: invalid statement was accepted', label;
END $$;

SELECT pg_temp.expect_true('eighteen_business_tables', (
    SELECT count(*) = 18 FROM information_schema.tables
    WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'));
SELECT pg_temp.expect_true('execution_ddl_has_no_sample_data', (
    SELECT (SELECT count(*) FROM execution_session) = 0 AND (SELECT count(*) FROM execution_segment) = 0));
SELECT pg_temp.expect_true('no_physical_foreign_keys', (
    SELECT count(*) = 0 FROM pg_constraint WHERE connamespace = current_schema()::regnamespace AND contype = 'f'));
SELECT pg_temp.expect_true('no_business_triggers', (
    SELECT count(*) = 0 FROM pg_trigger tr JOIN pg_class c ON c.oid = tr.tgrelid
    WHERE c.relnamespace = current_schema()::regnamespace AND NOT tr.tgisinternal));
SELECT pg_temp.expect_true('all_timestamps_have_millisecond_precision', (
    SELECT count(*) = 10 AND bool_and(datetime_precision = 3) FROM information_schema.columns
    WHERE table_schema = current_schema() AND table_name IN ('execution_session', 'execution_segment')
      AND data_type = 'timestamp with time zone'));
SELECT pg_temp.expect_true('execution_columns_have_comments', (
    SELECT bool_and(col_description(a.attrelid, a.attnum) IS NOT NULL) FROM pg_attribute a
    WHERE a.attrelid IN ('execution_session'::regclass, 'execution_segment'::regclass)
      AND a.attnum > 0 AND NOT a.attisdropped));
SELECT pg_temp.expect_true('seven_permissions_disabled_before_backend', (
    SELECT count(*) = 7 AND bool_and(NOT is_enabled AND is_builtin AND deleted_at IS NULL)
    FROM identity_permission WHERE code LIKE 'execution:%'));
SELECT pg_temp.expect_true('six_execution_menus_disabled_and_hidden', (
    SELECT count(*) = 6 AND bool_and(NOT is_enabled AND NOT is_visible)
    FROM identity_menu WHERE code = 'growth_executions' OR code LIKE 'growth_execution_%'));
SELECT pg_temp.expect_true('execution_default_grants_only_user', (
    SELECT count(*) = 7 AND bool_and(r.code = 'USER') FROM identity_role_permission rp
    JOIN identity_role r ON r.id = rp.role_id JOIN identity_permission p ON p.id = rp.permission_id
    WHERE p.code LIKE 'execution:%'));
SELECT pg_temp.expect_true('legacy_goal_delete_untouched_before_backend', (
    SELECT is_enabled FROM identity_permission WHERE code = 'goal:goal:delete'));

-- 固定时间点和隔离数据便于重复复核，不使用本机当前日历日期推导业务日期。
INSERT INTO goal(user_id, name, start_date) VALUES
    (101, 'review_goal', '2026-11-02'), (202, 'review_other_goal', '2026-11-02'),
    (101, 'review_future_goal', '2026-11-05');
INSERT INTO stage(user_id, goal_id, name, start_date, end_date)
SELECT user_id, id, name || '_stage', '2026-12-01', '2026-12-31' FROM goal;
INSERT INTO task(user_id, stage_id, name)
SELECT 101, s.id, v.name FROM stage s CROSS JOIN (VALUES
    ('duration'), ('timeout'), ('rounding'), ('midnight'), ('dst_fall'), ('dst_spring'),
    ('paused'), ('eligible_week'), ('eligible_today'), ('past_day'), ('future_day'),
    ('next_week'), ('unassigned'), ('future_task'), ('deleted_task'), ('completed_task')
) AS v(name) WHERE s.name = 'review_goal_stage';
INSERT INTO task(user_id, stage_id, name)
SELECT user_id, id, 'boundary' FROM stage WHERE name = 'review_other_goal_stage';
INSERT INTO task(user_id, stage_id, name)
SELECT user_id, id, 'future_goal_task' FROM stage WHERE name = 'review_future_goal_stage';
UPDATE task SET start_date = '2026-11-05' WHERE name = 'future_task';
UPDATE task SET deleted_at = '2026-11-04 00:00+08' WHERE name = 'deleted_task';
UPDATE task SET status = 'COMPLETED', completed_at = '2026-11-04 00:00+08' WHERE name = 'completed_task';
INSERT INTO weekly_plan(user_id, week_start_date) VALUES
    (101, '2026-11-02'), (101, '2026-11-09'), (202, '2026-11-02'),
    (101, '2026-10-26'), (101, '2026-03-02');
INSERT INTO daily_plan(user_id, weekly_plan_id, plan_date)
SELECT 101, w.id, v.d FROM weekly_plan w CROSS JOIN
    (VALUES (DATE '2026-11-03'), (DATE '2026-11-04'), (DATE '2026-11-05')) AS v(d)
WHERE w.user_id = 101 AND w.week_start_date = '2026-11-02';
INSERT INTO task_assignment(user_id, task_id, weekly_plan_id, daily_plan_id, assigned_at)
SELECT t.user_id, t.id, w.id, d.id, '2026-03-01 00:00Z' FROM task t
JOIN weekly_plan w ON w.user_id = t.user_id AND w.week_start_date = CASE t.name
    WHEN 'next_week' THEN DATE '2026-11-09' WHEN 'dst_fall' THEN DATE '2026-10-26'
    WHEN 'dst_spring' THEN DATE '2026-03-02' ELSE DATE '2026-11-02' END
LEFT JOIN daily_plan d ON d.weekly_plan_id = w.id AND d.plan_date = CASE t.name
    WHEN 'eligible_today' THEN DATE '2026-11-04' WHEN 'past_day' THEN DATE '2026-11-03'
    WHEN 'future_day' THEN DATE '2026-11-05' ELSE NULL END
WHERE t.name <> 'unassigned';

CREATE TEMP TABLE execution_fixture (label text PRIMARY KEY, id bigint NOT NULL);
CREATE FUNCTION pg_temp.fixture_session(label text, owner_id bigint, task_name text, state text,
    starts timestamptz, heartbeat timestamptz, finishes timestamptz DEFAULT NULL)
RETURNS bigint LANGUAGE plpgsql AS $$
DECLARE session_id bigint;
BEGIN
    INSERT INTO execution_session(user_id, task_id, status, started_at, last_heartbeat_at,
        finished_at, finish_reason, controller_session_id)
    SELECT owner_id, t.id, state, starts, heartbeat, finishes,
        CASE WHEN state = 'FINISHED' THEN 'USER_FINISH' END,
        CASE WHEN state = 'RUNNING' THEN 'sql-review-login-session' END
    FROM task t WHERE t.name = task_name RETURNING id INTO STRICT session_id;
    INSERT INTO execution_fixture VALUES (label, session_id);
    RETURN session_id;
END $$;
CREATE FUNCTION pg_temp.fixture_segment(session_label text, seq integer, starts timestamptz,
    ends timestamptz, reason text, deadline timestamptz, zone text DEFAULT 'Asia/Shanghai')
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO execution_segment(execution_id, assignment_id, segment_no, started_at,
        ended_at, end_reason, eligible_until, time_zone)
    SELECT e.id, a.id, seq, starts, ends, reason, deadline, zone FROM execution_fixture f
    JOIN execution_session e ON e.id = f.id JOIN task_assignment a ON a.task_id = e.task_id
      AND a.user_id = e.user_id AND a.ended_at IS NULL WHERE f.label = session_label;
END $$;

SELECT pg_temp.fixture_session('duration', 101, 'duration', 'FINISHED',
    '2026-11-04 09:00+08', '2026-11-04 10:44:40+08', '2026-11-04 10:45+08');
SELECT pg_temp.fixture_segment('duration', 1, '2026-11-04 09:00+08', '2026-11-04 09:25+08',
    'USER_PAUSE', '2026-11-05 00:00+08');
-- 第一段结束后改期，后续段保留新的安排引用，不覆盖第一段。
UPDATE task_assignment SET ended_at = '2026-11-04 09:30+08', end_reason = 'RESCHEDULED', version = version + 1
WHERE task_id = (SELECT id FROM task WHERE name = 'duration');
INSERT INTO task_assignment(user_id, task_id, weekly_plan_id, daily_plan_id, assigned_at)
SELECT 101, t.id, d.weekly_plan_id, d.id, '2026-11-04 09:30+08' FROM task t CROSS JOIN daily_plan d
WHERE t.name = 'duration' AND d.user_id = 101 AND d.plan_date = '2026-11-04';
SELECT pg_temp.fixture_segment('duration', 2, '2026-11-04 09:40+08', '2026-11-04 10:10+08',
    'USER_PAUSE', '2026-11-05 00:00+08');
SELECT pg_temp.fixture_segment('duration', 3, '2026-11-04 10:30+08', '2026-11-04 10:45+08',
    'FINISH', '2026-11-05 00:00+08');
SELECT pg_temp.fixture_session('timeout', 101, 'timeout', 'RUNNING',
    '2026-11-04 09:55+08', '2026-11-04 10:00+08');
SELECT pg_temp.fixture_segment('timeout', 1, '2026-11-04 09:55+08', NULL, NULL, '2026-11-05 00:00+08');
SELECT pg_temp.fixture_session('boundary', 202, 'boundary', 'RUNNING',
    '2026-11-04 23:59+08', '2026-11-04 23:59:50+08');
SELECT pg_temp.fixture_segment('boundary', 1, '2026-11-04 23:59+08', NULL, NULL, '2026-11-05 00:00+08');
SELECT pg_temp.fixture_session('paused', 101, 'paused', 'PAUSED',
    '2026-11-04 09:00+08', '2026-11-04 09:24:40+08');
SELECT pg_temp.fixture_segment('paused', 1, '2026-11-04 09:00+08', '2026-11-04 09:25+08',
    'USER_PAUSE', '2026-11-05 00:00+08');
SELECT pg_temp.fixture_session('rounding', 101, 'rounding', 'PAUSED',
    '2026-11-04 10:00+08', '2026-11-04 10:01+08');
SELECT pg_temp.fixture_segment('rounding', 1, '2026-11-04 10:00+08', '2026-11-04 10:00:00.500+08',
    'USER_PAUSE', '2026-11-05 00:00+08');
SELECT pg_temp.fixture_segment('rounding', 2, '2026-11-04 10:01+08', '2026-11-04 10:01:00.500+08',
    'USER_PAUSE', '2026-11-05 00:00+08');
SELECT pg_temp.fixture_session('midnight', 101, 'midnight', 'FINISHED',
    '2026-11-04 23:50+08', '2026-11-05 00:09:40+08', '2026-11-05 00:10+08');
SELECT pg_temp.fixture_segment('midnight', 1, '2026-11-04 23:50+08', '2026-11-05 00:10+08',
    'FINISH', '2026-11-09 00:00+08');
SELECT pg_temp.fixture_session('dst_fall', 101, 'dst_fall', 'FINISHED',
    '2026-11-01 00:00-04', '2026-11-01 23:59:40-05', '2026-11-02 00:00-05');
SELECT pg_temp.fixture_segment('dst_fall', 1, '2026-11-01 00:00-04', '2026-11-02 00:00-05',
    'FINISH', '2026-11-02 00:00-05', 'America/New_York');
SELECT pg_temp.fixture_session('dst_spring', 101, 'dst_spring', 'FINISHED',
    '2026-03-08 00:00-05', '2026-03-08 23:59:40-04', '2026-03-09 00:00-04');
SELECT pg_temp.fixture_segment('dst_spring', 1, '2026-03-08 00:00-05', '2026-03-09 00:00-04',
    'FINISH', '2026-03-09 00:00-04', 'America/New_York');

-- 关键约束：每条非法写入在子事务内回滚，不能用其他异常冒充期望的约束错误。
SELECT pg_temp.expect_rejection('explicit_generated_id_rejected', $$INSERT INTO execution_session(id,user_id,task_id,started_at,last_heartbeat_at,controller_session_id) VALUES(99999,901,1,now(),now(),'sid')$$, '428C9');
SELECT pg_temp.expect_rejection('one_running_per_user', $$INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id) VALUES(101,99999,now(),now(),'sid')$$, '23505');
SELECT pg_temp.expect_rejection('one_unfinished_per_task', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at) SELECT user_id,task_id,'PAUSED',started_at,last_heartbeat_at FROM execution_session WHERE id=(SELECT id FROM execution_fixture WHERE label='paused')$$, '23505');
SELECT pg_temp.expect_rejection('running_and_paused_same_task_conflict', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at) SELECT user_id,task_id,'PAUSED',started_at,last_heartbeat_at FROM execution_session WHERE id=(SELECT id FROM execution_fixture WHERE label='timeout')$$, '23505');
SELECT pg_temp.expect_rejection('unknown_session_status', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at) VALUES(901,1,'STOPPED',now(),now())$$, '23514');
SELECT pg_temp.expect_rejection('running_without_controller', $$INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at) VALUES(901,1,now(),now())$$, '23514');
SELECT pg_temp.expect_rejection('blank_controller_rejected', $$INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id) VALUES(901,1,now(),now(),'   ')$$, '23514');
SELECT pg_temp.expect_rejection('paused_with_controller_rejected', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,controller_session_id) VALUES(901,1,'PAUSED',now(),now(),'sid')$$, '23514');
SELECT pg_temp.expect_rejection('finished_requires_time_and_reason', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at) VALUES(901,1,'FINISHED',now(),now(),now())$$, '23514');
SELECT pg_temp.expect_rejection('unfinished_cannot_have_finish_metadata', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finish_reason) VALUES(901,1,'PAUSED',now(),now(),'USER_FINISH')$$, '23514');
SELECT pg_temp.expect_rejection('invalid_finish_reason', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason) VALUES(901,1,'FINISHED',now(),now(),now(),'AUTO_COMPLETE')$$, '23514');
SELECT pg_temp.expect_rejection('heartbeat_cannot_precede_start', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at) VALUES(901,1,'PAUSED',now(),now()-interval '1 second')$$, '23514');
SELECT pg_temp.expect_rejection('finish_cannot_precede_heartbeat', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason) VALUES(901,1,'FINISHED',now(),now(),now()-interval '1 second','USER_FINISH')$$, '23514');
SELECT pg_temp.expect_rejection('infinite_session_time_rejected', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at) VALUES(901,1,'PAUSED','infinity','infinity')$$, '23514');
SELECT pg_temp.expect_rejection('invalid_owner_task_version', $$INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,version) VALUES(0,-1,'PAUSED',now(),now(),-1)$$, '23514');
SELECT pg_temp.expect_rejection('one_open_segment_per_session', $$INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,eligible_until,time_zone) SELECT id,1,2,'2026-11-04 10:01+08','2026-11-05 00:00+08','Asia/Shanghai' FROM execution_fixture WHERE label='timeout'$$, '23505');
SELECT pg_temp.expect_rejection('segment_sequence_unique', $$INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,ended_at,end_reason,eligible_until,time_zone) SELECT id,1,1,'2026-11-04 11:00+08','2026-11-04 11:01+08','USER_PAUSE','2026-11-05 00:00+08','Asia/Shanghai' FROM execution_fixture WHERE label='duration'$$, '23505');
SELECT pg_temp.expect_rejection('segment_end_requires_reason', $$UPDATE execution_segment SET end_reason=NULL WHERE end_reason='FINISH'$$, '23514');
SELECT pg_temp.expect_rejection('open_segment_cannot_have_reason', $$UPDATE execution_segment SET end_reason='AUTO_PAUSE' WHERE ended_at IS NULL$$, '23514');
SELECT pg_temp.expect_rejection('segment_end_before_start', $$UPDATE execution_segment SET ended_at=started_at-interval '1 second' WHERE ended_at IS NOT NULL$$, '23514');
SELECT pg_temp.expect_rejection('segment_end_after_eligibility', $$UPDATE execution_segment SET ended_at=eligible_until+interval '1 second' WHERE ended_at IS NOT NULL$$, '23514');
SELECT pg_temp.expect_rejection('empty_eligibility_window', $$UPDATE execution_segment SET eligible_until=started_at WHERE ended_at IS NULL$$, '23514');
SELECT pg_temp.expect_rejection('invalid_segment_reason', $$UPDATE execution_segment SET end_reason='BACKFILL' WHERE ended_at IS NOT NULL$$, '23514');
SELECT pg_temp.expect_rejection('invalid_segment_sequence', $$UPDATE execution_segment SET segment_no=0 WHERE ended_at IS NULL$$, '23514');
SELECT pg_temp.expect_rejection('invalid_timeout_snapshot', $$UPDATE execution_segment SET heartbeat_timeout_seconds=0 WHERE ended_at IS NULL$$, '23514');
SELECT pg_temp.expect_rejection('blank_timezone_rejected', $$UPDATE execution_segment SET time_zone='   ' WHERE ended_at IS NULL$$, '23514');

CREATE TEMP VIEW execution_review_duration AS
SELECT e.user_id, e.id AS execution_id, f.label, s.id AS segment_id, s.started_at,
    COALESCE(s.ended_at, LEAST(TIMESTAMPTZ '2026-11-05 00:03+08',
        e.last_heartbeat_at + make_interval(secs => s.heartbeat_timeout_seconds), s.eligible_until)) AS effective_end
FROM execution_session e JOIN execution_segment s ON s.execution_id = e.id
JOIN execution_fixture f ON f.id = e.id;
SELECT pg_temp.expect_true('pause_intervals_excluded_4200_seconds', (
    SELECT SUM(EXTRACT(EPOCH FROM effective_end-started_at)) = 4200
    FROM execution_review_duration WHERE label='duration'));
SELECT pg_temp.expect_true('reschedule_preserves_segment_assignment', (
    SELECT count(DISTINCT assignment_id)=2 AND count(*)=3 FROM execution_segment
    WHERE execution_id=(SELECT id FROM execution_fixture WHERE label='duration')));
SELECT pg_temp.expect_true('expired_open_segment_capped_at_last_heartbeat_plus_90', (
    SELECT effective_end=TIMESTAMPTZ '2026-11-04 10:01:30+08'
      AND EXTRACT(EPOCH FROM effective_end-started_at)=390
    FROM execution_review_duration WHERE label='timeout'));
SELECT pg_temp.expect_true('eligibility_boundary_precedes_heartbeat_deadline', (
    SELECT effective_end=TIMESTAMPTZ '2026-11-05 00:00+08'
      AND EXTRACT(EPOCH FROM effective_end-started_at)=60
    FROM execution_review_duration WHERE label='boundary'));
SELECT pg_temp.expect_true('round_only_after_summing_segments', (
    SELECT floor(SUM(EXTRACT(EPOCH FROM effective_end-started_at)))=1
      AND SUM(floor(EXTRACT(EPOCH FROM effective_end-started_at)))=0
    FROM execution_review_duration WHERE label='rounding'));
SELECT pg_temp.expect_true('cross_midnight_first_day_600_seconds', (
    SELECT SUM(EXTRACT(EPOCH FROM LEAST(effective_end,TIMESTAMPTZ '2026-11-05 00:00+08')
        -GREATEST(started_at,TIMESTAMPTZ '2026-11-04 00:00+08')))=600
    FROM execution_review_duration WHERE label='midnight'
      AND started_at<TIMESTAMPTZ '2026-11-05 00:00+08' AND effective_end>TIMESTAMPTZ '2026-11-04 00:00+08'));
SELECT pg_temp.expect_true('cross_midnight_second_day_600_seconds', (
    SELECT SUM(EXTRACT(EPOCH FROM LEAST(effective_end,TIMESTAMPTZ '2026-11-06 00:00+08')
        -GREATEST(started_at,TIMESTAMPTZ '2026-11-05 00:00+08')))=600
    FROM execution_review_duration WHERE label='midnight'
      AND started_at<TIMESTAMPTZ '2026-11-06 00:00+08' AND effective_end>TIMESTAMPTZ '2026-11-05 00:00+08'));
SELECT pg_temp.expect_true('dst_fall_day_is_25_hours', (
    SELECT EXTRACT(EPOCH FROM effective_end-started_at)=25*3600
      AND effective_end=(DATE '2026-11-02')::timestamp AT TIME ZONE 'America/New_York'
    FROM execution_review_duration WHERE label='dst_fall'));
SELECT pg_temp.expect_true('dst_spring_day_is_23_hours', (
    SELECT EXTRACT(EPOCH FROM effective_end-started_at)=23*3600
      AND effective_end=(DATE '2026-03-09')::timestamp AT TIME ZONE 'America/New_York'
    FROM execution_review_duration WHERE label='dst_spring'));
SELECT pg_temp.expect_true('other_owner_not_in_history_query', (
    SELECT count(*)=0 FROM execution_review_duration WHERE user_id=101 AND label='boundary'));
SELECT pg_temp.expect_true('session_count_differs_from_segment_count', (
    SELECT count(DISTINCT execution_id)=1 AND count(*)=3 FROM execution_review_duration WHERE label='duration'));

-- 当前可执行资格的查询原型。阶段日期只是参考；任务和目标日期及父链状态参与限制。
CREATE TEMP VIEW execution_review_eligible AS
SELECT t.id, t.name, a.id AS assignment_id FROM task t
JOIN stage s ON s.id=t.stage_id AND s.user_id=t.user_id
JOIN goal g ON g.id=s.goal_id AND g.user_id=t.user_id
JOIN task_assignment a ON a.task_id=t.id AND a.user_id=t.user_id AND a.ended_at IS NULL
JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=t.user_id
LEFT JOIN daily_plan d ON d.id=a.daily_plan_id
WHERE t.user_id=101 AND t.deleted_at IS NULL AND t.status<>'COMPLETED'
  AND s.deleted_at IS NULL AND s.status<>'COMPLETED'
  AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status<>'COMPLETED'
  AND (g.start_date IS NULL OR g.start_date<=DATE '2026-11-04')
  AND (t.start_date IS NULL OR t.start_date<=DATE '2026-11-04')
  AND w.deleted_at IS NULL AND w.week_start_date=DATE '2026-11-02'
  AND (a.daily_plan_id IS NULL OR (d.id IS NOT NULL AND d.user_id=t.user_id
    AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL AND d.plan_date=DATE '2026-11-04'));
SELECT pg_temp.expect_true('undated_current_week_task_is_eligible', EXISTS (
    SELECT 1 FROM execution_review_eligible WHERE name='eligible_week'));
SELECT pg_temp.expect_true('today_task_is_eligible_despite_advisory_stage_dates', EXISTS (
    SELECT 1 FROM execution_review_eligible WHERE name='eligible_today'));
SELECT pg_temp.expect_true('past_future_unassigned_and_inactive_tasks_excluded', NOT EXISTS (
    SELECT 1 FROM execution_review_eligible WHERE name IN ('past_day','future_day','next_week',
        'unassigned','future_task','future_goal_task','deleted_task','completed_task','boundary')));
UPDATE goal SET archived_at='2026-11-04 11:00+08' WHERE name='review_goal';
SELECT pg_temp.expect_true('archived_goal_not_executable', NOT EXISTS (SELECT 1 FROM execution_review_eligible));
SELECT pg_temp.expect_true('archive_keeps_execution_history', (
    SELECT SUM(EXTRACT(EPOCH FROM d.effective_end-d.started_at))=4200 FROM execution_review_duration d
    JOIN execution_session e ON e.id=d.execution_id JOIN task t ON t.id=e.task_id AND t.user_id=e.user_id
    JOIN stage s ON s.id=t.stage_id AND s.user_id=t.user_id JOIN goal g ON g.id=s.goal_id AND g.user_id=t.user_id
    WHERE d.label='duration' AND g.archived_at IS NOT NULL));
UPDATE task SET deleted_at='2026-11-04 11:00+08' WHERE name='duration';
SELECT pg_temp.expect_true('deleted_task_keeps_execution_history', (
    SELECT count(*)=3 FROM execution_review_duration d JOIN execution_session e ON e.id=d.execution_id
    JOIN task t ON t.id=e.task_id AND t.user_id=e.user_id WHERE d.label='duration' AND t.deleted_at IS NOT NULL));
UPDATE goal SET archived_at=NULL WHERE name='review_goal';
UPDATE stage SET status='COMPLETED' WHERE name='review_goal_stage';
SELECT pg_temp.expect_true('completed_parent_stage_not_executable', NOT EXISTS (SELECT 1 FROM execution_review_eligible));
UPDATE stage SET status='NOT_STARTED' WHERE name='review_goal_stage';

SELECT pg_temp.expect_true('multiple_paused_tasks_supported', (
    SELECT count(*)=2 FROM execution_session WHERE user_id=101 AND status='PAUSED'));
UPDATE execution_session SET status='FINISHED', finished_at='2026-11-04 15:00+08', finish_reason='USER_FINISH',
    version=version+1 WHERE id=(SELECT id FROM execution_fixture WHERE label='paused');
SELECT pg_temp.expect_true('finish_after_pause_adds_no_segment_or_time', (
    SELECT count(*)=1 AND SUM(EXTRACT(EPOCH FROM s.ended_at-s.started_at))=1500
    FROM execution_segment s WHERE s.execution_id=(SELECT id FROM execution_fixture WHERE label='paused')));
-- 同任务可以再次拥有已结束会话；终态不恢复、跨表恰好一个开放段仍是 Service 的职责。
INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
SELECT user_id,task_id,'FINISHED','2026-11-04 16:00+08','2026-11-04 16:09:40+08',
    '2026-11-04 16:10+08','USER_FINISH' FROM execution_session WHERE id=(SELECT id FROM execution_fixture WHERE label='paused');
SELECT pg_temp.expect_true('multiple_finished_sessions_per_task', (
    SELECT count(*)=2 FROM execution_session WHERE task_id=(SELECT id FROM task WHERE name='paused') AND status='FINISHED'));

-- 明确验证无外键与触发器的边界，不把这些检查报告成已经完成业务校验。
SELECT pg_temp.expect_true('cross_table_rules_remain_service_responsibility', (
    SELECT count(*)=0 FROM pg_constraint WHERE conrelid IN ('execution_session'::regclass,'execution_segment'::regclass) AND contype='f'));
