# 后端架构约定

2026-11-05：用户批准新增独立 agent 工程，当前十三个 Maven 子模块，保留各业务独立工程，common 集中公共能力。Agent 按 P1.1～P1.6 顺序开发，每阶段用户验收后才进入下一阶段。

| 工程 | 当前内容 | 后续职责 |
|---|---|---|
| app | 唯一启动类、本地 application.yml、10 份 SQL（26 表定义及看板／统计只读原型） | 启动和组装，不新增其他 Java 类 |
| common | 公共依赖、统一结果、异常、安全、工具与配置 | 全局异常、统一响应、security、工具、配置及业务目录之外的其他内容 |
| 10 个业务工程 | 均已有后端实现，新增四模块及三个主体状态历史；只读模块不虚构持久化表 | 仅在指定目录实现业务；通知与调度按需添加 |
| agent | P1.1 Java 认证代理及普通／流式对话入口；仅依赖 common | Python 独立服务负责模型调用；会话和工具留待后续验收阶段 |

```text
app → common + 全部业务工程
各业务工程 → common
common → 框架依赖
```

第三方公共运行依赖集中声明于 common，父 POM 统一管理版本。app 负责可执行打包，其余工程生成普通 JAR。当前业务工程互不依赖，common 不反向引用业务。

后续类固定采用 XxxService 接口与 XxxServiceImpl 实现，Controller 只注入接口，ServiceImpl 调用 Mapper。XML 在所属业务工程 resources/mapper/<模块>/XxxMapper.xml，不使用 SQL 注解。实体归 entity，请求归 dto，响应归 vo。

数据库文件集中在 app/src/main/resources/sql/<业务工程>.sql，一业务工程一文件，无版本前缀、无自动迁移、无自动执行。既有 identity 十表、成长六表和 execution 两表保持；后续新增习惯四表、通知一表、三类主体状态历史三表，共二十六张业务表。dashboard、statistics 不另建表，已实现只读聚合 Mapper/XML。后续八表已在 SQL 第一阶段通过隔离校验，本轮为启动后端手工补齐本机开发库。

common 已实现统一 ResultVO、异常处理、Security、JWT、Redis 会话、验证码、限流、Mapper 自动发现和启动依赖检查。identity SQL 验收见 [阶段材料](../docs/identity/SQL阶段验收.md)；后端编码状态见 [后端实现与启动说明](../docs/identity/后端实现与启动说明.md)，业务测试尚未执行。

账户认证采用 JWT + Security + Redis，多端独立会话、连续七天无有效操作过期；不再采用 JDBC 会话。公共安全机制放 common，经中立安全契约调用 identity 的 Service，common 不引用业务 Mapper。会员后续作为独立权益接入，本轮仅三个 RBAC 身份。

同一主体的增删改查使用同一组 Mapper、Service 接口与实现、Controller；三个关联表由用户或角色链路在同一事务维护。统一结果类型使用 common.result.ResultVO 与 PageResultVO。

成长业务的跨模块用户锁、关系投影和基础校验集中于 common 的 GrowthContextUtils 与 GrowthSupportMapper/XML。各业务 Service 不依赖其他业务工程。所有成长写事务先锁本人 identity_user 行，再检查资源、父链、日期及版本；成功审计通过中立接口交给 Identity，与业务一起提交。

具体实现范围见 [成长后端说明](../docs/growth/后端实现与启动说明.md)。

Execution 已完成后端指定分层，execution/scheduling 只触发到期扫描；配置与 ExecutionLifecycleProvider 公共联动契约在 common。用户行锁先于会话和时间段锁；任务、目标、计划、账户在原事务内调用联动，避免业务模块互相依赖。控制心跳和自动 current 轮询不延长登录有效期，管理身份不开放个人计时。

目标改为只归档：旧删除入口返回 410，归档结束全部未结束会话与有效安排，恢复不自动重开。执行历史和逻辑删除标识保留；原目标页面只同步必要合同兼容，执行计时新界面留在后续阶段。当前实现和验证边界以 [Execution 后端说明](../docs/execution/后端实现与启动说明.md) 为准。

后续八表 SQL 阶段见 [验收说明](../docs/remaining/SQL阶段验收说明.md)，对应后端已实现，见 [实现说明](../docs/remaining/后端实现与启动说明.md)。习惯用修订保留每日规则，打卡绑定当时修订；三个成长主体各保留自己的状态历史。目标达标通知同轮唯一，条件缓存不替代实时进度。看板与统计只读业务事实，不另建汇总表。源码授权默认准备态，本地手工初始化已启用后端动作，新增页面继续隐藏。
