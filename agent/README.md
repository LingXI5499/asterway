# Asterway Agent

V2.1 已支持多知识库、批量导入和会话选库。聊天 API Key、向量 API Key、地址与模型 ID 均填写在 `agent/.env`；配置表、数据库准备及完整启动顺序见 [根目录 README](../README.md)，可复制模板为 [.env.example](.env.example)。

> 2026-11-06 收尾：P1.1—P1.6 已接齐并完成真实联调（LangGraph 持久化确认、pgvector 1024 维向量、文档不可变版本与引用回看、长期记忆、知识库、规划工作流、复盘与运行记录、五个前端页面）。本轮修复复盘截断/失败重试/限流与任务超时回收、文档任务领取代次、方案确认后执行失败的恢复重试；结果见 [Phase 2 收尾与验收](../docs/agent/Phase2收尾与验收_20261106.md)。
>
> 2026-11-07 执行反馈闭环：新增查询预算与结果复用（`tool_budget`、`get_goal_roadmap`）、对话内 `prepare_growth_plan`/`review_growth_progress` 工具、复盘事实 `trackedPlan` 与 `POST /agent/reviews/{id}/proposals` 调整草案、方案真实进度 `progress`、失败记忆任务 `POST /agent/runs/tasks/{id}/retry`；规划输出预算 8192、单轮预算 240s，并透传 Java 业务原因。结果见 [Agent 执行反馈闭环](../docs/agent/Agent执行反馈闭环_20261107.md)。

Python 3.12 的独立对话服务。已完成 P1.3 多轮会话持久化、上下文与摘要、SSE 流式回复、停止与断连恢复，以及 29 个只读业务工具；P1.4 长期记忆与知识库（上传、解析、分块、pgvector 1024 维向量、版本、引用原文、后台任务）；P1.5 规划工作流（方案生成/修订、显式确认、LangGraph 数据库检查点、Java 原子幂等提交、过期/取消）；P1.6 成长复盘、调用用量与运行记录。由 Java `backend/agent` 负责公开入口认证和流式转发。每阶段用户验收后再进入下一阶段。

## 本地运行

在本目录创建虚拟环境并安装本项目：

```powershell
python -m venv .venv
.venv/Scripts/python.exe -m pip install -e .
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

仅在被 Git 忽略的 `.env` 填写 `LLM_API_KEY`、`EMBEDDING_API_KEY`、`AGENT_DATABASE_URL` 与 `AGENT_INTERNAL_KEY`。`JAVA_API_BASE_URL` 使用本地 Java 地址；`AGENT_INTERNAL_KEY` 与 Java 私密 YAML 的 `asterway.agent.internal-key` 一致，填写 32～128 位安全字符。示例中的真实密钥必须保持为空。

```powershell
# Windows 必须使用 run.py：psycopg 异步连接要求 SelectorEventLoop，默认 Proactor 会连接失败。
.venv/Scripts/python.exe run.py
# 等价手工命令（指定自定义 loop 实现）：
.venv/Scripts/python.exe -m uvicorn app.main:app --host 127.0.0.1 --port 8001 `
  --loop app.core.event_loop:selector_event_loop
```

修改 `.env` 后重启 Python；默认热重载不监视该文件。先确保 Java 与 Python 都启动，再访问 Vue 的成长助手页面。

内部端点：`/internal/agent/chat(/stream)`、`/internal/agent/conversations/**`、`/internal/agent/memories/**`、`/internal/agent/knowledge/**`、`/internal/agent/planning/proposals/**`、`/internal/agent/reviews/**`、`/internal/agent/runs/calls`、`/internal/agent/runs/tasks`，必须同时提供 Java 用户 Bearer 和 `X-Asterway-Agent-Key`。浏览器统一调用 `/asterway/agent/**`，内部密钥不能发送给浏览器。复盘生成失败后可通过公开 `POST /asterway/agent/reviews/{id}/retry` 原地重试（内部同名路由），只有 `FAILED` 可重试；失败记忆任务可通过公开 `POST /asterway/agent/runs/tasks/{id}/retry` 重新排队，来源不可用或连续失败会给出可读提示。

会话、消息、摘要、工具调用、记忆、资料、方案、复盘的调用记录保存在 PostgreSQL 的 `agent` schema。普通查询工具只读；规划提交由用户确认后通过 Java 原子批量接口执行，Python 不直接写成长业务表。记忆、知识库、规划、复盘与五个工作区页面均已接入 Java 公共代理和前端。对话轮次内可先后调用只读查询、`prepare_growth_plan`（生成/修订草案）与 `review_growth_progress`（真实统计复盘）；右侧会持续展示方案与真实进度，复盘完成后可生成调整草案，确认后由 Java 更新原计划的安排，不复制任务。

> 对话侧 AI 工具全部只读是安全设计：新建/调整目标、阶段、任务、计划或习惯统一走聊天内联的【生成成长方案】→用户显式确认→Java 原子幂等提交的录入流程；方案卡片与真实进度持续渲染在右侧“方案与进度”面板，用户可直接“接受并创建/接受并更新安排”或“暂不接受”。用户要求复盘时，`review_growth_progress` 会读取真实统计并把复盘显示在右侧；复盘 READY 后可生成调整草案，核对改期前后信息后再接受，由 Java 更新原安排并保留任务与历史。系统提示词已明确要求模型引导该入口，不得回答“没有录入功能”或在未确认时声称已创建。

关键运行配置（`.env` / 环境变量同名）：`AGENT_BACKGROUND_TASK_TIMEOUT_SECONDS=300`、`AGENT_BACKGROUND_MAX_ATTEMPTS=3`、`AGENT_REVIEW_MAX_OUTPUT_TOKENS=4096`、`AGENT_REVIEW_MAX_ATTEMPTS=2`、`AGENT_REVIEW_RATE_LIMIT_COUNT=6`、`AGENT_REVIEW_RATE_LIMIT_SECONDS=3600`、`AGENT_PLANNING_MAX_OUTPUT_TOKENS=8192`、`AGENT_TURN_TIMEOUT_SECONDS=240`、`AGENT_TOOL_MAX_ROUNDS=5`、`AGENT_TOOL_MAX_CALLS=12`、`AGENT_WORKFLOW_TTL_SECONDS=900`。调用模型 `response_format=json_object` 时提示词必须包含字面量 `json`（DeepSeek 接口要求），规划与记忆抽取提示词已满足；规划输出被截断时会追加“精简/去重/必要字段”约束重试，仍超限返回可操作的 422。数据库角色只授予 agent schema 的最小权限（仅 `knowledge_chunk`、`memory_embedding` 允许 DELETE 以支持解析重试和记忆向量重建）。

接口、事件、配置、已执行检查及验收范围见：

- [Phase 2 收尾与验收](../docs/agent/Phase2收尾与验收_20261106.md)
- [P1.4.0 记忆与知识库 SQL 实现](../docs/agent/P1.4.0记忆与知识库SQL实现.md)
- [P1.4.1 长期记忆实现](../docs/agent/P1.4.1长期记忆实现.md)
- [P1.4.2 知识库与后台任务实现](../docs/agent/P1.4.2知识库与后台任务实现.md)
- [P1.4.3 知识检索与 RAG 实现](../docs/agent/P1.4.3知识检索与RAG实现.md)
- [P1.4.4 上下文预算与协作实现](../docs/agent/P1.4.4上下文预算与协作实现.md)
- [P1.5.0 工作流 SQL 与接口契约](../docs/agent/P1.5.0工作流SQL与接口契约.md)
- [P1.5.1 工作流运行时与方案生成](../docs/agent/P1.5.1工作流运行时与方案生成.md)
- [P1.5.2 Java 原子批量提交实现](../docs/agent/P1.5.2Java原子批量提交实现.md)
- [P1.5.3 目标阶段任务工作流](../docs/agent/P1.5.3目标阶段任务工作流.md)
- [P1.5.4 周计划日计划工作流](../docs/agent/P1.5.4周计划日计划工作流.md)
- [P1.5.5 习惯工作流与综合成长规划](../docs/agent/P1.5.5习惯工作流与综合成长规划.md)
- [P1.3.1 实际验收与整改](../docs/agent/P1.3.1实际验收与整改.md)
- [P1.3.2a 成长查询工具实现](../docs/agent/P1.3.2a成长查询工具实现.md)
- [P1.3.2b 计划查询工具实现](../docs/agent/P1.3.2b计划查询工具实现.md)
- [P1.3.2c 执行查询工具实现](../docs/agent/P1.3.2c执行查询工具实现.md)
- [P1.3.2d 习惯查询工具实现](../docs/agent/P1.3.2d习惯查询工具实现.md)
- [P1.3.2e 统计与时间线查询工具实现](../docs/agent/P1.3.2e统计与时间线查询工具实现.md)
- [P1.2 会话与上下文后端实现](../docs/agent/P1.2会话与上下文后端实现.md)
