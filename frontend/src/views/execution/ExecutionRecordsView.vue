<!-- 渲染执行计时记录列表页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, watch, onMounted, onBeforeUnmount } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRoute, useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { executionApi, executionStates, duration } from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow, validDate } from "../../api/growth";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 引入 ../../components/personal/ScopeFilters.vue 提供的组件、状态函数或请求方法。 */
import ScopeFilters from "../../components/personal/ScopeFilters.vue";
/* 引入 ../../components/personal/ExecutionTable.vue 提供的组件、状态函数或请求方法。 */
import ExecutionTable from "../../components/personal/ExecutionTable.vue";
/* 引入 ../../components/personal/ExecutionDetails.vue 提供的组件、状态函数或请求方法。 */
import ExecutionDetails from "../../components/personal/ExecutionDetails.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* route保存取得当前页面路径和查询参数。 */ route = useRoute(),
  /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */
  router = useRouter();
const /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ filters = reactive({
  /* 目标编号（goalId）字段使用固定文字 ''。 */
  goalId: "",
  /* 阶段编号（stageId）字段使用固定文字 ''。 */
  stageId: "",
  /* 任务编号（taskId）字段使用优先使用前面的有效值，没有时采用后面的备用值：route.query.taskId || ""。 */
  taskId: route.query.taskId || "",
  /* 状态（status）字段使用固定文字 ''。 */
  status: "",
  /* dates字段使用一个空列表，用来保存后续多条数据。 */
  dates: [],
});
const /* 汇总（summary）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ summary = ref(null),
  /* 汇总Dates（summaryDates）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  summaryDates = ref([]),
  /* show保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  show = ref(false),
  /* 详情编号（detailId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detailId = ref("");
let /* applied保存表达式 { taskId: filters.taskId } 的结果。 */ applied = { /* 任务编号（taskId）字段使用表达式 filters.taskId 的结果。 */ taskId: filters.taskId },
  /* 当前最新请求序号（sequence）保存固定数值 0。 */
  sequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
const /* 列表（list）保存调用 usePaged( (params, signal) => executionApi.list 完成列表处理。 */ list = usePaged(
  (params, signal) => executionApi.list({ ...applied, ...params }, signal),
  { /* immediate字段使用假，表示条件不成立或开关关闭。 */ immediate: false },
);
/* 详情（details）：调用 details 完成详情处理。 */
function details(id) {
  /* 把编号（id）写入 detailId.value。 */
  detailId.value = id;
  /* 把真，表示条件成立或开关打开写入 show.value。 */
  show.value = true;
}
/* 搜索（search）：调用 search 完成搜索处理。 */
async function search(reset = false) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* dates保存优先使用前面的有效值，没有时采用后面的备用值：filters.dates || []。 */ dates = filters.dates || [];
    /* 检查条件 dates.length && (dates.length !== 2 || !dates.every(validDate) || dates[0] > dates[1]) 是否成立，成立时走这一分支。 */
    if (
      dates.length &&
      (dates.length !== 2 || !dates.every(validDate) || dates[0] > dates[1])
    )
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择完整、顺序正确的日期区间");
    /* 把表达式 { goalId: filters.goalId, stageId: filters.stageId, taskId: filters.taskId, status: filters.status, dateFrom: dates[0], … 的结果写入 applied。 */
    applied = {
      /* 目标编号（goalId）字段使用表达式 filters.goalId 的结果。 */
      goalId: filters.goalId,
      /* 阶段编号（stageId）字段使用表达式 filters.stageId 的结果。 */
      stageId: filters.stageId,
      /* 任务编号（taskId）字段使用表达式 filters.taskId 的结果。 */
      taskId: filters.taskId,
      /* 状态（status）字段使用表达式 filters.status 的结果。 */
      status: filters.status,
      /* 日期来源（dateFrom）字段使用表达式 dates[0] 的结果。 */
      dateFrom: dates[0],
      /* 日期To（dateTo）字段使用表达式 dates[1] 的结果。 */
      dateTo: dates[1],
    };
    const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
    /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
    controller?.abort();
    /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
    controller = new AbortController();
    /* 把空值，表示当前还没有对应内容写入 summary.value。 */
    summary.value = null;
    const /* 结果（[, result]）保存等待 根据前面的条件，从两个候选结果中选择一个：Promise.all([ list.load(reset), canGrow("execution:session:read") ? executionApi.summary( { dateFrom… 完成后取得结果。 */ [, result] = await Promise.all([
      list.load(reset),
      canGrow("execution:session:read")
        ? executionApi.summary(
            { /* 日期来源（dateFrom）字段使用表达式 dates[0] 的结果。 */ dateFrom: dates[0], /* 日期To（dateTo）字段使用表达式 dates[1] 的结果。 */ dateTo: dates[1] },
            controller.signal,
          )
        : Promise.resolve(null),
    ]);
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) {
      /* 把结果（result）写入 summary.value。 */
      summary.value = result;
      /* 把表达式 [...dates] 的结果写入 summaryDates.value。 */
      summaryDates.value = [...dates];
    }
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 e.name !== "AbortError" && e.code !== "SESSION_CHANGED" 是否成立，成立时走这一分支。 */
    if (e.name !== "AbortError" && e.code !== "SESSION_CHANGED") /* 调用 report 完成复盘报告处理。 */ report(e);
  }
}
/* reset：调用 reset 完成reset处理。 */
function reset() {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(filters, {
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* 任务编号（taskId）字段使用固定文字 ''。 */
    taskId: "",
    /* 状态（status）字段使用固定文字 ''。 */
    status: "",
    /* dates字段使用一个空列表，用来保存后续多条数据。 */
    dates: [],
  });
  /* 调用 search 完成搜索处理。 */
  search(true);
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => route.query.taskId,
  (value) => {
    /* 把优先使用前面的有效值，没有时采用后面的备用值：value || ""写入 filters.taskId。 */
    filters.taskId = value || "";
    /* 调用 search 完成搜索处理。 */
    search(true);
  },
);
/* 注册组件首次挂载完成后的处理。 */
onMounted(() => search());
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
});
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>每一段真实投入，都有记录</h2>
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 router.push('/execution')。 -->
      <el-button @click="router.push('/execution')">返回执行工作台</el-button>
    </div>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示数据范围筛选条件（ScopeFilters）组件；filters 取自 filters。 -->
      <ScopeFilters :filters="filters" /><!-- 显示Element Plus 的 select 组件；与 filters.status 双向同步；启用 clearable 属性；未输入时提示 全部会话状态。 --><el-select
        v-model="filters.status"
        clearable
        placeholder="全部会话状态"
        ><!-- 显示Element Plus 的 option 组件；按 (name, code) in executionStates 为每条数据重复生成内容；key 取自 code；value 取自 code；label 取自 name。 --><el-option
          v-for="(name, code) in executionStates"
          :key="code"
          :value="code"
          :label="name"
      /></el-select>
      <!-- 显示Element Plus 的 date-picker 组件；与 filters.dates 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 开始日期；end-placeholder 固定为 结束日期。 -->
      <el-date-picker
        v-model="filters.dates"
        type="daterange"
        value-format="YYYY-MM-DD"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
      />
      <!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 search(true)。 -->
      <el-button type="primary" @click="search(true)">查询</el-button
      ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 reset。 --><el-button @click="reset">重置</el-button>
    </div>
    <!-- 显示一段文字；仅当 summary 成立时渲染；用样式类 hint 控制外观。 -->
    <p v-if="summary" class="hint">
      账户<!-- 把 summaryDates.length ? summaryDates.join(" 至 ") : "今日" 的当前值转成文字显示。 -->{{ summaryDates.length ? summaryDates.join(" 至 ") : "今日" }}实际投入
      <!-- 把 duration(summary.rangeActualSeconds) 的当前值转成文字显示。 -->
      {{ duration(summary.rangeActualSeconds) }}；累计
      <!-- 把 duration(summary.totalActualSeconds) 的当前值转成文字显示。 -->
      {{
        duration(summary.totalActualSeconds)
      }}。上方耗时为账户口径，下表按所选来源筛选。
    </p>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 list.busy && !list.loaded；error 取自 list.error；retry 事件触发时执行 list.load。 -->
    <LoadState
      :loading="list.busy && !list.loaded"
      :error="list.error"
      @retry="list.load"
    >
      <!-- 显示执行计时表（ExecutionTable）组件；rows 取自 list.rows；loading 取自 list.busy；detail 事件触发时执行 details。 -->
      <ExecutionTable
        :rows="list.rows"
        :loading="list.busy"
        @detail="details"
      />
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 list.load。 -->
      <PagePagination :state="list" @change="list.load" />
    </LoadState>
  </section>
  <!-- 显示执行计时详情（ExecutionDetails）组件；与 show 双向同步；id 取自 detailId；changed 事件触发时执行 list.load。 -->
  <ExecutionDetails v-model="show" :id="detailId" @changed="list.load" />
</template>
