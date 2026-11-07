import { reactive, watch, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import { session, authSessionKey } from "../api/http";
import { canGrow } from "../api/growth";
import { executionApi, notificationApi, signalChange } from "../api/personal";
import { confirmAction } from "../api/ui";

export const execution = reactive({
  current: null,
  busy: "",
  error: "",
  blocked: false,
  tick: 0,
  revision: 0,
});
export const messages = reactive({ unread: 0, error: "", blocked: false });
let generation = 0,
  reading = 0,
  notificationReading = 0,
  requestNumber = 0,
  timer,
  ticker,
  noticeTimer;
const key = () => String(session.user?.id || "") + ":" + authSessionKey();
const stamp = (row) =>
  row ? { ...row, _receivedAt: performance.now() } : null;
function apply(row) {
  if (
    row &&
    execution.current?.id === row.id &&
    (row.version < execution.current.version ||
      (row.version === execution.current.version &&
        Date.parse(row.serverNow) < Date.parse(execution.current.serverNow)))
  )
    return;
  const next = row?.status === "RUNNING" ? stamp(row) : null;
  const oldSignature = [
    execution.current?.id,
    execution.current?.version,
    execution.current?.status,
  ].join(":");
  const nextSignature = [next?.id, next?.version, next?.status].join(":");
  execution.current = next;
  if (oldSignature !== nextSignature) execution.revision++;
}
export function elapsedSeconds(row = execution.current) {
  if (!row) return 0;
  const current = execution.current?.id === row.id ? execution.current : row;
  // 依赖一个共享时钟；仅本地平滑展示，始终封顶到服务端给出的截止。
  void execution.tick;
  const base = Number(current.actualSeconds || 0);
  if (current.status !== "RUNNING") return base;
  const server = Date.parse(current.serverNow);
  const end = Math.min(
    Date.parse(current.heartbeatDeadline),
    Date.parse(current.eligibleUntil),
  );
  if (
    !Number.isFinite(server) ||
    !Number.isFinite(end) ||
    current._receivedAt === undefined
  )
    return base;
  return (
    base +
    Math.floor(
      Math.max(
        0,
        Math.min(performance.now() - current._receivedAt, end - server),
      ) / 1000,
    )
  );
}
export async function refreshExecution() {
  if (
    reading ||
    execution.busy ||
    execution.blocked ||
    !canGrow("execution:session:read")
  )
    return;
  const epoch = generation,
    identity = key();
  const ticket = ++requestNumber;
  reading = ticket;
  try {
    const row = await executionApi.current();
    if (epoch !== generation || identity !== key()) return;
    apply(row);
    execution.error = "";
  } catch (error) {
    if (epoch === generation && identity === key()) {
      execution.error =
        error.status === 403
          ? "执行权限已变化，请通过页面导航更新账户信息"
          : "执行状态暂不可用，请重试；离线时间不会自行补入";
      if (error.status === 403) {
        execution.blocked = true;
        apply(null);
      }
    }
  } finally {
    if (reading === ticket) reading = 0;
  }
}
async function poll() {
  if (
    execution.busy ||
    reading ||
    execution.blocked ||
    !canGrow("execution:session:read")
  )
    return;
  const row = execution.current;
  if (
    !row?.canHeartbeat ||
    !canGrow("execution:session:heartbeat") ||
    row.status !== "RUNNING"
  )
    return refreshExecution();
  const epoch = generation,
    identity = key();
  const ticket = ++requestNumber;
  reading = ticket;
  try {
    const value = await executionApi.heartbeat(row.id, row.activeSegmentId);
    if (epoch === generation && identity === key()) {
      apply(value);
      execution.error = "";
    }
  } catch (error) {
    if (epoch === generation && identity === key()) {
      execution.error =
        error.status === 409
          ? "执行段或控制端已变化，正在读取最新状态"
          : "心跳暂不可用，计时显示已按服务端截止封顶";
      if (error.status === 403) {
        execution.blocked = true;
        execution.error = "执行权限已变化，请通过页面导航更新账户信息";
        apply(null);
      }
    }
  } finally {
    if (reading === ticket) reading = 0;
  }
  if (epoch === generation && execution.error && !execution.busy)
    await refreshExecution();
}
export async function refreshUnread() {
  if (
    notificationReading ||
    !canGrow("notification:message:list") ||
    messages.blocked
  )
    return;
  const epoch = generation,
    identity = key();
  const ticket = ++requestNumber;
  notificationReading = ticket;
  try {
    const result = await notificationApi.unread();
    if (epoch === generation && identity === key()) {
      messages.unread = result.unreadCount;
      messages.error = "";
    }
  } catch (error) {
    if (epoch === generation && identity === key()) {
      messages.error = "未读数暂不可用";
      if (error.status === 403) messages.blocked = true;
    }
  } finally {
    if (notificationReading === ticket) notificationReading = 0;
  }
}
export async function startExecution(task) {
  if (!canGrow("execution:session:start")) throw new Error("没有开始执行权限");
  return command(
    "start",
    () =>
      executionApi.start(
        task.taskId || task.id,
        task.taskVersion ?? task.version,
      ),
    "已开始执行",
  );
}
export async function controlExecution(action, row) {
  if (!canGrow("execution:session:" + action))
    throw new Error("没有此执行操作权限");
  const expected = { id: row.id, version: row.version };
  if (action === "finish")
    await confirmAction(
      "结束本次执行？暂停间隔不会计入耗时，结束后不能恢复此会话，任务仍需单独确认完成。",
      "结束本次执行",
    );
  return command(
    action,
    () => executionApi.control(action, expected.id, expected.version),
    { pause: "执行已暂停", resume: "已继续原执行", finish: "执行已结束" }[
      action
    ],
  );
}
async function command(action, operation, success) {
  if (execution.busy) throw new Error("执行操作正在处理中");
  const identity = key(),
    epoch = ++generation;
  // 先前读取仍可完成，但不能阻塞命令后的重新读取，也不能覆盖新命令结果。
  reading = 0;
  execution.busy = action;
  execution.error = "";
  try {
    const response = await operation();
    if (identity !== key() || epoch !== generation)
      throw new Error("登录会话已变化");
    const row = response?.execution || response;
    apply(row);
    execution.revision++;
    signalChange("execution");
    ElMessage.success(success);
    return row;
  } catch (error) {
    if (identity === key() && epoch === generation)
      execution.error = error.message || "操作结果暂不明确，请先刷新执行状态";
    throw error;
  } finally {
    if (identity === key() && epoch === generation) {
      execution.busy = "";
      // 只重新读取状态，不盲目重发开始、恢复或结束命令。
      await refreshExecution();
    }
  }
}
function clearTimers() {
  clearInterval(timer);
  clearInterval(ticker);
  clearInterval(noticeTimer);
}
export function usePersonalRuntime() {
  const stop = watch(
    () =>
      [
        key(),
        !!session.user?.mustChangePassword,
        session.user?.roles?.join(","),
        session.permissions.join(","),
      ].join("|"),
    () => {
      generation++;
      clearTimers();
      reading = 0;
      notificationReading = 0;
      execution.current = null;
      execution.busy = "";
      execution.error = "";
      execution.blocked = false;
      execution.revision++;
      messages.unread = 0;
      messages.error = "";
      messages.blocked = false;
      if (session.user?.mustChangePassword) return;
      if (canGrow("execution:session:read")) {
        refreshExecution();
        timer = setInterval(poll, 20000);
        ticker = setInterval(() => {
          execution.tick++;
        }, 1000);
      }
      if (canGrow("notification:message:list")) {
        refreshUnread();
        noticeTimer = setInterval(() => {
          if (!document.hidden) refreshUnread();
        }, 30000);
      }
    },
    { immediate: true },
  );
  const foreground = () => {
    if (!document.hidden) {
      refreshExecution();
      refreshUnread();
    }
  };
  const changed = (event) => {
    if (["growth", "goal", "notification"].includes(event.detail))
      refreshUnread();
    if (["growth", "goal"].includes(event.detail)) refreshExecution();
  };
  document.addEventListener("visibilitychange", foreground);
  window.addEventListener("asterway:personal-change", changed);
  onBeforeUnmount(() => {
    stop();
    generation++;
    clearTimers();
    document.removeEventListener("visibilitychange", foreground);
    window.removeEventListener("asterway:personal-change", changed);
  });
}
