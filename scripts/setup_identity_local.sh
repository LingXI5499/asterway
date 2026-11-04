#!/bin/bash
# 在本机 Ubuntu WSL2 以 root 执行；不会安装软件或访问远程数据库。
set -euo pipefail
[[ $(id -u) == 0 ]] || { echo 'Run as root in WSL2.' >&2; exit 1; }
[[ $(/usr/lib/postgresql/17/bin/postgres --version) == *'PostgreSQL) 17.11 '* ]] || { echo 'PostgreSQL 17.11 is required.' >&2; exit 1; }
ROOT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
pg_ctlcluster 17 main start || pg_ctlcluster 17 main status
systemctl enable --now redis-server
redis-cli CONFIG SET appendonly yes >/dev/null
redis-cli CONFIG REWRITE >/dev/null
runuser -u postgres -- psql -X -v ON_ERROR_STOP=1 postgres <<'SQL'
DO $body$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'asterway_dev') THEN
        CREATE ROLE asterway_dev LOGIN PASSWORD 'asterway_dev_local';
    END IF;
END
$body$;
SELECT 'CREATE DATABASE asterway_dev TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_dev') \gexec
SELECT 'CREATE DATABASE asterway_identity_test TEMPLATE template0 ENCODING ''UTF8'' LOCALE ''C.UTF-8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'asterway_identity_test') \gexec
ALTER DATABASE asterway_dev SET timezone='UTC';
ALTER DATABASE asterway_identity_test SET timezone='UTC';
SQL
echo 'Local databases prepared. No business SQL has been applied by this script.'
