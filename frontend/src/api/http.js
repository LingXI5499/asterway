import { reactive } from "vue";
// 只持久化令牌，权限和菜单始终由服务端重新读取。
export const session = reactive({
  token: localStorage.getItem("asterway.token") || "",
  user: null,
  permissions: [],
  menus: [],
});
let epoch = 0;
let renewedUntil = 0;
export function clearSession() {
  epoch++;
  renewedUntil = 0;
  Object.assign(session, { token: "", user: null, permissions: [], menus: [] });
  localStorage.removeItem("asterway.token");
}
export function setToken(token) {
  session.token = token;
  localStorage.setItem("asterway.token", token);
}
function sessionId(token) {
  try {
    return JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
    ).sid;
  } catch {
    return null;
  }
}
window.addEventListener("storage", (event) => {
  if (event.key === "asterway.token") {
    const changedSession =
      sessionId(event.newValue || "") !== sessionId(session.token);
    if (changedSession || !event.newValue) {
      epoch++;
      renewedUntil = 0;
      session.user = null;
    }
    session.token = event.newValue || "";
    if (!event.newValue || changedSession) {
      // 标签页之间登录身份变化时交给路由处理，避免整页刷新。
      session.permissions = [];
      session.menus = [];
      window.dispatchEvent(new Event("asterway:unauthorized"));
    }
  }
});
export class ApiError extends Error {
  constructor(message, status, code, traceId) {
    super(message);
    Object.assign(this, { status, code, traceId });
  }
}
// 普通 JSON 与 SSE 共用会话响应处理，旧请求不能恢复退出后的登录状态。
export function captureSessionRequest() {
  return { epoch, token: session.token, sid: sessionId(session.token) };
}
export function checkSessionRequest(context) {
  if (
    context.epoch !== epoch ||
    (context.token && context.sid !== sessionId(session.token))
  ) {
    throw new ApiError("登录状态已变化，请重新登录", 401, "SESSION_CHANGED");
  }
}
export function acceptSessionHeaders(context, response) {
  checkSessionRequest(context);
  const renewed = response.headers.get("X-Session-Token");
  const until = Number(response.headers.get("X-Session-Expires-At"));
  if (
    renewed &&
    session.token &&
    context.sid === sessionId(renewed) &&
    until > renewedUntil
  ) {
    renewedUntil = until;
    setToken(renewed);
  }
}
export function rejectUnauthorized(context) {
  checkSessionRequest(context);
  if (context.token) {
    clearSession();
    window.dispatchEvent(new Event("asterway:unauthorized"));
  }
}
export async function api(path, options = {}) {
  const context = captureSessionRequest();
  const token = context.token;
  const response = await fetch("/asterway" + path, {
    method: options.method || "GET",
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: options.rawBody ?? (options.body ? JSON.stringify(options.body) : undefined),
    signal: options.signal,
  });
  const result = await response.json().catch(() => ({}));
  acceptSessionHeaders(context, response);
  if (!response.ok || result.code !== "OK") {
    if (response.status === 401) rejectUnauthorized(context);
    throw new ApiError(
      result.message || `请求失败（${response.status}）`,
      response.status,
      result.code,
      result.traceId,
    );
  }
  return result.data;
}
export async function loadCurrent() {
  const currentEpoch = epoch;
  const data = await api("/me");
  if (currentEpoch !== epoch || !session.token)
    throw new ApiError("登录状态已变化，请重新登录", 401);
  Object.assign(session, {
    user: data.user,
    permissions: [...data.permissions],
    menus: data.menus,
  });
  return data;
}
export const hasPermission = (code) => session.permissions.includes(code);
// 仅用于清理不同登录会话的前端状态，不作为认证或授权依据。
export const authSessionKey = () => sessionId(session.token) || "";
export async function allPermissions() {
  const result = [];
  for (let page = 1; ; page++) {
    const data = await api(`/permissions?page=${page}&size=100`);
    result.push(...data.items);
    if (result.length >= data.total) return result;
  }
}
