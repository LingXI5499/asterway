<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from "vue";
import { canGrow, validDate } from "../../api/growth";
import { executionApi, duration } from "../../api/personal";
import { execution } from "../../stores/personal";
import { usePaged } from "../../composables/usePaged";
import StateHistory from "./StateHistory.vue";
import ExecutionDetails from "./ExecutionDetails.vue";
import ExecutionTable from "./ExecutionTable.vue";
import PagePagination from "./PagePagination.vue";
const props = defineProps({ kind: String, id: [String, Number] });
const dates = ref([]),
  appliedDates = ref([]),
  summary = ref(null),
  error = ref(""),
  busy = ref(false),
  historyOpen = ref(false),
  detailOpen = ref(false),
  executionId = ref("");
let sequence = 0,
  applied = {},
  controller;
const records = usePaged(
  (params, signal) =>
    executionApi.taskRecords(props.id, { ...applied, ...params }, signal),
  { immediate: false },
);
async function load() {
  if (!props.id) return;
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  records.cancel();
  busy.value = true;
  error.value = "";
  try {
    const values = dates.value || [];
    if (
      values.length &&
      (values.length !== 2 || !values.every(validDate) || values[0] > values[1])
    )
      throw new Error("请选择完整且顺序正确的日期区间");
    const params = { dateFrom: values[0], dateTo: values[1] };
    if (canGrow("execution:session:read")) {
      const result = await executionApi.scopeSummary(
        props.kind,
        props.id,
        params,
        controller.signal,
      );
      if (seq !== sequence) return;
      summary.value = result;
      appliedDates.value = [...values];
    }
    applied = params;
    if (props.kind === "task" && canGrow("execution:session:list"))
      await records.load(true);
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
function details(id) {
  executionId.value = id;
  detailOpen.value = true;
}
watch(() => [props.kind, props.id], load, { immediate: true });
watch(() => execution.revision, load);
const changed = (event) => {
  if (["growth", "goal"].includes(event.detail)) load();
};
onMounted(() => window.addEventListener("asterway:personal-change", changed));
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
  window.removeEventListener("asterway:personal-change", changed);
});
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <h2>实际投入与状态历史</h2>
      <el-button
        v-if="canGrow(kind + ':' + kind + ':read')"
        @click="historyOpen = true"
        >查看状态历史</el-button
      >
    </div>
    <template v-if="canGrow('execution:session:read')">
      <div class="toolbar">
        <el-date-picker
          v-model="dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="投入起始日期"
          end-placeholder="投入结束日期"
        /><el-button :loading="busy" @click="load">查询投入</el-button
        ><el-button
          @click="
            dates = [];
            load();
          "
          >重置为今天</el-button
        >
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <div v-if="summary" class="metrics-grid">
        <article class="metric inset-metric">
          <span>累计实际投入</span
          ><strong>{{ duration(summary.totalActualSeconds) }}</strong>
        </article>
        <article class="metric inset-metric">
          <span
            >{{
              appliedDates.length ? appliedDates.join(" 至 ") : "今日"
            }}实际投入</span
          ><strong>{{ duration(summary.rangeActualSeconds) }}</strong>
        </article>
        <article class="metric inset-metric">
          <span>区间参与会话</span><strong>{{ summary.rangeSessions }}</strong>
        </article>
      </div>
      <p class="hint">
        暂停间隔不计时，逻辑删除任务仍保留真实投入；耗时不等于任务完成状态。
      </p>
    </template>
    <template v-if="kind === 'task' && canGrow('execution:session:list')">
      <h3>任务执行记录</h3>
      <el-alert
        v-if="records.error"
        :title="records.error"
        type="error"
        :closable="false"
      />
      <ExecutionTable
        :rows="records.rows"
        :loading="records.busy"
        controls
        @detail="details"
        @changed="load"
      /><PagePagination :state="records" @change="records.load" />
    </template>
  </section>
  <StateHistory v-model="historyOpen" :kind="kind" :id="id" />
  <ExecutionDetails v-model="detailOpen" :id="executionId" @changed="load" />
</template>
