<script setup>
// 目标与阶段级联选择，远程搜索避免一次装载所有个人数据。
import { ref, watch, onMounted } from "vue";
import { query, getResource } from "../../api/growth";
import { report } from "../../api/ui";
const props = defineProps({
  goalId: [String, Number],
  stageId: [String, Number],
  goalOnly: Boolean,
  disabled: Boolean,
});
const emit = defineEmits(["update:goalId", "update:stageId"]);
const goals = ref([]),
  stages = ref([]),
  loading = ref(false);
let goalSequence = 0,
  stageSequence = 0;
async function loadGoals(keyword = "") {
  const seq = ++goalSequence;
  try {
    const data = await query("/goals", {
      timing: "ALL",
      archived: false,
      size: 100,
      keyword,
    });
    if (seq !== goalSequence) return;
    goals.value = data.items;
    if (
      props.goalId &&
      !goals.value.some((r) => r.id === String(props.goalId))
    ) {
      goals.value.unshift(await getResource("goal", props.goalId));
    }
  } catch (e) {
    report(e);
  }
}
async function loadStages(keyword = "") {
  const seq = ++stageSequence;
  if (!props.goalId) {
    stages.value = [];
    return;
  }
  loading.value = true;
  try {
    const data = await query(`/goals/${props.goalId}/stages`, {
      size: 100,
      keyword,
    });
    if (seq !== stageSequence) return;
    stages.value = data.items;
    if (
      props.stageId &&
      !stages.value.some((r) => r.id === String(props.stageId))
    )
      stages.value.unshift(await getResource("stage", props.stageId));
  } catch (e) {
    report(e);
  } finally {
    if (seq === stageSequence) loading.value = false;
  }
}
function selectGoal(id) {
  emit("update:goalId", id || "");
  emit("update:stageId", "");
}
watch(
  () => props.goalId,
  () => loadStages(),
);
onMounted(() => {
  loadGoals();
  loadStages();
});
</script>
<template>
  <div class="growth-selectors">
    <el-select
      :model-value="goalId"
      @update:model-value="selectGoal"
      :disabled="disabled"
      placeholder="选择目标"
      filterable
      remote
      clearable
      :remote-method="loadGoals"
      ><el-option
        v-for="g in goals"
        :key="g.id"
        :value="g.id"
        :label="g.name" /></el-select
    ><el-select
      v-if="!goalOnly"
      :model-value="stageId"
      @update:model-value="emit('update:stageId', $event || '')"
      :disabled="disabled || !goalId"
      placeholder="选择阶段"
      filterable
      remote
      clearable
      :remote-method="loadStages"
      :loading="loading"
      ><el-option v-for="s in stages" :key="s.id" :value="s.id" :label="s.name"
    /></el-select>
  </div>
</template>
