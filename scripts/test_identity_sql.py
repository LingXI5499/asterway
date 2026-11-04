"""真实 SQL 验证，仅重建本机 asterway_identity_test.public；不测试尚未实现的 HTTP 业务。"""
import argparse
import hashlib
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import threading
import time
import unittest
import uuid

import psycopg2
from psycopg2 import sql
from argon2 import PasswordHasher

ROOT = Path(__file__).resolve().parents[1]
DDL = (ROOT / 'backend/app/src/main/resources/sql/identity.sql').read_text(encoding='utf-8')
DSN = 'dbname=asterway_identity_test user=postgres host=/var/run/postgresql'
HASH = PasswordHasher(time_cost=2, memory_cost=19456, parallelism=1).hash('OnlyForSqlTests123')
PERMISSIONS = """SELECT DISTINCT p.code FROM identity_user u
JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id
JOIN identity_role_permission rp ON rp.role_id=r.id JOIN identity_permission p ON p.id=rp.permission_id
WHERE u.id=%s AND u.is_enabled AND u.deleted_at IS NULL AND r.is_enabled AND r.deleted_at IS NULL
AND p.is_enabled AND p.deleted_at IS NULL ORDER BY p.code"""
MANAGEABLE = """SELECT u.id FROM identity_user u WHERE u.deleted_at IS NULL AND NOT EXISTS (
SELECT 1 FROM identity_user_role ur JOIN identity_role r ON r.id=ur.role_id
WHERE ur.user_id=u.id AND r.code IN ('ADMIN','SUPER_ADMIN')) ORDER BY u.id"""
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


def connect():
    c = psycopg2.connect(DSN, application_name='asterway_identity_sql_test')
    with c.cursor() as q:
        q.execute("SET statement_timeout='8s'; SET lock_timeout='4s'; SET TIME ZONE 'UTC'")
    c.commit()
    return c


def one(c, statement, params=()):
    with c.cursor() as q:
        q.execute(statement, params or None)
        return q.fetchone()[0]


def add_user(c, name, roles=('USER',), **changes):
    values = dict(username=name, password_hash=HASH, nickname=name, time_zone='Asia/Shanghai')
    values.update(changes)
    uid = one(c, sql.SQL('INSERT INTO identity_user ({}) VALUES ({}) RETURNING id').format(
        sql.SQL(',').join(map(sql.Identifier, values)), sql.SQL(',').join(sql.Placeholder() for _ in values)), tuple(values.values()))
    with c.cursor() as q:
        q.execute('INSERT INTO identity_user_role(user_id,role_id) SELECT %s,id FROM identity_role WHERE code=ANY(%s)', (uid, list(roles)))
    return uid


class IdentitySqlTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.c = connect()
        cls.normal = add_user(cls.c, '测试用户')
        cls.admin = add_user(cls.c, '测试管理员', ('USER', 'ADMIN'))
        cls.super = add_user(cls.c, '测试超管', ('USER', 'SUPER_ADMIN'))
        cls.disabled = add_user(cls.c, '停用用户', is_enabled=False)
        cls.deleted = add_user(cls.c, '已删用户', is_enabled=False, deleted_at='2026-01-01T00:00:00Z')
        cls.c.commit()

    @classmethod
    def tearDownClass(cls):
        cls.c.close()

    def tearDown(self):
        self.c.rollback()

    def reject(self, statement, params, state):
        with self.c.cursor() as q:
            q.execute('SAVEPOINT rejected_input')
            with self.assertRaises(psycopg2.Error) as error:
                q.execute(statement, params)
            self.assertEqual(error.exception.pgcode, state)
            q.execute('ROLLBACK TO SAVEPOINT rejected_input')

    def codes(self, uid):
        with self.c.cursor() as q:
            q.execute(PERMISSIONS, (uid,))
            return {row[0] for row in q.fetchall()}

    def test_01_server_version(self):
        self.assertEqual(one(self.c, "SHOW server_version_num"), '170011')

    def test_02_exactly_ten_tables(self):
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_tables WHERE schemaname='public' AND tablename LIKE 'identity_%'"), 10)

    def test_03_no_foreign_keys(self):
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_constraint WHERE contype='f' AND connamespace='public'::regnamespace"), 0)

    def test_04_no_triggers(self):
        self.assertEqual(one(self.c, "SELECT count(*) FROM pg_trigger WHERE NOT tgisinternal AND tgrelid IN (SELECT oid FROM pg_class WHERE relnamespace='public'::regnamespace)"), 0)

    def test_05_three_builtin_roles(self):
        self.assertEqual(one(self.c, "SELECT array_agg(code ORDER BY code) FROM identity_role WHERE is_builtin"), ['ADMIN', 'SUPER_ADMIN', 'USER'])

    def test_06_seed_counts(self):
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_permission'), 37)
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_menu'), 39)
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_setting'), 2)

    def test_07_username_case_insensitive(self):
        add_user(self.c, 'LingXI')
        self.reject('INSERT INTO identity_user(username,password_hash,nickname,time_zone) VALUES (%s,%s,%s,%s)', ('lingxi', HASH, '重复', 'UTC'), '23505')

    def test_08_deleted_username_reserved(self):
        self.reject('INSERT INTO identity_user(username,password_hash,nickname,time_zone) VALUES (%s,%s,%s,%s)', ('已删用户', HASH, '重复', 'UTC'), '23505')

    def test_09_valid_username_boundaries(self):
        for name in ['甲', 'A'*30, '中文ABC123', '\U00020000']:
            self.assertGreater(add_user(self.c, name), 0)

    def test_10_nicknames_can_repeat(self):
        add_user(self.c, '同昵称甲', nickname='相同昵称')
        add_user(self.c, '同昵称乙', nickname='相同昵称')
        self.assertEqual(one(self.c, "SELECT count(*) FROM identity_user WHERE nickname='相同昵称'"), 2)

    def test_11_user_role_pair_unique(self):
        self.reject("INSERT INTO identity_user_role(user_id,role_id) SELECT %s,id FROM identity_role WHERE code='USER'", (self.normal,), '23505')

    def test_12_role_permission_pair_unique(self):
        self.reject('INSERT INTO identity_role_permission(role_id,permission_id) SELECT role_id,permission_id FROM identity_role_permission LIMIT 1', (), '23505')

    def test_13_role_menu_pair_unique(self):
        self.reject('INSERT INTO identity_role_menu(role_id,menu_id) SELECT role_id,menu_id FROM identity_role_menu LIMIT 1', (), '23505')

    def test_14_normal_permissions(self):
        self.assertEqual(len(self.codes(self.normal)), 7)
        self.assertNotIn('identity:user:list', self.codes(self.normal))

    def test_15_admin_permission_union(self):
        rights = self.codes(self.admin)
        self.assertEqual(len(rights), 19)
        self.assertIn('identity:user:reset_password', rights)
        self.assertNotIn('identity:user:assign_role', rights)
        self.assertFalse(any(x.startswith('identity:role:') for x in rights))

    def test_16_super_has_all_seed_permissions(self):
        self.assertEqual(len(self.codes(self.super)), 37)

    def test_17_disabled_user_no_permissions(self):
        self.assertEqual(self.codes(self.disabled), set())

    def test_18_deleted_user_no_permissions(self):
        self.assertEqual(self.codes(self.deleted), set())

    def test_19_disabled_role_filters_rights(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_role SET is_enabled=false WHERE code='ADMIN'")
        self.assertEqual(len(self.codes(self.admin)), 7)

    def test_20_permission_change_visible_next_query(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_permission SET is_enabled=false WHERE code='identity:user:list'")
        self.assertNotIn('identity:user:list', self.codes(self.admin))

    def test_21_admin_target_scope(self):
        with self.c.cursor() as q:
            q.execute(MANAGEABLE)
            ids = {x[0] for x in q.fetchall()}
        self.assertEqual(ids, {self.normal, self.disabled})

    def test_22_normal_menu_tree(self):
        with self.c.cursor() as q:
            q.execute(MENU_TREE, (self.normal, self.normal))
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root', 'profile', 'devices'})

    def test_23_admin_menu_tree(self):
        with self.c.cursor() as q:
            q.execute(MENU_TREE, (self.admin, self.admin))
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root','profile','devices','system_root','users','sessions','login_logs','operation_logs'})

    def test_24_disabled_parent_hides_branch(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_menu SET is_enabled=false WHERE code='system_root'")
            q.execute(MENU_TREE, (self.super, self.super))
            self.assertEqual({x[0] for x in q.fetchall()}, {'account_root','profile','devices'})

    def test_25_menu_grant_does_not_grant_api(self):
        with self.c.cursor() as q:
            q.execute("INSERT INTO identity_role_menu(role_id,menu_id) SELECT r.id,m.id FROM identity_role r CROSS JOIN identity_menu m WHERE r.code='USER' AND m.code IN ('users','system_root')")
            q.execute(MENU_TREE, (self.normal, self.normal))
            self.assertNotIn('users', {x[0] for x in q.fetchall()})
        self.assertNotIn('identity:user:list', self.codes(self.normal))

    def test_26_orphan_association_is_not_db_constraint(self):
        with self.c.cursor() as q:
            q.execute("INSERT INTO identity_user_role(user_id,role_id) SELECT 999999,id FROM identity_role WHERE code='USER'")
        self.assertEqual(one(self.c, 'SELECT count(*) FROM identity_user_role ur LEFT JOIN identity_user u ON u.id=ur.user_id WHERE u.id IS NULL'), 1)
        # 这是无外键设计的实际边界：SQL 允许孤儿，后续 Service 必须拒绝。

    def test_27_seed_associations_are_valid(self):
        for relation,left,right in [('user_role','user','role'),('role_permission','role','permission'),('role_menu','role','menu')]:
            statement=f'SELECT count(*) FROM identity_{relation} x LEFT JOIN identity_{left} l ON l.id=x.{left}_id LEFT JOIN identity_{right} r ON r.id=x.{right}_id WHERE l.id IS NULL OR r.id IS NULL'
            self.assertEqual(one(self.c, statement), 0)

    def test_28_optimistic_version_compare(self):
        with self.c.cursor() as q:
            q.execute('UPDATE identity_user SET nickname=%s,version=version+1 WHERE id=%s AND version=0', ('新版',self.normal))
            self.assertEqual(q.rowcount, 1)
            q.execute('UPDATE identity_user SET nickname=%s,version=version+1 WHERE id=%s AND version=0', ('旧请求',self.normal))
            self.assertEqual(q.rowcount, 0)

    def test_29_user_role_transaction_rollback(self):
        name='事务回滚'
        with self.c.cursor() as q:
            q.execute('SAVEPOINT registration')
            uid=add_user(self.c,name,roles=())
            with self.assertRaises(psycopg2.Error):
                q.execute('INSERT INTO identity_user_role(user_id,role_id) VALUES (%s,0)', (uid,))
            q.execute('ROLLBACK TO SAVEPOINT registration')
        self.assertEqual(one(self.c,'SELECT count(*) FROM identity_user WHERE username=%s',(name,)),0)

    def test_30_grant_replacement_rollback(self):
        with self.c.cursor() as q:
            q.execute('SAVEPOINT grant_replace')
            q.execute("DELETE FROM identity_role_permission WHERE role_id=(SELECT id FROM identity_role WHERE code='ADMIN')")
            with self.assertRaises(psycopg2.Error):
                q.execute('INSERT INTO identity_role_permission VALUES (0,0,now(),NULL)')
            q.execute('ROLLBACK TO SAVEPOINT grant_replace')
        self.assertEqual(len(self.codes(self.admin)),19)

    def test_31_soft_delete_preserves_logs(self):
        with self.c.cursor() as q:
            q.execute("INSERT INTO identity_login_log(user_id,username_snapshot,outcome,reason_code,trace_id) VALUES (%s,'测试用户','FAILURE','INVALID_CREDENTIALS','sql-test')",(self.normal,))
            q.execute('UPDATE identity_user SET is_enabled=false,deleted_at=now(),auth_version=auth_version+1 WHERE id=%s',(self.normal,))
        self.assertEqual(self.codes(self.normal),set())
        self.assertEqual(one(self.c,'SELECT count(*) FROM identity_login_log WHERE user_id=%s',(self.normal,)),1)

    def test_32_security_version_increments(self):
        self.assertEqual(one(self.c,'UPDATE identity_user SET auth_version=auth_version+1 WHERE id=%s RETURNING auth_version',(self.normal,)),1)

    def test_33_registration_settings(self):
        self.assertEqual(one(self.c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled'"),True)
        with self.c.cursor() as q:
            q.execute("UPDATE identity_setting SET setting_value='false',version=version+1 WHERE setting_key='registration_enabled'")
        self.assertEqual(one(self.c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled'"),False)

    def test_34_timezone_names_validation_query(self):
        self.assertTrue(one(self.c,'SELECT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name=%s)',('Asia/Shanghai',)))
        self.assertFalse(one(self.c,'SELECT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name=%s)',('Invalid/Zone',)))

    def test_35_timezone_dst_day_lengths(self):
        for day,hours in [('2026-03-08',23),('2026-11-01',25)]:
            length=one(self.c,"SELECT extract(epoch FROM (((%s::date+1)::timestamp AT TIME ZONE 'America/New_York')-(%s::date::timestamp AT TIME ZONE 'America/New_York')))/3600",(day,day))
            self.assertEqual(length,hours)

    def test_36_pending_timezone_not_early(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_user SET pending_time_zone='UTC',time_zone_effective_at=now()+interval '1 day' WHERE id=%s",(self.normal,))
            q.execute('UPDATE identity_user SET time_zone=pending_time_zone,pending_time_zone=NULL,time_zone_effective_at=NULL WHERE id=%s AND time_zone_effective_at<=now()',(self.normal,))
            self.assertEqual(q.rowcount,0)

    def test_37_pending_timezone_apply_once(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_user SET pending_time_zone='UTC',time_zone_effective_at=now()-interval '1 second' WHERE id=%s",(self.normal,))
            statement='UPDATE identity_user SET time_zone=pending_time_zone,pending_time_zone=NULL,time_zone_effective_at=NULL,version=version+1 WHERE id=%s AND time_zone_effective_at<=now()'
            q.execute(statement,(self.normal,));self.assertEqual(q.rowcount,1)
            q.execute(statement,(self.normal,));self.assertEqual(q.rowcount,0)

    def test_38_unknown_username_login_log(self):
        self.assertGreater(one(self.c,"INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id,ip_address) VALUES ('unknown','FAILURE','INVALID_CREDENTIALS','sql-test','::1') RETURNING id"),0)

    def test_39_success_login_requires_session(self):
        self.reject("INSERT INTO identity_login_log(user_id,username_snapshot,outcome,reason_code,trace_id) VALUES (%s,'user','SUCCESS','SUCCESS','sql-test')",(self.normal,),'23514')

    def test_40_safe_operation_summary_roundtrip(self):
        summary={'before':{'isEnabled':True},'after':{'isEnabled':False}}
        actual=one(self.c,"INSERT INTO identity_operation_log(actor_id,target_user_id,resource_type,resource_id,action_code,outcome,change_summary,trace_id) VALUES (%s,%s,'USER',%s,'identity:user:status','SUCCESS',%s,'sql-test') RETURNING change_summary",(self.admin,self.normal,str(self.normal),json.dumps(summary)))
        self.assertEqual(actual,summary)

    def test_41_pagination_stable(self):
        with self.c.cursor() as q:
            q.execute(MANAGEABLE+' LIMIT 1 OFFSET 0'); first=q.fetchone()[0]
            q.execute(MANAGEABLE+' LIMIT 1 OFFSET 1'); second=q.fetchone()[0]
        self.assertNotEqual(first,second)

    def test_42_initialization_failure_is_atomic(self):
        with self.c.cursor() as q:
            q.execute('CREATE SCHEMA identity_init_failure')
        self.c.commit()
        c=connect();c.autocommit=True
        try:
            with c.cursor() as q:
                q.execute('SET search_path=identity_init_failure')
                with self.assertRaises(psycopg2.Error):
                    q.execute(DDL.replace('COMMIT;',"INSERT INTO identity_setting VALUES ('bad','null',0,now(),now(),NULL);\nCOMMIT;"))
                q.execute('ROLLBACK')
                self.assertEqual(one(c,"SELECT count(*) FROM pg_tables WHERE schemaname='identity_init_failure'"),0)
        finally:
            c.close()
            with self.c.cursor() as q:q.execute('DROP SCHEMA identity_init_failure')
            self.c.commit()

    def test_43_reapplication_refuses_existing_schema(self):
        c=connect();c.autocommit=True
        try:
            with c.cursor() as q:
                with self.assertRaises(psycopg2.Error) as error:q.execute(DDL)
                self.assertEqual(error.exception.pgcode,'42P07')
                q.execute('ROLLBACK')
            self.assertEqual(one(c,'SELECT count(*) FROM identity_role'),3)
        finally:c.close()

    def test_44_concurrent_username_uniqueness(self):
        name='并发'+uuid.uuid4().hex[:12];barrier=threading.Barrier(2)
        def worker():
            c=connect()
            try:
                barrier.wait(timeout=5)
                with c: add_user(c,name)
                return 'CREATED'
            except psycopg2.errors.UniqueViolation:return 'DUPLICATE'
            finally:c.close()
        try:
            with ThreadPoolExecutor(max_workers=2) as pool:
                futures=[pool.submit(worker) for _ in range(2)]
                self.assertEqual(sorted(f.result(timeout=10) for f in futures),['CREATED','DUPLICATE'])
        finally:
            c=connect()
            with c:
                with c.cursor() as q:
                    q.execute('DELETE FROM identity_user_role WHERE user_id IN (SELECT id FROM identity_user WHERE username=%s)',(name,))
                    q.execute('DELETE FROM identity_user WHERE username=%s',(name,))
            c.close()

    def test_45_last_super_guard_serializes(self):
        c=connect()
        with c:second=add_user(c,'超管并发'+uuid.uuid4().hex[:8],('USER','SUPER_ADMIN'))
        c.close();barrier=threading.Barrier(2)
        def worker(uid):
            c=connect()
            try:
                barrier.wait(timeout=5)
                with c:
                    one(c,"SELECT id FROM identity_role WHERE code='SUPER_ADMIN' FOR UPDATE")
                    count=one(c,"SELECT count(DISTINCT u.id) FROM identity_user u JOIN identity_user_role ur ON ur.user_id=u.id JOIN identity_role r ON r.id=ur.role_id WHERE r.code='SUPER_ADMIN' AND u.is_enabled AND u.deleted_at IS NULL")
                    if count<=1:return 'PROTECTED'
                    with c.cursor() as q:q.execute('UPDATE identity_user SET is_enabled=false,auth_version=auth_version+1 WHERE id=%s',(uid,))
                return 'DISABLED'
            finally:c.close()
        try:
            with ThreadPoolExecutor(max_workers=2) as pool:
                fs=[pool.submit(worker,uid) for uid in [self.super,second]]
                self.assertEqual(sorted(f.result(timeout=10) for f in fs),['DISABLED','PROTECTED'])
        finally:
            c=connect()
            with c:
                with c.cursor() as q:
                    q.execute('UPDATE identity_user SET is_enabled=true,auth_version=0 WHERE id=%s',(self.super,))
                    q.execute('DELETE FROM identity_user_role WHERE user_id=%s',(second,))
                    q.execute('DELETE FROM identity_user WHERE id=%s',(second,))
            c.close()

    def test_46_index_query_plans(self):
        cases=[("EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE lower(username COLLATE \"C\")=lower(%s COLLATE \"C\")",('测试用户',),'uk_identity_user_username'),
               ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_user WHERE deleted_at IS NULL ORDER BY created_at DESC,id DESC LIMIT 10',(),'ix_identity_user_active_page'),
               ('EXPLAIN (FORMAT JSON) SELECT id FROM identity_login_log WHERE user_id=%s ORDER BY occurred_at DESC,id DESC LIMIT 10',(self.normal,),'ix_identity_login_log_user_time')]
        with self.c.cursor() as q:
            q.execute('SET LOCAL enable_seqscan=off')
            for statement,params,index in cases:
                q.execute(statement,params);plan=q.fetchone()[0]
                self.assertIn(index,json.dumps(plan))
                INDEX_PLANS.append({'query':statement,'plan':plan,'note':'Small fixture: sequential scan disabled only to check index eligibility; not a performance benchmark.'})

    def test_47_menu_multi_node_cycle_detectable(self):
        with self.c.cursor() as q:
            q.execute("UPDATE identity_menu SET parent_id=(SELECT id FROM identity_menu WHERE code='profile') WHERE code='account_root'")
            q.execute(MENU_TREE,(self.normal,self.normal))
            self.assertEqual(q.fetchall(),[])
            q.execute("""WITH RECURSIVE walk AS (
            SELECT id,parent_id,ARRAY[id] path,false cycle FROM identity_menu
            UNION ALL SELECT m.id,m.parent_id,w.path||m.id,m.id=ANY(w.path)
            FROM walk w JOIN identity_menu m ON m.id=w.parent_id WHERE NOT w.cycle
            ) SELECT count(*) FROM walk WHERE cycle""")
            self.assertGreater(q.fetchone()[0],0)

    def test_48_menu_seed_references_valid(self):
        self.assertEqual(one(self.c,"SELECT count(*) FROM identity_menu m LEFT JOIN identity_menu p ON p.id=m.parent_id WHERE m.parent_id IS NOT NULL AND p.id IS NULL"),0)
        self.assertEqual(one(self.c,"SELECT count(*) FROM identity_menu m LEFT JOIN identity_permission p ON p.id=m.permission_id WHERE m.permission_id IS NOT NULL AND p.id IS NULL"),0)

    def test_49_log_append_only_application_privileges(self):
        with self.c.cursor() as q:
            q.execute('GRANT USAGE ON SCHEMA public TO asterway_dev; GRANT SELECT,INSERT ON identity_login_log,identity_operation_log TO asterway_dev; GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO asterway_dev; SET LOCAL ROLE asterway_dev')
            q.execute("INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id) VALUES ('unknown','FAILURE','INVALID_CREDENTIALS','sql-test')")
        self.reject('DELETE FROM identity_login_log',(),'42501')
        self.reject("UPDATE identity_operation_log SET outcome='SUCCESS'",(),'42501')
        with self.c.cursor() as q:q.execute('RESET ROLE')

    def test_50_transaction_rechecks_registration_switch(self):
        updater=connect();waiting=threading.Event();backend=[]
        with updater.cursor() as q:
            q.execute("UPDATE identity_setting SET setting_value='false' WHERE setting_key='registration_enabled'")
        def registration():
            c=connect()
            try:
                backend.append(c.get_backend_pid());waiting.set()
                with c:
                    enabled=one(c,"SELECT setting_value FROM identity_setting WHERE setting_key='registration_enabled' FOR SHARE")
                    if not enabled:return 'CLOSED'
                    add_user(c,'注册开关并发')
                return 'CREATED'
            finally:c.close()
        try:
            with ThreadPoolExecutor(max_workers=1) as pool:
                future=pool.submit(registration)
                self.assertTrue(waiting.wait(3))
                observer=connect()
                try:
                    deadline=time.monotonic()+3;blocked=False
                    while time.monotonic()<deadline:
                        if one(observer,'SELECT cardinality(pg_blocking_pids(%s))>0',(backend[0],)):
                            blocked=True;break
                        time.sleep(0.01)
                    self.assertTrue(blocked)
                finally:observer.close()
                updater.commit()
                self.assertEqual(future.result(timeout=5),'CLOSED')
        finally:
            updater.rollback()
            with updater:
                with updater.cursor() as q:
                    q.execute("UPDATE identity_setting SET setting_value='true' WHERE setting_key='registration_enabled'")
                    q.execute("DELETE FROM identity_user_role WHERE user_id IN (SELECT id FROM identity_user WHERE username='注册开关并发')")
                    q.execute("DELETE FROM identity_user WHERE username='注册开关并发'")
            updater.close()

    def test_51_disabled_user_no_menus(self):
        with self.c.cursor() as q:
            q.execute(MENU_TREE,(self.disabled,self.disabled))
            self.assertEqual(q.fetchall(),[])

    def test_52_deleted_user_no_menus(self):
        with self.c.cursor() as q:
            q.execute(MENU_TREE,(self.deleted,self.deleted))
            self.assertEqual(q.fetchall(),[])


INVALID_USERS = {
    'empty_username':({'username':''},'23514'), 'underscore':({'username':'bad_name'},'23514'),
    'space':({'username':'bad name'},'23514'), 'emoji':({'username':'用户😀'},'23514'),
    'too_long':({'username':'a'*31},'22001'), 'null_username':({'username':None},'23502'),
    'blank_nickname':({'nickname':'\t\n '},'23514'), 'long_nickname':({'nickname':'a'*31},'22001'),
    'plaintext_password':({'password_hash':'plaintext'},'23514'), 'negative_version':({'version':-1},'23514'),
    'negative_auth_version':({'auth_version':-1},'23514'), 'blank_zone':({'time_zone':''},'23514'),
    'pending_without_date':({'pending_time_zone':'UTC'},'23514'),
    'date_without_pending':({'time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514'),
    'pending_same_zone':({'pending_time_zone':'Asia/Shanghai','time_zone_effective_at':'2026-11-04T00:00:00Z'},'23514'),
    'deleted_still_enabled':({'deleted_at':'2026-01-01T00:00:00Z'},'23514'),
}
for name,(changes,state) in INVALID_USERS.items():
    def test(self, changes=changes,state=state):
        values=dict(username='非法样例',password_hash=HASH,nickname='样例',time_zone='UTC');values.update(changes)
        # pending_same_zone 在这个测试的基准时区为 UTC。
        if changes.get('pending_time_zone')=='Asia/Shanghai':values['time_zone']='Asia/Shanghai'
        statement=sql.SQL('INSERT INTO identity_user ({}) VALUES ({})').format(sql.SQL(',').join(map(sql.Identifier,values)),sql.SQL(',').join(sql.Placeholder() for _ in values))
        self.reject(statement,tuple(values.values()),state)
    setattr(IdentitySqlTests,'test_invalid_user_'+name,test)

INVALID_STATEMENTS = {
    'unknown_setting':("INSERT INTO identity_setting(setting_key,setting_value) VALUES ('other','true')",'23514'),
    'registration_not_boolean':("UPDATE identity_setting SET setting_value='\"false\"' WHERE setting_key='registration_enabled'",'23514'),
    'site_empty':("UPDATE identity_setting SET setting_value='\"\"' WHERE setting_key='site_name'",'23514'),
    'site_blank':("UPDATE identity_setting SET setting_value='\"   \"' WHERE setting_key='site_name'",'23514'),
    'site_not_string':("UPDATE identity_setting SET setting_value='true' WHERE setting_key='site_name'",'23514'),
    'duplicate_role':("INSERT INTO identity_role(code,name) VALUES ('USER','重复')",'23505'),
    'invalid_builtin':("INSERT INTO identity_role(code,name,is_builtin) VALUES ('VIP','无效内置角色',true)",'23514'),
    'invalid_permission_code':("INSERT INTO identity_permission(code,name) VALUES ('BAD','错误编码')",'23514'),
    'duplicate_permission':("INSERT INTO identity_permission(code,name) SELECT code,name FROM identity_permission LIMIT 1",'23505'),
    'page_missing_path':("INSERT INTO identity_menu(code,name,menu_type,component_key,permission_id) VALUES ('bad_page','错误页面','PAGE','system/users',1)",'23514'),
    'button_missing_permission':("INSERT INTO identity_menu(code,name,menu_type) VALUES ('bad_button','错误按钮','BUTTON')",'23514'),
    'directory_has_component':("INSERT INTO identity_menu(code,name,menu_type,route_path,component_key) VALUES ('bad_dir','错误目录','DIRECTORY','/bad','bad')",'23514'),
    'menu_self_parent':("UPDATE identity_menu SET parent_id=id WHERE code='profile'",'23514'),
    'duplicate_menu_route':("INSERT INTO identity_menu(code,name,menu_type,route_path) VALUES ('dupe_route','重复路由','DIRECTORY','/account')",'23505'),
    'summary_not_object':("INSERT INTO identity_operation_log(resource_type,action_code,outcome,trace_id,change_summary) VALUES ('USER','TEST','SUCCESS','sql-test','[]')",'23514'),
    'invalid_ip':("INSERT INTO identity_login_log(username_snapshot,outcome,reason_code,trace_id,ip_address) VALUES ('user','FAILURE','BAD','sql-test','invalid-ip')",'22P02'),
}
for name,(statement,state) in INVALID_STATEMENTS.items():
    def test(self,statement=statement,state=state):self.reject(statement,(),state)
    setattr(IdentitySqlTests,'test_invalid_'+name,test)

INDEX_PLANS=[]
class Result(unittest.TextTestResult):
    def startTest(self,test):
        super().startTest(test);self.started=time.perf_counter()
    def addSuccess(self,test):
        super().addSuccess(test);RECORDS.append({'test':test.id().split('.')[-1],'status':'PASS','seconds':round(time.perf_counter()-self.started,4)})
    def addFailure(self,test,err):
        super().addFailure(test,err);RECORDS.append({'test':test.id().split('.')[-1],'status':'FAIL','detail':str(err[1])})
    def addError(self,test,err):
        super().addError(test,err);RECORDS.append({'test':test.id().split('.')[-1],'status':'ERROR','detail':str(err[1])})

RECORDS=[]
def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--reset-test-schema',action='store_true',required=True)
    parser.add_argument('--report',type=Path,required=True)
    args=parser.parse_args()
    c=connect();c.autocommit=True
    assert one(c,'SELECT current_database()')=='asterway_identity_test'
    assert one(c,'SELECT inet_server_addr()') is None, 'Only local Unix socket is allowed.'
    with c.cursor() as q:
        q.execute('DROP SCHEMA public CASCADE; CREATE SCHEMA public')
        q.execute(DDL)
    version=one(c,'SHOW server_version');c.close()
    result=unittest.TextTestRunner(verbosity=2,resultclass=Result).run(unittest.defaultTestLoader.loadTestsFromTestCase(IdentitySqlTests))
    report={'database':'asterway_identity_test','postgresql':version,'sql_sha256':hashlib.sha256(DDL.encode('utf-8')).hexdigest(),'tests':result.testsRun,'failures':len(result.failures),'errors':len(result.errors),'skipped':len(result.skipped),'records':RECORDS,'index_plans':INDEX_PLANS,'boundary':'SQL constraints, queries and transaction prototypes only; no Mapper, Service, JWT, CAPTCHA or HTTP implementation has been tested.'}
    args.report.parent.mkdir(parents=True,exist_ok=True);args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    raise SystemExit(0 if result.wasSuccessful() else 1)


if __name__=='__main__':main()
