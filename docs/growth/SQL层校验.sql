-- 仅用于隔离事务的 SQL 层校验，不部署到开发库；隔离检查脚本装载四份原始 DDL。
-- 必须通过 scripts/review_growth_sql.ps1 执行，禁止直接在开发库执行。
DO $$ BEGIN
    IF current_database() <> 'asterway_identity_test'
       OR current_schema() NOT LIKE 'asterway_growth_review_%' THEN
        RAISE EXCEPTION 'Refusing to run outside the dedicated SQL review schema';
    END IF;
END $$;
CREATE TEMP TABLE validation_result (name text NOT NULL, passed boolean NOT NULL);
CREATE FUNCTION pg_temp.expect_rejection(label text, statement text, expected_state text) RETURNS void
LANGUAGE plpgsql AS $$
DECLARE actual_state text;
BEGIN
    BEGIN
        EXECUTE statement;
    EXCEPTION WHEN OTHERS THEN
        GET STACKED DIAGNOSTICS actual_state = RETURNED_SQLSTATE;
        IF actual_state <> expected_state THEN RAISE EXCEPTION '%: expected %, got %', label, expected_state, actual_state; END IF;
        INSERT INTO validation_result VALUES (label, true);
        RETURN;
    END;
    RAISE EXCEPTION '%: invalid input was accepted', label;
END $$;
CREATE FUNCTION pg_temp.expect_true(label text, condition boolean) RETURNS void
LANGUAGE plpgsql AS $$
BEGIN
    IF condition IS DISTINCT FROM true THEN RAISE EXCEPTION '%: assertion failed', label; END IF;
    INSERT INTO validation_result VALUES (label, true);
END $$;

INSERT INTO goal (user_id,name,start_date,expected_end_date) VALUES
 (101,'goal_a','2026-11-20','2026-12-31'),(101,'goal_b',NULL,NULL),(202,'other_user',NULL,NULL);
INSERT INTO stage (user_id,goal_id,name,start_date,end_date)
 SELECT 101,id,'stage_a','2026-11-01','2027-01-01' FROM goal WHERE name='goal_a';
INSERT INTO stage (user_id,goal_id,name) SELECT 101,id,'stage_b' FROM goal WHERE name='goal_b';
INSERT INTO stage (user_id,goal_id,name) SELECT 101,id,'empty_stage' FROM goal WHERE name='goal_b';
INSERT INTO task (user_id,stage_id,name,weight,status,completed_at)
 SELECT 101,id,'done',2,'COMPLETED',CURRENT_TIMESTAMP FROM stage WHERE name='stage_a';
INSERT INTO task (user_id,stage_id,name,weight,status)
 SELECT 101,id,'paused',3,'PAUSED' FROM stage WHERE name='stage_a';
INSERT INTO task (user_id,stage_id,name,weight) SELECT 101,id,'pending',5 FROM stage WHERE name='stage_a';
INSERT INTO task (user_id,stage_id,name,weight,status,completed_at,deleted_at)
 SELECT 101,id,'deleted_done',100,'COMPLETED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM stage WHERE name='stage_a';
INSERT INTO task (user_id,stage_id,name) SELECT 101,id,'second_goal_task' FROM stage WHERE name='stage_b';
INSERT INTO weekly_plan (user_id,week_start_date) VALUES (101,'2026-11-02'),(101,'2026-11-09'),(202,'2026-11-02');
INSERT INTO daily_plan (user_id,weekly_plan_id,plan_date)
 SELECT 101,id,'2026-11-04' FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-02';
INSERT INTO daily_plan (user_id,weekly_plan_id,plan_date)
 SELECT 101,id,'2026-11-05' FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-02';

SELECT pg_temp.expect_true('six_tables_created', (SELECT count(*)=6 FROM information_schema.tables WHERE table_schema=current_schema()));
SELECT pg_temp.expect_true('no_foreign_keys', (SELECT count(*)=0 FROM pg_constraint WHERE connamespace=current_schema()::regnamespace AND contype='f'));
SELECT pg_temp.expect_true('no_business_triggers', (SELECT count(*)=0 FROM pg_trigger tr JOIN pg_class c ON c.oid=tr.tgrelid WHERE c.relnamespace=current_schema()::regnamespace AND NOT tr.tgisinternal));
SELECT pg_temp.expect_true('future_goal_and_independent_stage_dates', (SELECT count(*)=1 FROM goal g JOIN stage s ON s.goal_id=g.id WHERE s.start_date < g.start_date AND s.end_date > g.expected_end_date));
SELECT pg_temp.expect_true('task_defaults', (SELECT weight=1 AND status='NOT_STARTED' AND priority='MEDIUM' AND estimated_minutes IS NULL FROM task WHERE name='second_goal_task'));
SELECT pg_temp.expect_rejection('blank_goal_name', $$INSERT INTO goal(user_id,name) VALUES(101,'   ')$$,'23514');
SELECT pg_temp.expect_rejection('negative_owner', $$INSERT INTO goal(user_id,name) VALUES(-1,'bad')$$,'23514');
SELECT pg_temp.expect_rejection('goal_reversed_dates', $$INSERT INTO goal(user_id,name,start_date,expected_end_date) VALUES(101,'bad','2026-12-01','2026-11-01')$$,'23514');
SELECT pg_temp.expect_rejection('infinite_date', $$INSERT INTO goal(user_id,name,start_date) VALUES(101,'bad','infinity')$$,'23514');
SELECT pg_temp.expect_rejection('goal_unknown_status', $$INSERT INTO goal(user_id,name,status) VALUES(101,'bad','PAUSED')$$,'23514');
SELECT pg_temp.expect_rejection('stage_reversed_dates', $$INSERT INTO stage(user_id,goal_id,name,start_date,end_date) VALUES(101,1,'bad','2026-12-01','2026-11-01')$$,'23514');
SELECT pg_temp.expect_rejection('zero_weight', $$INSERT INTO task(user_id,stage_id,name,weight) VALUES(101,1,'bad',0)$$,'23514');
SELECT pg_temp.expect_rejection('zero_estimate', $$INSERT INTO task(user_id,stage_id,name,estimated_minutes) VALUES(101,1,'bad',0)$$,'23514');
SELECT pg_temp.expect_rejection('task_invalid_priority', $$INSERT INTO task(user_id,stage_id,name,priority) VALUES(101,1,'bad','TOP')$$,'23514');
SELECT pg_temp.expect_rejection('completed_without_time', $$INSERT INTO task(user_id,stage_id,name,status) VALUES(101,1,'bad','COMPLETED')$$,'23514');
SELECT pg_temp.expect_rejection('incomplete_with_time', $$INSERT INTO task(user_id,stage_id,name,completed_at) VALUES(101,1,'bad',CURRENT_TIMESTAMP)$$,'23514');
SELECT pg_temp.expect_rejection('task_reversed_dates', $$INSERT INTO task(user_id,stage_id,name,start_date,due_date) VALUES(101,1,'bad','2026-12-01','2026-11-01')$$,'23514');
SELECT pg_temp.expect_rejection('negative_version', $$INSERT INTO goal(user_id,name,version) VALUES(101,'bad',-1)$$,'23514');
SELECT pg_temp.expect_rejection('week_must_be_monday', $$INSERT INTO weekly_plan(user_id,week_start_date) VALUES(101,'2026-11-03')$$,'23514');
SELECT pg_temp.expect_rejection('duplicate_live_week', $$INSERT INTO weekly_plan(user_id,week_start_date) VALUES(101,'2026-11-02')$$,'23505');
SELECT pg_temp.expect_rejection('duplicate_live_day', $$INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date) SELECT 101,id,'2026-11-04' FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-02'$$,'23505');

SELECT pg_temp.expect_true('weighted_stage_progress_20_percent', (
 SELECT round(100.0 * COALESCE(SUM(t.weight::numeric) FILTER(WHERE t.status='COMPLETED'),0)/NULLIF(SUM(t.weight::numeric),0),2)=20.00
 FROM stage s LEFT JOIN task t ON t.stage_id=s.id AND t.user_id=s.user_id AND t.deleted_at IS NULL
 WHERE s.user_id=101 AND s.name='stage_a' AND s.deleted_at IS NULL));
SELECT pg_temp.expect_true('empty_stage_progress_null', (
 SELECT (100.0 * COALESCE(SUM(t.weight::numeric) FILTER(WHERE t.status='COMPLETED'),0)/NULLIF(SUM(t.weight::numeric),0)) IS NULL
 FROM stage s LEFT JOIN task t ON t.stage_id=s.id AND t.user_id=s.user_id AND t.deleted_at IS NULL
 WHERE s.user_id=101 AND s.name='empty_stage' AND s.deleted_at IS NULL));
SELECT pg_temp.expect_true('weighted_goal_progress_20_percent', (
 SELECT round(100.0 * COALESCE(SUM(t.weight::numeric) FILTER(WHERE t.status='COMPLETED'),0)/NULLIF(SUM(t.weight::numeric),0),2)=20.00
 FROM goal g LEFT JOIN stage s ON s.goal_id=g.id AND s.user_id=g.user_id AND s.deleted_at IS NULL
 LEFT JOIN task t ON t.stage_id=s.id AND t.user_id=g.user_id AND t.deleted_at IS NULL
 WHERE g.user_id=101 AND g.name='goal_a' AND g.deleted_at IS NULL));
SELECT pg_temp.expect_true('owner_filtered_goal_query', (SELECT count(*)=2 FROM goal WHERE user_id=101 AND deleted_at IS NULL));
DO $$DECLARE changed integer;
BEGIN
 UPDATE task SET description='edited',version=version+1,updated_at=CURRENT_TIMESTAMP WHERE user_id=101 AND name='pending' AND version=0 AND deleted_at IS NULL;
 GET DIAGNOSTICS changed=ROW_COUNT;
 PERFORM pg_temp.expect_true('optimistic_update_success',changed=1);
 UPDATE task SET description='stale',version=version+1 WHERE user_id=101 AND name='pending' AND version=0 AND deleted_at IS NULL;
 GET DIAGNOSTICS changed=ROW_COUNT;
 PERFORM pg_temp.expect_true('stale_version_updates_zero_rows',changed=0);
 UPDATE task SET description='foreign owner' WHERE user_id=202 AND name='pending' AND deleted_at IS NULL;
 GET DIAGNOSTICS changed=ROW_COUNT;
 PERFORM pg_temp.expect_true('foreign_owner_update_zero_rows',changed=0);
END $$;

INSERT INTO task_assignment(user_id,task_id,weekly_plan_id)
 SELECT 101,t.id,w.id FROM task t CROSS JOIN weekly_plan w WHERE t.name='pending' AND w.user_id=101 AND w.week_start_date='2026-11-02';
SELECT pg_temp.expect_rejection('one_current_assignment', $$INSERT INTO task_assignment(user_id,task_id,weekly_plan_id) SELECT 101,t.id,w.id FROM task t CROSS JOIN weekly_plan w WHERE t.name='pending' AND w.user_id=101 AND w.week_start_date='2026-11-09'$$,'23505');
SELECT pg_temp.expect_rejection('current_unique_even_wrong_owner', $$INSERT INTO task_assignment(user_id,task_id,weekly_plan_id) SELECT 202,t.id,w.id FROM task t CROSS JOIN weekly_plan w WHERE t.name='pending' AND w.user_id=202 AND w.week_start_date='2026-11-02'$$,'23505');
SELECT pg_temp.expect_rejection('end_reason_without_time', $$UPDATE task_assignment SET end_reason='RESCHEDULED' WHERE ended_at IS NULL$$,'23514');
SELECT pg_temp.expect_rejection('end_time_without_reason', $$UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP WHERE ended_at IS NULL$$,'23514');
SELECT pg_temp.expect_rejection('end_before_start', $$UPDATE task_assignment SET ended_at=assigned_at-interval '1 second',end_reason='RESCHEDULED' WHERE ended_at IS NULL$$,'23514');
SELECT pg_temp.expect_rejection('unknown_end_reason', $$UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='BAD' WHERE ended_at IS NULL$$,'23514');
UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='RESCHEDULED',version=version+1 WHERE ended_at IS NULL;
INSERT INTO task_assignment(user_id,task_id,weekly_plan_id,daily_plan_id)
 SELECT 101,t.id,d.weekly_plan_id,d.id FROM task t CROSS JOIN daily_plan d WHERE t.name='pending' AND d.user_id=101 AND d.plan_date='2026-11-04';
UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='RESCHEDULED',version=version+1 WHERE ended_at IS NULL;
INSERT INTO task_assignment(user_id,task_id,weekly_plan_id,daily_plan_id)
 SELECT 101,t.id,d.weekly_plan_id,d.id FROM task t CROSS JOIN daily_plan d WHERE t.name='pending' AND d.user_id=101 AND d.plan_date='2026-11-05';
SELECT pg_temp.expect_true('same_week_reschedule_keeps_three_records', (SELECT count(*)=3 AND count(*) FILTER(WHERE ended_at IS NULL)=1 FROM task_assignment));
UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='RESCHEDULED',version=version+1 WHERE ended_at IS NULL;
INSERT INTO task_assignment(user_id,task_id,weekly_plan_id)
 SELECT 101,t.id,w.id FROM task t CROSS JOIN weekly_plan w WHERE t.name='pending' AND w.user_id=101 AND w.week_start_date='2026-11-09';
SELECT pg_temp.expect_true('cross_week_keeps_task_identity', (SELECT count(DISTINCT task_id)=1 AND count(*)=4 FROM task_assignment));
UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='RETURNED_TO_POOL',version=version+1 WHERE ended_at IS NULL;
SELECT pg_temp.expect_true('return_to_pool_preserves_history', (SELECT count(*)=4 AND count(*) FILTER(WHERE ended_at IS NULL)=0 FROM task_assignment));
SELECT pg_temp.expect_true('pool_query_finds_original_task', (SELECT count(*)=1 FROM task t WHERE t.user_id=101 AND t.name='pending' AND t.deleted_at IS NULL AND NOT EXISTS(SELECT 1 FROM task_assignment a WHERE a.task_id=t.id AND a.ended_at IS NULL)));

INSERT INTO task_assignment(user_id,task_id,weekly_plan_id,daily_plan_id)
 SELECT 101,t.id,d.weekly_plan_id,d.id FROM task t CROSS JOIN daily_plan d WHERE t.name IN('pending','second_goal_task') AND d.user_id=101 AND d.plan_date='2026-11-04';
SELECT pg_temp.expect_true('weekly_plan_spans_multiple_goals', (SELECT count(DISTINCT s.goal_id)=2 FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id WHERE a.user_id=101 AND a.ended_at IS NULL));
SELECT pg_temp.expect_true('overdue_pending_query', (SELECT count(*)=2 FROM task_assignment a JOIN task t ON t.id=a.task_id AND t.user_id=a.user_id AND t.deleted_at IS NULL JOIN stage s ON s.id=t.stage_id AND s.user_id=a.user_id AND s.deleted_at IS NULL JOIN goal g ON g.id=s.goal_id AND g.user_id=a.user_id AND g.deleted_at IS NULL AND g.archived_at IS NULL JOIN weekly_plan w ON w.id=a.weekly_plan_id AND w.user_id=a.user_id AND w.deleted_at IS NULL LEFT JOIN daily_plan d ON d.id=a.daily_plan_id AND d.user_id=a.user_id AND d.weekly_plan_id=w.id AND d.deleted_at IS NULL WHERE a.user_id=101 AND a.ended_at IS NULL AND t.status<>'COMPLETED' AND (a.daily_plan_id IS NULL OR d.id IS NOT NULL) AND (d.plan_date<DATE '2026-11-10' OR w.week_start_date+6<DATE '2026-11-10')));

UPDATE task_assignment SET ended_at=CURRENT_TIMESTAMP,end_reason='TASK_DELETED',version=version+1 WHERE task_id=(SELECT id FROM task WHERE name='pending') AND ended_at IS NULL;
UPDATE task SET deleted_at=CURRENT_TIMESTAMP,version=version+1 WHERE name='pending';
SELECT pg_temp.expect_true('task_soft_delete_preserves_all_history', (SELECT count(*)=5 AND count(*) FILTER(WHERE ended_at IS NULL)=0 FROM task_assignment WHERE task_id=(SELECT id FROM task WHERE name='pending')));
INSERT INTO weekly_plan(user_id,week_start_date,deleted_at) VALUES(101,'2026-11-16',CURRENT_TIMESTAMP);
INSERT INTO weekly_plan(user_id,week_start_date) VALUES(101,'2026-11-16');
SELECT pg_temp.expect_true('deleted_week_allows_new_live_record', (SELECT count(*)=2 AND count(*) FILTER(WHERE deleted_at IS NULL)=1 FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-16'));
INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date,deleted_at) SELECT 101,id,'2026-11-17',CURRENT_TIMESTAMP FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-16' AND deleted_at IS NULL;
INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date) SELECT 101,id,'2026-11-17' FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-16' AND deleted_at IS NULL;
SELECT pg_temp.expect_true('deleted_day_allows_new_live_record', (SELECT count(*)=2 AND count(*) FILTER(WHERE deleted_at IS NULL)=1 FROM daily_plan WHERE user_id=101 AND plan_date='2026-11-17'));
-- 明确证明边界：没有外键/触发器时，错误关系可写入，未来必须由 Service 拒绝。
INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date) SELECT 101,id,'2026-12-01' FROM weekly_plan WHERE user_id=101 AND week_start_date='2026-11-16' AND deleted_at IS NULL;
SELECT pg_temp.expect_true('cross_table_date_requires_service_validation', (SELECT count(*)=1 FROM daily_plan d JOIN weekly_plan w ON w.id=d.weekly_plan_id WHERE d.plan_date>w.week_start_date+6));
DELETE FROM daily_plan WHERE user_id=101 AND plan_date='2026-12-01';
INSERT INTO stage(user_id,goal_id,name) SELECT 202,id,'wrong_owner_probe' FROM goal WHERE name='goal_a';
SELECT pg_temp.expect_true('cross_table_owner_requires_service_validation', (SELECT count(*)=1 FROM stage s JOIN goal g ON g.id=s.goal_id WHERE s.user_id<>g.user_id));
DELETE FROM stage WHERE name='wrong_owner_probe';

SELECT 'GROWTH_RESULT:' || json_build_object('checks',count(*),'passed',count(*) FILTER(WHERE passed),'cases',json_agg(name ORDER BY name))::text FROM validation_result;
