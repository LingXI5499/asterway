import { api, session, hasPermission } from "./http";
export const states = {
  NOT_STARTED: "未开始",
  IN_PROGRESS: "进行中",
  PAUSED: "暂停",
  COMPLETED: "已完成",
};
export const priorities = {
  LOW: "低",
  MEDIUM: "中",
  HIGH: "高",
  URGENT: "紧急",
};
export const positions = {
  POOL: "任务池",
  WEEK: "已安排到周",
  DAY: "已安排到日",
};
export const isManager = () =>
  (session.user?.roles || []).some((role) =>
    ["ADMIN", "SUPER_ADMIN"].includes(role),
  );
export const isGrowthPermission = (code) =>
  /^(goal|stage|task|planning|execution|habit|notification|dashboard|statistics|agent):/.test(
    code || "",
  );
export const canGrow = (code) =>
  !isManager() &&
  (session.user?.roles || []).includes("USER") &&
  hasPermission(code);
export const resources = { goal: "goals", stage: "stages", task: "tasks" };
export const permission = (kind, action) => `${kind}:${kind}:${action}`;
export function query(path, params = {}, options = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== "" && value !== null && value !== undefined)
      search.set(key, String(value));
  });
  return api(path + "?" + search, options);
}
export const getResource = (kind, id) => api(`/${resources[kind]}/${id}`);
export const detailPath = (kind, id) => `/growth/${resources[kind]}/${id}`;
export function today() {
  const u = session.user || {};
  const zone =
    u.pendingTimeZone && new Date(u.timeZoneEffectiveAt).getTime() <= Date.now()
      ? u.pendingTimeZone
      : u.timeZone || "UTC";
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: zone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const value = (t) => parts.find((p) => p.type === t).value;
  return `${value("year")}-${value("month")}-${value("day")}`;
}
export function addDays(date, count) {
  const d = new Date(date + "T12:00:00Z");
  d.setUTCDate(d.getUTCDate() + count);
  return d.toISOString().slice(0, 10);
}
export function monday(date) {
  const day = new Date(date + "T12:00:00Z").getUTCDay();
  return addDays(date, -(day === 0 ? 6 : day - 1));
}
export function validDate(value) {
  return (
    /^\d{4}-\d{2}-\d{2}$/.test(value || "") &&
    !Number.isNaN(Date.parse(value + "T12:00:00Z")) &&
    new Date(value + "T12:00:00Z").toISOString().slice(0, 10) === value
  );
}
export async function requireActive(kind, row) {
  if (!canGrow("goal:goal:read")) return null;
  const goalId =
    kind === "goal"
      ? row.id
      : kind === "stage"
        ? row.goalId
        : row.goalId || (await getResource("stage", row.stageId)).goalId;
  const goal = await getResource("goal", goalId);
  if (goal.archivedAt) throw new Error("目标已归档，请先恢复目标");
  return goal;
}
