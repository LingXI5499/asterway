<!-- 任务卡片只展示日常决策需要的信息，完整资料通过详情弹窗查看。 -->
<script setup>
import { computed } from "vue";
import { canGrow, priorities } from "../../api/growth";
import { api } from "../../api/http";
import { canStartTask } from "../../api/personal";
import { execution, startExecution } from "../../stores/personal";
import { report } from "../../api/ui";
import "../../styles/planning.css";

const props = defineProps({ row: { type: Object, required: true }, readOnly: Boolean });
const emit = defineEmits(["detail", "edit", "status", "remove", "assign", "unassign", "history", "changed"]);
const archived = computed(() => !!(props.row.archivedAt || props.row.goalArchivedAt));
const assigned = computed(() => !!props.row.weeklyPlanId || ["WEEK", "DAY"].includes(props.row.assignmentStatus));
const canArrange = computed(() => canGrow("planning:assignment:manage") && canGrow("planning:assignment:read"));
const canRemoveArrangement = computed(() => canGrow("planning:assignment:remove") && canGrow("planning:assignment:read"));

// 沿用现有计时状态，任何入口开始执行都会更新同一条底部执行栏。
async function begin() {
  try {
    const task = props.row.taskId ? await api(`/tasks/${props.row.taskId}`) : props.row;
    await startExecution(task);
    emit("changed");
  } catch (error) { report(error); }
}
</script>

<template>
  <article class="plan-task-card" :class="{ 'is-completed': row.status === 'COMPLETED' }">
    <div class="plan-task-title">
      <button v-if="canGrow('task:task:read')" type="button" class="plan-task-name" @click="emit('detail', row)">{{ row.taskName || row.name }}</button>
      <h3 v-else class="plan-task-name">{{ row.taskName || row.name }}</h3>
      <AppStatusTag :value="row.status" />
    </div>
    <p class="plan-task-origin" :title="[row.goalName, row.stageName].filter(Boolean).join(' / ')">{{ row.goalName || '所属目标' }} <span>/</span> {{ row.stageName || '所属阶段' }}</p>
    <p v-if="row.description" class="plan-task-description">{{ row.description }}</p>
    <div class="plan-task-meta">
      <span :class="{ urgent: ['HIGH', 'URGENT'].includes(row.priority) }">{{ priorities[row.priority] || '中' }}优先级</span>
      <span v-if="row.estimatedMinutes">预计 {{ row.estimatedMinutes }} 分钟</span>
      <span v-if="row.planDate">{{ row.planDate }}</span>
      <span v-else>{{ row.dailyPlanId || row.assignmentStatus === 'DAY' ? '已安排到日' : assigned ? '已排入周，日期待定' : '尚未安排' }}</span>
      <span v-if="archived">目标已归档</span>
    </div>
    <div class="plan-task-actions">
      <el-button v-if="canStartTask() && canGrow('task:task:read')" size="small" type="success" plain
        :disabled="readOnly || archived || !assigned || row.status === 'COMPLETED' || !!execution.current || !!execution.busy" @click="begin">开始执行</el-button>
      <el-button v-if="canGrow('task:task:read')" size="small" @click="emit('detail', row)">详情</el-button>
      <el-button v-if="canGrow('task:task:update') && canGrow('task:task:read')" size="small" :disabled="readOnly || archived" @click="emit('edit', row)">编辑</el-button>
      <el-button v-if="canArrange" size="small" :disabled="archived || row.status === 'COMPLETED'" @click="emit('assign', row)">{{ assigned ? '改期' : '安排' }}</el-button>
      <el-button v-if="canGrow('task:task:status')" size="small" :disabled="readOnly || archived" @click="emit('status', row)">状态</el-button>
      <el-dropdown v-if="canRemoveArrangement || canGrow('planning:assignment:read') || canGrow('task:task:delete')" trigger="click" @command="command => emit(command, row)">
        <el-button size="small" aria-label="更多任务操作">更多<AppIcon name="more" :size="14" /></el-button>
        <template #dropdown><el-dropdown-menu>
          <el-dropdown-item v-if="canRemoveArrangement" command="unassign" :disabled="!assigned || archived">移出安排，退回任务池</el-dropdown-item>
          <el-dropdown-item v-if="canGrow('planning:assignment:read')" command="history">安排历史</el-dropdown-item>
          <el-dropdown-item v-if="canGrow('task:task:delete')" command="remove" :disabled="readOnly || archived">删除任务</el-dropdown-item>
        </el-dropdown-menu></template>
      </el-dropdown>
    </div>
  </article>
</template>
