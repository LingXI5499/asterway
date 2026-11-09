<!-- 渲染成长业务证明信息界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, watch, onMounted, onBeforeUnmount } from "vue";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow, validDate } from "../../api/growth";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import { executionApi, duration } from "../../api/personal";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import { execution } from "../../stores/personal";
/* 引入 ../../composables/usePaged 提供的组件、状态函数或请求方法。 */
import { usePaged } from "../../composables/usePaged";
/* 引入 ./StateHistory.vue 提供的组件、状态函数或请求方法。 */
import StateHistory from "./StateHistory.vue";
/* 引入 ./ExecutionDetails.vue 提供的组件、状态函数或请求方法。 */
import ExecutionDetails from "./ExecutionDetails.vue";
/* 引入 ./ExecutionTable.vue 提供的组件、状态函数或请求方法。 */
import ExecutionTable from "./ExecutionTable.vue";
/* 引入 ./PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "./PagePagination.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 种类（kind）字段使用可为空的文字（String）。 */ kind: String, centered: Boolean, /* 编号（id）字段使用表达式 [String, Number] 的结果。 */ id: [String, Number] });
const /* dates保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ dates = ref([]),
  /* appliedDates保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  appliedDates = ref([]),
  /* 汇总（summary）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  summary = ref(null),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref(""),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 历史是否展开（historyOpen）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  historyOpen = ref(false),
  /* 详情是否展开（detailOpen）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  detailOpen = ref(false),
  /* 执行计时编号（executionId）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  executionId = ref("");
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* applied保存一个空的键值对应表。 */
  applied = {},
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
const /* 记录列表（records）保存调用 usePaged( (params, signal) => executionApi.taskRecords 完成任务记录列表处理。 */ records = usePaged(
  (params, signal) =>
    executionApi.taskRecords(props.id, { ...applied, ...params }, signal),
  { /* immediate字段使用假，表示条件不成立或开关关闭。 */ immediate: false },
);
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 检查条件 !props.id 是否成立，成立时走这一分支。 */
  if (!props.id) /* 返回空内容，当前函数到此结束。 */ return;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
  controller = new AbortController();
  /* 调用 records.cancel 完成取消处理。 */
  records.cancel();
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 值列表（values）保存优先使用前面的有效值，没有时采用后面的备用值：dates.value || []。 */ values = dates.value || [];
    /* 检查条件 values.length && (values.length !== 2 || !values.every(validDate) || values[0] > values[1]) 是否成立，成立时走这一分支。 */
    if (
      values.length &&
      (values.length !== 2 || !values.every(validDate) || values[0] > values[1])
    )
      /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
      throw new Error("请选择完整且顺序正确的日期区间");
    const /* 参数集合（params）保存表达式 { dateFrom: values[0], dateTo: values[1] } 的结果。 */ params = { /* 日期来源（dateFrom）字段使用表达式 values[0] 的结果。 */ dateFrom: values[0], /* 日期To（dateTo）字段使用表达式 values[1] 的结果。 */ dateTo: values[1] };
    /* 检查当前用户是否具有需要的权限：canGrow("execution:session:read")，成立时走这一分支。 */
    if (canGrow("execution:session:read")) {
      const /* 结果（result）保存等待 调用 executionApi.scopeSummary 完成数据范围汇总处理 完成后取得结果。 */ result = await executionApi.scopeSummary(
        props.kind,
        props.id,
        params,
        controller.signal,
      );
      /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
      if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
      /* 把结果（result）写入 summary.value。 */
      summary.value = result;
      /* 把表达式 [...values] 的结果写入 appliedDates.value。 */
      appliedDates.value = [...values];
    }
    /* 把参数集合（params）写入 applied。 */
    applied = params;
    /* 检查当前用户是否具有需要的权限：props.kind === "task" && canGrow("execution:session:list")，成立时走这一分支。 */
    if (props.kind === "task" && canGrow("execution:session:list"))
      /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
      await records.load(true);
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 详情（details）：调用 details 完成详情处理。 */
function details(id) {
  /* 把编号（id）写入 executionId.value。 */
  executionId.value = id;
  /* 把真，表示条件成立或开关打开写入 detailOpen.value。 */
  detailOpen.value = true;
}
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => [props.kind, props.id], load, { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true });
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(() => execution.revision, load);
const /* 已修改（changed）保存判断列表或文字是否包含指定内容。 */ changed = (event) => {
  /* 检查条件 ["growth", "goal"].includes(event.detail) 是否成立，成立时走这一分支。 */
  if (["growth", "goal"].includes(event.detail)) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
};
/* 调用 onMounted(() => window.addEventListener 完成加入事件Listener处理。 */
onMounted(() => window.addEventListener("asterway:personal-change", changed));
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 移除 事件Listener（EventListener）。 */
  window.removeEventListener("asterway:personal-change", changed);
});
</script>
<template>
  <!-- 显示一个有独立含义的内容区；用样式类 card 控制外观。 -->
  <section class="card">
    <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
    <div class="section-heading">
      <!-- 显示二级标题。 -->
      <h2>实际投入与状态历史</h2>
      <!-- 显示Element Plus 的 button 组件；仅当 canGrow(kind + ':' + kind + ':read') 成立时渲染；click 事件触发时执行 historyOpen = true。 -->
      <el-button
        v-if="canGrow(kind + ':' + kind + ':read')"
        @click="historyOpen = true"
        >查看状态历史</el-button
      >
    </div>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 canGrow('execution:session:read') 成立时渲染。 -->
    <template v-if="canGrow('execution:session:read')">
      <!-- 显示一个布局容器；用样式类 toolbar 控制外观。 -->
      <div class="toolbar">
        <!-- 显示Element Plus 的 date-picker 组件；与 dates 双向同步；类型设为 daterange；value-format 固定为 YYYY-MM-DD；start-placeholder 固定为 投入起始日期；end-placeholder 固定为 投入结束日期。 -->
        <el-date-picker
          v-model="dates"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="投入起始日期"
          end-placeholder="投入结束日期"
        /><!-- 显示Element Plus 的 button 组件；loading 取自 busy；click 事件触发时执行 load。 --><el-button :loading="busy" @click="load">查询投入</el-button
        ><!-- 显示Element Plus 的 button 组件；click 事件触发时执行 dates = []; load();。 --><el-button
          @click="
            dates = [];
            load();
          "
          >重置为今天</el-button
        >
      </div>
      <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <!-- 显示一个布局容器；仅当 summary 成立时渲染；用样式类 metrics-grid 控制外观。 -->
      <div v-if="summary" class="metrics-grid">
        <!-- 显示一条独立内容；用样式类 metric inset-metric 控制外观。 -->
        <article class="metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>累计实际投入</span
          ><!-- 显示重点文字。 --><strong><!-- 把 duration(summary.totalActualSeconds) 的当前值转成文字显示。 -->{{ duration(summary.totalActualSeconds) }}</strong>
        </article>
        <!-- 显示一条独立内容；用样式类 metric inset-metric 控制外观。 -->
        <article class="metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span
            ><!-- 把 appliedDates.length ? appliedDates.join(" 至 ") : "今日" 的当前值转成文字显示。 -->{{
              appliedDates.length ? appliedDates.join(" 至 ") : "今日"
            }}实际投入</span
          ><!-- 显示重点文字。 --><strong><!-- 把 duration(summary.rangeActualSeconds) 的当前值转成文字显示。 -->{{ duration(summary.rangeActualSeconds) }}</strong>
        </article>
        <!-- 显示一条独立内容；用样式类 metric inset-metric 控制外观。 -->
        <article class="metric inset-metric">
          <!-- 显示行内文字区域。 -->
          <span>区间参与会话</span><!-- 显示重点文字。 --><strong><!-- 把 summary.rangeSessions 的当前值转成文字显示。 -->{{ summary.rangeSessions }}</strong>
        </article>
      </div>
      <!-- 显示一段文字；用样式类 hint 控制外观。 -->
      <p class="hint">
        暂停间隔不计时，逻辑删除任务仍保留真实投入；耗时不等于任务完成状态。
      </p>
    </template>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 kind === 'task' && canGrow('execution:session:list') 成立时渲染。 -->
    <template v-if="kind === 'task' && canGrow('execution:session:list')">
      <!-- 显示三级标题。 -->
      <h3>任务执行记录</h3>
      <!-- 显示Element Plus 的 alert 组件；仅当 records.error 成立时渲染；title 取自 records.error；类型设为 error；closable 取自 false。 -->
      <el-alert
        v-if="records.error"
        :title="records.error"
        type="error"
        :closable="false"
      />
      <!-- 显示执行计时表（ExecutionTable）组件；rows 取自 records.rows；loading 取自 records.busy；启用 controls 属性；detail 事件触发时执行 details；changed 事件触发时执行 load。 -->
      <ExecutionTable
        :rows="records.rows"
        :loading="records.busy"
        controls
        @detail="details"
        @changed="load"
      /><!-- 显示当前页Pagination（PagePagination）组件；state 取自 records；change 事件触发时执行 records.load。 --><PagePagination :state="records" @change="records.load" />
    </template>
  </section>
  <!-- 显示当前状态历史（StateHistory）组件；与 historyOpen 双向同步；kind 取自 kind；id 取自 id。 -->
  <StateHistory :centered="centered" v-model="historyOpen" :kind="kind" :id="id" />
  <!-- 显示执行计时详情（ExecutionDetails）组件；与 detailOpen 双向同步；id 取自 executionId；changed 事件触发时执行 load。 -->
  <ExecutionDetails :centered="centered" v-model="detailOpen" :id="executionId" @changed="load" />
</template>
