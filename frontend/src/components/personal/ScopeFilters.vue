<script setup>
import { ref } from "vue";
import { query, canGrow } from "../../api/growth";
import PagedSelect from "./PagedSelect.vue";
const props = defineProps({
  filters: { type: Object, required: true },
  includeTask: { type: Boolean, default: true },
});
const archived = ref(false);
const goals = (params, signal) =>
  query(
    "/goals",
    { ...params, timing: "ALL", archived: archived.value },
    { signal },
  );
const stages = (params, signal) =>
  query("/goals/" + props.filters.goalId + "/stages", params, { signal });
const tasks = (params, signal) =>
  query(
    "/tasks",
    { ...params, goalId: props.filters.goalId, stageId: props.filters.stageId },
    { signal },
  );
function goal(value) {
  Object.assign(props.filters, { goalId: value, stageId: "", taskId: "" });
}
function stage(value) {
  Object.assign(props.filters, { stageId: value, taskId: "" });
}
</script>
<template>
  <div class="scope-filters">
    <PagedSelect
      :model-value="filters.goalId"
      @update:model-value="goal"
      :loader="goals"
      :resource-key="String(archived)"
      placeholder="全部目标"
      :disabled="!canGrow('goal:goal:list')"
    />
    <PagedSelect
      :model-value="filters.stageId"
      @update:model-value="stage"
      :loader="stages"
      :resource-key="filters.goalId"
      placeholder="全部阶段"
      :disabled="!filters.goalId || !canGrow('stage:stage:list')"
    />
    <PagedSelect
      v-if="includeTask"
      v-model="filters.taskId"
      :loader="tasks"
      :resource-key="String(filters.goalId) + ':' + filters.stageId"
      placeholder="全部任务"
      :disabled="!canGrow('task:task:list')"
    />
    <el-checkbox
      v-if="canGrow('goal:goal:list')"
      v-model="archived"
      @change="goal('')"
      >查看归档目标选项</el-checkbox
    >
  </div>
</template>
