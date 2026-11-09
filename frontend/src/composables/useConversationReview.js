/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 管理聊天复盘状态、失败重试与生成待确认的调整草案。 */
import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
/* 引入 ../api/agentWorkspace 提供的组件、状态函数或请求方法。 */
import { workspaceApi } from "../api/agentWorkspace";
/* 引入 ../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../api/http";
/* 引入 ../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../api/growth";

export /* use对话复盘（useConversationReview）：组合复盘展示、重试以及生成调整草案的状态。 */ function useConversationReview(props, emit) {
  const /* 复盘报告（report）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ report = ref(null), /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ error = ref(""), /* 操作进行中（acting）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ acting = ref(false), /* 操作（action）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ action = ref("");
  const /* 是否可以复盘（canReview）保存随相关状态变化自动重算的值。 */ canReview = computed(() => canGrow("agent:review:manage"));
  const /* 是否可以Adjust（canAdjust）保存随相关状态变化自动重算的值。 */ canAdjust = computed(() => canGrow("agent:workflow:manage") && report.value?.status === "READY");
  let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0, /* 声明 定时器（timer），稍后再填入处理结果。 */ timer, /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */ controller, /* 页面是否仍处于活动状态（active）保存真，表示条件成立或开关打开。 */ active = true, /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */ disposed = false;
  /* 停止（stop）：调用 stop 完成停止处理。 */
  function stop() { /* 取消尚未触发的一次性定时器。 */ clearTimeout(timer); /* 把空值，表示当前还没有对应内容写入 timer。 */ timer = undefined; }
  /* 安排（schedule）：调用 schedule 完成安排处理。 */
  function schedule() {
    /* 调用 stop 完成停止处理。 */
    stop();
    /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
    if (active && !disposed && props.open && ["PENDING", "RUNNING"].includes(report.value?.status)) /* 把过指定毫秒后执行一次处理写入 timer。 */ timer = setTimeout(() => load(report.value.id), 2000);
  }
  /* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  async function load(id = null) {
    /* 检查页面已离开或当前处理已停用，成立时走这一分支。 */
    if (!canGrow("agent:review:read") || !active || disposed) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++sequence。 */ ticket = ++sequence, /* 对话编号（conversationId）保存表达式 props.conversationId 的结果。 */ conversationId = props.conversationId;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    controller?.abort(); /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */ controller = new AbortController();
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      const /* 数据（data）保存根据前面的条件，从两个候选结果中选择一个：id ? await workspaceApi.get(`reviews/${id}`, {}, controller.signal) : conversationId ? (await worksp…。 */ data = id ? await workspaceApi.get(`reviews/${id}`, {}, controller.signal)
        : conversationId ? (await workspaceApi.get(`reviews/latest/conversation/${conversationId}`, {}, controller.signal)).review : null;
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (ticket !== sequence || disposed || props.conversationId !== conversationId) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把数据（data）写入 report.value。 */
      report.value = data; /* 把固定文字 ''写入 error.value。 */ error.value = "";
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence && e.name !== "AbortError" && e.code !== "SESSION_CHANGED"，成立时走这一分支。 */ if (ticket === sequence && e.name !== "AbortError" && e.code !== "SESSION_CHANGED") /* 把优先使用前面的有效值，没有时采用后面的备用值：e.message || "复盘暂未读取成功"写入 error.value。 */ error.value = e.message || "复盘暂未读取成功"; }
    finally { /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence，成立时走这一分支。 */ if (ticket === sequence) /* 调用 schedule 完成安排处理。 */ schedule(); }
  }
  /* operate：调用 operate 完成operate处理。 */
  async function operate(kind, work) {
    /* 检查条件 acting.value || props.busy || !props.conversationId 是否成立，成立时走这一分支。 */
    if (acting.value || props.busy || !props.conversationId) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 对话编号（conversationId）保存表达式 props.conversationId 的结果。 */ conversationId = props.conversationId;
    /* 把真，表示条件成立或开关打开写入 acting.value。 */
    acting.value = true; /* 把种类（kind）写入 action.value。 */ action.value = kind; /* 把固定文字 ''写入 error.value。 */ error.value = ""; /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("working", true);
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      const /* 结果（result）保存等待 调用 work 完成work处理 完成后取得结果。 */ result = await work(conversationId);
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (props.conversationId !== conversationId || disposed) /* 返回空内容，当前函数到此结束。 */ return;
      /* 检查条件 kind === "adjust" 是否成立，成立时走这一分支。 */
      if (kind === "adjust") /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("adjusted", result);
      else { /* 把结果（result）写入 report.value。 */ report.value = result; /* 调用 schedule 完成安排处理。 */ schedule(); /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("reveal"); }
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */ if (props.conversationId === conversationId) /* 把优先使用前面的有效值，没有时采用后面的备用值：e.message || "操作未完成，请重试"写入 error.value。 */ error.value = e.message || "操作未完成，请重试"; }
    finally { /* 把假，表示条件不成立或开关关闭写入 acting.value。 */ acting.value = false; /* 把固定文字 ''写入 action.value。 */ action.value = ""; /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("working", false); }
  }
  /* 开始（start）：调用 start 完成开始处理。 */
  async function start() {
    /* 检查条件 !canReview.value 是否成立，成立时走这一分支。 */
    if (!canReview.value) /* 返回空内容，当前函数到此结束。 */ return;
    /* 等待设置 UTC日期（UTCDate） 的值。 */
    await operate("review", async conversationId => {
      const /* calendar保存等待 调用 api 完成接口处理 完成后取得结果。 */ calendar = await api("/dashboard/overview");
      /* 检查条件 !calendar.today 是否成立，成立时走这一分支。 */
      if (!calendar.today) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("暂时无法取得账号日期，请稍后重试");
      const /* 结束（end）保存表达式 calendar.today 的结果。 */ end = calendar.today, /* begin保存新建 日期（Date） 实例。 */ begin = new Date(`${end}T12:00:00Z`);
      /* 设置 UTC日期（UTCDate） 的值。 */
      begin.setUTCDate(begin.getUTCDate() - 6);
      /* 返回根据前面的条件，从两个候选结果中选择一个：workspaceApi.post("reviews", { dateFrom: begin.toISOString().slice(0, 10), dateTo: end, conversation…，当前函数到此结束。 */
      return workspaceApi.post("reviews", { /* 日期来源（dateFrom）字段使用调用 begin.toISOString 完成toISO文字处理。 */ dateFrom: begin.toISOString().slice(0, 10), /* 日期To（dateTo）字段使用结束（end）。 */ dateTo: end,
        /* 直接使用已有的对话编号（conversationId）作为同名字段。 */
        conversationId, ...(props.sourceRunId ? { /* 来源方案运行编号（sourceRunId）字段使用表达式 props.sourceRunId 的结果。 */ sourceRunId: props.sourceRunId } : {}) });
    });
  }
  /* 重试（retry）：调用 retry 完成重试处理。 */
  async function retry() {
    /* 检查条件 canReview.value && report.value?.status === "FAILED" 是否成立，成立时走这一分支。 */
    if (canReview.value && report.value?.status === "FAILED") /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */ await operate("retry", () => workspaceApi.post(`reviews/${report.value.id}/retry`));
  }
  /* adjust：调用 adjust 完成adjust处理。 */
  async function adjust() {
    /* 检查条件 canAdjust.value 是否成立，成立时走这一分支。 */
    if (canAdjust.value) /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */ await operate("adjust", conversationId => workspaceApi.post(`reviews/${report.value.id}/proposals`, { /* 直接使用已有的对话编号（conversationId）作为同名字段。 */ conversationId }));
  }
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.conversationId, () => { /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ ++sequence; /* 调用 stop 完成停止处理。 */ stop(); /* 把空值，表示当前还没有对应内容写入 report.value。 */ report.value = null; /* 把固定文字 ''写入 error.value。 */ error.value = ""; /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(props.externalId || null); }, { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true });
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.externalId, id => { /* 检查条件 id 是否成立，成立时走这一分支。 */ if (id) { /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(id); /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("reveal"); } });
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.event, event => {
    /* 检查条件 event && String(event.conversationId) === String(props.conversationId) 是否成立，成立时走这一分支。 */
    if (event && String(event.conversationId) === String(props.conversationId)) { /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(event.reviewId); /* 向父组件发送事件，通知数据或界面状态发生变化。 */ emit("reveal"); }
  });
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.open, open => { /* 检查条件 open 是否成立，成立时走这一分支。 */ if (open) { /* 检查条件 report.value 是否成立，成立时走这一分支。 */ if (report.value) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(report.value.id); else /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(props.externalId || null); } else /* 调用 stop 完成停止处理。 */ stop(); });
  /* 注册缓存组件重新显示时的处理。 */
  onActivated(() => { /* 把真，表示条件成立或开关打开写入 active。 */ active = true; /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(report.value?.id || props.externalId || null); });
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  onDeactivated(() => { /* 把假，表示条件不成立或开关关闭写入 active。 */ active = false; /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ ++sequence; /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort(); /* 调用 stop 完成停止处理。 */ stop(); });
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  onBeforeUnmount(() => { /* 把真，表示条件成立或开关打开写入 disposed。 */ disposed = true; /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ ++sequence; /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort(); /* 调用 stop 完成停止处理。 */ stop(); });
  /* 返回表达式 { report, error, acting, action, canReview, canAdjust, start, retry, adjust, load } 的结果，当前函数到此结束。 */
  return { /* 直接使用已有的复盘报告（report）作为同名字段。 */ report, /* 直接使用已有的错误（error）作为同名字段。 */ error, /* 直接使用已有的操作进行中（acting）作为同名字段。 */ acting, /* 直接使用已有的操作（action）作为同名字段。 */ action, /* 直接使用已有的是否可以复盘（canReview）作为同名字段。 */ canReview, /* 直接使用已有的是否可以Adjust（canAdjust）作为同名字段。 */ canAdjust, /* 直接使用已有的开始（start）作为同名字段。 */ start, /* 直接使用已有的重试（retry）作为同名字段。 */ retry, /* 直接使用已有的adjust作为同名字段。 */ adjust, /* 直接使用已有的加载（load）作为同名字段。 */ load };
}
