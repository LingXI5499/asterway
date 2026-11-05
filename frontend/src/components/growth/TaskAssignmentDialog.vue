<script setup>
// 任务改期表单：先查询当前安排，显式创建计划，并携带安排标识与版本。
import { ref, onMounted, computed } from "vue";
import { api } from "../../api/http";
import { monday, today, addDays, canGrow } from "../../api/growth";
const props = defineProps({
  taskId: [String, Number],
  weekStart: String,
  planDate: String,
});
const emit = defineEmits(["close", "saved"]);
const current = ref(null),
  week = ref(null),
  day = ref(null),
  weekDate = ref(props.weekStart || monday(today())),
  dayDate = ref(props.planDate || ""),
  order = ref(0),
  busy = ref(false),
  loading = ref(true),
  error = ref("");
const pastWeek = computed(
  () => weekDate.value && addDays(weekDate.value, 6) < today(),
);
let weekSequence = 0,
  daySequence = 0;
async function weekChanged() {
  const seq = ++weekSequence;
  ++daySequence;
  week.value = null;
  day.value = null;
  dayDate.value = "";
  if (!weekDate.value) return;
  weekDate.value = monday(weekDate.value);
  try {
    const loaded = await api(`/weekly-plans/by-week/${weekDate.value}`);
    if (seq !== weekSequence) return;
    week.value = loaded;
    error.value = "";
  } catch (e) {
    error.value = e.message;
  }
}
async function dayChanged() {
  const seq = ++daySequence;
  day.value = null;
  if (!dayDate.value || !week.value) return;
  if (
    dayDate.value < weekDate.value ||
    dayDate.value > addDays(weekDate.value, 6)
  ) {
    error.value = "请选择本周内的日期";
    return;
  }
  try {
    const loaded = await api(
      `/weekly-plans/${week.value.id}/daily-plans/${dayDate.value}`,
    );
    if (seq !== daySequence) return;
    day.value = loaded;
    error.value = "";
  } catch (e) {
    error.value = e.message;
  }
}
async function create(kind) {
  busy.value = true;
  try {
    if (kind === "week")
      week.value = await api(`/weekly-plans/by-week/${weekDate.value}`, {
        method: "PUT",
      });
    else
      day.value = await api(
        `/weekly-plans/${week.value.id}/daily-plans/${dayDate.value}`,
        { method: "PUT" },
      );
    error.value = "";
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function save() {
  if (busy.value) return;
  error.value = "";
  try {
    if (!week.value || week.value.weekStartDate !== weekDate.value)
      throw new Error("请先创建或选择周计划");
    if (
      dayDate.value &&
      (day.value?.planDate !== dayDate.value ||
        day.value?.weeklyPlanId !== week.value.id)
    )
      throw new Error("日计划尚未加载或与所选周不一致");
    if (dayDate.value && !day.value) throw new Error("请先创建或选择日计划");
    if (pastWeek.value || (dayDate.value && dayDate.value < today()))
      throw new Error("不能安排到过去的计划");
    busy.value = true;
    await api(`/tasks/${props.taskId}/assignment`, {
      method: "PUT",
      body: {
        weeklyPlanId: week.value.id,
        dailyPlanId: dayDate.value ? day.value.id : null,
        assignmentId: current.value?.id || null,
        version: current.value?.version ?? null,
        sortOrder: order.value,
      },
    });
    emit("saved");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(async () => {
  try {
    current.value = await api(`/tasks/${props.taskId}/assignment`);
    order.value = current.value?.sortOrder || 0;
    let originalWeek, originalDay;
    if (current.value) {
      originalWeek = await api(`/weekly-plans/${current.value.weeklyPlanId}`);
      if (current.value.dailyPlanId)
        originalDay = await api(`/daily-plans/${current.value.dailyPlanId}`);
    }
    if (!props.weekStart && originalWeek)
      weekDate.value = originalWeek.weekStartDate;
    week.value = await api(`/weekly-plans/by-week/${weekDate.value}`);
    dayDate.value =
      props.planDate || (!props.weekStart ? originalDay?.planDate || "" : "");
    await dayChanged();
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
</script>
<template>
  <el-dialog
    :model-value="true"
    @close="emit('close')"
    title="安排任务／改期"
    width="min(560px,94vw)"
    :close-on-click-modal="false"
    ><el-alert
      title="改期保留原任务及安排历史，不会复制任务。清空日期表示只安排到周。"
      type="info"
      :closable="false"
    /><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-skeleton v-if="loading" :rows="4" /><el-form
      v-else
      label-position="top"
      @submit.prevent="save"
      ><el-form-item label="目标周"
        ><el-date-picker
          v-model="weekDate" :disabled="busy"
          type="date"
          value-format="YYYY-MM-DD"
          :clearable="false"
          @change="weekChanged"
        />
        <p class="hint">
          周一 {{ weekDate }} 至周日 {{ weekDate ? addDays(weekDate, 6) : "" }}
        </p></el-form-item
      ><el-button
        v-if="
          !week && weekDate && !pastWeek && canGrow('planning:weekly:manage')
        "
        @click="create('week')"
        :loading="busy"
        >创建该周计划</el-button
      ><el-form-item label="具体日期（可空）"
        ><el-date-picker
          v-model="dayDate"
          value-format="YYYY-MM-DD"
          :disabled="!week || busy"
          @change="dayChanged"
          :disabled-date="
            (d) => {
              const local = [
                d.getFullYear(),
                String(d.getMonth() + 1).padStart(2, '0'),
                String(d.getDate()).padStart(2, '0'),
              ].join('-');
              return (
                local < weekDate ||
                local > addDays(weekDate, 6) ||
                local < today()
              );
            }
          "
          clearable /></el-form-item
      ><el-button
        v-if="
          week &&
          dayDate &&
          !day &&
          dayDate >= today() &&
          canGrow('planning:daily:manage')
        "
        @click="create('day')"
        :loading="busy"
        >创建该日日计划</el-button
      ><el-form-item label="计划内排序"
        ><el-input-number
          v-model="order"
          :min="0"
          :max="2147483647"
          :precision="0"
      /></el-form-item>
      <div class="dialog-actions">
        <el-button @click="emit('close')">取消</el-button
        ><el-button
          v-if="canGrow('planning:assignment:manage')"
          type="primary"
          native-type="submit"
          :loading="busy"
          :disabled="!week || (!!dayDate && !day) || pastWeek"
          >保存安排</el-button
        >
      </div></el-form
    ></el-dialog
  >
</template>
