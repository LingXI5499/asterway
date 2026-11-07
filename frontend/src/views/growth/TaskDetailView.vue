<script setup>
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { api } from "../../api/http";
import {
  getResource,
  states,
  priorities,
  positions,
  canGrow,
} from "../../api/growth";
import { dateText } from "../../api/ui";
import GrowthActions from "../../components/growth/GrowthActions.vue";
import GrowthEvidence from "../../components/personal/GrowthEvidence.vue";
import { canStartTask } from "../../api/personal";
import { execution, startExecution } from "../../stores/personal";
import { report } from "../../api/ui";
async function begin() {
  try {
    await startExecution(record.value);
    await load();
  } catch (error) {
    report(error);
  }
}
const route = useRoute(),
  router = useRouter(),
  record = ref(null),
  goal = ref(null),
  assignment = ref(null),
  assignedWeek = ref(null),
  assignedDay = ref(null),
  loading = ref(false),
  error = ref(""),
  actions = ref(null);
async function load() {
  loading.value = true;
  try {
    record.value = await getResource("task", route.params.id);
    goal.value = await getResource("goal", record.value.goalId);
    if (canGrow("planning:assignment:read")) {
      assignment.value = await api(`/tasks/${record.value.id}/assignment`);
      assignedWeek.value = assignment.value
        ? await api(`/weekly-plans/${assignment.value.weeklyPlanId}`)
        : null;
      assignedDay.value = assignment.value?.dailyPlanId
        ? await api(`/daily-plans/${assignment.value.dailyPlanId}`)
        : null;
    }
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function changed() {
  try {
    await getResource("task", route.params.id);
    load();
  } catch {
    router.push("/growth/tasks");
  }
}
onMounted(load);
</script>
<template>
  <el-alert
    v-if="error"
    :title="error"
    type="error"
    :closable="false"
  /><el-skeleton v-if="loading && !record" :rows="8" /><template v-if="record"
    ><section class="card">
      <div class="toolbar">
        <el-button text @click="router.push(`/growth/stages/${record.stageId}`)"
          >返回所属阶段</el-button
        ><AppStatusTag :value="record.status" /><el-tag
          v-if="goal?.archivedAt"
          type="info"
          >目标已归档</el-tag
        >
      </div>
      <h2>{{ record.name }}</h2>
      <p class="preserve-text">{{ record.description || "尚未填写描述" }}</p>
      <el-descriptions :column="2" border
        ><el-descriptions-item label="来源目标">{{
          record.goalName
        }}</el-descriptions-item
        ><el-descriptions-item label="来源阶段">{{
          record.stageName
        }}</el-descriptions-item
        ><el-descriptions-item label="完成条件" :span="2"
          ><span class="preserve-text">{{
            record.completionCriteria || "未填写"
          }}</span></el-descriptions-item
        ><el-descriptions-item label="优先级">{{
          priorities[record.priority]
        }}</el-descriptions-item
        ><el-descriptions-item label="权重">{{
          record.weight
        }}</el-descriptions-item
        ><el-descriptions-item label="预计耗时">{{
          record.estimatedMinutes ? record.estimatedMinutes + " 分钟" : "未指定"
        }}</el-descriptions-item
        ><el-descriptions-item label="完成时间">{{
          dateText(record.completedAt)
        }}</el-descriptions-item
        ><el-descriptions-item label="开始日期">{{
          record.startDate || "未指定"
        }}</el-descriptions-item
        ><el-descriptions-item label="截止日期">{{
          record.dueDate || "未指定"
        }}</el-descriptions-item
        ><el-descriptions-item label="当前位置">{{
          positions[record.assignmentStatus]
        }}</el-descriptions-item
        ><el-descriptions-item label="当前安排"
          >{{ assignedDay?.planDate || assignedWeek?.weekStartDate || "未安排"
          }}{{
            !assignedDay && assignedWeek ? " 所在周" : ""
          }}</el-descriptions-item
        ></el-descriptions
      >
      <div class="toolbar detail-toolbar">
        <el-button
          v-if="canStartTask()"
          type="success"
          :disabled="
            !!execution.current ||
            !!execution.busy ||
            !assignment ||
            !!goal?.archivedAt ||
            record.status === 'COMPLETED'
          "
          @click="begin"
          >开始执行</el-button
        >
        <el-button
          v-if="canGrow('task:task:update')"
          :disabled="!!goal?.archivedAt"
          type="primary"
          @click="actions.edit('task', record)"
          >编辑任务</el-button
        ><el-button
          v-if="canGrow('task:task:status')"
          :disabled="!!goal?.archivedAt"
          @click="actions.status('task', record)"
          >修改状态</el-button
        ><el-button
          v-if="
            canGrow('planning:assignment:manage') &&
            canGrow('planning:assignment:read')
          "
          :disabled="record.status === 'COMPLETED' || !!goal?.archivedAt"
          @click="actions.assign(record)"
          >安排／改期</el-button
        ><el-button
          v-if="
            canGrow('planning:assignment:remove') &&
            canGrow('planning:assignment:read')
          "
          :disabled="!assignment || !!goal?.archivedAt"
          @click="actions.unassign(record)"
          >移出计划</el-button
        ><el-button
          v-if="canGrow('planning:assignment:read')"
          @click="actions.history(record)"
          >安排历史</el-button
        ><el-button
          v-if="canGrow('task:task:delete')"
          :disabled="!!goal?.archivedAt"
          type="danger"
          plain
          @click="actions.remove('task', record)"
          >删除任务</el-button
        >
      </div>
    </section>
    <GrowthEvidence kind="task" :id="record.id" /></template
  ><GrowthActions ref="actions" @changed="changed" />
</template>
