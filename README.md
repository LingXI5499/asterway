# Asterway

个人成长管理系统：目标 → 阶段 → 任务 → 周／日计划 → 执行计时，并提供独立习惯、站内提醒、今日看板和真实历史统计。

维护者：**LingXI5499**。本次代码发布标记为 **V1**，API 统一前缀为 `/asterway`，不在接口路径中增加版本号。保留当前工程与技术依赖版本。

## 已实现范围

- 账户：三角色完整 RBAC，用户、角色、权限、菜单，JWT + Spring Security + Redis 可撤销多端会话，用户管理与账户安全。
- 成长：目标、阶段、任务，周／日计划，任务池与安排历史；目标只归档，保留历史事实。
- 执行：开始、暂停、继续、结束、心跳、超时与计划边界处理，连续执行时间段和实际耗时，任务／目标／账户联动。
- 习惯：用户自有分类，每天或指定星期，今日打卡与撤销，生命周期与规则修订，真实历史完成率。
- 通知：目标达标提醒，已读与业务处理独立，确认完成或继续保持。
- 看板与统计：今日任务、习惯和投入；总／周／月／年／自定义区间，执行趋势、习惯分类分组及真实成长时间线。
- 前端：保留账户十五页、成长九页，新增八页，共三十二页；本轮新增七十三项交互和七类弹窗／抽屉。

个人核心业务仅供普通用户使用，管理员和超级管理员只做管理；同时带有 USER 的管理身份也不开放个人业务。权限和数据所属人由后端校验。

## 工程结构

十二个 Maven 子模块：app、common、identity、goal、stage、task、planning、execution、habit、dashboard、statistics、notification；父工程计入 Reactor 共十三项。

- app 只保留唯一启动类、YAML 和按业务工程分开的 SQL。
- common 集中配置、安全、统一 ResultVO、全局异常、工具和中立联动接口。
- 业务模块使用 Entity → Mapper/XML → DTO/VO → Service 接口及实现 → Controller，不增加业务模块之间的 Maven 依赖。
- PostgreSQL 保存业务事实，Redis 保存认证会话。不使用物理外键、级联删除、Flyway 或自动建表。

后端保持 Java 17、Spring Boot 3.5.16、MyBatis Starter 3.0.5；前端保持 Vue、Vue Router、Element Plus、ECharts 和 Vite 的原有锁定依赖。

## 私密配置与本地运行

`backend/app/src/main/resources/application.yml` 是随代码发布的配置模板。数据库 URL、用户名、密码，Redis 主机、密码以及 JWT 密钥均为空。仓库不提供实际连接信息或默认登录密码。

实际配置保存到未跟踪的 `.asterway-private/local-runtime/config/application.yml`，复制模板后仅在该私密文件中填写。Spring Boot 从启动工作目录的 config/application.yml 读取配置，不需要环境变量、profile 或配置导入。不要将填写后的文件放回 Git。

```powershell
# 在仓库根目录，使用 JDK 17 和 Maven 构建
mvn --no-transfer-progress -f backend/pom.xml -DskipTests clean package

# 准备好私密配置与数据库后，在私密工作目录启动
cd .asterway-private/local-runtime
java -jar ../../backend/app/target/app-0.1.0-SNAPSHOT.jar
```

前端在 frontend 目录执行 `npm ci`、`npm run dev`，构建使用 `npm run build`。开发端口为 5175，后端为 8089，前端代理 `/asterway` 到本地后端。

## SQL 与初始化

十份业务 SQL 位于 app/src/main/resources/sql，共二十六张业务表。SQL 中包含阶段标记、授权准备态及查询原型，应按对应说明手工执行；应用不会自动建表或迁移。

1. [账户数据库与授权](docs/identity/数据库与授权设计.md)及[账户 SQL 阶段记录](docs/identity/SQL阶段验收.md)。
2. [成长 SQL 阶段](docs/growth/SQL层验收说明.md)。
3. [执行 SQL 阶段](docs/execution/SQL阶段验收说明.md)。
4. [后续模块 SQL 阶段](docs/remaining/SQL阶段验收说明.md)。

初始超级管理员密码由初始化脚本随机生成并写入指定私密文件，首次登录必须修改。已实现前端完成后，使用 `scripts/enable_remaining_frontend_local.ps1` 手工启用入口；该脚本固定本地开发库，不会重建表或清理业务事实。

历史连接检查工具只接受 `--connection-file` 指定的私密 JSON；本地基础设施准备脚本的数据库密码从私密文件读取，不在源码中硬编码。

## 开发与验证记录

- [后端架构说明](backend/ARCHITECTURE.md)
- [后续后端接口](docs/remaining/后端业务与接口约定.md)
- [前端实现、页面和人工验收](docs/remaining/前端实现与验收说明.md)
- [前端构建和启动记录](docs/remaining/frontend-build-results.json)
- [V1 发布记录](docs/releases/V1.md)

本地前后端构建、后端十三项 Reactor 打包、Java 17 启动、数据库与 Redis 启动连接检查以及八页只读显示检查已完成。按当前要求，未新增或运行严格单元、系统、集成测试；完整业务链路验收为 **NOT_VERIFIED**。

旧设计资料保留业务讨论背景，不作为当前表结构、接口或已实现功能的承诺。本次仅发布 GitHub 开发分支、main 和 V1 标签，不包含远程部署或远程数据库初始化。
