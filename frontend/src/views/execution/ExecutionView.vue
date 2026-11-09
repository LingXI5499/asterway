<!-- 渲染执行计时页面界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, watch, onMounted } from "vue";
/* 引入 vue-router 提供的组件、状态函数或请求方法。 */
import { useRouter } from "vue-router";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  executionApi,
  duration,
  instantText,
  canStartTask,
} from "../../api/personal";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import {
  execution,
  refreshExecution,
  elapsedSeconds,
  startExecution,
  controlExecution,
} from "../../stores/personal";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 引入 ../../components/personal/ExecutionTaskPicker.vue 提供的组件、状态函数或请求方法。 */
import ExecutionTaskPicker from "../../components/personal/ExecutionTaskPicker.vue";
/* 引入 ../../components/personal/ExecutionTable.vue 提供的组件、状态函数或请求方法。 */
import ExecutionTable from "../../components/personal/ExecutionTable.vue";
/* 引入 ../../components/personal/ExecutionDetails.vue 提供的组件、状态函数或请求方法。 */
import ExecutionDetails from "../../components/personal/ExecutionDetails.vue";
/* 引入 ../../components/personal/PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "../../components/personal/PagePagination.vue";
/* 引入 ../../components/personal/LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "../../components/personal/LoadState.vue";
const /* HTTP 路由登记器（router）保存取得用于切换页面的路由器。 */ router = useRouter(),
  /* 已选（selected）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  selected = ref(null),
  /* picker保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  picker = ref(null),
  /* 汇总（summary）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  summary = ref(null),
  /* 汇总错误（summaryError）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  summaryError = ref(""),
  /* show保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  show = ref(false),
  /* 详情编号（detailId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detailId = ref("");
const /* 已暂停（paused）保存调用 usePaged((params, signal) => executionApi.list 完成列表处理。 */ paused = usePaged((params, signal) =>
  executionApi.list({ ...params, /* 状态（status）字段使用固定状态或编码 PAUSED（已暂停）。 */ status: "PAUSED" }, signal),
);
/* 加载汇总（loadSummary）：加载 汇总（Summary）。 */
async function loadSummary() {
  /* 检查当前用户是否具有需要的权限：!canGrow("execution:session:read")，成立时走这一分支。 */
  if (!canGrow("execution:session:read")) /* 返回空内容，当前函数到此结束。 */ return;
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 把等待 调用 executionApi.summary 完成汇总处理 完成后取得结果写入 summary.value。 */
    summary.value = await executionApi.summary();
    /* 把固定文字 ''写入 summaryError.value。 */
    summaryError.value = "";
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 把表达式 e.message 的结果写入 summaryError.value。 */
    summaryError.value = e.message;
  }
}
/* 刷新（refresh）：调用 refresh 完成刷新处理。 */
async function refresh() {
  /* 等待调用 Promise.allSettled 完成全部Settled处理。 */
  await Promise.allSettled([
    refreshExecution(),
    paused.load(),
    loadSummary(),
    picker.value?.reload(),
  ]);
}
/* 开始（start）：调用 start 完成开始处理。 */
async function start() {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 startExecution 完成开始执行计时处理。 */
    await startExecution(selected.value);
    /* 把空值，表示当前还没有对应内容写入 selected.value。 */
    selected.value = null;
    /* 调用 picker.value?.reload 完成reload处理。 */
    picker.value?.reload();
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* control：调用 control 完成control处理。 */
async function control(action) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 controlExecution 完成control执行计时处理。 */
    await controlExecution(action, execution.current);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
  }
}
/* 详情（details）：调用 details 完成详情处理。 */
function details(id) {
  /* 把编号（id）写入 detailId.value。 */
  detailId.value = id;
  /* 把真，表示条件成立或开关打开写入 show.value。 */
  show.value = true;
}
/* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
watch(
  () => execution.revision,
  () => {
    /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    paused.load();
    /* 加载 汇总（Summary）。 */
    loadSummary();
  },
);
/* 注册组件首次挂载完成后的处理。 */
onMounted(loadSummary);
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card focus-card 控制外观。 -->
  <section class="card focus-card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示行内文字区域；用样式类 eyebrow 控制外观。 -->
        <span class="eyebrow">EXECUTION</span>
        <!-- 显示二级标题。 -->
        <h2>一次，只投入当前这件事</h2>
      </div>
      <!-- 显示Element Plus 的 button 组件；loading 取自 !!execution.busy；click 事件触发时执行 refresh。 -->
      <el-button :loading="!!execution.busy" @click="refresh"
        >刷新执行状态</el-button
      >
    </div>
    <!-- 显示Element Plus 的 alert 组件；仅当 execution.error || summaryError 成立时渲染；title 取自 execution.error || summaryError；类型设为 warning；closable 取自 false。 -->
    <el-alert
      v-if="execution.error || summaryError"
      :title="execution.error || summaryError"
      type="warning"
      :closable="false"
    />
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 execution.current 成立时渲染。 -->
    <template v-if="execution.current">
      <!-- 显示一个布局容器；用样式类 focus-title 控制外观。 -->
      <div class="focus-title">
        <!-- 显示应用状态标签（AppStatusTag）组件；value 固定为 RUNNING。 -->
        <AppStatusTag value="RUNNING" /><!-- 显示重点文字。 --><strong><!-- 把 execution.current.taskName 的当前值转成文字显示。 -->{{
          execution.current.taskName
        }}</strong>
      </div>
      <!-- 显示一个布局容器；用样式类 focus-clock 控制外观；role 固定为 timer；aria-label 提供无障碍说明 本次实际投入时长；aria-live 提供无障碍说明 off。 -->
      <div
        class="focus-clock"
        role="timer"
        aria-label="本次实际投入时长"
        aria-live="off"
      >
        <!-- 把 duration(elapsedSeconds()) 的当前值转成文字显示。 -->
        {{ duration(elapsedSeconds()) }}
      </div>
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">
        本次实际投入 · 执行边界
        <!-- 把 instantText(execution.current.eligibleUntil) 的当前值转成文字显示。 -->
        {{ instantText(execution.current.eligibleUntil) }}
      </p>
      <!-- 显示一段文字；仅当 !execution.current.canHeartbeat 成立时渲染；用样式类 hint 控制外观。 -->
      <p v-if="!execution.current.canHeartbeat" class="hint">
        由其他登录端控制。本端可以暂停和结束，接管需先暂停，再从暂停列表继续。
      </p>
      <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
      <div class="toolbar">
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('execution:session:pause') 成立时渲染；类型设为 primary；disabled 取自 !!execution.busy；click 事件触发时执行 control('pause')。 -->
        <el-button
          v-if="canGrow('execution:session:pause')"
          type="primary"
          :disabled="!!execution.busy"
          @click="control('pause')"
          >暂停执行</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('execution:session:finish') 成立时渲染；类型设为 danger；启用 plain 属性；disabled 取自 !!execution.busy；click 事件触发时执行 control('finish')。 -->
        <el-button
          v-if="canGrow('execution:session:finish')"
          type="danger"
          plain
          :disabled="!!execution.busy"
          @click="control('finish')"
          >结束执行</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('execution:session:read') 成立时渲染；click 事件触发时执行 details(execution.current.id)。 -->
        <el-button
          v-if="canGrow('execution:session:read')"
          @click="details(execution.current.id)"
          >会话时间线</el-button
        >
        <!-- 显示Element Plus 的 button 组件；仅当 canGrow('task:task:read') 成立时渲染；启用 text 属性；click 事件触发时执行 router.push('/growth/tasks/' + execution.current.taskId)。 -->
        <el-button
          v-if="canGrow('task:task:read')"
          text
          @click="router.push('/growth/tasks/' + execution.current.taskId)"
          >任务详情</el-button
        >
      </div>
    </template>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；前面条件均不成立时显示这个备用分支。 -->
    <template v-else>
      <!-- 显示一个布局容器；用样式类 focus-clock idle 控制外观。 -->
      <div class="focus-clock idle">00:00:00</div>
      <!-- 显示一段文字；用样式类 muted 控制外观。 -->
      <p class="muted">当前没有运行中的执行，选择已安排任务后开始。</p>
      <!-- 显示一个布局容器；用样式类 start-selection 控制外观。 -->
      <div class="start-selection">
        <!-- 显示行内文字区域；用样式类 selected-task 控制外观。 -->
        <span class="selected-task"
          ><!-- 显示应用Icon（AppIcon）组件；name 取自 selected ? 'task' : 'inbox'。 --><AppIcon :name="selected ? 'task' : 'inbox'" /><!-- 把 selected?.name || "请在下方选择一个已安排任务" 的当前值转成文字显示。 -->{{
            selected?.name || "请在下方选择一个已安排任务"
          }}</span
        ><!-- 显示Element Plus 的 button 组件；仅当 canStartTask() 成立时渲染；类型设为 primary；disabled 取自 !selected || !!execution.busy；loading 取自 execution.busy === 'start'；click 事件触发时执行 start。 --><el-button
          v-if="canStartTask()"
          type="primary"
          :disabled="!selected || !!execution.busy"
          :loading="execution.busy === 'start'"
          @click="start"
          >开始执行</el-button
        >
      </div>
    </template>
    <!-- 显示Element Plus 的 button 组件；用样式类 focus-record-link 控制外观；启用 link 属性；类型设为 primary；click 事件触发时执行 router.push('/execution/records')。 -->
    <el-button
      class="focus-record-link"
      link
      type="primary"
      @click="router.push('/execution/records')"
      >查看全部执行记录</el-button
    >
  </section>
  <!-- 显示一个布局容器；仅当 summary 成立时渲染；用样式类 metrics-grid 控制外观。 -->
  <div v-if="summary" class="metrics-grid">
    <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
    <article class="card metric">
      <!-- 显示行内文字区域。 -->
      <span>今日实际投入</span
      ><!-- 显示重点文字。 --><strong><!-- 把 duration(summary.rangeActualSeconds) 的当前值转成文字显示。 -->{{ duration(summary.rangeActualSeconds) }}</strong>
    </article>
    <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
    <article class="card metric">
      <!-- 显示行内文字区域。 -->
      <span>累计实际投入</span
      ><!-- 显示重点文字。 --><strong><!-- 把 duration(summary.totalActualSeconds) 的当前值转成文字显示。 -->{{ duration(summary.totalActualSeconds) }}</strong>
    </article>
    <!-- 显示一条独立内容；用样式类 card metric 控制外观。 -->
    <article class="card metric">
      <!-- 显示行内文字区域。 -->
      <span>暂停会话</span><!-- 显示重点文字。 --><strong><!-- 把 summary.pausedSessions 的当前值转成文字显示。 -->{{ summary.pausedSessions }}</strong>
    </article>
  </div>
  <!-- 显示执行计时任务Picker（ExecutionTaskPicker）组件；仅当 canStartTask() 成立时渲染；把组件或元素引用保存为 picker，供脚本读取；selected 事件触发时执行 selected = $event。 -->
  <ExecutionTaskPicker
    v-if="canStartTask()"
    ref="picker"
    @selected="selected = $event"
  />
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示一个布局容器。 -->
      <div>
        <!-- 显示二级标题。 -->
        <h2>暂停中的会话</h2>
        <!-- 显示一段文字；用样式类 hint 控制外观。 -->
        <p class="hint">暂停期间不计时。继续时重新检查任务日期与计划安排。</p>
      </div>
    </div>
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 paused.busy && !paused.loaded；error 取自 paused.error；retry 事件触发时执行 paused.load。 -->
    <LoadState
      :loading="paused.busy && !paused.loaded"
      :error="paused.error"
      @retry="paused.load"
    >
      <!-- 显示执行计时表（ExecutionTable）组件；rows 取自 paused.rows；loading 取自 paused.busy；启用 controls 属性；detail 事件触发时执行 details；changed 事件触发时执行 paused.load。 -->
      <ExecutionTable
        :rows="paused.rows"
        :loading="paused.busy"
        controls
        @detail="details"
        @changed="paused.load"
      />
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 paused；change 事件触发时执行 paused.load。 -->
      <PagePagination :state="paused" @change="paused.load" />
    </LoadState>
  </section>
  <!-- 显示执行计时详情（ExecutionDetails）组件；与 show 双向同步；id 取自 detailId；changed 事件触发时执行 refresh。 -->
  <ExecutionDetails v-model="show" :id="detailId" @changed="refresh" />
</template>
