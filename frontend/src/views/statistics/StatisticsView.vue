<!-- 渲染统计页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import {
  ref,
  reactive,
  onMounted,
  onBeforeUnmount,
  defineAsyncComponent,
} from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  statisticsApi,
  statisticsParams,
  duration,
  rateText,
  kindNames,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { today, states } from "../../api/growth";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../components/personal/PeriodFilters.vue 提供的组件、状态函数或请求方法。 */
import PeriodFilters from "../../components/personal/PeriodFilters.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* 复盘报告Chart（ReportChart）保存调用 defineAsyncComponent 完成define异步Component处理。 */ ReportChart = defineAsyncComponent(
  () => import("../../components/personal/ReportChart.vue"),
);
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* 筛选条件（filters）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  filters = reactive({
    /* 统计周期（period）字段使用固定状态或编码 MONTH（月）。 */
    period: "MONTH",
    /* anchor日期（anchorDate）字段使用调用 today 完成今天处理。 */
    anchorDate: today(),
    /* dates字段使用一个空列表，用来保存后续多条数据。 */
    dates: [],
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* 任务编号（taskId）字段使用固定文字 ''。 */
    taskId: "",
  });
const /* 汇总（summary）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ summary = ref(null),
  /* objects保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  objects = ref([]),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 分组按（groupBy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  groupBy = ref("HABIT"),
  /* 习惯复盘报告（habitReport）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  habitReport = ref(null);
let /* applied保存调用 statisticsParams 完成统计参数集合处理。 */ applied = statisticsParams(filters),
  /* 当前最新请求序号（sequence）保存固定数值 0。 */
  sequence = 0,
  /* 分组请求序号（groupSequence）保存固定数值 0。 */
  groupSequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
const /* 趋势列表（trends）保存调用 usePaged( (params, signal) => statisticsApi.trends 完成趋势列表处理。 */ trends = usePaged(
  (params, signal) => statisticsApi.trends({ ...applied, ...params }, signal),
  { /* immediate字段使用假，表示条件不成立或开关关闭。 */ immediate: false, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 },
);
const /* 分组列表（groups）保存调用 usePaged( async (params, signal) => { const seq = ++groupSequence; const result = await statisticsApi.habits 完成习惯列表处理。 */ groups = usePaged(
  async (params, signal) => {
    const /* seq保存按表达式计算得到的值：++groupSequence。 */ seq = ++groupSequence;
    const /* 结果（result）保存等待 调用 statisticsApi.habits 完成习惯列表处理 完成后取得结果。 */ result = await statisticsApi.habits(
      { ...applied, ...params, /* 分组按（groupBy）字段使用表达式 groupBy.value 的结果。 */ groupBy: groupBy.value },
      signal,
    );
    /* 检查条件 seq === groupSequence && !signal.aborted 是否成立，成立时走这一分支。 */
    if (seq === groupSequence && !signal.aborted) /* 把结果（result）写入 habitReport.value。 */ habitReport.value = result;
    /* 返回表达式 result.groups 的结果，当前函数到此结束。 */
    return result.groups;
  },
  { /* immediate字段使用假，表示条件不成立或开关关闭。 */ immediate: false },
);
/* 搜索（search）：调用 search 完成搜索处理。 */
async function search() {
  let /* 声明 参数集合（params），稍后再填入处理结果。 */ params;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把调用 statisticsParams 完成统计参数集合处理写入 params。 */
    params = statisticsParams(filters);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 error.value。 */
    error.value = e.message;
    /* 返回空内容，当前函数到此结束。 */
    return;
  }
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
  controller = new AbortController();
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 调用 trends.cancel 完成取消处理。 */
  trends.cancel();
  /* 调用 groups.cancel 完成取消处理。 */
  groups.cancel();
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 结果（result）保存等待 调用 Promise.all 完成全部处理 完成后取得结果。 */ result = await Promise.all([
      statisticsApi.summary(params, controller.signal),
      statisticsApi.objects(params, controller.signal),
    ]);
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把参数集合（params）写入 applied。 */
    applied = params;
    /* 把表达式 result[0] 的结果写入 summary.value。 */
    summary.value = result[0];
    /* 把表达式 result[1] 的结果写入 objects.value。 */
    objects.value = result[1];
    /* 把空值，表示当前还没有对应内容写入 habitReport.value。 */
    habitReport.value = null;
    /* 调用 trends.cancel 完成取消处理。 */
    trends.cancel(true);
    /* 调用 groups.cancel 完成取消处理。 */
    groups.cancel(true);
    /* 等待调用 Promise.all 完成全部处理。 */
    await Promise.all([trends.load(true), groups.load(true)]);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* reset：调用 reset 完成reset处理。 */
function reset() {
  /* 调用 Object.assign 完成安排处理。 */
  Object.assign(filters, {
    /* 统计周期（period）字段使用固定状态或编码 MONTH（月）。 */
    period: "MONTH",
    /* anchor日期（anchorDate）字段使用调用 today 完成今天处理。 */
    anchorDate: today(),
    /* dates字段使用一个空列表，用来保存后续多条数据。 */
    dates: [],
    /* 目标编号（goalId）字段使用固定文字 ''。 */
    goalId: "",
    /* 阶段编号（stageId）字段使用固定文字 ''。 */
    stageId: "",
    /* 任务编号（taskId）字段使用固定文字 ''。 */
    taskId: "",
  });
  /* 调用 search 完成搜索处理。 */
  search();
}
/* regroup：调用 regroup 完成regroup处理。 */
function regroup(value) {
  /* 检查条件 busy.value || !summary.value 是否成立，成立时走这一分支。 */
  if (busy.value || !summary.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把值（value）写入 groupBy.value。 */
  groupBy.value = value;
  /* 把空值，表示当前还没有对应内容写入 habitReport.value。 */
  habitReport.value = null;
  /* 调用 groups.cancel 完成取消处理。 */
  groups.cancel(true);
  /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
  groups.load(true);
}
/* 注册组件首次挂载完成后的处理。 */
onMounted(search);
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
      <h2>从真实投入里，看见自己的节奏</h2>
      <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 router.push('/statistics/timeline')。 -->
      <el-button @click="router.push('/statistics/timeline')"
        >查看成长时间线</el-button
      >
    </div>
    <!-- 显示Period筛选条件（PeriodFilters）组件；filters 取自 filters；busy 取自 busy；search 事件触发时执行 search；reset 事件触发时执行 reset。 -->
    <PeriodFilters
      :filters="filters"
      :busy="busy"
      @search="search"
      @reset="reset"
    />
    <!-- 显示一段文字；仅当 summary 成立时渲染；用样式类 hint 控制外观。 -->
    <p v-if="summary" class="hint">
      查询日期：<!-- 把 summary.dateFrom 的当前值转成文字显示。 -->{{ summary.dateFrom }} 至 <!-- 把 summary.dateTo 的当前值转成文字显示。 -->{{ summary.dateTo }} ·
      <!-- 把 summary.timeZone 的当前值转成文字显示。 -->
      {{
        summary.timeZone
      }}。当前周期截至今天，习惯保持账户维度，不跟随目标筛选。
    </p>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 busy && !summary；error 取自 error；retry 事件触发时执行 search。 -->
    <LoadState :loading="busy && !summary" :error="error" @retry="search">
      <!-- 显示一个布局容器；仅当 summary 成立时渲染；用样式类 metrics-grid four 控制外观。 -->
      <div v-if="summary" class="metrics-grid four">
        <!-- 显示一条独立内容；用样式类 card metric inset-metric 控制外观。 -->
        <article class="card metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>区间实际投入</span
          ><!-- 显示重点文字；用样式类 metric-duration 控制外观。 --><strong class="metric-duration"><!-- 把 duration(summary.execution.rangeActualSeconds) 的当前值转成文字显示。 -->{{
            duration(summary.execution.rangeActualSeconds)
          }}</strong>
          <!-- 显示一段文字；用样式类 hint 控制外观。 -->
          <p class="hint">
            累计 <!-- 把 duration(summary.execution.totalActualSeconds) 的当前值转成文字显示。 -->{{ duration(summary.execution.totalActualSeconds) }}
          </p>
        </article>
        <!-- 显示一条独立内容；用样式类 card metric inset-metric 控制外观。 -->
        <article class="card metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>参与执行会话</span
          ><!-- 显示重点文字。 --><strong><!-- 把 summary.execution.rangeSessions 的当前值转成文字显示。 -->{{ summary.execution.rangeSessions }}</strong>
          <!-- 显示一段文字；用样式类 hint 控制外观。 -->
          <p class="hint">
            区间已结束 <!-- 把 summary.execution.rangeFinishedSessions 的当前值转成文字显示。 -->{{ summary.execution.rangeFinishedSessions }}
          </p>
        </article>
        <!-- 显示一条独立内容；用样式类 card metric inset-metric 控制外观。 -->
        <article class="card metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>习惯完成率</span
          ><!-- 显示重点文字。 --><strong><!-- 把 rateText(summary.habits.completionRate) 的当前值转成文字显示。 -->{{ rateText(summary.habits.completionRate) }}</strong>
          <!-- 显示一段文字；用样式类 hint 控制外观。 -->
          <p class="hint">
            <!-- 把 summary.habits.doneCount 的当前值转成文字显示。 -->
            {{ summary.habits.doneCount }} /
            <!-- 把 summary.habits.dueCount 的当前值转成文字显示。 -->
            {{ summary.habits.dueCount }} 次应做
          </p>
        </article>
        <!-- 显示一条独立内容；用样式类 card metric inset-metric 控制外观。 -->
        <article class="card metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>真实任务完成动作</span
          ><!-- 显示重点文字。 --><strong><!-- 把 summary.states.taskCompletionActions 的当前值转成文字显示。 -->{{ summary.states.taskCompletionActions }}</strong>
          <!-- 显示一段文字；用样式类 hint 控制外观。 -->
          <p class="hint">
            去重完成任务 <!-- 把 summary.states.completedDistinctTasks 的当前值转成文字显示。 -->{{ summary.states.completedDistinctTasks }}
          </p>
        </article>
      </div>
    </LoadState>
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>逐日趋势</h2>
      <!-- 显示行内文字区域；用样式类 hint 控制外观。 -->
      <span class="hint">图表与表格均为当前日期页，不将无应做显示为 0%</span>
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 trends.busy && !trends.loaded；error 取自 trends.error；retry 事件触发时执行 trends.load。 -->
    <LoadState
      :loading="trends.busy && !trends.loaded"
      :error="trends.error"
      @retry="trends.load"
    >
      <!-- 显示一个布局容器；仅当 trends.rows.length 成立时渲染；用样式类 report-charts 控制外观。 -->
      <div v-if="trends.rows.length" class="report-charts">
        <!-- 显示一个布局容器；用样式类 chart-panel wide 控制外观。 -->
        <div class="chart-panel wide">
          <!-- 显示三级标题。 -->
          <h3>实际投入</h3>
          <!-- 显示复盘报告Chart（ReportChart）组件；rows 取自 trends.rows；kind 固定为 effort；label 固定为 当前日期页每日实际投入，单位小时。 -->
          <ReportChart
            :rows="trends.rows"
            kind="effort"
            label="当前日期页每日实际投入，单位小时"
          />
        </div>
        <!-- 显示一个布局容器；用样式类 chart-panel 控制外观。 -->
        <div class="chart-panel">
          <!-- 显示三级标题。 -->
          <h3>真实完成动作</h3>
          <!-- 显示复盘报告Chart（ReportChart）组件；rows 取自 trends.rows；kind 固定为 actions；label 固定为 目标阶段任务实际完成动作数量。 -->
          <ReportChart
            :rows="trends.rows"
            kind="actions"
            label="目标阶段任务实际完成动作数量"
          />
        </div>
        <!-- 显示一个布局容器；用样式类 chart-panel 控制外观。 -->
        <div class="chart-panel">
          <!-- 显示三级标题。 -->
          <h3>习惯完成率</h3>
          <!-- 显示复盘报告Chart（ReportChart）组件；rows 取自 trends.rows；kind 固定为 habits；label 固定为 每日习惯完成率，没有应做时留空。 -->
          <ReportChart
            :rows="trends.rows"
            kind="habits"
            label="每日习惯完成率，没有应做时留空"
          />
        </div>
      </div>
      <!-- 显示Element Plus 的 table 组件；data 取自 trends.rows；loading 指令使用 trends.busy；empty-text 固定为 所选周期暂无日期数据。 -->
      <el-table
        :data="trends.rows"
        v-loading="trends.busy"
        empty-text="所选周期暂无日期数据"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '所选周期暂无日期数据'。 --><AppEmptyState
            :description="
              error ? '加载失败，请查看提示后重试。' : '所选周期暂无日期数据'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 businessDate；label 固定为 日期；min-width 固定为 130。 --><el-table-column
          prop="businessDate"
          label="日期"
          min-width="130"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 实际投入；min-width 固定为 120。 --><el-table-column label="实际投入" min-width="120"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 duration(row.actualSeconds) 的当前值转成文字显示。 -->{{
            duration(row.actualSeconds)
          }}</template></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 习惯；min-width 固定为 160。 -->
        <el-table-column label="习惯" min-width="160"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 把 row.habitDone 的当前值转成文字显示。 -->{{ row.habitDone }} / <!-- 把 row.habitDue 的当前值转成文字显示。 -->{{ row.habitDue }} ·
            <!-- 把 rateText(row.habitCompletionRate) 的当前值转成文字显示。 -->
            {{ rateText(row.habitCompletionRate) }}</template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 taskCompletionActions；label 固定为 任务完成动作；min-width 固定为 130。 -->
        <el-table-column
          prop="taskCompletionActions"
          label="任务完成动作"
          min-width="130"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 completedDistinctTasks；label 固定为 去重完成任务；min-width 固定为 130。 --><el-table-column
          prop="completedDistinctTasks"
          label="去重完成任务"
          min-width="130"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 sessions；label 固定为 参与会话；width 固定为 110。 --><el-table-column
          prop="sessions"
          label="参与会话"
          width="110"
        /> </el-table
      ><!-- 显示当前页Pagination（PagePagination）组件；state 取自 trends；change 事件触发时执行 trends.load。 --><PagePagination :state="trends" @change="trends.load" />
    </LoadState>
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>习惯统计</h2>
      <!-- 显示Element Plus 的 radio-group 组件；model-value 取自 groupBy；disabled 取自 busy || !summary；update:model-value 事件触发时执行 regroup。 -->
      <el-radio-group
        :model-value="groupBy"
        :disabled="busy || !summary"
        @update:model-value="regroup"
        ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 HABIT。 --><el-radio-button value="HABIT">按习惯</el-radio-button
        ><!-- 显示Element Plus 的 radio-button 组件；value 固定为 CATEGORY。 --><el-radio-button value="CATEGORY"
          >按历史分类归属</el-radio-button
        ></el-radio-group
      >
    </div>
    <!-- 显示一段文字；用样式类 hint 控制外观。 -->
    <p class="hint">
      总完成数 /
      总应做数，不平均各习惯的百分比；已删除习惯及已停用分类保留历史。
    </p>
    <!-- 显示一段文字；仅当 habitReport 成立时渲染；用样式类 hint 控制外观。 -->
    <p v-if="habitReport" class="hint">
      区间总计 <!-- 把 habitReport.totals.doneCount 的当前值转成文字显示。 -->{{ habitReport.totals.doneCount }} /
      <!-- 把 habitReport.totals.dueCount 的当前值转成文字显示。 -->
      {{ habitReport.totals.dueCount }} ·
      <!-- 把 rateText(habitReport.totals.completionRate) 的当前值转成文字显示。 -->
      {{ rateText(habitReport.totals.completionRate) }}
    </p>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 groups.busy && !groups.loaded；error 取自 groups.error；retry 事件触发时执行 groups.load。 -->
    <LoadState
      :loading="groups.busy && !groups.loaded"
      :error="groups.error"
      @retry="groups.load"
    >
      <!-- 显示Element Plus 的 table 组件；data 取自 groups.rows；loading 指令使用 groups.busy；empty-text 固定为 当前周期没有应做或有效打卡。 -->
      <el-table
        :data="groups.rows"
        v-loading="groups.busy"
        empty-text="当前周期没有应做或有效打卡"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '当前周期没有应做或有效打卡'。 --><AppEmptyState
            :description="
              error
                ? '加载失败，请查看提示后重试。'
                : '当前周期没有应做或有效打卡'
            " /></template
        ><!-- 显示Element Plus 的 table-column 组件；label 固定为 名称；min-width 固定为 200。 --><el-table-column label="名称" min-width="200"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}
            <!-- 显示Element Plus 的 tag 组件；仅当 row.deleted 成立时渲染；类型设为 info；size 固定为 small。 -->
            <el-tag v-if="row.deleted" type="info" size="small"
              >历史保留</el-tag
            ></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；prop 固定为 doneCount；label 固定为 完成；width 固定为 110。 -->
        <el-table-column
          prop="doneCount"
          label="完成"
          width="110"
        /><!-- 显示Element Plus 的 table-column 组件；prop 固定为 dueCount；label 固定为 应做；width 固定为 110。 --><el-table-column
          prop="dueCount"
          label="应做"
          width="110"
        /><!-- 显示Element Plus 的 table-column 组件；label 固定为 完成率；width 固定为 150。 --><el-table-column label="完成率" width="150"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 rateText(row.completionRate) 的当前值转成文字显示。 -->{{
            rateText(row.completionRate)
          }}</template></el-table-column
        > </el-table
      ><!-- 显示当前页Pagination（PagePagination）组件；state 取自 groups；change 事件触发时执行 groups.load。 --><PagePagination :state="groups" @change="groups.load" />
    </LoadState>
  </section>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示二级标题。 -->
    <h2>当前对象概况</h2>
    <!-- 显示一段文字；用样式类 hint 控制外观。 -->
    <p class="hint">仅统计当前有效未归档对象，与上方历史完成动作分开。</p>
    <!-- 显示Element Plus 的 table 组件；data 取自 objects；empty-text 固定为 当前没有有效对象。 -->
    <el-table :data="objects" empty-text="当前没有有效对象"
      ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 --><template #empty
        ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '当前没有有效对象'。 --><AppEmptyState
          :description="
            error ? '加载失败，请查看提示后重试。' : '当前没有有效对象'
          " /></template
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 对象。 --><el-table-column label="对象"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 kindNames[row.kind] 的当前值转成文字显示。 -->{{
          kindNames[row.kind]
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；label 固定为 当前状态。 --><el-table-column label="当前状态"
        ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"><!-- 把 states[row.status] 的当前值转成文字显示。 -->{{
          states[row.status]
        }}</template></el-table-column
      ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 objectCount；label 固定为 数量。 --><el-table-column prop="objectCount" label="数量"
    /></el-table>
  </section>
</template>
