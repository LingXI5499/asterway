/* 引入 vue 提供的组件、状态函数或请求方法。 */
/* 管理方案加载、实时状态、接受、取消、重试和实际进度轮询。 */
import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
/* 引入 ../api/agentWorkspace 提供的组件、状态函数或请求方法。 */
import { workspaceApi } from "../api/agentWorkspace";
/* 引入 ../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../api/growth";

export /* use对话方案（useConversationPlan）：组合方案加载、确认、重试和真实进度跟踪状态。 */ function useConversationPlan(props, emit) {
  const /* 详情（detail）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ detail = ref(null), /* 流式实时状态（live）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ live = ref(null), /* 正在加载（loading）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ loading = ref(false), /* 操作进行中（acting）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ acting = ref(false);
  const /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ error = ref(""), /* 目标列表（goals）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ goals = ref([]), /* 进度错误（progressError）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ progressError = ref("");
  const /* 实际进度（tracking）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ tracking = ref(null);
  let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0, /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */ controller, /* 声明 定时器（timer），稍后再填入处理结果。 */ timer, /* 页面是否仍处于活动状态（active）保存真，表示条件成立或开关打开。 */ active = true, /* 组件是否已经销毁（disposed）保存假，表示条件不成立或开关关闭。 */ disposed = false;
  const /* 方案（plan）保存随依赖自动重算、从列表中找到的匹配对象。 */ plan = computed(() => detail.value?.plans?.find(item => String(item.id) === String(detail.value.run.latestPlanVersionId)));
  const /* 方案运行（run）保存随相关状态变化自动重算的值。 */ run = computed(() => detail.value?.run || null);
  const /* 提交回执（commit）保存随相关状态变化自动重算的值。 */ commit = computed(() => detail.value?.commit || null);
  const /* 当前步骤（phase）保存随相关状态变化自动重算的值。 */ phase = computed(() => live.value?.phase || run.value?.progress?.phase || run.value?.status || "EMPTY");
  const /* 进度（progress）保存随相关状态变化自动重算的值。 */ progress = computed(() => live.value || run.value?.progress || {});
  const /* 已成功（succeeded）保存随相关状态变化自动重算的值。 */ succeeded = computed(() => run.value?.status === "SUCCEEDED" && commit.value?.status === "SUCCEEDED");
  const /* 等待确认（waiting）保存随相关状态变化自动重算的值。 */ waiting = computed(() => run.value?.status === "WAITING_CONFIRM" && phase.value === "WAITING_CONFIRM");
  const /* 是否有方案管理权限（canManage）保存随相关状态变化自动重算的值。 */ canManage = computed(() => canGrow("agent:workflow:manage"));
  const /* 是否允许接受当前方案（canAccept）保存随相关状态变化自动重算的值。 */ canAccept = computed(() => canManage.value && waiting.value && !props.busy && !acting.value);
  const /* 是否允许重试已确认的提交（canRetry）保存随相关状态变化自动重算的值。 */ canRetry = computed(() => canManage.value && !succeeded.value && plan.value?.status === "CONFIRMED"
    && ["CONFIRMED", "EXECUTING", "FAILED"].includes(run.value?.status) && (commit.value?.attempts || 0) < 10);

  /* 停止轮询（stopPoll）：清除上一次刷新定时器，停止继续轮询。 */
  function stopPoll() { /* 取消尚未触发的一次性定时器。 */ clearTimeout(timer); /* 把空值，表示当前还没有对应内容写入 timer。 */ timer = undefined; }
  /* 安排轮询（schedulePoll）：按当前运行状态安排下一次刷新。 */
  function schedulePoll() {
    /* 清除上一次刷新定时器，停止继续轮询。 */
    stopPoll();
    /* 检查页面已离开或当前处理已停用，成立时走这一分支。 */
    if (!active || disposed || !run.value || !props.conversationId) /* 返回空内容，当前函数到此结束。 */ return;
    const /* fast保存优先使用前面的有效值，没有时采用后面的备用值：["CONTEXT", "GENERATING"].includes(phase.value) || ["CONFIRMED", "EXECUTING"].includes(run.value.status)。 */ fast = ["CONTEXT", "GENERATING"].includes(phase.value) || ["CONFIRMED", "EXECUTING"].includes(run.value.status);
    /* 检查条件 fast || run.value.status === "WAITING_CONFIRM" || (succeeded.value && props.open !== false) 是否成立，成立时走这一分支。 */
    if (fast || run.value.status === "WAITING_CONFIRM" || (succeeded.value && props.open !== false)) /* 把根据前面的条件，从两个候选结果中选择一个：setTimeout(() => load(), fast ? 2000 : 30000)写入 timer。 */ timer = setTimeout(() => load(), fast ? 2000 : 30000);
  }
  /* 通知父组件（announce）：把方案当前是否可用及其状态通知父组件。 */
  function announce() {
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("available", { /* 是否可用（available）字段使用条件判断的结果：!!run.value。 */ available: !!run.value, /* 编号（id）字段使用沿 run.value?.id 读取数据；中间对象为空时返回空值。 */ id: run.value?.id, /* 状态（status）字段使用沿 run.value?.status 读取数据；中间对象为空时返回空值。 */ status: run.value?.status });
  }
  /* 读取方案真实进度（readProgress）：读取已成功提交方案对应的真实业务进度。 */
  async function readProgress(data, ticket, signal) {
    /* 把固定文字 ''写入 progressError.value。 */
    progressError.value = "";
    /* 检查条件 data?.commit?.status !== "SUCCEEDED" 是否成立，成立时走这一分支。 */
    if (data?.commit?.status !== "SUCCEEDED") { /* 把一个空列表，用来保存后续多条数据写入 goals.value。 */ goals.value = []; /* 把空值，表示当前还没有对应内容写入 tracking.value。 */ tracking.value = null; /* 返回空内容，当前函数到此结束。 */ return; }
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      const /* 当前状态（state）保存等待 读取服务端数据；认证信息由公共请求层统一附带 完成后取得结果。 */ state = await workspaceApi.get(`planning/proposals/${data.run.id}/progress`, {}, signal);
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (ticket !== sequence || disposed) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把当前状态（state）写入 tracking.value。 */
      tracking.value = state;
      /* 把沿 state.groups?.goals || [] 读取数据；中间对象为空时返回空值写入 goals.value。 */
      goals.value = state.groups?.goals || [];
      /* 检查条件 state.partial 是否成立，成立时走这一分支。 */
      if (state.partial) /* 把沿 state.notes?.join('；') || '当前展示部分跟踪对象，完成数量仅针对已加载范围。' 读取数据；中间对象为空时返回空值写入 progressError.value。 */ progressError.value = state.notes?.join('；') || '当前展示部分跟踪对象，完成数量仅针对已加载范围。';
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
      /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence && e.name !== 'AbortError'，成立时走这一分支。 */
      if (ticket === sequence && e.name !== 'AbortError') /* 把优先使用前面的有效值，没有时采用后面的备用值：e.message || '实际进度暂未读取成功，请刷新重试。'写入 progressError.value。 */ progressError.value = e.message || '实际进度暂未读取成功，请刷新重试。';
    }
  }
  /* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  async function load(id = null) {
    /* 检查页面已离开或当前处理已停用，成立时走这一分支。 */
    if (!active || disposed || !props.conversationId || !canGrow("agent:workflow:read")) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 本次请求的序号（ticket）保存按表达式计算得到的值：++sequence。 */ ticket = ++sequence, /* 对话编号（conversationId）保存表达式 props.conversationId 的结果。 */ conversationId = props.conversationId;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    controller?.abort(); /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */ controller = new AbortController();
    const /* 取消信号（signal）保存表达式 controller.signal 的结果。 */ signal = controller.signal;
    /* 把条件判断的结果：!detail.value写入 loading.value。 */
    loading.value = !detail.value;
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      let /* 工作流运行编号（runId）保存编号（id）。 */ runId = id;
      /* 检查条件 !runId 是否成立，成立时走这一分支。 */
      if (!runId) {
        const /* 数据（data）保存等待 读取服务端数据；认证信息由公共请求层统一附带 完成后取得结果。 */ data = await workspaceApi.get("planning/proposals", { /* 直接使用已有的对话编号（conversationId）作为同名字段。 */ conversationId, /* 每页条数（size）字段使用固定数值 1。 */ size: 1 }, signal);
        /* 把沿 data.items?.[0]?.id 读取数据；中间对象为空时返回空值写入 runId。 */
        runId = data.items?.[0]?.id;
      }
      const /* 数据（data）保存根据前面的条件，从两个候选结果中选择一个：runId ? await workspaceApi.get(`planning/proposals/${runId}`, {}, signal) : null。 */ data = runId ? await workspaceApi.get(`planning/proposals/${runId}`, {}, signal) : null;
      /* 检查组件已销毁、会话已变化或请求已过时；此时不再更新页面，成立时走这一分支。 */
      if (ticket !== sequence || disposed || props.conversationId !== conversationId) /* 返回空内容，当前函数到此结束。 */ return;
      /* 检查条件 data && String(data.run.conversationId) !== String(conversationId) 是否成立，成立时走这一分支。 */
      if (data && String(data.run.conversationId) !== String(conversationId)) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("方案与当前对话不一致，请刷新重试");
      /* 把数据（data）写入 detail.value。 */
      detail.value = data;
      /* 把空值，表示当前还没有对应内容写入 live.value。 */
      live.value = null;
      /* 把固定文字 ''写入 error.value。 */
      error.value = "";
      /* 把方案当前是否可用及其状态通知父组件。 */
      announce();
      /* 等待读取已成功提交方案对应的真实业务进度。 */
      await readProgress(data, ticket, signal);
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
      /* 检查当前结果仍属于最新请求，并且其余条件也都满足：e.name !== "AbortError" && e.code !== "SESSION_CHANGED" && ticket === sequence，成立时走这一分支。 */
      if (e.name !== "AbortError" && e.code !== "SESSION_CHANGED" && ticket === sequence) /* 把优先使用前面的有效值，没有时采用后面的备用值：e.message || "方案加载失败"写入 error.value。 */ error.value = e.message || "方案加载失败";
    } finally {
      /* 检查当前结果仍属于最新请求，并且其余条件也都满足：ticket === sequence，成立时走这一分支。 */
      if (ticket === sequence) { /* 把假，表示条件不成立或开关关闭写入 loading.value。 */ loading.value = false; /* 按当前运行状态安排下一次刷新。 */ schedulePoll(); }
    }
  }
  /* 接受（accept）：先确认当前方案版本，再执行提交并重新核对回执。 */
  async function accept() {
    /* 检查条件 !canAccept.value && (!canRetry.value || props.busy || acting.value) 是否成立，成立时走这一分支。 */
    if (!canAccept.value && (!canRetry.value || props.busy || acting.value)) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 工作流运行编号（runId）保存表达式 run.value.id 的结果。 */ runId = run.value.id, /* 方案版本编号（planId）保存表达式 plan.value.id 的结果。 */ planId = plan.value.id, /* 对话编号（conversationId）保存表达式 props.conversationId 的结果。 */ conversationId = props.conversationId;
    const /* 首次接受（firstAccept）保存表达式 waiting.value 的结果。 */ firstAccept = waiting.value;
    /* 把真，表示条件成立或开关打开写入 acting.value。 */
    acting.value = true; /* 把固定文字 ''写入 error.value。 */ error.value = "";
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      /* 检查条件 firstAccept 是否成立，成立时走这一分支。 */
      if (firstAccept) {
        /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */
        await workspaceApi.post(`planning/proposals/${runId}/confirm`, { /* 方案版本号编号（planVersionId）字段使用方案版本编号（planId）。 */ planVersionId: planId });
        /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */
        if (props.conversationId === conversationId) /* 把表达式 { ...progress.value, phase: "CONFIRMED" } 的结果写入 live.value。 */ live.value = { ...progress.value, /* 当前步骤（phase）字段使用固定状态或编码 CONFIRMED（已确认）。 */ phase: "CONFIRMED" };
      }
      /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */
      if (props.conversationId === conversationId) /* 把表达式 { ...progress.value, phase: "EXECUTING" } 的结果写入 live.value。 */ live.value = { ...progress.value, /* 当前步骤（phase）字段使用固定状态或编码 EXECUTING（正在执行）。 */ phase: "EXECUTING" };
      /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */
      await workspaceApi.post(`planning/proposals/${runId}/execute`);
      /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */
      if (props.conversationId === conversationId) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(runId);
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
      /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */
      if (props.conversationId === conversationId) {
        const /* 消息（message）保存优先使用前面的有效值，没有时采用后面的备用值：e.message || "创建结果尚未核实，请刷新查看"。 */ message = e.message || "创建结果尚未核实，请刷新查看";
        /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
        await load(runId);
        /* 把消息（message）写入 error.value。 */
        error.value = message;
      }
    } finally { /* 把假，表示条件不成立或开关关闭写入 acting.value。 */ acting.value = false; }
  }
  /* 暂不接受（decline）：取消当前待确认方案，不写入其业务内容。 */
  async function decline() {
    /* 检查条件 !run.value || props.busy || acting.value || !canManage.value 是否成立，成立时走这一分支。 */
    if (!run.value || props.busy || acting.value || !canManage.value) /* 返回空内容，当前函数到此结束。 */ return;
    const /* 工作流运行编号（runId）保存表达式 run.value.id 的结果。 */ runId = run.value.id, /* 对话编号（conversationId）保存表达式 props.conversationId 的结果。 */ conversationId = props.conversationId;
    /* 把真，表示条件成立或开关打开写入 acting.value。 */
    acting.value = true; /* 把固定文字 ''写入 error.value。 */ error.value = "";
    /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
    try {
      /* 等待提交一个业务动作或创建请求；认证信息由公共请求层统一附带。 */
      await workspaceApi.post(`planning/proposals/${runId}/cancel`);
      /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */
      if (props.conversationId === conversationId) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await load(runId);
    } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) { /* 检查条件 props.conversationId === conversationId 是否成立，成立时走这一分支。 */ if (props.conversationId === conversationId) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message; }
    finally { /* 把假，表示条件不成立或开关关闭写入 acting.value。 */ acting.value = false; }
  }
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  watch(() => props.conversationId, () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    ++sequence; /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort(); /* 清除上一次刷新定时器，停止继续轮询。 */ stopPoll(); /* 把表达式 live.value = null 的结果写入 detail.value。 */ detail.value = live.value = null;
    /* 把一个空列表，用来保存后续多条数据写入 goals.value。 */
    goals.value = []; /* 把空值，表示当前还没有对应内容写入 tracking.value。 */ tracking.value = null; /* 把表达式 progressError.value = "" 的结果写入 error.value。 */ error.value = progressError.value = ""; /* 把方案当前是否可用及其状态通知父组件。 */ announce(); /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
  }, { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true });
  /* 判断列表或文字是否包含指定内容。 */
  watch(() => props.event, event => {
    /* 检查条件 !event || String(event.conversationId) !== String(props.conversationId) 是否成立，成立时走这一分支。 */
    if (!event || String(event.conversationId) !== String(props.conversationId)) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把事件（event）写入 live.value。 */
    live.value = event; /* 把固定文字 ''写入 error.value。 */ error.value = "";
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("available", { /* 是否可用（available）字段使用真，表示条件成立或开关打开。 */ available: true, /* 编号（id）字段使用表达式 event.runId 的结果。 */ id: event.runId, /* 状态（status）字段使用根据前面的条件，从两个候选结果中选择一个：event.phase === "WAITING_CONFIRM" ? "WAITING_CONFIRM" : "DRAFT"。 */ status: event.phase === "WAITING_CONFIRM" ? "WAITING_CONFIRM" : "DRAFT" });
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("reveal");
    /* 检查条件 ["WAITING_CONFIRM", "ERROR", "INTERRUPTED"].includes(event.phase) 是否成立，成立时走这一分支。 */
    if (["WAITING_CONFIRM", "ERROR", "INTERRUPTED"].includes(event.phase)) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(event.runId);
  });
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.refreshKey, () => load());
  /* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
  watch(() => props.open, open => { /* 检查条件 open 是否成立，成立时走这一分支。 */ if (open) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(); else /* 检查条件 succeeded.value 是否成立，成立时走这一分支。 */ if (succeeded.value) /* 清除上一次刷新定时器，停止继续轮询。 */ stopPoll(); });
  /* 注册缓存组件重新显示时的处理。 */
  onActivated(() => { /* 把真，表示条件成立或开关打开写入 active。 */ active = true; /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load(); });
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  onDeactivated(() => { /* 把假，表示条件不成立或开关关闭写入 active。 */ active = false; /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ ++sequence; /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort(); /* 清除上一次刷新定时器，停止继续轮询。 */ stopPoll(); });
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  onBeforeUnmount(() => { /* 把真，表示条件成立或开关打开写入 disposed。 */ disposed = true; /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */ ++sequence; /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort(); /* 清除上一次刷新定时器，停止继续轮询。 */ stopPoll(); });
  /* 返回表达式 { detail, run, plan, commit, progress, phase, succeeded, loading, acting, error, goals, progressError, tracking, canMana… 的结果，当前函数到此结束。 */
  return { /* 直接使用已有的详情（detail）作为同名字段。 */ detail, /* 直接使用已有的方案运行（run）作为同名字段。 */ run, /* 直接使用已有的方案（plan）作为同名字段。 */ plan, /* 直接使用已有的提交回执（commit）作为同名字段。 */ commit, /* 直接使用已有的进度（progress）作为同名字段。 */ progress, /* 直接使用已有的当前步骤（phase）作为同名字段。 */ phase, /* 直接使用已有的已成功（succeeded）作为同名字段。 */ succeeded, /* 直接使用已有的正在加载（loading）作为同名字段。 */ loading, /* 直接使用已有的操作进行中（acting）作为同名字段。 */ acting, /* 直接使用已有的错误（error）作为同名字段。 */ error, /* 直接使用已有的目标列表（goals）作为同名字段。 */ goals,
    /* 直接使用已有的进度错误（progressError）作为同名字段。 */
    progressError, /* 直接使用已有的实际进度（tracking）作为同名字段。 */ tracking, /* 直接使用已有的是否有方案管理权限（canManage）作为同名字段。 */ canManage, /* 直接使用已有的是否允许接受当前方案（canAccept）作为同名字段。 */ canAccept, /* 直接使用已有的是否允许重试已确认的提交（canRetry）作为同名字段。 */ canRetry, /* 直接使用已有的等待确认（waiting）作为同名字段。 */ waiting, /* 直接使用已有的加载（load）作为同名字段。 */ load, /* 直接使用已有的接受（accept）作为同名字段。 */ accept, /* 直接使用已有的暂不接受（decline）作为同名字段。 */ decline };
}
