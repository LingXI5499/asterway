<script setup>
import { ref, watch, onMounted } from "vue";
import { useRouter } from "vue-router";
import { canGrow } from "../../api/growth";
import {
  executionApi,
  duration,
  instantText,
  canStartTask,
} from "../../api/personal";
import {
  execution,
  refreshExecution,
  elapsedSeconds,
  startExecution,
  controlExecution,
} from "../../stores/personal";
import { usePaged } from "../../composables/usePaged";
import { report } from "../../api/ui";
import ExecutionTaskPicker from "../../components/personal/ExecutionTaskPicker.vue";
import ExecutionTable from "../../components/personal/ExecutionTable.vue";
import ExecutionDetails from "../../components/personal/ExecutionDetails.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const router = useRouter(),
  selected = ref(null),
  picker = ref(null),
  summary = ref(null),
  summaryError = ref(""),
  show = ref(false),
  detailId = ref("");
const paused = usePaged((params, signal) =>
  executionApi.list({ ...params, status: "PAUSED" }, signal),
);
async function loadSummary() {
  if (!canGrow("execution:session:read")) return;
  try {
    summary.value = await executionApi.summary();
    summaryError.value = "";
  } catch (e) {
    summaryError.value = e.message;
  }
}
async function refresh() {
  await Promise.allSettled([
    refreshExecution(),
    paused.load(),
    loadSummary(),
    picker.value?.reload(),
  ]);
}
async function start() {
  try {
    await startExecution(selected.value);
    selected.value = null;
    picker.value?.reload();
  } catch (e) {
    report(e);
  }
}
async function control(action) {
  try {
    await controlExecution(action, execution.current);
  } catch (e) {
    report(e);
  }
}
function details(id) {
  detailId.value = id;
  show.value = true;
}
watch(
  () => execution.revision,
  () => {
    paused.load();
    loadSummary();
  },
);
onMounted(loadSummary);
</script>
<template>
  <section class="card focus-card">
    <div class="section-heading">
      <div>
        <span class="eyebrow">EXECUTION</span>
        <h2>一次，只投入当前这件事</h2>
      </div>
      <el-button :loading="!!execution.busy" @click="refresh"
        >刷新执行状态</el-button
      >
    </div>
    <el-alert
      v-if="execution.error || summaryError"
      :title="execution.error || summaryError"
      type="warning"
      :closable="false"
    />
    <template v-if="execution.current">
      <div class="focus-title">
        <AppStatusTag value="RUNNING" /><strong>{{
          execution.current.taskName
        }}</strong>
      </div>
      <div
        class="focus-clock"
        role="timer"
        aria-label="本次实际投入时长"
        aria-live="off"
      >
        {{ duration(elapsedSeconds()) }}
      </div>
      <p class="hint">
        本次实际投入 · 执行边界
        {{ instantText(execution.current.eligibleUntil) }}
      </p>
      <p v-if="!execution.current.canHeartbeat" class="hint">
        由其他登录端控制。本端可以暂停和结束，接管需先暂停，再从暂停列表继续。
      </p>
      <div class="toolbar">
        <el-button
          v-if="canGrow('execution:session:pause')"
          type="primary"
          :disabled="!!execution.busy"
          @click="control('pause')"
          >暂停执行</el-button
        >
        <el-button
          v-if="canGrow('execution:session:finish')"
          type="danger"
          plain
          :disabled="!!execution.busy"
          @click="control('finish')"
          >结束执行</el-button
        >
        <el-button
          v-if="canGrow('execution:session:read')"
          @click="details(execution.current.id)"
          >会话时间线</el-button
        >
        <el-button
          v-if="canGrow('task:task:read')"
          text
          @click="router.push('/growth/tasks/' + execution.current.taskId)"
          >任务详情</el-button
        >
      </div>
    </template>
    <template v-else>
      <div class="focus-clock idle">00:00:00</div>
      <p class="muted">当前没有运行中的执行，选择已安排任务后开始。</p>
      <div class="start-selection">
        <span class="selected-task"
          ><AppIcon :name="selected ? 'task' : 'inbox'" />{{
            selected?.name || "请在下方选择一个已安排任务"
          }}</span
        ><el-button
          v-if="canStartTask()"
          type="primary"
          :disabled="!selected || !!execution.busy"
          :loading="execution.busy === 'start'"
          @click="start"
          >开始执行</el-button
        >
      </div>
    </template>
    <el-button
      class="focus-record-link"
      link
      type="primary"
      @click="router.push('/execution/records')"
      >查看全部执行记录</el-button
    >
  </section>
  <div v-if="summary" class="metrics-grid">
    <article class="card metric">
      <span>今日实际投入</span
      ><strong>{{ duration(summary.rangeActualSeconds) }}</strong>
    </article>
    <article class="card metric">
      <span>累计实际投入</span
      ><strong>{{ duration(summary.totalActualSeconds) }}</strong>
    </article>
    <article class="card metric">
      <span>暂停会话</span><strong>{{ summary.pausedSessions }}</strong>
    </article>
  </div>
  <ExecutionTaskPicker
    v-if="canStartTask()"
    ref="picker"
    @selected="selected = $event"
  />
  <section class="card">
    <div class="section-heading">
      <div>
        <h2>暂停中的会话</h2>
        <p class="hint">暂停期间不计时。继续时重新检查任务日期与计划安排。</p>
      </div>
    </div>
    <LoadState
      :loading="paused.busy && !paused.loaded"
      :error="paused.error"
      @retry="paused.load"
    >
      <ExecutionTable
        :rows="paused.rows"
        :loading="paused.busy"
        controls
        @detail="details"
        @changed="paused.load"
      />
      <PagePagination :state="paused" @change="paused.load" />
    </LoadState>
  </section>
  <ExecutionDetails v-model="show" :id="detailId" @changed="refresh" />
</template>
