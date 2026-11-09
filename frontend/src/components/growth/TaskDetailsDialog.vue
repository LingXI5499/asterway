<!-- 用居中弹窗承接原任务详情页，资料、实际投入与状态历史仍完整保留。 -->
<script setup>
import { ref, computed, watch, onBeforeUnmount } from "vue";
import { api } from "../../api/http";
import { canGrow, getResource, positions, today } from "../../api/growth";
import { pastAssignment } from "../../api/planning";
import { dateText } from "../../api/ui";
import PlanTaskCard from "./PlanTaskCard.vue";
import GrowthEvidence from "../personal/GrowthEvidence.vue";

const props = defineProps({ id: [String, Number] });
const emit = defineEmits(["close", "edit", "status", "remove", "assign", "unassign", "history", "changed"]);
const record = ref(null), assignment = ref(null), week = ref(null), day = ref(null), goal = ref(null);
const busy = ref(false), error = ref(""), tab = ref("details");
let sequence = 0;
const row = computed(() => ({
  ...record.value, weeklyPlanId: assignment.value?.weeklyPlanId,
  dailyPlanId: assignment.value?.dailyPlanId, planDate: day.value?.planDate,
  weekStartDate: week.value?.weekStartDate, goalArchivedAt: goal.value?.archivedAt,
}));

async function load() {
  const seq = ++sequence;
  busy.value = true; error.value = "";
  try {
    const task = await getResource("task", props.id);
    const a = canGrow("planning:assignment:read") ? await api(`/tasks/${props.id}/assignment`) : null;
    const [g, w, d] = await Promise.all([
      canGrow("goal:goal:read") ? getResource("goal", task.goalId) : null,
      a && canGrow("planning:weekly:read") ? api(`/weekly-plans/${a.weeklyPlanId}`) : null,
      a?.dailyPlanId && canGrow("planning:daily:read") ? api(`/daily-plans/${a.dailyPlanId}`) : null,
    ]);
    if (seq !== sequence) return;
    record.value = task; assignment.value = a; goal.value = g; week.value = w; day.value = d;
  } catch (e) {
    if (seq === sequence) {
      if (e.status === 404) emit("close");
      else error.value = e.message;
    }
  } finally { if (seq === sequence) busy.value = false; }
}
function changed() { load(); emit("changed"); }
watch(() => props.id, () => { record.value = null; tab.value = "details"; load(); }, { immediate: true });
onBeforeUnmount(() => { sequence++; });
defineExpose({ load });
</script>

<template>
  <el-dialog :model-value="true" title="任务详情" width="min(920px,94vw)" align-center append-to-body class="plan-dialog" @close="emit('close')">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="error" @click="load">重新加载</el-button>
    <el-skeleton v-if="busy && !record" :rows="8" animated />
    <template v-if="record">
      <PlanTaskCard :row="row" :read-only="pastAssignment(row, today())" @detail="tab = 'details'"
        @edit="r => emit('edit', r)" @status="r => emit('status', r)" @remove="r => emit('remove', r)"
        @assign="r => emit('assign', r)" @unassign="r => emit('unassign', r)" @history="r => emit('history', r)" @changed="changed" />
      <el-tabs v-model="tab" class="plan-detail-tabs">
        <el-tab-pane label="完整资料" name="details">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="任务描述"><span class="preserve-text">{{ record.description || '尚未填写' }}</span></el-descriptions-item>
            <el-descriptions-item label="完成标准"><span class="preserve-text">{{ record.completionCriteria || '尚未填写' }}</span></el-descriptions-item>
            <el-descriptions-item label="权重">{{ record.weight }}</el-descriptions-item>
            <el-descriptions-item label="预计耗时">{{ record.estimatedMinutes ? record.estimatedMinutes + ' 分钟' : '未指定' }}</el-descriptions-item>
            <el-descriptions-item label="任务日期">{{ record.startDate || '未指定开始日期' }} 至 {{ record.dueDate || '未指定截止日期' }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ dateText(record.completedAt) }}</el-descriptions-item>
            <el-descriptions-item label="当前安排">{{ day?.planDate || (week ? week.weekStartDate + ' 当周，日期待定' : positions[record.assignmentStatus] || '尚未安排') }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="执行记录与状态历史" name="evidence" lazy><GrowthEvidence centered kind="task" :id="record.id" /></el-tab-pane>
      </el-tabs>
    </template>
    <template #footer><el-button @click="emit('close')">关闭</el-button></template>
  </el-dialog>
</template>
