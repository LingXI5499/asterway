#!/bin/bash
# 按明确的本地路径组织setup账户管理本地操作，调用已有命令完成启动或校验。
# 在本机 Ubuntu WSL2 以 root 执行；不会安装软件或访问远程数据库。
# 遇到失败、未设置变量或管道中的错误立即停止，避免带着错误继续初始化。
set -euo pipefail
# 确认当前在 WSL 管理身份下运行，否则数据库管理命令没有所需权限。
[[ $(id -u) == 0 ]] || { echo 'Run as root in WSL2.' >&2; exit 1; }
# 核对 PostgreSQL 主版本和补丁版本符合当前工程约定。
[[ $(/usr/lib/postgresql/17/bin/postgres --version) == *'PostgreSQL) 17.11 '* ]] || { echo 'PostgreSQL 17.11 is required.' >&2; exit 1; }
# 根据脚本所在目录确定仓库根目录，不依赖调用时所在的位置。
ROOT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
# 把第一个命令行参数作为私密数据库密码文件路径，缺省时先留空。
PASSWORD_FILE=${1:-}
# 检查路径已提供且文件可读取，不满足时输出用法并退出。
[[ -n "$PASSWORD_FILE" && -r "$PASSWORD_FILE" ]] || {
# 把本地准备结果或正确用法输出给调用者。
    echo 'Usage: setup_identity_local.sh /path/to/private/database-password-file' >&2
# 结束当前脚本，以非零状态表示本次准备没有完成。
    exit 1
}
# 启动本机 PostgreSQL；已经运行时只读取状态，不重建数据库。
pg_ctlcluster 17 main start || pg_ctlcluster 17 main status
# 启动本机 Redis 服务，并让其随本机环境启动。
systemctl enable --now redis-server
# 为本机 Redis 打开追加日志持久化，减少服务重启造成的会话丢失。
redis-cli CONFIG SET appendonly yes >/dev/null
# 把刚设置的 Redis 持久化开关保存回其本机配置。
redis-cli CONFIG REWRITE >/dev/null
# 密码通过已打开的私密文件传入，不写入源码、命令行或日志；已有角色不改密码。
# 下面在 PostgreSQL 管理身份下执行内嵌 Python；实际密码只从已打开的私密文件读入。
# 内嵌 Python 第 1 行：引入 sys，供下面调用其功能。
# 内嵌 Python 第 2 行：引入 psycopg2，供下面调用其功能。
# 内嵌 Python 第 3 行：从 psycopg2 引入 sql。
# 内嵌 Python 第 4 行：把执行“去掉文字两端的空白”得到的结果保存到 密码（password）。
# 内嵌 Python 第 5 行：检查条件 not password 是否成立；成立时执行缩进的分支。
# 内嵌 Python 第 6 行：中止本轮正常处理，抛出 执行“调用 SystemExit 完成系统Exit处理”得到的结果。
# 内嵌 Python 第 7 行：进入执行“调用 psycopg2.connect 完成连接处理”得到的结果，命名为 connection；退出时由上下文管理器提交、回滚或释放资源。
# 内嵌 Python 第 8 行：进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
# 内嵌 Python 第 9 行：执行传入的数据库语句并取得结果。
# 内嵌 Python 第 10 行：检查cursor.fetchone() is None，即对应内容是否不存在；成立时执行缩进的分支。
# 内嵌 Python 第 11 行：执行传入的数据库语句并取得结果。
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
# 下面把这一段 SQL 交给本机 psql；SQL 只是按存在性创建开发库和测试库，不清空已有数据。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
# 内嵌 SQL：给字段或条件设置这里的值，沿用前面的所属人、状态与版本筛选规则。
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 postgres <<'SQL'
SELECT 'CREATE DATABASE asterway_dev TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_dev') \gexec
SELECT 'CREATE DATABASE asterway_identity_test TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_identity_test') \gexec
ALTER DATABASE asterway_dev SET timezone='UTC';
ALTER DATABASE asterway_identity_test SET timezone='UTC';
SQL
# 把本地准备结果或正确用法输出给调用者。
echo 'Local databases prepared. No business SQL has been applied by this script.'
