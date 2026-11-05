<script setup>
// 只读分页查看当前任务的安排历史，不提供修改历史入口。
import { ref, onMounted } from "vue";
import { query } from "../../api/growth";
import { dateText } from "../../api/ui";
const props = defineProps({ taskId: [String, Number] });
const emit = defineEmits(["close"]);
const rows = ref([]),
  total = ref(0),
  page = ref(1),
  busy = ref(false),
  error = ref("");
const reasons = {
  RESCHEDULED: "改期",
  RETURNED_TO_POOL: "退回任务池",
  TASK_DELETED: "任务删除",
  GOAL_ARCHIVED: "目标归档",
  DAILY_PLAN_DELETED: "日计划删除",
  WEEKLY_PLAN_DELETED: "周计划删除",
};
async function load() {
  busy.value = true;
  try {
    const d = await query(`/tasks/${props.taskId}/assignment-history`, {
      page: page.value,
      size: 20,
    });
    rows.value = d.items;
    total.value = d.total;
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <el-drawer
    :model-value="true"
    @close="emit('close')"
    title="任务安排历史"
    size="min(800px,100%)"
    ><p class="hint">
      改期保留原记录，任务本身不会复制。周／日编号对应当时的历史计划。
    </p>
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false" /><el-table :data="rows" v-loading="busy"
      ><el-table-column prop="weeklyPlanId" label="周计划" /><el-table-column
        prop="dailyPlanId"
        label="日计划"
      /><el-table-column label="安排时间" min-width="180"
        ><template #default="{ row }">{{
          dateText(row.assignedAt)
        }}</template></el-table-column
      ><el-table-column label="结束时间" min-width="180"
        ><template #default="{ row }">{{
          dateText(row.endedAt)
        }}</template></el-table-column
      ><el-table-column label="原因" min-width="130"
        ><template #default="{ row }"
          ><el-tag :type="row.endedAt ? 'info' : 'success'">{{
            row.endedAt ? reasons[row.endReason] || row.endReason : "当前安排"
          }}</el-tag></template
        ></el-table-column
      ></el-table
    ><el-pagination
      class="pagination"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="total,prev,pager,next"
      @current-change="load"
  /></el-drawer>
</template>
