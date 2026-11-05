<script setup>
import { duration, instantText, executionStates } from "../../api/personal";
import { canGrow } from "../../api/growth";
import { execution, controlExecution } from "../../stores/personal";
import { report } from "../../api/ui";
defineProps({ rows: Array, loading: Boolean, controls: Boolean });
const emit = defineEmits(["detail", "changed"]);
async function control(action, row) {
  try {
    await controlExecution(action, row);
    emit("changed");
  } catch (e) {
    report(e);
    emit("changed");
  }
}
</script>
<template>
  <el-table :data="rows" v-loading="loading" empty-text="暂无执行记录">
    <el-table-column
      prop="taskName"
      label="任务"
      min-width="180"
      show-overflow-tooltip
    />
    <el-table-column label="状态" width="104"
      ><template #default="{ row }"
        ><el-tag
          :type="
            row.status === 'RUNNING'
              ? 'success'
              : row.status === 'PAUSED'
                ? 'warning'
                : 'info'
          "
          >{{ executionStates[row.status] }}</el-tag
        ></template
      ></el-table-column
    >
    <el-table-column label="首次开始" min-width="180"
      ><template #default="{ row }">{{
        instantText(row.startedAt)
      }}</template></el-table-column
    >
    <el-table-column label="实际投入" min-width="120"
      ><template #default="{ row }">{{
        duration(row.actualSeconds)
      }}</template></el-table-column
    >
    <el-table-column label="结束时间" min-width="180"
      ><template #default="{ row }">{{
        instantText(row.finishedAt)
      }}</template></el-table-column
    >
    <el-table-column
      label="操作"
      :min-width="controls ? 240 : 110"
      fixed="right"
      ><template #default="{ row }">
        <el-button
          v-if="canGrow('execution:session:read')"
          type="primary"
          link
          @click="emit('detail', row.id)"
          >详情</el-button
        >
        <el-button
          v-if="
            controls &&
            row.status === 'PAUSED' &&
            canGrow('execution:session:resume')
          "
          type="primary"
          link
          :disabled="!!execution.busy"
          @click="control('resume', row)"
          >继续</el-button
        >
        <el-button
          v-if="
            controls &&
            row.status === 'RUNNING' &&
            canGrow('execution:session:pause')
          "
          type="warning"
          link
          :disabled="!!execution.busy"
          @click="control('pause', row)"
          >暂停</el-button
        >
        <el-button
          v-if="
            controls &&
            row.status !== 'FINISHED' &&
            canGrow('execution:session:finish')
          "
          type="danger"
          link
          :disabled="!!execution.busy"
          @click="control('finish', row)"
          >结束</el-button
        >
      </template></el-table-column
    >
  </el-table>
</template>
