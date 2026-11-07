<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import { api } from "../../api/http";
import { query, today, monday, canGrow } from "../../api/growth";
import { execution } from "../../stores/personal";
import PagePagination from "./PagePagination.vue";
import LoadState from "./LoadState.vue";
const emit = defineEmits(["selected"]);
const list = ref({ rows: [], total: 0, page: 1, size: 20 }),
  keyword = ref(""),
  busy = ref(false),
  error = ref(""),
  chosen = ref("");
const allowed = computed(() =>
  [
    "planning:weekly:read",
    "task:task:read",
    "goal:goal:read",
    "stage:stage:read",
  ].every(canGrow),
);
const day = computed(() => {
  void execution.tick;
  return today();
});
let sequence = 0,
  disposed = false,
  weekId = "";
function reason(row) {
  if (row.status === "COMPLETED") return "任务已完成";
  if (row.archivedAt) return "目标已归档";
  if (row.planDate && row.planDate !== day.value)
    return "安排在 " + row.planDate;
  return "";
}
async function load(reset = false) {
  if (!allowed.value) return;
  if (reset === true) list.value.page = 1;
  const seq = ++sequence;
  busy.value = true;
  error.value = "";
  chosen.value = "";
  emit("selected", null);
  try {
    const week = await api("/weekly-plans/by-week/" + monday(day.value));
    if (seq !== sequence || disposed) return;
    weekId = week?.id;
    if (!weekId) {
      Object.assign(list.value, { rows: [], total: 0 });
      return;
    }
    const result = await query("/weekly-plans/" + weekId + "/tasks", {
      page: list.value.page,
      size: list.value.size,
      keyword: keyword.value,
    });
    if (seq === sequence && !disposed)
      Object.assign(list.value, { rows: result.items, total: result.total });
  } catch (e) {
    if (seq === sequence && !disposed) error.value = e.message;
  } finally {
    if (seq === sequence && !disposed) busy.value = false;
  }
}
async function select(row) {
  if (reason(row) || busy.value) return;
  const seq = ++sequence;
  busy.value = true;
  error.value = "";
  try {
    const [task, goal, stage] = await Promise.all([
      api("/tasks/" + row.taskId),
      api("/goals/" + row.goalId),
      api("/stages/" + row.stageId),
    ]);
    if (seq !== sequence || disposed) return;
    if (
      goal.archivedAt ||
      goal.status === "COMPLETED" ||
      stage.status === "COMPLETED" ||
      task.status === "COMPLETED" ||
      (goal.startDate && goal.startDate > day.value) ||
      (task.startDate && task.startDate > day.value)
    ) {
      throw new Error(
        "该任务或来源状态尚不支持执行，请先调整目标、阶段、任务或安排",
      );
    }
    chosen.value = row.taskId;
    emit("selected", task);
  } catch (e) {
    if (seq === sequence && !disposed) {
      error.value = e.message;
      chosen.value = "";
      emit("selected", null);
    }
  } finally {
    if (seq === sequence && !disposed) busy.value = false;
  }
}
watch(day, () => load(true));
onMounted(() => load());
onBeforeUnmount(() => {
  disposed = true;
  sequence++;
});
defineExpose({ reload: () => load() });
</script>
<template>
  <section class="card">
    <div class="section-heading">
      <div>
        <h2>从本周计划选择任务</h2>
        <p class="hint">
          指定日期的任务在安排当天执行；本周尚未定日的任务可直接开始。
        </p>
      </div>
    </div>
    <el-alert
      v-if="!allowed"
      type="info"
      :closable="false"
      title="选择任务需要周计划和来源资料读取权限，可由管理员在普通用户角色中补齐"
    />
    <template v-else>
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索本周任务"
          clearable
          @keyup.enter="load(true)"
        /><el-button @click="load(true)">查询</el-button
        ><el-button
          @click="
            keyword = '';
            load(true);
          "
          >重置</el-button
        >
      </div>
      <LoadState
        :loading="busy && !list.rows.length"
        :error="error"
        @retry="load"
      >
        <el-table
          :data="list.rows"
          v-loading="busy"
          empty-text="本周暂无安排，可先前往周计划添加任务"
        >
          <template #empty
            ><AppEmptyState
              :description="
                error
                  ? '加载失败，请查看提示后重试。'
                  : '本周暂无安排，可先前往周计划添加任务'
              " /></template
          ><el-table-column label="选择" width="80"
            ><template #default="{ row }"
              ><el-radio
                :model-value="chosen"
                :value="row.taskId"
                :disabled="!!reason(row)"
                @change="select(row)"
                :aria-label="'选择' + row.taskName"
                ><span class="sr-only">{{ row.taskName }}</span></el-radio
              ></template
            ></el-table-column
          >
          <el-table-column
            prop="taskName"
            label="任务"
            min-width="180"
          /><el-table-column prop="goalName" label="目标" min-width="140" />
          <el-table-column label="安排" min-width="160"
            ><template #default="{ row }"
              ><el-tag :type="reason(row) ? 'info' : 'success'">{{
                reason(row) || row.planDate || "本周未定日"
              }}</el-tag></template
            ></el-table-column
          >
        </el-table>
        <PagePagination :state="list" @change="load" />
      </LoadState>
    </template>
  </section>
</template>
