# 检查test账户管理sql相关行为，断言失败时报告具体不符合预期的场景。
# 保留命令行帮助原先使用的说明文字；它是普通变量赋值，不是文档注释。
__doc__ = '真实 SQL 验证，仅重建本机 asterway_identity_test.public；不测试尚未实现的 HTTP 业务。'
# 真实 SQL 验证，仅重建本机 asterway_identity_test.public；不测试尚未实现的 HTTP 业务。

# 引入 argparse，供下面调用其功能。
import argparse

# 引入 hashlib，供下面调用其功能。
import hashlib

# 从 concurrent.futures 引入 Thread连接池Executor（ThreadPoolExecutor）。
from concurrent.futures import ThreadPoolExecutor

# 引入 JSON 编码和解析库（json），供下面调用其功能。
import json

# 从 pathlib 引入 路径（Path）。
from pathlib import Path

# 引入 threading，供下面调用其功能。
import threading

# 引入 时间与耗时工具库（time），供下面调用其功能。
import time

# 引入 unittest，供下面调用其功能。
import unittest

# 引入 uuid，供下面调用其功能。
import uuid

# 引入 psycopg2，供下面调用其功能。
import psycopg2

# 从 psycopg2 引入 sql。
from psycopg2 import sql

# 从 argon2 引入 密码摘要生成与校验器（PasswordHasher）。
from argon2 import PasswordHasher

# 把从执行“调用 Path(__file__).resolve 完成resolve处理”得到的结果的parents属性中按固定数值 1取出的内容保存到 根（ROOT）。
ROOT = Path(__file__).resolve().parents[1]
# 把执行“按指定编码读取文件文字”得到的结果保存到 DDL。
DDL = (ROOT / 'backend/app/src/main/resources/sql/identity.sql').read_text(encoding='utf-8')
# 把固定文字 'dbname=asterway_identity_test user=postgres host=/var/run/postgresql'保存到 DSN。
DSN = 'dbname=asterway_identity_test user=postgres host=/var/run/postgresql'
# 把执行“判断是否有 h”得到的结果保存到 摘要值（HASH）。
HASH = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash('OnlyForSqlTests123')
# 把表达式 'SELECT DISTINCT p.code FROM identity_user u\nJOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id… 的结果保存到 权限列表（PERMISSIONS）。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：按这里的关联条件连接另一张表的记录。
# 内嵌 SQL：按这里的关联条件连接另一张表的记录。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：再增加一个必须同时满足的筛选条件。
PERMISSIONS = """SELECT DISTINCT p.code FROM identity_user u
JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id
JOIN identity_role_permission rp ON rp.role_id=r.id JOIN identity_permission p ON p.id=rp.permission_id
WHERE u.id=%s AND u.is_enabled AND u.deleted_at IS NULL AND r.is_enabled AND r.deleted_at IS NULL
AND p.is_enabled AND p.deleted_at IS NULL ORDER BY p.code"""
# 把表达式 "SELECT u.id FROM identity_user u WHERE u.deleted_at IS NULL AND NOT EXISTS (\nSELECT 1 FROM identity_user_role ur JOIN … 的结果保存到 MANAGEABLE。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
MANAGEABLE = """SELECT u.id FROM identity_user u WHERE u.deleted_at IS NULL AND NOT EXISTS (
SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
WHERE ur.user_id=u.id AND r.code IN ('ADMIN','SUPER_ADMIN')) ORDER BY u.id"""
# 把表达式 "WITH RECURSIVE grants AS (\n SELECT DISTINCT rm.menu_id FROM identity_user u JOIN identity_user_role ur ON ur.user_id=u… 的结果保存到 菜单TREE（MENU_TREE）。
# 内嵌 SQL：先定义可复用的临时查询结果，后续语句继续引用它。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：按这里的关联条件连接另一张表的记录。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：), rights AS (。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：按这里的关联条件连接另一张表的记录。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：), tree AS (。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：合并前后两段查询结果，保留重复行。
# 内嵌 SQL：读取后面列出的字段或计算结果。
# 内嵌 SQL：按这里的关联条件连接另一张表的记录。
# 内嵌 SQL：只处理符合这里条件的记录，其他记录不进入本次查询或修改。
# 内嵌 SQL：再增加一个必须同时满足的筛选条件。
# 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：) SELECT code FROM tree WHERE menu_type <> 'BUTTON' ORDER BY code。
MENU_TREE = """WITH RECURSIVE grants AS (
 SELECT DISTINCT rm.menu_id FROM identity_user u JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id
 JOIN identity_role_menu rm ON rm.role_id=r.id
 WHERE ur.user_id=%s AND u.is_enabled AND u.deleted_at IS NULL AND r.is_enabled AND r.deleted_at IS NULL
), rights AS (
 SELECT DISTINCT rp.permission_id FROM identity_user u JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id
 JOIN identity_role_permission rp ON rp.role_id=r.id JOIN identity_permission p ON p.id=rp.permission_id
 WHERE ur.user_id=%s AND u.is_enabled AND u.deleted_at IS NULL AND r.is_enabled AND r.deleted_at IS NULL AND p.is_enabled AND p.deleted_at IS NULL
), tree AS (
 SELECT m.id,m.code,m.menu_type,ARRAY[m.id] path FROM identity_menu m JOIN grants g ON g.menu_id=m.id
 WHERE m.parent_id IS NULL AND m.menu_type='DIRECTORY' AND m.is_enabled AND m.is_visible AND m.deleted_at IS NULL
 UNION ALL
 SELECT m.id,m.code,m.menu_type,t.path||m.id FROM identity_menu m JOIN tree t ON t.id=m.parent_id
 JOIN grants g ON g.menu_id=m.id
 WHERE m.is_enabled AND m.is_visible AND m.deleted_at IS NULL AND NOT m.id=ANY(t.path)
 AND (m.menu_type='DIRECTORY' OR m.permission_id IN (SELECT permission_id FROM rights))
) SELECT code FROM tree WHERE menu_type <> 'BUTTON' ORDER BY code"""


# 函数 connect：调用 connect 完成连接处理。
def connect():
    # 把执行“调用 psycopg2.connect 完成连接处理”得到的结果保存到 c。
    c = psycopg2.connect(DSN, application_name='asterway_identity_sql_test')
    # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
    with c.cursor() as q:
        # 执行传入的数据库语句并取得结果。
        q.execute("SET statement_timeout='8s'; SET lock_timeout='4s'; SET TIME ZONE 'UTC'")
    # 提交本次数据库事务，使本次修改一起生效。
    c.commit()
    # 把c交还给调用方，本函数到此结束。
    return c


# 函数 one：调用 one 完成one处理；接收 c、statement、参数集合（params）。
def one(c, statement, params=()):
    # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
    with c.cursor() as q:
        # 执行传入的数据库语句并取得结果。
        q.execute(statement, params or None)
        # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容交还给调用方，本函数到此结束。
        return q.fetchone()[0]


# 函数 add_user：调用 add_user 完成加入用户处理；接收 c、名称（name）、角色列表（roles）。
def add_user(c, name, roles=('USER',), **changes):
    # 把执行“调用 dict 完成dict处理”得到的结果保存到 值列表（values）。
    values = dict(username=name, password_hash=HASH, nickname=name, time_zone='Asia/Shanghai')
    # 调用 values.update 完成更新处理。
    values.update(changes)
    # 把执行“调用 one 完成one处理”得到的结果保存到 uid。
    uid = one(c, sql.SQL('INSERT INTO identity_user ({}) VALUES ({}) RETURNING id').format(
        # 继续传入或组合调用 sql.SQL 完成SQL处理，完成上面尚未结束的表达式。
        sql.SQL(',').join(map(sql.Identifier, values)), sql.SQL(',').join(sql.Placeholder() for _ in values)), tuple(values.values()))
    # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
    with c.cursor() as q:
        # 执行传入的数据库语句并取得结果。
        q.execute('INSERT INTO identity_user_role(user_id,role_id) SELECT %s,id FROM identity_role WHERE code=ANY(%s)', (uid, list(roles)))
    # 把uid交还给调用方，本函数到此结束。
    return uid


# 定义 IdentitySqlTests，把账户管理SqlTests需要的数据与操作放在同一个对象中。
class IdentitySqlTests(unittest.TestCase):
    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 setUpClass：设置 Up类（UpClass） 的值。
    def setUpClass(cls):
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 当前类中的c（cls.c）。
        cls.c = connect()
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 当前类中的normal（cls.normal）。
        cls.normal = add_user(cls.c, '测试用户')
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 当前类中的admin（cls.admin）。
        cls.admin = add_user(cls.c, '测试管理员', ('USER', 'ADMIN'))
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 当前类中的super（cls.super）。
        cls.super = add_user(cls.c, '测试超管', ('USER', 'SUPER_ADMIN'))
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 当前类中的已禁用（cls.disabled）。
        cls.disabled = add_user(cls.c, '停用用户', is_enabled=False)
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 当前类中的删除标记（cls.deleted）。
        cls.deleted = add_user(cls.c, '已删用户', is_enabled=False, deleted_at='2026-01-01T00:00:00Z')
        # 提交本次数据库事务，使本次修改一起生效。
        cls.c.commit()

    # 这个方法接收当前类，供统一构造或校验使用。
    @classmethod
    # 函数 tearDownClass：调用 tearDownClass 完成tearDown类处理。
    def tearDownClass(cls):
        # 释放连接、文件或客户端占用的资源。
        cls.c.close()

    # 函数 tearDown：调用 tearDown 完成tearDown处理。
    def tearDown(self):
        # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
        self.c.rollback()

    # 函数 reject：调用 reject 完成拒绝处理；接收 statement、参数集合（params）、当前状态（state）。
    def reject(self, statement, params, state):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('SAVEPOINT rejected_input')
            # 进入执行“断言 Raises”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
            with self.assertRaises(psycopg2.Error) as error:
                # 执行传入的数据库语句并取得结果。
                q.execute(statement, params)
            # 断言 Equal。
            self.assertEqual(error.exception.pgcode, state)
            # 执行传入的数据库语句并取得结果。
            q.execute('ROLLBACK TO SAVEPOINT rejected_input')

    # 函数 codes：调用 codes 完成codes处理；接收 uid。
    def codes(self, uid):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(PERMISSIONS, (uid,))
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
            return {row[0] for row in q.fetchall()}

    # 函数 test_01_server_version：调用 test_01_server_version 完成test01server版本号处理。
    def test_01_server_version(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, "SHOW server_version_num"), '170011')

    # 函数 test_02_exactly_ten_tables：调用 test_02_exactly_ten_tables 完成test02exactlytentables处理。
    def test_02_exactly_ten_tables(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'identity_%'"), 10)

    # 函数 test_03_no_foreign_keys：调用 test_03_no_foreign_keys 完成test03no外部键列表处理。
    def test_03_no_foreign_keys(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace"), 0)

    # 函数 test_04_no_triggers：调用 test_04_no_triggers 完成test04notriggers处理。
    def test_04_no_triggers(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal AND tgrelid IN (SELECT oid FROM pg_class WHERE relnamespace='public'::regnamespace)"), 0)

    # 函数 test_05_three_builtin_roles：调用 test_05_three_builtin_roles 完成test05threebuiltin角色列表处理。
    def test_05_three_builtin_roles(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, "SELECT array_agg(code ORDER BY code) FROM identity_role WHERE is_builtin"), ['ADMIN', 'SUPER_ADMIN', 'USER'])

    # 函数 test_06_seed_counts：调用 test_06_seed_counts 完成test06seedcounts处理。
    def test_06_seed_counts(self):
        # 断言 Equal。
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_permission'), 37)
        # 断言 Equal。
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_menu'), 39)
        # 断言 Equal。
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_setting'), 2)

    # 函数 test_07_username_case_insensitive：调用 test_07_username_case_insensitive 完成test07用户名caseinsensitive处理。
    def test_07_username_case_insensitive(self):
        # 调用 add_user 完成加入用户处理。
        add_user(self.c, 'LingXI')
        # 调用 self.reject 完成拒绝处理。
        self.reject('INSERT INTO identity_user(username,password_hash,nickname,time_zone) VALUES (%s,%s,%s,%s)', ('lingxi', HASH, '重复', 'UTC'), '23505')

    # 函数 test_08_deleted_username_reserved：调用 test_08_deleted_username_reserved 完成test08删除标记用户名reserved处理。
    def test_08_deleted_username_reserved(self):
        # 调用 self.reject 完成拒绝处理。
        self.reject('INSERT INTO identity_user(username,password_hash,nickname,time_zone) VALUES (%s,%s,%s,%s)', ('已删用户', HASH, '重复', 'UTC'), '23505')

    # 函数 test_09_valid_username_boundaries：调用 test_09_valid_username_boundaries 完成test09有效用户名boundaries处理。
    def test_09_valid_username_boundaries(self):
        # 从依次包含 固定文字 '甲'、按 'A' * 30 计算的结果、固定文字 '中文ABC123'、固定文字 '𠀀' 的集合依次取出名称（name），每项执行一次下面的处理。
        for name in ['甲', 'A'*30, '中文ABC123', '\U00020000']:
            # 断言 Greater。
            self.assertGreater(add_user(self.c, name), 0)

    # 函数 test_10_nicknames_can_repeat：调用 test_10_nicknames_can_repeat 完成test10nicknames是否可以repeat处理。
    def test_10_nicknames_can_repeat(self):
        # 调用 add_user 完成加入用户处理。
        add_user(self.c, '同昵称甲', nickname='相同昵称')
        # 调用 add_user 完成加入用户处理。
        add_user(self.c, '同昵称乙', nickname='相同昵称')
        # 断言 Equal。
        self.assertEqual(one(self.c, "SELECT count(*) FROM identity_user WHERE nickname='相同昵称'"), 2)

    # 函数 test_11_user_role_pair_unique：调用 test_11_user_role_pair_unique 完成test11用户角色pair唯一处理。
    def test_11_user_role_pair_unique(self):
        # 调用 self.reject 完成拒绝处理。
        self.reject("INSERT INTO identity_user_role(user_id,role_id) SELECT %s,id FROM identity_role WHERE code='USER'", (self.normal,), '23505')

    # 函数 test_12_role_permission_pair_unique：调用 test_12_role_permission_pair_unique 完成test12角色权限pair唯一处理。
    def test_12_role_permission_pair_unique(self):
        # 调用 self.reject 完成拒绝处理。
        self.reject('INSERT INTO identity_role_permission(role_id,permission_id) SELECT role_id,permission_id FROM identity_role_permission LIMIT 1', (), '23505')

    # 函数 test_13_role_menu_pair_unique：调用 test_13_role_menu_pair_unique 完成test13角色菜单pair唯一处理。
    def test_13_role_menu_pair_unique(self):
        # 调用 self.reject 完成拒绝处理。
        self.reject('INSERT INTO identity_role_menu(role_id,menu_id) SELECT role_id,menu_id FROM identity_role_menu LIMIT 1', (), '23505')

    # 函数 test_14_normal_permissions：调用 test_14_normal_permissions 完成test14normal权限列表处理。
    def test_14_normal_permissions(self):
        # 断言 Equal。
        self.assertEqual(len(self.codes(self.normal)), 7)
        # 断言 NotIn。
        self.assertNotIn('identity:user:list', self.codes(self.normal))

    # 函数 test_15_admin_permission_union：调用 test_15_admin_permission_union 完成test15admin权限union处理。
    def test_15_admin_permission_union(self):
        # 把执行“调用 self.codes 完成codes处理”得到的结果保存到 rights。
        rights = self.codes(self.admin)
        # 断言 Equal。
        self.assertEqual(len(rights), 19)
        # 断言 In。
        self.assertIn('identity:user:reset_password', rights)
        # 断言 NotIn。
        self.assertNotIn('identity:user:assign_role', rights)
        # 断言 不成立（False）。
        self.assertFalse(any(x.startswith('identity:role:') for x in rights))

    # 函数 test_16_super_has_all_seed_permissions：调用 test_16_super_has_all_seed_permissions 完成test16super是否存在全部seed权限列表处理。
    def test_16_super_has_all_seed_permissions(self):
        # 断言 Equal。
        self.assertEqual(len(self.codes(self.super)), 37)

    # 函数 test_17_disabled_user_no_permissions：调用 test_17_disabled_user_no_permissions 完成test17已禁用用户no权限列表处理。
    def test_17_disabled_user_no_permissions(self):
        # 断言 Equal。
        self.assertEqual(self.codes(self.disabled), set())

    # 函数 test_18_deleted_user_no_permissions：调用 test_18_deleted_user_no_permissions 完成test18删除标记用户no权限列表处理。
    def test_18_deleted_user_no_permissions(self):
        # 断言 Equal。
        self.assertEqual(self.codes(self.deleted), set())

    # 函数 test_19_disabled_role_filters_rights：调用 test_19_disabled_role_filters_rights 完成test19已禁用角色筛选条件rights处理。
    def test_19_disabled_role_filters_rights(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_role SET is_enabled=false WHERE code='ADMIN'")
        # 断言 Equal。
        self.assertEqual(len(self.codes(self.admin)), 7)

    # 函数 test_20_permission_change_visible_next_query：调用 test_20_permission_change_visible_next_query 完成test20权限修改是否显示下一个查询条件处理。
    def test_20_permission_change_visible_next_query(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_permission SET is_enabled=false WHERE code='identity:user:list'")
        # 断言 NotIn。
        self.assertNotIn('identity:user:list', self.codes(self.admin))

    # 函数 test_21_admin_target_scope：调用 test_21_admin_target_scope 完成test21admin目标值数据范围处理。
    def test_21_admin_target_scope(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(MANAGEABLE)
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 编号列表（ids）。
            ids = {x[0] for x in q.fetchall()}
        # 断言 Equal。
        self.assertEqual(ids, {self.normal, self.disabled})

    # 函数 test_22_normal_menu_tree：调用 test_22_normal_menu_tree 完成test22normal菜单tree处理。
    def test_22_normal_menu_tree(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE, (self.normal, self.normal))
            # 断言 Equal。
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root', 'profile', 'devices'})

    # 函数 test_23_admin_menu_tree：调用 test_23_admin_menu_tree 完成test23admin菜单tree处理。
    def test_23_admin_menu_tree(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE, (self.admin, self.admin))
            # 断言 Equal。
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root','profile','devices','system_root','users','sessions','login_logs','operation_logs'})

    # 函数 test_24_disabled_parent_hides_branch：调用 test_24_disabled_parent_hides_branch 完成test24已禁用上级hidesbranch处理。
    def test_24_disabled_parent_hides_branch(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_menu SET is_enabled=false WHERE code='system_root'")
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE, (self.super, self.super))
            # 断言 Equal。
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root','profile','devices'})

    # 函数 test_25_menu_grant_does_not_grant_api：调用 test_25_menu_grant_does_not_grant_api 完成test25菜单grantdoesnotgrant接口处理。
    def test_25_menu_grant_does_not_grant_api(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("INSERT INTO identity_role_menu(role_id,menu_id) SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m WHERE r.code='USER' AND m.code IN ('users','system_root')")
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE, (self.normal, self.normal))
            # 断言 NotIn。
            self.assertNotIn('users', {x[0] for x in q.fetchall()})
        # 断言 NotIn。
        self.assertNotIn('identity:user:list', self.codes(self.normal))

    # 函数 test_26_orphan_association_is_not_db_constraint：调用 test_26_orphan_association_is_not_db_constraint 完成test26orphanassociation是否not数据库约束处理。
    def test_26_orphan_association_is_not_db_constraint(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("INSERT INTO identity_user_role(user_id,role_id) SELECT 999999,id FROM identity_role WHERE code='USER'")
        # 断言 Equal。
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_user_role ur LEFT JOIN identity_user u ON u.id=ur.user_id WHERE u.id IS NULL'), 1)
        # 这是无外键设计的实际边界：SQL 允许孤儿，后续 Service 必须拒绝。

    # 函数 test_27_seed_associations_are_valid：调用 test_27_seed_associations_are_valid 完成test27seedassociationsare有效处理。
    def test_27_seed_associations_are_valid(self):
        # 从依次包含 依次包含 固定文字 'user_role'、固定文字 'user'、固定文字 'role' 的集合、依次包含 固定文字 'role_permission'、固定文字 'role'、固定文字 'permission' 的集合、依次包含 固定文字 'role_menu'、固定文字 'role'、固定文字 'menu' 的集合 的集合依次取出relationleftright（(relation, left, right)），每项执行一次下面的处理。
        for relation,left,right in [('user_role','user','role'),('role_permission','role','permission'),('role_menu','role','menu')]:
            # 把在文字占位处填入变量后组成的字符串保存到 statement。
            statement=f'SELECT count(*) FROM identity_{relation} x LEFT JOIN identity_{left} l ON l.id=x.{left}_id LEFT JOIN identity_{right} r ON r.id=x.{right}_id WHERE l.id IS NULL OR r.id IS NULL'
            # 断言 Equal。
            self.assertEqual(one(self.c, statement), 0)

    # 函数 test_28_optimistic_version_compare：调用 test_28_optimistic_version_compare 完成test28optimistic版本号compare处理。
    def test_28_optimistic_version_compare(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('UPDATE identity_user SET nickname=%s,version=version+1 WHERE id=%s AND version=0', ('新版',self.normal))
            # 断言 Equal。
            self.assertEqual(q.rowcount, 1)
            # 执行传入的数据库语句并取得结果。
            q.execute('UPDATE identity_user SET nickname=%s,version=version+1 WHERE id=%s AND version=0', ('旧请求',self.normal))
            # 断言 Equal。
            self.assertEqual(q.rowcount, 0)

    # 函数 test_29_user_role_transaction_rollback：调用 test_29_user_role_transaction_rollback 完成test29用户角色事务rollback处理。
    def test_29_user_role_transaction_rollback(self):
        # 把固定文字 '事务回滚'保存到 名称（name）。
        name='事务回滚'
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('SAVEPOINT registration')
            # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 uid。
            uid=add_user(self.c,name,roles=())
            # 进入执行“断言 Raises”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
            with self.assertRaises(psycopg2.Error):
                # 执行传入的数据库语句并取得结果。
                q.execute('INSERT INTO identity_user_role(user_id,role_id) VALUES (%s,0)', (uid,))
            # 执行传入的数据库语句并取得结果。
            q.execute('ROLLBACK TO SAVEPOINT registration')
        # 断言 Equal。
        self.assertEqual(one(self.c,'SELECT count(*) FROM identity_user WHERE username=%s',(name,)),0)

    # 函数 test_30_grant_replacement_rollback：调用 test_30_grant_replacement_rollback 完成test30grantreplacementrollback处理。
    def test_30_grant_replacement_rollback(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('SAVEPOINT grant_replace')
            # 执行传入的数据库语句并取得结果。
            q.execute("DELETE FROM identity_role_permission WHERE role_id=(SELECT id FROM identity_role WHERE code='ADMIN')")
            # 进入执行“断言 Raises”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
            with self.assertRaises(psycopg2.Error):
                # 执行传入的数据库语句并取得结果。
                q.execute('INSERT INTO identity_role_permission VALUES (0,0,now(),NULL)')
            # 执行传入的数据库语句并取得结果。
            q.execute('ROLLBACK TO SAVEPOINT grant_replace')
        # 断言 Equal。
        self.assertEqual(len(self.codes(self.admin)),19)

    # 函数 test_31_soft_delete_preserves_logs：调用 test_31_soft_delete_preserves_logs 完成test31soft删除preserveslogs处理。
    def test_31_soft_delete_preserves_logs(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("INSERT INTO identity_login_log(user_id,username_snapshot,outcome,reason_code,trace_id) VALUES (%s,'测试用户','FAILURE','INVALID_CREDENTIALS','sql-test')",(self.normal,))
            # 执行传入的数据库语句并取得结果。
            q.execute('UPDATE identity_user SET is_enabled=false,deleted_at=now(),auth_version=auth_version+1 WHERE id=%s',(self.normal,))
        # 断言 Equal。
        self.assertEqual(self.codes(self.normal),set())
        # 断言 Equal。
        self.assertEqual(one(self.c,'SELECT count(*) FROM identity_login_log WHERE user_id=%s',(self.normal,)),1)

    # 函数 test_32_security_version_increments：调用 test_32_security_version_increments 完成test32security版本号increments处理。
    def test_32_security_version_increments(self):
        # 断言 Equal。
        self.assertEqual(one(self.c,'UPDATE identity_user SET auth_version=auth_version+1 WHERE id=%s RETURNING auth_version',(self.normal,)),1)

    # 函数 test_33_registration_settings：调用 test_33_registration_settings 完成test33registration配置处理。
    def test_33_registration_settings(self):
        # 断言 Equal。
        self.assertEqual(one(self.c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled'"),True)
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_setting SET setting_value='false',version=version+1 WHERE setting_key='registration_enabled'")
        # 断言 Equal。
        self.assertEqual(one(self.c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled'"),False)

    # 函数 test_34_timezone_names_validation_query：调用 test_34_timezone_names_validation_query 完成test34timezonenamesvalidation查询条件处理。
    def test_34_timezone_names_validation_query(self):
        # 断言 成立（True）。
        self.assertTrue(one(self.c,'SELECT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name=%s)',('Asia/Shanghai',)))
        # 断言 不成立（False）。
        self.assertFalse(one(self.c,'SELECT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name=%s)',('Invalid/Zone',)))

    # 函数 test_35_timezone_dst_day_lengths：调用 test_35_timezone_dst_day_lengths 完成test35timezonedst日lengths处理。
    def test_35_timezone_dst_day_lengths(self):
        # 从依次包含 依次包含 固定文字 '2026-03-08'、固定数值 23 的集合、依次包含 固定文字 '2026-11-01'、固定数值 25 的集合 的集合依次取出日小时数（(day, hours)），每项执行一次下面的处理。
        for day,hours in [('2026-03-08',23),('2026-11-01',25)]:
            # 把执行“调用 one 完成one处理”得到的结果保存到 长度（length）。
            length=one(self.c,"SELECT extract(epoch FROM (((%s::date+1)::timestamp AT TIME ZONE 'America/New_York')-(%s::date::timestamp AT TIME ZONE 'America/New_York')))/3600",(day,day))
            # 断言 Equal。
            self.assertEqual(length,hours)

    # 函数 test_36_pending_timezone_not_early：调用 test_36_pending_timezone_not_early 完成test36等待处理timezonenotearly处理。
    def test_36_pending_timezone_not_early(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_user SET pending_time_zone='UTC',time_zone_effective_at=now()+interval '1 day' WHERE id=%s",(self.normal,))
            # 执行传入的数据库语句并取得结果。
            q.execute('UPDATE identity_user SET time_zone=pending_time_zone,pending_time_zone=NULL,time_zone_effective_at=NULL WHERE id=%s AND time_zone_effective_at<=now()',(self.normal,))
            # 断言 Equal。
            self.assertEqual(q.rowcount,0)

    # 函数 test_37_pending_timezone_apply_once：调用 test_37_pending_timezone_apply_once 完成test37等待处理timezoneapplyonce处理。
    def test_37_pending_timezone_apply_once(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_user SET pending_time_zone='UTC',time_zone_effective_at=now()-interval '1 second' WHERE id=%s",(self.normal,))
            # 把固定文字 'UPDATE identity_user SET time_zone=pending_time_zone,pending_time_zone=NULL,time…'保存到 statement。
            statement='UPDATE identity_user SET time_zone=pending_time_zone,pending_time_zone=NULL,time_zone_effective_at=NULL,version=version+1 WHERE id=%s AND time_zone_effective_at<=now()'
            # 执行传入的数据库语句并取得结果。
            # 断言 Equal。
            q.execute(statement,(self.normal,));self.assertEqual(q.rowcount,1)
            # 执行传入的数据库语句并取得结果。
            # 断言 Equal。
            q.execute(statement,(self.normal,));self.assertEqual(q.rowcount,0)

    # 函数 test_38_unknown_username_login_log：调用 test_38_unknown_username_login_log 完成test38unknown用户名登录日志处理。
    def test_38_unknown_username_login_log(self):
        # 断言 Greater。
        self.assertGreater(one(self.c,"INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id,ip_address) VALUES ('unknown','FAILURE','INVALID_CREDENTIALS','sql-test','::1') RETURNING id"),0)

    # 函数 test_39_success_login_requires_session：调用 test_39_success_login_requires_session 完成test39成功登录requires会话处理。
    def test_39_success_login_requires_session(self):
        # 调用 self.reject 完成拒绝处理。
        self.reject("INSERT INTO identity_login_log(user_id,username_snapshot,outcome,reason_code,trace_id) VALUES (%s,'user','SUCCESS','SUCCESS','sql-test')",(self.normal,),'23514')

    # 函数 test_40_safe_operation_summary_roundtrip：调用 test_40_safe_operation_summary_roundtrip 完成test40safe操作汇总roundtrip处理。
    def test_40_safe_operation_summary_roundtrip(self):
        # 把按字段名保存多个值的字典保存到 汇总（summary）。
        summary={'before':{'isEnabled':True},'after':{'isEnabled':False}}
        # 把执行“调用 one 完成one处理”得到的结果保存到 实际（actual）。
        actual=one(self.c,"INSERT INTO identity_operation_log(actor_id,target_user_id,resource_type,resource_id,action_code,outcome,change_summary,trace_id) VALUES (%s,%s,'USER',%s,'identity:user:status','SUCCESS',%s,'sql-test') RETURNING change_summary",(self.admin,self.normal,str(self.normal),json.dumps(summary)))
        # 断言 Equal。
        self.assertEqual(actual,summary)

    # 函数 test_41_pagination_stable：调用 test_41_pagination_stable 完成test41paginationstable处理。
    def test_41_pagination_stable(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 首次（first）。
            q.execute(MANAGEABLE+' LIMIT 1 OFFSET 0'); first=q.fetchone()[0]
            # 执行传入的数据库语句并取得结果。
            # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 秒（second）。
            q.execute(MANAGEABLE+' LIMIT 1 OFFSET 1'); second=q.fetchone()[0]
        # 断言 NotEqual。
        self.assertNotEqual(first,second)

    # 函数 test_42_initialization_failure_is_atomic：调用 test_42_initialization_failure_is_atomic 完成test42initialization失败是否atomic处理。
    def test_42_initialization_failure_is_atomic(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('CREATE SCHEMA identity_init_failure')
        # 提交本次数据库事务，使本次修改一起生效。
        self.c.commit()
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
        # 把真，表示条件成立或开关打开保存到 c中的autocommit（c.autocommit）。
        c=connect();c.autocommit=True
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
            with c.cursor() as q:
                # 执行传入的数据库语句并取得结果。
                q.execute('SET search_path=identity_init_failure')
                # 进入执行“断言 Raises”得到的结果；退出时由上下文管理器提交、回滚或释放资源。
                with self.assertRaises(psycopg2.Error):
                    # 执行传入的数据库语句并取得结果。
                    q.execute(DDL.replace('COMMIT;',"INSERT INTO identity_setting VALUES ('bad','null',0,now(),now(),NULL);\nCOMMIT;"))
                # 执行传入的数据库语句并取得结果。
                q.execute('ROLLBACK')
                # 断言 Equal。
                self.assertEqual(one(c,"SELECT count(*) FROM pg_tables WHERE schemaname='identity_init_failure'"),0)
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 释放连接、文件或客户端占用的资源。
            c.close()
            # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
            # 执行传入的数据库语句并取得结果。
            with self.c.cursor() as q:q.execute('DROP SCHEMA identity_init_failure')
            # 提交本次数据库事务，使本次修改一起生效。
            self.c.commit()

    # 函数 test_43_reapplication_refuses_existing_schema：调用 test_43_reapplication_refuses_existing_schema 完成test43reapplicationrefusesexisting数据库分区处理。
    def test_43_reapplication_refuses_existing_schema(self):
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
        # 把真，表示条件成立或开关打开保存到 c中的autocommit（c.autocommit）。
        c=connect();c.autocommit=True
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
            with c.cursor() as q:
                # 进入执行“断言 Raises”得到的结果，命名为 error；退出时由上下文管理器提交、回滚或释放资源。
                # 执行传入的数据库语句并取得结果。
                with self.assertRaises(psycopg2.Error) as error:q.execute(DDL)
                # 断言 Equal。
                self.assertEqual(error.exception.pgcode,'42P07')
                # 执行传入的数据库语句并取得结果。
                q.execute('ROLLBACK')
            # 断言 Equal。
            self.assertEqual(one(c,'SELECT count(*) FROM identity_role'),3)
        # 释放连接、文件或客户端占用的资源。
        finally:c.close()

    # 函数 test_44_concurrent_username_uniqueness：调用 test_44_concurrent_username_uniqueness 完成test44concurrent用户名uniqueness处理。
    def test_44_concurrent_username_uniqueness(self):
        # 把按 '并发' + uuid.uuid4().hex[:12] 计算的结果保存到 名称（name）。
        # 把执行“调用 threading.Barrier 完成Barrier处理”得到的结果保存到 barrier。
        name='并发'+uuid.uuid4().hex[:12];barrier=threading.Barrier(2)
        # 函数 worker：调用 worker 完成后台处理器处理。
        def worker():
            # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
            c=connect()
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 调用 barrier.wait 完成等待处理。
                barrier.wait(timeout=5)
                # 进入c；退出时由上下文管理器提交、回滚或释放资源。
                # 调用 add_user 完成加入用户处理。
                with c: add_user(c,name)
                # 把固定状态或编码 CREATED（创建时）交还给调用方，本函数到此结束。
                return 'CREATED'
            # 捕获psycopg2.errors.UniqueViolation异常，执行下面的错误处理。
            # 把固定状态或编码 DUPLICATE（DUPLICATE）交还给调用方，本函数到此结束。
            except psycopg2.errors.UniqueViolation:return 'DUPLICATE'
            # 释放连接、文件或客户端占用的资源。
            finally:c.close()
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 ThreadPoolExecutor 完成Thread连接池Executor处理”得到的结果，命名为 pool；退出时由上下文管理器提交、回滚或释放资源。
            with ThreadPoolExecutor(max_workers=2) as pool:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 futures。
                futures=[pool.submit(worker) for _ in range(2)]
                # 断言 Equal。
                self.assertEqual(sorted(f.result(timeout=10) for f in futures),['CREATED','DUPLICATE'])
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
            c=connect()
            # 进入c；退出时由上下文管理器提交、回滚或释放资源。
            with c:
                # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
                with c.cursor() as q:
                    # 执行传入的数据库语句并取得结果。
                    q.execute('DELETE FROM identity_user_role WHERE user_id IN (SELECT id FROM identity_user WHERE username=%s)',(name,))
                    # 执行传入的数据库语句并取得结果。
                    q.execute('DELETE FROM identity_user WHERE username=%s',(name,))
            # 释放连接、文件或客户端占用的资源。
            c.close()

    # 函数 test_45_last_super_guard_serializes：调用 test_45_last_super_guard_serializes 完成test45最后一次superguardserializes处理。
    def test_45_last_super_guard_serializes(self):
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
        c=connect()
        # 进入c；退出时由上下文管理器提交、回滚或释放资源。
        # 把执行“调用 add_user 完成加入用户处理”得到的结果保存到 秒（second）。
        with c:second=add_user(c,'超管并发'+uuid.uuid4().hex[:8],('USER','SUPER_ADMIN'))
        # 释放连接、文件或客户端占用的资源。
        # 把执行“调用 threading.Barrier 完成Barrier处理”得到的结果保存到 barrier。
        c.close();barrier=threading.Barrier(2)
        # 函数 worker：调用 worker 完成后台处理器处理；接收 uid。
        def worker(uid):
            # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
            c=connect()
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 调用 barrier.wait 完成等待处理。
                barrier.wait(timeout=5)
                # 进入c；退出时由上下文管理器提交、回滚或释放资源。
                with c:
                    # 调用 one 完成one处理。
                    one(c,"SELECT id FROM identity_role WHERE code='SUPER_ADMIN' FOR UPDATE")
                    # 把执行“调用 one 完成one处理”得到的结果保存到 数量（count）。
                    count=one(c,"SELECT count(DISTINCT u.id) FROM identity_user u JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id WHERE r.code='SUPER_ADMIN' AND u.is_enabled AND u.deleted_at IS NULL")
                    # 检查条件 count <= 1 是否成立；成立时执行缩进的分支。
                    # 把固定状态或编码 PROTECTED（PROTECTED）交还给调用方，本函数到此结束。
                    if count<=1:return 'PROTECTED'
                    # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
                    # 执行传入的数据库语句并取得结果。
                    with c.cursor() as q:q.execute('UPDATE identity_user SET is_enabled=false,auth_version=auth_version+1 WHERE id=%s',(uid,))
                # 把固定状态或编码 DISABLED（已禁用）交还给调用方，本函数到此结束。
                return 'DISABLED'
            # 释放连接、文件或客户端占用的资源。
            finally:c.close()
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 ThreadPoolExecutor 完成Thread连接池Executor处理”得到的结果，命名为 pool；退出时由上下文管理器提交、回滚或释放资源。
            with ThreadPoolExecutor(max_workers=2) as pool:
                # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 fs。
                fs=[pool.submit(worker,uid) for uid in [self.super,second]]
                # 断言 Equal。
                self.assertEqual(sorted(f.result(timeout=10) for f in fs),['DISABLED','PROTECTED'])
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
            c=connect()
            # 进入c；退出时由上下文管理器提交、回滚或释放资源。
            with c:
                # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
                with c.cursor() as q:
                    # 执行传入的数据库语句并取得结果。
                    q.execute('UPDATE identity_user SET is_enabled=true,auth_version=0 WHERE id=%s',(self.super,))
                    # 执行传入的数据库语句并取得结果。
                    q.execute('DELETE FROM identity_user_role WHERE user_id=%s',(second,))
                    # 执行传入的数据库语句并取得结果。
                    q.execute('DELETE FROM identity_user WHERE id=%s',(second,))
            # 释放连接、文件或客户端占用的资源。
            c.close()

    # 函数 test_46_index_query_plans：调用 test_46_index_query_plans 完成test46索引查询条件方案列表处理。
    def test_46_index_query_plans(self):
        # 把依次包含 依次包含 固定文字 'EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE lower(username COLLATE …'、依次包含 固定文字 '测试用户' 的集合、固定文字 'uk_identity_user_username' 的集合、依次包含 固定文字 'EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE deleted_at IS NULL ORDE…'、空列表或集合、固定文字 'ix_identity_user_active_page' 的集合、依次包含 固定文字 'EXPLAIN (FORMAT JSON) SELECT id FROM identity_login_log WHERE user_id=%s ORDER B…'、依次包含 当前实例（self）的normal属性 的集合、固定文字 'ix_identity_login_log_user_time' 的集合 的集合保存到 cases。
        cases=[("EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE lower(username COLLATE \"C\")=lower(%s COLLATE \"C\")",('测试用户',),'uk_identity_user_username'),
               # 继续传入或组合表达式 ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE deleted_at IS NULL ORDER BY created_at DESC,id DESC LIMIT 10'… 的结果，完成上面尚未结束的表达式。
               ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE deleted_at IS NULL ORDER BY created_at DESC,id DESC LIMIT 10',(),'ix_identity_user_active_page'),
               # 继续传入或组合表达式 ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_login_log WHERE user_id=%s ORDER BY occurred_at DESC,id DESC LIMIT 10',(… 的结果，完成上面尚未结束的表达式。
               ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_login_log WHERE user_id=%s ORDER BY occurred_at DESC,id DESC LIMIT 10',(self.normal,),'ix_identity_login_log_user_time')]
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('SET LOCAL enable_seqscan=off')
            # 从cases依次取出statement参数集合索引（(statement, params, index)），每项执行一次下面的处理。
            for statement,params,index in cases:
                # 执行传入的数据库语句并取得结果。
                # 把从执行“取出查询结果中的一行”得到的结果中按固定数值 0取出的内容保存到 方案（plan）。
                q.execute(statement,params);plan=q.fetchone()[0]
                # 断言 In。
                self.assertIn(index,json.dumps(plan))
                # 把新条目放到列表末尾。
                INDEX_PLANS.append({'query':statement,'plan':plan,'note':'Small fixture: sequential scan disabled only to check index eligibility; not a performance benchmark.'})

    # 函数 test_47_menu_multi_node_cycle_detectable：调用 test_47_menu_multi_node_cycle_detectable 完成test47菜单multi节点cycledetectable处理。
    def test_47_menu_multi_node_cycle_detectable(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_menu SET parent_id=(SELECT id FROM identity_menu WHERE code='profile') WHERE code='account_root'")
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE,(self.normal,self.normal))
            # 断言 Equal。
            self.assertEqual(q.fetchall(),[])
            # 执行传入的数据库语句并取得结果。
            # 内嵌 SQL：先定义可复用的临时查询结果，后续语句继续引用它。
            # 内嵌 SQL：读取后面列出的字段或计算结果。
            # 内嵌 SQL：合并前后两段查询结果，保留重复行。
            # 内嵌 SQL：指定本次查询主要读取的表或子查询。
            # 内嵌 SQL：继续补齐上面的 SQL 表达式、字段值或约束组合：) SELECT count(*) FROM walk WHERE cycle。
            q.execute("""WITH RECURSIVE walk AS (
            SELECT id,parent_id,ARRAY[id] path,false cycle FROM identity_menu
            UNION ALL SELECT m.id,m.parent_id,w.path||m.id,m.id=ANY(w.path)
            FROM walk w JOIN identity_menu m ON m.id=w.parent_id WHERE NOT w.cycle
            ) SELECT count(*) FROM walk WHERE cycle""")
            # 断言 Greater。
            self.assertGreater(q.fetchone()[0],0)

    # 函数 test_48_menu_seed_references_valid：调用 test_48_menu_seed_references_valid 完成test48菜单seedreferences有效处理。
    def test_48_menu_seed_references_valid(self):
        # 断言 Equal。
        self.assertEqual(one(self.c,"SELECT count(*) FROM identity_menu m LEFT JOIN identity_menu p ON p.id=m.parent_id WHERE m.parent_id IS NOT NULL AND p.id IS NULL"),0)
        # 断言 Equal。
        self.assertEqual(one(self.c,"SELECT count(*) FROM identity_menu m LEFT JOIN identity_permission p ON p.id=m.permission_id WHERE m.permission_id IS NOT NULL AND p.id IS NULL"),0)

    # 函数 test_49_log_append_only_application_privileges：调用 test_49_log_append_only_application_privileges 完成test49日志追加仅applicationprivileges处理。
    def test_49_log_append_only_application_privileges(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute('GRANT USAGE ON SCHEMA public TO asterway_dev; GRANT SELECT,INSERT ON identity_login_log,identity_operation_log TO asterway_dev; GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO asterway_dev; SET LOCAL ROLE asterway_dev')
            # 执行传入的数据库语句并取得结果。
            q.execute("INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id) VALUES ('unknown','FAILURE','INVALID_CREDENTIALS','sql-test')")
        # 调用 self.reject 完成拒绝处理。
        self.reject('DELETE FROM identity_login_log',(),'42501')
        # 调用 self.reject 完成拒绝处理。
        self.reject("UPDATE identity_operation_log SET outcome='SUCCESS'",(),'42501')
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        # 执行传入的数据库语句并取得结果。
        with self.c.cursor() as q:q.execute('RESET ROLE')

    # 函数 test_50_transaction_rechecks_registration_switch：调用 test_50_transaction_rechecks_registration_switch 完成test50事务rechecksregistrationswitch处理。
    def test_50_transaction_rechecks_registration_switch(self):
        # 把执行“调用 connect 完成连接处理”得到的结果保存到 updater。
        # 把执行“调用 threading.Event 完成事件处理”得到的结果保存到 等待确认（waiting）。
        # 把空列表或集合保存到 backend。
        updater=connect();waiting=threading.Event();backend=[]
        # 进入执行“调用 updater.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with updater.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute("UPDATE identity_setting SET setting_value='false' WHERE setting_key='registration_enabled'")
        # 函数 registration：调用 registration 完成registration处理。
        def registration():
            # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
            c=connect()
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 把新条目放到列表末尾。
                # 调用 waiting.set 完成设置处理。
                backend.append(c.get_backend_pid());waiting.set()
                # 进入c；退出时由上下文管理器提交、回滚或释放资源。
                with c:
                    # 把执行“调用 one 完成one处理”得到的结果保存到 是否启用（enabled）。
                    enabled=one(c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled' FOR SHARE")
                    # 检查条件 not enabled 是否成立；成立时执行缩进的分支。
                    # 把固定状态或编码 CLOSED（CLOSED）交还给调用方，本函数到此结束。
                    if not enabled:return 'CLOSED'
                    # 调用 add_user 完成加入用户处理。
                    add_user(c,'注册开关并发')
                # 把固定状态或编码 CREATED（创建时）交还给调用方，本函数到此结束。
                return 'CREATED'
            # 释放连接、文件或客户端占用的资源。
            finally:c.close()
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 进入执行“调用 ThreadPoolExecutor 完成Thread连接池Executor处理”得到的结果，命名为 pool；退出时由上下文管理器提交、回滚或释放资源。
            with ThreadPoolExecutor(max_workers=1) as pool:
                # 把执行“调用 pool.submit 完成提交处理”得到的结果保存到 future。
                future=pool.submit(registration)
                # 断言 成立（True）。
                self.assertTrue(waiting.wait(3))
                # 把执行“调用 connect 完成连接处理”得到的结果保存到 observer。
                observer=connect()
                # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
                try:
                    # 把按 time.monotonic() + 3 计算的结果保存到 截止时间（deadline）。
                    # 把假，表示条件不成立或开关关闭保存到 blocked。
                    deadline=time.monotonic()+3;blocked=False
                    # 只要条件 time.monotonic() < deadline 是否成立，就继续执行下面的循环。
                    while time.monotonic()<deadline:
                        # 检查条件 one(observer, 'SELECT cardinality(pg_blocking_pids(%s))>0', (backend[0],)) 是否成立；成立时执行缩进的分支。
                        if one(observer,'SELECT cardinality(pg_blocking_pids(%s))>0',(backend[0],)):
                            # 把真，表示条件成立或开关打开保存到 blocked。
                            # 立即退出当前循环，不再处理后续条目。
                            blocked=True;break
                        # 等待指定时间后再继续，给轮询或其他任务留出执行机会。
                        time.sleep(0.01)
                    # 断言 成立（True）。
                    self.assertTrue(blocked)
                # 释放连接、文件或客户端占用的资源。
                finally:observer.close()
                # 提交本次数据库事务，使本次修改一起生效。
                updater.commit()
                # 断言 Equal。
                self.assertEqual(future.result(timeout=5),'CLOSED')
        # 不论前面成功、失败还是取消，都执行下面的资源清理。
        finally:
            # 回滚本次数据库事务，撤销这一轮尚未提交的修改。
            updater.rollback()
            # 进入updater；退出时由上下文管理器提交、回滚或释放资源。
            with updater:
                # 进入执行“调用 updater.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
                with updater.cursor() as q:
                    # 执行传入的数据库语句并取得结果。
                    q.execute("UPDATE identity_setting SET setting_value='true' WHERE setting_key='registration_enabled'")
                    # 执行传入的数据库语句并取得结果。
                    q.execute("DELETE FROM identity_user_role WHERE user_id IN (SELECT id FROM identity_user WHERE username='注册开关并发')")
                    # 执行传入的数据库语句并取得结果。
                    q.execute("DELETE FROM identity_user WHERE username='注册开关并发'")
            # 释放连接、文件或客户端占用的资源。
            updater.close()

    # 函数 test_51_disabled_user_no_menus：调用 test_51_disabled_user_no_menus 完成test51已禁用用户no菜单列表处理。
    def test_51_disabled_user_no_menus(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE,(self.disabled,self.disabled))
            # 断言 Equal。
            self.assertEqual(q.fetchall(),[])

    # 函数 test_52_deleted_user_no_menus：调用 test_52_deleted_user_no_menus 完成test52删除标记用户no菜单列表处理。
    def test_52_deleted_user_no_menus(self):
        # 进入执行“调用 self.c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
        with self.c.cursor() as q:
            # 执行传入的数据库语句并取得结果。
            q.execute(MENU_TREE,(self.deleted,self.deleted))
            # 断言 Equal。
            self.assertEqual(q.fetchall(),[])


# 把按字段名保存多个值的字典保存到 不合法用户列表（INVALID_USERS）。
INVALID_USERS = {
    # 填写空状态用户名（empty_username）字段，值为表达式 ({'username':''},'23514'), 'underscore':({'username':'bad_name'},'23514') 的结果。
    'empty_username':({'username':''},'23514'), 'underscore':({'username':'bad_name'},'23514'),
    # 填写space字段，值为表达式 ({'username':'bad name'},'23514'), 'emoji':({'username':'用户😀'},'23514') 的结果。
    'space':({'username':'bad name'},'23514'), 'emoji':({'username':'用户😀'},'23514'),
    # 填写too长整数（too_long）字段，值为按表达式计算得到的值：({'username':'a'*31},'22001'), 'null_username':({'username':None},'23502')。
    'too_long':({'username':'a'*31},'22001'), 'null_username':({'username':None},'23502'),
    # 填写blank昵称（blank_nickname）字段，值为按表达式计算得到的值：({'nickname':'\t\n '},'23514'), 'long_nickname':({'nickname':'a'*31},'22001')。
    'blank_nickname':({'nickname':'\t\n '},'23514'), 'long_nickname':({'nickname':'a'*31},'22001'),
    # 填写plaintext密码（plaintext_password）字段，值为表达式 ({'password_hash':'plaintext'},'23514'), 'negative_version':({'version':-1},'23514') 的结果。
    'plaintext_password':({'password_hash':'plaintext'},'23514'), 'negative_version':({'version':-1},'23514'),
    # 填写negative认证版本号（negative_auth_version）字段，值为表达式 ({'auth_version':-1},'23514'), 'blank_zone':({'time_zone':''},'23514') 的结果。
    'negative_auth_version':({'auth_version':-1},'23514'), 'blank_zone':({'time_zone':''},'23514'),
    # 填写等待处理without日期（pending_without_date）字段，值为表达式 ({'pending_time_zone':'UTC'},'23514') 的结果。
    'pending_without_date':({'pending_time_zone':'UTC'},'23514'),
    # 填写日期without等待处理（date_without_pending）字段，值为表达式 ({'time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514') 的结果。
    'date_without_pending':({'time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514'),
    # 填写等待处理same时区（pending_same_zone）字段，值为按表达式计算得到的值：({'pending_time_zone':'Asia/Shanghai','time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514')。
    'pending_same_zone':({'pending_time_zone':'Asia/Shanghai','time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514'),
    # 填写删除标记still是否启用（deleted_still_enabled）字段，值为表达式 ({'deleted_at':'2026-01-01T00:00:00Z'},'23514') 的结果。
    'deleted_still_enabled':({'deleted_at':'2026-01-01T00:00:00Z'},'23514'),
}
# 从执行“调用 INVALID_USERS.items 完成数据列表处理”得到的结果依次取出名称changes当前状态（(name, (changes, state))），每项执行一次下面的处理。
for name,(changes,state) in INVALID_USERS.items():
    # 函数 test：调用 test 完成test处理；接收 changes、当前状态（state）。
    def test(self, changes=changes,state=state):
        # 把执行“调用 dict 完成dict处理”得到的结果保存到 值列表（values）。
        # 调用 values.update 完成更新处理。
        values=dict(username='非法样例',password_hash=HASH,nickname='样例',time_zone='UTC');values.update(changes)
        # pending_same_zone 在这个测试的基准时区为 UTC。
        # 检查条件 changes.get('pending_time_zone') == 'Asia/Shanghai' 是否成立；成立时执行缩进的分支。
        # 把固定文字 'Asia/Shanghai'保存到 值列表时间时区（values['time_zone']）。
        if changes.get('pending_time_zone')=='Asia/Shanghai':values['time_zone']='Asia/Shanghai'
        # 把执行“调用 sql.SQL('INSERT INTO identity_user ({}) VALUES ({})').format 完成格式处理”得到的结果保存到 statement。
        statement=sql.SQL('INSERT INTO identity_user ({}) VALUES ({})').format(sql.SQL(',').join(map(sql.Identifier,values)),sql.SQL(',').join(sql.Placeholder() for _ in values))
        # 调用 self.reject 完成拒绝处理。
        self.reject(statement,tuple(values.values()),state)
    # 设置 attr 的值。
    setattr(IdentitySqlTests,'test_invalid_user_'+name,test)

# 把按字段名保存多个值的字典保存到 不合法STATEMENTS（INVALID_STATEMENTS）。
INVALID_STATEMENTS = {
    # 填写unknownsetting（unknown_setting）字段，值为表达式 ("INSERT INTO identity_setting(setting_key,setting_value) VALUES ('other','true')",'23514') 的结果。
    'unknown_setting':("INSERT INTO identity_setting(setting_key,setting_value) VALUES ('other','true')",'23514'),
    # 填写registrationnot真假值（registration_not_boolean）字段，值为表达式 ("UPDATE identity_setting SET setting_value='\"false\"' WHERE setting_key='registration_enabled'",'23514') 的结果。
    'registration_not_boolean':("UPDATE identity_setting SET setting_value='\"false\"' WHERE setting_key='registration_enabled'",'23514'),
    # 填写site空状态（site_empty）字段，值为表达式 ("UPDATE identity_setting SET setting_value='\"\"' WHERE setting_key='site_name'",'23514') 的结果。
    'site_empty':("UPDATE identity_setting SET setting_value='\"\"' WHERE setting_key='site_name'",'23514'),
    # 填写siteblank（site_blank）字段，值为表达式 ("UPDATE identity_setting SET setting_value='\" \"' WHERE setting_key='site_name'",'23514') 的结果。
    'site_blank':("UPDATE identity_setting SET setting_value='\"   \"' WHERE setting_key='site_name'",'23514'),
    # 填写sitenot文字（site_not_string）字段，值为表达式 ("UPDATE identity_setting SET setting_value='true' WHERE setting_key='site_name'",'23514') 的结果。
    'site_not_string':("UPDATE identity_setting SET setting_value='true' WHERE setting_key='site_name'",'23514'),
    # 填写duplicate角色（duplicate_role）字段，值为表达式 ("INSERT INTO identity_role(code,name) VALUES ('USER','重复')",'23505') 的结果。
    'duplicate_role':("INSERT INTO identity_role(code,name) VALUES ('USER','重复')",'23505'),
    # 填写不合法builtin（invalid_builtin）字段，值为表达式 ("INSERT INTO identity_role(code,name,is_builtin) VALUES ('VIP','无效内置角色',true)",'23514') 的结果。
    'invalid_builtin':("INSERT INTO identity_role(code,name,is_builtin) VALUES ('VIP','无效内置角色',true)",'23514'),
    # 填写不合法权限编码（invalid_permission_code）字段，值为表达式 ("INSERT INTO identity_permission(code,name) VALUES ('BAD','错误编码')",'23514') 的结果。
    'invalid_permission_code':("INSERT INTO identity_permission(code,name) VALUES ('BAD','错误编码')",'23514'),
    # 填写duplicate权限（duplicate_permission）字段，值为表达式 ("INSERT INTO identity_permission(code,name) SELECT code,name FROM identity_permission LIMIT 1",'23505') 的结果。
    'duplicate_permission':("INSERT INTO identity_permission(code,name) SELECT code,name FROM identity_permission LIMIT 1",'23505'),
    # 填写当前页missing路径（page_missing_path）字段，值为按表达式计算得到的值：("INSERT INTO identity_menu(code,name,menu_type,component_key,permission_id) VALUES ('bad_page','错误页面','PAGE','system/us…。
    'page_missing_path':("INSERT INTO identity_menu(code,name,menu_type,component_key,permission_id) VALUES ('bad_page','错误页面','PAGE','system/users',1)",'23514'),
    # 填写buttonmissing权限（button_missing_permission）字段，值为表达式 ("INSERT INTO identity_menu(code,name,menu_type) VALUES ('bad_button','错误按钮','BUTTON')",'23514') 的结果。
    'button_missing_permission':("INSERT INTO identity_menu(code,name,menu_type) VALUES ('bad_button','错误按钮','BUTTON')",'23514'),
    # 填写directory是否存在component（directory_has_component）字段，值为按表达式计算得到的值：("INSERT INTO identity_menu(code,name,menu_type,route_path,component_key) VALUES ('bad_dir','错误目录','DIRECTORY','/bad','b…。
    'directory_has_component':("INSERT INTO identity_menu(code,name,menu_type,route_path,component_key) VALUES ('bad_dir','错误目录','DIRECTORY','/bad','bad')",'23514'),
    # 填写菜单self上级（menu_self_parent）字段，值为表达式 ("UPDATE identity_menu SET parent_id=id WHERE code='profile'",'23514') 的结果。
    'menu_self_parent':("UPDATE identity_menu SET parent_id=id WHERE code='profile'",'23514'),
    # 填写duplicate菜单route（duplicate_menu_route）字段，值为按表达式计算得到的值：("INSERT INTO identity_menu(code,name,menu_type,route_path) VALUES ('dupe_route','重复路由','DIRECTORY','/account')",'23505'…。
    'duplicate_menu_route':("INSERT INTO identity_menu(code,name,menu_type,route_path) VALUES ('dupe_route','重复路由','DIRECTORY','/account')",'23505'),
    # 填写汇总not对象（summary_not_object）字段，值为表达式 ("INSERT INTO identity_operation_log(resource_type,action_code,outcome,trace_id,change_summary) VALUES ('USER','TEST','S… 的结果。
    'summary_not_object':("INSERT INTO identity_operation_log(resource_type,action_code,outcome,trace_id,change_summary) VALUES ('USER','TEST','SUCCESS','sql-test','[]')",'23514'),
    # 填写不合法ip（invalid_ip）字段，值为表达式 ("INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id,ip_address) VALUES ('user','FAILURE','BA… 的结果。
    'invalid_ip':("INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id,ip_address) VALUES ('user','FAILURE','BAD','sql-test','invalid-ip')",'22P02'),
}
# 从执行“调用 INVALID_STATEMENTS.items 完成数据列表处理”得到的结果依次取出名称statement当前状态（(name, (statement, state))），每项执行一次下面的处理。
for name,(statement,state) in INVALID_STATEMENTS.items():
    # 函数 test：调用 test 完成test处理；接收 statement、当前状态（state）。
    # 调用 self.reject 完成拒绝处理。
    def test(self,statement=statement,state=state):self.reject(statement,(),state)
    # 设置 attr 的值。
    setattr(IdentitySqlTests,'test_invalid_'+name,test)

# 把空列表或集合保存到 索引方案列表（INDEX_PLANS）。
INDEX_PLANS=[]
# 定义 Result，把结果需要的数据与操作放在同一个对象中。
class Result(unittest.TextTestResult):
    # 函数 startTest：调用 startTest 完成开始Test处理；接收 test。
    def startTest(self,test):
        # 调用 super().startTest 完成开始Test处理。
        # 把执行“调用 time.perf_counter 完成perf计数器处理”得到的结果保存到 当前实例中的开始时（self.started）。
        super().startTest(test);self.started=time.perf_counter()
    # 函数 addSuccess：调用 addSuccess 完成加入成功处理；接收 test。
    def addSuccess(self,test):
        # 调用 super().addSuccess 完成加入成功处理。
        # 把新条目放到列表末尾。
        super().addSuccess(test);RECORDS.append({'test':test.id().split('.')[-1],'status':'PASS','seconds':round(time.perf_counter()-self.started,4)})
    # 函数 addFailure：调用 addFailure 完成加入失败处理；接收 test、捕获到的异常（err）。
    def addFailure(self,test,err):
        # 调用 super().addFailure 完成加入失败处理。
        # 把新条目放到列表末尾。
        super().addFailure(test,err);RECORDS.append({'test':test.id().split('.')[-1],'status':'FAIL','detail':str(err[1])})
    # 函数 addError：调用 addError 完成加入错误处理；接收 test、捕获到的异常（err）。
    def addError(self,test,err):
        # 调用 super().addError 完成加入错误处理。
        # 把新条目放到列表末尾。
        super().addError(test,err);RECORDS.append({'test':test.id().split('.')[-1],'status':'ERROR','detail':str(err[1])})

# 把空列表或集合保存到 记录列表（RECORDS）。
RECORDS=[]
# 函数 main：调用 main 完成程序入口处理。
def main():
    # 把执行“调用 argparse.ArgumentParser 完成命令行参数读取器处理”得到的结果保存到 parser。
    parser=argparse.ArgumentParser(description=__doc__)
    # 声明一个命令行参数的名称、类型和限制。
    parser.add_argument('--reset-test-schema',action='store_true',required=True)
    # 声明一个命令行参数的名称、类型和限制。
    parser.add_argument('--report',type=Path,required=True)
    # 把执行“读取命令行参数并校验必填项”得到的结果保存到 参数（args）。
    args=parser.parse_args()
    # 把执行“调用 connect 完成连接处理”得到的结果保存到 c。
    # 把真，表示条件成立或开关打开保存到 c中的autocommit（c.autocommit）。
    c=connect();c.autocommit=True
    # 断言条件 one(c, 'SELECT current_database()') == 'asterway_identity_test' 是否成立；不满足时本项检查失败。
    assert one(c,'SELECT current_database()')=='asterway_identity_test'
    # 断言one(c, 'SELECT inet_server_addr()') is None，即对应内容是否不存在；不满足时本项检查失败。
    assert one(c,'SELECT inet_server_addr()') is None, 'Only local Unix socket is allowed.'
    # 进入执行“调用 c.cursor 完成数据库语句执行器处理”得到的结果，命名为 q；退出时由上下文管理器提交、回滚或释放资源。
    with c.cursor() as q:
        # 执行传入的数据库语句并取得结果。
        q.execute('DROP SCHEMA public CASCADE; CREATE SCHEMA public')
        # 执行传入的数据库语句并取得结果。
        q.execute(DDL)
    # 把执行“调用 one 完成one处理”得到的结果保存到 版本号（version）。
    # 释放连接、文件或客户端占用的资源。
    version=one(c,'SHOW server_version');c.close()
    # 把执行“调用 unittest.TextTestRunner(verbosity=2, resultclass=Result).run 完成方案运行处理”得到的结果保存到 结果（result）。
    result=unittest.TextTestRunner(verbosity=2,resultclass=Result).run(unittest.defaultTestLoader.loadTestsFromTestCase(IdentitySqlTests))
    # 把按字段名保存多个值的字典保存到 复盘报告（report）。
    report={'database':'asterway_identity_test','postgresql':version,'sql_sha256':hashlib.sha256(DDL.encode('utf-8')).hexdigest(),'tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'records':RECORDS,'index_plans':INDEX_PLANS,'boundary':'SQL constraints, queries and transaction prototypes only; no Mapper, Service, JWT, CAPTCHA or HTTP implementation has been tested.'}
    # 调用 args.report.parent.mkdir 完成mkdir处理。
    # 按指定编码写入文件文字。
    args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    # 中止本轮正常处理，抛出 执行“调用 SystemExit 完成系统Exit处理”得到的结果。
    raise SystemExit(0 if result.wasSuccessful() else 1)


# 检查条件 __name__ == '__main__' 是否成立；成立时执行缩进的分支。
# 调用 main 完成程序入口处理。
if __name__=='__main__':main()
