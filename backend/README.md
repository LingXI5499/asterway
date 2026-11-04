# 后端框架与账户模块

2026-11-04：common 与 identity 后端编码已完成，编译打包和启动通过。统一结果使用 common.result.ResultVO；没有运行单元、系统、集成或接口业务测试。具体职责、运行方式与验证边界见 [后端实现与启动说明](../docs/identity/后端实现与启动说明.md)。

## 工程结构

父工程聚合 12 个子工程：10 个业务工程 + app + common；连同父工程为 13 个 Reactor 项目。依赖方向：app → common + 10 个业务工程；每个业务工程 → common。common 不依赖 app 或业务工程。

```text
backend/
  pom.xml
  app/
    src/main/java/com/asterway/app/AsterwayApplication.java
    src/main/resources/
      application.yml
      sql/                       # 每个业务工程一份 SQL，目前仅注释占位
  common/src/main/java/com/asterway/common/
    config/ dto/ exception/ result/ security/ utils/ filter/
  <业务工程>/src/main/
    java/com/asterway/<模块>/
      entity/ dto/ vo/ mapper/ service/ service/impl/ controller/
    resources/mapper/<模块>/
```

10 个业务工程为 identity、goal、stage、task、planning、execution、habit、dashboard、statistics、notification。空目录由 .gitkeep 保留。common 与 identity 已有具体实现，其余九个业务工程继续保留空目录。

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

后续全部 Service 使用接口与实现类，Mapper 使用接口与 XML；统一扫描等配置归 common。通知与定时任务有实际需求后再在对应业务工程增加。

## 版本与构建

保持 Java 17、Spring Boot 3.5.16、MyBatis Starter 3.0.5、Springdoc 2.8.17、Bouncy Castle 1.80；PostgreSQL JDBC、Spring Security、Redis 与 JWT 支持继续由同一 Spring Boot 父版本管理。现有构建环境为 Maven 3.9.4。Flyway 依赖已移除。

在仓库根目录执行：

```powershell
mvn -B -ntp -f backend/pom.xml clean verify
java -jar backend/app/target/app-0.1.0-SNAPSHOT.jar
```

IDEA 导入 backend/pom.xml，使用 JDK 17，运行 app 的 AsterwayApplication。仅 app 生成可执行 JAR，其余模块为普通 JAR。

## 本地配置与 SQL

app 中只有一个 application.yml，不使用环境变量、profile 或外部导入。端口 8089；本机 WSL2 开发库使用 127.0.0.1:5432/asterway_dev，开发角色 asterway_dev；Redis 使用 127.0.0.1:6379。均为本机专用配置，不连接远程数据库。开发前从仓库根目录运行 scripts/start_identity_local.ps1。

app/src/main/resources/sql 下各业务工程各一份 SQL。identity.sql 已实现十张表、三角色、37 项权限、39 个菜单/按钮及两项配置；其余业务 SQL 仍是注释占位。后续手工维护与执行，不使用迁移版本管理；框架不会自动执行 SQL，本轮已初始化本机专用开发库和测试库，未操作远程或旧数据库。账户认证已确定采用 JWT + Security + Redis，不建立 JDBC 会话表。后端已移除 Spring Session JDBC，改用原 Boot 版本管理的 Redis 与 JWT 支持。

全局异常、统一结果、JWT + Security + Redis、账户 RBAC、用户与设备管理代码已实现。旧前端仍使用此前接口，需后续按新契约重做对接。业务功能尚未验收，历史文档中的行为不能直接视为当前实现已验证。

完整约束见 [架构约定](ARCHITECTURE.md) 与 [开发规则](AGENTS.md)。

## 本次验证

2026-11-04 按十个独立业务工程修正后，13 项 Reactor 的 clean verify 全部通过。当前没有测试类，未运行具体业务测试。可执行 JAR 已核对包含 10 个业务工程、common、唯一启动 class 和 10 份 SQL，占位资源完整，不再包含 growth、insights 或 administration 工程。此处为框架重建阶段的历史记录；当前 SQL 验收结果见下。

2026-11-04 identity SQL 阶段：真实 PostgreSQL 17.11 的 84 项 SQL 用例全部通过；Windows JDK 17 JDBC 认证查询和 Redis PING 通过。SQL 事务原型不等于 Service 已实现，详见验收材料。

本轮后端：十三项 Reactor 使用 -Dmaven.test.skip=true 编译打包通过，最终 JAR 与数据库、Redis 启动连接检查通过；没有运行业务测试。
