# 核心成长业务 SQL 层验收说明

> 此文件记录上一阶段的 SQL 验收基线，已获用户确认。当前后端实现、授权初始化及验证边界见《后端实现与启动说明》。

本轮范围为目标、阶段、任务、周计划、日计划、任务安排，共 6 张表；仅完成 SQL 层，不进入 Mapper、Service、Controller 或前端。参考用户提供的《Asterway 核心成长业务模块开发设计说明 V1.0》，不改变现有技术版本。

## 1. 文件与执行

| 文件 | 表 |
|---|---|
| backend/app/src/main/resources/sql/goal.sql | goal |
| backend/app/src/main/resources/sql/stage.sql | stage |
| backend/app/src/main/resources/sql/task.sql | task |
| backend/app/src/main/resources/sql/planning.sql | weekly_plan、daily_plan、task_assignment |

继续使用 PostgreSQL 17.11、数据库自增 BIGINT、TIMESTAMPTZ、乐观锁和逻辑关联。每文件自带 BEGIN/COMMIT，手工执行；同名对象已存在时直接失败，不包含 DROP、TRUNCATE、IF NOT EXISTS、Flyway、外键、级联、触发器、定时任务或自动迁移。人工确认后建议按 goal → stage → task → planning 顺序执行。四文件分别提交，不代表跨文件原子部署；本轮没有向开发库部署，已有 identity.sql 不变。

日期字段均为可空规划日期（周一与日计划日期必填），拒绝 PostgreSQL 的无限日期值；填写起止日期时结束不得早于开始。日期按用户当前已生效时区解释，不按服务器日期推断。时间点使用 TIMESTAMPTZ，展示时转换为用户时区。

## 2. 状态与处理决定

| 项目 | 本轮采用 |
|---|---|
| 目标、阶段状态 | NOT_STARTED、IN_PROGRESS、COMPLETED；默认 NOT_STARTED |
| 任务状态 | NOT_STARTED、IN_PROGRESS、PAUSED、COMPLETED；默认 NOT_STARTED |
| 优先级 | LOW、MEDIUM、HIGH、URGENT；默认 MEDIUM，排序时映射等级，不按字母排序 |
| 权重 | 正整数，默认 1；暂停任务仍参与总权重 |
| 预计耗时 | 可空；填写时为正整数分钟，未知用 NULL，不用 0 |
| 空描述／完成条件 | 空字符串；名称不能是空白，最长 100 字 |
| 完成时间 | COMPLETED 必须有 completed_at，其他状态必须没有；重新打开任务须同时清空 |
| 进度 | 按任务权重聚合，不存目标或阶段进度字段；没有有效任务返回 NULL |
| 未来开始 | 允许；状态不靠 CHECK/CURRENT_DATE 随时间自动变化，当前列表按用户日期筛选 |
| 归档 | archived_at 独立于删除和完成状态；归档期间禁止新增阶段、任务或安排 |
| 名称／排序 | 不要求同名唯一、排序值唯一；排序相同时使用 id 作稳定次序 |
| 任务池 | 没有 ended_at IS NULL 的安排，不另建任务池表 |
| 计划重建 | 删除后的同周／同日可重新创建新记录；旧 id 和历史保留 |
| 任务完成 | 保留当前安排作为计划展示位置，不把完成误当作改期；禁止作为未完成任务重新安排 |
| 进度到 100% | 不在 SQL 中自动修改目标／阶段状态，不增加触发器 |

安排结束原因统一为：

| 编码 | 场景 |
|---|---|
| RESCHEDULED | 周内变更日期、从周进入日、跨周改期；结束旧记录并插入新记录 |
| RETURNED_TO_POOL | 用户移出计划；只结束旧记录 |
| TASK_DELETED | 逻辑删除任务时结束当前安排 |
| GOAL_ARCHIVED | 用户明确选择归档时结束未完成任务安排 |
| DAILY_PLAN_DELETED | 用户明确处理日计划内安排后删除日计划 |
| WEEKLY_PLAN_DELETED | 用户明确处理周计划内安排后删除周计划 |

历史安排保留原 task_id、weekly_plan_id、daily_plan_id 和 assigned_at，不覆盖原计划标识；不额外建执行历史或延期事件表。历史引用允许指向已逻辑删除对象，展示历史时必须显式按历史口径读取，不能误用正常有效数据过滤。

## 3. 数据字典

### goal

个人成长目标；归档与逻辑删除独立，进度由有效任务权重聚合计算。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| name | `varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]')` | 名称，1～100 字且不能仅为空白 |
| description | `text NOT NULL DEFAULT ''` | 描述，未填写时为空字符串 |
| completion_criteria | `text NOT NULL DEFAULT ''` | 完成条件，未填写时为空字符串 |
| start_date | `date CHECK (isfinite(start_date))` | 计划开始日期，可空；允许未来日期 |
| expected_end_date | `date CHECK (isfinite(expected_end_date))` | 预计结束日期，可空，不作为强制截止 |
| status | `varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))` | 状态：未开始、进行中、已完成 |
| archived_at | `timestamptz` | 归档时间，NULL 表示未归档 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |
| deleted_at | `timestamptz` | 逻辑删除时间，NULL 表示未删除 |

### stage

目标下的阶段；不得脱离所属用户与目标，进度不重复存储。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| goal_id | `bigint NOT NULL CHECK (goal_id > 0)` | 所属目标逻辑标识，须与 user_id 同属当前用户 |
| name | `varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]')` | 名称，1～100 字且不能仅为空白 |
| description | `text NOT NULL DEFAULT ''` | 描述，未填写时为空字符串 |
| completion_criteria | `text NOT NULL DEFAULT ''` | 完成条件，未填写时为空字符串 |
| start_date | `date CHECK (isfinite(start_date))` | 阶段开始日期，可空，不强制限制在目标日期内 |
| end_date | `date CHECK (isfinite(end_date))` | 阶段结束日期，可空 |
| sort_order | `integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0)` | 显示排序，数值越小越靠前；相同值按主键排序 |
| status | `varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'))` | 状态：未开始、进行中、已完成 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |
| deleted_at | `timestamptz` | 逻辑删除时间，NULL 表示未删除 |

### task

阶段下的唯一任务实体；安排和改期不复制任务，逻辑删除后保留历史标识。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| stage_id | `bigint NOT NULL CHECK (stage_id > 0)` | 所属阶段逻辑标识，目标通过阶段追溯 |
| name | `varchar(100) NOT NULL CHECK (name ~ '[^[:space:]]')` | 名称，1～100 字且不能仅为空白 |
| description | `text NOT NULL DEFAULT ''` | 描述，未填写时为空字符串 |
| completion_criteria | `text NOT NULL DEFAULT ''` | 完成条件，未填写时为空字符串 |
| priority | `varchar(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))` | 优先级：低、中、高、紧急；业务排序使用显式优先级映射 |
| weight | `integer NOT NULL DEFAULT 1 CHECK (weight > 0)` | 进度权重，正整数，默认 1 |
| estimated_minutes | `integer CHECK (estimated_minutes > 0)` | 预计耗时分钟，未填写为 NULL，填写时须大于 0 |
| start_date | `date CHECK (isfinite(start_date))` | 计划开始日期，可空 |
| due_date | `date CHECK (isfinite(due_date))` | 任务截止日期，可空 |
| status | `varchar(20) NOT NULL DEFAULT 'NOT_STARTED' CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED'))` | 任务状态：未开始、进行中、暂停、已完成 |
| sort_order | `integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0)` | 显示排序，数值越小越靠前；相同值按主键排序 |
| completed_at | `timestamptz` | 完成时间，仅 COMPLETED 状态有值 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |
| deleted_at | `timestamptz` | 逻辑删除时间，NULL 表示未删除 |

### weekly_plan

用户维度的自然周计划，可汇集不同目标和阶段的任务。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| week_start_date | `date NOT NULL CHECK (isfinite(week_start_date) AND EXTRACT(ISODOW FROM week_start_date) = 1)` | 自然周周一日期，周日由该日期加 6 天计算 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |
| deleted_at | `timestamptz` | 逻辑删除时间，NULL 表示未删除 |

### daily_plan

周计划内按需创建的日计划；日期归属与同用户关系由应用事务校验。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| weekly_plan_id | `bigint NOT NULL CHECK (weekly_plan_id > 0)` | 所属周计划逻辑标识，必须是本人未删除的周计划 |
| plan_date | `date NOT NULL CHECK (isfinite(plan_date))` | 用户本地计划日期，必须位于关联周一至周日内 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |
| deleted_at | `timestamptz` | 逻辑删除时间，NULL 表示未删除 |

### task_assignment

任务当前安排与安排历史；ended_at 为 NULL 表示当前安排，不使用逻辑删除。

| 字段 | 定义 | 含义 |
|---|---|---|
| id | `bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY` | 主键，数据库生成的 BIGINT 标识 |
| user_id | `bigint NOT NULL CHECK (user_id > 0)` | 所属用户，必须取当前认证身份 |
| task_id | `bigint NOT NULL CHECK (task_id > 0)` | 逻辑关联本人任务；历史任务标识永久保留 |
| weekly_plan_id | `bigint NOT NULL CHECK (weekly_plan_id > 0)` | 安排所属周计划，不关联单一目标 |
| daily_plan_id | `bigint CHECK (daily_plan_id > 0)` | 可空；为空表示只安排到周，非空时须属于同一周计划 |
| sort_order | `integer NOT NULL DEFAULT 0 CHECK (sort_order >= 0)` | 显示排序，数值越小越靠前；相同值按主键排序 |
| assigned_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 本次安排建立时间；不是未来的执行日期 |
| ended_at | `timestamptz` | 本次安排结束时间，NULL 表示当前安排 |
| end_reason | `varchar(30)` | 结束原因，与 ended_at 同时填写；历史安排不得覆盖改期 |
| version | `bigint NOT NULL DEFAULT 0 CHECK (version >= 0)` | 乐观锁版本，由应用更新时递增 |
| created_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 创建时间，按时间点存储 |
| updated_at | `timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP` | 更新时间，由应用更新时维护 |

## 4. 唯一性与索引

### goal

```sql
CREATE INDEX ix_goal_user_active_page ON goal (user_id, created_at DESC, id DESC) WHERE deleted_at IS NULL AND archived_at IS NULL;
CREATE INDEX ix_goal_user_state_start ON goal (user_id, status, start_date, id) WHERE deleted_at IS NULL AND archived_at IS NULL;
CREATE INDEX ix_goal_user_archive_page ON goal (user_id, archived_at DESC, id DESC) WHERE deleted_at IS NULL AND archived_at IS NOT NULL;
```

### stage

```sql
CREATE INDEX ix_stage_user_goal_order ON stage (user_id, goal_id, sort_order, id) WHERE deleted_at IS NULL;
```

### task

```sql
CREATE INDEX ix_task_user_stage_order ON task (user_id, stage_id, sort_order, id) WHERE deleted_at IS NULL;
CREATE INDEX ix_task_user_status_due ON task (user_id, status, due_date, id) WHERE deleted_at IS NULL;
```

### weekly_plan

```sql
CREATE UNIQUE INDEX uk_weekly_plan_user_week_active ON weekly_plan (user_id, week_start_date) WHERE deleted_at IS NULL;
```

### daily_plan

```sql
CREATE UNIQUE INDEX uk_daily_plan_user_date_active ON daily_plan (user_id, plan_date) WHERE deleted_at IS NULL;
CREATE INDEX ix_daily_plan_user_week_date ON daily_plan (user_id, weekly_plan_id, plan_date, id) WHERE deleted_at IS NULL;
```

### task_assignment

```sql
CREATE UNIQUE INDEX uk_task_assignment_task_current ON task_assignment (task_id) WHERE ended_at IS NULL;
CREATE INDEX ix_task_assignment_user_week_current ON task_assignment (user_id, weekly_plan_id, sort_order, id) WHERE ended_at IS NULL;
CREATE INDEX ix_task_assignment_user_day_current ON task_assignment (user_id, daily_plan_id, sort_order, id) WHERE ended_at IS NULL AND daily_plan_id IS NOT NULL;
CREATE INDEX ix_task_assignment_user_task_history ON task_assignment (user_id, task_id, assigned_at DESC, id DESC);
```

## 5. 数据库与 Service 的边界

数据库检查仅保证行内形状、数值、日期顺序、状态和唯一性；没有物理外键和触发器，直接 SQL 能写入错误跨表引用，不能宣称已经实现跨用户访问隔离。

| 规则 | SQL 层 | 后续 Service 必须完成 |
|---|---|---|
| 用户归属 | user_id 正数、用户前导索引 | 从认证身份取 user_id，所有查询与更新限定本人；管理员身份也不能越界 |
| 阶段归属 | goal_id 正数 | 目标存在、未删除、同用户；创建／修改受归档限制 |
| 任务归属 | stage_id 正数 | 阶段和目标存在、未删除、同用户，目标未归档 |
| 日计划日期 | 必填有限日期、用户日唯一 | 周计划未删除、同用户；plan_date 位于其周一至周日 |
| 安排引用 | 标识正数、结束字段成对 | 任务、阶段、目标、周计划和日计划有效且同用户；日计划所属周必须一致 |
| 当前安排 | task_id 的部分唯一索引 | 先结束旧安排，再插入新安排；同位置重复请求返回原安排，不生成虚假历史 |
| 历史不可篡改 | 不提供物理删除或级联语句 | 结束后禁止改变历史归属、时间、排序；不能将 ended_at 重新置 NULL |
| 逻辑删除 | deleted_at 区分有效记录 | 正常查询同时过滤有效祖先，删除条件也限定 owner 和 version |
| 父级删除 | 没有级联 | 目标须无有效阶段、阶段须无有效任务；计划须先显式处理有效日计划／安排；禁止隐藏地自动处理 |
| 归档策略 | archived_at；支持 GOAL_ARCHIVED 原因 | 用户明确选择保留安排只读或结束未完成安排退回任务池；不自动删除任务。恢复时不自动恢复旧安排 |
| 已完成任务 | 完成状态与时间一致 | 禁止重新安排，原安排可用于展示已完成事项；改状态与关联处理须同事务 |
| 过去日计划 | 保留日期和历史 | 普通编辑只读，明确改期操作另行校验；以用户当前生效时区求今天 |
| 乐观锁／更新时间 | version >= 0，有创建默认值 | UPDATE 附带 id、user_id、旧 version；同时 version + 1、updated_at = CURRENT_TIMESTAMP，影响 0 行为冲突或不可见 |

不把同用户、日期归属等跨表规则伪装为已由 CHECK 保证。查询参考和事务边界见《SQL查询与事务约定》。新增成长业务权限和菜单初始化待后续接口权限清单确认后再实施，本轮不改现有 Identity 授权数据。

## 6. 本轮验收范围

- 审阅四份 DDL 与六张表的数据字典、状态枚举、删除和归档决定。
- 核对用户周唯一、用户日唯一、单任务当前安排唯一，以及逻辑删除后重建、历史保留的口径。
- 核对查询中的权重进度、任务池、待处理、周内任务来源和用户条件。
- SQL 语法及关键约束在本地专用数据库的隔离 schema 内执行，事务最终回滚；结果单独记录。
- 没有修改现有开发库业务表或数据，没有编写后端／前端业务代码，没有新增单元、系统或集成测试框架。
- 跨表归属、归档、只读与并发业务链路尚未由 Service 实现，待本次 SQL 经用户验收后继续。

## 7. 已执行结果与复核方式

本地 PostgreSQL 17.11 已执行 45 项检查，45 项通过，隔离 schema 已回滚。校验库固定为 asterway_identity_test，没有改变开发库 asterway_dev 的表和数据。具体名称、执行时间、数据库版本和四份 SQL 的 SHA256 见 sql-check-results.json。

在项目根目录手工运行：

```powershell
./scripts/review_growth_sql.ps1
```

脚本只读取这四份 DDL 和 docs/growth/SQL层校验.sql，每次使用新的隔离 schema。检查语句自带目标数据库和 schema 保护，禁止直接在开发库运行。失败时 psql 关闭连接并回滚；成功也显式 ROLLBACK。临时执行文件和日志写入已忽略的私有运行目录，验收结果写入 docs/growth/sql-check-results.json。

45 项检查不代表完成 HTTP、Service 并发或权限验收。SQL 层无外键，因此两项跨表边界检查通过的含义是“确认需要 Service 校验”，不是“数据库已经拦截了错误归属”。

## 8. 验收后部署时的数据库权限

本轮不执行部署。以后若由数据库管理员建立这六张表，需给应用角色授予使用权限；原 Identity 初始化时的一次性授权不会自动覆盖未来新表。以下仅是本地角色的手工授权示例，执行前应先确认六张表已建立：

```sql
GRANT SELECT, INSERT, UPDATE ON
    goal, stage, task, weekly_plan, daily_plan, task_assignment TO asterway_dev;
GRANT USAGE, SELECT ON SEQUENCE
    goal_id_seq, stage_id_seq, task_id_seq, weekly_plan_id_seq,
    daily_plan_id_seq, task_assignment_id_seq TO asterway_dev;
REVOKE DELETE, TRUNCATE ON
    goal, stage, task, weekly_plan, daily_plan, task_assignment FROM asterway_dev;
```

业务删除通过 UPDATE 标记完成，不需要应用角色拥有物理删除权限。安排历史的不可篡改仍须由 Service 限制更新范围，不把该授权示例当作行级历史保护机制。
