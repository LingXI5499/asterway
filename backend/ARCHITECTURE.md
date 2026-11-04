# 后端架构约定

2026-11-04：本文件替代此前迁移设计，保留多模块框架，不迁移旧业务能力；identity 已完成 SQL 和后端编码，common 公共能力已实现；新前端留待后续。

| 工程 | 当前内容 | 后续职责 |
|---|---|---|
| app | 唯一启动类、本地 application.yml、10 份 SQL（identity 已实现，其余占位） | 启动和组装，不新增其他 Java 类 |
| common | 公共依赖、统一结果、异常、安全、工具与配置 | 全局异常、统一响应、security、工具、配置及业务目录之外的其他内容 |
| 10 个业务工程 | identity 已实现指定分层，其余工程保留 entity、dto、vo、mapper、service/impl、controller 与 XML 目录 | 仅在指定目录实现业务；通知与调度按需添加 |

```text
app → common + 全部业务工程
各业务工程 → common
common → 框架依赖
```

第三方公共运行依赖集中声明于 common，父 POM 统一管理版本。app 负责可执行打包，其余工程生成普通 JAR。当前业务工程互不依赖，common 不反向引用业务。

后续类固定采用 XxxService 接口与 XxxServiceImpl 实现，Controller 只注入接口，ServiceImpl 调用 Mapper。XML 在所属业务工程 resources/mapper/<模块>/XxxMapper.xml，不使用 SQL 注解。实体归 entity，请求归 dto，响应归 vo。

数据库文件集中在 app/src/main/resources/sql/<业务工程>.sql，一业务工程一文件，无版本前缀、无自动迁移、无自动执行。identity 已实现十张表及初始授权数据，其余业务仍为空。

common 已实现统一 ResultVO、异常处理、Security、JWT、Redis 会话、验证码、限流、Mapper 扫描和启动依赖检查。identity SQL 验收见 [阶段材料](../docs/identity/SQL阶段验收.md)；后端编码状态见 [后端实现与启动说明](../docs/identity/后端实现与启动说明.md)，业务测试尚未执行。

账户认证采用 JWT + Security + Redis，多端独立会话、连续七天无有效操作过期；不再采用 JDBC 会话。公共安全机制放 common，经中立安全契约调用 identity 的 Service，common 不引用业务 Mapper。会员后续作为独立权益接入，本轮仅三个 RBAC 身份。

同一主体的增删改查使用同一组 Mapper、Service 接口与实现、Controller；三个关联表由用户或角色链路在同一事务维护。统一结果类型使用 common.result.ResultVO 与 PageResultVO。
