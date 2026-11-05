<script setup>
import { ref, computed, watch, onBeforeUnmount } from "vue";
import {
  executionApi,
  duration,
  instantText,
  executionStates,
  reasons,
} from "../../api/personal";
import { canGrow } from "../../api/growth";
import {
  execution,
  controlExecution,
  elapsedSeconds,
} from "../../stores/personal";
import { report } from "../../api/ui";
import PagePagination from "./PagePagination.vue";
import LoadState from "./LoadState.vue";
const props = defineProps({ modelValue: Boolean, id: [String, Number] });
const emit = defineEmits(["update:modelValue", "changed"]);
const row = ref(null),
  busy = ref(false),
  error = ref("");
const segments = ref({ rows: [], total: 0, page: 1, size: 20 });
let sequence = 0,
  controller;
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
async function load() {
  if (!props.modelValue || !props.id) return;
  const seq = ++sequence;
  controller?.abort();
  controller = new AbortController();
  busy.value = true;
  error.value = "";
  try {
    const data = await executionApi.detail(props.id, controller.signal);
    if (seq !== sequence) return;
    row.value = { ...data.execution, _receivedAt: performance.now() };
    Object.assign(segments.value, {
      rows: data.segments.items,
      total: data.segments.total,
      page: 1,
      size: 20,
    });
  } catch (e) {
    if (seq === sequence && e.name !== "AbortError") error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
async function loadSegments() {
  const seq = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const data = await executionApi.segments(props.id, {
      page: segments.value.page,
      size: segments.value.size,
    });
    if (seq === sequence)
      Object.assign(segments.value, { rows: data.items, total: data.total });
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
async function control(action) {
  try {
    await controlExecution(action, row.value);
    await load();
    emit("changed");
  } catch (e) {
    report(e);
    await load();
  }
}
watch(
  () => [props.modelValue, props.id],
  () => {
    sequence++;
    row.value = null;
    if (props.modelValue) load();
    else controller?.abort();
  },
  { immediate: true },
);
// 全局暂停、结束或超时后同步详情，普通心跳不会重置时间线分页。
watch(
  () => execution.revision,
  () => {
    if (props.modelValue) load();
  },
);
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
});
</script>
<template>
  <el-drawer
    v-model="visible"
    title="执行详情与时间线"
    size="min(880px, 100vw)"
    destroy-on-close
  >
    <LoadState :loading="busy && !row" :error="error" @retry="load">
      <template v-if="row">
        <div class="section-heading">
          <h2>{{ row.taskName }}</h2>
          <el-tag
            :type="
              row.status === 'RUNNING'
                ? 'success'
                : row.status === 'PAUSED'
                  ? 'warning'
                  : 'info'
            "
            >{{ executionStates[row.status] }}</el-tag
          >
        </div>
        <div class="duration-block">
          <strong>{{ duration(elapsedSeconds(row)) }}</strong
          ><span>本会话实际投入，暂停时间不计入</span>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="首次开始">{{
            instantText(row.startedAt)
          }}</el-descriptions-item>
          <el-descriptions-item label="最终结束">{{
            instantText(row.finishedAt)
          }}</el-descriptions-item>
          <el-descriptions-item v-if="row.finishReason" label="结束原因">{{
            reasons[row.finishReason] || "执行已结束"
          }}</el-descriptions-item>
          <el-descriptions-item
            v-if="row.status === 'RUNNING'"
            label="本段执行边界"
            >{{ instantText(row.eligibleUntil) }}</el-descriptions-item
          >
        </el-descriptions>
        <div class="toolbar detail-toolbar">
          <el-button
            v-if="
              row.status === 'RUNNING' && canGrow('execution:session:pause')
            "
            :disabled="!!execution.busy"
            @click="control('pause')"
            >暂停执行</el-button
          >
          <el-button
            v-if="
              row.status === 'PAUSED' && canGrow('execution:session:resume')
            "
            type="primary"
            :disabled="!!execution.busy"
            @click="control('resume')"
            >继续原会话</el-button
          >
          <el-button
            v-if="
              row.status !== 'FINISHED' && canGrow('execution:session:finish')
            "
            :disabled="!!execution.busy"
            type="danger"
            plain
            @click="control('finish')"
            >结束执行</el-button
          >
          <el-button @click="load" :loading="busy">刷新详情</el-button>
        </div>
        <p v-if="row.status === 'RUNNING' && !row.canHeartbeat" class="hint">
          当前由其他登录端控制。可先暂停，再在本端继续以接管；此页面不会代发心跳。
        </p>
        <h3>连续执行时间段</h3>
        <el-table
          :data="segments.rows"
          v-loading="busy"
          empty-text="尚无执行时间段"
        >
          <el-table-column prop="segmentNo" label="段" width="64" />
          <el-table-column label="开始 / 继续" min-width="180"
            ><template #default="{ row: s }">{{
              instantText(s.startedAt, s.timeZone)
            }}</template></el-table-column
          >
          <el-table-column label="暂停 / 结束" min-width="180"
            ><template #default="{ row: s }">{{
              s.endedAt
                ? instantText(s.endedAt, s.timeZone)
                : "运行中，按有效截止封顶"
            }}</template></el-table-column
          >
          <el-table-column label="实际投入" min-width="110"
            ><template #default="{ row: s }">{{
              duration(s.actualSeconds)
            }}</template></el-table-column
          >
          <el-table-column label="原因" min-width="150"
            ><template #default="{ row: s }">{{
              reasons[s.endReason] || "尚未结束"
            }}</template></el-table-column
          >
          <el-table-column prop="timeZone" label="开始时区" min-width="150" />
        </el-table>
        <PagePagination :state="segments" @change="loadSegments" />
      </template>
    </LoadState>
  </el-drawer>
</template>
