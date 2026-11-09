<!-- 渲染等待处理页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, onMounted } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { query, canGrow } from "../../api/growth";
/* 引入 ../../components/growth/GrowthSelector.vue 提供的组件、状态函数或请求方法。 */
import GrowthSelector from "../../components/growth/GrowthSelector.vue";
/* 引入 ../../components/growth/GrowthActions.vue 提供的组件、状态函数或请求方法。 */
import GrowthActions from "../../components/growth/GrowthActions.vue";
/* 引入 ../../components/growth/GrowthTaskTable.vue 提供的组件、状态函数或请求方法。 */
import GrowthTaskTable from "../../components/growth/GrowthTaskTable.vue";
const /* 筛选（filter）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filter = ref({ /* 目标编号（goalId）字段使用固定文字 ''。 */ goalId: "", /* 阶段编号（stageId）字段使用固定文字 ''。 */ stageId: "", /* keyword字段使用固定文字 ''。 */ keyword: "", /* 状态（status）字段使用固定文字 ''。 */ status: "" }),
  /* 区间（range）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  range = ref([]),
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
    const /* 数据（data）保存沿 await query("/planning/pending-tasks", { ...filter.value, dateFrom: range.value?.[0], dateTo: range.value?.[1], pag… 读取数据；中间对象为空时返回空值。 */ data = await query("/planning/pending-tasks", {
      ...filter.value,
      /* 日期来源（dateFrom）字段使用沿 range.value?.[0] 读取数据；中间对象为空时返回空值。 */
      dateFrom: range.value?.[0],
      /* 日期To（dateTo）字段使用沿 range.value?.[1] 读取数据；中间对象为空时返回空值。 */
      dateTo: range.value?.[1],
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
  /* 把表达式 { goalId: "", stageId: "", keyword: "", status: "" } 的结果写入 filter.value。 */
  filter.value = { /* 目标编号（goalId）字段使用固定文字 ''。 */ goalId: "", /* 阶段编号（stageId）字段使用固定文字 ''。 */ stageId: "", /* keyword字段使用固定文字 ''。 */ keyword: "", /* 状态（status）字段使用固定文字 ''。 */ status: "" };
  /* 把一个空列表，用来保存后续多条数据写入 range.value。 */
  range.value = [];
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
    <!-- 显示二级标题。 -->
    <h2>由你决定下一步</h2>
    <!-- 显示一段文字；用样式类 muted 控制外观。 -->
    <p class="muted">
      这里汇总已过计划日期或所在周已结束的未完成任务。系统不会自动延期或复制任务。
    </p>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示成长业务Selector（GrowthSelector）组件；与 filter.goalId 双向同步（goal-id）；与 filter.stageId 双向同步（stage-id）。 -->
      <GrowthSelector
        v-model:goal-id="filter.goalId"
        v-model:stage-id="filter.stageId"
      /><!-- 显示Element Plus 的 input 组件；与 filter.keyword 双向同步；未输入时提示 任务名称；启用 clearable 属性；keyup 事件触发时执行 page = 1; load();。 --><el-input
        v-model="filter.keyword"
        placeholder="任务名称"
        clearable
        @keyup.enter="
          page = 1;
          load();
        "
      /><!-- 显示Element Plus 的 select 组件；与 filter.status 双向同步；启用 clearable 属性；未输入时提示 全部未完成状态。 --><el-select
        v-model="filter.status"
        clearable
        placeholder="全部未完成状态"
        ><!-- 显示Element Plus 的 option 组件；label 固定为 未开始；value 固定为 NOT_STARTED。 --><el-option label="未开始" value="NOT_STARTED" /><!-- 显示Element Plus 的 option 组件；label 固定为 进行中；value 固定为 IN_PROGRESS。 --><el-option
          label="进行中"
          value="IN_PROGRESS" /><!-- 显示Element Plus 的 option 组件；label 固定为 暂停；value 固定为 PAUSED。 --><el-option
          label="暂停"
          value="PAUSED" /></el-select
      ><!-- 显示Element Plus 的 date-picker 组件；与 range 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 原安排开始日期；end-placeholder 固定为 原安排结束日期。 --><el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="原安排开始日期"
        end-placeholder="原安排结束日期"
      /><!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 page = 1; load();。 --><el-button
        type="primary"
        @click="
          page = 1;
          load();
        "
        >查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 reset。 --><el-button @click="reset">重置</el-button>
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
