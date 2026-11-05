# 核心成长业务 SQL 查询与事务约定

以下是 SQL 阶段形成的查询参考与事务约定；本轮后端已按对应说明实现，实际接口契约见《后端接口与权限清单》。查询中的 :user_id、:today、:goal_id 等是绑定参数示意，不能直接作为完整 psql 命令执行。user_id 来自当前认证身份，today 按用户当前已生效时区计算，不能由外部参数替代身份。所有名称读取和更新都应显式列出需要的字段。

## 1. 当前目标与未来目标

```sql
-- 当前目标；允许未填写开始日期。状态未开始不等于未来开始。
SELECT id, name, status, start_date, expected_end_date, version
FROM goal
WHERE user_id = :user_id AND deleted_at IS NULL AND archived_at IS NULL
  AND status <> 'COMPLETED'
  AND (start_date IS NULL OR start_date <= :today)
ORDER BY created_at DESC, id DESC;

-- 尚未到开始日期的目标，单独展示。
SELECT id, name, status, start_date, expected_end_date, version
FROM goal
WHERE user_id = :user_id AND deleted_at IS NULL AND archived_at IS NULL
  AND status <> 'COMPLETED' AND start_date > :today
ORDER BY start_date, id;
```

预计结束日期只用于规划；不会因为超过该日期而自动删除、归档或创建延期数据。所有列表使用有上限的分页。

## 2. 阶段与目标进度

```sql
-- 一个阶段的进度；有效祖先和任务必须属于同一个用户。
SELECT s.id,
       ROUND(100.0 * COALESCE(SUM(t.weight::numeric)
             FILTER (WHERE t.status = 'COMPLETED'), 0)
             / NULLIF(SUM(t.weight::numeric), 0), 2) AS progress
FROM stage s
JOIN goal g ON g.id = s.goal_id AND g.user_id = s.user_id
           AND g.deleted_at IS NULL
LEFT JOIN task t ON t.stage_id = s.id AND t.user_id = s.user_id
                 AND t.deleted_at IS NULL
WHERE s.id = :stage_id AND s.user_id = :user_id AND s.deleted_at IS NULL
GROUP BY s.id;

-- 目标内的任务统一加权；不能平均阶段百分比。
SELECT g.id,
       ROUND(100.0 * COALESCE(SUM(t.weight::numeric)
             FILTER (WHERE t.status = 'COMPLETED'), 0)
             / NULLIF(SUM(t.weight::numeric), 0), 2) AS progress
FROM goal g
LEFT JOIN stage s ON s.goal_id = g.id AND s.user_id = g.user_id
                  AND s.deleted_at IS NULL
LEFT JOIN task t ON t.stage_id = s.id AND t.user_id = g.user_id
                 AND t.deleted_at IS NULL
WHERE g.id = :goal_id AND g.user_id = :user_id AND g.deleted_at IS NULL
GROUP BY g.id;
```

权重使用 numeric 汇总，避免把整型 SUM 后再做整除。暂停任务计入分母；已删除任务与已删除阶段下的任务不参与；没有有效任务时分母为 NULL，最终进度为 NULL。归档详情仍可查看进度。

## 3. 任务池

```sql
SELECT t.id, t.name, t.status, t.priority, t.weight, t.version,
       s.id AS stage_id, s.name AS stage_name, g.id AS goal_id, g.name AS goal_name
FROM task t
JOIN stage s ON s.id = t.stage_id AND s.user_id = t.user_id AND s.deleted_at IS NULL
JOIN goal g ON g.id = s.goal_id AND g.user_id = t.user_id
           AND g.deleted_at IS NULL AND g.archived_at IS NULL
WHERE t.user_id = :user_id AND t.deleted_at IS NULL AND t.status <> 'COMPLETED'
  AND NOT EXISTS (
      SELECT 1 FROM task_assignment a
      WHERE a.task_id = t.id AND a.user_id = t.user_id AND a.ended_at IS NULL
  )
ORDER BY t.sort_order, t.id;
```

默认可安排任务池只展示未完成任务。若查询“所有未安排任务”，可另按显式筛选展示完成任务，但完成任务仍不能再次安排。是否达到任务开始日期不改变业务归属。

## 4. 周计划与日计划任务

```sql
SELECT a.id AS assignment_id, a.version AS assignment_version,
       t.id AS task_id, t.name AS task_name, t.status, t.priority, t.weight,
       s.id AS stage_id, s.name AS stage_name, g.id AS goal_id, g.name AS goal_name,
       a.daily_plan_id, d.plan_date, a.sort_order
FROM task_assignment a
JOIN task t ON t.id = a.task_id AND t.user_id = a.user_id AND t.deleted_at IS NULL
JOIN stage s ON s.id = t.stage_id AND s.user_id = a.user_id AND s.deleted_at IS NULL
JOIN goal g ON g.id = s.goal_id AND g.user_id = a.user_id AND g.deleted_at IS NULL
JOIN weekly_plan w ON w.id = a.weekly_plan_id AND w.user_id = a.user_id
                  AND w.deleted_at IS NULL
LEFT JOIN daily_plan d ON d.id = a.daily_plan_id AND d.user_id = a.user_id
                      AND d.weekly_plan_id = w.id AND d.deleted_at IS NULL
WHERE a.user_id = :user_id AND a.weekly_plan_id = :weekly_plan_id
  AND a.ended_at IS NULL
  AND (a.daily_plan_id IS NULL OR d.id IS NOT NULL)
ORDER BY a.sort_order, a.id;
```

日计划任务在同一口径上增加 a.daily_plan_id = :daily_plan_id。原任务不复制，显示来源目标与阶段。已完成任务在这里保留显示；归档目标的保留安排只读。历史安排使用明确的历史查询，不要求原目标、任务和计划仍未删除，但所有关联都仍限定同用户，不能混用“当前安排”和“所有历史记录”。

## 5. 待处理查询

```sql
SELECT t.id, t.name, t.status, a.id AS assignment_id,
       a.version AS assignment_version, w.id AS weekly_plan_id,
       w.week_start_date, d.id AS daily_plan_id, d.plan_date,
       g.id AS goal_id, g.name AS goal_name, s.id AS stage_id, s.name AS stage_name
FROM task_assignment a
JOIN task t ON t.id = a.task_id AND t.user_id = a.user_id AND t.deleted_at IS NULL
JOIN stage s ON s.id = t.stage_id AND s.user_id = a.user_id AND s.deleted_at IS NULL
JOIN goal g ON g.id = s.goal_id AND g.user_id = a.user_id
           AND g.deleted_at IS NULL AND g.archived_at IS NULL
JOIN weekly_plan w ON w.id = a.weekly_plan_id AND w.user_id = a.user_id
                  AND w.deleted_at IS NULL
LEFT JOIN daily_plan d ON d.id = a.daily_plan_id AND d.user_id = a.user_id
                      AND d.weekly_plan_id = w.id AND d.deleted_at IS NULL
WHERE a.user_id = :user_id AND a.ended_at IS NULL AND t.status <> 'COMPLETED'
  AND (a.daily_plan_id IS NULL OR d.id IS NOT NULL)
  AND (d.plan_date < :today OR w.week_start_date + 6 < :today)
ORDER BY COALESCE(d.plan_date, w.week_start_date + 6), a.id;
```

周日当天未结束，该周“未指定日期”的任务不会提前算作过周；周一才进入已结束周汇总。已过期日计划中的未完成任务可以在同周提前进入待处理。只读查询不自动创建日计划、不更改安排、不复制到下一天或下一周。

## 6. 写事务、锁与冲突

MVP 推荐对同一用户的成长业务写操作使用统一用户行锁，避免归档／删除父对象与并发新增子对象之间发生检查后失效；不同用户不相互串行。锁仅在短事务内持有，不在事务内执行网络调用：

```sql
BEGIN;
SELECT id FROM identity_user
WHERE id = :user_id AND is_enabled AND deleted_at IS NULL
FOR UPDATE;
-- 确认身份有效；然后按本人条件读取并校验所有关联。
-- 以一致顺序处理对象：目标 → 阶段 → 任务 → 周计划 → 日计划 → 当前安排。
-- 同类多行按 id 升序。所有成长写入口必须遵守同一约定。
-- 更新旧 version 时影响 0 行应明确报告不可见或冲突，不能继续后续写入。
COMMIT;
```

GrowthContextUtils.write 现已接入所有成长写事务，持有当前用户锁；本轮未执行并发业务验收。统一锁使相同用户的父级删除、归档、子级新增、状态修改与改期在未来应用事务内串行；唯一索引作为数据库最后防线。直接 SQL 绕过约定仍可写入错误关系。

### 6.1 创建或获取周／日计划

在上述用户锁内先按 user_id、日期及 deleted_at IS NULL 查询；不存在时插入，返回同一份有效计划。数据库部分唯一索引防止重复。日计划创建前校验本人周计划未删除及日期在该周范围。周计划和日计划的历史记录不能作为新任务安排的目标。

### 6.2 改期或进入日计划

1. 锁定本人用户，读取本人任务、有效阶段和目标，拒绝归档目标、已完成任务、已删除对象。
2. 校验目标周计划、可选日计划与用户归属；日计划周标识和日期范围都必须一致。
3. 读取本人当前安排和 version。目标位置相同且版本正确时直接返回，不插入历史。
4. 改期时更新旧记录 ended_at、end_reason = RESCHEDULED、version + 1、updated_at。
5. 插入新安排，task_id 不变；任一步失败整体回滚。必须先结束旧记录，后插入新记录。
6. 若无当前安排，需确认客户端预期也是“尚未安排”；并发 stale assignment_id/version 不得静默覆盖新安排。

### 6.3 返回任务池、删除与归档

- 返回任务池：只结束当前安排，原因 RETURNED_TO_POOL。
- 删除任务：结束当前安排（TASK_DELETED），再更新任务 deleted_at 和版本，不物理删除历史。
- 删除阶段：必须没有有效任务；删除目标：必须没有有效阶段。拒绝自动级联。
- 删除日计划：用户先明确选择退回周队列或任务池，结束旧安排并按选择新建周级安排，再删除日计划。
- 删除周计划：必须先处理所有当前安排与有效日计划，不级联删除任务。
- 归档目标：用户明确选择保留安排只读，或将未完成任务退回任务池；不得暗中删除或复制任务。保留已完成任务位置便于查阅；恢复目标不自动复活结束安排。
- 过去日计划不允许普通编辑；明确改期操作遵守相同所有权与并发规则。

## 7. SQL 检查的范围

隔离 schema 中检查 DDL、行内约束、唯一性、简单 CRUD、权重计算、安排历史与用户过滤查询；没有建立单元或集成测试框架。没有进行两个真实应用连接的并发业务验收、HTTP 验收或未来 Service 行为验证。没有外键与触发器时错误跨表关系可写入的结果，是需要保留并据此实现应用校验的边界证明。
