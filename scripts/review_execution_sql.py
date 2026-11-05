"""Execution SQL 隔离复核；不用单元测试框架，不连接或部署开发库。"""

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import threading
import time
import uuid

import psycopg2
from psycopg2 import sql


ROOT = Path(__file__).resolve().parents[1]
REVIEW_DATABASE = "asterway_identity_test"
DSN = f"dbname={REVIEW_DATABASE} user=postgres host=/var/run/postgresql"
MODULES = ("identity", "goal", "stage", "task", "planning", "execution")
CHECKS_PATH = ROOT / "docs/execution/SQL层校验.sql"
QUERIES_PATH = ROOT / "docs/execution/SQL查询与事务约定.md"


def connect(schema=None):
    connection = psycopg2.connect(DSN, application_name="asterway_execution_sql_review")
    with connection.cursor() as cursor:
        cursor.execute("SELECT current_database()")
        if cursor.fetchone()[0] != REVIEW_DATABASE:
            connection.close()
            raise RuntimeError("Refusing to connect outside the dedicated SQL review database")
        cursor.execute("SET LOCAL statement_timeout = '8s'")
        cursor.execute("SET LOCAL lock_timeout = '5s'")
        cursor.execute("SET LOCAL TIME ZONE 'UTC'")
        if schema:
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
    return connection


def ddl_body(contents):
    # 复用原始模块 SQL，移除最外层事务，让全部定义进入隔离回滚事务。
    return re.sub(r"(?m)^(?:BEGIN|COMMIT);[ \t]*\r?$", "", contents)


def authorization_block(contents):
    return contents.split("-- BEGIN EXECUTION AUTHORIZATION", 1)[1].split("-- END EXECUTION AUTHORIZATION", 1)[0]


def assert_sql(cursor, name, query):
    cursor.execute(sql.SQL("SELECT pg_temp.expect_true({}, ({}))").format(sql.Literal(name), sql.SQL(query)))


def authorization_checks(cursor, block):
    # 验证准备、后端启用和前端启用三个手工阶段；全部仍在回滚事务内。
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'true'")
    cursor.execute("SELECT pg_temp.expect_rejection(%s, %s, 'P0001')",
                   ("frontend_cannot_be_enabled_before_backend", block))
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
    cursor.execute("SAVEPOINT custom_code_check")
    cursor.execute("UPDATE identity_permission SET is_builtin=false WHERE code='execution:session:heartbeat'")
    try:
        cursor.execute(block)
    except psycopg2.Error as error:
        if error.pgcode != "P0001":
            raise
        cursor.execute("ROLLBACK TO SAVEPOINT custom_code_check")
        assert_sql(cursor, "custom_permission_collision_rejected", "SELECT true")
    else:
        raise RuntimeError("Custom permission code was silently reused")
    cursor.execute("RELEASE SAVEPOINT custom_code_check")

    cursor.execute("SET LOCAL asterway.execution_backend_ready = 'true'")
    cursor.execute(block)
    assert_sql(cursor, "backend_ready_enables_all_seven_permissions", """
        SELECT count(*)=7 AND bool_and(is_enabled) FROM identity_permission WHERE code LIKE 'execution:%'
    """)
    assert_sql(cursor, "backend_ready_keeps_frontend_entry_hidden", """
        SELECT count(*)=6 AND bool_and(is_enabled AND NOT is_visible) FROM identity_menu
        WHERE code='growth_executions' OR code LIKE 'growth_execution_%'
    """)
    assert_sql(cursor, "goal_delete_permission_and_button_retired", """
        SELECT NOT p.is_enabled AND NOT m.is_enabled AND NOT m.is_visible FROM identity_permission p
        CROSS JOIN identity_menu m WHERE p.code='goal:goal:delete' AND m.code='growth_goal_goal_delete'
    """)
    assert_sql(cursor, "goal_delete_grants_removed_but_dictionary_retained", """
        SELECT (SELECT count(*) FROM identity_permission WHERE code='goal:goal:delete')=1
        AND NOT EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_permission p ON p.id=rp.permission_id
            WHERE p.code='goal:goal:delete')
        AND NOT EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_menu m ON m.id=rm.menu_id
            WHERE m.code='growth_goal_goal_delete')
    """)
    cursor.execute("""
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code='execution:session:read';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_executions';
    """)
    cursor.execute(block)
    assert_sql(cursor, "manual_execution_grants_removed_from_management_roles", """
        SELECT NOT EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'execution:%')
        AND NOT EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_executions')
    """)
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'true'")
    cursor.execute(block)
    assert_sql(cursor, "frontend_ready_reveals_only_execution_page", """
        SELECT count(*)=6 AND count(*) FILTER (WHERE is_visible)=1 AND bool_and(is_enabled)
        AND bool_and(is_visible=(code='growth_executions')) FROM identity_menu
        WHERE code='growth_executions' OR code LIKE 'growth_execution_%'
    """)
    version_query = """
        SELECT jsonb_build_object(
            'roles',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role),
            'permissions',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),
            'menus',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu))
    """
    cursor.execute(version_query)
    versions_before = cursor.fetchone()[0]
    cursor.execute(block)
    cursor.execute(version_query)
    assert_sql(cursor, "authorization_replay_does_not_change_versions",
               "SELECT " + ("true" if cursor.fetchone()[0] == versions_before else "false"))
    cursor.execute("SET LOCAL asterway.execution_backend_ready = 'false'")
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
    cursor.execute(block)
    assert_sql(cursor, "preparation_replay_preserves_completed_feature", """
        SELECT count(*)=7 AND bool_and(is_enabled) FROM identity_permission WHERE code LIKE 'execution:%'
    """)


def readonly_query_checks(cursor):
    prototypes = dict(re.findall(r"```sql\r?\n-- review:([a-z_]+)\r?\n(.*?)```",
                                 QUERIES_PATH.read_text(encoding="utf-8-sig"), flags=re.DOTALL))
    if set(prototypes) != {"eligible", "totals"}:
        raise RuntimeError("Expected exactly two marked read-only query prototypes")
    for statement in prototypes.values():
        cursor.execute(statement)
    cursor.execute("EXECUTE execution_eligible(%s,%s)", (101, "2026-11-04"))
    actual_ids = {row[0] for row in cursor.fetchall()}
    cursor.execute("SELECT id FROM task WHERE name=ANY(%s)",
                   (["timeout", "rounding", "midnight", "paused", "eligible_week", "eligible_today"],))
    expected_ids = {row[0] for row in cursor.fetchall()}
    assert_sql(cursor, "documented_eligibility_query_executes_with_correct_owner_and_date",
               "SELECT " + ("true" if actual_ids == expected_ids else "false"))
    cursor.execute("EXECUTE execution_totals(%s,%s,%s,%s)", (
        101, "2026-11-05 00:03+08", "2026-11-04 00:00+08", "2026-11-05 00:00+08"))
    totals = dict(zip((column.name for column in cursor.description), cursor.fetchone()))
    correct = (totals["total_actual_seconds"] == 180091 and totals["range_actual_seconds"] == 6691
               and totals["total_sessions"] == 8 and totals["range_sessions"] == 5)
    assert_sql(cursor, "documented_summary_query_clips_range_and_counts_sessions_once",
               "SELECT " + ("true" if correct else "false"))


def rollback_review(sources):
    schema = "asterway_execution_review_" + uuid.uuid4().hex
    connection = connect()
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            cursor.execute("SET LOCAL asterway.execution_backend_ready = 'false'")
            cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
            for contents in sources.values():
                cursor.execute(ddl_body(contents))
            cursor.execute(CHECKS_PATH.read_text(encoding="utf-8-sig"))
            authorization_checks(cursor, authorization_block(sources["identity"]))
            readonly_query_checks(cursor)
            cursor.execute("SELECT name FROM execution_validation_result WHERE passed ORDER BY name")
            cases = [row[0] for row in cursor.fetchall()]
            cursor.execute("SELECT current_setting('server_version')")
            version = cursor.fetchone()[0]
        connection.rollback()
        with connection.cursor() as cursor:
            cursor.execute("SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
            if not cursor.fetchone()[0]:
                raise RuntimeError("Review schema survived the rollback")
        return cases, version
    finally:
        connection.rollback()
        connection.close()


def unique_race(observer, schema, first_statement, second_statement, expected_constraint):
    first, second = connect(schema), connect(schema)
    started = threading.Event()
    try:
        with first.cursor() as cursor:
            cursor.execute(first_statement)

        def competing_insert():
            started.set()
            try:
                with second.cursor() as cursor:
                    cursor.execute(second_statement)
            except psycopg2.Error as error:
                second.rollback()
                return error.pgcode, error.diag.constraint_name
            second.rollback()
            return "accepted", None

        with ThreadPoolExecutor(max_workers=1) as executor:
            pending = executor.submit(competing_insert)
            if not started.wait(timeout=2):
                raise RuntimeError("Competing SQL connection did not start")
            # 从第三连接观察真实索引锁等待，不用固定睡眠猜测并发是否发生。
            lock_wait_seen = False
            limit = time.monotonic() + 3
            while time.monotonic() < limit and not pending.done():
                with observer.cursor() as cursor:
                    cursor.execute("SELECT pg_stat_clear_snapshot()")
                    cursor.execute("SELECT wait_event_type FROM pg_stat_activity WHERE pid=%s",
                                   (second.get_backend_pid(),))
                    row = cursor.fetchone()
                    if row and row[0] == "Lock":
                        lock_wait_seen = True
                        break
                time.sleep(0.03)
            first.commit()
            actual = pending.result(timeout=8)
            if not lock_wait_seen or actual != ("23505", expected_constraint):
                raise RuntimeError(f"Concurrent uniqueness failed: waited={lock_wait_seen}, result={actual}")
    finally:
        first.rollback()
        second.rollback()
        first.close()
        second.close()


def concurrent_review(execution_ddl):
    # 两连接的真实索引竞争需要对方可见的 DDL，单独使用随机 schema。
    # 仅这个临时 schema 内提交竞争赢家，最后删除本次创建的 schema；不接触 public。
    schema = "asterway_execution_race_" + uuid.uuid4().hex
    if not re.fullmatch(r"asterway_execution_race_[0-9a-f]{32}", schema):
        raise RuntimeError("Unexpected concurrent review schema name")
    connection = connect()
    created = False
    cases = []
    try:
        with connection.cursor() as cursor:
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            cursor.execute(ddl_body(execution_ddl))
            cursor.execute("""
                INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,controller_session_id)
                VALUES(904,1,'RUNNING','2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid');
                INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
                VALUES(905,1,'FINISHED','2026-11-04 00:00Z','2026-11-04 00:00Z','2026-11-04 00:01Z','USER_FINISH');
                SELECT user_id,id FROM execution_session;
            """)
            parents = dict(cursor.fetchall())
        connection.commit()
        created = True
        running = """INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id)
            VALUES(901,{task},'2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid')"""
        unique_race(connection, schema, running.format(task=1), running.format(task=2),
                    "uk_execution_session_user_running")
        cases.append("concurrent_one_running_per_user")
        paused = """INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at)
            VALUES(902,1,'PAUSED','2026-11-04 00:00Z','2026-11-04 00:00Z')"""
        same_task_running = """INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id)
            VALUES(902,1,'2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid')"""
        unique_race(connection, schema, paused, same_task_running,
                    "uk_execution_session_user_task_unfinished")
        cases.append("concurrent_one_unfinished_per_task")
        open_segment = """INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,eligible_until,time_zone)
            VALUES({parent},1,{seq},'2026-11-04 00:00Z','2026-11-05 00:00Z','UTC')"""
        unique_race(connection, schema, open_segment.format(parent=parents[904], seq=1),
                    open_segment.format(parent=parents[904], seq=2), "uk_execution_segment_open")
        cases.append("concurrent_one_open_segment")
        closed_segment = """INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,
            ended_at,end_reason,eligible_until,time_zone) VALUES({parent},1,1,'2026-11-04 00:00Z',
            '2026-11-04 00:01Z','FINISH','2026-11-05 00:00Z','UTC')""".format(parent=parents[905])
        unique_race(connection, schema, closed_segment, closed_segment, "uk_execution_segment_sequence")
        cases.append("concurrent_segment_sequence_unique")
    finally:
        connection.rollback()
        if created:
            with connection.cursor() as cursor:
                cursor.execute("SELECT current_database()")
                if cursor.fetchone()[0] != REVIEW_DATABASE:
                    raise RuntimeError("Refusing concurrent review cleanup outside the review database")
                cursor.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(schema)))
            connection.commit()
            with connection.cursor() as cursor:
                cursor.execute("SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
                if not cursor.fetchone()[0]:
                    raise RuntimeError("Concurrent review schema was not removed")
        connection.close()
    return cases


def main():
    raw_sources = {module: (ROOT / f"backend/app/src/main/resources/sql/{module}.sql").read_bytes()
                   for module in MODULES}
    sources = {module: contents.decode("utf-8-sig") for module, contents in raw_sources.items()}
    hashes = {f"backend/app/src/main/resources/sql/{module}.sql": hashlib.sha256(contents).hexdigest()
              for module, contents in raw_sources.items()}
    verification_files = (CHECKS_PATH, QUERIES_PATH, Path(__file__), ROOT / "scripts/review_execution_sql.ps1")
    verification_hashes = {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                           for path in verification_files}
    cases, version = rollback_review(sources)
    concurrent_cases = concurrent_review(sources["execution"])
    for relative_path, digest in {**hashes, **verification_hashes}.items():
        if hashlib.sha256((ROOT / relative_path).read_bytes()).hexdigest() != digest:
            raise RuntimeError("SQL review input changed during execution; repeat the review")
    report = {
        "status": "PASSED",
        "verifiedAt": datetime.now(timezone.utc).isoformat(),
        "database": REVIEW_DATABASE,
        "postgresqlVersion": version,
        "scope": "SQL-only constraints, query arithmetic, staged seeds and PostgreSQL index concurrency; no Service/API acceptance",
        "checks": len(cases) + len(concurrent_cases),
        "passed": len(cases) + len(concurrent_cases),
        "mainReviewSchemaRolledBack": True,
        "concurrentReviewSchemaRemoved": True,
        "concurrentReviewCommittedOnlyInsideTemporarySchema": True,
        "developmentDatabaseConnected": False,
        "developmentDatabaseChanged": False,
        "sqlSha256": hashes,
        "verificationSha256": verification_hashes,
        "cases": cases,
        "concurrentCases": concurrent_cases,
    }
    (ROOT / "docs/execution/sql-check-results.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Execution SQL checks passed: {report['passed']}/{report['checks']}; "
          "review transaction rolled back; concurrent schema removed; development database not connected.")


if __name__ == "__main__":
    main()
