#!/bin/bash
# 在本机 Ubuntu WSL2 以 root 执行；不会安装软件或访问远程数据库。
set -euo pipefail
[[ $(id -u) == 0 ]] || { echo 'Run as root in WSL2.' >&2; exit 1; }
[[ $(/usr/lib/postgresql/17/bin/postgres --version) == *'PostgreSQL) 17.11 '* ]] || { echo 'PostgreSQL 17.11 is required.' >&2; exit 1; }
ROOT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
PASSWORD_FILE=${1:-}
[[ -n "$PASSWORD_FILE" && -r "$PASSWORD_FILE" ]] || {
    echo 'Usage: setup_identity_local.sh /path/to/private/database-password-file' >&2
    exit 1
}
pg_ctlcluster 17 main start || pg_ctlcluster 17 main status
systemctl enable --now redis-server
redis-cli CONFIG SET appendonly yes >/dev/null
redis-cli CONFIG REWRITE >/dev/null
# 密码通过已打开的私密文件传入，不写入源码、命令行或日志；已有角色不改密码。
runuser -u postgres -- python3 -c '
import sys
import psycopg2
from psycopg2 import sql
password = sys.stdin.read().strip()
if not password:
    raise SystemExit("The private database password file is empty.")
with psycopg2.connect(dbname="postgres", user="postgres", host="/var/run/postgresql") as connection:
    with connection.cursor() as cursor:
        cursor.execute("SELECT 1 FROM pg_roles WHERE rolname=%s", ("asterway_dev",))
        if cursor.fetchone() is None:
            cursor.execute(sql.SQL("CREATE ROLE {} LOGIN PASSWORD {}").format(
                sql.Identifier("asterway_dev"), sql.Literal(password)))
' < "$PASSWORD_FILE"
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 postgres <<'SQL'
SELECT 'CREATE DATABASE asterway_dev TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_dev') \gexec
SELECT 'CREATE DATABASE asterway_identity_test TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_identity_test') \gexec
ALTER DATABASE asterway_dev SET timezone='UTC';
ALTER DATABASE asterway_identity_test SET timezone='UTC';
SQL
echo 'Local databases prepared. No business SQL has been applied by this script.'
