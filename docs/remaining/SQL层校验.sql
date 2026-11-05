-- 仅由 scripts/review_remaining_sql.ps1 在专用校验库的随机 schema 中执行，所有数据为隔离样例。
-- 不在开发库执行；这里验证数据库行为，不代替未来 Service 的资格检查和 HTTP 验收。
DO $$ BEGIN
    IF current_database()<>'asterway_identity_test' OR current_schema() NOT LIKE 'asterway_remaining_review_%' THEN
        RAISE EXCEPTION 'Refusing to run outside the remaining SQL review schema';
    END IF;
END $$;
CREATE TEMP TABLE remaining_validation_result(name text PRIMARY KEY,passed boolean NOT NULL CHECK(passed));
CREATE FUNCTION pg_temp.remaining_true(label text,condition boolean) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM true THEN RAISE EXCEPTION '%: assertion failed',label; END IF;
    INSERT INTO remaining_validation_result VALUES(label,true);
END $$;
CREATE FUNCTION pg_temp.remaining_reject(label text,statement text,expected_state text DEFAULT '23514') RETURNS void LANGUAGE plpgsql AS $$
DECLARE actual text;
BEGIN
    BEGIN EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual=RETURNED_SQLSTATE;
        IF actual<>expected_state THEN RAISE EXCEPTION '%: expected %, got %',label,expected_state,actual; END IF;
        INSERT INTO remaining_validation_result VALUES(label,true);
        RETURN;
    END;
    RAISE EXCEPTION '%: invalid SQL was accepted',label;
END $$;

-- 无可登录密码的 SQL 样例账号，仅满足哈希格式约束，不执行认证。
INSERT INTO identity_user(id,username,password_hash,nickname,time_zone,is_enabled) OVERRIDING SYSTEM VALUE VALUES
    (101,'habitreview','$argon2id$SQLfixture','habit','Asia/Shanghai',true),
    (202,'otherreview','$argon2id$SQLfixture','other','Asia/Shanghai',true),
    (301,'notifyreview','$argon2id$SQLfixture','notify','Asia/Shanghai',true),
    (302,'mixedreview','$argon2id$SQLfixture','mixed','Asia/Shanghai',true),
    (303,'disabledreview','$argon2id$SQLfixture','disabled','Asia/Shanghai',false);
INSERT INTO identity_user_role(user_id,role_id)
SELECT u.id,r.id FROM identity_user u CROSS JOIN identity_role r WHERE r.code='USER';
INSERT INTO identity_user_role(user_id,role_id) SELECT 302,id FROM identity_role WHERE code='ADMIN';

INSERT INTO habit_category(user_id,name,created_at) VALUES(101,'A','2026-11-01 00:00Z'),(101,'B','2026-11-01 00:00Z'),(202,'A','2026-11-01 00:00Z');
CREATE TEMP TABLE review_habit_key(name text PRIMARY KEY,habit_id bigint UNIQUE);
DO $$
DECLARE item text; new_id bigint;
BEGIN
    FOREACH item IN ARRAY ARRAY['reading','sport','deleted_history','future_start','wednesday'] LOOP
        INSERT INTO habit(user_id,created_at) VALUES(101,'2026-11-02 00:00Z') RETURNING id INTO new_id;
        INSERT INTO review_habit_key VALUES(item,new_id);
    END LOOP;
    INSERT INTO habit(user_id,created_at) VALUES(202,'2026-11-02 00:00Z') RETURNING id INTO new_id;
    INSERT INTO review_habit_key VALUES('other_owner',new_id);
END $$;
INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,status_snapshot,
    effective_date,changed_at,time_zone)
SELECT CASE WHEN k.name='other_owner' THEN 202 ELSE 101 END,k.habit_id,1,'CREATED',k.name,c.id,
    CASE WHEN k.name='wednesday' THEN 'WEEKLY_DAYS' ELSE 'DAILY' END,
    CASE WHEN k.name='wednesday' THEN 4 ELSE 127 END,
    CASE WHEN k.name='future_start' THEN date '2026-11-10' ELSE date '2026-11-02' END,'ACTIVE',
    '2026-11-02','2026-11-02 00:00Z','Asia/Shanghai'
FROM review_habit_key k LEFT JOIN habit_category c ON c.user_id=CASE WHEN k.name='other_owner' THEN 202 ELSE 101 END
    AND c.name=CASE WHEN k.name='deleted_history' THEN 'B' WHEN k.name='wednesday' THEN NULL ELSE 'A' END;

-- 同一天“打卡后改分类再暂停”：完成事实继续归打卡时修订，不能同时在两个分类各算一次。
INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,status_snapshot,
    effective_date,changed_at,time_zone)
SELECT 101,k.habit_id,v.n,v.action_type,'reading',c.id,'WEEKLY_DAYS',4,'2026-11-02',v.status,
    v.day::date,v.changed::timestamptz,'Asia/Shanghai'
FROM review_habit_key k CROSS JOIN(VALUES
    (2,'EDITED','B','ACTIVE','2026-11-03','2026-11-03 00:00Z'),
    (3,'EDITED','A','ACTIVE','2026-11-04','2026-11-04 02:00Z'),
    (4,'PAUSED','A','PAUSED','2026-11-04','2026-11-04 03:00Z')
) AS v(n,action_type,category_name,status,day,changed)
JOIN habit_category c ON c.user_id=101 AND c.name=v.category_name WHERE k.name='reading';
UPDATE habit SET status='PAUSED',current_revision_no=4 WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='reading');
INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,status_snapshot,
    deleted_snapshot,effective_date,changed_at,time_zone)
SELECT 101,k.habit_id,2,'DELETED','deleted_history',c.id,'DAILY',127,'2026-11-02','ACTIVE',true,
    '2026-11-04','2026-11-04 00:00Z','Asia/Shanghai'
FROM review_habit_key k JOIN habit_category c ON c.user_id=101 AND c.name='B' WHERE k.name='deleted_history';
UPDATE habit SET deleted_at='2026-11-04 00:00Z',current_revision_no=2
WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='deleted_history');

INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at,cancelled_at)
SELECT 101,k.habit_id,r.id,v.day::date,'Asia/Shanghai',v.checked::timestamptz,v.cancelled::timestamptz
FROM(VALUES
    ('reading',1,'2026-11-02','2026-11-02 01:00Z',NULL),
    ('reading',2,'2026-11-04','2026-11-04 00:00Z','2026-11-04 00:01Z'),
    ('reading',2,'2026-11-04','2026-11-04 01:00Z',NULL),
    ('sport',1,'2026-11-03','2026-11-03 01:00Z',NULL),
    ('deleted_history',1,'2026-11-03','2026-11-03 01:00Z',NULL)
) AS v(name,revision_no,day,checked,cancelled)
JOIN review_habit_key k ON k.name=v.name JOIN habit_revision r ON r.habit_id=k.habit_id AND r.revision_no=v.revision_no;
UPDATE habit_category SET deleted_at='2026-11-04 00:00Z' WHERE user_id=101 AND name='B';

-- 精确达标、权重四舍五入达标、空目标、已完成、归档、管理身份和停用账号。
INSERT INTO goal(user_id,name,status,archived_at,created_at) VALUES
    (301,'notify_qualified','IN_PROGRESS',NULL,'2026-11-01 00:00Z'),
    (301,'notify_rounded','IN_PROGRESS',NULL,'2026-11-01 00:00Z'),
    (301,'notify_empty','IN_PROGRESS',NULL,'2026-11-01 00:00Z'),
    (301,'notify_completed','COMPLETED',NULL,'2026-11-01 00:00Z'),
    (301,'notify_archived','IN_PROGRESS','2026-11-02 00:00Z','2026-11-01 00:00Z'),
    (302,'notify_managed','IN_PROGRESS',NULL,'2026-11-01 00:00Z'),
    (303,'notify_disabled','IN_PROGRESS',NULL,'2026-11-01 00:00Z');
INSERT INTO stage(user_id,goal_id,name) SELECT user_id,id,name||'_stage' FROM goal WHERE name LIKE 'notify_%';
INSERT INTO task(user_id,stage_id,name,weight,status,completed_at,created_at)
SELECT s.user_id,s.id,CASE g.name WHEN 'notify_qualified' THEN 'notify_done' ELSE g.name||'_done' END,
    CASE WHEN g.name='notify_rounded' THEN 1000000 ELSE 1 END,'COMPLETED','2026-11-03 01:00Z','2026-11-01 00:00Z'
FROM stage s JOIN goal g ON g.id=s.goal_id WHERE g.name LIKE 'notify_%' AND g.name<>'notify_empty';
INSERT INTO task(user_id,stage_id,name) SELECT user_id,id,'notify_tiny_incomplete' FROM stage WHERE name='notify_rounded_stage';
INSERT INTO task(user_id,stage_id,name,status,completed_at,created_at)
SELECT user_id,id,'untrusted_future_completion','COMPLETED','2099-01-01 01:00Z','2026-11-01 00:00Z'
FROM stage WHERE name='notify_completed_stage';

-- 任务当前为进行中，历史同一天两次完成；归档／恢复各有真实事件。
INSERT INTO goal(user_id,name,status) VALUES(401,'history_goal','IN_PROGRESS');
INSERT INTO goal(user_id,name,status,completion_condition_met,completion_round)
VALUES(99999,'orphaned_legacy_goal','IN_PROGRESS',true,4);
INSERT INTO stage(user_id,goal_id,name) SELECT 401,id,'history_stage' FROM goal WHERE name='history_goal';
INSERT INTO task(user_id,stage_id,name,status,version) SELECT 401,id,'history_task','IN_PROGRESS',4 FROM stage WHERE name='history_stage';
INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,entity_version,occurred_at)
SELECT 401,t.id,v.event_type,v.before_status,v.after_status,t.name,v.version,v.time::timestamptz
FROM task t CROSS JOIN(VALUES
    ('CREATED',NULL,'NOT_STARTED',0,'2026-11-03 00:00Z'),
    ('STATUS_CHANGED','NOT_STARTED','COMPLETED',1,'2026-11-03 01:00Z'),
    ('STATUS_CHANGED','COMPLETED','IN_PROGRESS',2,'2026-11-03 02:00Z'),
    ('STATUS_CHANGED','IN_PROGRESS','COMPLETED',3,'2026-11-03 03:00Z'),
    ('STATUS_CHANGED','COMPLETED','IN_PROGRESS',4,'2026-11-04 01:00Z')
) AS v(event_type,before_status,after_status,version,time) WHERE t.name='history_task';
INSERT INTO goal_state_history(user_id,goal_id,event_type,before_status,after_status,before_archived,after_archived,name_snapshot,entity_version,occurred_at)
SELECT 401,g.id,v.event_type,v.before_status,v.after_status,v.before_archived,v.after_archived,g.name,v.version,v.time::timestamptz
FROM goal g CROSS JOIN(VALUES
    ('CREATED',NULL,'NOT_STARTED',NULL::boolean,false,0,'2026-11-03 00:00Z'),
    ('ARCHIVED','IN_PROGRESS','IN_PROGRESS',false,true,1,'2026-11-03 03:00Z'),
    ('RESTORED','IN_PROGRESS','IN_PROGRESS',true,false,2,'2026-11-04 01:00Z')
) AS v(event_type,before_status,after_status,before_archived,after_archived,version,time) WHERE g.name='history_goal';

-- 执行耗时毫秒、跨午夜、超时、逻辑删除保留及夏令时样例。
INSERT INTO goal(user_id,name) VALUES(501,'execution_stats_goal');
INSERT INTO stage(user_id,goal_id,name) SELECT 501,id,'execution_stats_stage' FROM goal WHERE name='execution_stats_goal';
INSERT INTO task(user_id,stage_id,name,deleted_at)
SELECT 501,id,'execution_deleted_task','2026-11-04 04:00Z' FROM stage WHERE name='execution_stats_stage';
INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
VALUES(501,1001,'FINISHED','2026-11-03 15:59:59.400Z','2026-11-04 00:00:00.600Z','2026-11-04 00:00:00.600Z','USER_FINISH');
INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,ended_at,end_reason,eligible_until,time_zone)
SELECT id,1001,v.n,v.starts::timestamptz,v.ends::timestamptz,'USER_PAUSE','2026-11-04 16:00Z','Asia/Shanghai'
FROM execution_session CROSS JOIN(VALUES
    (1,'2026-11-03 15:59:59.400Z','2026-11-03 16:00:00.100Z'),
    (2,'2026-11-04 00:00Z','2026-11-04 00:00:00.600Z')
) AS v(n,starts,ends) WHERE user_id=501;
INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
SELECT 501,id,'FINISHED','2026-11-04 01:00Z','2026-11-04 01:01Z','2026-11-04 01:01Z','TASK_DELETED'
FROM task WHERE name='execution_deleted_task';
INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,ended_at,end_reason,eligible_until,time_zone)
SELECT es.id,1002,1,es.started_at,es.finished_at,'FINISH','2026-11-04 16:00Z','Asia/Shanghai'
FROM execution_session es JOIN task t ON t.id=es.task_id WHERE t.name='execution_deleted_task' AND es.user_id=501;
INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,controller_session_id)
VALUES(501,1003,'RUNNING','2026-11-04 02:00Z','2026-11-04 02:00:10Z','SQL-review-sid');
INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,eligible_until,time_zone)
SELECT id,1003,1,started_at,'2026-11-04 16:00Z','Asia/Shanghai' FROM execution_session WHERE user_id=501 AND status='RUNNING';
INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
VALUES(503,2001,'FINISHED','2026-03-08 05:00Z','2026-03-09 04:00Z','2026-03-09 04:00Z','USER_FINISH'),
      (503,2002,'FINISHED','2026-11-01 04:00Z','2026-11-02 05:00Z','2026-11-02 05:00Z','USER_FINISH');
INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,ended_at,end_reason,eligible_until,time_zone)
SELECT id,task_id,1,started_at,finished_at,'FINISH',finished_at,'America/New_York' FROM execution_session WHERE user_id=503;

-- 看板一条今天、一条周未安排、一条明天；进度为完成权重 2 / 总权重 10。
INSERT INTO goal(user_id,name) VALUES(601,'dashboard_goal');
INSERT INTO stage(user_id,goal_id,name) SELECT 601,id,'dashboard_stage' FROM goal WHERE name='dashboard_goal';
INSERT INTO task(user_id,stage_id,name,weight,status,completed_at)
SELECT 601,s.id,v.name,v.weight,v.status,v.completed::timestamptz FROM stage s CROSS JOIN(VALUES
    ('dashboard_today',2,'COMPLETED','2026-11-04 01:00Z'),
    ('dashboard_undated',3,'NOT_STARTED',NULL),
    ('dashboard_tomorrow',5,'NOT_STARTED',NULL)
) AS v(name,weight,status,completed) WHERE s.name='dashboard_stage';
INSERT INTO weekly_plan(user_id,week_start_date) VALUES(601,'2026-11-02');
INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date) SELECT 601,id,v.day::date FROM weekly_plan
CROSS JOIN(VALUES('2026-11-04'),('2026-11-05')) AS v(day) WHERE user_id=601;
INSERT INTO task_assignment(user_id,task_id,weekly_plan_id,daily_plan_id)
SELECT 601,t.id,w.id,d.id FROM task t CROSS JOIN weekly_plan w LEFT JOIN daily_plan d
    ON d.user_id=601 AND d.weekly_plan_id=w.id AND d.plan_date=CASE t.name
        WHEN 'dashboard_today' THEN date '2026-11-04' WHEN 'dashboard_tomorrow' THEN date '2026-11-05' ELSE NULL END
WHERE t.user_id=601 AND w.user_id=601;

SELECT pg_temp.remaining_true('cancelled_checkin_kept_alongside_one_valid_record',
    (SELECT count(*)=2 AND count(*) FILTER(WHERE cancelled_at IS NULL)=1 FROM habit_checkin
    WHERE habit_id=(SELECT habit_id FROM review_habit_key WHERE name='reading') AND business_date='2026-11-04'));
SELECT pg_temp.remaining_true('goal_rounded_progress_fixture_is_100',
    (SELECT round(100.0*sum(t.weight::numeric) FILTER(WHERE t.status='COMPLETED')/sum(t.weight::numeric),2)=100.00
    FROM task t JOIN stage s ON s.id=t.stage_id JOIN goal g ON g.id=s.goal_id WHERE g.name='notify_rounded'));

SELECT pg_temp.remaining_reject('category_blank_name',$case$INSERT INTO habit_category(user_id,name) VALUES(101,'   ')$case$,'23514');
SELECT pg_temp.remaining_reject('category_untrimmed_name',$case$INSERT INTO habit_category(user_id,name) VALUES(101,' A')$case$,'23514');
SELECT pg_temp.remaining_reject('category_owner_positive',$case$INSERT INTO habit_category(user_id,name) VALUES(0,'bad')$case$,'23514');
SELECT pg_temp.remaining_reject('category_sort_nonnegative',$case$INSERT INTO habit_category(user_id,name,sort_order) VALUES(101,'bad',-1)$case$,'23514');
SELECT pg_temp.remaining_reject('category_duplicate_case_insensitive',$case$INSERT INTO habit_category(user_id,name) VALUES(101,'a')$case$,'23505');
SELECT pg_temp.remaining_reject('habit_invalid_status',$case$INSERT INTO habit(user_id,status) VALUES(101,'ARCHIVED')$case$,'23514');
SELECT pg_temp.remaining_reject('habit_end_time_required',$case$INSERT INTO habit(user_id,status) VALUES(101,'ENDED')$case$,'23514');
SELECT pg_temp.remaining_reject('habit_active_cannot_have_end_time',$case$INSERT INTO habit(user_id,ended_at) VALUES(101,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('habit_revision_number_positive',$case$INSERT INTO habit(user_id,current_revision_no) VALUES(101,0)$case$,'23514');
SELECT pg_temp.remaining_reject('habit_version_nonnegative',$case$INSERT INTO habit(user_id,version) VALUES(101,-1)$case$,'23514');
SELECT pg_temp.remaining_reject('habit_locked_date_finite',$case$INSERT INTO habit(user_id,history_locked_through) VALUES(101,'infinity')$case$,'23514');
SELECT pg_temp.remaining_reject('checkin_valid_date_unique',$case$INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at) SELECT 101,habit_id,1,'2026-11-04','UTC','2026-11-04 01:00Z' FROM review_habit_key WHERE name='reading'$case$,'23505');
SELECT pg_temp.remaining_reject('checkin_cancel_not_before_check',$case$INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at,cancelled_at) VALUES(101,900,1,'2026-11-04','UTC','2026-11-04 01:00Z','2026-11-04 00:59Z')$case$,'23514');
SELECT pg_temp.remaining_reject('checkin_business_date_finite',$case$INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at) VALUES(101,900,1,'infinity','UTC',clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('checkin_timezone_nonblank',$case$INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at) VALUES(101,900,1,'2026-11-04',' ',clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('checkin_owner_positive',$case$INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at) VALUES(-1,900,1,'2026-11-04','UTC',clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('notice_round_positive',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content) VALUES(101,1,0,'t','c')$case$,'23514');
SELECT pg_temp.remaining_reject('notice_type_is_known',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,message_type) VALUES(101,1,1,'t','c','BROADCAST')$case$,'23514');
SELECT pg_temp.remaining_reject('notice_pending_cannot_have_handled_time',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,handled_at) VALUES(101,1,1,'t','c',clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('notice_processed_requires_handled_time',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,handle_status) VALUES(101,1,1,'t','c','CONFIRMED')$case$,'23514');
SELECT pg_temp.remaining_reject('notice_read_time_not_before_created',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,read_at) VALUES(101,1,1,'t','c','2020-01-01')$case$,'23514');
SELECT pg_temp.remaining_reject('notice_title_nonblank',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content) VALUES(101,1,1,' ','c')$case$,'23514');
SELECT pg_temp.remaining_reject('notice_version_nonnegative',$case$INSERT INTO notification_message(user_id,goal_id,condition_round,title,content,version) VALUES(101,1,1,'t','c',-1)$case$,'23514');
SELECT pg_temp.remaining_reject('goal_condition_requires_positive_round',$case$INSERT INTO goal(user_id,name,completion_condition_met) VALUES(101,'bad',true)$case$,'23514');
SELECT pg_temp.remaining_reject('goal_completed_cannot_retain_met_condition',$case$INSERT INTO goal(user_id,name,status,completion_condition_met,completion_round) VALUES(101,'bad','COMPLETED',true,1)$case$,'23514');
SELECT pg_temp.remaining_reject('goal_archived_cannot_retain_met_condition',$case$INSERT INTO goal(user_id,name,archived_at,completion_condition_met,completion_round) VALUES(101,'bad',clock_timestamp(),true,1)$case$,'23514');
SELECT pg_temp.remaining_reject('goal_round_nonnegative',$case$INSERT INTO goal(user_id,name,completion_round) VALUES(101,'bad',-1)$case$,'23514');
SELECT pg_temp.remaining_reject('history_no_fake_noop_status_event',$case$INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,entity_version,occurred_at) VALUES(101,999,'STATUS_CHANGED','IN_PROGRESS','IN_PROGRESS','bad',0,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('history_status_changed_requires_before',$case$INSERT INTO stage_state_history(user_id,stage_id,event_type,after_status,name_snapshot,entity_version,occurred_at) VALUES(101,999,'STATUS_CHANGED','IN_PROGRESS','bad',0,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('goal_history_requires_real_archive_transition',$case$INSERT INTO goal_state_history(user_id,goal_id,event_type,before_status,after_status,before_archived,after_archived,name_snapshot,entity_version,occurred_at) VALUES(101,999,'ARCHIVED','IN_PROGRESS','IN_PROGRESS',true,true,'bad',0,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('history_invalid_import_source',$case$INSERT INTO task_state_history(user_id,task_id,event_type,after_status,name_snapshot,entity_version,occurred_at) VALUES(101,999,'COMPLETION_IMPORTED','COMPLETED','bad',0,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('history_negative_subject_version',$case$INSERT INTO task_state_history(user_id,task_id,event_type,after_status,name_snapshot,entity_version,occurred_at) VALUES(101,999,'CREATED','NOT_STARTED','bad',-1,clock_timestamp())$case$,'23514');
SELECT pg_temp.remaining_reject('history_infinite_occurrence_time',$case$INSERT INTO task_state_history(user_id,task_id,event_type,after_status,name_snapshot,entity_version,occurred_at) VALUES(101,999,'CREATED','NOT_STARTED','bad',0,'infinity')$case$,'23514');
SELECT pg_temp.remaining_reject('history_same_subject_version_event_unique',$case$INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,entity_version,occurred_at) SELECT 401,id,'STATUS_CHANGED','NOT_STARTED','IN_PROGRESS','dup',1,clock_timestamp() FROM task WHERE name='history_task'$case$,'23505');
SELECT pg_temp.remaining_reject('revision_daily_requires_all_weekdays',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,1,'CREATED','bad','DAILY',1,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_weekday_mask_not_zero',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,1,'CREATED','bad','WEEKLY_DAYS',0,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_weekday_mask_not_above_127',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,1,'CREATED','bad','WEEKLY_DAYS',128,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_time_minutes_only',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone,expected_time) VALUES(101,999,1,'CREATED','bad','DAILY',127,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC','12:34:01')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_time_cannot_be_24h',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone,expected_time) VALUES(101,999,1,'CREATED','bad','DAILY',127,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC','24:00')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_created_action_requires_first',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,2,'CREATED','bad','DAILY',127,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_pause_requires_paused_snapshot',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,2,'PAUSED','bad','DAILY',127,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_delete_requires_deleted_snapshot',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) VALUES(101,999,2,'DELETED','bad','DAILY',127,'2026-11-04','ACTIVE','2026-11-04',clock_timestamp(),'UTC')$case$,'23514');
SELECT pg_temp.remaining_reject('revision_number_unique',$case$INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,start_date,status_snapshot,effective_date,changed_at,time_zone) SELECT 101,habit_id,1,'CREATED','dup','DAILY',127,'2026-11-02','ACTIVE','2026-11-02',clock_timestamp(),'UTC' FROM review_habit_key WHERE name='reading'$case$,'23505');
-- 无物理外键意味着 Service 必须校验归属，普通 CHECK 不伪装成跨表一致性。
SELECT pg_temp.remaining_true('checkin_supports_valid_recheck_after_cancel',
    (SELECT count(*) FILTER(WHERE cancelled_at IS NOT NULL)=1 FROM habit_checkin
    WHERE habit_id=(SELECT habit_id FROM review_habit_key WHERE name='reading')));
SELECT pg_temp.remaining_true('habit_current_snapshot_is_paused',
    (SELECT h.status='PAUSED' AND h.current_revision_no=4 FROM habit h JOIN review_habit_key k ON k.habit_id=h.id WHERE k.name='reading'));
