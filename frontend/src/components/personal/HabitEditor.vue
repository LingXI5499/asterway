<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
import { ElMessage } from "element-plus";
import { habitApi, signalChange } from "../../api/personal";
import { today, canGrow } from "../../api/growth";
import { confirmAction, report } from "../../api/ui";
import PagedSelect from "./PagedSelect.vue";
const props = defineProps({ modelValue: Boolean, source: Object });
const emit = defineEmits(["update:modelValue", "saved"]);
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit("update:modelValue", v),
});
const formRef = ref(null),
  loading = ref(false),
  saving = ref(false),
  ready = ref(false),
  error = ref(""),
  conflict = ref(false),
  categoryLabel = ref(""),
  categoryDeleted = ref(false);
const form = reactive({
  name: "",
  description: "",
  completionCriteria: "",
  categoryId: "",
  cycleType: "DAILY",
  weekdays: [],
  startDate: "",
  expectedTime: "",
  version: undefined,
});
const rules = {
  name: [
    {
      required: true,
      whitespace: true,
      message: "请填写习惯名称",
      trigger: "blur",
    },
  ],
  startDate: [{ required: true, message: "请选择开始日期", trigger: "change" }],
};
const categories = (params, signal) => habitApi.categories(params, signal);
let sequence = 0;
async function load() {
  const seq = ++sequence;
  error.value = "";
  conflict.value = false;
  loading.value = true;
  ready.value = false;
  try {
    const row = props.source?.id
      ? canGrow("habit:habit:read")
        ? await habitApi.detail(props.source.id)
        : props.source
      : {};
    if (seq !== sequence) return;
    Object.assign(form, {
      name: row.name || "",
      description: row.description || "",
      completionCriteria: row.completionCriteria || "",
      categoryId: row.categoryId || "",
      cycleType: row.cycleType || "DAILY",
      weekdays: row.weekdays || [],
      startDate: row.startDate || today(),
      expectedTime: row.expectedTime || "",
      version: row.version,
    });
    categoryLabel.value = row.categoryName || "";
    categoryDeleted.value = !!row.categoryDeleted;
    ready.value = true;
    formRef.value?.clearValidate();
  } catch (e) {
    if (seq === sequence) error.value = e.message;
  } finally {
    if (seq === sequence) loading.value = false;
  }
}
async function reload() {
  try {
    await confirmAction("重新读取会替换当前尚未保存的资料，是否继续？");
    await load();
  } catch (e) {
    report(e);
  }
}
async function save() {
  if (saving.value || !ready.value) return;
  if (!(await formRef.value?.validate().catch(() => false))) return;
  if (form.cycleType === "WEEKLY_DAYS" && !form.weekdays.length) {
    error.value = "请至少选择一个执行星期";
    return;
  }
  saving.value = true;
  error.value = "";
  try {
    const row = await habitApi.save(props.source?.id, {
      ...form,
      name: form.name.trim(),
      categoryId: form.categoryId || null,
      weekdays: form.cycleType === "DAILY" ? null : [...form.weekdays],
      expectedTime: form.expectedTime || null,
    });
    ElMessage.success(props.source?.id ? "习惯已保存" : "习惯已创建");
    signalChange("habit");
    emit("saved", row);
    visible.value = false;
  } catch (e) {
    error.value = e.message;
    conflict.value = e.status === 409;
  } finally {
    saving.value = false;
  }
}
watch(
  () => [props.modelValue, props.source?.id],
  () => {
    sequence++;
    if (props.modelValue) load();
  },
  { immediate: true },
);
onBeforeUnmount(() => sequence++);
</script>
<template>
  <el-dialog
    v-model="visible"
    :title="source?.id ? '编辑习惯和执行规则' : '创建独立习惯'"
    width="min(720px, 96vw)"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :before-close="
      (done) => {
        if (!saving) done();
      }
    "
    destroy-on-close
  >
    <el-skeleton v-if="loading" :rows="7" animated />
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-button v-if="!loading && !ready" text type="primary" @click="load"
      >重新读取习惯资料</el-button
    >
    <el-form
      v-if="!loading && ready"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="save"
    >
      <el-button
        v-if="conflict && source?.id && canGrow('habit:habit:read')"
        text
        type="primary"
        @click="reload"
        >重新读取当前资料</el-button
      >
      <el-form-item label="习惯名称" prop="name"
        ><el-input v-model="form.name" maxlength="100" show-word-limit
      /></el-form-item>
      <div class="form-grid">
        <el-form-item label="个人分类">
          <PagedSelect
            v-model="form.categoryId"
            :loader="categories"
            :label="categoryLabel"
            :selected-disabled="categoryDeleted"
            placeholder="未分类"
            :disabled="!canGrow('habit:category:list')"
          />
          <span v-if="categoryDeleted" class="hint"
            >原分类已删除，可保留引用或改为其他有效分类。</span
          > </el-form-item
        ><el-form-item label="开始日期" prop="startDate"
          ><el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
        /></el-form-item>
      </div>
      <el-form-item label="执行周期"
        ><el-radio-group v-model="form.cycleType"
          ><el-radio-button value="DAILY">每天</el-radio-button
          ><el-radio-button value="WEEKLY_DAYS"
            >指定星期</el-radio-button
          ></el-radio-group
        ></el-form-item
      >
      <el-form-item v-if="form.cycleType === 'WEEKLY_DAYS'" label="执行星期">
        <el-checkbox-group v-model="form.weekdays"
          ><el-checkbox
            v-for="(name, index) in ['一', '二', '三', '四', '五', '六', '日']"
            :key="name"
            :value="index + 1"
            >周{{ name }}</el-checkbox
          ></el-checkbox-group
        >
      </el-form-item>
      <el-form-item label="提示时间（可选）"
        ><el-time-picker
          v-model="form.expectedTime"
          format="HH:mm"
          value-format="HH:mm"
          clearable
          placeholder="不设置"
        /><span class="hint field-hint"
          >仅作时间提示，不是截止时间，也不会自动发送提醒。</span
        ></el-form-item
      >
      <el-form-item label="完成标准"
        ><el-input
          v-model="form.completionCriteria"
          type="textarea"
          :rows="2"
          maxlength="10000"
          placeholder="今天做到什么，就算完成"
      /></el-form-item>
      <el-form-item label="描述"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="10000"
      /></el-form-item>
      <p class="hint">
        修改对今天生效，不回写过去；今天已经完成的打卡仍保留一次完成。
      </p>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="visible = false">取消</el-button
      ><el-button
        type="primary"
        :disabled="loading || !ready"
        :loading="saving"
        @click="save"
        >保存习惯</el-button
      ></template
    >
  </el-dialog>
</template>
