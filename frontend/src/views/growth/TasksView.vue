<script setup>
import { ref, onMounted } from "vue";
import {
  query,
  states,
  priorities,
  positions,
  canGrow,
  getResource,
} from "../../api/growth";
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
const filters = ref({
    goalId: "",
    stageId: "",
    keyword: "",
    status: "",
    priority: "",
    assignmentStatus: "",
  }),
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
    const data = await query("/tasks", {
      ...filters.value,
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
  filters.value = {
    goalId: "",
    stageId: "",
    keyword: "",
    status: "",
    priority: "",
    assignmentStatus: "",
  };
  page.value = 1;
  load();
}
onMounted(load);
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <div>
        <h2>任务中心</h2>
        <p class="hint">任务保留原有目标与阶段，通过安排进入周计划和日计划。</p>
      </div>
      <el-button
        v-if="canGrow('task:task:create')"
        type="primary"
        @click="actions.edit('task', null, filters.stageId || null)"
        >新增任务</el-button
      >
    </div>
    <div class="toolbar">
      <GrowthSelector
        v-model:goal-id="filters.goalId"
        v-model:stage-id="filters.stageId"
      /><el-input
        v-model="filters.keyword"
        clearable
        placeholder="任务名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><el-select v-model="filters.status" clearable placeholder="全部状态"
        ><el-option
          v-for="(label, code) in states"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><el-select v-model="filters.priority" clearable placeholder="全部优先级"
        ><el-option
          v-for="(label, code) in priorities"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><el-select
        v-model="filters.assignmentStatus"
        clearable
        placeholder="全部安排位置"
        ><el-option
          v-for="(label, code) in positions"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><el-button @click="reset">重置</el-button
      ><el-button
        v-if="canGrow('task:task:sort')"
        :disabled="!filters.stageId"
        @click="actions.sort('task', filters.stageId)"
        >阶段内排序</el-button
      >
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
