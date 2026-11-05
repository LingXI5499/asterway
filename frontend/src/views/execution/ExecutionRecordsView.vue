<script setup>
import { ref, reactive, watch, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { executionApi, executionStates, duration } from "../../api/personal";
import { canGrow, validDate } from "../../api/growth";
import { usePaged } from "../../composables/usePaged";
import { report } from "../../api/ui";
import ScopeFilters from "../../components/personal/ScopeFilters.vue";
import ExecutionTable from "../../components/personal/ExecutionTable.vue";
import ExecutionDetails from "../../components/personal/ExecutionDetails.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const route = useRoute(),
  router = useRouter();
const filters = reactive({
  goalId: "",
  stageId: "",
  taskId: route.query.taskId || "",
  status: "",
  dates: [],
});
const summary = ref(null),
  summaryDates = ref([]),
  show = ref(false),
  detailId = ref("");
let applied = { taskId: filters.taskId },
  sequence = 0,
  controller;
const list = usePaged(
  (params, signal) => executionApi.list({ ...applied, ...params }, signal),
  { immediate: false },
);
function details(id) {
  detailId.value = id;
  show.value = true;
}
async function search(reset = false) {
  try {
    const dates = filters.dates || [];
    if (
      dates.length &&
      (dates.length !== 2 || !dates.every(validDate) || dates[0] > dates[1])
    )
      throw new Error("请选择完整、顺序正确的日期区间");
    applied = {
      goalId: filters.goalId,
      stageId: filters.stageId,
      taskId: filters.taskId,
      status: filters.status,
      dateFrom: dates[0],
      dateTo: dates[1],
    };
    const seq = ++sequence;
    controller?.abort();
    controller = new AbortController();
    summary.value = null;
    const [, result] = await Promise.all([
      list.load(reset),
      canGrow("execution:session:read")
        ? executionApi.summary(
            { dateFrom: dates[0], dateTo: dates[1] },
            controller.signal,
          )
        : Promise.resolve(null),
    ]);
    if (seq === sequence) {
      summary.value = result;
      summaryDates.value = [...dates];
    }
  } catch (e) {
    if (e.name !== "AbortError" && e.code !== "SESSION_CHANGED") report(e);
  }
}
function reset() {
  Object.assign(filters, {
    goalId: "",
    stageId: "",
    taskId: "",
    status: "",
    dates: [],
  });
  search(true);
}
watch(
  () => route.query.taskId,
  (value) => {
    filters.taskId = value || "";
    search(true);
  },
);
onMounted(() => search());
onBeforeUnmount(() => {
  sequence++;
  controller?.abort();
});
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <h2>每一段真实投入，都有记录</h2>
      <el-button @click="router.push('/execution')">返回执行工作台</el-button>
    </div>
    <div class="toolbar">
      <ScopeFilters :filters="filters" /><el-select
        v-model="filters.status"
        clearable
        placeholder="全部会话状态"
        ><el-option
          v-for="(name, code) in executionStates"
          :key="code"
          :value="code"
          :label="name"
      /></el-select>
      <el-date-picker
        v-model="filters.dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />
      <el-button type="primary" @click="search(true)">查询</el-button
      ><el-button @click="reset">重置</el-button>
    </div>
    <p v-if="summary" class="hint">
      账户{{ summaryDates.length ? summaryDates.join(" 至 ") : "今日" }}实际投入
      {{ duration(summary.rangeActualSeconds) }}；累计
      {{
        duration(summary.totalActualSeconds)
      }}。上方耗时为账户口径，下表按所选来源筛选。
    </p>
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <ExecutionTable
        :rows="list.rows"
        :loading="list.busy"
        @detail="details"
      />
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <ExecutionDetails v-model="show" :id="detailId" @changed="list.load" />
</template>
