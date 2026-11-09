<!-- 渲染习惯Editor界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
import { ElMessage } from "element-plus";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { habitApi, signalChange } from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { today, canGrow } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { confirmAction, report } from "../../api/ui";
/* 引入 ./PagedSelect.vue 提供的组件、状态函数或请求方法。 */
import PagedSelect from "./PagedSelect.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */ modelValue: Boolean, /* 来源（source）字段使用对象（Object）。 */ source: Object });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue", "saved"]);
const /* 是否显示（visible）保存随相关状态变化自动重算的值。 */ visible = computed({
  /* 读取（get）字段使用表达式 () => props.modelValue 的结果。 */
  get: () => props.modelValue,
  /* 设置（set）字段使用表达式 (v) => emit("update:modelValue", v) 的结果。 */
  set: (v) => emit("update:modelValue", v),
});
const /* form响应式引用（formRef）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ formRef = ref(null),
  /* 正在加载（loading）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  loading = ref(false),
  /* saving保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  saving = ref(false),
  /* 已就绪（ready）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  ready = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* conflict保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  conflict = ref(false),
  /* 分类显示名称（categoryLabel）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  categoryLabel = ref(""),
  /* 分类删除标记（categoryDeleted）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  categoryDeleted = ref(false);
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = reactive({
  /* 名称（name）字段使用固定文字 ''。 */
  name: "",
  /* 说明（description）字段使用固定文字 ''。 */
  description: "",
  /* 完成情况判断标准（completionCriteria）字段使用固定文字 ''。 */
  completionCriteria: "",
  /* 分类编号（categoryId）字段使用固定文字 ''。 */
  categoryId: "",
  /* cycle类型（cycleType）字段使用固定状态或编码 DAILY（每日）。 */
  cycleType: "DAILY",
  /* weekdays字段使用一个空列表，用来保存后续多条数据。 */
  weekdays: [],
  /* 开始日期（startDate）字段使用固定文字 ''。 */
  startDate: "",
  /* 预计时间（expectedTime）字段使用固定文字 ''。 */
  expectedTime: "",
  /* 版本号（version）字段使用空值，表示当前还没有对应内容。 */
  version: undefined,
});
const /* rules保存表达式 { name: [ { required: true, whitespace: true, message: "请填写习惯名称", trigger: "blur", }, ], startDate: [{ required: true, m… 的结果。 */ rules = {
  /* 名称（name）字段使用表达式 [ { required: true, whitespace: true, message: "请填写习惯名称", trigger: "blur", }, ] 的结果。 */
  name: [
    {
      /* 必填（required）字段使用真，表示条件成立或开关打开。 */
      required: true,
      /* whitespace字段使用真，表示条件成立或开关打开。 */
      whitespace: true,
      /* 消息（message）字段使用固定文字 '请填写习惯名称'。 */
      message: "请填写习惯名称",
      /* trigger字段使用固定文字 'blur'。 */
      trigger: "blur",
    },
  ],
  /* 开始日期（startDate）字段使用表达式 [{ required: true, message: "请选择开始日期", trigger: "change" }] 的结果。 */
  startDate: [{ /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true, /* 消息（message）字段使用固定文字 '请选择开始日期'。 */ message: "请选择开始日期", /* trigger字段使用固定文字 'change'。 */ trigger: "change" }],
};
const /* 分类列表（categories）保存调用 (params, signal) => habitApi.categories 完成分类列表处理。 */ categories = (params, signal) => habitApi.categories(params, signal);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 把假，表示条件不成立或开关关闭写入 conflict.value。 */
  conflict.value = false;
  /* 把真，表示条件成立或开关打开写入 loading.value。 */
  loading.value = true;
  /* 把假，表示条件不成立或开关关闭写入 ready.value。 */
  ready.value = false;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 当前记录（row）保存沿 props.source?.id ? canGrow("habit:habit:read") ? await habitApi.detail(props.source.id) : props.source : {} 读取数据；中间对象为空时返回空值。 */ row = props.source?.id
      ? canGrow("habit:habit:read")
        ? await habitApi.detail(props.source.id)
        : props.source
      : {};
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(form, {
      /* 名称（name）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.name || ""。 */
      name: row.name || "",
      /* 说明（description）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.description || ""。 */
      description: row.description || "",
      /* 完成情况判断标准（completionCriteria）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.completionCriteria || ""。 */
      completionCriteria: row.completionCriteria || "",
      /* 分类编号（categoryId）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.categoryId || ""。 */
      categoryId: row.categoryId || "",
      /* cycle类型（cycleType）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.cycleType || "DAILY"。 */
      cycleType: row.cycleType || "DAILY",
      /* weekdays字段使用优先使用前面的有效值，没有时采用后面的备用值：row.weekdays || []。 */
      weekdays: row.weekdays || [],
      /* 开始日期（startDate）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.startDate || today()。 */
      startDate: row.startDate || today(),
      /* 预计时间（expectedTime）字段使用优先使用前面的有效值，没有时采用后面的备用值：row.expectedTime || ""。 */
      expectedTime: row.expectedTime || "",
      /* 版本号（version）字段使用表达式 row.version 的结果。 */
      version: row.version,
    });
    /* 把优先使用前面的有效值，没有时采用后面的备用值：row.categoryName || ""写入 categoryLabel.value。 */
    categoryLabel.value = row.categoryName || "";
    /* 把条件判断的结果：!!row.categoryDeleted写入 categoryDeleted.value。 */
    categoryDeleted.value = !!row.categoryDeleted;
    /* 把真，表示条件成立或开关打开写入 ready.value。 */
    ready.value = true;
    /* 调用 formRef.value?.clearValidate 完成清除检查合法性处理。 */
    formRef.value?.clearValidate();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 loading.value。 */ loading.value = false;
  }
}
/* reload：调用 reload 完成reload处理。 */
async function reload() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待确认 操作（Action）。 */
    await confirmAction("重新读取会替换当前尚未保存的资料，是否继续？");
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 保存（save）：调用 save 完成保存处理。 */
async function save() {
  /* 检查条件 saving.value || !ready.value 是否成立，成立时走这一分支。 */
  if (saving.value || !ready.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 !(await formRef.value?.validate().catch(() => false)) 是否成立，成立时走这一分支。 */
  if (!(await formRef.value?.validate().catch(() => false))) /* 返回空内容，当前函数到此结束。 */ return;
  /* 检查条件 form.cycleType === "WEEKLY_DAYS" && !form.weekdays.length 是否成立，成立时走这一分支。 */
  if (form.cycleType === "WEEKLY_DAYS" && !form.weekdays.length) {
    /* 把固定文字 '请至少选择一个执行星期'写入 error.value。 */
    error.value = "请至少选择一个执行星期";
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 把真，表示条件成立或开关打开写入 saving.value。 */
  saving.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 当前记录（row）保存沿 await habitApi.save(props.source?.id, { ...form, name: form.name.trim(), categoryId: form.categoryId || null, weekd… 读取数据；中间对象为空时返回空值。 */ row = await habitApi.save(props.source?.id, {
      ...form,
      /* 名称（name）字段使用去掉文字两端的空白。 */
      name: form.name.trim(),
      /* 分类编号（categoryId）字段使用优先使用前面的有效值，没有时采用后面的备用值：form.categoryId || null。 */
      categoryId: form.categoryId || null,
      /* weekdays字段使用根据前面的条件，从两个候选结果中选择一个：form.cycleType === "DAILY" ? null : [...form.weekdays]。 */
      weekdays: form.cycleType === "DAILY" ? null : [...form.weekdays],
      /* 预计时间（expectedTime）字段使用优先使用前面的有效值，没有时采用后面的备用值：form.expectedTime || null。 */
      expectedTime: form.expectedTime || null,
    });
    /* 调用 ElMessage.success 完成成功处理。 */
    ElMessage.success(props.source?.id ? "习惯已保存" : "习惯已创建");
    /* 调用 signalChange 完成取消信号修改处理。 */
    signalChange("habit");
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("saved", row);
    /* 把假，表示条件不成立或开关关闭写入 visible.value。 */
    visible.value = false;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
    /* 把条件判断的结果：e.status === 409写入 conflict.value。 */
    conflict.value = e.status === 409;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 saving.value。 */
    saving.value = false;
  }
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => [props.modelValue, props.source?.id],
  () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
);
/* 注册组件销毁前的清理，取消请求并释放定时器。 */
onBeforeUnmount(() => sequence++);
</script>
<template>
  <!-- 显示Element Plus 的 dialog 组件；与 visible 双向同步；title 取自 source?.id ? '编辑习惯和执行规则' : '创建独立习惯'；width 固定为 min(720px, 96vw)；close-on-click-modal 取自 false；close-on-press-escape 取自 !saving；before-close 取自 (done) => { if (!saving) done(); }；启用 destroy-on-close 属性。 -->
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
    <!-- 显示Element Plus 的 skeleton 组件；仅当 loading 成立时渲染；rows 取自 7；启用 animated 属性。 -->
    <el-skeleton v-if="loading" :rows="7" animated />
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <!-- 显示Element Plus 的 button 组件；仅当 !loading && !ready 成立时渲染；启用 text 属性；类型设为 primary；click 事件触发时执行 load。 -->
    <el-button v-if="!loading && !ready" text type="primary" @click="load"
      >重新读取习惯资料</el-button
    >
    <!-- 显示Element Plus 的 form 组件；仅当 !loading && ready 成立时渲染；把组件或元素引用保存为 formRef，供脚本读取；model 取自 form；rules 取自 rules；label-position 固定为 top；submit 事件触发时执行 save。 -->
    <el-form
      v-if="!loading && ready"
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      @submit.prevent="save"
    >
      <!-- 显示Element Plus 的 button 组件；仅当 conflict && source?.id && canGrow('habit:habit:read') 成立时渲染；启用 text 属性；类型设为 primary；click 事件触发时执行 reload。 -->
      <el-button
        v-if="conflict && source?.id && canGrow('habit:habit:read')"
        text
        type="primary"
        @click="reload"
        >重新读取当前资料</el-button
      >
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 习惯名称；prop 固定为 name。 -->
      <el-form-item label="习惯名称" prop="name"
        ><!-- 显示Element Plus 的 input 组件；与 form.name 双向同步；maxlength 固定为 100；启用 show-word-limit 属性。 --><el-input v-model="form.name" maxlength="100" show-word-limit
      /></el-form-item>
      <!-- 显示一个布局容器；用样式类 form-grid 控制外观。 -->
      <div class="form-grid">
        <!-- 显示Element Plus 的 form-item 组件；label 固定为 个人分类。 -->
        <el-form-item label="个人分类">
          <!-- 显示Paged选择（PagedSelect）组件；与 form.categoryId 双向同步；loader 取自 categories；label 取自 categoryLabel；selected-disabled 取自 categoryDeleted；未输入时提示 未分类；disabled 取自 !canGrow('habit:category:list')。 -->
          <PagedSelect
            v-model="form.categoryId"
            :loader="categories"
            :label="categoryLabel"
            :selected-disabled="categoryDeleted"
            placeholder="未分类"
            :disabled="!canGrow('habit:category:list')"
          />
          <!-- 显示行内文字区域；仅当 categoryDeleted 成立时渲染；用样式类 hint 控制外观。 -->
          <span v-if="categoryDeleted" class="hint"
            >原分类已删除，可保留引用或改为其他有效分类。</span
          > </el-form-item
        ><!-- 显示Element Plus 的 form-item 组件；label 固定为 开始日期；prop 固定为 startDate。 --><el-form-item label="开始日期" prop="startDate"
          ><!-- 显示Element Plus 的 date-picker 组件；与 form.startDate 双向同步；类型设为 date；value-format 固定为 YYYY-MM-DD。 --><el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
        /></el-form-item>
      </div>
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 执行周期。 -->
      <el-form-item label="执行周期"
        ><!-- 显示Element Plus 的 radio-group 组件；与 form.cycleType 双向同步。 --><el-radio-group v-model="form.cycleType"
          ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 DAILY。 --><el-radio-button value="DAILY">每天</el-radio-button
          ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 WEEKLY_DAYS。 --><el-radio-button value="WEEKLY_DAYS"
            >指定星期</el-radio-button
          ></el-radio-group
        ></el-form-item
      >
      <!-- 显示Element Plus 的 form-item 组件；仅当 form.cycleType === 'WEEKLY_DAYS' 成立时渲染；label 固定为 执行星期。 -->
      <el-form-item v-if="form.cycleType === 'WEEKLY_DAYS'" label="执行星期">
        <!-- 显示Element Plus 的 checkbox-group 组件；与 form.weekdays 双向同步。 -->
        <el-checkbox-group v-model="form.weekdays"
          ><!-- 显示Element Plus 的 checkbox 组件；按 (name, index) in ['一', '二', '三', '四', '五', '六', '日'] 为每条数据重复生成内容；key 取自 name；value 取自 index + 1。 --><el-checkbox
            v-for="(name, index) in ['一', '二', '三', '四', '五', '六', '日']"
            :key="name"
            :value="index + 1"
            >周<!-- 把 name 的当前值转成文字显示。 -->{{ name }}</el-checkbox
          ></el-checkbox-group
        >
      </el-form-item>
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 提示时间（可选）。 -->
      <el-form-item label="提示时间（可选）"
        ><!-- 显示Element Plus 的 time-picker 组件；与 form.expectedTime 双向同步；format 固定为 HH:mm；value-format 固定为 HH:mm；启用 clearable 属性；未输入时提示 不设置。 --><el-time-picker
          v-model="form.expectedTime"
          format="HH:mm"
          value-format="HH:mm"
          clearable
          placeholder="不设置"
        /><!-- 显示行内文字区域；用样式类 hint field-hint 控制外观。 --><span class="hint field-hint"
          >仅作时间提示，不是截止时间，也不会自动发送提醒。</span
        ></el-form-item
      >
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 完成标准。 -->
      <el-form-item label="完成标准"
        ><!-- 显示Element Plus 的 input 组件；与 form.completionCriteria 双向同步；类型设为 textarea；rows 取自 2；maxlength 固定为 10000；未输入时提示 今天做到什么，就算完成。 --><el-input
          v-model="form.completionCriteria"
          type="textarea"
          :rows="2"
          maxlength="10000"
          placeholder="今天做到什么，就算完成"
      /></el-form-item>
      <!-- 显示Element Plus 的 form-item 组件；label 固定为 描述。 -->
      <el-form-item label="描述"
        ><!-- 显示Element Plus 的 input 组件；与 form.description 双向同步；类型设为 textarea；rows 取自 3；maxlength 固定为 10000。 --><el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="10000"
      /></el-form-item>
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">
        修改对今天生效，不回写过去；今天已经完成的打卡仍保留一次完成。
      </p>
    </el-form>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 footer 插槽。 -->
    <template #footer
      ><!-- 显示Element Plus 的 button 组件；disabled 取自 saving；click 事件触发时执行 visible = false。 --><el-button :disabled="saving" @click="visible = false">取消</el-button
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；disabled 取自 loading || !ready；loading 取自 saving；click 事件触发时执行 save。 --><el-button
        type="primary"
        :disabled="loading || !ready"
        :loading="saving"
        @click="save"
        >保存习惯</el-button
      ></template
    >
  </el-dialog>
</template>
