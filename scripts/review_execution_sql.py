# 检查复盘执行计时sql相关行为，断言失败时报告具体不符合预期的场景。
# Execution SQL 隔离复核；不用单元测试框架，不连接或部署开发库。

# 从 concurrent.futures 引入 Thread连接池Executor（ThreadPoolExecutor）。
from concurrent.futures import ThreadPoolExecutor

# 从 datetime 引入 日期时间类型（datetime）、timezone。
from datetime import datetime, timezone

# 引入 hashlib，供下面调用其功能。
import hashlib

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 引入 re，供下面调用其功能。
import re

# 引入 threading，供下面调用其功能。
import threading

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 引入 uuid，供下面调用其功能。
import uuid

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 从 psycopg2 引入 sql。
from psycopg2 import sql


# 把从执行“调用 Path(__file__).resolve 完成resolve处理”得到的结果的parents属性中按固定数值 1取出的内容保存到 根（ROOT）。
ROOT = Path(__file__).resolve().parents[1]
# 把固定文字 'asterway_identity_test'保存到 复盘数据库（REVIEW_DATABASE）。
REVIEW_DATABASE = "asterway_identity_test"
# 把在文字占位处填入变量后组成的字符串保存到 DSN。
DSN = f"dbname={REVIEW_DATABASE} user=postgres host=/var/run/postgresql"
# 把依次包含 固定文字 'identity'、固定文字 'goal'、固定文字 'stage'、固定文字 'task'、固定文字 'planning'等内容 的集合保存到 MODULES。
MODULES = ("identity", "goal", "stage", "task", "planning", "execution")
# 把按 ROOT / 'docs/execution/SQL层校验.sql' 计算的结果保存到 CHECKS路径（CHECKS_PATH）。
CHECKS_PATH = ROOT / "docs/execution/SQL层校验.sql"
# 把按 ROOT / 'docs/execution/SQL查询与事务约定.md' 计算的结果保存到 QUERIES路径（QUERIES_PATH）。
QUERIES_PATH = ROOT / "docs/execution/SQL查询与事务约定.md"


# 函数 connect：调用 connect 完成连接处理；接收 数据库分区（schema）。
def connect(schema=None):
    # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = psycopg2.connect(DSN, application_name="asterway_execution_sql_review")
    # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
    with connection.cursor() as cursor:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT current_database()")
        # 检查条件 cursor.fetchone()[0] != REVIEW_DATABASE 是否成立；成立时执行缩进的分支。
        if cursor.fetchone()[0] != REVIEW_DATABASE:
            # 释放连接、文件或客户端占用的资源。
            connection.close()
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("Refusing to connect outside the dedicated SQL review database")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL statement_timeout = '8s'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL lock_timeout = '5s'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL TIME ZONE 'UTC'")
        # 检查条件 schema 是否成立；成立时执行缩进的分支。
        if schema:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
    # 把数据库连接（connection）交还给调用方，本函数到此结束。
    return connection


# 函数 ddl_body：调用 ddl_body 完成ddl请求正文处理；接收 contents。
def ddl_body(contents):
    # 复用原始模块 SQL，移除最外层事务，让全部定义进入隔离回滚事务。
    # 把执行“调用 re.sub 完成sub处理”得到的结果交还给调用方，本函数到此结束。
    return re.sub(r"(?m)^(?:BEGIN|COMMIT);[ \t]*\r?$", "", contents)


# 函数 authorization_block：调用 authorization_block 完成授权block处理；接收 contents。
def authorization_block(contents):
    # 把从执行“按指定分隔规则拆分文字”得到的结果中按固定数值 0取出的内容交还给调用方，本函数到此结束。
    return contents.split("-- BEGIN EXECUTION AUTHORIZATION", 1)[1].split("-- END EXECUTION AUTHORIZATION", 1)[0]


# 函数 assert_sql：断言 sql；接收 数据库语句执行器（cursor）、名称（name）、查询条件（query）。
def assert_sql(cursor, name, query):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(sql.SQL("SELECT pg_temp.expect_true({}, ({}))").format(sql.Literal(name), sql.SQL(query)))


# 函数 authorization_checks：调用 authorization_checks 完成授权checks处理；接收 数据库语句执行器（cursor）、block。
def authorization_checks(cursor, block):
    # 验证准备、后端启用和前端启用三个手工阶段；全部仍在回滚事务内。
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'true'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SELECT pg_temp.expect_rejection(%s, %s, 'P0001')",
                   # 继续传入或组合表达式 ("frontend_cannot_be_enabled_before_backend", block)) 的结果，完成上面尚未结束的表达式。
                   ("frontend_cannot_be_enabled_before_backend", block))
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT custom_code_check")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE identity_permission SET is_builtin=false WHERE code='execution:session:heartbeat'")
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 执行传入的数据库语句并取得结果。
        cursor.execute(block)
    # 捕获psycopg2.Error异常，执行下面的错误处理，异常对象保存为 error。
    except psycopg2.Error as error:
        # 检查条件 error.pgcode != 'P0001' 是否成立；成立时执行缩进的分支。
        if error.pgcode != "P0001":
            # 中止本轮正常处理，把当前异常继续向上交给调用方。
            raise
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT custom_code_check")
        # 断言 sql。
        assert_sql(cursor, "custom_permission_collision_rejected", "SELECT true")
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Custom permission code was silently reused")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT custom_code_check")

    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_backend_ready = 'true'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assert_sql(cursor, "backend_ready_enables_all_seven_permissions", """
        SELECT count(*)=7 AND bool_and(is_enabled) FROM identity_permission WHERE code LIKE 'execution:%'
    """)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assert_sql(cursor, "backend_ready_keeps_frontend_entry_hidden", """
        SELECT count(*)=6 AND bool_and(is_enabled AND NOT is_visible) FROM identity_menu
        WHERE code='growth_executions' OR code LIKE 'growth_execution_%'
    """)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    assert_sql(cursor, "goal_delete_permission_and_button_retired", """
        SELECT NOT p.is_enabled AND NOT m.is_enabled AND NOT m.is_visible FROM identity_permission p
        CROSS JOIN identity_menu m WHERE p.code='goal:goal:delete' AND m.code='growth_goal_goal_delete'
    """)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assert_sql(cursor, "goal_delete_grants_removed_but_dictionary_retained", """
        SELECT (SELECT count(*) FROM identity_permission WHERE code='goal:goal:delete')=1
        AND NOT EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_permission p ON p.id=rp.permission_id
            WHERE p.code='goal:goal:delete')
        AND NOT EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_menu m ON m.id=rm.menu_id
            WHERE m.code='growth_goal_goal_delete')
    """)
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code='execution:session:read';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_executions';
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assert_sql(cursor, "manual_execution_grants_removed_from_management_roles", """
        SELECT NOT EXISTS (SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'execution:%')
        AND NOT EXISTS (SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id
            WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_executions')
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'true'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assert_sql(cursor, "frontend_ready_reveals_only_execution_page", """
        SELECT count(*)=6 AND count(*) FILTER (WHERE is_visible)=1 AND bool_and(is_enabled)
        AND bool_and(is_visible=(code='growth_executions')) FROM identity_menu
        WHERE code='growth_executions' OR code LIKE 'growth_execution_%'
    """)
    # 把表达式 "\n SELECT jsonb_build_object(\n 'roles',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role… 的结果保存到 版本号查询条件（version_query）。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'roles',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'permissions',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'menus',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu))。
    version_query = """
        SELECT jsonb_build_object(
            'roles',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role),
            'permissions',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),
            'menus',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu))
    """
    # 执行传入的数据库语句并取得结果。
    cursor.execute(version_query)
    # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 versions变化前（versions_before）。
    versions_before = cursor.fetchone()[0]
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(version_query)
    # 断言 sql。
    assert_sql(cursor, "authorization_replay_does_not_change_versions",
               # 继续传入或组合取出查询结果中的一行，完成上面尚未结束的表达式。
               "SELECT " + ("true" if cursor.fetchone()[0] == versions_before else "false"))
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_backend_ready = 'false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block)
    # 断言 sql。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assert_sql(cursor, "preparation_replay_preserves_completed_feature", """
        SELECT count(*)=7 AND bool_and(is_enabled) FROM identity_permission WHERE code LIKE 'execution:%'
    """)


# 函数 readonly_query_checks：读取 仅查询条件checks（only_query_checks）；接收 数据库语句执行器（cursor）。
def readonly_query_checks(cursor):
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 prototypes。
    prototypes = dict(re.findall(r"```sql\r?\n-- review:([a-z_]+)\r?\n(.*?)```",
                                 # 继续传入或组合按指定编码读取文件文字，完成上面尚未结束的表达式。
                                 QUERIES_PATH.read_text(encoding="utf-8-sig"), flags=re.DOTALL))
    # 检查条件 set(prototypes) != {'eligible', 'totals'} 是否成立；成立时执行缩进的分支。
    if set(prototypes) != {"eligible", "totals"}:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Expected exactly two marked read-only query prototypes")
    # 从执行“调用 prototypes.values 完成值列表处理”得到的结果依次取出statement，每项执行一次下面的处理。
    for statement in prototypes.values():
        # 执行传入的数据库语句并取得结果。
        cursor.execute(statement)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("EXECUTE execution_eligible(%s,%s)", (101, "2026-11-04"))
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 实际编号列表（actual_ids）。
    actual_ids = {row[0] for row in cursor.fetchall()}
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SELECT id FROM task WHERE name=ANY(%s)",
                   # 继续传入或组合表达式 (["timeout", "rounding", "midnight", "paused", "eligible_week", "eligible_today"],)) 的结果，完成上面尚未结束的表达式。
                   (["timeout", "rounding", "midnight", "paused", "eligible_week", "eligible_today"],))
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 预计编号列表（expected_ids）。
    expected_ids = {row[0] for row in cursor.fetchall()}
    # 断言 sql。
    assert_sql(cursor, "documented_eligibility_query_executes_with_correct_owner_and_date",
               # 继续传入或组合条件判断的结果："SELECT " + ("true" if actual_ids == expected_ids else "false"))，完成上面尚未结束的表达式。
               "SELECT " + ("true" if actual_ids == expected_ids else "false"))
    # 执行传入的数据库语句并取得结果。
    cursor.execute("EXECUTE execution_totals(%s,%s,%s,%s)", (
        # 继续传入或组合按表达式计算得到的值：101, "2026-11-05 00:03+08", "2026-11-04 00:00+08", "2026-11-05 00:00+08"))，完成上面尚未结束的表达式。
        101, "2026-11-05 00:03+08", "2026-11-04 00:00+08", "2026-11-05 00:00+08"))
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 totals。
    totals = dict(zip((column.name for column in cursor.description), cursor.fetchone()))
    # 把条件 totals['total_actual_seconds'] == 180091 是否成立并且条件 totals['range_actual_seconds'] == 6691 是否成立并且条件 totals['total_sessions'] == 8 是否成立并且条件 totals['range_sessions'] == 5 是否成立保存到 correct。
    correct = (totals["total_actual_seconds"] == 180091 and totals["range_actual_seconds"] == 6691
               # 继续补充本次调用或判断：条件判断的结果：and totals["total_sessions"] == 8 and totals["range_sessions"] == 5)。
               and totals["total_sessions"] == 8 and totals["range_sessions"] == 5)
    # 断言 sql。
    assert_sql(cursor, "documented_summary_query_clips_range_and_counts_sessions_once",
               # 继续传入或组合按表达式计算得到的值："SELECT " + ("true" if correct else "false"))，完成上面尚未结束的表达式。
               "SELECT " + ("true" if correct else "false"))


# 函数 rollback_review：调用 rollback_review 完成rollback复盘处理；接收 sources。
def rollback_review(sources):
    # 把按 'asterway_execution_review_' + uuid.uuid4().hex 计算的结果保存到 数据库分区（schema）。
    schema = "asterway_execution_review_" + uuid.uuid4().hex
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = connect()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SET LOCAL asterway.execution_backend_ready = 'false'")
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SET LOCAL asterway.execution_frontend_ready = 'false'")
            # 从执行“调用 sources.values 完成值列表处理”得到的结果依次取出contents，每项执行一次下面的处理。
            for contents in sources.values():
                # 执行传入的数据库语句并取得结果。
                cursor.execute(ddl_body(contents))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(CHECKS_PATH.read_text(encoding="utf-8-sig"))
            # 调用 authorization_checks 完成授权checks处理。
            authorization_checks(cursor, authorization_block(sources["identity"]))
            # 读取 仅查询条件checks（only_query_checks）。
            readonly_query_checks(cursor)
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SELECT name FROM execution_validation_result WHERE passed ORDER BY name")
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 cases。
            cases = [row[0] for row in cursor.fetchall()]
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SELECT current_setting('server_version')")
            # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 版本号（version）。
            version = cursor.fetchone()[0]
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
            # 检查条件 not cursor.fetchone()[0] 是否成立；成立时执行缩进的分支。
            if not cursor.fetchone()[0]:
                # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                raise RuntimeError("Review schema survived the rollback")
        # 把依次包含 cases、版本号（version） 的集合交还给调用方，本函数到此结束。
        return cases, version
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 释放连接、文件或客户端占用的资源。
        connection.close()


# 函数 unique_race：调用 unique_race 完成唯一race处理；接收 observer、数据库分区（schema）、首次statement（first_statement）、秒statement（second_statement）、预计约束（expected_constraint）。
def unique_race(observer, schema, first_statement, second_statement, expected_constraint):
    # 把依次包含 执行“调用 connect 完成连接处理”得到的结果、执行“调用 connect 完成连接处理”得到的结果 的集合保存到 首次秒（(first, second)）。
    first, second = connect(schema), connect(schema)
    # 把执行“调用 threading.Event 完成事件处理”得到的结果保存到 开始时（started）。
    started = threading.Event()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 first.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with first.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(first_statement)

        # 函数 competing_insert：调用 competing_insert 完成competing写入新记录处理。
        def competing_insert():
            # 调用 started.set 完成设置处理。
            started.set()
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 进入执行“调用 second.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
                with second.cursor() as cursor:
                    # 执行传入的数据库语句并取得结果。
                    cursor.execute(second_statement)
            # 捕获psycopg2.Error异常，执行下面的错误处理，异常对象保存为 error。
            except psycopg2.Error as error:
                # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
                second.rollback()
                # 把依次包含 错误（error）的pgcode属性、错误（error）的diag属性的约束名称（constraint_name）属性 的集合交还给调用方，本函数到此结束。
                return error.pgcode, error.diag.constraint_name
            # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
            second.rollback()
            # 把依次包含 固定文字 'accepted'、空值，表示当前还没有对应内容 的集合交还给调用方，本函数到此结束。
            return "accepted", None

        # 进入执行“调用 ThreadPoolExecutor 完成Thread连接池Executor处理”得到的结果，命名为 executor；退出时由上下文管理器提交、回滚或释放资源。
        with ThreadPoolExecutor(max_workers=1) as executor:
            # 把执行“调用 executor.submit 完成提交处理”得到的结果保存到 等待处理（pending）。
            pending = executor.submit(competing_insert)
            # 检查条件 not started.wait(timeout=2) 是否成立；成立时执行缩进的分支。
            if not started.wait(timeout=2):
                # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                raise RuntimeError("Competing SQL connection did not start")
            # 从第三连接观察真实索引锁等待，不用固定睡眠猜测并发是否发生。
            # 把假，表示条件不成立或开关关闭保存到 加锁等待已读（lock_wait_seen）。
            lock_wait_seen = False
            # 把按 time.monotonic() + 3 计算的结果保存到 上限（limit）。
            limit = time.monotonic() + 3
            # 只要条件 time.monotonic() < limit and (not pending.done()) 是否成立，就继续执行下面的循环。
            while time.monotonic() < limit and not pending.done():
                # 进入执行“调用 observer.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
                with observer.cursor() as cursor:
                    # 执行传入的数据库语句并取得结果。
                    cursor.execute("SELECT pg_stat_clear_snapshot()")
                    # 执行传入的数据库语句并取得结果。
                    cursor.execute("SELECT wait_event_type FROM pg_stat_activity WHERE pid=%s",
                                   # 继续传入或组合读取 backendpid（_backend_pid） 的值，完成上面尚未结束的表达式。
                                   (second.get_backend_pid(),))
                    # 把执行“取出查询结果中的一行”得到的结果保存到 当前记录（row）。
                    row = cursor.fetchone()
                    # 检查条件 row and row[0] == 'Lock' 是否成立；成立时执行缩进的分支。
                    if row and row[0] == "Lock":
                        # 把真，表示条件成立或开关打开保存到 加锁等待已读（lock_wait_seen）。
                        lock_wait_seen = True
                        # 立即退出当前循环，不再处理后续条目。
                        break
                # 等待指定时间后再继续，给轮询或其他任务留出执行机会。
                time.sleep(0.03)
            # 提交本次数据库事务，使本次修改一起生效。
            first.commit()
            # 把执行“调用 pending.result 完成结果处理”得到的结果保存到 实际（actual）。
            actual = pending.result(timeout=8)
            # 检查条件 not lock_wait_seen or actual != ('23505', expected_constraint) 是否成立；成立时执行缩进的分支。
            if not lock_wait_seen or actual != ("23505", expected_constraint):
                # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                raise RuntimeError(f"Concurrent uniqueness failed: waited={lock_wait_seen}, result={actual}")
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        first.rollback()
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        second.rollback()
        # 释放连接、文件或客户端占用的资源。
        first.close()
        # 释放连接、文件或客户端占用的资源。
        second.close()


# 函数 concurrent_review：调用 concurrent_review 完成concurrent复盘处理；接收 执行计时ddl（execution_ddl）。
def concurrent_review(execution_ddl):
    # 两连接的真实索引竞争需要对方可见的 DDL，单独使用随机 schema。
    # 仅这个临时 schema 内提交竞争赢家，最后删除本次创建的 schema；不接触 public。
    # 把按 'asterway_execution_race_' + uuid.uuid4().hex 计算的结果保存到 数据库分区（schema）。
    schema = "asterway_execution_race_" + uuid.uuid4().hex
    # 检查条件 not re.fullmatch('asterway_execution_race_[0-9a-f]{32}', schema) 是否成立；成立时执行缩进的分支。
    if not re.fullmatch(r"asterway_execution_race_[0-9a-f]{32}", schema):
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Unexpected concurrent review schema name")
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = connect()
    # 把假，表示条件不成立或开关关闭保存到 创建时（created）。
    created = False
    # 把空列表或集合保存到 cases。
    cases = []
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(ddl_body(execution_ddl))
            # 执行传入的数据库语句并取得结果。
            # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
            # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
            # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
            # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
            # 内嵌 SQL：读取后面列出的字段或计算结果。
            cursor.execute("""
                INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,controller_session_id)
                VALUES(904,1,'RUNNING','2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid');
                INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at,finished_at,finish_reason)
                VALUES(905,1,'FINISHED','2026-11-04 00:00Z','2026-11-04 00:00Z','2026-11-04 00:01Z','USER_FINISH');
                SELECT user_id,id FROM execution_session;
            """)
            # 把执行“调用 dict 完成dict处理”得到的结果保存到 parents。
            parents = dict(cursor.fetchall())
        # 提交本次数据库事务，使本次修改一起生效。
        connection.commit()
        # 把真，表示条件成立或开关打开保存到 创建时（created）。
        created = True
        # 把固定文字 'INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,contr…'保存到 运行中（running）。
        # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        running = """INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id)
            VALUES(901,{task},'2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid')"""
        # 调用 unique_race 完成唯一race处理。
        unique_race(connection, schema, running.format(task=1), running.format(task=2),
                    # 继续传入或组合表达式 "uk_execution_session_user_running") 的结果，完成上面尚未结束的表达式。
                    "uk_execution_session_user_running")
        # 把新条目放到列表末尾。
        cases.append("concurrent_one_running_per_user")
        # 把固定文字 'INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_a…'保存到 已暂停（paused）。
        # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        paused = """INSERT INTO execution_session(user_id,task_id,status,started_at,last_heartbeat_at)
            VALUES(902,1,'PAUSED','2026-11-04 00:00Z','2026-11-04 00:00Z')"""
        # 把固定文字 'INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,contr…'保存到 same任务运行中（same_task_running）。
        # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        same_task_running = """INSERT INTO execution_session(user_id,task_id,started_at,last_heartbeat_at,controller_session_id)
            VALUES(902,1,'2026-11-04 00:00Z','2026-11-04 00:00Z','race-sid')"""
        # 调用 unique_race 完成唯一race处理。
        unique_race(connection, schema, paused, same_task_running,
                    # 继续传入或组合表达式 "uk_execution_session_user_task_unfinished") 的结果，完成上面尚未结束的表达式。
                    "uk_execution_session_user_task_unfinished")
        # 把新条目放到列表末尾。
        cases.append("concurrent_one_unfinished_per_task")
        # 把固定文字 'INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,e…'保存到 是否展开连续计时段（open_segment）。
        # 内嵌 SQL：向 execution_segment 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        open_segment = """INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,eligible_until,time_zone)
            VALUES({parent},1,{seq},'2026-11-04 00:00Z','2026-11-05 00:00Z','UTC')"""
        # 调用 unique_race 完成唯一race处理。
        unique_race(connection, schema, open_segment.format(parent=parents[904], seq=1),
                    # 继续传入或组合调用 open_segment.format 完成格式处理，完成上面尚未结束的表达式。
                    open_segment.format(parent=parents[904], seq=2), "uk_execution_segment_open")
        # 把新条目放到列表末尾。
        cases.append("concurrent_one_open_segment")
        # 把执行“调用 "INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,\n ended_at,end_reason,eligible_until,time_zone) VALUES({parent},1,1,'2026-11-04 00:00Z',\n '2026-11-04 00:01Z','FINISH','2026-11-05 00:00Z','UTC')".format 完成格式处理”得到的结果保存到 closed连续计时段（closed_segment）。
        # 内嵌 SQL：向 execution_segment 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：结束前面的条件表达式、循环或数据库程序块。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'2026-11-04 00:01Z','FINISH','2026-11-05 00:00Z','UTC')。
        closed_segment = """INSERT INTO execution_segment(execution_id,assignment_id,segment_no,started_at,
            ended_at,end_reason,eligible_until,time_zone) VALUES({parent},1,1,'2026-11-04 00:00Z',
            '2026-11-04 00:01Z','FINISH','2026-11-05 00:00Z','UTC')""".format(parent=parents[905])
        # 调用 unique_race 完成唯一race处理。
        unique_race(connection, schema, closed_segment, closed_segment, "uk_execution_segment_sequence")
        # 把新条目放到列表末尾。
        cases.append("concurrent_segment_sequence_unique")
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 检查条件 created 是否成立；成立时执行缩进的分支。
        if created:
            # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
            with connection.cursor() as cursor:
                # 执行传入的数据库语句并取得结果。
                cursor.execute("SELECT current_database()")
                # 检查条件 cursor.fetchone()[0] != REVIEW_DATABASE 是否成立；成立时执行缩进的分支。
                if cursor.fetchone()[0] != REVIEW_DATABASE:
                    # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                    raise RuntimeError("Refusing concurrent review cleanup outside the review database")
                # 执行传入的数据库语句并取得结果。
                cursor.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(schema)))
            # 提交本次数据库事务，使本次修改一起生效。
            connection.commit()
            # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
            with connection.cursor() as cursor:
                # 执行传入的数据库语句并取得结果。
                cursor.execute("SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
                # 检查条件 not cursor.fetchone()[0] 是否成立；成立时执行缩进的分支。
                if not cursor.fetchone()[0]:
                    # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                    raise RuntimeError("Concurrent review schema was not removed")
        # 释放连接、文件或客户端占用的资源。
        connection.close()
    # 把cases交还给调用方，本函数到此结束。
    return cases


# 函数 main：调用 main 完成程序入口处理。
def main():
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 原始值sources（raw_sources）。
    raw_sources = {module: (ROOT / f"backend/app/src/main/resources/sql/{module}.sql").read_bytes()
                   # 继续传入或组合表达式 for module in MODULES} 的结果，完成上面尚未结束的表达式。
                   for module in MODULES}
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 sources。
    sources = {module: contents.decode("utf-8-sig") for module, contents in raw_sources.items()}
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 hashes。
    hashes = {f"backend/app/src/main/resources/sql/{module}.sql": hashlib.sha256(contents).hexdigest()
              # 继续传入或组合调用 for module, contents in raw_sources.items 完成数据列表处理，完成上面尚未结束的表达式。
              for module, contents in raw_sources.items()}
    # 把依次包含 CHECKS路径（CHECKS_PATH）、QUERIES路径（QUERIES_PATH）、执行“调用 Path 完成路径处理”得到的结果、按 ROOT / 'scripts/review_execution_sql.ps1' 计算的结果 的集合保存到 校验结果文件列表（verification_files）。
    verification_files = (CHECKS_PATH, QUERIES_PATH, Path(__file__), ROOT / "scripts/review_execution_sql.ps1")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 校验结果hashes（verification_hashes）。
    verification_hashes = {path.relative_to(ROOT).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
                           # 继续传入或组合表达式 for path in verification_files} 的结果，完成上面尚未结束的表达式。
                           for path in verification_files}
    # 把执行“调用 rollback_review 完成rollback复盘处理”得到的结果保存到 cases版本号（(cases, version)）。
    cases, version = rollback_review(sources)
    # 把执行“调用 concurrent_review 完成concurrent复盘处理”得到的结果保存到 concurrentcases（concurrent_cases）。
    concurrent_cases = concurrent_review(sources["execution"])
    # 从执行“调用 {**hashes, **verification_hashes}.items 完成数据列表处理”得到的结果依次取出relative路径digest（(relative_path, digest)），每项执行一次下面的处理。
    for relative_path, digest in {**hashes, **verification_hashes}.items():
        # 检查条件 hashlib.sha256((ROOT / relative_path).read_bytes()).hexdigest() != digest 是否成立；成立时执行缩进的分支。
        if hashlib.sha256((ROOT / relative_path).read_bytes()).hexdigest() != digest:
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("SQL review input changed during execution; repeat the review")
    # 把按字段名保存多个值的字典保存到 复盘报告（report）。
    report = {
        # 填写状态（status）字段，值为固定状态或编码 PASSED（PASSED）。
        "status": "PASSED",
        # 填写verifiedAt字段，值为当前真实时间，用于记录操作发生的时刻。
        "verifiedAt": datetime.now(timezone.utc).isoformat(),
        # 填写数据库（database）字段，值为复盘数据库（REVIEW_DATABASE）。
        "database": REVIEW_DATABASE,
        # 填写postgresql版本号（postgresqlVersion）字段，值为版本号（version）。
        "postgresqlVersion": version,
        # 填写数据范围（scope）字段，值为固定文字 'SQL-only constraints, query arithmetic, staged seeds and PostgreSQL index concur…'。
        "scope": "SQL-only constraints, query arithmetic, staged seeds and PostgreSQL index concurrency; no Service/API acceptance",
        # 填写checks字段，值为集合或文字的长度。
        "checks": len(cases) + len(concurrent_cases),
        # 填写passed字段，值为集合或文字的长度。
        "passed": len(cases) + len(concurrent_cases),
        # 填写主入口复盘数据库分区RolledBack（mainReviewSchemaRolledBack）字段，值为真，表示条件成立或开关打开。
        "mainReviewSchemaRolledBack": True,
        # 填写concurrent复盘数据库分区Removed（concurrentReviewSchemaRemoved）字段，值为真，表示条件成立或开关打开。
        "concurrentReviewSchemaRemoved": True,
        # 填写concurrent复盘Committed仅InsideTemporary数据库分区（concurrentReviewCommittedOnlyInsideTemporarySchema）字段，值为真，表示条件成立或开关打开。
        "concurrentReviewCommittedOnlyInsideTemporarySchema": True,
        # 填写development数据库Connected（developmentDatabaseConnected）字段，值为假，表示条件不成立或开关关闭。
        "developmentDatabaseConnected": False,
        # 填写development数据库已修改（developmentDatabaseChanged）字段，值为假，表示条件不成立或开关关闭。
        "developmentDatabaseChanged": False,
        # 填写sqlSha256字段，值为hashes。
        "sqlSha256": hashes,
        # 填写校验结果Sha256（verificationSha256）字段，值为校验结果hashes（verification_hashes）。
        "verificationSha256": verification_hashes,
        # 填写cases字段，值为cases。
        "cases": cases,
        # 填写concurrentCases字段，值为concurrentcases（concurrent_cases）。
        "concurrentCases": concurrent_cases,
    }
    # 按指定编码写入文件文字。
    (ROOT / "docs/execution/sql-check-results.json").write_text(
        # 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的表达式。
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # 调用 print 完成print处理。
    print(f"Execution SQL checks passed: {report['passed']}/{report['checks']}; "
          "review transaction rolled back; concurrent schema removed; development database not connected.")


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == "__main__":
    # 调用 main 完成程序入口处理。
    main()
