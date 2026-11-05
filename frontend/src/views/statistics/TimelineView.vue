<script setup>
import { reactive, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  statisticsApi,
  statisticsParams,
  instantText,
  actions,
  reasons,
  kindNames,
} from "../../api/personal";
import { today } from "../../api/growth";
import { usePaged } from "../../composables/usePaged";
import PeriodFilters from "../../components/personal/PeriodFilters.vue";
import PagePagination from "../../components/personal/PagePagination.vue";
import LoadState from "../../components/personal/LoadState.vue";
const router = useRouter(),
  error = ref("");
const filters = reactive({
  period: "MONTH",
  anchorDate: today(),
  dates: [],
  goalId: "",
  stageId: "",
  taskId: "",
});
let applied = statisticsParams(filters);
const list = usePaged(
  (params, signal) => statisticsApi.timeline({ ...applied, ...params }, signal),
  { immediate: false },
);
function search() {
  try {
    applied = statisticsParams(filters);
    error.value = "";
    list.load(true);
  } catch (e) {
    error.value = e.message;
  }
}
function reset() {
  Object.assign(filters, {
    period: "MONTH",
    anchorDate: today(),
    dates: [],
    goalId: "",
    stageId: "",
    taskId: "",
  });
  search();
}
onMounted(search);
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <h2>留下发生过的事</h2>
      <el-button @click="router.push('/statistics')">返回历史统计</el-button>
    </div>
    <PeriodFilters
      :filters="filters"
      :busy="list.busy"
      @search="search"
      @reset="reset"
    />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <p class="hint">
      目标、阶段、任务的真实状态，执行时间段、打卡与计划安排各自保留。习惯独立于目标筛选。
    </p>
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <el-timeline v-if="list.rows.length" class="fact-timeline">
        <el-timeline-item
          v-for="row in list.rows"
          :key="row.kind + ':' + row.factId + ':' + row.eventType"
          :timestamp="instantText(row.occurredAt)"
          placement="top"
          :type="row.eventType === 'BASELINE' ? 'info' : 'primary'"
        >
          <article class="timeline-fact">
            <div class="toolbar">
              <el-tag size="small" type="info">{{ kindNames[row.kind] }}</el-tag
              ><strong>{{
                actions[row.eventType] || reasons[row.eventType] || "历史事件"
              }}</strong>
            </div>
            <p>
              {{
                row.label ||
                (row.kind === "EXECUTION" || row.kind === "ASSIGNMENT"
                  ? "任务 "
                  : "对象 ") + row.objectId
              }}
            </p>
            <p v-if="row.eventType === 'BASELINE'" class="hint">
              这是初始化时可得的状态，不代表该日完成过这个对象。
            </p>
            <p v-if="row.eventType === 'COMPLETION_IMPORTED'" class="hint">
              依据旧任务可信完成时间导入，未知的过去没有补造。
            </p>
          </article>
        </el-timeline-item> </el-timeline
      ><el-empty v-else description="当前区间还没有真实事件记录" />
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
</template>
