/* 引入 node:test 提供的组件、状态函数或请求方法。 */
/* 检查成长助手规划工作流test相关行为，断言失败时报告具体不符合预期的场景。 */
import test from "node:test";
/* 引入 node:assert/strict 提供的组件、状态函数或请求方法。 */
import assert from "node:assert/strict";
/* 引入 node:fs/promises 提供的组件、状态函数或请求方法。 */
import { readFile } from "node:fs/promises";
/* 引入 node:vm 提供的组件、状态函数或请求方法。 */
import vm from "node:vm";
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import * as Vue from "vue";

/* isolatedModule：判断是否为 olatedModule。 */
async function isolatedModule(path, imports, extra = "") {
  const /* 来源（source）保存等待 读取 文件（File） 完成后取得结果。 */ source = await readFile(new URL(path, import.meta.url), "utf8");
  const /* 上下文（context）保存创建 上下文（Context）。 */ context = vm.createContext({ /* 直接使用已有的文字Decoder（TextDecoder）作为同名字段。 */ TextDecoder, /* 直接使用已有的DOMException作为同名字段。 */ DOMException, /* 直接使用已有的Abort请求入口（AbortController）作为同名字段。 */ AbortController, /* 直接使用已有的地址搜索参数集合（URLSearchParams）作为同名字段。 */ URLSearchParams, /* 直接使用已有的设置超时（setTimeout）作为同名字段。 */ setTimeout, /* 直接使用已有的清除超时（clearTimeout）作为同名字段。 */ clearTimeout });
  const /* module保存新建 vm中的来源文字Module（vm.SourceTextModule） 实例。 */ module = new vm.SourceTextModule(source + extra, { /* 直接使用已有的上下文（context）作为同名字段。 */ context });
  /* 等待调用 module.link 完成link处理。 */
  await module.link(async name => {
    const /* 值列表（values）保存表达式 imports[name] 的结果。 */ values = imports[name];
    /* 调用 assert.ok 完成ok处理。 */
    assert.ok(values, `Unexpected import: ${name}`);
    /* 返回新建 vm中的SyntheticModule（vm.SyntheticModule） 实例，当前函数到此结束。 */
    return new vm.SyntheticModule(Object.keys(values), function () {
      /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
      for (const /* 声明 键值（[key, value]），稍后再填入处理结果。 */ [key, value] of Object.entries(values)) /* 设置 Export 的值。 */ this.setExport(key, value);
    }, { /* 直接使用已有的上下文（context）作为同名字段。 */ context });
  });
  /* 等待调用 module.evaluate 完成evaluate处理。 */
  await module.evaluate();
  /* 返回表达式 module.namespace 的结果，当前函数到此结束。 */
  return module.namespace;
}

/* 定义 ApiError，把相关状态与方法放在同一个对象中。 */
class ApiError extends Error { /* constructor：调用 constructor 完成constructor处理。 */ constructor(message, status, code) { /* 调用 super 完成super处理。 */ super(message); /* 调用 Object.assign 完成安排处理。 */ Object.assign(this, { /* 直接使用已有的状态（status）作为同名字段。 */ status, /* 直接使用已有的编码（code）作为同名字段。 */ code }); } }
const /* 会话Boundary（sessionBoundary）保存表达式 { ApiError, acceptSessionHeaders() {}, captureSessionRequest() { return {}; }, checkSessionRequest() {}, rejectUnauthori… 的结果。 */ sessionBoundary = {
  /* 直接使用已有的接口错误（ApiError）作为同名字段。 */
  ApiError, /* 接受会话请求头集合（acceptSessionHeaders）：调用 acceptSessionHeaders 完成接受会话请求头集合处理。 */ acceptSessionHeaders() {}, /* capture会话请求（captureSessionRequest）：调用 captureSessionRequest 完成capture会话请求处理。 */ captureSessionRequest() { /* 返回一个空的键值对应表，当前函数到此结束。 */ return {}; },
  /* 核对会话请求（checkSessionRequest）：核对 会话请求（SessionRequest）。 */
  checkSessionRequest() {}, /* 拒绝Unauthorized（rejectUnauthorized）：调用 rejectUnauthorized 完成拒绝Unauthorized处理。 */ rejectUnauthorized() {}, /* 接口（api）：调用 api 完成接口处理。 */ api() {},
};
const /* 分段回复流Module（streamModule）保存等待 判断是否为 olatedModule 完成后取得结果。 */ streamModule = await isolatedModule("../src/api/agent.js", { /* HTTP请求（./http）字段使用会话Boundary（sessionBoundary）。 */ "./http": sessionBoundary }, "\nexport { readEventStream };\n");
/* 响应（response）：调用 response 完成响应处理。 */
function response(events) {
  const /* bytes保存新建 文字Encoder（TextEncoder） 实例。 */ bytes = new TextEncoder().encode(events.map(([type, data]) => `event: ${type}\ndata: ${JSON.stringify(data)}\n\n`).join(""));
  /* 返回新建 响应（Response） 实例，当前函数到此结束。 */
  return new Response(new ReadableStream({ /* 开始（start）：调用 start 完成开始处理。 */ start(controller) {
    // 切开 UTF-8 字符和 SSE 帧，覆盖逐字回复的真实边界。
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    for (let /* 索引（index）保存固定数值 0。 */ index = 0; index < bytes.length; index += 7) /* 调用 controller.enqueue 完成enqueue处理。 */ controller.enqueue(bytes.slice(index, index + 7));
    /* 释放连接、文件或客户端占用的资源。 */
    controller.close();
  } }));
}

/* 读取 事件分段回复流（EventStream）。 */
test("SSE streams text, execution summaries and workflow updates together", async () => {
  const /* received保存一个空列表，用来保存后续多条数据。 */ received = [];
  /* 等待读取 事件分段回复流（EventStream）。 */
  await streamModule.readEventStream(response([
    ["message_start", { /* 本轮对话编号（turnId）字段使用固定文字 '31'。 */ turnId: "31" }],
    ["activity", { /* 编号（id）字段使用固定文字 'context'。 */ id: "context", /* 显示名称（label）字段使用固定文字 '读取上下文'。 */ label: "读取上下文", /* 状态（status）字段使用固定文字 'ok'。 */ status: "ok" }],
    ["tool_start", { /* 编号（id）字段使用固定文字 'p1'。 */ id: "p1", /* 名称（name）字段使用固定文字 'prepare_growth_plan'。 */ name: "prepare_growth_plan", /* resetReply字段使用真，表示条件成立或开关打开。 */ resetReply: true }],
    ["workflow_update", { /* 工作流运行编号（runId）字段使用固定文字 '41'。 */ runId: "41", /* 对话编号（conversationId）字段使用固定文字 '11'。 */ conversationId: "11", /* 当前步骤（phase）字段使用固定状态或编码 WAITING_CONFIRM（等待确认确认）。 */ phase: "WAITING_CONFIRM" }],
    ["review_update", { /* 复盘编号（reviewId）字段使用固定文字 '9'。 */ reviewId: "9", /* 对话编号（conversationId）字段使用固定文字 '11'。 */ conversationId: "11", /* 状态（status）字段使用固定状态或编码 READY（已就绪）。 */ status: "READY" }],
    ["tool_end", { /* 编号（id）字段使用固定文字 'p1'。 */ id: "p1", /* 状态（status）字段使用固定文字 'ok'。 */ status: "ok", /* 汇总（summary）字段使用固定文字 '草案已就绪'。 */ summary: "草案已就绪" }],
    ["delta", { /* 文字（text）字段使用固定文字 '方案已整理，'。 */ text: "方案已整理，" }], ["delta", { /* 文字（text）字段使用固定文字 '请核对。'。 */ text: "请核对。" }],
    ["message_end", { /* finish原因（finishReason）字段使用固定文字 'stop'。 */ finishReason: "stop", /* 完成（complete）字段使用真，表示条件成立或开关打开。 */ complete: true }],
  ]), { /* 上下文（context）字段使用一个空的键值对应表。 */ context: {}, /* 事件发生时Delta（onDelta）字段使用调用 text => received.push 完成push处理。 */ onDelta: text => received.push(text), /* 事件发生时Activity（onActivity）字段使用调用 data => received.push 完成push处理。 */ onActivity: data => received.push(data.id),
    /* 事件发生时规划工作流（onWorkflow）字段使用调用 data => received.push 完成push处理。 */
    onWorkflow: data => received.push(data.phase), /* 事件发生时复盘（onReview）字段使用调用 data => received.push 完成push处理。 */ onReview: data => received.push(data.status) });
  /* 调用 assert.deepEqual 完成deepEqual处理。 */
  assert.deepEqual(received, ["context", "WAITING_CONFIRM", "READY", "方案已整理，", "请核对。"]);
});

/* 调用 test("SSE refuses planning events before message_start and incomplete replies", async () => { await assert.rejects 完成rejects处理。 */
test("SSE refuses planning events before message_start and incomplete replies", async () => {
  /* 等待调用 assert.rejects 完成rejects处理。 */
  await assert.rejects(streamModule.readEventStream(response([["workflow_update", { /* 工作流运行编号（runId）字段使用固定文字 '41'。 */ runId: "41" }]]), { /* 上下文（context）字段使用一个空的键值对应表。 */ context: {} }), { /* 编码（code）字段使用固定状态或编码 INVALID_STREAM（不合法分段回复流）。 */ code: "INVALID_STREAM" });
  /* 等待调用 assert.rejects 完成rejects处理。 */
  await assert.rejects(streamModule.readEventStream(response([["message_start", {}], ["delta", { /* 文字（text）字段使用固定文字 '部分回复'。 */ text: "部分回复" }]]), { /* 上下文（context）字段使用一个空的键值对应表。 */ context: {} }), { /* 编码（code）字段使用固定状态或编码 STREAM_INCOMPLETE（分段回复流INCOMPLETE）。 */ code: "STREAM_INCOMPLETE" });
});

const /* 暂停（pause）保存表达式 () => new Promise(resolve => setTimeout(resolve, 0)) 的结果。 */ pause = () => new Promise(resolve => setTimeout(resolve, 0));
const /* 规划工作流（workflow）保存表达式 (id = "41", conversationId = "11", status = "WAITING_CONFIRM") => ({ run: { id, conversationId, status, latestPlanVersio… 的结果。 */ workflow = (id = "41", conversationId = "11", status = "WAITING_CONFIRM") => ({
  /* 方案运行（run）字段使用表达式 { id, conversationId, status, latestPlanVersionId: "81", progress: { phase: status } } 的结果。 */
  run: { /* 直接使用已有的编号（id）作为同名字段。 */ id, /* 直接使用已有的对话编号（conversationId）作为同名字段。 */ conversationId, /* 直接使用已有的状态（status）作为同名字段。 */ status, /* 最新方案版本号编号（latestPlanVersionId）字段使用固定文字 '81'。 */ latestPlanVersionId: "81", /* 进度（progress）字段使用表达式 { phase: status } 的结果。 */ progress: { /* 当前步骤（phase）字段使用状态（status）。 */ phase: status } },
  /* 方案列表（plans）字段使用根据前面的条件，从两个候选结果中选择一个：[{ id: "81", versionNo: 1, status: status === "WAITING_CONFIRM" ? status : "CONFIRMED", snapshot: {}…。 */
  plans: [{ /* 编号（id）字段使用固定文字 '81'。 */ id: "81", /* 版本号No（versionNo）字段使用固定数值 1。 */ versionNo: 1, /* 状态（status）字段使用根据前面的条件，从两个候选结果中选择一个：status === "WAITING_CONFIRM" ? status : "CONFIRMED"。 */ status: status === "WAITING_CONFIRM" ? status : "CONFIRMED", /* 确认时的数据快照（snapshot）字段使用一个空的键值对应表。 */ snapshot: {} }],
  /* 提交回执（commit）字段使用根据前面的条件，从两个候选结果中选择一个：status === "SUCCEEDED" ? { status, result: {} } : null。 */
  commit: status === "SUCCEEDED" ? { /* 直接使用已有的状态（status）作为同名字段。 */ status, /* 结果（result）字段使用一个空的键值对应表。 */ result: {} } : null,
});
/* 方案Harness（planHarness）：调用 planHarness 完成方案Harness处理。 */
async function planHarness({ /* 直接使用已有的读取（get）作为同名字段。 */ get, /* 直接使用已有的post作为同名字段。 */ post, /* 直接使用已有的正在处理不成立（busy = false）作为同名字段。 */ busy = false } = {}) {
  const /* disposals保存一个空列表，用来保存后续多条数据。 */ disposals = [];
  const /* module保存等待 调用 isolatedModule("../src/composables/useConversationPlan.js", { vue: { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push 完成push处理 完成后取得结果。 */ module = await isolatedModule("../src/composables/useConversationPlan.js", {
    /* vue字段使用调用 { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push 完成push处理。 */
    vue: { ...Vue, /* 事件发生时Activated（onActivated）：注册缓存组件重新显示时的处理。 */ onActivated() {}, /* 事件发生时Deactivated（onDeactivated）：注册缓存组件暂时离开时的清理处理。 */ onDeactivated() {}, /* 事件发生时变化前Unmount（onBeforeUnmount）：注册组件销毁前的清理，取消请求并释放定时器。 */ onBeforeUnmount(fn) { /* 调用 disposals.push 完成push处理。 */ disposals.push(fn); } },
    /* 接口成长助手工作区（../api/agentWorkspace）字段使用表达式 { workspaceApi: { get, post } } 的结果。 */
    "../api/agentWorkspace": { /* 工作区接口（workspaceApi）字段使用表达式 { get, post } 的结果。 */ workspaceApi: { /* 直接使用已有的读取（get）作为同名字段。 */ get, /* 直接使用已有的post作为同名字段。 */ post } },
    /* 接口HTTP请求（../api/http）字段使用表达式 { api: async () => ({}) } 的结果。 */
    "../api/http": { /* 接口（api）字段使用表达式 async () => ({}) 的结果。 */ api: async () => ({}) }, /* 接口成长业务（../api/growth）字段使用表达式 { canGrow: () => true } 的结果。 */ "../api/growth": { /* 个人业务权限检查（canGrow）字段使用表达式 () => true 的结果。 */ canGrow: () => true },
  });
  const /* 父组件传入的数据（props）保存创建页面可跟踪属性变化的状态对象。 */ props = Vue.reactive({ /* 对话编号（conversationId）字段使用固定文字 '11'。 */ conversationId: "11", /* 直接使用已有的正在处理（busy）作为同名字段。 */ busy, /* 刷新键（refreshKey）字段使用固定数值 0。 */ refreshKey: 0, /* 事件（event）字段使用空值，表示当前还没有对应内容。 */ event: null });
  const /* 数据范围（scope）保存调用 Vue.effectScope 完成effect数据范围处理。 */ scope = Vue.effectScope();
  const /* 当前状态（state）保存调用 scope.run 完成方案运行处理。 */ state = scope.run(() => module.useConversationPlan(props, () => {}));
  /* 等待调用 pause 完成暂停处理。 */
  await pause(); /* 等待调用 pause 完成暂停处理。 */ await pause();
  /* 返回调用 { props, state, close() { for (const fn of disposals) fn(); scope.stop 完成停止处理，当前函数到此结束。 */
  return { /* 直接使用已有的父组件传入的数据（props）作为同名字段。 */ props, /* 直接使用已有的当前状态（state）作为同名字段。 */ state, /* 关闭（close）：释放连接、文件或客户端占用的资源。 */ close() { /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */ for (const /* 声明 fn，稍后再填入处理结果。 */ fn of disposals) /* 调用 fn 完成fn处理。 */ fn(); /* 调用 scope.stop 完成停止处理。 */ scope.stop(); } };
}

/* 调用 test("acceptance binds the reviewed version, submits once, then verifies the receipt", async () => { const calls = []; let completed = false; const harness = await planHarness({ get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow("41", "11", completed ? "SUCCEEDED" : "WAITING_CONFIRM"), post: async (path, body) => { calls.push 完成push处理。 */
test("acceptance binds the reviewed version, submits once, then verifies the receipt", async () => {
  const /* 调用列表（calls）保存一个空列表，用来保存后续多条数据。 */ calls = []; let /* 已完成（completed）保存假，表示条件不成立或开关关闭。 */ completed = false;
  const /* harness保存等待 根据前面的条件，从两个候选结果中选择一个：planHarness({ get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflo… 完成后取得结果。 */ harness = await planHarness({
    /* 读取（get）字段使用根据前面的条件，从两个候选结果中选择一个：async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow("41", "11", compl…。 */
    get: async path => path === "planning/proposals" ? { /* 数据列表（items）字段使用表达式 [{ id: "41" }] 的结果。 */ items: [{ /* 编号（id）字段使用固定文字 '41'。 */ id: "41" }] } : workflow("41", "11", completed ? "SUCCEEDED" : "WAITING_CONFIRM"),
    /* post字段使用调用 async (path, body) => { calls.push 完成push处理。 */
    post: async (path, body) => { /* 调用 calls.push 完成push处理。 */ calls.push({ /* 直接使用已有的路径（path）作为同名字段。 */ path, /* 直接使用已有的请求正文（body）作为同名字段。 */ body }); /* 检查条件 path.endsWith("/execute") 是否成立，成立时走这一分支。 */ if (path.endsWith("/execute")) /* 把真，表示条件成立或开关打开写入 completed。 */ completed = true; },
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 等待处理（pending）保存先确认当前方案版本，再执行提交并重新核对回执。 */ pending = harness.state.accept();
    /* 等待先确认当前方案版本，再执行提交并重新核对回执。 */
    await harness.state.accept(); // 双击不能发出第二次提交。
    /* 更新本次处理状态：pending。 */
    await pending;
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls.length, 2);
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].path, "planning/proposals/41/confirm");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].body.planVersionId, "81");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[1].path, "planning/proposals/41/execute");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(harness.state.succeeded.value, true);
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 调用 test("busy chat cannot accept a plan and stale-version rejection never executes", async () => { const calls = []; const harness = await planHarness({ busy: true, get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow(), post: async path => { calls.push 完成push处理。 */
test("busy chat cannot accept a plan and stale-version rejection never executes", async () => {
  const /* 调用列表（calls）保存一个空列表，用来保存后续多条数据。 */ calls = [];
  const /* harness保存等待 根据前面的条件，从两个候选结果中选择一个：planHarness({ busy: true, get: async path => path === "planning/proposals" ? { items: [{ id: "41" }]… 完成后取得结果。 */ harness = await planHarness({ /* 正在处理（busy）字段使用真，表示条件成立或开关打开。 */ busy: true,
    /* 读取（get）字段使用根据前面的条件，从两个候选结果中选择一个：async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow()。 */
    get: async path => path === "planning/proposals" ? { /* 数据列表（items）字段使用表达式 [{ id: "41" }] 的结果。 */ items: [{ /* 编号（id）字段使用固定文字 '41'。 */ id: "41" }] } : workflow(),
    /* post字段使用调用 async path => { calls.push 完成push处理。 */
    post: async path => { /* 调用 calls.push 完成push处理。 */ calls.push(path); /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("方案已更新，请核对新版本"); },
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待先确认当前方案版本，再执行提交并重新核对回执。 */
    await harness.state.accept(); /* 调用 assert.equal 完成equal处理。 */ assert.equal(calls.length, 0);
    /* 把假，表示条件不成立或开关关闭写入 harness.props.busy。 */
    harness.props.busy = false; /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */ await Vue.nextTick();
    /* 等待先确认当前方案版本，再执行提交并重新核对回执。 */
    await harness.state.accept();
    /* 调用 assert.deepEqual 完成deepEqual处理。 */
    assert.deepEqual(calls, ["planning/proposals/41/confirm"]);
    /* 调用 assert.match 完成match处理。 */
    assert.match(harness.state.error.value, /新版本/);
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 判断文字是否具有指定后缀。 */
test("a delayed old-conversation request cannot replace the current plan", async () => {
  let /* 声明 finish旧（finishOld），稍后再填入处理结果。 */ finishOld;
  const /* harness保存等待 根据前面的条件，从两个候选结果中选择一个：planHarness({ get: async (path, params) => { if (path === "planning/proposals") return { items: [{ i… 完成后取得结果。 */ harness = await planHarness({
    /* 读取（get）字段使用根据前面的条件，从两个候选结果中选择一个：async (path, params) => { if (path === "planning/proposals") return { items: [{ id: params.conversat…。 */
    get: async (path, params) => {
      /* 检查条件 path === "planning/proposals" 是否成立，成立时走这一分支。 */
      if (path === "planning/proposals") /* 返回根据前面的条件，从两个候选结果中选择一个：{ items: [{ id: params.conversationId === "11" ? "41" : "42" }] }，当前函数到此结束。 */ return { /* 数据列表（items）字段使用根据前面的条件，从两个候选结果中选择一个：[{ id: params.conversationId === "11" ? "41" : "42" }]。 */ items: [{ /* 编号（id）字段使用根据前面的条件，从两个候选结果中选择一个：params.conversationId === "11" ? "41" : "42"。 */ id: params.conversationId === "11" ? "41" : "42" }] };
      /* 检查条件 path.endsWith("41") 是否成立，成立时走这一分支。 */
      if (path.endsWith("41")) /* 返回新建 Promise 实例，当前函数到此结束。 */ return new Promise(resolve => { /* 把resolve写入 finishOld。 */ finishOld = resolve; });
      /* 返回调用 workflow 完成规划工作流处理，当前函数到此结束。 */
      return workflow("42", "12");
    }, /* post字段使用表达式 async () => {} 的结果。 */ post: async () => {},
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把固定文字 '12'写入 harness.props.conversationId。 */
    harness.props.conversationId = "12"; /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */ await Vue.nextTick(); /* 等待调用 pause 完成暂停处理。 */ await pause(); /* 等待调用 pause 完成暂停处理。 */ await pause();
    /* 调用 finishOld 完成finish旧处理。 */
    finishOld(workflow("41", "11")); /* 等待调用 pause 完成暂停处理。 */ await pause();
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(harness.state.run.value.id, "42");
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 判断文字是否具有指定后缀。 */
test("failed creation is not reported as success", async () => {
  const /* harness保存等待 根据前面的条件，从两个候选结果中选择一个：planHarness({ get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflo… 完成后取得结果。 */ harness = await planHarness({
    /* 读取（get）字段使用根据前面的条件，从两个候选结果中选择一个：async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow()。 */
    get: async path => path === "planning/proposals" ? { /* 数据列表（items）字段使用表达式 [{ id: "41" }] 的结果。 */ items: [{ /* 编号（id）字段使用固定文字 '41'。 */ id: "41" }] } : workflow(),
    /* post字段使用判断文字是否具有指定后缀。 */
    post: async path => { /* 检查条件 path.endsWith("/execute") 是否成立，成立时走这一分支。 */ if (path.endsWith("/execute")) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("提交结果待核对"); },
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待先确认当前方案版本，再执行提交并重新核对回执。 */
    await harness.state.accept();
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(harness.state.succeeded.value, false);
    /* 调用 assert.match 完成match处理。 */
    assert.match(harness.state.error.value, /待核对/);
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 复盘Harness（reviewHarness）：调用 reviewHarness 完成复盘Harness处理。 */
async function reviewHarness({ /* 直接使用已有的读取（get）作为同名字段。 */ get, /* 直接使用已有的post作为同名字段。 */ post, /* 直接使用已有的正在处理不成立（busy = false）作为同名字段。 */ busy = false } = {}) {
  const /* disposals保存一个空列表，用来保存后续多条数据。 */ disposals = [], /* 事件列表（events）保存一个空列表，用来保存后续多条数据。 */ events = [];
  const /* module保存等待 调用 isolatedModule("../src/composables/useConversationReview.js", { vue: { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push 完成push处理 完成后取得结果。 */ module = await isolatedModule("../src/composables/useConversationReview.js", {
    /* vue字段使用调用 { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push 完成push处理。 */
    vue: { ...Vue, /* 事件发生时Activated（onActivated）：注册缓存组件重新显示时的处理。 */ onActivated() {}, /* 事件发生时Deactivated（onDeactivated）：注册缓存组件暂时离开时的清理处理。 */ onDeactivated() {}, /* 事件发生时变化前Unmount（onBeforeUnmount）：注册组件销毁前的清理，取消请求并释放定时器。 */ onBeforeUnmount(fn) { /* 调用 disposals.push 完成push处理。 */ disposals.push(fn); } },
    /* 接口成长助手工作区（../api/agentWorkspace）字段使用表达式 { workspaceApi: { get, post } } 的结果。 */
    "../api/agentWorkspace": { /* 工作区接口（workspaceApi）字段使用表达式 { get, post } 的结果。 */ workspaceApi: { /* 直接使用已有的读取（get）作为同名字段。 */ get, /* 直接使用已有的post作为同名字段。 */ post } },
    /* 接口HTTP请求（../api/http）字段使用表达式 { api: async () => ({ today: "2026-11-06" }) } 的结果。 */
    "../api/http": { /* 接口（api）字段使用表达式 async () => ({ today: "2026-11-06" }) 的结果。 */ api: async () => ({ /* 今天（today）字段使用固定文字 '2026-11-06'。 */ today: "2026-11-06" }) },
    /* 接口成长业务（../api/growth）字段使用表达式 { canGrow: () => true } 的结果。 */
    "../api/growth": { /* 个人业务权限检查（canGrow）字段使用表达式 () => true 的结果。 */ canGrow: () => true },
  });
  const /* 父组件传入的数据（props）保存创建页面可跟踪属性变化的状态对象。 */ props = Vue.reactive({ /* 对话编号（conversationId）字段使用固定文字 '11'。 */ conversationId: "11", /* 直接使用已有的正在处理（busy）作为同名字段。 */ busy, /* 事件（event）字段使用空值，表示当前还没有对应内容。 */ event: null, /* external编号（externalId）字段使用固定文字 ''。 */ externalId: "", /* 是否展开（open）字段使用假，表示条件不成立或开关关闭。 */ open: false });
  const /* 数据范围（scope）保存调用 Vue.effectScope 完成effect数据范围处理。 */ scope = Vue.effectScope();
  const /* 当前状态（state）保存调用 scope.run 完成方案运行处理。 */ state = scope.run(() => module.useConversationReview(props, (...event) => events.push(event)));
  /* 等待调用 pause 完成暂停处理。 */
  await pause(); /* 等待调用 pause 完成暂停处理。 */ await pause();
  /* 返回调用 { props, state, events, close() { for (const fn of disposals) fn(); scope.stop 完成停止处理，当前函数到此结束。 */
  return { /* 直接使用已有的父组件传入的数据（props）作为同名字段。 */ props, /* 直接使用已有的当前状态（state）作为同名字段。 */ state, /* 直接使用已有的事件列表（events）作为同名字段。 */ events, /* 关闭（close）：释放连接、文件或客户端占用的资源。 */ close() { /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */ for (const /* 声明 fn，稍后再填入处理结果。 */ fn of disposals) /* 调用 fn 完成fn处理。 */ fn(); /* 调用 scope.stop 完成停止处理。 */ scope.stop(); } };
}

/* 调用 test("review adjustment creates a draft once and never confirms it automatically", async () => { const calls = []; const harness = await reviewHarness({ get: async () => ({ review: { id: "9", status: "READY" } }), post: async (path, body) => { calls.push 完成push处理。 */
test("review adjustment creates a draft once and never confirms it automatically", async () => {
  const /* 调用列表（calls）保存一个空列表，用来保存后续多条数据。 */ calls = [];
  const /* harness保存等待 调用 reviewHarness({ get: async () => ({ review: { id: "9", status: "READY" } }), post: async (path, body) => { calls.push 完成push处理 完成后取得结果。 */ harness = await reviewHarness({
    /* 读取（get）字段使用表达式 async () => ({ review: { id: "9", status: "READY" } }) 的结果。 */
    get: async () => ({ /* 复盘（review）字段使用表达式 { id: "9", status: "READY" } 的结果。 */ review: { /* 编号（id）字段使用固定文字 '9'。 */ id: "9", /* 状态（status）字段使用固定状态或编码 READY（已就绪）。 */ status: "READY" } }),
    /* post字段使用调用 async (path, body) => { calls.push 完成push处理。 */
    post: async (path, body) => { /* 调用 calls.push 完成push处理。 */ calls.push({ /* 直接使用已有的路径（path）作为同名字段。 */ path, /* 直接使用已有的请求正文（body）作为同名字段。 */ body }); /* 等待调用 pause 完成暂停处理。 */ await pause(); /* 返回调用 workflow 完成规划工作流处理，当前函数到此结束。 */ return workflow(); },
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 等待处理（pending）保存调用 harness.state.adjust 完成adjust处理。 */ pending = harness.state.adjust();
    /* 等待调用 harness.state.adjust 完成adjust处理。 */
    await harness.state.adjust(); /* 更新本次处理状态：pending。 */ await pending;
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls.length, 1);
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].path, "reviews/9/proposals");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].body.conversationId, "11");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(harness.events.filter(event => event[0] === "adjusted").length, 1);
    /* 调用 assert.ok 完成ok处理。 */
    assert.ok(!calls.some(call => /confirm|execute/.test(call.path)));
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 调用 test("an old-conversation adjustment cannot reveal or replace the current draft", async () => { let complete; const harness = await reviewHarness({ get: async () => ({ review: { id: "9", status: "READY" } }), post: async () => new Promise(resolve => { complete = resolve; }), }); try { const pending = harness.state.adjust 完成adjust处理。 */
test("an old-conversation adjustment cannot reveal or replace the current draft", async () => {
  let /* 声明 完成（complete），稍后再填入处理结果。 */ complete;
  const /* harness保存等待 调用 reviewHarness 完成复盘Harness处理 完成后取得结果。 */ harness = await reviewHarness({
    /* 读取（get）字段使用表达式 async () => ({ review: { id: "9", status: "READY" } }) 的结果。 */
    get: async () => ({ /* 复盘（review）字段使用表达式 { id: "9", status: "READY" } 的结果。 */ review: { /* 编号（id）字段使用固定文字 '9'。 */ id: "9", /* 状态（status）字段使用固定状态或编码 READY（已就绪）。 */ status: "READY" } }),
    /* post字段使用表达式 async () => new Promise(resolve => { complete = resolve; }) 的结果。 */
    post: async () => new Promise(resolve => { /* 把resolve写入 complete。 */ complete = resolve; }),
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 等待处理（pending）保存调用 harness.state.adjust 完成adjust处理。 */ pending = harness.state.adjust();
    /* 把固定文字 '12'写入 harness.props.conversationId。 */
    harness.props.conversationId = "12"; /* 等待等待页面完成当前一轮渲染，再读取或调整界面。 */ await Vue.nextTick();
    /* 调用 complete 完成完成处理。 */
    complete(workflow()); /* 更新本次处理状态：pending。 */ await pending;
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(harness.events.filter(event => event[0] === "adjusted").length, 0);
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});

/* 调用 test("a pending review cannot produce an adjustment and new review dates use the account calendar", async () => { const calls = []; const harness = await reviewHarness({ get: async () => ({ review: { id: "9", status: "RUNNING" } }), post: async (path, body) => { calls.push 完成push处理。 */
test("a pending review cannot produce an adjustment and new review dates use the account calendar", async () => {
  const /* 调用列表（calls）保存一个空列表，用来保存后续多条数据。 */ calls = [];
  const /* harness保存等待 调用 reviewHarness({ get: async () => ({ review: { id: "9", status: "RUNNING" } }), post: async (path, body) => { calls.push 完成push处理 完成后取得结果。 */ harness = await reviewHarness({
    /* 读取（get）字段使用表达式 async () => ({ review: { id: "9", status: "RUNNING" } }) 的结果。 */
    get: async () => ({ /* 复盘（review）字段使用表达式 { id: "9", status: "RUNNING" } 的结果。 */ review: { /* 编号（id）字段使用固定文字 '9'。 */ id: "9", /* 状态（status）字段使用固定状态或编码 RUNNING（运行中）。 */ status: "RUNNING" } }),
    /* post字段使用调用 async (path, body) => { calls.push 完成push处理。 */
    post: async (path, body) => { /* 调用 calls.push 完成push处理。 */ calls.push({ /* 直接使用已有的路径（path）作为同名字段。 */ path, /* 直接使用已有的请求正文（body）作为同名字段。 */ body }); /* 返回表达式 { id: "10", status: "PENDING" } 的结果，当前函数到此结束。 */ return { /* 编号（id）字段使用固定文字 '10'。 */ id: "10", /* 状态（status）字段使用固定状态或编码 PENDING（等待处理）。 */ status: "PENDING" }; },
  });
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 harness.state.adjust 完成adjust处理。 */
    await harness.state.adjust(); /* 调用 assert.equal 完成equal处理。 */ assert.equal(calls.length, 0);
    /* 等待调用 harness.state.start 完成开始处理。 */
    await harness.state.start();
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].body.dateFrom, "2026-10-31");
    /* 调用 assert.equal 完成equal处理。 */
    assert.equal(calls[0].body.dateTo, "2026-11-06");
  } finally { /* 释放连接、文件或客户端占用的资源。 */ harness.close(); }
});
