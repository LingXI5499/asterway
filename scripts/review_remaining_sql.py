# 检查复盘剩余sql相关行为，断言失败时报告具体不符合预期的场景。
# 后续模块 SQL 的隔离复核；仅使用专用校验库，不连接开发库、不运行 Java 测试框架。

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 引入 hashlib，供下面调用其功能。
import hashlib

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 引入 re，供下面调用其功能。
import re

# 引入 sys，供下面调用其功能。
import sys

# 引入 uuid，供下面调用其功能。
import uuid

# 从 zoneinfo 引入 时区信息（ZoneInfo）。
from zoneinfo import ZoneInfo

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 从 psycopg2 引入 sql。
from psycopg2 import sql

# 复用既有 SQL 竞争校验函数，不在共享工作区生成导入缓存。
# 把真，表示条件成立或开关打开保存到 sys中的dont写入bytecode（sys.dont_write_bytecode）。
sys.dont_write_bytecode = True

# 引入 执行计时复盘（execution_review），供下面调用其功能。
import review_execution_sql as execution_review


# 把从执行“调用 Path(__file__).resolve 完成resolve处理”得到的结果的parents属性中按固定数值 1取出的内容保存到 根（ROOT）。
ROOT = Path(__file__).resolve().parents[1]
# 把按 ROOT / 'backend/app/src/main/resources/sql' 计算的结果保存到 SQL根（SQL_ROOT）。
SQL_ROOT = ROOT / "backend/app/src/main/resources/sql"
# 把按 ROOT / 'docs/remaining' 计算的结果保存到 复盘报告根（REPORT_ROOT）。
REPORT_ROOT = ROOT / "docs/remaining"
# 把固定文字 'asterway_identity_test'保存到 数据库（DATABASE）。
DATABASE = "asterway_identity_test"
# 把依次包含 固定文字 'identity'、固定文字 'goal'、固定文字 'stage'、固定文字 'task'、固定文字 'planning'等内容 的集合保存到 MODULES。
MODULES = ("identity", "goal", "stage", "task", "planning", "execution", "habit", "notification")
# 把依次包含 固定文字 'habit_category'、固定文字 'habit'、固定文字 'habit_revision'、固定文字 'habit_checkin'、固定文字 'notification_message'等内容 的集合保存到 新TABLES（NEW_TABLES）。
NEW_TABLES = ("habit_category", "habit", "habit_revision", "habit_checkin", "notification_message",
              # 继续传入或组合表达式 "goal_state_history", "stage_state_history", "task_state_history") 的结果，完成上面尚未结束的表达式。
              "goal_state_history", "stage_state_history", "task_state_history")
# 把按字段名保存多个值的字典保存到 MARKERS。
MARKERS = {
    # 填写账户管理（identity）字段，值为表达式 ("REMAINING AUTHORIZATION",) 的结果。
    "identity": ("REMAINING AUTHORIZATION",),
    # 填写目标（goal）字段，值为表达式 ("GOAL CONDITION UPGRADE", "GOAL STATE HISTORY", "REMAINING GOAL HISTORY BOOTSTRAP") 的结果。
    "goal": ("GOAL CONDITION UPGRADE", "GOAL STATE HISTORY", "REMAINING GOAL HISTORY BOOTSTRAP"),
    # 填写阶段（stage）字段，值为表达式 ("STAGE STATE HISTORY", "REMAINING STAGE HISTORY BOOTSTRAP") 的结果。
    "stage": ("STAGE STATE HISTORY", "REMAINING STAGE HISTORY BOOTSTRAP"),
    # 填写任务（task）字段，值为表达式 ("TASK STATE HISTORY", "REMAINING TASK HISTORY BOOTSTRAP") 的结果。
    "task": ("TASK STATE HISTORY", "REMAINING TASK HISTORY BOOTSTRAP"),
}
# 把空列表或集合保存到 CASES。
CASES = []


# 函数 connect：调用 connect 完成连接处理；接收 数据库分区（schema）。
def connect(schema=None):
    # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = psycopg2.connect(
        # 传入dbname，值为按表达式计算得到的值：DATABASE, user="postgres", host="/var/run/postgresql"。
        dbname=DATABASE, user="postgres", host="/var/run/postgresql",
        # 传入application名称（application_name），值为表达式 "asterway_remaining_sql_review") 的结果。
        application_name="asterway_remaining_sql_review")
    # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
    with connection.cursor() as cursor:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT current_database()")
        # 检查条件 cursor.fetchone()[0] != DATABASE 是否成立；成立时执行缩进的分支。
        if cursor.fetchone()[0] != DATABASE:
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("Refusing SQL review outside the dedicated review database")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL statement_timeout='10s'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL lock_timeout='5s'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL TIME ZONE 'UTC'")
        # 从依次包含 固定文字 'remaining_backend_ready'、固定文字 'remaining_frontend_ready'、固定文字 'remaining_history_bootstrap'、固定文字 'execution_backend_ready'、固定文字 'execution_frontend_ready' 的集合依次取出名称（name），每项执行一次下面的处理。
        for name in ("remaining_backend_ready", "remaining_frontend_ready", "remaining_history_bootstrap",
                     # 继续传入或组合表达式 "execution_backend_ready", "execution_frontend_ready"): 的结果，完成上面尚未结束的表达式。
                     "execution_backend_ready", "execution_frontend_ready"):
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SELECT set_config(%s,'false',true)", ("asterway." + name,))
        # 检查条件 schema 是否成立；成立时执行缩进的分支。
        if schema:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
    # 把数据库连接（connection）交还给调用方，本函数到此结束。
    return connection


# 函数 body：调用 body 完成请求正文处理；接收 contents。
def body(contents):
    # 把执行“调用 execution_review.ddl_body 完成ddl请求正文处理”得到的结果交还给调用方，本函数到此结束。
    return execution_review.ddl_body(contents)


# 函数 block：调用 block 完成block处理；接收 contents、标记（marker）。
def block(contents, marker):
    # 把依次包含 按 '— BEGIN ' + marker 计算的结果、按 '— END ' + marker 计算的结果 的集合保存到 begin结束（(begin, end)）。
    begin, end = "-- BEGIN " + marker, "-- END " + marker
    # 检查条件 contents.count(begin) != 1 or contents.count(end) != 1 是否成立；成立时执行缩进的分支。
    if contents.count(begin) != 1 or contents.count(end) != 1:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Expected one manual SQL block: " + marker)
    # 把从执行“按指定分隔规则拆分文字”得到的结果中按固定数值 0取出的内容交还给调用方，本函数到此结束。
    return contents.split(begin, 1)[1].split(end, 1)[0]


# 函数 old_source：调用 old_source 完成旧来源处理；接收 module、contents。
def old_source(module, contents):
    # 从执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果依次取出标记（marker），每项执行一次下面的处理。
    for marker in MARKERS.get(module, ()):
        # 把执行“调用 re.sub 完成sub处理”得到的结果保存到 contents。
        contents = re.sub(r"-- BEGIN " + re.escape(marker) + r".*?-- END " + re.escape(marker),
                          # 继续传入或组合表达式 "", contents, count=1, flags=re.DOTALL) 的结果，完成上面尚未结束的表达式。
                          "", contents, count=1, flags=re.DOTALL)
    # 把contents交还给调用方，本函数到此结束。
    return contents


# 函数 check：调用 check 完成核对处理；接收 名称（name）、condition。
def check(name, condition):
    # 检查条件 not condition 是否成立；成立时执行缩进的分支。
    if not condition:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("SQL review assertion failed: " + name)
    # 检查条件 name in CASES 是否成立；成立时执行缩进的分支。
    if name in CASES:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Duplicate SQL review case: " + name)
    # 把新条目放到列表末尾。
    CASES.append(name)


# 函数 assertion：断言 ion；接收 数据库语句执行器（cursor）、名称（name）、statement、parameters。
def assertion(cursor, name, statement, parameters=None):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(statement, parameters)
    # 调用 check 完成核对处理。
    check(name, cursor.fetchone()[0] is True)


# 函数 rows：调用 rows 完成记录列表处理；接收 数据库语句执行器（cursor）、statement、parameters。
def rows(cursor, statement, parameters=None):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(statement, parameters)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 names。
    names = [column.name for column in cursor.description]
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
    return [dict(zip(names, row)) for row in cursor.fetchall()]


# 函数 rejection：调用 rejection 完成rejection处理；接收 数据库语句执行器（cursor）、名称（name）、statement、预计（expected）。
def rejection(cursor, name, statement, expected="23514"):
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT rejected_statement")
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 执行传入的数据库语句并取得结果。
        cursor.execute(statement)
    # 捕获psycopg2.Error异常，执行下面的错误处理，异常对象保存为 error。
    except psycopg2.Error as error:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT rejected_statement")
        # 调用 check 完成核对处理。
        check(name, error.pgcode == expected)
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT rejected_statement")
        # 调用 check 完成核对处理。
        check(name, False)
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("RELEASE SAVEPOINT rejected_statement")


# 函数 fingerprint：调用 fingerprint 完成fingerprint处理；接收 数据库语句执行器（cursor）。
def fingerprint(cursor):
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续列出本次使用的字段：a中的attidentity（a.attidentity）。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：按连接条件把关联记录合并进本次查询；LEFT JOIN 会保留左侧没有匹配项的记录。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        SELECT c.relname,a.attnum,a.attname,format_type(a.atttypid,a.atttypmod),a.attnotnull,
            a.attidentity,pg_get_expr(d.adbin,d.adrelid)
        FROM pg_class c JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
        LEFT JOIN pg_attrdef d ON d.adrelid=c.oid AND d.adnum=a.attnum
        WHERE c.relnamespace=current_schema()::regnamespace AND c.relkind='r' ORDER BY c.relname,a.attnum
    """)
    # 把执行“取出查询结果中的所有行”得到的结果保存到 columns。
    columns = cursor.fetchall()
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        SELECT c.relname,con.conname,con.contype,pg_get_constraintdef(con.oid)
        FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid
        WHERE c.relnamespace=current_schema()::regnamespace ORDER BY c.relname,con.conname
    """)
    # 把执行“取出查询结果中的所有行”得到的结果保存到 constraints。
    constraints = cursor.fetchall()
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    cursor.execute("""
        SELECT tablename,indexname,replace(indexdef,current_schema()||'.','')
        FROM pg_indexes WHERE schemaname=current_schema() ORDER BY tablename,indexname
    """)
    # 把依次包含 columns、constraints、执行“取出查询结果中的所有行”得到的结果 的集合交还给调用方，本函数到此结束。
    return columns, constraints, cursor.fetchall()


# 函数 run_isolated：调用 run_isolated 完成方案运行isolated处理；接收 前缀（prefix）、callback。
def run_isolated(prefix, callback):
    # 把按 prefix + uuid.uuid4().hex 计算的结果保存到 数据库分区（schema）。
    schema = prefix + uuid.uuid4().hex
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
            # 把执行“调用 callback 完成callback处理”得到的结果保存到 结果（result）。
            result = callback(cursor)
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 断言 ion。
            assertion(cursor, prefix + "rolled_back",
                      # 继续传入或组合表达式 "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,)) 的结果，完成上面尚未结束的表达式。
                      "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
        # 把结果（result）交还给调用方，本函数到此结束。
        return result
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 释放连接、文件或客户端占用的资源。
        connection.close()


# 函数 load_queries：加载 queries；接收 数据库语句执行器（cursor）、sources。
def load_queries(cursor, sources):
    # 从依次包含 固定文字 'statistics'、固定文字 'dashboard' 的集合依次取出module，每项执行一次下面的处理。
    for module in ("statistics", "dashboard"):
        # 执行传入的数据库语句并取得结果。
        cursor.execute(sources[module])


# 函数 auth_checks：调用 auth_checks 完成认证checks处理；接收 数据库语句执行器（cursor）、sources。
def auth_checks(cursor, sources):
    # 把执行“调用 block 完成block处理”得到的结果保存到 当前登录授权数据（auth）。
    auth = block(sources["identity"], "REMAINING AUTHORIZATION")
    # 把固定文字 "split_part(code,':',1) IN ('habit','notification','dashboard','statistics')"保存到 predicate。
    predicate = "split_part(code,':',1) IN ('habit','notification','dashboard','statistics')"
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assertion(cursor, "new_permissions_default_disabled", f"""
        SELECT count(*)=23 AND bool_and(NOT is_enabled) FROM identity_permission WHERE {predicate}
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "new_menu_nodes_default_hidden_disabled", f"""
        SELECT count(*)=23 AND bool_and(NOT m.is_enabled AND NOT m.is_visible)
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='true'")
    # 调用 rejection 完成rejection处理。
    rejection(cursor, "cannot_enable_frontend_without_backend", auth, "P0001")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='false'")
    # 从依次包含 依次包含 固定文字 "UPDATE identity_permission SET is_builtin=false WHERE code='habit:habit:read'"、固定文字 'custom_permission_collision' 的集合、依次包含 固定文字 "UPDATE identity_menu SET is_builtin=false WHERE code='growth_habits'"、固定文字 'custom_menu_collision' 的集合、依次包含 固定文字 "UPDATE identity_menu SET route_path='/wrong' WHERE code='growth_habits'"、固定文字 'menu_route_contract_collision' 的集合、依次包含 固定文字 'UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHE…'、固定文字 'deleted_permission_collision' 的集合 的集合依次取出statement名称（(statement, name)），每项执行一次下面的处理。
    for statement, name in (
        # 继续传入或组合表达式 ("UPDATE identity_permission SET is_builtin=false WHERE code='habit:habit:read'", "custom_permission_collision") 的结果，完成上面尚未结束的表达式。
        ("UPDATE identity_permission SET is_builtin=false WHERE code='habit:habit:read'", "custom_permission_collision"),
        # 继续传入或组合表达式 ("UPDATE identity_menu SET is_builtin=false WHERE code='growth_habits'", "custom_menu_collision") 的结果，完成上面尚未结束的表达式。
        ("UPDATE identity_menu SET is_builtin=false WHERE code='growth_habits'", "custom_menu_collision"),
        # 继续传入或组合按表达式计算得到的值：("UPDATE identity_menu SET route_path='/wrong' WHERE code='growth_habits'", "menu_route_contract_collision")，完成上面尚未结束的表达式。
        ("UPDATE identity_menu SET route_path='/wrong' WHERE code='growth_habits'", "menu_route_contract_collision"),
        # 继续传入或组合当前真实时间，用于记录操作发生的时刻，完成上面尚未结束的表达式。
        ("UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHERE code='habit:checkin:read'", "deleted_permission_collision"),
    ):
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SAVEPOINT collision_fixture")
        # 执行传入的数据库语句并取得结果。
        cursor.execute(statement)
        # 调用 rejection 完成rejection处理。
        rejection(cursor, name, auth, "P0001")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT collision_fixture")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("RELEASE SAVEPOINT collision_fixture")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT missing_table_fixture")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ALTER TABLE habit_checkin RENAME TO habit_checkin_not_ready")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    # 调用 rejection 完成rejection处理。
    rejection(cursor, "cannot_enable_backend_before_all_tables", auth, "P0001")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT missing_table_fixture")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT missing_table_fixture")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(auth)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assertion(cursor, "backend_stage_enables_23_permissions", f"""
        SELECT count(*)=23 AND bool_and(is_enabled) FROM identity_permission WHERE {predicate}
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "backend_stage_keeps_all_new_entries_hidden", """
        SELECT count(*)=23 AND bool_and(m.is_enabled AND NOT m.is_visible)
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
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
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code='habit:habit:list';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_habits';
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(auth)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    assertion(cursor, "management_roles_new_grants_removed", """
        SELECT NOT EXISTS(SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
            JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code IN ('ADMIN','SUPER_ADMIN')
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics'))
        AND NOT EXISTS(SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
            JOIN identity_menu m ON m.id=rm.menu_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code='growth_habits')
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    assertion(cursor, "ordinary_role_owns_all_new_permission_relations", """
        SELECT count(*)=23 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
        JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code='USER'
            AND split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='true'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(auth)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "frontend_stage_reveals_exactly_five_pages", """
        SELECT count(*)=23 AND count(*) FILTER(WHERE m.is_visible)=5
            AND bool_and(m.is_enabled AND m.is_visible=(m.menu_type='PAGE'))
        FROM identity_menu m JOIN identity_permission p ON p.id=m.permission_id
        WHERE split_part(p.code,':',1) IN ('habit','notification','dashboard','statistics')
    """)
    # 把表达式 "SELECT jsonb_build_object(\n 'p',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission)… 的结果保存到 versions。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'p',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'m',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'r',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role))。
    versions = """SELECT jsonb_build_object(
        'p',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_permission),
        'm',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_menu),
        'r',(SELECT jsonb_agg(jsonb_build_array(id,version) ORDER BY id) FROM identity_role))"""
    # 执行传入的数据库语句并取得结果。
    cursor.execute(versions)
    # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 变化前（before）。
    before = cursor.fetchone()[0]
    # 执行传入的数据库语句并取得结果。
    cursor.execute(auth)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(versions)
    # 调用 check 完成核对处理。
    check("authorization_replay_keeps_versions", cursor.fetchone()[0] == before)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_frontend_ready='false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(auth)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assertion(cursor, "preparation_replay_preserves_enabled_features", f"""
        SELECT count(*)=23 AND bool_and(is_enabled) FROM identity_permission WHERE {predicate}
    """)


# 函数 initialize_facts：调用 initialize_facts 完成初始化facts处理；接收 数据库语句执行器（cursor）、sources。
def initialize_facts(cursor, sources):
    # 从依次包含 固定文字 'goal'、固定文字 'stage'、固定文字 'task' 的集合依次取出module，每项执行一次下面的处理。
    for module in ("goal", "stage", "task"):
        # 执行传入的数据库语句并取得结果。
        cursor.execute(block(sources[module], "REMAINING " + module.upper() + " HISTORY BOOTSTRAP"))
    # 执行传入的数据库语句并取得结果。
    cursor.execute(block(sources["notification"], "REMAINING NOTIFICATION BOOTSTRAP"))


# 函数 fresh_review：调用 fresh_review 完成fresh复盘处理；接收 sources。
def fresh_review(sources):
    # 函数 run：调用 run 完成方案运行处理；接收 数据库语句执行器（cursor）。
    def run(cursor):
        # 从MODULES依次取出module，每项执行一次下面的处理。
        for module in MODULES:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(sources[module]))
        # 把执行“调用 fingerprint 完成fingerprint处理”得到的结果保存到 数据库分区shape（schema_shape）。
        schema_shape = fingerprint(cursor)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        assertion(cursor, "fresh_database_has_26_business_tables", """
            SELECT count(*)=26 FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'
        """)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        assertion(cursor, "no_physical_foreign_keys", """
            SELECT NOT EXISTS(SELECT 1 FROM pg_constraint WHERE connamespace=current_schema()::regnamespace AND contype='f')
        """)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        assertion(cursor, "no_permanent_business_triggers_or_functions", """
            SELECT NOT EXISTS(SELECT 1 FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid
                WHERE c.relnamespace=current_schema()::regnamespace AND NOT t.tgisinternal)
            AND NOT EXISTS(SELECT 1 FROM pg_proc WHERE pronamespace=current_schema()::regnamespace)
        """)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：指定本次查询主要读取的表或子查询。
        # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        assertion(cursor, "all_new_table_fields_have_comments", """
            SELECT bool_and(col_description(c.oid,a.attnum) IS NOT NULL)
            FROM pg_class c JOIN pg_attribute a ON a.attrelid=c.oid AND a.attnum>0 AND NOT a.attisdropped
            WHERE c.relnamespace=current_schema()::regnamespace AND c.relname=ANY(%s)
        """, (list(NEW_TABLES),))
        # 加载 queries。
        load_queries(cursor, sources)
        # 执行传入的数据库语句并取得结果。
        cursor.execute((REPORT_ROOT / "SQL层校验.sql").read_text(encoding="utf-8-sig"))
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT name FROM remaining_validation_result ORDER BY name")
        # 从执行“取出查询结果中的所有行”得到的结果依次取出当前记录（row），每项执行一次下面的处理。
        for row in cursor.fetchall():
            # 调用 check 完成核对处理。
            check(row[0], True)
        # 从依次包含 依次包含 固定文字 'ALTER TABLE goal ALTER COLUMN completion_condition_met SET DEFAULT true'、固定文字 'upgrade_rejects_incompatible_condition_default' 的集合、依次包含 固定文字 'ALTER TABLE goal ALTER COLUMN completion_round DROP NOT NULL'、固定文字 'upgrade_rejects_nullable_round' 的集合 的集合依次取出mutation名称（(mutation, name)），每项执行一次下面的处理。
        for mutation, name in (
            # 继续传入或组合表达式 ("ALTER TABLE goal ALTER COLUMN completion_condition_met SET DEFAULT true", "upgrade_rejects_incompatible_condition_defa… 的结果，完成上面尚未结束的表达式。
            ("ALTER TABLE goal ALTER COLUMN completion_condition_met SET DEFAULT true", "upgrade_rejects_incompatible_condition_default"),
            # 继续传入或组合表达式 ("ALTER TABLE goal ALTER COLUMN completion_round DROP NOT NULL", "upgrade_rejects_nullable_round") 的结果，完成上面尚未结束的表达式。
            ("ALTER TABLE goal ALTER COLUMN completion_round DROP NOT NULL", "upgrade_rejects_nullable_round"),
        ):
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SAVEPOINT incompatible_goal_columns")
            # 执行传入的数据库语句并取得结果。
            cursor.execute(mutation)
            # 调用 rejection 完成rejection处理。
            rejection(cursor, name, block(sources["goal"], "GOAL CONDITION UPGRADE"), "P0001")
            # 执行传入的数据库语句并取得结果。
            cursor.execute("ROLLBACK TO SAVEPOINT incompatible_goal_columns")
            # 执行传入的数据库语句并取得结果。
            cursor.execute("RELEASE SAVEPOINT incompatible_goal_columns")
        # 调用 auth_checks 完成认证checks处理。
        auth_checks(cursor, sources)
        # 调用 fact_checks 完成factchecks处理。
        fact_checks(cursor, sources)
        # 调用 query_checks 完成查询条件checks处理。
        query_checks(cursor)
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT current_setting('server_version')")
        # 把依次包含 数据库分区shape（schema_shape）、从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容 的集合交还给调用方，本函数到此结束。
        return schema_shape, cursor.fetchone()[0]
    # 把执行“调用 run_isolated 完成方案运行isolated处理”得到的结果交还给调用方，本函数到此结束。
    return run_isolated("asterway_remaining_review_", run)


# 函数 fact_checks：调用 fact_checks 完成factchecks处理；接收 数据库语句执行器（cursor）、sources。
def fact_checks(cursor, sources):
    # SQL fixture 由校验文件建立；默认脚本重放不应悄悄导入历史或发送提醒。
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    assertion(cursor, "bootstrap_default_does_not_write_facts", """
        SELECT NOT EXISTS(SELECT 1 FROM goal_state_history WHERE event_type='BASELINE')
            AND NOT EXISTS(SELECT 1 FROM notification_message WHERE user_id=301)
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='true'")
    # 调用 rejection 完成rejection处理。
    rejection(cursor, "bootstrap_requires_approved_backend",
              # 继续传入或组合调用 block 完成block处理，完成上面尚未结束的表达式。
              block(sources["goal"], "REMAINING GOAL HISTORY BOOTSTRAP"), "P0001")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    assertion(cursor, "goal_baseline_uses_initialization_time", """
        SELECT count(*)>0 AND bool_and(source_type='SYSTEM_INIT' AND before_status IS NULL
            AND occurred_at>=transaction_timestamp()) FROM goal_state_history WHERE event_type='BASELINE'
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "trusted_old_task_completion_imported_once", """
        SELECT count(*)=1 AND bool_and(h.occurred_at=t.completed_at)
        FROM task_state_history h JOIN task t ON t.id=h.task_id
        WHERE t.name='notify_done' AND h.event_type='COMPLETION_IMPORTED'
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "future_old_task_completion_not_invented", """
        SELECT NOT EXISTS(SELECT 1 FROM task_state_history h JOIN task t ON t.id=h.task_id
            WHERE t.name='untrusted_future_completion' AND h.event_type='COMPLETION_IMPORTED')
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assertion(cursor, "object_with_real_history_gets_no_baseline", """
        SELECT NOT EXISTS(SELECT 1 FROM task_state_history h WHERE h.user_id=401 AND h.event_type='BASELINE')
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "only_exact_qualified_ordinary_goal_gets_initial_notice", """
        SELECT count(*)=1 AND bool_and(g.name='notify_qualified' AND n.condition_round=1
            AND n.created_at>=transaction_timestamp() AND n.handle_status='PENDING')
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE n.user_id IN(301,302,303)
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "rounded_100_percent_goal_does_not_qualify", """
        SELECT NOT g.completion_condition_met AND g.completion_round=0
        FROM goal g WHERE name='notify_rounded'
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "orphaned_legacy_owner_cannot_receive_notice", """
        SELECT NOT g.completion_condition_met AND g.completion_round=4
            AND NOT EXISTS(SELECT 1 FROM notification_message n WHERE n.goal_id=g.id)
        FROM goal g WHERE g.name='orphaned_legacy_goal'
    """)
    # 把按表达式计算得到的值："SELECT jsonb_build_object(\n 'g',(SELECT jsonb_agg(jsonb_build_array(id,version,completion_round) ORDER BY id) FROM goa…保存到 snapshots。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续列出本次使用的字段：版本号（version）。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'gh',(SELECT count(*) FROM goal_state_history),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'sh',(SELECT count(*) FROM stage_state_history),。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'th',(SELECT count(*) FROM task_state_history),。
    # 内嵌 SQL：继续列出本次使用的字段：版本号（version）。
    snapshots = """SELECT jsonb_build_object(
        'g',(SELECT jsonb_agg(jsonb_build_array(id,version,completion_round) ORDER BY id) FROM goal),
        'gh',(SELECT count(*) FROM goal_state_history),
        'sh',(SELECT count(*) FROM stage_state_history),
        'th',(SELECT count(*) FROM task_state_history),
        'n',(SELECT jsonb_agg(jsonb_build_array(id,version,handle_status) ORDER BY id) FROM notification_message))"""
    # 执行传入的数据库语句并取得结果。
    cursor.execute(snapshots)
    # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 变化前（before）。
    before = cursor.fetchone()[0]
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(snapshots)
    # 调用 check 完成核对处理。
    check("fact_bootstrap_replay_is_idempotent", cursor.fetchone()[0] == before)
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        UPDATE notification_message SET read_at=clock_timestamp(),updated_at=clock_timestamp()
        WHERE user_id=301 AND handle_status='PENDING'
    """)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "read_does_not_handle_or_complete_goal", """
        SELECT n.handle_status='PENDING' AND n.handled_at IS NULL AND g.status='IN_PROGRESS'
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        UPDATE notification_message SET handle_status='CONTINUED',handled_at=clock_timestamp(),updated_at=clock_timestamp()
        WHERE user_id=301 AND handle_status='PENDING'
    """)
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "continue_does_not_send_again_in_same_round", """
        SELECT count(*)=1 AND bool_and(n.handle_status='CONTINUED' AND g.completion_round=1 AND g.status='IN_PROGRESS')
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE task SET status='IN_PROGRESS',completed_at=NULL WHERE name='notify_done'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    assertion(cursor, "falling_condition_preserves_round", """
        SELECT NOT completion_condition_met AND completion_round=1 FROM goal WHERE name='notify_qualified'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "requalification_generates_exactly_next_round", """
        SELECT count(*)=2 AND max(n.condition_round)=2 AND count(*) FILTER(WHERE n.handle_status='PENDING')=1
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE task SET status='IN_PROGRESS',completed_at=NULL WHERE name='notify_done'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "falling_condition_invalidates_pending_without_marking_read", """
        SELECT n.handle_status='INVALIDATED' AND n.read_at IS NULL AND n.handled_at IS NOT NULL
        FROM notification_message n JOIN goal g ON g.id=n.goal_id WHERE g.name='notify_qualified' AND n.condition_round=2
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT atomic_notice")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT atomic_notice")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT atomic_notice")
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "task_condition_and_notification_can_rollback_together", """
        SELECT t.status='IN_PROGRESS' AND NOT g.completion_condition_met AND g.completion_round=2
            AND NOT EXISTS(SELECT 1 FROM notification_message n WHERE n.goal_id=g.id AND n.condition_round=3)
        FROM task t JOIN stage s ON s.id=t.stage_id JOIN goal g ON g.id=s.goal_id WHERE t.name='notify_done'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT management_reclassification")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("UPDATE task SET status='COMPLETED',completed_at=clock_timestamp() WHERE name='notify_done'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("INSERT INTO identity_user_role(user_id,role_id) SELECT 301,id FROM identity_role WHERE code='ADMIN'")
    # 调用 initialize_facts 完成初始化facts处理。
    initialize_facts(cursor, sources)
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "management_reclassification_invalidates_personal_pending_notice", """
        SELECT NOT g.completion_condition_met AND g.completion_round=3 AND n.handle_status='INVALIDATED'
        FROM goal g JOIN notification_message n ON n.goal_id=g.id AND n.condition_round=3 WHERE g.name='notify_qualified'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT management_reclassification")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT management_reclassification")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='false'")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.remaining_backend_ready='false'")


# 函数 query_checks：调用 query_checks 完成查询条件checks处理；接收 数据库语句执行器（cursor）。
def query_checks(cursor):
    # 只执行已交付的 PREPARE 原型，不在脚本中复制另一份业务查询实现。
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 结果（result）。
    result = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    # 把执行“调用 next 完成下一个处理”得到的结果保存到 总数（total）。
    total = next(row for row in result if row["dimension"] == "TOTAL")
    # 调用 check 完成核对处理。
    check("habit_total_uses_sums_and_keeps_history", total["done_count"] == 4 and total["due_count"] == 8
          # 继续补充本次调用或判断：条件判断的结果：and total["completion_rate"] == 50)。
          and total["completion_rate"] == 50)
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 按分类（by_category）。
    by_category = {row["category_id"]: row for row in result if row["dimension"] == "CATEGORY"}
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SELECT name,id FROM habit_category WHERE user_id=101")
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 分类编号列表（category_ids）。
    category_ids = dict(cursor.fetchall())
    # 调用 check 完成核对处理。
    check("completed_habit_uses_completion_time_category",
          # 继续传入或组合条件判断的结果：by_category[category_ids["A"]]["done_count"] == 2 and by_category[category_ids["A"]]["due_count"] == 4，完成上面尚未结束的表达式。
          by_category[category_ids["A"]]["done_count"] == 2 and by_category[category_ids["A"]]["due_count"] == 4
          # 继续补充本次调用或判断：条件判断的结果：and by_category[category_ids["B"]]["done_count"] == 2 and by_category[category_ids["B"]]["due_count"] == 3。
          and by_category[category_ids["B"]]["done_count"] == 2 and by_category[category_ids["B"]]["due_count"] == 3
          # 继续补充本次调用或判断：条件判断的结果：and by_category[None]["due_count"] == 1)。
          and by_category[None]["due_count"] == 1)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 日列表（days）。
    days = rows(cursor, "EXECUTE statistics_habit_days(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    assertion(cursor, "soft_deleted_category_keeps_existing_references", """
        SELECT c.deleted_at IS NOT NULL AND count(r.id)>0 FROM habit_category c JOIN habit_revision r ON r.category_id=c.id
        WHERE c.user_id=101 AND c.name='B' GROUP BY c.id
    """)
    # 调用 check 完成核对处理。
    check("only_valid_checkins_count_and_no_future_start",
          # 继续传入或组合集合或文字的长度，完成上面尚未结束的表达式。
          len(days) == 8 and sum(row["done_count"] for row in days) == 4)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 空状态（empty）。
    empty = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (99999, "2026-11-02", "2026-11-04"))
    # 调用 check 完成核对处理。
    check("zero_denominator_returns_null", len(empty) == 1 and empty[0]["due_count"] == 0
          # 继续补充本次调用或判断：条件判断的结果：and empty[0]["done_count"] == 0 and empty[0]["completion_rate"] is None)。
          and empty[0]["done_count"] == 0 and empty[0]["completion_rate"] is None)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 own。
    own = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (202, "2026-11-02", "2026-11-04"))
    # 把执行“调用 next 完成下一个处理”得到的结果保存到 other总数（other_total）。
    other_total = next(row for row in own if row["dimension"] == "TOTAL")
    # 调用 check 完成核对处理。
    check("habit_statistics_isolate_users", other_total["done_count"] == 0 and other_total["due_count"] == 3)

    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT category_name_reuse")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("INSERT INTO habit_category(user_id,name) VALUES(101,'B')")
    # 断言 ion。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    assertion(cursor, "deleted_category_name_can_be_reused_without_rewriting_references", """
        SELECT count(*)=2 AND count(*) FILTER(WHERE deleted_at IS NULL)=1
        FROM habit_category WHERE user_id=101 AND name='B'
    """)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT category_name_reuse")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT category_name_reuse")

    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT cancel_paused_today")
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    cursor.execute("""UPDATE habit_checkin SET cancelled_at='2026-11-04 10:00Z',updated_at=clock_timestamp()
        WHERE habit_id=(SELECT id FROM habit WHERE user_id=101 AND id IN(
            SELECT habit_id FROM habit_revision WHERE name='reading')) AND business_date='2026-11-04' AND cancelled_at IS NULL""")
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 canceled。
    canceled = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    # 把执行“调用 next 完成下一个处理”得到的结果保存到 总数canceled（total_canceled）。
    total_canceled = next(row for row in canceled if row["dimension"] == "TOTAL")
    # 调用 check 完成核对处理。
    check("cancel_then_paused_rule_recomputes_today_without_rewriting_past",
          # 继续传入或组合条件判断的结果：total_canceled["done_count"] == 3 and total_canceled["due_count"] == 7)，完成上面尚未结束的表达式。
          total_canceled["done_count"] == 3 and total_canceled["due_count"] == 7)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT cancel_paused_today")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT cancel_paused_today")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT ended_and_deleted_habit")
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：向 habit_revision 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：继续列出本次使用的字段：状态确认时的数据快照（status_snapshot）、生效日期（effective_date）、已修改at（changed_at）。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'ENDED','2026-11-04','2026-11-04 04:00Z',r.time_zone FROM habit_revision r。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,
            status_snapshot,effective_date,changed_at,time_zone)
        SELECT r.user_id,r.habit_id,5,'ENDED',r.name,r.category_id,r.cycle_type,r.weekday_mask,r.start_date,
            'ENDED','2026-11-04','2026-11-04 04:00Z',r.time_zone FROM habit_revision r
        WHERE r.name='reading' AND r.revision_no=4;
        UPDATE habit SET status='ENDED',ended_at='2026-11-04 04:00Z',current_revision_no=5
        WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='reading')
    """)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 已结束（ended）。
    ended = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    # 调用 check 完成核对处理。
    check("completed_today_remains_one_of_one_after_ending", ended == result)
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：向 habit_revision 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：继续列出本次使用的字段：状态确认时的数据快照（status_snapshot）、删除标记确认时的数据快照（deleted_snapshot）、生效日期（effective_date）、已修改at（changed_at）。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：继续列出本次使用的字段：成立（true）。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""
        INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,category_id,cycle_type,weekday_mask,start_date,
            status_snapshot,deleted_snapshot,effective_date,changed_at,time_zone)
        SELECT r.user_id,r.habit_id,6,'DELETED',r.name,r.category_id,r.cycle_type,r.weekday_mask,r.start_date,
            'ENDED',true,'2026-11-04','2026-11-04 05:00Z',r.time_zone FROM habit_revision r
        WHERE r.name='reading' AND r.revision_no=5;
        UPDATE habit SET deleted_at='2026-11-04 05:00Z',current_revision_no=6
        WHERE id=(SELECT habit_id FROM review_habit_key WHERE name='reading')
    """)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 删除标记（deleted）。
    deleted = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)", (101, "2026-11-02", "2026-11-04"))
    # 调用 check 完成核对处理。
    check("completed_today_and_past_survive_habit_deletion", deleted == result)
    # 执行传入的数据库语句并取得结果。
    cursor.execute("ROLLBACK TO SAVEPOINT ended_and_deleted_habit")
    # 执行传入的数据库语句并取得结果。
    cursor.execute("RELEASE SAVEPOINT ended_and_deleted_habit")

    # 把按字段名保存多个值的字典保存到 预计periods（expected_periods）。
    expected_periods = {"TOTAL": ("2026-11-02", "2026-11-04"), "WEEK": ("2026-11-02", "2026-11-04"),
                        # 填写月（MONTH）字段，值为表达式 ("2026-11-01", "2026-11-04"), "YEAR": ("2026-01-01", "2026-11-04") 的结果。
                        "MONTH": ("2026-11-01", "2026-11-04"), "YEAR": ("2026-01-01", "2026-11-04"),
                        # 填写区间（RANGE）字段，值为表达式 ("2026-11-03", "2026-11-04")} 的结果。
                        "RANGE": ("2026-11-03", "2026-11-04")}
    # 从执行“调用 expected_periods.items 完成数据列表处理”得到的结果依次取出period预计（(period, expected)），每项执行一次下面的处理。
    for period, expected in expected_periods.items():
        # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 bounds。
        bounds = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                      # 继续传入或组合表达式 (101, period, None, "2026-11-03", "2026-11-04", "2026-11-04")) 的结果，完成上面尚未结束的表达式。
                      (101, period, None, "2026-11-03", "2026-11-04", "2026-11-04"))
        # 调用 check 完成核对处理。
        check("habit_period_" + period.lower(), len(bounds) == 1
              # 继续补充本次调用或判断：条件判断的结果：and (str(bounds[0]["date_from"]), str(bounds[0]["date_to"])) == expected)。
              and (str(bounds[0]["date_from"]), str(bounds[0]["date_to"])) == expected)
        # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 stats。
        stats = rows(cursor, "EXECUTE statistics_habit_summary(%s,%s,%s)",
                     # 继续传入或组合表达式 (101, bounds[0]["date_from"], bounds[0]["date_to"])) 的结果，完成上面尚未结束的表达式。
                     (101, bounds[0]["date_from"], bounds[0]["date_to"]))
        # 把执行“调用 next 完成下一个处理”得到的结果保存到 period总数（period_total）。
        period_total = next(row for row in stats if row["dimension"] == "TOTAL")
        # 把条件 period == 'RANGE' 是否成立时使用依次包含 固定数值 3、固定数值 5 的集合，否则使用依次包含 固定数值 4、固定数值 8 的集合保存到 预计counts（expected_counts）。
        expected_counts = (3, 5) if period == "RANGE" else (4, 8)
        # 调用 check 完成核对处理。
        check("habit_period_statistics_" + period.lower(),
              # 继续传入或组合条件判断的结果：(period_total["done_count"], period_total["due_count"]) == expected_counts)，完成上面尚未结束的表达式。
              (period_total["done_count"], period_total["due_count"]) == expected_counts)
    # 从依次包含 依次包含 固定文字 'future_range'、固定状态或编码 RANGE（区间）、空值，表示当前还没有对应内容、固定文字 '2026-11-04'、固定文字 '2026-11-05' 的集合、依次包含 固定文字 'reversed_range'、固定状态或编码 RANGE（区间）、空值，表示当前还没有对应内容、固定文字 '2026-11-04'、固定文字 '2026-11-03' 的集合、依次包含 固定文字 'unknown_period'、固定状态或编码 OTHER（OTHER）、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容 的集合、依次包含 固定文字 'future_anchor'、固定状态或编码 MONTH（月）、固定文字 '2026-12-01'、空值，表示当前还没有对应内容、空值，表示当前还没有对应内容 的集合 的集合依次取出名称periodanchor开始结束（(name, period, anchor, start, end)），每项执行一次下面的处理。
    for name, period, anchor, start, end in (
        # 继续传入或组合表达式 ("future_range", "RANGE", None, "2026-11-04", "2026-11-05") 的结果，完成上面尚未结束的表达式。
        ("future_range", "RANGE", None, "2026-11-04", "2026-11-05"),
        # 继续传入或组合表达式 ("reversed_range", "RANGE", None, "2026-11-04", "2026-11-03") 的结果，完成上面尚未结束的表达式。
        ("reversed_range", "RANGE", None, "2026-11-04", "2026-11-03"),
        # 继续传入或组合表达式 ("unknown_period", "OTHER", None, None, None) 的结果，完成上面尚未结束的表达式。
        ("unknown_period", "OTHER", None, None, None),
        # 继续传入或组合表达式 ("future_anchor", "MONTH", "2026-12-01", None, None) 的结果，完成上面尚未结束的表达式。
        ("future_anchor", "MONTH", "2026-12-01", None, None),
    ):
        # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 不合法（invalid）。
        invalid = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                       # 继续传入或组合表达式 (101, period, anchor, start, end, "2026-11-04")) 的结果，完成上面尚未结束的表达式。
                       (101, period, anchor, start, end, "2026-11-04"))
        # 调用 check 完成核对处理。
        check("invalid_" + name + "_has_no_bounds", not invalid)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 bounds。
    bounds = rows(cursor, "EXECUTE statistics_habit_period(%s,%s,%s,%s,%s,%s)",
                  # 继续传入或组合表达式 (101, "WEEK", "2027-01-01", None, None, "2027-01-02")) 的结果，完成上面尚未结束的表达式。
                  (101, "WEEK", "2027-01-01", None, None, "2027-01-02"))
    # 调用 check 完成核对处理。
    check("iso_week_crosses_calendar_year", str(bounds[0]["date_from"]) == "2026-12-28"
          # 继续补充本次调用或判断：条件判断的结果：and str(bounds[0]["date_to"]) == "2027-01-02")。
          and str(bounds[0]["date_to"]) == "2027-01-02")

    # 把从执行“调用 rows 完成记录列表处理”得到的结果中按固定数值 0取出的内容保存到 totals。
    totals = rows(cursor, "EXECUTE statistics_execution_totals(%s,%s,%s,%s)",
                  # 继续传入或组合表达式 (501, "2026-11-03 16:00Z", "2026-11-04 16:00Z", "2026-11-04 02:05Z"))[0] 的结果，完成上面尚未结束的表达式。
                  (501, "2026-11-03 16:00Z", "2026-11-04 16:00Z", "2026-11-04 02:05Z"))[0]
    # 调用 check 完成核对处理。
    check("execution_aggregates_milliseconds_before_seconds", totals["total_actual_milliseconds"] == 161300
          # 继续补充本次调用或判断：条件判断的结果：and totals["range_actual_milliseconds"] == 160700 and totals["total_actual_seconds"] == 161。
          and totals["range_actual_milliseconds"] == 160700 and totals["total_actual_seconds"] == 161
          # 继续补充本次调用或判断：条件判断的结果：and totals["range_actual_seconds"] == 160)。
          and totals["range_actual_seconds"] == 160)
    # 调用 check 完成核对处理。
    check("execution_session_counts_are_not_segment_counts", totals["total_sessions"] == 3
          # 继续补充本次调用或判断：条件判断的结果：and totals["range_sessions"] == 3 and totals["total_finished_sessions"] == 2)。
          and totals["range_sessions"] == 3 and totals["total_finished_sessions"] == 2)
    # 调用 check 完成核对处理。
    check("deleted_task_execution_facts_still_contribute", totals["range_actual_milliseconds"] == 160700)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 每日（daily）。
    daily = rows(cursor, "EXECUTE statistics_execution_days(%s,%s,%s,%s,%s)",
                 # 继续传入或组合按表达式计算得到的值：(501, "2026-11-03", "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))，完成上面尚未结束的表达式。
                 (501, "2026-11-03", "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))
    # 调用 check 完成核对处理。
    check("execution_cross_midnight_and_timeout_cap", [row["actual_milliseconds"] for row in daily] == [600,160700])
    # 从依次包含 依次包含 固定文字 'spring'、固定文字 '2026-03-08'、固定数值 82800 的集合、依次包含 固定文字 'autumn'、固定文字 '2026-11-01'、固定数值 90000 的集合 的集合依次取出名称日期值预计秒数（(name, date_value, expected_seconds)），每项执行一次下面的处理。
    for name, date_value, expected_seconds in (("spring", "2026-03-08", 82800), ("autumn", "2026-11-01", 90000)):
        # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 日（day）。
        day = rows(cursor, "EXECUTE statistics_execution_days(%s,%s,%s,%s,%s)",
                   # 继续传入或组合按表达式计算得到的值：(503, date_value, date_value, "America/New_York", "2026-11-04 02:05Z"))，完成上面尚未结束的表达式。
                   (503, date_value, date_value, "America/New_York", "2026-11-04 02:05Z"))
        # 调用 check 完成核对处理。
        check("dst_" + name + "_day_length", day[0]["actual_seconds"] == expected_seconds)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 states。
    states = rows(cursor, "EXECUTE statistics_state_days(%s,%s,%s,%s)",
                  # 继续传入或组合按表达式计算得到的值：(401, "2026-11-03", "2026-11-04", "Asia/Shanghai"))，完成上面尚未结束的表达式。
                  (401, "2026-11-03", "2026-11-04", "Asia/Shanghai"))
    # 调用 check 完成核对处理。
    check("historical_completions_not_inferred_from_current_status",
          # 继续传入或组合条件判断的结果：states[0]["task_completion_actions"] == 2 and states[0]["completed_distinct_tasks"] == 1，完成上面尚未结束的表达式。
          states[0]["task_completion_actions"] == 2 and states[0]["completed_distinct_tasks"] == 1
          # 继续补充本次调用或判断：条件判断的结果：and states[1]["task_completion_actions"] == 0)。
          and states[1]["task_completion_actions"] == 0)
    # 调用 check 完成核对处理。
    check("goal_archive_restore_are_real_events", states[0]["goal_archive_actions"] == 1
          # 继续补充本次调用或判断：条件判断的结果：and states[1]["goal_restore_actions"] == 1)。
          and states[1]["goal_restore_actions"] == 1)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 成长时间线（timeline）。
    timeline = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                    # 继续传入或组合表达式 (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 100, 0)) 的结果，完成上面尚未结束的表达式。
                    (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 100, 0))
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 首次（first）。
    first = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                 # 继续传入或组合表达式 (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 0)) 的结果，完成上面尚未结束的表达式。
                 (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 0))
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 秒（second）。
    second = rows(cursor, "EXECUTE statistics_timeline(%s,%s,%s,%s,%s)",
                  # 继续传入或组合表达式 (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 2)) 的结果，完成上面尚未结束的表达式。
                  (401, "2026-11-02 16:00Z", "2026-11-04 16:00Z", 2, 2))
    # 调用 check 完成核对处理。
    check("timeline_stable_pagination_and_user_filter",
          # 继续传入或组合集合或文字的长度，完成上面尚未结束的表达式。
          len([event for event in timeline if event["event_type"] != "BASELINE"]) == 8
          # 继续补充本次调用或判断：条件判断的结果：and first + second == timeline[:4])。
          and first + second == timeline[:4])

    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 dashboard。
    dashboard = rows(cursor, "EXECUTE dashboard_habits(%s,%s)", (101, "2026-11-04"))
    # 调用 check 完成核对处理。
    check("dashboard_today_keeps_completed_paused_habit",
          # 继续传入或组合集合或文字的长度，完成上面尚未结束的表达式。
          len(dashboard) == 3 and sum(row["done_count"] for row in dashboard) == 1)
    # 把从执行“调用 rows 完成记录列表处理”得到的结果中按固定数值 0取出的内容保存到 overview。
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    # 继续传入或组合按表达式计算得到的值：(101, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]，完成上面尚未结束的表达式。
                    (101, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    # 调用 check 完成核对处理。
    check("dashboard_habit_counts_match_statistics", overview["habit_done"] == 1 and overview["habit_due"] == 3)
    # 把从执行“调用 rows 完成记录列表处理”得到的结果中按固定数值 0取出的内容保存到 overview。
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    # 继续传入或组合按表达式计算得到的值：(501, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]，完成上面尚未结束的表达式。
                    (501, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    # 调用 check 完成核对处理。
    check("dashboard_actual_time_matches_clipped_statistics", overview["actual_seconds"] == 160)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 方案记录列表（plan_rows）。
    plan_rows = rows(cursor, "EXECUTE dashboard_tasks(%s,%s,%s,%s)", (601, "2026-11-04", 100, 0))
    # 调用 check 完成核对处理。
    check("dashboard_separates_today_and_week_undated",
          # 继续传入或组合集合或文字的长度，完成上面尚未结束的表达式。
          len(plan_rows) == 2 and {row["section"] for row in plan_rows} == {"TODAY","WEEK_UNDATED"})
    # 把从执行“调用 rows 完成记录列表处理”得到的结果中按固定数值 0取出的内容保存到 overview。
    overview = rows(cursor, "EXECUTE dashboard_overview(%s,%s,%s,%s)",
                    # 继续传入或组合按表达式计算得到的值：(601, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]，完成上面尚未结束的表达式。
                    (601, "2026-11-04", "Asia/Shanghai", "2026-11-04 02:05Z"))[0]
    # 调用 check 完成核对处理。
    check("dashboard_counts_do_not_multiply_with_fact_joins",
          # 继续传入或组合条件判断的结果：overview["today_tasks"] == 1 and overview["today_current_completed_tasks"] == 1，完成上面尚未结束的表达式。
          overview["today_tasks"] == 1 and overview["today_current_completed_tasks"] == 1
          # 继续补充本次调用或判断：条件判断的结果：and overview["week_undated_tasks"] == 1)。
          and overview["week_undated_tasks"] == 1)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 目标列表（goals）。
    goals = rows(cursor, "EXECUTE dashboard_goals(%s,%s,%s)", (601, 20, 0))
    # 调用 check 完成核对处理。
    check("dashboard_weighted_goal_progress", len(goals) == 1 and goals[0]["task_count"] == 3
          # 继续补充本次调用或判断：条件判断的结果：and goals[0]["completed_tasks"] == 1 and goals[0]["current_progress"] == 20)。
          and goals[0]["completed_tasks"] == 1 and goals[0]["current_progress"] == 20)
    # 把执行“调用 rows 完成记录列表处理”得到的结果保存到 objects。
    objects = rows(cursor, "EXECUTE statistics_objects(%s)", (601,))
    # 调用 check 完成核对处理。
    check("current_object_summary_is_distinct_from_history", sum(row["object_count"] for row in objects if row["kind"] == "TASK") == 3)


# 函数 upgrade_review：调用 upgrade_review 完成upgrade复盘处理；接收 sources、预计shape（expected_shape）。
def upgrade_review(sources, expected_shape):
    # 函数 run：调用 run 完成方案运行处理；接收 数据库语句执行器（cursor）。
    def run(cursor):
        # 从依次包含 固定文字 'identity'、固定文字 'goal'、固定文字 'stage'、固定文字 'task'、固定文字 'planning'等内容 的集合依次取出module，每项执行一次下面的处理。
        for module in ("identity", "goal", "stage", "task", "planning", "execution"):
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(old_source(module, sources[module])))
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        assertion(cursor, "existing_baseline_has_18_tables", """
            SELECT count(*)=18 FROM information_schema.tables WHERE table_schema=current_schema() AND table_type='BASE TABLE'
        """)
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL asterway.execution_backend_ready='true'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute(execution_review.authorization_block(sources["identity"]))
        # 执行传入的数据库语句并取得结果。
        # 内嵌 SQL：向 identity_user 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        # 内嵌 SQL：向 identity_user_role 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：向 goal 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：向 stage 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：向 task 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
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
        # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 变化前（before）。
        before = cursor.fetchone()[0]
        # 执行传入的数据库语句并取得结果。
        # 内嵌 SQL：向 weekly_plan 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：向 daily_plan 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：向 task_assignment 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：指定本次查询主要读取的表或子查询。
        # 内嵌 SQL：向 execution_session 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：向 execution_segment 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：指定本次查询主要读取的表或子查询。
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
        # 把表达式 "SELECT jsonb_build_object(\n 'u',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_user x),\n 's',(SELECT jsonb_… 的结果保存到 preserved查询条件（preserved_query）。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'u',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_user x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'s',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM stage x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'t',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM task x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'w',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM weekly_plan x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'d',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM daily_plan x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'a',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM task_assignment x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'es',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM execution_session x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'sg',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM execution_segment x),。
        # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：'ep',(SELECT jsonb_agg(to_jsonb(x) ORDER BY id) FROM identity_permission x WHERE code LIKE 'execution:%'),。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
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
        # 执行传入的数据库语句并取得结果。
        cursor.execute(preserved_query)
        # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 preserved。
        preserved = cursor.fetchone()[0]
        # 从依次包含 固定文字 'goal'、固定文字 'stage'、固定文字 'task' 的集合依次取出module，每项执行一次下面的处理。
        for module in ("goal", "stage", "task"):
            # 从从MARKERS中按module取出的内容依次取出标记（marker），每项执行一次下面的处理。
            for marker in MARKERS[module]:
                # 执行传入的数据库语句并取得结果。
                cursor.execute(block(sources[module], marker))
        # 从依次包含 固定文字 'habit'、固定文字 'notification' 的集合依次取出module，每项执行一次下面的处理。
        for module in ("habit", "notification"):
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(sources[module]))
        # 执行传入的数据库语句并取得结果。
        cursor.execute(block(sources["identity"], "REMAINING AUTHORIZATION"))
        # 调用 check 完成核对处理。
        check("upgrade_and_fresh_database_have_identical_schema", fingerprint(cursor) == expected_shape)
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT jsonb_agg(to_jsonb(g)-'completion_condition_met'-'completion_round' ORDER BY id) FROM goal g")
        # 调用 check 完成核对处理。
        check("existing_goal_values_and_versions_preserved", cursor.fetchone()[0] == before)
        # 执行传入的数据库语句并取得结果。
        cursor.execute(preserved_query)
        # 调用 check 完成核对处理。
        check("upgrade_preserves_accounts_planning_execution_and_existing_authorization", cursor.fetchone()[0] == preserved)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：指定本次查询主要读取的表或子查询。
        assertion(cursor, "existing_task_row_preserved", """
            SELECT count(*)=1 AND bool_and(name='preserve_old_task' AND completed_at='2026-11-03 01:00Z')
            FROM task
        """)
        # 执行传入的数据库语句并取得结果。
        cursor.execute(block(sources["goal"], "GOAL CONDITION UPGRADE"))
        # 执行传入的数据库语句并取得结果。
        cursor.execute(block(sources["identity"], "REMAINING AUTHORIZATION"))
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        assertion(cursor, "upgrade_preparation_replay_writes_no_business_facts", """
            SELECT NOT EXISTS(SELECT 1 FROM goal_state_history) AND NOT EXISTS(SELECT 1 FROM task_state_history)
                AND NOT EXISTS(SELECT 1 FROM notification_message)
        """)
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL asterway.remaining_backend_ready='true'")
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SET LOCAL asterway.remaining_history_bootstrap='true'")
        # 调用 initialize_facts 完成初始化facts处理。
        initialize_facts(cursor, sources)
        # 调用 initialize_facts 完成初始化facts处理。
        initialize_facts(cursor, sources)
        # 断言 ion。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        assertion(cursor, "old_database_baseline_and_known_completion_are_idempotent", """
            SELECT (SELECT count(*)=1 FROM goal_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM stage_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM task_state_history WHERE event_type='BASELINE')
                AND (SELECT count(*)=1 FROM task_state_history WHERE event_type='COMPLETION_IMPORTED')
        """)
    # 调用 run_isolated 完成方案运行isolated处理。
    run_isolated("asterway_remaining_upgrade_", run)


# 函数 legacy_review：调用 legacy_review 完成legacy复盘处理；接收 sources、module。
def legacy_review(sources, module):
    # 保留历史校验文件；只适配因本次增加三张历史表而变化的表总数断言，其余原样执行。
    # 检查条件 module == 'growth' 是否成立；成立时执行缩进的分支。
    if module == "growth":
        # 把依次包含 固定文字 'goal'、固定文字 'stage'、固定文字 'task'、固定文字 'planning' 的集合保存到 modules。
        modules = ("goal", "stage", "task", "planning")
        # 把按 ROOT / 'docs/growth/SQL层校验.sql' 计算的结果保存到 核对路径（check_path）。
        check_path = ROOT / "docs/growth/SQL层校验.sql"
        # 把依次包含 固定文字 'count(*)=6'、固定文字 'count(*)=9'、固定文字 'validation_result' 的集合保存到 旧数量新数量结果表（(old_count, new_count, result_table)）。
        old_count, new_count, result_table = "count(*)=6", "count(*)=9", "validation_result"
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 把依次包含 固定文字 'identity'、固定文字 'goal'、固定文字 'stage'、固定文字 'task'、固定文字 'planning'等内容 的集合保存到 modules。
        modules = ("identity", "goal", "stage", "task", "planning", "execution")
        # 把按 ROOT / 'docs/execution/SQL层校验.sql' 计算的结果保存到 核对路径（check_path）。
        check_path = ROOT / "docs/execution/SQL层校验.sql"
        # 把依次包含 固定文字 'count(*) = 18'、固定文字 'count(*) = 21'、固定文字 'execution_validation_result' 的集合保存到 旧数量新数量结果表（(old_count, new_count, result_table)）。
        old_count, new_count, result_table = "count(*) = 18", "count(*) = 21", "execution_validation_result"
    # 函数 run：调用 run 完成方案运行处理；接收 数据库语句执行器（cursor）。
    def run(cursor):
        # 从modules依次取出名称（name），每项执行一次下面的处理。
        for name in modules:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(sources[name]))
        # 把执行“按指定编码读取文件文字”得到的结果保存到 contents。
        contents = check_path.read_text(encoding="utf-8-sig")
        # 检查条件 contents.count(old_count) != 1 是否成立；成立时执行缩进的分支。
        if contents.count(old_count) != 1:
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("Legacy table-count assertion has changed; review adapter before running")
        # 执行传入的数据库语句并取得结果。
        cursor.execute(contents.replace(old_count, new_count, 1))
        # 检查条件 module == 'execution' 是否成立；成立时执行缩进的分支。
        if module == "execution":
            # 调用 execution_review.authorization_checks 完成授权checks处理。
            execution_review.authorization_checks(cursor, execution_review.authorization_block(sources["identity"]))
            # 读取 仅查询条件checks（only_query_checks）。
            execution_review.readonly_query_checks(cursor)
        # 执行传入的数据库语句并取得结果。
        cursor.execute(sql.SQL("SELECT name FROM {} WHERE passed ORDER BY name").format(sql.Identifier(result_table)))
        # 从执行“取出查询结果中的所有行”得到的结果依次取出当前记录（row），每项执行一次下面的处理。
        for row in cursor.fetchall():
            # 调用 check 完成核对处理。
            check("legacy_" + module + "_" + row[0], True)
    # 调用 run_isolated 完成方案运行isolated处理。
    run_isolated("asterway_" + module + "_review_", run)


# 函数 concurrent_review：调用 concurrent_review 完成concurrent复盘处理；接收 sources。
def concurrent_review(sources):
    # 把按 'asterway_remaining_race_' + uuid.uuid4().hex 计算的结果保存到 数据库分区（schema）。
    schema = "asterway_remaining_race_" + uuid.uuid4().hex
    # 检查条件 not re.fullmatch('asterway_remaining_race_[0-9a-f]{32}', schema) 是否成立；成立时执行缩进的分支。
    if not re.fullmatch(r"asterway_remaining_race_[0-9a-f]{32}", schema):
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Invalid isolated concurrent schema")
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = connect()
    # 把假，表示条件不成立或开关关闭保存到 创建时（created）。
    created = False
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(schema)))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(sources["habit"]))
            # 通知默认初始化块为 no-op，依赖表无需为本组唯一索引竞争重复建立。
            # 把执行“调用 re.sub 完成sub处理”得到的结果保存到 通知（notification）。
            notification = re.sub(r"-- BEGIN REMAINING NOTIFICATION BOOTSTRAP.*?-- END REMAINING NOTIFICATION BOOTSTRAP",
                                  # 继续传入或组合表达式 "", sources["notification"], flags=re.DOTALL) 的结果，完成上面尚未结束的表达式。
                                  "", sources["notification"], flags=re.DOTALL)
            # 执行传入的数据库语句并取得结果。
            cursor.execute(body(notification))
            # 执行传入的数据库语句并取得结果。
            cursor.execute(block(sources["task"], "TASK STATE HISTORY"))
        # 提交本次数据库事务，使本次修改一起生效。
        connection.commit()
        # 把真，表示条件成立或开关打开保存到 创建时（created）。
        created = True
        # 把执行计时复盘（execution_review）的唯一race（unique_race）属性保存到 race。
        race = execution_review.unique_race
        # 把固定文字 'INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,c…'保存到 statement。
        # 内嵌 SQL：向 habit_checkin 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        statement = """INSERT INTO habit_checkin(user_id,habit_id,revision_id,business_date,time_zone,checked_at)
            VALUES(901,1,1,'2026-11-04','Asia/Shanghai','2026-11-04 01:00Z')"""
        # 调用 race 完成race处理。
        race(connection, schema, statement, statement, "uk_habit_checkin_valid_date")
        # 调用 check 完成核对处理。
        check("concurrent_one_valid_checkin_per_habit_date", True)
        # 调用 race 完成race处理。
        race(connection, schema,
             # 继续传入或组合固定文字 "INSERT INTO habit_category(user_id,name) VALUES(901,'Reading')"，完成上面尚未结束的表达式。
             "INSERT INTO habit_category(user_id,name) VALUES(901,'Reading')",
             # 继续传入或组合固定文字 "INSERT INTO habit_category(user_id,name) VALUES(901,'reading')"，完成上面尚未结束的表达式。
             "INSERT INTO habit_category(user_id,name) VALUES(901,'reading')",
             # 继续传入或组合表达式 "uk_habit_category_user_name_active") 的结果，完成上面尚未结束的表达式。
             "uk_habit_category_user_name_active")
        # 调用 check 完成核对处理。
        check("concurrent_category_case_insensitive_uniqueness", True)
        # 把表达式 "INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,\n start_date,status_s… 的结果保存到 修订版本（revision）。
        # 内嵌 SQL：向 habit_revision 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：开始日期（start_date）、状态确认时的数据快照（status_snapshot）、生效日期（effective_date）、已修改at（changed_at）。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        revision = """INSERT INTO habit_revision(user_id,habit_id,revision_no,action_type,name,cycle_type,weekday_mask,
            start_date,status_snapshot,effective_date,changed_at,time_zone)
            VALUES(901,2,1,'CREATED','race','DAILY',127,'2026-11-04','ACTIVE','2026-11-04','2026-11-04 01:00Z','UTC')"""
        # 调用 race 完成race处理。
        race(connection, schema, revision, revision, "uk_habit_revision_number")
        # 调用 check 完成核对处理。
        check("concurrent_revision_number_uniqueness", True)
        # 把固定文字 'INSERT INTO notification_message(user_id,goal_id,condition_round,title,content)\\…'保存到 达标提醒（notice）。
        # 内嵌 SQL：向 notification_message 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        notice = """INSERT INTO notification_message(user_id,goal_id,condition_round,title,content)
            VALUES(901,1,1,'race','concurrent goal round')"""
        # 调用 race 完成race处理。
        race(connection, schema, notice, notice, "uk_notification_goal_round")
        # 调用 check 完成核对处理。
        check("concurrent_goal_round_delivers_once", True)
        # 把表达式 "INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,\n entity_version,oc… 的结果保存到 历史（history）。
        # 内嵌 SQL：向 task_state_history 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：版本号（entity_version）、1、1。
        history = """INSERT INTO task_state_history(user_id,task_id,event_type,before_status,after_status,name_snapshot,
            entity_version,occurred_at) VALUES(901,1,'STATUS_CHANGED','NOT_STARTED','IN_PROGRESS','race',1,'2026-11-04 01:00Z')"""
        # 调用 race 完成race处理。
        race(connection, schema, history, history, "uk_task_state_history_event")
        # 调用 check 完成核对处理。
        check("concurrent_same_state_event_is_unique", True)
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
                # 检查条件 cursor.fetchone()[0] != DATABASE 是否成立；成立时执行缩进的分支。
                if cursor.fetchone()[0] != DATABASE:
                    # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                    raise RuntimeError("Refusing schema cleanup outside dedicated SQL review database")
                # 执行传入的数据库语句并取得结果。
                cursor.execute(sql.SQL("DROP SCHEMA {} CASCADE").format(sql.Identifier(schema)))
            # 提交本次数据库事务，使本次修改一起生效。
            connection.commit()
            # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
            with connection.cursor() as cursor:
                # 断言 ion。
                assertion(cursor, "remaining_concurrent_schema_removed",
                          # 继续传入或组合表达式 "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,)) 的结果，完成上面尚未结束的表达式。
                          "SELECT NOT EXISTS(SELECT 1 FROM pg_namespace WHERE nspname=%s)", (schema,))
        # 释放连接、文件或客户端占用的资源。
        connection.close()


# 函数 main：调用 main 完成程序入口处理。
def main():
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 paths。
    paths = [SQL_ROOT / (module + ".sql") for module in MODULES + ("dashboard", "statistics")]
    # 把 paths 加上 依次包含 执行“调用 Path 完成路径处理”得到的结果、按 ROOT / 'scripts/review_remaining_sql.ps1' 计算的结果、按 REPORT_ROOT / 'SQL层校验.sql' 计算的结果、按 ROOT / 'scripts/review_execution_sql.py' 计算的结果、按 ROOT / 'docs/growth/SQL层校验.sql' 计算的结果等内容 的集合，再保存回原变量。
    paths += [Path(__file__), ROOT / "scripts/review_remaining_sql.ps1", REPORT_ROOT / "SQL层校验.sql",
              # 继续传入或组合按表达式计算得到的值：ROOT / "scripts/review_execution_sql.py", ROOT / "docs/growth/SQL层校验.sql"，完成上面尚未结束的表达式。
              ROOT / "scripts/review_execution_sql.py", ROOT / "docs/growth/SQL层校验.sql",
              # 继续传入或组合按表达式计算得到的值：ROOT / "docs/execution/SQL层校验.sql", ROOT / "docs/execution/SQL查询与事务约定.md"]，完成上面尚未结束的表达式。
              ROOT / "docs/execution/SQL层校验.sql", ROOT / "docs/execution/SQL查询与事务约定.md"]
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 hashes。
    hashes = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 sources。
    sources = {module: (SQL_ROOT / (module + ".sql")).read_text(encoding="utf-8-sig")
               # 继续传入或组合按表达式计算得到的值：for module in MODULES + ("dashboard", "statistics")}，完成上面尚未结束的表达式。
               for module in MODULES + ("dashboard", "statistics")}
    # 把空列表或集合保存到 分组列表（groups）。
    groups = []
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 把执行“调用 fresh_review 完成fresh复盘处理”得到的结果保存到 shape版本号（(shape, version)）。
    shape, version = fresh_review(sources)
    # 把新条目放到列表末尾。
    groups.append({"name": "new_schema_constraints_seeds_bootstrap_queries", "passed": len(CASES)-before})
    # 调用 print 完成print处理。
    print("Fresh schema, constraints, initialization, permissions and query arithmetic passed.", flush=True)
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 调用 upgrade_review 完成upgrade复盘处理。
    upgrade_review(sources, shape)
    # 把新条目放到列表末尾。
    groups.append({"name": "existing_18_to_26_additive_upgrade", "passed": len(CASES)-before})
    # 调用 print 完成print处理。
    print("Existing 18-table database additive upgrade passed.", flush=True)
    # 从依次包含 固定文字 'growth'、固定文字 'execution' 的集合依次取出module，每项执行一次下面的处理。
    for module in ("growth", "execution"):
        # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
        before = len(CASES)
        # 调用 legacy_review 完成legacy复盘处理。
        legacy_review(sources, module)
        # 把新条目放到列表末尾。
        groups.append({"name": "legacy_" + module + "_sql_regression", "passed": len(CASES)-before})
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 从执行“调用 execution_review.concurrent_review 完成concurrent复盘处理”得到的结果依次取出名称（name），每项执行一次下面的处理。
    for name in execution_review.concurrent_review(sources["execution"]):
        # 调用 check 完成核对处理。
        check("legacy_" + name, True)
    # 把新条目放到列表末尾。
    groups.append({"name": "execution_index_concurrency", "passed": len(CASES)-before})
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 调用 concurrent_review 完成concurrent复盘处理。
    concurrent_review(sources)
    # 把新条目放到列表末尾。
    groups.append({"name": "remaining_index_concurrency", "passed": len(CASES)-before})
    # 从执行“调用 hashes.items 完成数据列表处理”得到的结果依次取出路径digest（(path, digest)），每项执行一次下面的处理。
    for path, digest in hashes.items():
        # 检查条件 hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest 是否成立；成立时执行缩进的分支。
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest:
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("SQL inputs changed during review; rerun with the final files")
    # 把按字段名保存多个值的字典保存到 复盘报告（report）。
    report = {
        # 填写状态（status）字段，值为当前真实时间，用于记录操作发生的时刻。
        "status": "PASSED", "verifiedAt": datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(),
        # 填写数据库（database）字段，值为表达式 DATABASE, "postgresqlVersion": version, "businessTablesBefore": 18, "businessTablesAfter": 26 的结果。
        "database": DATABASE, "postgresqlVersion": version, "businessTablesBefore": 18, "businessTablesAfter": 26,
        # 填写新Tables（newTables）字段，值为调用 list 完成列表处理。
        "newTables": list(NEW_TABLES), "checks": len(CASES), "passed": len(CASES),
        # 填写数据范围（scope）字段，值为固定文字 'SQL DDL, constraints, additive old-schema upgrade, staged seeds, initialization,…'。
        "scope": "SQL DDL, constraints, additive old-schema upgrade, staged seeds, initialization, read-query arithmetic, database uniqueness races and prior SQL regressions",
        # 填写strictTestFrameworkUsed字段，值为表达式 False, "serviceOrHttpAcceptance": "NOT_VERIFIED" 的结果。
        "strictTestFrameworkUsed": False, "serviceOrHttpAcceptance": "NOT_VERIFIED",
        # 填写development数据库Connected（developmentDatabaseConnected）字段，值为表达式 False, "developmentDatabaseChanged": False 的结果。
        "developmentDatabaseConnected": False, "developmentDatabaseChanged": False,
        # 填写rollbackSchemasRemoved字段，值为表达式 True, "concurrentSchemasRemoved": True 的结果。
        "rollbackSchemasRemoved": True, "concurrentSchemasRemoved": True,
        # 填写concurrentCommits仅InsideOwnRandom复盘Schemas（concurrentCommitsOnlyInsideOwnRandomReviewSchemas）字段，值为真，表示条件成立或开关打开。
        "concurrentCommitsOnlyInsideOwnRandomReviewSchemas": True,
        # 填写legacyAssertionAdapters字段，值为表达式 {"growth": "business table count 6 -> 9", "execution": "business table count 18 -> 21"} 的结果。
        "legacyAssertionAdapters": {"growth": "business table count 6 -> 9", "execution": "business table count 18 -> 21"},
        # 填写复盘分组列表（reviewGroups）字段，值为表达式 groups, "inputSha256": hashes, "cases": CASES 的结果。
        "reviewGroups": groups, "inputSha256": hashes, "cases": CASES,
    }
    # 按指定编码写入文件文字。
    (REPORT_ROOT / "sql-check-results.json").write_text(
        # 继续传入或组合把对象编码为 JSON 文字，完成上面尚未结束的表达式。
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # 调用 print 完成print处理。
    print(f"Remaining SQL checks passed: {len(CASES)}/{len(CASES)}; "
          "all review schemas removed; development database not connected.")


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == "__main__":
    # 调用 main 完成程序入口处理。
    main()
