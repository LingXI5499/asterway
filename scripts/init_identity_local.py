"""本地开发库初始化；仅接受 Unix socket 和 asterway_dev，不覆盖已有业务表。"""
import argparse
import os
from pathlib import Path
import secrets

import psycopg2
from argon2 import PasswordHasher


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--credentials-out', required=True, type=Path)
    args = parser.parse_args()
    if args.credentials_out.exists():
        raise SystemExit('Credential output already exists; refusing to overwrite.')
    connection = psycopg2.connect(dbname='asterway_dev', user='postgres', host='/var/run/postgresql')
    password = secrets.token_urlsafe(18)
    encoded = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash(password)
    with connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
            if cursor.fetchone()[0]:
                raise SystemExit('Development database is not empty; no data has been changed.')
            # 将业务 DDL 与超管、权限初始化放进同一个外层事务。
            ddl = (Path(__file__).resolve().parents[1] / 'backend/app/src/main/resources/sql/identity.sql').read_text(encoding='utf-8')
            cursor.execute(ddl.replace('BEGIN;', '', 1).rsplit('COMMIT;', 1)[0])
            cursor.execute("INSERT INTO identity_user (username,password_hash,nickname,time_zone,must_change_password) VALUES (%s,%s,%s,%s,true) RETURNING id", ('superadmin', encoded, '超级管理员', 'Asia/Shanghai'))
            user_id = cursor.fetchone()[0]
            cursor.execute("INSERT INTO identity_user_role (user_id,role_id) SELECT %s,id FROM identity_role WHERE code IN ('USER','SUPER_ADMIN')", (user_id,))
            cursor.execute('REVOKE CREATE ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO asterway_dev; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO asterway_dev; GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO asterway_dev; REVOKE UPDATE,DELETE,TRUNCATE ON identity_login_log,identity_operation_log FROM asterway_dev')
            descriptor = os.open(args.credentials_out, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            with os.fdopen(descriptor, 'w', encoding='utf-8') as output:
                output.write('Local development bootstrap account\nUsername: superadmin\nPassword: ' + password + '\nPassword change is required after the authentication module is implemented.\n')
    connection.close()
    print('Initialized local development database and bootstrap account; credentials saved privately.')


if __name__ == '__main__':
    main()
