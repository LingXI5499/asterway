/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 保存并共享个人页面状态，相关组件读取同一份数据。 */
import { reactive, watch, onBeforeUnmount } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { session, authSessionKey } from "../api/http";
/* 引入 ../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../api/growth";
/* 引入 ../api/personal 提供的组件、状态函数或请求方法。 */
import { executionApi, notificationApi, signalChange } from "../api/personal";
/* 引入 ../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction } from "../api/ui";

export const /* 执行计时（execution）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ execution = reactive({
  /* 当前（current）字段使用空值，表示当前还没有对应内容。 */
  current: null,
  /* 正在处理（busy）字段使用固定文字 ''。 */
  busy: "",
  /* 错误（error）字段使用固定文字 ''。 */
  error: "",
  /* blocked字段使用假，表示条件不成立或开关关闭。 */
  blocked: false,
  /* tick字段使用固定数值 0。 */
  tick: 0,
  /* 修订版本（revision）字段使用固定数值 0。 */
  revision: 0,
});
export const /* 消息列表（messages）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ messages = reactive({ /* unread字段使用固定数值 0。 */ unread: 0, /* 错误（error）字段使用固定文字 ''。 */ error: "", /* blocked字段使用假，表示条件不成立或开关关闭。 */ blocked: false });
let /* generation保存固定数值 0。 */ generation = 0,
  /* reading保存固定数值 0。 */
  reading = 0,
  /* 通知Reading（notificationReading）保存固定数值 0。 */
  notificationReading = 0,
  /* 请求数值（requestNumber）保存固定数值 0。 */
  requestNumber = 0,
  /* 声明 定时器（timer），稍后再填入处理结果。 */
  timer,
  /* 声明 ticker，稍后再填入处理结果。 */
  ticker,
  /* 声明 达标提醒定时器（noticeTimer），稍后再填入处理结果。 */
  noticeTimer;
const /* 键（key）保存沿 () => String(session.user?.id || "") + ":" + authSessionKey() 读取数据；中间对象为空时返回空值。 */ key = () => String(session.user?.id || "") + ":" + authSessionKey();
const /* stamp保存根据前面的条件，从两个候选结果中选择一个：(row) => row ? { ...row, _receivedAt: performance.now() } : null。 */ stamp = (row) =>
  row ? { ...row, /* receivedAt（_receivedAt）字段使用调用 performance.now 完成当前时间处理。 */ _receivedAt: performance.now() } : null;
/* apply：调用 apply 完成apply处理。 */
function apply(row) {
  /* 检查当前记录版本是否符合请求要求：row && execution.current?.id === row.id && (row.version < execution.current.version || (row.version === execution.current.version && Date.parse(r…，成立时走这一分支。 */
  if (
    row &&
    execution.current?.id === row.id &&
    (row.version < execution.current.version ||
      (row.version === execution.current.version &&
        Date.parse(row.serverNow) < Date.parse(execution.current.serverNow)))
  )
    /* 返回空内容，当前函数到此结束。 */
    return;
  const /* 下一个（next）保存沿 row?.status === "RUNNING" ? stamp(row) : null 读取数据；中间对象为空时返回空值。 */ next = row?.status === "RUNNING" ? stamp(row) : null;
  const /* 旧Signature（oldSignature）保存沿 [ execution.current?.id, execution.current?.version, execution.current?.status, ].join(":") 读取数据；中间对象为空时返回空值。 */ oldSignature = [
    execution.current?.id,
    execution.current?.version,
    execution.current?.status,
  ].join(":");
  const /* 下一个Signature（nextSignature）保存沿 [next?.id, next?.version, next?.status].join(":") 读取数据；中间对象为空时返回空值。 */ nextSignature = [next?.id, next?.version, next?.status].join(":");
  /* 把下一个（next）写入 execution.current。 */
  execution.current = next;
  /* 检查条件 oldSignature !== nextSignature 是否成立，成立时走这一分支。 */
  if (oldSignature !== nextSignature) /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ execution.revision++;
}
export /* 已耗时秒数（elapsedSeconds）：调用 elapsedSeconds 完成已耗时秒数处理。 */ function elapsedSeconds(row = execution.current) {
  /* 检查条件 !row 是否成立，成立时走这一分支。 */
  if (!row) /* 返回固定数值 0，当前函数到此结束。 */ return 0;
  const /* 当前（current）保存沿 execution.current?.id === row.id ? execution.current : row 读取数据；中间对象为空时返回空值。 */ current = execution.current?.id === row.id ? execution.current : row;
  // 依赖一个共享时钟；仅本地平滑展示，始终封顶到服务端给出的截止。
  /* 更新本次处理状态：void execution.tick。 */
  void execution.tick;
  const /* 基础（base）保存优先使用前面的有效值，没有时采用后面的备用值：Number(current.actualSeconds || 0)。 */ base = Number(current.actualSeconds || 0);
  /* 检查条件 current.status !== "RUNNING" 是否成立，成立时走这一分支。 */
  if (current.status !== "RUNNING") /* 返回基础（base），当前函数到此结束。 */ return base;
  const /* server保存调用 Date.parse 完成解析处理。 */ server = Date.parse(current.serverNow);
  const /* 结束（end）保存调用 Math.min 完成最小处理。 */ end = Math.min(
    Date.parse(current.heartbeatDeadline),
    Date.parse(current.eligibleUntil),
  );
  /* 检查条件 !Number.isFinite(server) || !Number.isFinite(end) || current._receivedAt === undefined 是否成立，成立时走这一分支。 */
  if (
    !Number.isFinite(server) ||
    !Number.isFinite(end) ||
    current._receivedAt === undefined
  )
    /* 返回基础（base），当前函数到此结束。 */
    return base;
  /* 返回调用 ( base + Math.floor 完成floor处理，当前函数到此结束。 */
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
export /* 刷新执行计时（refreshExecution）：刷新 执行计时（Execution）。 */ async function refreshExecution() {
  /* 检查当前用户是否具有需要的权限：reading || execution.busy || execution.blocked || !canGrow("execution:session:read")，成立时走这一分支。 */
  if (
    reading ||
    execution.busy ||
    execution.blocked ||
    !canGrow("execution:session:read")
  )
    /* 返回空内容，当前函数到此结束。 */
    return;
  const /* epoch保存generation。 */ epoch = generation,
    /* 账户管理（identity）保存把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键。 */
    identity = key();
  const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++requestNumber。 */ ticket = ++requestNumber;
  /* 把本次请求的序号（ticket）写入 reading。 */
  reading = ticket;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 当前记录（row）保存等待 调用 executionApi.current 完成当前处理 完成后取得结果。 */ row = await executionApi.current();
    /* 检查条件 epoch !== generation || identity !== key() 是否成立，成立时走这一分支。 */
    if (epoch !== generation || identity !== key()) /* 返回空内容，当前函数到此结束。 */ return;
    /* 调用 apply 完成apply处理。 */
    apply(row);
    /* 把固定文字 ''写入 execution.error。 */
    execution.error = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 epoch === generation && identity === key() 是否成立，成立时走这一分支。 */
    if (epoch === generation && identity === key()) {
      /* 把根据前面的条件，从两个候选结果中选择一个：error.status === 403 ? "执行权限已变化，请通过页面导航更新账户信息" : "执行状态暂不可用，请重试；离线时间不会自行补入"写入 execution.error。 */
      execution.error =
        error.status === 403
          ? "执行权限已变化，请通过页面导航更新账户信息"
          : "执行状态暂不可用，请重试；离线时间不会自行补入";
      /* 检查条件 error.status === 403 是否成立，成立时走这一分支。 */
      if (error.status === 403) {
        /* 把真，表示条件成立或开关打开写入 execution.blocked。 */
        execution.blocked = true;
        /* 调用 apply 完成apply处理。 */
        apply(null);
      }
    }
  } finally {
    /* 检查条件 reading === ticket 是否成立，成立时走这一分支。 */
    if (reading === ticket) /* 把固定数值 0写入 reading。 */ reading = 0;
  }
}
/* 轮询（poll）：调用 poll 完成轮询处理。 */
async function poll() {
  /* 检查当前用户是否具有需要的权限：execution.busy || reading || execution.blocked || !canGrow("execution:session:read")，成立时走这一分支。 */
  if (
    execution.busy ||
    reading ||
    execution.blocked ||
    !canGrow("execution:session:read")
  )
    /* 返回空内容，当前函数到此结束。 */
    return;
  const /* 当前记录（row）保存表达式 execution.current 的结果。 */ row = execution.current;
  /* 检查当前用户是否具有需要的权限：!row?.canHeartbeat || !canGrow("execution:session:heartbeat") || row.status !== "RUNNING"，成立时走这一分支。 */
  if (
    !row?.canHeartbeat ||
    !canGrow("execution:session:heartbeat") ||
    row.status !== "RUNNING"
  )
    /* 返回刷新 执行计时（Execution），当前函数到此结束。 */
    return refreshExecution();
  const /* epoch保存generation。 */ epoch = generation,
    /* 账户管理（identity）保存把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键。 */
    identity = key();
  const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++requestNumber。 */ ticket = ++requestNumber;
  /* 把本次请求的序号（ticket）写入 reading。 */
  reading = ticket;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 值（value）保存等待 调用 executionApi.heartbeat 完成心跳处理 完成后取得结果。 */ value = await executionApi.heartbeat(row.id, row.activeSegmentId);
    /* 检查条件 epoch === generation && identity === key() 是否成立，成立时走这一分支。 */
    if (epoch === generation && identity === key()) {
      /* 调用 apply 完成apply处理。 */
      apply(value);
      /* 把固定文字 ''写入 execution.error。 */
      execution.error = "";
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 epoch === generation && identity === key() 是否成立，成立时走这一分支。 */
    if (epoch === generation && identity === key()) {
      /* 把根据前面的条件，从两个候选结果中选择一个：error.status === 409 ? "执行段或控制端已变化，正在读取最新状态" : "心跳暂不可用，计时显示已按服务端截止封顶"写入 execution.error。 */
      execution.error =
        error.status === 409
          ? "执行段或控制端已变化，正在读取最新状态"
          : "心跳暂不可用，计时显示已按服务端截止封顶";
      /* 检查条件 error.status === 403 是否成立，成立时走这一分支。 */
      if (error.status === 403) {
        /* 把真，表示条件成立或开关打开写入 execution.blocked。 */
        execution.blocked = true;
        /* 把固定文字 '执行权限已变化，请通过页面导航更新账户信息'写入 execution.error。 */
        execution.error = "执行权限已变化，请通过页面导航更新账户信息";
        /* 调用 apply 完成apply处理。 */
        apply(null);
      }
    }
  } finally {
    /* 检查条件 reading === ticket 是否成立，成立时走这一分支。 */
    if (reading === ticket) /* 把固定数值 0写入 reading。 */ reading = 0;
  }
  /* 检查条件 epoch === generation && execution.error && !execution.busy 是否成立，成立时走这一分支。 */
  if (epoch === generation && execution.error && !execution.busy)
    /* 等待刷新 执行计时（Execution）。 */
    await refreshExecution();
}
export /* 刷新Unread（refreshUnread）：刷新 Unread。 */ async function refreshUnread() {
  /* 检查当前用户是否具有需要的权限：notificationReading || !canGrow("notification:message:list") || messages.blocked，成立时走这一分支。 */
  if (
    notificationReading ||
    !canGrow("notification:message:list") ||
    messages.blocked
  )
    /* 返回空内容，当前函数到此结束。 */
    return;
  const /* epoch保存generation。 */ epoch = generation,
    /* 账户管理（identity）保存把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键。 */
    identity = key();
  const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++requestNumber。 */ ticket = ++requestNumber;
  /* 把本次请求的序号（ticket）写入 notificationReading。 */
  notificationReading = ticket;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 结果（result）保存等待 调用 notificationApi.unread 完成unread处理 完成后取得结果。 */ result = await notificationApi.unread();
    /* 检查条件 epoch === generation && identity === key() 是否成立，成立时走这一分支。 */
    if (epoch === generation && identity === key()) {
      /* 把表达式 result.unreadCount 的结果写入 messages.unread。 */
      messages.unread = result.unreadCount;
      /* 把固定文字 ''写入 messages.error。 */
      messages.error = "";
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 epoch === generation && identity === key() 是否成立，成立时走这一分支。 */
    if (epoch === generation && identity === key()) {
      /* 把固定文字 '未读数暂不可用'写入 messages.error。 */
      messages.error = "未读数暂不可用";
      /* 检查条件 error.status === 403 是否成立，成立时走这一分支。 */
      if (error.status === 403) /* 把真，表示条件成立或开关打开写入 messages.blocked。 */ messages.blocked = true;
    }
  } finally {
    /* 检查条件 notificationReading === ticket 是否成立，成立时走这一分支。 */
    if (notificationReading === ticket) /* 把固定数值 0写入 notificationReading。 */ notificationReading = 0;
  }
}
export /* 开始执行计时（startExecution）：调用 startExecution 完成开始执行计时处理。 */ async function startExecution(task) {
  /* 检查当前用户是否具有需要的权限：!canGrow("execution:session:start")，成立时走这一分支。 */
  if (!canGrow("execution:session:start")) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("没有开始执行权限");
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：command( "start", () => executionApi.start( task.taskId || task.id, task.taskVersion ?? task.version, ), "已开始执…，当前函数到此结束。 */
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
export /* control执行计时（controlExecution）：调用 controlExecution 完成control执行计时处理。 */ async function controlExecution(action, row) {
  /* 检查当前用户是否具有需要的权限：!canGrow("execution:session:" + action)，成立时走这一分支。 */
  if (!canGrow("execution:session:" + action))
    /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
    throw new Error("没有此执行操作权限");
  const /* 预计（expected）保存表达式 { id: row.id, version: row.version } 的结果。 */ expected = { /* 编号（id）字段使用表达式 row.id 的结果。 */ id: row.id, /* 版本号（version）字段使用表达式 row.version 的结果。 */ version: row.version };
  /* 检查条件 action === "finish" 是否成立，成立时走这一分支。 */
  if (action === "finish")
    /* 等待确认 操作（Action）。 */
    await confirmAction(
      "结束本次执行？暂停间隔不会计入耗时，结束后不能恢复此会话，任务仍需单独确认完成。",
      "结束本次执行",
    );
  /* 返回调用 command( action, () => executionApi.control 完成control处理，当前函数到此结束。 */
  return command(
    action,
    () => executionApi.control(action, expected.id, expected.version),
    { /* 暂停（pause）字段使用固定文字 '执行已暂停'。 */ pause: "执行已暂停", /* 继续（resume）字段使用固定文字 '已继续原执行'。 */ resume: "已继续原执行", /* finish字段使用固定文字 '执行已结束'。 */ finish: "执行已结束" }[
      action
    ],
  );
}
/* command：调用 command 完成command处理。 */
async function command(action, operation, success) {
  /* 检查条件 execution.busy 是否成立，成立时走这一分支。 */
  if (execution.busy) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("执行操作正在处理中");
  const /* 账户管理（identity）保存把工具名称和按字段名排序的参数 JSON 组合成可比较的缓存键。 */ identity = key(),
    /* epoch保存按表达式计算得到的值：++generation。 */
    epoch = ++generation;
  // 先前读取仍可完成，但不能阻塞命令后的重新读取，也不能覆盖新命令结果。
  /* 把固定数值 0写入 reading。 */
  reading = 0;
  /* 把操作（action）写入 execution.busy。 */
  execution.busy = action;
  /* 把固定文字 ''写入 execution.error。 */
  execution.error = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 响应（response）保存等待 调用 operation 完成操作处理 完成后取得结果。 */ response = await operation();
    /* 检查条件 identity !== key() || epoch !== generation 是否成立，成立时走这一分支。 */
    if (identity !== key() || epoch !== generation)
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("登录会话已变化");
    const /* 当前记录（row）保存沿 response?.execution || response 读取数据；中间对象为空时返回空值。 */ row = response?.execution || response;
    /* 调用 apply 完成apply处理。 */
    apply(row);
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    execution.revision++;
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("execution");
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success(success);
    /* 返回当前记录（row），当前函数到此结束。 */
    return row;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (error) {
    /* 检查条件 identity === key() && epoch === generation 是否成立，成立时走这一分支。 */
    if (identity === key() && epoch === generation)
      /* 把优先使用前面的有效值，没有时采用后面的备用值：error.message || "操作结果暂不明确，请先刷新执行状态"写入 execution.error。 */
      execution.error = error.message || "操作结果暂不明确，请先刷新执行状态";
    /* 抛出错误（error），当前操作按失败处理。 */
    throw error;
  } finally {
    /* 检查条件 identity === key() && epoch === generation 是否成立，成立时走这一分支。 */
    if (identity === key() && epoch === generation) {
      /* 把固定文字 ''写入 execution.busy。 */
      execution.busy = "";
      // 只重新读取状态，不盲目重发开始、恢复或结束命令。
      /* 等待刷新 执行计时（Execution）。 */
      await refreshExecution();
    }
  }
}
/* 清除Timers（clearTimers）：调用 clearTimers 完成清除Timers处理。 */
function clearTimers() {
  /* 停止重复执行的定时器。 */
  clearInterval(timer);
  /* 停止重复执行的定时器。 */
  clearInterval(ticker);
  /* 停止重复执行的定时器。 */
  clearInterval(noticeTimer);
}
export /* use个人Runtime（usePersonalRuntime）：调用 usePersonalRuntime 完成use个人Runtime处理。 */ function usePersonalRuntime() {
  const /* 停止（stop）保存沿 watch( () => [ key(), !!session.user?.mustChangePassword, session.user?.roles?.join(","), session.permissions.join(… 读取数据；中间对象为空时返回空值。 */ stop = watch(
    () =>
      [
        key(),
        !!session.user?.mustChangePassword,
        session.user?.roles?.join(","),
        session.permissions.join(","),
      ].join("|"),
    () => {
      /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
      generation++;
      /* 调用 clearTimers 完成清除Timers处理。 */
      clearTimers();
      /* 把固定数值 0写入 reading。 */
      reading = 0;
      /* 把固定数值 0写入 notificationReading。 */
      notificationReading = 0;
      /* 把空值，表示当前还没有对应内容写入 execution.current。 */
      execution.current = null;
      /* 把固定文字 ''写入 execution.busy。 */
      execution.busy = "";
      /* 把固定文字 ''写入 execution.error。 */
      execution.error = "";
      /* 把假，表示条件不成立或开关关闭写入 execution.blocked。 */
      execution.blocked = false;
      /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
      execution.revision++;
      /* 把固定数值 0写入 messages.unread。 */
      messages.unread = 0;
      /* 把固定文字 ''写入 messages.error。 */
      messages.error = "";
      /* 把假，表示条件不成立或开关关闭写入 messages.blocked。 */
      messages.blocked = false;
      /* 检查条件 session.user?.mustChangePassword 是否成立，成立时走这一分支。 */
      if (session.user?.mustChangePassword) /* 返回空内容，当前函数到此结束。 */ return;
      /* 检查当前用户是否具有需要的权限：canGrow("execution:session:read")，成立时走这一分支。 */
      if (canGrow("execution:session:read")) {
        /* 刷新 执行计时（Execution）。 */
        refreshExecution();
        /* 把每隔指定毫秒重复执行处理写入 timer。 */
        timer = setInterval(poll, 20000);
        /* 把每隔指定毫秒重复执行处理写入 ticker。 */
        ticker = setInterval(() => {
          /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
          execution.tick++;
        }, 1000);
      }
      /* 检查当前用户是否具有需要的权限：canGrow("notification:message:list")，成立时走这一分支。 */
      if (canGrow("notification:message:list")) {
        /* 刷新 Unread。 */
        refreshUnread();
        /* 把每隔指定毫秒重复执行处理写入 noticeTimer。 */
        noticeTimer = setInterval(() => {
          /* 检查条件 !document.hidden 是否成立，成立时走这一分支。 */
          if (!document.hidden) /* 刷新 Unread。 */ refreshUnread();
        }, 30000);
      }
    },
    { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
  );
  const /* foreground保存条件判断的结果：() => { if (!document.hidden) { refreshExecution(); refreshUnread(); } }。 */ foreground = () => {
    /* 检查条件 !document.hidden 是否成立，成立时走这一分支。 */
    if (!document.hidden) {
      /* 刷新 执行计时（Execution）。 */
      refreshExecution();
      /* 刷新 Unread。 */
      refreshUnread();
    }
  };
  const /* 已修改（changed）保存判断列表或文字是否包含指定内容。 */ changed = (event) => {
    /* 检查条件 ["growth", "goal", "notification"].includes(event.detail) 是否成立，成立时走这一分支。 */
    if (["growth", "goal", "notification"].includes(event.detail))
      /* 刷新 Unread。 */
      refreshUnread();
    /* 检查条件 ["growth", "goal"].includes(event.detail) 是否成立，成立时走这一分支。 */
    if (["growth", "goal"].includes(event.detail)) /* 刷新 执行计时（Execution）。 */ refreshExecution();
  };
  /* 调用 document.addEventListener 完成加入事件Listener处理。 */
  document.addEventListener("visibilitychange", foreground);
  /* 调用 window.addEventListener 完成加入事件Listener处理。 */
  window.addEventListener("asterway:personal-change", changed);
  /* 移除 事件Listener（EventListener）。 */
  onBeforeUnmount(() => {
    /* 调用 stop 完成停止处理。 */
    stop();
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    generation++;
    /* 调用 clearTimers 完成清除Timers处理。 */
    clearTimers();
    /* 移除 事件Listener（EventListener）。 */
    document.removeEventListener("visibilitychange", foreground);
    /* 移除 事件Listener（EventListener）。 */
    window.removeEventListener("asterway:personal-change", changed);
  });
}
