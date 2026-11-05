<script setup>
import { ref, onMounted } from "vue";
import { query, canGrow } from "../../api/growth";
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
const filter = ref({ goalId: "", stageId: "", keyword: "", status: "" }),
  range = ref([]),
  rows = ref([]),
  page = ref(1),
  size = ref(20),
  total = ref(0),
  busy = ref(false),
  error = ref(""),
  actions = ref(null);
let sequence = 0;
async function load() {
  const seq = ++sequence;
  busy.value = true;
  try {
    const data = await query("/planning/pending-tasks", {
      ...filter.value,
      dateFrom: range.value?.[0],
      dateTo: range.value?.[1],
      page: page.value,
      size: size.value,
    });
    if (seq !== sequence) return;
    rows.value = data.items;
    total.value = data.total;
    error.value = "";
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) busy.value = false;
  }
}
function reset() {
  filter.value = { goalId: "", stageId: "", keyword: "", status: "" };
  range.value = [];
  page.value = 1;
  load();
}
onMounted(load);
</script>
<template>
  <section class="card">
    <h2>由你决定下一步</h2>
    <p class="muted">
      这里汇总已过计划日期或所在周已结束的未完成任务。系统不会自动延期或复制任务。
    </p>
    <div class="toolbar">
      <GrowthSelector
        v-model:goal-id="filter.goalId"
        v-model:stage-id="filter.stageId"
      /><el-input
        v-model="filter.keyword"
        placeholder="任务名称"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-select
        v-model="filter.status"
        clearable
        placeholder="全部未完成状态"
        ><el-option label="未开始" value="NOT_STARTED" /><el-option
          label="进行中"
          value="IN_PROGRESS" /><el-option
          label="暂停"
          value="PAUSED" /></el-select
      ><el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="原安排开始日期"
        end-placeholder="原安排结束日期"
      /><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><el-button @click="reset">重置</el-button>
    </div>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><GrowthTaskTable
      :rows="rows"
      :loading="busy"
      @detail="(r) => actions.details('task', r)"
      @edit="(r) => actions.edit('task', r)"
      @status="(r) => actions.status('task', r)"
      @remove="(r) => actions.remove('task', r)"
      @assign="(r) => actions.assign(r)"
      @unassign="(r) => actions.unassign(r)"
      @history="(r) => actions.history(r)"
    /><el-pagination
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total,sizes,prev,pager,next"
      @change="load"
    />
  </section>
  <GrowthActions ref="actions" @changed="load" />
</template>
