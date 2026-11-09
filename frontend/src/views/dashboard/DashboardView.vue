<!-- 渲染Dashboard页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  dashboardApi,
  duration,
  rateText,
  canStartTask,
} from "../../api/personal";
/* 引入 ../../api/habit-actions 提供的组件、状态函数或请求方法。 */
import { checkHabit, cancelHabitToday } from "../../api/habit-actions";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow, states } from "../../api/growth";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import {
  execution,
  startExecution,
  elapsedSeconds,
} from "../../stores/personal";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
/* 引入 ../../components/growth/ProgressValue.vue 提供的组件、状态函数或请求方法。 */
import ProgressValue from "../../components/growth/ProgressValue.vue";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* overview保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  overview = ref(null),
  /* overview错误（overviewError）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  overviewError = ref(""),
  /* overview正在处理（overviewBusy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  overviewBusy = ref(false),
  /* 分组（group）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  group = ref("TODAY"),
  /* 等待处理（pending）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  pending = ref("");
const /* metrics保存随相关状态变化自动重算的值。 */ metrics = computed(() => overview.value?.metrics);
const /* 任务列表（tasks）保存调用 usePaged( (params, signal) => dashboardApi.tasks 完成任务列表处理。 */ tasks = usePaged(
  (params, signal) =>
    dashboardApi.tasks({ ...params, /* section字段使用表达式 group.value 的结果。 */ section: group.value }, signal),
  { /* 每页条数（size）字段使用固定数值 10。 */ size: 10 },
);
const /* 目标列表（goals）保存调用 usePaged((params, signal) => dashboardApi.goals 完成目标列表处理。 */ goals = usePaged((params, signal) => dashboardApi.goals(params, signal), {
  /* 每页条数（size）字段使用固定数值 10。 */
  size: 10,
});
const /* 习惯列表（habits）保存调用 usePaged( (params, signal) => dashboardApi.habits 完成习惯列表处理。 */ habits = usePaged(
  (params, signal) => dashboardApi.habits(params, signal),
  { /* 每页条数（size）字段使用固定数值 10。 */ size: 10 },
);
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0;
/* 加载Overview（loadOverview）：加载 Overview。 */
async function loadOverview() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 overviewBusy.value。 */
  overviewBusy.value = true;
  /* 把固定文字 ''写入 overviewError.value。 */
  overviewError.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 结果（result）保存等待 调用 dashboardApi.overview 完成overview处理 完成后取得结果。 */ result = await dashboardApi.overview();
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把结果（result）写入 overview.value。 */ overview.value = result;
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把表达式 e.message 的结果写入 overviewError.value。 */ overviewError.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 overviewBusy.value。 */ overviewBusy.value = false;
  }
}
/* 刷新（refresh）：调用 refresh 完成刷新处理。 */
async function refresh() {
  /* 等待调用 Promise.allSettled 完成全部Settled处理。 */
  await Promise.allSettled([
    loadOverview(),
    tasks.load(),
    goals.load(),
    habits.load(),
  ]);
}
/* 开始（start）：调用 start 完成开始处理。 */
async function start(row) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 startExecution 完成开始执行计时处理。 */
    await startExecution(row);
    /* 等待加载 Overview。 */
    await loadOverview();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 习惯（habit）：调用 habit 完成习惯处理。 */
async function habit(action, row) {
  /* 检查条件 pending.value 是否成立，成立时走这一分支。 */
  if (pending.value) /* 返回空内容，当前函数到此结束。 */ return;
  /* 把表达式 row.habitId 的结果写入 pending.value。 */
  pending.value = row.habitId;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 检查条件 action === "check" 是否成立，成立时走这一分支。 */
    if (action === "check") /* 等待核对 习惯（Habit）。 */ await checkHabit(row);
    else /* 等待取消 习惯今天（HabitToday）。 */ await cancelHabitToday(row);
    /* 等待调用 Promise.all 完成全部处理。 */
    await Promise.all([habits.load(), loadOverview()]);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 检查条件 e.status === 409 || e.status === 404 是否成立，成立时走这一分支。 */
    if (e.status === 409 || e.status === 404) /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ await habits.load();
  } finally {
    /* 把固定文字 ''写入 pending.value。 */
    pending.value = "";
  }
}
/* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
watch(group, () => tasks.load(true));
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => execution.revision, loadOverview);
const /* 已修改（changed）保存优先使用前面的有效值，没有时采用后面的备用值：(event) => { if (event.detail === "goal" || event.detail === "notification") { loadOverview(); goals.load(); }…。 */ changed = (event) => {
  /* 检查条件 event.detail === "goal" || event.detail === "notification" 是否成立，成立时走这一分支。 */
  if (event.detail === "goal" || event.detail === "notification") {
    /* 加载 Overview。 */
    loadOverview();
    /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    goals.load();
  }
};
/* 调用 onMounted(() => { loadOverview(); window.addEventListener 完成加入事件Listener处理。 */
onMounted(() => {
  /* 加载 Overview。 */
  loadOverview();
  /* 调用 window.addEventListener 完成加入事件Listener处理。 */
  window.addEventListener("asterway:personal-change", changed);
});
/* 移除 事件Listener（EventListener）。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 移除 事件Listener（EventListener）。 */
  window.removeEventListener("asterway:personal-change", changed);
});
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card personal-hero dashboard-welcome 控制外观。 -->
  <section class="card personal-hero dashboard-welcome">
    <!-- 显示一个布局容器。 -->
    <div>
      <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
      <span class="eyebrow"><!-- 把 overview?.today || "TODAY" 的当前值转成文字显示。 -->{{ overview?.today || "TODAY" }} · 今日看板</span>
      <!-- 显示二级标题。 -->
      <h2>今天，先完成一件重要的事</h2>
      <!-- 显示一段文字；用样式类 muted 控制外观。 -->
      <p class="muted">看清安排、开始投入，也给每天的坚持留下一次记录。</p>
    </div>
    <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
    <div class="toolbar">
      <!-- 显示Element Plus 的 button 组件；loading 取自 overviewBusy；click 事件触发时执行 refresh。 -->
      <el-button :loading="overviewBusy" @click="refresh">刷新今日</el-button
      ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('notification:message:list') 成立时渲染；click 事件触发时执行 router.push('/notifications')。 --><el-button
        v-if="canGrow('notification:message:list')"
        @click="router.push('/notifications')"
        >消息中心</el-button
      >
    </div>
  </section>
  <!-- 显示一个有独立含义的内容区；仅当 execution.current 成立时渲染；用样式类 card dashboard-focus 控制外观。 -->
  <section v-if="execution.current" class="card dashboard-focus">
    <!-- 显示一个布局容器；用样式类 dashboard-focus-copy 控制外观。 -->
    <div class="dashboard-focus-copy">
      <!-- 显示一个布局容器；用样式类 dashboard-focus-icon 控制外观。 -->
      <div class="dashboard-focus-icon">
        <!-- 显示应用Icon（AppIcon）组件；name 固定为 execution；size 取自 24。 -->
        <AppIcon name="execution" :size="24" />
      </div>
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
        <span class="eyebrow">当前专注</span>
        <!-- 显示二级标题。 -->
        <h2><!-- 把 execution.current.taskName 的当前值转成文字显示。 -->{{ execution.current.taskName }}</h2>
        <!-- 显示行内文字区域；用样式类 mini-clock 控制外观。 -->
        <span class="mini-clock"><!-- 把 duration(elapsedSeconds()) 的当前值转成文字显示。 -->{{ duration(elapsedSeconds()) }}</span>
      </div>
    </div>
    <!-- 显示Element Plus 的 button 组件；类型设为 primary；click 事件触发时执行 router.push('/execution')。 -->
    <el-button type="primary" @click="router.push('/execution')"
      >进入执行工作台<!-- 显示应用Icon（AppIcon）组件；name 固定为 arrow；size 取自 16。 --><AppIcon name="arrow" :size="16"
    /></el-button>
  </section>
  <!-- 显示一个有独立含义的内容区；前面条件均不成立时显示这个备用分支；用样式类 card dashboard-focus 控制外观。 -->
  <section v-else class="card dashboard-focus">
    <!-- 显示一个布局容器；用样式类 dashboard-focus-copy 控制外观。 -->
    <div class="dashboard-focus-copy">
      <!-- 显示一个布局容器；用样式类 dashboard-focus-icon 控制外观。 -->
      <div class="dashboard-focus-icon">
        <!-- 显示应用Icon（AppIcon）组件；name 固定为 execution；size 取自 24。 -->
        <AppIcon name="execution" :size="24" />
      </div>
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示二级标题。 -->
        <h2>留一段时间，专注眼前的事</h2>
        <!-- 显示一段文字；用样式类 muted 控制外观。 -->
        <p class="muted">从已安排的任务开始，系统会记录你的真实投入。</p>
      </div>
    </div>
    <!-- 显示Element Plus 的 button 组件；仅当 canGrow('execution:session:list') 成立时渲染；类型设为 primary；click 事件触发时执行 router.push('/execution')。 -->
    <el-button
      v-if="canGrow('execution:session:list')"
      type="primary"
      @click="router.push('/execution')"
      ><!-- 显示应用Icon（AppIcon）组件；name 固定为 plus；size 取自 16。 --><AppIcon name="plus" :size="16" /><!-- 显示行内文字区域。 --><span>选择任务开始</span></el-button
    >
  </section>
  <!-- 显示加载当前状态（LoadState）组件；loading 取自 overviewBusy && !overview；error 取自 overviewError；retry 事件触发时执行 loadOverview。 -->
  <LoadState
    :loading="overviewBusy && !overview"
    :error="overviewError"
    @retry="loadOverview"
  >
    <!-- 显示一个布局容器；仅当 metrics 成立时渲染；用样式类 metrics-grid four 控制外观。 -->
    <div v-if="metrics" class="metrics-grid four">
      <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
      <article class="card metric">
        <!-- 显示行内文字区域；用样式类 metric-icon 控制外观。 -->
        <span class="metric-icon">今日计划完成<!-- 显示应用Icon（AppIcon）组件；name 固定为 task。 --><AppIcon name="task" /></span
        ><!-- 显示重点文字；仅当 metrics.todayTasks 成立时渲染。 --><strong v-if="metrics.todayTasks"
          ><!-- 把 metrics.todayCurrentCompletedTasks 的当前值转成文字显示。 -->{{ metrics.todayCurrentCompletedTasks
          }}<!-- 显示辅助说明文字。 --><small> / <!-- 把 metrics.todayTasks 的当前值转成文字显示。 -->{{ metrics.todayTasks }}</small></strong
        >
        <!-- 显示重点文字；前面条件均不成立时显示这个备用分支；用样式类 metric-duration 控制外观。 -->
        <strong v-else class="metric-duration">暂无安排</strong>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">今日安排任务的当前状态</p>
      </article>
      <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
      <article class="card metric">
        <!-- 显示行内文字区域；用样式类 metric-icon 控制外观。 -->
        <span class="metric-icon">今日实际投入<!-- 显示应用Icon（AppIcon）组件；name 固定为 clock。 --><AppIcon name="clock" /></span
        ><!-- 显示重点文字；用样式类 metric-duration 控制外观。 --><strong class="metric-duration"><!-- 把 duration(metrics.actualSeconds) 的当前值转成文字显示。 -->{{
          duration(metrics.actualSeconds)
        }}</strong>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">已排除暂停间隔</p>
      </article>
      <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
      <article class="card metric">
        <!-- 显示行内文字区域；用样式类 metric-icon 控制外观。 -->
        <span class="metric-icon">今日习惯完成<!-- 显示应用Icon（AppIcon）组件；name 固定为 habit。 --><AppIcon name="habit" /></span
        ><!-- 显示重点文字；仅当 metrics.habitDue 成立时渲染。 --><strong v-if="metrics.habitDue"
          ><!-- 把 metrics.habitDone 的当前值转成文字显示。 -->{{ metrics.habitDone
          }}<!-- 显示辅助说明文字。 --><small> / <!-- 把 metrics.habitDue 的当前值转成文字显示。 -->{{ metrics.habitDue }}</small></strong
        >
        <!-- 显示重点文字；前面条件均不成立时显示这个备用分支；用样式类 metric-duration 控制外观。 -->
        <strong v-else class="metric-duration">无应做</strong>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">
          <!-- 把 metrics.habitDue ? rateText(metrics.habitCompletionRate) : "按今日规则与有效打卡统计" 的当前值转成文字显示。 -->
          {{
            metrics.habitDue
              ? rateText(metrics.habitCompletionRate)
              : "按今日规则与有效打卡统计"
          }}
        </p>
      </article>
      <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
      <article class="card metric">
        <!-- 显示行内文字区域；用样式类 metric-icon 控制外观。 -->
        <span class="metric-icon">本周尚未定日<!-- 显示应用Icon（AppIcon）组件；name 固定为 week。 --><AppIcon name="week" /></span
        ><!-- 显示重点文字。 --><strong><!-- 把 metrics.weekUndatedTasks 的当前值转成文字显示。 -->{{ metrics.weekUndatedTasks }}</strong>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">不计入今日计划任务数</p>
      </article>
    </div>
  </LoadState>
  <!-- 显示一个布局容器；用样式类 dashboard-grid 控制外观。 -->
  <div class="dashboard-grid">
    <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
    <section class="card">
      <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
      <div class="section-heading">
        <!-- 显示二级标题。 -->
        <h2>任务安排</h2>
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('planning:weekly:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 router.push('/planning/weekly-plans')。 -->
        <el-button
          v-if="canGrow('planning:weekly:read')"
          link
          type="primary"
          @click="router.push('/planning/weekly-plans')"
          >查看周计划</el-button
        >
      </div>
      <!-- 显示Element Plus 的 tabs 组件；与 group 双向同步。 -->
      <el-tabs v-model="group"
        ><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 今日任务；name 固定为 TODAY。 --><el-tab-pane label="今日任务" name="TODAY" /><!-- 显示Element Plus 的 tab-pane 组件；label 固定为 本周未定日；name 固定为 WEEK_UNDATED。 --><el-tab-pane
          label="本周未定日"
          name="WEEK_UNDATED"
      /></el-tabs>
      <!-- 显示加载当前状态（LoadState）组件；loading 取自 tasks.busy && !tasks.loaded；error 取自 tasks.error；retry 事件触发时执行 tasks.load。 -->
      <LoadState
        :loading="tasks.busy && !tasks.loaded"
        :error="tasks.error"
        @retry="tasks.load"
      >
        <!-- 显示Element Plus 的 table 组件；data 取自 tasks.rows；loading 指令使用 tasks.busy；empty-text 固定为 这个分组暂无安排。 -->
        <el-table
          :data="tasks.rows"
          v-loading="tasks.busy"
          empty-text="这个分组暂无安排"
        >
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
          <template #empty
            ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 '这个分组暂无安排'。 --><AppEmptyState :description="'这个分组暂无安排'" /></template
          ><!-- 显示Element Plus 的 table-column 组件；label 固定为 任务；min-width 固定为 160。 --><el-table-column label="任务" min-width="160"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:read') 成立时渲染；类型设为 primary；启用 link 属性；click 事件触发时执行 router.push('/growth/tasks/' + row.taskId)。 --><el-button
                v-if="canGrow('task:task:read')"
                type="primary"
                link
                @click="router.push('/growth/tasks/' + row.taskId)"
                ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
              ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</span></template
            ></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 100。 -->
          <el-table-column label="状态" width="100"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template
          ></el-table-column>
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 操作；width 固定为 100。 -->
          <el-table-column label="操作" width="100"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
              ><!-- 显示Element Plus 的 button 组件；仅当 canStartTask() 成立时渲染；类型设为 primary；启用 link 属性；disabled 取自 row.status === 'COMPLETED' || !!execution.current || !!execution.busy；click 事件触发时执行 start(row)。 --><el-button
                v-if="canStartTask()"
                type="primary"
                link
                :disabled="
                  row.status === 'COMPLETED' ||
                  !!execution.current ||
                  !!execution.busy
                "
                @click="start(row)"
                >开始执行</el-button
              ></template
            ></el-table-column
          > </el-table
        ><!-- 显示当前页Pagination（PagePagination）组件；state 取自 tasks；change 事件触发时执行 tasks.load。 --><PagePagination :state="tasks" @change="tasks.load" />
      </LoadState>
    </section>
    <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
    <section class="card">
      <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
      <div class="section-heading">
        <!-- 显示二级标题。 -->
        <h2>今日习惯</h2>
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('habit:habit:list') 成立时渲染；类型设为 primary；启用 link 属性；click 事件触发时执行 router.push('/habits')。 -->
        <el-button
          v-if="canGrow('habit:habit:list')"
          type="primary"
          link
          @click="router.push('/habits')"
          >管理习惯</el-button
        >
      </div>
      <!-- 显示加载当前状态（LoadState）组件；loading 取自 habits.busy && !habits.loaded；error 取自 habits.error；retry 事件触发时执行 habits.load。 -->
      <LoadState
        :loading="habits.busy && !habits.loaded"
        :error="habits.error"
        @retry="habits.load"
      >
        <!-- 显示一个布局容器；仅当 habits.rows.length 成立时渲染；用样式类 habit-check-list 控制外观。 -->
        <div v-if="habits.rows.length" class="habit-check-list">
          <!-- 显示一条独立内容；按 row in habits.rows 为每条数据重复生成内容；key 取自 row.habitId；用样式类 habit-check-row 控制外观。 -->
          <article
            v-for="row in habits.rows"
            :key="row.habitId"
            class="habit-check-row"
          >
            <!-- 显示一个布局容器。 -->
            <div>
              <!-- 显示重点文字。 -->
              <strong><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</strong>
              <!-- 显示一段文字；用样式类 hint 控制外观。 -->
              <p class="hint">
                <!-- 把 row.expectedTime || "时间自定" 的当前值转成文字显示。 -->
                {{ row.expectedTime || "时间自定" }} ·
                <!-- 把 row.doneCount ? "今日已完成" : "今日应做" 的当前值转成文字显示。 -->
                {{ row.doneCount ? "今日已完成" : "今日应做" }}
              </p>
            </div>
            <!-- 显示Element Plus 的 button 组件；仅当 !row.doneCount && canGrow('habit:checkin:create') && canGrow('habit:habit:read') 成立时渲染；类型设为 primary；启用 plain 属性；disabled 取自 !!pending || !row.canCheckin；click 事件触发时执行 habit('check', row)。 -->
            <el-button
              v-if="
                !row.doneCount &&
                canGrow('habit:checkin:create') &&
                canGrow('habit:habit:read')
              "
              type="primary"
              plain
              :disabled="!!pending || !row.canCheckin"
              @click="habit('check', row)"
              >打卡</el-button
            >
            <!-- 显示Element Plus 的 button 组件；前一条件不成立且 row.doneCount && canGrow('habit:checkin:cancel') && canGrow('habit:checkin:read') 成立时渲染；类型设为 success；启用 plain 属性；disabled 取自 !!pending || !row.canCancel；click 事件触发时执行 habit('cancel', row)。 -->
            <el-button
              v-else-if="
                row.doneCount &&
                canGrow('habit:checkin:cancel') &&
                canGrow('habit:checkin:read')
              "
              type="success"
              plain
              :disabled="!!pending || !row.canCancel"
              @click="habit('cancel', row)"
              >已完成 · 撤销</el-button
            >
            <!-- 显示Element Plus 的 tag 组件；前面条件均不成立时显示这个备用分支；type 取自 row.doneCount ? 'success' : 'info'。 -->
            <el-tag v-else :type="row.doneCount ? 'success' : 'info'"><!-- 把 row.doneCount ? "已完成" : "未完成" 的当前值转成文字显示。 -->{{
              row.doneCount ? "已完成" : "未完成"
            }}</el-tag>
          </article>
        </div>
        <!-- 显示应用空状态当前状态（AppEmptyState）组件；前面条件均不成立时显示这个备用分支；description 固定为 今天暂无应做习惯。 -->
        <AppEmptyState v-else description="今天暂无应做习惯" />
        <!-- 显示当前页Pagination（PagePagination）组件；state 取自 habits；change 事件触发时执行 habits.load。 -->
        <PagePagination :state="habits" @change="habits.load" />
      </LoadState>
    </section>
  </div>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>当前目标</h2>
      <!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:list') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 router.push('/growth/goals')。 -->
      <el-button
        v-if="canGrow('goal:goal:list')"
        link
        type="primary"
        @click="router.push('/growth/goals')"
        >目标管理</el-button
      >
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 goals.busy && !goals.loaded；error 取自 goals.error；retry 事件触发时执行 goals.load。 -->
    <LoadState
      :loading="goals.busy && !goals.loaded"
      :error="goals.error"
      @retry="goals.load"
    >
      <!-- 显示Element Plus 的 table 组件；data 取自 goals.rows；loading 指令使用 goals.busy；empty-text 固定为 还没有目标，可以从一个小目标开始。 -->
      <el-table
        :data="goals.rows"
        v-loading="goals.busy"
        empty-text="还没有目标，可以从一个小目标开始"
      >
        <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
        <template #empty
          ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 '还没有目标，可以从一个小目标开始'。 --><AppEmptyState
            :description="'还没有目标，可以从一个小目标开始'" /></template
        ><!-- 显示Element Plus 的 table-column 组件；label 固定为 目标；min-width 固定为 200。 --><el-table-column label="目标" min-width="200"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示Element Plus 的 button 组件；仅当 canGrow('goal:goal:read') 成立时渲染；启用 link 属性；类型设为 primary；click 事件触发时执行 router.push('/growth/goals/' + row.id)。 --><el-button
              v-if="canGrow('goal:goal:read')"
              link
              type="primary"
              @click="router.push('/growth/goals/' + row.id)"
              ><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</el-button
            ><!-- 显示行内文字区域；前面条件均不成立时显示这个备用分支。 --><span v-else><!-- 把 row.name 的当前值转成文字显示。 -->{{ row.name }}</span></template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 当前进度；min-width 固定为 200。 -->
        <el-table-column label="当前进度" min-width="200"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示进度值（ProgressValue）组件；value 取自 row.currentProgress。 --><ProgressValue :value="row.currentProgress" /></template
        ></el-table-column>
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 有效任务；min-width 固定为 120。 -->
        <el-table-column label="有效任务" min-width="120"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 把 row.completedTasks 的当前值转成文字显示。 -->{{ row.completedTasks }} / <!-- 把 row.taskCount 的当前值转成文字显示。 -->{{ row.taskCount }}</template
          ></el-table-column
        >
        <!-- 显示Element Plus 的 table-column 组件；label 固定为 状态；width 固定为 110。 -->
        <el-table-column label="状态" width="110"
          ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row }。 --><template #default="{ row }"
            ><!-- 显示应用状态标签（AppStatusTag）组件；value 取自 row.status。 --><AppStatusTag :value="row.status" /></template
        ></el-table-column> </el-table
      ><!-- 显示当前页Pagination（PagePagination）组件；state 取自 goals；change 事件触发时执行 goals.load。 --><PagePagination :state="goals" @change="goals.load" />
    </LoadState>
  </section>
</template>
