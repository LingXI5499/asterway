/* 引入 ./http 提供的组件、状态函数或请求方法。 */
/* 封装个人接口请求，供页面复用同一套路径、参数和响应处理。 */
import { api, session } from "./http";
/* 引入 ./growth 提供的组件、状态函数或请求方法。 */
import { query, today, canGrow } from "./growth";

const /* 单条数据（item）保存表达式 (value) => encodeURIComponent(String(value)) 的结果。 */ item = (value) => encodeURIComponent(String(value));
const /* 列表（list）保存表达式 (path, params, signal) => query(path, params, { signal }) 的结果。 */ list = (path, params, signal) => query(path, params, { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal });
const /* post保存表达式 (path, body) => api(path, { method: "POST", body }) 的结果。 */ post = (path, body) => api(path, { /* 方法（method）字段使用固定状态或编码 POST（POST）。 */ method: "POST", /* 直接使用已有的请求正文（body）作为同名字段。 */ body });

export const /* 执行计时接口（executionApi）保存按表达式计算得到的值：{ current: () => api("/executions/current"), list: (params, signal) => list("/executions", params, signal), detail: (id,…。 */ executionApi = {
  /* 当前（current）字段使用按表达式计算得到的值：() => api("/executions/current")。 */
  current: () => api("/executions/current"),
  /* 列表（list）字段使用按表达式计算得到的值：(params, signal) => list("/executions", params, signal)。 */
  list: (params, signal) => list("/executions", params, signal),
  /* 详情（detail）字段使用按表达式计算得到的值：(id, signal) => api("/executions/" + item(id), { signal })。 */
  detail: (id, signal) => api("/executions/" + item(id), { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  /* 计时段列表（segments）字段使用按表达式计算得到的值：(id, params, signal) => list("/executions/" + item(id) + "/segments", params, signal)。 */
  segments: (id, params, signal) =>
    list("/executions/" + item(id) + "/segments", params, signal),
  /* 开始（start）字段使用按表达式计算得到的值：(taskId, taskVersion) => post("/executions", { taskId, taskVersion })。 */
  start: (taskId, taskVersion) => post("/executions", { /* 直接使用已有的任务编号（taskId）作为同名字段。 */ taskId, /* 直接使用已有的任务版本号（taskVersion）作为同名字段。 */ taskVersion }),
  /* control字段使用按表达式计算得到的值：(action, id, version) => post("/executions/" + item(id) + "/" + action, { version })。 */
  control: (action, id, version) =>
    post("/executions/" + item(id) + "/" + action, { /* 直接使用已有的版本号（version）作为同名字段。 */ version }),
  /* 心跳（heartbeat）字段使用按表达式计算得到的值：(id, segmentId) => post("/executions/" + item(id) + "/heartbeat", { segmentId })。 */
  heartbeat: (id, segmentId) =>
    post("/executions/" + item(id) + "/heartbeat", { /* 直接使用已有的连续计时段编号（segmentId）作为同名字段。 */ segmentId }),
  /* 汇总（summary）字段使用按表达式计算得到的值：(params, signal) => query("/executions/summary", params, { signal })。 */
  summary: (params, signal) => query("/executions/summary", params, { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  /* 任务记录列表（taskRecords）字段使用按表达式计算得到的值：(id, params, signal) => list("/tasks/" + item(id) + "/executions", params, signal)。 */
  taskRecords: (id, params, signal) =>
    list("/tasks/" + item(id) + "/executions", params, signal),
  /* 数据范围汇总（scopeSummary）字段使用按表达式计算得到的值：(kind, id, params, signal) => list("/" + kind + "s/" + item(id) + "/execution-summary", params, signal)。 */
  scopeSummary: (kind, id, params, signal) =>
    list("/" + kind + "s/" + item(id) + "/execution-summary", params, signal),
};
export const /* 习惯接口（habitApi）保存按表达式计算得到的值：{ list: (params, signal) => list("/habits", params, signal), today: (params, signal) => list("/habits/today", params, si…。 */ habitApi = {
  /* 列表（list）字段使用按表达式计算得到的值：(params, signal) => list("/habits", params, signal)。 */
  list: (params, signal) => list("/habits", params, signal),
  /* 今天（today）字段使用按表达式计算得到的值：(params, signal) => list("/habits/today", params, signal)。 */
  today: (params, signal) => list("/habits/today", params, signal),
  /* 详情（detail）字段使用按表达式计算得到的值：(id) => api("/habits/" + item(id))。 */
  detail: (id) => api("/habits/" + item(id)),
  /* 保存（save）字段使用根据前面的条件，从两个候选结果中选择一个：(id, body) => api("/habits" + (id ? "/" + item(id) : ""), { method: id ? "PUT" : "POST", body, })。 */
  save: (id, body) =>
    api("/habits" + (id ? "/" + item(id) : ""), {
      /* 方法（method）字段使用根据前面的条件，从两个候选结果中选择一个：id ? "PUT" : "POST"。 */
      method: id ? "PUT" : "POST",
      /* 直接使用已有的请求正文（body）作为同名字段。 */
      body,
    }),
  /* control字段使用按表达式计算得到的值：(action, row) => post("/habits/" + item(row.id) + "/" + action, { version: row.version })。 */
  control: (action, row) =>
    post("/habits/" + item(row.id) + "/" + action, { /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version }),
  /* 移除（remove）字段使用按表达式计算得到的值：(row) => api("/habits/" + item(row.id) + "?version=" + row.version, { method: "DELETE", })。 */
  remove: (row) =>
    api("/habits/" + item(row.id) + "?version=" + row.version, {
      /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */
      method: "DELETE",
    }),
  /* 打卡（checkin）字段使用按表达式计算得到的值：(row) => post("/habits/" + item(row.id) + "/checkins", { version: row.version })。 */
  checkin: (row) =>
    post("/habits/" + item(row.id) + "/checkins", { /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version }),
  /* 取消（cancel）字段使用按表达式计算得到的值：(habitId, row) => api( "/habits/" + item(habitId) + "/checkins/" + item(row.id) + "?version=" + row.version, { method: "…。 */
  cancel: (habitId, row) =>
    api(
      "/habits/" +
        item(habitId) +
        "/checkins/" +
        item(row.id) +
        "?version=" +
        row.version,
      { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" },
    ),
  /* 打卡记录（checkins）字段使用按表达式计算得到的值：(id, params, signal) => list("/habits/" + item(id) + "/checkins", params, signal)。 */
  checkins: (id, params, signal) =>
    list("/habits/" + item(id) + "/checkins", params, signal),
  /* 修订记录（revisions）字段使用按表达式计算得到的值：(id, params, signal) => list("/habits/" + item(id) + "/revisions", params, signal)。 */
  revisions: (id, params, signal) =>
    list("/habits/" + item(id) + "/revisions", params, signal),
  /* 分类列表（categories）字段使用按表达式计算得到的值：(params, signal) => list("/habit-categories", params, signal)。 */
  categories: (params, signal) => list("/habit-categories", params, signal),
  /* 保存分类（saveCategory）字段使用根据前面的条件，从两个候选结果中选择一个：(id, body) => api("/habit-categories" + (id ? "/" + item(id) : ""), { method: id ? "PUT" : "POST", b…。 */
  saveCategory: (id, body) =>
    api("/habit-categories" + (id ? "/" + item(id) : ""), {
      /* 方法（method）字段使用根据前面的条件，从两个候选结果中选择一个：id ? "PUT" : "POST"。 */
      method: id ? "PUT" : "POST",
      /* 直接使用已有的请求正文（body）作为同名字段。 */
      body,
    }),
  /* 移除分类（removeCategory）字段使用按表达式计算得到的值：(row) => api("/habit-categories/" + item(row.id) + "?version=" + row.version, { method: "DELETE", })。 */
  removeCategory: (row) =>
    api("/habit-categories/" + item(row.id) + "?version=" + row.version, {
      /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */
      method: "DELETE",
    }),
};
export const /* 通知接口（notificationApi）保存按表达式计算得到的值：{ list: (params, signal) => list("/notifications", params, signal), detail: (id, signal) => api("/notifications/" + item…。 */ notificationApi = {
  /* 列表（list）字段使用按表达式计算得到的值：(params, signal) => list("/notifications", params, signal)。 */
  list: (params, signal) => list("/notifications", params, signal),
  /* 详情（detail）字段使用按表达式计算得到的值：(id, signal) => api("/notifications/" + item(id), { signal })。 */
  detail: (id, signal) => api("/notifications/" + item(id), { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  /* unread字段使用按表达式计算得到的值：() => api("/notifications/unread-count")。 */
  unread: () => api("/notifications/unread-count"),
  /* mark读取（markRead）字段使用按表达式计算得到的值：(row) => api("/notifications/" + item(row.id) + "/read", { method: "PATCH", body: { version: row.version }, })。 */
  markRead: (row) =>
    api("/notifications/" + item(row.id) + "/read", {
      /* 方法（method）字段使用固定状态或编码 PATCH（PATCH）。 */
      method: "PATCH",
      /* 请求正文（body）字段使用表达式 { version: row.version } 的结果。 */
      body: { /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version },
    }),
  /* mark全部读取（markAllRead）字段使用按表达式计算得到的值：() => post("/notifications/read-all")。 */
  markAllRead: () => post("/notifications/read-all"),
  /* handle字段使用按表达式计算得到的值：(action, row) => post("/notifications/" + item(row.id) + "/" + action, { version: row.version, goalVersion: row.goalVers…。 */
  handle: (action, row) =>
    post("/notifications/" + item(row.id) + "/" + action, {
      /* 版本号（version）字段使用表达式 row.version 的结果。 */
      version: row.version,
      /* 目标版本号（goalVersion）字段使用表达式 row.goalVersion 的结果。 */
      goalVersion: row.goalVersion,
    }),
};
export const /* dashboard接口（dashboardApi）保存按表达式计算得到的值：{ overview: () => api("/dashboard/overview"), tasks: (params, signal) => list("/dashboard/tasks", params, signal), goals…。 */ dashboardApi = {
  /* overview字段使用按表达式计算得到的值：() => api("/dashboard/overview")。 */
  overview: () => api("/dashboard/overview"),
  /* 任务列表（tasks）字段使用按表达式计算得到的值：(params, signal) => list("/dashboard/tasks", params, signal)。 */
  tasks: (params, signal) => list("/dashboard/tasks", params, signal),
  /* 目标列表（goals）字段使用按表达式计算得到的值：(params, signal) => list("/dashboard/goals", params, signal)。 */
  goals: (params, signal) => list("/dashboard/goals", params, signal),
  /* 习惯列表（habits）字段使用按表达式计算得到的值：(params, signal) => list("/dashboard/habits", params, signal)。 */
  habits: (params, signal) => list("/dashboard/habits", params, signal),
};
export const /* 统计接口（statisticsApi）保存按表达式计算得到的值：{ summary: (params, signal) => list("/statistics/summary", params, signal), trends: (params, signal) => list("/statistic…。 */ statisticsApi = {
  /* 汇总（summary）字段使用按表达式计算得到的值：(params, signal) => list("/statistics/summary", params, signal)。 */
  summary: (params, signal) => list("/statistics/summary", params, signal),
  /* 趋势列表（trends）字段使用按表达式计算得到的值：(params, signal) => list("/statistics/trends", params, signal)。 */
  trends: (params, signal) => list("/statistics/trends", params, signal),
  /* 成长时间线（timeline）字段使用按表达式计算得到的值：(params, signal) => list("/statistics/timeline", params, signal)。 */
  timeline: (params, signal) => list("/statistics/timeline", params, signal),
  /* 习惯列表（habits）字段使用按表达式计算得到的值：(params, signal) => list("/statistics/habits", params, signal)。 */
  habits: (params, signal) => list("/statistics/habits", params, signal),
  /* objects字段使用按表达式计算得到的值：(params, signal) => list("/statistics/objects", params, signal)。 */
  objects: (params, signal) => list("/statistics/objects", params, signal),
};
export const /* 当前状态历史（stateHistory）保存按表达式计算得到的值：(kind, id, params, signal) => list("/" + kind + "s/" + item(id) + "/state-history", params, signal)。 */ stateHistory = (kind, id, params, signal) =>
  list("/" + kind + "s/" + item(id) + "/state-history", params, signal);

// 时间点按账号有效时区展示，ISO 本地日期保持原值，不按浏览器时区搬移打卡。
export /* 账号时区（accountZone）：调用 accountZone 完成账号时区处理。 */ function accountZone() {
  const /* 用户（user）保存优先使用前面的有效值，没有时采用后面的备用值：session.user || {}。 */ user = session.user || {};
  /* 返回当前真实时间，用于记录操作发生的时刻，当前函数到此结束。 */
  return user.pendingTimeZone &&
    Date.parse(user.timeZoneEffectiveAt) <= Date.now()
    ? user.pendingTimeZone
    : user.timeZone || "UTC";
}
export /* 持续时间（duration）：调用 duration 完成持续时间处理。 */ function duration(value) {
  /* 检查value === null || value === undefined || !Number.isFinite(Number(value))，即对应内容是否不存在，成立时走这一分支。 */
  if (value === null || value === undefined || !Number.isFinite(Number(value)))
    /* 返回固定文字 '—'，当前函数到此结束。 */
    return "—";
  const /* 秒数（seconds）保存调用 Math.max 完成最大处理。 */ seconds = Math.max(0, Math.floor(Number(value)));
  /* 返回调用 [ Math.floor 完成floor处理，当前函数到此结束。 */
  return [
    Math.floor(seconds / 3600),
    Math.floor(seconds / 60) % 60,
    seconds % 60,
  ]
    .map((number) => String(number).padStart(2, "0"))
    .join(":");
}
export const /* 频率文字（rateText）保存根据前面的条件，从两个候选结果中选择一个：(value) => value === null || value === undefined ? "无应做" : Number(value).toFixed(1) + "%"。 */ rateText = (value) =>
  value === null || value === undefined
    ? "无应做"
    : Number(value).toFixed(1) + "%";
export const /* 执行计时States（executionStates）保存表达式 { RUNNING: "运行中", PAUSED: "已暂停", FINISHED: "已结束", } 的结果。 */ executionStates = {
  /* 运行中（RUNNING）字段使用固定文字 '运行中'。 */
  RUNNING: "运行中",
  /* 已暂停（PAUSED）字段使用固定文字 '已暂停'。 */
  PAUSED: "已暂停",
  /* FINISHED字段使用固定文字 '已结束'。 */
  FINISHED: "已结束",
};
export const /* 习惯States（habitStates）保存表达式 { ACTIVE: "进行中", PAUSED: "已暂停", ENDED: "已结束", } 的结果。 */ habitStates = {
  /* 页面是否仍处于活动状态（ACTIVE）字段使用固定文字 '进行中'。 */
  ACTIVE: "进行中",
  /* 已暂停（PAUSED）字段使用固定文字 '已暂停'。 */
  PAUSED: "已暂停",
  /* 已结束（ENDED）字段使用固定文字 '已结束'。 */
  ENDED: "已结束",
};
export const /* handleStates保存表达式 { PENDING: "待处理", CONFIRMED: "已确认完成", CONTINUED: "继续保持", INVALIDATED: "已失效", } 的结果。 */ handleStates = {
  /* 等待处理（PENDING）字段使用固定文字 '待处理'。 */
  PENDING: "待处理",
  /* 已确认（CONFIRMED）字段使用固定文字 '已确认完成'。 */
  CONFIRMED: "已确认完成",
  /* CONTINUED字段使用固定文字 '继续保持'。 */
  CONTINUED: "继续保持",
  /* INVALIDATED字段使用固定文字 '已失效'。 */
  INVALIDATED: "已失效",
};
export const /* reasons保存表达式 { USER_PAUSE: "主动暂停", AUTO_PAUSE: "心跳超时暂停", PLAN_BOUNDARY: "到达计划或时区边界", AUTH_REVOKED: "登录会话失效", FINISH: "结束执行", USER_FIN… 的结果。 */ reasons = {
  /* 用户暂停（USER_PAUSE）字段使用固定文字 '主动暂停'。 */
  USER_PAUSE: "主动暂停",
  /* AUTO暂停（AUTO_PAUSE）字段使用固定文字 '心跳超时暂停'。 */
  AUTO_PAUSE: "心跳超时暂停",
  /* 方案BOUNDARY（PLAN_BOUNDARY）字段使用固定文字 '到达计划或时区边界'。 */
  PLAN_BOUNDARY: "到达计划或时区边界",
  /* 认证REVOKED（AUTH_REVOKED）字段使用固定文字 '登录会话失效'。 */
  AUTH_REVOKED: "登录会话失效",
  /* FINISH字段使用固定文字 '结束执行'。 */
  FINISH: "结束执行",
  /* 用户FINISH（USER_FINISH）字段使用固定文字 '主动结束'。 */
  USER_FINISH: "主动结束",
  /* 任务删除标记（TASK_DELETED）字段使用固定文字 '任务删除'。 */
  TASK_DELETED: "任务删除",
  /* 目标已归档（GOAL_ARCHIVED）字段使用固定文字 '目标归档'。 */
  GOAL_ARCHIVED: "目标归档",
  /* RESCHEDULED字段使用固定文字 '重新安排'。 */
  RESCHEDULED: "重新安排",
  /* 已返回TO连接池（RETURNED_TO_POOL）字段使用固定文字 '移回任务池'。 */
  RETURNED_TO_POOL: "移回任务池",
  /* 每日方案删除标记（DAILY_PLAN_DELETED）字段使用固定文字 '日计划删除'。 */
  DAILY_PLAN_DELETED: "日计划删除",
  /* 每周方案删除标记（WEEKLY_PLAN_DELETED）字段使用固定文字 '周计划删除'。 */
  WEEKLY_PLAN_DELETED: "周计划删除",
};
export const /* actions保存表达式 { CREATED: "创建", EDITED: "修改资料或规则", PAUSED: "暂停", RESUMED: "恢复", ENDED: "结束归档", REOPENED: "重新开启", DELETED: "逻辑删除", SYSTE… 的结果。 */ actions = {
  /* 创建时（CREATED）字段使用固定文字 '创建'。 */
  CREATED: "创建",
  /* EDITED字段使用固定文字 '修改资料或规则'。 */
  EDITED: "修改资料或规则",
  /* 已暂停（PAUSED）字段使用固定文字 '暂停'。 */
  PAUSED: "暂停",
  /* RESUMED字段使用固定文字 '恢复'。 */
  RESUMED: "恢复",
  /* 已结束（ENDED）字段使用固定文字 '结束归档'。 */
  ENDED: "结束归档",
  /* REOPENED字段使用固定文字 '重新开启'。 */
  REOPENED: "重新开启",
  /* 删除标记（DELETED）字段使用固定文字 '逻辑删除'。 */
  DELETED: "逻辑删除",
  /* 系统已暂停（SYSTEM_PAUSED）字段使用固定文字 '账户状态变化后暂停'。 */
  SYSTEM_PAUSED: "账户状态变化后暂停",
  /* 状态已修改（STATUS_CHANGED）字段使用固定文字 '状态变化'。 */
  STATUS_CHANGED: "状态变化",
  /* 已归档（ARCHIVED）字段使用固定文字 '归档'。 */
  ARCHIVED: "归档",
  /* RESTORED字段使用固定文字 '恢复归档'。 */
  RESTORED: "恢复归档",
  /* BASELINE字段使用固定文字 '初始化状态基线'。 */
  BASELINE: "初始化状态基线",
  /* 完成情况IMPORTED（COMPLETION_IMPORTED）字段使用固定文字 '历史完成记录'。 */
  COMPLETION_IMPORTED: "历史完成记录",
  /* 打卡（CHECKIN）字段使用固定文字 '习惯打卡'。 */
  CHECKIN: "习惯打卡",
  /* 打卡已取消（CHECKIN_CANCELLED）字段使用固定文字 '撤销打卡'。 */
  CHECKIN_CANCELLED: "撤销打卡",
  /* 连续计时段开始时（SEGMENT_STARTED）字段使用固定文字 '开始连续执行'。 */
  SEGMENT_STARTED: "开始连续执行",
  /* ASSIGNED字段使用固定文字 '加入计划'。 */
  ASSIGNED: "加入计划",
};
export const /* sources保存表达式 { USER_ACTION: "用户操作", NOTIFICATION: "消息确认", EXECUTION_START: "执行开始", SYSTEM_INIT: "历史初始化", } 的结果。 */ sources = {
  /* 用户操作（USER_ACTION）字段使用固定文字 '用户操作'。 */
  USER_ACTION: "用户操作",
  /* 通知（NOTIFICATION）字段使用固定文字 '消息确认'。 */
  NOTIFICATION: "消息确认",
  /* 执行计时开始（EXECUTION_START）字段使用固定文字 '执行开始'。 */
  EXECUTION_START: "执行开始",
  /* 系统INIT（SYSTEM_INIT）字段使用固定文字 '历史初始化'。 */
  SYSTEM_INIT: "历史初始化",
};
export const /* 种类Names（kindNames）保存表达式 { GOAL: "目标", STAGE: "阶段", TASK: "任务", HABIT: "习惯", EXECUTION: "执行", ASSIGNMENT: "计划安排", } 的结果。 */ kindNames = {
  /* 目标（GOAL）字段使用固定文字 '目标'。 */
  GOAL: "目标",
  /* 阶段（STAGE）字段使用固定文字 '阶段'。 */
  STAGE: "阶段",
  /* 任务（TASK）字段使用固定文字 '任务'。 */
  TASK: "任务",
  /* 习惯（HABIT）字段使用固定文字 '习惯'。 */
  HABIT: "习惯",
  /* 执行计时（EXECUTION）字段使用固定文字 '执行'。 */
  EXECUTION: "执行",
  /* 任务安排（ASSIGNMENT）字段使用固定文字 '计划安排'。 */
  ASSIGNMENT: "计划安排",
};
export /* cycle文字（cycleText）：调用 cycleText 完成cycle文字处理。 */ function cycleText(row) {
  /* 检查条件 row.cycleType === "DAILY" 是否成立，成立时走这一分支。 */
  if (row.cycleType === "DAILY") /* 返回固定文字 '每天'，当前函数到此结束。 */ return "每天";
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：(row.weekdays || []) .map((day) => ["一", "二", "三", "四", "五", "六", "日"][day - 1]) .map((day) => "周" + day) .joi…，当前函数到此结束。 */
  return (row.weekdays || [])
    .map((day) => ["一", "二", "三", "四", "五", "六", "日"][day - 1])
    .map((day) => "周" + day)
    .join("、");
}
export /* 统计参数集合（statisticsParams）：调用 statisticsParams 完成统计参数集合处理。 */ function statisticsParams(form) {
  const /* 参数集合（params）保存优先使用前面的有效值，没有时采用后面的备用值：{ period: form.period || "MONTH", goalId: form.goalId, stageId: form.stageId, taskId: form.taskId, }。 */ params = {
    /* 统计周期（period）字段使用优先使用前面的有效值，没有时采用后面的备用值：form.period || "MONTH"。 */
    period: form.period || "MONTH",
    /* 目标编号（goalId）字段使用表达式 form.goalId 的结果。 */
    goalId: form.goalId,
    /* 阶段编号（stageId）字段使用表达式 form.stageId 的结果。 */
    stageId: form.stageId,
    /* 任务编号（taskId）字段使用表达式 form.taskId 的结果。 */
    taskId: form.taskId,
  };
  /* 检查条件 params.period === "RANGE" 是否成立，成立时走这一分支。 */
  if (params.period === "RANGE") {
    const /* 来源to（[from, to]）保存优先使用前面的有效值，没有时采用后面的备用值：form.dates || []。 */ [from, to] = form.dates || [];
    /* 检查条件 !from || !to || from > to || to > today() 是否成立，成立时走这一分支。 */
    if (!from || !to || from > to || to > today())
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择完整且不在未来的起止日期");
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(params, { /* 日期来源（dateFrom）字段使用来源（from）。 */ dateFrom: from, /* 日期To（dateTo）字段使用to。 */ dateTo: to });
  } else /* 检查条件 params.period !== "TOTAL" 是否成立，成立时走这一分支。 */ if (params.period !== "TOTAL") {
    /* 检查条件 form.anchorDate > today() 是否成立，成立时走这一分支。 */
    if (form.anchorDate > today()) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("不能查询未来统计周期");
    /* 把优先使用前面的有效值，没有时采用后面的备用值：form.anchorDate || today()写入 params.anchorDate。 */
    params.anchorDate = form.anchorDate || today();
  }
  /* 返回参数集合（params），当前函数到此结束。 */
  return params;
}
export const /* 是否可以开始任务（canStartTask）保存条件判断的结果：() => canGrow("execution:session:start") && canGrow("execution:session:read")。 */ canStartTask = () =>
  canGrow("execution:session:start") && canGrow("execution:session:read");

export /* instant文字（instantText）：调用 instantText 完成instant文字处理。 */ function instantText(value, zone = accountZone()) {
  /* 检查条件 !value || !Number.isFinite(Date.parse(value)) 是否成立，成立时走这一分支。 */
  if (!value || !Number.isFinite(Date.parse(value))) /* 返回固定文字 '—'，当前函数到此结束。 */ return "—";
  /* 返回新建 Intl中的日期时间格式（Intl.DateTimeFormat） 实例，当前函数到此结束。 */
  return new Intl.DateTimeFormat("zh-CN", {
    /* 用户使用的时区（timeZone）字段使用时区（zone）。 */
    timeZone: zone,
    /* 年（year）字段使用固定文字 'numeric'。 */
    year: "numeric",
    /* 月（month）字段使用固定文字 '2-digit'。 */
    month: "2-digit",
    /* 日（day）字段使用固定文字 '2-digit'。 */
    day: "2-digit",
    /* 小时（hour）字段使用固定文字 '2-digit'。 */
    hour: "2-digit",
    /* 分钟（minute）字段使用固定文字 '2-digit'。 */
    minute: "2-digit",
    /* 秒（second）字段使用固定文字 '2-digit'。 */
    second: "2-digit",
    /* 小时12（hour12）字段使用假，表示条件不成立或开关关闭。 */
    hour12: false,
  }).format(new Date(value));
}
export /* 取消信号修改（signalChange）：调用 signalChange 完成取消信号修改处理。 */ function signalChange(kind) {
  /* 调用 window.dispatchEvent 完成dispatch事件处理。 */
  window.dispatchEvent(
    new CustomEvent("asterway:personal-change", { /* 详情（detail）字段使用种类（kind）。 */ detail: kind }),
  );
}
