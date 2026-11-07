import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
import { workspaceApi } from "../api/agentWorkspace";
import { canGrow } from "../api/growth";

export function useConversationPlan(props, emit) {
  const detail = ref(null), live = ref(null), loading = ref(false), acting = ref(false);
  const error = ref(""), goals = ref([]), progressError = ref("");
  const tracking = ref(null);
  let sequence = 0, controller, timer, active = true, disposed = false;
  const plan = computed(() => detail.value?.plans?.find(item => String(item.id) === String(detail.value.run.latestPlanVersionId)));
  const run = computed(() => detail.value?.run || null);
  const commit = computed(() => detail.value?.commit || null);
  const phase = computed(() => live.value?.phase || run.value?.progress?.phase || run.value?.status || "EMPTY");
  const progress = computed(() => live.value || run.value?.progress || {});
  const succeeded = computed(() => run.value?.status === "SUCCEEDED" && commit.value?.status === "SUCCEEDED");
  const waiting = computed(() => run.value?.status === "WAITING_CONFIRM" && phase.value === "WAITING_CONFIRM");
  const canManage = computed(() => canGrow("agent:workflow:manage"));
  const canAccept = computed(() => canManage.value && waiting.value && !props.busy && !acting.value);
  const canRetry = computed(() => canManage.value && !succeeded.value && plan.value?.status === "CONFIRMED"
    && ["CONFIRMED", "EXECUTING", "FAILED"].includes(run.value?.status) && (commit.value?.attempts || 0) < 10);

  function stopPoll() { clearTimeout(timer); timer = undefined; }
  function schedulePoll() {
    stopPoll();
    if (!active || disposed || !run.value || !props.conversationId) return;
    const fast = ["CONTEXT", "GENERATING"].includes(phase.value) || ["CONFIRMED", "EXECUTING"].includes(run.value.status);
    if (fast || run.value.status === "WAITING_CONFIRM" || (succeeded.value && props.open !== false)) timer = setTimeout(() => load(), fast ? 2000 : 30000);
  }
  function announce() {
    emit("available", { available: !!run.value, id: run.value?.id, status: run.value?.status });
  }
  async function readProgress(data, ticket, signal) {
    progressError.value = "";
    if (data?.commit?.status !== "SUCCEEDED") { goals.value = []; tracking.value = null; return; }
    try {
      const state = await workspaceApi.get(`planning/proposals/${data.run.id}/progress`, {}, signal);
      if (ticket !== sequence || disposed) return;
      tracking.value = state;
      goals.value = state.groups?.goals || [];
      if (state.partial) progressError.value = state.notes?.join('；') || '当前展示部分跟踪对象，完成数量仅针对已加载范围。';
    } catch (e) {
      if (ticket === sequence && e.name !== 'AbortError') progressError.value = e.message || '实际进度暂未读取成功，请刷新重试。';
    }
  }
  async function load(id = null) {
    if (!active || disposed || !props.conversationId || !canGrow("agent:workflow:read")) return;
    const ticket = ++sequence, conversationId = props.conversationId;
    controller?.abort(); controller = new AbortController();
    const signal = controller.signal;
    loading.value = !detail.value;
    try {
      let runId = id;
      if (!runId) {
        const data = await workspaceApi.get("planning/proposals", { conversationId, size: 1 }, signal);
        runId = data.items?.[0]?.id;
      }
      const data = runId ? await workspaceApi.get(`planning/proposals/${runId}`, {}, signal) : null;
      if (ticket !== sequence || disposed || props.conversationId !== conversationId) return;
      if (data && String(data.run.conversationId) !== String(conversationId)) throw new Error("方案与当前对话不一致，请刷新重试");
      detail.value = data;
      live.value = null;
      error.value = "";
      announce();
      await readProgress(data, ticket, signal);
    } catch (e) {
      if (e.name !== "AbortError" && e.code !== "SESSION_CHANGED" && ticket === sequence) error.value = e.message || "方案加载失败";
    } finally {
      if (ticket === sequence) { loading.value = false; schedulePoll(); }
    }
  }
  async function accept() {
    if (!canAccept.value && (!canRetry.value || props.busy || acting.value)) return;
    const runId = run.value.id, planId = plan.value.id, conversationId = props.conversationId;
    const firstAccept = waiting.value;
    acting.value = true; error.value = "";
    try {
      if (firstAccept) {
        await workspaceApi.post(`planning/proposals/${runId}/confirm`, { planVersionId: planId });
        if (props.conversationId === conversationId) live.value = { ...progress.value, phase: "CONFIRMED" };
      }
      if (props.conversationId === conversationId) live.value = { ...progress.value, phase: "EXECUTING" };
      await workspaceApi.post(`planning/proposals/${runId}/execute`);
      if (props.conversationId === conversationId) await load(runId);
    } catch (e) {
      if (props.conversationId === conversationId) {
        const message = e.message || "创建结果尚未核实，请刷新查看";
        await load(runId);
        error.value = message;
      }
    } finally { acting.value = false; }
  }
  async function decline() {
    if (!run.value || props.busy || acting.value || !canManage.value) return;
    const runId = run.value.id, conversationId = props.conversationId;
    acting.value = true; error.value = "";
    try {
      await workspaceApi.post(`planning/proposals/${runId}/cancel`);
      if (props.conversationId === conversationId) await load(runId);
    } catch (e) { if (props.conversationId === conversationId) error.value = e.message; }
    finally { acting.value = false; }
  }
  watch(() => props.conversationId, () => {
    ++sequence; controller?.abort(); stopPoll(); detail.value = live.value = null;
    goals.value = []; tracking.value = null; error.value = progressError.value = ""; announce(); load();
  }, { immediate: true });
  watch(() => props.event, event => {
    if (!event || String(event.conversationId) !== String(props.conversationId)) return;
    live.value = event; error.value = "";
    emit("available", { available: true, id: event.runId, status: event.phase === "WAITING_CONFIRM" ? "WAITING_CONFIRM" : "DRAFT" });
    emit("reveal");
    if (["WAITING_CONFIRM", "ERROR", "INTERRUPTED"].includes(event.phase)) load(event.runId);
  });
  watch(() => props.refreshKey, () => load());
  watch(() => props.open, open => { if (open) load(); else if (succeeded.value) stopPoll(); });
  onActivated(() => { active = true; load(); });
  onDeactivated(() => { active = false; ++sequence; controller?.abort(); stopPoll(); });
  onBeforeUnmount(() => { disposed = true; ++sequence; controller?.abort(); stopPoll(); });
  return { detail, run, plan, commit, progress, phase, succeeded, loading, acting, error, goals,
    progressError, tracking, canManage, canAccept, canRetry, waiting, load, accept, decline };
}
