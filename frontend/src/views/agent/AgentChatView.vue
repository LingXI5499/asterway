<script setup>
import {
  computed,
  inject,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import AgentWorkspaceNav from "../../components/agent/AgentWorkspaceNav.vue";
import AgentPlanWorkspace from "../../components/agent/AgentPlanWorkspace.vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { agentApi, streamConversationMessage } from "../../api/agent";
import { authSessionKey } from "../../api/http";
import { canGrow } from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import AgentConversationList from "../../components/agent/AgentConversationList.vue";
import AgentConversationPane from "../../components/agent/AgentConversationPane.vue";

const route = useRoute();
const router = useRouter();
const sidebar = inject("workspaceSidebar", { toggle() {}, close() {} });
const resourceLinks = [
  ["/agent/memories", "agent:memory:read"], ["/agent/knowledge", "agent:knowledge:read"],
  ["/agent/runs", "agent:run:read"], ["/agent/reviews", "agent:review:read"],
];
const firstResource = computed(() => resourceLinks.find(([, permission]) => canGrow(permission))?.[0]);
const resourcesOpen = computed(() => route.path.startsWith("/agent/"));
function closeResources() { router.push({ path: "/agent", query: route.query.conversationId ? { conversationId: route.query.conversationId } : {} }); }
const TITLE_DEFAULT = "新对话";
const STORAGE_KEY = "asterway.agent.conversation";
const LIST_SIZE = 30;
const MESSAGE_SIZE = 24;
const RECOVERY_DELAY = 2000;

const canSend = computed(() => canGrow("agent:chat:send"));
const canCreate = computed(() => canGrow("agent:conversation:create"));
const canList = computed(() => canGrow("agent:conversation:list"));
const canUpdate = computed(() => canGrow("agent:conversation:update"));
const canRemove = computed(() => canGrow("agent:conversation:delete"));

const list = reactive({
  rows: [],
  total: 0,
  page: 1,
  size: LIST_SIZE,
  hasMore: false,
  busy: false,
  error: "",
  loaded: false,
});
const filters = reactive({ keyword: "" });
const activeId = ref("");
const conversation = ref(null);
const messages = ref([]);
const hasMore = ref(false);
const nextBefore = ref(null);
const loadingConversation = ref(false);
const olderBusy = ref(false);
const sending = ref(false);
const generating = ref(false);
const creating = ref(false);
const recovering = ref(false);
const recoveryTurn = ref(null);
const draft = ref("");
const pane = ref(null);
const planOpen = ref(false), planAvailable = ref(false), planEvent = ref(null), planRefreshKey = ref(0);
const reviewEvent = ref(null), reviewWorking = ref(false);
const externalReviewId = computed(() => /^\d+$/.test(String(route.query.reviewId || '')) ? String(route.query.reviewId) : '');
const planWidth = ref(400), resizingPlan = ref(false), planLayout = ref(null);
try { const saved = Number(localStorage.getItem("asterway.ui.planWidth")); if (saved >= 320 && saved <= 720) planWidth.value = saved; } catch {}
let planDrag = null;
function savePlanWidth() { try { localStorage.setItem("asterway.ui.planWidth", String(planWidth.value)); } catch {} }
function planWidthLimit() { return Math.max(320, Math.min(720, (planLayout.value?.clientWidth || 1080) - 360)); }
function planResizeStart(event) {
  if (event.button !== 0 || window.matchMedia('(max-width: 1199px)').matches) return;
  const actual = planLayout.value?.querySelector('.agent-plan-workspace')?.getBoundingClientRect().width || planWidth.value;
  planDrag = { pointer: event.pointerId, x: event.clientX, width: actual };
  event.currentTarget.setPointerCapture(event.pointerId); resizingPlan.value = true; event.preventDefault();
}
function planResizeMove(event) {
  if (!planDrag || planDrag.pointer !== event.pointerId) return;
  planWidth.value = Math.round(Math.max(320, Math.min(planWidthLimit(), planDrag.width + planDrag.x - event.clientX)));
}
function planResizeEnd() { if (planDrag) { planDrag = null; resizingPlan.value = false; savePlanWidth(); } }
function planResizeKey(event) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End', 'Enter'].includes(event.key)) return;
  event.preventDefault();
  const step = event.shiftKey ? 32 : 16;
  planWidth.value = event.key === 'Home' ? 320 : event.key === 'End' ? planWidthLimit() : event.key === 'Enter' ? Math.min(400, planWidthLimit())
    : Math.max(320, Math.min(planWidthLimit(), planWidth.value + (event.key === 'ArrowLeft' ? step : -step)));
  savePlanWidth();
}
function resetPlanWidth() { planWidth.value = Math.min(400, planWidthLimit()); savePlanWidth(); }
let presentedPlanId = "";
function planAvailability(info) {
  planAvailable.value = info.available;
  if (info.available && info.id !== presentedPlanId && ["DRAFT", "WAITING_CONFIRM", "CONFIRMED", "EXECUTING", "FAILED"].includes(info.status)) {
    presentedPlanId = info.id;
    planOpen.value = true;
  }
}
async function revisePlan(text) { draft.value = text; await nextTick(); pane.value?.focus(); }
watch(activeId, () => { planOpen.value = !!externalReviewId.value; planAvailable.value = false; planEvent.value = null; reviewEvent.value = null; presentedPlanId = ""; });
watch(externalReviewId, id => { if (id) planOpen.value = true; }, { immediate: true });

let disposed = false;
let listSequence = 0;
let listController = null;
let openSequence = 0;
let streamController = null;
let activeTurn = null;
let recoveryTimer = 0;

function filtered() {
  return !!filters.keyword.trim();
}
function titleFrom(text) {
  const clean = text.replace(/\s+/g, " ").trim();
  return Array.from(clean).slice(0, 24).join("") || TITLE_DEFAULT;
}
function toRow(detail) {
  return {
    id: String(detail.id),
    title: detail.title,
    lastMessageAt: detail.lastMessageAt || null,
    version: Number(detail.version) || 0,
  };
}
function toMessage(row) {
  return {
    id: String(row.id),
    messageNo: Number(row.messageNo) || 0,
    role: row.role,
    content: row.content || "",
    // Java 公开 VO 字段是 complete；Python 内部的 isComplete 不对外暴露。
    complete: row.complete === true,
    finishReason: row.finishReason || "",
    model: row.model || "",
    usage: row.usage || null,
    citations: row.citations || [],
    usedMemoryIds: row.usedMemoryIds || [],
    createdAt: row.createdAt || "",
    pending: false,
    stopped: false,
    error: "",
    traceId: "",
    tools: [],
    activities: [],
  };
}
function mergeRows(existing, incoming) {
  const seen = new Set(existing.map((row) => row.id));
  return [...existing, ...incoming.filter((row) => !seen.has(row.id))];
}
function updateRow(detail) {
  if (!detail) return;
  const row = toRow(detail);
  const index = list.rows.findIndex((item) => item.id === row.id);
  if (index < 0) return;
  list.rows.splice(index, 1);
  list.rows.splice(filtered() ? index : 0, 0, row);
}
function addCreatedRow(detail) {
  const row = toRow(detail);
  if (list.rows.some((item) => item.id === row.id) || filtered()) return;
  list.rows.unshift(row);
  list.total += 1;
  list.hasMore = list.rows.length < list.total;
}
function activeRow() {
  const id = activeId.value;
  return (
    list.rows.find((row) => row.id === id) ||
    (conversation.value ? toRow(conversation.value) : null)
  );
}

const turns = computed(() => {
  const result = [];
  let current = null;
  for (const message of messages.value) {
    if (message.role === "USER") {
      current = { id: message.id, question: message.content, answer: null };
      result.push(current);
      continue;
    }
    if (message.role === "ASSISTANT" && current && !current.answer) {
      current.answer = message;
      current = null;
    }
  }
  return result;
});


async function loadConversations({ reset = false } = {}) {
  if (!canList.value) {
    list.error = "当前账号尚未开通会话列表权限";
    list.loaded = true;
    return;
  }
  const request = ++listSequence;
  const page = reset ? 1 : list.page;
  listController?.abort();
  listController = new AbortController();
  if (reset) list.page = 1;
  list.busy = true;
  list.error = "";
  try {
    const data = await agentApi.conversations(
      {
        page,
        size: list.size,
        keyword: filters.keyword.trim(),
      },
      listController.signal,
    );
    if (disposed || request !== listSequence) return;
    const items = Array.isArray(data.items) ? data.items : [];
    list.rows = reset ? items : mergeRows(list.rows, items);
    list.total = Number(data.total) || 0;
    list.hasMore = list.rows.length < list.total;
    list.loaded = true;
  } catch (error) {
    if (disposed || request !== listSequence) return;
    if (error.name === "AbortError" || error.code === "SESSION_CHANGED") return;
    if (!reset && page > 1 && list.page === page) list.page = page - 1;
    list.error =
      error.status === 403
        ? "当前账号尚未开通会话列表权限"
        : error.message || "会话列表加载失败，请重试";
    list.loaded = true;
  } finally {
    if (!disposed && request === listSequence) list.busy = false;
  }
}
async function loadMore() {
  if (list.busy || !list.hasMore || !canList.value) return;
  list.page += 1;
  await loadConversations();
}

function clearActive() {
  openSequence += 1;
  stopRecovery();
  streamController?.abort();
  streamController = null;
  activeTurn = null;
  activeId.value = "";
  conversation.value = null;
  messages.value = [];
  hasMore.value = false;
  nextBefore.value = null;
  loadingConversation.value = false;
  olderBusy.value = false;
  sessionStorage.removeItem(STORAGE_KEY);
}

async function openConversation(id) {
  const value = String(id || "");
  if (!value) return;
  const request = ++openSequence;
  stopRecovery();
  activeId.value = value;
  conversation.value = null;
  messages.value = [];
  hasMore.value = false;
  nextBefore.value = null;
  loadingConversation.value = true;
  sessionStorage.setItem(STORAGE_KEY, value);
  try {
    const [detail, page] = await Promise.all([
      agentApi.conversation(value),
      agentApi.messages(value, { size: MESSAGE_SIZE }),
    ]);
    if (disposed || request !== openSequence) return;
    conversation.value = detail;
    messages.value = (page.items || []).map(toMessage);
    hasMore.value = page.hasMore === true;
    nextBefore.value = page.nextBeforeMessageNo ?? null;
    if (detail.runningTurn?.status === "RUNNING")
      scheduleRecovery(detail.runningTurn);
  } catch (error) {
    if (disposed || request !== openSequence) return;
    if (error.status === 404) {
      ElMessage.info("会话不存在或已被删除");
      clearActive();
      await loadConversations({ reset: true });
      return;
    }
    if (error.status === 403) ElMessage.error("当前账号没有读取该会话的权限");
    else report(error);
    clearActive();
  } finally {
    if (!disposed && request === openSequence)
      loadingConversation.value = false;
  }
}
async function selectConversation(id) {
  const value = String(id || "");
  sidebar.close();
  if (!value || value === activeId.value) return;
  if (sending.value) {
    ElMessage.info("请先停止当前生成，再切换会话");
    return;
  }
  await openConversation(value);
}
async function refreshActive({ recover = true } = {}) {
  const id = activeId.value;
  if (!id || disposed) return null;
  try {
    const detail = await agentApi.conversation(id);
    if (disposed || activeId.value !== id) return null;
    conversation.value = detail;
    updateRow(detail);
    if (
      recover &&
      detail.runningTurn?.status === "RUNNING" &&
      !generating.value
    )
      scheduleRecovery(detail.runningTurn);
    return detail;
  } catch (error) {
    if (disposed || activeId.value !== id) return null;
    if (error.status === 404) {
      ElMessage.info("会话不存在或已被删除");
      clearActive();
      await loadConversations({ reset: true });
    }
    return null;
  }
}

async function loadOlderMessages() {
  const id = activeId.value;
  if (!id || olderBusy.value || !hasMore.value || loadingConversation.value)
    return;
  olderBusy.value = true;
  try {
    const page = await agentApi.messages(id, {
      beforeMessageNo: nextBefore.value,
      size: MESSAGE_SIZE,
    });
    if (disposed || activeId.value !== id) return;
    const seen = new Set(messages.value.map((message) => message.id));
    const older = (page.items || [])
      .map(toMessage)
      .filter((message) => !seen.has(message.id));
    messages.value = [...older, ...messages.value];
    hasMore.value = page.hasMore === true;
    nextBefore.value = page.nextBeforeMessageNo ?? null;
  } catch (error) {
    if (!disposed && error.status !== 401) report(error);
  } finally {
    if (!disposed) olderBusy.value = false;
  }
}
async function refreshLatestMessages() {
  const id = activeId.value;
  if (!id) return;
  try {
    const page = await agentApi.messages(id, { size: MESSAGE_SIZE });
    if (disposed || activeId.value !== id) return;
    const previousAnswer = messages.value.filter(item => item.role === "ASSISTANT").at(-1);
    const incoming = (page.items || []).map(row => {
      const item = toMessage(row);
      const previous = messages.value.find(old => old.id === item.id)
        || (item.role === "ASSISTANT" && row === page.items?.at(-1) ? previousAnswer : null);
      if (previous) { item.tools = previous.tools || []; item.activities = previous.activities || []; }
      return item;
    });
    const ids = new Set(incoming.map((message) => message.id));
    const kept = messages.value.filter(
      (message) => !message.id.startsWith("local-") && !ids.has(message.id),
    );
    messages.value = [...kept, ...incoming].sort(
      (left, right) => left.messageNo - right.messageNo,
    );
  } catch {
    // 最新页回读失败不影响已展示的流式正文。
  }
}

function stopRecovery() {
  if (recoveryTimer) clearTimeout(recoveryTimer);
  recoveryTimer = 0;
  recovering.value = false;
  recoveryTurn.value = null;
}
function announceRecovery(turn) {
  const messagesByStatus = {
    COMPLETED: ["success", "上一轮回复已完成"],
    CANCELLED: ["warning", "上一轮回复已停止"],
    FAILED: ["warning", "上一轮回复生成失败，已保留部分内容"],
    INCOMPLETE: ["warning", "上一轮回复未完整结束"],
  };
  const item = messagesByStatus[turn?.status];
  if (item) ElMessage({ type: item[0], message: item[1] });
}
function scheduleRecovery(turn) {
  stopRecovery();
  if (!turn || turn.status !== "RUNNING" || disposed) return;
  recovering.value = true;
  recoveryTurn.value = turn;
  const id = activeId.value;
  const tick = async () => {
    recoveryTimer = 0;
    if (disposed || !recovering.value) return;
    if (!id || activeId.value !== id) return stopRecovery();
    try {
      const latest = await agentApi.turn(id, turn.id);
      if (disposed || !recovering.value || activeId.value !== id) return;
      recoveryTurn.value = latest;
      if (latest.status === "RUNNING") {
        recoveryTimer = setTimeout(tick, RECOVERY_DELAY);
        return;
      }
      stopRecovery();
      await refreshLatestMessages();
      await refreshActive();
      announceRecovery(latest);
    } catch (error) {
      stopRecovery();
      if (!disposed && error.status !== 401) report(error);
    }
  };
  recoveryTimer = setTimeout(tick, 800);
}
async function stopRecoveryTurn() {
  const turn = recoveryTurn.value;
  const id = activeId.value;
  if (!turn || !id) return;
  stopRecovery();
  try {
    await agentApi.cancelTurn(id, turn.id, turn.version);
    ElMessage.info("已停止上一轮生成");
  } catch (error) {
    if (error.status !== 409) report(error);
  }
  await refreshLatestMessages();
  await refreshActive();
}

async function createConversation() {
  if (!canCreate.value) return;
  if (creating.value) return;
  if (sending.value) {
    ElMessage.info("请先停止当前生成，再新建会话");
    return;
  }
  clearActive();
  draft.value = "";
  sidebar.close();
  if (route.path !== "/agent" || route.query.conversationId) await router.push("/agent");
  await nextTick();
  pane.value?.focus();
}
async function renameConversation(row) {
  if (!row || !canUpdate.value) return;
  if (sending.value) return ElMessage.info("请先停止当前生成，再修改标题");
  let value = "";
  try {
    ({ value } = await ElMessageBox.prompt("请输入新的会话标题", "重命名会话", {
      confirmButtonText: "保存",
      cancelButtonText: "取消",
      inputValue: row.title,
      inputValidator(text) {
        const title = (text || "").trim();
        if (!title) return "标题不能为空";
        if (Array.from(title).length > 100) return "标题不能超过 100 字";
        return true;
      },
    }));
  } catch {
    return;
  }
  const title = (value || "").trim();
  if (title === row.title) return;
  try {
    const detail = await agentApi.renameConversation(
      row.id,
      title,
      row.version,
    );
    if (activeId.value === String(row.id)) conversation.value = detail;
    updateRow(detail);
    ElMessage.success("会话已重命名");
  } catch (error) {
    report(error);
    if (error.status === 409) await refreshActive();
  }
}
async function removeConversation(row) {
  if (!row || !canRemove.value) return;
  if (sending.value) return ElMessage.info("请先停止当前生成，再删除会话");
  try {
    await confirmAction(
      `确定删除会话「${row.title}」？删除后会从列表移除，历史消息仍保留在数据库中。`,
      "删除会话",
    );
  } catch {
    return;
  }
  try {
    await agentApi.deleteConversation(row.id, row.version);
    list.rows = list.rows.filter((item) => item.id !== String(row.id));
    list.total = Math.max(0, list.total - 1);
    list.hasMore = list.rows.length < list.total;
    if (activeId.value === String(row.id)) {
      clearActive();
      setDraft("");
    }
    ElMessage.success("会话已删除");
  } catch (error) {
    report(error);
    if (error.status === 409) {
      await refreshActive();
      await loadConversations({ reset: true });
    }
  }
}

function setDraft(value) {
  draft.value = value;
}
function stopStream() {
  const turn = activeTurn;
  const id = activeId.value;
  streamController?.abort();
  if (turn?.id && id && canSend.value)
    agentApi.cancelTurn(id, turn.id, turn.version).catch((error) => {
      if (!disposed && error.status !== 409) report(error);
    });
}
function handleStop() {
  if (recovering.value) stopRecoveryTurn();
  else stopStream();
}

async function send(question = draft.value) {
  if (reviewWorking.value) return ElMessage.info('正在整理复盘调整方案，请完成后继续对话');
  const text = (question || "").trim();
  if (!text || sending.value) return;
  if (!canSend.value) return ElMessage.warning("当前账号没有发送提问的权限");
  if (Array.from(text).length > 8000)
    return ElMessage.warning("提问不能超过 8000 字");
  if (recovering.value)
    return ElMessage.info("上一轮回复仍在生成，请先等待或停止生成");
  sending.value = true;
  try {
    let detail = conversation.value;
    if (!detail) {
      detail = await agentApi.createConversation(titleFrom(text));
      if (disposed) return;
      conversation.value = detail;
      activeId.value = String(detail.id);
      sessionStorage.setItem(STORAGE_KEY, activeId.value);
      if (filtered()) await loadConversations({ reset: true });
      else addCreatedRow(detail);
    } else if (detail.title === TITLE_DEFAULT && canUpdate.value) {
      try {
        detail = await agentApi.renameConversation(
          detail.id,
          titleFrom(text),
          detail.version,
        );
        if (activeId.value === String(detail.id)) conversation.value = detail;
        updateRow(detail);
      } catch {
        // 标题生成失败不阻断提问。
      }
    }

    const conversationId = activeId.value;
    const requestId = crypto.randomUUID();
    const now = new Date().toISOString();
    const questionMessage = reactive({
      id: "local-user-" + requestId,
      messageNo: Number.MAX_SAFE_INTEGER - 1,
      role: "USER",
      content: text,
      complete: true,
      finishReason: "",
      model: "",
      usage: null,
      createdAt: now,
      pending: false,
      stopped: false,
      error: "",
      traceId: "",
    });
    const answerMessage = reactive({
      id: "local-assistant-" + requestId,
      messageNo: Number.MAX_SAFE_INTEGER,
      role: "ASSISTANT",
      content: "",
      complete: false,
      finishReason: "",
      model: "",
      usage: null,
      createdAt: now,
      pending: true,
      stopped: false,
      error: "",
      traceId: "",
      tools: [],
      activities: [],
    });
    messages.value.push(questionMessage, answerMessage);
    if (draft.value.trim() === text) draft.value = "";

    generating.value = true;
    const controller = new AbortController();
    const turn = { id: "", version: 0, requestId };
    let ended = false;
    streamController = controller;
    activeTurn = turn;
    try {
      await streamConversationMessage(
        conversationId,
        { message: text, requestId },
        {
          signal: controller.signal,
          onStart(data) {
            turn.id = data.turnId || "";
            turn.version = Number(data.turnVersion) || 0;
            answerMessage.model = data.model || "";
          },
          onTool(event, data) {
            if (!Array.isArray(answerMessage.tools)) answerMessage.tools = [];
            if (event === "tool_start") {
              // 查询轮中的过渡说明不是最终回答；与新的助手占位保持一致。
              if (data.resetReply === true) answerMessage.content = "";
              answerMessage.tools.push({
                id: data.id || "",
                name: data.name || "",
                label: data.label || data.name || "业务数据",
                status: "running",
              });
              return;
            }
            const running = [...answerMessage.tools]
              .reverse()
              .find(
                (tool) =>
                  tool.status === "running" &&
                  (!data.id || tool.id === data.id) &&
                  (!data.name || tool.name === data.name),
              );
            if (running) {
              running.status = ['ok', 'limited'].includes(data.status) ? data.status : 'error';
              running.durationMs = Number(data.durationMs) || 0;
              running.summary = data.summary || "";
            }
          },
          onActivity(data) {
            const existing = answerMessage.activities.find(item => item.id === data.id);
            if (existing) Object.assign(existing, data);
            else answerMessage.activities.push({ ...data });
          },
          onWorkflow(data) {
            if (String(data.conversationId) !== conversationId) return;
            planEvent.value = { ...data };
            planOpen.value = true;
          },
          onReview(data) {
            if (String(data.conversationId) !== conversationId) return;
            reviewEvent.value = { ...data };
            planOpen.value = true;
          },
          onDelta(delta) {
            answerMessage.content += delta;
          },
          onEnd(data) {
            turn.version = Number(data.turnVersion) || turn.version;
            answerMessage.complete =
              data.complete === true && data.finishReason === "stop";
            answerMessage.finishReason = data.finishReason || "";
            answerMessage.model = data.model || answerMessage.model;
            answerMessage.usage = data.usage || null;
            answerMessage.citations = data.citations || [];
            answerMessage.usedMemoryIds = data.usedMemoryIds || [];
            answerMessage.pending = false;
            ended = true;
          },
        },
      );
    } catch (error) {
      answerMessage.pending = false;
      if (
        controller.signal.aborted ||
        error.name === "AbortError" ||
        error.code === "TURN_CANCELLED"
      ) {
        answerMessage.stopped = true;
      } else {
        answerMessage.error = error.message || "提问失败，请稍后重试";
        answerMessage.traceId = error.traceId || "";
        if (error.status === 409) await refreshActive();
      }
    } finally {
      answerMessage.pending = false;
      for (const tool of answerMessage.tools || []) {
        if (tool.status === "running")
          tool.status = answerMessage.stopped ? "cancelled" : "error";
      }
      for (const activity of answerMessage.activities || []) {
        if (activity.status === "running") activity.status = answerMessage.stopped ? "cancelled" : "error";
      }
      generating.value = false;
      if (streamController === controller) streamController = null;
      if (activeTurn === turn) activeTurn = null;
      const aborted = controller.signal.aborted;
      await refreshActive({ recover: !aborted });
      if (ended && activeId.value === conversationId)
        await refreshLatestMessages();
      if (activeId.value === conversationId) planRefreshKey.value++;
    }
  } catch (error) {
    if (!disposed) report(error);
  } finally {
    sending.value = false;
  }
}

function resetForSession() {
  listSequence += 1;
  openSequence += 1;
  listController?.abort();
  listController = null;
  clearActive();
  list.rows = [];
  list.total = 0;
  list.page = 1;
  list.hasMore = false;
  list.loaded = false;
  list.error = "";
  list.busy = false;
  filters.keyword = "";
  draft.value = "";
  if (!disposed) loadConversations({ reset: true });
}

watch(authSessionKey, resetForSession);
watch(() => route.query.conversationId, (id) => {
  if (id && String(id) !== activeId.value) selectConversation(id);
});

onMounted(async () => {
  await loadConversations({ reset: true });
  if (disposed || !canList.value) return;
  const remembered = sessionStorage.getItem(STORAGE_KEY);
  const target =
    route.query.conversationId || list.rows.find((row) => row.id === remembered)?.id;
  if (target) await openConversation(target);
  else if (externalReviewId.value && canCreate.value) {
    const created = await agentApi.createConversation('复盘与计划调整');
    await loadConversations({ reset: true });
    await openConversation(created.id);
    planOpen.value = true;
  }
});

onBeforeUnmount(() => {
  disposed = true;
  listSequence += 1;
  openSequence += 1;
  listController?.abort();
  stopRecovery();
  if (streamController) {
    const turn = activeTurn;
    const id = activeId.value;
    streamController.abort();
    if (turn?.id && id)
      agentApi.cancelTurn(id, turn.id, turn.version).catch(() => {});
  }
});
</script>

<template>
  <section ref="planLayout" class="agent-page" :style="{ '--agent-plan-width': planWidth + 'px' }" :class="{ 'has-plan-panel': planOpen, 'is-resizing-plan': resizingPlan }" aria-label="成长助手会话">
    <Teleport defer to="#assistant-new-chat">
      <button v-if="canCreate" type="button" class="sidebar-new-chat" :disabled="sending || generating" @click="createConversation"><AppIcon name="compose" :size="18" /><span>新对话</span><AppIcon name="plus" :size="16" /></button>
    </Teleport>
    <Teleport defer to="#assistant-conversations">
      <AgentConversationList
        v-model:keyword="filters.keyword"
        :rows="list.rows"
        :active-id="activeId"
        :busy="list.busy"
        :error="list.error"
        :loaded="list.loaded"
        :has-more="list.hasMore"
        :total="list.total"
        :can-create="canCreate"
        :can-update="canUpdate"
        :can-delete="canRemove"
        @select="selectConversation"
        @create="createConversation"
        @refresh="loadConversations({ reset: true })"
        @load-more="loadMore"
        @rename="renameConversation"
        @remove="removeConversation"
      />
    </Teleport>
    <AgentConversationPane
      ref="pane"
      class="agent-pane"
      v-model:draft="draft"
      :conversation="conversation"
      :turns="turns"
      :loading="loadingConversation"
      :loading-older="olderBusy"
      :has-more="hasMore"
      :sending="sending"
      :busy="generating || reviewWorking"
      :recovering="recovering"
      :can-send="canSend"
      :can-create="canCreate"
      :can-update="canUpdate"
      :can-delete="canRemove"
      @send="send"
      @stop="handleStop"
      @retry="send"
      @load-older="loadOlderMessages"
      @rename="renameConversation(activeRow())"
      @remove="removeConversation(activeRow())"
      @create="createConversation"
      @toggle-list="sidebar.toggle"
    >
      <template #tools>
        <el-button v-if="canGrow('agent:workflow:read')" text :aria-expanded="planOpen" @click="planOpen = !planOpen"><AppIcon name="goal" :size="16" /><span>{{ planAvailable ? '方案与进度' : '成长方案' }}</span></el-button>
        <el-button v-if="firstResource" text class="agent-resource-trigger" @click="router.push(firstResource)"><AppIcon name="folder" :size="16" /><span>资料与记录</span></el-button>
      </template>
    </AgentConversationPane>
    <button v-if="planOpen" type="button" class="plan-panel-backdrop" aria-label="收起方案面板" @click="planOpen = false"></button>
    <AgentPlanWorkspace v-if="canGrow('agent:workflow:read')" :conversation-id="activeId" :event="planEvent" :review-event="reviewEvent" :external-review-id="externalReviewId" :refresh-key="planRefreshKey" :busy="sending || generating || recovering" :open="planOpen" @close="planOpen = false" @available="planAvailability" @reveal="planOpen = true" @revise="revisePlan" @working="reviewWorking = $event">
      <template #resize><div class="plan-resize-handle" role="separator" aria-label="调整方案面板宽度" aria-orientation="vertical" aria-valuemin="320" :aria-valuemax="720" :aria-valuenow="planWidth" tabindex="0" title="拖动调整方案宽度，双击恢复默认" @pointerdown="planResizeStart" @pointermove="planResizeMove" @pointerup="planResizeEnd" @pointercancel="planResizeEnd" @lostpointercapture="planResizeEnd" @keydown="planResizeKey" @dblclick="resetPlanWidth"></div></template>
    </AgentPlanWorkspace>
    <el-drawer :model-value="resourcesOpen" class="agent-resource-drawer" title="助手空间" size="min(960px, 100vw)" append-to-body @close="closeResources">
      <AgentWorkspaceNav />
      <RouterView :key="route.path" />
    </el-drawer>
  </section>
</template>

<style scoped>
.agent-page {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  height: 100%;
  min-height: 0;
  min-width: 0;
}
.agent-page.has-plan-panel { grid-template-columns: minmax(0, 1fr) clamp(320px, var(--agent-plan-width), calc(100% - 360px)); }
.plan-resize-handle { position: absolute; left: 0; top: 0; bottom: 0; width: 8px; z-index: 4; touch-action: none; cursor: col-resize; }
.plan-resize-handle::after { content: ''; position: absolute; top: 0; bottom: 0; left: 0; width: 2px; background: transparent; }
.plan-resize-handle:hover::after, .plan-resize-handle:focus-visible::after, .is-resizing-plan .plan-resize-handle::after { background: var(--aw-brand); }
.is-resizing-plan, .is-resizing-plan * { cursor: col-resize !important; user-select: none !important; }
.plan-panel-backdrop { display: none; }
.agent-pane {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  background: var(--aw-bg);
}
@media (max-width: 1199px) {
  .plan-resize-handle { display: none; }
  .agent-page.has-plan-panel { grid-template-columns: minmax(0, 1fr); }
  .plan-panel-backdrop { display: block; position: absolute; inset: 0; z-index: 510; background: var(--aw-overlay); border: 0; }
}
</style>
