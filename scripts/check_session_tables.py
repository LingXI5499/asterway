"""只读检查：Spring Session 表是否存在、在哪个 schema，以及 Flyway 已用版本。

不执行任何写操作、不建表、不改数据。
"""
import argparse
import json
from pathlib import Path
import sys

import psycopg2

def connection_parameters():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--connection-file', required=True, type=Path,
                        help='Private JSON connection file; keep it outside version control.')
    args = parser.parse_args()
    data = json.loads(args.connection_file.read_text(encoding='utf-8'))
    if not isinstance(data, dict) or any(not data.get(key) for key in ('host', 'dbname', 'user', 'password')):
        parser.error('The private file must define host, dbname, user and password.')
    return data

QUERIES = {
    "会话表": """
        SELECT table_schema, table_name
        FROM information_schema.tables
        WHERE lower(table_name) IN ('spring_session', 'spring_session_attributes')
        ORDER BY table_schema, table_name;
    """,
    "session_store 内所有对象": """
        SELECT table_name, table_type
        FROM information_schema.tables
        WHERE table_schema = 'session_store'
        ORDER BY table_name;
    """,
    "public 内所有表": """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public'
        ORDER BY table_name;
    """,
    "当前 search_path": "SHOW search_path;",
    "Flyway 已用版本(migration_meta)": """
        SELECT version, description, success, installed_on
        FROM migration_meta.flyway_schema_history
        ORDER BY installed_rank;
    """,
    "app_user 列": """
        SELECT column_name, data_type, column_default
        FROM information_schema.columns
        WHERE table_schema = 'app' AND table_name = 'app_user'
        ORDER BY ordinal_position;
    """,
}


def main():
    try:
        conn = psycopg2.connect(**connection_parameters())
    except Exception as error:
        print("连接失败，请检查私密配置、网络和数据库状态。")
        sys.exit(1)

    conn.set_session(readonly=True, autocommit=True)

    with conn.cursor() as cur:
        for label, sql in QUERIES.items():
            print("\n=== %s ===" % label)
            try:
                cur.execute(sql)
                rows = cur.fetchall()
                if not rows:
                    print("(无结果)")
                for row in rows:
                    print(row)
            except Exception as error:
                print("查询失败：%s" % error)

    conn.close()


if __name__ == "__main__":
    main()
