/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 封装HTTP请求接口请求，供页面复用同一套路径、参数和响应处理。 */
import { reactive } from "vue";
// 只持久化令牌，权限和菜单始终由服务端重新读取。
export const /* 当前会话（session）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ session = reactive({
  /* 身份凭据（token）字段使用优先使用前面的有效值，没有时采用后面的备用值：localStorage.getItem("asterway.token") || ""。 */
  token: localStorage.getItem("asterway.token") || "",
  /* 用户（user）字段使用空值，表示当前还没有对应内容。 */
  user: null,
  /* 权限列表（permissions）字段使用一个空列表，用来保存后续多条数据。 */
  permissions: [],
  /* 菜单列表（menus）字段使用一个空列表，用来保存后续多条数据。 */
  menus: [],
});
let /* epoch保存固定数值 0。 */ epoch = 0;
let /* renewedUntil保存固定数值 0。 */ renewedUntil = 0;
export /* 清除会话（clearSession）：调用 clearSession 完成清除会话处理。 */ function clearSession() {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  epoch++;
  /* 把固定数值 0写入 renewedUntil。 */
  renewedUntil = 0;
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(session, { /* 身份凭据（token）字段使用固定文字 ''。 */ token: "", /* 用户（user）字段使用空值，表示当前还没有对应内容。 */ user: null, /* 权限列表（permissions）字段使用一个空列表，用来保存后续多条数据。 */ permissions: [], /* 菜单列表（menus）字段使用一个空列表，用来保存后续多条数据。 */ menus: [] });
  /* 移除 单条数据（Item）。 */
  localStorage.removeItem("asterway.token");
}
export /* 设置身份凭据（setToken）：设置 身份凭据（Token） 的值。 */ function setToken(token) {
  /* 把身份凭据（token）写入 session.token。 */
  session.token = token;
  /* 设置 单条数据（Item） 的值。 */
  localStorage.setItem("asterway.token", token);
}
/* 会话编号（sessionId）：调用 sessionId 完成会话编号处理。 */
function sessionId(token) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 返回调用 JSON.parse 完成解析处理，当前函数到此结束。 */
    return JSON.parse(
      atob(token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/")),
    ).sid;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch {
    /* 返回空值，表示当前还没有对应内容，当前函数到此结束。 */
    return null;
  }
}
/* 调用 window.addEventListener 完成加入事件Listener处理。 */
window.addEventListener("storage", (event) => {
  /* 检查条件 event.key === "asterway.token" 是否成立，成立时走这一分支。 */
  if (event.key === "asterway.token") {
    const /* 已修改会话（changedSession）保存优先使用前面的有效值，没有时采用后面的备用值：sessionId(event.newValue || "") !== sessionId(session.token)。 */ changedSession =
      sessionId(event.newValue || "") !== sessionId(session.token);
    /* 检查条件 changedSession || !event.newValue 是否成立，成立时走这一分支。 */
    if (changedSession || !event.newValue) {
      /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
      epoch++;
      /* 把固定数值 0写入 renewedUntil。 */
      renewedUntil = 0;
      /* 把空值，表示当前还没有对应内容写入 session.user。 */
      session.user = null;
    }
    /* 把优先使用前面的有效值，没有时采用后面的备用值：event.newValue || ""写入 session.token。 */
    session.token = event.newValue || "";
    /* 检查条件 !event.newValue || changedSession 是否成立，成立时走这一分支。 */
    if (!event.newValue || changedSession) {
      // 标签页之间登录身份变化时交给路由处理，避免整页刷新。
      /* 把一个空列表，用来保存后续多条数据写入 session.permissions。 */
      session.permissions = [];
      /* 把一个空列表，用来保存后续多条数据写入 session.menus。 */
      session.menus = [];
      /* 调用 window.dispatchEvent 完成dispatch事件处理。 */
      window.dispatchEvent(new Event("asterway:unauthorized"));
    }
  }
});
export /* 定义 ApiError，把相关状态与方法放在同一个对象中。 */ class ApiError extends Error {
  /* constructor：调用 constructor 完成constructor处理。 */
  constructor(message, status, code, traceId) {
    /* 调用 super 完成super处理。 */
    super(message);
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(this, { /* 直接使用已有的状态（status）作为同名字段。 */ status, /* 直接使用已有的编码（code）作为同名字段。 */ code, /* 直接使用已有的本次请求的追踪编号（traceId）作为同名字段。 */ traceId });
  }
}
// 普通 JSON 与 SSE 共用会话响应处理，旧请求不能恢复退出后的登录状态。
export /* capture会话请求（captureSessionRequest）：调用 captureSessionRequest 完成capture会话请求处理。 */ function captureSessionRequest() {
  /* 返回表达式 { epoch, token: session.token, sid: sessionId(session.token) } 的结果，当前函数到此结束。 */
  return { /* 直接使用已有的epoch作为同名字段。 */ epoch, /* 身份凭据（token）字段使用表达式 session.token 的结果。 */ token: session.token, /* 本次登录会话编号（sid）字段使用调用 sessionId 完成会话编号处理。 */ sid: sessionId(session.token) };
}
export /* 核对会话请求（checkSessionRequest）：核对 会话请求（SessionRequest）。 */ function checkSessionRequest(context) {
  /* 检查条件 context.epoch !== epoch || (context.token && context.sid !== sessionId(session.token)) 是否成立，成立时走这一分支。 */
  if (
    context.epoch !== epoch ||
    (context.token && context.sid !== sessionId(session.token))
  ) {
    /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
    throw new ApiError("登录状态已变化，请重新登录", 401, "SESSION_CHANGED");
  }
}
export /* 接受会话请求头集合（acceptSessionHeaders）：调用 acceptSessionHeaders 完成接受会话请求头集合处理。 */ function acceptSessionHeaders(context, response) {
  /* 核对 会话请求（SessionRequest）。 */
  checkSessionRequest(context);
  const /* renewed保存按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值。 */ renewed = response.headers.get("X-Session-Token");
  const /* until保存把输入转换成数值，供比较、计算或按编号查询。 */ until = Number(response.headers.get("X-Session-Expires-At"));
  /* 检查条件 renewed && session.token && context.sid === sessionId(renewed) && until > renewedUntil 是否成立，成立时走这一分支。 */
  if (
    renewed &&
    session.token &&
    context.sid === sessionId(renewed) &&
    until > renewedUntil
  ) {
    /* 把until写入 renewedUntil。 */
    renewedUntil = until;
    /* 设置 身份凭据（Token） 的值。 */
    setToken(renewed);
  }
}
export /* 拒绝Unauthorized（rejectUnauthorized）：调用 rejectUnauthorized 完成拒绝Unauthorized处理。 */ function rejectUnauthorized(context) {
  /* 核对 会话请求（SessionRequest）。 */
  checkSessionRequest(context);
  /* 检查条件 context.token 是否成立，成立时走这一分支。 */
  if (context.token) {
    /* 调用 clearSession 完成清除会话处理。 */
    clearSession();
    /* 调用 window.dispatchEvent 完成dispatch事件处理。 */
    window.dispatchEvent(new Event("asterway:unauthorized"));
  }
}
export /* 接口（api）：调用 api 完成接口处理。 */ async function api(path, options = {}) {
  const /* 上下文（context）保存调用 captureSessionRequest 完成capture会话请求处理。 */ context = captureSessionRequest();
  const /* 身份凭据（token）保存表达式 context.token 的结果。 */ token = context.token;
  const /* 响应（response）保存等待 根据前面的条件，从两个候选结果中选择一个：fetch("/asterway" + path, { method: options.method || "GET", headers: { ...(options.body ? { "Conten… 完成后取得结果。 */ response = await fetch("/asterway" + path, {
    /* 方法（method）字段使用优先使用前面的有效值，没有时采用后面的备用值：options.method || "GET"。 */
    method: options.method || "GET",
    /* 请求头集合（headers）字段使用根据前面的条件，从两个候选结果中选择一个：{ ...(options.body ? { "Content-Type": "application/json" } : {}), ...(options.headers || {}), ...(t…。 */
    headers: {
      ...(options.body ? { /* 内容类型（Content-Type）字段使用固定文字 'application/json'。 */ "Content-Type": "application/json" } : {}),
      ...(options.headers || {}),
      ...(token ? { /* 授权（Authorization）字段使用表达式 `Bearer ${token}` 的结果。 */ Authorization: `Bearer ${token}` } : {}),
    },
    /* 请求正文（body）字段使用根据前面的条件，从两个候选结果中选择一个：options.rawBody ?? (options.body ? JSON.stringify(options.body) : undefined)。 */
    body: options.rawBody ?? (options.body ? JSON.stringify(options.body) : undefined),
    /* 取消信号（signal）字段使用表达式 options.signal 的结果。 */
    signal: options.signal,
  });
  const /* 结果（result）保存等待 调用 response.json 完成JSON 编码和解析库处理 完成后取得结果。 */ result = await response.json().catch(() => ({}));
  /* 调用 acceptSessionHeaders 完成接受会话请求头集合处理。 */
  acceptSessionHeaders(context, response);
  /* 检查条件 !response.ok || result.code !== "OK" 是否成立，成立时走这一分支。 */
  if (!response.ok || result.code !== "OK") {
    /* 检查条件 response.status === 401 是否成立，成立时走这一分支。 */
    if (response.status === 401) /* 调用 rejectUnauthorized 完成拒绝Unauthorized处理。 */ rejectUnauthorized(context);
    /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
    throw new ApiError(
      result.message || `请求失败（${response.status}）`,
      response.status,
      result.code,
      result.traceId,
    );
  }
  /* 返回表达式 result.data 的结果，当前函数到此结束。 */
  return result.data;
}
export /* 加载当前（loadCurrent）：加载 当前（Current）。 */ async function loadCurrent() {
  const /* 当前Epoch（currentEpoch）保存epoch。 */ currentEpoch = epoch;
  const /* 数据（data）保存等待 调用 api 完成接口处理 完成后取得结果。 */ data = await api("/me");
  /* 检查条件 currentEpoch !== epoch || !session.token 是否成立，成立时走这一分支。 */
  if (currentEpoch !== epoch || !session.token)
    /* 抛出新建 接口错误（ApiError） 实例，当前操作按失败处理。 */
    throw new ApiError("登录状态已变化，请重新登录", 401);
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(session, {
    /* 用户（user）字段使用表达式 data.user 的结果。 */
    user: data.user,
    /* 权限列表（permissions）字段使用表达式 [...data.permissions] 的结果。 */
    permissions: [...data.permissions],
    /* 菜单列表（menus）字段使用表达式 data.menus 的结果。 */
    menus: data.menus,
  });
  /* 返回数据（data），当前函数到此结束。 */
  return data;
}
export const /* 是否存在权限（hasPermission）保存判断列表或文字是否包含指定内容。 */ hasPermission = (code) => session.permissions.includes(code);
// 仅用于清理不同登录会话的前端状态，不作为认证或授权依据。
export const /* 认证会话键（authSessionKey）保存优先使用前面的有效值，没有时采用后面的备用值：() => sessionId(session.token) || ""。 */ authSessionKey = () => sessionId(session.token) || "";
export /* 全部权限列表（allPermissions）：调用 allPermissions 完成全部权限列表处理。 */ async function allPermissions() {
  const /* 结果（result）保存一个空列表，用来保存后续多条数据。 */ result = [];
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (let /* 当前页（page）保存固定数值 1。 */ page = 1; ; page++) {
    const /* 数据（data）保存等待 调用 api 完成接口处理 完成后取得结果。 */ data = await api(`/permissions?page=${page}&size=100`);
    /* 调用 result.push 完成push处理。 */
    result.push(...data.items);
    /* 检查条件 result.length >= data.total 是否成立，成立时走这一分支。 */
    if (result.length >= data.total) /* 返回结果（result），当前函数到此结束。 */ return result;
  }
}
