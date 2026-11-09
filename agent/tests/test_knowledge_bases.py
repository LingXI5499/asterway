# 文件解析采用真实解析器；数据库用显式指定的专用校验库和随机schema。
import asyncio
import json
import os
import re
from contextlib import asynccontextmanager
from datetime import UTC, datetime
from io import BytesIO
from pathlib import Path
from unittest.mock import AsyncMock
from uuid import uuid4

import pytest
import pytest_asyncio
from sqlalchemy import select
from sqlalchemy.engine import make_url
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core.errors import AgentError
from app.db.models import Base, ConversationTurn, KnowledgeChunk, KnowledgeDocument
from app.db.workspace_models import KnowledgeRevision
from app.services import conversation_service as conversations
from app.services import knowledge_base_service as bases
from app.services import knowledge_formats as formats
from app.services import knowledge_revision_service as revisions
from app.services import knowledge_service as documents
from app.services import retrieval_service as retrieval
from app.services import turn_service


def test_capabilities_and_utf8_bom():
    caps = formats.capabilities()
    assert caps["maxFileBytes"] <= 25 * 1024 * 1024
    assert caps["maxBatchFiles"] == 20 and caps["uploadConcurrency"] == 2
    assert caps["accept"] == ".txt,.md,.pdf,.docx"
    assert formats.extract_sections(file_name="x.TXT", mime_type="text/plain", data=b"\xef\xbb\xbfhello")[0][2] == "hello"
    with pytest.raises(AgentError) as error:
        formats.extract_sections(file_name="x.txt", mime_type="text/plain", data=b"\xff\xfe")
    assert error.value.code == "KNOWLEDGE_ENCODING_INVALID"


@pytest.mark.parametrize("name,data,code", [("empty.txt", b"", "KNOWLEDGE_EMPTY"), ("old.doc", b"word", "KNOWLEDGE_TYPE_UNSUPPORTED"), ("a.txt", b"x" * (25 * 1024 * 1024 + 1), "KNOWLEDGE_TOO_LARGE")])
def test_upload_rejects_empty_unsupported_and_oversize(name, data, code):
    with pytest.raises(AgentError) as error:
        formats.validate_file(name, "application/octet-stream", data)
    assert error.value.code == code


def test_markdown_headings_and_docx_table_order():
    sections = formats.extract_sections(file_name="x.md", mime_type="text/markdown", data="# 英语\nEnglish\n## Java\nJVM".encode())
    assert [r[1] for r in sections] == ["英语", "Java"]
    from docx import Document
    doc = Document()
    doc.add_paragraph("表格前")
    table = doc.add_table(rows=1, cols=2)
    table.cell(0, 0).text = "单词"
    table.cell(0, 1).text = "meaning"
    doc.add_paragraph("表格后")
    file = BytesIO(); doc.save(file)
    text = formats.extract_sections(file_name="words.docx", mime_type="application/octet-stream", data=file.getvalue())[0][2]
    assert text == "表格前\n单词\tmeaning\n表格后"


def test_pdf_scanned_encrypted_and_corrupt_errors():
    from pypdf import PdfWriter
    writer = PdfWriter(); writer.add_blank_page(width=200, height=200)
    file = BytesIO(); writer.write(file)
    with pytest.raises(AgentError) as error:
        formats.extract_sections(file_name="scan.pdf", mime_type="application/pdf", data=file.getvalue())
    assert error.value.code == "KNOWLEDGE_PDF_NO_TEXT" and "OCR" in error.value.message
    writer.encrypt("password"); file = BytesIO(); writer.write(file)
    with pytest.raises(AgentError) as error:
        formats.extract_sections(file_name="protected.pdf", mime_type="application/pdf", data=file.getvalue())
    assert error.value.code == "KNOWLEDGE_PDF_ENCRYPTED"
    with pytest.raises(AgentError) as error:
        formats.extract_sections(file_name="broken.pdf", mime_type="application/pdf", data=b"broken")
    assert error.value.code == "KNOWLEDGE_PDF_INVALID"


def test_encrypted_docx_has_specific_message():
    with pytest.raises(AgentError) as error:
        formats.extract_sections(file_name="protected.docx", mime_type="application/octet-stream",
            data=bytes.fromhex("D0CF11E0A1B11AE1") + b"encrypted document")
    assert error.value.code == "KNOWLEDGE_DOCX_ENCRYPTED"
    assert "未加密" in error.value.message


_initialized = False


@pytest_asyncio.fixture
async def kbdb(tmp_path, monkeypatch):
    config_path = os.environ.get("ASTERWAY_KB_TEST_CONFIG")
    if not config_path:
        pytest.skip("Set ASTERWAY_KB_TEST_CONFIG to run isolated PostgreSQL checks")
    config = json.loads(Path(config_path).read_text(encoding="utf-8"))
    assert make_url(config["url"]).database == "asterway_identity_test"
    assert re.fullmatch(r"kb_api_[a-f0-9]{12}", config["schema"])
    engine = create_async_engine(config["url"], execution_options={"schema_translate_map": {"agent": config["schema"]}})
    # 注册项目已有的全部模型，避免创建一套不等同于项目的测试表。
    from app.main import app
    assert app
    global _initialized
    if not _initialized:
        source = (Path(__file__).resolve().parents[2] / "backend/app/src/main/resources/sql/agent.sql").read_text(encoding="utf-8-sig")
        migration = source.split("-- ASTERWAY_KNOWLEDGE_BASES_BEGIN", 1)[1].split("-- ASTERWAY_KNOWLEDGE_BASES_END", 1)[0]
        migration = migration.replace("agent.", config["schema"] + ".").replace("table_schema='agent'", "table_schema='" + config["schema"] + "'")
        async with engine.begin() as conn:
            await conn.run_sync(Base.metadata.create_all)
            raw = await conn.get_raw_connection()
            await raw.driver_connection.execute(migration)
        _initialized = True
    factory = async_sessionmaker(engine, expire_on_commit=False)

    class TestDatabase:
        @asynccontextmanager
        async def session(self):
            async with factory() as session:
                try:
                    yield session
                    await session.commit()
                except BaseException:
                    await session.rollback()
                    raise

    monkeypatch.setattr(documents, "_storage_root", lambda: tmp_path)
    monkeypatch.setattr(revisions, "_storage_root", lambda: tmp_path)
    user = int(uuid4().hex[:7], 16) + 10_000
    try:
        yield TestDatabase(), user, user + 1
    finally:
        await engine.dispose()


async def upload(db, user, base, text="共同术语：英语资料", filename="a.txt"):
    return await documents.upload(db, user, file_name=filename, mime_type="text/plain", data=text.encode(), title=None, knowledge_base_id=int(base["id"]))


@pytest.mark.asyncio
async def test_library_ownership_names_and_nonempty_delete(kbdb):
    db, user, foreign = kbdb
    first = await bases.save_base(db, user, "Java", "学习资料")
    with pytest.raises(AgentError) as error:
        await bases.save_base(db, user, " java ", "")
    assert error.value.code == "KNOWLEDGE_BASE_DUPLICATE"
    with pytest.raises(AgentError) as error:
        await bases.save_base(db, foreign, "偷改", "", base_id=int(first["id"]), version=first["version"])
    assert error.value.status_code == 404
    await upload(db, user, first)
    with pytest.raises(AgentError) as error:
        await bases.delete_base(db, user, int(first["id"]), first["version"])
    assert error.value.code == "KNOWLEDGE_BASE_NOT_EMPTY"


@pytest.mark.asyncio
async def test_same_base_concurrent_dedup_and_cross_base_upload(kbdb):
    db, user, _ = kbdb
    first = await bases.save_base(db, user, "英语", "")
    second = await bases.save_base(db, user, "Java", "")
    results = await asyncio.gather(upload(db, user, first), upload(db, user, first))
    assert sum(created for _, created in results) == 1
    assert results[0][0].id == results[1][0].id
    other, created = await upload(db, user, second)
    assert created and other.id != results[0][0].id
    one, total = await documents.list_documents(db, user, status=None, keyword=None, page=1, size=20, knowledge_base_id=int(first["id"]))
    assert total == 1 and one[0].knowledge_base_name == "英语" and one[0].index_status == "PENDING"
    assert len(await bases.list_bases(db, user)) == 2


@pytest.mark.asyncio
async def test_document_and_processing_pages_keep_totals_inside_one_library(kbdb):
    db, user, _ = kbdb
    first = await bases.save_base(db, user, "分页资料", "")
    other = await bases.save_base(db, user, "其他资料", "")
    expected = set()
    for number in range(23):
        row, _ = await upload(db, user, first, f"分页文档{number}", f"page-{number}.txt")
        expected.add(row.id)
    await upload(db, user, other, "另一库的文档")
    documents_seen, tasks_seen = set(), set()
    for page in range(1, 4):
        rows, total = await documents.list_documents(db, user, status=None, keyword=None,
            page=page, size=10, knowledge_base_id=int(first["id"]))
        tasks, task_total = await documents.page_tasks(db, user, status=None,
            page=page, size=10, knowledge_base_id=int(first["id"]))
        assert total == task_total == 23
        documents_seen.update(row.id for row in rows)
        tasks_seen.update(task.ref_id for task in tasks)
        assert all(documents.task_vo(task)["knowledgeBaseName"] == "分页资料" for task in tasks)
        assert all(documents.task_vo(task)["documentTitle"] for task in tasks)
    assert documents_seen == tasks_seen == expected


@pytest.mark.asyncio
async def test_move_preserves_identity_and_rejects_duplicate_atomically(kbdb):
    db, user, foreign = kbdb
    a = await bases.save_base(db, user, "英语", "")
    b = await bases.save_base(db, user, "Python", "")
    doc, _ = await upload(db, user, a)
    original = (doc.id, doc.current_revision_id, doc.checksum, doc.storage_key)
    await bases.move_documents(db, user, int(b["id"]), [{"id": doc.id, "version": doc.version}])
    moved = await documents.get_document(db, user, doc.id)
    assert (moved.id, moved.current_revision_id, moved.checksum, moved.storage_key) == original
    assert moved.knowledge_base_id == int(b["id"])
    duplicate, _ = await upload(db, user, a)
    extra, _ = await upload(db, user, a, "另一个文件")
    with pytest.raises(AgentError) as error:
        await bases.move_documents(db, user, int(b["id"]), [{"id": duplicate.id, "version": duplicate.version}, {"id": extra.id, "version": extra.version}])
    assert error.value.code == "KNOWLEDGE_DUPLICATE"
    assert (await documents.get_document(db, user, extra.id)).knowledge_base_id == int(a["id"])
    with pytest.raises(AgentError) as error:
        await bases.move_documents(db, foreign, int(b["id"]), [{"id": extra.id, "version": extra.version}])
    assert error.value.status_code == 404


@pytest.mark.asyncio
async def test_revision_keeps_original_document_and_library(kbdb):
    db, user, _ = kbdb
    base = await bases.save_base(db, user, "学习", "")
    doc, _ = await upload(db, user, base)
    replaced = await revisions.replace(db, user, doc.id, version=doc.version, file_name="v2.txt", mime_type="text/plain", data=b"version two")
    assert replaced.id == doc.id and replaced.knowledge_base_id == doc.knowledge_base_id
    assert replaced.current_revision_id != doc.current_revision_id
    async with db.session() as session:
        old = await session.get(KnowledgeRevision, doc.current_revision_id)
        assert old is not None and old.checksum == doc.checksum


async def searchable(db, user, base, text):
    doc, _ = await upload(db, user, base, text)
    async with db.session() as session:
        stored = await session.get(KnowledgeDocument, doc.id)
        revision = await session.get(KnowledgeRevision, doc.current_revision_id)
        stored.status = revision.status = "READY"
        revision.index_status = "READY"
        chunk = KnowledgeChunk(user_id=user, document_id=doc.id, revision_id=revision.id, chunk_no=0, content=text, token_count=10)
        session.add(chunk); await session.flush()
        return doc, chunk.id


@pytest.mark.asyncio
async def test_keyword_fallback_never_crosses_selected_libraries(kbdb, monkeypatch):
    db, user, _ = kbdb
    a = await bases.save_base(db, user, "英语", "")
    b = await bases.save_base(db, user, "Java", "")
    doc_a, _ = await searchable(db, user, a, "共同术语 英语")
    doc_b, _ = await searchable(db, user, b, "共同术语 Java")
    embedding = AsyncMock(side_effect=AgentError(503, "EMBEDDING_FAILED", "测试回退"))
    monkeypatch.setattr(documents, "embed_texts", embedding)
    assert await retrieval.retrieve(db, user, "共同术语", []) == []
    embedding.assert_not_called()
    selected = await retrieval.retrieve(db, user, "共同术语", [int(a["id"])])
    assert [c.document_id for c in selected] == [doc_a.id]
    multiple = await retrieval.retrieve(db, user, "共同术语", [int(a["id"]), int(b["id"])])
    assert {c.document_id for c in multiple} == {doc_a.id, doc_b.id}
    metadata = {}
    await retrieval.context_block(db, user, "共同术语", knowledge_base_ids=[int(a["id"])], metadata=metadata)
    assert metadata["citations"][0]["knowledgeBaseName"] == "英语"


@pytest.mark.asyncio
async def test_conversation_selection_and_running_turn_scope_are_independent(kbdb):
    db, user, foreign = kbdb
    a = await bases.save_base(db, user, "英语", "")
    b = await bases.save_base(db, user, "Python", "")
    new = await conversations.create_conversation(db, user, "新会话")
    assert new.knowledge_base_ids == []
    selected = await conversations.update_knowledge_bases(db, user, int(new.id), [int(a["id"])], new.version)
    request = uuid4()
    started = await turn_service.start_turn(db, user, int(new.id), "共同术语", request, [int(a["id"])])
    detail = await conversations.get_conversation(db, user, int(new.id))
    await conversations.update_knowledge_bases(db, user, int(new.id), [int(b["id"])], detail.version)
    assert started.knowledge_base_ids == [int(a["id"])]
    async with db.session() as session:
        turn = await session.get(ConversationTurn, started.turn_id)
        assert turn.knowledge_base_ids == [a["id"]]
    assert (await conversations.get_conversation(db, user, int(new.id))).knowledge_base_ids == [b["id"]]
    with pytest.raises(AgentError) as error:
        await turn_service.start_turn(db, user, int(new.id), "共同术语", request, [int(b["id"])])
    assert error.value.code == "REQUEST_ID_REUSED"
    with pytest.raises(AgentError) as error:
        await conversations.create_conversation(db, foreign, "非法范围", [int(a["id"])])
    assert error.value.code == "KNOWLEDGE_SCOPE_INVALID"
    assert selected.knowledge_base_ids == [a["id"]]


@pytest.mark.asyncio
async def test_empty_library_delete_clears_conversation_binding(kbdb):
    db, user, _ = kbdb
    base = await bases.save_base(db, user, "空库", "")
    conversation = await conversations.create_conversation(db, user, "测试", [int(base["id"])])
    await bases.delete_base(db, user, int(base["id"]), base["version"])
    assert (await conversations.get_conversation(db, user, int(conversation.id))).knowledge_base_ids == []


@pytest.mark.asyncio
async def test_real_parse_index_failure_retry_and_vector_scope(kbdb, monkeypatch):
    from app.core.config import get_settings
    from app.db.models import BackgroundTask
    db, user, _ = kbdb
    base = await bases.save_base(db, user, "解析测试", "")
    other = await bases.save_base(db, user, "另一个库", "")
    doc, _ = await upload(db, user, base, "真实解析与索引资料：English grammar and Java class")

    async def claim(kind):
        async with db.session() as session:
            task = await session.scalar(select(BackgroundTask).where(BackgroundTask.user_id == user,
                BackgroundTask.ref_id == doc.id, BackgroundTask.kind == kind, BackgroundTask.status == "PENDING"))
            assert task is not None
            task.status = "RUNNING"; task.attempts += 1; task.version += 1
            task.started_at = task.updated_at = datetime.now(UTC)
            await session.flush(); await session.refresh(task)
            return task

    parse_task = await claim("DOCUMENT_PARSE")
    await documents.process_parse(db, parse_task)
    parsed = await documents.get_document(db, user, doc.id)
    assert parsed.status == "READY" and parsed.index_status == "PENDING" and parsed.chunk_count > 0
    embedding_task = await claim("DOCUMENT_EMBEDDING")
    monkeypatch.setattr(documents, "embed_texts", AsyncMock(side_effect=AgentError(503, "EMBEDDING_FAILED", "模拟索引失败")))
    with pytest.raises(AgentError):
        await documents.process_embedding(db, embedding_task)
    # 失败补偿由现有后台工作器统一执行，按真实的处理顺序推进索引状态。
    await documents.revision_index_failed(db, embedding_task, "EMBEDDING_FAILED", "模拟索引失败")
    async with db.session() as session:
        task = await session.get(BackgroundTask, embedding_task.id)
        task.status = "FAILED"; task.finished_at = datetime.now(UTC)
    failed = await documents.get_document(db, user, doc.id)
    assert failed.index_status == "FAILED"
    await revisions.retry(db, user, doc.id, failed.version)
    unit = [1.0] + [0.0] * 1023
    monkeypatch.setattr(documents, "embed_texts", AsyncMock(side_effect=lambda texts: [unit for _ in texts]))
    retry_task = await claim("DOCUMENT_EMBEDDING")
    await documents.process_embedding(db, retry_task)
    ready = await documents.get_document(db, user, doc.id)
    assert ready.index_status == "READY"
    found = await retrieval.retrieve(db, user, "grammar", [int(base["id"])])
    assert found and all(c.document_id == doc.id for c in found)
    assert await retrieval.retrieve(db, user, "grammar", [int(other["id"])]) == []
    assert get_settings().embedding_model


@pytest.mark.asyncio
async def test_http_library_upload_scope_and_cross_user_guards(kbdb):
    from fastapi import FastAPI
    from fastapi.responses import JSONResponse
    from httpx import ASGITransport, AsyncClient
    from pydantic import SecretStr

    from app.api.routes import knowledge
    from app.core.context import AgentContext, get_agent_context
    from app.db.session import get_database
    db, user, foreign = kbdb
    app = FastAPI()
    app.include_router(knowledge.router)
    actor = {"id": user}
    permissions = frozenset({"agent:knowledge:read", "agent:knowledge:manage"})
    app.dependency_overrides[get_database] = lambda: db
    app.dependency_overrides[get_agent_context] = lambda: AgentContext(str(actor["id"]), frozenset({"USER"}), permissions, SecretStr("test"), "test", 9999999999)

    @app.exception_handler(AgentError)
    async def error_handler(request, error):
        return JSONResponse(status_code=error.status_code, content={"code": error.code, "message": error.message})

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as client:
        caps = await client.get("/internal/agent/knowledge/capabilities")
        assert caps.status_code == 200 and caps.json()["maxBatchFiles"] == 20
        created = await client.post("/internal/agent/knowledge/bases", json={"name": "HTTP英语", "description": "测试"})
        assert created.status_code == 201
        base = created.json()
        response = await client.post("/internal/agent/knowledge", params={"fileName": "one.txt", "knowledgeBaseId": base["id"]},
            headers={"X-File-Type": "text/plain"}, content=b"hello")
        assert response.status_code == 201 and response.json()["created"]
        assert response.json()["document"]["knowledgeBaseId"] == base["id"]
        actor["id"] = foreign
        assert (await client.get("/internal/agent/knowledge", params={"knowledgeBaseId": base["id"]})).status_code == 404
        assert (await client.post("/internal/agent/knowledge", params={"fileName": "bad.txt", "knowledgeBaseId": base["id"]}, content=b"bad")).status_code == 404
