# 后端模块与编码约定

本文件以 2026-11-04 后端重建要求为准，替代此前 app 承载 Controller、DTO、VO 的分层约定。

1. common 与 identity 已获批开发后端；其他业务工程保留空框架。本轮只做编码、编译打包与启动验证，不新增或执行单元、系统、集成测试，不开发新前端。
2. app 只放一个启动类、application.yml，以及 src/main/resources/sql 下按业务工程命名的 SQL，不放其他 Java 类。
3. common 集中全局异常、统一响应、安全、配置、工具、常量、枚举、过滤器、拦截器及业务目录之外的其他内容。不得依赖 app 或业务工程。
4. 业务工程仅允许 entity、dto、vo、mapper、service、service/impl、controller；XML 放自身 src/main/resources/mapper/<模块>。不设置 api、model、config、security、constant 等额外目录，其他内容暂归 common。
5. 所有 Service 后续必须使用 service/XxxService.java 接口和 service/impl/XxxServiceImpl.java 实现；只给实现类加 @Service，调用方注入接口。
6. Mapper 必须使用接口与对应 XML，不使用 SQL 注解；namespace 对应 Mapper 全限定名，语句 id 对应方法，参数使用绑定方式。后续统一扫描配置归 common，当前不创建配置类。
7. 调用方向为 Controller → Service 接口 → ServiceImpl → Mapper/XML。当前每个业务工程只依赖 common，不增加业务工程间依赖。
8. 站内消息通知、定时任务确有需要时，再在对应业务模块增加专属目录；当前不创建具体通知或调度代码。
9. SQL 按业务工程一工程一文件，集中在 app/src/main/resources/sql，手工维护与执行。不使用 Flyway，不维护迁移版本链，不自动执行 SQL。
10. 保持既有技术版本；仅使用 app/application.yml 本地开发配置，不使用环境变量、profile 或配置导入，不放真实凭据。
11. 固定在 feature/LingXI-develop 本地开发；保留其他未授权修改、暂存区与既有 stash。feature 推送与 main 合入分别取得本次成果的明确审批。

12. 按业务独立拆分为 identity（账户）、goal（目标）、stage（阶段）、task（任务）、planning（计划）、execution（执行计时）、habit（习惯）、dashboard（看板）、statistics（历史统计）、notification（系统通知）。不得重新合并为 growth/insights，当前不设置 administration 工程。加 app 和 common 共 12 个 Maven 子模块，父工程不计入子模块。

13. identity SQL 已获用户确认，保留系统配置表，本轮继续完成后端。新前端留待后续。认证方案为 JWT + Security + Redis 多端会话，连续七天无有效操作过期；会员独立于 RBAC，本轮只启用三角色。

14. 用户已确认 SQL，现允许实现 common 与 identity 后端。统一结果使用 common.result.ResultVO，不使用 Response 命名；按表或业务主体保留一组 Mapper/Service/Controller，不按增删改查拆分。各 Service 为接口与实现类，命名后缀明确，中文注释、正常换行。用户明确本轮只验证编译打包与启动，不新增或运行单元、系统、集成测试，不开发新前端。
