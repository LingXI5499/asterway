<!-- 渲染成长业务Editor界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 目标、阶段、任务共用资料表单，保存字段采用明确白名单。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted, useId } from "vue";
/* 引入 ../../api/http 提供的组件、状态函数或请求方法。 */
import { api } from "../../api/http";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { getResource, resources, canGrow, permission } from "../../api/growth";
/* 引入 ./GrowthSelector.vue 提供的组件、状态函数或请求方法。 */
import GrowthSelector from "./GrowthSelector.vue";
import { assignNewTask } from "../../api/planning";
import { today } from "../../api/growth";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 种类（kind）字段使用可为空的文字（String）。 */
  kind: String,
  /* 编号（id）字段使用表达式 [String, Number] 的结果。 */
  id: [String, Number],
  /* 上级编号（parentId）字段使用表达式 [String, Number] 的结果。 */
  parentId: [String, Number],
  // 从某天新增任务时，可以选择保存后一起安排到当天。
  planDate: String,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["close", "saved", "created"]);
const /* form保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ form = ref({
    /* 名称（name）字段使用固定文字 ''。 */
    name: "",
    /* 说明（description）字段使用固定文字 ''。 */
    description: "",
    /* 完成情况判断标准（completionCriteria）字段使用固定文字 ''。 */
    completionCriteria: "",
    /* 开始日期（startDate）字段使用空值，表示当前还没有对应内容。 */
    startDate: null,
    /* 预计结束日期（expectedEndDate）字段使用空值，表示当前还没有对应内容。 */
    expectedEndDate: null,
    /* 结束日期（endDate）字段使用空值，表示当前还没有对应内容。 */
    endDate: null,
    /* 截止日期（dueDate）字段使用空值，表示当前还没有对应内容。 */
    dueDate: null,
    /* priority字段使用固定状态或编码 MEDIUM（MEDIUM）。 */
    priority: "MEDIUM",
    /* 权重（weight）字段使用固定数值 1。 */
    weight: 1,
    /* estimated分钟数（estimatedMinutes）字段使用空值，表示当前还没有对应内容。 */
    estimatedMinutes: null,
    /* 排序顺序（sortOrder）字段使用固定数值 0。 */
    sortOrder: 0,
  }),
  /* 目标编号（goalId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  goalId = ref(""),
  /* 阶段编号（stageId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  stageId = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 正在加载（loading）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  loading = ref(true),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
/* 注册组件首次挂载完成后的处理。 */
onMounted(async () => {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 props.id 是否成立，成立时走这一分支。 */
    if (props.id) /* 把等待 读取 Resource 的值 完成后取得结果写入 form.value。 */ form.value = await getResource(props.kind, props.id);
    else /* 检查条件 props.parentId 是否成立，成立时走这一分支。 */ if (props.parentId) {
      /* 检查条件 props.kind === "stage" 是否成立，成立时走这一分支。 */
      if (props.kind === "stage") /* 把把输入转换成文字，统一比较或展示时的格式写入 goalId.value。 */ goalId.value = String(props.parentId);
      /* 检查条件 props.kind === "task" 是否成立，成立时走这一分支。 */
      if (props.kind === "task") {
        /* 把把输入转换成文字，统一比较或展示时的格式写入 stageId.value。 */
        stageId.value = String(props.parentId);
        /* 把表达式 (await getResource("stage", props.parentId)).goalId 的结果写入 goalId.value。 */
        goalId.value = (await getResource("stage", props.parentId)).goalId;
      }
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 loading.value。 */
    loading.value = false;
  }
});
/* 保存（save）：调用 save 完成保存处理。 */
// 创建成功后记住编号；后续安排失败只重试安排，不再重复创建任务。
const createdTask = ref(null);
const formId = useId();
function beforeClose(done) { if (!busy.value) done(); }
async function save(destination = "pool") {
  /* 检查条件 busy.value 是否成立，成立时走这一分支。 */
  if (busy.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 !form.value.name?.trim() || Array.from(form.value.name).length > 100 是否成立，成立时走这一分支。 */
    if (!form.value.name?.trim() || Array.from(form.value.name).length > 100)
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("名称须为 1～100 个字符");
    const /* f保存表达式 form.value 的结果。 */ f = form.value,
      /* 结束（end）保存根据前面的条件，从两个候选结果中选择一个：props.kind === "goal" ? f.expectedEndDate : props.kind === "stage" ? f.endDate : f.dueDate。 */
      end =
        props.kind === "goal"
          ? f.expectedEndDate
          : props.kind === "stage"
            ? f.endDate
            : f.dueDate;
    /* 检查条件 f.startDate && end && end < f.startDate 是否成立，成立时走这一分支。 */
    if (f.startDate && end && end < f.startDate)
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("结束日期不能早于开始日期");
    const /* 请求正文（body）保存根据前面的条件，从两个候选结果中选择一个：{ name: f.name, description: f.description || "", completionCriteria: f.completionCriteria || "", st…。 */ body = {
      /* 名称（name）字段使用表达式 f.name 的结果。 */
      name: f.name,
      /* 说明（description）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.description || ""。 */
      description: f.description || "",
      /* 完成情况判断标准（completionCriteria）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.completionCriteria || ""。 */
      completionCriteria: f.completionCriteria || "",
      /* 开始日期（startDate）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.startDate || null。 */
      startDate: f.startDate || null,
      ...(props.id ? { /* 版本号（version）字段使用表达式 f.version 的结果。 */ version: f.version } : {}),
    };
    /* 检查条件 props.kind === "goal" 是否成立，成立时走这一分支。 */
    if (props.kind === "goal") /* 把优先使用前面的有效值，没有时采用后面的备用值：end || null写入 body.expectedEndDate。 */ body.expectedEndDate = end || null;
    /* 检查条件 props.kind === "stage" 是否成立，成立时走这一分支。 */
    if (props.kind === "stage")
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(body, {
        /* 结束日期（endDate）字段使用优先使用前面的有效值，没有时采用后面的备用值：end || null。 */
        endDate: end || null,
        /* 排序顺序（sortOrder）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.sortOrder || 0。 */
        sortOrder: f.sortOrder || 0,
      });
    /* 检查条件 props.kind === "task" 是否成立，成立时走这一分支。 */
    if (props.kind === "task") {
      /* 检查条件 !Number.isInteger(f.weight) || f.weight <= 0 是否成立，成立时走这一分支。 */
      if (!Number.isInteger(f.weight) || f.weight <= 0)
        /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
        throw new Error("权重须为正整数");
      /* 检查f.estimatedMinutes !== null && f.estimatedMinutes !== undefined && (!Number.isInteger(f.estimatedMin…，即对应内容是否不存在，成立时走这一分支。 */
      if (
        f.estimatedMinutes !== null &&
        f.estimatedMinutes !== undefined &&
        (!Number.isInteger(f.estimatedMinutes) || f.estimatedMinutes <= 0)
      )
        /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
        throw new Error("预计耗时须为正整数，未知可留空");
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(body, {
        /* 截止日期（dueDate）字段使用优先使用前面的有效值，没有时采用后面的备用值：end || null。 */
        dueDate: end || null,
        /* priority字段使用表达式 f.priority 的结果。 */
        priority: f.priority,
        /* 权重（weight）字段使用表达式 f.weight 的结果。 */
        weight: f.weight,
        /* estimated分钟数（estimatedMinutes）字段使用前面的值为空时才使用后面的默认值：f.estimatedMinutes ?? null。 */
        estimatedMinutes: f.estimatedMinutes ?? null,
        /* 排序顺序（sortOrder）字段使用优先使用前面的有效值，没有时采用后面的备用值：f.sortOrder || 0。 */
        sortOrder: f.sortOrder || 0,
      });
    }
    let /* 路径（path）保存根据前面的条件，从两个候选结果中选择一个：props.id ? `/${resources[props.kind]}/${props.id}` : props.kind === "goal" ? "/goals" : props.kind =…。 */ path = props.id
      ? `/${resources[props.kind]}/${props.id}`
      : props.kind === "goal"
        ? "/goals"
        : props.kind === "stage"
          ? `/goals/${goalId.value}/stages`
          : `/stages/${stageId.value}/tasks`;
    /* 检查条件 !props.id && props.kind === "stage" && !goalId.value 是否成立，成立时走这一分支。 */
    if (!props.id && props.kind === "stage" && !goalId.value)
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择所属目标");
    /* 检查条件 !props.id && props.kind === "task" && !stageId.value 是否成立，成立时走这一分支。 */
    if (!props.id && props.kind === "task" && !stageId.value)
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择所属阶段");
    /* 把真，表示条件成立或开关打开写入 busy.value。 */
    busy.value = true;
    /* 等待调用 api 完成接口处理。 */
    const saved = createdTask.value || await api(path, { method: props.id ? "PUT" : "POST", body });
    if (!props.id && props.kind === "task" && !createdTask.value) {
      createdTask.value = saved;
      emit("created", saved);
    }
    if (destination === "day" && props.kind === "task" && !props.id) {
      if (!props.planDate || props.planDate < today()) throw new Error("任务已存入任务池，请选择今天或之后的日期安排");
      await assignNewTask(saved.id, props.planDate);
    }
    emit("saved", saved);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = createdTask.value ? "任务已创建并保留在任务池，安排未完成：" + e.message : e.message;
  } finally {
    /* 把假，表示条件不成立或开关关闭写入 busy.value。 */
    busy.value = false;
  }
}
</script>
<template>
  <!-- 显示Element Plus 的 dialog 组件；model-value 取自 true；close 事件触发时执行 emit('close')；title 取自 (id ? '编辑' : '新增') + { goal: '目标', stage: '阶段', task: '任务' }[kind]；width 固定为 min(640px,94vw)；close-on-click-modal 取自 false。 -->
  <el-dialog
    :model-value="true"
    @close="emit('close')"
    :title="
      (id ? '编辑' : '新增') +
      { goal: '目标', stage: '阶段', task: '任务' }[kind]
    "
    width="min(640px,94vw)"
    :close-on-click-modal="false"
    align-center append-to-body :before-close="beforeClose" class="plan-dialog"
    ><!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 --><el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示Element Plus 的 skeleton 组件；仅当 loading 成立时渲染；rows 取自 6。 --><el-skeleton v-if="loading" :rows="6" /><!-- 显示Element Plus 的 form 组件；前面条件均不成立时显示这个备用分支；label-position 固定为 top；submit 事件触发时执行 save。 --><el-form
      v-else
      :id="formId"
      label-position="top"
      :disabled="busy || !!createdTask"
      @submit.prevent="save('pool')"
      ><!-- 显示Element Plus 的 form-item 组件；仅当 !id && kind !== 'goal' 成立时渲染；label 固定为 业务归属。 --><el-form-item v-if="!id && kind !== 'goal'" label="业务归属"
        ><!-- 显示成长业务Selector（GrowthSelector）组件；与 goalId 双向同步（goal-id）；与 stageId 双向同步（stage-id）；goal-only 取自 kind === 'stage'；disabled 取自 !!parentId。 --><GrowthSelector
          v-model:goal-id="goalId"
          v-model:stage-id="stageId"
          :goal-only="kind === 'stage'"
          :disabled="!!parentId" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 名称。 --><el-form-item label="名称"
        ><!-- 显示Element Plus 的 input 组件；与 form.name 双向同步；maxlength 固定为 100；启用 show-word-limit 属性。 --><el-input
          v-model="form.name"
          maxlength="100"
          show-word-limit /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 描述。 --><el-form-item label="描述"
        ><!-- 显示Element Plus 的 input 组件；与 form.description 双向同步；类型设为 textarea；rows 取自 3；maxlength 固定为 20000。 --><el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="20000" /></el-form-item
      ><!-- 显示Element Plus 的 form-item 组件；label 固定为 完成条件。 --><el-form-item label="完成条件"
        ><!-- 显示Element Plus 的 input 组件；与 form.completionCriteria 双向同步；类型设为 textarea；rows 取自 2；maxlength 固定为 20000。 --><el-input
          v-model="form.completionCriteria"
          type="textarea"
          :rows="2"
          maxlength="20000"
      /></el-form-item>
      <!-- 显示一个布局容器；用样式类 form-grid 控制外观。 -->
      <div class="form-grid">
        <!-- 显示Element Plus 的 form-item 组件；label 固定为 开始日期。 -->
        <el-form-item label="开始日期"
          ><!-- 显示Element Plus 的 date-picker 组件；与 form.startDate 双向同步；类型设为 date；value-format 固定为 YYYY-MM-DD；启用 clearable 属性。 --><el-date-picker
            v-model="form.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            clearable /></el-form-item
        ><!-- 显示Element Plus 的 form-item 组件；label 取自 kind === 'goal' ? '预计结束日期' : kind === 'stage' ? '结束日期' : '截止日期'。 --><el-form-item
          :label="
            kind === 'goal'
              ? '预计结束日期'
              : kind === 'stage'
                ? '结束日期'
                : '截止日期'
          "
          ><!-- 显示Element Plus 的 date-picker 组件；仅当 kind === 'goal' 成立时渲染；与 form.expectedEndDate 双向同步；value-format 固定为 YYYY-MM-DD；启用 clearable 属性。 --><el-date-picker
            v-if="kind === 'goal'"
            v-model="form.expectedEndDate"
            value-format="YYYY-MM-DD"
            clearable /><!-- 显示Element Plus 的 date-picker 组件；前一条件不成立且 kind === 'stage' 成立时渲染；与 form.endDate 双向同步；value-format 固定为 YYYY-MM-DD；启用 clearable 属性。 --><el-date-picker
            v-else-if="kind === 'stage'"
            v-model="form.endDate"
            value-format="YYYY-MM-DD"
            clearable /><!-- 显示Element Plus 的 date-picker 组件；前面条件均不成立时显示这个备用分支；与 form.dueDate 双向同步；value-format 固定为 YYYY-MM-DD；启用 clearable 属性。 --><el-date-picker
            v-else
            v-model="form.dueDate"
            value-format="YYYY-MM-DD"
            clearable
        /></el-form-item>
      </div>
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 kind === 'task' 成立时渲染。 -->
      <template v-if="kind === 'task'"
        ><!-- 显示一个布局容器；用样式类 form-grid 控制外观。 --><div class="form-grid">
          <!-- 显示Element Plus 的 form-item 组件；label 固定为 优先级。 -->
          <el-form-item label="优先级"
            ><!-- 显示Element Plus 的 select 组件；与 form.priority 双向同步。 --><el-select v-model="form.priority"
              ><!-- 显示Element Plus 的 option 组件；按 (label, value) in { LOW: '低', MEDIUM: '中', HIGH: '高', URGENT: '紧急', } 为每条数据重复生成内容；key 取自 value；value 取自 value；label 取自 label。 --><el-option
                v-for="(label, value) in {
                  LOW: '低',
                  MEDIUM: '中',
                  HIGH: '高',
                  URGENT: '紧急',
                }"
                :key="value"
                :value="value"
                :label="label" /></el-select></el-form-item
          ><!-- 显示Element Plus 的 form-item 组件；label 固定为 进度权重。 --><el-form-item label="进度权重"
            ><!-- 显示Element Plus 的 input-number 组件；与 form.weight 双向同步；min 取自 1；max 取自 2147483647；precision 取自 0。 --><el-input-number
              v-model="form.weight"
              :min="1"
              :max="2147483647"
              :precision="0"
          /></el-form-item>
        </div>
        <!-- 显示Element Plus 的 form-item 组件；label 固定为 预计耗时（分钟，未知留空）。 -->
        <el-form-item label="预计耗时（分钟，未知留空）"
          ><!-- 显示Element Plus 的 input-number 组件；与 form.estimatedMinutes 双向同步；min 取自 1；max 取自 2147483647；precision 取自 0。 --><el-input-number
            v-model="form.estimatedMinutes"
            :min="1"
            :max="2147483647"
            :precision="0" /></el-form-item></template
      ><!-- 显示Element Plus 的 form-item 组件；仅当 kind !== 'goal' 成立时渲染；label 固定为 排序。 --><el-form-item v-if="kind !== 'goal'" label="排序"
        ><!-- 显示Element Plus 的 input-number 组件；与 form.sortOrder 双向同步；min 取自 0；max 取自 2147483647；precision 取自 0。 --><el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="2147483647"
          :precision="0"
      /></el-form-item>
      <!-- 显示一个布局容器；用样式类 dialog-actions 控制外观。 -->
      </el-form>
      <template #footer><div v-if="!loading" class="dialog-actions">
        <el-button :disabled="busy" @click="emit('close')">{{ createdTask ? '关闭' : '取消' }}</el-button>
        <el-button v-if="canGrow(permission(kind, id ? 'update' : 'create'))" :form="formId" native-type="submit" :disabled="busy" :loading="busy" :type="planDate && !id ? 'default' : 'primary'">{{ kind === 'task' && !id ? (createdTask ? '保留在任务池' : '存入任务池') : '保存' }}</el-button>
        <el-button v-if="kind === 'task' && !id && planDate && canGrow('planning:assignment:manage') && canGrow('planning:assignment:read')" type="primary" :loading="busy" :disabled="busy || planDate < today()" @click="save('day')">{{ createdTask ? '重试安排到当天' : '保存并安排到当天' }}</el-button>
      </div></template></el-dialog
  >
</template>
