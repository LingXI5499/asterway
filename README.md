# Asterway V2.0

个人成长管理系统。围绕目标、阶段、任务、周／日计划、实际执行与成长复盘，形成持续可调整的行动流程。

维护者：**LingXI5499**。发布标签：**V2.0**。API 前缀为 `/asterway`，保留现有工程及依赖版本。

## 功能

- 账户与权限：三角色 RBAC、用户／角色／权限／菜单管理、多端会话、账户安全和操作日志。
- 成长业务：目标、阶段、任务、周／日计划、任务池、安排历史和实际执行计时。
- 习惯与反馈：习惯规则、打卡、提醒、今日看板、投入统计和成长时间线。
- 全局成长助手：连续会话、流式回复、停止生成、历史恢复，以及可调整宽度的导航和方案面板。
- 业务工具：30 个只读查询工具，支持目标全貌、查询结果复用、权限与预算校验。
- 记忆与资料：已确认长期记忆、资料上传解析、向量检索、不可变文档版本与引用回看。
- 规划与执行：自动澄清、整理草案、用户显式接受后原子创建；支持方案版本、取消、过期及幂等重试。
- 执行反馈闭环：实际进度 → 复盘 → 调整草案 → 用户确认 → 更新原安排 → 继续跟踪；改期保留原任务与安排历史。

## 工程

`backend` 为 Java 17 / Spring Boot 多模块服务，`agent` 为 Python 3.12 独立服务，`frontend` 为 Vue 工作区。

- Java 负责公开接口、身份与业务权限、成长事实及原子幂等提交。
- Python 负责对话、工具编排、知识检索、规划草案和复盘。
- PostgreSQL 保存业务事实与会话；Redis 保存认证会话；pgvector 用于 1024 维资料与记忆检索。
- 业务 SQL 按工程集中在 `backend/app/src/main/resources/sql`，由部署者手工初始化；应用不自动建表或迁移。

## 配置与启动

发布的 `application.yml` 和 `.env.example` 均为无凭据模板。部署者在受控私密配置中填写数据库、Redis、JWT、模型与内部通信密钥，内部密钥须在 Java 和 Python 两侧一致。

```powershell
# 仓库根目录构建 Java
mvn --no-transfer-progress -f backend/pom.xml -DskipTests package

# 使用自行准备的私密配置目录启动
java -jar backend/app/target/app-0.1.0-SNAPSHOT.jar --spring.config.additional-location=file:./.asterway-private/local-runtime/config/

# Python：复制模板后填写本地 .env
cd agent
python -m venv .venv
.venv/Scripts/python.exe -m pip install -e ".[dev]"
Copy-Item .env.example .env
.venv/Scripts/python.exe run.py

# 前端：在 frontend 目录执行
npm ci
npm run dev
```

Python 在 Windows 使用 `run.py` 启动，以启用数据库异步驱动所需的事件循环。前端开发服务代理 `/asterway` 到 Java；生产部署应自行配置反向代理。

## 验证

Python 40 项、前端逻辑 9 项、隔离 Java 事务 18 项、隔离公开 HTTP 闭环 22 项、浏览器交互 16 项已通过。结构化规划、确认写入和反馈调整使用合成账号与可丢弃数据验收。

```powershell
# agent 目录
.venv/Scripts/python.exe -m pytest -q
.venv/Scripts/python.exe -m ruff check app tests

# frontend 目录
npm run test:agent
npm run build
```

详见 [V2.0 发布说明](docs/releases/V2.0.md)、[成长助手说明](agent/README.md)、[后端说明](backend/README.md) 和 [前端说明](frontend/README.md)。
