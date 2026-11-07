import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, watch } from "vue";
import { workspaceApi } from "../api/agentWorkspace";
import { api } from "../api/http";
import { canGrow } from "../api/growth";

export function useConversationReview(props, emit) {
  const report = ref(null), error = ref(""), acting = ref(false), action = ref("");
  const canReview = computed(() => canGrow("agent:review:manage"));
  const canAdjust = computed(() => canGrow("agent:workflow:manage") && report.value?.status === "READY");
  let sequence = 0, timer, controller, active = true, disposed = false;
  function stop() { clearTimeout(timer); timer = undefined; }
  function schedule() {
    stop();
    if (active && !disposed && props.open && ["PENDING", "RUNNING"].includes(report.value?.status)) timer = setTimeout(() => load(report.value.id), 2000);
  }
  async function load(id = null) {
    if (!canGrow("agent:review:read") || !active || disposed) return;
    const ticket = ++sequence, conversationId = props.conversationId;
    controller?.abort(); controller = new AbortController();
    try {
      const data = id ? await workspaceApi.get(`reviews/${id}`, {}, controller.signal)
        : conversationId ? (await workspaceApi.get(`reviews/latest/conversation/${conversationId}`, {}, controller.signal)).review : null;
      if (ticket !== sequence || disposed || props.conversationId !== conversationId) return;
      report.value = data; error.value = "";
    } catch (e) { if (ticket === sequence && e.name !== "AbortError" && e.code !== "SESSION_CHANGED") error.value = e.message || "复盘暂未读取成功"; }
    finally { if (ticket === sequence) schedule(); }
  }
  async function operate(kind, work) {
    if (acting.value || props.busy || !props.conversationId) return;
    const conversationId = props.conversationId;
    acting.value = true; action.value = kind; error.value = ""; emit("working", true);
    try {
      const result = await work(conversationId);
      if (props.conversationId !== conversationId || disposed) return;
      if (kind === "adjust") emit("adjusted", result);
      else { report.value = result; schedule(); emit("reveal"); }
    } catch (e) { if (props.conversationId === conversationId) error.value = e.message || "操作未完成，请重试"; }
    finally { acting.value = false; action.value = ""; emit("working", false); }
  }
  async function start() {
    if (!canReview.value) return;
    await operate("review", async conversationId => {
      const calendar = await api("/dashboard/overview");
      if (!calendar.today) throw new Error("暂时无法取得账号日期，请稍后重试");
      const end = calendar.today, begin = new Date(`${end}T12:00:00Z`);
      begin.setUTCDate(begin.getUTCDate() - 6);
      return workspaceApi.post("reviews", { dateFrom: begin.toISOString().slice(0, 10), dateTo: end,
        conversationId, ...(props.sourceRunId ? { sourceRunId: props.sourceRunId } : {}) });
    });
  }
  async function retry() {
    if (canReview.value && report.value?.status === "FAILED") await operate("retry", () => workspaceApi.post(`reviews/${report.value.id}/retry`));
  }
  async function adjust() {
    if (canAdjust.value) await operate("adjust", conversationId => workspaceApi.post(`reviews/${report.value.id}/proposals`, { conversationId }));
  }
  watch(() => props.conversationId, () => { ++sequence; stop(); report.value = null; error.value = ""; load(props.externalId || null); }, { immediate: true });
  watch(() => props.externalId, id => { if (id) { load(id); emit("reveal"); } });
  watch(() => props.event, event => {
    if (event && String(event.conversationId) === String(props.conversationId)) { load(event.reviewId); emit("reveal"); }
  });
  watch(() => props.open, open => { if (open) { if (report.value) load(report.value.id); else load(props.externalId || null); } else stop(); });
  onActivated(() => { active = true; load(report.value?.id || props.externalId || null); });
  onDeactivated(() => { active = false; ++sequence; controller?.abort(); stop(); });
  onBeforeUnmount(() => { disposed = true; ++sequence; controller?.abort(); stop(); });
  return { report, error, acting, action, canReview, canAdjust, start, retry, adjust, load };
}
