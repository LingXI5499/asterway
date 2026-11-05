# Execution SQL 查询与事务约定

本文件用于后续 Mapper/XML 实现和 SQL 验收。下列两个带 `review:` 标记的只读 PREPARE 原型由隔离复核脚本实际装载并执行；不注册数据库函数、视图或其他永久业务对象。Mapper 阶段改用绑定参数，不拼接外部输入。

## 1. 当前可执行任务

参数：`$1` 为认证用户，`$2` 为在用户锁取得后、按账号当前有效时区求得的当地日期。不能用数据库服务器 CURRENT_DATE 代替。查询不能代替普通用户身份检查。

```sql
-- review:eligible
PREPARE execution_eligible(bigint, date) AS
SELECT t.id AS task_id, t.version AS task_version, t.stage_id, s.goal_id,
       a.id AS assignment_id, a.weekly_plan_id, a.daily_plan_id,
       w.week_start_date, d.plan_date
FROM task t
JOIN stage s ON s.id = t.stage_id AND s.user_id = t.user_id
JOIN goal g ON g.id = s.goal_id AND g.user_id = t.user_id
JOIN task_assignment a ON a.task_id = t.id AND a.user_id = t.user_id AND a.ended_at IS NULL
JOIN weekly_plan w ON w.id = a.weekly_plan_id AND w.user_id = t.user_id
LEFT JOIN daily_plan d ON d.id = a.daily_plan_id
WHERE t.user_id = $1 AND t.deleted_at IS NULL AND t.status <> 'COMPLETED'
  AND s.deleted_at IS NULL AND s.status <> 'COMPLETED'
  AND g.deleted_at IS NULL AND g.archived_at IS NULL AND g.status <> 'COMPLETED'
  AND (g.start_date IS NULL OR g.start_date <= $2)
  AND (t.start_date IS NULL OR t.start_date <= $2)
  AND w.deleted_at IS NULL
  AND w.week_start_date = $2 - (EXTRACT(ISODOW FROM $2)::integer - 1)
  AND (a.daily_plan_id IS NULL OR (d.id IS NOT NULL AND d.user_id = t.user_id
      AND d.weekly_plan_id = w.id AND d.deleted_at IS NULL AND d.plan_date = $2));
```

实际开始／恢复再限定 taskId，不将候选列表当作后续写操作的授权凭证。目标和任务未设开始日期代表不设下限；阶段日期不参与硬性限制。根据查询结果用日历日期和 ZoneId 求 `eligible_until`：日安排取 planDate+1 当地零点，周安排取 weekStart+7 当地零点，再与待生效时区时间取较早值。

## 2. 耗时和日期汇总

参数：`$1` 用户、`$2` 本次捕获的服务端当前时间、`$3` 查询起点、`$4` 查询终点。后两者是有效用户时区下的日历边界，必须满足 `$3 < $4`。

```sql
-- review:totals
PREPARE execution_totals(bigint, timestamptz, timestamptz, timestamptz) AS
WITH scoped_session AS (
    SELECT id, status, last_heartbeat_at FROM execution_session WHERE user_id = $1
), effective_segment AS (
    SELECT e.id AS execution_id, s.started_at,
           COALESCE(s.ended_at, GREATEST(s.started_at, LEAST($2,
               e.last_heartbeat_at + make_interval(secs => s.heartbeat_timeout_seconds),
               s.eligible_until))) AS effective_end
    FROM scoped_session e JOIN execution_segment s ON s.execution_id = e.id
), per_session AS (
    SELECT e.id, e.status,
           COALESCE(SUM(EXTRACT(EPOCH FROM s.effective_end - s.started_at)), 0) AS total_seconds,
           COALESCE(SUM(GREATEST(EXTRACT(EPOCH FROM
               LEAST(s.effective_end, $4) - GREATEST(s.started_at, $3)), 0))
               FILTER (WHERE s.execution_id IS NOT NULL), 0) AS range_seconds,
           COALESCE(bool_or(s.started_at < $4 AND s.effective_end > $3), false) AS in_range,
           MAX(s.started_at) AS latest_execution_at
    FROM scoped_session e LEFT JOIN effective_segment s ON s.execution_id = e.id
    GROUP BY e.id, e.status
)
SELECT COALESCE(floor(SUM(total_seconds)), 0)::bigint AS total_actual_seconds,
       COALESCE(floor(SUM(range_seconds)), 0)::bigint AS range_actual_seconds,
       COUNT(*) AS total_sessions,
       COUNT(*) FILTER (WHERE in_range) AS range_sessions,
       COUNT(*) FILTER (WHERE status = 'FINISHED') AS finished_sessions,
       COUNT(*) FILTER (WHERE status = 'RUNNING') AS running_sessions,
       COUNT(*) FILTER (WHERE status = 'PAUSED') AS paused_sessions,
       MAX(latest_execution_at) AS latest_execution_at
FROM per_session;
```

每个会话先汇总实际时间段，计数不随段数膨胀；秒数保留小数参与累计，最终统一取整。关闭段读取落库时间，开放段按心跳／资格截止封顶。LEFT JOIN 后没有时间段的异常会话按零时长处理，避免 LEAST/GREATEST 忽略 NULL 时误算整天。`GREATEST(started_at, ...)` 只防御异常负时长，不代表允许创建未来执行记录。

任务／阶段／目标汇总在 scoped_session 中加入 owner 一致的任务和父链关联及指定标识条件；历史查询不使用 deleted_at、archived_at、status 的当前有效过滤。任务的阶段和目标关系沿用现有不可变归属；不保存重复的目标、阶段名称或整份任务快照。

列表日期筛选使用有效段与区间交集的 EXISTS，再分页会话。详情和时间段查询先限定父会话 owner，不能只按段 ID 查询。列表和单项汇总分别按自身范围累计后取整，不能累加已经取整的列表展示秒数代替精确汇总。

## 3. 写事务和锁顺序

- 新建、恢复、暂停、结束、心跳及跨模块联动，统一先锁目标用户的 identity_user 行，再读取／修改会话和时间段。
- 获取用户锁后捕获服务端时间；不要使用长时间等待锁之前的事务开始时间作为操作时间。
- 开始／恢复同步更新任务状态及版本、会话状态及版本、段和控制 SID，全部提交或全部回滚。段序号在用户锁内按会话下一序号分配。
- 心跳匹配运行会话、owner、SID、当前开放段和未超时／未越界条件；更新会话最近心跳，不改控制版本。过期先关闭当前段，不先续心跳。
- 状态控制更新附带 owner、id、旧 version；影响零行时区分不可见与冲突。已到目标状态的暂停／结束返回现状，不重写时间。
- 命令前的到期归一化独立短事务提交；父业务已经持有用户锁时，执行联动加入现有事务，不再开启等待同一锁的新事务。
- 扫描先无锁读取候选，再逐用户用 FOR UPDATE SKIP LOCKED 加锁并重读；不先锁会话再反向等待用户行。
- RUNNING 的执行对应时间段必须存在且开放；PAUSED/FINISHED 无开放段。跨表状态、终态不可恢复、段不重叠和 owner 一致均由 Service 保证。

心跳和 current 自动轮询不续登录 TTL。身份撤销的数据库联动在提交前完成；Redis 登录会话撤销沿用提交后处理。任务删除／目标归档关闭所有相关未结束会话，包括暂停会话，并保留执行、安排及任务的历史引用。

## 4. 旧目标归档安排预览

本阶段未连接开发库，**没有查询或修正开发库现有记录**。以下语句供 SQL 验收后手工预览；不能把隔离校验样例结果当作真实修正清单。

先检查有效安排是否存在悬空或跨用户父链；发现异常先处理，不通过缺少 owner 条件来扩大修正范围：

```sql
SELECT a.id AS assignment_id, a.user_id, a.task_id, t.stage_id, s.goal_id
FROM task_assignment a
LEFT JOIN task t ON t.id = a.task_id
LEFT JOIN stage s ON s.id = t.stage_id
LEFT JOIN goal g ON g.id = s.goal_id
WHERE a.ended_at IS NULL AND (t.id IS NULL OR s.id IS NULL OR g.id IS NULL
    OR a.user_id IS DISTINCT FROM t.user_id
    OR a.user_id IS DISTINCT FROM s.user_id
    OR a.user_id IS DISTINCT FROM g.user_id);
```

列出已归档目标仍有效的安排，包含已完成任务和已逻辑删除子项，不过滤掉需要保留的历史引用：

```sql
SELECT a.id AS assignment_id, a.user_id, g.id AS goal_id, g.archived_at,
       t.id AS task_id, t.status AS task_status, a.assigned_at,
       w.week_start_date, d.plan_date,
       EXISTS (SELECT 1 FROM execution_session e WHERE e.user_id = a.user_id
           AND e.task_id = t.id AND e.status IN ('RUNNING', 'PAUSED')) AS has_unfinished_execution
FROM task_assignment a
JOIN task t ON t.id = a.task_id AND t.user_id = a.user_id
JOIN stage s ON s.id = t.stage_id AND s.user_id = a.user_id
JOIN goal g ON g.id = s.goal_id AND g.user_id = a.user_id
LEFT JOIN weekly_plan w ON w.id = a.weekly_plan_id AND w.user_id = a.user_id
LEFT JOIN daily_plan d ON d.id = a.daily_plan_id AND d.user_id = a.user_id
WHERE a.ended_at IS NULL AND g.archived_at IS NOT NULL
ORDER BY a.user_id, g.id, a.id;
```

后端和目标归档新规则完成后，针对已审阅清单手工处理：

1. 开启事务，按 user_id 升序锁定涉及的用户行，再重读同一范围。
2. 任一悬空／跨用户引用或相关未结束执行存在时停止直接修正，改由已实现的目标归档联动处理，不能只改安排留下执行记录。
3. 使用取得锁后的一个服务端时间，把仍满足“目标已归档且安排未结束”的记录结束为 GOAL_ARCHIVED，version+1 并维护 updated_at；保留原 task_id、weekly_plan_id、daily_plan_id、assigned_at 和所有历史记录。
4. 先展示 RETURNING 清单并回滚试跑；审阅确认后再提交正式处理。恢复目标时不重开这些安排。

本轮不提供自动执行的数据清理入口，不删除目标的 deleted_at 列，不回写旧任务状态或重建过去计划。

## 5. 后续手工部署权限

两张表验收并由数据库管理员手工创建后，应用角色需要以下权限。这里仅列出权限约定，本轮未执行：

```sql
GRANT SELECT, INSERT, UPDATE ON execution_session, execution_segment TO asterway_dev;
GRANT USAGE, SELECT ON SEQUENCE execution_session_id_seq, execution_segment_id_seq TO asterway_dev;
REVOKE DELETE, TRUNCATE ON execution_session, execution_segment FROM asterway_dev;
```

应用角色不可作为表所有者来依赖 REVOKE 阻止物理删除；权限约定也不等同于字段级或行级历史不可篡改机制。历史保护仍由 Service 的可执行更新范围保证。
