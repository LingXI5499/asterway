"""P1.2 SQL 专项隔离校验，不建立单元／系统／集成测试框架，不连接开发库。"""

import hashlib
import json
import re
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from pathlib import Path
from threading import Event
from zoneinfo import ZoneInfo

import psycopg2
from psycopg2 import sql

ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "backend/app/src/main/resources/sql/agent.sql"
IDENTITY_PATH = ROOT / "backend/app/src/main/resources/sql/identity.sql"
DATABASE = "asterway_identity_test"
CASES = []
T0 = "2026-11-05T00:00:00Z"


def connect():
    connection = psycopg2.connect(
        dbname=DATABASE,
        user="postgres",
        host="/var/run/postgresql",
        application_name="asterway_agent_sql_review",
    )
    with connection.cursor() as cursor:
        cursor.execute("SELECT current_database()")
        if cursor.fetchone()[0] != DATABASE:
            connection.close()
            raise RuntimeError("SQL review refused outside the dedicated review database")
        cursor.execute(
            "SET LOCAL statement_timeout='8s'; SET LOCAL lock_timeout='5s'; SET LOCAL TIME ZONE 'UTC'"
        )
    return connection


def check(name, condition):
    if not condition:
        raise RuntimeError("Agent SQL assertion failed: " + name)
    if name in CASES:
        raise RuntimeError("Duplicate SQL assertion: " + name)
    CASES.append(name)


def scalar(cursor, statement, parameters=None):
    cursor.execute(statement, parameters)
    return cursor.fetchone()[0]


def reject(cursor, name, statement, parameters=None, code="23514"):
    cursor.execute("SAVEPOINT expected_rejection")
    try:
        cursor.execute(statement, parameters)
    except psycopg2.Error as error:
        cursor.execute("ROLLBACK TO SAVEPOINT expected_rejection")
        check(name, error.pgcode == code)
    else:
        cursor.execute("ROLLBACK TO SAVEPOINT expected_rejection")
        check(name, False)
    finally:
        cursor.execute("RELEASE SAVEPOINT expected_rejection")


def block(source, name):
    pattern = r"-- BEGIN " + re.escape(name) + r"(.*?)-- END " + re.escape(name)
    matches = re.findall(pattern, source, re.DOTALL)
    if len(matches) != 1:
        raise RuntimeError("Expected one SQL block: " + name)
    return matches[0]


def source_for_schema(source, fact_schema):
    # 只改固定 schema 标识符；权限中的 agent: 前缀和阶段开关保持原样。
    source = source.replace("SCHEMA agent", "SCHEMA " + fact_schema)
    source = re.sub(r"\bagent\.", fact_schema + ".", source)
    return re.sub(r"(?mi)^\s*(?:BEGIN|COMMIT);\s*$", "", source)


def initialize(cursor, source, identity_source, identity_schema, fact_schema):
    cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(identity_schema)))
    cursor.execute(
        sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(identity_schema))
    )
    # 使用现有六张 Identity 结构及最小内置授权夹具，不复制任何用户数据。
    identity_ddl = identity_source.split("CREATE TABLE identity_role (", 1)[1].split(
        "CREATE TABLE identity_login_log (", 1
    )[0]
    cursor.execute("CREATE TABLE identity_role (" + identity_ddl)
    cursor.execute("""INSERT INTO identity_role(code,name,is_builtin)
        VALUES ('USER','Ordinary user',true),('ADMIN','Administrator',true),('SUPER_ADMIN','Super administrator',true);
        INSERT INTO identity_menu(code,name,menu_type,route_path,is_builtin)
        VALUES ('growth_root','Growth','DIRECTORY','/growth',true);""")
    for name in ("agent_backend_ready", "agent_frontend_ready", "agent_conversation_backend_ready"):
        cursor.execute("SELECT set_config(%s,'false',true)", ("asterway." + name,))
    transformed = source_for_schema(source, fact_schema)
    cursor.execute(transformed)
    cursor.execute(
        sql.SQL("SET LOCAL search_path TO {}, {}, pg_catalog").format(
            sql.Identifier(fact_schema), sql.Identifier(identity_schema)
        )
    )
    return transformed


def conversation(cursor, user=1001, title="Conversation"):
    return scalar(
        cursor,
        """INSERT INTO conversation(user_id,title,created_at,updated_at)
        VALUES (%s,%s,%s,%s) RETURNING id""",
        (user, title, T0, T0),
    )


def turn(cursor, conversation_id, user=1001, request_id=None):
    return scalar(
        cursor,
        """INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash,
        created_at,updated_at,started_at,deadline_at)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s::timestamptz+interval '120 seconds') RETURNING id""",
        (user, conversation_id, str(request_id or uuid.uuid4()), "a" * 64, T0, T0, T0, T0),
    )


def message(cursor, conversation_id, turn_id, number, role, content, **extra):
    row = {
        "user_id": 1001,
        "conversation_id": conversation_id,
        "turn_id": turn_id,
        "message_no": number,
        "role": role,
        "content": content,
        "created_at": T0,
        "updated_at": T0,
        **extra,
    }
    columns = list(row)
    statement = sql.SQL("INSERT INTO conversation_message ({}) VALUES ({}) RETURNING id").format(
        sql.SQL(",").join(map(sql.Identifier, columns)),
        sql.SQL(",").join(sql.Placeholder() for _ in columns),
    )
    return scalar(cursor, statement, [row[key] for key in columns])


def finish(cursor, turn_id, status="COMPLETED", reason="stop", error=None):
    cursor.execute(
        """UPDATE conversation_turn SET status=%s,finish_reason=%s,error_code=%s,
        finished_at=%s::timestamptz+interval '1 second',updated_at=%s::timestamptz+interval '1 second',
        version=version+1 WHERE id=%s""",
        (status, reason, error, T0, T0, turn_id),
    )


def authorization_review(cursor, transformed):
    legacy = block(transformed, "AGENT AUTHORIZATION")
    current = block(transformed, "AGENT CONVERSATION AUTHORIZATION")
    check(
        "new_permission_count",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%'",
        )
        == 6,
    )
    check(
        "new_permissions_disabled",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        == 0,
    )
    check(
        "new_menus_hidden",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent_conversation_%' AND (is_enabled OR is_visible)",
        )
        == 0,
    )
    cursor.execute(
        "SET LOCAL asterway.agent_backend_ready='true'; SET LOCAL asterway.agent_frontend_ready='true';"
    )
    cursor.execute(legacy)
    check(
        "p11_flag_does_not_enable_p12",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        == 0,
    )
    check(
        "p11_page_still_available",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_menu WHERE code='growth_agent' AND is_visible AND is_enabled",
        )
        == 1,
    )
    cursor.execute("SET LOCAL asterway.agent_conversation_backend_ready='true'")
    reject(
        cursor,
        "missing_table_blocks_enable",
        "ALTER TABLE conversation RENAME TO absent_conversation;" + current,
        code="P0001",
    )
    cursor.execute("""INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='ADMIN' AND p.code LIKE 'agent:conversation:%';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='SUPER_ADMIN' AND m.code LIKE 'growth_agent_conversation_%';""")
    cursor.execute(current)
    check(
        "p12_permissions_enable_only_after_flag",
        scalar(
            cursor,
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        == 6,
    )
    check(
        "ordinary_user_has_all_six_actions",
        scalar(
            cursor,
            """SELECT count(*) FROM identity_role_permission rp
        JOIN identity_role r ON r.id=rp.role_id JOIN identity_permission p ON p.id=rp.permission_id
        WHERE r.code='USER' AND p.code LIKE 'agent:conversation:%'""",
        )
        == 6,
    )
    check(
        "manager_grants_removed",
        scalar(
            cursor,
            """SELECT NOT EXISTS(
        SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
        JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'agent:conversation:%')
        AND NOT EXISTS(SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
        JOIN identity_menu m ON m.id=rm.menu_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code LIKE 'growth_agent_conversation_%')""",
        ),
    )
    snapshot = scalar(cursor, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r")
    permission_snapshot = scalar(
        cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p"
    )
    menu_snapshot = scalar(cursor, "SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m")
    cursor.execute(legacy + current)
    check(
        "repeated_seed_preserves_role_versions",
        snapshot
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r"),
    )
    check(
        "repeated_seed_preserves_permission_versions",
        permission_snapshot
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p"),
    )
    check(
        "repeated_seed_preserves_menu_versions",
        menu_snapshot
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m"),
    )
    reject(
        cursor,
        "custom_reserved_code_rejected",
        "UPDATE identity_permission SET is_builtin=false WHERE code='agent:conversation:create';"
        + current,
        code="P0001",
    )
    reject(
        cursor,
        "deleted_reserved_code_rejected",
        "UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHERE code='agent:conversation:read';"
        + current,
        code="P0001",
    )
    reject(
        cursor,
        "reserved_menu_contract_rejected",
        "UPDATE identity_menu SET parent_id=(SELECT id FROM identity_menu WHERE code='growth_root') WHERE code='growth_agent_conversation_read';"
        + current,
        code="P0001",
    )


def constraints_review(cursor):
    first, second = conversation(cursor), conversation(cursor, 1002, "Second user")
    first_turn = turn(cursor, first)
    for name, statement in (
        ("invalid_conversation_status", "UPDATE conversation SET status='UNKNOWN' WHERE id=%s"),
        ("blank_title", "UPDATE conversation SET title='   ' WHERE id=%s"),
        ("closed_requires_time", "UPDATE conversation SET status='CLOSED' WHERE id=%s"),
        ("active_cannot_be_deleted", "UPDATE conversation SET deleted_at=created_at WHERE id=%s"),
        ("negative_version", "UPDATE conversation SET version=-1 WHERE id=%s"),
        ("infinite_conversation_time", "UPDATE conversation SET updated_at='infinity' WHERE id=%s"),
        (
            "summary_requires_boundary_and_time",
            "UPDATE conversation SET context_summary='summary' WHERE id=%s",
        ),
        (
            "empty_summary_cannot_advance_boundary",
            "UPDATE conversation SET summary_through_message_no=1,next_message_no=2 WHERE id=%s",
        ),
        (
            "summary_cannot_cover_future_message",
            "UPDATE conversation SET context_summary='summary',summary_through_message_no=1,summary_updated_at=created_at WHERE id=%s",
        ),
        (
            "summary_size_limit",
            "UPDATE conversation SET context_summary=repeat('x',16001) WHERE id=%s",
        ),
    ):
        reject(cursor, name, statement, (first,))
    for name, statement in (
        ("turn_invalid_status", "UPDATE conversation_turn SET status='OTHER' WHERE id=%s"),
        (
            "turn_completed_requires_finish_time",
            "UPDATE conversation_turn SET status='COMPLETED',finish_reason='stop' WHERE id=%s",
        ),
        (
            "turn_completed_cannot_have_null_reason",
            "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at WHERE id=%s",
        ),
        (
            "turn_length_is_not_completed",
            "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at,finish_reason='length' WHERE id=%s",
        ),
        (
            "cancel_requires_error_code",
            "UPDATE conversation_turn SET status='CANCELLED',finished_at=started_at WHERE id=%s",
        ),
        (
            "failed_requires_finish_time",
            "UPDATE conversation_turn SET status='FAILED',error_code='MODEL_FAILED' WHERE id=%s",
        ),
        (
            "incomplete_requires_non_stop_reason",
            "UPDATE conversation_turn SET status='INCOMPLETE',finished_at=started_at,error_code='TRUNCATED',finish_reason='stop' WHERE id=%s",
        ),
        ("deadline_after_start", "UPDATE conversation_turn SET deadline_at=started_at WHERE id=%s"),
        (
            "turn_infinite_deadline",
            "UPDATE conversation_turn SET deadline_at='infinity' WHERE id=%s",
        ),
        ("turn_negative_version", "UPDATE conversation_turn SET version=-1 WHERE id=%s"),
        (
            "turn_nil_request_id",
            "UPDATE conversation_turn SET request_id='00000000-0000-0000-0000-000000000000' WHERE id=%s",
        ),
        (
            "request_hash_must_be_sha256",
            "UPDATE conversation_turn SET request_hash='not-a-hash' WHERE id=%s",
        ),
    ):
        reject(cursor, name, statement, (first_turn,))
    user_message = message(cursor, first, first_turn, 1, "USER", "Keep the daily time limit")
    assistant = message(
        cursor,
        first,
        first_turn,
        2,
        "ASSISTANT",
        "Partial",
        is_complete=False,
        model="review-model",
    )
    for name, statement, params in (
        (
            "blank_user_message",
            "UPDATE conversation_message SET content=' ' WHERE id=%s",
            (user_message,),
        ),
        (
            "user_message_size_limit",
            "UPDATE conversation_message SET content=repeat('x',8001) WHERE id=%s",
            (user_message,),
        ),
        (
            "user_cannot_be_partial",
            "UPDATE conversation_message SET is_complete=false WHERE id=%s",
            (user_message,),
        ),
        (
            "user_cannot_have_model",
            "UPDATE conversation_message SET model='model' WHERE id=%s",
            (user_message,),
        ),
        (
            "json_array_required",
            "UPDATE conversation_message SET tool_calls='{}'::jsonb WHERE id=%s",
            (assistant,),
        ),
        (
            "usage_object_required",
            "UPDATE conversation_message SET usage='[]'::jsonb WHERE id=%s",
            (assistant,),
        ),
        (
            "assistant_requires_model",
            "UPDATE conversation_message SET model=NULL WHERE id=%s",
            (assistant,),
        ),
        (
            "complete_assistant_requires_finish_reason",
            "UPDATE conversation_message SET is_complete=true WHERE id=%s",
            (assistant,),
        ),
        (
            "truncated_reply_not_complete",
            "UPDATE conversation_message SET is_complete=true,finish_reason='length' WHERE id=%s",
            (assistant,),
        ),
        (
            "message_size_limit",
            "UPDATE conversation_message SET content=repeat('x',65537) WHERE id=%s",
            (assistant,),
        ),
        (
            "message_sequence_positive",
            "UPDATE conversation_message SET message_no=0 WHERE id=%s",
            (assistant,),
        ),
        (
            "duplicate_message_no",
            "UPDATE conversation_message SET message_no=1 WHERE id=%s",
            (assistant,),
        ),
        (
            "tool_requires_parent_call_and_name",
            "UPDATE conversation_message SET role='TOOL',model=NULL WHERE id=%s",
            (assistant,),
        ),
    ):
        reject(
            cursor,
            name,
            statement,
            params,
            code="23505" if name == "duplicate_message_no" else "23514",
        )
    cursor.execute(
        "UPDATE conversation_message SET content='Complete answer',is_complete=true,finish_reason='stop' WHERE id=%s",
        (assistant,),
    )
    finish(cursor, first_turn)
    second_turn = turn(cursor, first)
    message(cursor, first, second_turn, 3, "USER", "Continue with the same constraints")
    calls = json.dumps(
        [
            {
                "id": "shared_call",
                "type": "function",
                "function": {"name": "growth_get_goals", "arguments": "{}"},
            }
        ]
    )
    request_message = message(
        cursor,
        first,
        second_turn,
        4,
        "ASSISTANT",
        "",
        model="review-model",
        finish_reason="tool_calls",
        tool_calls=calls,
    )
    tool_result = message(
        cursor,
        first,
        second_turn,
        5,
        "TOOL",
        '{"items":[]}',
        tool_request_message_id=request_message,
        tool_call_id="shared_call",
        tool_name="growth_get_goals",
    )
    reject(
        cursor,
        "tool_result_cannot_reference_itself",
        "UPDATE conversation_message SET tool_request_message_id=id WHERE id=%s",
        (tool_result,),
    )
    reject(
        cursor,
        "duplicate_tool_result_rejected",
        """INSERT INTO conversation_message(user_id,conversation_id,turn_id,message_no,role,content,
        tool_request_message_id,tool_call_id,tool_name) SELECT user_id,conversation_id,turn_id,99,role,content,
        tool_request_message_id,tool_call_id,tool_name FROM conversation_message WHERE id=%s""",
        (tool_result,),
        code="23505",
    )
    another_request = message(
        cursor,
        first,
        second_turn,
        6,
        "ASSISTANT",
        "",
        model="review-model",
        finish_reason="tool_calls",
        tool_calls=calls,
    )
    message(
        cursor,
        first,
        second_turn,
        7,
        "TOOL",
        '{"items":[]}',
        tool_request_message_id=another_request,
        tool_call_id="shared_call",
        tool_name="growth_get_goals",
    )
    message(
        cursor,
        first,
        second_turn,
        8,
        "ASSISTANT",
        "Finished",
        model="review-model",
        finish_reason="stop",
        usage='{"totalTokens":10}',
    )
    finish(cursor, second_turn)
    third_turn = turn(cursor, first)
    message(cursor, first, third_turn, 9, "USER", "Keep this constraint even if cancelled")
    message(
        cursor,
        first,
        third_turn,
        10,
        "ASSISTANT",
        "Interrupted answer",
        model="review-model",
        is_complete=False,
    )
    finish(cursor, third_turn, "CANCELLED", None, "USER_CANCELLED")
    other_turn = turn(cursor, second, user=1002)
    message(cursor, second, other_turn, 1, "USER", "Another user", user_id=1002)
    finish(cursor, other_turn)
    check(
        "same_sequence_allowed_across_conversations",
        scalar(cursor, "SELECT count(*) FROM conversation_message WHERE message_no=1") == 2,
    )
    check(
        "raw_history_keeps_partial_text",
        scalar(
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        == 10,
    )
    check(
        "tool_result_ids_scoped_to_assistant_message",
        scalar(cursor, "SELECT count(*) FROM conversation_message WHERE tool_call_id='shared_call'")
        == 2,
    )
    check(
        "owner_filter_rejects_other_user",
        scalar(cursor, "SELECT count(*) FROM conversation WHERE id=%s AND user_id=1002", (first,))
        == 0,
    )
    cursor.execute(
        "SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_id=%s ORDER BY message_no DESC LIMIT 3",
        (first,),
    )
    page = [row[0] for row in cursor.fetchall()]
    check("history_cursor_first_page", page == [10, 9, 8])
    cursor.execute(
        "SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_id=%s AND message_no<%s ORDER BY message_no DESC LIMIT 3",
        (first, min(page)),
    )
    check(
        "history_cursor_second_page_no_overlap", [row[0] for row in cursor.fetchall()] == [7, 6, 5]
    )
    cursor.execute(
        """UPDATE conversation SET next_message_no=11,last_message_at=%s,
        context_summary='Retain daily time limit and confirmed constraints',summary_through_message_no=8,
        summary_updated_at=%s,version=version+1 WHERE id=%s AND version=0""",
        (T0, T0, first),
    )
    check("summary_compare_and_set_first_update", cursor.rowcount == 1)
    cursor.execute(
        "UPDATE conversation SET title='Stale overwrite' WHERE id=%s AND version=0", (first,)
    )
    check("stale_summary_or_title_update_rejected", cursor.rowcount == 0)
    context = scalar(
        cursor,
        """SELECT count(*) FROM conversation_message m JOIN conversation_turn t
        ON t.id=m.turn_id AND t.user_id=m.user_id AND t.conversation_id=m.conversation_id
        JOIN conversation c ON c.id=m.conversation_id AND c.user_id=m.user_id
        WHERE m.user_id=1001 AND c.id=%s AND c.deleted_at IS NULL
        AND m.message_no>c.summary_through_message_no
        AND (m.role='USER' OR (m.is_complete AND t.status='COMPLETED'))""",
        (first,),
    )
    check("context_keeps_cancelled_user_but_not_partial_answer", context == 1)
    check(
        "summary_does_not_delete_messages",
        scalar(
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        == 10,
    )
    cursor.execute(
        "UPDATE conversation SET status='CLOSED',closed_at=created_at,deleted_at=created_at,version=version+1 WHERE id=%s",
        (first,),
    )
    check(
        "logical_delete_hides_conversation",
        scalar(
            cursor, "SELECT count(*) FROM conversation WHERE id=%s AND deleted_at IS NULL", (first,)
        )
        == 0,
    )
    check(
        "logical_delete_retains_history",
        scalar(
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        == 10,
    )
    expired_conversation = conversation(cursor)
    expired_turn = turn(cursor, expired_conversation)
    cursor.execute(
        """UPDATE conversation_turn SET status='FAILED',error_code='AGENT_TIMEOUT',
        finished_at=clock_timestamp(),updated_at=clock_timestamp(),version=version+1
        WHERE id=%s AND status='RUNNING' AND deadline_at<=clock_timestamp()""",
        (expired_turn,),
    )
    check("expired_turn_can_be_normalized", cursor.rowcount == 1)
    check("new_turn_allowed_after_timeout", turn(cursor, expired_conversation) > expired_turn)
    old_request = scalar(
        cursor, "SELECT request_id::text FROM conversation_turn WHERE id=%s", (expired_turn,)
    )
    reject(
        cursor,
        "old_request_remains_idempotent_after_timeout",
        """INSERT INTO conversation_turn(user_id,conversation_id,
        request_id,request_hash,status,finish_reason,started_at,deadline_at,finished_at,created_at,updated_at)
        VALUES (1001,%s,%s,%s,'COMPLETED','stop',%s,%s::timestamptz+interval '120 seconds',%s,%s,%s)""",
        (second, old_request, "c" * 64, T0, T0, T0, T0, T0),
        code="23505",
    )


def run_rollback_review(source, identity_source):
    suffix = uuid.uuid4().hex
    identity_schema, fact_schema = "agent_review_ident_" + suffix, "agent_review_fact_" + suffix
    connection = connect()
    try:
        with connection.cursor() as cursor:
            version = scalar(cursor, "SHOW server_version")
            transformed = initialize(cursor, source, identity_source, identity_schema, fact_schema)
            check(
                "exactly_three_agent_tables",
                scalar(
                    cursor,
                    "SELECT count(*) FROM pg_class WHERE relnamespace=%s::regnamespace AND relkind='r'",
                    (fact_schema,),
                )
                == 3,
            )
            check(
                "no_physical_foreign_keys",
                scalar(
                    cursor,
                    "SELECT count(*) FROM pg_constraint WHERE connamespace=%s::regnamespace AND contype='f'",
                    (fact_schema,),
                )
                == 0,
            )
            check(
                "all_timestamps_use_milliseconds",
                scalar(
                    cursor,
                    """SELECT bool_and(datetime_precision=3)
                FROM information_schema.columns WHERE table_schema=%s AND data_type='timestamp with time zone'""",
                    (fact_schema,),
                ),
            )
            check(
                "schema_not_granted_to_public",
                scalar(
                    cursor,
                    """SELECT NOT EXISTS(SELECT 1 FROM pg_namespace n,
                LATERAL aclexplode(COALESCE(n.nspacl,acldefault('n',n.nspowner))) a
                WHERE n.nspname=%s AND a.grantee=0)""",
                    (fact_schema,),
                ),
            )
            authorization_review(cursor, transformed)
            constraints_review(cursor)
        connection.rollback()
        with connection.cursor() as cursor:
            check(
                "ddl_fixtures_and_metadata_rolled_back",
                scalar(
                    cursor,
                    "SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)",
                    (identity_schema, fact_schema),
                )
                == 0,
            )
        return version
    finally:
        connection.rollback()
        connection.close()


def race(
    name,
    fact_schema,
    statement_a,
    params_a,
    statement_b,
    params_b,
    expected_code=None,
    constraint=None,
):
    """确认 B 确实等待 A 的事务锁，再提交 A，验证数据库的并发约束。"""
    ready_a, ready_b, release = Event(), Event(), Event()
    pids = {}

    def worker(label, statement, parameters):
        connection = connect()
        try:
            with connection.cursor() as cursor:
                cursor.execute(
                    sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                        sql.Identifier(fact_schema)
                    )
                )
                pids[label] = connection.get_backend_pid()
                if label == "b":
                    if not ready_a.wait(4):
                        raise RuntimeError("First SQL transaction did not become ready")
                    ready_b.set()
                cursor.execute(statement, parameters)
                rows = cursor.fetchall() if cursor.description else []
                if label == "a":
                    ready_a.set()
                    if not release.wait(5):
                        raise RuntimeError("SQL race release timed out")
            connection.commit()
            return {"code": None, "rows": rows, "constraint": None}
        except psycopg2.Error as error:
            connection.rollback()
            return {"code": error.pgcode, "rows": [], "constraint": error.diag.constraint_name}
        finally:
            connection.close()

    observer = connect()
    try:
        with ThreadPoolExecutor(max_workers=2, thread_name_prefix="agent-sql-race") as executor:
            first = executor.submit(worker, "a", statement_a, params_a)
            second = executor.submit(worker, "b", statement_b, params_b)
            try:
                if not ready_b.wait(4):
                    raise RuntimeError("Second SQL transaction did not become ready")
                blocked = False
                until = time.monotonic() + 3
                while time.monotonic() < until:
                    with observer.cursor() as cursor:
                        cursor.execute(
                            "SELECT %s=ANY(pg_blocking_pids(%s))", (pids["a"], pids["b"])
                        )
                        blocked = cursor.fetchone()[0]
                    if blocked:
                        break
                    time.sleep(0.03)
                check(name + "_actually_blocked", blocked)
            finally:
                release.set()
            result_a, result_b = first.result(timeout=8), second.result(timeout=8)
            check(name + "_first_committed", result_a["code"] is None)
            check(name + "_second_result", result_b["code"] == expected_code)
            if constraint:
                check(name + "_correct_constraint", result_b["constraint"] == constraint)
            return result_a["rows"], result_b["rows"]
    finally:
        observer.rollback()
        observer.close()


def cleanup_own_schemas(identity_schema, fact_schema):
    for name in (identity_schema, fact_schema):
        if not re.fullmatch(r"agent_review_(?:ident|fact)_[0-9a-f]{32}", name):
            raise RuntimeError("Refusing to remove a non-review schema")
    connection = connect()
    try:
        with connection.cursor() as cursor:
            for name in (fact_schema, identity_schema):
                cursor.execute(
                    sql.SQL("DROP SCHEMA IF EXISTS {} CASCADE").format(sql.Identifier(name))
                )
        connection.commit()
        with connection.cursor() as cursor:
            check(
                "concurrent_review_schemas_removed",
                scalar(
                    cursor,
                    "SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)",
                    (identity_schema, fact_schema),
                )
                == 0,
            )
    finally:
        connection.rollback()
        connection.close()


def concurrent_review(source, identity_source):
    suffix = uuid.uuid4().hex
    identity_schema, fact_schema = "agent_review_ident_" + suffix, "agent_review_fact_" + suffix
    setup = connect()
    try:
        with setup.cursor() as cursor:
            initialize(cursor, source, identity_source, identity_schema, fact_schema)
            open_id, request_a, request_b, allocation_id = [conversation(cursor) for _ in range(4)]
            another_user = conversation(cursor, user=1002)
            first_turn = turn(cursor, allocation_id)
            finish(cursor, first_turn)
            second_turn = turn(cursor, allocation_id)
            finish(cursor, second_turn)
        setup.commit()
        insert_turn = """INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash,
            created_at,updated_at,started_at,deadline_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s::timestamptz+interval '120 seconds') RETURNING id"""

        def params(conversation_id, request_id, user=1001):
            return (user, conversation_id, str(request_id), "b" * 64, T0, T0, T0, T0)

        race(
            "one_running_turn_per_conversation",
            fact_schema,
            insert_turn,
            params(open_id, uuid.uuid4()),
            insert_turn,
            params(open_id, uuid.uuid4()),
            expected_code="23505",
            constraint="uk_agent_turn_running_conversation",
        )
        request_id = uuid.uuid4()
        race(
            "idempotent_request_per_user",
            fact_schema,
            insert_turn,
            params(request_a, request_id),
            insert_turn,
            params(request_b, request_id),
            expected_code="23505",
            constraint="uk_agent_turn_user_request",
        )
        with setup.cursor() as cursor:
            cursor.execute(
                sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                    sql.Identifier(fact_schema)
                )
            )
            cursor.execute(insert_turn, params(another_user, request_id, user=1002))
            check("same_request_id_allowed_for_different_user", cursor.fetchone()[0] > 0)
        setup.commit()
        allocate = """WITH allocated AS (
            UPDATE conversation SET next_message_no=next_message_no+1,version=version+1
            WHERE id=%s RETURNING id,user_id,next_message_no-1 AS allocated_no
        ) INSERT INTO conversation_message(user_id,conversation_id,turn_id,message_no,role,content)
            SELECT user_id,id,%s,allocated_no,'USER','Concurrent message' FROM allocated RETURNING message_no"""
        rows_a, rows_b = race(
            "message_number_allocation",
            fact_schema,
            allocate,
            (allocation_id, first_turn),
            allocate,
            (allocation_id, second_turn),
        )
        check("concurrent_message_numbers_are_ordered", rows_a == [(1,)] and rows_b == [(2,)])
        with setup.cursor() as cursor:
            cursor.execute(
                sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                    sql.Identifier(fact_schema)
                )
            )
            check(
                "counter_advanced_exactly_twice",
                scalar(
                    cursor, "SELECT next_message_no FROM conversation WHERE id=%s", (allocation_id,)
                )
                == 3,
            )
    finally:
        setup.rollback()
        setup.close()
        cleanup_own_schemas(identity_schema, fact_schema)


def main():
    source = SQL_PATH.read_text(encoding="utf-8-sig")
    identity_source = IDENTITY_PATH.read_text(encoding="utf-8-sig")
    hashes = {
        str(path.relative_to(ROOT)).replace("\\", "/"): hashlib.sha256(
            path.read_bytes()
        ).hexdigest()
        for path in (SQL_PATH, IDENTITY_PATH)
    }
    before = len(CASES)
    version = run_rollback_review(source, identity_source)
    groups = [
        {"name": "ddl_constraints_permissions_history_and_summary", "passed": len(CASES) - before}
    ]
    print(
        "Agent SQL DDL, constraints, history queries and staged authorization passed; transaction rolled back.",
        flush=True,
    )
    before = len(CASES)
    concurrent_review(source, identity_source)
    groups.append({"name": "real_database_concurrency", "passed": len(CASES) - before})
    for path, digest in hashes.items():
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest:
            raise RuntimeError("SQL changed during review")
    report = {
        "phase": "P1.2_SQL_ONLY",
        "status": "PASSED",
        "userAcceptance": "PENDING",
        "verifiedAt": datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(),
        "database": DATABASE,
        "postgresqlVersion": version,
        "newSchema": "agent",
        "newTables": [
            "agent.conversation",
            "agent.conversation_turn",
            "agent.conversation_message",
        ],
        "checks": len(CASES),
        "passed": len(CASES),
        "reviewGroups": groups,
        "cases": CASES,
        "inputSha256": hashes,
        "strictTestFrameworkUsed": False,
        "rollbackSchemasRemoved": True,
        "concurrentSchemasRemoved": True,
        "concurrentCommitsOnlyInsideOwnRandomReviewSchemas": True,
        "developmentDatabaseConnected": False,
        "developmentDatabaseChanged": False,
        "backendOrFrontendImplemented": False,
        "serviceOrHttpAcceptance": "NOT_VERIFIED",
    }
    output = ROOT / "docs/agent/P1.2-sql-check-results.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(
        f"Agent SQL checks passed: {len(CASES)}/{len(CASES)}; own schemas removed; development database not connected."
    )


if __name__ == "__main__":
    main()
