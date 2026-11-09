<!-- 渲染任务列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import {
  query,
  states,
  priorities,
  positions,
  canGrow,
  getResource,
} from "../../api/growth";
/* 引入 ../../components/growth/GrowthSelector.vue 提供的组件、状态函数或请求方法。 */
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/growth/GrowthTaskTable.vue 提供的组件、状态函数或请求方法。 */
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = ref({
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* keyword字段使用固定文字 ''。 */
    keyword: "",
    /* 状态（status）字段使用固定文字 ''。 */
    status: "",
    /* priority字段使用固定文字 ''。 */
    priority: "",
    /* 任务安排状态（assignmentStatus）字段使用固定文字 ''。 */
    assignmentStatus: "",
  }),
  /* 记录列表（rows）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  rows = ref([]),
  /* 当前页（page）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  page = ref(1),
  /* 每页条数（size）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  size = ref(20),
  /* 总数（total）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  total = ref(0),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* actions保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  actions = ref(null);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 query 完成查询条件处理 完成后取得结果。 */ data = await query("/tasks", {
      ...filters.value,
      /* 当前页（page）字段使用表达式 page.value 的结果。 */
      page: page.value,
      /* 每页条数（size）字段使用表达式 size.value 的结果。 */
      size: size.value,
    });
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把表达式 data.items 的结果写入 rows.value。 */
    rows.value = data.items;
    /* 把表达式 data.total 的结果写入 total.value。 */
    total.value = data.total;
    /* 把固定文字 ''写入 error.value。 */
    error.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* reset：调用 reset 完成reset处理。 */
function reset() {
  /* 把表达式 { goalId: "", stageId: "", keyword: "", status: "", priority: "", assignmentStatus: "", } 的结果写入 filters.value。 */
  filters.value = {
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* keyword字段使用固定文字 ''。 */
    keyword: "",
    /* 状态（status）字段使用固定文字 ''。 */
    status: "",
    /* priority字段使用固定文字 ''。 */
    priority: "",
    /* 任务安排状态（assignmentStatus）字段使用固定文字 ''。 */
    assignmentStatus: "",
  };
  /* 把固定数值 1写入 page.value。 */
  page.value = 1;
  /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  load();
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(load);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示二级标题。 -->
        <h2>任务中心</h2>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">任务保留原有目标与阶段，通过安排进入周计划和日计划。</p>
      </div>
      <!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:create') 成立时渲染；类型设为 primary；click 事件触发时执行 actions.edit('task', null, filters.stageId || null)。 -->
      <el-button
        v-if="canGrow('task:task:create')"
        type="primary"
        @click="actions.edit('task', null, filters.stageId || null)"
        >新增任务</el-button
      >
    </div>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示成长业务Selector（GrowthSelector）组件；与 filters.goalId 双向同步（goal-id）；与 filters.stageId 双向同步（stage-id）。 -->
      <GrowthSelector
        v-model:goal-id="filters.goalId"
        v-model:stage-id="filters.stageId"
      /><!-- 显示Element Plus 的 input 组件；与 filters.keyword 双向同步；启用 clearable 属性；未输入时提示 任务名称；keyup 事件触发时执行 page = 1; load();。 --><el-input
        v-model="filters.keyword"
        clearable
        placeholder="任务名称"
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 select 组件；与 filters.status 双向同步；启用 clearable 属性；未输入时提示 全部状态。 --><el-select v-model="filters.status" clearable placeholder="全部状态"
        ><!-- 显示Element Plus 的 option 组件；按 (label, code) in states 为每条数据重复生成内容；key 取自 code；label 取自 label；value 取自 code。 --><el-option
          v-for="(label, code) in states"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><!-- 显示Element Plus 的 select 组件；与 filters.priority 双向同步；启用 clearable 属性；未输入时提示 全部优先级。 --><el-select v-model="filters.priority" clearable placeholder="全部优先级"
        ><!-- 显示Element Plus 的 option 组件；按 (label, code) in priorities 为每条数据重复生成内容；key 取自 code；label 取自 label；value 取自 code。 --><el-option
          v-for="(label, code) in priorities"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><!-- 显示Element Plus 的 select 组件；与 filters.assignmentStatus 双向同步；启用 clearable 属性；未输入时提示 全部安排位置。 --><el-select
        v-model="filters.assignmentStatus"
        clearable
        placeholder="全部安排位置"
        ><!-- 显示Element Plus 的 option 组件；按 (label, code) in positions 为每条数据重复生成内容；key 取自 code；label 取自 label；value 取自 code。 --><el-option
          v-for="(label, code) in positions"
          :key="code"
          :label="label"
          :value="code" /></el-select
      ><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 reset。 --><el-button @click="reset">重置</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:sort') 成立时渲染；disabled 取自 !filters.stageId；click 事件触发时执行 actions.sort('task', filters.stageId)。 --><el-button
        v-if="canGrow('task:task:sort')"
        :disabled="!filters.stageId"
        @click="actions.sort('task', filters.stageId)"
        >阶段内排序</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert
      v-if="error"
      :title="error"
      type="error"
      :closable="false"
    /><!-- 显示成长业务任务表（GrowthTaskTable）组件；rows 取自 rows；loading 取自 busy；detail 事件触发时执行 (r) => actions.details('task', r)；edit 事件触发时执行 (r) => actions.edit('task', r)；status 事件触发时执行 (r) => actions.status('task', r)；remove 事件触发时执行 (r) => actions.remove('task', r)；assign 事件触发时执行 (r) => actions.assign(r)；unassign 事件触发时执行 (r) => actions.unassign(r)；history 事件触发时执行 (r) => actions.history(r)。 --><GrowthTaskTable
      :rows="rows"
      :loading="busy"
      @detail="(r) => actions.details('task', r)"
      @edit="(r) => actions.edit('task', r)"
      @status="(r) => actions.status('task', r)"
      @remove="(r) => actions.remove('task', r)"
      @assign="(r) => actions.assign(r)"
      @unassign="(r) => actions.unassign(r)"
      @history="(r) => actions.history(r)"
    /><!-- 显示Element Plus 的 pagination 组件；用样式类 pagination 控制外观；与 page 双向同步（current-page）；与 size 双向同步（page-size）；total 取自 total；page-sizes 取自 [10, 20, 50, 100]；layout 固定为 total,sizes,prev,pager,next；change 事件触发时执行 load。 --><el-pagination
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total,sizes,prev,pager,next"
      @change="load"
    />
  </section>
  <!-- 显示成长业务Actions（GrowthActions）组件；把组件或元素引用保存为 actions，供脚本读取；changed 事件触发时执行 load。 -->
  <GrowthActions ref="actions" @changed="load" />
</template>
