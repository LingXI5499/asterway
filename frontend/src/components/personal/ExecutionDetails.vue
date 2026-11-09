<!-- 渲染执行计时详情界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 在任务详情中使用居中弹窗，其他入口沿用原来的侧边展示。
import { ElDialog, ElDrawer } from "element-plus";
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, computed, watch, onBeforeUnmount } from "vue";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  executionApi,
  duration,
  instantText,
  executionStates,
  reasons,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { canGrow } from "../../api/growth";
/* 引入 ../../stores/personal 提供的组件、状态函数或请求方法。 */
import {
  execution,
  controlExecution,
  elapsedSeconds,
} from "../../stores/personal";
/* 引入 ../../api/ui 提供的组件、状态函数或请求方法。 */
import { report } from "../../api/ui";
/* 引入 ./PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "./PagePagination.vue";
/* 引入 ./LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "./LoadState.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */ modelValue: Boolean, centered: Boolean, /* 编号（id）字段使用表达式 [String, Number] 的结果。 */ id: [String, Number] });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue", "changed"]);
const /* 当前记录（row）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ row = ref(null),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
const /* 计时段列表（segments）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ segments = ref({ /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */ rows: [], /* 总数（total）字段使用固定数值 0。 */ total: 0, /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 });
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
const /* 是否显示（visible）保存随相关状态变化自动重算的值。 */ visible = computed({
  /* 读取（get）字段使用表达式 () => props.modelValue 的结果。 */
  get: () => props.modelValue,
  /* 设置（set）字段使用表达式 (v) => emit("update:modelValue", v) 的结果。 */
  set: (v) => emit("update:modelValue", v),
});
/* 加载（load）：读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
async function load() {
  /* 检查条件 !props.modelValue || !props.id 是否成立，成立时走这一分支。 */
  if (!props.modelValue || !props.id) /* 返回空内容，当前函数到此结束。 */ return;
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
  /* 把新建 Abort请求入口（AbortController） 实例写入 controller。 */
  controller = new AbortController();
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 executionApi.detail 完成详情处理 完成后取得结果。 */ data = await executionApi.detail(props.id, controller.signal);
    /* 检查条件 seq !== sequence 是否成立，成立时走这一分支。 */
    if (seq !== sequence) /* 返回空内容，当前函数到此结束。 */ return;
    /* 把调用 { ...data.execution, _receivedAt: performance.now 完成当前时间处理写入 row.value。 */
    row.value = { ...data.execution, /* receivedAt（_receivedAt）字段使用调用 performance.now 完成当前时间处理。 */ _receivedAt: performance.now() };
    /* 调用 Object.assign 完成安排处理。 */
    Object.assign(segments.value, {
      /* 记录列表（rows）字段使用表达式 data.segments.items 的结果。 */
      rows: data.segments.items,
      /* 总数（total）字段使用表达式 data.segments.total 的结果。 */
      total: data.segments.total,
      /* 当前页（page）字段使用固定数值 1。 */
      page: 1,
      /* 每页条数（size）字段使用固定数值 20。 */
      size: 20,
    });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 加载计时段列表（loadSegments）：加载 计时段列表（Segments）。 */
async function loadSegments() {
  const /* seq保存按表达式计算得到的值：++sequence。 */ seq = ++sequence;
  /* 把真，表示条件成立或开关打开写入 busy.value。 */
  busy.value = true;
  /* 把固定文字 ''写入 error.value。 */
  error.value = "";
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    const /* 数据（data）保存等待 调用 executionApi.segments 完成计时段列表处理 完成后取得结果。 */ data = await executionApi.segments(props.id, {
      /* 当前页（page）字段使用表达式 segments.value.page 的结果。 */
      page: segments.value.page,
      /* 每页条数（size）字段使用表达式 segments.value.size 的结果。 */
      size: segments.value.size,
    });
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence)
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(segments.value, { /* 记录列表（rows）字段使用表达式 data.items 的结果。 */ rows: data.items, /* 总数（total）字段使用表达式 data.total 的结果。 */ total: data.total });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* control：调用 control 完成control处理。 */
async function control(action) {
  /* 执行可能失败的异步或业务处理，失败进入 catch，收尾放在 finally。 */
  try {
    /* 等待调用 controlExecution 完成control执行计时处理。 */
    await controlExecution(action, row.value);
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
    /* 向父组件发送事件，通知数据或界面状态发生变化。 */
    emit("changed");
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 调用 report 完成复盘报告处理。 */
    report(e);
    /* 等待读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */
    await load();
  }
}
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
watch(
  () => [props.modelValue, props.id],
  () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 把空值，表示当前还没有对应内容写入 row.value。 */
    row.value = null;
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
    else /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort();
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
);
// 全局暂停、结束或超时后同步详情，普通心跳不会重置时间线分页。
/* 监听指定状态，在其变化时同步加载数据或清理旧状态。 */
watch(
  () => execution.revision,
  () => {
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
  },
);
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
onBeforeUnmount(() => {
  /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
  sequence++;
  /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
  controller?.abort();
});
</script>
<template>
  <!-- 显示Element Plus 的 drawer 组件；与 visible 双向同步；title 固定为 执行详情与时间线；size 固定为 min(880px, 100vw)；启用 destroy-on-close 属性。 -->
  <component :is="centered ? ElDialog : ElDrawer"
    v-model="visible"
    title="执行详情与时间线"
    v-bind="centered ? { width: 'min(880px,94vw)', alignCenter: true, appendToBody: true, class: 'plan-dialog' } : { size: 'min(880px, 100vw)' }"
    destroy-on-close
  >
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 busy && !row；error 取自 error；retry 事件触发时执行 load。 -->
    <LoadState :loading="busy && !row" :error="error" @retry="load">
      <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 row 成立时渲染。 -->
      <template v-if="row">
        <!-- 显示一个布局容器；用样式类 section-heading 控制外观。 -->
        <div class="section-heading">
          <!-- 显示二级标题。 -->
          <h2><!-- 把 row.taskName 的当前值转成文字显示。 -->{{ row.taskName }}</h2>
          <!-- 显示Element Plus 的 tag 组件；type 取自 row.status === 'RUNNING' ? 'success' : row.status === 'PAUSED' ? 'warning' : 'info'。 -->
          <el-tag
            :type="
              row.status === 'RUNNING'
                ? 'success'
                : row.status === 'PAUSED'
                  ? 'warning'
                  : 'info'
            "
            ><!-- 把 executionStates[row.status] 的当前值转成文字显示。 -->{{ executionStates[row.status] }}</el-tag
          >
        </div>
        <!-- 显示一个布局容器；用样式类 duration-block 控制外观。 -->
        <div class="duration-block">
          <!-- 显示重点文字。 -->
          <strong><!-- 把 duration(elapsedSeconds(row)) 的当前值转成文字显示。 -->{{ duration(elapsedSeconds(row)) }}</strong
          ><!-- 显示行内文字区域。 --><span>本会话实际投入，暂停时间不计入</span>
        </div>
        <!-- 显示Element Plus 的 descriptions 组件；column 取自 1；启用 border 属性。 -->
        <el-descriptions :column="1" border>
          <!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 首次开始。 -->
          <el-descriptions-item label="首次开始"><!-- 把 instantText(row.startedAt) 的当前值转成文字显示。 -->{{
            instantText(row.startedAt)
          }}</el-descriptions-item>
          <!-- 显示Element Plus 的 descriptions-item 组件；label 固定为 最终结束。 -->
          <el-descriptions-item label="最终结束"><!-- 把 instantText(row.finishedAt) 的当前值转成文字显示。 -->{{
            instantText(row.finishedAt)
          }}</el-descriptions-item>
          <!-- 显示Element Plus 的 descriptions-item 组件；仅当 row.finishReason 成立时渲染；label 固定为 结束原因。 -->
          <el-descriptions-item v-if="row.finishReason" label="结束原因"><!-- 把 reasons[row.finishReason] || "执行已结束" 的当前值转成文字显示。 -->{{
            reasons[row.finishReason] || "执行已结束"
          }}</el-descriptions-item>
          <!-- 显示Element Plus 的 descriptions-item 组件；仅当 row.status === 'RUNNING' 成立时渲染；label 固定为 本段执行边界。 -->
          <el-descriptions-item
            v-if="row.status === 'RUNNING'"
            label="本段执行边界"
            ><!-- 把 instantText(row.eligibleUntil) 的当前值转成文字显示。 -->{{ instantText(row.eligibleUntil) }}</el-descriptions-item
          >
        </el-descriptions>
        <!-- 显示一个布局容器；用样式类 toolbar detail-toolbar 控制外观。 -->
        <div class="toolbar detail-toolbar">
          <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'RUNNING' && canGrow('execution:session:pause') 成立时渲染；disabled 取自 !!execution.busy；click 事件触发时执行 control('pause')。 -->
          <el-button
            v-if="
              row.status === 'RUNNING' && canGrow('execution:session:pause')
            "
            :disabled="!!execution.busy"
            @click="control('pause')"
            >暂停执行</el-button
          >
          <!-- 显示Element Plus 的 button 组件；仅当 row.status === 'PAUSED' && canGrow('execution:session:resume') 成立时渲染；类型设为 primary；disabled 取自 !!execution.busy；click 事件触发时执行 control('resume')。 -->
          <el-button
            v-if="
              row.status === 'PAUSED' && canGrow('execution:session:resume')
            "
            type="primary"
            :disabled="!!execution.busy"
            @click="control('resume')"
            >继续原会话</el-button
          >
          <!-- 显示Element Plus 的 button 组件；仅当 row.status !== 'FINISHED' && canGrow('execution:session:finish') 成立时渲染；disabled 取自 !!execution.busy；类型设为 danger；启用 plain 属性；click 事件触发时执行 control('finish')。 -->
          <el-button
            v-if="
              row.status !== 'FINISHED' && canGrow('execution:session:finish')
            "
            :disabled="!!execution.busy"
            type="danger"
            plain
            @click="control('finish')"
            >结束执行</el-button
          >
          <!-- 显示Element Plus 的 button 组件；click 事件触发时执行 load；loading 取自 busy。 -->
          <el-button @click="load" :loading="busy">刷新详情</el-button>
        </div>
        <!-- 显示一段文字；仅当 row.status === 'RUNNING' && !row.canHeartbeat 成立时渲染；用样式类 hint 控制外观。 -->
        <p v-if="row.status === 'RUNNING' && !row.canHeartbeat" class="hint">
          当前由其他登录端控制。可先暂停，再在本端继续以接管；此页面不会代发心跳。
        </p>
        <!-- 显示三级标题。 -->
        <h3>连续执行时间段</h3>
        <!-- 显示Element Plus 的 table 组件；data 取自 segments.rows；loading 指令使用 busy；empty-text 固定为 尚无执行时间段。 -->
        <el-table
          :data="segments.rows"
          v-loading="busy"
          empty-text="尚无执行时间段"
        >
          <!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 empty 插槽。 -->
          <template #empty
            ><!-- 显示应用空状态当前状态（AppEmptyState）组件；description 取自 error ? '加载失败，请查看提示后重试。' : '尚无执行时间段'。 --><AppEmptyState
              :description="
                error ? '加载失败，请查看提示后重试。' : '尚无执行时间段'
              " /></template
          ><!-- 显示Element Plus 的 table-column 组件；prop 固定为 segmentNo；label 固定为 段；width 固定为 64。 --><el-table-column prop="segmentNo" label="段" width="64" />
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 开始 / 继续；min-width 固定为 180。 -->
          <el-table-column label="开始 / 继续" min-width="180"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row: s }。 --><template #default="{ row: s }"><!-- 把 instantText(s.startedAt, s.timeZone) 的当前值转成文字显示。 -->{{
              instantText(s.startedAt, s.timeZone)
            }}</template></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 暂停 / 结束；min-width 固定为 180。 -->
          <el-table-column label="暂停 / 结束" min-width="180"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row: s }。 --><template #default="{ row: s }"><!-- 把 s.endedAt ? instantText(s.endedAt, s.timeZone) : "运行中，按有效截止封顶" 的当前值转成文字显示。 -->{{
              s.endedAt
                ? instantText(s.endedAt, s.timeZone)
                : "运行中，按有效截止封顶"
            }}</template></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 实际投入；min-width 固定为 110。 -->
          <el-table-column label="实际投入" min-width="110"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row: s }。 --><template #default="{ row: s }"><!-- 把 duration(s.actualSeconds) 的当前值转成文字显示。 -->{{
              duration(s.actualSeconds)
            }}</template></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；label 固定为 原因；min-width 固定为 150。 -->
          <el-table-column label="原因" min-width="150"
            ><!-- 显示只组织渲染规则、不额外生成普通容器的模板；定义 default 插槽，接收 { row: s }。 --><template #default="{ row: s }"><!-- 把 reasons[s.endReason] || "尚未结束" 的当前值转成文字显示。 -->{{
              reasons[s.endReason] || "尚未结束"
            }}</template></el-table-column
          >
          <!-- 显示Element Plus 的 table-column 组件；prop 固定为 timeZone；label 固定为 开始时区；min-width 固定为 150。 -->
          <el-table-column prop="timeZone" label="开始时区" min-width="150" />
        </el-table>
        <!-- 显示当前页Pagination（PagePagination）组件；state 取自 segments；change 事件触发时执行 loadSegments。 -->
        <PagePagination :state="segments" @change="loadSegments" />
      </template>
    </LoadState>
  </component>
</template>
