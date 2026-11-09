<!-- 渲染成长助手复盘Card界面；脚本管理数据，模板绑定事件，样式控制布局和显示。 -->
<script setup>
/* 引入 vue 提供的组件、状态函数或请求方法。 */
import { computed } from "vue";
/* 引入 ./AgentMarkdown.vue 提供的组件、状态函数或请求方法。 */
import AgentMarkdown from "./AgentMarkdown.vue";
/* 引入 ../../composables/useConversationReview 提供的组件、状态函数或请求方法。 */
import { useConversationReview } from "../../composables/useConversationReview";
const /* 父组件传入的数据（props）保存声明父组件可以传入的属性及其类型或默认值。 */ props = defineProps({ /* 对话编号（conversationId）字段使用可为空的文字（String）。 */ conversationId: String, /* 来源方案运行编号（sourceRunId）字段使用可为空的文字（String）。 */ sourceRunId: String, /* 事件（event）字段使用对象（Object）。 */ event: Object, /* external编号（externalId）字段使用可为空的文字（String）。 */ externalId: String, /* 正在处理（busy）字段使用可为空的真假值（Boolean）。 */ busy: Boolean, /* 是否展开（open）字段使用可为空的真假值（Boolean）。 */ open: Boolean });
const /* 向父组件发送事件的函数（emit）保存声明当前组件可以通知父组件的事件。 */ emit = defineEmits(["working", "adjusted", "reveal"]);
const /* 复盘报告错误操作进行中操作是否可以复盘是否可以Adjust开始重试adjust加载（{ report, error, acting, action, canReview, canAdjust, start, retry, adjust, load }）保存组合复盘展示、重试以及生成调整草案的状态。 */ { /* 直接使用已有的复盘报告（report）作为同名字段。 */ report, /* 直接使用已有的错误（error）作为同名字段。 */ error, /* 直接使用已有的操作进行中（acting）作为同名字段。 */ acting, /* 直接使用已有的操作（action）作为同名字段。 */ action, /* 直接使用已有的是否可以复盘（canReview）作为同名字段。 */ canReview, /* 直接使用已有的是否可以Adjust（canAdjust）作为同名字段。 */ canAdjust, /* 直接使用已有的开始（start）作为同名字段。 */ start, /* 直接使用已有的重试（retry）作为同名字段。 */ retry, /* 直接使用已有的adjust作为同名字段。 */ adjust, /* 直接使用已有的加载（load）作为同名字段。 */ load } = useConversationReview(props, emit);
const /* 等待处理（pending）保存随相关状态变化自动重算的值。 */ pending = computed(() => ["PENDING", "RUNNING"].includes(report.value?.status));
</script>

<template>
  <!-- 显示一个有独立含义的内容区；用样式类 review-card 控制外观；aria-label 提供无障碍说明 执行反馈与复盘。 -->
  <section class="review-card" aria-label="执行反馈与复盘">
    <!-- 显示一个布局容器；用样式类 review-heading 控制外观。 -->
    <div class="review-heading"><!-- 显示应用Icon（AppIcon）组件；name 固定为 history；size 取自 16。 --><AppIcon name="history" :size="16" /><!-- 显示三级标题。 --><h3>执行反馈与复盘</h3></div>
    <!-- 显示一段文字；仅当 !report 成立时渲染。 -->
    <p v-if="!report"><!-- 把 conversationId ? '根据实际投入、任务完成和逾期情况，检查当前安排，再决定下一步。' : '开始一次对话后，即可在这里复盘执行并调整安排。' 的当前值转成文字显示。 -->{{ conversationId ? '根据实际投入、任务完成和逾期情况，检查当前安排，再决定下一步。' : '开始一次对话后，即可在这里复盘执行并调整安排。' }}</p>
    <!-- 显示只组织渲染规则、不额外生成普通容器的模板；仅当 report 成立时渲染。 -->
    <template v-if="report">
      <!-- 显示一段文字；用样式类 review-range 控制外观。 -->
      <p class="review-range"><!-- 把 report.dateFrom 的当前值转成文字显示。 -->{{ report.dateFrom }} 至 <!-- 把 report.dateTo 的当前值转成文字显示。 -->{{ report.dateTo }} · <!-- 把 report.timeZone 的当前值转成文字显示。 -->{{ report.timeZone }}</p>
      <!-- 显示Element Plus 的 skeleton 组件；仅当 pending 成立时渲染；rows 取自 3；启用 animated 属性。 -->
      <el-skeleton v-if="pending" :rows="3" animated /><!-- 显示一段文字；仅当 pending 成立时渲染；role 固定为 status。 --><p v-if="pending" role="status">正在分析真实记录，完成后自动更新。</p>
      <!-- 显示Element Plus 的 alert 组件；仅当 report.status === 'FAILED' 成立时渲染；title 取自 report.errorMessage || '复盘暂未完成，可重新生成'；类型设为 warning；closable 取自 false。 -->
      <el-alert v-if="report.status === 'FAILED'" :title="report.errorMessage || '复盘暂未完成，可重新生成'" type="warning" :closable="false" />
      <!-- 显示可展开的详情区域；仅当 report.status === 'READY' 成立时渲染；用样式类 review-analysis 控制外观。 -->
      <details v-if="report.status === 'READY'" class="review-analysis"><!-- 显示详情区域的展开入口。 --><summary>查看复盘与调整依据</summary><!-- 显示成长助手Markdown（AgentMarkdown）组件；content 取自 report.content。 --><AgentMarkdown :content="report.content" /></details>
      <!-- 显示一段文字；仅当 report.status === 'READY' 成立时渲染。 -->
      <p v-if="report.status === 'READY'">可据此整理后续安排。调整方案会先展示改期前后信息，接受后才更新。</p>
    </template>
    <!-- 显示Element Plus 的 alert 组件；仅当 error 成立时渲染；title 取自 error；类型设为 error；closable 取自 false。 -->
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <!-- 显示一个布局容器；用样式类 review-actions 控制外观。 -->
    <div class="review-actions">
      <!-- 显示Element Plus 的 button 组件；仅当 canAdjust 成立时渲染；类型设为 primary；loading 取自 acting && action === 'adjust'；disabled 取自 busy || acting；click 事件触发时执行 adjust。 -->
      <el-button v-if="canAdjust" type="primary" :loading="acting && action === 'adjust'" :disabled="busy || acting" @click="adjust">根据复盘调整计划</el-button>
      <!-- 显示Element Plus 的 button 组件；仅当 canReview && report?.status === 'FAILED' 成立时渲染；loading 取自 acting；disabled 取自 busy || acting；click 事件触发时执行 retry。 -->
      <el-button v-if="canReview && report?.status === 'FAILED'" :loading="acting" :disabled="busy || acting" @click="retry">重新生成复盘</el-button>
      <!-- 显示Element Plus 的 button 组件；前一条件不成立且 canReview && !pending 成立时渲染；loading 取自 acting && action === 'review'；disabled 取自 busy || acting || !conversationId；click 事件触发时执行 start。 -->
      <el-button v-else-if="canReview && !pending" :loading="acting && action === 'review'" :disabled="busy || acting || !conversationId" @click="start"><!-- 把 report ? '复盘最近七天' : '复盘执行情况' 的当前值转成文字显示。 -->{{ report ? '复盘最近七天' : '复盘执行情况' }}</el-button>
      <!-- 显示Element Plus 的 button 组件；仅当 pending 成立时渲染；启用 text 属性；disabled 取自 acting；click 事件触发时执行 load(report.id)。 -->
      <el-button v-if="pending" text :disabled="acting" @click="load(report.id)">刷新复盘</el-button>
    </div>
    <!-- 显示一段文字；仅当 acting && action === 'adjust' 成立时渲染；role 固定为 status。 -->
    <p v-if="acting && action === 'adjust'" role="status">正在核对当前安排并整理调整方案…</p>
  </section>
</template>

<style scoped>
/* 以下规则应用于 .review-card 选中的元素。 */
.review-card { /* 设置元素外侧留白；取值 24px 0。 */ margin: 24px 0; /* 设置内容与边框之间的内侧留白；取值 16px。 */ padding: 16px; /* 设置边框宽度、线型与颜色；取值 1px solid var(—aw-border)。 */ border: 1px solid var(--aw-border); /* 设置边角圆弧大小；取值 12px。 */ border-radius: 12px; /* 设置背景颜色、图片或渐变；取值 var(—aw-bg)。 */ background: var(--aw-bg); }
/* 以下规则应用于 .review-heading 选中的元素。 */
.review-heading { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 设置 Flex 或网格条目在交叉方向上的对齐；取值 center。 */ align-items: center; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; }
/* 以下规则应用于 h3 选中的元素。 */
h3 { /* 设置元素外侧留白；取值 0。 */ margin: 0; /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; /* 设置字重；取值 600。 */ font-weight: 600; }
/* 以下规则应用于 p, summary 选中的元素。 */
p, summary { /* 设置字号；取值 12px。 */ font-size: 12px; /* 设置每行文字的高度；取值 16px。 */ line-height: 16px; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text-secondary)。 */ color: var(--aw-text-secondary); /* 设置元素外侧留白；取值 8px 0。 */ margin: 8px 0; }
/* 以下规则应用于 .review-range 选中的元素。 */
.review-range { /* 设置元素外侧留白；取值 12px 0。 */ margin: 12px 0; }
/* 以下规则应用于 .review-analysis summary 选中的元素。 */
.review-analysis summary { /* 设置鼠标移上来时的指针形状；取值 pointer。 */ cursor: pointer; /* 设置文字或继承该颜色的图形颜色；取值 var(—aw-text)。 */ color: var(--aw-text); }
/* 以下规则应用于 .review-analysis :deep(.agent-markdown) 选中的元素。 */
.review-analysis :deep(.agent-markdown) { /* 设置字号；取值 14px。 */ font-size: 14px; /* 设置每行文字的高度；取值 20px。 */ line-height: 20px; }
/* 以下规则应用于 .review-actions 选中的元素。 */
.review-actions { /* 决定元素采用普通块、行内、Flex 或网格布局；取值 flex。 */ display: flex; /* 决定一行放不下时是否换行；取值 wrap。 */ flex-wrap: wrap; /* 设置相邻布局条目之间的间隔；取值 8px。 */ gap: 8px; /* 设置上方外侧留白；取值 16px。 */ margin-top: 16px; }
/* 以下规则应用于 .review-actions :deep(.el-button + .el-button) 选中的元素。 */
.review-actions :deep(.el-button + .el-button) { /* 设置左侧外侧留白；取值 0。 */ margin-left: 0; }
/* 以下规则应用于 .review-actions :deep(.el-button) 选中的元素。 */
.review-actions :deep(.el-button) { /* 限制最大宽度；取值 100%。 */ max-width: 100%; /* 设置字号；取值 14px。 */ font-size: 14px; }
</style>
