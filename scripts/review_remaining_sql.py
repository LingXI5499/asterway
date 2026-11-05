"""后续模块 SQL 的隔离复核；仅使用专用校验库，不连接开发库、不运行 Java 测试框架。"""

from datetime import datetime
import hashlib
import json
from pathlib import Path
import re
import sys
import uuid
from zoneinfo import ZoneInfo

import psycopg2
from psycopg2 import sql

# 复用既有 SQL 竞争校验函数，不在共享工作区生成导入缓存。
sys.dont_write_bytecode = True
import review_execution_sql as execution_review


ROOT = Path(__file__).resolve().parents[1]
SQL_ROOT = ROOT / "backend/app/src/main/resources/sql"
REPORT_ROOT = ROOT / "docs/remaining"
DATABASE = "asterway_identity_test"
MODULES = ("identity", "goal", "stage", "task", "planning", "execution", "habit", "notification")
NEW_TABLES = ("habit_category", "habit", "habit_revision", "habit_checkin", "notification_message",
              "goal_state_history", "stage_state_history", "task_state_history")
MARKERS = {
    "identity": ("REMAINING AUTHORIZATION",),
    "goal": ("GOAL CONDITION UPGRADE", "GOAL STATE HISTORY", "REMAINING GOAL HISTORY BOOTSTRAP"),
    "stage": ("STAGE STATE HISTORY", "REMAINING STAGE HISTORY BOOTSTRAP"),
    "task": ("TASK STATE HISTORY", "REMAINING TASK HISTORY BOOTSTRAP"),
}
CASES = []


def connect(schema=None):
    connection = psycopg2.connect(
        dbname=DATABASE, user="postgres", host="/var/run/postgresql",
        application_name="asterway_remaining_sql_review")
    with connection.cursor() as cursor:
        cursor.execute("SELECT current_database()")
        if cursor.fetchone()[0] != DATABASE:
            raise RuntimeError("Refusing SQL review outside the dedicated review database")
        cursor.execute("SET LOCAL statement_timeout='10s'")
        cursor.execute("SET LOCAL lock_timeout='5s'")
        cursor.execute("SET LOCAL TIME ZONE 'UTC'")
        for name in ("remaining_backend_ready", "remaining_frontend_ready", "remaining_history_bootstrap",
                     "execution_backend_ready", "execution_frontend_ready"):
            cursor.execute("SELECT set_config(%s,'false',true)", ("asterway." + name,))
        if schema:
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
    return connection


def body(contents):
    return execution_review.ddl_body(contents)


def block(contents, marker):
    begin, end = "-- BEGIN " + marker, "-- END " + marker
    if contents.count(begin) != 1 or contents.count(end) != 1:
        raise RuntimeError("Expected one manual SQL block: " + marker)
    return contents.split(begin, 1)[1].split(end, 1)[0]


def old_source(module, contents):
    for marker in MARKERS.get(module, ()):
        contents = re.sub(r"-- BEGIN " + re.escape(marker) + r".*?-- END " + re.escape(marker),
                          "", contents, count=1, flags=re.DOTALL)
    return contents


def check(name, condition):
    if not condition:
        raise RuntimeError("SQL review assertion failed: " + name)
    if name in CASES:
        raise RuntimeError("Duplicate SQL review case: " + name)
    CASES.append(name)


def assertion(cursor, name, statement, parameters=None):
    cursor.execute(statement, parameters)
    check(name, cursor.fetchone()[0] is True)


def rows(cursor, statement, parameters=None):
    cursor.execute(statement, parameters)
    names = [column.name for column in cursor.description]
    return [dict(zip(names, row)) for row in cursor.fetchall()]


def rejection(cursor, name, statement, expected="23514"):
    cursor.execute("SAVEPOINT rejected_statement")
    try:
        cursor.execute(statement)
    except psycopg2.Error as error:
        cursor.execute("ROLLBACK TO SAVEPOINT rejected_statement")
        check(name, error.pgcode == expected)
    else:
        cursor.execute("ROLLBACK TO SAVEPOINT rejected_statement")
        check(name, False)
    finally:
        cursor.execute("RELEASE SAVEPOINT rejected_statement")


def fingerprint(cursor):
    cursor.execute("""
        SELECT c.relname,a.attnum,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull,
            a.attidentity,pg_get_expr(d.adbin,d.adrelid)
        FROM pg_class c JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
        LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum
        WHERE c.relnamespace=current_schema()::regnamespace AND c.relkind='r' ORDER BY c.relname,a.attnum
    """)
    columns = cursor.fetchall()
    cursor.execute("""
        SELECT c.relname,con.conname,con.contype,pg_get_constraintdef(con.oid)
        FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
        WHERE c.relnamespace=current_schema()::regnamespace ORDER BY c.relname,con.conname
    """)
    constraints = cursor.fetchall()
    cursor.execute("""
        SELECT tablename,indexname,replace(indexdef,current_schema()||'.','')
        FROM pg_indexes WHERE schemaname=current_schema() ORDER BY tablename,indexname
    """)
    return columns, constraints, cursor.fetchall()


def run_isolated(prefix, callback):
    schema = prefix + uuid.uuid4().hex
    connection = connect()
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            result = callback(cursor)
        connection.rollback()
        with connection.cursor() as cursor:
            assertion(cursor, prefix + "rolled_back",
                      "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
        return result
    finally:
        connection.rollback()
        connection.close()


def load_queries(cursor, sources):
    for module in ("statistics", "dashboard"):
        cursor.execute(sources[module])


def auth_checks(cursor, sources):
    auth = block(sources["identity"], "REMAINING AUTHORIZATION")
    predicate = "split_part(code,':',1) IN ('habit','notification','dashboard','statistics')"
    assertion(cursor, "new_permissions_default_disabled", f"""
        SELECT count(*)=23 AND bool_and(NOT is_enabled) FROM identity_permission WHERE {predicate}
    """)
    assertion(cursor, "new_menu_nodes_default_hidden_disabled", f"""
        SELECT count(*)=23 AND bool_and(NOT m.is_enabled AND NOT m.is_visible)
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='true'")
    rejection(cursor, "cannot_enable_frontend_without_backend", auth, "P0001")
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='false'")
    for statement, name in (
        ("UPDATE identity_permission SET is_builtin=false WHERE code='habit:habit:read'", "custom_permission_collision"),
        ("UPDATE identity_menu SET is_builtin=false WHERE code='growth_habits'", "custom_menu_collision"),
        ("UPDATE identity_menu SET route_path='/wrong' WHERE code='growth_habits'", "menu_route_contract_collision"),
        ("UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHERE code='habit:checkin:read'", "deleted_permission_collision"),
    ):
        cursor.execute("SAVEPOINT collision_fixture")
        cursor.execute(statement)
        rejection(cursor, name, auth, "P0001")
        cursor.execute("ROLLBACK TO SAVEPOINT collision_fixture")
        cursor.execute("RELEASE SAVEPOINT collision_fixture")
    cursor.execute("SAVEPOINT missing_table_fixture")
    cursor.execute("ALTER TABLE habit_checkin RENAME TO habit_checkin_not_ready")
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    rejection(cursor, "cannot_enable_backend_before_all_tables", auth, "P0001")
    cursor.execute("ROLLBACK TO SAVEPOINT missing_table_fixture")
    cursor.execute("RELEASE SAVEPOINT missing_table_fixture")
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    cursor.execute(auth)
    assertion(cursor, "backend_stage_enables_23_permissions", f"""
        SELECT count(*)=23 AND bool_and(is_enabled) FROM identity_permission WHERE {predicate}
    """)
    assertion(cursor, "backend_stage_keeps_all_new_entries_hidden", """
        SELECT count(*)=23 AND bool_and(m.is_enabled AND NOT m.is_visible)
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    cursor.execute("""
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code='habit:habit:list';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_habits';
    """)
    cursor.execute(auth)
    assertion(cursor, "management_roles_new_grants_removed", """
        SELECT NOT EXISTS(SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code IN ('ADMIN','SUPER_ADMIN')
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics'))
        AND NOT EXISTS(SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_habits')
    """)
    assertion(cursor, "ordinary_role_owns_all_new_permission_relations", """
        SELECT count(*)=23 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
        JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code='USER'
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='true'")
    cursor.execute(auth)
    assertion(cursor, "frontend_stage_reveals_exactly_five_pages", """
        SELECT count(*)=23 AND count(*) FILTER(WHERE m.is_visible)=5
            AND bool_and(m.is_enabled AND m.is_visible=(m.menu_type='PAGE'))
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    versions = """SELECT jsonb_build_object(
        'p',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),
        'm',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu),
        'r',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role))"""
    cursor.execute(versions)
    before = cursor.fetchone()[0]
    cursor.execute(auth)
    cursor.execute(versions)
    check("authorization_replay_keeps_versions", cursor.fetchone()[0] == before)
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='false'")
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='false'")
    cursor.execute(auth)
    assertion(cursor, "preparation_replay_preserves_enabled_features", f"""
        SELECT count(*)=23 AND bool_and(is_enabled) FROM identity_permission WHERE {predicate}
    """)


def initialize_facts(cursor, sources):
    for module in ("goal", "stage", "task"):
        cursor.execute(block(sources[module], "REMAINING " + module.upper() + " HISTORY BOOTSTRAP"))
    cursor.execute(block(sources["notification"], "REMAINING NOTIFICATION BOOTSTRAP"))


def fresh_review(sources):
    def run(cursor):
        for module in MODULES:
            cursor.execute(body(sources[module]))
        schema_shape = fingerprint(cursor)
        assertion(cursor, "fresh_database_has_26_business_tables", """
            SELECT count(*)=26 FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'
        """)
        assertion(cursor, "no_physical_foreign_keys", """
            SELECT NOT EXISTS(SELECT 1 FROM pg_constraint WHERE connamespace=current_schema()::regnamespace AND contype='f')
        """)
        assertion(cursor, "no_permanent_business_triggers_or_functions", """
            SELECT NOT EXISTS(SELECT 1 FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
                WHERE c.relnamespace=current_schema()::regnamespace AND NOT t.tgisinternal)
            AND NOT EXISTS(SELECT 1 FROM pg_proc WHERE pronamespace=current_schema()::regnamespace)
        """)
        assertion(cursor, "all_new_table_fields_have_comments", """
            SELECT bool_and(col_description(c.oid,a.attnum) IS NOT NULL)
            FROM pg_class c JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
            WHERE c.relnamespace=current_schema()::regnamespace AND c.relname=ANY(%s)
        """, (list(NEW_TABLES),))
        load_queries(cursor, sources)
        cursor.execute((REPORT_ROOT / "SQL层校验.sql").read_text(encoding="utf-8-sig"))
        cursor.execute("SELECT name FROM remaining_validation_result ORDER BY name")
        for row in cursor.fetchall():
            check(row[0], True)
        for mutation, name in (
            ("ALTER TABLE goal ALTER COLUMN completion_condition_met SET DEFAULT true", "upgrade_rejects_incompatible_condition_default"),
            ("ALTER TABLE goal ALTER COLUMN completion_round DROP NOT NULL", "upgrade_rejects_nullable_round"),
        ):
            cursor.execute("SAVEPOINT incompatible_goal_columns")
            cursor.execute(mutation)
            rejection(cursor, name, block(sources["goal"], "GOAL CONDITION UPGRADE"), "P0001")
            cursor.execute("ROLLBACK TO SAVEPOINT incompatible_goal_columns")
            cursor.execute("RELEASE SAVEPOINT incompatible_goal_columns")
        auth_checks(cursor, sources)
        fact_checks(cursor, sources)
        query_checks(cursor)
        cursor.execute("SELECT current_setting('server_version')")
        return schema_shape, cursor.fetchone()[0]
    return run_isolated("asterway_remaining_review_", run)


def fact_checks(cursor, sources):
    # SQL fixture 由校验文件建立；默认脚本重放不应悄悄导入历史或发送提醒。
    initialize_facts(cursor, sources)
    assertion(cursor, "bootstrap_default_does_not_write_facts", """
        SELECT NOT EXISTS(SELECT 1 FROM goal_state_history WHERE event_type='BASELINE')
            AND NOT EXISTS(SELECT 1 FROM notification_message WHERE user_id=301)
    """)
    cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='true'")
    rejection(cursor, "bootstrap_requires_approved_backend",
              block(sources["goal"], "REMAINING GOAL HISTORY BOOTSTRAP"), "P0001")
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    initialize_facts(cursor, sources)
    assertion(cursor, "goal_baseline_uses_initialization_time", """
        SELECT count(*)>0 AND bool_and(source_type='SYSTEM_INIT' AND before_status IS NULL
            AND occurred_at>=transaction_timestamp()) FROM goal_state_history WHERE event_type='BASELINE'
    """)
    assertion(cursor, "trusted_old_task_completion_imported_once", """
        SELECT count(*)=1 AND bool_and(h.occurred_at=t.completed_at)
        FROM task_state_history h JOIN task t ON t.id=h.task_id
        WHERE t.name='notify_done' AND h.event_type='COMPLETION_IMPORTED'
    """)
    assertion(cursor, "future_old_task_completion_not_invented", """
        SELECT NOT EXISTS(SELECT 1 FROM task_state_history h JOIN task t ON t.id=h.task_id
            WHERE t.name='untrusted_future_completion' AND h.event_type='COMPLETION_IMPORTED')
    """)
    assertion(cursor, "object_with_real_history_gets_no_baseline", """
        SELECT NOT EXISTS(SELECT 1 FROM task_state_history h WHERE h.user_id=401 AND h.event_type='BASELINE')
    """)
    assertion(cursor, "only_exact_qualified_ordinary_goal_gets_initial_notice", """
        SELECT count(*)=1 AND bool_and(g.name='notify_qualified' AND n.condition_round=1
            AND n.created_at>=transaction_timestamp() AND n.handle_status='PENDING')
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE n.user_id IN(301,302,303)
    """)
    assertion(cursor, "rounded_100_percent_goal_does_not_qualify", """
        SELECT NOT g.completion_condition_met AND g.completion_round=0
        FROM goal g WHERE name='notify_rounded'
    """)
    assertion(cursor, "orphaned_legacy_owner_cannot_receive_notice", """
        SELECT NOT g.completion_condition_met AND g.completion_round=4
            AND NOT EXISTS(SELECT 1 FROM notification_message n WHERE n.goal_id=g.id)
        FROM goal g WHERE g.name='orphaned_legacy_goal'
    """)
    snapshots = """SELECT jsonb_build_object(
        'g',(SELECT jsonb_agg(jsonb_build_array(id,version,completion_round) ORDER BY id) FROM goal),
        'gh',(SELECT count(*) FROM goal_state_history),
        'sh',(SELECT count(*) FROM stage_state_history),
        'th',(SELECT count(*) FROM task_state_history),
        'n',(SELECT jsonb_agg(jsonb_build_array(id,version,handle_status) ORDER BY id) FROM notification_message))"""
    cursor.execute(snapshots)
    before = cursor.fetchone()[0]
    initialize_facts(cursor, sources)
    cursor.execute(snapshots)
    check("fact_bootstrap_replay_is_idempotent", cursor.fetchone()[0] == before)
    cursor.execute("""
        UPDATE notification_message SET read_at=clock_timestamp(),updated_at=clock_timestamp()
        WHERE user_id=301 AND handle_status='PENDING'
    """)
    assertion(cursor, "read_does_not_handle_or_complete_goal", """
        SELECT n.handle_status='PENDING' AND n.handled_at IS NULL AND g.status='IN_PROGRESS'
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    cursor.execute("""
        UPDATE notification_message SET handle_status='CONTINUED',handled_at=clock_timestamp(),updated_at=clock_timestamp()
        WHERE user_id=301 AND handle_status='PENDING'
    """)
    initialize_facts(cursor, sources)
    assertion(cursor, "continue_does_not_send_again_in_same_round", """
        SELECT count(*)=1 AND bool_and(n.handle_status='CONTINUED' AND g.completion_round=1 AND g.status='IN_PROGRESS')
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    cursor.execute("UPDATE task SET status='IN_PROGRESS',completed_at=NULL WHERE name='notify_done'")
    initialize_facts(cursor, sources)
    assertion(cursor, "falling_condition_preserves_round", """
        SELECT NOT completion_condition_met AND completion_round=1 FROM goal WHERE name='notify_qualified'
    """)
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    initialize_facts(cursor, sources)
    assertion(cursor, "requalification_generates_exactly_next_round", """
        SELECT count(*)=2 AND max(n.condition_round)=2 AND count(*) FILTER(WHERE n.handle_status='PENDING')=1
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    cursor.execute("UPDATE task SET status='IN_PROGRESS',completed_at=NULL WHERE name='notify_done'")
    initialize_facts(cursor, sources)
    assertion(cursor, "falling_condition_invalidates_pending_without_marking_read", """
        SELECT n.handle_status='INVALIDATED' AND n.read_at IS NULL AND n.handled_at IS NOT NULL
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified' AND n.condition_round=2
    """)
    cursor.execute("SAVEPOINT atomic_notice")
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    initialize_facts(cursor, sources)
    cursor.execute("ROLLBACK TO SAVEPOINT atomic_notice")
    cursor.execute("RELEASE SAVEPOINT atomic_notice")
    assertion(cursor, "task_condition_and_notification_can_rollback_together", """
        SELECT t.status='IN_PROGRESS' AND NOT g.completion_condition_met AND g.completion_round=2
            AND NOT EXISTS(SELECT 1 FROM notification_message n WHERE n.goal_id=g.id AND n.condition_round=3)
        FROM task t JOIN stage s ON s.id=t.stage_id JOIN goal g ON g.id=s.goal_id WHERE t.name='notify_done'
    """)
    cursor.execute("SAVEPOINT management_reclassification")
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    initialize_facts(cursor, sources)
    cursor.execute("INSERT INTO identity_user_role(user_id,role_id) SELECT 301,id FROM identity_role WHERE code='ADMIN'")
    initialize_facts(cursor, sources)
    assertion(cursor, "management_reclassification_invalidates_personal_pending_notice", """
        SELECT NOT g.completion_condition_met AND g.completion_round=3 AND n.handle_status='INVALIDATED'
        FROM goal g JOIN notification_message n ON n.goal_id=g.id AND n.condition_round=3 WHERE g.name='notify_qualified'
    """)
    cursor.execute("ROLLBACK TO SAVEPOINT management_reclassification")
    cursor.execute("RELEASE SAVEPOINT management_reclassification")
    cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='false'")
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='false'")


def query_checks(cursor):
    # 只执行已交付的 PREPARE 原型，不在脚本中复制另一份业务查询实现。
    result = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    total = next(row for row in result if row["dimension"] == "TOTAL")
    check("habit_total_uses_sums_and_keeps_history", total["done_count"] == 4 and total["due_count"] == 8
          and total["completion_rate"] == 50)
    by_category = {row["category_id"]: row for row in result if row["dimension"] == "CATEGORY"}
    cursor.execute("SELECT name,id FROM habit_category WHERE user_id=101")
    category_ids = dict(cursor.fetchall())
    check("completed_habit_uses_completion_time_category",
          by_category[category_ids["A"]]["done_count"] == 2 and by_category[category_ids["A"]]["due_count"] == 4
          and by_category[category_ids["B"]]["done_count"] == 2 and by_category[category_ids["B"]]["due_count"] == 3
          and by_category[None]["due_count"] == 1)
    days = rows(cursor, "EXECUTE statistics_habit_days(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    assertion(cursor, "soft_deleted_category_keeps_existing_references", """
        SELECT c.deleted_at IS NOT NULL AND count(r.id)>0 FROM habit_category c JOIN habit_revision r ON r.category_id=c.id
        WHERE c.user_id=101 AND c.name='B' GROUP BY c.id
    """)
    check("only_valid_checkins_count_and_no_future_start",
          len(days) == 8 and sum(row["done_count"] for row in days) == 4)
    empty = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (99999, "2026-11-02", "2026-11-04"))
    check("zero_denominator_returns_null", len(empty) == 1 and empty[0]["due_count"] == 0
          and empty[0]["done_count"] == 0 and empty[0]["completion_rate"] is None)
    own = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (202, "2026-11-02", "2026-11-04"))
    other_total = next(row for row in own if row["dimension"] == "TOTAL")
    check("habit_statistics_isolate_users", other_total["done_count"] == 0 and other_total["due_count"] == 3)

    cursor.execute("SAVEPOINT category_name_reuse")
    cursor.execute("INSERT INTO habit_category(user_id,name) VALUES(101,'B')")
    assertion(cursor, "deleted_category_name_can_be_reused_without_rewriting_references", """
        SELECT count(*)=2 AND count(*) FILTER(WHERE deleted_at IS NULL)=1
        FROM habit_category WHERE user_id=101 AND name='B'
    """)
    cursor.execute("ROLLBACK TO SAVEPOINT category_name_reuse")
    cursor.execute("RELEASE SAVEPOINT category_name_reuse")

    cursor.execute("SAVEPOINT cancel_paused_today")
    cursor.execute("""UPDATE habit_checkin SET cancelled_at='2026-11-04 10:00Z',updated_at=clock_timestamp()
        WHERE habit_id=(SELECT id FROM habit WHERE user_id=101 AND id IN(
            SELECT habit_id FROM habit_revision WHERE name='reading')) AND business_date='2026-11-04' AND cancelled_at IS NULL""")
    canceled = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    total_canceled = next(row for row in canceled if row["dimension"] == "TOTAL")
    check("cancel_then_paused_rule_recomputes_today_without_rewriting_past",
          total_canceled["done_count"] == 3 and total_canceled["due_count"] == 7)
    cursor.execute("ROLLBACK TO SAVEPOINT cancel_paused_today")
    cursor.execute("RELEASE SAVEPOINT cancel_paused_today")
    cursor.execute("SAVEPOINT ended_and_deleted_habit")
    cursor.execute("""
        INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,
            status_snapshot,effective_date,changed_at,time_zone)
        SELECT r.user_id,r.habit_id,5,'ENDED',r.name,r.category_id,r.cycle_type,r.weekday_mask,r.start_date,
            'ENDED','2026-11-04','2026-11-04 04:00Z',r.time_zone FROM habit_revision r
        WHERE r.name='reading' AND r.revision_no=4;
        UPDATE habit SET status='ENDED',ended_at='2026-11-04 04:00Z',current_revision_no=5
        WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='reading')
    """)
    ended = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    check("completed_today_remains_one_of_one_after_ending", ended == result)
    cursor.execute("""
        INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,
            status_snapshot,deleted_snapshot,effective_date,changed_at,time_zone)
        SELECT r.user_id,r.habit_id,6,'DELETED',r.name,r.category_id,r.cycle_type,r.weekday_mask,r.start_date,
            'ENDED',true,'2026-11-04','2026-11-04 05:00Z',r.time_zone FROM habit_revision r
        WHERE r.name='reading' AND r.revision_no=5;
        UPDATE habit SET deleted_at='2026-11-04 05:00Z',current_revision_no=6
        WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='reading')
    """)
    deleted = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    check("completed_today_and_past_survive_habit_deletion", deleted == result)
    cursor.execute("ROLLBACK TO SAVEPOINT ended_and_deleted_habit")
    cursor.execute("RELEASE SAVEPOINT ended_and_deleted_habit")

    expected_periods = {"TOTAL": ("2026-11-02", "2026-11-04"), "WEEK": ("2026-11-02", "2026-11-04"),
                        "MONTH": ("2026-11-01", "2026-11-04"), "YEAR": ("2026-01-01", "2026-11-04"),
                        "RANGE": ("2026-11-03", "2026-11-04")}
    for period, expected in expected_periods.items():
        bounds = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                      (101, period, None, "2026-11-03", "2026-11-04", "2026-11-04"))
        check("habit_period_" + period.lower(), len(bounds) == 1
              and (str(bounds[0]["date_from"]), str(bounds[0]["date_to"])) == expected)
        stats = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)",
                     (101, bounds[0]["date_from"], bounds[0]["date_to"]))
        period_total = next(row for row in stats if row["dimension"] == "TOTAL")
        expected_counts = (3, 5) if period == "RANGE" else (4, 8)
        check("habit_period_statistics_" + period.lower(),
              (period_total["done_count"], period_total["due_count"]) == expected_counts)
    for name, period, anchor, start, end in (
        ("future_range", "RANGE", None, "2026-11-04", "2026-11-05"),
        ("reversed_range", "RANGE", None, "2026-11-04", "2026-11-03"),
        ("unknown_period", "OTHER", None, None, None),
        ("future_anchor", "MONTH", "2026-12-01", None, None),
    ):
        invalid = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                       (101, period, anchor, start, end, "2026-11-04"))
        check("invalid_" + name + "_has_no_bounds", not invalid)
    bounds = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                  (101, "WEEK", "2027-01-01", None, None, "2027-01-02"))
    check("iso_week_crosses_calendar_year", str(bounds[0]["date_from"]) == "2026-12-28"
          and str(bounds[0]["date_to"]) == "2027-01-02")

    totals = rows(cursor, "EXECUTE statistics_execution_totals(%s,%s,%s,%s)",
                  (501, "2026-11-03 16:00Z", "2026-11-04 16:00Z", "2026-11-04 02:05Z"))[0]
    check("execution_aggregates_milliseconds_before_seconds", totals["total_actual_milliseconds"] == 161300
          and totals["range_actual_milliseconds"] == 160700 and totals["total_actual_seconds"] == 161
          and totals["range_actual_seconds"] == 160)
    check("execution_session_counts_are_not_segment_counts", totals["total_sessions"] == 3
          and totals["range_sessions"] == 3 and totals["total_finished_sessions"] == 2)
    check("deleted_task_execution_facts_still_contribute", totals["range_actual_milliseconds"] == 160700)
    daily = rows(cursor, "EXECUTE statistics_execution_days(%s,%s,%s,%s,%s)",
                 (501, "2026-11-03", "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))
    check("execution_cross_midnight_and_timeout_cap", [row["actual_milliseconds"] for row in daily] == [600,160700])
    for name, date_value, expected_seconds in (("spring", "2026-03-08", 82800), ("autumn", "2026-11-01", 90000)):
        day = rows(cursor, "EXECUTE statistics_execution_days(%s,%s,%s,%s,%s)",
                   (503, date_value, date_value, "America/New_York", "2026-11-04 02:05Z"))
        check("dst_" + name + "_day_length", day[0]["actual_seconds"] == expected_seconds)
    states = rows(cursor, "EXECUTE statistics_state_days(%s,%s,%s,%s)",
                  (401, "2026-11-03", "2026-11-04", "Asia/Shanghai"))
    check("historical_completions_not_inferred_from_current_status",
          states[0]["task_completion_actions"] == 2 and states[0]["completed_distinct_tasks"] == 1
          and states[1]["task_completion_actions"] == 0)
    check("goal_archive_restore_are_real_events", states[0]["goal_archive_actions"] == 1
          and states[1]["goal_restore_actions"] == 1)
    timeline = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                    (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 100, 0))
    first = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                 (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 0))
    second = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                  (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 2))
    check("timeline_stable_pagination_and_user_filter",
          len([event for event in timeline if event["event_type"] != "BASELINE"]) == 8
          and first + second == timeline[:4])

    dashboard = rows(cursor, "EXECUTE dashboard_habits(%s,%s)", (101, "2026-11-04"))
    check("dashboard_today_keeps_completed_paused_habit",
          len(dashboard) == 3 and sum(row["done_count"] for row in dashboard) == 1)
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    (101, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    check("dashboard_habit_counts_match_statistics", overview["habit_done"] == 1 and overview["habit_due"] == 3)
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    (501, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    check("dashboard_actual_time_matches_clipped_statistics", overview["actual_seconds"] == 160)
    plan_rows = rows(cursor, "EXECUTE dashboard_tasks(%s,%s,%s,%s)", (601, "2026-11-04", 100, 0))
    check("dashboard_separates_today_and_week_undated",
          len(plan_rows) == 2 and {row["section"] for row in plan_rows} == {"TODAY","WEEK_UNDATED"})
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    (601, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    check("dashboard_counts_do_not_multiply_with_fact_joins",
          overview["today_tasks"] == 1 and overview["today_current_completed_tasks"] == 1
          and overview["week_undated_tasks"] == 1)
    goals = rows(cursor, "EXECUTE dashboard_goals(%s,%s,%s)", (601, 20, 0))
    check("dashboard_weighted_goal_progress", len(goals) == 1 and goals[0]["task_count"] == 3
          and goals[0]["completed_tasks"] == 1 and goals[0]["current_progress"] == 20)
    objects = rows(cursor, "EXECUTE statistics_objects(%s)", (601,))
    check("current_object_summary_is_distinct_from_history", sum(row["object_count"] for row in objects if row["kind"] == "TASK") == 3)


def upgrade_review(sources, expected_shape):
    def run(cursor):
        for module in ("identity", "goal", "stage", "task", "planning", "execution"):
            cursor.execute(body(old_source(module, sources[module])))
        assertion(cursor, "existing_baseline_has_18_tables", """
            SELECT count(*)=18 FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'
        """)
        cursor.execute("SET LOCAL asterway.execution_backend_ready='true'")
        cursor.execute(execution_review.authorization_block(sources["identity"]))
        cursor.execute("""
            INSERT INTO identity_user(id,username,password_hash,nickname,time_zone) OVERRIDING SYSTEM VALUE
            VALUES(901,'upgradereview','$argon2id$SQLfixture','upgrade','Asia/Shanghai');
            INSERT INTO identity_user_role(user_id,role_id) SELECT 901,id FROM identity_role WHERE code='USER';
            INSERT INTO goal(user_id,name,status,version) VALUES(901,'preserve_old_goal','IN_PROGRESS',7);
            INSERT INTO stage(user_id,goal_id,name) SELECT 901,id,'preserve_old_stage' FROM goal;
            INSERT INTO task(user_id,stage_id,name,status,completed_at,created_at)
            SELECT 901,id,'preserve_old_task','COMPLETED','2026-11-03 01:00Z','2026-11-01 00:00Z' FROM stage;
            SELECT jsonb_agg(to_jsonb(g) ORDER BY id) FROM goal g;
        """)
        before = cursor.fetchone()[0]
        cursor.execute("""
            INSERT INTO weekly_plan(user_id,week_start_date) VALUES(901,'2026-11-02');
            INSERT INTO daily_plan(user_id,weekly_plan_id,plan_date) SELECT 901,id,'2026-11-03' FROM weekly_plan;
            INSERT INTO task_assignment(user_id,task_id,weekly_plan_id,daily_plan_id,assigned_at,ended_at,end_reason)
            SELECT 901,t.id,w.id,d.id,'2026-11-03 00:00Z','2026-11-03 03:00Z','RETURNED_TO_POOL'
            FROM task t CROSS JOIN weekly_plan w JOIN daily_plan d ON d.weekly_plan_id=w.id;
            INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
            SELECT 901,id,'FINISHED','2026-11-03 01:00Z','2026-11-03 01:01Z','2026-11-03 01:01Z','USER_FINISH' FROM task;
            INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,ended_at,end_reason,eligible_until,time_zone)
            SELECT e.id,a.id,1,e.started_at,e.finished_at,'FINISH','2026-11-03 16:00Z','Asia/Shanghai'
            FROM execution_session e JOIN task_assignment a ON a.task_id=e.task_id;
        """)
        preserved_query = """SELECT jsonb_build_object(
            'u',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_user x),
            's',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM stage x),
            't',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM task x),
            'w',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM weekly_plan x),
            'd',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM daily_plan x),
            'a',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM task_assignment x),
            'es',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM execution_session x),
            'sg',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM execution_segment x),
            'ep',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_permission x WHERE code LIKE 'execution:%'),
            'em',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_menu x WHERE code='growth_executions' OR code LIKE 'growth_execution_%'))"""
        cursor.execute(preserved_query)
        preserved = cursor.fetchone()[0]
        for module in ("goal", "stage", "task"):
            for marker in MARKERS[module]:
                cursor.execute(block(sources[module], marker))
        for module in ("habit", "notification"):
            cursor.execute(body(sources[module]))
        cursor.execute(block(sources["identity"], "REMAINING AUTHORIZATION"))
        check("upgrade_and_fresh_database_have_identical_schema", fingerprint(cursor) == expected_shape)
        cursor.execute("SELECT jsonb_agg(to_jsonb(g)-'completion_condition_met'-'completion_round' ORDER BY id) FROM goal g")
        check("existing_goal_values_and_versions_preserved", cursor.fetchone()[0] == before)
        cursor.execute(preserved_query)
        check("upgrade_preserves_accounts_planning_execution_and_existing_authorization", cursor.fetchone()[0] == preserved)
        assertion(cursor, "existing_task_row_preserved", """
            SELECT count(*)=1 AND bool_and(name='preserve_old_task' AND completed_at='2026-11-03 01:00Z')
            FROM task
        """)
        cursor.execute(block(sources["goal"], "GOAL CONDITION UPGRADE"))
        cursor.execute(block(sources["identity"], "REMAINING AUTHORIZATION"))
        assertion(cursor, "upgrade_preparation_replay_writes_no_business_facts", """
            SELECT NOT EXISTS(SELECT 1 FROM goal_state_history) AND NOT EXISTS(SELECT 1 FROM task_state_history)
                AND NOT EXISTS(SELECT 1 FROM notification_message)
        """)
        cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
        cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='true'")
        initialize_facts(cursor, sources)
        initialize_facts(cursor, sources)
        assertion(cursor, "old_database_baseline_and_known_completion_are_idempotent", """
            SELECT (SELECT count(*)=1 FROM goal_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM stage_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM task_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM task_state_history WHERE event_type='COMPLETION_IMPORTED')
        """)
    run_isolated("asterway_remaining_upgrade_", run)


def legacy_review(sources, module):
    # 保留历史校验文件；只适配因本次增加三张历史表而变化的表总数断言，其余原样执行。
    if module == "growth":
        modules = ("goal", "stage", "task", "planning")
        check_path = ROOT / "docs/growth/SQL层校验.sql"
        old_count, new_count, result_table = "count(*)=6", "count(*)=9", "validation_result"
    else:
        modules = ("identity", "goal", "stage", "task", "planning", "execution")
        check_path = ROOT / "docs/execution/SQL层校验.sql"
        old_count, new_count, result_table = "count(*) = 18", "count(*) = 21", "execution_validation_result"
    def run(cursor):
        for name in modules:
            cursor.execute(body(sources[name]))
        contents = check_path.read_text(encoding="utf-8-sig")
        if contents.count(old_count) != 1:
            raise RuntimeError("Legacy table-count assertion has changed; review adapter before running")
        cursor.execute(contents.replace(old_count, new_count, 1))
        if module == "execution":
            execution_review.authorization_checks(cursor, execution_review.authorization_block(sources["identity"]))
            execution_review.readonly_query_checks(cursor)
        cursor.execute(sql.SQL("SELECT name FROM {} WHERE passed ORDER BY name").format(sql.Identifier(result_table)))
        for row in cursor.fetchall():
            check("legacy_" + module + "_" + row[0], True)
    run_isolated("asterway_" + module + "_review_", run)


def concurrent_review(sources):
    schema = "asterway_remaining_race_" + uuid.uuid4().hex
    if not re.fullmatch(r"asterway_remaining_race_[0-9a-f]{32}", schema):
        raise RuntimeError("Invalid isolated concurrent schema")
    connection = connect()
    created = False
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            cursor.execute(body(sources["habit"]))
            # 通知默认初始化块为 no-op，依赖表无需为本组唯一索引竞争重复建立。
            notification = re.sub(r"-- BEGIN REMAINING NOTIFICATION BOOTSTRAP.*?-- END REMAINING NOTIFICATION BOOTSTRAP",
                                  "", sources["notification"], flags=re.DOTALL)
            cursor.execute(body(notification))
            cursor.execute(block(sources["task"], "TASK STATE HISTORY"))
        connection.commit()
        created = True
        race = execution_review.unique_race
        statement = """INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at)
            VALUES(901,1,1,'2026-11-04','Asia/Shanghai','2026-11-04 01:00Z')"""
        race(connection, schema, statement, statement, "uk_habit_checkin_valid_date")
        check("concurrent_one_valid_checkin_per_habit_date", True)
        race(connection, schema,
             "INSERT INTO habit_category(user_id,name) VALUES(901,'Reading')",
             "INSERT INTO habit_category(user_id,name) VALUES(901,'reading')",
             "uk_habit_category_user_name_active")
        check("concurrent_category_case_insensitive_uniqueness", True)
        revision = """INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,
            start_date,status_snapshot,effective_date,changed_at,time_zone)
            VALUES(901,2,1,'CREATED','race','DAILY',127,'2026-11-04','ACTIVE','2026-11-04','2026-11-04 01:00Z','UTC')"""
        race(connection, schema, revision, revision, "uk_habit_revision_number")
        check("concurrent_revision_number_uniqueness", True)
        notice = """INSERT INTO notification_message(user_id,goal_id,condition_round,title,content)
            VALUES(901,1,1,'race','concurrent goal round')"""
        race(connection, schema, notice, notice, "uk_notification_goal_round")
        check("concurrent_goal_round_delivers_once", True)
        history = """INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,
            entity_version,occurred_at) VALUES(901,1,'STATUS_CHANGED','NOT_STARTED','IN_PROGRESS','race',1,'2026-11-04 01:00Z')"""
        race(connection, schema, history, history, "uk_task_state_history_event")
        check("concurrent_same_state_event_is_unique", True)
    finally:
        connection.rollback()
        if created:
            with connection.cursor() as cursor:
                cursor.execute("SELECT current_database()")
                if cursor.fetchone()[0] != DATABASE:
                    raise RuntimeError("Refusing schema cleanup outside dedicated SQL review database")
                cursor.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(schema)))
            connection.commit()
            with connection.cursor() as cursor:
                assertion(cursor, "remaining_concurrent_schema_removed",
                          "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
        connection.close()


def main():
    paths = [SQL_ROOT / (module + ".sql") for module in MODULES + ("dashboard", "statistics")]
    paths += [Path(__file__), ROOT / "scripts/review_remaining_sql.ps1", REPORT_ROOT / "SQL层校验.sql",
              ROOT / "scripts/review_execution_sql.py", ROOT / "docs/growth/SQL层校验.sql",
              ROOT / "docs/execution/SQL层校验.sql", ROOT / "docs/execution/SQL查询与事务约定.md"]
    hashes = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    sources = {module: (SQL_ROOT / (module + ".sql")).read_text(encoding="utf-8-sig")
               for module in MODULES + ("dashboard", "statistics")}
    groups = []
    before = len(CASES)
    shape, version = fresh_review(sources)
    groups.append({"name": "new_schema_constraints_seeds_bootstrap_queries", "passed": len(CASES)-before})
    print("Fresh schema, constraints, initialization, permissions and query arithmetic passed.", flush=True)
    before = len(CASES)
    upgrade_review(sources, shape)
    groups.append({"name": "existing_18_to_26_additive_upgrade", "passed": len(CASES)-before})
    print("Existing 18-table database additive upgrade passed.", flush=True)
    for module in ("growth", "execution"):
        before = len(CASES)
        legacy_review(sources, module)
        groups.append({"name": "legacy_" + module + "_sql_regression", "passed": len(CASES)-before})
    before = len(CASES)
    for name in execution_review.concurrent_review(sources["execution"]):
        check("legacy_" + name, True)
    groups.append({"name": "execution_index_concurrency", "passed": len(CASES)-before})
    before = len(CASES)
    concurrent_review(sources)
    groups.append({"name": "remaining_index_concurrency", "passed": len(CASES)-before})
    for path, digest in hashes.items():
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest:
            raise RuntimeError("SQL inputs changed during review; rerun with the final files")
    report = {
        "status": "PASSED", "verifiedAt": datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(),
        "database": DATABASE, "postgresqlVersion": version, "businessTablesBefore": 18, "businessTablesAfter": 26,
        "newTables": list(NEW_TABLES), "checks": len(CASES), "passed": len(CASES),
        "scope": "SQL DDL, constraints, additive old-schema upgrade, staged seeds, initialization, read-query arithmetic, database uniqueness races and prior SQL regressions",
        "strictTestFrameworkUsed": False, "serviceOrHttpAcceptance": "NOT_VERIFIED",
        "developmentDatabaseConnected": False, "developmentDatabaseChanged": False,
        "rollbackSchemasRemoved": True, "concurrentSchemasRemoved": True,
        "concurrentCommitsOnlyInsideOwnRandomReviewSchemas": True,
        "legacyAssertionAdapters": {"growth": "business table count 6 -> 9", "execution": "business table count 18 -> 21"},
        "reviewGroups": groups, "inputSha256": hashes, "cases": CASES,
    }
    (REPORT_ROOT / "sql-check-results.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Remaining SQL checks passed: {len(CASES)}/{len(CASES)}; "
          "all review schemas removed; development database not connected.")


if __name__ == "__main__":
    main()
