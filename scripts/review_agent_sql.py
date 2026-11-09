# 检查复盘成长助手sql相关行为，断言失败时报告具体不符合预期的场景。
# P1.2 SQL 专项隔离校验，不建立单元／系统／集成测试框架，不连接开发库。

# 引入 hashlib，供下面调用其功能。
import hashlib

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 引入 re，供下面调用其功能。
import re

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 引入 uuid，供下面调用其功能。
import uuid

# 从 concurrent.futures 引入 Thread连接池Executor（ThreadPoolExecutor）。
from concurrent.futures import ThreadPoolExecutor

# 从 datetime 引入 日期时间类型（datetime）。
from datetime import datetime

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 从 threading 引入 事件（Event）。
from threading import Event

# 从 zoneinfo 引入 时区信息（ZoneInfo）。
from zoneinfo import ZoneInfo

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 从 psycopg2 引入 sql。
from psycopg2 import sql

# 把从执行“调用 Path(__file__).resolve 完成resolve处理”得到的结果的parents属性中按固定数值 1取出的内容保存到 根（ROOT）。
ROOT = Path(__file__).resolve().parents[1]
# 把按 ROOT / 'backend/app/src/main/resources/sql/agent.sql' 计算的结果保存到 SQL路径（SQL_PATH）。
SQL_PATH = ROOT / "backend/app/src/main/resources/sql/agent.sql"
# 把按 ROOT / 'backend/app/src/main/resources/sql/identity.sql' 计算的结果保存到 账户管理路径（IDENTITY_PATH）。
IDENTITY_PATH = ROOT / "backend/app/src/main/resources/sql/identity.sql"
# 把固定文字 'asterway_identity_test'保存到 数据库（DATABASE）。
DATABASE = "asterway_identity_test"
# 把空列表或集合保存到 CASES。
CASES = []
# 把固定文字 '2026-11-05T00:00:00Z'保存到 T0。
T0 = "2026-11-05T00:00:00Z"


# 函数 connect：调用 connect 完成连接处理。
def connect():
    # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = psycopg2.connect(
        # 传入dbname，值为数据库（DATABASE）。
        dbname=DATABASE,
        # 传入用户（user），值为固定文字 'postgres'。
        user="postgres",
        # 传入host，值为固定文字 '/var/run/postgresql'。
        host="/var/run/postgresql",
        # 传入application名称（application_name），值为固定文字 'asterway_agent_sql_review'。
        application_name="asterway_agent_sql_review",
    )
    # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
    with connection.cursor() as cursor:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT current_database()")
        # 检查条件 cursor.fetchone()[0] != DATABASE 是否成立；成立时执行缩进的分支。
        if cursor.fetchone()[0] != DATABASE:
            # 释放连接、文件或客户端占用的资源。
            connection.close()
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("SQL review refused outside the dedicated review database")
        # 执行传入的数据库语句并取得结果。
        cursor.execute(
            # 继续传入或组合固定文字 "SET LOCAL statement_timeout='8s'; SET LOCAL lock_timeout='5s'; SET LOCAL TIME ZO…"，完成上面尚未结束的表达式。
            "SET LOCAL statement_timeout='8s'; SET LOCAL lock_timeout='5s'; SET LOCAL TIME ZONE 'UTC'"
        )
    # 把数据库连接（connection）交还给调用方，本函数到此结束。
    return connection


# 函数 check：调用 check 完成核对处理；接收 名称（name）、condition。
def check(name, condition):
    # 检查条件 not condition 是否成立；成立时执行缩进的分支。
    if not condition:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Agent SQL assertion failed: " + name)
    # 检查条件 name in CASES 是否成立；成立时执行缩进的分支。
    if name in CASES:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Duplicate SQL assertion: " + name)
    # 把新条目放到列表末尾。
    CASES.append(name)


# 函数 scalar：取得查询结果中的一个值；接收 数据库语句执行器（cursor）、statement、parameters。
def scalar(cursor, statement, parameters=None):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(statement, parameters)
    # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容交还给调用方，本函数到此结束。
    return cursor.fetchone()[0]


# 函数 reject：调用 reject 完成拒绝处理；接收 数据库语句执行器（cursor）、名称（name）、statement、parameters、编码（code）。
def reject(cursor, name, statement, parameters=None, code="23514"):
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SAVEPOINT expected_rejection")
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 执行传入的数据库语句并取得结果。
        cursor.execute(statement, parameters)
    # 捕获psycopg2.Error异常，执行下面的错误处理，异常对象保存为 error。
    except psycopg2.Error as error:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT expected_rejection")
        # 调用 check 完成核对处理。
        check(name, error.pgcode == code)
    # 前面的条件不满足时，按这里的条件或备用分支继续处理。
    else:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("ROLLBACK TO SAVEPOINT expected_rejection")
        # 调用 check 完成核对处理。
        check(name, False)
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 执行传入的数据库语句并取得结果。
        cursor.execute("RELEASE SAVEPOINT expected_rejection")


# 函数 block：调用 block 完成block处理；接收 来源（source）、名称（name）。
def block(source, name):
    # 把按 '— BEGIN ' + re.escape(name) + '(.*?)— END ' + re.escape(name) 计算的结果保存到 匹配规则（pattern）。
    pattern = r"-- BEGIN " + re.escape(name) + r"(.*?)-- END " + re.escape(name)
    # 把执行“查找 全部（all）”得到的结果保存到 matches。
    matches = re.findall(pattern, source, re.DOTALL)
    # 检查条件 len(matches) != 1 是否成立；成立时执行缩进的分支。
    if len(matches) != 1:
        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
        raise RuntimeError("Expected one SQL block: " + name)
    # 把从matches中按固定数值 0取出的内容交还给调用方，本函数到此结束。
    return matches[0]


# 函数 source_for_schema：调用 source_for_schema 完成来源for数据库分区处理；接收 来源（source）、fact数据库分区（fact_schema）。
def source_for_schema(source, fact_schema):
    # 只改固定 schema 标识符；权限中的 agent: 前缀和阶段开关保持原样。
    # 把执行“生成保留原对象其余字段的新副本，只替换明确指定的值”得到的结果保存到 来源（source）。
    source = source.replace("SCHEMA agent", "SCHEMA " + fact_schema)
    # 把执行“调用 re.sub 完成sub处理”得到的结果保存到 来源（source）。
    source = re.sub(r"\bagent\.", fact_schema + ".", source)
    # 把执行“调用 re.sub 完成sub处理”得到的结果交还给调用方，本函数到此结束。
    return re.sub(r"(?mi)^\s*(?:BEGIN|COMMIT);\s*$", "", source)


# 函数 initialize：调用 initialize 完成初始化处理；接收 数据库语句执行器（cursor）、来源（source）、账户管理来源（identity_source）、账户管理数据库分区（identity_schema）、fact数据库分区（fact_schema）。
def initialize(cursor, source, identity_source, identity_schema, fact_schema):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(identity_schema)))
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
        sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(sql.Identifier(identity_schema))
    )
    # 使用现有六张 Identity 结构及最小内置授权夹具，不复制任何用户数据。
    # 把从执行“按指定分隔规则拆分文字”得到的结果中按固定数值 0取出的内容保存到 账户管理ddl（identity_ddl）。
    identity_ddl = identity_source.split("CREATE TABLE identity_role (", 1)[1].split(
        # 继续传入或组合表达式 "CREATE TABLE identity_login_log (", 1 的结果，完成上面尚未结束的表达式。
        "CREATE TABLE identity_login_log (", 1
    # 继续传入或组合表达式 )[0] 的结果，完成上面尚未结束的表达式。
    )[0]
    # 执行传入的数据库语句并取得结果。
    cursor.execute("CREATE TABLE identity_role (" + identity_ddl)
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：向 identity_role 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
    # 内嵌 SQL：向 identity_menu 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
    cursor.execute("""INSERT INTO identity_role(code,name,is_builtin)
        VALUES ('USER','Ordinary user',true),('ADMIN','Administrator',true),('SUPER_ADMIN','Super administrator',true);
        INSERT INTO identity_menu(code,name,menu_type,route_path,is_builtin)
        VALUES ('growth_root','Growth','DIRECTORY','/growth',true);""")
    # 从依次包含 固定文字 'agent_backend_ready'、固定文字 'agent_frontend_ready'、固定文字 'agent_conversation_backend_ready' 的集合依次取出名称（name），每项执行一次下面的处理。
    for name in ("agent_backend_ready", "agent_frontend_ready", "agent_conversation_backend_ready"):
        # 执行传入的数据库语句并取得结果。
        cursor.execute("SELECT set_config(%s,'false',true)", ("asterway." + name,))
    # 把执行“调用 source_for_schema 完成来源for数据库分区处理”得到的结果保存到 transformed。
    transformed = source_for_schema(source, fact_schema)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(transformed)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
        sql.SQL("SET LOCAL search_path TO {}, {}, pg_catalog").format(
            # 继续传入或组合调用 sql.Identifier 完成Identifier处理，完成上面尚未结束的表达式。
            sql.Identifier(fact_schema), sql.Identifier(identity_schema)
        )
    )
    # 把transformed交还给调用方，本函数到此结束。
    return transformed


# 函数 conversation：调用 conversation 完成对话处理；接收 数据库语句执行器（cursor）、用户（user）、标题（title）。
def conversation(cursor, user=1001, title="Conversation"):
    # 把执行“取得查询结果中的一个值”得到的结果交还给调用方，本函数到此结束。
    return scalar(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合表达式 """INSERT INTO conversation(user_id,title,created_at,updated_at) 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：向 conversation 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        """INSERT INTO conversation(user_id,title,created_at,updated_at)
        VALUES (%s,%s,%s,%s) RETURNING id""",
        # 继续传入或组合表达式 (user, title, T0, T0) 的结果，完成上面尚未结束的表达式。
        (user, title, T0, T0),
    )


# 函数 turn：调用 turn 完成本轮对话处理；接收 数据库语句执行器（cursor）、对话编号（conversation_id）、用户（user）、请求编号（request_id）。
def turn(cursor, conversation_id, user=1001, request_id=None):
    # 把执行“取得查询结果中的一个值”得到的结果交还给调用方，本函数到此结束。
    return scalar(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合表达式 """INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：向 conversation_turn 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：创建时at（created_at）、更新时at（updated_at）、开始时at（started_at）。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        """INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash,
        created_at,updated_at,started_at,deadline_at)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s::timestamptz+interval '120 seconds') RETURNING id""",
        # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：(user, conversation_id, str(request_id or uuid.uuid4()), "a" * 64, T0, T0, T0, T0)，完成上面尚未结束的表达式。
        (user, conversation_id, str(request_id or uuid.uuid4()), "a" * 64, T0, T0, T0, T0),
    )


# 函数 message：调用 message 完成消息处理；接收 数据库语句执行器（cursor）、对话编号（conversation_id）、本轮对话编号（turn_id）、数值（number）、角色（role）、内容（content）。
def message(cursor, conversation_id, turn_id, number, role, content, **extra):
    # 把按字段名保存多个值的字典保存到 当前记录（row）。
    row = {
        # 填写用户编号（user_id）字段，值为固定数值 1001。
        "user_id": 1001,
        # 填写对话编号（conversation_id）字段，值为对话编号（conversation_id）。
        "conversation_id": conversation_id,
        # 填写本轮对话编号（turn_id）字段，值为本轮对话编号（turn_id）。
        "turn_id": turn_id,
        # 填写消息no（message_no）字段，值为数值（number）。
        "message_no": number,
        # 填写角色（role）字段，值为角色（role）。
        "role": role,
        # 填写内容（content）字段，值为内容（content）。
        "content": content,
        # 填写创建时at（created_at）字段，值为T0。
        "created_at": T0,
        # 填写更新时at（updated_at）字段，值为T0。
        "updated_at": T0,
        # 继续传入或组合按表达式计算得到的值：**extra，完成上面尚未结束的表达式。
        **extra,
    }
    # 把执行“调用 list 完成列表处理”得到的结果保存到 columns。
    columns = list(row)
    # 把执行“调用 sql.SQL('INSERT INTO conversation_message ({}) VALUES ({}) RETURNING id').format 完成格式处理”得到的结果保存到 statement。
    statement = sql.SQL("INSERT INTO conversation_message ({}) VALUES ({}) RETURNING id").format(
        # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
        sql.SQL(",").join(map(sql.Identifier, columns)),
        # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
        sql.SQL(",").join(sql.Placeholder() for _ in columns),
    )
    # 把执行“取得查询结果中的一个值”得到的结果交还给调用方，本函数到此结束。
    return scalar(cursor, statement, [row[key] for key in columns])


# 函数 finish：调用 finish 完成finish处理；接收 数据库语句执行器（cursor）、本轮对话编号（turn_id）、状态（status）、原因（reason）、错误（error）。
def finish(cursor, turn_id, status="COMPLETED", reason="stop", error=None):
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合表达式 """UPDATE conversation_turn SET status=%s,finish_reason=%s,error_code=%s 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        """UPDATE conversation_turn SET status=%s,finish_reason=%s,error_code=%s,
        finished_at=%s::timestamptz+interval '1 second',updated_at=%s::timestamptz+interval '1 second',
        version=version+1 WHERE id=%s""",
        # 继续传入或组合表达式 (status, reason, error, T0, T0, turn_id) 的结果，完成上面尚未结束的表达式。
        (status, reason, error, T0, T0, turn_id),
    )


# 函数 authorization_review：调用 authorization_review 完成授权复盘处理；接收 数据库语句执行器（cursor）、transformed。
def authorization_review(cursor, transformed):
    # 把执行“调用 block 完成block处理”得到的结果保存到 legacy。
    legacy = block(transformed, "AGENT AUTHORIZATION")
    # 把执行“调用 block 完成block处理”得到的结果保存到 当前（current）。
    current = block(transformed, "AGENT CONVERSATION AUTHORIZATION")
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'new_permission_count'，完成上面尚未结束的表达式。
        "new_permission_count",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%'"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%'",
        )
        # 继续传入或组合条件判断的结果：== 6，完成上面尚未结束的表达式。
        == 6,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'new_permissions_disabled'，完成上面尚未结束的表达式。
        "new_permissions_disabled",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' …"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
        == 0,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'new_menus_hidden'，完成上面尚未结束的表达式。
        "new_menus_hidden",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent_conversation_%'…"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_menu WHERE code LIKE 'growth_agent_conversation_%' AND (is_enabled OR is_visible)",
        )
        # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
        == 0,
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合固定文字 "SET LOCAL asterway.agent_backend_ready='true'; SET LOCAL asterway.agent_frontend…"，完成上面尚未结束的表达式。
        "SET LOCAL asterway.agent_backend_ready='true'; SET LOCAL asterway.agent_frontend_ready='true';"
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(legacy)
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'p11_flag_does_not_enable_p12'，完成上面尚未结束的表达式。
        "p11_flag_does_not_enable_p12",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' …"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
        == 0,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'p11_page_still_available'，完成上面尚未结束的表达式。
        "p11_page_still_available",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_menu WHERE code='growth_agent' AND is_visible AND …"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_menu WHERE code='growth_agent' AND is_visible AND is_enabled",
        )
        # 继续传入或组合条件判断的结果：== 1，完成上面尚未结束的表达式。
        == 1,
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute("SET LOCAL asterway.agent_conversation_backend_ready='true'")
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'missing_table_blocks_enable'，完成上面尚未结束的表达式。
        "missing_table_blocks_enable",
        # 继续传入或组合按表达式计算得到的值："ALTER TABLE conversation RENAME TO absent_conversation;" + current，完成上面尚未结束的表达式。
        "ALTER TABLE conversation RENAME TO absent_conversation;" + current,
        # 传入编码（code），值为固定状态或编码 P0001（P0001）。
        code="P0001",
    )
    # 执行传入的数据库语句并取得结果。
    # 内嵌 SQL：向 identity_role_permission 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：向 identity_role_menu 写入新记录，字段顺序与后面的值一一对应。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    cursor.execute("""INSERT INTO identity_role_permission(role_id,permission_id)
        SELECT r.id,p.id FROM identity_role r CROSS JOIN identity_permission p
        WHERE r.code='ADMIN' AND p.code LIKE 'agent:conversation:%';
        INSERT INTO identity_role_menu(role_id,menu_id)
        SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m
        WHERE r.code='SUPER_ADMIN' AND m.code LIKE 'growth_agent_conversation_%';""")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(current)
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'p12_permissions_enable_only_after_flag'，完成上面尚未结束的表达式。
        "p12_permissions_enable_only_after_flag",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合固定文字 "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' …"，完成上面尚未结束的表达式。
            "SELECT count(*) FROM identity_permission WHERE code LIKE 'agent:conversation:%' AND is_enabled",
        )
        # 继续传入或组合条件判断的结果：== 6，完成上面尚未结束的表达式。
        == 6,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'ordinary_user_has_all_six_actions'，完成上面尚未结束的表达式。
        "ordinary_user_has_all_six_actions",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合按表达式计算得到的值："""SELECT count(*) FROM identity_role_permission rp，完成上面尚未结束的表达式。
            # 内嵌 SQL：读取后面列出的字段或计算结果。
            # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
            # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
            """SELECT count(*) FROM identity_role_permission rp
        JOIN identity_role r ON r.id=rp.role_id JOIN identity_permission p ON p.id=rp.permission_id
        WHERE r.code='USER' AND p.code LIKE 'agent:conversation:%'""",
        )
        # 继续传入或组合条件判断的结果：== 6，完成上面尚未结束的表达式。
        == 6,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'manager_grants_removed'，完成上面尚未结束的表达式。
        "manager_grants_removed",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合表达式 """SELECT NOT EXISTS( 的结果，完成上面尚未结束的表达式。
            # 内嵌 SQL：读取后面列出的字段或计算结果。
            # 内嵌 SQL：读取后面列出的字段或计算结果。
            # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
            # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
            # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
            """SELECT NOT EXISTS(
        SELECT 1 FROM identity_role_permission rp JOIN identity_role r ON r.id=rp.role_id
        JOIN identity_permission p ON p.id=rp.permission_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code LIKE 'agent:conversation:%')
        AND NOT EXISTS(SELECT 1 FROM identity_role_menu rm JOIN identity_role r ON r.id=rm.role_id
        JOIN identity_menu m ON m.id=rm.menu_id WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND m.code LIKE 'growth_agent_conversation_%')""",
        ),
    )
    # 把执行“取得查询结果中的一个值”得到的结果保存到 确认时的数据快照（snapshot）。
    snapshot = scalar(cursor, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r")
    # 把执行“取得查询结果中的一个值”得到的结果保存到 权限确认时的数据快照（permission_snapshot）。
    permission_snapshot = scalar(
        # 继续传入或组合表达式 cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p" 的结果，完成上面尚未结束的表达式。
        cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p"
    )
    # 把执行“取得查询结果中的一个值”得到的结果保存到 菜单确认时的数据快照（menu_snapshot）。
    menu_snapshot = scalar(cursor, "SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m")
    # 执行传入的数据库语句并取得结果。
    cursor.execute(legacy + current)
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'repeated_seed_preserves_role_versions'，完成上面尚未结束的表达式。
        "repeated_seed_preserves_role_versions",
        # 继续传入或组合确认时的数据快照（snapshot），完成上面尚未结束的表达式。
        snapshot
        # 继续传入或组合条件判断的结果：== scalar(cursor, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r")，完成上面尚未结束的表达式。
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(r) ORDER BY id) FROM identity_role r"),
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'repeated_seed_preserves_permission_versions'，完成上面尚未结束的表达式。
        "repeated_seed_preserves_permission_versions",
        # 继续传入或组合权限确认时的数据快照（permission_snapshot），完成上面尚未结束的表达式。
        permission_snapshot
        # 继续传入或组合条件判断的结果：== scalar(cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p")，完成上面尚未结束的表达式。
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(p) ORDER BY id) FROM identity_permission p"),
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'repeated_seed_preserves_menu_versions'，完成上面尚未结束的表达式。
        "repeated_seed_preserves_menu_versions",
        # 继续传入或组合菜单确认时的数据快照（menu_snapshot），完成上面尚未结束的表达式。
        menu_snapshot
        # 继续传入或组合条件判断的结果：== scalar(cursor, "SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m")，完成上面尚未结束的表达式。
        == scalar(cursor, "SELECT jsonb_agg(to_jsonb(m) ORDER BY id) FROM identity_menu m"),
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'custom_reserved_code_rejected'，完成上面尚未结束的表达式。
        "custom_reserved_code_rejected",
        # 继续传入或组合固定文字 "UPDATE identity_permission SET is_builtin=false WHERE code='agent:conversation:c…"，完成上面尚未结束的表达式。
        "UPDATE identity_permission SET is_builtin=false WHERE code='agent:conversation:create';"
        # 继续传入或组合按表达式计算得到的值：+ current，完成上面尚未结束的表达式。
        + current,
        # 传入编码（code），值为固定状态或编码 P0001（P0001）。
        code="P0001",
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'deleted_reserved_code_rejected'，完成上面尚未结束的表达式。
        "deleted_reserved_code_rejected",
        # 继续传入或组合固定文字 'UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHE…'，完成上面尚未结束的表达式。
        "UPDATE identity_permission SET is_enabled=false,deleted_at=clock_timestamp() WHERE code='agent:conversation:read';"
        # 继续传入或组合按表达式计算得到的值：+ current，完成上面尚未结束的表达式。
        + current,
        # 传入编码（code），值为固定状态或编码 P0001（P0001）。
        code="P0001",
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'reserved_menu_contract_rejected'，完成上面尚未结束的表达式。
        "reserved_menu_contract_rejected",
        # 继续传入或组合固定文字 "UPDATE identity_menu SET parent_id=(SELECT id FROM identity_menu WHERE code='gro…"，完成上面尚未结束的表达式。
        "UPDATE identity_menu SET parent_id=(SELECT id FROM identity_menu WHERE code='growth_root') WHERE code='growth_agent_conversation_read';"
        # 继续传入或组合按表达式计算得到的值：+ current，完成上面尚未结束的表达式。
        + current,
        # 传入编码（code），值为固定状态或编码 P0001（P0001）。
        code="P0001",
    )


# 函数 constraints_review：调用 constraints_review 完成constraints复盘处理；接收 数据库语句执行器（cursor）。
def constraints_review(cursor):
    # 把依次包含 执行“调用 conversation 完成对话处理”得到的结果、执行“调用 conversation 完成对话处理”得到的结果 的集合保存到 首次秒（(first, second)）。
    first, second = conversation(cursor), conversation(cursor, 1002, "Second user")
    # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 首次本轮对话（first_turn）。
    first_turn = turn(cursor, first)
    # 从依次包含 依次包含 固定文字 'invalid_conversation_status'、固定文字 "UPDATE conversation SET status='UNKNOWN' WHERE id=%s" 的集合、依次包含 固定文字 'blank_title'、固定文字 "UPDATE conversation SET title=' ' WHERE id=%s" 的集合、依次包含 固定文字 'closed_requires_time'、固定文字 "UPDATE conversation SET status='CLOSED' WHERE id=%s" 的集合、依次包含 固定文字 'active_cannot_be_deleted'、固定文字 'UPDATE conversation SET deleted_at=created_at WHERE id=%s' 的集合、依次包含 固定文字 'negative_version'、固定文字 'UPDATE conversation SET version=-1 WHERE id=%s' 的集合等内容 的集合依次取出名称statement（(name, statement)），每项执行一次下面的处理。
    for name, statement in (
        # 继续传入或组合表达式 ("invalid_conversation_status", "UPDATE conversation SET status='UNKNOWN' WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("invalid_conversation_status", "UPDATE conversation SET status='UNKNOWN' WHERE id=%s"),
        # 继续传入或组合表达式 ("blank_title", "UPDATE conversation SET title=' ' WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("blank_title", "UPDATE conversation SET title='   ' WHERE id=%s"),
        # 继续传入或组合表达式 ("closed_requires_time", "UPDATE conversation SET status='CLOSED' WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("closed_requires_time", "UPDATE conversation SET status='CLOSED' WHERE id=%s"),
        # 继续传入或组合表达式 ("active_cannot_be_deleted", "UPDATE conversation SET deleted_at=created_at WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("active_cannot_be_deleted", "UPDATE conversation SET deleted_at=created_at WHERE id=%s"),
        # 继续传入或组合表达式 ("negative_version", "UPDATE conversation SET version=-1 WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("negative_version", "UPDATE conversation SET version=-1 WHERE id=%s"),
        # 继续传入或组合表达式 ("infinite_conversation_time", "UPDATE conversation SET updated_at='infinity' WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("infinite_conversation_time", "UPDATE conversation SET updated_at='infinity' WHERE id=%s"),
        (
            # 继续传入或组合固定文字 'summary_requires_boundary_and_time'，完成上面尚未结束的表达式。
            "summary_requires_boundary_and_time",
            # 继续传入或组合固定文字 "UPDATE conversation SET context_summary='summary' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation SET context_summary='summary' WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'empty_summary_cannot_advance_boundary'，完成上面尚未结束的表达式。
            "empty_summary_cannot_advance_boundary",
            # 继续传入或组合固定文字 'UPDATE conversation SET summary_through_message_no=1,next_message_no=2 WHERE id=…'，完成上面尚未结束的表达式。
            "UPDATE conversation SET summary_through_message_no=1,next_message_no=2 WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'summary_cannot_cover_future_message'，完成上面尚未结束的表达式。
            "summary_cannot_cover_future_message",
            # 继续传入或组合固定文字 "UPDATE conversation SET context_summary='summary',summary_through_message_no=1,s…"，完成上面尚未结束的表达式。
            "UPDATE conversation SET context_summary='summary',summary_through_message_no=1,summary_updated_at=created_at WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'summary_size_limit'，完成上面尚未结束的表达式。
            "summary_size_limit",
            # 继续传入或组合固定文字 "UPDATE conversation SET context_summary=repeat('x',16001) WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation SET context_summary=repeat('x',16001) WHERE id=%s",
        ),
    ):
        # 调用 reject 完成拒绝处理。
        reject(cursor, name, statement, (first,))
    # 从依次包含 依次包含 固定文字 'turn_invalid_status'、固定文字 "UPDATE conversation_turn SET status='OTHER' WHERE id=%s" 的集合、依次包含 固定文字 'turn_completed_requires_finish_time'、固定文字 "UPDATE conversation_turn SET status='COMPLETED',finish_reason='stop' WHERE id=%s" 的集合、依次包含 固定文字 'turn_completed_cannot_have_null_reason'、固定文字 "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at WHERE id=…" 的集合、依次包含 固定文字 'turn_length_is_not_completed'、固定文字 "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at,finish_re…" 的集合、依次包含 固定文字 'cancel_requires_error_code'、固定文字 "UPDATE conversation_turn SET status='CANCELLED',finished_at=started_at WHERE id=…" 的集合等内容 的集合依次取出名称statement（(name, statement)），每项执行一次下面的处理。
    for name, statement in (
        # 继续传入或组合表达式 ("turn_invalid_status", "UPDATE conversation_turn SET status='OTHER' WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("turn_invalid_status", "UPDATE conversation_turn SET status='OTHER' WHERE id=%s"),
        (
            # 继续传入或组合固定文字 'turn_completed_requires_finish_time'，完成上面尚未结束的表达式。
            "turn_completed_requires_finish_time",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='COMPLETED',finish_reason='stop' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='COMPLETED',finish_reason='stop' WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'turn_completed_cannot_have_null_reason'，完成上面尚未结束的表达式。
            "turn_completed_cannot_have_null_reason",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at WHERE id=…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'turn_length_is_not_completed'，完成上面尚未结束的表达式。
            "turn_length_is_not_completed",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at,finish_re…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='COMPLETED',finished_at=started_at,finish_reason='length' WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'cancel_requires_error_code'，完成上面尚未结束的表达式。
            "cancel_requires_error_code",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='CANCELLED',finished_at=started_at WHERE id=…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='CANCELLED',finished_at=started_at WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'failed_requires_finish_time'，完成上面尚未结束的表达式。
            "failed_requires_finish_time",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='FAILED',error_code='MODEL_FAILED' WHERE id=…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='FAILED',error_code='MODEL_FAILED' WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'incomplete_requires_non_stop_reason'，完成上面尚未结束的表达式。
            "incomplete_requires_non_stop_reason",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET status='INCOMPLETE',finished_at=started_at,error_co…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET status='INCOMPLETE',finished_at=started_at,error_code='TRUNCATED',finish_reason='stop' WHERE id=%s",
        ),
        # 继续传入或组合表达式 ("deadline_after_start", "UPDATE conversation_turn SET deadline_at=started_at WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("deadline_after_start", "UPDATE conversation_turn SET deadline_at=started_at WHERE id=%s"),
        (
            # 继续传入或组合固定文字 'turn_infinite_deadline'，完成上面尚未结束的表达式。
            "turn_infinite_deadline",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET deadline_at='infinity' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET deadline_at='infinity' WHERE id=%s",
        ),
        # 继续传入或组合表达式 ("turn_negative_version", "UPDATE conversation_turn SET version=-1 WHERE id=%s") 的结果，完成上面尚未结束的表达式。
        ("turn_negative_version", "UPDATE conversation_turn SET version=-1 WHERE id=%s"),
        (
            # 继续传入或组合固定文字 'turn_nil_request_id'，完成上面尚未结束的表达式。
            "turn_nil_request_id",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET request_id='00000000-0000-0000-0000-000000000000' W…"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET request_id='00000000-0000-0000-0000-000000000000' WHERE id=%s",
        ),
        (
            # 继续传入或组合固定文字 'request_hash_must_be_sha256'，完成上面尚未结束的表达式。
            "request_hash_must_be_sha256",
            # 继续传入或组合固定文字 "UPDATE conversation_turn SET request_hash='not-a-hash' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_turn SET request_hash='not-a-hash' WHERE id=%s",
        ),
    ):
        # 调用 reject 完成拒绝处理。
        reject(cursor, name, statement, (first_turn,))
    # 把执行“调用 message 完成消息处理”得到的结果保存到 用户消息（user_message）。
    user_message = message(cursor, first, first_turn, 1, "USER", "Keep the daily time limit")
    # 把执行“调用 message 完成消息处理”得到的结果保存到 assistant。
    assistant = message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合首次本轮对话（first_turn），完成上面尚未结束的表达式。
        first_turn,
        # 继续传入或组合固定数值 2，完成上面尚未结束的表达式。
        2,
        # 继续传入或组合固定状态或编码 ASSISTANT（ASSISTANT），完成上面尚未结束的表达式。
        "ASSISTANT",
        # 继续传入或组合固定文字 'Partial'，完成上面尚未结束的表达式。
        "Partial",
        # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
        is_complete=False,
        # 传入模型（model），值为固定文字 'review-model'。
        model="review-model",
    )
    # 从依次包含 依次包含 固定文字 'blank_user_message'、固定文字 "UPDATE conversation_message SET content=' ' WHERE id=%s"、依次包含 用户消息（user_message） 的集合 的集合、依次包含 固定文字 'user_message_size_limit'、固定文字 "UPDATE conversation_message SET content=repeat('x',8001) WHERE id=%s"、依次包含 用户消息（user_message） 的集合 的集合、依次包含 固定文字 'user_cannot_be_partial'、固定文字 'UPDATE conversation_message SET is_complete=false WHERE id=%s'、依次包含 用户消息（user_message） 的集合 的集合、依次包含 固定文字 'user_cannot_have_model'、固定文字 "UPDATE conversation_message SET model='model' WHERE id=%s"、依次包含 用户消息（user_message） 的集合 的集合、依次包含 固定文字 'json_array_required'、固定文字 "UPDATE conversation_message SET tool_calls='{}'::jsonb WHERE id=%s"、依次包含 assistant 的集合 的集合等内容 的集合依次取出名称statement参数集合（(name, statement, params)），每项执行一次下面的处理。
    for name, statement, params in (
        (
            # 继续传入或组合固定文字 'blank_user_message'，完成上面尚未结束的表达式。
            "blank_user_message",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET content=' ' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET content=' ' WHERE id=%s",
            # 继续传入或组合表达式 (user_message,) 的结果，完成上面尚未结束的表达式。
            (user_message,),
        ),
        (
            # 继续传入或组合固定文字 'user_message_size_limit'，完成上面尚未结束的表达式。
            "user_message_size_limit",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET content=repeat('x',8001) WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET content=repeat('x',8001) WHERE id=%s",
            # 继续传入或组合表达式 (user_message,) 的结果，完成上面尚未结束的表达式。
            (user_message,),
        ),
        (
            # 继续传入或组合固定文字 'user_cannot_be_partial'，完成上面尚未结束的表达式。
            "user_cannot_be_partial",
            # 继续传入或组合固定文字 'UPDATE conversation_message SET is_complete=false WHERE id=%s'，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET is_complete=false WHERE id=%s",
            # 继续传入或组合表达式 (user_message,) 的结果，完成上面尚未结束的表达式。
            (user_message,),
        ),
        (
            # 继续传入或组合固定文字 'user_cannot_have_model'，完成上面尚未结束的表达式。
            "user_cannot_have_model",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET model='model' WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET model='model' WHERE id=%s",
            # 继续传入或组合表达式 (user_message,) 的结果，完成上面尚未结束的表达式。
            (user_message,),
        ),
        (
            # 继续传入或组合固定文字 'json_array_required'，完成上面尚未结束的表达式。
            "json_array_required",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET tool_calls='{}'::jsonb WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET tool_calls='{}'::jsonb WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'usage_object_required'，完成上面尚未结束的表达式。
            "usage_object_required",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET usage='[]'::jsonb WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET usage='[]'::jsonb WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'assistant_requires_model'，完成上面尚未结束的表达式。
            "assistant_requires_model",
            # 继续传入或组合固定文字 'UPDATE conversation_message SET model=NULL WHERE id=%s'，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET model=NULL WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'complete_assistant_requires_finish_reason'，完成上面尚未结束的表达式。
            "complete_assistant_requires_finish_reason",
            # 继续传入或组合固定文字 'UPDATE conversation_message SET is_complete=true WHERE id=%s'，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET is_complete=true WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'truncated_reply_not_complete'，完成上面尚未结束的表达式。
            "truncated_reply_not_complete",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET is_complete=true,finish_reason='length' WHERE id…"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET is_complete=true,finish_reason='length' WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'message_size_limit'，完成上面尚未结束的表达式。
            "message_size_limit",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET content=repeat('x',65537) WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET content=repeat('x',65537) WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'message_sequence_positive'，完成上面尚未结束的表达式。
            "message_sequence_positive",
            # 继续传入或组合固定文字 'UPDATE conversation_message SET message_no=0 WHERE id=%s'，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET message_no=0 WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'duplicate_message_no'，完成上面尚未结束的表达式。
            "duplicate_message_no",
            # 继续传入或组合固定文字 'UPDATE conversation_message SET message_no=1 WHERE id=%s'，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET message_no=1 WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
        (
            # 继续传入或组合固定文字 'tool_requires_parent_call_and_name'，完成上面尚未结束的表达式。
            "tool_requires_parent_call_and_name",
            # 继续传入或组合固定文字 "UPDATE conversation_message SET role='TOOL',model=NULL WHERE id=%s"，完成上面尚未结束的表达式。
            "UPDATE conversation_message SET role='TOOL',model=NULL WHERE id=%s",
            # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
            (assistant,),
        ),
    ):
        # 调用 reject 完成拒绝处理。
        reject(
            # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
            cursor,
            # 继续传入或组合名称（name），完成上面尚未结束的表达式。
            name,
            # 继续传入或组合statement，完成上面尚未结束的表达式。
            statement,
            # 继续传入或组合参数集合（params），完成上面尚未结束的表达式。
            params,
            # 传入编码（code），值为固定文字 '23505" if name == "duplicate_message_no" else "23514'。
            code="23505" if name == "duplicate_message_no" else "23514",
        )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合固定文字 "UPDATE conversation_message SET content='Complete answer',is_complete=true,finis…"，完成上面尚未结束的表达式。
        "UPDATE conversation_message SET content='Complete answer',is_complete=true,finish_reason='stop' WHERE id=%s",
        # 继续传入或组合表达式 (assistant,) 的结果，完成上面尚未结束的表达式。
        (assistant,),
    )
    # 调用 finish 完成finish处理。
    finish(cursor, first_turn)
    # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 秒本轮对话（second_turn）。
    second_turn = turn(cursor, first)
    # 调用 message 完成消息处理。
    message(cursor, first, second_turn, 3, "USER", "Continue with the same constraints")
    # 把执行“把对象编码为 JSON 文字”得到的结果保存到 调用列表（calls）。
    calls = json.dumps(
        [
            {
                # 填写编号（id）字段，值为固定文字 'shared_call'。
                "id": "shared_call",
                # 填写类型（type）字段，值为固定文字 'function'。
                "type": "function",
                # 填写function字段，值为表达式 {"name": "growth_get_goals", "arguments": "{}"} 的结果。
                "function": {"name": "growth_get_goals", "arguments": "{}"},
            }
        ]
    )
    # 把执行“调用 message 完成消息处理”得到的结果保存到 请求消息（request_message）。
    request_message = message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合秒本轮对话（second_turn），完成上面尚未结束的表达式。
        second_turn,
        # 继续传入或组合固定数值 4，完成上面尚未结束的表达式。
        4,
        # 继续传入或组合固定状态或编码 ASSISTANT（ASSISTANT），完成上面尚未结束的表达式。
        "ASSISTANT",
        # 继续传入或组合固定文字 ''，完成上面尚未结束的表达式。
        "",
        # 传入模型（model），值为固定文字 'review-model'。
        model="review-model",
        # 传入finish原因（finish_reason），值为固定文字 'tool_calls'。
        finish_reason="tool_calls",
        # 传入业务工具调用列表（tool_calls），值为调用列表（calls）。
        tool_calls=calls,
    )
    # 把执行“调用 message 完成消息处理”得到的结果保存到 业务工具结果（tool_result）。
    tool_result = message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合秒本轮对话（second_turn），完成上面尚未结束的表达式。
        second_turn,
        # 继续传入或组合固定数值 5，完成上面尚未结束的表达式。
        5,
        # 继续传入或组合固定状态或编码 TOOL（业务工具），完成上面尚未结束的表达式。
        "TOOL",
        # 继续传入或组合固定文字 '{"items":[]}'，完成上面尚未结束的表达式。
        '{"items":[]}',
        # 传入业务工具请求消息编号（tool_request_message_id），值为请求消息（request_message）。
        tool_request_message_id=request_message,
        # 传入业务工具调用编号（tool_call_id），值为固定文字 'shared_call'。
        tool_call_id="shared_call",
        # 传入业务工具名称（tool_name），值为固定文字 'growth_get_goals'。
        tool_name="growth_get_goals",
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'tool_result_cannot_reference_itself'，完成上面尚未结束的表达式。
        "tool_result_cannot_reference_itself",
        # 继续传入或组合固定文字 'UPDATE conversation_message SET tool_request_message_id=id WHERE id=%s'，完成上面尚未结束的表达式。
        "UPDATE conversation_message SET tool_request_message_id=id WHERE id=%s",
        # 继续传入或组合表达式 (tool_result,) 的结果，完成上面尚未结束的表达式。
        (tool_result,),
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'duplicate_tool_result_rejected'，完成上面尚未结束的表达式。
        "duplicate_tool_result_rejected",
        # 继续传入或组合表达式 """INSERT INTO conversation_message(user_id,conversation_id,turn_id,message_no,role,content 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：向 conversation_message 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：业务工具请求消息编号（tool_request_message_id）、业务工具调用编号（tool_call_id）、对话编号（conversation_id）、本轮对话编号（turn_id）、99、角色（role）、内容（content）。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        """INSERT INTO conversation_message(user_id,conversation_id,turn_id,message_no,role,content,
        tool_request_message_id,tool_call_id,tool_name) SELECT user_id,conversation_id,turn_id,99,role,content,
        tool_request_message_id,tool_call_id,tool_name FROM conversation_message WHERE id=%s""",
        # 继续传入或组合表达式 (tool_result,) 的结果，完成上面尚未结束的表达式。
        (tool_result,),
        # 传入编码（code），值为固定文字 '23505'。
        code="23505",
    )
    # 把执行“调用 message 完成消息处理”得到的结果保存到 another请求（another_request）。
    another_request = message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合秒本轮对话（second_turn），完成上面尚未结束的表达式。
        second_turn,
        # 继续传入或组合固定数值 6，完成上面尚未结束的表达式。
        6,
        # 继续传入或组合固定状态或编码 ASSISTANT（ASSISTANT），完成上面尚未结束的表达式。
        "ASSISTANT",
        # 继续传入或组合固定文字 ''，完成上面尚未结束的表达式。
        "",
        # 传入模型（model），值为固定文字 'review-model'。
        model="review-model",
        # 传入finish原因（finish_reason），值为固定文字 'tool_calls'。
        finish_reason="tool_calls",
        # 传入业务工具调用列表（tool_calls），值为调用列表（calls）。
        tool_calls=calls,
    )
    # 调用 message 完成消息处理。
    message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合秒本轮对话（second_turn），完成上面尚未结束的表达式。
        second_turn,
        # 继续传入或组合固定数值 7，完成上面尚未结束的表达式。
        7,
        # 继续传入或组合固定状态或编码 TOOL（业务工具），完成上面尚未结束的表达式。
        "TOOL",
        # 继续传入或组合固定文字 '{"items":[]}'，完成上面尚未结束的表达式。
        '{"items":[]}',
        # 传入业务工具请求消息编号（tool_request_message_id），值为another请求（another_request）。
        tool_request_message_id=another_request,
        # 传入业务工具调用编号（tool_call_id），值为固定文字 'shared_call'。
        tool_call_id="shared_call",
        # 传入业务工具名称（tool_name），值为固定文字 'growth_get_goals'。
        tool_name="growth_get_goals",
    )
    # 调用 message 完成消息处理。
    message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合秒本轮对话（second_turn），完成上面尚未结束的表达式。
        second_turn,
        # 继续传入或组合固定数值 8，完成上面尚未结束的表达式。
        8,
        # 继续传入或组合固定状态或编码 ASSISTANT（ASSISTANT），完成上面尚未结束的表达式。
        "ASSISTANT",
        # 继续传入或组合固定文字 'Finished'，完成上面尚未结束的表达式。
        "Finished",
        # 传入模型（model），值为固定文字 'review-model'。
        model="review-model",
        # 传入finish原因（finish_reason），值为固定文字 'stop'。
        finish_reason="stop",
        # 传入用量（usage），值为固定文字 '{"totalTokens":10}'。
        usage='{"totalTokens":10}',
    )
    # 调用 finish 完成finish处理。
    finish(cursor, second_turn)
    # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 third本轮对话（third_turn）。
    third_turn = turn(cursor, first)
    # 调用 message 完成消息处理。
    message(cursor, first, third_turn, 9, "USER", "Keep this constraint even if cancelled")
    # 调用 message 完成消息处理。
    message(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合首次（first），完成上面尚未结束的表达式。
        first,
        # 继续传入或组合third本轮对话（third_turn），完成上面尚未结束的表达式。
        third_turn,
        # 继续传入或组合固定数值 10，完成上面尚未结束的表达式。
        10,
        # 继续传入或组合固定状态或编码 ASSISTANT（ASSISTANT），完成上面尚未结束的表达式。
        "ASSISTANT",
        # 继续传入或组合固定文字 'Interrupted answer'，完成上面尚未结束的表达式。
        "Interrupted answer",
        # 传入模型（model），值为固定文字 'review-model'。
        model="review-model",
        # 传入是否完成（is_complete），值为假，表示条件不成立或开关关闭。
        is_complete=False,
    )
    # 调用 finish 完成finish处理。
    finish(cursor, third_turn, "CANCELLED", None, "USER_CANCELLED")
    # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 other本轮对话（other_turn）。
    other_turn = turn(cursor, second, user=1002)
    # 调用 message 完成消息处理。
    message(cursor, second, other_turn, 1, "USER", "Another user", user_id=1002)
    # 调用 finish 完成finish处理。
    finish(cursor, other_turn)
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'same_sequence_allowed_across_conversations'，完成上面尚未结束的表达式。
        "same_sequence_allowed_across_conversations",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(cursor, "SELECT count(*) FROM conversation_message WHERE message_no=1") == 2,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'raw_history_keeps_partial_text'，完成上面尚未结束的表达式。
        "raw_history_keeps_partial_text",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合按表达式计算得到的值：cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)，完成上面尚未结束的表达式。
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        # 继续传入或组合条件判断的结果：== 10，完成上面尚未结束的表达式。
        == 10,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'tool_result_ids_scoped_to_assistant_message'，完成上面尚未结束的表达式。
        "tool_result_ids_scoped_to_assistant_message",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(cursor, "SELECT count(*) FROM conversation_message WHERE tool_call_id='shared_call'")
        # 继续传入或组合条件判断的结果：== 2，完成上面尚未结束的表达式。
        == 2,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'owner_filter_rejects_other_user'，完成上面尚未结束的表达式。
        "owner_filter_rejects_other_user",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(cursor, "SELECT count(*) FROM conversation WHERE id=%s AND user_id=1002", (first,))
        # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
        == 0,
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合固定文字 'SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_…'，完成上面尚未结束的表达式。
        "SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_id=%s ORDER BY message_no DESC LIMIT 3",
        # 继续传入或组合表达式 (first,) 的结果，完成上面尚未结束的表达式。
        (first,),
    )
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 当前页（page）。
    page = [row[0] for row in cursor.fetchall()]
    # 调用 check 完成核对处理。
    check("history_cursor_first_page", page == [10, 9, 8])
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合固定文字 'SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_…'，完成上面尚未结束的表达式。
        "SELECT message_no FROM conversation_message WHERE user_id=1001 AND conversation_id=%s AND message_no<%s ORDER BY message_no DESC LIMIT 3",
        # 继续传入或组合表达式 (first, min(page)) 的结果，完成上面尚未结束的表达式。
        (first, min(page)),
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合取出查询结果中的所有行，完成上面尚未结束的表达式。
        "history_cursor_second_page_no_overlap", [row[0] for row in cursor.fetchall()] == [7, 6, 5]
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合表达式 """UPDATE conversation SET next_message_no=11,last_message_at=%s 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        """UPDATE conversation SET next_message_no=11,last_message_at=%s,
        context_summary='Retain daily time limit and confirmed constraints',summary_through_message_no=8,
        summary_updated_at=%s,version=version+1 WHERE id=%s AND version=0""",
        # 继续传入或组合表达式 (T0, T0, first) 的结果，完成上面尚未结束的表达式。
        (T0, T0, first),
    )
    # 调用 check 完成核对处理。
    check("summary_compare_and_set_first_update", cursor.rowcount == 1)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合表达式 "UPDATE conversation SET title='Stale overwrite' WHERE id=%s AND version=0", (first,) 的结果，完成上面尚未结束的表达式。
        "UPDATE conversation SET title='Stale overwrite' WHERE id=%s AND version=0", (first,)
    )
    # 调用 check 完成核对处理。
    check("stale_summary_or_title_update_rejected", cursor.rowcount == 0)
    # 把执行“取得查询结果中的一个值”得到的结果保存到 上下文（context）。
    context = scalar(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合按表达式计算得到的值："""SELECT count(*) FROM conversation_message m JOIN conversation_turn t，完成上面尚未结束的表达式。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        # 内嵌 SQL：按这里的关联条件连接另一张表的记录。
        # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        # 内嵌 SQL：再增加一个必须同时满足的筛选条件。
        """SELECT count(*) FROM conversation_message m JOIN conversation_turn t
        ON t.id=m.turn_id AND t.user_id=m.user_id AND t.conversation_id=m.conversation_id
        JOIN conversation c ON c.id=m.conversation_id AND c.user_id=m.user_id
        WHERE m.user_id=1001 AND c.id=%s AND c.deleted_at IS NULL
        AND m.message_no>c.summary_through_message_no
        AND (m.role='USER' OR (m.is_complete AND t.status='COMPLETED'))""",
        # 继续传入或组合表达式 (first,) 的结果，完成上面尚未结束的表达式。
        (first,),
    )
    # 调用 check 完成核对处理。
    check("context_keeps_cancelled_user_but_not_partial_answer", context == 1)
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'summary_does_not_delete_messages'，完成上面尚未结束的表达式。
        "summary_does_not_delete_messages",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合按表达式计算得到的值：cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)，完成上面尚未结束的表达式。
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        # 继续传入或组合条件判断的结果：== 10，完成上面尚未结束的表达式。
        == 10,
    )
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合固定文字 "UPDATE conversation SET status='CLOSED',closed_at=created_at,deleted_at=created_…"，完成上面尚未结束的表达式。
        "UPDATE conversation SET status='CLOSED',closed_at=created_at,deleted_at=created_at,version=version+1 WHERE id=%s",
        # 继续传入或组合表达式 (first,) 的结果，完成上面尚未结束的表达式。
        (first,),
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'logical_delete_hides_conversation'，完成上面尚未结束的表达式。
        "logical_delete_hides_conversation",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合按表达式计算得到的值：cursor, "SELECT count(*) FROM conversation WHERE id=%s AND deleted_at IS NULL", (first,)，完成上面尚未结束的表达式。
            cursor, "SELECT count(*) FROM conversation WHERE id=%s AND deleted_at IS NULL", (first,)
        )
        # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
        == 0,
    )
    # 调用 check 完成核对处理。
    check(
        # 继续传入或组合固定文字 'logical_delete_retains_history'，完成上面尚未结束的表达式。
        "logical_delete_retains_history",
        # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
        scalar(
            # 继续传入或组合按表达式计算得到的值：cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)，完成上面尚未结束的表达式。
            cursor, "SELECT count(*) FROM conversation_message WHERE conversation_id=%s", (first,)
        )
        # 继续传入或组合条件判断的结果：== 10，完成上面尚未结束的表达式。
        == 10,
    )
    # 把执行“调用 conversation 完成对话处理”得到的结果保存到 已过期对话（expired_conversation）。
    expired_conversation = conversation(cursor)
    # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 已过期本轮对话（expired_turn）。
    expired_turn = turn(cursor, expired_conversation)
    # 执行传入的数据库语句并取得结果。
    cursor.execute(
        # 继续传入或组合表达式 """UPDATE conversation_turn SET status='FAILED',error_code='AGENT_TIMEOUT' 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
        # 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
        # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        """UPDATE conversation_turn SET status='FAILED',error_code='AGENT_TIMEOUT',
        finished_at=clock_timestamp(),updated_at=clock_timestamp(),version=version+1
        WHERE id=%s AND status='RUNNING' AND deadline_at<=clock_timestamp()""",
        # 继续传入或组合表达式 (expired_turn,) 的结果，完成上面尚未结束的表达式。
        (expired_turn,),
    )
    # 调用 check 完成核对处理。
    check("expired_turn_can_be_normalized", cursor.rowcount == 1)
    # 调用 check 完成核对处理。
    check("new_turn_allowed_after_timeout", turn(cursor, expired_conversation) > expired_turn)
    # 把执行“取得查询结果中的一个值”得到的结果保存到 旧请求（old_request）。
    old_request = scalar(
        # 继续传入或组合表达式 cursor, "SELECT request_id::text FROM conversation_turn WHERE id=%s", (expired_turn,) 的结果，完成上面尚未结束的表达式。
        cursor, "SELECT request_id::text FROM conversation_turn WHERE id=%s", (expired_turn,)
    )
    # 调用 reject 完成拒绝处理。
    reject(
        # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
        cursor,
        # 继续传入或组合固定文字 'old_request_remains_idempotent_after_timeout'，完成上面尚未结束的表达式。
        "old_request_remains_idempotent_after_timeout",
        # 继续传入或组合表达式 """INSERT INTO conversation_turn(user_id,conversation_id 的结果，完成上面尚未结束的表达式。
        # 内嵌 SQL：向 conversation_turn 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：请求编号（request_id）、请求摘要值（request_hash）、状态（status）、finish原因（finish_reason）、开始时at（started_at）、截止时间at（deadline_at）、finishedat（finished_at）、创建时at（created_at）。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        """INSERT INTO conversation_turn(user_id,conversation_id,
        request_id,request_hash,status,finish_reason,started_at,deadline_at,finished_at,created_at,updated_at)
        VALUES (1001,%s,%s,%s,'COMPLETED','stop',%s,%s::timestamptz+interval '120 seconds',%s,%s,%s)""",
        # 继续传入或组合按表达式计算得到的值：(second, old_request, "c" * 64, T0, T0, T0, T0, T0)，完成上面尚未结束的表达式。
        (second, old_request, "c" * 64, T0, T0, T0, T0, T0),
        # 传入编码（code），值为固定文字 '23505'。
        code="23505",
    )


# 函数 run_rollback_review：调用 run_rollback_review 完成方案运行rollback复盘处理；接收 来源（source）、账户管理来源（identity_source）。
def run_rollback_review(source, identity_source):
    # 把执行“调用 uuid.uuid4 完成uuid4处理”得到的结果的hex属性保存到 后缀（suffix）。
    suffix = uuid.uuid4().hex
    # 把依次包含 按 'agent_review_ident_' + suffix 计算的结果、按 'agent_review_fact_' + suffix 计算的结果 的集合保存到 账户管理数据库分区fact数据库分区（(identity_schema, fact_schema)）。
    identity_schema, fact_schema = "agent_review_ident_" + suffix, "agent_review_fact_" + suffix
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = connect()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 把执行“取得查询结果中的一个值”得到的结果保存到 版本号（version）。
            version = scalar(cursor, "SHOW server_version")
            # 把执行“调用 initialize 完成初始化处理”得到的结果保存到 transformed。
            transformed = initialize(cursor, source, identity_source, identity_schema, fact_schema)
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'exactly_three_agent_tables'，完成上面尚未结束的表达式。
                "exactly_three_agent_tables",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合固定文字 "SELECT count(*) FROM pg_class WHERE relnamespace=%s::regnamespace AND relkind='r…"，完成上面尚未结束的表达式。
                    "SELECT count(*) FROM pg_class WHERE relnamespace=%s::regnamespace AND relkind='r'",
                    # 继续传入或组合表达式 (fact_schema,) 的结果，完成上面尚未结束的表达式。
                    (fact_schema,),
                )
                # 继续传入或组合条件判断的结果：== 3，完成上面尚未结束的表达式。
                == 3,
            )
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'no_physical_foreign_keys'，完成上面尚未结束的表达式。
                "no_physical_foreign_keys",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合固定文字 'SELECT count(*) FROM pg_constraint WHERE connamespace=%s::regnamespace AND conty…'，完成上面尚未结束的表达式。
                    "SELECT count(*) FROM pg_constraint WHERE connamespace=%s::regnamespace AND contype='f'",
                    # 继续传入或组合表达式 (fact_schema,) 的结果，完成上面尚未结束的表达式。
                    (fact_schema,),
                )
                # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
                == 0,
            )
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'all_timestamps_use_milliseconds'，完成上面尚未结束的表达式。
                "all_timestamps_use_milliseconds",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合表达式 """SELECT bool_and(datetime_precision=3) 的结果，完成上面尚未结束的表达式。
                    # 内嵌 SQL：读取后面列出的字段或计算结果。
                    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
                    """SELECT bool_and(datetime_precision=3)
                FROM information_schema.columns WHERE table_schema=%s AND data_type='timestamp with time zone'""",
                    # 继续传入或组合表达式 (fact_schema,) 的结果，完成上面尚未结束的表达式。
                    (fact_schema,),
                ),
            )
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'schema_not_granted_to_public'，完成上面尚未结束的表达式。
                "schema_not_granted_to_public",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合表达式 """SELECT NOT EXISTS(SELECT 1 FROM pg_namespace n 的结果，完成上面尚未结束的表达式。
                    # 内嵌 SQL：读取后面列出的字段或计算结果。
                    # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：LATERAL aclexplode(COALESCE(n.nspacl,acldefault('n',n.nspowner))) a。
                    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
                    """SELECT NOT EXISTS(SELECT 1 FROM pg_namespace n,
                LATERAL aclexplode(COALESCE(n.nspacl,acldefault('n',n.nspowner))) a
                WHERE n.nspname=%s AND a.grantee=0)""",
                    # 继续传入或组合表达式 (fact_schema,) 的结果，完成上面尚未结束的表达式。
                    (fact_schema,),
                ),
            )
            # 调用 authorization_review 完成授权复盘处理。
            authorization_review(cursor, transformed)
            # 调用 constraints_review 完成constraints复盘处理。
            constraints_review(cursor)
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'ddl_fixtures_and_metadata_rolled_back'，完成上面尚未结束的表达式。
                "ddl_fixtures_and_metadata_rolled_back",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合固定文字 'SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)'，完成上面尚未结束的表达式。
                    "SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)",
                    # 继续传入或组合表达式 (identity_schema, fact_schema) 的结果，完成上面尚未结束的表达式。
                    (identity_schema, fact_schema),
                )
                # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
                == 0,
            )
        # 把版本号（version）交还给调用方，本函数到此结束。
        return version
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 释放连接、文件或客户端占用的资源。
        connection.close()


# 函数 race：确认 B 确实等待 A 的事务锁，再提交 A，验证数据库的并发约束。；接收 名称（name）、fact数据库分区（fact_schema）、statementa（statement_a）、参数集合a（params_a）、statementb（statement_b）、参数集合b（params_b）、预计编码（expected_code）、约束（constraint）。
def race(
    # 继续传入或组合名称（name），完成上面尚未结束的表达式。
    name,
    # 继续传入或组合fact数据库分区（fact_schema），完成上面尚未结束的表达式。
    fact_schema,
    # 继续传入或组合statementa（statement_a），完成上面尚未结束的表达式。
    statement_a,
    # 继续传入或组合参数集合a（params_a），完成上面尚未结束的表达式。
    params_a,
    # 继续传入或组合statementb（statement_b），完成上面尚未结束的表达式。
    statement_b,
    # 继续传入或组合参数集合b（params_b），完成上面尚未结束的表达式。
    params_b,
    # 传入预计编码（expected_code），值为空值，表示当前还没有对应内容。
    expected_code=None,
    # 传入约束（constraint），值为空值，表示当前还没有对应内容。
    constraint=None,
):
    # 确认 B 确实等待 A 的事务锁，再提交 A，验证数据库的并发约束。
    # 把依次包含 执行“调用 Event 完成事件处理”得到的结果、执行“调用 Event 完成事件处理”得到的结果、执行“调用 Event 完成事件处理”得到的结果 的集合保存到 已就绪a已就绪brelease（(ready_a, ready_b, release)）。
    ready_a, ready_b, release = Event(), Event(), Event()
    # 把按字段名保存多个值的字典，这里先创建空字典保存到 pids。
    pids = {}

    # 函数 worker：调用 worker 完成后台处理器处理；接收 显示名称（label）、statement、parameters。
    def worker(label, statement, parameters):
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
        connection = connect()
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
            with connection.cursor() as cursor:
                # 执行传入的数据库语句并取得结果。
                cursor.execute(
                    # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
                    sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                        # 继续传入或组合调用 sql.Identifier 完成Identifier处理，完成上面尚未结束的表达式。
                        sql.Identifier(fact_schema)
                    )
                )
                # 把执行“读取 backendpid（_backend_pid） 的值”得到的结果保存到 pids显示名称（pids[label]）。
                pids[label] = connection.get_backend_pid()
                # 检查条件 label == 'b' 是否成立；成立时执行缩进的分支。
                if label == "b":
                    # 检查条件 not ready_a.wait(4) 是否成立；成立时执行缩进的分支。
                    if not ready_a.wait(4):
                        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                        raise RuntimeError("First SQL transaction did not become ready")
                    # 调用 ready_b.set 完成设置处理。
                    ready_b.set()
                # 执行传入的数据库语句并取得结果。
                cursor.execute(statement, parameters)
                # 把条件 cursor.description 是否成立时使用执行“取出查询结果中的所有行”得到的结果，否则使用空列表或集合保存到 记录列表（rows）。
                rows = cursor.fetchall() if cursor.description else []
                # 检查条件 label == 'a' 是否成立；成立时执行缩进的分支。
                if label == "a":
                    # 调用 ready_a.set 完成设置处理。
                    ready_a.set()
                    # 检查条件 not release.wait(5) 是否成立；成立时执行缩进的分支。
                    if not release.wait(5):
                        # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                        raise RuntimeError("SQL race release timed out")
            # 提交本次数据库事务，使本次修改一起生效。
            connection.commit()
            # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
            return {"code": None, "rows": rows, "constraint": None}
        # 捕获psycopg2.Error异常，执行下面的错误处理，异常对象保存为 error。
        except psycopg2.Error as error:
            # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
            connection.rollback()
            # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
            return {"code": error.pgcode, "rows": [], "constraint": error.diag.constraint_name}
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 释放连接、文件或客户端占用的资源。
            connection.close()

    # 把执行“调用 connect 完成连接处理”得到的结果保存到 observer。
    observer = connect()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 ThreadPoolExecutor 完成Thread连接池Executor处理”得到的结果，命名为 executor；退出时由上下文管理器提交、回滚或释放资源。
        with ThreadPoolExecutor(max_workers=2, thread_name_prefix="agent-sql-race") as executor:
            # 把执行“调用 executor.submit 完成提交处理”得到的结果保存到 首次（first）。
            first = executor.submit(worker, "a", statement_a, params_a)
            # 把执行“调用 executor.submit 完成提交处理”得到的结果保存到 秒（second）。
            second = executor.submit(worker, "b", statement_b, params_b)
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 检查条件 not ready_b.wait(4) 是否成立；成立时执行缩进的分支。
                if not ready_b.wait(4):
                    # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
                    raise RuntimeError("Second SQL transaction did not become ready")
                # 把假，表示条件不成立或开关关闭保存到 blocked。
                blocked = False
                # 把按 time.monotonic() + 3 计算的结果保存到 until。
                until = time.monotonic() + 3
                # 只要条件 time.monotonic() < until 是否成立，就继续执行下面的循环。
                while time.monotonic() < until:
                    # 进入执行“调用 observer.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
                    with observer.cursor() as cursor:
                        # 执行传入的数据库语句并取得结果。
                        cursor.execute(
                            # 继续传入或组合表达式 "SELECT %s=ANY(pg_blocking_pids(%s))", (pids["a"], pids["b"]) 的结果，完成上面尚未结束的表达式。
                            "SELECT %s=ANY(pg_blocking_pids(%s))", (pids["a"], pids["b"])
                        )
                        # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 blocked。
                        blocked = cursor.fetchone()[0]
                    # 检查条件 blocked 是否成立；成立时执行缩进的分支。
                    if blocked:
                        # 立即退出当前循环，不再处理后续条目。
                        break
                    # 等待指定时间后再继续，给轮询或其他任务留出执行机会。
                    time.sleep(0.03)
                # 调用 check 完成核对处理。
                check(name + "_actually_blocked", blocked)
            # 不论前面成功、失败还是取消，都执行下面的资源清理。
            finally:
                # 调用 release.set 完成设置处理。
                release.set()
            # 把依次包含 执行“调用 first.result 完成结果处理”得到的结果、执行“调用 second.result 完成结果处理”得到的结果 的集合保存到 结果a结果b（(result_a, result_b)）。
            result_a, result_b = first.result(timeout=8), second.result(timeout=8)
            # 调用 check 完成核对处理。
            check(name + "_first_committed", result_a["code"] is None)
            # 调用 check 完成核对处理。
            check(name + "_second_result", result_b["code"] == expected_code)
            # 检查条件 constraint 是否成立；成立时执行缩进的分支。
            if constraint:
                # 调用 check 完成核对处理。
                check(name + "_correct_constraint", result_b["constraint"] == constraint)
            # 把依次包含 从结果a（result_a）中按固定文字 'rows'取出的内容、从结果b（result_b）中按固定文字 'rows'取出的内容 的集合交还给调用方，本函数到此结束。
            return result_a["rows"], result_b["rows"]
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        observer.rollback()
        # 释放连接、文件或客户端占用的资源。
        observer.close()


# 函数 cleanup_own_schemas：调用 cleanup_own_schemas 完成cleanupownschemas处理；接收 账户管理数据库分区（identity_schema）、fact数据库分区（fact_schema）。
def cleanup_own_schemas(identity_schema, fact_schema):
    # 从依次包含 账户管理数据库分区（identity_schema）、fact数据库分区（fact_schema） 的集合依次取出名称（name），每项执行一次下面的处理。
    for name in (identity_schema, fact_schema):
        # 检查条件 not re.fullmatch('agent_review_(?:ident|fact)_[0-9a-f]{32}', name) 是否成立；成立时执行缩进的分支。
        if not re.fullmatch(r"agent_review_(?:ident|fact)_[0-9a-f]{32}", name):
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("Refusing to remove a non-review schema")
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = connect()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 从依次包含 fact数据库分区（fact_schema）、账户管理数据库分区（identity_schema） 的集合依次取出名称（name），每项执行一次下面的处理。
            for name in (fact_schema, identity_schema):
                # 执行传入的数据库语句并取得结果。
                cursor.execute(
                    # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
                    sql.SQL("DROP SCHEMA IF EXISTS {} CASCADE").format(sql.Identifier(name))
                )
        # 提交本次数据库事务，使本次修改一起生效。
        connection.commit()
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'concurrent_review_schemas_removed'，完成上面尚未结束的表达式。
                "concurrent_review_schemas_removed",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合数据库语句执行器（cursor），完成上面尚未结束的表达式。
                    cursor,
                    # 继续传入或组合固定文字 'SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)'，完成上面尚未结束的表达式。
                    "SELECT count(*) FROM pg_namespace WHERE nspname IN (%s,%s)",
                    # 继续传入或组合表达式 (identity_schema, fact_schema) 的结果，完成上面尚未结束的表达式。
                    (identity_schema, fact_schema),
                )
                # 继续传入或组合条件判断的结果：== 0，完成上面尚未结束的表达式。
                == 0,
            )
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        connection.rollback()
        # 释放连接、文件或客户端占用的资源。
        connection.close()


# 函数 concurrent_review：调用 concurrent_review 完成concurrent复盘处理；接收 来源（source）、账户管理来源（identity_source）。
def concurrent_review(source, identity_source):
    # 把执行“调用 uuid.uuid4 完成uuid4处理”得到的结果的hex属性保存到 后缀（suffix）。
    suffix = uuid.uuid4().hex
    # 把依次包含 按 'agent_review_ident_' + suffix 计算的结果、按 'agent_review_fact_' + suffix 计算的结果 的集合保存到 账户管理数据库分区fact数据库分区（(identity_schema, fact_schema)）。
    identity_schema, fact_schema = "agent_review_ident_" + suffix, "agent_review_fact_" + suffix
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 setup。
    setup = connect()
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 进入执行“调用 setup.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with setup.cursor() as cursor:
            # 调用 initialize 完成初始化处理。
            initialize(cursor, source, identity_source, identity_schema, fact_schema)
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 是否展开编号请求a请求ballocation编号（(open_id, request_a, request_b, allocation_id)）。
            open_id, request_a, request_b, allocation_id = [conversation(cursor) for _ in range(4)]
            # 把执行“调用 conversation 完成对话处理”得到的结果保存到 another用户（another_user）。
            another_user = conversation(cursor, user=1002)
            # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 首次本轮对话（first_turn）。
            first_turn = turn(cursor, allocation_id)
            # 调用 finish 完成finish处理。
            finish(cursor, first_turn)
            # 把执行“调用 turn 完成本轮对话处理”得到的结果保存到 秒本轮对话（second_turn）。
            second_turn = turn(cursor, allocation_id)
            # 调用 finish 完成finish处理。
            finish(cursor, second_turn)
        # 提交本次数据库事务，使本次修改一起生效。
        setup.commit()
        # 把按表达式计算得到的值："INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash,\n created_at,updated_at,started_at,deadl…保存到 写入新记录本轮对话（insert_turn）。
        # 内嵌 SQL：向 conversation_turn 写入新记录，字段顺序与后面的值一一对应。
        # 内嵌 SQL：继续列出本次使用的字段：创建时at（created_at）、更新时at（updated_at）、开始时at（started_at）。
        # 内嵌 SQL：按 INSERT 声明的字段顺序提供要写入的值。
        insert_turn = """INSERT INTO conversation_turn(user_id,conversation_id,request_id,request_hash,
            created_at,updated_at,started_at,deadline_at)
            VALUES (%s,%s,%s,%s,%s,%s,%s,%s::timestamptz+interval '120 seconds') RETURNING id"""

        # 函数 params：调用 params 完成参数集合处理；接收 对话编号（conversation_id）、请求编号（request_id）、用户（user）。
        def params(conversation_id, request_id, user=1001):
            # 把依次包含 用户（user）、对话编号（conversation_id）、执行“调用 str 完成str处理”得到的结果、按 'b' * 64 计算的结果、T0等内容 的集合交还给调用方，本函数到此结束。
            return (user, conversation_id, str(request_id), "b" * 64, T0, T0, T0, T0)

        # 调用 race 完成race处理。
        race(
            # 继续传入或组合固定文字 'one_running_turn_per_conversation'，完成上面尚未结束的表达式。
            "one_running_turn_per_conversation",
            # 继续传入或组合fact数据库分区（fact_schema），完成上面尚未结束的表达式。
            fact_schema,
            # 继续传入或组合写入新记录本轮对话（insert_turn），完成上面尚未结束的表达式。
            insert_turn,
            # 继续传入或组合调用 params(open_id, uuid.uuid4 完成uuid4处理，完成上面尚未结束的表达式。
            params(open_id, uuid.uuid4()),
            # 继续传入或组合写入新记录本轮对话（insert_turn），完成上面尚未结束的表达式。
            insert_turn,
            # 继续传入或组合调用 params(open_id, uuid.uuid4 完成uuid4处理，完成上面尚未结束的表达式。
            params(open_id, uuid.uuid4()),
            # 传入预计编码（expected_code），值为固定文字 '23505'。
            expected_code="23505",
            # 传入约束（constraint），值为固定文字 'uk_agent_turn_running_conversation'。
            constraint="uk_agent_turn_running_conversation",
        )
        # 把执行“调用 uuid.uuid4 完成uuid4处理”得到的结果保存到 请求编号（request_id）。
        request_id = uuid.uuid4()
        # 调用 race 完成race处理。
        race(
            # 继续传入或组合固定文字 'idempotent_request_per_user'，完成上面尚未结束的表达式。
            "idempotent_request_per_user",
            # 继续传入或组合fact数据库分区（fact_schema），完成上面尚未结束的表达式。
            fact_schema,
            # 继续传入或组合写入新记录本轮对话（insert_turn），完成上面尚未结束的表达式。
            insert_turn,
            # 继续传入或组合调用 params 完成参数集合处理，完成上面尚未结束的表达式。
            params(request_a, request_id),
            # 继续传入或组合写入新记录本轮对话（insert_turn），完成上面尚未结束的表达式。
            insert_turn,
            # 继续传入或组合调用 params 完成参数集合处理，完成上面尚未结束的表达式。
            params(request_b, request_id),
            # 传入预计编码（expected_code），值为固定文字 '23505'。
            expected_code="23505",
            # 传入约束（constraint），值为固定文字 'uk_agent_turn_user_request'。
            constraint="uk_agent_turn_user_request",
        )
        # 进入执行“调用 setup.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with setup.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(
                # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
                sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                    # 继续传入或组合调用 sql.Identifier 完成Identifier处理，完成上面尚未结束的表达式。
                    sql.Identifier(fact_schema)
                )
            )
            # 执行传入的数据库语句并取得结果。
            cursor.execute(insert_turn, params(another_user, request_id, user=1002))
            # 调用 check 完成核对处理。
            check("same_request_id_allowed_for_different_user", cursor.fetchone()[0] > 0)
        # 提交本次数据库事务，使本次修改一起生效。
        setup.commit()
        # 把按表达式计算得到的值："WITH allocated AS (\n UPDATE conversation SET next_message_no=next_message_no+1,version=version+1\n WHERE id=%s RETURNI…保存到 allocate。
        # 内嵌 SQL：先定义可复用的临时查询结果，后续语句继续引用它。
        # 内嵌 SQL：按后续条件修改指定记录；没有被条件选中的记录不变。
        # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
        # 内嵌 SQL：继续列出本次使用的字段：对话编号（conversation_id）、本轮对话编号（turn_id）、消息no（message_no）、角色（role）。
        # 内嵌 SQL：读取后面列出的字段或计算结果。
        allocate = """WITH allocated AS (
            UPDATE conversation SET next_message_no=next_message_no+1,version=version+1
            WHERE id=%s RETURNING id,user_id,next_message_no-1 AS allocated_no
        ) INSERT INTO conversation_message(user_id,conversation_id,turn_id,message_no,role,content)
            SELECT user_id,id,%s,allocated_no,'USER','Concurrent message' FROM allocated RETURNING message_no"""
        # 把执行“调用 race 完成race处理”得到的结果保存到 记录列表a记录列表b（(rows_a, rows_b)）。
        rows_a, rows_b = race(
            # 继续传入或组合固定文字 'message_number_allocation'，完成上面尚未结束的表达式。
            "message_number_allocation",
            # 继续传入或组合fact数据库分区（fact_schema），完成上面尚未结束的表达式。
            fact_schema,
            # 继续传入或组合allocate，完成上面尚未结束的表达式。
            allocate,
            # 继续传入或组合表达式 (allocation_id, first_turn) 的结果，完成上面尚未结束的表达式。
            (allocation_id, first_turn),
            # 继续传入或组合allocate，完成上面尚未结束的表达式。
            allocate,
            # 继续传入或组合表达式 (allocation_id, second_turn) 的结果，完成上面尚未结束的表达式。
            (allocation_id, second_turn),
        )
        # 调用 check 完成核对处理。
        check("concurrent_message_numbers_are_ordered", rows_a == [(1,)] and rows_b == [(2,)])
        # 进入执行“调用 setup.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with setup.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute(
                # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
                sql.SQL("SET LOCAL search_path TO {}, pg_catalog").format(
                    # 继续传入或组合调用 sql.Identifier 完成Identifier处理，完成上面尚未结束的表达式。
                    sql.Identifier(fact_schema)
                )
            )
            # 调用 check 完成核对处理。
            check(
                # 继续传入或组合固定文字 'counter_advanced_exactly_twice'，完成上面尚未结束的表达式。
                "counter_advanced_exactly_twice",
                # 继续传入或组合取得查询结果中的一个值，完成上面尚未结束的表达式。
                scalar(
                    # 继续传入或组合表达式 cursor, "SELECT next_message_no FROM conversation WHERE id=%s", (allocation_id,) 的结果，完成上面尚未结束的表达式。
                    cursor, "SELECT next_message_no FROM conversation WHERE id=%s", (allocation_id,)
                )
                # 继续传入或组合条件判断的结果：== 3，完成上面尚未结束的表达式。
                == 3,
            )
    # 不论前面成功、失败还是取消，都执行下面的资源清理。
    finally:
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        setup.rollback()
        # 释放连接、文件或客户端占用的资源。
        setup.close()
        # 调用 cleanup_own_schemas 完成cleanupownschemas处理。
        cleanup_own_schemas(identity_schema, fact_schema)


# 函数 main：调用 main 完成程序入口处理。
def main():
    # 把执行“按指定编码读取文件文字”得到的结果保存到 来源（source）。
    source = SQL_PATH.read_text(encoding="utf-8-sig")
    # 把执行“按指定编码读取文件文字”得到的结果保存到 账户管理来源（identity_source）。
    identity_source = IDENTITY_PATH.read_text(encoding="utf-8-sig")
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 hashes。
    hashes = {
        # 继续传入或组合内容的 SHA-256 摘要，用于核对内容或构造幂等键，完成上面尚未结束的表达式。
        str(path.relative_to(ROOT)).replace("\\", "/"): hashlib.sha256(
            # 继续传入或组合读取文件原始字节，完成上面尚未结束的表达式。
            path.read_bytes()
        # 继续传入或组合调用 ).hexdigest 完成hexdigest处理，完成上面尚未结束的表达式。
        ).hexdigest()
        # 继续传入或组合表达式 for path in (SQL_PATH, IDENTITY_PATH) 的结果，完成上面尚未结束的表达式。
        for path in (SQL_PATH, IDENTITY_PATH)
    }
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 把执行“调用 run_rollback_review 完成方案运行rollback复盘处理”得到的结果保存到 版本号（version）。
    version = run_rollback_review(source, identity_source)
    # 把依次包含 按字段名保存多个值的字典 的集合保存到 分组列表（groups）。
    groups = [
        # 继续传入或组合按表达式计算得到的值：{"name": "ddl_constraints_permissions_history_and_summary", "passed": len(CASES) - before}，完成上面尚未结束的表达式。
        {"name": "ddl_constraints_permissions_history_and_summary", "passed": len(CASES) - before}
    ]
    # 调用 print 完成print处理。
    print(
        # 继续传入或组合固定文字 'Agent SQL DDL, constraints, history queries and staged authorization passed; tra…'，完成上面尚未结束的表达式。
        "Agent SQL DDL, constraints, history queries and staged authorization passed; transaction rolled back.",
        # 传入发送缓冲内容（flush），值为真，表示条件成立或开关打开。
        flush=True,
    )
    # 把执行“调用 len 完成len处理”得到的结果保存到 变化前（before）。
    before = len(CASES)
    # 调用 concurrent_review 完成concurrent复盘处理。
    concurrent_review(source, identity_source)
    # 把新条目放到列表末尾。
    groups.append({"name": "real_database_concurrency", "passed": len(CASES) - before})
    # 从执行“调用 hashes.items 完成数据列表处理”得到的结果依次取出路径digest（(path, digest)），每项执行一次下面的处理。
    for path, digest in hashes.items():
        # 检查条件 hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest 是否成立；成立时执行缩进的分支。
        if hashlib.sha256((ROOT / path).read_bytes()).hexdigest() != digest:
            # 中止本轮正常处理，抛出 执行“调用 RuntimeError 完成Runtime错误处理”得到的结果。
            raise RuntimeError("SQL changed during review")
    # 把按字段名保存多个值的字典保存到 复盘报告（report）。
    report = {
        # 填写当前步骤（phase）字段，值为固定文字 'P1.2_SQL_ONLY'。
        "phase": "P1.2_SQL_ONLY",
        # 填写状态（status）字段，值为固定状态或编码 PASSED（PASSED）。
        "status": "PASSED",
        # 填写用户Acceptance（userAcceptance）字段，值为固定状态或编码 PENDING（等待处理）。
        "userAcceptance": "PENDING",
        # 填写verifiedAt字段，值为当前真实时间，用于记录操作发生的时刻。
        "verifiedAt": datetime.now(ZoneInfo("Asia/Shanghai")).isoformat(),
        # 填写数据库（database）字段，值为数据库（DATABASE）。
        "database": DATABASE,
        # 填写postgresql版本号（postgresqlVersion）字段，值为版本号（version）。
        "postgresqlVersion": version,
        # 填写新数据库分区（newSchema）字段，值为固定文字 'agent'。
        "newSchema": "agent",
        # 填写新Tables（newTables）字段，值为表达式 [ 的结果。
        "newTables": [
            # 继续传入或组合固定文字 'agent.conversation'，完成上面尚未结束的表达式。
            "agent.conversation",
            # 继续传入或组合固定文字 'agent.conversation_turn'，完成上面尚未结束的表达式。
            "agent.conversation_turn",
            # 继续传入或组合固定文字 'agent.conversation_message'，完成上面尚未结束的表达式。
            "agent.conversation_message",
        ],
        # 填写checks字段，值为集合或文字的长度。
        "checks": len(CASES),
        # 填写passed字段，值为集合或文字的长度。
        "passed": len(CASES),
        # 填写复盘分组列表（reviewGroups）字段，值为分组列表（groups）。
        "reviewGroups": groups,
        # 填写cases字段，值为CASES。
        "cases": CASES,
        # 填写输入Sha256（inputSha256）字段，值为hashes。
        "inputSha256": hashes,
        # 填写strictTestFrameworkUsed字段，值为假，表示条件不成立或开关关闭。
        "strictTestFrameworkUsed": False,
        # 填写rollbackSchemasRemoved字段，值为真，表示条件成立或开关打开。
        "rollbackSchemasRemoved": True,
        # 填写concurrentSchemasRemoved字段，值为真，表示条件成立或开关打开。
        "concurrentSchemasRemoved": True,
        # 填写concurrentCommits仅InsideOwnRandom复盘Schemas（concurrentCommitsOnlyInsideOwnRandomReviewSchemas）字段，值为真，表示条件成立或开关打开。
        "concurrentCommitsOnlyInsideOwnRandomReviewSchemas": True,
        # 填写development数据库Connected（developmentDatabaseConnected）字段，值为假，表示条件不成立或开关关闭。
        "developmentDatabaseConnected": False,
        # 填写development数据库已修改（developmentDatabaseChanged）字段，值为假，表示条件不成立或开关关闭。
        "developmentDatabaseChanged": False,
        # 填写backendOrFrontendImplemented字段，值为假，表示条件不成立或开关关闭。
        "backendOrFrontendImplemented": False,
        # 填写业务服务OrHTTP请求Acceptance（serviceOrHttpAcceptance）字段，值为固定状态或编码 NOT_VERIFIED（NOTVERIFIED）。
        "serviceOrHttpAcceptance": "NOT_VERIFIED",
    }
    # 把按 ROOT / 'docs/agent/P1.2-sql-check-results.json' 计算的结果保存到 输出（output）。
    output = ROOT / "docs/agent/P1.2-sql-check-results.json"
    # 调用 output.parent.mkdir 完成mkdir处理。
    output.parent.mkdir(parents=True, exist_ok=True)
    # 按指定编码写入文件文字。
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # 调用 print 完成print处理。
    print(
        # 继续传入或组合条件判断的结果：f"Agent SQL checks passed: {len(CASES)}/{len(CASES)}; own schemas removed; development database not connected."，完成上面尚未结束的表达式。
        f"Agent SQL checks passed: {len(CASES)}/{len(CASES)}; own schemas removed; development database not connected."
    )


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == "__main__":
    # 调用 main 完成程序入口处理。
    main()
