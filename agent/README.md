# Asterway 成长助手

V2.0 的独立 Python 服务。负责连续对话、30 个只读业务查询工具、上下文摘要、长期记忆、知识检索、方案草案、成长复盘与运行记录。

## 运行

使用 Python 3.12 创建虚拟环境，执行 `pip install -e ".[dev]"`，将 `.env.example` 复制为 `.env` 并填写模型、Embedding、内部通信密钥及受限数据库连接。在 Windows 使用 `.venv/Scripts/python.exe run.py` 启动。

默认聊天服务使用 DeepSeek；Embedding 单独配置。内部通信密钥必须与 Java 一致。真实配置、上传资料和运行数据不提交到仓库。

## 业务边界

浏览器只通过 Java 的 `/asterway/agent/**` 访问；内部 `/internal/agent/**` 同时验证用户身份与内部通信密钥。Python 数据库角色只访问 `agent` schema，成长事实由 Java 权限校验与原子事务维护。

方案生成、修订和复盘分析不会自动接受或创建成长业务内容。用户在方案面板显式接受后才允许提交；接受、实际创建与任务完成分别展示。提交按不可变方案版本及幂等键执行。

## 存储与流程

- PostgreSQL：会话、消息、摘要、记忆、资料及不可变版本、后台任务、方案与版本、复盘、调用记录及 LangGraph 检查点。
- pgvector：1024 维知识与记忆检索。引用关联原始文档版本，可回看对应内容。
- 查询预算：默认每轮最多 12 次、5 轮，等价成功查询复用；规划与复盘各有独立调用机会。
- 方案调整：复用原任务与安排版本，保留改期历史，冲突时整批回滚。
- 恢复：方案提交幂等重试、失败复盘重试、失败记忆任务重新排队。

## 验证

`python -m pytest -q`、`python -m ruff check app tests`、`python -m compileall -q app`。配置与预算详见 `.env.example` 及 `app/core/config.py`；公开发布内容见 [V2.0 发布说明](../docs/releases/V2.0.md)。
