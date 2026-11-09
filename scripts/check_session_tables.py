# 实现核对会话tables处理；下面逐项说明输入、分支与调用的作用。
# 保留命令行帮助原先使用的说明文字；它是普通变量赋值，不是文档注释。
__doc__ = '只读检查：Spring Session 表是否存在、在哪个 schema，以及 Flyway 已用版本。\n\n不执行任何写操作、不建表、不改数据。\n'
# 只读检查：Spring Session 表是否存在、在哪个 schema，以及 Flyway 已用版本。
#
# 不执行任何写操作、不建表、不改数据。

# 引入 argparse，供下面调用其功能。
import argparse

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 引入 sys，供下面调用其功能。
import sys

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 函数 connection_parameters：调用 connection_parameters 完成数据库连接parameters处理。
def connection_parameters():
    # 把执行“调用 argparse.ArgumentParser 完成命令行参数读取器处理”得到的结果保存到 parser。
    parser = argparse.ArgumentParser(description=__doc__)
    # 声明一个命令行参数的名称、类型和限制。
    parser.add_argument('--connection-file', required=True, type=Path,
                        # 传入help，值为表达式 'Private JSON connection file; keep it outside version control.') 的结果。
                        help='Private JSON connection file; keep it outside version control.')
    # 把执行“读取命令行参数并校验必填项”得到的结果保存到 参数（args）。
    args = parser.parse_args()
    # 把执行“把 JSON 文字解析为对象”得到的结果保存到 数据（data）。
    data = json.loads(args.connection_file.read_text(encoding='utf-8'))
    # 检查条件 not isinstance(data, dict) or any((not data.get(key) for key in ('host', 'dbname', 'user', 'password'))) 是否成立；成立时执行缩进的分支。
    if not isinstance(data, dict) or any(not data.get(key) for key in ('host', 'dbname', 'user', 'password')):
        # 调用 parser.error 完成错误处理。
        parser.error('The private file must define host, dbname, user and password.')
    # 把数据（data）交还给调用方，本函数到此结束。
    return data

# 把按字段名保存多个值的字典保存到 QUERIES。
QUERIES = {
    # 填写会话表字段，值为固定文字 '"'。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按列出的字段排序，ASC 为升序，DESC 为降序。
    "会话表": """
        SELECT table_schema, table_name
        FROM information_schema.tables
        WHERE lower(table_name) IN ('spring_session', 'spring_session_attributes')
        ORDER BY table_schema, table_name;
    """,
    # 继续传入或组合固定文字 'session_store 内所有对象": ""'，完成上面尚未结束的表达式。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按列出的字段排序，ASC 为升序，DESC 为降序。
    "session_store 内所有对象": """
        SELECT table_name, table_type
        FROM information_schema.tables
        WHERE table_schema = 'session_store'
        ORDER BY table_name;
    """,
    # 继续传入或组合固定文字 'public 内所有表": ""'，完成上面尚未结束的表达式。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按列出的字段排序，ASC 为升序，DESC 为降序。
    "public 内所有表": """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        ORDER BY table_name;
    """,
    # 继续传入或组合固定文字 '当前 search_path": "SHOW search_path;'，完成上面尚未结束的表达式。
    "当前 search_path": "SHOW search_path;",
    # 继续传入或组合固定文字 'Flyway 已用版本(migration_meta)": ""'，完成上面尚未结束的表达式。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：按列出的字段排序，ASC 为升序，DESC 为降序。
    "Flyway 已用版本(migration_meta)": """
        SELECT version, description, success, installed_on
        FROM migration_meta.flyway_schema_history
        ORDER BY installed_rank;
    """,
    # 继续传入或组合固定文字 'app_user 列": ""'，完成上面尚未结束的表达式。
    # 内嵌 SQL：读取后面列出的字段或计算结果。
    # 内嵌 SQL：指定本次查询主要读取的表或子查询。
    # 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
    # 内嵌 SQL：按列出的字段排序，ASC 为升序，DESC 为降序。
    "app_user 列": """
        SELECT column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_schema = 'app' AND table_name = 'app_user'
        ORDER BY ordinal_position;
    """,
}


# 函数 main：调用 main 完成程序入口处理。
def main():
    # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
    try:
        # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 conn。
        conn = psycopg2.connect(**connection_parameters())
    # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
    except Exception as error:
        # 调用 print 完成print处理。
        print("连接失败，请检查私密配置、网络和数据库状态。")
        # 调用 sys.exit 完成exit处理。
        sys.exit(1)

    # 设置 当前会话（_session） 的值。
    conn.set_session(readonly=True, autocommit=True)

    # 进入执行“调用 conn.cursor 完成数据库语句执行器处理”得到的结果，命名为 cur；退出时由上下文管理器提交、回滚或释放资源。
    with conn.cursor() as cur:
        # 从执行“调用 QUERIES.items 完成数据列表处理”得到的结果依次取出显示名称sql（(label, sql)），每项执行一次下面的处理。
        for label, sql in QUERIES.items():
            # 调用 print 完成print处理。
            print("\n=== %s ===" % label)
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 执行传入的数据库语句并取得结果。
                cur.execute(sql)
                # 把执行“取出查询结果中的所有行”得到的结果保存到 记录列表（rows）。
                rows = cur.fetchall()
                # 检查条件 not rows 是否成立；成立时执行缩进的分支。
                if not rows:
                    # 调用 print 完成print处理。
                    print("(无结果)")
                # 从记录列表（rows）依次取出当前记录（row），每项执行一次下面的处理。
                for row in rows:
                    # 调用 print 完成print处理。
                    print(row)
            # 捕获Exception异常，执行下面的错误处理，异常对象保存为 error。
            except Exception as error:
                # 调用 print 完成print处理。
                print("查询失败：%s" % error)

    # 释放连接、文件或客户端占用的资源。
    conn.close()


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == "__main__":
    # 调用 main 完成程序入口处理。
    main()
