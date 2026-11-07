# Asterway 后端

Java 17、Spring Boot 3.5.16、MyBatis 多模块工程。十三个子模块为 app、common、identity、goal、stage、task、planning、execution、habit、dashboard、statistics、notification、agent。

## 分层与边界

`app` 仅承载启动类、配置模板和业务 SQL。`common` 集中安全、异常、配置及中立联动接口；业务工程采用 Controller → Service 接口 → ServiceImpl → Mapper/XML，不增加业务工程间的 Maven 依赖。

`agent` 提供成长助手公开代理、会话及工作区接口和原子方案提交。业务写入校验本人归属、角色权限、不可变方案快照、版本与幂等键；失败整批回滚，保留提交回执与安排历史。

## 构建与部署

在仓库根目录执行 `mvn --no-transfer-progress -f backend/pom.xml -DskipTests package`。仅 app 生成可执行 JAR，其余模块为普通 JAR。

发布的 `app/src/main/resources/application.yml` 是无凭据模板。启动时通过受控私密配置目录提供实际连接与密钥，例如 `--spring.config.additional-location=file:./.asterway-private/local-runtime/config/`。业务数据库、Redis、JWT 与内部通信密钥由部署者配置。

SQL 位于 `app/src/main/resources/sql`，按模块手工初始化；不使用自动建表或自动迁移。Agent schema 使用受限角色，Java 仅按工作流需要读写相关方案与提交记录。

接口统一前缀 `/asterway`。V2.0 功能及验证结果见 [发布说明](../docs/releases/V2.0.md)。
