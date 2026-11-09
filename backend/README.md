# 后端框架与账户模块

2026-11-04：common 与 identity 后端编码已完成，编译打包和启动通过。统一结果使用 common.result.ResultVO；没有运行单元、系统、集成或接口业务测试。具体职责、运行方式与验证边界见 [后端实现与启动说明](../docs/identity/后端实现与启动说明.md)。

## 工程结构

父工程聚合 13 个子工程：11 个业务工程 + app + common；连同父工程为 14 个 Reactor 项目。依赖方向：app → common + 11 个业务工程；每个业务工程 → common。common 不依赖 app 或业务工程。

```text
backend/
  pom.xml
  app/
    src/main/java/com/asterway/AsterwayApplication.java
    src/main/resources/
      application.yml
      sql/                       # 每个业务工程一份 SQL，包含后续八表与只读聚合原型
  common/src/main/java/com/asterway/common/
    config/ dto/ exception/ result/ security/ utils/ filter/
  <业务工程>/src/main/
    java/com/asterway/<模块>/
      entity/ dto/ vo/ mapper/ service/ service/impl/ controller/
    resources/mapper/<模块>/
```

10 个业务工程为 identity、goal、stage、task、planning、execution、habit、dashboard、statistics、notification，均已有后端实现。后续四个模块及三个主体状态历史新增 38 个 HTTP 操作，前端本轮未修改，业务流程仍待人工验收。

另新增第 11 个业务工程 agent，P1.1 提供普通对话与 SSE 代理；不包含 Entity 或 Mapper，因为本阶段不持久化数据。公共 HTTP 客户端及配置放 common，Python 服务独立放在仓库根目录 agent。见 [P1.1 实现与验收](../docs/agent/P1.1基础对话实现与验收.md)。

| 业务 | Maven 工程 |
|---|---|
| 账户 | `identity` |
| 目标 | `goal` |
| 阶段 | `stage` |
| 任务 | `task` |
| 计划 | `planning` |
| 执行计时 | `execution` |
| 习惯 | `habit` |
| 看板 | `dashboard` |
| 历史统计 | `statistics` |
| 系统通知 | `notification` |
| 成长助手代理 | `agent` |

后续全部 Service 使用接口与实现类，Mapper 使用接口与 XML；统一扫描等配置归 common。通知与定时任务有实际需求后再在对应业务工程增加。

## 版本与构建

保持 Java 17、Spring Boot 3.5.16、MyBatis Starter 3.0.5、Springdoc 2.8.17、Bouncy Castle 1.80；PostgreSQL JDBC、Spring Security、Redis 与 JWT 支持继续由同一 Spring Boot 父版本管理。现有构建环境为 Maven 3.9.4。Flyway 依赖已移除。

在仓库根目录执行：

```powershell
mvn -B -ntp -f backend/pom.xml '-Dmaven.test.skip=true' package
java -jar backend/app/target/app-0.1.0-SNAPSHOT.jar
```

IDEA 导入 backend/pom.xml，使用 JDK 17，运行 app 的 AsterwayApplication。仅 app 生成可执行 JAR，其余模块为普通 JAR。

## 本地配置与 SQL

app 中只有一个 application.yml，不使用环境变量、profile 或外部导入。端口 8089；本机 WSL2 开发库使用 127.0.0.1:5432/asterway_dev，开发角色 asterway_dev；Redis 使用 127.0.0.1:6379。均为本机专用配置，不连接远程数据库。开发前从仓库根目录运行 scripts/start_identity_local.ps1。

app/src/main/resources/sql 下各业务工程各一份 SQL。既有十八表为账户十表、成长六表、执行两表；后续新增习惯四表、通知一表和目标／阶段／任务状态历史三表，共二十六张业务表。看板、统计已实现只读 Mapper 与 Service，不增加表。成长前端已有入口继续启用；执行计时和本轮新增页面保持隐藏，旧目标删除动作已停用。源 SQL 的新授权默认准备态，本机已手工启用二十三项后端权限并补齐八表，保留旧业务事实。SQL 仍手工维护与执行，不使用迁移版本管理，框架不会自动执行。账户认证采用 JWT + Security + Redis，不建立 JDBC 会话表。

全局异常、统一结果、JWT + Security + Redis、账户 RBAC、用户与设备管理代码已实现。账户前端已在此前完成；本轮只完成成长业务后端，成长前端现已实现。业务功能尚未验收，历史文档中的行为不能直接视为当前实现已验证。

完整约束见 [架构约定](ARCHITECTURE.md) 与 [开发规则](AGENTS.md)。

后续模块 SQL 与后端已完成本地编码。SQL 第一阶段的 262 项隔离校验结果保持，后端按其表结构实现习惯、通知、看板、统计和状态历史，新增 38 个操作。验收入口为 [后端实现与启动说明](../docs/remaining/后端实现与启动说明.md)、[业务与接口约定](../docs/remaining/后端业务与接口约定.md) 和 [构建启动结果](../docs/remaining/backend-build-results.json)。只验证编译打包及启动，不将其视为业务验收。

## 本次验证

2026-11-04 按十个独立业务工程修正后，13 项 Reactor 的 clean verify 全部通过。当前没有测试类，未运行具体业务测试。可执行 JAR 已核对包含 10 个业务工程、common、唯一启动 class 和 10 份 SQL，占位资源完整，不再包含 growth、insights 或 administration 工程。此处为框架重建阶段的历史记录；当前 SQL 验收结果见下。

2026-11-04 identity SQL 阶段：真实 PostgreSQL 17.11 的 84 项 SQL 用例全部通过；Windows JDK 17 JDBC 认证查询和 Redis PING 通过。SQL 事务原型不等于 Service 已实现，详见验收材料。

本轮后端：十三项 Reactor 使用 -Dmaven.test.skip=true 编译打包通过，最终 JAR 与数据库、Redis 启动连接检查通过；没有运行业务测试。

## 核心成长后端

目标、阶段、任务、周计划、日计划、任务安排共六条单表业务链路、42 个 HTTP 映射已编码。补齐了权重进度、归档恢复、排序、任务池、改期历史和待处理查询。角色权限与数据归属同时生效，核心成长只允许普通用户，管理员与超级管理员不使用成长业务。

本地手工运行 scripts/init_growth_local.ps1 初始化成长表、32 项新增权限与 27 个菜单节点（7 个目录/页面入口启用）。新库授权定义位于 identity.sql 中；应用不自动执行 SQL，不使用 Flyway。现有库不需要重跑整份 identity.sql。

详见 [后端实现与启动说明](../docs/growth/后端实现与启动说明.md) 和 [接口与授权清单](../docs/growth/后端接口与权限清单.md)。本轮只检查编译打包与启动，没有运行完整业务接口验收。

## 执行计时后端

Execution 已实现执行会话与连续时间段、十四个 REST 映射、SID 控制心跳、到期暂停和实际投入汇总。任务、计划、目标及登录会话通过 common 中立契约联动；普通用户专属限制和权限白名单同步生效。

已手工运行 scripts/init_execution_local.ps1 初始化两表和七项 USER 后端权限，执行页面仍隐藏。目标删除接口退役为 410，归档请求改为 `{version, confirmed:true}`，结束全部未结束执行及有效安排；原目标页面只同步合同兼容，尚未新增 Execution 页面。当前共十八张业务表。

十三项 Reactor 打包及 Java 17 启动通过，没有运行单元、系统、集成测试或完整业务验收。详见 [Execution 后端实现与启动说明](../docs/execution/后端实现与启动说明.md)。前面的成长 SQL／前端记录保留为此前阶段说明。
