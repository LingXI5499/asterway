<!-- 渲染成长业务Selector界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 目标与阶段级联选择，远程搜索避免一次装载所有个人数据。
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, getResource } from "../../api/growth";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 目标编号（goalId）字段使用表达式 [String, Number] 的结果。 */
  goalId: [String, Number],
  /* 阶段编号（stageId）字段使用表达式 [String, Number] 的结果。 */
  stageId: [String, Number],
  /* 目标仅（goalOnly）字段使用可为空的真假值（Boolean）。 */
  goalOnly: Boolean,
  /* 已禁用（disabled）字段使用可为空的真假值（Boolean）。 */
  disabled: Boolean,
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:goalId", "update:stageId"]);
const /* 目标列表（goals）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ goals = ref([]),
  /* 阶段列表（stages）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  stages = ref([]),
  /* 正在加载（loading）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  loading = ref(false);
let /* 目标请求序号（goalSequence）保存固定数值 0。 */ goalSequence = 0,
  /* 阶段请求序号（stageSequence）保存固定数值 0。 */
  stageSequence = 0;
/* 加载目标列表（loadGoals）：加载 目标列表（Goals）。 */
async function loadGoals(keyword = "") {
  const /* seq保存按表达式计算得到的值：++goalSequence。 */ seq = ++goalSequence;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ data = await query("/goals", {
      /* timing字段使用固定状态或编码 ALL（全部）。 */
      timing: "ALL",
      /* 已归档（archived）字段使用假，表示条件不成立或开关关闭。 */
      archived: false,
      /* 每页条数（size）字段使用固定数值 100。 */
      size: 100,
      /* 直接使用已有的keyword作为同名字段。 */
      keyword,
    });
    /* 检查条件 seq !== goalSequence 是否成立，成立时走这一分支。 */
    if (seq !== goalSequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把表达式 data.items 的结果写入 goals.value。 */
    goals.value = data.items;
    /* 检查条件 props.goalId && !goals.value.some((r) => r.id === String(props.goalId)) 是否成立，成立时走这一分支。 */
    if (
      props.goalId &&
      !goals.value.some((r) => r.id === String(props.goalId))
    ) {
      /* 调用 goals.value.unshift 完成unshift处理。 */
      goals.value.unshift(await getResource("goal", props.goalId));
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 加载阶段列表（loadStages）：加载 阶段列表（Stages）。 */
async function loadStages(keyword = "") {
  const /* seq保存按表达式计算得到的值：++stageSequence。 */ seq = ++stageSequence;
  /* 检查条件 !props.goalId 是否成立，成立时走这一分支。 */
  if (!props.goalId) {
    /* 把一个空列表，用来保存后续多条数据写入 stages.value。 */
    stages.value = [];
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  /* 把真，表示条件成立或开关打开写入 loading.value。 */
  loading.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ data = await query(`/goals/${props.goalId}/stages`, {
      /* 每页条数（size）字段使用固定数值 100。 */
      size: 100,
      /* 直接使用已有的keyword作为同名字段。 */
      keyword,
    });
    /* 检查条件 seq !== stageSequence 是否成立，成立时走这一分支。 */
    if (seq !== stageSequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把表达式 data.items 的结果写入 stages.value。 */
    stages.value = data.items;
    /* 检查条件 props.stageId && !stages.value.some((r) => r.id === String(props.stageId)) 是否成立，成立时走这一分支。 */
    if (
      props.stageId &&
      !stages.value.some((r) => r.id === String(props.stageId))
    )
      /* 调用 stages.value.unshift 完成unshift处理。 */
      stages.value.unshift(await getResource("stage", props.stageId));
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  } finally {
    /* 检查条件 seq === stageSequence 是否成立，成立时走这一分支。 */
    if (seq === stageSequence) /* 把假，表示条件不成立或开关关闭写入 loading.value。 */ loading.value = false;
  }
}
/* 选择目标（selectGoal）：调用 selectGoal 完成选择目标处理。 */
function selectGoal(id) {
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("update:goalId", id || "");
  /* 向父组件发送事件，通知数据或界面状态发生变化。 */
  emit("update:stageId", "");
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => props.goalId,
  () => loadStages(),
);
/* 注册组件首次挂载完成后的处理。 */
onMounted(() => {
  /* 加载 目标列表（Goals）。 */
  loadGoals();
  /* 加载 阶段列表（Stages）。 */
  loadStages();
});
</script>
<template>
  <!-- 显示一个布局容器；用样式类 growth-selectors 控制外观。 -->
  <div class="growth-selectors">
    <!-- 显示Element Plus 的 select 组件；model-value 取自 goalId；update:model-value 事件触发时执行 selectGoal；disabled 取自 disabled；未输入时提示 选择目标；启用 filterable 属性；启用 remote 属性；启用 clearable 属性；remote-method 取自 loadGoals。 -->
    <el-select
      :model-value="goalId"
      @update:model-value="selectGoal"
      :disabled="disabled"
      placeholder="选择目标"
      filterable
      remote
      clearable
      :remote-method="loadGoals"
      ><!-- 显示Element Plus 的 option 组件；按 g in goals 为每条数据重复生成内容；key 取自 g.id；value 取自 g.id；label 取自 g.name。 --><el-option
        v-for="g in goals"
        :key="g.id"
        :value="g.id"
        :label="g.name" /></el-select
    ><!-- 显示Element Plus 的 select 组件；仅当 !goalOnly 成立时渲染；model-value 取自 stageId；update:model-value 事件触发时执行 emit('update:stageId', $event || '')；disabled 取自 disabled || !goalId；未输入时提示 选择阶段；启用 filterable 属性；启用 remote 属性；启用 clearable 属性；remote-method 取自 loadStages；loading 取自 loading。 --><el-select
      v-if="!goalOnly"
      :model-value="stageId"
      @update:model-value="emit('update:stageId', $event || '')"
      :disabled="disabled || !goalId"
      placeholder="选择阶段"
      filterable
      remote
      clearable
      :remote-method="loadStages"
      :loading="loading"
      ><!-- 显示Element Plus 的 option 组件；按 s in stages 为每条数据重复生成内容；key 取自 s.id；value 取自 s.id；label 取自 s.name。 --><el-option v-for="s in stages" :key="s.id" :value="s.id" :label="s.name"
    /></el-select>
  </div>
</template>
