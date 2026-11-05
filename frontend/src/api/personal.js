import { api, session } from "./http";
import { query, today, canGrow } from "./growth";

const item = (value) => encodeURIComponent(String(value));
const list = (path, params, signal) => query(path, params, { signal });
const post = (path, body) => api(path, { method: "POST", body });

export const executionApi = {
  current: () => api("/executions/current"),
  list: (params, signal) => list("/executions", params, signal),
  detail: (id, signal) => api("/executions/" + item(id), { signal }),
  segments: (id, params, signal) =>
    list("/executions/" + item(id) + "/segments", params, signal),
  start: (taskId, taskVersion) => post("/executions", { taskId, taskVersion }),
  control: (action, id, version) =>
    post("/executions/" + item(id) + "/" + action, { version }),
  heartbeat: (id, segmentId) =>
    post("/executions/" + item(id) + "/heartbeat", { segmentId }),
  summary: (params, signal) => query("/executions/summary", params, { signal }),
  taskRecords: (id, params, signal) =>
    list("/tasks/" + item(id) + "/executions", params, signal),
  scopeSummary: (kind, id, params, signal) =>
    list("/" + kind + "s/" + item(id) + "/execution-summary", params, signal),
};
export const habitApi = {
  list: (params, signal) => list("/habits", params, signal),
  today: (params, signal) => list("/habits/today", params, signal),
  detail: (id) => api("/habits/" + item(id)),
  save: (id, body) =>
    api("/habits" + (id ? "/" + item(id) : ""), {
      method: id ? "PUT" : "POST",
      body,
    }),
  control: (action, row) =>
    post("/habits/" + item(row.id) + "/" + action, { version: row.version }),
  remove: (row) =>
    api("/habits/" + item(row.id) + "?version=" + row.version, {
      method: "DELETE",
    }),
  checkin: (row) =>
    post("/habits/" + item(row.id) + "/checkins", { version: row.version }),
  cancel: (habitId, row) =>
    api(
      "/habits/" +
        item(habitId) +
        "/checkins/" +
        item(row.id) +
        "?version=" +
        row.version,
      { method: "DELETE" },
    ),
  checkins: (id, params, signal) =>
    list("/habits/" + item(id) + "/checkins", params, signal),
  revisions: (id, params, signal) =>
    list("/habits/" + item(id) + "/revisions", params, signal),
  categories: (params, signal) => list("/habit-categories", params, signal),
  saveCategory: (id, body) =>
    api("/habit-categories" + (id ? "/" + item(id) : ""), {
      method: id ? "PUT" : "POST",
      body,
    }),
  removeCategory: (row) =>
    api("/habit-categories/" + item(row.id) + "?version=" + row.version, {
      method: "DELETE",
    }),
};
export const notificationApi = {
  list: (params, signal) => list("/notifications", params, signal),
  detail: (id, signal) => api("/notifications/" + item(id), { signal }),
  unread: () => api("/notifications/unread-count"),
  markRead: (row) =>
    api("/notifications/" + item(row.id) + "/read", {
      method: "PATCH",
      body: { version: row.version },
    }),
  markAllRead: () => post("/notifications/read-all"),
  handle: (action, row) =>
    post("/notifications/" + item(row.id) + "/" + action, {
      version: row.version,
      goalVersion: row.goalVersion,
    }),
};
export const dashboardApi = {
  overview: () => api("/dashboard/overview"),
  tasks: (params, signal) => list("/dashboard/tasks", params, signal),
  goals: (params, signal) => list("/dashboard/goals", params, signal),
  habits: (params, signal) => list("/dashboard/habits", params, signal),
};
export const statisticsApi = {
  summary: (params, signal) => list("/statistics/summary", params, signal),
  trends: (params, signal) => list("/statistics/trends", params, signal),
  timeline: (params, signal) => list("/statistics/timeline", params, signal),
  habits: (params, signal) => list("/statistics/habits", params, signal),
  objects: (params, signal) => list("/statistics/objects", params, signal),
};
export const stateHistory = (kind, id, params, signal) =>
  list("/" + kind + "s/" + item(id) + "/state-history", params, signal);

// 时间点按账号有效时区展示，ISO 本地日期保持原值，不按浏览器时区搬移打卡。
export function accountZone() {
  const user = session.user || {};
  return user.pendingTimeZone &&
    Date.parse(user.timeZoneEffectiveAt) <= Date.now()
    ? user.pendingTimeZone
    : user.timeZone || "UTC";
}
export function duration(value) {
  if (value === null || value === undefined || !Number.isFinite(Number(value)))
    return "—";
  const seconds = Math.max(0, Math.floor(Number(value)));
  return [
    Math.floor(seconds / 3600),
    Math.floor(seconds / 60) % 60,
    seconds % 60,
  ]
    .map((number) => String(number).padStart(2, "0"))
    .join(":");
}
export const rateText = (value) =>
  value === null || value === undefined
    ? "无应做"
    : Number(value).toFixed(1) + "%";
export const executionStates = {
  RUNNING: "运行中",
  PAUSED: "已暂停",
  FINISHED: "已结束",
};
export const habitStates = {
  ACTIVE: "进行中",
  PAUSED: "已暂停",
  ENDED: "已结束",
};
export const handleStates = {
  PENDING: "待处理",
  CONFIRMED: "已确认完成",
  CONTINUED: "继续保持",
  INVALIDATED: "已失效",
};
export const reasons = {
  USER_PAUSE: "主动暂停",
  AUTO_PAUSE: "心跳超时暂停",
  PLAN_BOUNDARY: "到达计划或时区边界",
  AUTH_REVOKED: "登录会话失效",
  FINISH: "结束执行",
  USER_FINISH: "主动结束",
  TASK_DELETED: "任务删除",
  GOAL_ARCHIVED: "目标归档",
  RESCHEDULED: "重新安排",
  RETURNED_TO_POOL: "移回任务池",
  DAILY_PLAN_DELETED: "日计划删除",
  WEEKLY_PLAN_DELETED: "周计划删除",
};
export const actions = {
  CREATED: "创建",
  EDITED: "修改资料或规则",
  PAUSED: "暂停",
  RESUMED: "恢复",
  ENDED: "结束归档",
  REOPENED: "重新开启",
  DELETED: "逻辑删除",
  SYSTEM_PAUSED: "账户状态变化后暂停",
  STATUS_CHANGED: "状态变化",
  ARCHIVED: "归档",
  RESTORED: "恢复归档",
  BASELINE: "初始化状态基线",
  COMPLETION_IMPORTED: "历史完成记录",
  CHECKIN: "习惯打卡",
  CHECKIN_CANCELLED: "撤销打卡",
  SEGMENT_STARTED: "开始连续执行",
  ASSIGNED: "加入计划",
};
export const sources = {
  USER_ACTION: "用户操作",
  NOTIFICATION: "消息确认",
  EXECUTION_START: "执行开始",
  SYSTEM_INIT: "历史初始化",
};
export const kindNames = {
  GOAL: "目标",
  STAGE: "阶段",
  TASK: "任务",
  HABIT: "习惯",
  EXECUTION: "执行",
  ASSIGNMENT: "计划安排",
};
export function cycleText(row) {
  if (row.cycleType === "DAILY") return "每天";
  return (row.weekdays || [])
    .map((day) => ["一", "二", "三", "四", "五", "六", "日"][day - 1])
    .map((day) => "周" + day)
    .join("、");
}
export function statisticsParams(form) {
  const params = {
    period: form.period || "MONTH",
    goalId: form.goalId,
    stageId: form.stageId,
    taskId: form.taskId,
  };
  if (params.period === "RANGE") {
    const [from, to] = form.dates || [];
    if (!from || !to || from > to || to > today())
      throw new Error("请选择完整且不在未来的起止日期");
    Object.assign(params, { dateFrom: from, dateTo: to });
  } else if (params.period !== "TOTAL") {
    if (form.anchorDate > today()) throw new Error("不能查询未来统计周期");
    params.anchorDate = form.anchorDate || today();
  }
  return params;
}
export const canStartTask = () =>
  canGrow("execution:session:start") && canGrow("execution:session:read");

export function instantText(value, zone = accountZone()) {
  if (!value || !Number.isFinite(Date.parse(value))) return "—";
  return new Intl.DateTimeFormat("zh-CN", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date(value));
}
export function signalChange(kind) {
  window.dispatchEvent(
    new CustomEvent("asterway:personal-change", { detail: kind }),
  );
}
