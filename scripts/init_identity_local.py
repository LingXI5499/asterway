# 实现init账户管理本地处理；下面逐项说明输入、分支与调用的作用。
# 保留命令行帮助原先使用的说明文字；它是普通变量赋值，不是文档注释。
__doc__ = '本地开发库初始化；仅接受 Unix socket 和 asterway_dev，不覆盖已有业务表。'
# 本地开发库初始化；仅接受 Unix socket 和 asterway_dev，不覆盖已有业务表。

# 引入 argparse，供下面调用其功能。
import argparse

# 引入 os，供下面调用其功能。
import os

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 引入 安全随机值（secrets），供下面调用其功能。
import secrets

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 从 argon2 引入 密码摘要生成与校验器（PasswordHasher）。
from argon2 import PasswordHasher


# 函数 main：调用 main 完成程序入口处理。
def main():
    # 把执行“调用 argparse.ArgumentParser 完成命令行参数读取器处理”得到的结果保存到 parser。
    parser = argparse.ArgumentParser(description=__doc__)
    # 声明一个命令行参数的名称、类型和限制。
    parser.add_argument('--credentials-out', required=True, type=Path)
    # 把执行“读取命令行参数并校验必填项”得到的结果保存到 参数（args）。
    args = parser.parse_args()
    # 检查条件 args.credentials_out.exists() 是否成立；成立时执行缩进的分支。
    if args.credentials_out.exists():
        # 中止本轮正常处理，抛出 执行“调用 SystemExit 完成系统Exit处理”得到的结果。
        raise SystemExit('Credential output already exists; refusing to overwrite.')
    # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 数据库连接（connection）。
    connection = psycopg2.connect(dbname='asterway_dev', user='postgres', host='/var/run/postgresql')
    # 把执行“调用 secrets.token_urlsafe 完成身份凭据urlsafe处理”得到的结果保存到 密码（password）。
    password = secrets.token_urlsafe(18)
    # 把执行“判断是否有 h”得到的结果保存到 encoded。
    encoded = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash(password)
    # 进入数据库连接（connection）；退出时由上下文管理器提交、回滚或释放资源。
    with connection:
        # 进入执行“调用 connection.cursor 完成数据库语句执行器处理”得到的结果，命名为 cursor；退出时由上下文管理器提交、回滚或释放资源。
        with connection.cursor() as cursor:
            # 执行传入的数据库语句并取得结果。
            cursor.execute("SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
            # 检查条件 cursor.fetchone()[0] 是否成立；成立时执行缩进的分支。
            if cursor.fetchone()[0]:
                # 中止本轮正常处理，抛出 执行“调用 SystemExit 完成系统Exit处理”得到的结果。
                raise SystemExit('Development database is not empty; no data has been changed.')
            # 将业务 DDL 与超管、权限初始化放进同一个外层事务。
            # 把执行“按指定编码读取文件文字”得到的结果保存到 ddl。
            ddl = (Path(__file__).resolve().parents[1] / 'backend/app/src/main/resources/sql/identity.sql').read_text(encoding='utf-8')
            # 执行传入的数据库语句并取得结果。
            cursor.execute(ddl.replace('BEGIN;', '', 1).rsplit('COMMIT;', 1)[0])
            # 执行传入的数据库语句并取得结果。
            cursor.execute("INSERT INTO identity_user (username,password_hash,nickname,time_zone,must_change_password) VALUES (%s,%s,%s,%s,true) RETURNING id", ('superadmin', encoded, '超级管理员', 'Asia/Shanghai'))
            # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 用户编号（user_id）。
            user_id = cursor.fetchone()[0]
            # 执行传入的数据库语句并取得结果。
            cursor.execute("INSERT INTO identity_user_role (user_id,role_id) SELECT %s,id FROM identity_role WHERE code IN ('USER','SUPER_ADMIN')", (user_id,))
            # 执行传入的数据库语句并取得结果。
            cursor.execute('REVOKE CREATE ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO asterway_dev; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO asterway_dev; GRANT USAGE,SELECT ON ALL SEQUENCES IN SCHEMA public TO asterway_dev; REVOKE UPDATE,DELETE,TRUNCATE ON identity_login_log,identity_operation_log FROM asterway_dev')
            # 把执行“调用 os.open 完成是否展开处理”得到的结果保存到 descriptor。
            descriptor = os.open(args.credentials_out, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
            # 进入执行“调用 os.fdopen 完成fdopen处理”得到的结果，命名为 output；退出时由上下文管理器提交、回滚或释放资源。
            with os.fdopen(descriptor, 'w', encoding='utf-8') as output:
                # 把内容写入当前输出或响应。
                output.write('Local development bootstrap account\nUsername: superadmin\nPassword: ' + password + '\nPassword change is required after the authentication module is implemented.\n')
    # 释放连接、文件或客户端占用的资源。
    connection.close()
    # 调用 print 完成print处理。
    print('Initialized local development database and bootstrap account; credentials saved privately.')


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
if __name__ == '__main__':
    # 调用 main 完成程序入口处理。
    main()
