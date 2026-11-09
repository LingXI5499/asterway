# 只在专用校验库的新建随机schema验证迁移，整次校验回滚，不触碰开发资料。
from pathlib import Path
import json
import re
from uuid import uuid4

import psycopg2
from psycopg2 import sql


def main():
    schema = "kb_review_" + uuid4().hex[:12]
    source = (Path(__file__).resolve().parents[1] / "backend/app/src/main/resources/sql/agent.sql").read_text(encoding="utf-8-sig")
    migration = source.split("-- ASTERWAY_KNOWLEDGE_BASES_BEGIN", 1)[1].split("-- ASTERWAY_KNOWLEDGE_BASES_END", 1)[0]
    migration = migration.replace("agent.", schema + ".").replace("table_schema='agent'", "table_schema='" + schema + "'")
    db = psycopg2.connect(dbname="asterway_identity_test", user="postgres", host="/var/run/postgresql")
    checks = []
    try:
        with db.cursor() as cursor:
            cursor.execute("SELECT current_database()")
            assert cursor.fetchone()[0] == "asterway_identity_test"
            cursor.execute(sql.SQL("CREATE SCHEMA {}").format(sql.Identifier(schema)))
            cursor.execute(sql.SQL("SET LOCAL search_path TO {},public").format(sql.Identifier(schema)))
            cursor.execute("""CREATE TABLE knowledge_document(id bigint PRIMARY KEY,user_id bigint NOT NULL,checksum text,status text,created_at timestamptz DEFAULT now(),deleted_at timestamptz);
                CREATE UNIQUE INDEX uk_agent_knowledge_document_user_checksum ON knowledge_document(user_id,checksum) WHERE deleted_at IS NULL;
                CREATE TABLE conversation(id bigint PRIMARY KEY,user_id bigint NOT NULL,deleted_at timestamptz);
                CREATE TABLE conversation_turn(id bigint PRIMARY KEY,user_id bigint NOT NULL);
                INSERT INTO knowledge_document(id,user_id,checksum,status) VALUES(11,1,'original','READY');
                INSERT INTO conversation VALUES(21,1,NULL); INSERT INTO conversation_turn VALUES(31,1);""")
            cursor.execute(migration)
            cursor.execute("SELECT id,knowledge_base_id,checksum FROM knowledge_document")
            row = cursor.fetchone()
            assert row[0] == 11 and row[2] == "original"
            base = row[1]
            checks.append("existing-document-preserved")
            cursor.execute("SELECT count(*) FROM conversation_knowledge_base")
            assert cursor.fetchone()[0] == 1
            cursor.execute("SELECT knowledge_base_ids FROM conversation_turn WHERE id=31")
            assert cursor.fetchone()[0] == [str(base)]
            checks.append("legacy-conversation-and-turn-scope")
            cursor.execute("DELETE FROM conversation_knowledge_base")
            cursor.execute("INSERT INTO conversation VALUES(22,1,NULL); INSERT INTO conversation_turn VALUES(32,1)")
            cursor.execute(migration)
            cursor.execute("SELECT count(*) FROM conversation_knowledge_base")
            assert cursor.fetchone()[0] == 0
            cursor.execute("SELECT knowledge_base_ids FROM conversation_turn WHERE id=32")
            assert cursor.fetchone()[0] == []
            checks.append("rerun-preserves-explicit-empty-and-new-conversations")
            cursor.execute("INSERT INTO knowledge_base(user_id,name) VALUES(1,'英语') RETURNING id")
            other = cursor.fetchone()[0]
            cursor.execute("INSERT INTO knowledge_document(id,user_id,knowledge_base_id,checksum,status) VALUES(12,1,%s,'original','READY')", (other,))
            checks.append("same-content-in-different-bases")
            for name, statement, parameters in [
                ("same-base-duplicate-rejected", "INSERT INTO knowledge_document(id,user_id,knowledge_base_id,checksum) VALUES(13,1,%s,'original')", (base,)),
                ("cross-user-document-rejected", "INSERT INTO knowledge_document(id,user_id,knowledge_base_id,checksum) VALUES(14,2,%s,'foreign')", (base,)),
                ("cross-user-conversation-rejected", "INSERT INTO conversation_knowledge_base VALUES(2,21,%s)", (base,)),
            ]:
                cursor.execute("SAVEPOINT rejected_case")
                try:
                    cursor.execute(statement, parameters)
                except psycopg2.IntegrityError:
                    cursor.execute("ROLLBACK TO SAVEPOINT rejected_case")
                    checks.append(name)
                else:
                    raise AssertionError(name)
            print(json.dumps({"passed": len(checks), "checks": checks, "rolledBack": True}))
    finally:
        db.rollback()
        db.close()


if __name__ == "__main__":
    main()
