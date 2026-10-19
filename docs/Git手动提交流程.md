# Git 手动提交流程

> 日期：2026-10-19。  
> 适用目录：`F:\project\asterway`。本次变更为技术版本统一、远程数据库部署叙述及阅读页同步。  
> 操作约定：下列命令均由开发者手动执行；文档修改不自动暂存、提交、推送或合并。

## 1. 确认仓库和当前分支

在 PowerShell 中执行：

```powershell
Set-Location -LiteralPath 'F:\project\asterway'
git status --short --branch
git branch --show-current
git remote -v
git log -5 --oneline
```

编写本文时当前分支为 `feature/Lingxi-develop`，跟踪 `origin/feature/Lingxi-develop`；本地 `main` 跟踪 `origin/main`。远程为 `https://github.com/LingXI5499/asterway.git`。执行时以实际输出为准；以下当前分支命令按 `feature/Lingxi-develop` 编写。

## 2. 检查本次改动

现有 Markdown 使用 CRLF 和双空格硬换行，下文的空白检查按这一格式执行；`-c` 参数仅对当前命令生效。

```powershell
git diff --stat
git -c 'core.whitespace=-blank-at-eol,cr-at-eol' diff --check
git diff -- docs/README.md
git diff -- 'docs/依据/V1模块划分与技术选型.md'
git diff -- 'docs/依据/V1系统架构设计.md'
git diff -- 'docs/09-需求接口数据追溯与变更记录.md'
git ls-files --others --exclude-standard
```

确认 JDK 17、Maven 3.9.4、Boot 3.5.16、远程 PostgreSQL 17.11、pgvector RPM 0.8.7 及前端版本与 [12 技术版本基线](12-技术版本基线与环境核验.md)一致。新文件尚未跟踪时不会出现在普通 `git diff` 中，需要直接打开核对。

打开 `docs/index.html`，检查“技术版本与环境”及“Git 手动提交”入口，搜索 JDK、Spring Boot、PostgreSQL，确认阅读页与 Markdown 一致。`index.html` 的内嵌数据较长，使用阅读页检查展示，并用 Git 查看其文件差异统计。

本次只做文档静态核对，不运行尚不存在的 Maven、前端构建或数据库迁移；应用验证状态继续保持 `NOT_VERIFIED`。

## 3. 暂存并检查暂存区

确认 `docs` 内全部改动都属于本次提交后执行：

```powershell
git add -- docs
git diff --cached --name-only
git diff --cached --stat
git -c 'core.whitespace=-blank-at-eol,cr-at-eol' diff --cached --check
```

有其他工作的文件时，改用 `git add -- '具体文件路径'` 逐个暂存。暂存区应仅包含本次相关文档及 `docs/index.html`，不包含口令、证书、日志或服务器连接秘密。需要查看完整内容时执行 `git diff --cached -- '具体文件路径'`。

若某文件暂存有误，以下操作只取消其暂存，保留工作区修改：

```powershell
git restore --staged -- '具体文件路径'
```

## 4. 创建本地提交

```powershell
git commit -m "docs: 统一技术版本与远程 PostgreSQL 部署基线"
git show --stat --oneline HEAD
git status --short --branch
```

提交只包含暂存区内容。若 Git 提示未配置身份，使用自己的真实姓名和邮箱在当前仓库执行 `git config user.name '你的姓名'`、`git config user.email '你的邮箱'`，再重新提交。

## 5. 同步远程并推送当前开发分支

先确认需要保留的修改已提交，工作区与暂存区干净，再执行：

```powershell
git fetch origin
git log --oneline --left-right HEAD...origin/feature/Lingxi-develop
git rebase origin/feature/Lingxi-develop
```

日志中的 `<` 表示仅本地存在，`>` 表示仅远程存在。远程未变化时 rebase 会保持当前结果；远程有新提交时，将本次本地提交接在其后。

出现冲突时，先运行 `git status`，编辑冲突文件并保留最终统一版本，重新同步阅读页，然后对已解决文件执行 `git add -- '具体冲突文件路径'`，最后执行 `git rebase --continue`。需要取消本次 rebase 时执行 `git rebase --abort`。完成后重新检查差异和阅读页。

同步成功后执行：

```powershell
git -c 'core.whitespace=-blank-at-eol,cr-at-eol' diff --check
git status --short --branch
git push origin HEAD:feature/Lingxi-develop
git fetch origin
git rev-parse HEAD
git rev-parse origin/feature/Lingxi-develop
git status --short --branch
```

两个提交哈希应一致，状态应显示工作区干净。推送被拒绝且原因是远程又有新提交时，重新 fetch、rebase、检查后推送；按普通推送处理，不使用强制覆盖。

## 6. 可选：合入 main

需要将本次文档同步到主分支时，在开发分支已推送、检查完成且工作区干净后执行：

```powershell
git switch main
git pull --ff-only origin main
git merge --ff-only feature/Lingxi-develop
git log -3 --oneline
git push origin main
git fetch origin
git rev-parse main
git rev-parse origin/main
git switch feature/Lingxi-develop
git status --short --branch
```

如果 `pull --ff-only` 或 `merge --ff-only` 报分支已分叉，先检查 `git log --graph --oneline --decorate --all -20`，按实际分支关系处理，或通过仓库的 Pull Request 流程合并。主分支同步完成后，核对本地 `main` 与 `origin/main` 哈希一致。
