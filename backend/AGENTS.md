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

15. 当前进入核心成长业务 SQL 阶段：按用户提供的 V1.0 设计说明，实现 goal.sql、stage.sql、task.sql、planning.sql，共目标、阶段、任务、周计划、日计划、任务安排六表。仅允许 SQL 设计、隔离回滚校验和验收文档；用户验收 SQL 后再开发相应 Mapper、Service、Controller 与前端。此阶段不部署开发库、不修改 Identity 授权数据、不提前实现执行计时、习惯、统计或通知；第 1、13、14 条的旧阶段范围以本条和用户最新授权为准。

16. 核心成长 SQL 已获用户确认，现允许按 entity → Mapper/XML → DTO/VO → Service 接口/实现 → Controller 完成 goal、stage、task、planning 后端，随后更新 Identity 的成长权限、菜单和手工初始化。仅开发后端，不修改前端，不新增或运行单元、系统、集成测试；只编译打包与启动检查。本机可手工初始化已确认的六表和授权，但应用仍不自动执行 SQL。该最新授权替代第 15 条的 SQL-only 阶段限制。

17. 当前用户已批准成长前端 9 个页面与 68 项交互。核心成长仅限不带 ADMIN/SUPER_ADMIN 的普通用户；管理身份优先于保留的 USER 基础角色。同步调整权限有效集、Service 拒绝、菜单过滤、角色授权及种子数据；成长默认授权仅 USER，启用已完成的成长前端入口。继续只做编码、构建、启动，不建立或运行单元、系统、集成测试；未经本次成果单独审批不提交或推送。

18. 当前用户已批准 Execution 分阶段计划，先交付 execution_session、execution_segment 两表 SQL、Identity 分阶段权限菜单初始化片段、目标旧归档安排处理说明和隔离 SQL 校验。SQL 未经用户验收前，不部署开发库、不实现 Execution Java／前端、不提前切换目标旧删除／归档接口。计时为服务端时间、同用户一条运行、同任务一条未结束、每次暂停继续保留时间段；目标新规则为只归档、不删除，归档结束全部未结束执行和全部有效安排，后端阶段同批接入。权限菜单准备态禁用隐藏，后端和前端完成后分别手工启用。SQL 校验不等同于 Service／HTTP 验收；保留现有工作区和 stash，仍不得提交、推送或合并。

19. 用户已确认继续 Execution 后端，现按已验收 SQL 实现实体、Mapper/XML、DTO/VO、Service 接口及实现、Controller 和到期调度，同批接入任务／计划／目标／账户生命周期。允许手工初始化已确认的两表和后端动作授权，前端入口仍隐藏；历史归档安排先预览，不擅自清理。本轮只编码、编译打包和启动验证，不建立或运行单元、系统、集成测试，不开发新前端；固定本地分支，不提交、推送或合并。

20. Execution 后端已完成本地编码、打包和 Java 17 启动，原目标页面按批准计划只同步归档确认／新请求体和删除入口退役，不新增 Execution 页面；兼容改动通过前端构建。当前开发库已手工建立两张执行表、启用七项后端权限，执行菜单继续隐藏。业务流程尚未做系统／集成验收，后续前端仍先确认页数和交互，再开发新页面。

21. 用户已批准后续模块方案并授权第一阶段 SQL 开发与 SQL 测试：新增习惯四表、目标达标通知一表、目标／阶段／任务状态历史三表，共八张表；补齐目标达标条件轮次、准备态权限菜单和只读看板／统计查询原型。仅在 asterway_identity_test 随机隔离 schema 验证新库、既有十八表补齐、初始化、统计口径与数据库并发约束，最终回滚或清理本次 schema；不连接或部署开发库，不提前编写 Java／前端，不提交、推送或合并。此阶段不新建测试框架，SQL 校验结果不等同于 Service／HTTP 业务验收。

22. 用户已审阅 SQL 第一阶段并明确继续后端开发，现允许按 Entity → Mapper/XML → DTO/VO → Service 接口/实现 → Controller 完成 habit、notification、dashboard、statistics，接入目标／阶段／任务状态历史、执行开始、账户状态和时区联动，补齐 Identity 权限有效集及菜单初始化。为编译打包和运行启动，可手工补齐已确认的八表与后端授权，保留全部旧业务事实，前端入口继续隐藏。只验证编译打包和运行启动，不新增或运行严格单元、系统、集成测试，不开发前端，不提交、推送或合并。

23. 后续四模块及三个主体状态历史已完成本地后端编码，新增三十八个 HTTP 操作；十三项 Reactor clean package 与 Java 17 启动通过。已手工补齐本机开发库为二十六表，启用二十三项新后端权限，新页面全部隐藏，旧阶段／任务／计划／安排／执行事实保持不变。应用角色仅可读取和追加状态历史及习惯修订。业务流程仍为 NOT_VERIFIED，未修改前端、未创建提交或推送；具体证据见 docs/remaining 的后端说明、接口约定和构建记录。

24. 用户已明确批准前端八页、七类弹窗／抽屉和七十三项交互，现允许实现执行、习惯、通知、看板、统计及原详情历史接入，沿用 Vue、Element Plus、ECharts 既有版本，保留旧页面。普通用户使用核心业务，管理身份优先限制。前端完成构建及启动后，手工启用对应已实现菜单，并补齐必要的组件白名单兼容；不建立严格单元／系统／集成测试，不提交、推送或合并。

25. 八页前端及七十三项交互已完成本地编码，保留原二十四页；前端构建、后端十三项 Reactor 打包、Java 17 启动及八页只读显示检查完成。同步补齐菜单白名单、看板任务分组分页和无执行段日期的零耗时聚合，未改表结构或已验收 SQL。六个新菜单已手工启用，仅普通用户可用；执行记录和时间线从主页面进入。未调用业务变更接口或建立严格测试体系，完整业务验收为 NOT_VERIFIED；验证及源文件哈希见 docs/remaining/frontend-build-results.json。保留原分支、工作区、暂存与 stash，不提交、推送或合并。

26. 用户现已明确授权将本次完整成果推送 feature/LingXI-develop 和 main，并创建指定的 V1 标签；该标签按本次明确命名，不套用默认前缀。用户随后取消远程部署和数据库初始化，只发布 GitHub。发布 YAML 的实际连接信息和密钥置空，私密运行配置与现有暂存、stash 留在本地；使用独立索引生成发布树，不切换或新建本地分支。适用构建、启动与脱敏检查完成后可执行本次推送，随后核对远程引用、作者和发布文件；不运行严格测试体系。
