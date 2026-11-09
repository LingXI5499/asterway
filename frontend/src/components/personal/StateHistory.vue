<!-- 渲染当前状态历史界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
// 在任务详情中使用居中弹窗，其他入口沿用原来的侧边展示。
import { ElDialog, ElDrawer } from "element-plus";
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { ref, reactive, computed, watch, onBeforeUnmount } from "vue";
/* 引入 ../../api/personal 提供的组件、状态函数或请求方法。 */
import {
  stateHistory,
  instantText,
  actions,
  sources,
} from "../../api/personal";
/* 引入 ../../api/growth 提供的组件、状态函数或请求方法。 */
import { states } from "../../api/growth";
/* 引入 ./PagePagination.vue 提供的组件、状态函数或请求方法。 */
import PagePagination from "./PagePagination.vue";
/* 引入 ./LoadState.vue 提供的组件、状态函数或请求方法。 */
import LoadState from "./LoadState.vue";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({
  /* 模型值（modelValue）字段使用可为空的真假值（Boolean）。 */
  modelValue: Boolean, centered: Boolean,
  /* 种类（kind）字段使用可为空的文字（String）。 */
  kind: String,
  /* 编号（id）字段使用表达式 [String, Number] 的结果。 */
  id: [String, Number],
});
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["update:modelValue"]);
const /* 是否显示（visible）保存随相关状态变化自动重算的值。 */ visible = computed({
  /* 读取（get）字段使用表达式 () => props.modelValue 的结果。 */
  get: () => props.modelValue,
  /* 设置（set）字段使用表达式 (v) => emit("update:modelValue", v) 的结果。 */
  set: (v) => emit("update:modelValue", v),
});
const /* 列表（list）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */ list = reactive({ /* 记录列表（rows）字段使用一个空列表，用来保存后续多条数据。 */ rows: [], /* 总数（total）字段使用固定数值 0。 */ total: 0, /* 当前页（page）字段使用固定数值 1。 */ page: 1, /* 每页条数（size）字段使用固定数值 20。 */ size: 20 }),
  /* 正在处理（busy）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  busy = ref(false),
  /* 错误（error）保存可被页面跟踪变化的状态，值改变时相关界面会更新。 */
  error = ref("");
let /* 当前最新请求序号（sequence）保存固定数值 0。 */ sequence = 0,
  /* 声明 用于取消旧请求的控制器（controller），稍后再填入处理结果。 */
  controller;
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
    const /* 数据（data）保存等待 调用 stateHistory 完成当前状态历史处理 完成后取得结果。 */ data = await stateHistory(
      props.kind,
      props.id,
      { /* 当前页（page）字段使用表达式 list.page 的结果。 */ page: list.page, /* 每页条数（size）字段使用表达式 list.size 的结果。 */ size: list.size },
      controller.signal,
    );
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence)
      /* 调用 Object.assign 完成安排处理。 */
      Object.assign(list, { /* 记录列表（rows）字段使用表达式 data.items 的结果。 */ rows: data.items, /* 总数（total）字段使用表达式 data.total 的结果。 */ total: data.total });
  } /* 捕获前面处理失败的原因，并转换为页面提示或恢复状态。 */ catch (e) {
    /* 检查条件 seq === sequence && e.name !== "AbortError" 是否成立，成立时走这一分支。 */
    if (seq === sequence && e.name !== "AbortError") /* 把表达式 e.message 的结果写入 error.value。 */ error.value = e.message;
  } finally {
    /* 检查条件 seq === sequence 是否成立，成立时走这一分支。 */
    if (seq === sequence) /* 把假，表示条件不成立或开关关闭写入 busy.value。 */ busy.value = false;
  }
}
/* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */
watch(
  () => [props.modelValue, props.kind, props.id],
  () => {
    /* 推进这次请求或循环的序号，使较早返回的结果可以被识别并丢弃。 */
    sequence++;
    /* 把固定数值 1写入 list.page。 */
    list.page = 1;
    /* 把一个空列表，用来保存后续多条数据写入 list.rows。 */
    list.rows = [];
    /* 检查条件 props.modelValue 是否成立，成立时走这一分支。 */
    if (props.modelValue) /* 读取当前对象或当前对话的数据，并用请求序号丢弃过时结果。 */ load();
    else /* 取消尚未完成的旧请求，避免过时结果覆盖当前页面。 */ controller?.abort();
  },
  { /* immediate字段使用真，表示条件成立或开关打开。 */ immediate: true },
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
  <!-- 显示Element Plus 的 drawer 组件；与 visible 双向同步；title 固定为 真实状态历史；size 固定为 min(800px, 100vw)；启用 destroy-on-close 属性。 -->
  <component :is="centered ? ElDialog : ElDrawer"
    v-model="visible"
    title="真实状态历史"
    v-bind="centered ? { width: 'min(800px,94vw)', alignCenter: true, appendToBody: true, class: 'plan-dialog' } : { size: 'min(800px, 100vw)' }"
    destroy-on-close
  >
    <!-- 显示加载当前状态（LoadState）组件；loading 取自 busy && !list.rows.length；error 取自 error；retry 事件触发时执行 load。 -->
    <LoadState
      :loading="busy && !list.rows.length"
      :error="error"
      @retry="load"
    >
      <!-- 显示Element Plus 的 timeline 组件；仅当 list.rows.length 成立时渲染；用样式类 fact-timeline 控制外观。 -->
      <el-timeline v-if="list.rows.length" class="fact-timeline"
        ><!-- 显示Element Plus 的 timeline-item 组件；按 row in list.rows 为每条数据重复生成内容；key 取自 row.id；timestamp 取自 instantText(row.occurredAt)；placement 固定为 top。 --><el-timeline-item
          v-for="row in list.rows"
          :key="row.id"
          :timestamp="instantText(row.occurredAt)"
          placement="top"
        >
          <!-- 显示一条独立内容；用样式类 timeline-fact 控制外观。 -->
          <article class="timeline-fact">
            <!-- 显示重点文字。 -->
            <strong
              ><!-- 把 actions[row.eventType] || "状态事件" 的当前值转成文字显示。 -->{{ actions[row.eventType] || "状态事件" }} ·
              <!-- 把 row.nameSnapshot 的当前值转成文字显示。 -->
              {{ row.nameSnapshot }}</strong
            >
            <!-- 显示一段文字。 -->
            <p>
              <!-- 把 row.beforeStatus ? states[row.beforeStatus] + " → " : "" 的当前值转成文字显示。 -->
              {{ row.beforeStatus ? states[row.beforeStatus] + " → " : ""
              }}<!-- 把 states[row.afterStatus] 的当前值转成文字显示。 -->{{ states[row.afterStatus] }}
            </p>
            <!-- 显示一段文字；仅当 kind === 'goal' && row.beforeArchived !== null 成立时渲染；用样式类 hint 控制外观。 -->
            <p
              v-if="kind === 'goal' && row.beforeArchived !== null"
              class="hint"
            >
              归档状态：<!-- 把 row.beforeArchived ? "已归档" : "未归档" 的当前值转成文字显示。 -->{{ row.beforeArchived ? "已归档" : "未归档" }} →
              <!-- 把 row.afterArchived ? "已归档" : "未归档" 的当前值转成文字显示。 -->
              {{ row.afterArchived ? "已归档" : "未归档" }}
            </p>
            <!-- 显示一段文字；用样式类 hint 控制外观。 -->
            <p class="hint">
              <!-- 把 sources[row.sourceType] 的当前值转成文字显示。 -->
              {{ sources[row.sourceType]
              }}<!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 row.eventType === 'BASELINE' 成立时渲染。 --><template v-if="row.eventType === 'BASELINE'">
                · 初始化基线，不计历史完成</template
              >
            </p>
          </article>
        </el-timeline-item></el-timeline
      ><!-- 显示应用空状态当前状态（AppEmptyState）组件；前面条件均不成立时显示这个备用分支；description 固定为 还没有状态历史。 --><AppEmptyState v-else description="还没有状态历史" />
      <!-- 显示当前页Pagination（PagePagination）组件；state 取自 list；change 事件触发时执行 load。 -->
      <PagePagination :state="list" @change="load" />
    </LoadState>
  </component>
</template>
