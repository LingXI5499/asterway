# Asterway

个人成长管理系统。把目标拆成阶段和任务，在计划页面安排日期，通过执行计时、习惯打卡和历史统计记录真实进展。成长助手支持连续对话、规划与复盘，以及按知识库范围引用自己的资料。

当前发布版本：**V2.1**。维护者：**LingXI5499**。公开接口统一使用 `/asterway` 前缀。

## API 填在哪里

**聊天模型和向量服务的 API 都填写到 `agent/.env`。** 从 [agent/.env.example](agent/.env.example) 复制后填写，修改后重启 Python Agent。

| 需要配置的内容 | 填写位置 | 说明 |
| --- | --- | --- |
| 聊天 API Key | `agent/.env` → `LLM_API_KEY` | 模型供应商提供的密钥 |
| 聊天 API 地址 | `agent/.env` → `LLM_BASE_URL` | 兼容接口的基础地址，按供应商说明填写；不要填完整的 `/chat/completions` 地址 |
| 聊天模型 ID | `agent/.env` → `LLM_MODEL` | 填供应商实际提供的模型 ID，需支持工具调用和流式回复 |
| 向量 API Key | `agent/.env` → `EMBEDDING_API_KEY` | 用于知识文档和长期记忆的向量化，可与聊天服务使用不同供应商 |
| 向量 API 地址和模型 | `agent/.env` → `EMBEDDING_BASE_URL`、`EMBEDDING_MODEL` | 基础地址不要带末尾的 `/embeddings`；当前数据库使用 **1024 维**向量 |
| Agent 数据库连接 | `agent/.env` → `AGENT_DATABASE_URL` | 使用 `postgresql+asyncpg://`，连接与 Java 相同的数据库，使用独立 Agent 角色 |
| 服务间密钥 | Python 的 `AGENT_INTERNAL_KEY` 和 Java 的 `asterway.agent.internal-key` | 自己生成的一段 32～128 位安全字符，两边必须完全一致 |
| Java 数据库、Redis、JWT | `.asterway-private/local-runtime/config/application.yml` | 从 [config/application.example.yml](config/application.example.yml) 复制后填写 |

服务间密钥与 JWT 密钥分别生成。它们用于本项目内部校验，模型供应商的 API Key 只填在 Python 配置中。前端不需要填写模型密钥。

可以用 `py -3.12 -c "import secrets; print(secrets.token_urlsafe(48))"` 生成安全随机值。服务间密钥和 JWT 密钥分别运行一次，只把结果保存到私密配置。使用 DeepSeek 时将 `LLM_PROVIDER` 设为 `deepseek`，其他兼容服务可保留模板中的 `openai-compatible`。

模板中的地址 `https://api.example.com/v1` 和模型名 `your-chat-model`、`your-embedding-model` 都是占位内容，必须替换。密钥、密码在模板中保持空白。数据库密码若包含 `@`、`:`、`/` 等字符，写入 `AGENT_DATABASE_URL` 前需要做 URL 编码。

聊天与向量服务分别配置。没有向量密钥时，文档可能已经解析成功，但索引会失败；配置补齐后可在知识库页面重试处理。未选知识库的新会话不会检索文档。

## 环境要求

| 组件 | 要求 |
| --- | --- |
| Java | JDK 17 |
| Maven | 已使用 3.9.4 完成构建 |
| Python | 3.12，工程约束为 `>=3.12,<3.13` |
| Node.js | 满足 Vite 的 `^20.19.0` 或 `>=22.12.0`；本版本使用 24.15.0 构建 |
| PostgreSQL | 使用 17，安装 pgvector 扩展，数据库使用 UTF-8 |
| Redis | 用于登录会话与撤销校验，Java 启动前需可连接 |
| 模型接口 | 兼容聊天、工具调用、流式输出；知识检索另需 1024 维向量接口 |

Java、Python 和前端是三个进程。默认地址分别为 `http://127.0.0.1:8089`、`http://127.0.0.1:8001`、`http://127.0.0.1:5175`。前端的 `/asterway` 请求由 Vite 代理到 Java，Python 内部接口不直接提供给浏览器。

## 第一次配置

以下命令使用 PowerShell，均从仓库根目录开始。Linux 下将 `.venv/Scripts/python.exe` 换成 `.venv/bin/python`。

```powershell
git clone https://github.com/LingXI5499/asterway.git
cd asterway

New-Item -ItemType Directory -Force .asterway-private/local-runtime/config | Out-Null
if (!(Test-Path .asterway-private/local-runtime/config/application.yml)) {
    Copy-Item config/application.example.yml .asterway-private/local-runtime/config/application.yml
}
if (!(Test-Path agent/.env)) {
    Copy-Item agent/.env.example agent/.env
}

py -3.12 -m venv agent/.venv
agent/.venv/Scripts/python.exe -m pip install -e "./agent[dev]"

Push-Location frontend
npm ci
Pop-Location
```

接着编辑这两个私密文件：

1. `.asterway-private/local-runtime/config/application.yml`：填写 PostgreSQL、Redis、JWT 和服务间密钥。
2. `agent/.env`：填写聊天与向量 API、Agent 数据库连接和相同的服务间密钥。

`backend/app/src/main/resources/application.yml` 是代码内的空值配置基线，实际启动时由上面的私密 YAML 覆盖。请保留填好配置的文件在 Git 忽略目录中。

## 数据库准备

**应用不会自动建表、迁移或创建初始账号。先准备数据库，再启动服务。**

- PostgreSQL 需要建立业务数据库、Java 登录角色和独立 Agent 登录角色。两种角色使用各自密码，Java 和 Agent 连接同一个数据库。
- SQL 集中在 `backend/app/src/main/resources/sql/`。新库按 `identity → goal → stage → task → planning → execution → habit → notification → dashboard → statistics → agent` 顺序人工初始化；按各 SQL 和阶段说明配置角色、权限、菜单的启用状态。
- `agent.sql` 包含会话、知识文档、不可变版本、向量索引、工作流检查点和多知识库增量段。需要提前安装 pgvector，并由有权限的数据库维护者执行 SQL。
- Java 角色访问成长与账户业务表，并按现有提交契约访问方案相关对象；Agent 使用受限角色访问 `agent` schema。新知识库需要主体表的查询／新增／更新权限、会话关联表的查询／新增／更新／删除权限，以及知识库序列的使用权限。不要让应用使用数据库超级用户。
- 已有数据库先备份，再按缺少的增量段更新。**从 V2.0 升级 V2.1 时，重点执行 `ASTERWAY_KNOWLEDGE_BASES_BEGIN` 标记对应的独立事务段及上述新增对象授权，不需要重建原有资料。**迁移会将旧资料放入各用户的默认知识库，并保留旧会话范围。

初始超管由人工初始化流程创建，密码只写入私密文件，首次登录必须修改。仓库不提供默认密码。普通用户使用成长功能；管理员和超管负责账户、角色、菜单管理，不开放个人成长业务。

数据库与权限细节见 [账户数据库说明](docs/identity/数据库与授权设计.md)、[Agent SQL](backend/app/src/main/resources/sql/agent.sql) 和 [多知识库改造与验证](docs/agent/多知识库改造_20261109.md)。`scripts/` 中带 `local` 的辅助脚本有本地开发库、WSL 和阶段范围限制，使用前先阅读脚本，不能当作通用的一键部署程序。

## 启动项目

先启动 PostgreSQL 与 Redis。Java、Agent、前端分别使用一个终端，下面三个终端都从仓库根目录开始。

### 终端一：Java 后端

```powershell
mvn --no-transfer-progress -f backend/pom.xml -DskipTests package

$configPath = (Resolve-Path .asterway-private/local-runtime/config/application.yml).Path
$configUri = ([System.Uri]$configPath).AbsoluteUri
java -jar backend/app/target/app-0.1.0-SNAPSHOT.jar "--spring.config.additional-location=$configUri"
```

更换运行包前先停止旧 Java 进程，再重新打包启动。配置填错或数据库／Redis 不可达时，先检查 Java 启动日志。

### 终端二：Python Agent

```powershell
cd agent
.venv/Scripts/python.exe run.py
```

必须在 `agent` 目录运行，以便读取 `.env` 和知识文件目录。Windows 使用 `run.py`，其中已配置异步数据库需要的事件循环。修改 `.env` 后需要重启这个进程。

### 终端三：前端

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

浏览器打开 **http://127.0.0.1:5175**，登录后使用项目。前端端口固定，端口占用时先停止本项目的旧进程。修改端口时，还需同步 Java 的允许来源、Vite 代理和 Agent 的 Java 地址。

### 确认启动成功

- Java 日志显示 `Started AsterwayApplication`，数据库和 Redis 检查通过；公开接口说明位于 `http://127.0.0.1:8089/swagger-ui/index.html`。
- `http://127.0.0.1:8001/health` 返回 `success: true`，使用持久化功能时检查 `storageReady`。
- 前端可以正常登录；知识库页面能加载列表和上传格式说明。只启动前端时，页面会出现代理失败；新前端搭配旧 Java 包时，会提示不支持新增资源，需要更新后端并完成对应 SQL。

## V2.1 功能

- 统一计划页面：周／月选日期，下方处理当天任务，任务管理、详情、编辑与改期使用弹窗，保留执行计时与安排历史。
- 聊天输入：Enter 发送、Ctrl+Enter 换行，兼容 Shift+Enter 和中文输入法。
- 多知识库：创建、重命名、简介、单篇／批量移动，文档版本、索引和历史引用保持可用，非空库不能直接删除。
- 批量导入：每批最多 20 个文件，同时上传 2 个；支持 TXT、Markdown、文字 PDF、DOCX，单文件最高 25 MB，逐项展示上传、解析、索引和失败重试。
- 会话检索范围：每个会话可选择多个知识库，新会话默认不引用；每轮固定范围，向量检索和关键词回退都按库隔离。
- 保留账户 RBAC、目标与阶段、任务池、执行计时、习惯打卡、通知、看板、历史统计，以及规划、复盘与调整闭环。

## 检查与发布范围

```powershell
# 前端逻辑检查和构建
Push-Location frontend
npm run test:agent
npm run test:planning
npm run test:knowledge
npm run build
Pop-Location

# Python 检查
Push-Location agent
.venv/Scripts/python.exe -m ruff check app tests/test_knowledge_bases.py
.venv/Scripts/python.exe -m pytest -q
Pop-Location
```

知识库数据库专项检查要求显式提供 `ASTERWAY_KB_TEST_CONFIG`，只允许专用测试库和随机隔离 schema；未配置时会跳过，不能据此声称数据库验证通过。配置及隔离校验说明见 [多知识库改造与验证](docs/agent/多知识库改造_20261109.md)。

发布标签表示对应源码快照，不代表已经部署到其他服务器。运行配置、真实知识文件、数据库备份、依赖、缓存、构建输出和本地 IDE 文件不包含在发布中。发布变更见 [V2.1 说明](docs/releases/V2.1.md)，更早阶段记录保留在 `docs/`。
