# 上传和替换文档使用同一套能力说明与校验，页面无需另写一份格式白名单。
import re
from io import BytesIO
from pathlib import Path

from app.core.config import get_settings
from app.core.errors import AgentError

FORMATS = [
    {"extension": ".txt", "name": "TXT", "description": "UTF-8文本，包含UTF-8 BOM"},
    {"extension": ".md", "name": "Markdown", "description": "正文与标题"},
    {"extension": ".pdf", "name": "PDF", "description": "含文字层的PDF，保留页码"},
    {"extension": ".docx", "name": "DOCX", "description": "正文与表格文字"},
]
ALLOWED_TYPES = {"application/octet-stream", "text/plain", "text/markdown", "text/x-markdown",
                 "application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"}


def capabilities() -> dict:
    return {"formats": FORMATS, "accept": ",".join(f["extension"] for f in FORMATS),
            "maxFileBytes": min(get_settings().agent_knowledge_max_bytes, 25 * 1024 * 1024),
            "maxBatchFiles": 20, "uploadConcurrency": 2, "ocrSupported": False,
            "scanNotice": "纯扫描PDF无法提取文字，请先完成OCR再导入。"}


def validate_file(name: str, mime: str, data: bytes) -> str:
    mime = (mime or "application/octet-stream").split(";")[0].strip().lower()
    if Path(name).suffix.lower() not in {f["extension"] for f in FORMATS} or mime not in ALLOWED_TYPES:
        raise AgentError(422, "KNOWLEDGE_TYPE_UNSUPPORTED", "支持TXT、Markdown（.md）、PDF和DOCX文件")
    if not data:
        raise AgentError(422, "KNOWLEDGE_EMPTY", "文件为空，请选择有内容的文档")
    if len(data) > capabilities()["maxFileBytes"]:
        raise AgentError(413, "KNOWLEDGE_TOO_LARGE", "文件超过允许的大小上限，请拆分后导入")
    return mime


def extract_sections(*, file_name: str, mime_type: str, data: bytes):
    suffix = Path(file_name).suffix.lower()
    if suffix == ".pdf":
        try:
            from pypdf import PdfReader
            reader = PdfReader(BytesIO(data))
            if reader.is_encrypted:
                raise AgentError(422, "KNOWLEDGE_PDF_ENCRYPTED", "PDF已加密，请解除密码保护后导入")
            sections = [(n, None, page.extract_text() or "") for n, page in enumerate(reader.pages, 1)]
            if not any(text.strip() for _, _, text in sections):
                raise AgentError(422, "KNOWLEDGE_PDF_NO_TEXT", "PDF没有可提取文字，扫描件请先完成OCR再导入")
            return sections
        except AgentError:
            raise
        except ImportError:
            raise AgentError(503, "KNOWLEDGE_PARSER_MISSING", "PDF解析组件暂不可用") from None
        except Exception as error:
            raise AgentError(422, "KNOWLEDGE_PDF_INVALID", "PDF内容损坏或无法解析，请检查文件") from error
    if suffix == ".docx":
        # 加密的Office文档使用OLE容器，无法当作普通DOCX压缩包直接读取。
        if data.startswith(bytes.fromhex("D0CF11E0A1B11AE1")):
            raise AgentError(422, "KNOWLEDGE_DOCX_ENCRYPTED", "DOCX已加密或不是标准DOCX，请另存为未加密的DOCX后导入")
        try:
            from docx import Document
            from docx.table import Table
            document = Document(BytesIO(data))
            parts = []
            # 按原文顺序读取段落和表格，避免只取段落而遗漏表格资料。
            for block in document.iter_inner_content():
                if isinstance(block, Table):
                    parts.extend("\t".join(cell.text for cell in row.cells) for row in block.rows)
                else:
                    parts.append(block.text)
            return [(None, None, "\n".join(parts))]
        except ImportError:
            raise AgentError(503, "KNOWLEDGE_PARSER_MISSING", "DOCX解析组件暂不可用") from None
        except Exception as error:
            raise AgentError(422, "KNOWLEDGE_DOCX_INVALID", "DOCX内容损坏或无法解析，请检查文件") from error
    if suffix not in {".txt", ".md"}:
        raise AgentError(422, "KNOWLEDGE_TYPE_UNSUPPORTED", "不支持的文件格式")
    try:
        text = data.decode("utf-8-sig")
    except UnicodeError:
        raise AgentError(422, "KNOWLEDGE_ENCODING_INVALID", "文本编码无法识别，请另存为UTF-8后导入") from None
    if suffix == ".txt":
        return [(None, None, text)]
    sections, lines, heading = [], [], None
    for line in text.splitlines():
        match = re.match(r"^#{1,6}\s+(.+)$", line)
        if match:
            if lines:
                sections.append((None, heading, "\n".join(lines)))
            heading, lines = match.group(1)[:300], [line]
        else:
            lines.append(line)
    if lines:
        sections.append((None, heading, "\n".join(lines)))
    return sections
