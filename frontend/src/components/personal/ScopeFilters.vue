<!-- 渲染数据范围筛选条件界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, canGrow } from "../../api/growth";
/* 引入 ./PagedSelect.vue 提供的组件、状态函数或请求方法。 */
import PagedSelect from "./PagedSelect.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 筛选条件（filters）字段使用表达式 { type: Object, required: true } 的结果。 */
  filters: { /* 类型（type）字段使用对象（Object）。 */ type: Object, /* 必填（required）字段使用真，表示条件成立或开关打开。 */ required: true },
  /* include任务（includeTask）字段使用表达式 { type: Boolean, default: true } 的结果。 */
  includeTask: { /* 类型（type）字段使用可为空的真假值（Boolean）。 */ type: Boolean, /* 默认值（default）字段使用真，表示条件成立或开关打开。 */ default: true },
});
const /* 已归档（archived）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ archived = ref(false);
const /* 目标列表（goals）保存按表达式计算得到的值：(params, signal) => query( "/goals", { ...params, timing: "ALL", archived: archived.value }, { signal }, )。 */ goals = (params, signal) =>
  query(
    "/goals",
    { ...params, /* timing字段使用固定状态或编码 ALL（全部）。 */ timing: "ALL", /* 已归档（archived）字段使用表达式 archived.value 的结果。 */ archived: archived.value },
    { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal },
  );
const /* 阶段列表（stages）保存按表达式计算得到的值：(params, signal) => query("/goals/" + props.filters.goalId + "/stages", params, { signal })。 */ stages = (params, signal) =>
  query("/goals/" + props.filters.goalId + "/stages", params, { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal });
const /* 任务列表（tasks）保存按表达式计算得到的值：(params, signal) => query( "/tasks", { ...params, goalId: props.filters.goalId, stageId: props.filters.stageId }, { sign…。 */ tasks = (params, signal) =>
  query(
    "/tasks",
    { ...params, /* 目标编号（goalId）字段使用表达式 props.filters.goalId 的结果。 */ goalId: props.filters.goalId, /* 阶段编号（stageId）字段使用表达式 props.filters.stageId 的结果。 */ stageId: props.filters.stageId },
    { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal },
  );
/* 目标（goal）：调用 goal 完成目标处理。 */
function goal(value) {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(props.filters, { /* 目标编号（goalId）字段使用值（value）。 */ goalId: value, /* 阶段编号（stageId）字段使用固定文字 ''。 */ stageId: "", /* 任务编号（taskId）字段使用固定文字 ''。 */ taskId: "" });
}
/* 阶段（stage）：调用 stage 完成阶段处理。 */
function stage(value) {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(props.filters, { /* 阶段编号（stageId）字段使用值（value）。 */ stageId: value, /* 任务编号（taskId）字段使用固定文字 ''。 */ taskId: "" });
}
</script>
<template>
  <!-- 显示一个布局容器；用样式类 scope-filters 控制外观。 -->
  <div class="scope-filters">
    <!-- 显示Paged选择（PagedSelect）组件；model-value 取自 filters.goalId；update:model-value 事件触发时执行 goal；loader 取自 goals；resource-key 取自 String(archived)；未输入时提示 全部目标；disabled 取自 !canGrow('goal:goal:list')。 -->
    <PagedSelect
      :model-value="filters.goalId"
      @update:model-value="goal"
      :loader="goals"
      :resource-key="String(archived)"
      placeholder="全部目标"
      :disabled="!canGrow('goal:goal:list')"
    />
    <!-- 显示Paged选择（PagedSelect）组件；model-value 取自 filters.stageId；update:model-value 事件触发时执行 stage；loader 取自 stages；resource-key 取自 filters.goalId；未输入时提示 全部阶段；disabled 取自 !filters.goalId || !canGrow('stage:stage:list')。 -->
    <PagedSelect
      :model-value="filters.stageId"
      @update:model-value="stage"
      :loader="stages"
      :resource-key="filters.goalId"
      placeholder="全部阶段"
      :disabled="!filters.goalId || !canGrow('stage:stage:list')"
    />
    <!-- 显示Paged选择（PagedSelect）组件；仅当 includeTask 成立时渲染；与 filters.taskId 双向同步；loader 取自 tasks；resource-key 取自 String(filters.goalId) + ':' + filters.stageId；未输入时提示 全部任务；disabled 取自 !canGrow('task:task:list')。 -->
    <PagedSelect
      v-if="includeTask"
      v-model="filters.taskId"
      :loader="tasks"
      :resource-key="String(filters.goalId) + ':' + filters.stageId"
      placeholder="全部任务"
      :disabled="!canGrow('task:task:list')"
    />
    <!-- 显示Element Plus 的 checkbox 组件；仅当 canGrow('goal:goal:list') 成立时渲染；与 archived 双向同步；change 事件触发时执行 goal('')。 -->
    <el-checkbox
      v-if="canGrow('goal:goal:list')"
      v-model="archived"
      @change="goal('')"
      >查看归档目标选项</el-checkbox
    >
  </div>
</template>
