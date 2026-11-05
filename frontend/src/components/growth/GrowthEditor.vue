<script setup>
// 目标、阶段、任务共用资料表单，保存字段采用明确白名单。
import { ref, onMounted } from "vue";
import { api } from "../../api/http";
import { getResource, resources, canGrow, permission } from "../../api/growth";
import GrowthSelector from "./GrowthSelector.vue";
const props = defineProps({
  kind: String,
  id: [String, Number],
  parentId: [String, Number],
});
const emit = defineEmits(["close", "saved"]);
const form = ref({
    name: "",
    description: "",
    completionCriteria: "",
    startDate: null,
    expectedEndDate: null,
    endDate: null,
    dueDate: null,
    priority: "MEDIUM",
    weight: 1,
    estimatedMinutes: null,
    sortOrder: 0,
  }),
  goalId = ref(""),
  stageId = ref(""),
  busy = ref(false),
  loading = ref(true),
  error = ref("");
onMounted(async () => {
  try {
    if (props.id) form.value = await getResource(props.kind, props.id);
    else if (props.parentId) {
      if (props.kind === "stage") goalId.value = String(props.parentId);
      if (props.kind === "task") {
        stageId.value = String(props.parentId);
        goalId.value = (await getResource("stage", props.parentId)).goalId;
      }
    }
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
});
async function save() {
  if (busy.value) return;
  error.value = "";
  try {
    if (!form.value.name?.trim() || Array.from(form.value.name).length > 100)
      throw new Error("名称须为 1～100 个字符");
    const f = form.value,
      end =
        props.kind === "goal"
          ? f.expectedEndDate
          : props.kind === "stage"
            ? f.endDate
            : f.dueDate;
    if (f.startDate && end && end < f.startDate)
      throw new Error("结束日期不能早于开始日期");
    const body = {
      name: f.name,
      description: f.description || "",
      completionCriteria: f.completionCriteria || "",
      startDate: f.startDate || null,
      ...(props.id ? { version: f.version } : {}),
    };
    if (props.kind === "goal") body.expectedEndDate = end || null;
    if (props.kind === "stage")
      Object.assign(body, {
        endDate: end || null,
        sortOrder: f.sortOrder || 0,
      });
    if (props.kind === "task") {
      if (!Number.isInteger(f.weight) || f.weight <= 0)
        throw new Error("权重须为正整数");
      if (
        f.estimatedMinutes !== null &&
        f.estimatedMinutes !== undefined &&
        (!Number.isInteger(f.estimatedMinutes) || f.estimatedMinutes <= 0)
      )
        throw new Error("预计耗时须为正整数，未知可留空");
      Object.assign(body, {
        dueDate: end || null,
        priority: f.priority,
        weight: f.weight,
        estimatedMinutes: f.estimatedMinutes ?? null,
        sortOrder: f.sortOrder || 0,
      });
    }
    let path = props.id
      ? `/${resources[props.kind]}/${props.id}`
      : props.kind === "goal"
        ? "/goals"
        : props.kind === "stage"
          ? `/goals/${goalId.value}/stages`
          : `/stages/${stageId.value}/tasks`;
    if (!props.id && props.kind === "stage" && !goalId.value)
      throw new Error("请选择所属目标");
    if (!props.id && props.kind === "task" && !stageId.value)
      throw new Error("请选择所属阶段");
    busy.value = true;
    await api(path, { method: props.id ? "PUT" : "POST", body });
    emit("saved");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <el-dialog
    :model-value="true"
    @close="emit('close')"
    :title="
      (id ? '编辑' : '新增') +
      { goal: '目标', stage: '阶段', task: '任务' }[kind]
    "
    width="min(640px,94vw)"
    :close-on-click-modal="false"
    ><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><el-skeleton v-if="loading" :rows="6" /><el-form
      v-else
      label-position="top"
      @submit.prevent="save"
      ><el-form-item v-if="!id && kind !== 'goal'" label="业务归属"
        ><GrowthSelector
          v-model:goal-id="goalId"
          v-model:stage-id="stageId"
          :goal-only="kind === 'stage'"
          :disabled="!!parentId" /></el-form-item
      ><el-form-item label="名称"
        ><el-input
          v-model="form.name"
          maxlength="100"
          show-word-limit /></el-form-item
      ><el-form-item label="描述"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="20000" /></el-form-item
      ><el-form-item label="完成条件"
        ><el-input
          v-model="form.completionCriteria"
          type="textarea"
          :rows="2"
          maxlength="20000"
      /></el-form-item>
      <div class="form-grid">
        <el-form-item label="开始日期"
          ><el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            clearable /></el-form-item
        ><el-form-item
          :label="
            kind === 'goal'
              ? '预计结束日期'
              : kind === 'stage'
                ? '结束日期'
                : '截止日期'
          "
          ><el-date-picker
            v-if="kind === 'goal'"
            v-model="form.expectedEndDate"
            value-format="YYYY-MM-DD"
            clearable /><el-date-picker
            v-else-if="kind === 'stage'"
            v-model="form.endDate"
            value-format="YYYY-MM-DD"
            clearable /><el-date-picker
            v-else
            v-model="form.dueDate"
            value-format="YYYY-MM-DD"
            clearable
        /></el-form-item>
      </div>
      <template v-if="kind === 'task'"
        ><div class="form-grid">
          <el-form-item label="优先级"
            ><el-select v-model="form.priority"
              ><el-option
                v-for="(label, value) in {
                  LOW: '低',
                  MEDIUM: '中',
                  HIGH: '高',
                  URGENT: '紧急',
                }"
                :key="value"
                :value="value"
                :label="label" /></el-select></el-form-item
          ><el-form-item label="进度权重"
            ><el-input-number
              v-model="form.weight"
              :min="1"
              :max="2147483647"
              :precision="0"
          /></el-form-item>
        </div>
        <el-form-item label="预计耗时（分钟，未知留空）"
          ><el-input-number
            v-model="form.estimatedMinutes"
            :min="1"
            :max="2147483647"
            :precision="0" /></el-form-item></template
      ><el-form-item v-if="kind !== 'goal'" label="排序"
        ><el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="2147483647"
          :precision="0"
      /></el-form-item>
      <div class="dialog-actions">
        <el-button @click="emit('close')">取消</el-button
        ><el-button
          v-if="canGrow(permission(kind, id ? 'update' : 'create'))"
          type="primary"
          native-type="submit"
          :loading="busy"
          >保存</el-button
        >
      </div></el-form
    ></el-dialog
  >
</template>
