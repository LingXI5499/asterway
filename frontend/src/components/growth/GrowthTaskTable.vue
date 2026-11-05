<script setup>
// 任务与计划查询共用表格，依据动作权限、归档和只读状态显示操作。
import { states, priorities, positions, canGrow } from "../../api/growth";
import { api } from "../../api/http";
import { canStartTask } from "../../api/personal";
import { execution, startExecution } from "../../stores/personal";
import { report } from "../../api/ui";
async function begin(row) {
  try {
    // 计划行的 version 属于安排，开始执行必须重新取得任务自己的版本。
    const task = row.taskId ? await api("/tasks/" + row.taskId) : row;
    const value = await startExecution(task);
    row.status = "IN_PROGRESS";
    if (!row.taskId) row.version = value.taskVersion;
  } catch (error) {
    report(error);
  }
}
const props = defineProps({ rows: Array, loading: Boolean, readOnly: Boolean });
defineEmits([
  "detail",
  "edit",
  "status",
  "remove",
  "assign",
  "unassign",
  "history",
]);
</script>
<template>
  <el-table
    :data="rows"
    v-loading="loading"
    empty-text="暂无任务，先创建任务或调整筛选条件"
    ><el-table-column label="任务" min-width="200"
      ><template #default="{ row }"
        ><el-button
          v-if="canGrow('task:task:read')"
          link
          type="primary"
          @click="$emit('detail', row)"
          >{{ row.taskName || row.name }}</el-button
        ><span v-else>{{ row.taskName || row.name }}</span
        ><el-tag
          v-if="row.archivedAt || row.goalArchivedAt"
          type="info"
          size="small"
          >目标已归档</el-tag
        ></template
      ></el-table-column
    ><el-table-column
      prop="goalName"
      label="来源目标"
      min-width="140"
      show-overflow-tooltip
    /><el-table-column
      prop="stageName"
      label="来源阶段"
      min-width="140"
      show-overflow-tooltip
    /><el-table-column label="状态" width="100"
      ><template #default="{ row }"
        ><el-tag
          :type="
            row.status === 'COMPLETED'
              ? 'success'
              : row.status === 'PAUSED'
                ? 'warning'
                : 'info'
          "
          >{{ states[row.status] }}</el-tag
        ></template
      ></el-table-column
    ><el-table-column label="优先级" width="90"
      ><template #default="{ row }">{{
        priorities[row.priority]
      }}</template></el-table-column
    ><el-table-column prop="weight" label="权重" width="70" /><el-table-column
      label="安排"
      min-width="130"
      ><template #default="{ row }">{{
        row.planDate ||
        positions[row.assignmentStatus] ||
        (row.dailyPlanId ? "已安排到日" : "已安排到周")
      }}</template></el-table-column
    ><el-table-column label="操作" min-width="480" fixed="right"
      ><template #default="{ row }"
        ><el-button
          v-if="canStartTask() && canGrow('task:task:read')"
          link
          type="success"
          :disabled="
            readOnly ||
            !!execution.busy ||
            !!execution.current ||
            row.status === 'COMPLETED' ||
            !!(row.archivedAt || row.goalArchivedAt) ||
            row.assignmentStatus === 'POOL'
          "
          @click="begin(row)"
          >开始执行</el-button
        ><el-button
          v-if="canGrow('task:task:update') && canGrow('task:task:read')"
          :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
          link
          type="primary"
          @click="$emit('edit', row)"
          >编辑</el-button
        ><el-button
          v-if="canGrow('task:task:status')"
          :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
          link
          type="primary"
          @click="$emit('status', row)"
          >状态</el-button
        ><el-button
          v-if="
            canGrow('planning:assignment:manage') &&
            canGrow('planning:assignment:read')
          "
          :disabled="
            row.status === 'COMPLETED' ||
            !!(row.archivedAt || row.goalArchivedAt)
          "
          link
          type="primary"
          @click="$emit('assign', row)"
          >安排／改期</el-button
        ><el-button
          v-if="
            canGrow('planning:assignment:remove') &&
            canGrow('planning:assignment:read')
          "
          :disabled="
            !!(row.archivedAt || row.goalArchivedAt) ||
            row.assignmentStatus === 'POOL'
          "
          link
          type="warning"
          @click="$emit('unassign', row)"
          >移出</el-button
        ><el-button
          v-if="canGrow('planning:assignment:read')"
          link
          type="primary"
          @click="$emit('history', row)"
          >历史</el-button
        ><el-button
          v-if="canGrow('task:task:delete')"
          :disabled="readOnly || !!(row.archivedAt || row.goalArchivedAt)"
          link
          type="danger"
          @click="$emit('remove', row)"
          >删除</el-button
        ></template
      ></el-table-column
    ></el-table
  >
</template>
