/* 引入 ./http 提供的组件、状态函数或请求方法。 */
/* 封装成长业务接口请求，供页面复用同一套路径、参数和响应处理。 */
import { api, session, hasPermission } from "./http";
export const /* states保存表达式 { NOT_STARTED: "未开始", IN_PROGRESS: "进行中", PAUSED: "暂停", COMPLETED: "已完成", } 的结果。 */ states = {
  /* NOT开始时（NOT_STARTED）字段使用固定文字 '未开始'。 */
  NOT_STARTED: "未开始",
  /* IN进度（IN_PROGRESS）字段使用固定文字 '进行中'。 */
  IN_PROGRESS: "进行中",
  /* 已暂停（PAUSED）字段使用固定文字 '暂停'。 */
  PAUSED: "暂停",
  /* 已完成（COMPLETED）字段使用固定文字 '已完成'。 */
  COMPLETED: "已完成",
};
export const /* priorities保存表达式 { LOW: "低", MEDIUM: "中", HIGH: "高", URGENT: "紧急", } 的结果。 */ priorities = {
  /* LOW字段使用固定文字 '低'。 */
  LOW: "低",
  /* MEDIUM字段使用固定文字 '中'。 */
  MEDIUM: "中",
  /* HIGH字段使用固定文字 '高'。 */
  HIGH: "高",
  /* URGENT字段使用固定文字 '紧急'。 */
  URGENT: "紧急",
};
export const /* positions保存表达式 { POOL: "任务池", WEEK: "已安排到周", DAY: "已安排到日", } 的结果。 */ positions = {
  /* 连接池（POOL）字段使用固定文字 '任务池'。 */
  POOL: "任务池",
  /* 周（WEEK）字段使用固定文字 '已安排到周'。 */
  WEEK: "已安排到周",
  /* 日（DAY）字段使用固定文字 '已安排到日'。 */
  DAY: "已安排到日",
};
export const /* 是否管理员（isManager）保存沿 () => (session.user?.roles || []).some((role) => ["ADMIN", "SUPER_ADMIN"].includes(role), ) 读取数据；中间对象为空时返回空值。 */ isManager = () =>
  (session.user?.roles || []).some((role) =>
    ["ADMIN", "SUPER_ADMIN"].includes(role),
  );
export const /* 是否成长业务权限（isGrowthPermission）保存优先使用前面的有效值，没有时采用后面的备用值：(code) => /^(goal|stage|task|planning|execution|habit|notification|dashboard|statistics|agent):/.test( code ||…。 */ isGrowthPermission = (code) =>
  /^(goal|stage|task|planning|execution|habit|notification|dashboard|statistics|agent):/.test(
    code || "",
  );
export const /* 个人业务权限检查（canGrow）保存沿 (code) => !isManager() && (session.user?.roles || []).includes("USER") && hasPermission(code) 读取数据；中间对象为空时返回空值。 */ canGrow = (code) =>
  !isManager() &&
  (session.user?.roles || []).includes("USER") &&
  hasPermission(code);
export const /* resources保存表达式 { goal: "goals", stage: "stages", task: "tasks" } 的结果。 */ resources = { /* 目标（goal）字段使用固定文字 'goals'。 */ goal: "goals", /* 阶段（stage）字段使用固定文字 'stages'。 */ stage: "stages", /* 任务（task）字段使用固定文字 'tasks'。 */ task: "tasks" };
export const /* 权限（permission）保存表达式 (kind, action) => `${kind}:${kind}:${action}` 的结果。 */ permission = (kind, action) => `${kind}:${kind}:${action}`;
export /* 查询条件（query）：调用 query 完成查询条件处理。 */ function query(path, params = {}, options = {}) {
  const /* 搜索（search）保存新建 地址搜索参数集合（URLSearchParams） 实例。 */ search = new URLSearchParams();
  /* 调用 Object.entries 完成entries处理。 */
  Object.entries(params).forEach(([key, value]) => {
    /* 检查value !== "" && value !== null && value !== undefined，即对应内容是否不存在，成立时走这一分支。 */
    if (value !== "" && value !== null && value !== undefined)
      /* 调用 search.set 完成设置处理。 */
      search.set(key, String(value));
  });
  /* 返回调用 api 完成接口处理，当前函数到此结束。 */
  return api(path + "?" + search, options);
}
export const /* 读取Resource（getResource）保存按表达式计算得到的值：(kind, id) => api(`/${resources[kind]}/${id}`)。 */ getResource = (kind, id) => api(`/${resources[kind]}/${id}`);
export const /* 详情路径（detailPath）保存按表达式计算得到的值：(kind, id) => `/growth/${resources[kind]}/${id}`。 */ detailPath = (kind, id) => `/growth/${resources[kind]}/${id}`;
export /* 今天（today）：调用 today 完成今天处理。 */ function today() {
  const /* u保存优先使用前面的有效值，没有时采用后面的备用值：session.user || {}。 */ u = session.user || {};
  const /* 时区（zone）保存当前真实时间，用于记录操作发生的时刻。 */ zone =
    u.pendingTimeZone && new Date(u.timeZoneEffectiveAt).getTime() <= Date.now()
      ? u.pendingTimeZone
      : u.timeZone || "UTC";
  const /* parts保存新建 Intl中的日期时间格式（Intl.DateTimeFormat） 实例。 */ parts = new Intl.DateTimeFormat("en-US", {
    /* 用户使用的时区（timeZone）字段使用时区（zone）。 */
    timeZone: zone,
    /* 年（year）字段使用固定文字 'numeric'。 */
    year: "numeric",
    /* 月（month）字段使用固定文字 '2-digit'。 */
    month: "2-digit",
    /* 日（day）字段使用固定文字 '2-digit'。 */
    day: "2-digit",
  }).formatToParts(new Date());
  const /* 值（value）保存取得第一条符合条件的数据。 */ value = (t) => parts.find((p) => p.type === t).value;
  /* 返回表达式 `${value("year")}-${value("month")}-${value("day")}` 的结果，当前函数到此结束。 */
  return `${value("year")}-${value("month")}-${value("day")}`;
}
export /* 加入日列表（addDays）：调用 addDays 完成加入日列表处理。 */ function addDays(date, count) {
  const /* d保存新建 日期（Date） 实例。 */ d = new Date(date + "T12:00:00Z");
  /* 设置 UTC日期（UTCDate） 的值。 */
  d.setUTCDate(d.getUTCDate() + count);
  /* 返回调用 d.toISOString 完成toISO文字处理，当前函数到此结束。 */
  return d.toISOString().slice(0, 10);
}
export /* monday：调用 monday 完成monday处理。 */ function monday(date) {
  const /* 日（day）保存新建 日期（Date） 实例。 */ day = new Date(date + "T12:00:00Z").getUTCDay();
  /* 返回根据前面的条件，从两个候选结果中选择一个：addDays(date, -(day === 0 ? 6 : day - 1))，当前函数到此结束。 */
  return addDays(date, -(day === 0 ? 6 : day - 1));
}
export /* 有效日期（validDate）：调用 validDate 完成有效日期处理。 */ function validDate(value) {
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：( /^\d{4}-\d{2}-\d{2}$/.test(value || "") && !Number.isNaN(Date.parse(value + "T12:00:00Z")) && new Date(value…，当前函数到此结束。 */
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(value || "") &&
    !Number.isNaN(Date.parse(value + "T12:00:00Z")) &&
    new Date(value + "T12:00:00Z").toISOString().slice(0, 10) === value
  );
}
export /* require当前有效（requireActive）：要求满足 页面是否仍处于活动状态（Active）。 */ async function requireActive(kind, row) {
  /* 检查当前用户是否具有需要的权限：!canGrow("goal:goal:read")，成立时走这一分支。 */
  if (!canGrow("goal:goal:read")) /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */ return null;
  const /* 目标编号（goalId）保存根据前面的条件，从两个候选结果中选择一个：kind === "goal" ? row.id : kind === "stage" ? row.goalId : row.goalId || (await getResource("stage",…。 */ goalId =
    kind === "goal"
      ? row.id
      : kind === "stage"
        ? row.goalId
        : row.goalId || (await getResource("stage", row.stageId)).goalId;
  const /* 目标（goal）保存等待 读取 Resource 的值 完成后取得结果。 */ goal = await getResource("goal", goalId);
  /* 检查条件 goal.archivedAt 是否成立，成立时走这一分支。 */
  if (goal.archivedAt) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("目标已归档，请先恢复目标");
  /* 返回目标（goal），当前函数到此结束。 */
  return goal;
}
