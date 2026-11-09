// 计划页共用的日期、旧地址兼容和安排操作；浏览日期只使用读取接口。
import { api } from "./http";
import { addDays, monday, validDate, canGrow } from "./growth";

export const planningPath = "/planning/plans";
export const planningPermissions = [
  "planning:weekly:read", "planning:daily:read", "task:task:list",
  "task:task:read", "planning:pending:list",
];
export const weekLabels = ["周一", "周二", "周三", "周四", "周五", "周六", "周日"];

// 旧入口继续可用，日期和其他查询条件一起带入新页面。
export function planningRedirect(source, route) {
  const query = { ...route.query };
  if (!validDate(query.date) && validDate(query.week)) query.date = query.week;
  if (source === "/growth/tasks") query.manage = "all";
  if (source === "/planning/pending-tasks") query.manage = "overdue";
  if (source === "/growth/tasks/:id") query.task = route.params.id;
  return { path: planningPath, query, hash: route.hash };
}

// 使用中午的 UTC 日期计算日历，避免夏令时或本机时区让日期偏移。
export function monthDates(month) {
  const first = month.slice(0, 7) + "-01";
  const start = monday(first);
  return Array.from({ length: 42 }, (_, i) => addDays(start, i));
}

export function shiftMonth(date, offset) {
  const value = new Date(date.slice(0, 7) + "-01T12:00:00Z");
  value.setUTCMonth(value.getUTCMonth() + offset);
  return value.toISOString().slice(0, 10);
}

// 过去的安排允许查看和重新安排，原日期上的编辑及执行仍按只读处理。
export function pastAssignment(row, currentDate) {
  return row.planDate ? row.planDate < currentDate
    : !!row.weekStartDate && addDays(row.weekStartDate, 6) < currentDate;
}

// 只在确认保存时调用。已有周和日期会复用，空白日期不会因浏览而写入。
export async function ensurePlan({ weekStart, planDate = "" }) {
  if (!validDate(weekStart) || monday(weekStart) !== weekStart ||
      (planDate && (!validDate(planDate) || monday(planDate) !== weekStart))) {
    throw new Error("请选择同一周内的有效日期");
  }
  let week = await api(`/weekly-plans/by-week/${weekStart}`);
  if (!week) {
    if (!canGrow("planning:weekly:manage")) throw new Error("当前账号没有新建本周安排的权限");
    week = await api(`/weekly-plans/by-week/${weekStart}`, { method: "PUT" });
  }
  let day = null;
  if (planDate) {
    day = await api(`/weekly-plans/${week.id}/daily-plans/${planDate}`);
    if (!day) {
      if (!canGrow("planning:daily:manage")) throw new Error("当前账号没有新建当天安排的权限");
      day = await api(`/weekly-plans/${week.id}/daily-plans/${planDate}`, { method: "PUT" });
    }
  }
  return { week, day };
}

export async function assignNewTask(taskId, planDate) {
  const { week, day } = await ensurePlan({ weekStart: monday(planDate), planDate });
  return api(`/tasks/${taskId}/assignment`, {
    method: "PUT",
    body: { weeklyPlanId: week.id, dailyPlanId: day.id, assignmentId: null, version: null, sortOrder: 0 },
  });
}
