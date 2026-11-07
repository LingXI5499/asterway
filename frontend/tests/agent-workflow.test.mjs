import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";
import * as Vue from "vue";

async function isolatedModule(path, imports, extra = "") {
  const source = await readFile(new URL(path, import.meta.url), "utf8");
  const context = vm.createContext({ TextDecoder, DOMException, AbortController, URLSearchParams, setTimeout, clearTimeout });
  const module = new vm.SourceTextModule(source + extra, { context });
  await module.link(async name => {
    const values = imports[name];
    assert.ok(values, `Unexpected import: ${name}`);
    return new vm.SyntheticModule(Object.keys(values), function () {
      for (const [key, value] of Object.entries(values)) this.setExport(key, value);
    }, { context });
  });
  await module.evaluate();
  return module.namespace;
}

class ApiError extends Error { constructor(message, status, code) { super(message); Object.assign(this, { status, code }); } }
const sessionBoundary = {
  ApiError, acceptSessionHeaders() {}, captureSessionRequest() { return {}; },
  checkSessionRequest() {}, rejectUnauthorized() {}, api() {},
};
const streamModule = await isolatedModule("../src/api/agent.js", { "./http": sessionBoundary }, "\nexport { readEventStream };\n");
function response(events) {
  const bytes = new TextEncoder().encode(events.map(([type, data]) => `event: ${type}\ndata: ${JSON.stringify(data)}\n\n`).join(""));
  return new Response(new ReadableStream({ start(controller) {
    // 切开 UTF-8 字符和 SSE 帧，覆盖逐字回复的真实边界。
    for (let index = 0; index < bytes.length; index += 7) controller.enqueue(bytes.slice(index, index + 7));
    controller.close();
  } }));
}

test("SSE streams text, execution summaries and workflow updates together", async () => {
  const received = [];
  await streamModule.readEventStream(response([
    ["message_start", { turnId: "31" }],
    ["activity", { id: "context", label: "读取上下文", status: "ok" }],
    ["tool_start", { id: "p1", name: "prepare_growth_plan", resetReply: true }],
    ["workflow_update", { runId: "41", conversationId: "11", phase: "WAITING_CONFIRM" }],
    ["review_update", { reviewId: "9", conversationId: "11", status: "READY" }],
    ["tool_end", { id: "p1", status: "ok", summary: "草案已就绪" }],
    ["delta", { text: "方案已整理，" }], ["delta", { text: "请核对。" }],
    ["message_end", { finishReason: "stop", complete: true }],
  ]), { context: {}, onDelta: text => received.push(text), onActivity: data => received.push(data.id),
    onWorkflow: data => received.push(data.phase), onReview: data => received.push(data.status) });
  assert.deepEqual(received, ["context", "WAITING_CONFIRM", "READY", "方案已整理，", "请核对。"]);
});

test("SSE refuses planning events before message_start and incomplete replies", async () => {
  await assert.rejects(streamModule.readEventStream(response([["workflow_update", { runId: "41" }]]), { context: {} }), { code: "INVALID_STREAM" });
  await assert.rejects(streamModule.readEventStream(response([["message_start", {}], ["delta", { text: "部分回复" }]]), { context: {} }), { code: "STREAM_INCOMPLETE" });
});

const pause = () => new Promise(resolve => setTimeout(resolve, 0));
const workflow = (id = "41", conversationId = "11", status = "WAITING_CONFIRM") => ({
  run: { id, conversationId, status, latestPlanVersionId: "81", progress: { phase: status } },
  plans: [{ id: "81", versionNo: 1, status: status === "WAITING_CONFIRM" ? status : "CONFIRMED", snapshot: {} }],
  commit: status === "SUCCEEDED" ? { status, result: {} } : null,
});
async function planHarness({ get, post, busy = false } = {}) {
  const disposals = [];
  const module = await isolatedModule("../src/composables/useConversationPlan.js", {
    vue: { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push(fn); } },
    "../api/agentWorkspace": { workspaceApi: { get, post } },
    "../api/http": { api: async () => ({}) }, "../api/growth": { canGrow: () => true },
  });
  const props = Vue.reactive({ conversationId: "11", busy, refreshKey: 0, event: null });
  const scope = Vue.effectScope();
  const state = scope.run(() => module.useConversationPlan(props, () => {}));
  await pause(); await pause();
  return { props, state, close() { for (const fn of disposals) fn(); scope.stop(); } };
}

test("acceptance binds the reviewed version, submits once, then verifies the receipt", async () => {
  const calls = []; let completed = false;
  const harness = await planHarness({
    get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow("41", "11", completed ? "SUCCEEDED" : "WAITING_CONFIRM"),
    post: async (path, body) => { calls.push({ path, body }); if (path.endsWith("/execute")) completed = true; },
  });
  try {
    const pending = harness.state.accept();
    await harness.state.accept(); // 双击不能发出第二次提交。
    await pending;
    assert.equal(calls.length, 2);
    assert.equal(calls[0].path, "planning/proposals/41/confirm");
    assert.equal(calls[0].body.planVersionId, "81");
    assert.equal(calls[1].path, "planning/proposals/41/execute");
    assert.equal(harness.state.succeeded.value, true);
  } finally { harness.close(); }
});

test("busy chat cannot accept a plan and stale-version rejection never executes", async () => {
  const calls = [];
  const harness = await planHarness({ busy: true,
    get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow(),
    post: async path => { calls.push(path); throw new Error("方案已更新，请核对新版本"); },
  });
  try {
    await harness.state.accept(); assert.equal(calls.length, 0);
    harness.props.busy = false; await Vue.nextTick();
    await harness.state.accept();
    assert.deepEqual(calls, ["planning/proposals/41/confirm"]);
    assert.match(harness.state.error.value, /新版本/);
  } finally { harness.close(); }
});

test("a delayed old-conversation request cannot replace the current plan", async () => {
  let finishOld;
  const harness = await planHarness({
    get: async (path, params) => {
      if (path === "planning/proposals") return { items: [{ id: params.conversationId === "11" ? "41" : "42" }] };
      if (path.endsWith("41")) return new Promise(resolve => { finishOld = resolve; });
      return workflow("42", "12");
    }, post: async () => {},
  });
  try {
    harness.props.conversationId = "12"; await Vue.nextTick(); await pause(); await pause();
    finishOld(workflow("41", "11")); await pause();
    assert.equal(harness.state.run.value.id, "42");
  } finally { harness.close(); }
});

test("failed creation is not reported as success", async () => {
  const harness = await planHarness({
    get: async path => path === "planning/proposals" ? { items: [{ id: "41" }] } : workflow(),
    post: async path => { if (path.endsWith("/execute")) throw new Error("提交结果待核对"); },
  });
  try {
    await harness.state.accept();
    assert.equal(harness.state.succeeded.value, false);
    assert.match(harness.state.error.value, /待核对/);
  } finally { harness.close(); }
});

async function reviewHarness({ get, post, busy = false } = {}) {
  const disposals = [], events = [];
  const module = await isolatedModule("../src/composables/useConversationReview.js", {
    vue: { ...Vue, onActivated() {}, onDeactivated() {}, onBeforeUnmount(fn) { disposals.push(fn); } },
    "../api/agentWorkspace": { workspaceApi: { get, post } },
    "../api/http": { api: async () => ({ today: "2026-11-06" }) },
    "../api/growth": { canGrow: () => true },
  });
  const props = Vue.reactive({ conversationId: "11", busy, event: null, externalId: "", open: false });
  const scope = Vue.effectScope();
  const state = scope.run(() => module.useConversationReview(props, (...event) => events.push(event)));
  await pause(); await pause();
  return { props, state, events, close() { for (const fn of disposals) fn(); scope.stop(); } };
}

test("review adjustment creates a draft once and never confirms it automatically", async () => {
  const calls = [];
  const harness = await reviewHarness({
    get: async () => ({ review: { id: "9", status: "READY" } }),
    post: async (path, body) => { calls.push({ path, body }); await pause(); return workflow(); },
  });
  try {
    const pending = harness.state.adjust();
    await harness.state.adjust(); await pending;
    assert.equal(calls.length, 1);
    assert.equal(calls[0].path, "reviews/9/proposals");
    assert.equal(calls[0].body.conversationId, "11");
    assert.equal(harness.events.filter(event => event[0] === "adjusted").length, 1);
    assert.ok(!calls.some(call => /confirm|execute/.test(call.path)));
  } finally { harness.close(); }
});

test("an old-conversation adjustment cannot reveal or replace the current draft", async () => {
  let complete;
  const harness = await reviewHarness({
    get: async () => ({ review: { id: "9", status: "READY" } }),
    post: async () => new Promise(resolve => { complete = resolve; }),
  });
  try {
    const pending = harness.state.adjust();
    harness.props.conversationId = "12"; await Vue.nextTick();
    complete(workflow()); await pending;
    assert.equal(harness.events.filter(event => event[0] === "adjusted").length, 0);
  } finally { harness.close(); }
});

test("a pending review cannot produce an adjustment and new review dates use the account calendar", async () => {
  const calls = [];
  const harness = await reviewHarness({
    get: async () => ({ review: { id: "9", status: "RUNNING" } }),
    post: async (path, body) => { calls.push({ path, body }); return { id: "10", status: "PENDING" }; },
  });
  try {
    await harness.state.adjust(); assert.equal(calls.length, 0);
    await harness.state.start();
    assert.equal(calls[0].body.dateFrom, "2026-10-31");
    assert.equal(calls[0].body.dateTo, "2026-11-06");
  } finally { harness.close(); }
});
