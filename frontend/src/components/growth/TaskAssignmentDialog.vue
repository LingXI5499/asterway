<!-- 日期只是安排的选择条件，确认保存后才建立缺少的周和当天记录。 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { api } from "../../api/http";
import { monday, today, addDays, validDate, canGrow } from "../../api/growth";
import { ensurePlan } from "../../api/planning";

const props = defineProps({ taskId: [String, Number], weekStart: String, planDate: String });
const emit = defineEmits(["close", "saved"]);
const weekDate = ref(props.weekStart || monday(today())), dayDate = ref(props.planDate || "");
const current = ref(null), order = ref(0), loading = ref(true), busy = ref(false), error = ref("");
const pastWeek = computed(() => addDays(weekDate.value, 6) < today());
function weekChanged(value) {
  if (!validDate(value)) return;
  weekDate.value = monday(value);
  if (dayDate.value && monday(dayDate.value) !== weekDate.value) dayDate.value = "";
}
function disabledDate(value) {
  const date = [value.getFullYear(), String(value.getMonth() + 1).padStart(2, "0"), String(value.getDate()).padStart(2, "0")].join("-");
  return date < today() || monday(date) !== weekDate.value;
}
function beforeClose(done) { if (!busy.value) done(); }

async function save() {
  if (busy.value || loading.value) return;
  error.value = "";
  try {
    if (!validDate(weekDate.value) || pastWeek.value || (dayDate.value && (dayDate.value < today() || monday(dayDate.value) !== weekDate.value)))
      throw new Error("请选择今天或之后的有效安排日期");
    busy.value = true;
    const { week, day } = await ensurePlan({ weekStart: weekDate.value, planDate: dayDate.value || "" });
    await api(`/tasks/${props.taskId}/assignment`, {
      method: "PUT",
      body: { weeklyPlanId: week.id, dailyPlanId: day?.id || null,
        assignmentId: current.value?.id || null, version: current.value?.version ?? null, sortOrder: order.value },
    });
    emit("saved");
  } catch (e) { error.value = e.message; }
  finally { busy.value = false; }
}

onMounted(async () => {
  try {
    current.value = await api(`/tasks/${props.taskId}/assignment`);
    order.value = current.value?.sortOrder || 0;
    // 从任务自身进入改期时沿用原安排；从某日进入时使用该日作为默认值。
    if (current.value && !props.weekStart) {
      const week = await api(`/weekly-plans/${current.value.weeklyPlanId}`);
      weekDate.value = week.weekStartDate;
      if (current.value.dailyPlanId) dayDate.value = (await api(`/daily-plans/${current.value.dailyPlanId}`)).planDate;
    }
  } catch (e) { error.value = e.message; }
  finally { loading.value = false; }
});
</script>
<template>
  <el-dialog :model-value="true" title="安排任务／改期" width="min(560px,94vw)" align-center append-to-body
    :close-on-click-modal="false" :before-close="beforeClose" @close="emit('close')">
    <p class="muted">改期保留原任务和安排历史。日期留空时，只安排到这一周。</p>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-skeleton v-if="loading" :rows="4" animated />
    <el-form v-else label-position="top" :disabled="busy" @submit.prevent="save">
      <el-form-item label="目标周"><el-date-picker v-model="weekDate" type="date" value-format="YYYY-MM-DD" :clearable="false" @change="weekChanged" />
        <p class="hint">{{ weekDate }} 至 {{ addDays(weekDate, 6) }}</p></el-form-item>
      <el-form-item label="安排日期，可留空"><el-date-picker v-model="dayDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disabledDate" clearable /></el-form-item>
      <el-form-item label="计划内排序"><el-input-number v-model="order" :min="0" :max="2147483647" :precision="0" /></el-form-item>
      <div class="dialog-actions"><el-button :disabled="busy" @click="emit('close')">取消</el-button>
        <el-button v-if="canGrow('planning:assignment:manage')" type="primary" native-type="submit" :loading="busy" :disabled="pastWeek">保存安排</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>
